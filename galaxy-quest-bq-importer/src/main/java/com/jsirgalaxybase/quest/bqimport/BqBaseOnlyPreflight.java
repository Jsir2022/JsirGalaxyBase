package com.jsirgalaxybase.quest.bqimport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.sql.DataSource;

import com.jsirgalaxybase.quest.core.QuestChapterCatalogEntry;
import com.jsirgalaxybase.quest.core.StoredQuestChapter;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.postgres.JdbcQuestCenterQuery;
import com.jsirgalaxybase.quest.postgres.JdbcQuestChapterRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestConnectionManager;
import com.jsirgalaxybase.quest.postgres.JdbcQuestContentMigrationRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestDefinitionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestTransaction;
import com.jsirgalaxybase.quest.postgres.QuestContentMigrationBatch;
import com.jsirgalaxybase.quest.postgres.QuestContentMigrationItem;

/** Read-only preflight for an isolated Base-only Quest installation. */
public final class BqBaseOnlyPreflight {
    private static final Pattern SHA256 = Pattern.compile("(?i)[0-9a-f]{64}");
    private static final String ISOLATED_ACK = "ISOLATED_ONLY";

    private BqBaseOnlyPreflight() { }

    public static ArtifactReport verifyArtifact(Path jarPath, String expectedSha256) throws IOException {
        requireSha256(expectedSha256, "expected Base JAR SHA-256");
        if (jarPath == null || !Files.isRegularFile(jarPath) || !Files.isReadable(jarPath)) {
            throw new IllegalArgumentException("Base JAR is missing or unreadable: " + jarPath);
        }
        String actualSha256 = sha256(jarPath);
        if (!actualSha256.equalsIgnoreCase(expectedSha256)) {
            throw new IllegalStateException("Base JAR SHA-256 mismatch: expected " + expectedSha256
                + " but found " + actualSha256);
        }
        int classes = 0;
        boolean galaxyBase = false;
        boolean questModule = false;
        try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".class")) continue;
                classes++;
                String normalized = name.toLowerCase(Locale.ROOT);
                if (normalized.equals("com/jsirgalaxybase/galaxybase.class")) galaxyBase = true;
                if (normalized.equals("com/jsirgalaxybase/modules/quest/questmodule.class")) questModule = true;
                if (normalized.startsWith("com/jsirgalaxybase/quest/bqimport/")
                    || normalized.startsWith("com/google/gson/")
                    || normalized.startsWith("com/jsirgalaxybase/ui2/demo/")
                    || normalized.startsWith("betterquesting/")) {
                    throw new IllegalStateException("Base JAR contains a forbidden importer/demo/Gson/BQ class: "
                        + name);
                }
            }
        }
        if (!galaxyBase) throw new IllegalStateException("Base JAR is missing com/jsirgalaxybase/GalaxyBase.class");
        if (!questModule) throw new IllegalStateException("Base JAR is missing the QuestModule runtime entry");
        if (classes == 0) throw new IllegalStateException("Base JAR contains no class files");
        return new ArtifactReport(actualSha256, classes);
    }

    /** Restricts this CLI to an explicitly acknowledged local test database. */
    public static void requireIsolatedLoopbackUrl(String jdbcUrl, String confirmation) {
        if (!ISOLATED_ACK.equals(confirmation)) {
            throw new IllegalArgumentException("isolation confirmation must equal ISOLATED_ONLY");
        }
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("preflight requires an explicit jdbc:postgresql:// URL");
        }
        try {
            URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
            String host = uri.getHost();
            String path = uri.getPath();
            String normalizedHost = host == null ? "" : host.toLowerCase(Locale.ROOT);
            if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
                normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
            }
            if (!"postgresql".equals(uri.getScheme()) || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null) {
                throw new IllegalArgumentException("isolated JDBC URL may not contain credentials, query options, or fragments");
            }
            if (!("localhost".equals(normalizedHost) || "127.0.0.1".equals(normalizedHost)
                || "::1".equals(normalizedHost))) {
                throw new IllegalArgumentException("isolated database host must be localhost/loopback, not "
                    + (host == null ? "<missing>" : host));
            }
            if (uri.getPort() == 0 || uri.getPort() > 65535) {
                throw new IllegalArgumentException("isolated database port is invalid");
            }
            if (path == null || !path.matches("/[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException("isolated JDBC URL must name exactly one database");
            }
            String database = path.substring(1);
            if (!database.matches("(?i)[a-z0-9_]*(?:_test|_it)(?:_[a-z0-9_]+)?")
                || "galaxy-base".equalsIgnoreCase(database)) {
                throw new IllegalArgumentException("preflight database name must end in _test or _it: " + database);
            }
        } catch (IllegalArgumentException invalid) {
            throw invalid;
        } catch (RuntimeException malformed) {
            throw new IllegalArgumentException("preflight JDBC URL is malformed", malformed);
        }
    }

    /** Checks schema, published content and the applied import without writing or reading player progress. */
    public static DatabaseReport verifyInitializedDatabase(BqMigrationManifest manifest, DataSource dataSource) {
        if (manifest == null || dataSource == null) throw new IllegalArgumentException("manifest and data source are required");
        if (!manifest.isApplicable()) throw new IllegalStateException("source manifest is blocked");
        BqBehaviorInventory.from(manifest).requireRuntimeReady();

        JdbcQuestConnectionManager connections = new JdbcQuestConnectionManager(new ReadOnlyDataSource(dataSource));
        JdbcQuestCenterQuery center = new JdbcQuestCenterQuery(connections);
        center.validateSchema();
        center.validateSuppressedRewardStatus();

        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(connections);
        JdbcQuestChapterRepository chapters = new JdbcQuestChapterRepository(connections);
        JdbcQuestContentMigrationRepository migrations = new JdbcQuestContentMigrationRepository(connections);
        Optional<QuestContentMigrationBatch> applied = migrations.find(manifest.getBatchId());
        if (!applied.isPresent()) throw new IllegalStateException("source import batch is missing: " + manifest.getBatchId());
        QuestContentMigrationBatch batch = applied.get();
        if (batch.getStatus() != QuestContentMigrationBatch.Status.APPLIED) {
            throw new IllegalStateException("source import batch is not APPLIED: " + batch.getStatus());
        }
        if (!manifest.getSourceHash().equals(batch.getSourceHash())) {
            throw new IllegalStateException("applied batch source hash does not match the fixed source");
        }
        if (batch.getItems().size() != manifest.getQuests().size() + manifest.getChapters().size()) {
            throw new IllegalStateException("applied batch item count does not match source entity count");
        }

        BqMigrationDatabaseService migrationService = new BqMigrationDatabaseService(
            new JdbcQuestTransaction(connections), definitions, chapters, migrations);
        BqMigrationDryRun current = migrationService.dryRun(manifest);
        if (!current.isApplicable()) throw new IllegalStateException("source manifest became blocked during preflight");
        if (current.count(QuestContentMigrationItem.Action.CREATED) != 0
            || current.count(QuestContentMigrationItem.Action.VERSIONED) != 0
            || current.count(QuestContentMigrationItem.Action.UNCHANGED)
                != manifest.getQuests().size() + manifest.getChapters().size()) {
            throw new IllegalStateException("published content differs from source: created="
                + current.count(QuestContentMigrationItem.Action.CREATED) + ", versioned="
                + current.count(QuestContentMigrationItem.Action.VERSIONED) + ", unchanged="
                + current.count(QuestContentMigrationItem.Action.UNCHANGED));
        }

        List<StoredQuestDefinition> publishedQuests = definitions.findAllPublished();
        List<StoredQuestChapter> publishedChapters = chapters.findAllPublished();
        if (publishedQuests.size() != manifest.getQuests().size()) {
            throw new IllegalStateException("published quest count differs from source: expected "
                + manifest.getQuests().size() + " but found " + publishedQuests.size());
        }
        if (publishedChapters.size() != manifest.getChapters().size()) {
            throw new IllegalStateException("published chapter count differs from source: expected "
                + manifest.getChapters().size() + " but found " + publishedChapters.size());
        }
        List<String> expectedOrder = new ArrayList<String>();
        List<BqImportedChapter> ordered = new ArrayList<BqImportedChapter>(manifest.getChapters());
        Collections.sort(ordered, Comparator.comparingInt(BqImportedChapter::getDisplayOrder));
        for (BqImportedChapter chapter : ordered) expectedOrder.add(chapter.getDefinition().getId().toString());
        List<String> actualOrder = new ArrayList<String>();
        for (QuestChapterCatalogEntry chapter : chapters.listOrdered()) actualOrder.add(chapter.getChapterId().toString());
        if (!expectedOrder.equals(actualOrder)) throw new IllegalStateException("chapter catalog order differs from source");
        return new DatabaseReport(manifest.getBatchId(), manifest.getSourceHash(), publishedQuests.size(),
            publishedChapters.size(), current.count(QuestContentMigrationItem.Action.UNCHANGED));
    }

    private static String sha256(Path path) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[16384];
            for (int read; (read = input.read(buffer)) >= 0;) if (read > 0) digest.update(buffer, 0, read);
        }
        StringBuilder value = new StringBuilder(64);
        for (byte current : digest.digest()) value.append(String.format(Locale.ROOT, "%02x", current & 0xff));
        return value.toString();
    }

    private static void requireSha256(String value, String label) {
        if (value == null || !SHA256.matcher(value).matches()) throw new IllegalArgumentException(label + " must be 64 hex characters");
    }

    public static final class ArtifactReport {
        private final String sha256;
        private final int classCount;
        private ArtifactReport(String sha256, int classCount) { this.sha256 = sha256; this.classCount = classCount; }
        public String getSha256() { return sha256; }
        public int getClassCount() { return classCount; }
    }

    public static final class DatabaseReport {
        private final String batchId, sourceHash;
        private final int quests, chapters, unchangedEntities;
        private DatabaseReport(String batchId, String sourceHash, int quests, int chapters, int unchangedEntities) {
            this.batchId = batchId; this.sourceHash = sourceHash; this.quests = quests;
            this.chapters = chapters; this.unchangedEntities = unchangedEntities;
        }
        public String getBatchId() { return batchId; }
        public String getSourceHash() { return sourceHash; }
        public int getQuests() { return quests; }
        public int getChapters() { return chapters; }
        public int getUnchangedEntities() { return unchangedEntities; }
    }

    private static final class ReadOnlyDataSource implements DataSource {
        private final DataSource delegate;
        private ReadOnlyDataSource(DataSource delegate) { this.delegate = delegate; }
        @Override public Connection getConnection() throws SQLException { return readOnly(delegate.getConnection()); }
        @Override public Connection getConnection(String username, String password) throws SQLException {
            return readOnly(delegate.getConnection(username, password));
        }
        private static Connection readOnly(Connection connection) throws SQLException {
            try { connection.setReadOnly(true); return connection; }
            catch (SQLException failure) { try { connection.close(); } catch (SQLException ignored) { } throw failure; }
        }
        @Override public java.io.PrintWriter getLogWriter() throws SQLException { return delegate.getLogWriter(); }
        @Override public void setLogWriter(java.io.PrintWriter out) throws SQLException { delegate.setLogWriter(out); }
        @Override public void setLoginTimeout(int seconds) throws SQLException { delegate.setLoginTimeout(seconds); }
        @Override public int getLoginTimeout() throws SQLException { return delegate.getLoginTimeout(); }
        @Override public java.util.logging.Logger getParentLogger()
            throws java.sql.SQLFeatureNotSupportedException { return delegate.getParentLogger(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
    }
}

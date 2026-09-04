package com.jsirgalaxybase.modules.servertools.infrastructure.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.DriverManagerDataSource;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequestStatus;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.domain.RandomTeleportRecord;

public class ServerToolsPostgresIntegrationTest {

    private TestConfig config;
    private TestContext context;
    private JdbcPlayerTeleportRepository repository;
    private JdbcServerToolsAdminRepository adminRepository;

    @Before
    public void setUp() throws Exception {
        config = TestConfig.resolve();
        Assume.assumeTrue("PostgreSQL integration config not available", config.isConfigured());
        Assume.assumeTrue("PostgreSQL integration config is not reachable", config.canConnect());
        context = TestContext.create(config);
        repository = new JdbcPlayerTeleportRepository(new JdbcConnectionManager(context.schemaDataSource));
        adminRepository = new JdbcServerToolsAdminRepository(new JdbcConnectionManager(context.schemaDataSource));
    }

    @After
    public void tearDown() {
        if (context != null) context.close();
    }

    @Test
    public void factoryFailsFastWhenAcceptedTargetMigrationIsMissing() throws Exception {
        try (Connection connection = context.schemaDataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE player_tpa_request DROP COLUMN accepted_target_pitch");
        }
        try {
            JdbcServerToolsInfrastructureFactory.createShared(new JdbcConnectionManager(context.schemaDataSource));
            fail("Expected migrated TPA schema validation to fail");
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("player_tpa_request.accepted_target_pitch"));
            assertTrue(exception.getMessage().contains("scripts/db-migrate.sh"));
        }
    }

    @Test
    public void globalEntryRtpAuditMigrationUpgradesLegacyRecordRows() throws Exception {
        try (Connection connection = context.schemaDataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE player_rtp_record DROP CONSTRAINT player_rtp_record_request_id_key");
            statement.execute("ALTER TABLE player_rtp_record DROP COLUMN request_id, DROP COLUMN target_server_id");
            statement.execute("INSERT INTO player_rtp_record (player_uuid, source_server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch) VALUES ('legacy-player', 'lobby', 0, 1, 80, 2, 0, 0)");
            statement.execute(TestContext.GLOBAL_ENTRY_RTP_MIGRATION);
        }
        JdbcServerToolsInfrastructureFactory.createShared(new JdbcConnectionManager(context.schemaDataSource));
        try (Connection connection = context.schemaDataSource.getConnection(); Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT request_id, target_server_id FROM player_rtp_record WHERE player_uuid = 'legacy-player'")) {
            assertTrue(result.next());
            assertTrue(result.getString(1).startsWith("legacy-rtp-"));
            assertEquals("lobby", result.getString(2));
        }
    }

    @Test
    public void lifecycleMigrationUpgradesLegacyStatusConstraintAndAcceptedTargetColumns() throws Exception {
        try (Connection connection = context.schemaDataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE player_tpa_request DROP CONSTRAINT ck_player_tpa_request_accepted_target");
            statement.execute("ALTER TABLE player_tpa_request DROP CONSTRAINT ck_player_tpa_request_status");
            statement.execute("ALTER TABLE player_tpa_request DROP COLUMN accepted_target_dimension_id, DROP COLUMN accepted_target_x, DROP COLUMN accepted_target_y, DROP COLUMN accepted_target_z, DROP COLUMN accepted_target_yaw, DROP COLUMN accepted_target_pitch");
            statement.execute("ALTER TABLE player_tpa_request ADD CONSTRAINT ck_player_tpa_request_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED'))");
            statement.execute(TestContext.TPA_LIFECYCLE_MIGRATION);
        }
        JdbcServerToolsInfrastructureFactory.createShared(new JdbcConnectionManager(context.schemaDataSource));
        TpaRequest accepted = new TpaRequest("request-migrated", "requester-uuid", "Requester", "server-alpha",
            target("server-alpha", 0, 1, 65, 1), "Target", "server-beta", target("server-beta", 0, 2, 70, 2),
            TpaRequestStatus.ACCEPTED, Instant.now(), Instant.now().plusSeconds(30), Instant.now());
        repository.saveTpaRequest(accepted);
        assertEquals(TpaRequestStatus.ACCEPTED, repository.listAcceptedTpaRequestsForRequester("server-alpha",
            "requester-uuid", Instant.now()).get(0).getStatus());
    }

    @Test
    public void concurrentAcceptAndDeclineLeaveExactlyOneTerminalStateAndAcceptedRequestIsSourceScoped()
        throws Exception {
        Instant now = Instant.now();
        repository.saveTpaRequest(request("request-concurrent", now));
        Pair<Optional<TpaRequest>> result = concurrently(
            () -> repository.acceptPendingTpaRequest("Requester", "Target", "server-beta",
                target("server-beta", 4, 100, 72, -20), now.plusSeconds(1)),
            () -> repository.declinePendingTpaRequest("Requester", "Target", "server-beta", now.plusSeconds(1)));

        assertEquals(1, (result.first.isPresent() ? 1 : 0) + (result.second.isPresent() ? 1 : 0));
        if (result.first.isPresent()) {
            assertEquals(TpaRequestStatus.ACCEPTED, result.first.get().getStatus());
            assertEquals(1, repository.listAcceptedTpaRequestsForRequester("server-alpha", "requester-uuid",
                now.plusSeconds(2)).size());
            assertTrue(repository.listAcceptedTpaRequestsForRequester("server-beta", "requester-uuid",
                now.plusSeconds(2)).isEmpty());
        } else {
            assertEquals(TpaRequestStatus.DECLINED, result.second.get().getStatus());
            assertTrue(repository.listAcceptedTpaRequestsForRequester("server-alpha", "requester-uuid",
                now.plusSeconds(2)).isEmpty());
        }
    }

    @Test
    public void cancelAndExpiryOnlyChangePendingRequests() {
        Instant now = Instant.now();
        repository.saveTpaRequest(request("request-cancel", now));
        assertTrue(repository.cancelPendingTpaRequest("requester-uuid", "Target", "server-beta", now.plusSeconds(1))
            .isPresent());
        assertTrue(!repository.cancelPendingTpaRequest("requester-uuid", "Target", "server-beta", now.plusSeconds(2))
            .isPresent());

        TpaRequest expired = new TpaRequest("request-expired", "requester-uuid", "Requester", "server-alpha",
            target("server-alpha", 0, 1, 65, 1), "Target", "server-beta", TpaRequestStatus.PENDING,
            now.minusSeconds(60), now.minusSeconds(1), now.minusSeconds(60));
        repository.saveTpaRequest(expired);
        assertEquals(1, repository.expirePendingTpaRequests(now));
        boolean foundExpired = false;
        for (TpaRequest request : repository.listRecentTpaRequestsForRequester("server-alpha", "requester-uuid", 10)) {
            foundExpired |= request.getStatus() == TpaRequestStatus.EXPIRED;
        }
        assertTrue(foundExpired);
    }

    @Test
    public void administrativeWarpAndDirectoryUpdateAppendAuditRowsInSameSchema() throws Exception {
        try (Connection connection = context.schemaDataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO cluster_server_directory (server_id, display_name, local_server, enabled, created_at, updated_at) VALUES ('server-alpha', 'Alpha', TRUE, TRUE, now(), now())");
        }
        TeleportActor actor = new TeleportActor("admin-uuid", "Admin", "server-alpha",
            target("server-alpha", 0, 12, 72, -4));
        adminRepository.upsertLocalWarp(actor, "admin-warp-1", "hub", "Hub", "entry");
        adminRepository.setWarpEnabled(actor, "admin-warp-2", "hub", false);
        adminRepository.setServerEnabled(actor, "admin-server-1", "server-alpha", false);
        try (Connection connection = context.schemaDataSource.getConnection(); Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT count(*) FROM servertools_admin_operation")) {
            result.next(); assertEquals(3, result.getInt(1));
        }
        assertTrue(!adminRepository.findWarp("hub").get().isEnabled());
    }

    @Test
    public void targetServerRtpAuditStoresBothServersAndIsIdempotentByRequestId() throws Exception {
        RandomTeleportRecord first = repository.saveRandomTeleportRecord(new RandomTeleportRecord(0L, "rtp-request-1",
            "player-uuid", "lobby", target("s2", 7, 123, 80, -45), Instant.now()));
        RandomTeleportRecord replay = repository.saveRandomTeleportRecord(new RandomTeleportRecord(0L, "rtp-request-1",
            "player-uuid", "lobby", target("s2", 7, 999, 80, -999), Instant.now()));
        assertEquals(first.getRecordId(), replay.getRecordId());
        assertEquals("s2", replay.getTarget().getServerId());
        try (Connection connection = context.schemaDataSource.getConnection(); Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT source_server_id, target_server_id, count(*) OVER () FROM player_rtp_record")) {
            assertTrue(result.next());
            assertEquals("lobby", result.getString(1));
            assertEquals("s2", result.getString(2));
            assertEquals(1, result.getInt(3));
        }
    }

    private static TpaRequest request(String requestId, Instant now) {
        return new TpaRequest(requestId, "requester-uuid", "Requester", "server-alpha",
            target("server-alpha", 0, 1, 65, 1), "Target", "server-beta", TpaRequestStatus.PENDING, now,
            now.plusSeconds(30), now);
    }

    private static TeleportTarget target(String serverId, int dimension, double x, double y, double z) {
        return new TeleportTarget(serverId, dimension, x, y, z, 0.0F, 0.0F);
    }

    private static <T> Pair<T> concurrently(final Callable<T> first, final Callable<T> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> firstFuture = executor.submit(waiting(ready, start, first));
            Future<T> secondFuture = executor.submit(waiting(ready, start, second));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            return new Pair<T>(firstFuture.get(), secondFuture.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> Callable<T> waiting(final CountDownLatch ready, final CountDownLatch start,
        final Callable<T> callback) {
        return () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("concurrency start timed out");
            return callback.call();
        };
    }

    private static final class Pair<T> {

        private final T first;
        private final T second;

        private Pair(T first, T second) {
            this.first = first;
            this.second = second;
        }
    }

    private static final class TestContext {

        private static final String SERVER_TOOLS_DDL = loadDdl();
        private static final String TPA_LIFECYCLE_MIGRATION = loadMigration();
        private static final String GLOBAL_ENTRY_RTP_MIGRATION = loadGlobalEntryRtpMigration();
        private final DriverManagerDataSource baseDataSource;
        private final DriverManagerDataSource schemaDataSource;
        private final String schemaName;

        private TestContext(DriverManagerDataSource baseDataSource, DriverManagerDataSource schemaDataSource,
            String schemaName) {
            this.baseDataSource = baseDataSource;
            this.schemaDataSource = schemaDataSource;
            this.schemaName = schemaName;
        }

        private static TestContext create(TestConfig config) throws SQLException {
            DriverManagerDataSource base = new DriverManagerDataSource(config.jdbcUrl, config.username, config.password);
            String schema = "servertools_it_" + UUID.randomUUID().toString().replace("-", "");
            try (Connection connection = base.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA \"" + schema + "\"");
            }
            String schemaUrl = config.jdbcUrl + (config.jdbcUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
            DriverManagerDataSource scoped = new DriverManagerDataSource(schemaUrl, config.username, config.password);
            try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO \"" + schema + "\", public");
                statement.execute(SERVER_TOOLS_DDL);
            }
            return new TestContext(base, scoped, schema);
        }

        private void close() {
            try (Connection connection = baseDataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            } catch (SQLException ignored) {}
        }

        private static String loadDdl() {
            try {
                return new String(Files.readAllBytes(Paths.get("docs", "servertools-cluster-postgresql-ddl.sql")),
                    StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to load ServerTools DDL", exception);
            }
        }

        private static String loadMigration() {
            try {
                return new String(Files.readAllBytes(Paths.get("ops", "sql", "migrations",
                    "20260828_001_expand_tpa_request_lifecycle.sql")), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to load TPA lifecycle migration", exception);
            }
        }

        private static String loadGlobalEntryRtpMigration() {
            try {
                return new String(Files.readAllBytes(Paths.get("ops", "sql", "migrations",
                    "20260831_002_add_global_entry_rtp_audit.sql")), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to load global entry RTP migration", exception);
            }
        }
    }

    private static final class TestConfig {

        private final String jdbcUrl;
        private final String username;
        private final String password;

        private TestConfig(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }

        private static TestConfig resolve() {
            return new TestConfig(System.getenv("JGB_BANKING_IT_JDBC_URL"),
                System.getenv("JGB_BANKING_IT_JDBC_USERNAME"), System.getenv("JGB_BANKING_IT_JDBC_PASSWORD"));
        }

        private boolean isConfigured() {
            return notBlank(jdbcUrl) && notBlank(username) && notBlank(password);
        }

        private boolean canConnect() {
            if (!isConfigured()) return false;
            try {
                Class.forName("org.postgresql.Driver");
                try (Connection ignored = new DriverManagerDataSource(jdbcUrl, username, password).getConnection()) {
                    return true;
                }
            } catch (ClassNotFoundException | SQLException exception) {
                return false;
            }
        }

        private static boolean notBlank(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}

package com.jsirgalaxybase.quest.bqimport;

import java.nio.file.Paths;

import com.jsirgalaxybase.quest.postgres.DriverManagerQuestDataSource;

/** Explicit read-only entrypoint used before isolated Base-only Quest acceptance. */
public final class BqBaseOnlyPreflightMain {
    private BqBaseOnlyPreflightMain() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException(
            "Usage: BqBaseOnlyPreflightMain <DefaultQuests> <Base.jar> <expected-source-sha256> <expected-jar-sha256>");
        try {
            String expectedSourceHash = args[2];
            if (expectedSourceHash == null || !expectedSourceHash.matches("(?i)[0-9a-f]{64}")) {
                throw new IllegalArgumentException("expected source SHA-256 must be 64 hex characters");
            }
            BqMigrationManifest manifest = new BqMigrationPlanner().plan(Paths.get(args[0]));
            if (!expectedSourceHash.equalsIgnoreCase(manifest.getSourceHash())) {
                throw new IllegalStateException("DefaultQuests SHA-256 mismatch: expected " + expectedSourceHash
                    + " but found " + manifest.getSourceHash());
            }
            if (!manifest.isApplicable()) throw new IllegalStateException("DefaultQuests manifest is blocked");
            BqBehaviorInventory.from(manifest).requireRuntimeReady();
            BqBaseOnlyPreflight.ArtifactReport artifact = BqBaseOnlyPreflight.verifyArtifact(
                Paths.get(args[1]), args[3]);

            String jdbcUrl = required("BQ_PREFLIGHT_JDBC_URL");
            BqBaseOnlyPreflight.requireIsolatedLoopbackUrl(jdbcUrl,
                System.getenv("BQ_PREFLIGHT_ISOLATED_CONFIRM"));
            String username = required("BQ_PREFLIGHT_DB_USER");
            String password = required("BQ_PREFLIGHT_DB_PASSWORD");
            Class.forName("org.postgresql.Driver");
            BqBaseOnlyPreflight.DatabaseReport database = BqBaseOnlyPreflight.verifyInitializedDatabase(
                manifest, new DriverManagerQuestDataSource(jdbcUrl, username, password));

            System.out.println("result=PASS");
            System.out.println("sourceHash=" + manifest.getSourceHash());
            System.out.println("quests=" + database.getQuests());
            System.out.println("chapters=" + database.getChapters());
            System.out.println("appliedBatch=" + database.getBatchId());
            System.out.println("verifiedUnchangedEntities=" + database.getUnchangedEntities());
            System.out.println("baseJarSha256=" + artifact.getSha256());
            System.out.println("baseJarClasses=" + artifact.getClassCount());
            System.out.println("databaseMode=read-only");
        } catch (Exception failure) {
            System.err.println("result=FAIL");
            System.err.println("reason=" + (failure.getMessage() == null ? failure.getClass().getSimpleName()
                : failure.getMessage().replace('\n', ' ')));
            throw failure;
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}

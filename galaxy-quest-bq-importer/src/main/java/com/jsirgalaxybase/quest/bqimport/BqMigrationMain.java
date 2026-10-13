package com.jsirgalaxybase.quest.bqimport;

import java.nio.file.Paths;

import com.jsirgalaxybase.quest.postgres.DriverManagerQuestDataSource;
import com.jsirgalaxybase.quest.postgres.JdbcQuestChapterRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestConnectionManager;
import com.jsirgalaxybase.quest.postgres.JdbcQuestContentMigrationRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestDefinitionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestTransaction;
import com.jsirgalaxybase.quest.postgres.QuestContentMigrationBatch;

/** Explicit offline entrypoint. Mutations require an isolated-environment confirmation token. */
public final class BqMigrationMain {
    private BqMigrationMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException(
            "Usage: BqMigrationMain <plan|dry-run|preview|apply|rollback> <DefaultQuests|batch-id>");
        String command = args[0];
        if ("plan".equals(command)) {
            BqMigrationManifest manifest = new BqMigrationPlanner().plan(Paths.get(args[1]));
            System.out.print(manifest.toText());
            System.out.print(compatibility(manifest));
            return;
        }
        if ("rollback".equals(command)) {
            confirm();
            service().rollback(args[1]);
            System.out.println("rolledBack=" + args[1]);
            return;
        }
        if (!"dry-run".equals(command) && !"preview".equals(command) && !"apply".equals(command)) {
            throw new IllegalArgumentException("unknown command: " + command);
        }
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(Paths.get(args[1]));
        if ("apply".equals(command)) {
            // Reject unverified behavior before opening even a read-only database connection.
            BqBehaviorInventory.from(manifest).requireRuntimeReady();
        }
        if ("preview".equals(command) || "apply".equals(command)) confirm();
        BqMigrationDatabaseService service = service();
        BqMigrationDryRun preview = service.dryRun(manifest);
        System.out.print(manifest.toText());
        System.out.print(compatibility(manifest));
        System.out.print(preview.toText());
        if ("dry-run".equals(command)) return;
        if ("preview".equals(command)) {
            System.out.println("status=" + service.stage(manifest).getStatus());
            return;
        }
        QuestContentMigrationBatch batch = service.apply(manifest, System.currentTimeMillis());
        System.out.println("status=" + batch.getStatus());
    }

    private static String compatibility(BqMigrationManifest manifest) {
        return BqCompatibilityReport.create(manifest).toText();
    }

    private static BqMigrationDatabaseService service() {
        String url = required("BQ_MIGRATION_JDBC_URL");
        String user = required("BQ_MIGRATION_DB_USER");
        String password = required("BQ_MIGRATION_DB_PASSWORD");
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(
            new DriverManagerQuestDataSource(url, user, password));
        return new BqMigrationDatabaseService(new JdbcQuestTransaction(manager),
            new JdbcQuestDefinitionRepository(manager), new JdbcQuestChapterRepository(manager),
            new JdbcQuestContentMigrationRepository(manager));
    }

    private static void confirm() {
        if (!"ISOLATED_ONLY".equals(System.getenv("BQ_MIGRATION_WRITE_CONFIRM"))) {
            throw new IllegalStateException("preview/apply/rollback requires BQ_MIGRATION_WRITE_CONFIRM=ISOLATED_ONLY");
        }
        BqBaseOnlyPreflight.requireIsolatedLoopbackUrl(required("BQ_MIGRATION_JDBC_URL"), "ISOLATED_ONLY");
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}

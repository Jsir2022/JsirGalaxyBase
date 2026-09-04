package com.jsirgalaxybase.modules.itempolicy.infrastructure.jdbc;

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
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.DriverManagerDataSource;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyScope;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyAuditRecord;

/** Opt-in PostgreSQL coverage, using the same JGB_BANKING_IT_* contract as other integration tests. */
public class ItemPolicyPostgresIntegrationTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        Config config = Config.resolve();
        Assume.assumeTrue("PostgreSQL integration config not available", config.isUsable());
        context = Context.create(config, true);
    }

    @After
    public void tearDown() {
        if (context != null) context.close();
    }

    @Test
    public void factoryFailsFastWhenMigrationIsMissing() throws Exception {
        Context empty = Context.create(Config.resolve(), false);
        try {
            JdbcItemPolicyInfrastructureFactory.createShared(new JdbcConnectionManager(empty.dataSource));
            fail("expected missing policy schema to fail");
        } catch (BankingException expected) {
            assertTrue(expected.getMessage().contains("item_policy_audit"));
        } finally {
            empty.close();
        }
    }

    @Test
    public void deniedAuditIsPersistedWithActorScopeAndRule() throws Exception {
        JdbcItemPolicyAuditSink audit = JdbcItemPolicyInfrastructureFactory.createShared(
            new JdbcConnectionManager(context.dataSource));
        audit.recordDenied("lobby", "player-a", ItemPolicyScope.MARKET_CUSTODY, "terminal-deposit",
            "minecraft:command_block", 0, "dangerous-block");
        try (Connection connection = context.dataSource.getConnection(); Statement statement = connection.createStatement();
            ResultSet rows = statement.executeQuery("SELECT source_server_id, actor_player_ref, policy_scope, operation, item_registry_name, rule_id FROM item_policy_audit")) {
            assertTrue(rows.next());
            assertEquals("lobby", rows.getString(1));
            assertEquals("player-a", rows.getString(2));
            assertEquals("MARKET_CUSTODY", rows.getString(3));
            assertEquals("terminal-deposit", rows.getString(4));
            assertEquals("minecraft:command_block", rows.getString(5));
            assertEquals("dangerous-block", rows.getString(6));
        }
    }

    @Test
    public void terminalAuditQueryIsActorAndServerScopedAndNewestFirst() {
        JdbcItemPolicyAuditSink audit = JdbcItemPolicyInfrastructureFactory.createShared(
            new JdbcConnectionManager(context.dataSource));
        audit.recordDenied("lobby", "player-a", ItemPolicyScope.MARKET_CUSTODY, "first",
            "minecraft:command_block", 0, "rule-a");
        audit.recordDenied("s2", "player-a", ItemPolicyScope.BASE_VAULT, "other-server",
            "minecraft:bedrock", 0, "rule-b");
        audit.recordDenied("lobby", "player-b", ItemPolicyScope.BASE_VAULT, "other-player",
            "minecraft:bedrock", 0, "rule-b");
        audit.recordDenied("lobby", "player-a", ItemPolicyScope.BASE_VAULT, "last",
            "minecraft:bedrock", 0, "rule-c");

        JdbcItemPolicyAuditQuery query = new JdbcItemPolicyAuditQuery(new JdbcConnectionManager(context.dataSource));
        java.util.List<ItemPolicyAuditRecord> records = query.listRecentForActor("lobby", "player-a", 12);
        assertEquals(2, records.size());
        assertEquals("last", records.get(0).getOperation());
        assertEquals("first", records.get(1).getOperation());
        assertEquals("rule-c", records.get(0).getRuleId());
    }

    private static final class Context {
        private final DriverManagerDataSource root;
        private final DriverManagerDataSource dataSource;
        private final String schema;

        private Context(DriverManagerDataSource root, DriverManagerDataSource dataSource, String schema) {
            this.root = root;
            this.dataSource = dataSource;
            this.schema = schema;
        }

        private static Context create(Config config, boolean applyDdl) throws SQLException {
            DriverManagerDataSource root = new DriverManagerDataSource(config.url, config.username, config.password);
            String schema = "item_policy_it_" + UUID.randomUUID().toString().replace("-", "");
            try (Connection connection = root.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA \"" + schema + "\"");
            }
            String scopedUrl = config.url + (config.url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
            DriverManagerDataSource scoped = new DriverManagerDataSource(scopedUrl, config.username, config.password);
            if (applyDdl) {
                try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement()) {
                    statement.execute(loadMigration());
                }
            }
            return new Context(root, scoped, schema);
        }

        private void close() {
            try (Connection connection = root.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
            } catch (SQLException ignored) {}
        }

        private static String loadMigration() {
            try {
                return new String(Files.readAllBytes(Paths.get("ops", "sql", "migrations",
                    "20260830_001_add_item_policy_audit.sql")), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("failed to load item policy migration", exception);
            }
        }
    }

    private static final class Config {
        private final String url;
        private final String username;
        private final String password;

        private Config(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }

        private static Config resolve() {
            return new Config(System.getenv("JGB_BANKING_IT_JDBC_URL"), System.getenv("JGB_BANKING_IT_JDBC_USERNAME"),
                System.getenv("JGB_BANKING_IT_JDBC_PASSWORD"));
        }

        private boolean isUsable() {
            return notBlank(url) && notBlank(username) && notBlank(password);
        }

        private static boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
    }
}

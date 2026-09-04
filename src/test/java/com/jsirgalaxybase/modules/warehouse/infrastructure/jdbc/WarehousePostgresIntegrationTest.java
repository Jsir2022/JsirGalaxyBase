package com.jsirgalaxybase.modules.warehouse.infrastructure.jdbc;

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

import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.DriverManagerDataSource;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.warehouse.application.WarehouseDriveService;
import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseBayService;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveKey;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveResult;
import com.jsirgalaxybase.modules.warehouse.infrastructure.WarehouseInfrastructure;

public class WarehousePostgresIntegrationTest {
    private Context context;

    @Before public void setUp() throws Exception {
        Config config = Config.read();
        Assume.assumeTrue(config.available());
        context = Context.create(config, true);
    }
    @After public void tearDown() { if (context != null) context.close(); }

    @Test public void factoryFailsFastWithoutWarehouseSchema() throws Exception {
        Context empty = Context.create(Config.read(), false);
        try {
            JdbcWarehouseInfrastructureFactory.createShared(new JdbcConnectionManager(empty.scoped));
            fail("missing schema must fail");
        } catch (BankingException expected) {
            assertTrue(expected.getMessage().contains("missing table warehouse_drive"));
            assertTrue(expected.getMessage().contains("scripts/db-migrate.sh"));
        } finally { empty.close(); }
    }

    @Test public void concurrentRegistrationHasOneActiveOwnerAndOneAuditPerRequest() throws Exception {
        WarehouseDriveService first = service();
        WarehouseDriveService second = service();
        WarehouseDriveKey key = new WarehouseDriveKey("lobby", 0, 12, 64, -3);
        Pair<WarehouseDriveResult> results = concurrently(
            () -> first.register("warehouse-a", "a", false, key).getResult(),
            () -> second.register("warehouse-b", "b", false, key).getResult());
        int success = (results.first == WarehouseDriveResult.SUCCESS ? 1 : 0) + (results.second == WarehouseDriveResult.SUCCESS ? 1 : 0);
        int occupied = (results.first == WarehouseDriveResult.ALREADY_REGISTERED ? 1 : 0) + (results.second == WarehouseDriveResult.ALREADY_REGISTERED ? 1 : 0);
        assertEquals(1, success); assertEquals(1, occupied); assertEquals(1L, count("warehouse_drive")); assertEquals(2L, count("warehouse_drive_operation"));
    }

    @Test public void replayIsStableAndBreakSoftRemovesTheDrive() throws Exception {
        WarehouseDriveService service = service();
        WarehouseDriveKey key = new WarehouseDriveKey("lobby", 0, -12, 70, 4);
        assertEquals(WarehouseDriveResult.SUCCESS, service.register("replay", "owner", false, key).getResult());
        assertEquals(WarehouseDriveResult.SUCCESS, service.register("replay", "owner", false, key).getResult());
        assertEquals(WarehouseDriveResult.SUCCESS, service.authorizeBreak("break", "owner", false, key, false, true).getResult());
        assertEquals(1L, count("warehouse_drive"));
        assertEquals(2L, count("warehouse_drive_operation"));
        assertEquals(0, service.listOwned("owner").size());
        WarehouseDriveReceipt receipt = service.listRecent("owner", 4).get(0);
        assertEquals("owner", receipt.getBefore().getOwnerPlayerRef());
        assertEquals("owner", receipt.getAfter().getOwnerPlayerRef());
    }

    @Test public void terminalBayIsIsolatedFromDriveRowsAndRejectsNonCellMutation() throws Exception {
        TerminalWarehouseBayService service = new TerminalWarehouseBayService(new JdbcTerminalWarehouseBayRepository(
            new JdbcConnectionManager(context.scoped)), new JdbcWarehouseTransactionRunner(new JdbcConnectionManager(context.scoped)), "lobby",
            new com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseCellPolicy() {
                @Override public boolean isValidCell(net.minecraft.item.ItemStack stack) { return false; }
            });
        assertEquals("INVALID_CELL", service.commit("bay-invalid", "owner", 0L, null,
            new net.minecraft.item.ItemStack(new net.minecraft.item.Item()), "not a cell").getResult().name());
        assertEquals(0L, count("warehouse_terminal_bay"));
        assertEquals(1L, count("warehouse_terminal_bay_operation"));
    }

    private WarehouseDriveService service() {
        WarehouseInfrastructure infrastructure = JdbcWarehouseInfrastructureFactory.createShared(new JdbcConnectionManager(context.scoped));
        return new WarehouseDriveService(infrastructure.getRepository(), infrastructure.getTransactionRunner(), "lobby");
    }
    private long count(String table) throws SQLException {
        try (Connection connection = context.scoped.getConnection(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) { rows.next(); return rows.getLong(1); }
    }
    private static <T> Pair<T> concurrently(Callable<T> a, Callable<T> b) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2); CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> first = executor.submit(waiting(ready, start, a)); Future<T> second = executor.submit(waiting(ready, start, b));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown(); return new Pair<T>(first.get(), second.get());
        } finally { executor.shutdownNow(); }
    }
    private static <T> Callable<T> waiting(CountDownLatch ready, CountDownLatch start, Callable<T> value) {
        return () -> { ready.countDown(); if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("timeout"); return value.call(); };
    }
    private static final class Pair<T> { final T first; final T second; Pair(T first, T second) { this.first = first; this.second = second; } }

    private static final class Context {
        private static final String DDL = loadDdl();
        final DriverManagerDataSource base; final DriverManagerDataSource scoped; final String schema;
        private Context(DriverManagerDataSource base, DriverManagerDataSource scoped, String schema) { this.base = base; this.scoped = scoped; this.schema = schema; }
        static Context create(Config config, boolean ddl) throws SQLException {
            DriverManagerDataSource base = new DriverManagerDataSource(config.url, config.user, config.password);
            String schema = "warehouse_it_" + UUID.randomUUID().toString().replace("-", "");
            try (Connection connection = base.getConnection(); Statement statement = connection.createStatement()) { statement.execute("CREATE SCHEMA \"" + schema + "\""); }
            String scopedUrl = config.url + (config.url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
            DriverManagerDataSource scoped = new DriverManagerDataSource(scopedUrl, config.user, config.password);
            if (ddl) try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement()) { statement.execute(DDL); }
            return new Context(base, scoped, schema);
        }
        void close() { try (Connection connection = base.getConnection(); Statement statement = connection.createStatement()) { statement.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE"); } catch (SQLException ignored) {} }
        private static String loadDdl() { try { return new String(Files.readAllBytes(Paths.get("ops", "sql", "migrations", "20260831_003_add_ae2_warehouse_drive.sql")), StandardCharsets.UTF_8)
            + "\n" + new String(Files.readAllBytes(Paths.get("ops", "sql", "migrations", "20260901_001_add_terminal_warehouse_bay.sql")), StandardCharsets.UTF_8); } catch (IOException error) { throw new IllegalStateException(error); } }
    }
    private static final class Config {
        final String url; final String user; final String password;
        Config(String url, String user, String password) { this.url = url; this.user = user; this.password = password; }
        static Config read() { return new Config(System.getenv("JGB_BANKING_IT_JDBC_URL"), System.getenv("JGB_BANKING_IT_JDBC_USERNAME"), System.getenv("JGB_BANKING_IT_JDBC_PASSWORD")); }
        boolean available() { if (blank(url) || blank(user) || blank(password)) return false; try { Class.forName("org.postgresql.Driver"); try (Connection ignored = new DriverManagerDataSource(url, user, password).getConnection()) { return true; } } catch (Exception ignored) { return false; } }
        private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    }
}

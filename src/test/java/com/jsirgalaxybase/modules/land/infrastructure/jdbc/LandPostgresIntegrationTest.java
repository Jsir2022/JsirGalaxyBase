package com.jsirgalaxybase.modules.land.infrastructure.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.jsirgalaxybase.modules.land.application.LandProtectionIndex;
import com.jsirgalaxybase.modules.land.application.PersonalLandRules;
import com.jsirgalaxybase.modules.land.application.PersonalLandService;
import com.jsirgalaxybase.modules.land.domain.LandActionResult;
import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.PersonalLandActionReceipt;
import com.jsirgalaxybase.modules.land.infrastructure.LandInfrastructure;
import com.jsirgalaxybase.modules.land.port.PersonalLandRepository;

public class LandPostgresIntegrationTest {

    private PostgresTestConfig config;
    private PostgresTestContext context;

    @Before
    public void setUp() throws Exception {
        config = PostgresTestConfig.resolve();
        Assume.assumeTrue("PostgreSQL integration config not available", config.isConfigured());
        Assume.assumeTrue("PostgreSQL integration config is not reachable", config.canConnect());
        context = PostgresTestContext.create(config, true);
    }

    @After
    public void tearDown() {
        if (context != null) context.close();
    }

    @Test
    public void factoryFailsFastWhenSchemaIsMissing() throws Exception {
        PostgresTestContext empty = PostgresTestContext.create(config, false);
        try {
            JdbcLandInfrastructureFactory.createShared(new JdbcConnectionManager(empty.schemaDataSource));
            fail("Expected missing land schema to fail");
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("missing table land_title"));
            assertTrue(exception.getMessage().contains("scripts/db-migrate.sh"));
        } finally {
            empty.close();
        }
    }

    @Test
    public void titlesAndProtectionSnapshotSurviveServiceRestartAndRevocation() throws Exception {
        LandInfrastructure infrastructure = infrastructure();
        LandChunkKey key = new LandChunkKey("lobby", 0, -2, 7);
        PersonalLandService first = service(infrastructure, 4);
        PersonalLandActionReceipt claim = first.claim("pg-claim", "player-a", key);

        PersonalLandService restarted = service(infrastructure, 4);
        assertEquals(1, restarted.getProtectionIndex().size());
        assertFalse(restarted.getProtectionIndex().mayModify("player-b", key));
        assertEquals(LandActionResult.SUCCESS,
            restarted.unclaim("pg-unclaim", "player-a", key, claim.getAfterTitle().getVersion()).getResult());

        PersonalLandService afterRevoke = service(infrastructure, 4);
        assertEquals(0, afterRevoke.getProtectionIndex().size());
        assertEquals(LandActionResult.SUCCESS, afterRevoke.claim("pg-reclaim", "player-b", key).getResult());
        assertEquals(2L, countRows("land_title"));
        assertEquals(3L, countRows("land_operation_log"));
    }

    @Test
    public void concurrentChunkClaimAndQuotaChecksCannotBeBypassed() throws Exception {
        LandInfrastructure infrastructure = infrastructure();
        PersonalLandService first = service(infrastructure, 1);
        PersonalLandService second = service(infrastructure, 1);
        LandChunkKey shared = new LandChunkKey("lobby", 0, 3, 3);

        Pair<PersonalLandActionReceipt> sharedResults = concurrently(
            () -> first.claim("shared-a", "player-a", shared),
            () -> second.claim("shared-b", "player-b", shared));
        assertOneEach(sharedResults, LandActionResult.SUCCESS, LandActionResult.ALREADY_CLAIMED);

        Pair<PersonalLandActionReceipt> quotaResults = concurrently(
            () -> first.claim("quota-a", "player-c", new LandChunkKey("lobby", 0, 5, 5)),
            () -> second.claim("quota-b", "player-c", new LandChunkKey("lobby", 0, 6, 6)));
        assertOneEach(quotaResults, LandActionResult.SUCCESS, LandActionResult.LIMIT_REACHED);
    }

    @Test
    public void concurrentDuplicateRequestReplaysOnePersistedReceipt() throws Exception {
        LandInfrastructure infrastructure = infrastructure();
        PersonalLandService first = service(infrastructure, 4);
        PersonalLandService second = service(infrastructure, 4);
        LandChunkKey key = new LandChunkKey("lobby", 0, 9, 9);

        Pair<PersonalLandActionReceipt> results = concurrently(
            () -> first.claim("duplicate-request", "player-a", key),
            () -> second.claim("duplicate-request", "player-a", key));

        assertEquals(LandActionResult.SUCCESS, results.first.getResult());
        assertEquals(LandActionResult.SUCCESS, results.second.getResult());
        assertEquals(results.first.getAfterTitle().getTitleId(), results.second.getAfterTitle().getTitleId());
        assertEquals(1L, countRows("land_title"));
        assertEquals(1L, countRows("land_operation_log"));
    }

    @Test
    public void receiptFailureRollsBackTitleAndOperationTogether() throws Exception {
        LandInfrastructure infrastructure = infrastructure();
        PersonalLandRepository failing = new FailingReceiptRepository(infrastructure.getRepository());
        PersonalLandService service = new PersonalLandService(failing, PersonalLandRules.allowAll(4),
            new LandProtectionIndex(), infrastructure.getTransactionRunner(), "lobby");

        try {
            service.claim("forced-rollback", "player-a", new LandChunkKey("lobby", 0, 12, 12));
            fail("Expected forced receipt failure");
        } catch (IllegalStateException expected) {
            assertEquals("forced receipt failure", expected.getMessage());
        }
        assertEquals(0L, countRows("land_title"));
        assertEquals(0L, countRows("land_operation_log"));
    }

    private LandInfrastructure infrastructure() {
        return JdbcLandInfrastructureFactory.createShared(new JdbcConnectionManager(context.schemaDataSource));
    }

    private static PersonalLandService service(LandInfrastructure infrastructure, int maxClaims) {
        return new PersonalLandService(infrastructure.getRepository(), PersonalLandRules.allowAll(maxClaims),
            new LandProtectionIndex(), infrastructure.getTransactionRunner(), "lobby");
    }

    private long countRows(String table) throws SQLException {
        try (Connection connection = context.schemaDataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static void assertOneEach(Pair<PersonalLandActionReceipt> results, LandActionResult first,
        LandActionResult second) {
        int firstCount = (results.first.getResult() == first ? 1 : 0) + (results.second.getResult() == first ? 1 : 0);
        int secondCount = (results.first.getResult() == second ? 1 : 0)
            + (results.second.getResult() == second ? 1 : 0);
        assertEquals(1, firstCount);
        assertEquals(1, secondCount);
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

    private static final class FailingReceiptRepository implements PersonalLandRepository {

        private final PersonalLandRepository delegate;

        private FailingReceiptRepository(PersonalLandRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public long nextTitleId() {
            return delegate.nextTitleId();
        }

        @Override
        public java.util.Optional<com.jsirgalaxybase.modules.land.domain.PersonalLandTitle> findByChunkKey(
            LandChunkKey chunkKey) {
            return delegate.findByChunkKey(chunkKey);
        }

        @Override
        public java.util.Optional<com.jsirgalaxybase.modules.land.domain.PersonalLandTitle> lockByChunkKey(
            LandChunkKey chunkKey) {
            return delegate.lockByChunkKey(chunkKey);
        }

        @Override
        public java.util.List<com.jsirgalaxybase.modules.land.domain.PersonalLandTitle> findByOwnerPlayerRef(
            String ownerPlayerRef) {
            return delegate.findByOwnerPlayerRef(ownerPlayerRef);
        }

        @Override
        public java.util.Collection<com.jsirgalaxybase.modules.land.domain.PersonalLandTitle> findAll() {
            return delegate.findAll();
        }

        @Override
        public java.util.Collection<com.jsirgalaxybase.modules.land.domain.PersonalLandTitle> findAllByServerId(
            String serverId) {
            return delegate.findAllByServerId(serverId);
        }

        @Override
        public void save(com.jsirgalaxybase.modules.land.domain.PersonalLandTitle title) {
            delegate.save(title);
        }

        @Override
        public void remove(LandChunkKey chunkKey) {
            delegate.remove(chunkKey);
        }

        @Override
        public void lockRequest(String requestId) {
            delegate.lockRequest(requestId);
        }

        @Override
        public void lockOwner(String ownerPlayerRef) {
            delegate.lockOwner(ownerPlayerRef);
        }

        @Override
        public java.util.Optional<PersonalLandActionReceipt> findReceiptByRequestId(String requestId) {
            return delegate.findReceiptByRequestId(requestId);
        }

        @Override
        public void saveReceipt(PersonalLandActionReceipt receipt) {
            throw new IllegalStateException("forced receipt failure");
        }
    }

    private static final class PostgresTestContext {

        private static final String LAND_DDL = loadLandDdl();

        private final DriverManagerDataSource baseDataSource;
        private final DriverManagerDataSource schemaDataSource;
        private final String schemaName;

        private PostgresTestContext(DriverManagerDataSource baseDataSource, DriverManagerDataSource schemaDataSource,
            String schemaName) {
            this.baseDataSource = baseDataSource;
            this.schemaDataSource = schemaDataSource;
            this.schemaName = schemaName;
        }

        private static PostgresTestContext create(PostgresTestConfig config, boolean applyDdl) throws SQLException {
            DriverManagerDataSource base = new DriverManagerDataSource(config.jdbcUrl, config.username, config.password);
            String schema = "land_it_" + UUID.randomUUID().toString().replace("-", "");
            try (Connection connection = base.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA \"" + schema + "\"");
            }
            String schemaUrl = config.jdbcUrl + (config.jdbcUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
            DriverManagerDataSource scoped = new DriverManagerDataSource(schemaUrl, config.username, config.password);
            if (applyDdl) {
                try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement()) {
                    statement.execute("SET search_path TO \"" + schema + "\", public");
                    statement.execute(LAND_DDL);
                }
            }
            return new PostgresTestContext(base, scoped, schema);
        }

        private void close() {
            try (Connection connection = baseDataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            } catch (SQLException ignored) {}
        }

        private static String loadLandDdl() {
            Path path = Paths.get("ops", "sql", "migrations", "20260822_001_add_personal_land.sql");
            try {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to load personal land migration", exception);
            }
        }
    }

    private static final class PostgresTestConfig {

        private final String jdbcUrl;
        private final String username;
        private final String password;

        private PostgresTestConfig(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }

        private static PostgresTestConfig resolve() {
            return new PostgresTestConfig(System.getenv("JGB_BANKING_IT_JDBC_URL"),
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

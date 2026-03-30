package com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountStatus;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;
import com.jsirgalaxybase.modules.core.banking.repository.BankAccountRepository;

public class JdbcBankAccountRepository extends AbstractJdbcRepository implements BankAccountRepository {

    public JdbcBankAccountRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public Optional<BankAccount> findById(final long accountId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<BankAccount>>() {

            @Override
            public Optional<BankAccount> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM bank_account WHERE account_id = ?");
                try {
                    statement.setLong(1, accountId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapAccount(resultSet)) : Optional.<BankAccount>empty();
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public Optional<BankAccount> findByOwner(final String ownerType, final String ownerRef, final String currencyCode) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<BankAccount>>() {

            @Override
            public Optional<BankAccount> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM bank_account WHERE owner_type = ? AND owner_ref = ? AND currency_code = ?");
                try {
                    statement.setString(1, ownerType);
                    statement.setString(2, ownerRef);
                    statement.setString(3, currencyCode);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapAccount(resultSet)) : Optional.<BankAccount>empty();
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public BankAccount save(final BankAccount account) {
        return connectionManager.withConnection(new JdbcConnectionCallback<BankAccount>() {

            @Override
            public BankAccount doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO bank_account (account_no, account_type, owner_type, owner_ref, currency_code, available_balance, frozen_balance, status, version, display_name, metadata_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    statement.setString(1, account.getAccountNo());
                    statement.setString(2, account.getAccountType().name());
                    statement.setString(3, account.getOwnerType());
                    statement.setString(4, account.getOwnerRef());
                    statement.setString(5, account.getCurrencyCode());
                    statement.setLong(6, account.getAvailableBalance());
                    statement.setLong(7, account.getFrozenBalance());
                    statement.setString(8, account.getStatus().name());
                    statement.setLong(9, account.getVersion());
                    statement.setString(10, account.getDisplayName());
                    statement.setString(11, account.getMetadataJson());
                    statement.setTimestamp(12, java.sql.Timestamp.from(account.getCreatedAt()));
                    statement.setTimestamp(13, java.sql.Timestamp.from(account.getUpdatedAt()));
                    statement.executeUpdate();

                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new BankingException("failed to read generated bank_account key");
                        }
                        long accountId = generatedKeys.getLong(1);
                        return new BankAccount(accountId, account.getAccountNo(), account.getAccountType(),
                            account.getOwnerType(), account.getOwnerRef(), account.getCurrencyCode(),
                            account.getAvailableBalance(), account.getFrozenBalance(), account.getStatus(),
                            account.getVersion(), account.getDisplayName(), account.getMetadataJson(),
                            account.getCreatedAt(), account.getUpdatedAt());
                    } finally {
                        generatedKeys.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public BankAccount lockById(final long accountId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<BankAccount>() {

            @Override
            public BankAccount doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection
                    .prepareStatement("SELECT * FROM bank_account WHERE account_id = ? FOR UPDATE");
                try {
                    statement.setLong(1, accountId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        if (!resultSet.next()) {
                            throw new BankingException("account not found: " + accountId);
                        }
                        return mapAccount(resultSet);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public List<BankAccount> lockByIdsInOrder(final List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyList();
        }

        return connectionManager.withConnection(new JdbcConnectionCallback<List<BankAccount>>() {

            @Override
            public List<BankAccount> doInConnection(java.sql.Connection connection) throws SQLException {
                List<Long> orderedIds = new ArrayList<Long>(accountIds);
                Collections.sort(orderedIds);

                StringBuilder placeholders = new StringBuilder();
                for (int i = 0; i < orderedIds.size(); i++) {
                    if (i > 0) {
                        placeholders.append(", ");
                    }
                    placeholders.append('?');
                }

                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM bank_account WHERE account_id IN (" + placeholders.toString() + ") ORDER BY account_id ASC FOR UPDATE");
                try {
                    for (int i = 0; i < orderedIds.size(); i++) {
                        statement.setLong(i + 1, orderedIds.get(i).longValue());
                    }
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        List<BankAccount> results = new ArrayList<BankAccount>();
                        while (resultSet.next()) {
                            results.add(mapAccount(resultSet));
                        }
                        return results;
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public void updateBalances(final long accountId, final long availableBalance, final long frozenBalance,
        final long expectedVersion) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {

            @Override
            public Void doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "UPDATE bank_account SET available_balance = ?, frozen_balance = ?, version = version + 1 WHERE account_id = ? AND version = ?");
                try {
                    statement.setLong(1, availableBalance);
                    statement.setLong(2, frozenBalance);
                    statement.setLong(3, accountId);
                    statement.setLong(4, expectedVersion);
                    int updatedRows = statement.executeUpdate();
                    if (updatedRows != 1) {
                        throw new BankingException("bank_account optimistic update failed for accountId=" + accountId);
                    }
                    return null;
                } finally {
                    statement.close();
                }
            }
        });
    }

    private BankAccount mapAccount(ResultSet resultSet) throws SQLException {
        return new BankAccount(resultSet.getLong("account_id"), resultSet.getString("account_no"),
            BankAccountType.valueOf(resultSet.getString("account_type")), resultSet.getString("owner_type"),
            resultSet.getString("owner_ref"), resultSet.getString("currency_code"),
            resultSet.getLong("available_balance"), resultSet.getLong("frozen_balance"),
            BankAccountStatus.valueOf(resultSet.getString("status")), resultSet.getLong("version"),
            resultSet.getString("display_name"), resultSet.getString("metadata_json"),
            readInstant(resultSet, "created_at"), readInstant(resultSet, "updated_at"));
    }
}
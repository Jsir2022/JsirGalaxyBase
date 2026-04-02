package com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntry;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntrySide;
import com.jsirgalaxybase.modules.core.banking.repository.LedgerEntryRepository;

public class JdbcLedgerEntryRepository extends AbstractJdbcRepository implements LedgerEntryRepository {

    public JdbcLedgerEntryRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public void appendEntries(final List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {

            @Override
            public Void doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO ledger_entry (transaction_id, account_id, entry_side, amount, balance_before, balance_after, frozen_balance_before, frozen_balance_after, currency_code, sequence_in_tx, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                try {
                    for (LedgerEntry entry : entries) {
                        statement.setLong(1, entry.getTransactionId());
                        statement.setLong(2, entry.getAccountId());
                        statement.setString(3, entry.getEntrySide().name());
                        statement.setLong(4, entry.getAmount());
                        statement.setLong(5, entry.getBalanceBefore());
                        statement.setLong(6, entry.getBalanceAfter());
                        statement.setLong(7, entry.getFrozenBalanceBefore());
                        statement.setLong(8, entry.getFrozenBalanceAfter());
                        statement.setString(9, entry.getCurrencyCode());
                        statement.setShort(10, entry.getSequenceInTransaction());
                        statement.setTimestamp(11, java.sql.Timestamp.from(entry.getCreatedAt()));
                        statement.addBatch();
                    }
                    statement.executeBatch();
                    return null;
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public List<LedgerEntry> findByTransactionId(final long transactionId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<LedgerEntry>>() {

            @Override
            public List<LedgerEntry> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM ledger_entry WHERE transaction_id = ? ORDER BY sequence_in_tx ASC");
                try {
                    statement.setLong(1, transactionId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return mapEntries(resultSet);
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
    public List<LedgerEntry> findRecentByAccountId(final long accountId, final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<LedgerEntry>>() {

            @Override
            public List<LedgerEntry> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM ledger_entry WHERE account_id = ? ORDER BY entry_id DESC LIMIT ?");
                try {
                    statement.setLong(1, accountId);
                    statement.setInt(2, limit);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return mapEntries(resultSet);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private List<LedgerEntry> mapEntries(ResultSet resultSet) throws SQLException {
        List<LedgerEntry> entries = new ArrayList<LedgerEntry>();
        while (resultSet.next()) {
            entries.add(new LedgerEntry(resultSet.getLong("entry_id"), resultSet.getLong("transaction_id"),
                resultSet.getLong("account_id"), LedgerEntrySide.valueOf(resultSet.getString("entry_side")),
                resultSet.getLong("amount"), resultSet.getLong("balance_before"), resultSet.getLong("balance_after"),
                resultSet.getLong("frozen_balance_before"), resultSet.getLong("frozen_balance_after"),
                resultSet.getString("currency_code"), resultSet.getShort("sequence_in_tx"),
                readInstant(resultSet, "created_at")));
        }
        return entries;
    }
}
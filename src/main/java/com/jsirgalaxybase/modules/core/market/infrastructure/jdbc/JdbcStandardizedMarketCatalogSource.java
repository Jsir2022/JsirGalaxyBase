package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogBrowser;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogEntry;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogPage;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogSource;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketProductParser;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

import cpw.mods.fml.common.registry.GameRegistry;

/** The formal admission boundary for the standardized market. */
public final class JdbcStandardizedMarketCatalogSource
    implements StandardizedMarketCatalogSource, StandardizedMarketCatalogBrowser {

    public static final String SOURCE_KEY = "postgres-standardized-market-catalog";
    private static final String SOURCE_DESCRIPTION = "管理员维护的 PostgreSQL 标准商品目录";

    private final JdbcConnectionManager connectionManager;
    private final StandardizedMarketProductParser productParser;

    public JdbcStandardizedMarketCatalogSource(JdbcConnectionManager connectionManager) {
        this(connectionManager, new StandardizedMarketProductParser());
    }

    public JdbcStandardizedMarketCatalogSource(JdbcConnectionManager connectionManager,
        StandardizedMarketProductParser productParser) {
        this.connectionManager = connectionManager;
        this.productParser = productParser == null ? new StandardizedMarketProductParser() : productParser;
    }

    @Override
    public String getSourceKey() { return SOURCE_KEY; }

    @Override
    public String getSourceDescription() { return SOURCE_DESCRIPTION; }

    @Override
    public Optional<StandardizedMarketCatalogEntry> findEntryByProductKey(final String productKey) {
        if (productKey == null || productKey.trim().isEmpty()) {
            return Optional.empty();
        }
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<StandardizedMarketCatalogEntry>>() {
            @Override
            public Optional<StandardizedMarketCatalogEntry> doInConnection(Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM standardized_market_catalog WHERE product_key = ? AND enabled = TRUE");
                try {
                    statement.setString(1, productKey.trim());
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapEntry(resultSet)) : Optional.<StandardizedMarketCatalogEntry>empty();
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
    public Optional<StandardizedMarketCatalogEntry> findEntryByStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return Optional.empty();
        }
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        if (identifier == null) {
            return Optional.empty();
        }
        return findEntryByProductKey(identifier.modId + ":" + identifier.name + ":" + stack.getItemDamage());
    }

    @Override
    public StandardizedMarketCatalogPage browse(final String query, final int requestedPageIndex,
        final int requestedPageSize) {
        final String normalizedQuery = query == null ? "" : query.trim();
        final int pageSize = Math.max(1, Math.min(64, requestedPageSize));
        final int requestedPage = Math.max(0, requestedPageIndex);
        return connectionManager.withConnection(new JdbcConnectionCallback<StandardizedMarketCatalogPage>() {
            @Override
            public StandardizedMarketCatalogPage doInConnection(Connection connection) throws SQLException {
                int totalEntries = countEntries(connection, normalizedQuery);
                int maxPage = totalEntries == 0 ? 0 : (totalEntries - 1) / pageSize;
                int pageIndex = Math.min(requestedPage, maxPage);
                List<StandardizedMarketCatalogEntry> entries = loadEntries(connection, normalizedQuery, pageIndex, pageSize);
                return new StandardizedMarketCatalogPage(normalizedQuery, pageIndex, pageSize, totalEntries, entries);
            }
        });
    }

    private int countEntries(Connection connection, String query) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "SELECT count(*) FROM standardized_market_catalog WHERE enabled = TRUE "
                + "AND (? = '' OR lower(display_name) LIKE ? OR lower(product_key) LIKE ? OR lower(registry_name) LIKE ?)");
        try {
            bindSearch(statement, query);
            ResultSet resultSet = statement.executeQuery();
            try {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    private List<StandardizedMarketCatalogEntry> loadEntries(Connection connection, String query, int pageIndex,
        int pageSize) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "SELECT * FROM standardized_market_catalog WHERE enabled = TRUE "
                + "AND (? = '' OR lower(display_name) LIKE ? OR lower(product_key) LIKE ? OR lower(registry_name) LIKE ?) "
                + "ORDER BY sort_order ASC, display_name ASC, product_key ASC LIMIT ? OFFSET ?");
        try {
            bindSearch(statement, query);
            statement.setInt(5, pageSize);
            statement.setInt(6, pageIndex * pageSize);
            ResultSet resultSet = statement.executeQuery();
            try {
                List<StandardizedMarketCatalogEntry> entries = new ArrayList<StandardizedMarketCatalogEntry>();
                while (resultSet.next()) {
                    entries.add(mapEntry(resultSet));
                }
                return entries;
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    private void bindSearch(PreparedStatement statement, String query) throws SQLException {
        String pattern = "%" + query.toLowerCase(java.util.Locale.ROOT) + "%";
        statement.setString(1, query);
        statement.setString(2, pattern);
        statement.setString(3, pattern);
        statement.setString(4, pattern);
    }

    private StandardizedMarketCatalogEntry mapEntry(ResultSet resultSet) throws SQLException {
        StandardizedMarketProduct product = productParser.parse(resultSet.getString("product_key"));
        return new StandardizedMarketCatalogEntry(product,
            resultSet.getString("category_code"), resultSet.getString("admission_basis"),
            resultSet.getString("source_entry_label"), resultSet.getString("display_name"),
            resultSet.getString("unit_label"), resultSet.getInt("sort_order"), resultSet.getString("catalog_version"),
            resultSet.getBoolean("enabled"), resultSet.getLong("reference_price"));
    }
}

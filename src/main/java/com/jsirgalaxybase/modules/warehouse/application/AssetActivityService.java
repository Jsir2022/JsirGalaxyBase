package com.jsirgalaxybase.modules.warehouse.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

import com.jsirgalaxybase.modules.core.market.domain.CustomMarketItemSnapshot;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketDeliveryStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.domain.MarketAccountCenterQuery;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.infrastructure.MarketInfrastructure;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.application.VaultItemStackCodec;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperation;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationStatus;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityRow;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityType;

/** Read-only projection over the existing market and Vault ledgers. */
public final class AssetActivityService {

    private final MarketInfrastructure market;
    private final BaseVaultService vault;

    public AssetActivityService(MarketInfrastructure market, BaseVaultService vault) {
        this.market = market; this.vault = vault;
    }

    public AssetActivitySnapshot findRecent(String playerRef, int limit) {
        int bounded = Math.max(1, Math.min(AssetActivitySnapshot.MAX_ROWS, limit));
        List<AssetActivityRow> rows = new ArrayList<AssetActivityRow>();
        if (market != null) {
            addStandardTrades(rows, playerRef);
            addCustomTrades(rows, playerRef);
            addMarketExceptions(rows, playerRef);
        }
        if (vault != null) addVaultBusinessEvents(rows, playerRef);
        Collections.sort(rows, new Comparator<AssetActivityRow>() {
            @Override public int compare(AssetActivityRow left, AssetActivityRow right) {
                return right.getCreatedAt().compareTo(left.getCreatedAt());
            }
        });
        Map<String, AssetActivityRow> unique = new LinkedHashMap<String, AssetActivityRow>();
        for (AssetActivityRow row : rows) if (!unique.containsKey(row.getStableKey())) unique.put(row.getStableKey(), row);
        List<AssetActivityRow> result = new ArrayList<AssetActivityRow>(unique.values());
        return new AssetActivitySnapshot(result.subList(0, Math.min(bounded, result.size())));
    }

    private void addStandardTrades(List<AssetActivityRow> target, String playerRef) {
        if (market.getTradeRecordRepository() == null) return;
        MarketAccountCenterQuery query = new MarketAccountCenterQuery(MarketAccountCenterQuery.Tab.FILLS,
            "", "", null, MarketAccountCenterQuery.StatusGroup.ALL, null, 0, 20, "");
        for (MarketTradeRecord trade : market.getTradeRecordRepository().findPersonalTradeHistory(playerRef, query).getTrades()) {
            boolean buy = playerRef.equals(trade.getBuyerPlayerRef());
            if (buy) continue; // A buy becomes an asset event only when its custody is actually delivered to Vault.
            long amount = saturatedMultiply(trade.getUnitPrice(), trade.getQuantity());
            target.add(new AssetActivityRow("standard-trade:" + trade.getTradeId() + ":sell",
                AssetActivityType.STANDARD_SELL_SETTLED,
                productStack(trade.getProduct()), trade.getQuantity(), amount, trade.getFeeAmount(),
                "T" + trade.getTradeId(), "COMPLETED", trade.getCreatedAt()));
        }
    }

    private void addCustomTrades(List<AssetActivityRow> target, String playerRef) {
        if (market.getCustomMarketTradeRecordRepository() == null) return;
        for (CustomMarketTradeRecord trade : market.getCustomMarketTradeRecordRepository().findRecentByPlayer(playerRef, 20)) {
            boolean buy = playerRef.equals(trade.getBuyerPlayerRef());
            if (buy && trade.getDeliveryStatus() == CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM) continue;
            ItemStack stack = null;
            if (market.getCustomMarketItemSnapshotRepository() != null) {
                CustomMarketItemSnapshot snapshot = market.getCustomMarketItemSnapshotRepository()
                    .findByListingId(trade.getListingId()).orElse(null);
                try { stack = snapshot == null ? null : snapshot.toItemStack(); } catch (RuntimeException ignored) { }
            }
            AssetActivityType type = buy && trade.getDeliveryStatus() == CustomMarketDeliveryStatus.EXCEPTION
                ? AssetActivityType.DELIVERY_FAILED
                : buy ? AssetActivityType.CUSTOM_BUY_DELIVERED : AssetActivityType.CUSTOM_SELL_SETTLED;
            target.add(new AssetActivityRow("custom-trade:" + trade.getTradeId() + ":" + (buy ? "buy" : "sell"),
                type,
                stack, stack == null ? 1L : stack.stackSize, trade.getSettledAmount(), 0L,
                "C" + trade.getListingId(), trade.getDeliveryStatus().name(), trade.getCreatedAt()));
        }
    }

    private void addMarketExceptions(List<AssetActivityRow> target, String playerRef) {
        if (market.getOperationLogRepository() == null) return;
        MarketAccountCenterQuery query = new MarketAccountCenterQuery(MarketAccountCenterQuery.Tab.ASSETS_AND_DELIVERY,
            "", "", null, MarketAccountCenterQuery.StatusGroup.EXCEPTION, null, 0, 20, "");
        for (MarketOperationLog operation : market.getOperationLogRepository().findPersonalExceptions(playerRef, query).getOperations()) {
            AssetActivityType type = operation.getStatus() == MarketOperationStatus.RECOVERY_REQUIRED
                ? AssetActivityType.RECOVERY_REQUIRED : AssetActivityType.DELIVERY_FAILED;
            target.add(new AssetActivityRow("market-operation:" + operation.getOperationId(), type, null, 0L, 0L, 0L,
                "O" + operation.getOperationId(), operation.getStatus().name(), operation.getUpdatedAt()));
        }
    }

    private void addVaultBusinessEvents(List<AssetActivityRow> target, String playerRef) {
        for (VaultOperation operation : vault.findPersonalRecentOperations(playerRef, 20)) {
            if (!isBusinessVaultOperation(operation)) continue;
            // A completed custom-market delivery is already represented by the trade ledger. Keep the
            // Vault row only when it carries an exceptional/recovery state, otherwise the same business
            // event would be shown twice with unrelated low-level operation ids.
            if ("CUSTOM_MARKET".equals(operation.getSourceDomain())
                && operation.getStatus() == VaultOperationStatus.COMPLETED) continue;
            ItemStack stack = null;
            try { stack = VaultItemStackCodec.decode(operation.getItemSnapshot()); } catch (RuntimeException ignored) { }
            AssetActivityType type;
            if (operation.getStatus() == VaultOperationStatus.RECOVERY_REQUIRED) type = AssetActivityType.RECOVERY_REQUIRED;
            else if (operation.getStatus() == VaultOperationStatus.FAILED) type = AssetActivityType.DELIVERY_FAILED;
            else if ("MARKET_CLAIM".equals(operation.getSourceDomain())) type = AssetActivityType.STANDARD_BUY_DEPOSITED;
            else if ("CUSTOM_MARKET".equals(operation.getSourceDomain())) type = AssetActivityType.CUSTOM_BUY_DELIVERED;
            else if (isReturn(operation)) type = AssetActivityType.ASSET_RETURNED;
            else type = AssetActivityType.RECOVERY_COMPLETED;
            target.add(new AssetActivityRow("vault-operation:" + operation.getOperationId(), type, stack,
                operation.getQuantity(), 0L, 0L, "V" + operation.getOperationId(), operation.getStatus().name(),
                operation.getUpdatedAt()));
        }
    }

    private boolean isBusinessVaultOperation(VaultOperation operation) {
        if (operation == null || "VAULT_CONTAINER_MUTATION".equals(operation.getOperationType())
            || "VAULT_SORT".equals(operation.getOperationType())) return false;
        String source = operation.getSourceDomain(); String target = operation.getTargetDomain();
        boolean businessDomain = containsBusinessDomain(source) || containsBusinessDomain(target);
        if (!businessDomain) return false;
        return "VAULT_DELIVERY".equals(operation.getOperationType())
            || operation.getStatus() == VaultOperationStatus.RECOVERY_REQUIRED
            || operation.getStatus() == VaultOperationStatus.FAILED;
    }

    private boolean containsBusinessDomain(String value) {
        String normalized = value == null ? "" : value.toUpperCase(java.util.Locale.ROOT);
        return normalized.contains("MARKET") || normalized.contains("CUSTODY") || normalized.contains("RECOVERY")
            || normalized.contains("CANCEL") || normalized.contains("REFUND");
    }

    private boolean isReturn(VaultOperation operation) {
        String text = (operation.getSourceDomain() + " " + operation.getTargetDomain() + " "
            + operation.getOperationType()).toUpperCase(java.util.Locale.ROOT);
        return text.contains("CANCEL") || text.contains("RETURN") || text.contains("REFUND");
    }

    private ItemStack productStack(StandardizedMarketProduct product) {
        if (product == null) return null;
        String id = product.getRegistryName(); int separator = id == null ? -1 : id.indexOf(':');
        Item item = separator > 0 && separator < id.length() - 1
            ? GameRegistry.findItem(id.substring(0, separator), id.substring(separator + 1)) : null;
        return item == null ? null : new ItemStack(item, 1, product.getMeta());
    }

    private long saturatedMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }
}

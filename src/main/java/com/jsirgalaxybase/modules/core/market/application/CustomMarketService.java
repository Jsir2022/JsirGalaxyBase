package com.jsirgalaxybase.modules.core.market.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.application.command.FrozenBalanceCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.InternalTransferCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.market.application.command.CancelCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.PublishCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.PurchaseCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketAuditLog;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketAuditType;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketDeliveryStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketItemSnapshot;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListing;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListingStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketAuditLogRepository;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketItemSnapshotRepository;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketListingRepository;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

import net.minecraft.item.ItemStack;

public class CustomMarketService {

    private static final int DEFAULT_LIMIT = 20;

    private final CustomMarketListingRepository listingRepository;
    private final CustomMarketItemSnapshotRepository itemSnapshotRepository;
    private final CustomMarketTradeRecordRepository tradeRecordRepository;
    private final CustomMarketAuditLogRepository auditLogRepository;
    private final MarketTransactionRunner transactionRunner;
    private final MarketSettlementFacade settlementFacade;

    public CustomMarketService(CustomMarketListingRepository listingRepository,
        CustomMarketItemSnapshotRepository itemSnapshotRepository,
        CustomMarketTradeRecordRepository tradeRecordRepository,
        CustomMarketAuditLogRepository auditLogRepository,
        MarketTransactionRunner transactionRunner,
        MarketSettlementFacade settlementFacade) {
        this.listingRepository = listingRepository;
        this.itemSnapshotRepository = itemSnapshotRepository;
        this.tradeRecordRepository = tradeRecordRepository;
        this.auditLogRepository = auditLogRepository;
        this.transactionRunner = transactionRunner;
        this.settlementFacade = settlementFacade;
    }

    public PublishListingResult publishListing(final PublishCustomMarketListingCommand command) {
        validatePublishCommand(command);
        String semanticsKey = buildPublishSemanticsKey(command);
        Optional<CustomMarketAuditLog> existing = auditLogRepository.findByRequestId(command.getRequestId());
        if (existing.isPresent()) {
            ensureMatchingRequest(existing.get(), CustomMarketAuditType.LISTING_PUBLISH, semanticsKey, "publishListing");
            return loadPublishResult(existing.get().getListingId(), existing.get());
        }

        return transactionRunner.inTransaction(new java.util.function.Supplier<PublishListingResult>() {

            @Override
            public PublishListingResult get() {
                Instant now = Instant.now();
                CustomMarketListing savedListing = listingRepository.save(new CustomMarketListing(0L,
                    command.getPlayerRef(), null, command.getAskingPrice(), normalizedCurrency(command.getCurrencyCode()),
                    CustomMarketListingStatus.ACTIVE, CustomMarketDeliveryStatus.ESCROW_HELD,
                    command.getSourceServerId(), now, now));
                CustomMarketItemSnapshot savedSnapshot = itemSnapshotRepository.save(
                    CustomMarketItemSnapshot.capture(savedListing.getListingId(), command.getItemStack().copy(), now));
                CustomMarketAuditLog auditLog = auditLogRepository.save(new CustomMarketAuditLog(0L,
                    command.getRequestId(), CustomMarketAuditType.LISTING_PUBLISH, command.getPlayerRef(),
                    buildPublishSemanticsKey(command), savedListing.getListingId(), 0L, command.getSourceServerId(),
                    "custom listing published", now, now));
                return new PublishListingResult(savedListing, savedSnapshot, auditLog);
            }
        });
    }

    public List<ListingView> browseListings(int limit) {
        int normalizedLimit = sanitizeLimit(limit);
        List<CustomMarketListing> listings = listingRepository.findByStatus(CustomMarketListingStatus.ACTIVE,
            normalizedLimit);
        return loadListingViews(listings);
    }

    public List<ListingView> listSellerActiveListings(String sellerPlayerRef, int limit) {
        return loadListingViews(listingRepository.findBySellerAndDeliveryStatus(sellerPlayerRef,
            CustomMarketDeliveryStatus.ESCROW_HELD, sanitizeLimit(limit)));
    }

    public ListingView inspectListing(long listingId) {
        CustomMarketListing listing = listingRepository.findById(listingId)
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("custom market listing not found: " + listingId);
                }
            });
        CustomMarketItemSnapshot snapshot = requireSnapshot(listingId);
        Optional<CustomMarketTradeRecord> tradeRecord = tradeRecordRepository.findByListingId(listingId);
        return new ListingView(listing, snapshot, tradeRecord.orElse(null));
    }

    public PurchaseListingResult purchaseListing(final PurchaseCustomMarketListingCommand command) {
        validatePurchaseCommand(command);
        final String semanticsKey = buildSimpleSemanticsKey(command.getBuyerPlayerRef(), command.getListingId());
        Optional<CustomMarketAuditLog> existing = auditLogRepository.findByRequestId(command.getRequestId());
        if (existing.isPresent()) {
            ensureMatchingRequest(existing.get(), CustomMarketAuditType.LISTING_PURCHASE, semanticsKey,
                "purchaseListing");
            return loadPurchaseResult(existing.get().getListingId(), existing.get());
        }

        return transactionRunner.inTransaction(new java.util.function.Supplier<PurchaseListingResult>() {

            @Override
            public PurchaseListingResult get() {
                Instant now = Instant.now();
                CustomMarketListing lockedListing = listingRepository.lockById(command.getListingId());
                if (lockedListing.getListingStatus() != CustomMarketListingStatus.ACTIVE) {
                    throw new MarketOperationException("custom market listing is not active: " + lockedListing.getListingId());
                }
                if (command.getBuyerPlayerRef().equals(lockedListing.getSellerPlayerRef())) {
                    throw new MarketOperationException("seller cannot buy own custom listing");
                }
                BankAccount buyerAccount = requireSettlementFacade().requirePlayerAccount(command.getBuyerPlayerRef());
                BankAccount sellerAccount = requireSettlementFacade().requirePlayerAccount(lockedListing.getSellerPlayerRef());
                requireSettlementFacade().freezeBuyerFunds(new FrozenBalanceCommand(
                    command.getRequestId() + ":freeze",
                    com.jsirgalaxybase.modules.core.banking.domain.BankTransactionType.MARKET_FUNDS_FREEZE,
                    com.jsirgalaxybase.modules.core.banking.domain.BankBusinessType.MARKET_ORDER_FREEZE,
                    buyerAccount.getAccountId(), command.getSourceServerId(), BankingConstants.OPERATOR_TYPE_PLAYER,
                    command.getBuyerPlayerRef(), command.getBuyerPlayerRef(), lockedListing.getAskingPrice(),
                    "custom listing purchase reserve",
                    buildBusinessRef("custom-market-buy", lockedListing.getListingId()),
                    "{\"listingId\":" + lockedListing.getListingId() + "}"));
                requireSettlementFacade().settleFromFrozenFunds(new InternalTransferCommand(
                    command.getRequestId() + ":settle",
                    com.jsirgalaxybase.modules.core.banking.domain.BankTransactionType.MARKET_SETTLEMENT_FROM_FROZEN,
                    com.jsirgalaxybase.modules.core.banking.domain.BankBusinessType.MARKET_ORDER_SETTLEMENT,
                    buyerAccount.getAccountId(), sellerAccount.getAccountId(), command.getSourceServerId(),
                    BankingConstants.OPERATOR_TYPE_PLAYER, command.getBuyerPlayerRef(), command.getBuyerPlayerRef(),
                    lockedListing.getAskingPrice(), "custom listing purchase settled",
                    buildBusinessRef("custom-market-settle", lockedListing.getListingId()),
                    "{\"listingId\":" + lockedListing.getListingId() + "}"));

                CustomMarketListing updatedListing = listingRepository.update(lockedListing.withBuyerAndState(
                    command.getBuyerPlayerRef(), CustomMarketListingStatus.SOLD,
                    CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM, now));
                CustomMarketTradeRecord tradeRecord = tradeRecordRepository.save(new CustomMarketTradeRecord(0L,
                    updatedListing.getListingId(), updatedListing.getSellerPlayerRef(), command.getBuyerPlayerRef(),
                    updatedListing.getAskingPrice(), updatedListing.getCurrencyCode(),
                    CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM, now));
                CustomMarketAuditLog auditLog = auditLogRepository.save(new CustomMarketAuditLog(0L,
                    command.getRequestId(), CustomMarketAuditType.LISTING_PURCHASE, command.getBuyerPlayerRef(),
                    semanticsKey, updatedListing.getListingId(), tradeRecord.getTradeId(),
                    command.getSourceServerId(), "custom listing purchased", now, now));
                return new PurchaseListingResult(updatedListing, requireSnapshot(updatedListing.getListingId()),
                    tradeRecord, auditLog);
            }
        });
    }

    public CancelListingResult cancelListing(final CancelCustomMarketListingCommand command) {
        validateCancelCommand(command);
        final String semanticsKey = buildSimpleSemanticsKey(command.getSellerPlayerRef(), command.getListingId());
        Optional<CustomMarketAuditLog> existing = auditLogRepository.findByRequestId(command.getRequestId());
        if (existing.isPresent()) {
            ensureMatchingRequest(existing.get(), CustomMarketAuditType.LISTING_CANCEL, semanticsKey, "cancelListing");
            return loadCancelResult(existing.get().getListingId(), existing.get());
        }

        return transactionRunner.inTransaction(new java.util.function.Supplier<CancelListingResult>() {

            @Override
            public CancelListingResult get() {
                Instant now = Instant.now();
                CustomMarketListing lockedListing = listingRepository.lockById(command.getListingId());
                if (!command.getSellerPlayerRef().equals(lockedListing.getSellerPlayerRef())) {
                    throw new MarketOperationException("custom market listing can only be cancelled by seller");
                }
                if (lockedListing.getListingStatus() != CustomMarketListingStatus.ACTIVE) {
                    throw new MarketOperationException("only active custom listings can be cancelled");
                }
                CustomMarketListing cancelled = listingRepository.update(lockedListing.withState(
                    CustomMarketListingStatus.CANCELLED, CustomMarketDeliveryStatus.CANCELLED, now));
                CustomMarketAuditLog auditLog = auditLogRepository.save(new CustomMarketAuditLog(0L,
                    command.getRequestId(), CustomMarketAuditType.LISTING_CANCEL, command.getSellerPlayerRef(),
                    semanticsKey, cancelled.getListingId(), 0L, command.getSourceServerId(),
                    "custom listing cancelled", now, now));
                return new CancelListingResult(cancelled, requireSnapshot(cancelled.getListingId()), auditLog);
            }
        });
    }

    public ClaimListingResult claimPurchasedListing(final ClaimCustomMarketListingCommand command) {
        validateClaimCommand(command);
        final String semanticsKey = buildSimpleSemanticsKey(command.getBuyerPlayerRef(), command.getListingId());
        Optional<CustomMarketAuditLog> existing = auditLogRepository.findByRequestId(command.getRequestId());
        if (existing.isPresent()) {
            ensureMatchingRequest(existing.get(), CustomMarketAuditType.LISTING_CLAIM, semanticsKey,
                "claimPurchasedListing");
            return loadClaimResult(existing.get().getListingId(), existing.get());
        }

        return transactionRunner.inTransaction(new java.util.function.Supplier<ClaimListingResult>() {

            @Override
            public ClaimListingResult get() {
                Instant now = Instant.now();
                CustomMarketListing lockedListing = listingRepository.lockById(command.getListingId());
                if (!command.getBuyerPlayerRef().equals(lockedListing.getBuyerPlayerRef())) {
                    throw new MarketOperationException("custom market listing can only be claimed by buyer");
                }
                if (lockedListing.getListingStatus() != CustomMarketListingStatus.SOLD) {
                    throw new MarketOperationException("only sold custom listings can be claimed");
                }
                if (lockedListing.getDeliveryStatus() != CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM) {
                    throw new MarketOperationException("custom market listing is not awaiting buyer claim");
                }

                CustomMarketTradeRecord tradeRecord = tradeRecordRepository.findByListingId(command.getListingId())
                    .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                        @Override
                        public MarketOperationException get() {
                            return new MarketOperationException(
                                "custom market trade record not found for listingId=" + command.getListingId());
                        }
                    });
                CustomMarketListing completedListing = listingRepository.update(
                    lockedListing.withDeliveryStatus(CustomMarketDeliveryStatus.COMPLETED, now));
                CustomMarketTradeRecord completedTradeRecord = tradeRecordRepository.update(
                    tradeRecord.withDeliveryStatus(CustomMarketDeliveryStatus.COMPLETED));
                CustomMarketAuditLog auditLog = auditLogRepository.save(new CustomMarketAuditLog(0L,
                    command.getRequestId(), CustomMarketAuditType.LISTING_CLAIM, command.getBuyerPlayerRef(),
                    semanticsKey, completedListing.getListingId(), completedTradeRecord.getTradeId(),
                    command.getSourceServerId(), "custom listing claimed by buyer", now, now));
                return new ClaimListingResult(completedListing, requireSnapshot(completedListing.getListingId()),
                    completedTradeRecord, auditLog);
            }
        });
    }

    public List<ListingView> listSellerPendingDeliveries(String sellerPlayerRef) {
        return loadListingViews(listingRepository.findBySellerAndDeliveryStatus(sellerPlayerRef,
            CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM, DEFAULT_LIMIT));
    }

    public List<ListingView> listBuyerPendingClaims(String buyerPlayerRef) {
        return loadListingViews(listingRepository.findByBuyerAndDeliveryStatus(buyerPlayerRef,
            CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM, DEFAULT_LIMIT));
    }

    private PublishListingResult loadPublishResult(long listingId, CustomMarketAuditLog auditLog) {
        return new PublishListingResult(requireListing(listingId), requireSnapshot(listingId), auditLog);
    }

    private PurchaseListingResult loadPurchaseResult(long listingId, CustomMarketAuditLog auditLog) {
        CustomMarketListing listing = requireListing(listingId);
        CustomMarketTradeRecord tradeRecord = tradeRecordRepository.findByListingId(listingId)
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("custom market trade record not found for listingId=" + listingId);
                }
            });
        return new PurchaseListingResult(listing, requireSnapshot(listingId), tradeRecord, auditLog);
    }

    private CancelListingResult loadCancelResult(long listingId, CustomMarketAuditLog auditLog) {
        return new CancelListingResult(requireListing(listingId), requireSnapshot(listingId), auditLog);
    }

    private ClaimListingResult loadClaimResult(long listingId, CustomMarketAuditLog auditLog) {
        CustomMarketListing listing = requireListing(listingId);
        CustomMarketTradeRecord tradeRecord = tradeRecordRepository.findByListingId(listingId)
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("custom market trade record not found for listingId=" + listingId);
                }
            });
        return new ClaimListingResult(listing, requireSnapshot(listingId), tradeRecord, auditLog);
    }

    private List<ListingView> loadListingViews(List<CustomMarketListing> listings) {
        List<ListingView> views = new ArrayList<ListingView>(listings.size());
        for (CustomMarketListing listing : listings) {
            views.add(new ListingView(listing, requireSnapshot(listing.getListingId()),
                tradeRecordRepository.findByListingId(listing.getListingId()).orElse(null)));
        }
        return views;
    }

    private CustomMarketListing requireListing(final long listingId) {
        return listingRepository.findById(listingId).orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

            @Override
            public MarketOperationException get() {
                return new MarketOperationException("custom market listing not found: " + listingId);
            }
        });
    }

    private CustomMarketItemSnapshot requireSnapshot(final long listingId) {
        return itemSnapshotRepository.findByListingId(listingId)
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("custom market snapshot not found for listingId=" + listingId);
                }
            });
    }

    private void validatePublishCommand(PublishCustomMarketListingCommand command) {
        if (command == null) {
            throw new MarketOperationException("publishListing command must not be null");
        }
        if (command.getAskingPrice() <= 0L) {
            throw new MarketOperationException("askingPrice must be positive");
        }
        ItemStack stack = command.getItemStack();
        if (stack == null || stack.stackSize <= 0) {
            throw new MarketOperationException("custom listing requires a held item");
        }
        if (stack.stackSize != 1) {
            throw new MarketOperationException("custom market listing requires exactly one item in hand");
        }
    }

    private void validatePurchaseCommand(PurchaseCustomMarketListingCommand command) {
        if (command == null) {
            throw new MarketOperationException("purchaseListing command must not be null");
        }
        if (command.getListingId() <= 0L) {
            throw new MarketOperationException("listingId must be positive");
        }
    }

    private void validateCancelCommand(CancelCustomMarketListingCommand command) {
        if (command == null) {
            throw new MarketOperationException("cancelListing command must not be null");
        }
        if (command.getListingId() <= 0L) {
            throw new MarketOperationException("listingId must be positive");
        }
    }

    private void validateClaimCommand(ClaimCustomMarketListingCommand command) {
        if (command == null) {
            throw new MarketOperationException("claimPurchasedListing command must not be null");
        }
        if (command.getListingId() <= 0L) {
            throw new MarketOperationException("listingId must be positive");
        }
    }

    private void ensureMatchingRequest(CustomMarketAuditLog auditLog, CustomMarketAuditType expectedType,
        String semanticsKey, String action) {
        if (auditLog.getAuditType() != expectedType) {
            throw new MarketOperationException("requestId already used by a different custom market action: " + action);
        }
        if (!auditLog.getRequestSemanticsKey().equals(semanticsKey)) {
            throw new MarketOperationException("requestId semantics conflict for custom market action: " + action);
        }
    }

    private String buildPublishSemanticsKey(PublishCustomMarketListingCommand command) {
        CustomMarketItemSnapshot snapshot = CustomMarketItemSnapshot.capture(0L, command.getItemStack().copy(),
            Instant.EPOCH);
        return command.getPlayerRef() + "|" + command.getAskingPrice() + "|" + normalizedCurrency(command.getCurrencyCode())
            + "|" + snapshot.getItemId() + "|" + snapshot.getMeta() + "|" + snapshot.getStackSize() + "|"
            + snapshot.getNbtSnapshot();
    }

    private String buildSimpleSemanticsKey(String playerRef, long listingId) {
        return playerRef + "|" + listingId;
    }

    private String normalizedCurrency(String currencyCode) {
        return currencyCode == null || currencyCode.trim().isEmpty() ? BankingConstants.DEFAULT_CURRENCY_CODE
            : currencyCode.trim().toUpperCase();
    }

    private int sanitizeLimit(int limit) {
        return limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, 50);
    }

    private MarketSettlementFacade requireSettlementFacade() {
        if (settlementFacade == null) {
            throw new MarketOperationException("custom market settlement facade is not available");
        }
        return settlementFacade;
    }

    private String buildBusinessRef(String prefix, long listingId) {
        return prefix + ":listing:" + listingId;
    }

    public static final class PublishListingResult {

        private final CustomMarketListing listing;
        private final CustomMarketItemSnapshot snapshot;
        private final CustomMarketAuditLog auditLog;

        public PublishListingResult(CustomMarketListing listing, CustomMarketItemSnapshot snapshot,
            CustomMarketAuditLog auditLog) {
            this.listing = listing;
            this.snapshot = snapshot;
            this.auditLog = auditLog;
        }

        public CustomMarketListing getListing() {
            return listing;
        }

        public CustomMarketItemSnapshot getSnapshot() {
            return snapshot;
        }

        public CustomMarketAuditLog getAuditLog() {
            return auditLog;
        }
    }

    public static final class PurchaseListingResult {

        private final CustomMarketListing listing;
        private final CustomMarketItemSnapshot snapshot;
        private final CustomMarketTradeRecord tradeRecord;
        private final CustomMarketAuditLog auditLog;

        public PurchaseListingResult(CustomMarketListing listing, CustomMarketItemSnapshot snapshot,
            CustomMarketTradeRecord tradeRecord, CustomMarketAuditLog auditLog) {
            this.listing = listing;
            this.snapshot = snapshot;
            this.tradeRecord = tradeRecord;
            this.auditLog = auditLog;
        }

        public CustomMarketListing getListing() {
            return listing;
        }

        public CustomMarketItemSnapshot getSnapshot() {
            return snapshot;
        }

        public CustomMarketTradeRecord getTradeRecord() {
            return tradeRecord;
        }

        public CustomMarketAuditLog getAuditLog() {
            return auditLog;
        }
    }

    public static final class CancelListingResult {

        private final CustomMarketListing listing;
        private final CustomMarketItemSnapshot snapshot;
        private final CustomMarketAuditLog auditLog;

        public CancelListingResult(CustomMarketListing listing, CustomMarketItemSnapshot snapshot,
            CustomMarketAuditLog auditLog) {
            this.listing = listing;
            this.snapshot = snapshot;
            this.auditLog = auditLog;
        }

        public CustomMarketListing getListing() {
            return listing;
        }

        public CustomMarketItemSnapshot getSnapshot() {
            return snapshot;
        }

        public CustomMarketAuditLog getAuditLog() {
            return auditLog;
        }
    }

    public static final class ClaimListingResult {

        private final CustomMarketListing listing;
        private final CustomMarketItemSnapshot snapshot;
        private final CustomMarketTradeRecord tradeRecord;
        private final CustomMarketAuditLog auditLog;

        public ClaimListingResult(CustomMarketListing listing, CustomMarketItemSnapshot snapshot,
            CustomMarketTradeRecord tradeRecord, CustomMarketAuditLog auditLog) {
            this.listing = listing;
            this.snapshot = snapshot;
            this.tradeRecord = tradeRecord;
            this.auditLog = auditLog;
        }

        public CustomMarketListing getListing() {
            return listing;
        }

        public CustomMarketItemSnapshot getSnapshot() {
            return snapshot;
        }

        public CustomMarketTradeRecord getTradeRecord() {
            return tradeRecord;
        }

        public CustomMarketAuditLog getAuditLog() {
            return auditLog;
        }
    }

    public static final class ListingView {

        private final CustomMarketListing listing;
        private final CustomMarketItemSnapshot snapshot;
        private final CustomMarketTradeRecord tradeRecord;

        public ListingView(CustomMarketListing listing, CustomMarketItemSnapshot snapshot,
            CustomMarketTradeRecord tradeRecord) {
            this.listing = listing;
            this.snapshot = snapshot;
            this.tradeRecord = tradeRecord;
        }

        public CustomMarketListing getListing() {
            return listing;
        }

        public CustomMarketItemSnapshot getSnapshot() {
            return snapshot;
        }

        public CustomMarketTradeRecord getTradeRecord() {
            return tradeRecord;
        }
    }
}

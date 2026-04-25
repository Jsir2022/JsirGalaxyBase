package com.jsirgalaxybase.modules.core.market.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.command.FrozenBalanceCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.InternalTransferCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountStatus;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;
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

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class CustomMarketServiceTest {

    @Test
    public void publishListingStoresDedicatedSnapshotAndBrowseableListing() {
        FakeCustomMarketListingRepository listingRepository = new FakeCustomMarketListingRepository();
        FakeCustomMarketItemSnapshotRepository snapshotRepository = new FakeCustomMarketItemSnapshotRepository();
        FakeCustomMarketTradeRecordRepository tradeRecordRepository = new FakeCustomMarketTradeRecordRepository();
        FakeCustomMarketAuditLogRepository auditLogRepository = new FakeCustomMarketAuditLogRepository();
        CustomMarketService service = createService(listingRepository, snapshotRepository, tradeRecordRepository,
            auditLogRepository, new FakeMarketSettlementFacade());

        ItemStack stack = namedStack("Relic Stone", 1, "seller-a");
        CustomMarketService.PublishListingResult result = service.publishListing(new PublishCustomMarketListingCommand(
            "req-custom-publish", "seller-a", "test-server", 1200L, "STARCOIN", stack));

        assertEquals(CustomMarketListingStatus.ACTIVE, result.getListing().getListingStatus());
        assertEquals(CustomMarketDeliveryStatus.ESCROW_HELD, result.getListing().getDeliveryStatus());
        assertEquals("Relic Stone", result.getSnapshot().getDisplayName());
        assertEquals(CustomMarketAuditType.LISTING_PUBLISH, result.getAuditLog().getAuditType());

        List<CustomMarketService.ListingView> browse = service.browseListings(10);
        assertEquals(1, browse.size());
        assertEquals(result.getListing().getListingId(), browse.get(0).getListing().getListingId());
        assertEquals("seller-a", browse.get(0).getListing().getSellerPlayerRef());

        assertEquals(1, browse.get(0).getSnapshot().getStackSize());
        assertTrue(browse.get(0).getSnapshot().getNbtSnapshot() != null
            && !browse.get(0).getSnapshot().getNbtSnapshot().isEmpty());
    }

    @Test
    public void publishListingRejectsStackedListingForSingleItemBoundary() {
        FakeCustomMarketListingRepository listingRepository = new FakeCustomMarketListingRepository();
        FakeCustomMarketItemSnapshotRepository snapshotRepository = new FakeCustomMarketItemSnapshotRepository();
        FakeCustomMarketTradeRecordRepository tradeRecordRepository = new FakeCustomMarketTradeRecordRepository();
        FakeCustomMarketAuditLogRepository auditLogRepository = new FakeCustomMarketAuditLogRepository();
        CustomMarketService service = createService(listingRepository, snapshotRepository, tradeRecordRepository,
            auditLogRepository, new FakeMarketSettlementFacade());

        try {
            service.publishListing(new PublishCustomMarketListingCommand("req-custom-publish-stacked", "seller-a",
                "test-server", 1200L, "STARCOIN", namedStack("Relic Stone", 4, "seller-a")));
            fail("expected stacked custom listing to be rejected");
        } catch (MarketOperationException expected) {
            assertTrue(expected.getMessage().contains("exactly one item"));
        }
    }

    @Test
    public void purchaseListingUsesDedicatedPendingClaimStateAndSettlement() {
        FakeCustomMarketListingRepository listingRepository = new FakeCustomMarketListingRepository();
        FakeCustomMarketItemSnapshotRepository snapshotRepository = new FakeCustomMarketItemSnapshotRepository();
        FakeCustomMarketTradeRecordRepository tradeRecordRepository = new FakeCustomMarketTradeRecordRepository();
        FakeCustomMarketAuditLogRepository auditLogRepository = new FakeCustomMarketAuditLogRepository();
        FakeMarketSettlementFacade settlementFacade = new FakeMarketSettlementFacade();
        settlementFacade.registerPlayer("seller-a");
        settlementFacade.registerPlayer("buyer-b");
        CustomMarketService service = createService(listingRepository, snapshotRepository, tradeRecordRepository,
            auditLogRepository, settlementFacade);

        CustomMarketService.PublishListingResult publishResult = service.publishListing(new PublishCustomMarketListingCommand(
            "req-custom-publish-buy", "seller-a", "test-server", 2400L, "STARCOIN",
            namedStack("Ancient Gear", 1, "seller-a")));

        CustomMarketService.PurchaseListingResult purchaseResult = service.purchaseListing(
            new PurchaseCustomMarketListingCommand("req-custom-buy", "buyer-b", "test-server",
                publishResult.getListing().getListingId()));

        assertEquals(CustomMarketListingStatus.SOLD, purchaseResult.getListing().getListingStatus());
        assertEquals(CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM,
            purchaseResult.getListing().getDeliveryStatus());
        assertEquals(1, settlementFacade.freezeCommands.size());
        assertEquals(1, settlementFacade.settleCommands.size());
        assertEquals(1, service.listSellerPendingDeliveries("seller-a").size());
        assertEquals(1, service.listBuyerPendingClaims("buyer-b").size());
        assertNotNull(purchaseResult.getTradeRecord());
        assertEquals(CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM,
            purchaseResult.getTradeRecord().getDeliveryStatus());
    }

    @Test
    public void cancelListingMovesDedicatedListingOutOfActivePool() {
        FakeCustomMarketListingRepository listingRepository = new FakeCustomMarketListingRepository();
        FakeCustomMarketItemSnapshotRepository snapshotRepository = new FakeCustomMarketItemSnapshotRepository();
        FakeCustomMarketTradeRecordRepository tradeRecordRepository = new FakeCustomMarketTradeRecordRepository();
        FakeCustomMarketAuditLogRepository auditLogRepository = new FakeCustomMarketAuditLogRepository();
        CustomMarketService service = createService(listingRepository, snapshotRepository, tradeRecordRepository,
            auditLogRepository, new FakeMarketSettlementFacade());

        CustomMarketService.PublishListingResult publishResult = service.publishListing(new PublishCustomMarketListingCommand(
            "req-custom-publish-cancel", "seller-a", "test-server", 900L, "STARCOIN",
            namedStack("Used Drill", 1, "seller-a")));

        CustomMarketService.CancelListingResult cancelResult = service.cancelListing(
            new CancelCustomMarketListingCommand("req-custom-cancel", "seller-a", "test-server",
                publishResult.getListing().getListingId()));

        assertEquals(CustomMarketListingStatus.CANCELLED, cancelResult.getListing().getListingStatus());
        assertEquals(CustomMarketDeliveryStatus.CANCELLED, cancelResult.getListing().getDeliveryStatus());
        assertTrue(service.browseListings(10).isEmpty());
        assertEquals("Used Drill", cancelResult.getSnapshot().getDisplayName());
    }

    @Test
    public void claimPurchasedListingCompletesDeliveryAndRemovesPendingViews() {
        FakeCustomMarketListingRepository listingRepository = new FakeCustomMarketListingRepository();
        FakeCustomMarketItemSnapshotRepository snapshotRepository = new FakeCustomMarketItemSnapshotRepository();
        FakeCustomMarketTradeRecordRepository tradeRecordRepository = new FakeCustomMarketTradeRecordRepository();
        FakeCustomMarketAuditLogRepository auditLogRepository = new FakeCustomMarketAuditLogRepository();
        FakeMarketSettlementFacade settlementFacade = new FakeMarketSettlementFacade();
        settlementFacade.registerPlayer("seller-a");
        settlementFacade.registerPlayer("buyer-b");
        CustomMarketService service = createService(listingRepository, snapshotRepository, tradeRecordRepository,
            auditLogRepository, settlementFacade);

        CustomMarketService.PublishListingResult publishResult = service.publishListing(new PublishCustomMarketListingCommand(
            "req-custom-claim-publish", "seller-a", "test-server", 2400L, "STARCOIN",
            namedStack("Ancient Gear", 1, "seller-a")));
        service.purchaseListing(new PurchaseCustomMarketListingCommand("req-custom-claim-buy", "buyer-b",
            "test-server", publishResult.getListing().getListingId()));

        CustomMarketService.ClaimListingResult claimResult = service.claimPurchasedListing(
            new ClaimCustomMarketListingCommand("req-custom-claim", "buyer-b", "test-server",
                publishResult.getListing().getListingId()));

        assertEquals(CustomMarketDeliveryStatus.COMPLETED, claimResult.getListing().getDeliveryStatus());
        assertEquals(CustomMarketDeliveryStatus.COMPLETED, claimResult.getTradeRecord().getDeliveryStatus());
        assertTrue(service.listSellerPendingDeliveries("seller-a").isEmpty());
        assertTrue(service.listBuyerPendingClaims("buyer-b").isEmpty());
    }

    @Test
    public void publishListingRejectsRequestIdSemanticsConflict() {
        FakeCustomMarketListingRepository listingRepository = new FakeCustomMarketListingRepository();
        FakeCustomMarketItemSnapshotRepository snapshotRepository = new FakeCustomMarketItemSnapshotRepository();
        FakeCustomMarketTradeRecordRepository tradeRecordRepository = new FakeCustomMarketTradeRecordRepository();
        FakeCustomMarketAuditLogRepository auditLogRepository = new FakeCustomMarketAuditLogRepository();
        CustomMarketService service = createService(listingRepository, snapshotRepository, tradeRecordRepository,
            auditLogRepository, new FakeMarketSettlementFacade());

        service.publishListing(new PublishCustomMarketListingCommand("req-custom-conflict", "seller-a",
            "test-server", 1000L, "STARCOIN", namedStack("Relic A", 1, "seller-a")));

        try {
            service.publishListing(new PublishCustomMarketListingCommand("req-custom-conflict", "seller-a",
                "test-server", 2000L, "STARCOIN", namedStack("Relic B", 1, "seller-a")));
            fail("expected request semantics conflict");
        } catch (MarketOperationException expected) {
            assertTrue(expected.getMessage().contains("requestId semantics conflict"));
        }
    }

    private CustomMarketService createService(FakeCustomMarketListingRepository listingRepository,
        FakeCustomMarketItemSnapshotRepository snapshotRepository,
        FakeCustomMarketTradeRecordRepository tradeRecordRepository,
        FakeCustomMarketAuditLogRepository auditLogRepository,
        FakeMarketSettlementFacade settlementFacade) {
        return new CustomMarketService(listingRepository, snapshotRepository, tradeRecordRepository,
            auditLogRepository, new DirectMarketTransactionRunner(), settlementFacade);
    }

    private ItemStack namedStack(String displayName, int stackSize, String owner) {
        ItemStack stack = new ItemStack(Blocks.stone, stackSize, 0);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", displayName);
        tag.setTag("display", display);
        tag.setString("owner", owner);
        stack.setTagCompound(tag);
        return stack;
    }

    private static final class FakeCustomMarketListingRepository implements CustomMarketListingRepository {

        private final Map<Long, CustomMarketListing> listingsById = new HashMap<Long, CustomMarketListing>();
        private long nextId = 1L;

        @Override
        public CustomMarketListing save(CustomMarketListing listing) {
            CustomMarketListing persisted = new CustomMarketListing(nextId++, listing.getSellerPlayerRef(),
                listing.getBuyerPlayerRef(), listing.getAskingPrice(), listing.getCurrencyCode(),
                listing.getListingStatus(), listing.getDeliveryStatus(), listing.getSourceServerId(),
                listing.getCreatedAt(), listing.getUpdatedAt());
            listingsById.put(Long.valueOf(persisted.getListingId()), persisted);
            return persisted;
        }

        @Override
        public CustomMarketListing update(CustomMarketListing listing) {
            listingsById.put(Long.valueOf(listing.getListingId()), listing);
            return listing;
        }

        @Override
        public Optional<CustomMarketListing> findById(long listingId) {
            return Optional.ofNullable(listingsById.get(Long.valueOf(listingId)));
        }

        @Override
        public CustomMarketListing lockById(long listingId) {
            CustomMarketListing listing = listingsById.get(Long.valueOf(listingId));
            if (listing == null) {
                throw new MarketOperationException("custom market listing not found: " + listingId);
            }
            return listing;
        }

        @Override
        public List<CustomMarketListing> findByStatus(CustomMarketListingStatus status, int limit) {
            List<CustomMarketListing> matches = new ArrayList<CustomMarketListing>();
            for (CustomMarketListing listing : listingsById.values()) {
                if (listing.getListingStatus() == status) {
                    matches.add(listing);
                }
            }
            return clip(matches, limit);
        }

        @Override
        public List<CustomMarketListing> findBySellerAndDeliveryStatus(String sellerPlayerRef,
            CustomMarketDeliveryStatus deliveryStatus, int limit) {
            List<CustomMarketListing> matches = new ArrayList<CustomMarketListing>();
            for (CustomMarketListing listing : listingsById.values()) {
                if (sellerPlayerRef.equals(listing.getSellerPlayerRef())
                    && listing.getDeliveryStatus() == deliveryStatus) {
                    matches.add(listing);
                }
            }
            return clip(matches, limit);
        }

        @Override
        public List<CustomMarketListing> findByBuyerAndDeliveryStatus(String buyerPlayerRef,
            CustomMarketDeliveryStatus deliveryStatus, int limit) {
            List<CustomMarketListing> matches = new ArrayList<CustomMarketListing>();
            for (CustomMarketListing listing : listingsById.values()) {
                if (buyerPlayerRef.equals(listing.getBuyerPlayerRef())
                    && listing.getDeliveryStatus() == deliveryStatus) {
                    matches.add(listing);
                }
            }
            return clip(matches, limit);
        }

        private List<CustomMarketListing> clip(List<CustomMarketListing> listings, int limit) {
            if (listings.size() <= limit) {
                return listings;
            }
            return new ArrayList<CustomMarketListing>(listings.subList(0, limit));
        }
    }

    private static final class FakeCustomMarketItemSnapshotRepository implements CustomMarketItemSnapshotRepository {

        private final Map<Long, CustomMarketItemSnapshot> snapshotsByListingId = new HashMap<Long, CustomMarketItemSnapshot>();
        private long nextId = 1L;

        @Override
        public CustomMarketItemSnapshot save(CustomMarketItemSnapshot snapshot) {
            CustomMarketItemSnapshot persisted = new CustomMarketItemSnapshot(nextId++, snapshot.getListingId(),
                snapshot.getItemId(), snapshot.getMeta(), snapshot.getStackSize(), snapshot.isStackable(),
                snapshot.getDisplayName(), snapshot.getNbtSnapshot(), snapshot.getCreatedAt());
            snapshotsByListingId.put(Long.valueOf(persisted.getListingId()), persisted);
            return persisted;
        }

        @Override
        public Optional<CustomMarketItemSnapshot> findByListingId(long listingId) {
            return Optional.ofNullable(snapshotsByListingId.get(Long.valueOf(listingId)));
        }
    }

    private static final class FakeCustomMarketTradeRecordRepository implements CustomMarketTradeRecordRepository {

        private final Map<Long, CustomMarketTradeRecord> tradeByListingId = new HashMap<Long, CustomMarketTradeRecord>();
        private long nextId = 1L;

        @Override
        public CustomMarketTradeRecord save(CustomMarketTradeRecord tradeRecord) {
            CustomMarketTradeRecord persisted = new CustomMarketTradeRecord(nextId++, tradeRecord.getListingId(),
                tradeRecord.getSellerPlayerRef(), tradeRecord.getBuyerPlayerRef(), tradeRecord.getSettledAmount(),
                tradeRecord.getCurrencyCode(), tradeRecord.getDeliveryStatus(), tradeRecord.getCreatedAt());
            tradeByListingId.put(Long.valueOf(persisted.getListingId()), persisted);
            return persisted;
        }

        @Override
        public CustomMarketTradeRecord update(CustomMarketTradeRecord tradeRecord) {
            tradeByListingId.put(Long.valueOf(tradeRecord.getListingId()), tradeRecord);
            return tradeRecord;
        }

        @Override
        public Optional<CustomMarketTradeRecord> findByListingId(long listingId) {
            return Optional.ofNullable(tradeByListingId.get(Long.valueOf(listingId)));
        }
    }

    private static final class FakeCustomMarketAuditLogRepository implements CustomMarketAuditLogRepository {

        private final Map<String, CustomMarketAuditLog> auditByRequestId = new HashMap<String, CustomMarketAuditLog>();
        private long nextId = 1L;

        @Override
        public CustomMarketAuditLog save(CustomMarketAuditLog auditLog) {
            CustomMarketAuditLog persisted = new CustomMarketAuditLog(nextId++, auditLog.getRequestId(),
                auditLog.getAuditType(), auditLog.getPlayerRef(), auditLog.getRequestSemanticsKey(),
                auditLog.getListingId(), auditLog.getTradeId(), auditLog.getSourceServerId(),
                auditLog.getMessage(), auditLog.getCreatedAt(), auditLog.getUpdatedAt());
            auditByRequestId.put(persisted.getRequestId(), persisted);
            return persisted;
        }

        @Override
        public Optional<CustomMarketAuditLog> findByRequestId(String requestId) {
            return Optional.ofNullable(auditByRequestId.get(requestId));
        }
    }

    private static final class FakeMarketSettlementFacade extends MarketSettlementFacade {

        private final Map<String, BankAccount> playerAccounts = new HashMap<String, BankAccount>();
        private final List<FrozenBalanceCommand> freezeCommands = new ArrayList<FrozenBalanceCommand>();
        private final List<InternalTransferCommand> settleCommands = new ArrayList<InternalTransferCommand>();
        private long nextAccountId = 1L;

        private void registerPlayer(String playerRef) {
            playerAccounts.put(playerRef, new BankAccount(nextAccountId, "ACC-" + nextAccountId,
                BankAccountType.PLAYER, "player", playerRef, "STARCOIN", 100000L, 0L, BankAccountStatus.ACTIVE,
                0L, playerRef, "{}", Instant.now(), Instant.now()));
            nextAccountId++;
        }

        @Override
        public BankAccount requirePlayerAccount(String playerRef) {
            BankAccount account = playerAccounts.get(playerRef);
            if (account == null) {
                throw new MarketOperationException("player bank account not found: " + playerRef);
            }
            return account;
        }

        @Override
        public BankPostingResult freezeBuyerFunds(FrozenBalanceCommand command) {
            freezeCommands.add(command);
            return emptyPostingResult();
        }

        @Override
        public BankPostingResult settleFromFrozenFunds(InternalTransferCommand command) {
            settleCommands.add(command);
            return emptyPostingResult();
        }

        private BankPostingResult emptyPostingResult() {
            return new BankPostingResult(null, Collections.<BankAccount>emptyList(), Collections.emptyList(), null);
        }
    }

    private static final class DirectMarketTransactionRunner implements MarketTransactionRunner {

        @Override
        public <T> T inTransaction(java.util.function.Supplier<T> callback) {
            return callback.get();
        }

        @Override
        public void inTransaction(Runnable callback) {
            callback.run();
        }
    }
}
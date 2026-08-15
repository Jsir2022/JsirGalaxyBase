package com.jsirgalaxybase.modules.core.vault.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Test;

import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;
import com.jsirgalaxybase.modules.core.vault.domain.VaultAccount;
import com.jsirgalaxybase.modules.core.vault.domain.VaultAccountType;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperation;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationStatus;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationSlotChange;
import com.jsirgalaxybase.modules.core.vault.domain.VaultPermission;
import com.jsirgalaxybase.modules.core.vault.domain.VaultSlot;
import com.jsirgalaxybase.modules.core.vault.port.BaseVaultRepository;
import com.jsirgalaxybase.modules.core.vault.port.VaultAuthorityPort;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class BaseVaultServiceTest {

    private static final Item TEST_ITEM = new Item().setUnlocalizedName("base_vault_test_item");

    @Test
    public void personalVaultHasTwentySevenSlotsAndMergesCompatibleStacks() {
        BaseVaultService service = new BaseVaultService(new InMemoryRepository(), new DirectTransactionRunner());

        service.deliverToPersonalVault("vault-merge-a", "player-a", "TEST", new ItemStack(TEST_ITEM, 32));
        service.deliverToPersonalVault("vault-merge-b", "player-a", "TEST", new ItemStack(TEST_ITEM, 16));

        BaseVaultService.VaultView view = service.viewPersonalVault("player-a");
        assertEquals(27, view.getAccount().getSlotCount());
        assertEquals(27, view.getSlots().size());
        assertNotNull(view.getSlots().get(0).getStack());
        assertEquals(48, view.getSlots().get(0).getStack().stackSize);
    }

    @Test
    public void deliveryRequestIsIdempotent() {
        BaseVaultService service = new BaseVaultService(new InMemoryRepository(), new DirectTransactionRunner());
        ItemStack stack = new ItemStack(TEST_ITEM, 5);

        service.deliverToPersonalVault("vault-idempotent", "player-a", "MARKET_CLAIM", stack);
        service.deliverToPersonalVault("vault-idempotent", "player-a", "MARKET_CLAIM", stack);

        assertEquals(5, count(service.viewPersonalVault("player-a"), TEST_ITEM));
    }

    @Test(expected = VaultCapacityException.class)
    public void fullPersonalVaultRejectsAnotherUniqueStack() {
        BaseVaultService service = new BaseVaultService(new InMemoryRepository(), new DirectTransactionRunner());
        for (int index = 0; index < 27; index++) {
            ItemStack stack = new ItemStack(TEST_ITEM, 1);
            stack.setStackDisplayName("test-slot-" + index);
            service.deliverToPersonalVault("vault-full-" + index, "player-a", "TEST", stack);
        }

        service.deliverToPersonalVault("vault-full-overflow", "player-a", "TEST", new ItemStack(TEST_ITEM, 1));
    }

    @Test
    public void standardizedTakeRemovesOnlyRequestedQuantity() {
        BaseVaultService service = new BaseVaultService(new InMemoryRepository(), new DirectTransactionRunner());
        service.deliverToPersonalVault("vault-take-seed", "player-a", "TEST", new ItemStack(TEST_ITEM, 20));
        String registryName = String.valueOf(net.minecraft.item.Item.itemRegistry.getNameForObject(TEST_ITEM));

        ItemStack taken = service.takeStandardizedProduct("vault-take", "player-a", registryName, 0, 7,
            "MARKET_CUSTODY");

        assertEquals(7, taken.stackSize);
        assertEquals(13, count(service.viewPersonalVault("player-a"), TEST_ITEM));
        assertTrue(service.viewPersonalVault("player-a").getSlots().get(0).getVersion() > 0L);
    }

    @Test
    public void externalInventoryDepositStaysPendingUntilDeliveryIsProven() {
        BaseVaultService service = new BaseVaultService(new InMemoryRepository(), new DirectTransactionRunner());

        VaultOperation prepared = service.preparePersonalDeposit("vault-player-deposit", "player-a", "PLAYER_INVENTORY",
            new ItemStack(TEST_ITEM, 4));
        assertEquals(VaultOperationStatus.PROCESSING, prepared.getStatus());
        assertEquals(0, count(service.viewPersonalVault("player-a"), TEST_ITEM));

        service.completePreparedPersonalDeposit("vault-player-deposit", "player-a", new ItemStack(TEST_ITEM, 4));

        assertEquals(4, count(service.viewPersonalVault("player-a"), TEST_ITEM));
    }

    @Test
    public void preparedWithdrawalDoesNotReplayBeforeExternalDeliveryCompletes() {
        BaseVaultService service = new BaseVaultService(new InMemoryRepository(), new DirectTransactionRunner());
        service.deliverToPersonalVault("vault-withdraw-seed", "player-a", "TEST", new ItemStack(TEST_ITEM, 4));

        BaseVaultService.VaultWithdrawal prepared = service.preparePersonalWithdrawal("vault-withdraw", "player-a", 0, 2,
            "PLAYER_INVENTORY");
        assertEquals(2, prepared.getStack().stackSize);
        assertEquals(2, count(service.viewPersonalVault("player-a"), TEST_ITEM));
        try {
            service.preparePersonalWithdrawal("vault-withdraw", "player-a", 0, 1, "PLAYER_INVENTORY");
            org.junit.Assert.fail("pending operation must not replay");
        } catch (VaultException expected) {
            assertTrue(expected.getMessage().contains("pending recovery"));
        }

        BaseVaultService.VaultWithdrawal complete = service.completePreparedWithdrawal("vault-withdraw", prepared);
        assertEquals(VaultOperationStatus.COMPLETED, complete.getOperation().getStatus());
    }

    @Test
    public void capacityPreflightRequiresRoomForTheWholeDelivery() {
        BaseVaultService service = new BaseVaultService(new InMemoryRepository(), new DirectTransactionRunner());
        for (int index = 0; index < 27; index++) {
            ItemStack stack = new ItemStack(TEST_ITEM, 1);
            stack.setStackDisplayName("full-slot-" + index);
            service.deliverToPersonalVault("vault-preflight-" + index, "player-a", "TEST", stack);
        }
        assertTrue(!service.canFitPersonalVault("player-a", new ItemStack(TEST_ITEM, 1)));
    }

    @Test
    public void containerMutationCommitsVersionCheckedSlotSnapshot() {
        BaseVaultService service = new BaseVaultService(new InMemoryRepository(), new DirectTransactionRunner());
        service.deliverToPersonalVault("vault-container-seed", "player-a", "TEST", new ItemStack(TEST_ITEM, 6));
        BaseVaultService.VaultView opening = service.viewPersonalVault("player-a");
        List<ItemStack> changed = new ArrayList<ItemStack>();
        for (VaultSlot slot : opening.getSlots()) {
            changed.add(slot.getStack());
        }
        changed.get(0).stackSize = 2;
        changed.set(1, new ItemStack(TEST_ITEM, 4));

        service.commitPersonalContainerMutation("vault-container-mutation", "player-a", opening.getSlots(), changed,
            "test native slot click");

        BaseVaultService.VaultView latest = service.viewPersonalVault("player-a");
        assertEquals(2, latest.getSlots().get(0).getStack().stackSize);
        assertEquals(4, latest.getSlots().get(1).getStack().stackSize);
        assertTrue(latest.getSlots().get(0).getVersion() > opening.getSlots().get(0).getVersion());
    }

    @Test
    public void vaultSortMergesCompatibleStacksWithoutTouchingTotalQuantity() {
        InMemoryRepository repository = new InMemoryRepository();
        BaseVaultService service = new BaseVaultService(repository, new DirectTransactionRunner());
        service.deliverToPersonalVault("vault-sort-a", "player-a", "TEST", new ItemStack(TEST_ITEM, 20));
        service.deliverToPersonalVault("vault-sort-b", "player-a", "TEST", new ItemStack(TEST_ITEM, 12));

        BaseVaultService.VaultView opening = service.viewPersonalVault("player-a");
        List<ItemStack> split = new ArrayList<ItemStack>();
        for (VaultSlot slot : opening.getSlots()) split.add(slot.getStack());
        split.get(0).stackSize = 12;
        split.set(7, new ItemStack(TEST_ITEM, 20));
        service.commitPersonalContainerMutation("vault-sort-seed", "player-a", opening.getSlots(), split, "test split");

        BaseVaultService.VaultSortResult sorted = service.sortPersonalVault("vault-sort", "player-a");
        assertTrue(sorted.isChanged());
        assertEquals(32, sorted.getView().getSlots().get(0).getStack().stackSize);
        assertEquals(32, count(sorted.getView(), TEST_ITEM));
        assertEquals("VAULT_SORT", sorted.getOperation().getOperationType());
        assertEquals(2, repository.getOperationSlotChanges(sorted.getOperation().getOperationId()).size());
    }

    @Test
    public void enterpriseAndPublicVaultSortUseTheirFiftyFourSlotCapacity() {
        InMemoryRepository repository = new InMemoryRepository();
        BaseVaultService service = new BaseVaultService(repository, new DirectTransactionRunner());
        VaultAccountType[] accountTypes = { VaultAccountType.ENTERPRISE, VaultAccountType.PUBLIC };
        for (VaultAccountType type : accountTypes) {
            String accountRef = type.name().toLowerCase() + "-alpha";
            ItemStack later = new ItemStack(TEST_ITEM, 9, 1);
            ItemStack earlier = new ItemStack(TEST_ITEM, 7, 0);
            service.deposit("vault-sort-" + type.name(), type, accountRef, "TEST", "BASE_VAULT",
                later, "TEST_DELIVERY");
            service.deposit("vault-sort-second-" + type.name(), type, accountRef, "TEST", "BASE_VAULT",
                earlier, "TEST_DELIVERY");

            BaseVaultService.VaultSortResult sorted = service.sortVault("vault-sort-run-" + type.name(), type,
                accountRef);

            assertEquals(54, sorted.getView().getSlots().size());
            assertEquals(16, count(sorted.getView(), TEST_ITEM));
            assertTrue(sorted.isChanged());
            assertEquals(0, sorted.getView().getSlots().get(0).getStack().getItemDamage());
            assertEquals(1, sorted.getView().getSlots().get(1).getStack().getItemDamage());
            assertEquals(2, repository.getOperationSlotChanges(sorted.getOperation().getOperationId()).size());
        }
    }

    @Test
    public void personalVaultAccessIsLimitedToItsOwner() {
        BaseVaultService service = new BaseVaultService(new InMemoryRepository(), new DirectTransactionRunner());
        VaultAccessService access = new VaultAccessService(service);

        assertEquals(27, access.view("player-a", VaultAccountType.PERSONAL, "player-a")
            .getSlots().size());
        assertFalse(access.canAccess("player-b", VaultAccountType.PERSONAL, "player-a", VaultPermission.VIEW));
        try {
            access.view("player-b", VaultAccountType.PERSONAL, "player-a");
            org.junit.Assert.fail("another player must not read a personal Vault");
        } catch (VaultAccessDeniedException expected) {
            assertTrue(expected.getMessage().contains("access denied"));
        }
    }

    @Test
    public void organizationVaultsDefaultToDenyAndRequireExplicitRolePermission() {
        BaseVaultService service = new BaseVaultService(new InMemoryRepository(), new DirectTransactionRunner());
        VaultAccessService denied = new VaultAccessService(service);
        assertFalse(denied.canAccess("member-a", VaultAccountType.ENTERPRISE, "enterprise-a",
            VaultPermission.DEPOSIT));
        assertFalse(denied.canAccess("member-a", VaultAccountType.PUBLIC, "public-a",
            VaultPermission.WITHDRAW));

        VaultAuthorityPort authority = new VaultAuthorityPort() {
            @Override
            public boolean hasPermission(String actorRef, VaultAccountType accountType, String accountRef,
                VaultPermission permission) {
                return "member-a".equals(actorRef) && "enterprise-a".equals(accountRef)
                    && (permission == VaultPermission.VIEW || permission == VaultPermission.SORT);
            }
        };
        VaultAccessService allowed = new VaultAccessService(service, authority);
        assertEquals(54, allowed.view("member-a", VaultAccountType.ENTERPRISE, "enterprise-a")
            .getSlots().size());
        assertTrue(allowed.canAccess("member-a", VaultAccountType.ENTERPRISE, "enterprise-a",
            VaultPermission.SORT));
        assertFalse(allowed.canAccess("member-a", VaultAccountType.ENTERPRISE, "enterprise-a",
            VaultPermission.WITHDRAW));
    }

    private static int count(BaseVaultService.VaultView view, net.minecraft.item.Item item) {
        int total = 0;
        for (VaultSlot slot : view.getSlots()) {
            if (slot.getStack() != null && slot.getStack().getItem() == item) {
                total += slot.getStack().stackSize;
            }
        }
        return total;
    }

    private static final class DirectTransactionRunner implements MarketTransactionRunner {
        @Override
        public <T> T inTransaction(Supplier<T> callback) {
            return callback.get();
        }

        @Override
        public void inTransaction(Runnable callback) {
            callback.run();
        }
    }

    private static final class InMemoryRepository implements BaseVaultRepository {
        private final Map<String, VaultAccount> accounts = new HashMap<String, VaultAccount>();
        private final Map<Long, Map<Integer, VaultSlot>> slots = new HashMap<Long, Map<Integer, VaultSlot>>();
        private final Map<String, VaultOperation> operations = new HashMap<String, VaultOperation>();
        private final Map<Long, List<VaultOperationSlotChange>> operationSlotChanges =
            new HashMap<Long, List<VaultOperationSlotChange>>();
        private long nextAccountId = 1L;
        private long nextOperationId = 1L;

        @Override
        public VaultAccount ensureAccount(VaultAccountType type, String ref) {
            String key = type.name() + ":" + ref;
            VaultAccount account = accounts.get(key);
            if (account == null) {
                Instant now = Instant.now();
                account = new VaultAccount(nextAccountId++, type, ref, type.getDefaultSlotCount(), "ACTIVE", now, now);
                accounts.put(key, account);
                slots.put(Long.valueOf(account.getAccountId()), new HashMap<Integer, VaultSlot>());
            }
            return account;
        }

        @Override
        public VaultAccount lockAccount(VaultAccountType type, String ref) {
            return ensureAccount(type, ref);
        }

        @Override
        public List<VaultSlot> findSlots(long accountId) {
            return new ArrayList<VaultSlot>(slots.get(Long.valueOf(accountId)).values());
        }

        @Override
        public void saveSlot(long accountId, VaultSlot slot) {
            slots.get(Long.valueOf(accountId)).put(Integer.valueOf(slot.getSlotIndex()), slot);
        }

        @Override
        public Optional<VaultOperation> findOperationByRequestId(String requestId) {
            return Optional.ofNullable(operations.get(requestId));
        }

        @Override
        public VaultOperation saveOperation(VaultOperation operation) {
            VaultOperation saved = new VaultOperation(nextOperationId++, operation.getRequestId(), operation.getAccountId(),
                operation.getOperationType(), operation.getSourceDomain(), operation.getTargetDomain(),
                operation.getItemSnapshot(), operation.getQuantity(), operation.getStatus(), operation.getMessage(),
                operation.getCreatedAt(), operation.getUpdatedAt());
            operations.put(saved.getRequestId(), saved);
            return saved;
        }

        @Override
        public VaultOperation updateOperation(VaultOperation operation) {
            operations.put(operation.getRequestId(), operation);
            return operation;
        }

        @Override
        public void saveOperationSlotChanges(long operationId, List<VaultOperationSlotChange> changes) {
            operationSlotChanges.put(Long.valueOf(operationId), new ArrayList<VaultOperationSlotChange>(changes));
        }

        private List<VaultOperationSlotChange> getOperationSlotChanges(long operationId) {
            List<VaultOperationSlotChange> changes = operationSlotChanges.get(Long.valueOf(operationId));
            return changes == null ? new ArrayList<VaultOperationSlotChange>() : changes;
        }
    }
}

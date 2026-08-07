package com.jsirgalaxybase.modules.core.vault.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;
import com.jsirgalaxybase.modules.core.vault.domain.VaultAccount;
import com.jsirgalaxybase.modules.core.vault.domain.VaultAccountType;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperation;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationStatus;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationSlotChange;
import com.jsirgalaxybase.modules.core.vault.domain.VaultSlot;
import com.jsirgalaxybase.modules.core.vault.port.BaseVaultRepository;

import net.minecraft.item.ItemStack;

/**
 * Finite, account-bound storage. This service never touches a player inventory;
 * inventory adapters create a recovery log before calling it and settle outside
 * the database boundary.
 */
public final class BaseVaultService {

    private final BaseVaultRepository repository;
    private final MarketTransactionRunner transactionRunner;

    public BaseVaultService(BaseVaultRepository repository, MarketTransactionRunner transactionRunner) {
        if (repository == null) {
            throw new VaultException("base vault repository is required");
        }
        if (transactionRunner == null) {
            throw new VaultException("base vault transaction runner is required");
        }
        this.repository = repository;
        this.transactionRunner = transactionRunner;
    }

    public VaultAccount ensurePersonalVault(String playerRef) {
        return repository.ensureAccount(VaultAccountType.PERSONAL, requireText(playerRef, "playerRef"));
    }

    /**
     * Runs a cross-domain vault handoff in the shared banking/market transaction.
     * Callers use this for operations such as Vault -> market custody so the item
     * cannot be removed from one ledger while the other ledger rejects it.
     */
    public <T> T inSharedTransaction(Supplier<T> callback) {
        if (callback == null) {
            throw new VaultException("vault transaction callback is required");
        }
        return transactionRunner.inTransaction(callback);
    }

    public VaultView viewPersonalVault(String playerRef) {
        return viewVault(VaultAccountType.PERSONAL, playerRef);
    }

    /** Reads any finite Vault account. Callers must authorize non-personal accounts first. */
    public VaultView viewVault(VaultAccountType accountType, String accountRef) {
        VaultAccount account = repository.ensureAccount(requireAccountType(accountType), requireText(accountRef, "accountRef"));
        return new VaultView(account, fillSlots(account, repository.findSlots(account.getAccountId())));
    }

    /** Reorders database-owned Vault slots under the selected account lock. */
    public VaultSortResult sortPersonalVault(final String requestId, final String playerRef) {
        return sortVault(requestId, VaultAccountType.PERSONAL, playerRef);
    }

    /**
     * Server-authoritative sort for personal, enterprise, and public Base Vault
     * accounts. The caller owns authorization; this service owns the atomic
     * account lock, slot rewrite, and audit evidence.
     */
    public VaultSortResult sortVault(final String requestId, final VaultAccountType accountType,
        final String accountRef) {
        final String normalizedRequestId = requireText(requestId, "requestId");
        final VaultAccountType normalizedAccountType = requireAccountType(accountType);
        final String normalizedAccountRef = requireText(accountRef, "accountRef");
        return transactionRunner.inTransaction(new Supplier<VaultSortResult>() {
            @Override
            public VaultSortResult get() {
                Optional<VaultOperation> previous = repository.findOperationByRequestId(normalizedRequestId);
                if (previous.isPresent()) {
                    if (previous.get().getStatus() != VaultOperationStatus.COMPLETED) {
                        throw new VaultException("Vault sort is pending recovery: " + normalizedRequestId);
                    }
                    return new VaultSortResult(viewVault(normalizedAccountType, normalizedAccountRef), previous.get(), false);
                }
                VaultAccount account = repository.lockAccount(normalizedAccountType, normalizedAccountRef);
                List<VaultSlot> current = fillSlots(account, repository.findSlots(account.getAccountId()));
                List<ItemStack> source = new ArrayList<ItemStack>(current.size());
                for (VaultSlot slot : current) source.add(slot.getStack());
                List<ItemStack> sorted = VaultSortPlanner.sort(source, account.getSlotCount());
                List<VaultOperationSlotChange> changes = new ArrayList<VaultOperationSlotChange>();
                ItemStack auditStack = firstNonEmpty(source);
                for (int index = 0; index < current.size(); index++) {
                    ItemStack before = current.get(index).getStack();
                    ItemStack after = clean(sorted.get(index));
                    if (!sameStack(before, after)) {
                        changes.add(new VaultOperationSlotChange(index, before, after,
                            current.get(index).getVersion(), current.get(index).getVersion() + 1L));
                    }
                }
                if (changes.isEmpty()) return new VaultSortResult(new VaultView(account, current), null, false);
                if (auditStack == null) throw new VaultException("Vault sort cannot audit an empty Vault");
                VaultOperation operation = repository.saveOperation(createOperation(normalizedRequestId, account,
                    "VAULT_SORT", "BASE_VAULT", "BASE_VAULT", auditStack, VaultOperationStatus.PROCESSING,
                    "Base Vault sort " + VaultSortPlanner.POLICY_VERSION + " prepared: " + changes.size()
                        + " slot change(s)"));
                repository.saveOperationSlotChanges(operation.getOperationId(), changes);
                for (VaultOperationSlotChange change : changes) {
                    repository.saveSlot(account.getAccountId(), new VaultSlot(change.getSlotIndex(), change.getAfter(),
                        change.getAfterVersion()));
                }
                operation = repository.updateOperation(operation.withStatus(VaultOperationStatus.COMPLETED,
                    "Base Vault sort " + VaultSortPlanner.POLICY_VERSION + " completed: " + changes.size()
                        + " slot change(s)", Instant.now()));
                return new VaultSortResult(viewVault(normalizedAccountType, normalizedAccountRef), operation, true);
            }
        });
    }

    /** Persists the recovery marker before a native Container can mutate a player inventory. */
    public VaultOperation preparePersonalContainerMutation(final String requestId, final String playerRef,
        final ItemStack auditStack, final String operationMessage) {
        return transactionRunner.inTransaction(new Supplier<VaultOperation>() {
            @Override
            public VaultOperation get() {
                Optional<VaultOperation> prior = repository.findOperationByRequestId(requireText(requestId, "requestId"));
                if (prior.isPresent()) {
                    return replayPendingOperation(prior.get());
                }
                VaultAccount account = repository.lockAccount(VaultAccountType.PERSONAL,
                    requireText(playerRef, "playerRef"));
                return repository.saveOperation(createOperation(requestId, account, "VAULT_CONTAINER_MUTATION",
                    "PLAYER_INVENTORY", "BASE_VAULT", requireStack(auditStack), VaultOperationStatus.PROCESSING,
                    safeMessage(operationMessage, 0)));
            }
        });
    }

    /**
     * Commits one native Container interaction.  The Container keeps the normal
     * Minecraft click semantics in memory; this method is the sole point where
     * its changed Vault slots become durable.  Versions from the opening view
     * protect against another server/session silently overwriting the Vault.
     */
    public void commitPersonalContainerMutation(final String requestId, final String playerRef,
        final List<VaultSlot> expectedSlots, final List<ItemStack> changedStacks, final String operationMessage) {
        if (expectedSlots == null || changedStacks == null || expectedSlots.size() != changedStacks.size()) {
            throw new VaultException("vault container snapshot is invalid");
        }
        transactionRunner.inTransaction(new Runnable() {
            @Override
            public void run() {
                VaultAccount account = repository.lockAccount(VaultAccountType.PERSONAL,
                    requireText(playerRef, "playerRef"));
                List<VaultSlot> current = fillSlots(account, repository.findSlots(account.getAccountId()));
                if (current.size() != expectedSlots.size()) {
                    throw new VaultException("vault capacity changed during container interaction");
                }
                ItemStack auditStack = null;
                int changes = 0;
                for (int index = 0; index < current.size(); index++) {
                    VaultSlot expected = expectedSlots.get(index);
                    VaultSlot actual = current.get(index);
                    ItemStack next = clean(changedStacks.get(index));
                    if (expected.getVersion() != actual.getVersion()) {
                        throw new VaultException("vault slot version conflict at " + index);
                    }
                    if (!sameStack(actual.getStack(), next)) {
                        changes++;
                        if (auditStack == null) {
                            auditStack = next == null ? actual.getStack() : next;
                        }
                        repository.saveSlot(account.getAccountId(), new VaultSlot(index, next, actual.getVersion() + 1L));
                    }
                }
                if (changes == 0) {
                    return;
                }
                if (auditStack == null) {
                    throw new VaultException("vault container mutation has no auditable item");
                }
                Optional<VaultOperation> prepared = repository.findOperationByRequestId(requireText(requestId, "requestId"));
                VaultOperation operation;
                if (prepared.isPresent()) {
                    operation = prepared.get();
                    if (operation.getStatus() != VaultOperationStatus.PROCESSING) {
                        throw new VaultException("vault container operation cannot commit from " + operation.getStatus());
                    }
                } else {
                    operation = repository.saveOperation(createOperation(requestId, account,
                        "VAULT_CONTAINER_MUTATION", "PLAYER_INVENTORY", "BASE_VAULT", auditStack,
                        VaultOperationStatus.PROCESSING, safeMessage(operationMessage, changes)));
                }
                repository.updateOperation(operation.withStatus(VaultOperationStatus.COMPLETED,
                    "native container mutation completed: " + changes + " vault slot(s)", Instant.now()));
            }
        });
    }

    public int countPersonalProduct(String playerRef, String registryName, int meta) {
        VaultView view = viewPersonalVault(playerRef);
        long total = 0L;
        for (VaultSlot slot : view.getSlots()) {
            ItemStack stack = slot.getStack();
            if (matches(stack, registryName, meta)) {
                total += stack.stackSize;
            }
        }
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    /** Preflight only. The mutating delivery path repeats this check while locked. */
    public boolean canFitPersonalVault(String playerRef, ItemStack stack) {
        ItemStack source = requireStack(stack);
        VaultView view = viewPersonalVault(playerRef);
        return canFit(view.getSlots(), source);
    }

    public VaultDeliveryResult deliverToPersonalVault(String requestId, String playerRef, String sourceDomain,
        ItemStack stack) {
        return deposit(requestId, VaultAccountType.PERSONAL, playerRef, sourceDomain, "BASE_VAULT", stack,
            "VAULT_DELIVERY");
    }

    public VaultWithdrawal withdrawFromPersonalVault(String requestId, String playerRef, int slotIndex, int quantity,
        String targetDomain) {
        VaultWithdrawal prepared = preparePersonalWithdrawal(requestId, playerRef, slotIndex, quantity, targetDomain);
        return completePreparedWithdrawal(requestId, prepared);
    }

    /**
     * Removes the stack from the database vault and leaves an auditable pending
     * operation. A Minecraft-inventory adapter must either complete this after it
     * has proved delivery, or mark it for recovery. This is deliberately not an
     * automatic retry path.
     */
    public VaultWithdrawal preparePersonalWithdrawal(String requestId, String playerRef, int slotIndex, int quantity,
        String targetDomain) {
        return transactionRunner.inTransaction(new Supplier<VaultWithdrawal>() {
            @Override
            public VaultWithdrawal get() {
                VaultAccount account = repository.lockAccount(VaultAccountType.PERSONAL, requireText(playerRef, "playerRef"));
                List<VaultSlot> slots = fillSlots(account, repository.findSlots(account.getAccountId()));
                if (slotIndex < 0 || slotIndex >= slots.size()) {
                    throw new VaultException("vault slot is outside the account capacity");
                }
                VaultSlot source = slots.get(slotIndex);
                ItemStack existing = source.getStack();
                if (existing == null || existing.stackSize <= 0) {
                    throw new VaultException("vault slot is empty");
                }
                if (quantity <= 0 || quantity > existing.stackSize) {
                    throw new VaultException("withdraw quantity is outside the stored stack size");
                }
                Optional<VaultOperation> existingOperation = repository.findOperationByRequestId(requestId);
                if (existingOperation.isPresent()) {
                    return replayWithdrawal(existingOperation.get());
                }
                ItemStack withdrawn = existing.copy();
                withdrawn.stackSize = quantity;
                VaultOperation operation = createOperation(requestId, account, "VAULT_WITHDRAW", "BASE_VAULT",
                    targetDomain, withdrawn, VaultOperationStatus.PROCESSING, "vault withdrawal prepared");
                operation = repository.saveOperation(operation);
                existing.stackSize -= quantity;
                repository.saveSlot(account.getAccountId(), new VaultSlot(slotIndex,
                    existing.stackSize <= 0 ? null : existing, source.getVersion() + 1L));
                return new VaultWithdrawal(account, slotIndex, withdrawn, operation);
            }
        });
    }

    public VaultWithdrawal completePreparedWithdrawal(String requestId, VaultWithdrawal prepared) {
        final String normalizedRequestId = requireText(requestId, "requestId");
        return transactionRunner.inTransaction(new Supplier<VaultWithdrawal>() {
            @Override
            public VaultWithdrawal get() {
                VaultOperation operation = requireOperation(normalizedRequestId);
                if (operation.getStatus() == VaultOperationStatus.COMPLETED) {
                    return replayWithdrawal(operation);
                }
                if (operation.getStatus() != VaultOperationStatus.PROCESSING) {
                    throw new VaultException("vault withdrawal cannot be completed from " + operation.getStatus());
                }
                VaultOperation completed = repository.updateOperation(operation.withStatus(VaultOperationStatus.COMPLETED,
                    "vault withdrawal delivery completed", Instant.now()));
                return new VaultWithdrawal(prepared == null ? null : prepared.getAccount(),
                    prepared == null ? -1 : prepared.getSlotIndex(),
                    prepared == null ? VaultItemStackCodec.decode(completed.getItemSnapshot()) : prepared.getStack(), completed);
            }
        });
    }

    /** Creates an audit record before an external inventory is changed. */
    public VaultOperation preparePersonalDeposit(String requestId, String playerRef, String sourceDomain, ItemStack stack) {
        final String normalizedRequestId = requireText(requestId, "requestId");
        final ItemStack source = requireStack(stack);
        return transactionRunner.inTransaction(new Supplier<VaultOperation>() {
            @Override
            public VaultOperation get() {
                Optional<VaultOperation> previous = repository.findOperationByRequestId(normalizedRequestId);
                if (previous.isPresent()) {
                    return replayPendingOperation(previous.get());
                }
                VaultAccount account = repository.lockAccount(VaultAccountType.PERSONAL, requireText(playerRef, "playerRef"));
                List<VaultSlot> slots = fillSlots(account, repository.findSlots(account.getAccountId()));
                if (!canFit(slots, source)) {
                    throw new VaultCapacityException("Base Vault is full; move or claim assets after freeing space");
                }
                return repository.saveOperation(createOperation(normalizedRequestId, account, "PLAYER_TO_VAULT",
                    sourceDomain, "BASE_VAULT", source, VaultOperationStatus.PROCESSING,
                    "player inventory deposit prepared"));
            }
        });
    }

    public VaultDeliveryResult completePreparedPersonalDeposit(String requestId, String playerRef) {
        VaultOperation operation = requireOperation(requireText(requestId, "requestId"));
        return completePreparedPersonalDeposit(requestId, playerRef, VaultItemStackCodec.decode(operation.getItemSnapshot()));
    }

    /**
     * The inventory adapter supplies the exact stack it removed after it has
     * changed the physical inventory. This avoids decoding an unavailable item
     * registry entry merely to complete a known operation.
     */
    public VaultDeliveryResult completePreparedPersonalDeposit(String requestId, String playerRef, ItemStack deliveredStack) {
        final String normalizedRequestId = requireText(requestId, "requestId");
        final ItemStack confirmedStack = requireStack(deliveredStack);
        return transactionRunner.inTransaction(new Supplier<VaultDeliveryResult>() {
            @Override
            public VaultDeliveryResult get() {
                VaultOperation operation = requireOperation(normalizedRequestId);
                if (operation.getStatus() == VaultOperationStatus.COMPLETED) {
                    return replayDelivery(operation);
                }
                if (operation.getStatus() != VaultOperationStatus.PROCESSING) {
                    throw new VaultException("vault deposit cannot be completed from " + operation.getStatus());
                }
                VaultAccount account = repository.lockAccount(VaultAccountType.PERSONAL, requireText(playerRef, "playerRef"));
                if (!VaultItemStackCodec.encode(confirmedStack).equals(operation.getItemSnapshot())) {
                    throw new VaultException("inventory deposit stack no longer matches its prepared Vault operation");
                }
                ItemStack stack = confirmedStack;
                List<VaultSlot> slots = fillSlots(account, repository.findSlots(account.getAccountId()));
                if (!canFit(slots, stack)) {
                    throw new VaultCapacityException("Base Vault capacity changed before the inventory deposit completed");
                }
                mergeIntoSlots(account, slots, stack);
                VaultOperation completed = repository.updateOperation(operation.withStatus(VaultOperationStatus.COMPLETED,
                    "player inventory deposit completed", Instant.now()));
                return new VaultDeliveryResult(account, stack, completed);
            }
        });
    }

    public void markOperationFailed(String requestId, String message, boolean recoveryRequired) {
        final String normalizedRequestId = requireText(requestId, "requestId");
        transactionRunner.inTransaction(new Runnable() {
            @Override
            public void run() {
                VaultOperation operation = requireOperation(normalizedRequestId);
                if (operation.getStatus() == VaultOperationStatus.COMPLETED) {
                    throw new VaultException("completed vault operation cannot be downgraded");
                }
                repository.updateOperation(operation.withStatus(
                    recoveryRequired ? VaultOperationStatus.RECOVERY_REQUIRED : VaultOperationStatus.FAILED,
                    message == null || message.trim().isEmpty() ? "vault operation failed" : message.trim(), Instant.now()));
            }
        });
    }

    public VaultDeliveryResult deposit(String requestId, VaultAccountType accountType, String accountRef,
        String sourceDomain, String targetDomain, ItemStack stack, String operationType) {
        final String normalizedRequestId = requireText(requestId, "requestId");
        final ItemStack source = requireStack(stack);
        return transactionRunner.inTransaction(new Supplier<VaultDeliveryResult>() {
            @Override
            public VaultDeliveryResult get() {
                Optional<VaultOperation> previous = repository.findOperationByRequestId(normalizedRequestId);
                if (previous.isPresent()) {
                    return replayDelivery(previous.get());
                }
                VaultAccount account = repository.lockAccount(accountType, requireText(accountRef, "accountRef"));
                List<VaultSlot> slots = fillSlots(account, repository.findSlots(account.getAccountId()));
                if (!canFit(slots, source)) {
                    throw new VaultCapacityException("Base Vault is full; claim remains pending until space is available");
                }
                VaultOperation operation = repository.saveOperation(createOperation(normalizedRequestId, account,
                    operationType, sourceDomain, targetDomain, source, VaultOperationStatus.PROCESSING,
                    "vault delivery processing"));
                mergeIntoSlots(account, slots, source);
                operation = repository.updateOperation(operation.withStatus(VaultOperationStatus.COMPLETED,
                    "vault delivery completed", Instant.now()));
                return new VaultDeliveryResult(account, source.copy(), operation);
            }
        });
    }

    public ItemStack takeStandardizedProduct(String requestId, String playerRef, String registryName, int meta,
        int quantity, String targetDomain) {
        if (quantity <= 0) {
            throw new VaultException("quantity must be positive");
        }
        return transactionRunner.inTransaction(new Supplier<ItemStack>() {
            @Override
            public ItemStack get() {
                Optional<VaultOperation> replay = repository.findOperationByRequestId(requestId);
                if (replay.isPresent()) {
                    if (replay.get().getStatus() != VaultOperationStatus.COMPLETED) {
                        throw new VaultException("vault transfer is pending recovery: " + requestId);
                    }
                    return VaultItemStackCodec.decode(replay.get().getItemSnapshot());
                }
                VaultAccount account = repository.lockAccount(VaultAccountType.PERSONAL, requireText(playerRef, "playerRef"));
                List<VaultSlot> slots = fillSlots(account, repository.findSlots(account.getAccountId()));
                int remaining = quantity;
                for (VaultSlot slot : slots) {
                    ItemStack stack = slot.getStack();
                    if (stack == null || !matches(stack, registryName, meta)) {
                        continue;
                    }
                    remaining -= stack.stackSize;
                    if (remaining <= 0) {
                        break;
                    }
                }
                if (remaining > 0) {
                    throw new VaultException("Base Vault does not contain enough of the selected standardized item");
                }
                ItemStack extracted = null;
                int toRemove = quantity;
                for (VaultSlot slot : slots) {
                    ItemStack stack = slot.getStack();
                    if (toRemove <= 0 || stack == null || !matches(stack, registryName, meta)) {
                        continue;
                    }
                    int chunk = Math.min(toRemove, stack.stackSize);
                    if (extracted == null) {
                        extracted = stack.copy();
                        extracted.stackSize = 0;
                    }
                    extracted.stackSize += chunk;
                    stack.stackSize -= chunk;
                    repository.saveSlot(account.getAccountId(), new VaultSlot(slot.getSlotIndex(),
                        stack.stackSize <= 0 ? null : stack, slot.getVersion() + 1L));
                    toRemove -= chunk;
                }
                repository.saveOperation(createOperation(requestId, account, "VAULT_TO_MARKET", "BASE_VAULT",
                    targetDomain, extracted, VaultOperationStatus.COMPLETED, "vault item moved to market custody"));
                return extracted;
            }
        });
    }

    public ItemStack takeSingleItem(String requestId, String playerRef, int slotIndex, String targetDomain) {
        return takeVaultItemForInternalTransfer(requestId, playerRef, slotIndex, 1, targetDomain);
    }

    /**
     * Moves an item from a concrete Vault slot into another database-owned
     * domain (market custody, custom-listing escrow, exchange settlement).
     * This is intentionally distinct from {@link #preparePersonalWithdrawal}:
     * no Minecraft inventory is involved, so the operation can complete inside
     * the shared transaction.
     */
    public ItemStack takeVaultItemForInternalTransfer(String requestId, String playerRef, int slotIndex, int quantity,
        String targetDomain) {
        if (quantity <= 0) {
            throw new VaultException("quantity must be positive");
        }
        return transactionRunner.inTransaction(new Supplier<ItemStack>() {
            @Override
            public ItemStack get() {
                Optional<VaultOperation> replay = repository.findOperationByRequestId(requireText(requestId, "requestId"));
                if (replay.isPresent()) {
                    if (replay.get().getStatus() != VaultOperationStatus.COMPLETED) {
                        throw new VaultException("vault transfer is pending recovery: " + requestId);
                    }
                    return VaultItemStackCodec.decode(replay.get().getItemSnapshot());
                }
                VaultAccount account = repository.lockAccount(VaultAccountType.PERSONAL,
                    requireText(playerRef, "playerRef"));
                List<VaultSlot> slots = fillSlots(account, repository.findSlots(account.getAccountId()));
                if (slotIndex < 0 || slotIndex >= slots.size()) {
                    throw new VaultException("select a valid Base Vault slot before submitting this item");
                }
                VaultSlot slot = slots.get(slotIndex);
                ItemStack stored = slot.getStack();
                if (stored == null || stored.stackSize <= 0) {
                    throw new VaultException("the selected Base Vault slot is empty");
                }
                if (quantity > stored.stackSize) {
                    throw new VaultException("requested quantity exceeds the selected Base Vault stack");
                }
                ItemStack extracted = stored.copy();
                extracted.stackSize = quantity;
                stored.stackSize -= quantity;
                repository.saveSlot(account.getAccountId(), new VaultSlot(slotIndex,
                    stored.stackSize <= 0 ? null : stored, slot.getVersion() + 1L));
                repository.saveOperation(createOperation(requestId, account, "VAULT_TO_INTERNAL_MARKET", "BASE_VAULT",
                    targetDomain, extracted, VaultOperationStatus.COMPLETED,
                    "vault item moved to an internal market domain"));
                return extracted;
            }
        });
    }

    private VaultOperation createOperation(String requestId, VaultAccount account, String operationType,
        String sourceDomain, String targetDomain, ItemStack stack, VaultOperationStatus status, String message) {
        Instant now = Instant.now();
        return new VaultOperation(0L, requestId, account.getAccountId(), operationType, sourceDomain, targetDomain,
            VaultItemStackCodec.encode(stack), stack.stackSize, status, message, now, now);
    }

    private static ItemStack clean(ItemStack stack) {
        return stack == null || stack.stackSize <= 0 ? null : stack.copy();
    }

    private static VaultAccountType requireAccountType(VaultAccountType accountType) {
        if (accountType == null) {
            throw new VaultException("Vault account type is required");
        }
        return accountType;
    }

    private static ItemStack firstNonEmpty(List<ItemStack> stacks) {
        if (stacks != null) for (ItemStack stack : stacks) {
            if (stack != null && stack.getItem() != null && stack.stackSize > 0) return stack.copy();
        }
        return null;
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        if (left == null || left.stackSize <= 0) {
            return right == null || right.stackSize <= 0;
        }
        return right != null && right.stackSize > 0 && left.stackSize == right.stackSize
            && ItemStack.areItemStacksEqual(left, right);
    }

    private static String safeMessage(String message, int changes) {
        String normalized = message == null ? "" : message.trim();
        return normalized.isEmpty() ? "native container mutation prepared: " + changes + " vault slot(s)" : normalized;
    }

    private VaultDeliveryResult replayDelivery(VaultOperation operation) {
        if (operation.getStatus() != VaultOperationStatus.COMPLETED) {
            throw new VaultException("vault request is pending recovery: " + operation.getRequestId());
        }
        // A completed delivery is idempotent even when an old snapshot can no
        // longer be decoded by a later modpack version: no item must be moved
        // again, so the audit record itself is the authoritative result.
        return new VaultDeliveryResult(null, null, operation);
    }

    private VaultOperation replayPendingOperation(VaultOperation operation) {
        if (operation.getStatus() == VaultOperationStatus.COMPLETED) {
            return operation;
        }
        throw new VaultException("vault request is pending recovery: " + operation.getRequestId());
    }

    private VaultOperation requireOperation(String requestId) {
        Optional<VaultOperation> operation = repository.findOperationByRequestId(requestId);
        if (!operation.isPresent()) {
            throw new VaultException("vault operation does not exist: " + requestId);
        }
        return operation.get();
    }

    private VaultWithdrawal replayWithdrawal(VaultOperation operation) {
        if (operation.getStatus() != VaultOperationStatus.COMPLETED) {
            throw new VaultException("vault withdrawal is pending recovery: " + operation.getRequestId());
        }
        return new VaultWithdrawal(null, -1, VaultItemStackCodec.decode(operation.getItemSnapshot()), operation);
    }

    private void mergeIntoSlots(VaultAccount account, List<VaultSlot> slots, ItemStack source) {
        int remaining = source.stackSize;
        for (VaultSlot slot : slots) {
            ItemStack existing = slot.getStack();
            if (remaining <= 0 || existing == null || !canMerge(existing, source)) {
                continue;
            }
            int moved = Math.min(remaining, existing.getMaxStackSize() - existing.stackSize);
            if (moved <= 0) {
                continue;
            }
            existing.stackSize += moved;
            repository.saveSlot(account.getAccountId(), new VaultSlot(slot.getSlotIndex(), existing, slot.getVersion() + 1L));
            remaining -= moved;
        }
        for (VaultSlot slot : slots) {
            if (remaining <= 0) {
                break;
            }
            if (!slot.isEmpty()) {
                continue;
            }
            ItemStack placed = source.copy();
            placed.stackSize = Math.min(remaining, placed.getMaxStackSize());
            repository.saveSlot(account.getAccountId(), new VaultSlot(slot.getSlotIndex(), placed, slot.getVersion() + 1L));
            remaining -= placed.stackSize;
        }
        if (remaining != 0) {
            throw new VaultCapacityException("Base Vault capacity changed during delivery");
        }
    }

    private boolean canFit(List<VaultSlot> slots, ItemStack source) {
        int remaining = source.stackSize;
        for (VaultSlot slot : slots) {
            ItemStack existing = slot.getStack();
            if (existing == null) {
                remaining -= source.getMaxStackSize();
            } else if (canMerge(existing, source)) {
                remaining -= existing.getMaxStackSize() - existing.stackSize;
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private List<VaultSlot> fillSlots(VaultAccount account, List<VaultSlot> stored) {
        List<VaultSlot> result = new ArrayList<VaultSlot>(account.getSlotCount());
        for (int index = 0; index < account.getSlotCount(); index++) {
            result.add(new VaultSlot(index, null, 0L));
        }
        if (stored != null) {
            for (VaultSlot slot : stored) {
                if (slot != null && slot.getSlotIndex() >= 0 && slot.getSlotIndex() < result.size()) {
                    result.set(slot.getSlotIndex(), slot);
                }
            }
        }
        return result;
    }

    private ItemStack requireStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            throw new VaultException("a non-empty item stack is required");
        }
        return stack.copy();
    }

    private boolean matches(ItemStack stack, String registryName, int meta) {
        if (stack == null || stack.getItem() == null || stack.getItemDamage() != meta) {
            return false;
        }
        Object actual = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
        return registryName != null && registryName.equals(String.valueOf(actual));
    }

    private boolean canMerge(ItemStack existing, ItemStack incoming) {
        return existing.isItemEqual(incoming) && ItemStack.areItemStackTagsEqual(existing, incoming)
            && existing.stackSize < existing.getMaxStackSize();
    }

    private String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new VaultException(field + " is required");
        }
        return value.trim();
    }

    public static final class VaultView {
        private final VaultAccount account;
        private final List<VaultSlot> slots;
        private VaultView(VaultAccount account, List<VaultSlot> slots) {
            this.account = account;
            this.slots = Collections.unmodifiableList(new ArrayList<VaultSlot>(slots));
        }
        public VaultAccount getAccount() { return account; }
        public List<VaultSlot> getSlots() { return slots; }
    }

    public static final class VaultDeliveryResult {
        private final VaultAccount account;
        private final ItemStack deliveredStack;
        private final VaultOperation operation;
        private VaultDeliveryResult(VaultAccount account, ItemStack deliveredStack, VaultOperation operation) {
            this.account = account;
            this.deliveredStack = deliveredStack == null ? null : deliveredStack.copy();
            this.operation = operation;
        }
        public VaultAccount getAccount() { return account; }
        public ItemStack getDeliveredStack() { return deliveredStack == null ? null : deliveredStack.copy(); }
        public VaultOperation getOperation() { return operation; }
    }

    public static final class VaultSortResult {
        private final VaultView view;
        private final VaultOperation operation;
        private final boolean changed;
        private VaultSortResult(VaultView view, VaultOperation operation, boolean changed) {
            this.view = view;
            this.operation = operation;
            this.changed = changed;
        }
        public VaultView getView() { return view; }
        public VaultOperation getOperation() { return operation; }
        public boolean isChanged() { return changed; }
    }

    public static final class VaultWithdrawal {
        private final VaultAccount account;
        private final int slotIndex;
        private final ItemStack stack;
        private final VaultOperation operation;
        private VaultWithdrawal(VaultAccount account, int slotIndex, ItemStack stack, VaultOperation operation) {
            this.account = account;
            this.slotIndex = slotIndex;
            this.stack = stack == null ? null : stack.copy();
            this.operation = operation;
        }
        public VaultAccount getAccount() { return account; }
        public int getSlotIndex() { return slotIndex; }
        public ItemStack getStack() { return stack == null ? null : stack.copy(); }
        public VaultOperation getOperation() { return operation; }
    }
}

package com.jsirgalaxybase.modules.warehouse.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBay;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBayReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBayResult;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentAction;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentMutation;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;
import com.jsirgalaxybase.modules.warehouse.port.TerminalWarehouseBayRepository;
import com.jsirgalaxybase.modules.warehouse.port.WarehouseTransactionRunner;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Service-authoritative personal Cell Bay. It persists the Cell item, never an AE grid. */
public final class TerminalWarehouseBayService {
    private final TerminalWarehouseBayRepository repository;
    private final WarehouseTransactionRunner transactionRunner;
    private final String localServerId;
    private final TerminalWarehouseCellPolicy cellPolicy;
    private final TerminalWarehouseCellAccess cellAccess;

    public TerminalWarehouseBayService(TerminalWarehouseBayRepository repository, WarehouseTransactionRunner transactionRunner,
        String localServerId) {
        this(repository, transactionRunner, localServerId, new TerminalWarehouseCellPolicy() {
            @Override public boolean isValidCell(ItemStack stack) { return TerminalWarehouseCellInspector.isValidCell(stack); }
        });
    }

    public TerminalWarehouseBayService(TerminalWarehouseBayRepository repository, WarehouseTransactionRunner transactionRunner,
        String localServerId, TerminalWarehouseCellPolicy cellPolicy) {
        this(repository, transactionRunner, localServerId, cellPolicy, null);
    }

    public TerminalWarehouseBayService(TerminalWarehouseBayRepository repository, WarehouseTransactionRunner transactionRunner,
        String localServerId, TerminalWarehouseCellPolicy cellPolicy, TerminalWarehouseCellAccess cellAccess) {
        if (repository == null || transactionRunner == null) throw new IllegalArgumentException("Bay dependencies are required");
        if (cellPolicy == null) throw new IllegalArgumentException("Cell policy is required");
        this.repository = repository; this.transactionRunner = transactionRunner;
        this.localServerId = required(localServerId, "localServerId"); this.cellPolicy = cellPolicy;
        this.cellAccess = cellAccess;
    }

    public TerminalWarehouseBay view(String ownerPlayerRef) {
        String owner = required(ownerPlayerRef, "ownerPlayerRef");
        return repository.find(localServerId, owner).orElse(new TerminalWarehouseBay(localServerId, owner, null, 0L));
    }

    public List<TerminalWarehouseBayReceipt> listRecent(String ownerPlayerRef, int limit) {
        return repository.findRecent(localServerId, required(ownerPlayerRef, "ownerPlayerRef"), Math.max(1, Math.min(12, limit)));
    }

    public TerminalCellContentSnapshot viewContents(String ownerPlayerRef, int pageIndex, int pageSize) {
        TerminalWarehouseBay bay = view(ownerPlayerRef);
        if (!bay.hasCell()) return TerminalCellContentSnapshot.empty();
        return cellAccess().inspect(bay.getCell(), bay.getVersion(), Math.max(0, pageIndex),
            Math.max(1, Math.min(TerminalCellContentSnapshot.MAX_PAGE_SIZE, pageSize)));
    }

    public TerminalCellContentReceipt mutateContents(String requestId, String ownerPlayerRef, long expectedVersion,
        TerminalCellContentAction action, ItemStack target, long quantity) {
        final String request = required(requestId, "requestId");
        final String owner = required(ownerPlayerRef, "ownerPlayerRef");
        final TerminalCellContentAction requestedAction = action == null
            ? TerminalCellContentAction.INJECT_CURSOR : action;
        final ItemStack requestedStack = single(target);
        final long requestedQuantity = Math.max(1L, Math.min(Integer.MAX_VALUE, quantity));
        final String semantics = "terminal-cell|" + localServerId + "|" + owner + "|"
            + Math.max(0L, expectedVersion) + "|" + requestedAction.name() + "|"
            + fingerprint(requestedStack) + "|" + requestedQuantity;
        return transactionRunner.inTransaction(new Supplier<TerminalCellContentReceipt>() {
            @Override public TerminalCellContentReceipt get() {
                repository.lockOwner(localServerId, owner);
                Optional<TerminalWarehouseBayReceipt> replay = repository.findReceipt(request);
                if (replay.isPresent()) {
                    TerminalWarehouseBayReceipt prior = replay.get();
                    TerminalWarehouseBayReceipt result = semantics.equals(prior.getSemanticsKey()) ? prior
                        : new TerminalWarehouseBayReceipt(request, semantics, TerminalWarehouseBayResult.REQUEST_CONFLICT,
                            prior.getBefore(), prior.getAfter(), "requestId semantics conflict");
                    return new TerminalCellContentReceipt(result, null, 0L, false);
                }
                TerminalWarehouseBay current = view(owner);
                TerminalWarehouseBayResult result;
                TerminalCellContentMutation mutation = new TerminalCellContentMutation(current.getCell(), null, 0L);
                if (!current.hasCell()) result = TerminalWarehouseBayResult.NO_CELL;
                else if (expectedVersion != current.getVersion()) result = TerminalWarehouseBayResult.VERSION_CONFLICT;
                else if (requestedStack == null) result = TerminalWarehouseBayResult.ITEM_NOT_FOUND;
                else {
                    mutation = cellAccess().mutate(current.getCell(), requestedAction, requestedStack, requestedQuantity);
                    result = mutation.getMovedQuantity() > 0L ? TerminalWarehouseBayResult.SUCCESS
                        : requestedAction == TerminalCellContentAction.INJECT_CURSOR
                            ? TerminalWarehouseBayResult.CELL_FULL : TerminalWarehouseBayResult.ITEM_NOT_FOUND;
                }
                TerminalWarehouseBay next = current;
                if (result == TerminalWarehouseBayResult.SUCCESS) {
                    next = new TerminalWarehouseBay(localServerId, owner, mutation.getCell(), current.getVersion() + 1L);
                    repository.save(next);
                }
                TerminalWarehouseBayReceipt receipt = new TerminalWarehouseBayReceipt(request, semantics, result,
                    current, next, "cell content " + requestedAction.name() + " moved=" + mutation.getMovedQuantity());
                repository.saveReceipt(receipt, owner);
                return new TerminalCellContentReceipt(receipt, mutation.getMovedStack(),
                    mutation.getMovedQuantity(), result == TerminalWarehouseBayResult.SUCCESS);
            }
        });
    }

    /** Called only after vanilla Container click semantics have produced a candidate slot value. */
    public TerminalWarehouseBayReceipt commit(String requestId, String ownerPlayerRef, long expectedVersion,
        ItemStack beforeCell, ItemStack afterCell, String detail) {
        final String request = required(requestId, "requestId");
        final String owner = required(ownerPlayerRef, "ownerPlayerRef");
        final ItemStack before = single(beforeCell);
        final ItemStack after = single(afterCell);
        final String semantics = "terminal-bay|" + localServerId + "|" + owner + "|" + Math.max(0L, expectedVersion)
            + "|" + fingerprint(before) + "|" + fingerprint(after);
        return transactionRunner.inTransaction(new Supplier<TerminalWarehouseBayReceipt>() {
            @Override public TerminalWarehouseBayReceipt get() {
                repository.lockOwner(localServerId, owner);
                Optional<TerminalWarehouseBayReceipt> replay = repository.findReceipt(request);
                if (replay.isPresent()) {
                    TerminalWarehouseBayReceipt prior = replay.get();
                    return semantics.equals(prior.getSemanticsKey()) ? prior : new TerminalWarehouseBayReceipt(request, semantics,
                        TerminalWarehouseBayResult.REQUEST_CONFLICT, prior.getBefore(), prior.getAfter(), "requestId semantics conflict");
                }
                TerminalWarehouseBay current = view(owner);
                TerminalWarehouseBayResult result = expectedVersion != current.getVersion() ? TerminalWarehouseBayResult.VERSION_CONFLICT
                    : !validOrEmpty(after) ? TerminalWarehouseBayResult.INVALID_CELL : TerminalWarehouseBayResult.SUCCESS;
                TerminalWarehouseBay next = current;
                if (result == TerminalWarehouseBayResult.SUCCESS && !same(current.getCell(), after)) {
                    next = new TerminalWarehouseBay(localServerId, owner, after, current.getVersion() + 1L);
                    repository.save(next);
                }
                TerminalWarehouseBayReceipt receipt = new TerminalWarehouseBayReceipt(request, semantics, result, current, next,
                    safe(detail));
                repository.saveReceipt(receipt, owner);
                return receipt;
            }
        });
    }

    public String nextRequestId() { return "terminal-warehouse-bay-" + UUID.randomUUID().toString(); }

    private TerminalWarehouseCellAccess cellAccess() {
        return cellAccess == null ? new Ae2TerminalWarehouseCellAccess() : cellAccess;
    }

    private boolean validOrEmpty(ItemStack stack) { return stack == null || cellPolicy.isValidCell(stack); }
    private static ItemStack single(ItemStack stack) { if (stack == null || stack.stackSize <= 0) return null; ItemStack copy = stack.copy(); copy.stackSize = 1; return copy; }
    private static boolean same(ItemStack left, ItemStack right) { return left == null ? right == null : right != null && ItemStack.areItemStacksEqual(left, right); }
    private static String fingerprint(ItemStack stack) {
        if (stack == null) return "empty";
        Object key = Item.itemRegistry.getNameForObject(stack.getItem());
        return String.valueOf(key) + ":" + stack.getItemDamage() + ":" + String.valueOf(stack.getTagCompound());
    }
    private static String safe(String value) { String normalized = value == null ? "" : value.trim(); return normalized.length() > 480 ? normalized.substring(0, 480) : normalized; }
    private static String required(String value, String name) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required"); return value.trim(); }
}

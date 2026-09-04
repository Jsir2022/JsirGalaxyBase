package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft.BaseVaultSessionInventory;
import com.jsirgalaxybase.modules.itempolicy.application.ItemPolicyRuntime;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyScope;
import com.jsirgalaxybase.modules.warehouse.application.AssetActivityService;
import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseBayService;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentAction;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBay;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBayReceipt;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;

/**
 * One authoritative inventory session for the complete personal asset center.
 * Vault and Bay keep separate persistence contracts; this class only unifies
 * their native click surface and restores all touched inventories on failure.
 */
public final class TerminalAssetCenterContainer extends Container {
    public static final int VAULT_SLOT_COUNT = 27;
    public static final int BAY_SLOT_INDEX = 27;
    public static final int PLAYER_SLOT_START = 28;

    private final EntityPlayer player;
    private final String playerRef;
    private final BaseVaultService vaultService;
    private final TerminalWarehouseBayService bayService;
    private final BaseVaultSessionInventory vaultInventory;
    private final TerminalWarehouseBaySessionInventory bayInventory;
    private final AssetActivityService activityService;
    private TerminalAssetCenterTab activeTab;
    private int cellPage;
    private int cellPageSize = 20;
    private String cellFeedback = "";

    public TerminalAssetCenterContainer(EntityPlayer player, BaseVaultService vaultService,
        TerminalWarehouseBayService bayService, TerminalAssetCenterTab initialTab) {
        this(player, vaultService, bayService, null, initialTab);
    }

    public TerminalAssetCenterContainer(EntityPlayer player, BaseVaultService vaultService,
        TerminalWarehouseBayService bayService, AssetActivityService activityService,
        TerminalAssetCenterTab initialTab) {
        if (player == null || vaultService == null || bayService == null) {
            throw new IllegalArgumentException("player, Vault service and Bay service are required");
        }
        this.player = player;
        this.playerRef = player.getUniqueID().toString();
        this.vaultService = vaultService;
        this.bayService = bayService;
        this.activityService = activityService;
        this.activeTab = initialTab == null ? TerminalAssetCenterTab.STORAGE : initialTab;
        this.vaultInventory = new BaseVaultSessionInventory(vaultService.viewPersonalVault(playerRef));
        this.bayInventory = new TerminalWarehouseBaySessionInventory(bayService.view(playerRef));
        addVaultSlots();
        addSlotToContainer(new Slot(bayInventory, 0, 0, 0) {
            @Override public int getSlotStackLimit() { return 1; }
            @Override public boolean isItemValid(ItemStack stack) { return bayInventory.isItemValidForSlot(0, stack); }
        });
        addPlayerSlots(player.inventory);
        vaultInventory.openInventory();
    }

    public BaseVaultSessionInventory getVaultInventory() { return vaultInventory; }
    public TerminalWarehouseBaySessionInventory getBayInventory() { return bayInventory; }
    public TerminalAssetCenterTab getActiveTab() { return activeTab; }
    public void setActiveTab(TerminalAssetCenterTab tab) {
        activeTab = tab == null ? TerminalAssetCenterTab.STORAGE : tab;
        detectAndSendChanges();
    }

    @Override public boolean canInteractWith(EntityPlayer actor) {
        return actor != null && playerRef.equals(actor.getUniqueID().toString());
    }

    @Override public ItemStack slotClick(int slotId, int button, int mode, EntityPlayer actor) {
        if (!canInteractWith(actor) || mode == 3 || !slotAllowed(slotId)) return null;
        List<ItemStack> vaultBefore = vaultInventory.snapshotStacks();
        ItemStack bayBefore = bayInventory.getCell();
        List<ItemStack> playerBefore = snapshotPlayer(actor.inventory);
        ItemStack cursorBefore = copy(actor.inventory.getItemStack());
        String vaultRequestId = "asset-vault:" + playerRef + ":" + UUID.randomUUID();
        boolean vaultPrepared = false;
        try {
            if (activeTab == TerminalAssetCenterTab.STORAGE) {
                ItemStack audit = shouldPrepareVaultMutation(slotId, mode) ? resolveAuditStack(slotId, actor) : null;
                if (audit != null) {
                    vaultService.preparePersonalContainerMutation(vaultRequestId, playerRef, audit,
                        "asset center slot=" + slotId + ",button=" + button + ",mode=" + mode);
                    vaultPrepared = true;
                }
            }
            ItemStack result = super.slotClick(slotId, button, mode, actor);
            if (activeTab == TerminalAssetCenterTab.STORAGE) {
                commitVaultIfChanged(vaultRequestId, vaultPrepared, vaultBefore, slotId, button, mode);
                if (commitBayIfChanged(bayBefore, slotId, button, mode)) sendAssetSnapshot();
            }
            return result;
        } catch (RuntimeException exception) {
            vaultInventory.restore(vaultBefore);
            bayInventory.restore(bayBefore);
            restorePlayer(actor.inventory, playerBefore, cursorBefore);
            if (vaultPrepared) {
                try { vaultService.markOperationFailed(vaultRequestId,
                    "asset center mutation restored: " + exception.getMessage(), false); }
                catch (RuntimeException ignored) { }
            }
            GalaxyBase.LOG.warn("Personal asset center interaction restored for {}", playerRef, exception);
            detectAndSendChanges();
            return null;
        }
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer actor, int containerSlot) {
        if (!slotAllowed(containerSlot)) return null;
        Slot slot = containerSlot < 0 || containerSlot >= inventorySlots.size()
            ? null : (Slot) inventorySlots.get(containerSlot);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack source = slot.getStack();
        ItemStack original = source.copy();
        if (activeTab == TerminalAssetCenterTab.STORAGE) {
            if (containerSlot < VAULT_SLOT_COUNT) {
                if (!mergeItemStack(source, PLAYER_SLOT_START, inventorySlots.size(), true)) return null;
            } else if (containerSlot == BAY_SLOT_INDEX) {
                if (!mergeItemStack(source, PLAYER_SLOT_START, inventorySlots.size(), true)) return null;
            } else if (containerSlot >= PLAYER_SLOT_START) {
                if (!mergeItemStack(source, 0, VAULT_SLOT_COUNT, false)) return null;
            } else return null;
        } else return null;
        if (source.stackSize == 0) slot.putStack(null); else slot.onSlotChanged();
        if (source.stackSize == original.stackSize) return null;
        slot.onPickupFromSlot(actor, source);
        return original;
    }

    public void sortVault() {
        if (activeTab != TerminalAssetCenterTab.STORAGE) {
            throw new IllegalStateException("Base Vault sorting is only available on the storage page");
        }
        if (player.inventory.getItemStack() != null) throw new IllegalStateException("Place the cursor stack before sorting");
        BaseVaultService.VaultSortResult result = vaultService.sortPersonalVault(
            "asset-vault-sort:" + playerRef + ":" + UUID.randomUUID(), playerRef);
        vaultInventory.refreshExpectedSlots(result.getView());
        detectAndSendChanges();
    }

    public AssetActivitySnapshot createActivitySnapshot() {
        if (activityService == null) return AssetActivitySnapshot.empty();
        try {
            return activityService.findRecent(playerRef, AssetActivitySnapshot.MAX_ROWS);
        } catch (RuntimeException exception) {
            GalaxyBase.LOG.warn("Unable to read system asset activity for {}", playerRef, exception);
            return AssetActivitySnapshot.empty();
        }
    }

    public TerminalCellContentSnapshot createCellContentSnapshot() {
        try {
            return bayService.viewContents(playerRef, cellPage, cellPageSize)
                .withFeedback(cellFeedback);
        } catch (RuntimeException exception) {
            GalaxyBase.LOG.warn("Unable to read Cell contents for {}", playerRef, exception);
            return TerminalCellContentSnapshot.empty().withFeedback("RUNTIME_UNAVAILABLE");
        }
    }

    public void setCellViewport(int page, int pageSize) {
        cellPage = Math.max(0, Math.min(8, page));
        cellPageSize = Math.max(1, Math.min(TerminalCellContentSnapshot.MAX_PAGE_SIZE, pageSize));
    }

    public void handleCellContentAction(String requestId, long expectedVersion,
        TerminalCellContentAction action, ItemStack target) {
        if (activeTab != TerminalAssetCenterTab.STORAGE || !(player instanceof EntityPlayerMP)) return;
        TerminalCellContentAction requested = action == null ? TerminalCellContentAction.INJECT_CURSOR : action;
        ItemStack cursor = copy(player.inventory.getItemStack());
        ItemStack identity = requested == TerminalCellContentAction.INJECT_CURSOR ? cursor : copy(target);
        if (requested != TerminalCellContentAction.INJECT_CURSOR && cursor != null) {
            cellFeedback = "CURSOR_NOT_EMPTY";
            sendAssetSnapshot();
            return;
        }
        if (identity == null || identity.getItem() == null) {
            cellFeedback = requested == TerminalCellContentAction.INJECT_CURSOR ? "CURSOR_EMPTY" : "ITEM_NOT_FOUND";
            sendAssetSnapshot();
            return;
        }
        long quantity = requested == TerminalCellContentAction.EXTRACT_ONE ? 1L
            : requested == TerminalCellContentAction.EXTRACT_STACK ? Math.max(1, identity.getMaxStackSize())
                : Math.max(1, identity.stackSize);
        try {
            if (requested == TerminalCellContentAction.INJECT_CURSOR) {
                ItemPolicyRuntime.requireAllowed(ItemPolicyScope.BASE_VAULT, playerRef, "asset-cell-inject", identity);
            }
            TerminalCellContentReceipt receipt = bayService.mutateContents(requestId, playerRef, expectedVersion,
                requested, identity, quantity);
            cellFeedback = receipt.getBayReceipt().getResult().name();
            if (receipt.isSuccess() && receipt.isAppliedNow()) {
                if (requested == TerminalCellContentAction.INJECT_CURSOR) {
                    int left = Math.max(0, cursor.stackSize - (int) receipt.getMovedQuantity());
                    if (left <= 0) cursor = null; else cursor.stackSize = left;
                    player.inventory.setItemStack(cursor);
                } else {
                    ItemStack extracted = receipt.getMovedStack();
                    if (extracted != null) player.inventory.setItemStack(extracted);
                }
                TerminalWarehouseBay refreshed = receipt.getBayReceipt().getAfter();
                if (refreshed != null) bayInventory.refresh(refreshed);
                player.inventory.markDirty();
                ((EntityPlayerMP) player).playerNetServerHandler.sendPacket(
                    new S2FPacketSetSlot(-1, -1, player.inventory.getItemStack()));
                detectAndSendChanges();
            }
        } catch (RuntimeException exception) {
            cellFeedback = "RUNTIME_UNAVAILABLE";
            GalaxyBase.LOG.warn("Cell content action failed for {}", playerRef, exception);
        }
        sendAssetSnapshot();
    }

    @Override public void onContainerClosed(EntityPlayer actor) {
        super.onContainerClosed(actor);
        vaultInventory.closeInventory();
    }

    private boolean slotAllowed(int slotId) {
        if (slotId < 0) return activeTab.exposesInventory();
        if (slotId < VAULT_SLOT_COUNT) return activeTab == TerminalAssetCenterTab.STORAGE;
        if (slotId == BAY_SLOT_INDEX) return activeTab == TerminalAssetCenterTab.STORAGE;
        return slotId >= PLAYER_SLOT_START && activeTab.exposesInventory();
    }

    private void commitVaultIfChanged(String requestId, boolean prepared, List<ItemStack> before,
        int slotId, int button, int mode) {
        List<ItemStack> after = vaultInventory.snapshotStacks();
        if (sameSnapshot(before, after)) {
            if (prepared) vaultService.markOperationFailed(requestId, "asset center click did not change Vault state", false);
            return;
        }
        enforceVaultAdmission(before, after);
        if (!prepared) {
            ItemStack audit = firstNonEmpty(before, after);
            vaultService.preparePersonalContainerMutation(requestId, playerRef, audit,
                "asset center late audit slot=" + slotId);
        }
        vaultService.commitPersonalContainerMutation(requestId, playerRef,
            vaultInventory.getOpeningSlots(), after, "slot=" + slotId + ",button=" + button + ",mode=" + mode);
        vaultInventory.refreshExpectedSlots(vaultService.viewPersonalVault(playerRef));
    }

    private boolean commitBayIfChanged(ItemStack before, int slotId, int button, int mode) {
        ItemStack after = bayInventory.getCell();
        if (sameStack(before, after)) return false;
        TerminalWarehouseBayReceipt receipt = bayService.commit(bayService.nextRequestId(), playerRef,
            bayInventory.getOpeningVersion(), before, after,
            "asset center slot=" + slotId + ",button=" + button + ",mode=" + mode);
        if (!receipt.isSuccess()) throw new IllegalStateException("Bay mutation rejected: " + receipt.getResult());
        TerminalWarehouseBay refreshed = receipt.getAfter();
        bayInventory.refresh(refreshed);
        cellPage = 0;
        cellFeedback = receipt.getResult().name();
        return true;
    }

    private void sendAssetSnapshot() {
        if (player instanceof EntityPlayerMP) TerminalNetwork.CHANNEL.sendTo(
            new TerminalAssetCenterSnapshotMessage(createActivitySnapshot(), createCellContentSnapshot()),
            (EntityPlayerMP) player);
    }

    private void enforceVaultAdmission(List<ItemStack> before, List<ItemStack> after) {
        for (int index = 0; index < after.size(); index++) {
            ItemStack next = after.get(index); ItemStack previous = before.get(index);
            if (next == null || next.stackSize <= 0) continue;
            int beforeQuantity = previous == null ? 0 : previous.stackSize;
            if (previous == null || !previous.isItemEqual(next)
                || !ItemStack.areItemStackTagsEqual(previous, next) || next.stackSize > beforeQuantity) {
                ItemPolicyRuntime.requireAllowed(ItemPolicyScope.BASE_VAULT, playerRef, "asset-center", next);
            }
        }
    }

    private ItemStack resolveAuditStack(int slotId, EntityPlayer actor) {
        if (slotId >= 0 && slotId < inventorySlots.size()) {
            Slot slot = (Slot) inventorySlots.get(slotId);
            if (slot != null && slot.getHasStack()) return copy(slot.getStack());
        }
        if (actor.inventory.getItemStack() != null) return copy(actor.inventory.getItemStack());
        return firstNonEmpty(vaultInventory.snapshotStacks(), snapshotPlayer(actor.inventory));
    }

    private static boolean shouldPrepareVaultMutation(int slotId, int mode) {
        return slotId >= 0 && slotId < VAULT_SLOT_COUNT
            || mode == 1 && slotId >= PLAYER_SLOT_START;
    }

    private void addVaultSlots() {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlotToContainer(new Slot(vaultInventory, column + row * 9, 0, 0));
    }

    private void addPlayerSlots(InventoryPlayer inventory) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlotToContainer(new Slot(inventory, column + row * 9 + 9, 0, 0));
        for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(inventory, column, 0, 0));
    }

    private static List<ItemStack> snapshotPlayer(InventoryPlayer inventory) {
        List<ItemStack> result = new ArrayList<ItemStack>(inventory.mainInventory.length);
        for (ItemStack stack : inventory.mainInventory) result.add(copy(stack));
        return result;
    }

    private static void restorePlayer(InventoryPlayer inventory, List<ItemStack> snapshot, ItemStack cursor) {
        for (int index = 0; index < inventory.mainInventory.length; index++)
            inventory.mainInventory[index] = index < snapshot.size() ? copy(snapshot.get(index)) : null;
        inventory.setItemStack(copy(cursor)); inventory.markDirty();
    }

    private static ItemStack firstNonEmpty(List<ItemStack> first, List<ItemStack> second) {
        if (first != null) for (ItemStack stack : first) if (stack != null && stack.stackSize > 0) return copy(stack);
        if (second != null) for (ItemStack stack : second) if (stack != null && stack.stackSize > 0) return copy(stack);
        return null;
    }

    private static boolean sameSnapshot(List<ItemStack> before, List<ItemStack> after) {
        if (before.size() != after.size()) return false;
        for (int index = 0; index < before.size(); index++) if (!sameStack(before.get(index), after.get(index))) return false;
        return true;
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        return left == null ? right == null : right != null && left.stackSize == right.stackSize
            && ItemStack.areItemStacksEqual(left, right);
    }

    private static ItemStack copy(ItemStack stack) { return stack == null ? null : stack.copy(); }
}

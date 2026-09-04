package com.jsirgalaxybase.modules.warehouse.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEItemStack;

import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentAction;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentEntry;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentMutation;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Direct access to one detached AE2 Cell. It creates no grid node, channel or power source. */
public final class Ae2TerminalWarehouseCellAccess implements TerminalWarehouseCellAccess {
    private static final ISaveProvider NO_OP_SAVE_PROVIDER = new ISaveProvider() {
        @Override public void saveChanges(IMEInventory inventory) { }
    };
    private final BaseActionSource actionSource = new BaseActionSource();

    @Override public TerminalCellContentSnapshot inspect(ItemStack cell, long bayVersion, int pageIndex, int pageSize) {
        IMEInventoryHandler<IAEItemStack> handler = handler(cell == null ? null : cell.copy());
        ICellInventory inventory = cellInventory(handler);
        if (handler == null || inventory == null) return TerminalCellContentSnapshot.empty();
        int boundedSize = Math.max(1, Math.min(TerminalCellContentSnapshot.MAX_PAGE_SIZE, pageSize));
        List<IAEItemStack> available = new ArrayList<IAEItemStack>();
        IItemList<IAEItemStack> list = AEApi.instance().storage().createItemList();
        handler.getAvailableItems(list);
        for (IAEItemStack value : list) if (value != null && value.getStackSize() > 0L) available.add(value.copy());
        Collections.sort(available, new Comparator<IAEItemStack>() {
            @Override public int compare(IAEItemStack left, IAEItemStack right) {
                return stableKey(left).compareTo(stableKey(right));
            }
        });
        int pages = Math.max(1, (available.size() + boundedSize - 1) / boundedSize);
        int page = Math.max(0, Math.min(pageIndex, pages - 1));
        int start = page * boundedSize; int end = Math.min(available.size(), start + boundedSize);
        List<TerminalCellContentEntry> entries = new ArrayList<TerminalCellContentEntry>();
        for (int index = start; index < end; index++) {
            IAEItemStack value = available.get(index);
            ItemStack display = value.getItemStack();
            if (display != null && display.getItem() != null)
                entries.add(new TerminalCellContentEntry(display, value.getStackSize()));
        }
        return new TerminalCellContentSnapshot(true, bayVersion, inventory.getTotalBytes(), inventory.getUsedBytes(),
            inventory.getStoredItemCount(), inventory.getStoredItemTypes(), inventory.getTotalItemTypes(), page,
            available.size(), boundedSize, "", entries);
    }

    @Override public TerminalCellContentMutation mutate(ItemStack cell, TerminalCellContentAction action,
        ItemStack target, long quantity) {
        ItemStack mutableCell = cell == null ? null : cell.copy();
        IMEInventoryHandler<IAEItemStack> handler = handler(mutableCell);
        if (handler == null || target == null || target.getItem() == null || quantity <= 0L)
            return new TerminalCellContentMutation(mutableCell, null, 0L);
        long bounded = Math.min(Integer.MAX_VALUE, quantity);
        IAEItemStack request = AEItemStack.create(target.copy());
        if (request == null) return new TerminalCellContentMutation(mutableCell, null, 0L);
        request.setStackSize(bounded);
        if (action == TerminalCellContentAction.INJECT_CURSOR) {
            IAEItemStack simulatedRemainder = handler.injectItems(request.copy(), Actionable.SIMULATE, actionSource);
            long remainder = simulatedRemainder == null ? 0L : Math.max(0L, simulatedRemainder.getStackSize());
            long accepted = Math.max(0L, bounded - remainder);
            if (accepted <= 0L) return new TerminalCellContentMutation(mutableCell, null, 0L);
            IAEItemStack actual = request.copy(); actual.setStackSize(accepted);
            IAEItemStack actualRemainder = handler.injectItems(actual, Actionable.MODULATE, actionSource);
            long actualLeft = actualRemainder == null ? 0L : Math.max(0L, actualRemainder.getStackSize());
            long moved = Math.max(0L, accepted - actualLeft);
            return new TerminalCellContentMutation(mutableCell, stack(target, moved), moved);
        }
        IAEItemStack extracted = handler.extractItems(request, Actionable.MODULATE, actionSource);
        long moved = extracted == null ? 0L : Math.max(0L, extracted.getStackSize());
        return new TerminalCellContentMutation(mutableCell,
            extracted == null ? null : stack(extracted.getItemStack(), moved), moved);
    }

    @SuppressWarnings("unchecked")
    private IMEInventoryHandler<IAEItemStack> handler(ItemStack cell) {
        if (!TerminalWarehouseCellInspector.isValidCell(cell)) return null;
        try {
            return (IMEInventoryHandler<IAEItemStack>) AEApi.instance().registries().cell()
                .getCellInventory(cell, NO_OP_SAVE_PROVIDER, StorageChannel.ITEMS);
        } catch (RuntimeException ignored) { return null; }
    }

    private ICellInventory cellInventory(IMEInventoryHandler<IAEItemStack> handler) {
        return handler instanceof ICellInventoryHandler ? ((ICellInventoryHandler) handler).getCellInv() : null;
    }

    private static ItemStack stack(ItemStack template, long quantity) {
        if (template == null || quantity <= 0L) return null;
        ItemStack result = template.copy();
        result.stackSize = (int) Math.min(Integer.MAX_VALUE, quantity);
        return result;
    }

    private static String stableKey(IAEItemStack value) {
        ItemStack stack = value == null ? null : value.getItemStack();
        if (stack == null || stack.getItem() == null) return "";
        Object id = Item.itemRegistry.getNameForObject(stack.getItem());
        return String.valueOf(id) + ":" + stack.getItemDamage() + ":" + String.valueOf(stack.getTagCompound());
    }
}

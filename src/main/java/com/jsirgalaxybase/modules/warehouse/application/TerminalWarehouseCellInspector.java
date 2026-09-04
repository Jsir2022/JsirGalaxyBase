package com.jsirgalaxybase.modules.warehouse.application;

import appeng.api.AEApi;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import net.minecraft.item.ItemStack;

/** Read-only AE2 Cell facts. No grid node, power source or channel is created. */
public final class TerminalWarehouseCellInspector {
    private static final ISaveProvider NO_OP_SAVE_PROVIDER = new ISaveProvider() {
        @Override public void saveChanges(IMEInventory inventory) { }
    };
    private TerminalWarehouseCellInspector() { }

    public static boolean isValidCell(ItemStack stack) {
        return stack != null && stack.stackSize == 1 && stack.getItem() != null
            && AEApi.instance().registries().cell().isCellHandled(stack);
    }

    public static CellFacts inspect(ItemStack stack) {
        if (!isValidCell(stack)) return CellFacts.empty();
        try {
            IMEInventoryHandler handler = AEApi.instance().registries().cell().getCellInventory(stack.copy(),
                NO_OP_SAVE_PROVIDER, StorageChannel.ITEMS);
            ICellInventory cell = handler instanceof appeng.api.storage.ICellInventoryHandler
                ? ((appeng.api.storage.ICellInventoryHandler) handler).getCellInv() : null;
            return cell == null ? CellFacts.empty() : new CellFacts(true, cell.getTotalBytes(), cell.getUsedBytes(),
                cell.getFreeBytes(), cell.getStoredItemCount(), cell.getStoredItemTypes(), cell.getTotalItemTypes());
        } catch (RuntimeException ignored) {
            return CellFacts.empty();
        }
    }

    public static final class CellFacts {
        private final boolean valid;
        private final long totalBytes;
        private final long usedBytes;
        private final long freeBytes;
        private final long storedItems;
        private final long storedTypes;
        private final long totalTypes;
        private CellFacts(boolean valid, long totalBytes, long usedBytes, long freeBytes, long storedItems,
            long storedTypes, long totalTypes) {
            this.valid = valid; this.totalBytes = Math.max(0L, totalBytes); this.usedBytes = Math.max(0L, usedBytes);
            this.freeBytes = Math.max(0L, freeBytes); this.storedItems = Math.max(0L, storedItems);
            this.storedTypes = Math.max(0L, storedTypes); this.totalTypes = Math.max(0L, totalTypes);
        }
        public static CellFacts empty() { return new CellFacts(false, 0L, 0L, 0L, 0L, 0L, 0L); }
        public boolean isValid() { return valid; }
        public long getTotalBytes() { return totalBytes; }
        public long getUsedBytes() { return usedBytes; }
        public long getFreeBytes() { return freeBytes; }
        public long getStoredItems() { return storedItems; }
        public long getStoredTypes() { return storedTypes; }
        public long getTotalTypes() { return totalTypes; }
    }
}

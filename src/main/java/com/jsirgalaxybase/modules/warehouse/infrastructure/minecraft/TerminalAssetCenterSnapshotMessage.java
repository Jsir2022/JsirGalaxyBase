package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.modules.warehouse.client.GuiTerminalAssetCenter;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityRow;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityType;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentEntry;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Bounded actor-scoped system asset activity for the open asset center. */
public final class TerminalAssetCenterSnapshotMessage implements IMessage {
    private static final int VERSION = 0xA3;
    private static final int ACTIVITY_ONLY_VERSION = 0xA2;
    private AssetActivitySnapshot snapshot = AssetActivitySnapshot.empty();
    private TerminalCellContentSnapshot cellSnapshot = TerminalCellContentSnapshot.empty();

    public TerminalAssetCenterSnapshotMessage() { }
    public TerminalAssetCenterSnapshotMessage(AssetActivitySnapshot snapshot) {
        this(snapshot, TerminalCellContentSnapshot.empty());
    }
    public TerminalAssetCenterSnapshotMessage(AssetActivitySnapshot snapshot,
        TerminalCellContentSnapshot cellSnapshot) {
        this.snapshot = snapshot == null ? AssetActivitySnapshot.empty() : snapshot;
        this.cellSnapshot = cellSnapshot == null ? TerminalCellContentSnapshot.empty() : cellSnapshot;
    }
    public AssetActivitySnapshot getSnapshot() { return snapshot; }
    public TerminalCellContentSnapshot getCellSnapshot() { return cellSnapshot; }

    @Override public void fromBytes(ByteBuf buffer) {
        int version = buffer.readUnsignedByte();
        if (version == ACTIVITY_ONLY_VERSION) {
            readActivity(buffer);
            return;
        }
        if (version != VERSION) {
            readLegacy(version, buffer);
            return;
        }
        readActivity(buffer);
        readCell(buffer);
    }

    private void readActivity(ByteBuf buffer) {
        int count = Math.max(0, Math.min(AssetActivitySnapshot.MAX_ROWS, buffer.readUnsignedByte()));
        List<AssetActivityRow> rows = new ArrayList<AssetActivityRow>(count);
        for (int index = 0; index < count; index++) {
            String key = safe(ByteBufUtils.readUTF8String(buffer), 96);
            int typeCode = buffer.readUnsignedByte();
            AssetActivityType[] values = AssetActivityType.values();
            AssetActivityType type = typeCode < values.length ? values[typeCode] : AssetActivityType.RECOVERY_REQUIRED;
            ItemStack stack = ByteBufUtils.readItemStack(buffer);
            long quantity = buffer.readLong(); long amount = buffer.readLong(); long fee = buffer.readLong();
            String reference = safe(ByteBufUtils.readUTF8String(buffer), 64);
            String status = safe(ByteBufUtils.readUTF8String(buffer), 48);
            rows.add(new AssetActivityRow(key, type, stack, quantity, amount, fee, reference, status,
                Instant.ofEpochMilli(buffer.readLong())));
        }
        snapshot = new AssetActivitySnapshot(rows);
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeByte(VERSION);
        writeActivity(buffer);
        writeCell(buffer);
    }

    private void writeActivity(ByteBuf buffer) {
        List<AssetActivityRow> rows = snapshot.getRows();
        buffer.writeByte(rows.size());
        for (AssetActivityRow row : rows) {
            ByteBufUtils.writeUTF8String(buffer, safe(row.getStableKey(), 96));
            buffer.writeByte(row.getType().ordinal());
            ByteBufUtils.writeItemStack(buffer, row.getItemStack());
            buffer.writeLong(row.getQuantity()); buffer.writeLong(row.getAmount()); buffer.writeLong(row.getFee());
            ByteBufUtils.writeUTF8String(buffer, safe(row.getReference(), 64));
            ByteBufUtils.writeUTF8String(buffer, safe(row.getStatus(), 48));
            buffer.writeLong(row.getCreatedAt().toEpochMilli());
        }
    }

    private void readCell(ByteBuf buffer) {
        boolean present = buffer.readBoolean();
        long version = buffer.readLong();
        long totalBytes = buffer.readLong(); long usedBytes = buffer.readLong();
        long storedItems = buffer.readLong(); long storedTypes = buffer.readLong(); long totalTypes = buffer.readLong();
        int page = buffer.readUnsignedByte(); int totalRows = buffer.readUnsignedByte();
        int pageSize = Math.max(1, Math.min(TerminalCellContentSnapshot.MAX_PAGE_SIZE, buffer.readUnsignedByte()));
        String feedback = safe(ByteBufUtils.readUTF8String(buffer), 64);
        int count = Math.max(0, Math.min(pageSize, buffer.readUnsignedByte()));
        List<TerminalCellContentEntry> entries = new ArrayList<TerminalCellContentEntry>(count);
        for (int index = 0; index < count; index++) {
            ItemStack stack = ByteBufUtils.readItemStack(buffer);
            long quantity = Math.max(0L, buffer.readLong());
            if (stack != null && stack.getItem() != null) entries.add(new TerminalCellContentEntry(stack, quantity));
        }
        cellSnapshot = new TerminalCellContentSnapshot(present, version, totalBytes, usedBytes, storedItems,
            storedTypes, totalTypes, page, totalRows, pageSize, feedback, entries);
    }

    private void writeCell(ByteBuf buffer) {
        buffer.writeBoolean(cellSnapshot.isCellPresent());
        buffer.writeLong(cellSnapshot.getBayVersion());
        buffer.writeLong(cellSnapshot.getTotalBytes()); buffer.writeLong(cellSnapshot.getUsedBytes());
        buffer.writeLong(cellSnapshot.getStoredItems()); buffer.writeLong(cellSnapshot.getStoredTypes());
        buffer.writeLong(cellSnapshot.getTotalTypes());
        buffer.writeByte(cellSnapshot.getPageIndex()); buffer.writeByte(cellSnapshot.getTotalRows());
        buffer.writeByte(cellSnapshot.getPageSize());
        ByteBufUtils.writeUTF8String(buffer, safe(cellSnapshot.getFeedback(), 64));
        List<TerminalCellContentEntry> entries = cellSnapshot.getEntries();
        buffer.writeByte(entries.size());
        for (TerminalCellContentEntry entry : entries) {
            ByteBufUtils.writeItemStack(buffer, entry.getStack());
            buffer.writeLong(entry.getQuantity());
        }
    }

    private void readLegacy(int countByte, ByteBuf buffer) {
        int count = Math.max(0, Math.min(8, countByte));
        List<AssetActivityRow> rows = new ArrayList<AssetActivityRow>();
        for (int index = 0; index < count; index++) {
            String line = safe(ByteBufUtils.readUTF8String(buffer), 180);
            rows.add(new AssetActivityRow("legacy:" + index, AssetActivityType.RECOVERY_REQUIRED, null,
                0L, 0L, 0L, line, "LEGACY", Instant.EPOCH));
        }
        snapshot = new AssetActivitySnapshot(rows);
    }

    public static final class Handler implements IMessageHandler<TerminalAssetCenterSnapshotMessage, IMessage> {
        @Override public IMessage onMessage(TerminalAssetCenterSnapshotMessage message, MessageContext context) {
            if (Minecraft.getMinecraft().currentScreen instanceof GuiTerminalAssetCenter) {
                ((GuiTerminalAssetCenter) Minecraft.getMinecraft().currentScreen)
                    .applySnapshot(message.snapshot, message.cellSnapshot);
            }
            return null;
        }
    }

    private static String safe(String value, int limit) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() > limit ? normalized.substring(0, limit) : normalized;
    }
}

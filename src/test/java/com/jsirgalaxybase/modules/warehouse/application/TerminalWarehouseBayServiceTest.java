package com.jsirgalaxybase.modules.warehouse.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBay;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBayResult;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentAction;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentMutation;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;
import com.jsirgalaxybase.modules.warehouse.infrastructure.DirectWarehouseTransactionRunner;
import com.jsirgalaxybase.modules.warehouse.infrastructure.InMemoryTerminalWarehouseBayRepository;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class TerminalWarehouseBayServiceTest {
    @Test public void emptyBayIsPersonalAndNoopCommitIsAuditedIdempotently() {
        TerminalWarehouseBayService service = service();
        assertFalse(service.view("owner").hasCell());
        assertEquals(0L, service.view("owner").getVersion());
        assertEquals(TerminalWarehouseBayResult.SUCCESS, service.commit("same", "owner", 0L, null, null, "noop").getResult());
        assertEquals(TerminalWarehouseBayResult.SUCCESS, service.commit("same", "owner", 0L, null, null, "noop").getResult());
        assertEquals(1, service.listRecent("owner", 4).size());
        assertTrue(service.listRecent("other", 4).isEmpty());
    }
    @Test public void staleVersionCannotReplaceAnExistingBay() {
        InMemoryTerminalWarehouseBayRepository repository = new InMemoryTerminalWarehouseBayRepository();
        ItemStack placeholder = new ItemStack(new Item());
        repository.save(new TerminalWarehouseBay("lobby", "owner", placeholder, 4L));
        TerminalWarehouseBayService service = service(repository);
        assertEquals(TerminalWarehouseBayResult.VERSION_CONFLICT, service.commit("stale", "owner", 0L,
            placeholder, null, "stale remove").getResult());
        assertTrue(service.view("owner").hasCell());
        assertEquals(4L, service.view("owner").getVersion());
    }
    @Test public void nonAeItemIsRejectedBeforeItCanEnterTheBay() {
        TerminalWarehouseBayService service = service();
        assertEquals(TerminalWarehouseBayResult.INVALID_CELL, service.commit("invalid", "owner", 0L, null,
            new ItemStack(new Item()), "invalid item").getResult());
        assertFalse(service.view("owner").hasCell());
    }
    @Test public void acceptedCellIsSingleStackVersionedAndRequestIdsCannotChangeMeaning() {
        TerminalWarehouseBayService service = acceptingService();
        ItemStack stack = new ItemStack(new Item(), 9);
        assertEquals(TerminalWarehouseBayResult.SUCCESS, service.commit("insert", "owner", 0L, null, stack, "insert").getResult());
        assertEquals(1, service.view("owner").getCell().stackSize);
        assertEquals(1L, service.view("owner").getVersion());
        assertEquals(TerminalWarehouseBayResult.SUCCESS, service.commit("insert", "owner", 0L, null, stack, "insert").getResult());
        assertEquals(TerminalWarehouseBayResult.REQUEST_CONFLICT, service.commit("insert", "owner", 1L,
            service.view("owner").getCell(), null, "remove").getResult());
    }
    @Test public void cellContentsAreVersionedAndReplayDoesNotApplyCursorMutationTwice() {
        InMemoryTerminalWarehouseBayRepository repository = new InMemoryTerminalWarehouseBayRepository();
        TerminalWarehouseBayService service = contentService(repository);
        ItemStack cell = new ItemStack(new Item());
        assertEquals(TerminalWarehouseBayResult.SUCCESS,
            service.commit("install", "owner", 0L, null, cell, "install").getResult());
        ItemStack input = new ItemStack(new Item(), 12);
        TerminalCellContentReceipt first = service.mutateContents("inject", "owner", 1L,
            TerminalCellContentAction.INJECT_CURSOR, input, 12L);
        assertTrue(first.isSuccess()); assertTrue(first.isAppliedNow());
        assertEquals(12L, first.getMovedQuantity()); assertEquals(2L, service.view("owner").getVersion());
        TerminalCellContentReceipt replay = service.mutateContents("inject", "owner", 1L,
            TerminalCellContentAction.INJECT_CURSOR, input, 12L);
        assertTrue(replay.isSuccess()); assertFalse(replay.isAppliedNow());
        assertEquals(2L, service.view("owner").getVersion());
    }

    @Test public void contentMutationRejectsMissingCellAndStaleVersion() {
        InMemoryTerminalWarehouseBayRepository repository = new InMemoryTerminalWarehouseBayRepository();
        TerminalWarehouseBayService service = contentService(repository);
        ItemStack item = new ItemStack(new Item());
        assertEquals(TerminalWarehouseBayResult.NO_CELL, service.mutateContents("missing", "owner", 0L,
            TerminalCellContentAction.EXTRACT_ONE, item, 1L).getBayReceipt().getResult());
        service.commit("install", "owner", 0L, null, new ItemStack(new Item()), "install");
        assertEquals(TerminalWarehouseBayResult.VERSION_CONFLICT,
            service.mutateContents("stale-content", "owner", 0L, TerminalCellContentAction.EXTRACT_ONE,
                item, 1L).getBayReceipt().getResult());
    }
    private static TerminalWarehouseBayService service() {
        return service(new InMemoryTerminalWarehouseBayRepository());
    }
    private static TerminalWarehouseBayService service(InMemoryTerminalWarehouseBayRepository repository) {
        return new TerminalWarehouseBayService(repository, new DirectWarehouseTransactionRunner(), "lobby", new TerminalWarehouseCellPolicy() {
            @Override public boolean isValidCell(ItemStack stack) { return false; }
        });
    }
    private static TerminalWarehouseBayService acceptingService() {
        return new TerminalWarehouseBayService(new InMemoryTerminalWarehouseBayRepository(), new DirectWarehouseTransactionRunner(), "lobby", new TerminalWarehouseCellPolicy() {
            @Override public boolean isValidCell(ItemStack stack) { return stack != null; }
        });
    }

    private static TerminalWarehouseBayService contentService(InMemoryTerminalWarehouseBayRepository repository) {
        return new TerminalWarehouseBayService(repository, new DirectWarehouseTransactionRunner(), "lobby",
            new TerminalWarehouseCellPolicy() {
                @Override public boolean isValidCell(ItemStack stack) { return stack != null; }
            }, new TerminalWarehouseCellAccess() {
                @Override public TerminalCellContentSnapshot inspect(ItemStack cell, long version, int page, int size) {
                    int stored = cell != null && cell.hasTagCompound() ? cell.getTagCompound().getInteger("stored") : 0;
                    return new TerminalCellContentSnapshot(true, version, 1024L, stored, stored, stored > 0 ? 1 : 0,
                        63L, 0, stored > 0 ? 1 : 0, size, "", java.util.Collections.emptyList());
                }
                @Override public TerminalCellContentMutation mutate(ItemStack cell, TerminalCellContentAction action,
                    ItemStack target, long quantity) {
                    ItemStack next = cell.copy(); NBTTagCompound tag = next.hasTagCompound()
                        ? next.getTagCompound() : new NBTTagCompound();
                    int stored = tag.getInteger("stored");
                    int moved = (int) Math.min(quantity, action == TerminalCellContentAction.INJECT_CURSOR
                        ? 64 - stored : stored);
                    tag.setInteger("stored", action == TerminalCellContentAction.INJECT_CURSOR
                        ? stored + moved : stored - moved);
                    next.setTagCompound(tag);
                    ItemStack result = target.copy(); result.stackSize = moved;
                    return new TerminalCellContentMutation(next, moved <= 0 ? null : result, moved);
                }
            });
    }
}

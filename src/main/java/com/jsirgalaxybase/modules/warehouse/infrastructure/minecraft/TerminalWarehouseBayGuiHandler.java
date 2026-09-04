package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.warehouse.WarehouseModule;
import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseBayService;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

/** Forge bridge for the terminal-owned Cell Bay; no world position is accepted. */
public final class TerminalWarehouseBayGuiHandler implements IGuiHandler {
    public static final int TERMINAL_WAREHOUSE_BAY_GUI_ID = 42;
    public static boolean open(EntityPlayerMP player) {
        if (player == null || GalaxyBase.instance == null || GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) return false;
        WarehouseModule module = GalaxyBase.proxy.getModuleManager().findModule(WarehouseModule.class);
        if (module == null || module.getTerminalBayService() == null) return false;
        player.openGui(GalaxyBase.instance, TERMINAL_WAREHOUSE_BAY_GUI_ID, player.worldObj, 0, 0, 0); return true;
    }
    @Override public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TerminalWarehouseBayService service = resolve(); return id == TERMINAL_WAREHOUSE_BAY_GUI_ID && player != null && service != null ? new TerminalWarehouseBayContainer(player, service) : null;
    }
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return id != TERMINAL_WAREHOUSE_BAY_GUI_ID || player == null || GalaxyBase.proxy == null ? null : GalaxyBase.proxy.createTerminalWarehouseBayClientGui(player.inventory);
    }
    private static TerminalWarehouseBayService resolve() { if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) return null; WarehouseModule module = GalaxyBase.proxy.getModuleManager().findModule(WarehouseModule.class); return module == null ? null : module.getTerminalBayService(); }
}

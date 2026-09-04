package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft.BaseVaultContainer;
import com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft.BaseVaultGuiHandler;
import com.jsirgalaxybase.modules.warehouse.WarehouseModule;
import com.jsirgalaxybase.modules.warehouse.application.AssetActivityService;
import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseBayService;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

/** Single Forge GUI bridge for the unified asset center and both legacy IDs. */
public final class TerminalAssetCenterGuiHandler implements IGuiHandler {
    public static final int ASSET_CENTER_GUI_ID = 43;

    public static boolean open(EntityPlayerMP player, TerminalAssetCenterTab tab) {
        if (player == null || GalaxyBase.instance == null || resolveVault() == null || resolveBay() == null) return false;
        TerminalAssetCenterTab initial = tab == null ? TerminalAssetCenterTab.STORAGE : tab;
        player.openGui(GalaxyBase.instance, ASSET_CENTER_GUI_ID, player.worldObj, initial.getCode(), 0, 0);
        return true;
    }

    @Override public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (player == null) return null;
        if (id == BaseVaultGuiHandler.PERSONAL_VAULT_GUI_ID) {
            BaseVaultService vault = resolveVault();
            return vault == null ? null : new BaseVaultContainer(player, vault);
        }
        if (id == TerminalWarehouseBayGuiHandler.TERMINAL_WAREHOUSE_BAY_GUI_ID) {
            TerminalWarehouseBayService bay = resolveBay();
            return bay == null ? null : new TerminalWarehouseBayContainer(player, bay);
        }
        if (id != ASSET_CENTER_GUI_ID) return null;
        BaseVaultService vault = resolveVault(); TerminalWarehouseBayService bay = resolveBay();
        return vault == null || bay == null ? null
            : new TerminalAssetCenterContainer(player, vault, bay, resolveActivity(), TerminalAssetCenterTab.fromCode(x));
    }

    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (player == null || GalaxyBase.proxy == null) return null;
        if (id == BaseVaultGuiHandler.PERSONAL_VAULT_GUI_ID) return GalaxyBase.proxy.createBaseVaultClientGui(player.inventory);
        if (id == TerminalWarehouseBayGuiHandler.TERMINAL_WAREHOUSE_BAY_GUI_ID) return GalaxyBase.proxy.createTerminalWarehouseBayClientGui(player.inventory);
        return id == ASSET_CENTER_GUI_ID
            ? GalaxyBase.proxy.createTerminalAssetCenterClientGui(player.inventory, TerminalAssetCenterTab.fromCode(x).getCode()) : null;
    }

    private static BaseVaultService resolveVault() {
        if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) return null;
        InstitutionCoreModule module = GalaxyBase.proxy.getModuleManager().findModule(InstitutionCoreModule.class);
        return module == null ? null : module.getBaseVaultService();
    }

    private static TerminalWarehouseBayService resolveBay() {
        if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) return null;
        WarehouseModule module = GalaxyBase.proxy.getModuleManager().findModule(WarehouseModule.class);
        return module == null ? null : module.getTerminalBayService();
    }

    private static AssetActivityService resolveActivity() {
        if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) return null;
        InstitutionCoreModule module = GalaxyBase.proxy.getModuleManager().findModule(InstitutionCoreModule.class);
        return module == null ? null : new AssetActivityService(module.getMarketInfrastructure(), module.getBaseVaultService());
    }
}

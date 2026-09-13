package com.jsirgalaxybase.bootstrap;

import com.jsirgalaxybase.modules.core.vault.client.GuiBaseVault;
import com.jsirgalaxybase.modules.warehouse.client.GuiTerminalWarehouseBay;
import com.jsirgalaxybase.modules.warehouse.client.GuiTerminalAssetCenter;
import com.jsirgalaxybase.terminal.client.settings.TerminalAppearance;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    public ClientProxy() {
        super(true);
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        TerminalAppearance.INSTANCE.initialize(getConfiguration().getMinecraftDirectory());
    }

    @Override
    public Object createBaseVaultClientGui(InventoryPlayer inventory) {
        return new GuiBaseVault(inventory);
    }

    @Override
    public Object createTerminalWarehouseBayClientGui(InventoryPlayer inventory) {
        return new GuiTerminalWarehouseBay(inventory);
    }

    @Override
    public Object createTerminalAssetCenterClientGui(InventoryPlayer inventory, int initialTabCode) {
        return new GuiTerminalAssetCenter(inventory, initialTabCode);
    }
}

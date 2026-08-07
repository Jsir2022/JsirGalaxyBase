package com.jsirgalaxybase.bootstrap;

import com.jsirgalaxybase.modules.core.vault.client.GuiBaseVault;

import net.minecraft.entity.player.InventoryPlayer;

public class ClientProxy extends CommonProxy {

    public ClientProxy() {
        super(true);
    }

    @Override
    public Object createBaseVaultClientGui(InventoryPlayer inventory) {
        return new GuiBaseVault(inventory);
    }
}

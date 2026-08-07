package com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

/** Forge bridge for the personal Base Vault. */
public final class BaseVaultGuiHandler implements IGuiHandler {

    public static final int PERSONAL_VAULT_GUI_ID = 41;

    public static boolean openPersonalVault(EntityPlayerMP player) {
        if (player == null || GalaxyBase.instance == null || GalaxyBase.proxy == null
            || GalaxyBase.proxy.getModuleManager() == null) {
            return false;
        }
        InstitutionCoreModule module = GalaxyBase.proxy.getModuleManager().findModule(InstitutionCoreModule.class);
        if (module == null || module.getBaseVaultService() == null) {
            return false;
        }
        player.openGui(GalaxyBase.instance, PERSONAL_VAULT_GUI_ID, player.worldObj, 0, 0, 0);
        return true;
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BaseVaultService service = resolveService();
        return id == PERSONAL_VAULT_GUI_ID && player != null && service != null
            ? new BaseVaultContainer(player, service) : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != PERSONAL_VAULT_GUI_ID || player == null) {
            return null;
        }
        // The client receives the authoritative Container from the server.
        return GalaxyBase.proxy == null ? null : GalaxyBase.proxy.createBaseVaultClientGui(player.inventory);
    }

    private static BaseVaultService resolveService() {
        if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) {
            return null;
        }
        InstitutionCoreModule module = GalaxyBase.proxy.getModuleManager().findModule(InstitutionCoreModule.class);
        return module == null ? null : module.getBaseVaultService();
    }
}

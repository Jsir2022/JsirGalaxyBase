package com.jsirgalaxybase.modules.core.vault.infrastructure;

import com.jsirgalaxybase.modules.core.market.application.MarketClaimDeliveryException;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketItemSnapshot;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketDeliveryPort;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.application.VaultCapacityException;
import com.jsirgalaxybase.modules.core.vault.application.VaultException;

import net.minecraft.item.ItemStack;

/** Custom listing delivery shares the same finite claim destination as standard market assets. */
public final class VaultCustomMarketDeliveryPort implements CustomMarketDeliveryPort {

    private final BaseVaultService vaultService;

    public VaultCustomMarketDeliveryPort(BaseVaultService vaultService) {
        this.vaultService = vaultService;
    }

    @Override
    public void deliver(String requestId, String playerRef, String sourceServerId, CustomMarketItemSnapshot snapshot) {
        try {
            ItemStack stack = snapshot == null ? null : snapshot.toItemStack();
            vaultService.deliverToPersonalVault(requestId, playerRef, "CUSTOM_MARKET", stack);
        } catch (VaultCapacityException exception) {
            throw new MarketClaimDeliveryException("Base Vault has insufficient space; listing remains pending claim", true);
        } catch (VaultException exception) {
            throw new MarketClaimDeliveryException("Base Vault delivery failed: " + exception.getMessage(), true);
        }
    }
}

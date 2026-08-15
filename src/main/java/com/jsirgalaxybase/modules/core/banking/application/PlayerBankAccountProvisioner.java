package com.jsirgalaxybase.modules.core.banking.application;

import java.util.UUID;

import com.jsirgalaxybase.modules.core.banking.application.command.OpenAccountCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;

/**
 * Idempotent provisioning for a player's STARCOIN account.
 *
 * This deliberately creates an empty account only. It never attempts to infer
 * or migrate assets belonging to another UUID.
 */
public final class PlayerBankAccountProvisioner {

    private static final String TERMINAL_METADATA = "{\"kind\":\"player\",\"source\":\"terminal\"}";

    private PlayerBankAccountProvisioner() {}

    public static BankAccount ensurePersonalAccount(BankingApplicationService bankingService, UUID playerId,
        String displayName) {
        if (bankingService == null) {
            throw new BankingException("banking service is unavailable");
        }
        if (playerId == null) {
            throw new BankingException("player UUID is required for bank account provisioning");
        }

        String normalizedName = displayName == null || displayName.trim().isEmpty() ? "Player" : displayName.trim();
        return bankingService.openAccount(new OpenAccountCommand(
            null,
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            playerId.toString(),
            BankingConstants.DEFAULT_CURRENCY_CODE,
            normalizedName,
            TERMINAL_METADATA));
    }
}

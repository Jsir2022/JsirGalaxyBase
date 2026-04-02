package com.jsirgalaxybase.modules.core.market.application;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.application.command.FrozenBalanceCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.InternalTransferCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts;

public class MarketSettlementFacade {

    private final BankingInfrastructure bankingInfrastructure;
    private final BankingApplicationService bankingService;

    protected MarketSettlementFacade() {
        this.bankingInfrastructure = null;
        this.bankingService = null;
    }

    public MarketSettlementFacade(BankingInfrastructure bankingInfrastructure) {
        this.bankingInfrastructure = bankingInfrastructure;
        this.bankingService = bankingInfrastructure == null ? null : bankingInfrastructure.getBankingApplicationService();
    }

    public BankAccount requirePlayerAccount(String playerRef) {
        return bankingService.findAccount(BankingConstants.OWNER_TYPE_PLAYER_UUID, playerRef,
            BankingConstants.DEFAULT_CURRENCY_CODE).orElseThrow(new java.util.function.Supplier<BankingException>() {

                @Override
                public BankingException get() {
                    return new BankingException("player bank account not found: " + playerRef);
                }
            });
    }

    public BankAccount ensureTaxAccount() {
        return ManagedBankAccounts.ensureManagedAccount(bankingInfrastructure, ManagedBankAccounts.TAX_ACCOUNT);
    }

    public BankAccount ensureExchangeReserveAccount() {
        return ManagedBankAccounts.ensureManagedAccount(bankingInfrastructure, ManagedBankAccounts.EXCHANGE_RESERVE_ACCOUNT);
    }

    public BankingApplicationService getBankingService() {
        return bankingService;
    }

    public BankPostingResult freezeBuyerFunds(FrozenBalanceCommand command) {
        return bankingService.freezeFunds(command);
    }

    public BankPostingResult releaseBuyerFunds(FrozenBalanceCommand command) {
        return bankingService.releaseFunds(command);
    }

    public BankPostingResult settleFromFrozenFunds(InternalTransferCommand command) {
        return bankingService.settleFrozenTransfer(command);
    }

    public BankPostingResult collectTax(InternalTransferCommand command) {
        return bankingService.postInternalTransfer(command);
    }
}
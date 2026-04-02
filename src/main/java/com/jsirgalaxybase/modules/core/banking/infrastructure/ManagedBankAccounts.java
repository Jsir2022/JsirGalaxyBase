package com.jsirgalaxybase.modules.core.banking.infrastructure;

import java.time.Instant;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountStatus;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;

public final class ManagedBankAccounts {

    public static final ManagedAccountSpec SYSTEM_OPERATIONS_ACCOUNT = new ManagedAccountSpec(
        "ops",
        "SYS-OPS-FUND",
        BankAccountType.SYSTEM,
        BankingConstants.OWNER_TYPE_SYSTEM,
        "SYSTEM_OPERATIONS",
        "System Operations Fund",
        0L,
        "{\"purpose\":\"system-operations\"}");

    public static final ManagedAccountSpec EXCHANGE_RESERVE_ACCOUNT = new ManagedAccountSpec(
        "exchange",
        "EXCH-RESERVE",
        BankAccountType.EXCHANGE_RESERVE,
        BankingConstants.OWNER_TYPE_PUBLIC_FUND_CODE,
        "EXCHANGE_RESERVE",
        "Quest Exchange Reserve",
        0L,
        "{\"purpose\":\"exchange-reserve\"}");

    public static final ManagedAccountSpec TAX_ACCOUNT = new ManagedAccountSpec(
        "tax",
        "TAX-POOL",
        BankAccountType.PUBLIC_FUND,
        BankingConstants.OWNER_TYPE_PUBLIC_FUND_CODE,
        "TAX_POOL",
        "Market Tax Pool",
        0L,
        "{\"purpose\":\"market-tax\"}");

    private static final ManagedAccountSpec[] MANAGED_ACCOUNTS = {
        SYSTEM_OPERATIONS_ACCOUNT,
        EXCHANGE_RESERVE_ACCOUNT,
        TAX_ACCOUNT
    };

    private ManagedBankAccounts() {}

    public static ManagedAccountSpec[] getManagedAccounts() {
        return MANAGED_ACCOUNTS.clone();
    }

    public static ManagedAccountSpec requireManagedAccountSpec(String commandKey) {
        for (ManagedAccountSpec spec : MANAGED_ACCOUNTS) {
            if (spec.getCommandKey().equalsIgnoreCase(commandKey)) {
                return spec;
            }
        }
        throw new BankingException("unknown managed account key: " + commandKey);
    }

    public static BankAccount ensureManagedAccount(BankingInfrastructure bankingInfrastructure, ManagedAccountSpec spec) {
        Optional<BankAccount> existing = bankingInfrastructure.getBankAccountRepository().findByOwner(
            spec.getOwnerType(),
            spec.getOwnerRef(),
            BankingConstants.DEFAULT_CURRENCY_CODE);
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = Instant.now();
        Optional<BankAccount> created = bankingInfrastructure.getBankAccountRepository().saveIfOwnerAbsent(new BankAccount(
            0L,
            spec.getAccountNo(),
            spec.getAccountType(),
            spec.getOwnerType(),
            spec.getOwnerRef(),
            BankingConstants.DEFAULT_CURRENCY_CODE,
            spec.getInitialAvailableBalance(),
            0L,
            BankAccountStatus.ACTIVE,
            0L,
            spec.getDisplayName(),
            spec.getMetadataJson(),
            now,
            now));
        if (created.isPresent()) {
            return created.get();
        }

        return bankingInfrastructure.getBankAccountRepository().findByOwner(
            spec.getOwnerType(),
            spec.getOwnerRef(),
            BankingConstants.DEFAULT_CURRENCY_CODE)
            .orElseThrow(new java.util.function.Supplier<BankingException>() {

                @Override
                public BankingException get() {
                    return new BankingException("managed account create lost race but existing account could not be reloaded");
                }
            });
    }

    public static final class ManagedAccountSpec {

        private final String commandKey;
        private final String accountNo;
        private final BankAccountType accountType;
        private final String ownerType;
        private final String ownerRef;
        private final String displayName;
        private final long initialAvailableBalance;
        private final String metadataJson;

        public ManagedAccountSpec(String commandKey, String accountNo, BankAccountType accountType, String ownerType,
            String ownerRef, String displayName, long initialAvailableBalance, String metadataJson) {
            this.commandKey = commandKey;
            this.accountNo = accountNo;
            this.accountType = accountType;
            this.ownerType = ownerType;
            this.ownerRef = ownerRef;
            this.displayName = displayName;
            this.initialAvailableBalance = initialAvailableBalance;
            this.metadataJson = metadataJson;
        }

        public String getCommandKey() {
            return commandKey;
        }

        public String getAccountNo() {
            return accountNo;
        }

        public BankAccountType getAccountType() {
            return accountType;
        }

        public String getOwnerType() {
            return ownerType;
        }

        public String getOwnerRef() {
            return ownerRef;
        }

        public String getDisplayName() {
            return displayName;
        }

        public long getInitialAvailableBalance() {
            return initialAvailableBalance;
        }

        public String getMetadataJson() {
            return metadataJson;
        }
    }
}
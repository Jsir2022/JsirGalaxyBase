package com.jsirgalaxybase.modules.core.banking.application.command;

import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;

public class OpenAccountCommand {

    private final String accountNo;
    private final BankAccountType accountType;
    private final String ownerType;
    private final String ownerRef;
    private final String currencyCode;
    private final String displayName;
    private final String metadataJson;

    public OpenAccountCommand(String accountNo, BankAccountType accountType, String ownerType, String ownerRef,
        String currencyCode, String displayName, String metadataJson) {
        this.accountNo = accountNo;
        this.accountType = accountType;
        this.ownerType = ownerType;
        this.ownerRef = ownerRef;
        this.currencyCode = currencyCode;
        this.displayName = displayName;
        this.metadataJson = metadataJson;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMetadataJson() {
        return metadataJson;
    }
}
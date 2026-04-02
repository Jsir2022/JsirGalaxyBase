package com.jsirgalaxybase.modules.core.banking.domain;

public enum BankTransactionType {

    TRANSFER,
    EXCHANGE,
    ADJUSTMENT,
    SYSTEM_GRANT,
    SYSTEM_DEDUCT,
    MARKET_FUNDS_FREEZE,
    MARKET_FUNDS_RELEASE,
    MARKET_SETTLEMENT_FROM_FROZEN,
    MARKET_TAX_COLLECTION
}
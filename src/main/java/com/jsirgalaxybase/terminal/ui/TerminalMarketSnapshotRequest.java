package com.jsirgalaxybase.terminal.ui;

interface TerminalMarketSnapshotRequest {

    String getSelectedProductKey();

    long parseInstantBuyQuantity();

    long parseInstantSellQuantity();

    long parseLimitBuyQuantity();

    long parseLimitBuyPrice();

    long parseLimitSellQuantity();

    long parseLimitSellPrice();
}

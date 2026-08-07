package com.jsirgalaxybase.terminal.ui;

interface TerminalMarketSnapshotRequest {

    String getSelectedProductKey();

    String getBrowserQuery();

    int getBrowserPage();

    String getBrowserFilter();

    String getChartRange();

    long parseInstantBuyQuantity();

    long parseInstantSellQuantity();

    long parseLimitBuyQuantity();

    long parseLimitBuyPrice();

    long parseLimitSellQuantity();

    long parseLimitSellPrice();
}

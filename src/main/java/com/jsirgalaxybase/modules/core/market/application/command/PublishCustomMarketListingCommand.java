package com.jsirgalaxybase.modules.core.market.application.command;

import net.minecraft.item.ItemStack;

public class PublishCustomMarketListingCommand {

    private final String requestId;
    private final String playerRef;
    private final String sourceServerId;
    private final long askingPrice;
    private final String currencyCode;
    private final ItemStack itemStack;

    public PublishCustomMarketListingCommand(String requestId, String playerRef, String sourceServerId,
        long askingPrice, String currencyCode, ItemStack itemStack) {
        this.requestId = requestId;
        this.playerRef = playerRef;
        this.sourceServerId = sourceServerId;
        this.askingPrice = askingPrice;
        this.currencyCode = currencyCode;
        this.itemStack = itemStack;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPlayerRef() {
        return playerRef;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public long getAskingPrice() {
        return askingPrice;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
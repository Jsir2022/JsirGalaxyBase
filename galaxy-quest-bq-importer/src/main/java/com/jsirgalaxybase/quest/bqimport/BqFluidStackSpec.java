package com.jsirgalaxybase.quest.bqimport;

public final class BqFluidStackSpec {
    private final String fluidName;
    private final int amount;
    private final String tagJson;

    BqFluidStackSpec(String fluidName, int amount, String tagJson) {
        this.fluidName = fluidName;
        this.amount = amount;
        this.tagJson = tagJson;
    }

    public String getFluidName() { return fluidName; }
    public int getAmount() { return amount; }
    public String getTagJson() { return tagJson; }
}

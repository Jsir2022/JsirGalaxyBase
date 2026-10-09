package com.jsirgalaxybase.quest.bqimport;

public final class BqFluidStackSpec {
    private final String fluidName;
    private final int amount;
    private final String tagJson;
    private final boolean tagPresent;

    BqFluidStackSpec(String fluidName, int amount, String tagJson, boolean tagPresent) {
        this.fluidName = fluidName;
        this.amount = amount;
        this.tagJson = tagJson;
        this.tagPresent = tagPresent;
    }

    public String getFluidName() { return fluidName; }
    public int getAmount() { return amount; }
    public String getTagJson() { return tagJson; }
    public boolean isTagPresent() { return tagPresent; }
}

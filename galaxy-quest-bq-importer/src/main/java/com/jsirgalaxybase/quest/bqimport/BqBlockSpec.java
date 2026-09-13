package com.jsirgalaxybase.quest.bqimport;

public final class BqBlockSpec {
    private final String registryName;
    private final int meta;
    private final int amount;
    private final String oreDictionary;
    private final String nbtJson;

    BqBlockSpec(String registryName, int meta, int amount, String oreDictionary, String nbtJson) {
        this.registryName = registryName;
        this.meta = meta;
        this.amount = amount;
        this.oreDictionary = oreDictionary;
        this.nbtJson = nbtJson;
    }

    public String getRegistryName() { return registryName; }
    public int getMeta() { return meta; }
    public int getAmount() { return amount; }
    public String getOreDictionary() { return oreDictionary; }
    public String getNbtJson() { return nbtJson; }
}

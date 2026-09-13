package com.jsirgalaxybase.quest.bqimport;

public final class BqItemStackSpec {
    private final String registryName;
    private final int damage;
    private final int count;
    private final String oreDictionary;
    private final String tagJson;

    BqItemStackSpec(String registryName, int damage, int count, String oreDictionary, String tagJson) {
        this.registryName = registryName;
        this.damage = damage;
        this.count = count;
        this.oreDictionary = oreDictionary;
        this.tagJson = tagJson;
    }

    public String getRegistryName() { return registryName; }
    public int getDamage() { return damage; }
    public int getCount() { return count; }
    public String getOreDictionary() { return oreDictionary; }
    public String getTagJson() { return tagJson; }
}

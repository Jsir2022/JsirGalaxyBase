package com.jsirgalaxybase.modules.core.market.domain;

public class TaskCoinDescriptor {

    private final String registryName;
    private final String family;
    private final String tier;
    private final long faceValue;

    public TaskCoinDescriptor(String registryName, String family, String tier, long faceValue) {
        this.registryName = registryName;
        this.family = family;
        this.tier = tier;
        this.faceValue = faceValue;
    }

    public String getRegistryName() {
        return registryName;
    }

    public String getFamily() {
        return family;
    }

    public String getTier() {
        return tier;
    }

    public long getFaceValue() {
        return faceValue;
    }

    public String getBusinessKey() {
        return family + ":" + tier;
    }
}
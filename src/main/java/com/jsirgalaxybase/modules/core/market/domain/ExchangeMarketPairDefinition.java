package com.jsirgalaxybase.modules.core.market.domain;

public class ExchangeMarketPairDefinition {

    private final String pairCode;
    private final String inputAssetCode;
    private final String outputAssetCode;
    private final String inputDisplayName;
    private final String outputDisplayName;

    public ExchangeMarketPairDefinition(String pairCode, String inputAssetCode, String outputAssetCode,
        String inputDisplayName, String outputDisplayName) {
        this.pairCode = pairCode;
        this.inputAssetCode = inputAssetCode;
        this.outputAssetCode = outputAssetCode;
        this.inputDisplayName = inputDisplayName;
        this.outputDisplayName = outputDisplayName;
    }

    public String getPairCode() {
        return pairCode;
    }

    public String getInputAssetCode() {
        return inputAssetCode;
    }

    public String getOutputAssetCode() {
        return outputAssetCode;
    }

    public String getInputDisplayName() {
        return inputDisplayName;
    }

    public String getOutputDisplayName() {
        return outputDisplayName;
    }
}
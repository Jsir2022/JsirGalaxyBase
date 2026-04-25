package com.jsirgalaxybase.modules.core.market.application;

import com.jsirgalaxybase.modules.core.market.domain.TaskCoinDescriptor;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinExchangeQuote;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskCoinExchangePlannerTest {

    private final TaskCoinExchangePlanner planner = new TaskCoinExchangePlanner();

    @Test
    public void resolveRegistryNameRecognizesBaseTierCoin() {
        TaskCoinDescriptor descriptor = planner.resolveRegistryName("dreamcraft:item.CoinSmith").get();

        assertEquals("Smith", descriptor.getFamily());
        assertEquals("BASE", descriptor.getTier());
        assertEquals(1L, descriptor.getFaceValue());
    }

    @Test
    public void resolveRegistryNameRecognizesTierOneCoin() {
        TaskCoinDescriptor descriptor = planner.resolveRegistryName("dreamcraft:item.CoinTechnicianI").get();

        assertEquals("Technician", descriptor.getFamily());
        assertEquals("I", descriptor.getTier());
        assertEquals(10L, descriptor.getFaceValue());
    }

    @Test
    public void resolveRegistryNameRecognizesTierTwoCoin() {
        TaskCoinDescriptor descriptor = planner.resolveRegistryName("dreamcraft:item.CoinChemistII").get();

        assertEquals("Chemist", descriptor.getFamily());
        assertEquals("II", descriptor.getTier());
        assertEquals(100L, descriptor.getFaceValue());
    }

    @Test
    public void quoteRecognizesTierThreeCoinAndReturnsFixedRuleValues() {
        TaskCoinExchangeQuote quote = planner.quote("dreamcraft:item.CoinSurvivorIII", 4).get();

        assertEquals("Survivor", quote.getDescriptor().getFamily());
        assertEquals("III", quote.getDescriptor().getTier());
        assertEquals(1000L, quote.getDescriptor().getFaceValue());
        assertEquals(4000L, quote.getEffectiveExchangeValue());
        assertEquals(4000L, quote.getContributionValue());
        assertEquals(TaskCoinExchangePlanner.RULE_VERSION, quote.getExchangeRuleVersion());
    }

    @Test
    public void quoteRecognizesTierFourCoinAndReturnsFixedRuleValues() {
        TaskCoinExchangeQuote quote = planner.quote("dreamcraft:item.CoinMinerIV", 2).get();

        assertEquals("Miner", quote.getDescriptor().getFamily());
        assertEquals("IV", quote.getDescriptor().getTier());
        assertEquals(10000L, quote.getDescriptor().getFaceValue());
        assertEquals(20000L, quote.getEffectiveExchangeValue());
        assertEquals(20000L, quote.getContributionValue());
        assertEquals(TaskCoinExchangePlanner.RULE_VERSION, quote.getExchangeRuleVersion());
    }

    @Test
    public void quoteTracksFormalInputFieldsWithoutDiscount() {
        TaskCoinExchangeQuote quote = planner.quote("dreamcraft:item.CoinSmith", 7).get();

        assertEquals(7L, quote.getInputQuantity());
        assertEquals(7L, quote.getInputTotalFaceValue());
        assertEquals(0, quote.getDiscountBasisPoints());
    }

    @Test
    public void helperRecognizesUnsupportedHigherTierTaskCoin() {
        assertTrue(planner.isTaskCoinRegistryName("dreamcraft:item.CoinMinerV"));
        assertTrue(planner.isUnsupportedTaskCoinTier("dreamcraft:item.CoinMinerV"));
        assertFalse(planner.isUnsupportedTaskCoinTier("dreamcraft:item.CoinMinerIV"));
    }

    @Test
    public void resolveRegistryNameRejectsUnsupportedHigherRomanSuffix() {
        assertFalse(planner.resolveRegistryName("dreamcraft:item.CoinMinerV").isPresent());
    }

    @Test
    public void resolveRegistryNameRejectsUnsupportedItem() {
        assertFalse(planner.resolveRegistryName("minecraft:stick").isPresent());
    }
}
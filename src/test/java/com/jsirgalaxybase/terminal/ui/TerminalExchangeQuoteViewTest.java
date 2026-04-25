package com.jsirgalaxybase.terminal.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.modules.core.market.application.TaskCoinExchangePlanner;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketLimitPolicy;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketLimitStatus;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketPairDefinition;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteResult;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketRuleVersion;

public class TerminalExchangeQuoteViewTest {

    @Test
    public void supportedQuoteMapsFormalFieldsForTerminalDisplay() {
        TerminalExchangeQuoteView view = TerminalExchangeQuoteView.fromQuote(
            "化学家任务书硬币 x3",
            new ExchangeMarketQuoteResult(
                new ExchangeMarketPairDefinition("task-coin-to-starcoin", "TASK_COIN", "STARCOIN", "任务书硬币",
                    "星光币"),
                new ExchangeMarketRuleVersion(TaskCoinExchangePlanner.RULE_VERSION, "汇率市场任务书硬币固定规则 v1"),
                new ExchangeMarketLimitPolicy(ExchangeMarketLimitStatus.ALLOWED, "TASK_COIN_RULE_APPLIED",
                    "当前为汇率市场 v1 固定规则报价源；兼容旧 market quote/exchange hand 入口。"),
                "req-quote-1", "player-a", "s1-test", "dreamcraft:item.CoinChemistII", "Chemist", "II", 100L,
                3L, 300L, 300L, 300L, 300L, 3L, 0,
                "当前为汇率市场 v1 固定规则报价源；兼容旧 market quote/exchange hand 入口。"));

        assertEquals("task-coin-to-starcoin", view.pairCode);
        assertEquals("TASK_COIN", view.inputAssetCode);
        assertEquals("STARCOIN", view.outputAssetCode);
        assertEquals(TaskCoinExchangePlanner.RULE_VERSION, view.ruleVersion);
        assertEquals("ALLOWED", view.limitStatus);
        assertEquals("TASK_COIN_RULE_APPLIED", view.reasonCode);
        assertEquals("3", view.inputQuantity);
        assertEquals("300", view.nominalFaceValue);
        assertEquals("300", view.effectiveExchangeValue);
        assertEquals("300", view.contributionValue);
        assertEquals("按面值执行 / 无折扣", view.discountStatus);
        assertEquals("1", view.executableFlag);
        assertTrue(view.executionHint.contains("300 STARCOIN"));
    }

    @Test
    public void disallowedQuoteMapsFormalRejectionFieldsForTerminalDisplay() {
        TerminalExchangeQuoteView view = TerminalExchangeQuoteView.fromQuote(
            "矿工任务书硬币 x1",
            new ExchangeMarketQuoteResult(
                new ExchangeMarketPairDefinition("task-coin-to-starcoin", "TASK_COIN", "STARCOIN", "任务书硬币",
                    "星光币"),
                new ExchangeMarketRuleVersion(TaskCoinExchangePlanner.RULE_VERSION, "汇率市场任务书硬币固定规则 v1"),
                new ExchangeMarketLimitPolicy(ExchangeMarketLimitStatus.DISALLOWED, "TASK_COIN_ASSET_UNSUPPORTED",
                    "当前手持物品不属于汇率市场支持的任务书硬币资产对。"),
                "req-quote-2", "player-a", "s1-test", "minecraft:stone", "UNRESOLVED", "UNRESOLVED", 0L,
                1L, 0L, 0L, 0L, 0L, 1L, 10000, "当前手持物品不属于汇率市场支持的任务书硬币资产对。"));

        assertEquals("DISALLOWED", view.limitStatus);
        assertEquals("TASK_COIN_ASSET_UNSUPPORTED", view.reasonCode);
        assertEquals("minecraft:stone", view.inputRegistryName);
        assertEquals("0", view.nominalFaceValue);
        assertEquals("0", view.effectiveExchangeValue);
        assertEquals("禁兑 / 不允许执行", view.discountStatus);
        assertEquals("0", view.executableFlag);
        assertTrue(view.notes.contains("不属于汇率市场支持的任务书硬币资产对"));
        assertTrue(view.executionHint.contains("不能继续执行"));
    }

    @Test
    public void emptyStateKeepsExchangePageInExplicitNonExecutableMode() {
        TerminalExchangeQuoteView view = TerminalExchangeQuoteView.empty(
            "汇率市场在线 / 等待手持任务书硬币",
            "当前未检测到手持物品",
            "--",
            "请先把任务书硬币拿在手上，再刷新报价。",
            "当前不能继续执行兑换。");

        assertEquals("UNAVAILABLE", view.limitStatus);
        assertEquals("0", view.executableFlag);
        assertEquals("当前暂无可执行报价", view.discountStatus);
        assertEquals("请先把任务书硬币拿在手上，再刷新报价。", view.notes);
        assertFalse(view.hasFormalQuote());
    }
}
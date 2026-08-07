package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class TerminalExchangeQuoteConfirmationGateTest {

    @Test
    public void confirmationIsSingleUseAndRejectsAChangedFormalQuote() {
        TerminalExchangeQuoteConfirmationGate gate = new TerminalExchangeQuoteConfirmationGate();
        TerminalExchangeMarketActionPayload payload = new TerminalExchangeMarketActionPayload(
            TerminalExchangeMarketActionPayload.TARGET_TASK_COIN);
        TerminalExchangeMarketSectionSnapshot quoted = executableQuote("300", "100 / 1");

        gate.register("player-a", "session-a", payload, quoted);
        assertTrue(gate.consumeIfCurrent("player-a", "session-a", payload, quoted));
        assertFalse(gate.consumeIfCurrent("player-a", "session-a", payload, quoted));

        gate.register("player-a", "session-a", payload, quoted);
        assertFalse(gate.consumeIfCurrent("player-a", "session-a", payload, executableQuote("240", "80 / 1")));
    }

    private TerminalExchangeMarketSectionSnapshot executableQuote(String effectiveValue, String rateDisplay) {
        return new TerminalExchangeMarketSectionSnapshot(
            "汇率市场在线",
            "已生成正式报价。",
            Arrays.asList(TerminalExchangeMarketActionPayload.TARGET_TASK_COIN),
            Arrays.asList("任务书硬币 -> STARCOIN"),
            TerminalExchangeMarketActionPayload.TARGET_TASK_COIN,
            "任务书硬币正式兑换",
            "正式报价详情。",
            "手持任务书硬币 x3",
            "dreamcraft:item.CoinChemistII",
            "task-coin-to-starcoin",
            "TASK_COIN",
            "STARCOIN",
            "task-coin-v1",
            "ALLOWED",
            "TASK_COIN_RULE_APPLIED",
            "可执行",
            "3",
            "300",
            effectiveValue,
            effectiveValue,
            "按规则执行",
            rateDisplay,
            "确认后结算。",
            true,
            TerminalExchangeMarketSectionSnapshot.ActionFeedback.placeholder());
    }
}

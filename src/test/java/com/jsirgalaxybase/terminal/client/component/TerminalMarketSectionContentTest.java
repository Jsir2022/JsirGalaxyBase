package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

public class TerminalMarketSectionContentTest {

    @Test
    public void productClaimRuleAndBookBuildersKeepFullDatasets() {
        TerminalMarketSectionModel model = new TerminalMarketSectionModel(
            "market_standardized",
            "市场服务在线",
            "请选择商品",
            Arrays.asList("p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9"),
            Arrays.asList("商品1", "商品2", "商品3", "商品4", "商品5", "商品6", "商品7", "商品8", "商品9"),
            "p3",
            "商品3",
            "组",
            "12 STARCOIN",
            "11 STARCOIN",
            "13 STARCOIN",
            "64",
            "768 STARCOIN",
            "32",
            "8",
            "5",
            "144 STARCOIN",
            "完整数据测试",
            Arrays.asList("13 x 1", "14 x 2", "15 x 3", "16 x 4", "17 x 5", "18 x 6"),
            Arrays.asList("11 x 1", "10 x 2", "9 x 3", "8 x 4", "7 x 5", "6 x 6", "5 x 7"),
            Arrays.asList("订单1", "订单2", "订单3", "订单4"),
            Arrays.asList("claim1", "claim2", "claim3", "claim4", "claim5"),
            Arrays.asList("1", "2", "3", "4", "5"),
            Arrays.asList("规则1", "规则2", "规则3", "规则4", "规则5", "规则6"),
            new TerminalMarketSectionModel.LimitBuyDraftModel("p3", "12", "16", true),
            new TerminalMarketSectionModel.ActionFeedbackModel("反馈", "等待提交", "INFO"));

        assertEquals(9, TerminalMarketSectionContent.buildProductEntries(model).size());
        assertEquals(5, TerminalMarketSectionContent.buildClaimEntries(model).size());
        assertEquals(6, TerminalMarketSectionContent.buildRuleLines(model).size());
        assertEquals(11, TerminalMarketSectionContent.buildBookLines(model).size());
        assertTrue(TerminalMarketSectionContent.buildBookLines(model).get(10).contains("订单4"));
    }
}
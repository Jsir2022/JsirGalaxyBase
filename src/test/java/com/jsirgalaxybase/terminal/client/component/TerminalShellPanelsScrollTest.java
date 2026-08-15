package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.VerticalScrollPanel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel;

public class TerminalShellPanelsScrollTest {

    @Test
    public void navigationRailWrapsNavItemsInScrollPanel() {
        PanelContainer rail = TerminalShellPanels.createNavigationRail(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 112, 120),
            createHomeModel(10, 2, 1),
            null);

        VerticalScrollPanel scrollPanel = findFirstScrollPanel(rail);

        assertTrue(scrollPanel != null);
        assertTrue(scrollPanel.getMaxScrollOffset() > 0);
        assertTrue(scrollPanel.getBounds().getBottom() <= rail.getBounds().getBottom());
    }

    @Test
    public void persistentNavigationRailDoesNotInterceptClicksOutsideBounds() {
        final AtomicInteger opened = new AtomicInteger();
        PanelContainer rail = TerminalShellPanels.createNavigationRail(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 112, 120),
            createHomeModel(3, 1, 0),
            new TerminalShellPanels.NavigationHandler() {
                @Override
                public void open(TerminalHomeScreenModel.NavItemModel navItem) {
                    opened.incrementAndGet();
                }
            });

        boolean clicked = rail.mouseClicked(null, 150, 10, 0);

        assertTrue(!clicked);
        assertEquals(0, opened.get());
    }

    @Test
    public void persistentNavigationRailAllowsDirectItemPressInsideBounds() {
        PanelContainer rail = TerminalShellPanels.createNavigationRail(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 112, 120),
            createHomeModel(4, 1, 0),
            null);

        boolean clicked = rail.mouseClicked(null, 20, 14, 0);

        assertTrue(clicked);
    }

    @Test
    public void plainSectionBodyUsesScrollPanelForSectionsAndNotifications() {
        PanelContainer body = TerminalShellPanels.createSectionBody(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 320, 180),
            createHomeModel(5, 7, 5),
            null,
            null,
            new TerminalBankSectionState(),
            null,
            new TerminalMarketSectionState(),
            null);

        VerticalScrollPanel scrollPanel = findFirstScrollPanel(body);

        assertTrue(scrollPanel != null);
        assertTrue(scrollPanel.getMaxScrollOffset() > 0);
        assertTrue(scrollPanel.getBounds().getBottom() <= body.getBounds().getBottom());
    }

    @Test
    public void marketStandardizedBodyUsesDedicatedSectionWithIndependentInternalScrolls() {
        TerminalHomeScreenModel.PageSnapshotModel pageSnapshot = new TerminalHomeScreenModel.PageSnapshotModel(
            "market",
            "标准商品市场",
            "workflow-first",
            Collections.singletonList(TerminalHomeScreenModel.SectionModel.placeholder()),
            null,
            createMarketSectionModel(),
            null,
            null,
            null);
        TerminalHomeScreenModel model = new TerminalHomeScreenModel(
            "market_standardized",
            "银河终端 / Test",
            "phase 11 layout",
            TerminalHomeScreenModel.StatusBandModel.placeholder(),
            createHomeModel(5, 1, 0).getNavItems(),
            Collections.singletonList(pageSnapshot),
            Collections.<TerminalHomeScreenModel.NotificationModel>emptyList(),
            "session-market-standardized");

        PanelContainer body = TerminalShellPanels.createSectionBody(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 720, 260),
            model,
            null,
            null,
            new TerminalBankSectionState(),
            null,
            new TerminalMarketSectionState(),
            null);

        assertEquals(0, countImmediateScrollPanels(body));
        assertEquals(0, countAllScrollPanels(body));
    }

    @Test
    public void marketStandardizedSectionKeepsWorkbenchCardsInsideBounds() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.openStandardizedHistory();
        TerminalMarketSection section = new TerminalMarketSection(
            new TerminalPanelFactory(),
            createMarketSectionModel(),
            state,
            null);

        section.setBounds(new GuiRect(0, 0, 720, 240));

        assertEquals(3, section.getChildren().size() - 2);
        assertImmediateChildBoundsInside(section);
        assertEquals(0, countImmediateScrollPanels((PanelContainer) section.getChildren().get(2)));
        assertEquals(0, countImmediateScrollPanels((PanelContainer) section.getChildren().get(3)));
        assertEquals(0, countImmediateScrollPanels((PanelContainer) section.getChildren().get(4)));
        assertChildBoundsInside((PanelContainer) section.getChildren().get(4));
    }

    @Test
    public void marketStandardizedBrowseUsesSingleFullWidthGridSurfaceAtTypicalWindowWidth() {
        TerminalMarketSection section = new TerminalMarketSection(
            new TerminalPanelFactory(),
            createMarketSectionModel(),
            new TerminalMarketSectionState(),
            null);

        section.setBounds(new GuiRect(0, 0, 680, 260));

        GuiPanel browserCard = section.getChildren().get(2);
        GuiPanel dashboardCard = section.getChildren().get(3);

        assertEquals(680, browserCard.getBounds().getWidth());
        assertEquals(260, browserCard.getBounds().getHeight());
        assertEquals(0, dashboardCard.getBounds().getWidth());
        assertEquals(5, countAllButtons((PanelContainer) browserCard));
        assertChildBoundsInside((PanelContainer) browserCard);
    }

    @Test
    public void marketStandardizedCompactTextRowsDoNotOverlap() {
        TerminalMarketSection section = new TerminalMarketSection(
            new TerminalPanelFactory(),
            createMarketSectionModel(),
            new TerminalMarketSectionState(),
            null);

        section.setBounds(new GuiRect(0, 0, 680, 240));

        PanelContainer browserCard = (PanelContainer) section.getChildren().get(2);
        PanelContainer searchPanel = (PanelContainer) browserCard.getChildren().get(0);
        GuiPanel searchField = searchPanel.getChildren().get(0);
        GuiPanel searchStatus = searchPanel.getChildren().get(1);
        GuiPanel searchButton = searchPanel.getChildren().get(2);
        assertTrue(searchField.getBounds().getRight() <= searchStatus.getBounds().getX());
        assertTrue(searchStatus.getBounds().getRight() <= searchButton.getBounds().getX());
        GuiPanel pagerPanel = browserCard.getChildren().get(2);
        assertTrue(pagerPanel.getBounds().getBottom() <= browserCard.getBounds().getBottom());

        GuiPanel grid = browserCard.getChildren().get(1);
        assertTrue(grid.getBounds().getY() >= searchPanel.getBounds().getBottom());
        assertTrue(grid.getBounds().getBottom() <= pagerPanel.getBounds().getY());
    }

    @Test
    public void marketOverviewKeepsAllEntryButtonsInsideTerminalBounds() {
        TerminalMarketSection section = new TerminalMarketSection(
            new TerminalPanelFactory(),
            createMarketOverviewSectionModel(),
            new TerminalMarketSectionState(),
            null);

        section.setBounds(new GuiRect(0, 0, 540, 220));

        assertEquals(3, countAllButtons(section));
        assertChildBoundsInside(section);
    }

    @Test
    public void marketOverviewUsesSummaryStripAndThreePrimaryEntryCards() {
        TerminalMarketSection section = new TerminalMarketSection(
            new TerminalPanelFactory(),
            createMarketOverviewSectionModel(),
            new TerminalMarketSectionState(),
            null);

        section.setBounds(new GuiRect(0, 0, 720, 240));

        assertEquals(7, section.getChildren().size());
    }

    @Test
    public void compactStatusBandKeepsIconControlsInsideTitleBar() {
        PanelContainer band = TerminalShellPanels.createStatusBand(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 540, 13),
            createHomeModel(5, 1, 0).withSelectedPageId("market"),
            null,
            null,
            null,
            null);

        assertImmediateChildBoundsInside(band);
    }

    @Test
    public void marketOverviewKeepsThreeEntryCardsInOneRowAtScaledGuiWidth() {
        TerminalMarketSection section = new TerminalMarketSection(
            new TerminalPanelFactory(),
            createMarketOverviewSectionModel(),
            new TerminalMarketSectionState(),
            null);

        section.setBounds(new GuiRect(0, 0, 460, 220));

        GuiPanel standardizedCard = section.getChildren().get(2);
        GuiPanel customCard = section.getChildren().get(3);
        GuiPanel exchangeCard = section.getChildren().get(4);

        assertEquals(standardizedCard.getBounds().getY(), customCard.getBounds().getY());
        assertEquals(standardizedCard.getBounds().getY(), exchangeCard.getBounds().getY());
        assertTrue(customCard.getBounds().getX() >= standardizedCard.getBounds().getRight());
        assertTrue(exchangeCard.getBounds().getX() >= customCard.getBounds().getRight());
        assertImmediateChildBoundsInside(section);
    }

    @Test
    public void customAndExchangeMarketBodiesDoNotUseOuterScrollWrapper() {
        TerminalMarketSectionState marketState = new TerminalMarketSectionState();

        PanelContainer customBody = TerminalShellPanels.createSectionBody(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 460, 220),
            createMarketHomeModel("market_custom", null, TerminalCustomMarketSectionModel.placeholder(), null),
            null,
            null,
            new TerminalBankSectionState(),
            null,
            marketState,
            null);
        PanelContainer exchangeBody = TerminalShellPanels.createSectionBody(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 460, 220),
            createMarketHomeModel("market_exchange", null, null, TerminalExchangeMarketSectionModel.placeholder()),
            null,
            null,
            new TerminalBankSectionState(),
            null,
            marketState,
            null);

        assertEquals(0, countImmediateScrollPanels(customBody));
        assertEquals(0, countImmediateScrollPanels(exchangeBody));
        assertImmediateChildBoundsInside(customBody);
        assertImmediateChildBoundsInside(exchangeBody);
    }

    @Test
    public void customAndExchangeMarketSectionsKeepBrowseAndDetailSurfacesInsideBounds() {
        TerminalCustomMarketSection custom = new TerminalCustomMarketSection(
            new TerminalPanelFactory(),
            TerminalCustomMarketSectionModel.placeholder(),
            new TerminalCustomMarketSectionState(),
            null);
        TerminalExchangeMarketSection exchange = new TerminalExchangeMarketSection(
            new TerminalPanelFactory(),
            TerminalExchangeMarketSectionModel.placeholder(),
            new TerminalExchangeMarketSectionState(),
            null);

        custom.setBounds(new GuiRect(0, 0, 460, 220));
        exchange.setBounds(new GuiRect(0, 0, 460, 220));

        // Toolbar, four-column grid and independent detail surface share one route at a time.
        assertTrue(custom.getChildren().size() >= 3);
        assertTrue(exchange.getChildren().size() >= 3);
        assertImmediateChildBoundsInside(custom);
        assertImmediateChildBoundsInside(exchange);
        assertEquals(0, countAllScrollPanels(custom));
        assertEquals(0, countAllScrollPanels(exchange));

        PanelContainer customToolbar = (PanelContainer) custom.getChildren().get(0);
        PanelContainer exchangeToolbar = (PanelContainer) exchange.getChildren().get(0);
        assertEquals(5, customToolbar.getChildren().size());
        assertEquals(4, exchangeToolbar.getChildren().size());
        assertChildBoundsInside(customToolbar);
        assertChildBoundsInside(exchangeToolbar);
    }

    @Test
    public void customAndExchangeDetailActionsBelongToTheirDetailContainers() {
        TerminalCustomMarketSectionModel customModel = new TerminalCustomMarketSectionModel(
            "在线", "挂牌", "全部挂牌",
            Collections.singletonList("钻石镐 | 价格 9000"), Collections.singletonList("1"),
            Collections.<String>emptyList(), Collections.<String>emptyList(),
            Collections.<String>emptyList(), Collections.<String>emptyList(),
            "1", "钻石镐", "9000 STARCOIN", "ACTIVE / ESCROW_HELD",
            "卖家=demo", "minecraft:diamond_pickaxe@0", "尚未成交", "可购买",
            true, false, false, TerminalCustomMarketSectionModel.ActionFeedbackModel.placeholder());
        TerminalCustomMarketSectionState customState = new TerminalCustomMarketSectionState();
        customState.requestDetail("1");
        customState.applyModel(customModel);
        TerminalCustomMarketSection custom = new TerminalCustomMarketSection(
            new TerminalPanelFactory(), customModel, customState, null);

        TerminalExchangeMarketSectionModel exchangeModel = new TerminalExchangeMarketSectionModel(
            "在线", "报价", Collections.singletonList("task_coin"), Collections.singletonList("任务书硬币"),
            com.jsirgalaxybase.terminal.TerminalExchangeMarketActionPayload.TARGET_TASK_COIN,
            "魔法师币 $100", "DarkWizard / II", "个人仓第 1 格", "dreamcraft:itemQuestBook@0",
            "TASK_COIN_100_TO_STARCOIN", "TASK_COIN", "STARCOIN", "v1", "AVAILABLE", "OK", "",
            "1", "100", "100", "100", "无折扣", "1:1", "可兑换", true,
            TerminalExchangeMarketSectionModel.ActionFeedbackModel.placeholder());
        TerminalExchangeMarketSectionState exchangeState = new TerminalExchangeMarketSectionState();
        exchangeState.requestDetail("dreamcraft:itemQuestBook@0");
        exchangeState.applyModel(exchangeModel);
        TerminalExchangeMarketSection exchange = new TerminalExchangeMarketSection(
            new TerminalPanelFactory(), exchangeModel, exchangeState, null);

        custom.setBounds(new GuiRect(0, 0, 460, 220));
        exchange.setBounds(new GuiRect(0, 0, 460, 220));

        PanelContainer customDetail = (PanelContainer) custom.getChildren().get(2);
        PanelContainer exchangeDetail = (PanelContainer) exchange.getChildren().get(2);
        assertEquals(3, customDetail.getChildren().size());
        assertEquals(2, exchangeDetail.getChildren().size());
        assertChildBoundsInside(customDetail);
        assertChildBoundsInside(exchangeDetail);
    }

    @Test
    public void marketWorkspaceLayoutsStayInsideCompactScaledBounds() {
        TerminalMarketSection standardized = new TerminalMarketSection(
            new TerminalPanelFactory(),
            createMarketSectionModel(),
            new TerminalMarketSectionState(),
            null);
        TerminalMarketSection overview = new TerminalMarketSection(
            new TerminalPanelFactory(),
            createMarketOverviewSectionModel(),
            new TerminalMarketSectionState(),
            null);
        TerminalCustomMarketSection custom = new TerminalCustomMarketSection(
            new TerminalPanelFactory(),
            TerminalCustomMarketSectionModel.placeholder(),
            new TerminalCustomMarketSectionState(),
            null);
        TerminalExchangeMarketSection exchange = new TerminalExchangeMarketSection(
            new TerminalPanelFactory(),
            TerminalExchangeMarketSectionModel.placeholder(),
            new TerminalExchangeMarketSectionState(),
            null);

        standardized.setBounds(new GuiRect(0, 0, 300, 120));
        overview.setBounds(new GuiRect(0, 0, 300, 140));
        custom.setBounds(new GuiRect(0, 0, 220, 120));
        exchange.setBounds(new GuiRect(0, 0, 220, 120));

        assertImmediateChildBoundsInside(standardized);
        assertImmediateChildBoundsInside(overview);
        assertImmediateChildBoundsInside(custom);
        assertImmediateChildBoundsInside(exchange);
    }

    @Test
    public void marketStatusBandTextStaysCompactForOverviewAndWorkbenchRoutes() {
        TerminalHomeScreenModel.PageSnapshotModel overviewSnapshot = new TerminalHomeScreenModel.PageSnapshotModel(
            "market",
            "这是一个很长的市场标题，不应进入顶栏",
            "这是一个很长的市场说明，不应进入顶栏",
            Collections.singletonList(TerminalHomeScreenModel.SectionModel.placeholder()),
            null,
            createMarketOverviewSectionModel(),
            null,
            null,
            null);
        TerminalHomeScreenModel.PageSnapshotModel standardizedSnapshot = new TerminalHomeScreenModel.PageSnapshotModel(
            "market",
            "石头市场 - 一个不应该出现在顶栏里的很长标题",
            "超长商品摘要不应挤进状态栏",
            Collections.singletonList(TerminalHomeScreenModel.SectionModel.placeholder()),
            null,
            createMarketSectionModel(),
            null,
            null,
            null);
        TerminalHomeScreenModel.PageSnapshotModel customSnapshot = new TerminalHomeScreenModel.PageSnapshotModel(
            "market_custom",
            "定制市场",
            "custom",
            Collections.singletonList(TerminalHomeScreenModel.SectionModel.placeholder()),
            null,
            createMarketSectionModel(),
            TerminalCustomMarketSectionModel.placeholder(),
            null);
        TerminalHomeScreenModel.PageSnapshotModel exchangeSnapshot = new TerminalHomeScreenModel.PageSnapshotModel(
            "market_exchange",
            "汇率市场",
            "exchange",
            Collections.singletonList(TerminalHomeScreenModel.SectionModel.placeholder()),
            null,
            createMarketSectionModel(),
            null,
            TerminalExchangeMarketSectionModel.placeholder());
        TerminalHomeScreenModel model = createHomeModel(5, 1, 0).withSelectedPageId("market");

        assertEquals("市场 / 总入口", TerminalMarketShell.buildStatusBandText(model, overviewSnapshot));
        assertEquals("市场 / 标准商品 / 交易台", TerminalMarketShell.buildStatusBandText(model, standardizedSnapshot));
        assertEquals("市场 / 定制商品", TerminalMarketShell.buildStatusBandText(model, customSnapshot));
        assertEquals("市场 / 汇率市场", TerminalMarketShell.buildStatusBandText(model, exchangeSnapshot));
    }

    @Test
    public void evenSectionHeightSubtractsGapBeforeDivision() {
        int height = TerminalShellPanels.computeEvenSectionHeight(160, 4, 6, 54);

        assertEquals(54, height);
        assertTrue(TerminalShellPanels.computeStackHeight(4, height, 6) > 160);
    }

    @Test
    public void serverToolsBodyUsesDedicatedSectionWithoutOuterScrollWrapper() {
        TerminalHomeScreenModel.PageSnapshotModel pageSnapshot = new TerminalHomeScreenModel.PageSnapshotModel(
            "server_tools",
            "群组服传送",
            "transport",
            Collections.singletonList(TerminalHomeScreenModel.SectionModel.placeholder()),
            null,
            null,
            null,
            null,
            new TerminalServerToolsSectionModel(
                "runtime online",
                "lobby",
                Collections.singletonList("lobby | Lobby"),
                Collections.singletonList("lobby"),
                Arrays.asList("s2test", "lobbytest"),
                Arrays.asList("s2test", "lobbytest"),
                Arrays.asList("前往 S2", "返回 Lobby"),
                Arrays.asList("可用", "可用"),
                Collections.singletonList("05-18 10:00 | lobby -> s2 | DISPATCHED | proxy dispatch requested"),
                "s2test",
                "s2test",
                "target=s2",
                "s2",
                "dim 0 / 0, 80, 0",
                "前往 S2",
                true,
                "lobby",
                "s2",
                "DISPATCHED",
                "05-18 10:00",
                "proxy dispatch requested",
                TerminalServerToolsSectionModel.ActionFeedbackModel.placeholder()));
        TerminalHomeScreenModel model = new TerminalHomeScreenModel(
            "server_tools",
            "银河终端 / Test",
            "phase 10 layout",
            TerminalHomeScreenModel.StatusBandModel.placeholder(),
            createHomeModel(5, 1, 0).getNavItems(),
            Collections.singletonList(pageSnapshot),
            Collections.<TerminalHomeScreenModel.NotificationModel>emptyList(),
            "session-server-tools");

        PanelContainer body = TerminalShellPanels.createSectionBody(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 540, 260),
            model,
            null,
            null,
            new TerminalBankSectionState(),
            null,
            new TerminalMarketSectionState(),
            null,
            new TerminalServerToolsSectionState(),
            null);

        assertEquals(0, countImmediateScrollPanels(body));
        assertTrue(findFirstScrollPanel(body) != null);
    }

    @Test
    public void serverToolsSectionUsesSideBySideLayoutAtTypicalGuiWidth() {
        TerminalServerToolsSection section = new TerminalServerToolsSection(
            new TerminalPanelFactory(),
            createServerToolsModel(),
            new TerminalServerToolsSectionState(),
            null);

        section.setBounds(new GuiRect(0, 0, 360, 220));

        GuiPanel leftCard = section.getChildren().get(0);
        GuiPanel rightCard = section.getChildren().get(1);

        assertTrue(rightCard.getBounds().getX() >= leftCard.getBounds().getRight());
        assertEquals(220, leftCard.getBounds().getHeight());
        assertEquals(220, rightCard.getBounds().getHeight());
    }

    @Test
    public void serverToolsSectionIncludesIndependentWorkspaceScrollPanel() {
        TerminalServerToolsSection section = new TerminalServerToolsSection(
            new TerminalPanelFactory(),
            createServerToolsModel(),
            new TerminalServerToolsSectionState(),
            null);

        section.setBounds(new GuiRect(0, 0, 360, 120));

        assertTrue(countAllScrollPanels(section) >= 2);
        assertTrue(findLastScrollPanel(section).getMaxScrollOffset() > 0);
    }

    private static TerminalHomeScreenModel createHomeModel(int navCount, int sectionCount, int notificationCount) {
        List<TerminalHomeScreenModel.NavItemModel> navItems = new ArrayList<TerminalHomeScreenModel.NavItemModel>();
        for (int index = 0; index < navCount; index++) {
            navItems.add(new TerminalHomeScreenModel.NavItemModel(
                index == 0 ? "home" : "career",
                "入口" + index,
                "子标题 " + index,
                true,
                index == 0));
        }

        List<TerminalHomeScreenModel.SectionModel> sections = new ArrayList<TerminalHomeScreenModel.SectionModel>();
        for (int index = 0; index < sectionCount; index++) {
            sections.add(new TerminalHomeScreenModel.SectionModel(
                "section_" + index,
                "Section " + index,
                "summary " + index,
                "detail " + index));
        }

        List<TerminalHomeScreenModel.NotificationModel> notifications = new ArrayList<TerminalHomeScreenModel.NotificationModel>();
        for (int index = 0; index < notificationCount; index++) {
            notifications.add(new TerminalHomeScreenModel.NotificationModel(
                "通知 " + index,
                "这是一条较长的通知内容，用来验证普通首页正文交给滚动容器处理。",
                "INFO"));
        }

        return new TerminalHomeScreenModel(
            "home",
            "银河终端 / Test",
            "phase 10 layout",
            TerminalHomeScreenModel.StatusBandModel.placeholder(),
            navItems,
            Collections.singletonList(new TerminalHomeScreenModel.PageSnapshotModel(
                "home",
                "首页",
                "普通 section 页",
                sections)),
            notifications,
            "session-layout");
    }

    private static TerminalServerToolsSectionModel createServerToolsModel() {
        return new TerminalServerToolsSectionModel(
            "runtime online",
            "lobby",
            Collections.singletonList("lobby | Lobby"),
            Collections.singletonList("lobby"),
            Arrays.asList("s2test", "lobbytest"),
            Arrays.asList("s2test", "lobbytest"),
            Arrays.asList("前往 S2", "返回 Lobby"),
            Arrays.asList("可用", "可用"),
            Collections.singletonList("05-18 10:00 | lobby -> s2 | DISPATCHED | proxy dispatch requested"),
            "s2test",
            "s2test",
            "target=s2",
            "s2",
            "dim 0 / 0, 80, 0",
            "前往 S2 的测试传送点，使用真实跨服链路。",
            true,
            "lobby",
            "s2",
            "DISPATCHED",
            "05-18 10:00",
            "proxy dispatch requested",
            TerminalServerToolsSectionModel.ActionFeedbackModel.placeholder());
    }

    private static TerminalMarketSectionModel createMarketSectionModel() {
        return new TerminalMarketSectionModel(
            "market_standardized",
            "市场服务在线",
            "请选择商品",
            Arrays.asList("minecraft:stone:0", "minecraft:iron_ingot:0"),
            Arrays.asList("石头", "铁锭"),
            "minecraft:stone:0",
            "石头",
            "组",
            "12 STARCOIN",
            "11 STARCOIN",
            "13 STARCOIN",
            "12",
            "16",
            "64",
            "768 STARCOIN",
            "32",
            "8",
            "4",
            "120 STARCOIN",
            "摘要",
            "目录版本=default | 来源=runtime | 卖出来源=统一仓储 AVAILABLE",
            "当前 AVAILABLE=32，可直接卖出。",
            "冻结预计 204 STARCOIN。",
            "将锁定 AVAILABLE 数量。",
            "预计按当前卖盘成交。",
            "预计按当前买盘成交。",
            Arrays.asList("13 x 16", "14 x 32"),
            Arrays.asList("11 x 12", "10 x 48"),
            Arrays.asList("orderId=7 | BUY | OPEN | 16 @ 11"),
            Arrays.asList("7"),
            Arrays.asList("1"),
            Arrays.asList("custodyId=31 | 4 单位待提取"),
            Arrays.asList("31"),
            Arrays.asList("规则1", "规则2"),
            true,
            new TerminalMarketSectionModel.LimitBuyDraftModel("minecraft:stone:0", "12", "16", true),
            new TerminalMarketSectionModel.LimitSellDraftModel("minecraft:stone:0", "13", "8", true),
            new TerminalMarketSectionModel.InstantDraftModel("minecraft:stone:0", "5", true),
            new TerminalMarketSectionModel.InstantDraftModel("minecraft:stone:0", "6", true),
            new TerminalMarketSectionModel.ActionFeedbackModel("市场", "等待确认", "INFO"));
    }

    private static TerminalMarketSectionModel createMarketOverviewSectionModel() {
        return new TerminalMarketSectionModel(
            "market",
            "市场服务在线",
            "MARKET 根页只做入口",
            Arrays.asList("minecraft:stone:0", "minecraft:iron_ingot:0", "minecraft:gold_ingot:0"),
            Arrays.asList("石头", "铁锭", "金锭"),
            "",
            "未选中商品",
            "组",
            "12 STARCOIN",
            "11 STARCOIN",
            "13 STARCOIN",
            "12",
            "16",
            "64",
            "768 STARCOIN",
            "32",
            "8",
            "4",
            "120 STARCOIN",
            "摘要",
            "目录版本=default | 来源=runtime | 卖出来源=统一仓储 AVAILABLE",
            "当前 AVAILABLE=32，可直接卖出。",
            "冻结预计 204 STARCOIN。",
            "将锁定 AVAILABLE 数量。",
            "预计按当前卖盘成交。",
            "预计按当前买盘成交。",
            Arrays.asList("13 x 16"),
            Arrays.asList("11 x 12"),
            Arrays.asList("orderId=7 | BUY | OPEN | 16 @ 11"),
            Arrays.asList("7"),
            Arrays.asList("1"),
            Arrays.asList("custodyId=31 | 4 单位待提取"),
            Arrays.asList("31"),
            Arrays.asList("规则1", "规则2"),
            true,
            new TerminalMarketSectionModel.LimitBuyDraftModel("", "", "", false),
            new TerminalMarketSectionModel.LimitSellDraftModel("", "", "", false),
            new TerminalMarketSectionModel.InstantDraftModel("", "", false),
            new TerminalMarketSectionModel.InstantDraftModel("", "", false),
            new TerminalMarketSectionModel.ActionFeedbackModel("市场", "等待确认", "INFO"));
    }

    private static TerminalHomeScreenModel createMarketHomeModel(String selectedPageId,
        TerminalMarketSectionModel marketModel,
        TerminalCustomMarketSectionModel customModel,
        TerminalExchangeMarketSectionModel exchangeModel) {
        TerminalHomeScreenModel.PageSnapshotModel pageSnapshot = new TerminalHomeScreenModel.PageSnapshotModel(
            "market",
            "市场",
            "market",
            Collections.singletonList(TerminalHomeScreenModel.SectionModel.placeholder()),
            null,
            marketModel,
            customModel,
            exchangeModel,
            null);
        return new TerminalHomeScreenModel(
            selectedPageId,
            "银河终端 / Test",
            "market layout",
            TerminalHomeScreenModel.StatusBandModel.placeholder(),
            createHomeModel(5, 1, 0).getNavItems(),
            Collections.singletonList(pageSnapshot),
            Collections.<TerminalHomeScreenModel.NotificationModel>emptyList(),
            "session-market-layout");
    }

    private static VerticalScrollPanel findFirstScrollPanel(PanelContainer container) {
        for (GuiPanel child : container.getChildren()) {
            if (child instanceof VerticalScrollPanel) {
                return (VerticalScrollPanel) child;
            }
            if (child instanceof PanelContainer) {
                VerticalScrollPanel nested = findFirstScrollPanel((PanelContainer) child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static VerticalScrollPanel findLastScrollPanel(PanelContainer container) {
        VerticalScrollPanel found = null;
        for (GuiPanel child : container.getChildren()) {
            if (child instanceof VerticalScrollPanel) {
                found = (VerticalScrollPanel) child;
            }
            if (child instanceof PanelContainer) {
                VerticalScrollPanel nested = findLastScrollPanel((PanelContainer) child);
                if (nested != null) {
                    found = nested;
                }
            }
        }
        return found;
    }

    private static int countImmediateScrollPanels(PanelContainer container) {
        int count = 0;
        for (GuiPanel child : container.getChildren()) {
            if (child instanceof VerticalScrollPanel) {
                count++;
            }
        }
        return count;
    }

    private static int countAllScrollPanels(PanelContainer container) {
        int count = 0;
        for (GuiPanel child : container.getChildren()) {
            if (child instanceof VerticalScrollPanel) {
                count++;
            }
            if (child instanceof PanelContainer) {
                count += countAllScrollPanels((PanelContainer) child);
            }
        }
        return count;
    }

    private static int countAllButtons(PanelContainer container) {
        int count = 0;
        for (GuiPanel child : container.getChildren()) {
            if (child instanceof ButtonPanel) {
                count++;
            }
            if (child instanceof PanelContainer) {
                count += countAllButtons((PanelContainer) child);
            }
        }
        return count;
    }

    private static void assertChildBoundsInside(PanelContainer container) {
        GuiRect parent = container.getBounds();
        for (GuiPanel child : container.getChildren()) {
            GuiRect bounds = child.getBounds();
            assertTrue(bounds.getX() >= parent.getX());
            assertTrue(bounds.getY() >= parent.getY());
            assertTrue(bounds.getRight() <= parent.getRight());
            assertTrue(bounds.getBottom() <= parent.getBottom());
            if (child instanceof PanelContainer) {
                assertChildBoundsInside((PanelContainer) child);
            }
        }
    }

    private static void assertImmediateChildBoundsInside(PanelContainer container) {
        GuiRect parent = container.getBounds();
        for (GuiPanel child : container.getChildren()) {
            GuiRect bounds = child.getBounds();
            assertTrue(bounds.getX() >= parent.getX());
            assertTrue(bounds.getY() >= parent.getY());
            assertTrue(bounds.getRight() <= parent.getRight());
            assertTrue(bounds.getBottom() <= parent.getBottom());
        }
    }

    private static void assertNoVerticalOverlap(GuiPanel upper, GuiPanel lower) {
        if (upper.getBounds().getHeight() <= 0 || lower.getBounds().getHeight() <= 0) {
            return;
        }
        assertTrue(upper.getBounds().getBottom() <= lower.getBounds().getY());
    }
}

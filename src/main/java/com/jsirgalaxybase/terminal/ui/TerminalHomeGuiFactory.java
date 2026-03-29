package com.jsirgalaxybase.terminal.ui;

import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.FakePlayer;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TerminalHomeGuiFactory extends AbstractUIFactory<GuiData> {

    public static final TerminalHomeGuiFactory INSTANCE = new TerminalHomeGuiFactory();

    private TerminalHomeGuiFactory() {
        super("jsirgalaxybase:terminal_home");
    }

    public void open(EntityPlayerMP player) {
        if (player == null || player instanceof FakePlayer) {
            return;
        }
        GuiManager.open(this, new GuiData(player), player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player, GuiData guiData) {
        return player != null && guiData != null && player == guiData.getPlayer() && !player.isDead;
    }

    @Override
    public IGuiHolder<GuiData> getGuiHolder(GuiData data) {
        return TerminalHomeGuiHolder.INSTANCE;
    }

    @Override
    public void writeGuiData(GuiData guiData, PacketBuffer buffer) {}

    @Override
    public GuiData readGuiData(EntityPlayer player, PacketBuffer buffer) {
        return new GuiData(player);
    }

    private static final class TerminalHomeGuiHolder implements IGuiHolder<GuiData> {

        private static final TerminalHomeGuiHolder INSTANCE = new TerminalHomeGuiHolder();
        private static final int MIN_PANEL_WIDTH = 436;
        private static final int MIN_PANEL_HEIGHT = 272;
        private static final float PANEL_WIDTH_RATIO = 0.90f;
        private static final float PANEL_HEIGHT_RATIO = 0.90f;
        private static final float INNER_WIDTH_RATIO = 0.96f;
        private static final float INNER_HEIGHT_RATIO = 0.95f;
        private static final float NAV_WIDTH_RATIO = 0.13f;
        private static final float CONTENT_WIDTH_RATIO = 0.87f;
        private static final int TEXT_PRIMARY = Color.rgb(232, 237, 242);
        private static final int TEXT_SECONDARY = Color.rgb(171, 182, 193);
        private static final int TEXT_MUTED = Color.rgb(126, 141, 154);
        private static final int TEXT_ACCENT = Color.rgb(82, 155, 237);
        private static final int TEXT_HIGHLIGHT = Color.rgb(139, 208, 255);
        private static final int BG_ROOT = Color.rgb(21, 26, 33);
        private static final int BG_HEADER = Color.rgb(25, 31, 39);
        private static final int BG_NAV = Color.rgb(23, 28, 35);
        private static final int BG_SURFACE = Color.rgb(33, 41, 50);
        private static final int BG_SECTION = Color.rgb(39, 48, 58);
        private static final int BG_SECTION_ALT = Color.rgb(44, 54, 65);
        private static final int BG_ROW = Color.rgb(47, 57, 68);
        private static final int BG_BUTTON = Color.rgb(37, 45, 54);
        private static final int BORDER_STRONG = Color.rgb(95, 111, 127);
        private static final int BORDER_SOFT = Color.rgb(69, 82, 96);
        private static final int CELL_ENABLED = Color.rgb(69, 146, 229);
        private static final int CELL_DISABLED = Color.rgb(87, 96, 106);

        @Override
        public ModularPanel buildUI(GuiData data, PanelSyncManager syncManager, UISettings settings) {
            final int[] selectedPage = new int[] { TerminalPage.HOME.index };
            syncManager.syncValue(
                "career",
                new StringSyncValue(() -> TerminalHomeSnapshotProvider.INSTANCE.create(data.getPlayer())
                    .getCareer()));
            syncManager.syncValue(
                "contribution",
                new IntSyncValue(() -> TerminalHomeSnapshotProvider.INSTANCE.create(data.getPlayer())
                    .getContribution()));
            syncManager.syncValue(
                "reputation",
                new StringSyncValue(() -> TerminalHomeSnapshotProvider.INSTANCE.create(data.getPlayer())
                    .getReputation()));
            syncManager.syncValue(
                "publicTasks",
                new StringSyncValue(() -> TerminalHomeSnapshotProvider.INSTANCE.create(data.getPlayer())
                    .getPublicTasks()));
            syncManager.syncValue(
                "marketSummary",
                new StringSyncValue(() -> TerminalHomeSnapshotProvider.INSTANCE.create(data.getPlayer())
                    .getMarketSummary()));
            syncManager.syncValue("selectedPage", new IntSyncValue(() -> selectedPage[0], value -> selectedPage[0] = value));

            StringSyncValue careerSync = syncManager.findSyncHandler("career", StringSyncValue.class);
            IntSyncValue contributionSync = syncManager.findSyncHandler("contribution", IntSyncValue.class);
            StringSyncValue reputationSync = syncManager.findSyncHandler("reputation", StringSyncValue.class);
            StringSyncValue publicTasksSync = syncManager.findSyncHandler("publicTasks", StringSyncValue.class);
            StringSyncValue marketSummarySync = syncManager.findSyncHandler("marketSummary", StringSyncValue.class);
            IntSyncValue selectedPageSync = syncManager.findSyncHandler("selectedPage", IntSyncValue.class);

            ModularPanel panel = ModularPanel.defaultPanel(
                "jsirgalaxybase_terminal_home",
                MIN_PANEL_WIDTH,
                MIN_PANEL_HEIGHT);
            panel.resizer()
                .sizeRel(PANEL_WIDTH_RATIO, PANEL_HEIGHT_RATIO)
                .align(Alignment.Center);
            panel.child(ButtonWidget.panelCloseButton());
            panel.child(
                Flow.column()
                    .sizeRel(INNER_WIDTH_RATIO, INNER_HEIGHT_RATIO)
                    .center()
                    .background(new Rectangle().color(BG_ROOT))
                    .padding(5)
                    .child(
                        Flow.row()
                            .sizeRel(1f, 1f)
                            .childPadding(8)
                            .child(createNavigationColumn(selectedPageSync, data))
                            .child(
                                createContentColumn(
                                    selectedPageSync,
                                    careerSync,
                                    contributionSync,
                                    reputationSync,
                                    publicTasksSync,
                                    marketSummarySync,
                                    data))));
            return panel;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public ModularScreen createScreen(GuiData data, ModularPanel mainPanel) {
            return new ModularScreen("jsirgalaxybase", mainPanel);
        }

        private Flow createNavigationColumn(IntSyncValue selectedPageSync, GuiData data) {
            Flow navInner = Flow.column()
                .sizeRel(1f, 1f)
                .background(new Rectangle().color(BG_NAV))
                .padding(7)
                .child(createScrollableBody(createNavigationBody(selectedPageSync, data), 4));
            return Flow.column()
                .widthRel(NAV_WIDTH_RATIO)
                .heightRel(1f)
                .background(new Rectangle().color(BORDER_SOFT))
                .padding(1)
                .child(navInner);
        }

        private IWidget createNavigationBody(IntSyncValue selectedPageSync, GuiData data) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(6)
                .child(createNavigationTopSection(selectedPageSync, data))
                .child(createNavigationBottomSection());
        }

        private IWidget createNavigationTopSection(IntSyncValue selectedPageSync, GuiData data) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(6)
                .child(createNavigationBrand(selectedPageSync, data))
                .child(createSeparator())
                .child(
                    new TextWidget<>("终端导航")
                        .widthRel(1f)
                        .height(10)
                        .color(TEXT_MUTED))
                .child(createNavigationButton(TerminalPage.HOME, selectedPageSync))
                .child(createNavigationButton(TerminalPage.CAREER, selectedPageSync))
                .child(createNavigationButton(TerminalPage.PUBLIC_SERVICE, selectedPageSync))
                .child(createNavigationButton(TerminalPage.MARKET, selectedPageSync));
        }

        private IWidget createNavigationBottomSection() {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(5)
                .child(createSeparator())
                .child(createNavigationHint("控制台主题"))
                .child(createNavigationHint("窗口高度自适应"));
        }

        private IWidget createNavigationBrand(IntSyncValue selectedPageSync, GuiData data) {
            String playerName = data.getPlayer() == null ? "访客" : data.getPlayer().getCommandSenderName();
            return Flow.column()
                .widthRel(1f)
                .height(36)
                .background(new Rectangle().color(BG_HEADER))
                .padding(6, 3)
                .child(
                    new TextWidget<>(IKey.dynamic(() -> EnumChatFormatting.AQUA + "JGB Terminal"))
                        .widthRel(1f)
                        .height(11)
                        .color(TEXT_HIGHLIGHT))
                .child(
                    new TextWidget<>(
                        IKey.dynamic(
                            () -> TerminalPage.byIndex(selectedPageSync.getIntValue()).label + " / " + playerName))
                                .widthRel(1f)
                                .height(9)
                                .color(TEXT_SECONDARY));
        }

        private ButtonWidget<?> createNavigationButton(TerminalPage page, IntSyncValue selectedPageSync) {
            return new ButtonWidget<>().widthRel(1f)
                .height(20)
                .background(new Rectangle().color(BG_BUTTON))
                .overlay(
                    IKey.dynamic(
                        () -> (selectedPageSync.getIntValue() == page.index ? EnumChatFormatting.AQUA + "| "
                            : EnumChatFormatting.GRAY.toString() + "  ") + page.label))
                .onMousePressed(mouseButton -> {
                    selectedPageSync.setValue(page.index);
                    return true;
                });
        }

        private TextWidget<?> createNavigationHint(String text) {
            return new TextWidget<>(text)
                .widthRel(1f)
                .height(10)
                .color(TEXT_MUTED);
        }

        private Flow createContentColumn(IntSyncValue selectedPageSync, StringSyncValue careerSync,
            IntSyncValue contributionSync, StringSyncValue reputationSync, StringSyncValue publicTasksSync,
            StringSyncValue marketSummarySync, GuiData data) {
            Flow contentInner = Flow.column()
                .sizeRel(1f, 1f)
                .background(new Rectangle().color(BG_SURFACE))
                .child(
                    createScrollableBody(
                        createScrollableContentColumn(
                            selectedPageSync,
                            careerSync,
                            contributionSync,
                            reputationSync,
                            publicTasksSync,
                            marketSummarySync,
                            data),
                        6))
                .padding(7);
            return Flow.column()
                .widthRel(CONTENT_WIDTH_RATIO)
                .heightRel(1f)
                .background(new Rectangle().color(BORDER_SOFT))
                .padding(1)
                .child(contentInner);
        }

        private IWidget createScrollableContentColumn(IntSyncValue selectedPageSync, StringSyncValue careerSync,
            IntSyncValue contributionSync, StringSyncValue reputationSync, StringSyncValue publicTasksSync,
            StringSyncValue marketSummarySync, GuiData data) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(6)
                .child(createPageTitleBand(selectedPageSync))
                .child(
                    createPageBody(
                        selectedPageSync,
                        careerSync,
                        contributionSync,
                        reputationSync,
                        publicTasksSync,
                        marketSummarySync,
                        data))
                .child(createPageFooter(selectedPageSync));
        }

        private IWidget createScrollableBody(IWidget body, int rightPadding) {
            return new ListWidget<>()
                .sizeRel(1f, 1f)
                .crossAxisAlignment(Alignment.CrossAxis.START)
                .child(
                    Flow.column()
                        .widthRel(1f)
                        .coverChildrenHeight()
                        .paddingRight(rightPadding)
                        .child(body));
        }

        private IWidget createPageTitleBand(IntSyncValue selectedPageSync) {
            return Flow.row()
                .widthRel(1f)
                .height(28)
                .background(new Rectangle().color(BORDER_SOFT))
                .padding(1)
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .expanded()
                        .background(new Rectangle().color(BG_HEADER))
                        .padding(8, 0, 8, 0)
                        .childPadding(8)
                        .child(
                            new TextWidget<>(
                                IKey.dynamic(() -> TerminalPage.byIndex(selectedPageSync.getIntValue()).title))
                                    .widthRel(0.28f)
                                    .height(14)
                                    .color(TEXT_PRIMARY))
                        .child(
                            new TextWidget<>(
                                IKey.dynamic(() -> TerminalPage.byIndex(selectedPageSync.getIntValue()).lead))
                                    .widthRel(0.72f)
                                    .height(12)
                                    .alignment(Alignment.CenterLeft)
                                    .color(TEXT_SECONDARY)));
        }

        private IWidget createPageBody(IntSyncValue selectedPageSync, StringSyncValue careerSync,
            IntSyncValue contributionSync, StringSyncValue reputationSync, StringSyncValue publicTasksSync,
            StringSyncValue marketSummarySync, GuiData data) {
            TerminalPage page = TerminalPage.byIndex(selectedPageSync.getIntValue());
            switch (page) {
                case HOME:
                    return createHomeDashboard(careerSync, contributionSync, reputationSync, publicTasksSync, marketSummarySync);
                case CAREER:
                    return createDetailDashboard(page, TerminalHomeSnapshotProvider.INSTANCE.createCareerPageLines(data.getPlayer()));
                case PUBLIC_SERVICE:
                    return createDetailDashboard(page, TerminalHomeSnapshotProvider.INSTANCE.createPublicServicePageLines(data.getPlayer()));
                case MARKET:
                    return createDetailDashboard(page, TerminalHomeSnapshotProvider.INSTANCE.createMarketPageLines(data.getPlayer()));
                default:
                    return createDetailDashboard(page, new String[] { marketSummarySync.getValue(), "", "", "" });
            }
        }

        private IWidget createHomeDashboard(StringSyncValue careerSync, IntSyncValue contributionSync,
            StringSyncValue reputationSync, StringSyncValue publicTasksSync, StringSyncValue marketSummarySync) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(6)
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(54)
                        .childPadding(6)
                        .child(
                            createSectionShell(
                                "系统状态",
                                0.62f,
                                54,
                                Flow.row()
                                    .widthRel(1f)
                                    .heightRel(1f)
                                    .childPadding(4)
                                    .child(createMetricBlock("职业", careerSync::getValue, 0.42f))
                                    .child(createMetricBlock("贡献", () -> String.valueOf(contributionSync.getIntValue()), 0.20f))
                                    .child(createMetricBlock("声望", reputationSync::getValue, 0.18f))
                                    .child(createMetricBlock("会话", () -> "联机", 0.20f))))
                        .child(
                            createSectionShell(
                                "当前会话",
                                0.36f,
                                54,
                                Flow.row()
                                    .widthRel(1f)
                                    .heightRel(1f)
                                    .childPadding(4)
                                    .child(createMetricBlock("模式", () -> "制度控台", 0.58f))
                                    .child(createMetricBlock("布局", () -> "自适应", 0.42f)))))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(108)
                        .childPadding(6)
                        .child(createSectionShell("终端路由矩阵", 0.80f, 108, createMatrixPanel()))
                        .child(createSectionShell("联机概览", 0.20f, 108, createLegendPanel())))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(68)
                        .childPadding(6)
                        .child(createSectionShell("公共队列", 0.52f, 68, createQueuePanel(publicTasksSync)))
                        .child(createSectionShell("市场监控", 0.48f, 68, createMarketPanel(marketSummarySync))));
        }

        private IWidget createDetailDashboard(TerminalPage page, String[] lines) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(8)
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(108)
                        .childPadding(6)
                        .child(createSectionShell(page.title, 0.7f, 108, createBulletPanel(getLine(0, lines), getLine(1, lines), getLine(2, lines))))
                        .child(createSectionShell("状态", 0.3f, 108, createPageStatePanel(page))))
                .child(
                    createSectionShell(
                        "实施备注",
                        1f,
                        88,
                        createBulletPanel(
                            getLine(3, lines),
                            "当前页面已经切到统一控制台分区样式，后续更适合换成表格与筛选器。",
                            "现阶段仍然是只读信息页，下一步可以继续往操作终端方向推进。")));
        }

        private IWidget createSectionShell(String title, float widthRatio, int height, IWidget body) {
            Flow outer = Flow.column()
                .widthRel(widthRatio)
                .background(new Rectangle().color(BORDER_SOFT))
                .padding(1)
                .child(
                    Flow.column()
                        .widthRel(1f)
                        .expanded()
                        .background(new Rectangle().color(BG_SECTION))
                        .child(createSectionHeader(title))
                        .child(
                            Flow.column()
                                .widthRel(1f)
                                .expanded()
                                .padding(8)
                                .child(body)));
            if (height > 0) {
                outer.height(height);
            } else {
                outer.expanded();
            }
            return outer;
        }

        private IWidget createSectionHeader(String title) {
            return Flow.row()
                .widthRel(1f)
                .height(18)
                .background(new Rectangle().color(BG_HEADER))
                .padding(6, 0, 6, 0)
                .child(
                    new TextWidget<>(title)
                        .widthRel(1f)
                        .height(12)
                        .color(TEXT_SECONDARY));
        }

        private IWidget createMetricBlock(String title, Supplier<String> valueSupplier, float widthRatio) {
            return Flow.column()
                .widthRel(widthRatio)
                .heightRel(1f)
                .background(new Rectangle().color(BG_SECTION_ALT))
                .padding(5, 3)
                .child(
                    new TextWidget<>(title)
                        .widthRel(1f)
                        .height(9)
                        .color(TEXT_MUTED))
                .child(
                    new TextWidget<>(IKey.dynamic(valueSupplier::get))
                        .widthRel(1f)
                        .height(12)
                        .color(TEXT_PRIMARY));
        }

        private IWidget createInfoLine(String label, String value) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .background(new Rectangle().color(BG_SECTION_ALT))
                .padding(5, 3)
                .child(
                    new TextWidget<>(label)
                        .widthRel(1f)
                        .height(9)
                        .color(TEXT_MUTED))
                .child(
                    new TextWidget<>(value)
                        .widthRel(1f)
                        .height(11)
                        .color(TEXT_PRIMARY));
        }

        private IWidget createMatrixPanel() {
            String[] columns = { "职业", "公共", "市场", "账本", "物流" };
            String[] rows = { "摘要", "任务", "订单", "广播" };
            boolean[][] active = {
                { true, true, true, false, false },
                { false, true, false, false, true },
                { false, true, true, true, false },
                { true, false, true, false, true }
            };

            Flow gridColumn = Flow.column()
                .widthRel(1f)
                .expanded()
                .childPadding(2)
                .child(createMatrixColumnHeader(columns));

            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                gridColumn.child(createMatrixRow(rows[rowIndex], active[rowIndex], columns.length));
            }

            return Flow.row()
                .widthRel(1f)
                .expanded()
                .childPadding(4)
                .child(gridColumn);
        }

        private IWidget createMatrixColumnHeader(String[] columns) {
            Flow row = Flow.row()
                .widthRel(1f)
                .height(16)
                .childPadding(2);
            float cellWidth = 1f / columns.length;
            for (String column : columns) {
                row.child(
                    new TextWidget<>(column)
                        .widthRel(cellWidth)
                        .height(10)
                        .alignment(Alignment.Center)
                        .color(TEXT_SECONDARY));
            }
            return row;
        }

        private IWidget createMatrixRow(String label, boolean[] activeCells, int columnCount) {
            return Flow.row()
                .widthRel(1f)
                .height(18)
                .childPadding(2)
                .child(
                    new TextWidget<>(label)
                        .widthRel(0.12f)
                        .height(10)
                        .color(TEXT_SECONDARY))
                .child(createMatrixCellRow(activeCells, columnCount));
        }

        private IWidget createMatrixCellRow(boolean[] activeCells, int columnCount) {
            Flow row = Flow.row()
                .widthRel(0.88f)
                .height(18)
                .childPadding(2);
            float cellWidth = 1f / columnCount;
            for (boolean active : activeCells) {
                row.child(createMatrixCell(active, cellWidth));
            }
            return row;
        }

        private IWidget createMatrixCell(boolean active, float widthRatio) {
            return Flow.column()
                .widthRel(widthRatio)
                .height(14)
                .background(new Rectangle().color(BORDER_SOFT))
                .padding(1)
                .child(
                    Flow.column()
                        .widthRel(1f)
                        .expanded()
                        .background(new Rectangle().color(active ? CELL_ENABLED : CELL_DISABLED)));
        }

        private IWidget createLegendPanel() {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(3)
                .child(createLegendItem("接入", CELL_ENABLED))
                .child(createLegendItem("待接", CELL_DISABLED))
                .child(createSeparator())
                .child(createInfoLine("在线", "已接"));
        }

        private IWidget createLegendItem(String label, int color) {
            return Flow.row()
                .widthRel(1f)
                .height(16)
                .coverChildrenHeight()
                .childPadding(4)
                .child(
                    Flow.column()
                        .width(10)
                        .height(10)
                        .background(new Rectangle().color(BORDER_SOFT))
                        .padding(1)
                        .child(
                            Flow.column()
                                .widthRel(1f)
                                .expanded()
                                .background(new Rectangle().color(color))))
                .child(
                    new TextWidget<>(label)
                        .widthRel(1f)
                        .height(10)
                        .color(TEXT_SECONDARY));
        }

        private IWidget createQueuePanel(StringSyncValue publicTasksSync) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(3)
                .child(createDataRow("开放任务", publicTasksSync::getValue))
                .child(createDataRow("入口", () -> "统一收敛"));
        }

        private IWidget createMarketPanel(StringSyncValue marketSummarySync) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(3)
                .child(createDataRow("市场摘要", marketSummarySync::getValue))
                .child(createDataRow("目标", () -> "价格/订单"));
        }

        private IWidget createDataRow(String label, Supplier<String> valueSupplier) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .background(new Rectangle().color(BG_ROW))
                .padding(5, 3)
                .child(
                    new TextWidget<>(label)
                        .widthRel(1f)
                        .height(9)
                        .color(TEXT_MUTED))
                .child(
                    new TextWidget<>(IKey.dynamic(valueSupplier::get))
                        .widthRel(1f)
                        .height(11)
                        .color(TEXT_PRIMARY));
        }

        private IWidget createBulletPanel(String line1, String line2, String line3) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(4)
                .child(createBulletLine(line1))
                .child(createBulletLine(line2))
                .child(createBulletLine(line3));
        }

        private IWidget createBulletLine(String text) {
            return Flow.row()
                .widthRel(1f)
                .height(18)
                .coverChildrenHeight()
                .childPadding(5)
                .child(
                    Flow.column()
                        .width(6)
                        .height(6)
                        .marginTop(5)
                        .background(new Rectangle().color(TEXT_ACCENT)))
                .child(
                    new TextWidget<>(text)
                        .widthRel(1f)
                        .height(12)
                        .color(TEXT_PRIMARY));
        }

        private IWidget createPageStatePanel(TerminalPage page) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(4)
                .child(createInfoLine("当前页", page.label))
                .child(createInfoLine("显示模式", "制度控制台"))
                .child(createInfoLine("交互阶段", "只读页面壳"));
        }

        private IWidget createSeparator() {
            return Flow.column()
                .widthRel(1f)
                .height(1)
                .background(new Rectangle().color(BORDER_SOFT));
        }

        private TextWidget<?> createPageFooter(IntSyncValue selectedPageSync) {
            return new TextWidget<>(
                IKey.dynamic(
                    () -> EnumChatFormatting.GRAY + "当前分页："
                        + TerminalPage.byIndex(selectedPageSync.getIntValue()).label
                        + " | 深蓝灰控制台"))
                            .widthRel(1f)
                            .height(10)
                            .marginBottom(1);
        }

        private String getLine(int index, String... lines) {
            return index >= 0 && index < lines.length ? lines[index] : "";
        }

        private enum TerminalPage {

            HOME(0, "首页", "总览", "制度总览", "当前玩家制度摘要"),
            CAREER(1, "职业", "职业页", "职业与资格", "资格、阶段与进阶方向"),
            PUBLIC_SERVICE(2, "公共", "公共页", "公共任务与服务", "任务、福利与服务入口"),
            MARKET(3, "市场", "市场页", "市场与订单", "价格摘要与订单入口");

            private final int index;
            private final String label;
            private final String subtitle;
            private final String title;
            private final String lead;

            TerminalPage(int index, String label, String subtitle, String title, String lead) {
                this.index = index;
                this.label = label;
                this.subtitle = subtitle;
                this.title = title;
                this.lead = lead;
            }

            private static TerminalPage byIndex(int index) {
                for (TerminalPage page : values()) {
                    if (page.index == index) {
                        return page;
                    }
                }
                return HOME;
            }
        }
    }
}
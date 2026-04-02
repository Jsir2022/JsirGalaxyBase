package com.jsirgalaxybase.terminal.ui;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.FakePlayer;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.config.ModConfiguration;
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
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

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
        private static final float DEFAULT_PANEL_WIDTH_RATIO = 0.90f;
        private static final float DEFAULT_PANEL_HEIGHT_RATIO = 0.90f;
        private static final float INNER_WIDTH_RATIO = 0.96f;
        private static final float INNER_HEIGHT_RATIO = 0.95f;
        private static final float DEFAULT_NAV_WIDTH_RATIO = 0.13f;
        private static final int TEXT_PRIMARY = Color.rgb(232, 237, 242);
        private static final int TEXT_SECONDARY = Color.rgb(171, 182, 193);
        private static final int TEXT_MUTED = Color.rgb(126, 141, 154);
        private static final int DEFAULT_TEXT_ACCENT = Color.rgb(82, 155, 237);
        private static final int DEFAULT_TEXT_HIGHLIGHT = Color.rgb(139, 208, 255);
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
        private static final int DEFAULT_CELL_ENABLED = Color.rgb(69, 146, 229);
        private static final int CELL_DISABLED = Color.rgb(87, 96, 106);
        private static final String DEFAULT_BANK_ACTION_MESSAGE = "待命 / 可开户 / 可向玩家转账";
        private static final long BANK_TOAST_DURATION_MILLIS = 3000L;
        private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]*");

        @Override
        public ModularPanel buildUI(GuiData data, PanelSyncManager syncManager, UISettings settings) {
            final int[] selectedPage = new int[] { TerminalPage.HOME.index };
            final TerminalBankSessionState bankSessionState = new TerminalBankSessionState(data.getPlayer());
            final String[] bankClientDisplayCache = new String[18];
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
            StringSyncValue bankServiceStateSync = registerLiveBankString(
                syncManager,
                "bankServiceState",
                bankClientDisplayCache,
                0,
                () -> bankSessionState.getCurrentSnapshot().getServiceState());
            StringSyncValue bankPlayerBalanceSync = registerLiveBankString(
                syncManager,
                "bankPlayerBalance",
                bankClientDisplayCache,
                1,
                () -> bankSessionState.getCurrentSnapshot().getPlayerBalance());
            StringSyncValue bankPlayerStatusSync = registerLiveBankString(
                syncManager,
                "bankPlayerStatus",
                bankClientDisplayCache,
                2,
                () -> bankSessionState.getCurrentSnapshot().getPlayerStatus());
            StringSyncValue bankPlayerAccountNoSync = registerLiveBankString(
                syncManager,
                "bankPlayerAccountNo",
                bankClientDisplayCache,
                3,
                () -> bankSessionState.getCurrentSnapshot().getPlayerAccountNo());
            StringSyncValue bankPlayerUpdatedAtSync = registerLiveBankString(
                syncManager,
                "bankPlayerUpdatedAt",
                bankClientDisplayCache,
                4,
                () -> bankSessionState.getCurrentSnapshot().getPlayerUpdatedAt());
            StringSyncValue bankTransferStateSync = registerLiveBankString(
                syncManager,
                "bankTransferState",
                bankClientDisplayCache,
                5,
                () -> bankSessionState.getCurrentSnapshot().getTransferState());
            StringSyncValue bankPlayerLedger1Sync = registerLiveBankString(
                syncManager,
                "bankPlayerLedger1",
                bankClientDisplayCache,
                6,
                () -> getLine(0, bankSessionState.getCurrentSnapshot().getPlayerLedgerLines()));
            StringSyncValue bankPlayerLedger2Sync = registerLiveBankString(
                syncManager,
                "bankPlayerLedger2",
                bankClientDisplayCache,
                7,
                () -> getLine(1, bankSessionState.getCurrentSnapshot().getPlayerLedgerLines()));
            StringSyncValue bankPlayerLedger3Sync = registerLiveBankString(
                syncManager,
                "bankPlayerLedger3",
                bankClientDisplayCache,
                8,
                () -> getLine(2, bankSessionState.getCurrentSnapshot().getPlayerLedgerLines()));
            StringSyncValue bankPlayerLedger4Sync = registerLiveBankString(
                syncManager,
                "bankPlayerLedger4",
                bankClientDisplayCache,
                9,
                () -> getLine(3, bankSessionState.getCurrentSnapshot().getPlayerLedgerLines()));
            StringSyncValue bankExchangeBalanceSync = registerLiveBankString(
                syncManager,
                "bankExchangeBalance",
                bankClientDisplayCache,
                10,
                () -> bankSessionState.getCurrentSnapshot().getExchangeBalance());
            StringSyncValue bankExchangeStatusSync = registerLiveBankString(
                syncManager,
                "bankExchangeStatus",
                bankClientDisplayCache,
                11,
                () -> bankSessionState.getCurrentSnapshot().getExchangeStatus());
            StringSyncValue bankExchangeAccountNoSync = registerLiveBankString(
                syncManager,
                "bankExchangeAccountNo",
                bankClientDisplayCache,
                12,
                () -> bankSessionState.getCurrentSnapshot().getExchangeAccountNo());
            StringSyncValue bankExchangeUpdatedAtSync = registerLiveBankString(
                syncManager,
                "bankExchangeUpdatedAt",
                bankClientDisplayCache,
                13,
                () -> bankSessionState.getCurrentSnapshot().getExchangeUpdatedAt());
            StringSyncValue bankExchangeLedger1Sync = registerLiveBankString(
                syncManager,
                "bankExchangeLedger1",
                bankClientDisplayCache,
                14,
                () -> getLine(0, bankSessionState.getCurrentSnapshot().getExchangeLedgerLines()));
            StringSyncValue bankExchangeLedger2Sync = registerLiveBankString(
                syncManager,
                "bankExchangeLedger2",
                bankClientDisplayCache,
                15,
                () -> getLine(1, bankSessionState.getCurrentSnapshot().getExchangeLedgerLines()));
            StringSyncValue bankExchangeLedger3Sync = registerLiveBankString(
                syncManager,
                "bankExchangeLedger3",
                bankClientDisplayCache,
                16,
                () -> getLine(2, bankSessionState.getCurrentSnapshot().getExchangeLedgerLines()));
            StringSyncValue bankExchangeLedger4Sync = registerLiveBankString(
                syncManager,
                "bankExchangeLedger4",
                bankClientDisplayCache,
                17,
                () -> getLine(3, bankSessionState.getCurrentSnapshot().getExchangeLedgerLines()));
            StringSyncValue bankActionMessageSync = new StringSyncValue(
                bankSessionState::getActionMessage,
                bankSessionState::setActionMessage);
            StringSyncValue bankToastMessageSync = new StringSyncValue(
                bankSessionState::getToastMessage,
                bankSessionState::setToastMessage);
            StringSyncValue bankTransferTargetNameSync = new StringSyncValue(
                bankSessionState::getTransferTargetName,
                bankSessionState::setTransferTargetName);
            StringSyncValue bankTransferAmountTextSync = new StringSyncValue(
                bankSessionState::getTransferAmountText,
                bankSessionState::setTransferAmountText);
            StringSyncValue bankTransferCommentSync = new StringSyncValue(
                bankSessionState::getTransferComment,
                bankSessionState::setTransferComment);
            syncManager.syncValue("bankActionMessage", bankActionMessageSync);
            syncManager.syncValue("bankToastMessage", bankToastMessageSync);
            syncManager.syncValue("selectedPage", new IntSyncValue(() -> selectedPage[0], value -> selectedPage[0] = value));

            StringSyncValue careerSync = syncManager.findSyncHandler("career", StringSyncValue.class);
            IntSyncValue contributionSync = syncManager.findSyncHandler("contribution", IntSyncValue.class);
            StringSyncValue reputationSync = syncManager.findSyncHandler("reputation", StringSyncValue.class);
            StringSyncValue publicTasksSync = syncManager.findSyncHandler("publicTasks", StringSyncValue.class);
            StringSyncValue marketSummarySync = syncManager.findSyncHandler("marketSummary", StringSyncValue.class);
            BankPageSyncState bankSyncState = new BankPageSyncState(
                bankServiceStateSync,
                bankPlayerBalanceSync,
                bankPlayerStatusSync,
                bankPlayerAccountNoSync,
                bankPlayerUpdatedAtSync,
                bankTransferStateSync,
                new StringSyncValue[] {
                    bankPlayerLedger1Sync,
                    bankPlayerLedger2Sync,
                    bankPlayerLedger3Sync,
                    bankPlayerLedger4Sync
                },
                bankExchangeBalanceSync,
                bankExchangeStatusSync,
                bankExchangeAccountNoSync,
                bankExchangeUpdatedAtSync,
                new StringSyncValue[] {
                    bankExchangeLedger1Sync,
                    bankExchangeLedger2Sync,
                    bankExchangeLedger3Sync,
                    bankExchangeLedger4Sync
                },
                bankActionMessageSync,
                bankToastMessageSync,
                bankTransferTargetNameSync,
                bankTransferAmountTextSync,
                bankTransferCommentSync);
            IntSyncValue selectedPageSync = syncManager.findSyncHandler("selectedPage", IntSyncValue.class);

            ModularPanel panel = ModularPanel.defaultPanel(
                "jsirgalaxybase_terminal_home",
                MIN_PANEL_WIDTH,
                MIN_PANEL_HEIGHT);
            panel.resizer()
                .sizeRel(getPanelWidthRatio(), getPanelHeightRatio())
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
                                    bankSyncState,
                                    bankSessionState,
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
                .widthRel(getNavigationWidthRatio())
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
                .child(createNavigationButton(TerminalPage.MARKET, selectedPageSync))
                .child(createNavigationButton(TerminalPage.BANK, selectedPageSync));
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
                        .color(getHighlightColor()))
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
                        () -> (isNavigationSelected(page, selectedPageSync.getIntValue()) ? EnumChatFormatting.AQUA + "| "
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
            StringSyncValue marketSummarySync, BankPageSyncState bankSyncState,
            TerminalBankSessionState bankSessionState, GuiData data) {
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
                            bankSyncState,
                            bankSessionState,
                            data),
                        6))
                .padding(7);
            ParentWidget<?> contentLayer = new ParentWidget<>().sizeRel(1f, 1f)
                .child(contentInner)
                .child(createBankToastOverlay(selectedPageSync, bankSyncState));
            return Flow.column()
                .widthRel(getContentWidthRatio())
                .heightRel(1f)
                .background(new Rectangle().color(BORDER_SOFT))
                .padding(1)
                .child(contentLayer);
        }

        private IWidget createBankToastOverlay(IntSyncValue selectedPageSync, BankPageSyncState bankSyncState) {
            return Flow.column()
                .widthRel(0.76f)
                .coverChildrenHeight()
                .align(Alignment.TopCenter)
                .marginTop(8)
                .background(new Rectangle().color(BORDER_STRONG))
                .padding(1)
                .setEnabledIf(
                    widget -> isBankToastVisible(
                        selectedPageSync.getIntValue(),
                        bankSyncState.toastMessage.getValue()))
                .child(
                    Flow.column()
                        .widthRel(1f)
                        .coverChildrenHeight()
                        .background(new Rectangle().color(BG_HEADER))
                        .padding(8, 6)
                        .child(
                            new TextWidget<>(
                                IKey.dynamic(
                                    () -> getBankToastTitle(bankSyncState.toastMessage.getValue())))
                                        .widthRel(1f)
                                        .height(10)
                                        .color(TEXT_PRIMARY))
                        .child(
                            new TextWidget<>(
                                IKey.dynamic(
                                    () -> getBankToastBody(bankSyncState.toastMessage.getValue())))
                                        .widthRel(1f)
                                        .height(12)
                                        .color(TEXT_SECONDARY))
                        .child(
                            new TextWidget<>(EnumChatFormatting.GRAY + "提示将在 3 秒后自动关闭")
                                .widthRel(1f)
                                .height(9)
                                .color(TEXT_MUTED)));
        }

        private boolean isBankToastVisible(int selectedPageIndex, String actionMessage) {
            if (!TerminalPage.byIndex(selectedPageIndex).isBankPage()) {
                return false;
            }
            String plainMessage = stripFormatting(actionMessage);
            return !plainMessage.isEmpty() && !DEFAULT_BANK_ACTION_MESSAGE.equals(plainMessage);
        }

        private String getBankToastTitle(String actionMessage) {
            String plainMessage = stripFormatting(actionMessage);
            if (plainMessage.isEmpty()) {
                return EnumChatFormatting.GRAY + "银行通知";
            }
            if (isBankSuccessMessage(plainMessage)) {
                return EnumChatFormatting.GREEN + "操作成功";
            }
            if (isBankNoticeMessage(plainMessage)) {
                return EnumChatFormatting.GOLD + "账户提示";
            }
            if (isBankErrorMessage(plainMessage)) {
                return EnumChatFormatting.RED + "操作失败";
            }
            return EnumChatFormatting.AQUA + "银行通知";
        }

        private String getBankToastBody(String actionMessage) {
            String plainMessage = stripFormatting(actionMessage);
            if (plainMessage.isEmpty()) {
                return EnumChatFormatting.GRAY + DEFAULT_BANK_ACTION_MESSAGE;
            }
            if (isBankSuccessMessage(plainMessage)) {
                return EnumChatFormatting.GREEN + plainMessage;
            }
            if (isBankNoticeMessage(plainMessage)) {
                return EnumChatFormatting.GOLD + plainMessage;
            }
            if (isBankErrorMessage(plainMessage)) {
                return EnumChatFormatting.RED + plainMessage;
            }
            return EnumChatFormatting.AQUA + plainMessage;
        }

        private boolean isBankSuccessMessage(String message) {
            return message.contains("成功") || message.contains("已开户") || message.contains("已创建");
        }

        private boolean isBankNoticeMessage(String message) {
            return message.contains("已存在") || message.contains("无需重复") || message.contains("已就绪");
        }

        private boolean isBankErrorMessage(String message) {
            return message.contains("失败") || message.contains("请填写") || message.contains("不能")
                || message.contains("不支持") || message.contains("未启用") || message.contains("不可用")
                || message.contains("必须") || message.contains("在线玩家") || message.contains("不足")
                || message.contains("不存在");
        }

        private static String stripFormatting(String value) {
            if (value == null) {
                return "";
            }
            String trimmed = value.trim();
            StringBuilder plain = new StringBuilder(trimmed.length());
            boolean skipNext = false;
            for (int i = 0; i < trimmed.length(); i++) {
                char current = trimmed.charAt(i);
                if (skipNext) {
                    skipNext = false;
                    continue;
                }
                if (current == '\u00A7') {
                    skipNext = true;
                    continue;
                }
                plain.append(current);
            }
            return plain.toString().trim();
        }

        private IWidget createScrollableContentColumn(IntSyncValue selectedPageSync, StringSyncValue careerSync,
            IntSyncValue contributionSync, StringSyncValue reputationSync, StringSyncValue publicTasksSync,
            StringSyncValue marketSummarySync, BankPageSyncState bankSyncState,
            TerminalBankSessionState bankSessionState, GuiData data) {
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
                        bankSyncState,
                        bankSessionState,
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
            StringSyncValue marketSummarySync, BankPageSyncState bankSyncState,
            TerminalBankSessionState bankSessionState, GuiData data) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .collapseDisabledChild(true)
                .child(createPageContainer(
                    TerminalPage.HOME,
                    selectedPageSync,
                    createHomeDashboard(careerSync, contributionSync, reputationSync, publicTasksSync, marketSummarySync)))
                .child(createPageContainer(
                    TerminalPage.CAREER,
                    selectedPageSync,
                    createDetailDashboard(
                        TerminalPage.CAREER,
                        TerminalHomeSnapshotProvider.INSTANCE.createCareerPageLines(data.getPlayer()))))
                .child(createPageContainer(
                    TerminalPage.PUBLIC_SERVICE,
                    selectedPageSync,
                    createDetailDashboard(
                        TerminalPage.PUBLIC_SERVICE,
                        TerminalHomeSnapshotProvider.INSTANCE.createPublicServicePageLines(data.getPlayer()))))
                .child(createPageContainer(
                    TerminalPage.MARKET,
                    selectedPageSync,
                    createDetailDashboard(
                        TerminalPage.MARKET,
                        TerminalHomeSnapshotProvider.INSTANCE.createMarketPageLines(data.getPlayer()))))
                .child(createPageContainer(TerminalPage.BANK, selectedPageSync, createBankHomeDashboard(bankSyncState, selectedPageSync)))
                .child(createPageContainer(
                    TerminalPage.BANK_ACCOUNT,
                    selectedPageSync,
                    createBankAccountDashboard(bankSyncState, bankSessionState, selectedPageSync)))
                .child(createPageContainer(
                    TerminalPage.BANK_TRANSFER,
                    selectedPageSync,
                    createBankTransferDashboard(bankSyncState, bankSessionState, selectedPageSync)))
                .child(createPageContainer(
                    TerminalPage.BANK_EXCHANGE,
                    selectedPageSync,
                    createBankExchangeDashboard(bankSyncState, selectedPageSync)))
                .child(createPageContainer(
                    TerminalPage.BANK_LEDGER,
                    selectedPageSync,
                    createBankLedgerDashboard(bankSyncState, selectedPageSync)));
        }

        private Flow createPageContainer(TerminalPage page, IntSyncValue selectedPageSync, IWidget body) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .collapseDisabledChild(true)
                .child(body)
                .setEnabledIf(widget -> selectedPageSync.getIntValue() == page.index);
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

        private IWidget createBankHomeDashboard(BankPageSyncState bankSyncState, IntSyncValue selectedPageSync) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(6)
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(76)
                        .childPadding(6)
                        .child(
                            createSectionShell(
                                "个人账户摘要",
                                0.52f,
                                76,
                                Flow.column()
                                    .widthRel(1f)
                                    .coverChildrenHeight()
                                    .childPadding(3)
                                    .child(createDataRow("个人余额", bankSyncState.playerBalance::getValue))
                                    .child(createDataRow("账户状态", bankSyncState.playerStatus::getValue))
                                    .child(createDataRow("最近更新", bankSyncState.playerUpdatedAt::getValue))))
                        .child(
                            createSectionShell(
                                "Exchange 公开摘要",
                                0.48f,
                                76,
                                Flow.column()
                                    .widthRel(1f)
                                    .coverChildrenHeight()
                                    .childPadding(3)
                                    .child(createDataRow("储备余额", bankSyncState.exchangeBalance::getValue))
                                    .child(createDataRow("公开状态", bankSyncState.exchangeStatus::getValue))
                                    .child(createDataRow("服务状态", bankSyncState.serviceState::getValue)))))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(86)
                        .childPadding(6)
                        .child(createSectionShell("银行菜单", 0.52f, 86, createBankMenuPanel(selectedPageSync)))
                        .child(
                            createSectionShell(
                                "当前说明",
                                0.48f,
                                86,
                                createBulletPanel(
                                    "银行页先展示真实摘要，再提供二级子页跳转。",
                                    "当前已接入当前玩家开户，以及面向已开户玩家的转账。",
                                    "exchange 储备与最近流水面向普通玩家公开透明。"))))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(100)
                        .childPadding(6)
                        .child(
                            createSectionShell(
                                "个人近期流水预览",
                                0.5f,
                                100,
                                createBulletPanel(getSyncValues(bankSyncState.playerLedgerLines, 3))))
                        .child(
                            createSectionShell(
                                "Exchange 公开流水预览",
                                0.5f,
                                100,
                                createBulletPanel(getSyncValues(bankSyncState.exchangeLedgerLines, 3)))));
        }

        private IWidget createBankAccountDashboard(BankPageSyncState bankSyncState,
            TerminalBankSessionState bankSessionState, IntSyncValue selectedPageSync) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(6)
                .child(createBankSubNavigation(selectedPageSync))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(122)
                        .childPadding(6)
                        .child(
                            createSectionShell(
                                "个人账户",
                                0.44f,
                                122,
                                Flow.column()
                                    .widthRel(1f)
                                    .coverChildrenHeight()
                                    .childPadding(3)
                                    .child(createDataRow("个人余额", bankSyncState.playerBalance::getValue))
                                    .child(createDataRow("账户状态", bankSyncState.playerStatus::getValue))
                                    .child(createDataRow("账户编号", bankSyncState.playerAccountNo::getValue))
                                    .child(createDataRow("最近更新", bankSyncState.playerUpdatedAt::getValue))))
                        .child(
                            createSectionShell(
                                "账户操作",
                                0.56f,
                                122,
                                Flow.column()
                                    .widthRel(1f)
                                    .coverChildrenHeight()
                                    .childPadding(4)
                                    .child(createDataRow("服务状态", bankSyncState.serviceState::getValue))
                                    .child(createDataRow("最近操作", bankSyncState.actionMessage::getValue))
                                    .child(createBankActionButton(
                                        "立即开户 / 复用现有账户",
                                        bankSessionState::canSubmitOpenAccountLocally,
                                        createOpenAccountSyncHandler(bankSessionState)))
                                    .child(
                                        new TextWidget<>("只处理当前登录玩家，不代办其他玩家账户")
                                            .widthRel(1f)
                                            .height(10)
                                            .color(TEXT_MUTED)))))
                .child(
                    createSectionShell(
                        "最近个人流水",
                        1f,
                        112,
                        createBulletPanel(getSyncValues(bankSyncState.playerLedgerLines, 4))));
        }

        private IWidget createBankTransferDashboard(BankPageSyncState bankSyncState,
            TerminalBankSessionState bankSessionState, IntSyncValue selectedPageSync) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(6)
                .child(createBankSubNavigation(selectedPageSync))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(108)
                        .childPadding(6)
                        .child(
                            createSectionShell(
                                "转账状态",
                                0.40f,
                                108,
                                Flow.column()
                                    .widthRel(1f)
                                    .coverChildrenHeight()
                                    .childPadding(3)
                                    .child(createDataRow("当前余额", bankSyncState.playerBalance::getValue))
                                    .child(createDataRow("账户状态", bankSyncState.playerStatus::getValue))
                                    .child(createDataRow("当前阶段", bankSyncState.transferState::getValue))))
                        .child(
                            createSectionShell(
                                "操作反馈",
                                0.60f,
                                108,
                                Flow.column()
                                    .widthRel(1f)
                                    .coverChildrenHeight()
                                    .childPadding(3)
                                    .child(createDataRow("服务状态", bankSyncState.serviceState::getValue))
                                    .child(createDataRow("最近操作", bankSyncState.actionMessage::getValue))
                                    .child(
                                        new TextWidget<>("支持已开户玩家转账，离线目标还需服务端能解析其身份")
                                            .widthRel(1f)
                                            .height(20)
                                            .color(TEXT_MUTED)))))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(128)
                        .childPadding(6)
                        .child(
                            createSectionShell(
                                "转账表单",
                                0.64f,
                                128,
                                Flow.column()
                                    .widthRel(1f)
                                    .coverChildrenHeight()
                                    .childPadding(4)
                                    .child(createBankInputRow(
                                        "收款玩家",
                                        createPlayerNameField(bankSyncState.transferTargetName)))
                                    .child(createBankInputRow(
                                        "转账金额",
                                        createAmountField(bankSyncState.transferAmountText)))
                                    .child(createBankInputRow(
                                        "备注说明",
                                        createCommentField(bankSyncState.transferComment)))
                                    .child(createBankActionButton(
                                        "提交玩家转账",
                                        bankSessionState::canSubmitTransferLocally,
                                        createTransferSyncHandler(bankSessionState)))))
                        .child(
                            createSectionShell(
                                "当前规则",
                                0.36f,
                                128,
                                createBulletPanel(
                                    "玩家转账一期默认不收手续费。",
                                    "转账双方都需先开户，离线目标还需服务端可识别。",
                                    "转账成功后会回写个人余额与最近流水。"))));
        }

        private IWidget createBankExchangeDashboard(BankPageSyncState bankSyncState, IntSyncValue selectedPageSync) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(6)
                .child(createBankSubNavigation(selectedPageSync))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(92)
                        .childPadding(6)
                        .child(
                            createSectionShell(
                                "Exchange 储备账户",
                                0.52f,
                                92,
                                Flow.column()
                                    .widthRel(1f)
                                    .coverChildrenHeight()
                                    .childPadding(3)
                                    .child(createDataRow("储备余额", bankSyncState.exchangeBalance::getValue))
                                    .child(createDataRow("公开状态", bankSyncState.exchangeStatus::getValue))
                                    .child(createDataRow("账户编号", bankSyncState.exchangeAccountNo::getValue))
                                    .child(createDataRow("最近更新", bankSyncState.exchangeUpdatedAt::getValue))))
                        .child(
                            createSectionShell(
                                "公开说明",
                                0.48f,
                                92,
                                createBulletPanel(
                                    "这不是管理后台页，而是普通玩家公开页。",
                                    "目标是让兑换储备余额和最近账本不是黑箱。",
                                    "当前只展示真实数据，不伪造金融产品和图表。"))))
                .child(
                    createSectionShell(
                        "Exchange 最近账本",
                        1f,
                        118,
                        createBulletPanel(getSyncValues(bankSyncState.exchangeLedgerLines, 4))));
        }

        private IWidget createBankLedgerDashboard(BankPageSyncState bankSyncState, IntSyncValue selectedPageSync) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(6)
                .child(createBankSubNavigation(selectedPageSync))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(78)
                        .childPadding(6)
                        .child(
                            createSectionShell(
                                "个人流水状态",
                                0.48f,
                                78,
                                Flow.column()
                                    .widthRel(1f)
                                    .coverChildrenHeight()
                                    .childPadding(3)
                                    .child(createDataRow("当前余额", bankSyncState.playerBalance::getValue))
                                    .child(createDataRow("账户编号", bankSyncState.playerAccountNo::getValue))
                                    .child(createDataRow("最近更新", bankSyncState.playerUpdatedAt::getValue))))
                        .child(
                            createSectionShell(
                                "读取说明",
                                0.52f,
                                78,
                                createBulletPanel(
                                    "这里只显示你自己的最近账本分录。",
                                    "若未开户或还没有交易，页面会明确给出空状态。",
                                    "后续可以继续补筛选、分页和交易详情入口。"))))
                .child(
                    createSectionShell(
                        "最近个人账本",
                        1f,
                        118,
                        createBulletPanel(getSyncValues(bankSyncState.playerLedgerLines, 4))));
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
            String[] columns = { "职业", "公共", "市场", "银行", "账本", "物流" };
            String[] rows = { "摘要", "任务", "订单", "广播" };
            boolean[][] active = {
                { true, true, true, true, false, false },
                { false, true, false, false, false, true },
                { false, true, true, true, true, false },
                { true, false, true, false, false, true }
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
                        .widthRel(0.14f)
                        .height(10)
                        .color(TEXT_SECONDARY))
                .child(createMatrixCellRow(activeCells, columnCount));
        }

        private IWidget createMatrixCellRow(boolean[] activeCells, int columnCount) {
            Flow row = Flow.row()
                .widthRel(0.86f)
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
                        .background(new Rectangle().color(active ? getEnabledCellColor() : CELL_DISABLED)));
        }

        private IWidget createLegendPanel() {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(3)
                .child(createLegendItem("接入", getEnabledCellColor()))
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

        private StringSyncValue registerLiveBankString(PanelSyncManager syncManager, String key, String[] clientCache,
            int index, Supplier<String> serverGetter) {
            StringSyncValue syncValue = new StringSyncValue(
                () -> getCachedBankValue(clientCache, index),
                value -> clientCache[index] = normalizeBankValue(value),
                serverGetter,
                null);
            syncManager.syncValue(key, syncValue);
            return syncValue;
        }

        private String getCachedBankValue(String[] clientCache, int index) {
            if (index < 0 || index >= clientCache.length) {
                return "";
            }
            return clientCache[index] == null ? "" : clientCache[index];
        }

        private String normalizeBankValue(String value) {
            return value == null ? "" : value;
        }

        private IWidget createBankInputRow(String label, IWidget inputWidget) {
            return Flow.row()
                .widthRel(1f)
                .height(20)
                .childPadding(6)
                .child(
                    new TextWidget<>(label)
                        .widthRel(0.26f)
                        .height(12)
                        .color(TEXT_MUTED))
                .child(
                    Flow.column()
                        .widthRel(0.74f)
                        .height(18)
                        .child(inputWidget));
        }

        private TextFieldWidget createPlayerNameField(StringSyncValue valueSync) {
            return new TextFieldWidget().widthRel(1f)
                .height(18)
                .value(valueSync)
                .setMaxLength(16)
                .setPattern(PLAYER_NAME_PATTERN)
                .acceptsExpressions(false)
                .autoUpdateOnChange(true)
                .hintText("玩家名，离线需已开户且可识别")
                .setTextColor(TEXT_PRIMARY)
                .hintColor(TEXT_MUTED);
        }

        private TextFieldWidget createAmountField(StringSyncValue valueSync) {
            return new TextFieldWidget().widthRel(1f)
                .height(18)
                .value(valueSync)
                .setMaxLength(24)
                .setNumbersLong(() -> 1L, () -> Long.MAX_VALUE)
                .acceptsExpressions(false)
                .autoUpdateOnChange(true)
                .hintText("正整数 STARCOIN")
                .setTextColor(TEXT_PRIMARY)
                .hintColor(TEXT_MUTED);
        }

        private TextFieldWidget createCommentField(StringSyncValue valueSync) {
            return new TextFieldWidget().widthRel(1f)
                .height(18)
                .value(valueSync)
                .setMaxLength(96)
                .acceptsExpressions(false)
                .autoUpdateOnChange(true)
                .hintText("转账备注，可留空")
                .setTextColor(TEXT_PRIMARY)
                .hintColor(TEXT_MUTED);
        }

        private ButtonWidget<?> createBankActionButton(String label, Supplier<Boolean> enabledSupplier,
            InteractionSyncHandler syncHandler) {
            return new ButtonWidget<>().widthRel(1f)
                .height(24)
                .background(new Rectangle().color(BG_BUTTON))
                .overlay(
                    IKey.dynamic(
                        () -> (enabledSupplier.get() ? EnumChatFormatting.AQUA + "> "
                            : EnumChatFormatting.DARK_GRAY + "x ") + label))
                .syncHandler(syncHandler)
                .setEnabledIf(widget -> enabledSupplier.get());
        }

        private InteractionSyncHandler createOpenAccountSyncHandler(TerminalBankSessionState bankSessionState) {
            return new InteractionSyncHandler().setOnMouseTapped(mouseData -> {
                if (mouseData.isClient() || mouseData.mouseButton != 0) {
                    return;
                }
                bankSessionState.submitOpenAccount();
            });
        }

        private InteractionSyncHandler createTransferSyncHandler(TerminalBankSessionState bankSessionState) {
            return new InteractionSyncHandler().setOnMouseTapped(mouseData -> {
                if (mouseData.isClient() || mouseData.mouseButton != 0) {
                    return;
                }
                bankSessionState.submitTransfer();
            });
        }

        private IWidget createBulletPanel(String... lines) {
            Flow column = Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(4);
            for (String line : lines) {
                column.child(createBulletLine(line));
            }
            return column;
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
                        .background(new Rectangle().color(getAccentColor())))
                .child(
                    new TextWidget<>(text)
                        .widthRel(1f)
                        .height(12)
                        .color(TEXT_PRIMARY));
        }

        private ModConfiguration getCurrentConfiguration() {
            return GalaxyBase.proxy == null ? null : GalaxyBase.proxy.getConfiguration();
        }

        private float getPanelWidthRatio() {
            ModConfiguration configuration = getCurrentConfiguration();
            return configuration == null ? DEFAULT_PANEL_WIDTH_RATIO : configuration.getTerminalPanelWidthRatio();
        }

        private float getPanelHeightRatio() {
            ModConfiguration configuration = getCurrentConfiguration();
            return configuration == null ? DEFAULT_PANEL_HEIGHT_RATIO : configuration.getTerminalPanelHeightRatio();
        }

        private float getNavigationWidthRatio() {
            ModConfiguration configuration = getCurrentConfiguration();
            return configuration == null ? DEFAULT_NAV_WIDTH_RATIO : configuration.getTerminalNavigationWidthRatio();
        }

        private float getContentWidthRatio() {
            return 1f - getNavigationWidthRatio();
        }

        private int getAccentColor() {
            ModConfiguration configuration = getCurrentConfiguration();
            return configuration == null ? DEFAULT_TEXT_ACCENT : rgb(configuration.getTerminalAccentColor());
        }

        private int getHighlightColor() {
            return lighten(getAccentColor(), 46);
        }

        private int getEnabledCellColor() {
            return lighten(getAccentColor(), 18);
        }

        private int rgb(int color) {
            return Color.rgb((color >> 16) & 255, (color >> 8) & 255, color & 255);
        }

        private int lighten(int color, int delta) {
            int red = Math.min(255, ((color >> 16) & 255) + delta);
            int green = Math.min(255, ((color >> 8) & 255) + delta);
            int blue = Math.min(255, (color & 255) + delta);
            return Color.rgb(red, green, blue);
        }

        private IWidget createPageStatePanel(TerminalPage page) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(4)
                .child(createInfoLine("当前页", page.label))
                .child(createInfoLine("显示模式", page.isBankPage() ? "银行嵌套页" : "制度控制台"))
                .child(createInfoLine("交互阶段", page.isBankPage() ? "真实只读快照" : "只读页面壳"));
        }

        private IWidget createBankMenuPanel(IntSyncValue selectedPageSync) {
            return Flow.column()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(4)
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(22)
                        .childPadding(4)
                        .child(createBankRouteButton("个人账户", 0.5f, TerminalPage.BANK_ACCOUNT, selectedPageSync))
                        .child(createBankRouteButton("转账服务", 0.5f, TerminalPage.BANK_TRANSFER, selectedPageSync)))
                .child(
                    Flow.row()
                        .widthRel(1f)
                        .height(22)
                        .childPadding(4)
                        .child(createBankRouteButton("Exchange 公开页", 0.5f, TerminalPage.BANK_EXCHANGE, selectedPageSync))
                        .child(createBankRouteButton("个人流水", 0.5f, TerminalPage.BANK_LEDGER, selectedPageSync)));
        }

        private IWidget createBankSubNavigation(IntSyncValue selectedPageSync) {
            return createSectionShell(
                "银行子页导航",
                1f,
                48,
                Flow.row()
                    .widthRel(1f)
                    .height(24)
                    .childPadding(4)
                    .child(createBankRouteButton("银行主页", 0.18f, TerminalPage.BANK, selectedPageSync))
                    .child(createBankRouteButton("个人账户", 0.18f, TerminalPage.BANK_ACCOUNT, selectedPageSync))
                    .child(createBankRouteButton("转账服务", 0.18f, TerminalPage.BANK_TRANSFER, selectedPageSync))
                    .child(createBankRouteButton("Exchange 公开", 0.22f, TerminalPage.BANK_EXCHANGE, selectedPageSync))
                    .child(createBankRouteButton("个人流水", 0.20f, TerminalPage.BANK_LEDGER, selectedPageSync)));
        }

        private ButtonWidget<?> createBankRouteButton(String label, float widthRatio, TerminalPage targetPage,
            IntSyncValue selectedPageSync) {
            return new ButtonWidget<>().widthRel(widthRatio)
                .height(22)
                .background(new Rectangle().color(BG_BUTTON))
                .overlay(
                    IKey.dynamic(
                        () -> (selectedPageSync.getIntValue() == targetPage.index ? EnumChatFormatting.AQUA + "> "
                            : EnumChatFormatting.GRAY + "  ") + label))
                .onMousePressed(mouseButton -> {
                    selectedPageSync.setValue(targetPage.index);
                    return true;
                });
        }

        private boolean isNavigationSelected(TerminalPage page, int selectedPageIndex) {
            TerminalPage selectedPage = TerminalPage.byIndex(selectedPageIndex);
            return page == TerminalPage.BANK ? selectedPage.isBankPage() : selectedPage == page;
        }

        private String[] getSyncValues(StringSyncValue[] syncValues, int limit) {
            String[] values = new String[limit];
            for (int i = 0; i < limit; i++) {
                values[i] = i < syncValues.length && syncValues[i] != null ? syncValues[i].getValue() : "";
            }
            return values;
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
            MARKET(3, "市场", "市场页", "市场与订单", "价格摘要与订单入口"),
            BANK(4, "银行", "银行页", "银河银行", "个人账户、公开储备与子页入口"),
            BANK_ACCOUNT(5, "账户", "账户页", "个人账户信息", "余额、账户状态与开户策略"),
            BANK_TRANSFER(6, "转账", "转账页", "转账服务", "转账规则与后续正式操作入口"),
            BANK_EXCHANGE(7, "exchange", "公开页", "Exchange 公开页", "兑换储备余额与近期公开账本"),
            BANK_LEDGER(8, "流水", "流水页", "个人流水", "个人最近账本变化与空状态提示");

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

            private boolean isBankPage() {
                return this == BANK || this == BANK_ACCOUNT || this == BANK_TRANSFER || this == BANK_EXCHANGE
                    || this == BANK_LEDGER;
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

        private static final class TerminalBankSessionState {

            private final EntityPlayer player;
            private TerminalBankSnapshot currentSnapshot;
            private String actionMessage = DEFAULT_BANK_ACTION_MESSAGE;
            private String toastMessage = "";
            private long toastExpireAtMillis = 0L;
            private String transferTargetName = "";
            private String transferAmountText = "";
            private String transferComment = "";
            private boolean snapshotDirty = true;

            private TerminalBankSessionState(EntityPlayer player) {
                this.player = player;
            }

            private TerminalBankSnapshot getCurrentSnapshot() {
                if (snapshotDirty || currentSnapshot == null) {
                    currentSnapshot = TerminalBankSnapshotProvider.INSTANCE.create(player);
                    snapshotDirty = false;
                }
                return currentSnapshot;
            }

            private String getActionMessage() {
                return actionMessage;
            }

            private void setActionMessage(String actionMessage) {
                this.actionMessage = normalizeMessage(actionMessage);
                publishToast(actionMessage);
            }

            private String getToastMessage() {
                if (toastExpireAtMillis > 0L && System.currentTimeMillis() >= toastExpireAtMillis) {
                    toastMessage = "";
                    toastExpireAtMillis = 0L;
                }
                return toastMessage;
            }

            private void setToastMessage(String toastMessage) {
                publishToast(toastMessage);
            }

            private String getTransferTargetName() {
                return transferTargetName;
            }

            private void setTransferTargetName(String transferTargetName) {
                this.transferTargetName = sanitizePlayerName(transferTargetName);
            }

            private String getTransferAmountText() {
                return transferAmountText;
            }

            private void setTransferAmountText(String transferAmountText) {
                this.transferAmountText = sanitizeAmount(transferAmountText);
            }

            private String getTransferComment() {
                return transferComment;
            }

            private void setTransferComment(String transferComment) {
                this.transferComment = sanitizeComment(transferComment);
            }

            private boolean canSubmitOpenAccountLocally() {
                return player != null;
            }

            private boolean canSubmitTransferLocally() {
                return player != null && !transferTargetName.isEmpty() && parseTransferAmount() > 0L;
            }

            private void submitOpenAccount() {
                TerminalBankingService.ActionResult result = TerminalBankingService.INSTANCE.openOwnAccount(player);
                setActionMessage(result.getMessage());
                invalidateSnapshot();
            }

            private void submitTransfer() {
                long amount = parseTransferAmount();
                if (transferTargetName.isEmpty()) {
                    setActionMessage(EnumChatFormatting.RED + "请填写收款玩家名");
                    return;
                }
                if (amount <= 0L) {
                    setActionMessage(EnumChatFormatting.RED + "请填写大于 0 的转账金额");
                    return;
                }

                TerminalBankingService.ActionResult result = TerminalBankingService.INSTANCE.transferToPlayer(
                    player,
                    transferTargetName,
                    amount,
                    transferComment);
                setActionMessage(result.getMessage());
                if (result.isSuccess()) {
                    transferAmountText = "";
                    transferComment = "";
                }
                invalidateSnapshot();
            }

            private void invalidateSnapshot() {
                snapshotDirty = true;
            }

            private long parseTransferAmount() {
                if (transferAmountText == null || transferAmountText.trim().isEmpty()) {
                    return 0L;
                }
                try {
                    return Long.parseLong(transferAmountText.trim());
                } catch (NumberFormatException ignored) {
                    return 0L;
                }
            }

            private String sanitizePlayerName(String value) {
                if (value == null) {
                    return "";
                }
                String trimmed = value.trim();
                return PLAYER_NAME_PATTERN.matcher(trimmed).matches() ? trimmed : "";
            }

            private String sanitizeAmount(String value) {
                if (value == null) {
                    return "";
                }
                String trimmed = value.trim();
                StringBuilder digits = new StringBuilder(trimmed.length());
                for (int i = 0; i < trimmed.length(); i++) {
                    char current = trimmed.charAt(i);
                    if (current >= '0' && current <= '9') {
                        digits.append(current);
                    }
                }
                return digits.toString();
            }

            private String sanitizeComment(String value) {
                if (value == null) {
                    return "";
                }
                String trimmed = value.trim();
                return trimmed.length() > 96 ? trimmed.substring(0, 96) : trimmed;
            }

            private void publishToast(String value) {
                String normalized = normalizeToastMessage(value);
                this.toastMessage = normalized;
                this.toastExpireAtMillis = normalized.isEmpty() ? 0L : System.currentTimeMillis() + BANK_TOAST_DURATION_MILLIS;
            }

            private String normalizeToastMessage(String value) {
                if (value == null) {
                    return "";
                }
                String trimmed = value.trim();
                if (trimmed.isEmpty() || DEFAULT_BANK_ACTION_MESSAGE.equals(stripFormatting(trimmed))) {
                    return "";
                }
                return trimmed;
            }

            private String normalizeMessage(String value) {
                return value == null || value.trim().isEmpty() ? DEFAULT_BANK_ACTION_MESSAGE : value;
            }
        }

        private static final class BankPageSyncState {

            private final StringSyncValue serviceState;
            private final StringSyncValue playerBalance;
            private final StringSyncValue playerStatus;
            private final StringSyncValue playerAccountNo;
            private final StringSyncValue playerUpdatedAt;
            private final StringSyncValue transferState;
            private final StringSyncValue[] playerLedgerLines;
            private final StringSyncValue exchangeBalance;
            private final StringSyncValue exchangeStatus;
            private final StringSyncValue exchangeAccountNo;
            private final StringSyncValue exchangeUpdatedAt;
            private final StringSyncValue[] exchangeLedgerLines;
            private final StringSyncValue actionMessage;
            private final StringSyncValue toastMessage;
            private final StringSyncValue transferTargetName;
            private final StringSyncValue transferAmountText;
            private final StringSyncValue transferComment;

            private BankPageSyncState(StringSyncValue serviceState, StringSyncValue playerBalance,
                StringSyncValue playerStatus, StringSyncValue playerAccountNo, StringSyncValue playerUpdatedAt,
                StringSyncValue transferState, StringSyncValue[] playerLedgerLines, StringSyncValue exchangeBalance,
                StringSyncValue exchangeStatus, StringSyncValue exchangeAccountNo, StringSyncValue exchangeUpdatedAt,
                StringSyncValue[] exchangeLedgerLines, StringSyncValue actionMessage, StringSyncValue toastMessage,
                StringSyncValue transferTargetName, StringSyncValue transferAmountText,
                StringSyncValue transferComment) {
                this.serviceState = serviceState;
                this.playerBalance = playerBalance;
                this.playerStatus = playerStatus;
                this.playerAccountNo = playerAccountNo;
                this.playerUpdatedAt = playerUpdatedAt;
                this.transferState = transferState;
                this.playerLedgerLines = playerLedgerLines;
                this.exchangeBalance = exchangeBalance;
                this.exchangeStatus = exchangeStatus;
                this.exchangeAccountNo = exchangeAccountNo;
                this.exchangeUpdatedAt = exchangeUpdatedAt;
                this.exchangeLedgerLines = exchangeLedgerLines;
                this.actionMessage = actionMessage;
                this.toastMessage = toastMessage;
                this.transferTargetName = transferTargetName;
                this.transferAmountText = transferAmountText;
                this.transferComment = transferComment;
            }
        }
    }
}
package com.jsirgalaxybase.terminal.client.component;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.framework.VerticalScrollPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

public final class TerminalServerToolsSection extends PanelContainer {

    private static final ResourceLocation CLICK_SOUND = new ResourceLocation("gui.button.press");

    private static final int CARD_GAP = 6;
    private static final int CARD_PADDING = 8;
    private static final int HEADER_HEIGHT = 12;
    private static final int ENTRY_HEIGHT = 48;
    private static final int NARROW_STACK_THRESHOLD = 300;
    private static final int DETAIL_BLOCK_HEIGHT = 82;
    private static final int RECENT_BLOCK_HEIGHT = 94;
    private static final int FEEDBACK_HEIGHT = 32;
    private static final int WARNING_HEIGHT = 28;

    public interface ActionHandler {

        void selectWarp(String warpName);

        void confirmWarp();
    }

    private final TerminalPanelFactory panels;
    private final TerminalServerToolsSectionModel model;
    private final TerminalServerToolsSectionState state;
    private final ActionHandler actionHandler;

    private final TexturedCanvasPanel warpListCard;
    private final TexturedCanvasPanel workspaceCard;
    private final VerticalScrollPanel warpListScroll;
    private final VerticalScrollPanel workspaceScroll;
    private final LabelPanel warpListHeaderLabel;
    private final LabelPanel warpListCountLabel;
    private final LabelPanel warpListDirectoryLabel;
    private final LabelPanel warpListFootnoteLabel;

    private final DetailBlockPanel detailBlockPanel;
    private final RecentBlockPanel recentBlockPanel;
    private final ActionFeedbackPanel feedbackPanel;
    private final WarningPanel warningPanel;
    private final ButtonPanel confirmWarpButton;

    public TerminalServerToolsSection(TerminalPanelFactory panels, TerminalServerToolsSectionModel model,
        TerminalServerToolsSectionState state, ActionHandler actionHandler) {
        this.panels = panels;
        this.model = model == null ? TerminalServerToolsSectionModel.placeholder() : model;
        this.state = state == null ? new TerminalServerToolsSectionState() : state;
        this.actionHandler = actionHandler;

        this.warpListCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        this.workspaceCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        this.warpListScroll = panels.createScrollPanel(new GuiRect(0, 0, 0, 0), 0, 5);
        this.workspaceScroll = panels.createScrollPanel(new GuiRect(0, 0, 0, 0), 0, 6);
        this.warpListHeaderLabel = headerLabel("可用传送点");
        this.warpListCountLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return countAvailableWarpItems() + "/" + TerminalServerToolsSection.this.model.getWarpNames().size();
            }
        }, ThemeColorKey.TEXT_SECONDARY, false);
        this.warpListDirectoryLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return buildServerDirectorySummary();
            }
        }, ThemeColorKey.TEXT_SECONDARY, false);
        this.warpListFootnoteLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return "列表每 30 秒自动刷新";
            }
        }, ThemeColorKey.TEXT_SECONDARY, false);

        this.detailBlockPanel = new DetailBlockPanel();
        this.recentBlockPanel = new RecentBlockPanel();
        this.feedbackPanel = new ActionFeedbackPanel();
        this.warningPanel = new WarningPanel();
        this.confirmWarpButton = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return TerminalServerToolsSection.this.model.isSelectedWarpEnabled() ? "确认传送" : "传送不可用";
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (TerminalServerToolsSection.this.actionHandler != null) {
                    TerminalServerToolsSection.this.actionHandler.confirmWarp();
                }
            }
        }, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return Boolean.valueOf(TerminalServerToolsSection.this.model.isSelectedWarpEnabled()
                    && TerminalServerToolsSection.this.state.hasSelectedWarp());
            }
        });

        addChild(warpListCard);
        addChild(workspaceCard);
        configureWarpListCard();
        configureWorkspaceCard();
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        int totalWidth = Math.max(220, bounds.getWidth());
        int totalHeight = Math.max(1, bounds.getHeight());
        int x = bounds.getX();
        int y = bounds.getY();

        if (totalWidth < NARROW_STACK_THRESHOLD) {
            layoutNarrow(x, y, totalWidth, totalHeight);
            return;
        }

        int leftWidth = Math.max(146, Math.min(230, Math.round(totalWidth * 0.34F)));
        int rightWidth = Math.max(150, totalWidth - leftWidth - CARD_GAP);
        warpListCard.setBounds(new GuiRect(x, y, leftWidth, totalHeight));
        workspaceCard.setBounds(new GuiRect(x + leftWidth + CARD_GAP, y, rightWidth, totalHeight));
        layoutCards();
    }

    private void layoutNarrow(int x, int y, int width, int height) {
        int safeHeight = Math.max(1, height);
        int minWorkspaceHeight = Math.min(80, Math.max(0, safeHeight - CARD_GAP));
        int preferredLeftHeight = Math.max(92, Math.round(safeHeight * 0.46F));
        int maxLeftHeight = Math.max(1, safeHeight - CARD_GAP - minWorkspaceHeight);
        int leftHeight = Math.min(preferredLeftHeight, maxLeftHeight);
        warpListCard.setBounds(new GuiRect(x, y, width, leftHeight));
        workspaceCard.setBounds(new GuiRect(x, y + leftHeight + CARD_GAP, width, Math.max(0, safeHeight - leftHeight - CARD_GAP)));
        layoutCards();
    }

    private void configureWarpListCard() {
        warpListCard.addChild(warpListHeaderLabel);
        warpListCard.addChild(warpListCountLabel);
        warpListCard.addChild(warpListDirectoryLabel);
        warpListCard.addChild(warpListScroll);
        warpListCard.addChild(warpListFootnoteLabel);

        int count = Math.max(model.getWarpNames().size(),
            Math.max(model.getWarpLines().size(), Math.max(model.getWarpSubtitles().size(), model.getWarpStateLabels().size())));
        for (int index = 0; index < count; index++) {
            final String warpName = getListValue(model.getWarpNames(), index);
            final String title = getListValue(model.getWarpLines(), index);
            final String subtitle = getListValue(model.getWarpSubtitles(), index);
            final String stateLabel = getListValue(model.getWarpStateLabels(), index);
            warpListScroll.addScrollableChild(new WarpEntryPanel(warpName, title, subtitle, stateLabel), ENTRY_HEIGHT);
        }
    }

    private void configureWorkspaceCard() {
        workspaceScroll.addScrollableChild(detailBlockPanel, DETAIL_BLOCK_HEIGHT);
        workspaceScroll.addScrollableChild(recentBlockPanel, RECENT_BLOCK_HEIGHT);
        workspaceScroll.addScrollableChild(feedbackPanel, FEEDBACK_HEIGHT);
        workspaceScroll.addScrollableChild(warningPanel, WARNING_HEIGHT);
        workspaceScroll.addScrollableChild(confirmWarpButton, TerminalLayoutMetrics.BUTTON_HEIGHT);
        workspaceCard.addChild(workspaceScroll);
    }

    private void layoutCards() {
        layoutWarpListCard();
        layoutWorkspaceCard();
    }

    private void layoutWarpListCard() {
        GuiRect bounds = warpListCard.getBounds();
        int headerY = bounds.getY() + 8;
        warpListHeaderLabel.setBounds(new GuiRect(bounds.getX() + CARD_PADDING, headerY, bounds.getWidth() - 88, HEADER_HEIGHT));
        warpListCountLabel.setBounds(new GuiRect(bounds.getRight() - 64, headerY, 56, HEADER_HEIGHT));
        warpListDirectoryLabel.setBounds(new GuiRect(
            bounds.getX() + CARD_PADDING,
            bounds.getY() + 20,
            bounds.getWidth() - CARD_PADDING * 2,
            10));
        boolean showFootnote = bounds.getHeight() >= 72;
        int footerHeight = showFootnote ? 14 : 0;
        int listY = bounds.getY() + 34;
        int listBottom = bounds.getBottom() - CARD_PADDING - footerHeight;
        warpListScroll.setBounds(new GuiRect(
            bounds.getX() + CARD_PADDING,
            listY,
            bounds.getWidth() - CARD_PADDING * 2,
            Math.max(0, listBottom - listY)));
        warpListFootnoteLabel.setVisible(showFootnote);
        warpListFootnoteLabel.setBounds(new GuiRect(
            bounds.getX() + CARD_PADDING,
            bounds.getBottom() - 15,
            bounds.getWidth() - CARD_PADDING * 2,
            10));
    }

    private void layoutWorkspaceCard() {
        GuiRect bounds = workspaceCard.getBounds();
        int innerX = bounds.getX() + CARD_PADDING;
        int innerWidth = bounds.getWidth() - CARD_PADDING * 2;
        workspaceScroll.setBounds(new GuiRect(
            innerX,
            bounds.getY() + CARD_PADDING,
            Math.max(0, innerWidth),
            Math.max(0, bounds.getHeight() - CARD_PADDING * 2)));
    }

    private int countAvailableWarpItems() {
        int count = 0;
        for (String stateLabel : model.getWarpStateLabels()) {
            if ("可用".equals(stateLabel)) {
                count++;
            }
        }
        return count;
    }

    private int countRecentTransferItems() {
        int count = 0;
        for (String line : model.getRecentTransferLines()) {
            if (line != null && !line.trim().isEmpty() && !"当前没有最近传送记录。".equals(line.trim())) {
                count++;
            }
        }
        return count;
    }

    private LabelPanel headerLabel(final String text) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return text;
            }
        }, ThemeColorKey.TEXT_PRIMARY, false);
    }

    private static String getListValue(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return "";
        }
        String value = values.get(index);
        return value == null ? "" : value.trim();
    }

    private String compactServerDisplay(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty() || "--".equals(value)) {
            return "--";
        }
        String normalized = value.toLowerCase();
        if (normalized.contains("lobby")) {
            return "Lobby";
        }
        int sIndex = normalized.lastIndexOf("_s");
        if (sIndex >= 0 && sIndex + 2 < normalized.length()) {
            String suffix = normalized.substring(sIndex + 2).replaceAll("[^0-9].*$", "");
            if (!suffix.isEmpty()) {
                return "S" + suffix;
            }
        }
        String compact = value
            .replace("galaxy_gtnh284_", "")
            .replace("galaxy_gtnh_", "")
            .replace("galaxy_", "")
            .replace('_', ' ')
            .trim();
        return compact.isEmpty() ? value : compact;
    }

    private String compactRouteDisplay(String source, String target) {
        return compactServerDisplay(source) + " -> " + compactServerDisplay(target);
    }

    private String buildServerDirectorySummary() {
        List<String> serverIds = model.getServerIds();
        List<String> compactIds = new java.util.ArrayList<String>();
        for (String serverId : serverIds) {
            String compact = compactServerDisplay(serverId);
            if (!compact.isEmpty() && !"--".equals(compact) && !compactIds.contains(compact)) {
                compactIds.add(compact);
            }
        }
        String current = compactServerDisplay(model.getCurrentServerId());
        if (compactIds.isEmpty()) {
            return "当前 " + current + " | 目录不可用";
        }
        if (compactIds.size() <= 2) {
            return "当前 " + current + " | 目录 " + joinWithSlash(compactIds);
        }
        return "当前 " + current + " | 目录 " + compactIds.size() + " 服";
    }

    private String buildRecentHistoryPreview() {
        List<String> lines = model.getRecentTransferLines();
        if (lines == null || lines.size() <= 1) {
            return "当前没有更多历史记录。";
        }
        List<String> previews = new java.util.ArrayList<String>();
        for (int index = 1; index < lines.size() && previews.size() < 2; index++) {
            String preview = compactTicketPreview(lines.get(index));
            if (!preview.isEmpty()) {
                previews.add(preview);
            }
        }
        return previews.isEmpty() ? "当前没有更多历史记录。" : joinWithSlash(previews);
    }

    private String compactTicketPreview(String rawLine) {
        if (rawLine == null) {
            return "";
        }
        String line = rawLine.trim();
        if (line.isEmpty() || "当前没有最近传送记录。".equals(line)) {
            return "";
        }
        String[] parts = line.split("\\|");
        if (parts.length < 3) {
            return line;
        }
        String time = parts[0].trim();
        String route = compactRouteValue(parts[1].trim());
        String status = compactStatusValue(parts[2].trim());
        return route + " " + time + " " + status;
    }

    private String compactRouteValue(String rawRoute) {
        if (rawRoute == null || rawRoute.trim().isEmpty()) {
            return "--";
        }
        String[] routeParts = rawRoute.split("->");
        if (routeParts.length != 2) {
            return rawRoute.trim();
        }
        return compactRouteDisplay(routeParts[0].trim(), routeParts[1].trim());
    }

    private String compactStatusValue(String rawStatus) {
        String value = rawStatus == null ? "" : rawStatus.trim();
        if ("COMPLETED".equalsIgnoreCase(value)) {
            return "完成";
        }
        if ("FAILED".equalsIgnoreCase(value)) {
            return "失败";
        }
        if ("EXPIRED".equalsIgnoreCase(value)) {
            return "过期";
        }
        if ("DISPATCHED".equalsIgnoreCase(value)) {
            return "派发中";
        }
        if ("PENDING_GATEWAY".equalsIgnoreCase(value)) {
            return "待网关";
        }
        return value;
    }

    private String joinWithSlash(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(" / ");
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    private String compactWarpTitle(String rawTitle, String warpName) {
        String value = rawTitle == null ? "" : rawTitle.trim();
        if (value.isEmpty()) {
            return warpName == null ? "" : warpName;
        }
        value = value.replace("[可用]", "").replace("[不可用]", "").trim();
        int slashIndex = value.indexOf('/');
        if (slashIndex > 0) {
            value = value.substring(0, slashIndex).trim();
        }
        return value.isEmpty() ? (warpName == null ? "" : warpName) : value;
    }

    private String compactWarpSubtitle(String rawSubtitle) {
        String value = rawSubtitle == null ? "" : rawSubtitle.trim();
        if (value.isEmpty()) {
            return "目标信息暂不可用。";
        }
        value = value.replace("Gray rollout test warp back to ", "")
            .replace("Gray rollout test warp to ", "")
            .replace("spawn area", "spawn")
            .replace("login area", "login")
            .replace("Gray rollout test ", "")
            .trim();
        return value.isEmpty() ? rawSubtitle.trim() : value;
    }

    private String compactWarpDescription(String rawDescription) {
        String value = rawDescription == null ? "" : rawDescription.trim();
        if (value.isEmpty()) {
            return "当前没有额外传送说明。";
        }
        value = value.replace("Gray rollout test warp back to Lobby login area.", "返回 Lobby login。")
            .replace("Gray rollout test warp back to Lobby login area", "返回 Lobby login")
            .replace("Gray rollout test warp to S2 spawn area.", "前往 S2 spawn。")
            .replace("Gray rollout test warp to S2 spawn area", "前往 S2 spawn")
            .replace("Gray rollout test ", "")
            .trim();
        return value.isEmpty() ? rawDescription.trim() : value;
    }

    private String compactRecentSummary(String rawSummary) {
        String value = rawSummary == null ? "" : rawSummary.trim();
        if (value.isEmpty()) {
            return "当前没有最近传送记录。";
        }
        String normalized = value.toLowerCase();
        if (normalized.contains("target restore completed")) {
            return "目标服恢复完成。";
        }
        if (normalized.contains("proxy dispatch requested")) {
            return "已发往目标服。";
        }
        if (normalized.contains("dispatch")) {
            return "传送派发处理中。";
        }
        if (normalized.contains("completed")) {
            return "传送已完成。";
        }
        if (normalized.contains("failed")) {
            return "传送失败。";
        }
        return value;
    }

    private TerminalNotificationSeverity resolveRecentSeverity() {
        String status = model.getRecentTransferStatus() == null ? "" : model.getRecentTransferStatus().toLowerCase();
        if (status.contains("complete") || status.contains("success") || status.contains("成功")) {
            return TerminalNotificationSeverity.SUCCESS;
        }
        if (status.contains("fail") || status.contains("error") || status.contains("失败")) {
            return TerminalNotificationSeverity.ERROR;
        }
        if (status.contains("expire") || status.contains("warning") || status.contains("超时")) {
            return TerminalNotificationSeverity.WARNING;
        }
        return TerminalNotificationSeverity.INFO;
    }

    private final class DetailBlockPanel extends PanelContainer {

        private final LabelPanel headerLabel;
        private final DetailRowPanel currentServerRow;
        private final DetailRowPanel targetServerRow;
        private final DetailRowPanel warpNameRow;
        private final DetailRowPanel descriptionRow;

        private DetailBlockPanel() {
            this.headerLabel = headerLabel("传送详情");
            this.currentServerRow = new DetailRowPanel("当前服务器", new Supplier<String>() {
                @Override
                public String get() {
                    return compactServerDisplay(TerminalServerToolsSection.this.model.getCurrentServerId());
                }
            }, 0xFF76879D, false);
            this.targetServerRow = new DetailRowPanel("目标服务器", new Supplier<String>() {
                @Override
                public String get() {
                    return compactServerDisplay(TerminalServerToolsSection.this.model.getSelectedTargetServerId());
                }
            }, 0xFF65B95A, false);
            this.warpNameRow = new DetailRowPanel("传送点名称", new Supplier<String>() {
                @Override
                public String get() {
                    return compactWarpTitle(TerminalServerToolsSection.this.model.getSelectedWarpTitle(),
                        TerminalServerToolsSection.this.model.getSelectedWarpName());
                }
            }, 0xFF4F86D8, false);
            this.descriptionRow = new DetailRowPanel("传送说明", new Supplier<String>() {
                @Override
                public String get() {
                    return compactWarpDescription(TerminalServerToolsSection.this.model.getSelectedWarpDescription());
                }
            }, 0xFFA5AEB9, true);
            addChild(headerLabel);
            addChild(currentServerRow);
            addChild(targetServerRow);
            addChild(warpNameRow);
            addChild(descriptionRow);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            GuiRect blockBounds = getBounds();
            int innerX = blockBounds.getX() + 6;
            int innerWidth = blockBounds.getWidth() - 12;
            headerLabel.setBounds(new GuiRect(innerX, blockBounds.getY() + 6, innerWidth, HEADER_HEIGHT));
            int rowY = blockBounds.getY() + 22;
            boolean narrow = innerWidth < 205;
            int compactRowHeight = narrow ? 18 : 16;
            int compactGap = 1;
            currentServerRow.setBounds(new GuiRect(innerX, rowY, innerWidth, compactRowHeight));
            targetServerRow.setBounds(new GuiRect(innerX, rowY + compactRowHeight + compactGap, innerWidth, compactRowHeight));
            warpNameRow.setBounds(new GuiRect(innerX, rowY + (compactRowHeight + compactGap) * 2, innerWidth, compactRowHeight));
            descriptionRow.setBounds(new GuiRect(innerX, rowY + (compactRowHeight + compactGap) * 3, innerWidth, 18));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), 0xFF25313E);
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getRight() - 1, bounds.getBottom() - 1, 0xFF16212C);
        }
    }

    private final class RecentBlockPanel extends PanelContainer {

        private final LabelPanel headerLabel;
        private final LabelPanel historyCountLabel;
        private final RecentStatusPanel recentStatusPanel;
        private final LabelPanel historyPreviewLabel;

        private RecentBlockPanel() {
            this.headerLabel = headerLabel("最近传送状态");
            this.historyCountLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    int count = countRecentTransferItems();
                    return count <= 0 ? "暂无记录" : "最近 " + count + " 条";
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            this.recentStatusPanel = new RecentStatusPanel();
            this.historyPreviewLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return buildRecentHistoryPreview();
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            addChild(headerLabel);
            addChild(historyCountLabel);
            addChild(recentStatusPanel);
            addChild(historyPreviewLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            GuiRect blockBounds = getBounds();
            int innerX = blockBounds.getX() + 6;
            int innerWidth = blockBounds.getWidth() - 12;
            headerLabel.setBounds(new GuiRect(innerX, blockBounds.getY() + 6, innerWidth - 56, HEADER_HEIGHT));
            historyCountLabel.setBounds(new GuiRect(blockBounds.getRight() - 54, blockBounds.getY() + 6, 48, HEADER_HEIGHT));
            recentStatusPanel.setBounds(new GuiRect(innerX, blockBounds.getY() + 22, innerWidth, Math.max(24, blockBounds.getHeight() - 42)));
            historyPreviewLabel.setBounds(new GuiRect(innerX, blockBounds.getBottom() - 14, innerWidth, 10));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), 0xFF25313E);
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getRight() - 1, bounds.getBottom() - 1, 0xFF16212C);
        }
    }

    private final class WarpEntryPanel extends PanelContainer {

        private final String warpName;
        private final LabelPanel titleLabel;
        private final LabelPanel subtitleLabel;
        private final LabelPanel statusLabel;
        private final boolean enabled;
        private boolean pressed;

        private WarpEntryPanel(final String warpName, final String title, final String subtitle, final String stateLabel) {
            this.warpName = warpName == null ? "" : warpName.trim();
            this.enabled = !this.warpName.isEmpty() && "可用".equals(stateLabel);
            this.titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return compactWarpTitle(title, WarpEntryPanel.this.warpName);
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            this.subtitleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return compactWarpSubtitle(subtitle);
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            this.statusLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return stateLabel == null || stateLabel.trim().isEmpty() ? "不可用" : stateLabel.trim();
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            addChild(titleLabel);
            addChild(subtitleLabel);
            addChild(statusLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            GuiRect itemBounds = getBounds();
            int iconSpace = itemBounds.getWidth() < 170 ? 22 : 30;
            int rightWidth = itemBounds.getWidth() < 170 ? 12 : 40;
            int textX = itemBounds.getX() + iconSpace + 10;
            int textWidth = Math.max(40, itemBounds.getWidth() - iconSpace - rightWidth - 18);
            titleLabel.setBounds(new GuiRect(textX, itemBounds.getY() + 6, textWidth, 10));
            subtitleLabel.setBounds(new GuiRect(textX, itemBounds.getY() + 18, textWidth, 18));
            statusLabel.setBounds(new GuiRect(itemBounds.getRight() - rightWidth, itemBounds.getY() + 12, Math.max(0, rightWidth - 6), 10));
            statusLabel.setVisible(itemBounds.getWidth() >= 190);
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            boolean selected = warpName.equals(model.getSelectedWarpName()) || warpName.equals(state.getSelectedWarpName());
            boolean hovered = contains(mouseX, mouseY) && enabled;
            int borderColor = selected ? 0xFF529BED : 0xFF1A1E26;
            int fillColor = !enabled ? 0xFF12161C : selected ? 0xFF18283B : hovered ? 0xFF152232 : 0xFF10161D;
            Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), borderColor);
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getRight() - 1, bounds.getBottom() - 1, fillColor);
            if (selected) {
                Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getX() + 4, bounds.getBottom() - 1, 0xFF68A7F0);
            }

            int iconX = bounds.getX() + 8;
            int iconY = bounds.getY() + 10;
            Gui.drawRect(iconX, iconY, iconX + 14, iconY + 14, 0xFF1B4D93);
            Gui.drawRect(iconX + 2, iconY + 2, iconX + 12, iconY + 12, 0xFF245FB4);
            Gui.drawRect(iconX + 4, iconY + 4, iconX + 10, iconY + 10, 0xFF0F2038);

            int stateColor = enabled ? 0xFF6AD46F : 0xFF5E6670;
            int dotX = bounds.getRight() - 16;
            int dotY = bounds.getY() + bounds.getHeight() / 2 - 3;
            Gui.drawRect(dotX, dotY, dotX + 6, dotY + 6, stateColor);
        }

        @Override
        public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            if (!enabled) {
                return false;
            }
            pressed = mouseButton == 0 && contains(mouseX, mouseY);
            return pressed;
        }

        @Override
        public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            boolean shouldClick = pressed && mouseButton == 0 && contains(mouseX, mouseY) && enabled;
            pressed = false;
            if (!shouldClick) {
                return false;
            }
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null) {
                minecraft.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(CLICK_SOUND, 1.0F));
            }
            state.setSelectedWarpName(warpName);
            if (actionHandler != null) {
                actionHandler.selectWarp(warpName);
            }
            return true;
        }
    }

    private final class DetailRowPanel extends PanelContainer {

        private final LabelPanel titleLabel;
        private final LabelPanel valueLabel;
        private final int iconColor;
        private final boolean multiline;

        private DetailRowPanel(final String title, Supplier<String> valueSupplier, int iconColor, boolean multiline) {
            this.titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return title;
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            this.valueLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), valueSupplier, ThemeColorKey.TEXT_PRIMARY, false);
            this.iconColor = iconColor;
            this.multiline = multiline;
            addChild(titleLabel);
            addChild(valueLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            GuiRect rowBounds = getBounds();
            boolean narrow = rowBounds.getWidth() < 205;
            if (narrow) {
                int titleX = rowBounds.getX() + 18;
                int titleWidth = Math.max(40, rowBounds.getWidth() - 24);
                titleLabel.setBounds(new GuiRect(titleX, rowBounds.getY() + 1, titleWidth, 10));
                valueLabel.setBounds(new GuiRect(titleX, rowBounds.getY() + 9, titleWidth, Math.max(8, rowBounds.getHeight() - 10)));
            } else {
                int titleWidth = 62;
                int valueX = rowBounds.getX() + 82;
                int valueWidth = Math.max(40, rowBounds.getRight() - valueX - 6);
                titleLabel.setBounds(new GuiRect(rowBounds.getX() + 18, rowBounds.getY() + 2, titleWidth, 10));
                valueLabel.setBounds(new GuiRect(valueX, rowBounds.getY() + 2, valueWidth, 10));
            }
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            int iconX = bounds.getX() + 2;
            int iconY = bounds.getY() + 4;
            Gui.drawRect(iconX, iconY, iconX + 8, iconY + 8, iconColor);
            Gui.drawRect(bounds.getX(), bounds.getBottom() - 1, bounds.getRight(), bounds.getBottom(), 0x223D4D60);
        }
    }

    private final class RecentStatusPanel extends PanelContainer {

        private final LabelPanel routeLabel;
        private final LabelPanel timeLabel;
        private final LabelPanel statusLabel;
        private final LabelPanel summaryLabel;

        private RecentStatusPanel() {
            this.routeLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return compactRouteDisplay(model.getRecentSourceServerId(), model.getRecentTargetServerId());
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            this.timeLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return model.getRecentTransferTime();
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            this.statusLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return model.getRecentTransferStatus().replace("COMPLETED", "完成");
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            this.summaryLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return compactRecentSummary(model.getRecentTransferSummary());
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            addChild(routeLabel);
            addChild(timeLabel);
            addChild(statusLabel);
            addChild(summaryLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            GuiRect cardBounds = getBounds();
            int innerX = cardBounds.getX() + 36;
            int innerWidth = cardBounds.getWidth() - 48;
            boolean narrow = cardBounds.getWidth() < 205;
            if (narrow) {
                routeLabel.setBounds(new GuiRect(innerX, cardBounds.getY() + 7, innerWidth, 10));
                timeLabel.setBounds(new GuiRect(innerX, cardBounds.getY() + 18, innerWidth, 10));
                statusLabel.setBounds(new GuiRect(innerX, cardBounds.getY() + 29, innerWidth, 10));
                summaryLabel.setBounds(new GuiRect(innerX, cardBounds.getY() + 41, innerWidth, Math.max(12, cardBounds.getHeight() - 48)));
            } else {
                routeLabel.setBounds(new GuiRect(innerX, cardBounds.getY() + 7, innerWidth - 52, 10));
                timeLabel.setBounds(new GuiRect(innerX, cardBounds.getY() + 18, innerWidth - 52, 10));
                statusLabel.setBounds(new GuiRect(cardBounds.getRight() - 48, cardBounds.getY() + 7, 40, 10));
                summaryLabel.setBounds(new GuiRect(innerX, cardBounds.getY() + 31, innerWidth, Math.max(12, cardBounds.getHeight() - 38)));
            }
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            TerminalNotificationSeverity severity = resolveRecentSeverity();
            int accent = severity.getAccentColor();
            Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), 0xFF293544);
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getRight() - 1, bounds.getBottom() - 1, 0xFF16212C);
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getX() + 5, bounds.getBottom() - 1, accent);
            Gui.drawRect(bounds.getX() + 11, bounds.getY() + bounds.getHeight() / 2 - 7, bounds.getX() + 25,
                bounds.getY() + bounds.getHeight() / 2 + 7, severity.getBackgroundColor());
            Gui.drawRect(bounds.getX() + 14, bounds.getY() + bounds.getHeight() / 2 - 4, bounds.getX() + 22,
                bounds.getY() + bounds.getHeight() / 2 + 4, accent);
        }
    }

    private final class ActionFeedbackPanel extends PanelContainer {

        private final LabelPanel textLabel;

        private ActionFeedbackPanel() {
            this.textLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return model.getActionFeedback().getTitle() + " / " + model.getActionFeedback().getBody();
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            addChild(textLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            textLabel.setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 6, bounds.getWidth() - 16, bounds.getHeight() - 10));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            TerminalNotificationSeverity severity = model.getActionFeedback().getSeverity();
            Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), 0xFF25313E);
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getRight() - 1, bounds.getBottom() - 1,
                severity.getBackgroundColor());
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getX() + 5, bounds.getBottom() - 1,
                severity.getAccentColor());
        }
    }

    private final class WarningPanel extends PanelContainer {

        private final LabelPanel textLabel;

        private WarningPanel() {
            this.textLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return "传送过程中请勿移动或下线，以免传送失败。";
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            addChild(textLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            textLabel.setBounds(new GuiRect(bounds.getX() + 20, bounds.getY() + 7, bounds.getWidth() - 28, 10));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), 0xFF5D4D23);
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getRight() - 1, bounds.getBottom() - 1, 0xFF2A2418);
            Gui.drawRect(bounds.getX() + 7, bounds.getY() + 6, bounds.getX() + 11, bounds.getY() + 18, 0xFFD1A64C);
            Gui.drawRect(bounds.getX() + 7, bounds.getY() + 20, bounds.getX() + 11, bounds.getY() + 22, 0xFFD1A64C);
        }
    }
}

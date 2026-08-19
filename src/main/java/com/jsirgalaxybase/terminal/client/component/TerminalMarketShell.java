package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

final class TerminalMarketShell {

    private static final int SECTION_HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 0;
    private static final int GAP = 8;
    private static final int STACKED_LAYOUT_WIDTH = 330;

    private TerminalMarketShell() {}

    static String buildStatusBandText(TerminalHomeScreenModel model,
        TerminalHomeScreenModel.PageSnapshotModel snapshot) {
        if (snapshot == null) {
            return "市场";
        }
        if (snapshot.hasCustomMarketSectionModel()) {
            return "市场 / 定制商品";
        }
        if (snapshot.hasExchangeMarketSectionModel()) {
            return "市场 / 汇率市场";
        }
        if (snapshot.hasMarketSectionModel()) {
            TerminalMarketSectionModel market = snapshot.getMarketSectionModel();
            if (market != null && market.isOverviewRoute()) return "市场 / 总入口";
            if (market != null && market.isAccountCenterRoute()) return "市场 / 订单与资产中心";
            return "市场 / 标准商品 / 交易台";
        }
        String navLabel = model == null || model.getSelectedNavItem() == null ? "市场" : model.getSelectedNavItem().getLabel();
        String title = snapshot.getTitle() == null ? "" : snapshot.getTitle();
        return title.isEmpty() ? navLabel : navLabel + " / " + title;
    }

    static String buildSectionTitle(TerminalMarketSectionModel model) {
        if (model != null && model.isOverviewRoute()) return "市场总入口";
        return model != null && model.isAccountCenterRoute() ? "订单与资产中心" : "标准商品市场";
    }

    static String buildSectionLead(TerminalMarketSectionModel model) {
        if (model != null && model.isOverviewRoute()) return "标准入仓交易，定制玩家交付，汇率按规则报价。";
        return model != null && model.isAccountCenterRoute()
            ? "当前委托、成交记录、交付异常与历史查询均由服务端分页。"
            : "选择商品后，在右侧完成行情查看、订单管理和交易动作。";
    }

    static OverviewLayout computeOverviewLayout(GuiRect bounds) {
        int contentY = bounds.getY() + SECTION_HEADER_HEIGHT;
        int footerY = bounds.getBottom() - FOOTER_HEIGHT;
        int availableHeight = Math.max(0, footerY - contentY);
        if (bounds.getWidth() < STACKED_LAYOUT_WIDTH) {
            int entryHeight = Math.max(1, Math.min(64, Math.round(Math.max(1, availableHeight - GAP * 4) * 0.19F)));
            int bottomHeight = Math.max(1, Math.round(Math.max(1, availableHeight - entryHeight * 3 - GAP * 4) * 0.5F));
            GuiRect first = new GuiRect(bounds.getX(), contentY, bounds.getWidth(), entryHeight);
            GuiRect second = new GuiRect(bounds.getX(), first.getBottom() + GAP, bounds.getWidth(), entryHeight);
            GuiRect third = new GuiRect(bounds.getX(), second.getBottom() + GAP, bounds.getWidth(), entryHeight);
            GuiRect status = new GuiRect(bounds.getX(), third.getBottom() + GAP, bounds.getWidth(), bottomHeight);
            GuiRect help = new GuiRect(bounds.getX(), status.getBottom() + GAP, bounds.getWidth(),
                Math.max(0, footerY - (status.getBottom() + GAP)));
            GuiRect footer = new GuiRect(bounds.getX(), footerY, bounds.getWidth(), FOOTER_HEIGHT);
            return new OverviewLayout(first, second, third, status, help, footer);
        }

        int entryHeight = Math.max(0, Math.min(Math.min(120, availableHeight), Math.round(availableHeight * 0.54F)));
        int lowerY = contentY + entryHeight + GAP;
        int lowerHeight = Math.max(0, footerY - lowerY);
        int cardWidth = (bounds.getWidth() - GAP * 2) / 3;
        int lastWidth = bounds.getWidth() - cardWidth * 2 - GAP * 2;
        GuiRect first = new GuiRect(bounds.getX(), contentY, cardWidth, entryHeight);
        GuiRect second = new GuiRect(bounds.getX() + cardWidth + GAP, contentY, cardWidth, entryHeight);
        GuiRect third = new GuiRect(bounds.getX() + cardWidth * 2 + GAP * 2, contentY, lastWidth, entryHeight);
        int statusWidth = Math.max(180, Math.round(bounds.getWidth() * 0.64F));
        GuiRect status = new GuiRect(bounds.getX(), lowerY, statusWidth, lowerHeight);
        GuiRect help = new GuiRect(status.getRight() + GAP, lowerY,
            Math.max(96, bounds.getRight() - status.getRight() - GAP), lowerHeight);
        GuiRect footer = new GuiRect(bounds.getX(), footerY, bounds.getWidth(), FOOTER_HEIGHT);
        return new OverviewLayout(first, second, third, status, help, footer);
    }

    static WorkbenchLayout computeWorkbenchLayout(GuiRect bounds) {
        int contentY = bounds.getY();
        int availableHeight = Math.max(0, bounds.getHeight());
        if (bounds.getWidth() < STACKED_LAYOUT_WIDTH) {
            int browserHeight = Math.max(0, Math.min(96, Math.round(availableHeight * 0.42F)));
            GuiRect left = new GuiRect(bounds.getX(), contentY, bounds.getWidth(), browserHeight);
            GuiRect detail = new GuiRect(bounds.getX(), left.getBottom() + GAP, bounds.getWidth(),
                Math.max(0, availableHeight - browserHeight - GAP));
            return new WorkbenchLayout(left, detail);
        }
        int leftWidth = Math.max(150, Math.min(220, Math.round(bounds.getWidth() * 0.31F)));
        int rightWidth = Math.max(0, bounds.getWidth() - leftWidth - GAP);
        GuiRect left = new GuiRect(bounds.getX(), contentY, leftWidth, availableHeight);
        GuiRect detail = new GuiRect(bounds.getX() + leftWidth + GAP, contentY, rightWidth, availableHeight);
        return new WorkbenchLayout(left, detail);
    }

    static final class OverviewLayout {

        private final GuiRect standardizedBounds;
        private final GuiRect customBounds;
        private final GuiRect exchangeBounds;
        private final GuiRect statusBounds;
        private final GuiRect helpBounds;
        private final GuiRect footerBounds;

        OverviewLayout(GuiRect standardizedBounds, GuiRect customBounds, GuiRect exchangeBounds,
            GuiRect statusBounds, GuiRect helpBounds, GuiRect footerBounds) {
            this.standardizedBounds = standardizedBounds;
            this.customBounds = customBounds;
            this.exchangeBounds = exchangeBounds;
            this.statusBounds = statusBounds;
            this.helpBounds = helpBounds;
            this.footerBounds = footerBounds;
        }

        GuiRect getStandardizedBounds() {
            return standardizedBounds;
        }

        GuiRect getCustomBounds() {
            return customBounds;
        }

        GuiRect getExchangeBounds() {
            return exchangeBounds;
        }

        GuiRect getStatusBounds() {
            return statusBounds;
        }

        GuiRect getHelpBounds() {
            return helpBounds;
        }

        GuiRect getFooterBounds() {
            return footerBounds;
        }
    }

    static final class WorkbenchLayout {

        private final GuiRect browserBounds;
        private final GuiRect detailBounds;

        WorkbenchLayout(GuiRect browserBounds, GuiRect detailBounds) {
            this.browserBounds = browserBounds;
            this.detailBounds = detailBounds;
        }

        GuiRect getBrowserBounds() {
            return browserBounds;
        }

        GuiRect getDetailBounds() {
            return detailBounds;
        }
    }
}

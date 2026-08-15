package com.jsirgalaxybase.terminal.client.component;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import com.jsirgalaxybase.client.gui.framework.AbstractGuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

/** Passive, read-only market comparison card. */
final class MarketBrowseTooltipPanel extends AbstractGuiPanel {

    private final MarketBrowseItemModel item;

    MarketBrowseTooltipPanel(MarketBrowseItemModel item) { this.item = item; }

    @Override
    public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible() || item == null) { return; }
        GuiRect b = getBounds();
        RoundedRectPainter.draw(b, 0xFF4B6179, 0xF215202B);
        TerminalMarketVisuals.drawItemIconOrBadge(b.getX() + 8, b.getY() + 8, 22, item.getIconRef(), item.getKey());
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        draw(font, trim(font, item.getTitle(), b.getWidth() - 44), b.getX() + 36, b.getY() + 9, 0xFFE4EDF7);
        if (!item.isStandardized()) {
            drawSpecialized(font, b);
            return;
        }
        String change = item.hasDayChange() ? item.getDayChange() : "--";
        int changeColor = change.startsWith("-") ? 0xFFE56A64 : item.hasDayChange() ? 0xFF62D478 : 0xFF8FA2B8;
        draw(font, "最新 " + item.getCompactLatestPrice(), b.getX() + 8, b.getY() + 35, 0xFFF1CA62);
        draw(font, "今日 " + change, b.getRight() - 8 - font.getStringWidth("今日 " + change), b.getY() + 35,
            changeColor);
        draw(font, "零点基准 " + (item.getDayOpenPrice() > 0L ? item.getDayOpenPrice() : "--"), b.getX() + 8,
            b.getY() + 47, 0xFFBFCBDA);
        draw(font, "买一 " + item.getCompactBestBid(), b.getX() + 8, b.getY() + 59, 0xFF70D58A);
        draw(font, "卖一 " + item.getCompactBestAsk(), b.getX() + 8, b.getY() + 71, 0xFFE6746E);
        draw(font, "24h量 " + item.getVolume24h() + "  " + item.getLiquidityLabel(), b.getX() + 8,
            b.getY() + 83, 0xFFBFCBDA);
        draw(font, "可卖 " + item.getAvailable() + "  锁定 " + item.getEscrow() + "  待收 " + item.getClaimable(),
            b.getX() + 8, b.getY() + 95, 0xFF8FA2B8);
        if (item.getPricePoints().size() >= 2) {
            drawChangeBars(font, b.getX() + 8, b.getY() + 107, b.getWidth() - 16, 40,
                item.getPricePoints());
        }
    }

    private void drawSpecialized(FontRenderer font, GuiRect b) {
        if (item.getKind() == MarketBrowseItemModel.Kind.CUSTOM_LISTING) {
            draw(font, "挂牌价格 " + item.getCompactReferencePrice(), b.getX() + 8, b.getY() + 35, 0xFFF1CA62);
            draw(font, "交易方 " + item.getTooltipPrimary(), b.getX() + 8, b.getY() + 49, 0xFFBFCBDA);
            draw(font, "状态 " + item.getTooltipSecondary(), b.getX() + 8, b.getY() + 63, 0xFF8FA2B8);
            draw(font, "点击查看挂牌详情", b.getX() + 8, b.getY() + 81, 0xFF6AB6EE);
            return;
        }
        draw(font, "任务书硬币 " + item.getCompactReferencePrice(), b.getX() + 8, b.getY() + 35, 0xFFF1CA62);
        draw(font, "目录 " + item.getTooltipPrimary(), b.getX() + 8, b.getY() + 49, 0xFFBFCBDA);
        draw(font, "兑换状态 " + item.getTooltipSecondary(), b.getX() + 8, b.getY() + 63, 0xFF8FA2B8);
        draw(font, "点击查看正式报价", b.getX() + 8, b.getY() + 81, 0xFF6AB6EE);
    }

    private void drawChangeBars(FontRenderer font, int x, int y, int width, int height,
        List<TerminalMarketSectionModel.PricePointModel> points) {
        Gui.drawRect(x, y, x + width, y + height, 0xAA101820);
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (TerminalMarketSectionModel.PricePointModel point : points) {
            min = Math.min(min, point.getPrice());
            max = Math.max(max, point.getPrice());
        }
        long range = Math.max(1L, max - min);
        int axisWidth = Math.max(
            MarketCompactText.width(font, String.valueOf(max), MarketCompactText.AXIS_SCALE),
            MarketCompactText.width(font, String.valueOf(min), MarketCompactText.AXIS_SCALE)) + 3;
        int plotX = x + axisWidth;
        int plotY = y + 2;
        int plotWidth = Math.max(8, width - axisWidth - 2);
        int plotHeight = Math.max(10, height - 11);
        int baseline = plotY + plotHeight - (int) ((points.get(0).getPrice() - min) * plotHeight / range);
        Gui.drawRect(plotX, baseline, plotX + plotWidth, baseline + 1, 0x445B6F82);
        int barWidth = Math.max(1, plotWidth / Math.max(1, points.size()) - 1);
        for (int index = 0; index < points.size(); index++) {
            long price = points.get(index).getPrice();
            int pointY = plotY + plotHeight - (int) ((price - min) * plotHeight / range);
            int barX = plotX + index * plotWidth / Math.max(1, points.size());
            int top = Math.min(baseline, pointY);
            int bottom = Math.max(baseline + 1, pointY + 1);
            int color = price >= points.get(0).getPrice() ? 0xFF57C96B : 0xFFE05A55;
            Gui.drawRect(barX, top, Math.min(plotX + plotWidth, barX + barWidth), bottom, color);
        }
        MarketCompactText.draw(font, String.valueOf(max), x, plotY, 0xFF8FA2B8,
            MarketCompactText.AXIS_SCALE);
        MarketCompactText.draw(font, String.valueOf(min), x, plotY + plotHeight - 6, 0xFF8FA2B8,
            MarketCompactText.AXIS_SCALE);
        MarketCompactText.draw(font, "00:00", plotX, y + height - 6, 0xFF718396,
            MarketCompactText.AXIS_SCALE);
        String now = "现在";
        MarketCompactText.draw(font, now,
            plotX + plotWidth - MarketCompactText.width(font, now, MarketCompactText.AXIS_SCALE),
            y + height - 6, 0xFF718396, MarketCompactText.AXIS_SCALE);
    }

    private static void draw(FontRenderer font, String text, int x, int y, int color) { font.drawStringWithShadow(text, x, y, color); }
    private static String trim(FontRenderer font, String value, int width) { return font.trimStringToWidth(value, Math.max(8, width)); }
}

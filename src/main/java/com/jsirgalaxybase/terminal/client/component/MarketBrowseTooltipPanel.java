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
            drawSparkline(b.getX() + 8, b.getY() + 108, b.getWidth() - 16, 13, item.getPricePoints());
            draw(font, "今日真实成交走势", b.getX() + 8, b.getY() + 122, 0xFF8FA2B8);
        }
    }

    private void drawSparkline(int x, int y, int width, int height, List<TerminalMarketSectionModel.PricePointModel> points) {
        Gui.drawRect(x, y, x + width, y + height, 0xAA101820);
        if (points == null || points.size() < 2) { return; }
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (TerminalMarketSectionModel.PricePointModel point : points) { min = Math.min(min, point.getPrice()); max = Math.max(max, point.getPrice()); }
        long range = Math.max(1L, max - min);
        int previousX = x;
        int previousY = y + height - 3 - (int) ((points.get(0).getPrice() - min) * (height - 5) / range);
        for (int index = 1; index < points.size(); index++) {
            int pointX = x + index * Math.max(1, width - 2) / Math.max(1, points.size() - 1);
            int pointY = y + height - 3 - (int) ((points.get(index).getPrice() - min) * (height - 5) / range);
            drawLine(previousX, previousY, pointX, pointY, 0xFF63C2F0);
            previousX = pointX; previousY = pointY;
        }
    }

    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int step = 0; step <= steps; step++) {
            Gui.drawRect(x0 + (x1 - x0) * step / Math.max(1, steps), y0 + (y1 - y0) * step / Math.max(1, steps),
                x0 + (x1 - x0) * step / Math.max(1, steps) + 1, y0 + (y1 - y0) * step / Math.max(1, steps) + 1, color);
        }
    }

    private static void draw(FontRenderer font, String text, int x, int y, int color) { font.drawStringWithShadow(text, x, y, color); }
    private static String trim(FontRenderer font, String value, int width) { return font.trimStringToWidth(value, Math.max(8, width)); }
}

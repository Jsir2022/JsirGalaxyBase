package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.client.gui.framework.AbstractGuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.HoverOverlayPositioner;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;

/** Four-column browse surface. It is the only scrolling region in market browse mode. */
final class MarketItemGridPanel extends AbstractGuiPanel {

    private static final int COLUMNS = 4;
    private static final int GAP = 5;
    private static final int PADDING = 6;
    private static final int HOVER_DELAY_MS = 160;
    private static final int SCROLL_STEP = 38;
    private static final ResourceLocation CLICK_SOUND = new ResourceLocation("gui.button.press");

    interface Listener {
        void select(MarketBrowseItemModel item);
        void scrollOffsetChanged(int offset);
    }

    private final List<MarketBrowseItemModel> items;
    private final Listener listener;
    private int scrollOffset;
    private String hoverKey = "";
    private long hoverStartedAt;
    private String pressedKey = "";

    MarketItemGridPanel(List<MarketBrowseItemModel> items, Listener listener) {
        this.items = items == null ? Collections.<MarketBrowseItemModel>emptyList()
            : Collections.unmodifiableList(new ArrayList<MarketBrowseItemModel>(items));
        this.listener = listener;
    }

    static int getColumns() { return COLUMNS; }
    int getScrollOffset() { return scrollOffset; }
    void setScrollOffset(int value) { scrollOffset = clamp(value, 0, getMaxScrollOffset()); }
    int getMaxScrollOffset() { return Math.max(0, contentHeight() - getBounds().getHeight()); }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        setScrollOffset(scrollOffset);
    }

    @Override
    public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible()) { return; }
        GuiRect b = getBounds();
        RoundedRectPainter.draw(b, 0xFF324152, 0xFF121B25);
        beginClip(b);
        try {
            for (int index = 0; index < items.size(); index++) {
                MarketBrowseItemModel item = items.get(index);
                GuiRect cell = cellBounds(index);
                if (intersects(cell, b)) { drawCell(scene, item, cell, mouseX, mouseY); }
            }
        } finally { endClip(); }
        drawScrollbar(scene);
        updateHover(scene, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        MarketBrowseItemModel item = itemAt(mouseX, mouseY);
        pressedKey = mouseButton == 0 && item != null ? item.getKey() : "";
        return !pressedKey.isEmpty();
    }

    @Override
    public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        MarketBrowseItemModel item = itemAt(mouseX, mouseY);
        boolean activate = mouseButton == 0 && item != null && pressedKey.equals(item.getKey());
        pressedKey = "";
        if (!activate) { return false; }
        scene.closeHoverOverlay();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null) { minecraft.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(CLICK_SOUND, 1.0F)); }
        if (listener != null) { listener.select(item); }
        return true;
    }

    @Override
    public boolean mouseScrolled(GuiScene scene, int mouseX, int mouseY, int wheelDelta) {
        if (!contains(mouseX, mouseY) || wheelDelta == 0 || getMaxScrollOffset() <= 0) { return false; }
        scene.closeHoverOverlay();
        int steps = Math.max(1, Math.abs(wheelDelta) / 120);
        setScrollOffset(scrollOffset + (wheelDelta < 0 ? steps * SCROLL_STEP : -steps * SCROLL_STEP));
        if (listener != null) { listener.scrollOffsetChanged(scrollOffset); }
        return true;
    }

    private void drawCell(GuiScene scene, MarketBrowseItemModel item, GuiRect cell, int mouseX, int mouseY) {
        boolean hovered = cell.contains(mouseX, mouseY);
        RoundedRectPainter.draw(cell, hovered ? 0xFF4D91D9 : 0xFF273645, hovered ? 0xFF18293B : 0xFF101820);
        int iconSize = Math.min(24, Math.max(16, cell.getHeight() / 3));
        int iconX = cell.getX() + (cell.getWidth() - iconSize) / 2;
        TerminalMarketVisuals.drawItemIconOrBadge(iconX, cell.getY() + 5, iconSize, item.getIconRef(), item.getKey());
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int textWidth = Math.max(18, cell.getWidth() - 10);
        drawCentered(font, font.trimStringToWidth(item.getTitle(), textWidth), cell, cell.getY() + 31, 0xFFE4EDF7);
        String price = item.getCompactLatestPrice();
        drawCentered(font, font.trimStringToWidth(price, textWidth), cell, cell.getY() + 43, 0xFFF0C95B);
        String changeLabel = item.hasDayChange() ? item.getDayChange() : "今日暂无成交";
        boolean negative = changeLabel.startsWith("-");
        drawCentered(font, font.trimStringToWidth(changeLabel, textWidth), cell, cell.getY() + 55,
            negative ? 0xFFE56A64 : item.hasDayChange() ? 0xFF62D478 : 0xFF8294A7);
        drawSparkline(cell.getX() + 8, cell.getBottom() - 17, cell.getWidth() - 16, 11, item.getPricePoints(),
            negative ? 0xFFE56A64 : 0xFF55B7ED);
        int dotColor = "双边".equals(item.getLiquidityLabel()) ? 0xFF63D36E
            : "单边".equals(item.getLiquidityLabel()) ? 0xFFE0B34B : 0xFF657587;
        Gui.drawRect(cell.getRight() - 8, cell.getBottom() - 8, cell.getRight() - 5, cell.getBottom() - 5, dotColor);
    }

    private void updateHover(GuiScene scene, int mouseX, int mouseY) {
        MarketBrowseItemModel item = itemAt(mouseX, mouseY);
        if (item == null) {
            hoverKey = ""; hoverStartedAt = 0L; scene.closeHoverOverlay(); return;
        }
        if (!item.getKey().equals(hoverKey)) { hoverKey = item.getKey(); hoverStartedAt = System.currentTimeMillis(); return; }
        if (System.currentTimeMillis() - hoverStartedAt < HOVER_DELAY_MS) { return; }
        MarketBrowseTooltipPanel tooltip = new MarketBrowseTooltipPanel(item);
        int tooltipHeight = item.getPricePoints().size() < 2 ? 104 : 132;
        tooltip.setBounds(HoverOverlayPositioner.place(getBounds(), mouseX, mouseY,
            Math.min(214, Math.max(160, getBounds().getWidth() * 35 / 100)), tooltipHeight));
        scene.openHoverOverlay(tooltip);
    }

    private MarketBrowseItemModel itemAt(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) { return null; }
        for (int index = 0; index < items.size(); index++) {
            GuiRect cell = cellBounds(index);
            if (cell.contains(mouseX, mouseY)) { return items.get(index); }
        }
        return null;
    }

    private GuiRect cellBounds(int index) {
        GuiRect b = getBounds();
        int contentWidth = Math.max(1, b.getWidth() - PADDING * 2 - (getMaxScrollOffset() > 0 ? 7 : 0));
        int width = Math.max(20, (contentWidth - GAP * (COLUMNS - 1)) / COLUMNS);
        int row = index / COLUMNS, column = index % COLUMNS;
        int cellHeight = targetCellHeight();
        int x = b.getX() + PADDING + column * (width + GAP);
        int y = b.getY() + PADDING + row * (cellHeight + GAP) - scrollOffset;
        int actualWidth = column == COLUMNS - 1 ? b.getX() + PADDING + contentWidth - x : width;
        return new GuiRect(x, y, Math.max(1, actualWidth), cellHeight);
    }

    private int targetCellHeight() { return Math.max(72, (getBounds().getHeight() - PADDING * 2 - GAP * 2) / 3); }
    private int contentHeight() { return PADDING * 2 + ((items.size() + COLUMNS - 1) / COLUMNS) * (targetCellHeight() + GAP) - (items.isEmpty() ? 0 : GAP); }

    private static void drawCentered(FontRenderer font, String text, GuiRect cell, int y, int color) {
        font.drawStringWithShadow(text, cell.getX() + Math.max(0, (cell.getWidth() - font.getStringWidth(text)) / 2), y, color);
    }

    private static void drawSparkline(int x, int y, int width, int height,
        List<com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel.PricePointModel> points,
        int color) {
        if (points == null || points.size() < 2 || width <= 2) { return; }
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel.PricePointModel point : points) {
            min = Math.min(min, point.getPrice()); max = Math.max(max, point.getPrice());
        }
        long range = Math.max(1L, max - min);
        int lastX = x, lastY = y + height - 1 - (int) ((points.get(0).getPrice() - min) * (height - 2) / range);
        for (int index = 1; index < points.size(); index++) {
            int nextX = x + index * width / Math.max(1, points.size() - 1);
            int nextY = y + height - 1 - (int) ((points.get(index).getPrice() - min) * (height - 2) / range);
            int steps = Math.max(Math.abs(nextX - lastX), Math.abs(nextY - lastY));
            for (int step = 0; step <= steps; step++) {
                int px = lastX + (nextX - lastX) * step / Math.max(1, steps);
                int py = lastY + (nextY - lastY) * step / Math.max(1, steps);
                Gui.drawRect(px, py, px + 1, py + 1, color);
            }
            lastX = nextX; lastY = nextY;
        }
    }
    private void drawScrollbar(GuiScene scene) {
        if (getMaxScrollOffset() <= 0) { return; }
        GuiRect b = getBounds(); int trackX = b.getRight() - 5, trackY = b.getY() + 3, trackH = b.getHeight() - 6;
        int thumbH = Math.max(14, (int) (b.getHeight() / (float) contentHeight() * trackH));
        int thumbY = trackY + (trackH - thumbH) * scrollOffset / Math.max(1, getMaxScrollOffset());
        Gui.drawRect(trackX, trackY, trackX + 3, trackY + trackH, scene.getTheme().color(com.jsirgalaxybase.client.gui.theme.ThemeColorKey.PANEL_BORDER));
        Gui.drawRect(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xFF5792CA);
    }
    private static boolean intersects(GuiRect a, GuiRect b) { return a.getRight() > b.getX() && a.getX() < b.getRight() && a.getBottom() > b.getY() && a.getY() < b.getBottom(); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static void beginClip(GuiRect b) { Minecraft mc = Minecraft.getMinecraft(); ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight); GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(b.getX() * sr.getScaleFactor(), mc.displayHeight - b.getBottom() * sr.getScaleFactor(), b.getWidth() * sr.getScaleFactor(), b.getHeight() * sr.getScaleFactor()); }
    private static void endClip() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }
}

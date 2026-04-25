package com.jsirgalaxybase.client.gui.framework;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;

public class VerticalScrollPanel extends PanelContainer {

    private static final int DEFAULT_SCROLL_STEP = 18;
    private static final int MIN_SCROLLBAR_HEIGHT = 18;

    private final List<RowEntry> rows = new ArrayList<RowEntry>();
    private final int padding;
    private final int gap;
    private int scrollOffset;
    private int contentHeight;

    public VerticalScrollPanel(int padding, int gap) {
        this.padding = Math.max(0, padding);
        this.gap = Math.max(0, gap);
    }

    public VerticalScrollPanel addScrollableChild(GuiPanel panel, int preferredHeight) {
        if (panel == null) {
            return this;
        }
        super.addChild(panel);
        rows.add(new RowEntry(panel, Math.max(1, preferredHeight)));
        relayoutChildren();
        return this;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getMaxScrollOffset() {
        return Math.max(0, contentHeight - getBounds().getHeight());
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = clamp(scrollOffset, 0, getMaxScrollOffset());
        relayoutChildren();
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        setScrollOffset(scrollOffset);
    }

    @Override
    public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        if (!isVisible() || !contains(mouseX, mouseY)) {
            return false;
        }
        return super.mouseClicked(scene, mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(GuiScene scene, int mouseX, int mouseY, int wheelDelta) {
        if (!isVisible() || !contains(mouseX, mouseY) || wheelDelta == 0 || getMaxScrollOffset() <= 0) {
            return false;
        }
        int stepCount = Math.max(1, Math.abs(wheelDelta) / 120);
        int direction = wheelDelta < 0 ? 1 : -1;
        setScrollOffset(scrollOffset + direction * stepCount * DEFAULT_SCROLL_STEP);
        return true;
    }

    @Override
    public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible()) {
            return;
        }

        drawSelf(scene, mouseX, mouseY, partialTicks);
        GuiRect bounds = getBounds();
        beginClip(bounds);
        try {
            for (GuiPanel child : getChildren()) {
                if (child.isVisible() && intersects(child.getBounds(), bounds)) {
                    child.draw(scene, mouseX, mouseY, partialTicks);
                }
            }
        } finally {
            endClip();
        }
        drawScrollbar(scene);
    }

    private void relayoutChildren() {
        GuiRect bounds = getBounds();
        int totalHeight = padding * 2;
        for (int index = 0; index < rows.size(); index++) {
            totalHeight += rows.get(index).height;
            if (index < rows.size() - 1) {
                totalHeight += gap;
            }
        }
        contentHeight = totalHeight;
        scrollOffset = clamp(scrollOffset, 0, getMaxScrollOffset());

        int currentY = bounds.getY() + padding - scrollOffset;
        int rowWidth = Math.max(0, bounds.getWidth() - padding * 2 - (getMaxScrollOffset() > 0 ? 8 : 0));
        for (RowEntry row : rows) {
            row.panel.setBounds(new GuiRect(bounds.getX() + padding, currentY, rowWidth, row.height));
            currentY += row.height + gap;
        }
    }

    private void drawScrollbar(GuiScene scene) {
        int maxScrollOffset = getMaxScrollOffset();
        if (maxScrollOffset <= 0) {
            return;
        }

        GuiRect bounds = getBounds();
        int trackWidth = 4;
        int trackX = bounds.getRight() - trackWidth - 2;
        int trackY = bounds.getY() + 2;
        int trackHeight = Math.max(8, bounds.getHeight() - 4);
        Gui.drawRect(trackX, trackY, trackX + trackWidth, trackY + trackHeight,
            scene.getTheme().color(ThemeColorKey.PANEL_BORDER));

        int thumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT,
            Math.max(8, (int) ((bounds.getHeight() / (float) contentHeight) * trackHeight)));
        int travel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackY + (travel == 0 ? 0 : Math.round((scrollOffset / (float) maxScrollOffset) * travel));
        Gui.drawRect(trackX, thumbY, trackX + trackWidth, thumbY + thumbHeight,
            scene.getTheme().color(ThemeColorKey.BUTTON_FILL_HOVER));
    }

    private static boolean intersects(GuiRect first, GuiRect second) {
        return first != null && second != null
            && first.getRight() > second.getX()
            && first.getX() < second.getRight()
            && first.getBottom() > second.getY()
            && first.getY() < second.getBottom();
    }

    private static void beginClip(GuiRect bounds) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }
        ScaledResolution scaledResolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
        int scaleFactor = scaledResolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            bounds.getX() * scaleFactor,
            minecraft.displayHeight - bounds.getBottom() * scaleFactor,
            bounds.getWidth() * scaleFactor,
            bounds.getHeight() * scaleFactor);
    }

    private static void endClip() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class RowEntry {

        private final GuiPanel panel;
        private final int height;

        private RowEntry(GuiPanel panel, int height) {
            this.panel = panel;
            this.height = height;
        }
    }
}
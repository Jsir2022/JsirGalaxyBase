package com.jsirgalaxybase.terminal.client.component;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

import com.jsirgalaxybase.client.gui.framework.AbstractGuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;

final class TerminalIconButtonPanel extends AbstractGuiPanel {

    private static final ResourceLocation CLICK_SOUND = new ResourceLocation("gui.button.press");

    private final TerminalIconKind iconKind;
    private final Runnable onClick;
    private final Supplier<Boolean> enabledSupplier;
    private boolean pressed;

    TerminalIconButtonPanel(TerminalIconKind iconKind, Runnable onClick, Supplier<Boolean> enabledSupplier) {
        this.iconKind = iconKind == null ? TerminalIconKind.INFO : iconKind;
        this.onClick = onClick;
        this.enabledSupplier = enabledSupplier;
    }

    @Override
    public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible()) {
            return;
        }
        GuiRect bounds = getBounds();
        boolean enabled = isEnabled();
        boolean hovered = enabled && contains(mouseX, mouseY);
        int border = scene.getTheme().color(ThemeColorKey.PANEL_BORDER);
        int fill = scene.getTheme().color(!enabled ? ThemeColorKey.BUTTON_FILL_DISABLED
            : pressed && hovered ? ThemeColorKey.BUTTON_FILL_PRESSED
                : hovered ? ThemeColorKey.BUTTON_FILL_HOVER : ThemeColorKey.PANEL_ACCENT);
        RoundedRectPainter.draw(bounds, border, fill);
        int iconSize = Math.max(8, Math.min(11, Math.min(bounds.getWidth(), bounds.getHeight()) - 5));
        int iconX = bounds.getX() + (bounds.getWidth() - iconSize) / 2;
        int iconY = bounds.getY() + (bounds.getHeight() - iconSize) / 2;
        int iconColor = enabled ? TerminalIconPainter.ICON_PRIMARY : TerminalIconPainter.ICON_MUTED;
        TerminalIconPainter.draw(iconKind, iconX, iconY, iconSize, iconColor);
    }

    @Override
    public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        if (!isVisible() || !isEnabled()) {
            return false;
        }
        pressed = mouseButton == 0 && contains(mouseX, mouseY);
        return pressed;
    }

    @Override
    public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        boolean shouldClick = pressed && mouseButton == 0 && contains(mouseX, mouseY) && isEnabled();
        pressed = false;
        if (!shouldClick) {
            return false;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null) {
            minecraft.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(CLICK_SOUND, 1.0F));
        }
        if (onClick != null) {
            onClick.run();
        }
        return true;
    }

    private boolean isEnabled() {
        return enabledSupplier == null || Boolean.TRUE.equals(enabledSupplier.get());
    }
}

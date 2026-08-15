package com.jsirgalaxybase.terminal.client.component;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import com.jsirgalaxybase.client.gui.framework.AbstractGuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;

/** Trading-specific command button with stable buy/sell/cancel semantics. */
final class MarketActionButtonPanel extends AbstractGuiPanel {

    private static final ResourceLocation CLICK_SOUND = new ResourceLocation("gui.button.press");

    private final Supplier<String> label;
    private final Runnable action;
    private final Supplier<Boolean> enabled;
    private final int fill;
    private final int hoverFill;
    private boolean pressed;

    MarketActionButtonPanel(String label, Runnable action, Supplier<Boolean> enabled, int fill, int hoverFill) {
        this.label = () -> label;
        this.action = action;
        this.enabled = enabled;
        this.fill = fill;
        this.hoverFill = hoverFill;
    }

    @Override
    public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible()) { return; }
        GuiRect bounds = getBounds();
        boolean active = isEnabled();
        int currentFill = !active ? 0xCC26313D
            : pressed && contains(mouseX, mouseY) ? darken(hoverFill) : contains(mouseX, mouseY) ? hoverFill : fill;
        RoundedRectPainter.draw(bounds, active ? 0xFF71869B : 0xFF435160, currentFill);
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRenderer == null) { return; }
        FontRenderer font = minecraft.fontRenderer;
        String text = label.get();
        int x = bounds.getX() + Math.max(3, (bounds.getWidth() - font.getStringWidth(text)) / 2);
        int y = bounds.getY() + Math.max(0, (bounds.getHeight() - 8) / 2);
        font.drawStringWithShadow(text, x, y, active ? 0xFFFFFFFF : 0xFF758493);
    }

    @Override
    public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        pressed = mouseButton == 0 && isEnabled() && contains(mouseX, mouseY);
        return pressed;
    }

    @Override
    public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        boolean activate = pressed && mouseButton == 0 && isEnabled() && contains(mouseX, mouseY);
        pressed = false;
        if (!activate) { return false; }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null) {
            minecraft.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(CLICK_SOUND, 1.0F));
        }
        if (action != null) { action.run(); }
        return true;
    }

    private boolean isEnabled() {
        return enabled == null || Boolean.TRUE.equals(enabled.get());
    }

    private int darken(int color) {
        return (color & 0xFF000000) | (((color >> 16) & 0xFF) * 4 / 5 << 16)
            | (((color >> 8) & 0xFF) * 4 / 5 << 8) | ((color & 0xFF) * 4 / 5);
    }
}

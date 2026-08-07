package com.jsirgalaxybase.client.gui.framework;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;

public class ButtonPanel extends AbstractGuiPanel {

    private static final ResourceLocation CLICK_SOUND = new ResourceLocation("gui.button.press");

    private final Supplier<String> labelSupplier;
    private final Runnable onClick;
    private final Supplier<Boolean> enabledSupplier;
    private final float textScale;
    private boolean pressed;

    public ButtonPanel(Supplier<String> labelSupplier, Runnable onClick, Supplier<Boolean> enabledSupplier) {
        this(labelSupplier, onClick, enabledSupplier, 1.0F);
    }

    public ButtonPanel(Supplier<String> labelSupplier, Runnable onClick, Supplier<Boolean> enabledSupplier,
        float textScale) {
        this.labelSupplier = labelSupplier;
        this.onClick = onClick;
        this.enabledSupplier = enabledSupplier;
        this.textScale = Math.max(0.60F, Math.min(1.0F, textScale));
    }

    @Override
    public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible()) {
            return;
        }

        GuiRect bounds = getBounds();
        boolean enabled = isEnabled();
        boolean hovered = enabled && contains(mouseX, mouseY);
        ThemeColorKey fillKey = !enabled ? ThemeColorKey.BUTTON_FILL_DISABLED
            : pressed && hovered ? ThemeColorKey.BUTTON_FILL_PRESSED
                : hovered ? ThemeColorKey.BUTTON_FILL_HOVER : ThemeColorKey.BUTTON_FILL;
        int textColor = scene.getTheme().color(enabled ? ThemeColorKey.BUTTON_TEXT : ThemeColorKey.BUTTON_TEXT_DISABLED);
        RoundedRectPainter.draw(bounds, scene.getTheme().color(ThemeColorKey.PANEL_BORDER),
            scene.getTheme().color(fillKey));

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.fontRenderer != null) {
            FontRenderer fontRenderer = minecraft.fontRenderer;
            String label = labelSupplier == null ? "" : labelSupplier.get();
            String visibleLabel = label == null ? "" : label;
            int availableWidth = Math.max(4, Math.round((bounds.getWidth() - 8) / textScale));
            if (fontRenderer.getStringWidth(visibleLabel) > availableWidth) {
                visibleLabel = fontRenderer.trimStringToWidth(visibleLabel, availableWidth);
            }
            int textWidth = Math.round(fontRenderer.getStringWidth(visibleLabel) * textScale);
            int drawX = bounds.getX() + Math.max(4, (bounds.getWidth() - textWidth) / 2);
            int drawY = bounds.getY() + Math.max(0, (bounds.getHeight() - Math.round(8 * textScale)) / 2);
            GL11.glPushMatrix();
            GL11.glTranslatef(drawX, drawY, 0.0F);
            GL11.glScalef(textScale, textScale, 1.0F);
            fontRenderer.drawStringWithShadow(visibleLabel, 0, 0, textColor);
            GL11.glPopMatrix();
        }
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

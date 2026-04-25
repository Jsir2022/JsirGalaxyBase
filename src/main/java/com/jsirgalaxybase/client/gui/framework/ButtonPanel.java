package com.jsirgalaxybase.client.gui.framework;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.client.gui.theme.ThemeTextureKey;

public class ButtonPanel extends AbstractGuiPanel {

    private static final ResourceLocation CLICK_SOUND = new ResourceLocation("gui.button.press");

    private final Supplier<String> labelSupplier;
    private final Runnable onClick;
    private final Supplier<Boolean> enabledSupplier;
    private boolean pressed;

    public ButtonPanel(Supplier<String> labelSupplier, Runnable onClick, Supplier<Boolean> enabledSupplier) {
        this.labelSupplier = labelSupplier;
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
        ThemeColorKey fillKey = !enabled ? ThemeColorKey.BUTTON_FILL_DISABLED
            : pressed && hovered ? ThemeColorKey.BUTTON_FILL_PRESSED
                : hovered ? ThemeColorKey.BUTTON_FILL_HOVER : ThemeColorKey.BUTTON_FILL;
        int textColor = scene.getTheme().color(enabled ? ThemeColorKey.BUTTON_TEXT : ThemeColorKey.BUTTON_TEXT_DISABLED);
        scene.getTheme().texture(ThemeTextureKey.BUTTON_BACKGROUND)
            .draw(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), scene.getTheme().color(fillKey));

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.fontRenderer != null) {
            FontRenderer fontRenderer = minecraft.fontRenderer;
            String label = labelSupplier == null ? "" : labelSupplier.get();
            int textWidth = fontRenderer.getStringWidth(label == null ? "" : label);
            int drawX = bounds.getX() + Math.max(0, (bounds.getWidth() - textWidth) / 2);
            int drawY = bounds.getY() + Math.max(0, (bounds.getHeight() - 8) / 2);
            fontRenderer.drawStringWithShadow(label == null ? "" : label, drawX, drawY, textColor);
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
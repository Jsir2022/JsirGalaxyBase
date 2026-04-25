package com.jsirgalaxybase.client.gui.framework;

import net.minecraft.client.gui.Gui;

import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.client.gui.theme.ThemeTextureKey;

public class TexturedCanvasPanel extends PanelContainer {

    private final ThemeTextureKey textureKey;
    private final ThemeColorKey fillColorKey;
    private final ThemeColorKey borderColorKey;

    public TexturedCanvasPanel(ThemeTextureKey textureKey, ThemeColorKey fillColorKey, ThemeColorKey borderColorKey) {
        this.textureKey = textureKey;
        this.fillColorKey = fillColorKey;
        this.borderColorKey = borderColorKey;
    }

    @Override
    protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        GuiRect bounds = getBounds();
        int borderColor = scene.getTheme().color(borderColorKey);
        Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), borderColor);
        scene.getTheme().texture(textureKey)
            .draw(bounds.getX() + 1, bounds.getY() + 1, bounds.getWidth() - 2, bounds.getHeight() - 2,
                scene.getTheme().color(fillColorKey));
    }
}
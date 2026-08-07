package com.jsirgalaxybase.client.gui.framework;

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
        RoundedRectPainter.draw(bounds, borderColor, scene.getTheme().color(fillColorKey));
    }
}

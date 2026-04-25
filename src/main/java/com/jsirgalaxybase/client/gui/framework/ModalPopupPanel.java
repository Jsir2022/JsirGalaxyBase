package com.jsirgalaxybase.client.gui.framework;

import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.client.gui.theme.ThemeTextureKey;

public class ModalPopupPanel extends TexturedCanvasPanel {

    public ModalPopupPanel() {
        super(ThemeTextureKey.POPUP_BACKGROUND, ThemeColorKey.POPUP_FILL, ThemeColorKey.PANEL_BORDER);
    }
}
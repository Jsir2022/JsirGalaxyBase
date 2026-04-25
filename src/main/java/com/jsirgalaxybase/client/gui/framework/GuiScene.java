package com.jsirgalaxybase.client.gui.framework;

import com.jsirgalaxybase.client.gui.theme.GuiTheme;

public interface GuiScene {

    GuiTheme getTheme();

    void openPopup(GuiPanel panel);

    void closePopup();
}
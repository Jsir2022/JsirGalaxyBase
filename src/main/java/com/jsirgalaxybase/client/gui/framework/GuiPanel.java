package com.jsirgalaxybase.client.gui.framework;

public interface GuiPanel {

    GuiRect getBounds();

    void setBounds(GuiRect bounds);

    void init(GuiScene scene);

    void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks);

    boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton);

    boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton);

    default boolean mouseScrolled(GuiScene scene, int mouseX, int mouseY, int wheelDelta) {
        return false;
    }

    boolean keyTyped(GuiScene scene, char typedChar, int keyCode);

    boolean isVisible();

    void setVisible(boolean visible);
}
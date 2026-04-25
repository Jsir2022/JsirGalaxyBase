package com.jsirgalaxybase.client.gui.framework;

public abstract class AbstractGuiPanel implements GuiPanel {

    private GuiRect bounds = new GuiRect(0, 0, 0, 0);
    private boolean visible = true;

    @Override
    public GuiRect getBounds() {
        return bounds;
    }

    @Override
    public void setBounds(GuiRect bounds) {
        this.bounds = bounds == null ? new GuiRect(0, 0, 0, 0) : bounds;
    }

    @Override
    public void init(GuiScene scene) {}

    @Override
    public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean keyTyped(GuiScene scene, char typedChar, int keyCode) {
        return false;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    protected boolean contains(int mouseX, int mouseY) {
        return bounds != null && bounds.contains(mouseX, mouseY);
    }
}
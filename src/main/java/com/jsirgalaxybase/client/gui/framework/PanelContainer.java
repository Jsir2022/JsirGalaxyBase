package com.jsirgalaxybase.client.gui.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PanelContainer extends AbstractGuiPanel {

    private final List<GuiPanel> children = new ArrayList<GuiPanel>();

    public PanelContainer addChild(GuiPanel panel) {
        if (panel != null) {
            children.add(panel);
        }
        return this;
    }

    public List<GuiPanel> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void init(GuiScene scene) {
        for (GuiPanel child : children) {
            child.init(scene);
        }
    }

    @Override
    public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible()) {
            return;
        }

        drawSelf(scene, mouseX, mouseY, partialTicks);
        for (GuiPanel child : children) {
            if (child.isVisible()) {
                child.draw(scene, mouseX, mouseY, partialTicks);
            }
        }
    }

    @Override
    public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        if (!isVisible()) {
            return false;
        }

        for (int index = children.size() - 1; index >= 0; index--) {
            GuiPanel child = children.get(index);
            if (child.isVisible() && child.mouseClicked(scene, mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return onContainerClicked(scene, mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        if (!isVisible()) {
            return false;
        }

        boolean handled = false;
        for (int index = children.size() - 1; index >= 0; index--) {
            GuiPanel child = children.get(index);
            if (child.isVisible()) {
                handled = child.mouseReleased(scene, mouseX, mouseY, mouseButton) || handled;
            }
        }
        return onContainerReleased(scene, mouseX, mouseY, mouseButton) || handled;
    }

    @Override
    public boolean mouseScrolled(GuiScene scene, int mouseX, int mouseY, int wheelDelta) {
        if (!isVisible()) {
            return false;
        }

        for (int index = children.size() - 1; index >= 0; index--) {
            GuiPanel child = children.get(index);
            if (child.isVisible() && child.mouseScrolled(scene, mouseX, mouseY, wheelDelta)) {
                return true;
            }
        }
        return onContainerScrolled(scene, mouseX, mouseY, wheelDelta);
    }

    @Override
    public boolean keyTyped(GuiScene scene, char typedChar, int keyCode) {
        if (!isVisible()) {
            return false;
        }

        for (int index = children.size() - 1; index >= 0; index--) {
            GuiPanel child = children.get(index);
            if (child.isVisible() && child.keyTyped(scene, typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }

    protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {}

    protected boolean onContainerClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    protected boolean onContainerReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    protected boolean onContainerScrolled(GuiScene scene, int mouseX, int mouseY, int wheelDelta) {
        return false;
    }
}
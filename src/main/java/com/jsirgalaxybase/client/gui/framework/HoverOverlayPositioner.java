package com.jsirgalaxybase.client.gui.framework;

/** Keeps passive hover cards inside their owning terminal surface. */
public final class HoverOverlayPositioner {

    private HoverOverlayPositioner() {}

    public static GuiRect place(GuiRect owner, int mouseX, int mouseY, int width, int height) {
        int safeWidth = Math.max(1, Math.min(width, owner.getWidth()));
        int safeHeight = Math.max(1, Math.min(height, owner.getHeight()));
        int x = mouseX + 10;
        int y = mouseY + 10;
        if (x + safeWidth > owner.getRight()) {
            x = mouseX - safeWidth - 10;
        }
        if (y + safeHeight > owner.getBottom()) {
            y = mouseY - safeHeight - 10;
        }
        x = Math.max(owner.getX(), Math.min(x, owner.getRight() - safeWidth));
        y = Math.max(owner.getY(), Math.min(y, owner.getBottom() - safeHeight));
        return new GuiRect(x, y, safeWidth, safeHeight);
    }
}

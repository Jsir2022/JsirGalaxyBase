package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

/** Bounds-only layout contract for the compact asset overview inside the terminal shell. */
public final class TerminalWarehouseSectionLayout {
    public final GuiRect title;
    public final GuiRect hint;
    public final GuiRect vaultCard;
    public final GuiRect bayCard;
    public final GuiRect vaultButton;
    public final GuiRect bayButton;
    public final GuiRect auditCard;

    private TerminalWarehouseSectionLayout(GuiRect title, GuiRect hint, GuiRect vaultCard, GuiRect bayCard,
        GuiRect vaultButton, GuiRect bayButton, GuiRect auditCard) {
        this.title = title; this.hint = hint; this.vaultCard = vaultCard; this.bayCard = bayCard;
        this.vaultButton = vaultButton; this.bayButton = bayButton; this.auditCard = auditCard;
    }

    public static TerminalWarehouseSectionLayout compute(GuiRect bounds) {
        int x = bounds.getX() + 4, y = bounds.getY() + 3;
        int width = Math.max(1, bounds.getWidth() - 8), gap = 4;
        int column = Math.max(1, (width - gap) / 2);
        int cardY = y + 23;
        int buttonHeight = Math.min(15, Math.max(10, bounds.getBottom() - cardY - 8));
        int auditTarget = Math.min(44, Math.max(24, bounds.getHeight() / 4));
        int cardHeight = Math.max(32, bounds.getBottom() - cardY - buttonHeight - gap - auditTarget - gap - 3);
        int buttonsY = Math.min(bounds.getBottom() - buttonHeight - 3, cardY + cardHeight + gap);
        int auditY = Math.min(bounds.getBottom(), buttonsY + buttonHeight + gap);
        int auditHeight = Math.max(0, bounds.getBottom() - auditY - 3);
        return new TerminalWarehouseSectionLayout(
            new GuiRect(x, y, width, 11), new GuiRect(x, y + 11, width, 9),
            new GuiRect(x, cardY, column, Math.max(0, buttonsY - gap - cardY)),
            new GuiRect(x + column + gap, cardY, Math.max(1, width - column - gap), Math.max(0, buttonsY - gap - cardY)),
            new GuiRect(x, buttonsY, column, buttonHeight),
            new GuiRect(x + column + gap, buttonsY, Math.max(1, width - column - gap), buttonHeight),
            new GuiRect(x, auditY, width, auditHeight));
    }

    public boolean fitsWithin(GuiRect parent) {
        return inside(title, parent) && inside(hint, parent) && inside(vaultCard, parent) && inside(bayCard, parent)
            && inside(vaultButton, parent) && inside(bayButton, parent) && inside(auditCard, parent);
    }

    private static boolean inside(GuiRect child, GuiRect parent) {
        return child.getX() >= parent.getX() && child.getY() >= parent.getY()
            && child.getRight() <= parent.getRight() && child.getBottom() <= parent.getBottom();
    }
}

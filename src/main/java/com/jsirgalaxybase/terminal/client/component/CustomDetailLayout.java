package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

/** Fixed custom-listing detail geometry. Long listing text must stay inside these cards. */
final class CustomDetailLayout {

    private static final int GAP = 6;
    private static final int PADDING = 6;
    private static final int HERO_HEIGHT = 48;
    private static final int ACTION_HEIGHT = 30;

    final GuiRect hero;
    final GuiRect listing;
    final GuiRect delivery;
    final GuiRect actions;

    private CustomDetailLayout(GuiRect hero, GuiRect listing, GuiRect delivery, GuiRect actions) {
        this.hero = hero;
        this.listing = listing;
        this.delivery = delivery;
        this.actions = actions;
    }

    static CustomDetailLayout within(GuiRect bounds) {
        int x = bounds.getX() + PADDING;
        int y = bounds.getY() + PADDING;
        int width = Math.max(1, bounds.getWidth() - PADDING * 2);
        int height = Math.max(1, bounds.getHeight() - PADDING * 2);

        int heroHeight = Math.min(HERO_HEIGHT, Math.max(1, height / 4));
        GuiRect hero = new GuiRect(x, y, width, heroHeight);

        int actionHeight = Math.min(ACTION_HEIGHT, Math.max(1, height / 6));
        int actionY = Math.max(hero.getBottom() + GAP, y + height - actionHeight);
        GuiRect actions = new GuiRect(x, actionY, width, actionHeight);

        int bodyY = hero.getBottom() + GAP;
        int bodyHeight = Math.max(1, actionY - GAP - bodyY);
        int listingWidth = Math.max(1, Math.round((width - GAP) * 0.43F));
        GuiRect listing = new GuiRect(x, bodyY, listingWidth, bodyHeight);
        GuiRect delivery = new GuiRect(listing.getRight() + GAP, bodyY,
            Math.max(1, x + width - listing.getRight() - GAP), bodyHeight);
        return new CustomDetailLayout(hero, listing, delivery, actions);
    }
}

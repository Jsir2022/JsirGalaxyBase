package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

/** Fixed exchange workbench geometry. Exchange quotes do not use order-book panels. */
final class ExchangeDetailLayout {

    private static final int GAP = 6;
    private static final int PADDING = 6;
    private static final int HERO_HEIGHT = 48;
    private static final int ACTION_HEIGHT = 30;

    final GuiRect hero;
    final GuiRect source;
    final GuiRect quote;
    final GuiRect actions;

    private ExchangeDetailLayout(GuiRect hero, GuiRect source, GuiRect quote, GuiRect actions) {
        this.hero = hero;
        this.source = source;
        this.quote = quote;
        this.actions = actions;
    }

    static ExchangeDetailLayout within(GuiRect bounds) {
        int x = bounds.getX() + PADDING;
        int y = bounds.getY() + PADDING;
        int width = Math.max(1, bounds.getWidth() - PADDING * 2);
        int height = Math.max(1, bounds.getHeight() - PADDING * 2);
        int heroHeight = Math.min(HERO_HEIGHT, Math.max(1, height / 4));
        GuiRect hero = new GuiRect(x, y, width, heroHeight);

        int actionsHeight = Math.min(ACTION_HEIGHT, Math.max(1, height / 6));
        int actionsY = Math.max(hero.getBottom() + GAP, y + height - actionsHeight);
        GuiRect actions = new GuiRect(x, actionsY, width, actionsHeight);

        int bodyY = hero.getBottom() + GAP;
        int bodyHeight = Math.max(1, actionsY - GAP - bodyY);
        int sourceWidth = Math.max(1, Math.round((width - GAP) * 0.46F));
        GuiRect source = new GuiRect(x, bodyY, sourceWidth, bodyHeight);
        GuiRect quote = new GuiRect(source.getRight() + GAP, bodyY,
            Math.max(1, x + width - source.getRight() - GAP), bodyHeight);
        return new ExchangeDetailLayout(hero, source, quote, actions);
    }
}

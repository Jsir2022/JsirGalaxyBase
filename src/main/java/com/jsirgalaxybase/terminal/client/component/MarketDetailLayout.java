package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

/** Fixed-budget geometry for the modern split trading workstation. */
final class MarketDetailLayout {

    private static final int INSET = 6;
    private static final int GAP = 5;

    final GuiRect hero;
    final GuiRect chart;
    final GuiRect orderBook;
    final GuiRect ticket;
    final GuiRect footer;
    /** Compatibility slots for custom/exchange pages pending their own visual migration. */
    final GuiRect assets;
    final GuiRect actions;

    private MarketDetailLayout(GuiRect hero, GuiRect chart, GuiRect orderBook, GuiRect ticket, GuiRect footer) {
        this.hero = hero;
        this.chart = chart;
        this.orderBook = orderBook;
        this.ticket = ticket;
        this.footer = footer;
        this.assets = ticket;
        this.actions = footer;
    }

    static MarketDetailLayout within(GuiRect bounds) {
        int x = bounds.getX() + INSET;
        int y = bounds.getY() + INSET;
        int width = Math.max(0, bounds.getWidth() - INSET * 2);
        int height = Math.max(0, bounds.getHeight() - INSET * 2);
        int heroHeight = Math.min(44, Math.max(36, height / 8));
        int footerHeight = Math.min(36, Math.max(30, height / 9));
        int bodyY = y + heroHeight + GAP;
        int bodyHeight = Math.max(0, height - heroHeight - footerHeight - GAP * 2);
        int chartWidth = Math.max(80, (width - GAP * 2) * 40 / 100);
        int bookWidth = Math.max(72, (width - GAP * 2) * 27 / 100);
        int ticketWidth = Math.max(0, width - chartWidth - bookWidth - GAP * 2);
        GuiRect hero = new GuiRect(x, y, width, heroHeight);
        GuiRect chart = new GuiRect(x, bodyY, chartWidth, bodyHeight);
        GuiRect book = new GuiRect(chart.getRight() + GAP, bodyY, bookWidth, bodyHeight);
        GuiRect ticket = new GuiRect(book.getRight() + GAP, bodyY, ticketWidth, bodyHeight);
        GuiRect footer = new GuiRect(x, bodyY + bodyHeight + GAP, width, footerHeight);
        return new MarketDetailLayout(hero, chart, book, ticket, footer);
    }

    static MarketDetailLayout withinStandardSplit(GuiRect bounds) {
        int x = bounds.getX() + INSET;
        int y = bounds.getY() + INSET;
        int width = Math.max(0, bounds.getWidth() - INSET * 2);
        int height = Math.max(0, bounds.getHeight() - INSET * 2);
        int availableWidth = Math.max(0, width - GAP);
        int leftWidth = availableWidth * 48 / 100;
        int rightWidth = Math.max(0, availableWidth - leftWidth);
        int heroHeight = Math.min(30, Math.max(24, height / 11));
        int ticketHeight = Math.min(84, Math.max(76, height * 24 / 100));
        int bookY = y + heroHeight + GAP;
        int bookHeight = Math.max(0, height - heroHeight - ticketHeight - GAP * 2);
        int ticketY = bookY + bookHeight + GAP;

        GuiRect hero = new GuiRect(x, y, leftWidth, heroHeight);
        GuiRect book = new GuiRect(x, bookY, leftWidth, bookHeight);
        GuiRect ticket = new GuiRect(x, ticketY, leftWidth, ticketHeight);
        GuiRect chart = new GuiRect(x + leftWidth + GAP, y, rightWidth, height);
        GuiRect footer = new GuiRect(x, y + height, 0, 0);
        return new MarketDetailLayout(hero, chart, book, ticket, footer);
    }
}

package com.jsirgalaxybase.modules.warehouse.client;

/** Pure sizing contract for the full-screen asset container. */
public final class TerminalAssetCenterLayout {
    public static final int MIN_WIDTH = 330;
    public static final int MIN_HEIGHT = 214;
    public static final int MAX_WIDTH = 520;
    public static final int MAX_HEIGHT = 286;

    private final int width;
    private final int height;

    private TerminalAssetCenterLayout(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static TerminalAssetCenterLayout compute(int screenWidth, int screenHeight) {
        int availableWidth = Math.max(1, screenWidth - 8);
        int availableHeight = Math.max(1, screenHeight - 8);
        return new TerminalAssetCenterLayout(Math.min(MAX_WIDTH, Math.max(Math.min(MIN_WIDTH, availableWidth), availableWidth)),
            Math.min(MAX_HEIGHT, Math.max(Math.min(MIN_HEIGHT, availableHeight), availableHeight)));
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getHeaderBottom() { return 25; }
    public int getSummaryTop() { return 27; }
    public int getSummaryBottom() { return 49; }
    public int getTabsTop() { return 51; }
    public int getTabsBottom() { return 70; }
    public int getBodyTop() { return 72; }
    public int getBodyBottom() { return Math.max(getBodyTop() + 54, height - 79); }
    public int getPlayerTop() { return Math.max(getBodyTop() + 58, height - 76); }
    public boolean fitsScreen(int screenWidth, int screenHeight) { return width <= screenWidth && height <= screenHeight; }
}

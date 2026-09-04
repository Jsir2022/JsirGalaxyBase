package com.jsirgalaxybase.modules.warehouse.client;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

/** Left native inventories and right detached-Cell browser inside the terminal body. */
public final class TerminalAssetCenterContentLayout {
    private final GuiRect body, storageTab, activityTab, leftPane, rightPane, bayBand, cellGrid;
    private final GuiRect depositButton, previousButton, nextButton;
    private final int vaultX, vaultY, playerX, playerY, baySlotX, baySlotY, cellColumns, cellRows, bodyBottomRelative;

    private TerminalAssetCenterContentLayout(GuiRect body, GuiRect storageTab, GuiRect activityTab,
        GuiRect leftPane, GuiRect rightPane, GuiRect bayBand, GuiRect cellGrid, GuiRect depositButton,
        GuiRect previousButton, GuiRect nextButton, int vaultX, int vaultY, int playerX, int playerY,
        int baySlotX, int baySlotY, int cellColumns, int cellRows, int bodyBottomRelative) {
        this.body = body; this.storageTab = storageTab; this.activityTab = activityTab;
        this.leftPane = leftPane; this.rightPane = rightPane; this.bayBand = bayBand; this.cellGrid = cellGrid;
        this.depositButton = depositButton; this.previousButton = previousButton; this.nextButton = nextButton;
        this.vaultX = vaultX; this.vaultY = vaultY; this.playerX = playerX; this.playerY = playerY;
        this.baySlotX = baySlotX; this.baySlotY = baySlotY; this.cellColumns = cellColumns; this.cellRows = cellRows;
        this.bodyBottomRelative = bodyBottomRelative;
    }

    public static TerminalAssetCenterContentLayout compute(GuiRect body, GuiRect panel) {
        GuiRect b = body == null ? new GuiRect(0, 0, 1, 1) : body;
        GuiRect p = panel == null ? new GuiRect(0, 0, 1, 1) : panel;
        int gap = 3;
        int tabWidth = Math.max(42, Math.min(86, (b.getWidth() - gap) / 2));
        GuiRect storage = new GuiRect(b.getX() + 2, b.getY() + 1, tabWidth, 13);
        GuiRect activity = new GuiRect(storage.getRight() + gap, storage.getY(), tabWidth, 13);
        int contentY = b.getY() + 16;
        int contentHeight = Math.max(1, b.getBottom() - contentY - 2);
        int leftWidth = Math.min(166, Math.max(162, b.getWidth() - 84));
        GuiRect left = new GuiRect(b.getX() + 2, contentY, leftWidth, contentHeight);
        GuiRect right = new GuiRect(left.getRight() + gap, contentY,
            Math.max(1, b.getRight() - left.getRight() - gap - 2), contentHeight);
        GuiRect bay = new GuiRect(right.getX(), right.getY(), right.getWidth(), 21);
        GuiRect grid = new GuiRect(right.getX() + 2, bay.getBottom() + 2,
            Math.max(1, right.getWidth() - 4), Math.max(1, right.getHeight() - 49));
        int columns = Math.max(1, grid.getWidth() / 20);
        int rows = Math.max(1, grid.getHeight() / 20);
        GuiRect deposit = new GuiRect(bay.getX() + 22, bay.getY() + 2, Math.max(1, bay.getWidth() - 24), 17);
        GuiRect previous = new GuiRect(right.getX() + 2, right.getBottom() - 14, 18, 12);
        GuiRect next = new GuiRect(right.getRight() - 20, right.getBottom() - 14, 18, 12);
        int relativeLeftX = left.getX() - p.getX();
        int vaultY = left.getY() - p.getY() + 12;
        int playerY = left.getY() - p.getY() + 77;
        return new TerminalAssetCenterContentLayout(b, storage, activity, left, right, bay, grid, deposit,
            previous, next, relativeLeftX + 2, vaultY, relativeLeftX + 2, playerY,
            bay.getX() - p.getX() + 2, bay.getY() - p.getY() + 2, columns, rows,
            b.getBottom() - p.getY());
    }

    public GuiRect getBody() { return body; }
    public GuiRect getStorageTab() { return storageTab; }
    public GuiRect getActivityTab() { return activityTab; }
    public GuiRect getLeftPane() { return leftPane; }
    public GuiRect getRightPane() { return rightPane; }
    public GuiRect getBayBand() { return bayBand; }
    public GuiRect getCellGrid() { return cellGrid; }
    public GuiRect getDepositButton() { return depositButton; }
    public GuiRect getPreviousButton() { return previousButton; }
    public GuiRect getNextButton() { return nextButton; }
    public int getVaultX() { return vaultX; }
    public int getVaultY() { return vaultY; }
    public int getPlayerX() { return playerX; }
    public int getPlayerY() { return playerY; }
    public int getBaySlotX() { return baySlotX; }
    public int getBaySlotY() { return baySlotY; }
    public int getCellColumns() { return cellColumns; }
    public int getCellRows() { return cellRows; }
    public int getCellPageSize() { return Math.max(1, Math.min(35, cellColumns * cellRows)); }

    public GuiRect cellEntryBounds(int index) {
        int column = Math.max(0, index) % cellColumns;
        int row = Math.max(0, index) / cellColumns;
        return new GuiRect(cellGrid.getX() + column * 20, cellGrid.getY() + row * 20, 18, 18);
    }

    public boolean fits() {
        return leftPane.getRight() <= body.getRight() && rightPane.getRight() <= body.getRight()
            && vaultY + 54 <= bodyBottomRelative && playerY + 70 <= bodyBottomRelative;
    }

    public boolean isCompact() { return body.getWidth() < 350; }
}

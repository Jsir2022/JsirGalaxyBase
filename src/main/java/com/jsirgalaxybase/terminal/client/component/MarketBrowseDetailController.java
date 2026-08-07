package com.jsirgalaxybase.terminal.client.component;

/**
 * Client-only navigation state shared by the non-standard market pages.
 * The server remains authoritative for the selected listing or formal quote.
 */
final class MarketBrowseDetailController {

    enum Mode { BROWSE, DETAIL }

    private Mode mode = Mode.BROWSE;
    private String query = "";
    private int pageIndex;
    private int gridScrollOffset;
    private String selectedKey = "";

    Mode getMode() { return mode; }
    boolean isDetail() { return mode == Mode.DETAIL; }
    void openDetail(String key) { selectedKey = key == null ? "" : key; mode = Mode.DETAIL; }
    void openBrowse() { mode = Mode.BROWSE; }
    void reset() {
        mode = Mode.BROWSE;
        query = "";
        pageIndex = 0;
        gridScrollOffset = 0;
        selectedKey = "";
    }
    String getQuery() { return query; }
    void setQuery(String value) { query = value == null ? "" : value.trim(); pageIndex = 0; gridScrollOffset = 0; }
    int getPageIndex() { return pageIndex; }
    void setPageIndex(int value) { pageIndex = Math.max(0, value); gridScrollOffset = 0; }
    int getGridScrollOffset() { return gridScrollOffset; }
    void setGridScrollOffset(int value) { gridScrollOffset = Math.max(0, value); }
    String getSelectedKey() { return selectedKey; }
    void setSelectedKey(String value) { selectedKey = value == null ? "" : value; }
}

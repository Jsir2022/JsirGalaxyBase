package com.jsirgalaxybase.ui2.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete platform-neutral visual state for the production asset center. */
public final class AssetCenterVisualModel {
    public enum Tab { STORAGE, ACTIVITY }
    private final TerminalVisualModel shell;
    private final Tab tab;
    private final boolean cellPresent;
    private final long usedBytes, totalBytes;
    private final long storedTypes, totalTypes;
    private final int pageIndex, totalPages;
    private final boolean cursorOccupied, helpOpen;
    private final String feedback;
    private final List<VisualItem> cellItems;
    private final List<Activity> activities;

    public AssetCenterVisualModel(TerminalVisualModel shell, Tab tab, boolean cellPresent,
        long usedBytes, long totalBytes, long storedTypes, long totalTypes, int pageIndex,
        int totalPages, boolean cursorOccupied, String feedback, List<VisualItem> cellItems,
        List<Activity> activities, boolean helpOpen) {
        if (shell == null) throw new IllegalArgumentException("shell is required");
        this.shell = shell; this.tab = tab == null ? Tab.STORAGE : tab; this.cellPresent = cellPresent;
        this.usedBytes = Math.max(0, usedBytes); this.totalBytes = Math.max(0, totalBytes);
        this.storedTypes = Math.max(0, storedTypes); this.totalTypes = Math.max(0, totalTypes);
        this.pageIndex = Math.max(0, pageIndex); this.totalPages = Math.max(1, totalPages);
        this.cursorOccupied = cursorOccupied; this.feedback = feedback == null ? "" : feedback;
        this.cellItems = immutable(cellItems); this.activities = activities == null
            ? Collections.<Activity>emptyList() : Collections.unmodifiableList(new ArrayList<Activity>(activities));
        this.helpOpen = helpOpen;
    }
    private static List<VisualItem> immutable(List<VisualItem> value) { return value == null
        ? Collections.<VisualItem>emptyList() : Collections.unmodifiableList(new ArrayList<VisualItem>(value)); }
    public TerminalVisualModel getShell(){return shell;} public Tab getTab(){return tab;}
    public boolean isCellPresent(){return cellPresent;} public long getUsedBytes(){return usedBytes;}
    public long getTotalBytes(){return totalBytes;} public long getStoredTypes(){return storedTypes;}
    public long getTotalTypes(){return totalTypes;} public int getPageIndex(){return pageIndex;}
    public int getTotalPages(){return totalPages;} public boolean isCursorOccupied(){return cursorOccupied;}
    public String getFeedback(){return feedback;} public List<VisualItem> getCellItems(){return cellItems;}
    public List<Activity> getActivities(){return activities;} public boolean isHelpOpen(){return helpOpen;}

    public static final class Activity {
        private final String id, label, detail, time;
        private final VisualItem item;
        public Activity(String id,String label,String detail,String time,VisualItem item){this.id=id==null?"":id;this.label=label==null?"":label;this.detail=detail==null?"":detail;this.time=time==null?"":time;this.item=item;}
        public String getId(){return id;} public String getLabel(){return label;} public String getDetail(){return detail;}
        public String getTime(){return time;} public VisualItem getItem(){return item;}
    }
}

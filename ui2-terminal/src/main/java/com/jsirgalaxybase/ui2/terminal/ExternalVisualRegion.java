package com.jsirgalaxybase.ui2.terminal;

/** Description for content painted by Java2D or Minecraft after UI2 layout. */
public final class ExternalVisualRegion {
    public enum Kind { ITEM, SLOT_GRID, CHART, MAP }
    private final String id;
    private final Kind kind;
    private final String label;

    public ExternalVisualRegion(String id, Kind kind, String label) {
        if (id == null || id.trim().isEmpty() || kind == null) throw new IllegalArgumentException("region id and kind are required");
        this.id = id.trim(); this.kind = kind; this.label = label == null ? "" : label;
    }
    public String getId() { return id; }
    public Kind getKind() { return kind; }
    public String getLabel() { return label; }
}

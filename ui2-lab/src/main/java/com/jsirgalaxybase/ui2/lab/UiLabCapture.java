package com.jsirgalaxybase.ui2.lab;

import com.jsirgalaxybase.ui2.render.DrawList;

/** Immutable result shared by the headless exporter and structural tests. */
public final class UiLabCapture {
    private final DrawList drawList;
    private final String debugSnapshot;

    UiLabCapture(DrawList drawList, String debugSnapshot) {
        this.drawList = drawList;
        this.debugSnapshot = debugSnapshot;
    }

    public DrawList getDrawList() { return drawList; }
    public String getDebugSnapshot() { return debugSnapshot; }
}

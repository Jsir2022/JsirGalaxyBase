package com.jsirgalaxybase.ui2.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shared visual and native-slot grid math. Hit boxes remain rectangular. */
public final class SlotGridGeometry {
    public static final int SLOT_SIZE = 16;
    public static final int GAP = 2;
    public static final int TOP_INSET = 2;

    private SlotGridGeometry() {}

    public static List<UiRect> cells(UiRect region, int columns, int requestedCount) {
        if (region == null || columns < 1 || requestedCount < 1) return Collections.emptyList();
        int total = columns * SLOT_SIZE + (columns - 1) * GAP;
        int startX = region.getX() + Math.max(0, (region.getWidth() - total) / 2);
        int startY = region.getY() + TOP_INSET;
        List<UiRect> cells = new ArrayList<UiRect>();
        for (int index = 0; index < requestedCount; index++) {
            int x = startX + (index % columns) * (SLOT_SIZE + GAP);
            int y = startY + (index / columns) * (SLOT_SIZE + GAP);
            if (x + SLOT_SIZE <= region.getRight() && y + SLOT_SIZE <= region.getBottom()) {
                cells.add(new UiRect(x, y, SLOT_SIZE, SLOT_SIZE));
            }
        }
        return Collections.unmodifiableList(cells);
    }
}

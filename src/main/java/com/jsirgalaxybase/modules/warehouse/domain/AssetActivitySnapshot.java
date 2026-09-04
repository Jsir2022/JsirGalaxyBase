package com.jsirgalaxybase.modules.warehouse.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AssetActivitySnapshot {
    public static final int MAX_ROWS = 20;
    private final List<AssetActivityRow> rows;

    public AssetActivitySnapshot(List<AssetActivityRow> rows) {
        List<AssetActivityRow> bounded = new ArrayList<AssetActivityRow>();
        if (rows != null) for (AssetActivityRow row : rows) {
            if (row != null) bounded.add(row);
            if (bounded.size() >= MAX_ROWS) break;
        }
        this.rows = Collections.unmodifiableList(bounded);
    }

    public List<AssetActivityRow> getRows() { return rows; }
    public static AssetActivitySnapshot empty() { return new AssetActivitySnapshot(null); }
}

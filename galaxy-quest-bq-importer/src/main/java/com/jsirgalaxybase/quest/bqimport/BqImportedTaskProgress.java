package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BqImportedTaskProgress {
    private final String key;
    private final String typeId;
    private final boolean complete;
    private final List<Long> values;
    private final String rawJson;

    BqImportedTaskProgress(String key, String typeId, boolean complete, List<Long> values, String rawJson) {
        this.key = key;
        this.typeId = typeId;
        this.complete = complete;
        this.values = Collections.unmodifiableList(new ArrayList<Long>(values));
        this.rawJson = rawJson;
    }

    public String getKey() { return key; }
    public String getTypeId() { return typeId; }
    public boolean isComplete() { return complete; }
    public List<Long> getValues() { return values; }
    public String getRawJson() { return rawJson; }
}

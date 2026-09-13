package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BqStandardRewardSpec {
    private final String key;
    private final String typeId;
    private final Map<String, String> options;
    private final List<BqItemStackSpec> items;

    BqStandardRewardSpec(String key, String typeId, Map<String, String> options, List<BqItemStackSpec> items) {
        this.key = key;
        this.typeId = typeId;
        this.options = Collections.unmodifiableMap(new LinkedHashMap<String, String>(options));
        this.items = Collections.unmodifiableList(new ArrayList<BqItemStackSpec>(items));
    }

    public String getKey() { return key; }
    public String getTypeId() { return typeId; }
    public Map<String, String> getOptions() { return options; }
    public List<BqItemStackSpec> getItems() { return items; }
}

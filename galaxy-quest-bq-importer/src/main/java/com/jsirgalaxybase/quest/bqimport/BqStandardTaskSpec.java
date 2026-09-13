package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BqStandardTaskSpec {
    private final String key;
    private final String typeId;
    private final Map<String, String> options;
    private final List<BqItemStackSpec> items;
    private final List<BqFluidStackSpec> fluids;
    private final List<BqBlockSpec> blocks;

    BqStandardTaskSpec(String key, String typeId, Map<String, String> options, List<BqItemStackSpec> items,
        List<BqFluidStackSpec> fluids, List<BqBlockSpec> blocks) {
        this.key = key;
        this.typeId = typeId;
        this.options = Collections.unmodifiableMap(new LinkedHashMap<String, String>(options));
        this.items = Collections.unmodifiableList(new ArrayList<BqItemStackSpec>(items));
        this.fluids = Collections.unmodifiableList(new ArrayList<BqFluidStackSpec>(fluids));
        this.blocks = Collections.unmodifiableList(new ArrayList<BqBlockSpec>(blocks));
    }

    public String getKey() { return key; }
    public String getTypeId() { return typeId; }
    public Map<String, String> getOptions() { return options; }
    public List<BqItemStackSpec> getItems() { return items; }
    public List<BqFluidStackSpec> getFluids() { return fluids; }
    public List<BqBlockSpec> getBlocks() { return blocks; }
}

package com.jsirgalaxybase.modules.core.market.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.TaskCoinDescriptor;

/** The explicit Dreamcraft task-book coin catalog accepted by the exchange market. */
public final class TaskCoinCatalog {

    private static final String REGISTRY_PREFIX = TaskCoinExchangePlanner.REGISTRY_PREFIX;
    private static final String[] TIER_SUFFIXES = { "", "I", "II", "III", "IV" };
    private static final String[] TIER_NAMES = { "BASE", "I", "II", "III", "IV" };
    private static final long[] FACE_VALUES = { 1L, 10L, 100L, 1000L, 10000L };
    private static final String[][] FAMILIES = {
        { "DarkWizard", "魔法师" }, { "Technician", "技术员" }, { "Farmer", "农民" },
        { "Flower", "园艺家" }, { "Forestry", "护林员" }, { "Adventure", "探险家" },
        { "Bees", "养蜂员" }, { "Blood", "吸血鬼" }, { "Survivor", "幸存者" },
        { "Space", "太空" }, { "Blank", "空白" }, { "Chemist", "化学家" },
        { "Cook", "厨师" }, { "Smith", "匠师" }, { "Witch", "巫师" }
    };

    private static final TaskCoinCatalog DEFAULT = new TaskCoinCatalog();
    private final List<Entry> entries;
    private final Map<String, Entry> byRegistryName;

    private TaskCoinCatalog() {
        List<Entry> resolvedEntries = new ArrayList<Entry>();
        Map<String, Entry> resolvedByRegistry = new LinkedHashMap<String, Entry>();
        for (String[] family : FAMILIES) {
            for (int tierIndex = 0; tierIndex < TIER_SUFFIXES.length; tierIndex++) {
                String registryName = REGISTRY_PREFIX + family[0] + TIER_SUFFIXES[tierIndex];
                Entry entry = new Entry(registryName, family[0], family[1], TIER_NAMES[tierIndex], FACE_VALUES[tierIndex]);
                resolvedEntries.add(entry);
                resolvedByRegistry.put(registryName, entry);
            }
        }
        entries = Collections.unmodifiableList(resolvedEntries);
        byRegistryName = Collections.unmodifiableMap(resolvedByRegistry);
    }

    public static TaskCoinCatalog defaultCatalog() {
        return DEFAULT;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public Optional<Entry> find(String registryName) {
        if (registryName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byRegistryName.get(registryName.trim()));
    }

    public static final class Entry {
        private final String registryName;
        private final String familyCode;
        private final String familyDisplayName;
        private final String tier;
        private final long faceValue;

        private Entry(String registryName, String familyCode, String familyDisplayName, String tier, long faceValue) {
            this.registryName = registryName;
            this.familyCode = familyCode;
            this.familyDisplayName = familyDisplayName;
            this.tier = tier;
            this.faceValue = faceValue;
        }

        public String getRegistryName() { return registryName; }
        public String getFamilyCode() { return familyCode; }
        public String getFamilyDisplayName() { return familyDisplayName; }
        public String getTier() { return tier; }
        public long getFaceValue() { return faceValue; }
        public String getDisplayName() { return familyDisplayName + "币 $" + faceValue; }
        public TaskCoinDescriptor toDescriptor() {
            return new TaskCoinDescriptor(registryName, familyCode, tier, faceValue);
        }
    }
}

package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.PortableNbt;
import com.jsirgalaxybase.quest.core.RepeatPolicy;

/** Read-only M.2 inventory. A recognized codec is not evidence of equivalent runtime behavior. */
public final class BqBehaviorInventory {
    private final String sourceHash;
    private final Map<String, TypeUsage> tasks;
    private final Map<String, TypeUsage> rewards;
    private final Map<String, QuestProfile> questProfiles;
    private final Map<String, Integer> questCompletionProfiles;
    private final int locationTaskCount;
    private final Map<String, Map<String, Integer>> locationProfiles;
    private final int distinctLocationStructures;
    private final List<String> manifestBlockers;

    private BqBehaviorInventory(String sourceHash, Map<String, TypeUsage> tasks,
        Map<String, TypeUsage> rewards, Map<String, QuestProfile> questProfiles,
        Map<String, Integer> questCompletionProfiles, int locationTaskCount,
        Map<String, Map<String, Integer>> locationProfiles, int distinctLocationStructures,
        List<String> manifestBlockers) {
        this.sourceHash = sourceHash;
        this.tasks = Collections.unmodifiableMap(tasks);
        this.rewards = Collections.unmodifiableMap(rewards);
        this.questProfiles = Collections.unmodifiableMap(questProfiles);
        this.questCompletionProfiles = Collections.unmodifiableMap(questCompletionProfiles);
        this.locationTaskCount = locationTaskCount;
        this.locationProfiles = immutableNestedMap(locationProfiles);
        this.distinctLocationStructures = distinctLocationStructures;
        this.manifestBlockers = Collections.unmodifiableList(new ArrayList<String>(manifestBlockers));
    }

    public static BqBehaviorInventory from(BqMigrationManifest manifest) {
        Map<String, TypeUsage> tasks = new TreeMap<String, TypeUsage>();
        Map<String, TypeUsage> rewards = new TreeMap<String, TypeUsage>();
        Map<String, QuestProfile> questProfiles = new TreeMap<String, QuestProfile>();
        Map<String, Map<String, Integer>> locationProfiles = new TreeMap<String, Map<String, Integer>>();
        Set<String> locationStructures = new HashSet<String>();
        int locationTaskCount = 0;
        Map<UUID, BqImportedQuest> questsById = new HashMap<UUID, BqImportedQuest>();
        for (BqImportedQuest quest : manifest.getQuests()) questsById.put(quest.getDefinition().getId(), quest);
        Map<String, Integer> questCompletionProfiles = questCompletionProfiles(manifest.getQuests(), questsById);
        for (BqImportedQuest quest : manifest.getQuests()) {
            String questProfileKey = questProfileKey(quest.getDefinition());
            QuestProfile questProfile = questProfiles.get(questProfileKey);
            if (questProfile == null) {
                questProfile = new QuestProfile();
                questProfiles.put(questProfileKey, questProfile);
            }
            questProfile.add(quest);
            for (BqStandardTaskSpec spec : quest.getTaskSpecs()) {
                usage(tasks, spec.getTypeId()).add(spec.getOptions(), spec.getItems(), spec.getFluids(),
                    spec.getBlocks(), BqRuntimeCapabilityRegistry.taskIntegerOptionKeys().get(spec.getTypeId()));
                if ("bq_standard:location".equals(spec.getTypeId())) {
                    locationTaskCount++;
                    addLocationProfile(locationProfiles, spec.getOptions());
                    increment(locationProfiles, "repeatable", Boolean.toString(
                        quest.getDefinition().getRepeatPolicy().isRepeatable()));
                    increment(locationProfiles, "lockedProgress", Boolean.toString(
                        quest.getDefinition().getBehavior().isProgressWhileLocked()));
                    increment(locationProfiles, "globalShare", Boolean.toString(
                        quest.getDefinition().getBehavior().isGlobalShare()));
                    increment(locationProfiles, "autoClaim", Boolean.toString(
                        quest.getDefinition().getBehavior().isAutoClaim()));
                    increment(locationProfiles, "hasRewards", Boolean.toString(!quest.getRewardSpecs().isEmpty()));
                    increment(locationProfiles, "repeatCycle", repeatCycleProfile(quest.getDefinition()));
                    increment(locationProfiles, "repeatClaimGate", repeatClaimGateProfile(quest));
                    String structure = spec.getOptions().get("structure");
                    if (structure != null && !structure.trim().isEmpty()) locationStructures.add(structure.trim());
                }
            }
            for (BqStandardRewardSpec spec : quest.getRewardSpecs()) {
                usage(rewards, spec.getTypeId()).add(spec.getOptions(),
                    BqRuntimeCapabilityRegistry.rewardIntegerOptionKeys().get(spec.getTypeId()));
            }
        }
        List<String> manifestBlockers = new ArrayList<String>();
        for (BqMigrationIssue issue : manifest.getIssues()) if (issue.getSeverity() == BqMigrationIssue.Severity.ERROR) {
            manifestBlockers.add(issue.getCode() + " " + issue.getSubject());
        }
        return new BqBehaviorInventory(manifest.getSourceHash(), tasks, rewards, questProfiles, questCompletionProfiles,
            locationTaskCount, locationProfiles, locationStructures.size(), manifestBlockers);
    }

    public String getSourceHash() { return sourceHash; }
    public Map<String, TypeUsage> getTasks() { return tasks; }
    public Map<String, TypeUsage> getRewards() { return rewards; }
    public Map<String, QuestProfile> getQuestProfiles() { return questProfiles; }
    public Map<String, Integer> getQuestCompletionProfiles() { return questCompletionProfiles; }
    public int getLocationTaskCount() { return locationTaskCount; }
    public Map<String, Map<String, Integer>> getLocationProfiles() { return locationProfiles; }
    public int getDistinctLocationStructures() { return distinctLocationStructures; }

    /** Content parsing is not permission to publish unverified task or reward behavior. */
    public void requireRuntimeReady() {
        List<String> blocked = new ArrayList<String>();
        for (String blocker : manifestBlockers) blocked.add("MANIFEST " + blocker);
        for (String type : tasks.keySet()) if (BqRuntimeCapabilityRegistry.taskStatus(type)
            != BqCompatibilityStatus.SUPPORTED) blocked.add("TASK " + type + "="
                + BqRuntimeCapabilityRegistry.taskStatus(type));
        for (String type : rewards.keySet()) if (BqRuntimeCapabilityRegistry.rewardStatus(type)
            != BqCompatibilityStatus.SUPPORTED) blocked.add("REWARD " + type + "="
                + BqRuntimeCapabilityRegistry.rewardStatus(type));
        checkRuntimeOptions("TASK", tasks, BqRuntimeCapabilityRegistry.taskOptionKeys(), blocked);
        checkRuntimeOptions("REWARD", rewards, BqRuntimeCapabilityRegistry.rewardOptionKeys(), blocked);
        checkRuntimeIntegerOptions("TASK", tasks, blocked);
        checkRuntimeIntegerOptions("REWARD", rewards, blocked);
        if (!blocked.isEmpty()) throw new IllegalStateException("BQ runtime is not ready: " + blocked);
    }

    private static void checkRuntimeIntegerOptions(String kind, Map<String, TypeUsage> usages, List<String> blocked) {
        for (Map.Entry<String, TypeUsage> entry : usages.entrySet()) {
            for (String option : entry.getValue().getInvalidIntegerOptions().keySet()) {
                blocked.add(kind + " " + entry.getKey() + " INTEGER " + option + "=INVALID_VALUE");
            }
        }
    }

    private static void checkRuntimeOptions(String kind, Map<String, TypeUsage> usages,
        Map<String, Set<String>> supportedKeys, List<String> blocked) {
        for (Map.Entry<String, TypeUsage> entry : usages.entrySet()) {
            String type = entry.getKey();
            Set<String> expected = supportedKeys.get(type);
            if (expected == null) continue;
            for (String option : entry.getValue().getOptionKeys().keySet()) {
                if (!expected.contains(option)) blocked.add(kind + " " + type + " OPTION " + option + "=UNVERIFIED");
            }
            for (Map.Entry<String, Map<String, Integer>> flag : entry.getValue().getBehaviorFlags().entrySet()) {
                if (flag.getValue().containsKey("invalid")) {
                    blocked.add(kind + " " + type + " FLAG " + flag.getKey() + "=INVALID_VALUE");
                }
            }
        }
    }

    public String toText() {
        StringBuilder out = new StringBuilder("sourceHash=").append(sourceHash).append('\n');
        append(out, "TASK", tasks, true);
        append(out, "REWARD", rewards, false);
        for (Map.Entry<String, QuestProfile> profile : questProfiles.entrySet()) {
            QuestProfile usage = profile.getValue();
            out.append("QUEST_PROFILE|").append(profile.getKey()).append("|quests=")
                .append(usage.questCount).append("|tasks=").append(usage.taskTypes)
                .append("|rewards=").append(usage.rewardTypes).append('\n');
        }
        if (!questCompletionProfiles.isEmpty()) {
            out.append("QUEST_COMPLETION|references=").append(questCompletionProfiles).append('\n');
        }
        if (locationTaskCount > 0) {
            out.append("LOCATION_PROFILE|tasks=").append(locationTaskCount)
                .append("|uniqueStructures=").append(distinctLocationStructures);
            for (Map.Entry<String, Map<String, Integer>> profile : locationProfiles.entrySet()) {
                out.append('|').append(profile.getKey()).append('=').append(profile.getValue());
            }
            out.append('\n');
        }
        return out.toString();
    }

    private static String questProfileKey(QuestDefinition definition) {
        return "global=" + definition.getBehavior().isGlobal()
            + ",globalShare=" + definition.getBehavior().isGlobalShare()
            + ",autoClaim=" + definition.getBehavior().isAutoClaim()
            + ",lockedProgress=" + definition.getBehavior().isProgressWhileLocked()
            + ",simultaneous=" + definition.getBehavior().isSimultaneous()
            + ",repeatable=" + definition.getRepeatPolicy().isRepeatable();
    }

    /** Aggregate quest-level BQ flags and contained type frequencies without retaining quest identifiers. */
    public static final class QuestProfile {
        private int questCount;
        private final Map<String, Integer> taskTypes = new TreeMap<String, Integer>();
        private final Map<String, Integer> rewardTypes = new TreeMap<String, Integer>();

        private void add(BqImportedQuest quest) {
            questCount++;
            for (BqStandardTaskSpec task : quest.getTaskSpecs()) increment(taskTypes, task.getTypeId());
            for (BqStandardRewardSpec reward : quest.getRewardSpecs()) increment(rewardTypes, reward.getTypeId());
        }

        public int getQuestCount() { return questCount; }
        public Map<String, Integer> getTaskTypes() {
            return Collections.unmodifiableMap(taskTypes);
        }
        public Map<String, Integer> getRewardTypes() {
            return Collections.unmodifiableMap(rewardTypes);
        }
    }

    private static void addLocationProfile(Map<String, Map<String, Integer>> profiles,
        Map<String, String> options) {
        increment(profiles, "dimension", numericProfile(options.get("dimension"), "unknown"));
        increment(profiles, "biome", numericProfile(options.get("biome"), "unknown"));
        increment(profiles, "range", rangeProfile(options.get("range")));
        String structure = options.get("structure");
        increment(profiles, "structure", structure == null || structure.trim().isEmpty() ? "empty" : "configured");
        boolean origin = number(options.get("posX"), 0) == 0 && number(options.get("posY"), 0) == 0
            && number(options.get("posZ"), 0) == 0;
        increment(profiles, "target", origin ? "origin" : "nonOrigin");
        increment(profiles, "visible", flag(options.get("visible"), false));
        increment(profiles, "invert", flag(options.get("invert"), false));
        increment(profiles, "taxiCabDist", flag(options.get("taxiCabDist"), false));
        increment(profiles, "hideInfo", flag(options.get("hideInfo"), false));
    }

    private static String numericProfile(String raw, String missing) {
        if (raw == null || raw.trim().isEmpty()) return missing;
        try { return Integer.toString(Integer.parseInt(raw.trim())); }
        catch (NumberFormatException malformed) { return "invalid"; }
    }

    private static String rangeProfile(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "unknown";
        try { return Integer.parseInt(raw.trim()) <= 0 ? "unbounded" : "finite"; }
        catch (NumberFormatException malformed) { return "invalid"; }
    }

    private static String repeatCycleProfile(QuestDefinition definition) {
        RepeatPolicy repeat = definition.getRepeatPolicy();
        if (!repeat.isRepeatable()) return "none";
        String mode = repeat.isRelative() ? "relative-" : "aligned-";
        return mode + Long.toString(repeat.getCooldownMillis() / 50L);
    }

    private static String repeatClaimGateProfile(BqImportedQuest quest) {
        if (!quest.getDefinition().getRepeatPolicy().isRepeatable()) return "notRepeating";
        if (quest.getRewardSpecs().isEmpty()) return "repeat-noReward";
        return quest.getDefinition().getBehavior().isAutoClaim() ? "repeat-autoClaim" : "repeat-manualClaim";
    }

    private static int number(String raw, int fallback) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try { return Integer.parseInt(raw.trim()); }
        catch (NumberFormatException malformed) { return fallback; }
    }

    private static String flag(String raw, boolean fallback) {
        if (raw == null) return Boolean.toString(fallback);
        if ("1".equals(raw) || "true".equalsIgnoreCase(raw)) return "true";
        if ("0".equals(raw) || "false".equalsIgnoreCase(raw)) return "false";
        return "invalid";
    }

    private static void increment(Map<String, Map<String, Integer>> profiles, String key, String value) {
        Map<String, Integer> values = profiles.get(key);
        if (values == null) {
            values = new TreeMap<String, Integer>();
            profiles.put(key, values);
        }
        increment(values, value);
    }

    private static Map<String, Map<String, Integer>> immutableNestedMap(
        Map<String, Map<String, Integer>> source) {
        Map<String, Map<String, Integer>> result = new TreeMap<String, Map<String, Integer>>();
        for (Map.Entry<String, Map<String, Integer>> entry : source.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableMap(new TreeMap<String, Integer>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Integer> questCompletionProfiles(List<BqImportedQuest> quests,
        Map<UUID, BqImportedQuest> questsById) {
        Map<String, Integer> profiles = new TreeMap<String, Integer>();
        Set<UUID> sourceIds = new HashSet<UUID>();
        Set<UUID> targetIds = new HashSet<UUID>();
        for (BqImportedQuest source : quests) {
            UUID sourceId = source.getDefinition().getId();
            for (BqStandardRewardSpec reward : source.getRewardSpecs()) {
                if (!"bq_standard:questcompletion".equals(reward.getTypeId())) continue;
                increment(profiles, "references");
                sourceIds.add(sourceId);
                if (source.getDefinition().getRepeatPolicy().isRepeatable()) increment(profiles, "sourceRepeatable");
                if (source.getDefinition().getBehavior().isGlobal()) increment(profiles, "sourceGlobal");
                if (source.getDefinition().getBehavior().isGlobalShare()) increment(profiles, "sourceGlobalShare");
                if (source.getDefinition().getBehavior().isAutoClaim()) increment(profiles, "sourceAutoClaim");

                UUID targetId;
                try {
                    targetId = UUID.fromString(reward.getOptions().get("questUuid"));
                } catch (RuntimeException malformed) {
                    increment(profiles, "malformedTarget");
                    continue;
                }
                if (sourceId.equals(targetId)) increment(profiles, "selfReference");
                BqImportedQuest target = questsById.get(targetId);
                if (target == null) {
                    increment(profiles, "missingTarget");
                    continue;
                }
                targetIds.add(targetId);
                if (target.getDefinition().getRepeatPolicy().isRepeatable()) increment(profiles, "targetRepeatable");
                if (target.getDefinition().getBehavior().isGlobal()) increment(profiles, "targetGlobal");
                if (target.getDefinition().getBehavior().isGlobalShare()) increment(profiles, "targetGlobalShare");
                if (target.getDefinition().getBehavior().isAutoClaim()) increment(profiles, "targetAutoClaim");
            }
        }
        if (!profiles.isEmpty()) {
            String[] counters = { "sourceRepeatable", "sourceGlobal", "sourceGlobalShare", "targetRepeatable",
                "sourceAutoClaim", "targetGlobal", "targetGlobalShare", "targetAutoClaim", "malformedTarget",
                "selfReference", "missingTarget" };
            for (String counter : counters) if (!profiles.containsKey(counter)) profiles.put(counter, 0);
            profiles.put("uniqueSourceQuests", sourceIds.size());
            profiles.put("uniqueTargetQuests", targetIds.size());
        }
        return profiles;
    }

    private static void increment(Map<String, Integer> values, String key) {
        Integer previous = values.get(key);
        values.put(key, previous == null ? 1 : previous + 1);
    }

    private static TypeUsage usage(Map<String, TypeUsage> values, String type) {
        TypeUsage existing = values.get(type);
        if (existing != null) return existing;
        TypeUsage created = new TypeUsage();
        values.put(type, created);
        return created;
    }

    private static void append(StringBuilder out, String kind, Map<String, TypeUsage> usages, boolean task) {
        List<Map.Entry<String, TypeUsage>> ordered = new ArrayList<Map.Entry<String, TypeUsage>>(usages.entrySet());
        Collections.sort(ordered, new Comparator<Map.Entry<String, TypeUsage>>() {
            @Override public int compare(Map.Entry<String, TypeUsage> left, Map.Entry<String, TypeUsage> right) {
                int count = Integer.compare(right.getValue().count, left.getValue().count);
                return count != 0 ? count : left.getKey().compareTo(right.getKey());
            }
        });
        for (Map.Entry<String, TypeUsage> entry : ordered) {
            TypeUsage usage = entry.getValue();
            out.append(kind).append('|').append(entry.getKey()).append('|').append(usage.count)
                .append('|').append(task ? BqRuntimeCapabilityRegistry.taskStatus(entry.getKey())
                    : BqRuntimeCapabilityRegistry.rewardStatus(entry.getKey()))
                .append("|optionKeys=").append(usage.optionKeys)
                .append("|behaviorFlags=").append(usage.behaviorFlags)
                .append("|behaviorProfiles=").append(usage.behaviorProfiles);
            if (usage.itemRequirements > 0) out.append("|itemMatchProfiles=").append(usage.itemMatchProfiles);
            if (!usage.itemAmountProfiles.isEmpty()) out.append("|itemAmountValues=")
                .append(usage.itemAmountProfiles.size());
            if (!usage.invalidIntegerOptions.isEmpty()) out.append("|invalidIntegerOptions=")
                .append(usage.invalidIntegerOptions);
            if (!usage.integerOptionProfiles.isEmpty()) out.append("|integerOptionProfiles=")
                .append(usage.integerOptionProfiles);
            if (usage.fluidRequirements > 0) out.append("|fluidRequirements=").append(usage.fluidRequirements)
                .append("|fluidAmountProfiles=").append(usage.fluidAmountProfiles);
            if (!usage.blockAmountProfiles.isEmpty()) out.append("|blockAmountProfiles=")
                .append(usage.blockAmountProfiles);
            if (!usage.blockMetaProfiles.isEmpty()) out.append("|blockMetaProfiles=")
                .append(usage.blockMetaProfiles);
            out.append('\n');
        }
    }

    public static final class TypeUsage {
        private int count;
        private final Map<String, Integer> optionKeys = new TreeMap<String, Integer>();
        private final Map<String, Map<String, Integer>> behaviorFlags =
            new TreeMap<String, Map<String, Integer>>();
        private final Map<String, Integer> invalidIntegerOptions = new TreeMap<String, Integer>();
        private final Map<String, Map<String, Integer>> integerOptionProfiles =
            new TreeMap<String, Map<String, Integer>>();
        private final Map<String, Integer> behaviorProfiles = new TreeMap<String, Integer>();
        private final Map<String, Integer> itemMatchProfiles = new TreeMap<String, Integer>();
        private final Map<String, Integer> itemAmountProfiles = new TreeMap<String, Integer>();
        private int itemRequirements;
        private final Map<String, Integer> fluidAmountProfiles = new TreeMap<String, Integer>();
        private int fluidRequirements;
        private final Map<String, Integer> blockAmountProfiles = new TreeMap<String, Integer>();
        private final Map<String, Integer> blockMetaProfiles = new TreeMap<String, Integer>();
        private void add(Map<String, String> options, Set<String> integerKeys) {
            count++;
            for (String key : options.keySet()) {
                Integer previous = optionKeys.get(key);
                optionKeys.put(key, previous == null ? 1 : previous + 1);
                if (isBehaviorFlag(key)) {
                    String value = normalizeFlag(options.get(key));
                    Map<String, Integer> variants = behaviorFlags.get(key);
                    if (variants == null) {
                        variants = new TreeMap<String, Integer>();
                        behaviorFlags.put(key, variants);
                    }
                    Integer seen = variants.get(value);
                    variants.put(value, seen == null ? 1 : seen + 1);
                }
            }
            if (integerKeys != null) for (String key : integerKeys) {
                String value = options.get(key);
                if (value == null) continue;
                try {
                    String normalized = Integer.toString(Integer.parseInt(value.trim()));
                    Map<String, Integer> values = integerOptionProfiles.get(key);
                    if (values == null) {
                        values = new TreeMap<String, Integer>();
                        integerOptionProfiles.put(key, values);
                    }
                    increment(values, normalized);
                }
                catch (RuntimeException invalid) { increment(invalidIntegerOptions, key); }
            }
            String profile = behaviorProfile(options);
            if (!profile.isEmpty()) increment(behaviorProfiles, profile);
        }

        private static String behaviorProfile(Map<String, String> options) {
            List<String> keys = new ArrayList<String>();
            for (String key : options.keySet()) if (isBehaviorFlag(key)) keys.add(key);
            Collections.sort(keys);
            StringBuilder profile = new StringBuilder();
            for (String key : keys) {
                if (profile.length() > 0) profile.append(';');
                profile.append(key).append('=').append(normalizeFlag(options.get(key)));
            }
            return profile.toString();
        }

        private void add(Map<String, String> options, List<BqItemStackSpec> items,
            List<BqFluidStackSpec> fluids, List<BqBlockSpec> blocks, Set<String> integerKeys) {
            add(options, integerKeys);
            if (items != null) {
                boolean nbtCheckConfigured = options.containsKey("ignoreNBT") && "false".equals(options.get("ignoreNBT"));
                boolean partialMatchDisabled = options.containsKey("partialMatch")
                    && "false".equals(options.get("partialMatch"));
                for (BqItemStackSpec item : items) {
                    itemRequirements++;
                    increment(itemAmountProfiles, Integer.toString(item.getCount()));
                    increment(itemMatchProfiles, "itemRequirements");
                    boolean oreDictionary = item.getOreDictionary() != null && !item.getOreDictionary().trim().isEmpty();
                    boolean nonEmptyNbt = item.getTagJson() != null && !item.getTagJson().trim().isEmpty()
                        && !"{}".equals(item.getTagJson().trim());
                    if (oreDictionary) increment(itemMatchProfiles, "oreDictionary");
                    if (nonEmptyNbt) increment(itemMatchProfiles, "nonEmptyNbt");
                    if (partialMatchDisabled) increment(itemMatchProfiles, "partialMatchDisabledItemRequirements");
                    if (partialMatchDisabled && nbtCheckConfigured) {
                        increment(itemMatchProfiles, "partialMatchDisabledAndNbtCheckEnabled");
                    }
                    if (nbtCheckConfigured) {
                        increment(itemMatchProfiles, "itemRequirementsWithNbtCheckEnabled");
                        if (nonEmptyNbt) increment(itemMatchProfiles, "itemRequirementsWithNbtCheckEnabledWithData");
                        else increment(itemMatchProfiles, "itemRequirementsWithNbtCheckEnabledEmptyTag");
                    }
                    if (partialMatchDisabled && nbtCheckConfigured && nonEmptyNbt) {
                        increment(itemMatchProfiles, "partialMatchDisabledAndNbtCheckEnabledWithData");
                    }
                    if (oreDictionary && nonEmptyNbt) increment(itemMatchProfiles, "oreDictionaryAndNonEmptyNbt");
                    if (oreDictionary && nbtCheckConfigured) {
                        increment(itemMatchProfiles, "oreDictionaryAndNbtCheckEnabled");
                        if (nonEmptyNbt) increment(itemMatchProfiles, "oreDictionaryAndNbtCheckEnabledWithData");
                    }
                    if (nonEmptyNbt) addNbtShape(BqPortableNbtCodec.encodeJson(item.getTagJson()), nbtCheckConfigured);
                }
            }
            if (fluids != null) {
                for (BqFluidStackSpec fluid : fluids) {
                    fluidRequirements++;
                    increment(fluidAmountProfiles, Integer.toString(fluid.getAmount()));
                }
            }
            if (blocks != null) for (BqBlockSpec block : blocks) {
                increment(blockAmountProfiles, Integer.toString(block.getAmount()));
                increment(blockMetaProfiles, Integer.toString(block.getMeta()));
            }
        }

        private void addNbtShape(String encoded, boolean nbtCheckConfigured) {
            PortableNbt root = PortableNbt.decode(encoded);
            increment(itemMatchProfiles, "nbtRoot." + root.getKind().name().toLowerCase(java.util.Locale.ROOT));
            addNbtShapeNodes(root, nbtCheckConfigured);
        }

        private void addNbtShapeNodes(PortableNbt node, boolean nbtCheckConfigured) {
            increment(itemMatchProfiles, "nbtNode." + node.getKind().name().toLowerCase(java.util.Locale.ROOT));
            if (node.getKind() == PortableNbt.Kind.COMPOUND) {
                for (PortableNbt value : node.getFields().values()) addNbtShapeNodes(value, nbtCheckConfigured);
            } else if (node.getKind() == PortableNbt.Kind.LIST
                || node.getKind() == PortableNbt.Kind.BYTE_ARRAY
                || node.getKind() == PortableNbt.Kind.INT_ARRAY) {
                for (PortableNbt value : node.getValues()) {
                    if (node.getKind() == PortableNbt.Kind.LIST) {
                        String kind = value.getKind().name().toLowerCase(java.util.Locale.ROOT);
                        increment(itemMatchProfiles, "nbtListElement." + kind);
                        if (nbtCheckConfigured) increment(itemMatchProfiles,
                            "nbtListElementWithNbtCheckEnabled." + kind);
                    }
                    addNbtShapeNodes(value, nbtCheckConfigured);
                }
            }
        }

        private static void increment(Map<String, Integer> values, String key) {
            Integer previous = values.get(key);
            values.put(key, previous == null ? 1 : previous + 1);
        }

        public int getCount() { return count; }
        public Map<String, Integer> getOptionKeys() { return Collections.unmodifiableMap(optionKeys); }
        public Map<String, Integer> getBehaviorProfiles() {
            return Collections.unmodifiableMap(behaviorProfiles);
        }
        public Map<String, Map<String, Integer>> getBehaviorFlags() {
            Map<String, Map<String, Integer>> result = new TreeMap<String, Map<String, Integer>>();
            for (Map.Entry<String, Map<String, Integer>> entry : behaviorFlags.entrySet()) {
                result.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
            }
            return Collections.unmodifiableMap(result);
        }
        public Map<String, Integer> getInvalidIntegerOptions() {
            return Collections.unmodifiableMap(invalidIntegerOptions);
        }
        public Map<String, Map<String, Integer>> getIntegerOptionProfiles() {
            Map<String, Map<String, Integer>> result = new TreeMap<String, Map<String, Integer>>();
            for (Map.Entry<String, Map<String, Integer>> entry : integerOptionProfiles.entrySet()) {
                result.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
            }
            return Collections.unmodifiableMap(result);
        }
        public Map<String, Integer> getItemMatchProfiles() {
            return Collections.unmodifiableMap(itemMatchProfiles);
        }
        public Map<String, Integer> getItemAmountProfiles() {
            return Collections.unmodifiableMap(itemAmountProfiles);
        }
        public int getFluidRequirements() { return fluidRequirements; }
        public Map<String, Integer> getFluidAmountProfiles() {
            return Collections.unmodifiableMap(fluidAmountProfiles);
        }
        public Map<String, Integer> getBlockAmountProfiles() {
            return Collections.unmodifiableMap(blockAmountProfiles);
        }
        public Map<String, Integer> getBlockMetaProfiles() {
            return Collections.unmodifiableMap(blockMetaProfiles);
        }
        private static boolean isBehaviorFlag(String key) {
            return "consume".equals(key) || "autoConsume".equals(key) || "groupDetect".equals(key)
                || "ignoreNBT".equals(key) || "partialMatch".equals(key)
                || "allowCraftedFromStatistics".equals(key) || "allowCraft".equals(key)
                || "allowSmelt".equals(key) || "allowAnvil".equals(key) || "ignoreDisabled".equals(key)
                || "isLevels".equals(key) || "subtypes".equals(key) || "onHit".equals(key)
                || "onInteract".equals(key) || "targetSubtypes".equals(key) || "ignoreTargetNBT".equals(key)
                || "ignoreItemNBT".equals(key) || "ignoreNbt".equals(key) || "partialItemMatch".equals(key)
                || "invert".equals(key) || "visible".equals(key) || "taxiCabDist".equals(key)
                || "viaPlayer".equals(key) || "relative".equals(key);
        }

        private static String normalizeFlag(String value) {
            if ("1".equals(value) || "true".equalsIgnoreCase(value)) return "true";
            if ("0".equals(value) || "false".equalsIgnoreCase(value)) return "false";
            return "invalid";
        }
    }
}

package com.jsirgalaxybase.quest.bqimport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Machine-readable publication gate for the behavior types used by the pinned GTNH quest book.
 *
 * <p>Codec support only proves that content can be decoded. Entries in this registry additionally name the
 * runtime chain that has executable regression evidence. A newly encountered, merely decodable type therefore
 * remains ADAPTED and blocks publication until its runtime behavior is proven.</p>
 */
public final class BqRuntimeCapabilityRegistry {
    private static final Map<String, String> TASK_EVIDENCE = evidence(
        "bq_standard:block_break", "BlockVectorTaskEvaluator + authenticated block-break fact tests",
        "bq_standard:checkbox", "AuthenticatedCheckboxConfirmationService tests",
        "bq_standard:crafting", "ItemVectorTaskEvaluator + crafting statistics/event tests",
        "bq_standard:fluid", "FluidSnapshotTaskEvaluator + ConsumptionAppliedTaskEvaluator tests",
        "bq_standard:hunt", "SubjectCountTaskEvaluator + authenticated entity-kill fact tests",
        "bq_standard:location", "LocationTaskEvaluator + observation/repeat tests",
        "bq_standard:meeting", "MeetingTaskEvaluator + nearby-entity observation tests",
        "bq_standard:optional_retrieval", "InventorySnapshotTaskEvaluator optional-task tests",
        "bq_standard:retrieval", "InventorySnapshotTaskEvaluator + ConsumptionAppliedTaskEvaluator tests");

    private static final Map<String, String> REWARD_EVIDENCE = evidence(
        "bq_standard:choice", "MinecraftItemRewardDeliveryHandler + exactly-once selection tests",
        "bq_standard:item", "MinecraftItemRewardDeliveryHandler + delivery/retry tests",
        "bq_standard:questcompletion", "QuestCompletionRewardDeliveryHandler + dependency tests",
        "bq_standard:xp", "MinecraftXpRewardDeliveryHandler + exactly-once delivery tests");

    private static final Map<String,String> TASK_FACTS=evidence(
        "bq_standard:block_break","galaxy:block_broken",
        "bq_standard:checkbox","galaxy:task_confirmed",
        "bq_standard:crafting","galaxy:item_produced",
        "bq_standard:fluid","galaxy:fluid_inventory_observed,galaxy:consumption_applied",
        "bq_standard:hunt","galaxy:entity_killed",
        "bq_standard:location","galaxy:location_observed",
        "bq_standard:meeting","galaxy:nearby_entities_observed",
        "bq_standard:optional_retrieval","galaxy:inventory_observed,galaxy:consumption_applied",
        "bq_standard:retrieval","galaxy:inventory_observed,galaxy:consumption_applied");

    private static final Map<String,String> TASK_EVALUATORS=evidence(
        "bq_standard:block_break","BlockVectorTaskEvaluator",
        "bq_standard:checkbox","ConfirmationTaskEvaluator",
        "bq_standard:crafting","ItemVectorTaskEvaluator",
        "bq_standard:fluid","CompositeTaskEvaluator",
        "bq_standard:hunt","SubjectCountTaskEvaluator",
        "bq_standard:location","LocationTaskEvaluator",
        "bq_standard:meeting","MeetingTaskEvaluator",
        "bq_standard:optional_retrieval","CompositeTaskEvaluator",
        "bq_standard:retrieval","CompositeTaskEvaluator");

    private static final Map<String, Set<String>> TASK_OPTION_KEYS = keySets(
        "bq_standard:block_break", "blocks.raw",
        "bq_standard:checkbox", "",
        "bq_standard:crafting", "allowAnvil,allowCraft,allowCraftedFromStatistics,allowSmelt,ignoreNBT,partialMatch,requiredItems.raw",
        "bq_standard:fluid", "autoConsume,consume,groupDetect,ignoreNBT,requiredFluids.raw",
        "bq_standard:hunt", "damageType,ignoreNBT,required,subtypes,target,targetNBT.raw",
        "bq_standard:location", "biome,dimension,hideInfo,invert,name,posX,posY,posZ,range,structure,taxiCabDist,visible",
        "bq_standard:meeting", "amount,ignoreNBT,range,subtypes,target,targetNBT.raw",
        "bq_standard:optional_retrieval", "autoConsume,consume,groupDetect,ignoreNBT,partialMatch,requiredItems.raw",
        "bq_standard:retrieval", "autoConsume,consume,groupDetect,ignoreNBT,partialMatch,requiredItems.raw");

    private static final Map<String, Set<String>> REWARD_OPTION_KEYS = keySets(
        "bq_standard:choice", "choices.raw,ignoreDisabled",
        "bq_standard:item", "ignoreDisabled,rewards.raw",
        "bq_standard:questcompletion", "ignoreDisabled,questID,questUuid",
        "bq_standard:xp", "amount,ignoreDisabled,isLevels");

    private static final Map<String, Set<String>> TASK_INTEGER_OPTION_KEYS = keySets(
        "bq_standard:block_break", "",
        "bq_standard:checkbox", "",
        "bq_standard:crafting", "",
        "bq_standard:fluid", "",
        "bq_standard:hunt", "required",
        "bq_standard:location", "biome,dimension,posX,posY,posZ,range",
        "bq_standard:meeting", "amount,range",
        "bq_standard:optional_retrieval", "",
        "bq_standard:retrieval", "");

    private static final Map<String, Set<String>> REWARD_INTEGER_OPTION_KEYS = keySets(
        "bq_standard:choice", "",
        "bq_standard:item", "",
        "bq_standard:questcompletion", "",
        "bq_standard:xp", "amount");

    private BqRuntimeCapabilityRegistry() { }

    public static BqCompatibilityStatus taskStatus(String typeId) {
        if (TASK_EVIDENCE.containsKey(typeId)) return BqCompatibilityStatus.SUPPORTED;
        return BqStandardCodec.supportedTaskTypes().contains(typeId)
            ? BqCompatibilityStatus.ADAPTED : BqCompatibilityStatus.UNKNOWN;
    }

    public static BqCompatibilityStatus rewardStatus(String typeId) {
        if (REWARD_EVIDENCE.containsKey(typeId)) return BqCompatibilityStatus.SUPPORTED;
        return BqStandardCodec.supportedRewardTypes().contains(typeId)
            ? BqCompatibilityStatus.ADAPTED : BqCompatibilityStatus.UNKNOWN;
    }

    public static Map<String, String> taskEvidence() { return TASK_EVIDENCE; }
    public static Map<String, String> rewardEvidence() { return REWARD_EVIDENCE; }
    public static Map<String,String> taskFactTypes() { return TASK_FACTS; }
    public static Map<String,String> taskEvaluators() { return TASK_EVALUATORS; }
    public static Map<String, Set<String>> taskOptionKeys() { return TASK_OPTION_KEYS; }
    public static Map<String, Set<String>> rewardOptionKeys() { return REWARD_OPTION_KEYS; }
    public static Map<String, Set<String>> taskIntegerOptionKeys() { return TASK_INTEGER_OPTION_KEYS; }
    public static Map<String, Set<String>> rewardIntegerOptionKeys() { return REWARD_INTEGER_OPTION_KEYS; }

    private static Map<String, String> evidence(String... entries) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int index = 0; index < entries.length; index += 2) result.put(entries[index], entries[index + 1]);
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Set<String>> keySets(String... entries) {
        Map<String, Set<String>> result = new LinkedHashMap<String, Set<String>>();
        for (int index = 0; index < entries.length; index += 2) {
            Set<String> keys = new LinkedHashSet<String>();
            String value = entries[index + 1];
            if (!value.trim().isEmpty()) {
                for (String key : value.split(",")) keys.add(key.trim());
            }
            result.put(entries[index], Collections.unmodifiableSet(keys));
        }
        return Collections.unmodifiableMap(result);
    }
}

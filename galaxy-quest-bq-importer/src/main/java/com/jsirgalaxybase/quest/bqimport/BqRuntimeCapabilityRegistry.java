package com.jsirgalaxybase.quest.bqimport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private static Map<String, String> evidence(String... entries) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int index = 0; index < entries.length; index += 2) result.put(entries[index], entries[index + 1]);
        return Collections.unmodifiableMap(result);
    }
}

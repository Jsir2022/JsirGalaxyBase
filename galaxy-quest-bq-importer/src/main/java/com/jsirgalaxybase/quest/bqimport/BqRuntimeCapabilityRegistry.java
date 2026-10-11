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
        "bq_standard:fluid", "FluidVectorTaskEvaluator + inventory observation/consumption tests",
        "bq_standard:hunt", "ScalarTaskEvaluator + authenticated entity-kill fact tests",
        "bq_standard:location", "LocationTaskEvaluator + observation/repeat tests",
        "bq_standard:meeting", "MeetingTaskEvaluator + nearby-entity observation tests",
        "bq_standard:optional_retrieval", "ItemVectorTaskEvaluator optional-task tests",
        "bq_standard:retrieval", "ItemVectorTaskEvaluator + inventory observation/consumption tests");

    private static final Map<String, String> REWARD_EVIDENCE = evidence(
        "bq_standard:choice", "ChoiceRewardHandler + exactly-once selection tests",
        "bq_standard:item", "ItemRewardHandler + delivery/retry tests",
        "bq_standard:questcompletion", "QuestCompletionRewardHandler + dependency tests",
        "bq_standard:xp", "XpRewardHandler + exactly-once delivery tests");

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

    private static Map<String, String> evidence(String... entries) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int index = 0; index < entries.length; index += 2) result.put(entries[index], entries[index + 1]);
        return Collections.unmodifiableMap(result);
    }
}

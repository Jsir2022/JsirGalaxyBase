package com.jsirgalaxybase.quest.core;

import java.util.Arrays;

/** Evaluators for imported BetterQuesting types whose fact semantics are implemented by Base. */
public final class BqCompatibleTaskEvaluatorRegistry {
    private BqCompatibleTaskEvaluatorRegistry() {}

    public static TaskEvaluatorRegistry firstEventDrivenBatch() {
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new SubjectCountTaskEvaluator("bq_standard:hunt", "galaxy:entity_killed"));
        registry.register(new ItemVectorTaskEvaluator("bq_standard:crafting", "galaxy:item_produced"));
        registry.register(new ConfirmationTaskEvaluator("bq_standard:checkbox", "galaxy:task_confirmed"));
        registry.register(new BlockVectorTaskEvaluator("bq_standard:block_break", "galaxy:block_broken"));
        registry.register(new CompositeTaskEvaluator("bq_standard:retrieval", Arrays.<TaskEvaluator>asList(
            new InventorySnapshotTaskEvaluator("bq_standard:retrieval", "galaxy:inventory_observed"),
            new ConsumptionAppliedTaskEvaluator("bq_standard:retrieval", "item"))));
        registry.register(new CompositeTaskEvaluator("bq_standard:optional_retrieval", Arrays.<TaskEvaluator>asList(
            new InventorySnapshotTaskEvaluator("bq_standard:optional_retrieval", "galaxy:inventory_observed"),
            new ConsumptionAppliedTaskEvaluator("bq_standard:optional_retrieval", "item"))));
        registry.register(new CompositeTaskEvaluator("bq_standard:fluid", Arrays.<TaskEvaluator>asList(
            new FluidSnapshotTaskEvaluator("bq_standard:fluid", "galaxy:fluid_inventory_observed"),
            new ConsumptionAppliedTaskEvaluator("bq_standard:fluid", "fluid"))));
        registry.register(new LocationTaskEvaluator("bq_standard:location", "galaxy:location_observed"));
        registry.register(new MeetingTaskEvaluator("bq_standard:meeting", "galaxy:nearby_entities_observed"));
        registry.register(new ScoreboardTaskEvaluator("bq_standard:scoreboard", "galaxy:score_observed"));
        registry.register(new CompositeTaskEvaluator("bq_standard:xp", Arrays.<TaskEvaluator>asList(
            new XpSnapshotTaskEvaluator("bq_standard:xp", "galaxy:xp_observed"),
            new ConsumptionAppliedTaskEvaluator("bq_standard:xp", "xp"))));
        registry.register(new InteractionTaskEvaluator("bq_standard:interact_entity",
            "galaxy:entity_interacted", true));
        registry.register(new InteractionTaskEvaluator("bq_standard:interact_item",
            "galaxy:item_interacted", false));
        return registry;
    }
}

package com.jsirgalaxybase.quest.core;

import java.util.Collection;

/**
 * Explicit extension point for Base-owned quest types.
 *
 * <p>This is deliberately a plain Java contract.  Implementations are supplied by
 * the Base composition root; no mod discovery, reflection or BetterQuesting
 * runtime is involved.</p>
 */
public interface QuestRuntimeTypeExtension {
    /** Stable diagnostic name of this extension. */
    String getId();

    /** Evaluators contributed by this extension. */
    Collection<TaskEvaluator> taskEvaluators();

    /** Task and reward editor descriptors contributed by this extension. */
    Collection<QuestElementTypeDescriptor> editorTypes();
}

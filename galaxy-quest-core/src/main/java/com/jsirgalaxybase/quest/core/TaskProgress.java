package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TaskProgress {
    private final String taskKey;
    private final long value;
    private final long target;
    private final boolean complete;
    private final List<Long> values;
    private final List<Long> targets;

    public TaskProgress(String taskKey, long value, long target, boolean complete) {
        this(taskKey, Collections.singletonList(value), Collections.singletonList(target), complete);
    }

    public TaskProgress(String taskKey, List<Long> values, List<Long> targets, boolean complete) {
        this.taskKey = Objects.requireNonNull(taskKey, "taskKey");
        if (values == null || targets == null || values.isEmpty() || values.size() != targets.size()) {
            throw new IllegalArgumentException("progress values and targets must have the same non-zero size");
        }
        List<Long> valueCopy = new ArrayList<Long>(values.size());
        List<Long> targetCopy = new ArrayList<Long>(targets.size());
        boolean allComplete = true;
        for (int i = 0; i < values.size(); i++) {
            long componentValue = Objects.requireNonNull(values.get(i), "value");
            long componentTarget = Objects.requireNonNull(targets.get(i), "target");
            if (componentValue < 0 || componentTarget < 0) {
                throw new IllegalArgumentException("progress values must not be negative");
            }
            valueCopy.add(componentValue);
            targetCopy.add(componentTarget);
            allComplete &= componentValue >= componentTarget;
        }
        this.values = Collections.unmodifiableList(valueCopy);
        this.targets = Collections.unmodifiableList(targetCopy);
        this.value = saturatingSum(valueCopy);
        this.target = saturatingSum(targetCopy);
        this.complete = complete || allComplete;
    }

    public static TaskProgress empty(TaskDefinition definition) {
        String mode = definition.getParameters().get("progressMode");
        String prefix = null;
        if ("item-vector".equals(mode)) prefix = "item";
        else if ("fluid-vector".equals(mode)) prefix = "fluid";
        else if ("block-vector".equals(mode)) prefix = "block";
        if (prefix == null) return new TaskProgress(definition.getKey(), 0L, scalarTarget(definition), false);

        int count = parseCount(definition.getParameters().get(prefix + ".count"));
        if (count <= 0) return new TaskProgress(definition.getKey(), 0L, scalarTarget(definition), false);
        List<Long> values = new ArrayList<Long>(count);
        List<Long> targets = new ArrayList<Long>(count);
        for (int i = 0; i < count; i++) {
            values.add(0L);
            targets.add(parseTarget(definition.getParameters().get(prefix + "." + i + ".amount")));
        }
        return new TaskProgress(definition.getKey(), values, targets, false);
    }

    public String getTaskKey() { return taskKey; }
    public long getValue() { return value; }
    public long getTarget() { return target; }
    public boolean isComplete() { return complete; }
    public List<Long> getValues() { return values; }
    public List<Long> getTargets() { return targets; }

    public TaskProgress merge(long observedValue, boolean observedComplete) {
        if (observedValue < 0) throw new IllegalArgumentException("observedValue must not be negative");
        if (values.size() != 1) throw new IllegalStateException("scalar merge cannot update vector progress");
        return new TaskProgress(taskKey, Math.max(value, observedValue), target, complete || observedComplete);
    }

    public TaskProgress merge(List<Long> observedValues, boolean observedComplete) {
        if (observedValues == null || observedValues.size() != values.size()) {
            throw new IllegalArgumentException("observed progress shape does not match task progress");
        }
        List<Long> merged = new ArrayList<Long>(values.size());
        for (int i = 0; i < values.size(); i++) {
            long observed = Objects.requireNonNull(observedValues.get(i), "observedValue");
            if (observed < 0) throw new IllegalArgumentException("observed progress must not be negative");
            merged.add(Math.max(values.get(i), observed));
        }
        return new TaskProgress(taskKey, merged, targets, complete || observedComplete);
    }

    public TaskProgress merge(TaskProgress observed) {
        if (!taskKey.equals(observed.taskKey)) throw new IllegalArgumentException("task keys do not match");
        return merge(observed.values, observed.complete);
    }

    /** Adds a committed delta, saturating each component at its target. */
    public TaskProgress advance(List<Long> deltas) {
        if (deltas == null || deltas.size() != values.size()) {
            throw new IllegalArgumentException("progress delta shape does not match task progress");
        }
        List<Long> advanced = new ArrayList<Long>(values.size());
        for (int i = 0; i < values.size(); i++) {
            long delta = Objects.requireNonNull(deltas.get(i), "delta");
            if (delta < 0) throw new IllegalArgumentException("progress delta must not be negative");
            long value = values.get(i);
            long target = targets.get(i);
            advanced.add(delta >= target - Math.min(value, target) ? target : value + delta);
        }
        return new TaskProgress(taskKey, advanced, targets, complete);
    }

    /** Absolute detector observation. Completed tasks remain latched; incomplete values may legitimately decrease. */
    public TaskProgress replaceIncomplete(List<Long> observedValues) {
        if (complete) return this;
        if (observedValues == null || observedValues.size() != values.size()) {
            throw new IllegalArgumentException("observed progress shape does not match task progress");
        }
        return new TaskProgress(taskKey, observedValues, targets, false);
    }

    private static long saturatingSum(List<Long> values) {
        long result = 0L;
        for (Long value : values) {
            if (Long.MAX_VALUE - result < value) return Long.MAX_VALUE;
            result += value;
        }
        return result;
    }

    private static long scalarTarget(TaskDefinition definition) {
        return parseTarget(definition.getParameters().get("target"));
    }

    private static int parseCount(String raw) {
        if (raw == null) return 0;
        int value = Integer.parseInt(raw);
        if (value < 0) throw new IllegalArgumentException("component count must not be negative");
        return value;
    }

    private static long parseTarget(String raw) {
        if (raw == null) return 1L;
        long value = Long.parseLong(raw);
        if (value < 0) throw new IllegalArgumentException("target must not be negative");
        return value;
    }
}

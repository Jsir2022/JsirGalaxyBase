package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TaskProgress {
    private final String taskKey;
    private final long value;
    private final long target;
    private final boolean complete;
    private final List<Long> values;
    private final List<Long> targets;
    private final boolean sourceScoped;
    private final List<Long> sourceBaselineValues;
    private final Map<String, SourceContribution> sourceContributions;

    public TaskProgress(String taskKey, long value, long target, boolean complete) {
        this(taskKey, Collections.singletonList(value), Collections.singletonList(target), complete);
    }

    public TaskProgress(String taskKey, List<Long> values, List<Long> targets, boolean complete) {
        this(taskKey, values, targets, complete, false, Collections.<Long>emptyList(),
            Collections.<String, SourceContribution>emptyMap());
    }

    private TaskProgress(String taskKey, List<Long> values, List<Long> targets, boolean complete,
        boolean sourceScoped, List<Long> sourceBaselineValues, Map<String, SourceContribution> sourceContributions) {
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
        this.sourceScoped = sourceScoped;
        if (sourceScoped) {
            this.sourceBaselineValues = immutableValues(sourceBaselineValues, targetCopy.size(), "source baseline");
            Map<String, SourceContribution> contributions = new LinkedHashMap<String, SourceContribution>();
            if (sourceContributions != null) {
                for (Map.Entry<String, SourceContribution> entry : sourceContributions.entrySet()) {
                    String source = requireSource(entry.getKey());
                    SourceContribution contribution = Objects.requireNonNull(entry.getValue(), "source contribution");
                    if (contribution.getEventValues().size() != targetCopy.size()) {
                        throw new IllegalArgumentException("source progress shape does not match task progress");
                    }
                    contributions.put(source, contribution);
                }
            }
            this.sourceContributions = Collections.unmodifiableMap(contributions);
        } else {
            this.sourceBaselineValues = Collections.emptyList();
            this.sourceContributions = Collections.emptyMap();
        }
    }

    /** Restores the source-aware counters stored alongside the visible aggregate values. */
    public static TaskProgress restoreSourceScoped(String taskKey, List<Long> targets, boolean complete,
        List<Long> legacyBaselineValues, Map<String, SourceContribution> sourceContributions) {
        List<Long> normalizedTargets = immutableValues(targets, targets == null ? 0 : targets.size(), "targets");
        if (normalizedTargets.isEmpty()) throw new IllegalArgumentException("targets must not be empty");
        List<Long> baseline = immutableValues(legacyBaselineValues, normalizedTargets.size(), "source baseline");
        Map<String, SourceContribution> contributions = sourceContributions == null
            ? Collections.<String, SourceContribution>emptyMap() : sourceContributions;
        List<Long> aggregate = aggregate(normalizedTargets, baseline, contributions);
        return new TaskProgress(taskKey, aggregate, normalizedTargets, complete, true, baseline, contributions);
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
    public boolean isSourceScoped() { return sourceScoped; }
    public List<Long> getSourceBaselineValues() { return sourceBaselineValues; }
    public Map<String, SourceContribution> getSourceContributions() { return sourceContributions; }

    public TaskProgress merge(long observedValue, boolean observedComplete) {
        if (observedValue < 0) throw new IllegalArgumentException("observedValue must not be negative");
        if (values.size() != 1) throw new IllegalStateException("scalar merge cannot update vector progress");
        List<Long> observed = Collections.singletonList(Long.valueOf(Math.max(value, observedValue)));
        if (sourceScoped) return mergeLegacyBaseline(observed, observedComplete);
        return new TaskProgress(taskKey, observed.get(0).longValue(), target, complete || observedComplete);
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
        if (sourceScoped) return mergeLegacyBaseline(merged, observedComplete);
        return new TaskProgress(taskKey, merged, targets, complete || observedComplete);
    }

    public TaskProgress merge(TaskProgress observed) {
        if (!taskKey.equals(observed.taskKey)) throw new IllegalArgumentException("task keys do not match");
        if (observed.sourceScoped) {
            List<Long> baseline = sourceScoped ? componentMax(sourceBaselineValues, observed.sourceBaselineValues)
                : componentMax(values, observed.sourceBaselineValues);
            Map<String, SourceContribution> merged = new LinkedHashMap<String, SourceContribution>(sourceContributions);
            for (Map.Entry<String, SourceContribution> entry : observed.sourceContributions.entrySet()) {
                SourceContribution current = merged.get(entry.getKey());
                merged.put(entry.getKey(), current == null ? entry.getValue() : current.merge(entry.getValue()));
            }
            return restoreSourceScoped(taskKey, targets, complete || observed.complete, baseline, merged);
        }
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
        if (sourceScoped) return mergeLegacyBaseline(advanced, false);
        return new TaskProgress(taskKey, advanced, targets, complete);
    }

    /** Adds measured fluid volume without capping at the target; BQ can drain an indivisible full container. */
    public TaskProgress advanceMeasuredAmounts(List<Long> amounts) {
        if (amounts == null || amounts.size() != values.size()) {
            throw new IllegalArgumentException("measured amount shape does not match task progress");
        }
        List<Long> advanced = new ArrayList<Long>(values.size());
        for (int i = 0; i < values.size(); i++) {
            long amount = Objects.requireNonNull(amounts.get(i), "measured amount");
            if (amount < 0) throw new IllegalArgumentException("measured amount must not be negative");
            advanced.add(Long.valueOf(saturatingAdd(values.get(i), amount)));
        }
        if (sourceScoped) throw new IllegalStateException("measured fluid amounts cannot update source-scoped progress");
        return new TaskProgress(taskKey, advanced, targets, complete);
    }

    /** Adds an idempotently-ledgered event delta to one source, capped like BQ's Crafting event progress. */
    public TaskProgress advanceFromSource(String sourceServer, List<Long> deltas) {
        String source = requireSource(sourceServer);
        requireShape(deltas, "progress delta");
        List<Long> baseline = sourceScoped ? sourceBaselineValues : values;
        Map<String, SourceContribution> contributions = new LinkedHashMap<String, SourceContribution>(sourceContributions);
        SourceContribution current = contributions.get(source);
        if (current == null) current = SourceContribution.zero(values.size());
        List<Long> events = new ArrayList<Long>(values.size());
        for (int i = 0; i < values.size(); i++) {
            long delta = nonNegative(deltas.get(i), "delta");
            long eventTotal = saturatingAdd(current.getEventValues().get(i).longValue(), delta);
            events.add(Long.valueOf(Math.min(targets.get(i).longValue(), eventTotal)));
        }
        contributions.put(source, new SourceContribution(events, current.getObservedValues()));
        return restoreSourceScoped(taskKey, targets, complete, baseline, contributions);
    }

    /** Merges an absolute crafting-statistics observation into one source without counting its events twice. */
    public TaskProgress observeFromSource(String sourceServer, List<Long> observedValues) {
        String source = requireSource(sourceServer);
        requireShape(observedValues, "observed progress");
        List<Long> baseline = sourceScoped ? sourceBaselineValues : values;
        Map<String, SourceContribution> contributions = new LinkedHashMap<String, SourceContribution>(sourceContributions);
        SourceContribution current = contributions.get(source);
        if (current == null) current = SourceContribution.zero(values.size());
        List<Long> observations = new ArrayList<Long>(values.size());
        for (int i = 0; i < values.size(); i++) {
            long observed = nonNegative(observedValues.get(i), "observedValue");
            observations.add(Long.valueOf(Math.max(current.getObservedValues().get(i).longValue(), observed)));
        }
        contributions.put(source, new SourceContribution(current.getEventValues(), observations));
        return restoreSourceScoped(taskKey, targets, complete, baseline, contributions);
    }

    /** Absolute detector observation. Completed tasks remain latched; incomplete values may legitimately decrease. */
    public TaskProgress replaceIncomplete(List<Long> observedValues) {
        if (complete) return this;
        if (observedValues == null || observedValues.size() != values.size()) {
            throw new IllegalArgumentException("observed progress shape does not match task progress");
        }
        if (sourceScoped) return restoreSourceScoped(taskKey, targets, false, observedValues, sourceContributions);
        return new TaskProgress(taskKey, observedValues, targets, false);
    }

    /** Replaces an incomplete BQ retrieval snapshot while retaining individually satisfied requirements. */
    public TaskProgress replaceIncompleteComponents(List<Long> observedValues) {
        if (complete) return this;
        if (observedValues == null || observedValues.size() != values.size()) {
            throw new IllegalArgumentException("observed progress shape does not match task progress");
        }
        List<Long> replaced = new ArrayList<Long>(values.size());
        for (int i = 0; i < values.size(); i++) {
            long observed = Objects.requireNonNull(observedValues.get(i), "observedValue");
            if (observed < 0) throw new IllegalArgumentException("observed progress must not be negative");
            replaced.add(values.get(i) >= targets.get(i) ? values.get(i) : observed);
        }
        if (sourceScoped) return restoreSourceScoped(taskKey, targets, false, replaced, sourceContributions);
        return new TaskProgress(taskKey, replaced, targets, false);
    }

    /** Latches the BQ Task completion marker without rewriting its per-component observation values. */
    public TaskProgress markComplete() {
        if (complete) return this;
        return sourceScoped ? restoreSourceScoped(taskKey, targets, true, sourceBaselineValues, sourceContributions)
            : new TaskProgress(taskKey, values, targets, true);
    }

    private TaskProgress mergeLegacyBaseline(List<Long> observedValues, boolean observedComplete) {
        List<Long> baseline = sourceScoped ? componentMax(sourceBaselineValues, observedValues)
            : componentMax(values, observedValues);
        return restoreSourceScoped(taskKey, targets, complete || observedComplete, baseline, sourceContributions);
    }

    private void requireShape(List<Long> values, String label) {
        if (values == null || values.size() != this.values.size()) {
            throw new IllegalArgumentException(label + " shape does not match task progress");
        }
    }

    private static List<Long> aggregate(List<Long> targets, List<Long> baseline,
        Map<String, SourceContribution> contributions) {
        List<Long> result = new ArrayList<Long>(targets.size());
        for (int i = 0; i < targets.size(); i++) {
            long sourceTotal = 0L;
            if (contributions != null) {
                for (SourceContribution contribution : contributions.values()) {
                    sourceTotal = saturatingAdd(sourceTotal, contribution.getValue(i, targets.get(i).longValue()));
                }
            }
            long value = Math.max(baseline.get(i).longValue(), sourceTotal);
            // BQ Crafting statistics persist the absolute count even when it exceeds this requirement.
            // Completion is threshold-based, but the displayed/stored observation remains uncapped.
            result.add(Long.valueOf(value));
        }
        return result;
    }

    private static List<Long> immutableValues(List<Long> values, int expectedSize, String label) {
        if (values == null || values.size() != expectedSize) {
            throw new IllegalArgumentException(label + " shape does not match task progress");
        }
        List<Long> copy = new ArrayList<Long>(expectedSize);
        for (Long value : values) copy.add(Long.valueOf(nonNegative(value, label)));
        return Collections.unmodifiableList(copy);
    }

    private static List<Long> componentMax(List<Long> left, List<Long> right) {
        if (left.size() != right.size()) throw new IllegalArgumentException("progress shapes do not match");
        List<Long> result = new ArrayList<Long>(left.size());
        for (int i = 0; i < left.size(); i++) {
            result.add(Long.valueOf(Math.max(left.get(i).longValue(), right.get(i).longValue())));
        }
        return result;
    }

    private static long nonNegative(Long value, String label) {
        long result = Objects.requireNonNull(value, label).longValue();
        if (result < 0L) throw new IllegalArgumentException(label + " must not be negative");
        return result;
    }

    private static long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static String requireSource(String sourceServer) {
        String source = Objects.requireNonNull(sourceServer, "sourceServer").trim();
        if (source.isEmpty() || source.length() > 64) {
            throw new IllegalArgumentException("source server must contain 1 to 64 characters");
        }
        return source;
    }

    public static final class SourceContribution {
        private final List<Long> eventValues;
        private final List<Long> observedValues;

        public SourceContribution(List<Long> eventValues, List<Long> observedValues) {
            if (eventValues == null || observedValues == null || eventValues.isEmpty()
                || eventValues.size() != observedValues.size()) {
                throw new IllegalArgumentException("event and observed vectors must have the same non-zero size");
            }
            this.eventValues = immutableValues(eventValues, eventValues.size(), "event progress");
            this.observedValues = immutableValues(observedValues, eventValues.size(), "observed progress");
        }

        private static SourceContribution zero(int size) {
            List<Long> zeros = new ArrayList<Long>(size);
            for (int i = 0; i < size; i++) zeros.add(Long.valueOf(0L));
            return new SourceContribution(zeros, zeros);
        }

        public List<Long> getEventValues() { return eventValues; }
        public List<Long> getObservedValues() { return observedValues; }
        public long getValue(int index) {
            return Math.max(eventValues.get(index).longValue(), observedValues.get(index).longValue());
        }
        public long getValue(int index, long eventTarget) {
            long cappedEvent = Math.min(eventValues.get(index).longValue(), eventTarget);
            return Math.max(cappedEvent, observedValues.get(index).longValue());
        }

        private SourceContribution merge(SourceContribution other) {
            return new SourceContribution(componentMax(eventValues, other.eventValues),
                componentMax(observedValues, other.observedValues));
        }
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

package com.jsirgalaxybase.quest.core;

/** Read-only catalog and recovery counters. No player identities or progress are exposed. */
public final class QuestRuntimeHealthSummary {
    private final long quests, tasks, rewards, chapters, catalogRevision, failedRewards, pendingConsumption;

    public QuestRuntimeHealthSummary(long quests, long tasks, long rewards, long chapters,
        long catalogRevision, long failedRewards, long pendingConsumption) {
        if (quests < 0 || tasks < 0 || rewards < 0 || chapters < 0 || catalogRevision < 0
            || failedRewards < 0 || pendingConsumption < 0) {
            throw new IllegalArgumentException("health counters must be nonnegative");
        }
        this.quests = quests;
        this.tasks = tasks;
        this.rewards = rewards;
        this.chapters = chapters;
        this.catalogRevision = catalogRevision;
        this.failedRewards = failedRewards;
        this.pendingConsumption = pendingConsumption;
    }

    public long getQuests() { return quests; }
    public long getTasks() { return tasks; }
    public long getRewards() { return rewards; }
    public long getChapters() { return chapters; }
    public long getCatalogRevision() { return catalogRevision; }
    public long getFailedRewards() { return failedRewards; }
    public long getPendingConsumption() { return pendingConsumption; }
}

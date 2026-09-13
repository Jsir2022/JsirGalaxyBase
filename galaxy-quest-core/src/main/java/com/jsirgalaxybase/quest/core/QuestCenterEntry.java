package com.jsirgalaxybase.quest.core;

/** Latest published definition joined with one participant's progress and reward state. */
public final class QuestCenterEntry {
    private final QuestDefinition definition;
    private final QuestProgressSnapshot progress;
    private final QuestRewardSummary rewards;
    private final QuestStatus status;
    private final boolean tracked;

    public QuestCenterEntry(QuestDefinition definition, QuestProgressSnapshot progress, QuestRewardSummary rewards) {
        this(definition,progress,rewards,progress==null?QuestStatus.AVAILABLE:progress.getStatus(),false);
    }

    public QuestCenterEntry(QuestDefinition definition, QuestProgressSnapshot progress, QuestRewardSummary rewards,
        QuestStatus status) {this(definition,progress,rewards,status,false);}
    public QuestCenterEntry(QuestDefinition definition, QuestProgressSnapshot progress, QuestRewardSummary rewards,
        QuestStatus status,boolean tracked) {
        if(definition==null)throw new IllegalArgumentException("definition is required");
        if(progress!=null&&(!progress.getQuestId().equals(definition.getId())
            ||progress.getDefinitionVersion()!=definition.getVersion()))throw new IllegalArgumentException("progress identity mismatch");
        this.definition=definition;this.progress=progress;this.rewards=rewards==null?QuestRewardSummary.EMPTY:rewards;
        this.status=status==null?(progress==null?QuestStatus.AVAILABLE:progress.getStatus()):status;
        this.tracked=tracked;
    }
    public QuestDefinition getDefinition(){return definition;} public QuestProgressSnapshot getProgress(){return progress;}
    public QuestRewardSummary getRewards(){return rewards;}
    public QuestStatus getStatus(){return status;}
    public boolean isTracked(){return tracked;}
}

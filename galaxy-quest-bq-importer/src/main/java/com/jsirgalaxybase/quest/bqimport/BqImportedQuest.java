package com.jsirgalaxybase.quest.bqimport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.quest.core.QuestDefinition;

public final class BqImportedQuest {
    private final QuestDefinition definition;
    private final Path source;
    private final String rawJson;
    private final List<BqStandardTaskSpec> taskSpecs;
    private final List<BqStandardRewardSpec> rewardSpecs;

    BqImportedQuest(QuestDefinition definition, Path source, String rawJson, List<BqStandardTaskSpec> taskSpecs,
        List<BqStandardRewardSpec> rewardSpecs) {
        this.definition = definition;
        this.source = source;
        this.rawJson = rawJson;
        this.taskSpecs = Collections.unmodifiableList(new ArrayList<BqStandardTaskSpec>(taskSpecs));
        this.rewardSpecs = Collections.unmodifiableList(new ArrayList<BqStandardRewardSpec>(rewardSpecs));
    }

    public QuestDefinition getDefinition() { return definition; }
    public Path getSource() { return source; }
    public String getRawJson() { return rawJson; }
    public List<BqStandardTaskSpec> getTaskSpecs() { return taskSpecs; }
    public List<BqStandardRewardSpec> getRewardSpecs() { return rewardSpecs; }
}

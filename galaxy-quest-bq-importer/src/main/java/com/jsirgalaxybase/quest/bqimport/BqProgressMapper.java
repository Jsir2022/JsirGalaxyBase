package com.jsirgalaxybase.quest.bqimport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;

public final class BqProgressMapper {
    public Map<String, TaskProgress> toCore(QuestDefinition definition, BqImportedQuestProgress imported) {
        if (!definition.getId().equals(imported.getQuestId())) {
            throw new BqProgressShapeException("quest progress ID does not match definition");
        }
        Map<String, BqImportedTaskProgress> source = new LinkedHashMap<String, BqImportedTaskProgress>();
        for (BqImportedTaskProgress task : imported.getTasks()) source.put(task.getKey(), task);

        Map<String, TaskProgress> result = new LinkedHashMap<String, TaskProgress>();
        for (TaskDefinition task : definition.getTasks()) {
            TaskProgress progress = TaskProgress.empty(task);
            BqImportedTaskProgress observed = source.remove(task.getKey());
            if (observed != null) {
                if (!task.getTypeId().equals(observed.getTypeId())) {
                    throw new BqProgressShapeException("task type changed for " + task.getKey());
                }
                List<Long> values = observed.getValues();
                if (!values.isEmpty()) {
                    if (values.size() != progress.getValues().size()) {
                        throw new BqProgressShapeException("task progress width changed for " + task.getKey()
                            + ": expected " + progress.getValues().size() + " but was " + values.size());
                    }
                    progress = progress.merge(values, observed.isComplete());
                } else if (observed.isComplete()) {
                    progress = progress.merge(progress.getValues(), true);
                }
            }
            result.put(task.getKey(), progress);
        }
        if (!source.isEmpty()) throw new BqProgressShapeException("progress contains unknown tasks: " + source.keySet());
        return result;
    }
}

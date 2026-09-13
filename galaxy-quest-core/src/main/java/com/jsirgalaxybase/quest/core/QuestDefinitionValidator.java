package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class QuestDefinitionValidator {
    public List<String> validate(QuestDefinition definition, Set<String> knownTaskTypes, Set<String> knownRewardTypes) {
        List<String> errors = new ArrayList<String>();
        Set<String> keys = new HashSet<String>();
        for (TaskDefinition task : definition.getTasks()) {
            if (!keys.add(task.getKey())) errors.add("duplicate task key: " + task.getKey());
            if (!knownTaskTypes.contains(task.getTypeId())) errors.add("unknown task type: " + task.getTypeId());
        }
        keys.clear();
        for (RewardDefinition reward : definition.getRewards()) {
            if (!keys.add(reward.getKey())) errors.add("duplicate reward key: " + reward.getKey());
            if (!knownRewardTypes.contains(reward.getTypeId())) errors.add("unknown reward type: " + reward.getTypeId());
        }
        if (definition.getPrerequisites().contains(definition.getId())) errors.add("quest cannot require itself");
        return errors;
    }

    public List<QuestEditorValidationIssue> validate(QuestDefinition definition, QuestEditorTypeRegistry registry) {
        if (definition == null) throw new IllegalArgumentException("definition must not be null");
        if (registry == null) throw new IllegalArgumentException("registry must not be null");
        List<QuestEditorValidationIssue> issues = new ArrayList<QuestEditorValidationIssue>();
        Set<String> taskKeys = new HashSet<String>();
        for (TaskDefinition task : definition.getTasks()) {
            String prefix = "tasks." + task.getKey();
            if (!taskKeys.add(task.getKey())) issue(issues, prefix + ".key", "duplicate", "Duplicate task key");
            try {
                for (QuestEditorValidationIssue issue : registry.validate(task)) issues.add(prefixed(prefix, issue));
            } catch (IllegalArgumentException unknown) {
                issue(issues, prefix + ".type", "unknown_type", unknown.getMessage());
            }
        }
        Set<String> rewardKeys = new HashSet<String>();
        for (RewardDefinition reward : definition.getRewards()) {
            String prefix = "rewards." + reward.getKey();
            if (!rewardKeys.add(reward.getKey())) issue(issues, prefix + ".key", "duplicate", "Duplicate reward key");
            try {
                for (QuestEditorValidationIssue issue : registry.validate(reward)) issues.add(prefixed(prefix, issue));
            } catch (IllegalArgumentException unknown) {
                issue(issues, prefix + ".type", "unknown_type", unknown.getMessage());
            }
        }
        if (definition.getPrerequisites().contains(definition.getId())) {
            issue(issues, "prerequisites", "self_reference", "Quest cannot require itself");
        }
        return issues;
    }

    private static QuestEditorValidationIssue prefixed(String prefix, QuestEditorValidationIssue issue) {
        String field = issue.getFieldKey();
        return new QuestEditorValidationIssue(field.isEmpty() ? prefix : prefix + "." + field,
            issue.getCode(), issue.getMessage());
    }

    private static void issue(List<QuestEditorValidationIssue> issues, String field, String code, String message) {
        issues.add(new QuestEditorValidationIssue(field, code, message));
    }
}

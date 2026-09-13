package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuestDraftManagementResult {
    public enum Status { SUCCESS, FORBIDDEN, INVALID, CONFLICT, NOT_FOUND }

    private final Status status;
    private final StoredQuestDefinition definition;
    private final List<QuestEditorValidationIssue> issues;

    private QuestDraftManagementResult(Status status, StoredQuestDefinition definition,
        List<QuestEditorValidationIssue> issues) {
        this.status = status;
        this.definition = definition;
        this.issues = Collections.unmodifiableList(new ArrayList<QuestEditorValidationIssue>(issues));
    }

    public static QuestDraftManagementResult success(StoredQuestDefinition definition) {
        return new QuestDraftManagementResult(Status.SUCCESS, definition,
            Collections.<QuestEditorValidationIssue>emptyList());
    }

    public static QuestDraftManagementResult status(Status status) {
        return new QuestDraftManagementResult(status, null, Collections.<QuestEditorValidationIssue>emptyList());
    }

    public static QuestDraftManagementResult invalid(List<QuestEditorValidationIssue> issues) {
        return new QuestDraftManagementResult(Status.INVALID, null, issues);
    }

    public Status getStatus() { return status; }
    public StoredQuestDefinition getDefinition() { return definition; }
    public List<QuestEditorValidationIssue> getIssues() { return issues; }
    public boolean isSuccess() { return status == Status.SUCCESS; }
}

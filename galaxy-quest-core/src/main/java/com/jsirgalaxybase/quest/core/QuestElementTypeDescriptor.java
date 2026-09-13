package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class QuestElementTypeDescriptor {
    private final String typeId;
    private final QuestElementKind kind;
    private final String displayName;
    private final List<QuestEditorFieldDescriptor> fields;
    private final QuestElementParameterValidator validator;

    public QuestElementTypeDescriptor(String typeId, QuestElementKind kind, String displayName,
        List<QuestEditorFieldDescriptor> fields, QuestElementParameterValidator validator) {
        if (typeId == null || typeId.trim().isEmpty()) throw new IllegalArgumentException("typeId must not be blank");
        if (kind == null) throw new IllegalArgumentException("kind must not be null");
        this.typeId = typeId;
        this.kind = kind;
        this.displayName = displayName == null || displayName.trim().isEmpty() ? typeId : displayName;
        this.fields = Collections.unmodifiableList(new ArrayList<QuestEditorFieldDescriptor>(
            fields == null ? Collections.<QuestEditorFieldDescriptor>emptyList() : fields));
        this.validator = validator;
    }

    public String getTypeId() { return typeId; }
    public QuestElementKind getKind() { return kind; }
    public String getDisplayName() { return displayName; }
    public List<QuestEditorFieldDescriptor> getFields() { return fields; }

    public List<QuestEditorValidationIssue> validate(Map<String, String> parameters) {
        List<QuestEditorValidationIssue> issues = QuestEditorParameterValidation.validate(fields, parameters);
        if (validator != null) issues.addAll(validator.validate(parameters));
        return Collections.unmodifiableList(issues);
    }
}

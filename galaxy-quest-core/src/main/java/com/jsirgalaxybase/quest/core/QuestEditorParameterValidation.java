package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class QuestEditorParameterValidation {
    private QuestEditorParameterValidation() {}

    static List<QuestEditorValidationIssue> validate(List<QuestEditorFieldDescriptor> fields,
        Map<String, String> input) {
        Map<String, String> values = input == null ? Collections.<String, String>emptyMap() : input;
        List<QuestEditorValidationIssue> result = new ArrayList<QuestEditorValidationIssue>();
        for (QuestEditorFieldDescriptor field : fields) validate(field, values, result);
        return result;
    }

    private static void validate(QuestEditorFieldDescriptor field, Map<String, String> values,
        List<QuestEditorValidationIssue> result) {
        String value = values.get(field.getKey());
        if (isList(field.getType())) {
            validateList(field, values, result);
            return;
        }
        if (empty(value)) {
            if (field.isRequired()) issue(result, field.getKey(), "required", field.getLabel() + " is required");
            return;
        }
        switch (field.getType()) {
            case BOOLEAN:
                if (!("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)
                    || "1".equals(value) || "0".equals(value))) {
                    issue(result, field.getKey(), "boolean", field.getLabel() + " must be true or false");
                }
                break;
            case INTEGER:
            case LONG:
                validateLong(field, value, result);
                break;
            case ENUM:
                if (!field.getOptions().contains(value)) {
                    issue(result, field.getKey(), "option", field.getLabel() + " has an unsupported value");
                }
                break;
            case QUEST_REFERENCE:
                try { UUID.fromString(value); } catch (RuntimeException invalid) {
                    issue(result, field.getKey(), "uuid", field.getLabel() + " must be a UUID");
                }
                break;
            default:
                break;
        }
    }

    private static void validateLong(QuestEditorFieldDescriptor field, String value,
        List<QuestEditorValidationIssue> result) {
        try {
            long parsed = Long.parseLong(value);
            if (field.getMinimum() != null && parsed < field.getMinimum().longValue()) {
                issue(result, field.getKey(), "minimum", field.getLabel() + " is below its minimum");
            }
            if (field.getMaximum() != null && parsed > field.getMaximum().longValue()) {
                issue(result, field.getKey(), "maximum", field.getLabel() + " exceeds its maximum");
            }
        } catch (NumberFormatException invalid) {
            issue(result, field.getKey(), "number", field.getLabel() + " must be an integer");
        }
    }

    private static void validateList(QuestEditorFieldDescriptor field, Map<String, String> values,
        List<QuestEditorValidationIssue> result) {
        String countKey = field.getKey() + ".count";
        int count;
        try { count = Integer.parseInt(values.get(countKey)); }
        catch (RuntimeException invalid) {
            if (field.isRequired()) issue(result, field.getKey(), "required", field.getLabel() + " requires entries");
            return;
        }
        if (count < 0) {
            issue(result, countKey, "minimum", field.getLabel() + " entry count cannot be negative");
            return;
        }
        if (field.isRequired() && count == 0) issue(result, field.getKey(), "required", field.getLabel() + " requires entries");
        for (int index = 0; index < count; index++) {
            String prefix = field.getKey() + "." + index + ".";
            String identity = field.getType() == QuestEditorFieldType.FLUID_LIST
                ? values.get(prefix + "name") : values.get(prefix + "registryName");
            if (empty(identity)) issue(result, prefix, "identity", field.getLabel() + " entry is missing its identity");
            String amount = values.get(prefix + "amount");
            if (empty(amount)) issue(result, prefix + "amount", "required", "Entry amount is required");
            else {
                try {
                    if (Long.parseLong(amount) <= 0L) issue(result, prefix + "amount", "minimum", "Entry amount must be positive");
                } catch (NumberFormatException invalid) {
                    issue(result, prefix + "amount", "number", "Entry amount must be an integer");
                }
            }
        }
    }

    private static boolean isList(QuestEditorFieldType type) {
        return type == QuestEditorFieldType.ITEM_LIST || type == QuestEditorFieldType.FLUID_LIST
            || type == QuestEditorFieldType.BLOCK_LIST;
    }

    private static boolean empty(String value) { return value == null || value.trim().isEmpty(); }
    private static void issue(List<QuestEditorValidationIssue> result, String field, String code, String message) {
        result.add(new QuestEditorValidationIssue(field, code, message));
    }
}

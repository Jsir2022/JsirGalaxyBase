package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.Map;

public interface QuestElementParameterValidator {
    List<QuestEditorValidationIssue> validate(Map<String, String> parameters);
}

package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class QuestBooleanParameterTest {
    @Test
    public void parsesBqNumericAndEditorBooleanValues() {
        assertTrue(QuestBooleanParameter.parse("1", false));
        assertFalse(QuestBooleanParameter.parse("0", true));
        assertTrue(QuestBooleanParameter.parse(" true ", false));
        assertFalse(QuestBooleanParameter.parse("FALSE", true));
        assertTrue(QuestBooleanParameter.parse(null, true));
        assertFalse(QuestBooleanParameter.parse("unexpected", false));
    }
}

package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QuestLogicTest {
    @Test public void mirrorsBetterQuestingLogicTruthTables() {
        boolean[][] expected = {
            {false, false, true},  // AND
            {true, true, false},   // NAND
            {false, true, true},   // OR
            {true, false, false},  // NOR
            {false, true, false},  // XOR
            {false, true, false}   // XNOR for two inputs
        };
        QuestLogic[] values = QuestLogic.values();
        assertEquals(expected.length, values.length);
        for (int logic = 0; logic < values.length; logic++) {
            for (int completed = 0; completed <= 2; completed++) {
                assertEquals(values[logic] + " at " + completed, expected[logic][completed],
                    values[logic].satisfied(completed, 2));
            }
            assertTrue(values[logic].satisfied(0, 0));
        }
    }
}

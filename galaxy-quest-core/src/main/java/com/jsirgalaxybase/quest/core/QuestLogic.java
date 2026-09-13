package com.jsirgalaxybase.quest.core;

public enum QuestLogic {
    AND,
    OR;

    public boolean satisfied(int completed, int total) {
        if (total <= 0) {
            return true;
        }
        return this == AND ? completed >= total : completed > 0;
    }
}

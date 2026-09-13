package com.jsirgalaxybase.quest.core;

/** BetterQuesting-compatible visibility policy, evaluated by the presentation/query layer. */
public enum QuestVisibility {
    HIDDEN,
    SECRET,
    UNLOCKED,
    NORMAL,
    COMPLETED,
    CHAIN,
    ALWAYS
}

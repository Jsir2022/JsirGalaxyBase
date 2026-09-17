package com.jsirgalaxybase.quest.postgres;

import com.jsirgalaxybase.quest.core.QuestChapterDefinition;
import com.jsirgalaxybase.quest.core.QuestDefinition;

public final class QuestContentHashes {
    private QuestContentHashes() {}
    public static String definition(QuestDefinition value){return new QuestDefinitionJsonCodec().encode(value).hash;}
    public static String chapter(QuestChapterDefinition value){return new QuestChapterJsonCodec().encode(value).hash;}
}

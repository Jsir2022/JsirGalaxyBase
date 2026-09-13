package com.jsirgalaxybase.quest.bqimport;

import java.nio.file.Path;

import com.jsirgalaxybase.quest.core.QuestChapterDefinition;

public final class BqImportedChapter {
    private final QuestChapterDefinition definition;
    private final Path sourceDirectory;
    private final int displayOrder;

    BqImportedChapter(QuestChapterDefinition definition, Path sourceDirectory, int displayOrder) {
        this.definition = definition;
        this.sourceDirectory = sourceDirectory;
        this.displayOrder = displayOrder;
    }

    public QuestChapterDefinition getDefinition() { return definition; }
    public Path getSourceDirectory() { return sourceDirectory; }
    public int getDisplayOrder() { return displayOrder; }
}

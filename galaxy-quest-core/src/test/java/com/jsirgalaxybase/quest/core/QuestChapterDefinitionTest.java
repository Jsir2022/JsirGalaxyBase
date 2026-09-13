package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

public class QuestChapterDefinitionTest {
    @Test
    public void placementHitTestUsesHalfOpenBoundsAndTopmostEntry() {
        QuestChapterEntry first = new QuestChapterEntry(UUID.randomUUID(), 0, 0, 24, 24);
        QuestChapterEntry top = new QuestChapterEntry(UUID.randomUUID(), 12, 12, 24, 24);
        QuestChapterDefinition chapter = new QuestChapterDefinition(UUID.randomUUID(), 1, "Chapter", "", "", "",
            Arrays.asList(first, top));

        assertEquals(first, chapter.entryAt(0, 0));
        assertEquals(top, chapter.entryAt(12, 12));
        assertEquals(top, chapter.entryAt(35, 35));
        assertNull(chapter.entryAt(36, 36));
    }

    @Test
    public void rejectsDuplicateQuestPlacementAndInvalidDimensions() {
        UUID quest = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new QuestChapterDefinition(UUID.randomUUID(), 1, "C", "",
            "", "", Arrays.asList(new QuestChapterEntry(quest, 0, 0, 24, 24),
                new QuestChapterEntry(quest, 30, 0, 24, 24))));
        assertThrows(IllegalArgumentException.class, () -> new QuestChapterEntry(quest, 0, 0, 0, 24));
    }

    @Test
    public void chapterEditorMovesAndResizesWithoutMutatingPublishedSource() {
        UUID quest = UUID.randomUUID();
        QuestChapterDefinition source = new QuestChapterDefinition(UUID.randomUUID(), 1, "Chapter", "", "", "",
            Arrays.asList(new QuestChapterEntry(quest, 0, 0, 24, 24)));
        QuestChapterDefinition edited = QuestChapterDraftEditor.nextVersion(source).name("Edited")
            .move(quest, 48, -12).resize(quest, 32, 20).build();

        assertEquals(0, source.getEntries().get(0).getX());
        assertEquals(2, edited.getVersion());
        assertEquals(48, edited.getEntries().get(0).getX());
        assertEquals(20, edited.getEntries().get(0).getHeight());
    }
}

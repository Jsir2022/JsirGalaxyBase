package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

/** Platform-neutral equivalent of BetterQuesting's quest-line placement entry. */
public final class QuestChapterEntry {
    private final UUID questId;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public QuestChapterEntry(UUID questId, int x, int y, int width, int height) {
        this.questId = Objects.requireNonNull(questId, "questId");
        if (width < 1 || height < 1) throw new IllegalArgumentException("entry dimensions must be positive");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public UUID getQuestId() { return questId; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean contains(int pointX, int pointY) {
        return pointX >= x && pointX < (long) x + width && pointY >= y && pointY < (long) y + height;
    }
}

package com.jsirgalaxybase.quest.bqimport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class BqImportedPlayerProgress {
    private final UUID playerId;
    private final Path source;
    private final List<BqImportedQuestProgress> quests;

    BqImportedPlayerProgress(UUID playerId, Path source, List<BqImportedQuestProgress> quests) {
        this.playerId = playerId;
        this.source = source;
        this.quests = Collections.unmodifiableList(new ArrayList<BqImportedQuestProgress>(quests));
    }

    public UUID getPlayerId() { return playerId; }
    public Path getSource() { return source; }
    public List<BqImportedQuestProgress> getQuests() { return quests; }
}

package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class QuestChapterEditRequest {
    private final UUID chapterId;private final int version;private final String expectedHash;private final List<QuestChapterEditOperation> operations;
    public QuestChapterEditRequest(UUID chapterId,int version,String expectedHash,List<QuestChapterEditOperation> operations){this.chapterId=Objects.requireNonNull(chapterId,"chapterId");if(version<1)throw new IllegalArgumentException("version must be positive");this.version=version;this.expectedHash=expectedHash==null?"":expectedHash.trim();if(this.expectedHash.isEmpty())throw new IllegalArgumentException("expected hash is required");if(operations==null||operations.isEmpty()||operations.size()>128)throw new IllegalArgumentException("chapter edit requires 1 to 128 operations");this.operations=Collections.unmodifiableList(new ArrayList<QuestChapterEditOperation>(operations));}
    public UUID getChapterId(){return chapterId;}public int getVersion(){return version;}public String getExpectedHash(){return expectedHash;}public List<QuestChapterEditOperation> getOperations(){return operations;}
}

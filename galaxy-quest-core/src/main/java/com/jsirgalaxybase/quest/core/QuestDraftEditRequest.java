package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Bounded edit batch committed under one expected content hash. */
public final class QuestDraftEditRequest {
    private final UUID questId;private final int version;private final String expectedContentHash;
    private final List<QuestDraftEditOperation> operations;
    public QuestDraftEditRequest(UUID questId,int version,String expectedContentHash,List<QuestDraftEditOperation> operations){
        this.questId=Objects.requireNonNull(questId,"questId");if(version<1)throw new IllegalArgumentException("version must be positive");this.version=version;
        String hash=expectedContentHash==null?"":expectedContentHash.trim();if(hash.isEmpty()||hash.length()>128)throw new IllegalArgumentException("expected content hash is required");this.expectedContentHash=hash;
        if(operations==null||operations.isEmpty()||operations.size()>128)throw new IllegalArgumentException("1..128 edit operations are required");
        ArrayList<QuestDraftEditOperation> copy=new ArrayList<QuestDraftEditOperation>(operations.size());for(QuestDraftEditOperation operation:operations)copy.add(Objects.requireNonNull(operation,"operation"));this.operations=Collections.unmodifiableList(copy);
    }
    public UUID getQuestId(){return questId;}public int getVersion(){return version;}public String getExpectedContentHash(){return expectedContentHash;}public List<QuestDraftEditOperation> getOperations(){return operations;}
}

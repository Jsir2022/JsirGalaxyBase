package com.jsirgalaxybase.quest.postgres;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuestContentMigrationBatch {
    public enum Status { PREVIEW, BLOCKED, APPLIED, ROLLED_BACK }
    private final String batchId, sourceHash, manifestText, previousChapterOrder;
    private final Status status; private final int errorCount,warningCount; private final List<QuestContentMigrationItem> items;
    public QuestContentMigrationBatch(String batchId,String sourceHash,Status status,String manifestText,String previousChapterOrder,int errorCount,int warningCount,List<QuestContentMigrationItem> items){
        this.batchId=batchId;this.sourceHash=sourceHash;this.status=status;this.manifestText=manifestText;this.previousChapterOrder=previousChapterOrder;
        this.errorCount=Math.max(0,errorCount);this.warningCount=Math.max(0,warningCount);
        this.items=Collections.unmodifiableList(new ArrayList<QuestContentMigrationItem>(items));
    }
    public String getBatchId(){return batchId;} public String getSourceHash(){return sourceHash;} public Status getStatus(){return status;}
    public String getManifestText(){return manifestText;} public String getPreviousChapterOrder(){return previousChapterOrder;}
    public int getErrorCount(){return errorCount;} public int getWarningCount(){return warningCount;}
    public List<QuestContentMigrationItem> getItems(){return items;}
}

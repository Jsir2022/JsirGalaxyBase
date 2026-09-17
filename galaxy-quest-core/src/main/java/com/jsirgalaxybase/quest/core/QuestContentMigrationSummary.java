package com.jsirgalaxybase.quest.core;

public final class QuestContentMigrationSummary {
    private final String batchId,sourceHash,status,diagnostic;private final long appliedAt,rolledBackAt;private final int created,versioned,unchanged,errorCount,warningCount;
    public QuestContentMigrationSummary(String batchId,String sourceHash,String status,long appliedAt,long rolledBackAt,int created,int versioned,int unchanged,int errorCount,int warningCount,String diagnostic){this.batchId=batchId;this.sourceHash=sourceHash;this.status=status;this.appliedAt=appliedAt;this.rolledBackAt=rolledBackAt;this.created=created;this.versioned=versioned;this.unchanged=unchanged;this.errorCount=errorCount;this.warningCount=warningCount;this.diagnostic=diagnostic==null?"":diagnostic;}
    public String getBatchId(){return batchId;}public String getSourceHash(){return sourceHash;}public String getStatus(){return status;}public long getAppliedAt(){return appliedAt;}public long getRolledBackAt(){return rolledBackAt;}public int getCreated(){return created;}public int getVersioned(){return versioned;}public int getUnchanged(){return unchanged;}public int getErrorCount(){return errorCount;}public int getWarningCount(){return warningCount;}public String getDiagnostic(){return diagnostic;}
}

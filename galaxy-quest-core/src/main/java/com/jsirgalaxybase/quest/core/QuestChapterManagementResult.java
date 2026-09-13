package com.jsirgalaxybase.quest.core;

public final class QuestChapterManagementResult {
    public enum Status { SUCCESS,FORBIDDEN,INVALID,CONFLICT,NOT_FOUND }
    private final Status status;private final StoredQuestChapter chapter;private final String message;
    private QuestChapterManagementResult(Status status,StoredQuestChapter chapter,String message){this.status=status;this.chapter=chapter;this.message=message==null?"":message;}
    public static QuestChapterManagementResult success(StoredQuestChapter value){return new QuestChapterManagementResult(Status.SUCCESS,value,"");}
    public static QuestChapterManagementResult status(Status value){return new QuestChapterManagementResult(value,null,"");}
    public static QuestChapterManagementResult invalid(String message){return new QuestChapterManagementResult(Status.INVALID,null,message);}
    public Status getStatus(){return status;}public StoredQuestChapter getChapter(){return chapter;}public String getMessage(){return message;}public boolean isSuccess(){return status==Status.SUCCESS;}
}

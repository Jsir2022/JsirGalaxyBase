package com.jsirgalaxybase.quest.core;

/** Compact chapter row; unlike a chapter definition it does not carry every placement. */
public final class QuestCenterChapterSummary {
    private final String id;
    private final String name;
    private final int completed;
    private final int total;

    public QuestCenterChapterSummary(String id,String name,int completed,int total){
        this.id=id==null?"":id;this.name=name==null?"":name;this.completed=Math.max(0,completed);
        this.total=Math.max(this.completed,total);
    }
    public String getId(){return id;} public String getName(){return name;}
    public int getCompleted(){return completed;} public int getTotal(){return total;}
}

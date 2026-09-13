package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

/** Allow-listed semantic mutation for one chapter draft. */
public final class QuestChapterEditOperation {
    public enum Kind { SET_NAME,SET_DESCRIPTION,SET_ICON,SET_BACKGROUND,SET_BACKGROUND_SIZE,SET_VISIBILITY,PLACE,MOVE,RESIZE,REMOVE }
    private final Kind kind;private final String text;private final UUID questId;private final int first,second,third,fourth;
    private QuestChapterEditOperation(Kind kind,String text,UUID questId,int first,int second,int third,int fourth){this.kind=Objects.requireNonNull(kind,"kind");this.text=bounded(text,4096);this.questId=questId;this.first=first;this.second=second;this.third=third;this.fourth=fourth;}
    public static QuestChapterEditOperation name(String value){return text(Kind.SET_NAME,value);}
    public static QuestChapterEditOperation description(String value){return text(Kind.SET_DESCRIPTION,value);}
    public static QuestChapterEditOperation icon(String value){return text(Kind.SET_ICON,value);}
    public static QuestChapterEditOperation background(String value){return text(Kind.SET_BACKGROUND,value);}
    public static QuestChapterEditOperation backgroundSize(int value){return numbers(Kind.SET_BACKGROUND_SIZE,null,value,0,0,0);}
    public static QuestChapterEditOperation visibility(QuestVisibility value){return text(Kind.SET_VISIBILITY,Objects.requireNonNull(value,"visibility").name());}
    public static QuestChapterEditOperation place(UUID id,int x,int y,int width,int height){return numbers(Kind.PLACE,id,x,y,width,height);}
    public static QuestChapterEditOperation move(UUID id,int x,int y){return numbers(Kind.MOVE,id,x,y,0,0);}
    public static QuestChapterEditOperation resize(UUID id,int width,int height){return numbers(Kind.RESIZE,id,width,height,0,0);}
    public static QuestChapterEditOperation remove(UUID id){return numbers(Kind.REMOVE,id,0,0,0,0);}
    private static QuestChapterEditOperation text(Kind kind,String value){return new QuestChapterEditOperation(kind,value,null,0,0,0,0);}
    private static QuestChapterEditOperation numbers(Kind kind,UUID id,int a,int b,int c,int d){return new QuestChapterEditOperation(kind,"",id==null?null:Objects.requireNonNull(id,"questId"),a,b,c,d);}
    public Kind getKind(){return kind;}public String getText(){return text;}public UUID getQuestId(){return questId;}public int getFirst(){return first;}public int getSecond(){return second;}public int getThird(){return third;}public int getFourth(){return fourth;}
    private static String bounded(String value,int max){String result=value==null?"":value.trim();if(result.length()>max)throw new IllegalArgumentException("chapter edit text exceeds "+max);return result;}
}

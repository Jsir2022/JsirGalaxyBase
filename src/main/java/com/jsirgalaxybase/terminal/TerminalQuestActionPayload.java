package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Client intent for task-center navigation. It cannot carry a player or participant identity. */
public final class TerminalQuestActionPayload {
    private final String chapterId,questId,filter,query,rewardKey,managementLifecycle,managementHash,managementTemplate;private final int chapterPage,questPage,choiceIndex,managementPage,managementVersion;
    public TerminalQuestActionPayload(String chapterId,String questId,String filter,String query,int chapterPage,int questPage){
        this(chapterId,questId,filter,query,chapterPage,questPage,"",-1,"",0,0,"");}
    public TerminalQuestActionPayload(String chapterId,String questId,String filter,String query,int chapterPage,int questPage,
        String rewardKey,int choiceIndex){this(chapterId,questId,filter,query,chapterPage,questPage,rewardKey,choiceIndex,"",0,0,"");}
    public TerminalQuestActionPayload(String chapterId,String questId,String filter,String query,int chapterPage,int questPage,
        String rewardKey,int choiceIndex,String managementLifecycle,int managementPage){this(chapterId,questId,filter,query,chapterPage,questPage,rewardKey,choiceIndex,managementLifecycle,managementPage,0,"");}
    public TerminalQuestActionPayload(String chapterId,String questId,String filter,String query,int chapterPage,int questPage,
        String rewardKey,int choiceIndex,String managementLifecycle,int managementPage,int managementVersion){this(chapterId,questId,filter,query,chapterPage,questPage,rewardKey,choiceIndex,managementLifecycle,managementPage,managementVersion,"");}
    public TerminalQuestActionPayload(String chapterId,String questId,String filter,String query,int chapterPage,int questPage,
        String rewardKey,int choiceIndex,String managementLifecycle,int managementPage,int managementVersion,String managementHash){
        this(chapterId,questId,filter,query,chapterPage,questPage,rewardKey,choiceIndex,managementLifecycle,managementPage,managementVersion,managementHash,"");}
    public TerminalQuestActionPayload(String chapterId,String questId,String filter,String query,int chapterPage,int questPage,
        String rewardKey,int choiceIndex,String managementLifecycle,int managementPage,int managementVersion,String managementHash,String managementTemplate){
        this.chapterId=limit(chapterId,64);this.questId=limit(questId,64);this.filter=limit(filter,16);this.query=limit(query,96);
        this.chapterPage=Math.max(0,Math.min(10000,chapterPage));this.questPage=Math.max(0,Math.min(100000,questPage));
        this.rewardKey=limit(rewardKey,64);this.choiceIndex=Math.max(-1,Math.min(31,choiceIndex));
        this.managementLifecycle=limit(managementLifecycle,16);this.managementPage=Math.max(0,Math.min(100000,managementPage));this.managementVersion=Math.max(0,managementVersion);this.managementHash=limit(managementHash,64);this.managementTemplate=limit(managementTemplate,32);}
    public static TerminalQuestActionPayload empty(){return new TerminalQuestActionPayload("","","all","",0,0);}
    public String encode(){return part(chapterId)+"|"+part(questId)+"|"+part(filter)+"|"+part(query)+"|"+chapterPage+"|"+questPage+"|"+part(rewardKey)+"|"+choiceIndex+"|"+part(managementLifecycle)+"|"+managementPage+"|"+managementVersion+"|"+part(managementHash)+"|"+part(managementTemplate);}
    public static TerminalQuestActionPayload decode(String value){if(value==null)return empty();String[] parts=value.split("\\|",-1);if(parts.length!=6&&parts.length!=8&&parts.length!=10&&parts.length!=11&&parts.length!=12&&parts.length!=13)return empty();try{return new TerminalQuestActionPayload(unpart(parts[0]),unpart(parts[1]),unpart(parts[2]),unpart(parts[3]),Integer.parseInt(parts[4]),Integer.parseInt(parts[5]),parts.length>=8?unpart(parts[6]):"",parts.length>=8?Integer.parseInt(parts[7]):-1,parts.length>=10?unpart(parts[8]):"",parts.length>=10?Integer.parseInt(parts[9]):0,parts.length>=11?Integer.parseInt(parts[10]):0,parts.length>=12?unpart(parts[11]):"",parts.length==13?unpart(parts[12]):"");}catch(RuntimeException invalid){return empty();}}
    public String getChapterId(){return chapterId;}public String getQuestId(){return questId;}public String getFilter(){return filter;}public String getQuery(){return query;}public int getChapterPage(){return chapterPage;}public int getQuestPage(){return questPage;}public String getRewardKey(){return rewardKey;}public int getChoiceIndex(){return choiceIndex;}public String getManagementLifecycle(){return managementLifecycle;}public int getManagementPage(){return managementPage;}public int getManagementVersion(){return managementVersion;}public String getManagementHash(){return managementHash;}public String getManagementTemplate(){return managementTemplate;}
    private static String part(String value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
    private static String unpart(String value){return limit(new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8),96);}
    private static String limit(String value,int max){String result=value==null?"":value.trim();return result.length()<=max?result:result.substring(0,max);}
}

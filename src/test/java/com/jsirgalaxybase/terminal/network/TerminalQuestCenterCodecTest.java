package com.jsirgalaxybase.terminal.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TerminalQuestCenterCodecTest {
    @Test
    public void roundTripsBoundedBrowseAndDetailPayload() {
        TerminalQuestCenterSectionSnapshot.Quest quest = new TerminalQuestCenterSectionSnapshot.Quest(
            "quest-a", "chapter-a", "高炉", "完成目标", "CLAIMABLE", 2, 2,
            "minecraft:iron_ingot", 0, "铁锭",true);
        TerminalQuestCenterSectionSnapshot.Detail detail = new TerminalQuestCenterSectionSnapshot.Detail(
            quest, "建造一座高炉", "前置任务 1 项", "不可重复",
            Arrays.asList(new TerminalQuestCenterSectionSnapshot.Task("task-a", "物品任务", "必需目标", 64, 64)),
            Arrays.asList(new TerminalQuestCenterSectionSnapshot.Reward("reward-a", "选择奖励", "bq_standard:choice",
                "minecraft:diamond", 0, "钻石", 4, true, false,Arrays.asList(
                    new TerminalQuestCenterSectionSnapshot.Choice("minecraft:diamond",0,"钻石",4),
                    new TerminalQuestCenterSectionSnapshot.Choice("minecraft:emerald",0,"绿宝石",8)),1)));
        TerminalQuestCenterSectionSnapshot original = new TerminalQuestCenterSectionSnapshot(
            "READY", "", TerminalQuestCenterSectionSnapshot.View.DETAIL, "chapter-a", "quest-a", "claimable",
            "高炉", Arrays.asList(new TerminalQuestCenterSectionSnapshot.Chapter("chapter-a", "钢铁时代", 3, 8)),
            1, 6, 9, Arrays.asList(quest), 2, 7, 17, detail);

        ByteBuf buffer = Unpooled.buffer();
        OpenTerminalApprovedMessage.writeQuestCenter(buffer, original);
        TerminalQuestCenterSectionSnapshot decoded = OpenTerminalApprovedMessage.readQuestCenter(buffer);

        assertEquals(TerminalQuestCenterSectionSnapshot.View.DETAIL, decoded.getView());
        assertEquals("高炉", decoded.getQuery());
        assertEquals(9, decoded.getChapterTotalEntries());
        assertEquals(17, decoded.getQuestTotalEntries());
        assertEquals("quest-a", decoded.getQuests().get(0).getId());
        assertTrue(decoded.getQuests().get(0).isTracked());
        assertNotNull(decoded.getDetail());
        assertEquals(64, decoded.getDetail().getTasks().get(0).getValue());
        assertEquals("minecraft:diamond", decoded.getDetail().getRewards().get(0).getItemRegistry());
        assertTrue(decoded.getDetail().getRewards().get(0).isClaimable());
        assertEquals(2,decoded.getDetail().getRewards().get(0).getChoices().size());
        assertEquals("minecraft:emerald",decoded.getDetail().getRewards().get(0).getChoices().get(1).getItemRegistry());
        assertEquals(1,decoded.getDetail().getRewards().get(0).getSelectedChoiceIndex());
    }

    @Test
    public void writerTruncatesUntrustedCollectionsToProtocolLimits() {
        List<TerminalQuestCenterSectionSnapshot.Chapter> chapters = new ArrayList<TerminalQuestCenterSectionSnapshot.Chapter>();
        List<TerminalQuestCenterSectionSnapshot.Quest> quests = new ArrayList<TerminalQuestCenterSectionSnapshot.Quest>();
        for (int index = 0; index < 80; index++) {
            chapters.add(new TerminalQuestCenterSectionSnapshot.Chapter("c" + index, "C", 0, 1));
            quests.add(new TerminalQuestCenterSectionSnapshot.Quest("q" + index, "c0", "Q", "", "AVAILABLE",
                0, 1, "", 0, ""));
        }
        TerminalQuestCenterSectionSnapshot original = new TerminalQuestCenterSectionSnapshot(
            "READY", "", TerminalQuestCenterSectionSnapshot.View.BROWSE, "", "", "all", "", chapters,
            0, 12, chapters.size(), quests, 0, 24, quests.size(), null);

        ByteBuf buffer = Unpooled.buffer();
        OpenTerminalApprovedMessage.writeQuestCenter(buffer, original);
        TerminalQuestCenterSectionSnapshot decoded = OpenTerminalApprovedMessage.readQuestCenter(buffer);

        assertEquals(12, decoded.getChapters().size());
        assertEquals(24, decoded.getQuests().size());
    }

    @Test
    public void roundTripsBoundedManagementProjection() {
        List<TerminalQuestCenterSectionSnapshot.Definition> rows=new ArrayList<TerminalQuestCenterSectionSnapshot.Definition>();
        for(int i=0;i<60;i++)rows.add(new TerminalQuestCenterSectionSnapshot.Definition("q-"+i,i+1,"任务 "+i,
            "DRAFT","hash-"+i,0L,i,2));
        TerminalQuestCenterSectionSnapshot.Management management=new TerminalQuestCenterSectionSnapshot.Management(
            "steel","DRAFT",2,20,80L,rows);
        TerminalQuestCenterSectionSnapshot original=new TerminalQuestCenterSectionSnapshot("READY","",
            TerminalQuestCenterSectionSnapshot.View.ADMIN,"","","all","steel",
            java.util.Collections.<TerminalQuestCenterSectionSnapshot.Chapter>emptyList(),0,6,0,
            java.util.Collections.<TerminalQuestCenterSectionSnapshot.Quest>emptyList(),0,7,0,null,management);
        ByteBuf buffer=Unpooled.buffer();OpenTerminalApprovedMessage.writeQuestCenter(buffer,original);
        TerminalQuestCenterSectionSnapshot decoded=OpenTerminalApprovedMessage.readQuestCenter(buffer);
        assertEquals(TerminalQuestCenterSectionSnapshot.View.ADMIN,decoded.getView());
        assertNotNull(decoded.getManagement());assertEquals(50,decoded.getManagement().getDefinitions().size());
        assertEquals("hash-0",decoded.getManagement().getDefinitions().get(0).getContentHash());
        assertEquals(80L,decoded.getManagement().getTotal());
    }

    @Test public void roundTripsManagedDefinitionDetailAndParameterMaps(){TerminalQuestCenterSectionSnapshot.Definition summary=new TerminalQuestCenterSectionSnapshot.Definition("550e8400-e29b-41d4-a716-446655440000",2,"钢材补给","DRAFT","hash",0,1,1);java.util.Map<String,String> parameters=new java.util.LinkedHashMap<String,String>();parameters.put("item.count","1");parameters.put("item.0.registryName","minecraft:iron_ingot");TerminalQuestCenterSectionSnapshot.DefinitionDetail detail=new TerminalQuestCenterSectionSnapshot.DefinitionDetail(summary,"说明","AND","OR",java.util.Collections.singletonList("550e8400-e29b-41d4-a716-446655440001"),java.util.Collections.singletonList(new TerminalQuestCenterSectionSnapshot.Element("steel","bq_standard:retrieval",false,parameters)),java.util.Collections.singletonList(new TerminalQuestCenterSectionSnapshot.Element("reward","bq_standard:item",false,parameters)),java.util.Collections.singletonMap("behavior.visibility","NORMAL"));TerminalQuestCenterSectionSnapshot.Field field=new TerminalQuestCenterSectionSnapshot.Field("item.count","数量","LONG",true,Long.valueOf(1),Long.valueOf(1000000),java.util.Collections.<String>emptyList());TerminalQuestCenterSectionSnapshot.ElementType taskType=new TerminalQuestCenterSectionSnapshot.ElementType("bq_standard:retrieval","物品提交","TASK",java.util.Collections.singletonList(field));TerminalQuestCenterSectionSnapshot.Management management=new TerminalQuestCenterSectionSnapshot.Management("","DRAFT",0,20,1,java.util.Collections.singletonList(summary),detail,java.util.Collections.singletonList(taskType),java.util.Collections.<TerminalQuestCenterSectionSnapshot.ElementType>emptyList());TerminalQuestCenterSectionSnapshot original=new TerminalQuestCenterSectionSnapshot("READY","",TerminalQuestCenterSectionSnapshot.View.ADMIN,"","","all","",java.util.Collections.<TerminalQuestCenterSectionSnapshot.Chapter>emptyList(),0,6,0,java.util.Collections.<TerminalQuestCenterSectionSnapshot.Quest>emptyList(),0,7,0,null,management);ByteBuf buffer=Unpooled.buffer();OpenTerminalApprovedMessage.writeQuestCenter(buffer,original);TerminalQuestCenterSectionSnapshot decoded=OpenTerminalApprovedMessage.readQuestCenter(buffer);assertNotNull(decoded.getManagement().getDetail());assertEquals("OR",decoded.getManagement().getDetail().getTaskLogic());assertEquals("minecraft:iron_ingot",decoded.getManagement().getDetail().getTasks().get(0).getParameters().get("item.0.registryName"));assertEquals("NORMAL",decoded.getManagement().getDetail().getOptions().get("behavior.visibility"));assertEquals("LONG",decoded.getManagement().getTaskTypes().get(0).getFields().get(0).getType());assertEquals(Long.valueOf(1000000),decoded.getManagement().getTaskTypes().get(0).getFields().get(0).getMaximum());}

    @Test
    public void roundTripsChapterManagementSpatialProjection() {
        TerminalQuestCenterSectionSnapshot.ManagedChapter summary = new TerminalQuestCenterSectionSnapshot.ManagedChapter(
            "chapter-1", 2, "工业时代", "DRAFT", "chapter-hash", 0L, 1);
        TerminalQuestCenterSectionSnapshot.ChapterDetail detail = new TerminalQuestCenterSectionSnapshot.ChapterDetail(
            summary, "章节说明", "minecraft:iron_ingot", "background", 512, "NORMAL",
            Arrays.asList(new TerminalQuestCenterSectionSnapshot.ChapterPlacement(
                "quest-1", "冶炼铁锭", -40, 25, 32, 24)),
            Arrays.asList(new TerminalQuestCenterSectionSnapshot.ChapterCandidate("quest-2", "建设焦炉")),
            Arrays.asList(new TerminalQuestCenterSectionSnapshot.ChapterDependency("quest-0", "quest-1", "INTERNAL"),
                new TerminalQuestCenterSectionSnapshot.ChapterDependency("quest-x", "quest-1", "EXTERNAL")));
        TerminalQuestCenterSectionSnapshot.ChapterManagement chapters =
            new TerminalQuestCenterSectionSnapshot.ChapterManagement("iron", "DRAFT", 1, 20, 24L,
                Arrays.asList(summary), detail);
        TerminalQuestCenterSectionSnapshot original = new TerminalQuestCenterSectionSnapshot("READY", "",
            TerminalQuestCenterSectionSnapshot.View.ADMIN, "", "", "all", "",
            java.util.Collections.<TerminalQuestCenterSectionSnapshot.Chapter>emptyList(), 0, 6, 0,
            java.util.Collections.<TerminalQuestCenterSectionSnapshot.Quest>emptyList(), 0, 7, 0, null, null, chapters);
        ByteBuf buffer = Unpooled.buffer();
        OpenTerminalApprovedMessage.writeQuestCenter(buffer, original);
        TerminalQuestCenterSectionSnapshot decoded = OpenTerminalApprovedMessage.readQuestCenter(buffer);
        assertNotNull(decoded.getChapterManagement());
        assertEquals(24L, decoded.getChapterManagement().getTotal());
        assertEquals("chapter-hash", decoded.getChapterManagement().getDetail().getSummary().getContentHash());
        assertEquals(-40, decoded.getChapterManagement().getDetail().getPlacements().get(0).getX());
        assertEquals("建设焦炉", decoded.getChapterManagement().getDetail().getCandidates().get(0).getName());
        assertEquals(2, decoded.getChapterManagement().getDetail().getDependencies().size());
        assertEquals("INTERNAL", decoded.getChapterManagement().getDetail().getDependencies().get(0).getKind());
    }
}

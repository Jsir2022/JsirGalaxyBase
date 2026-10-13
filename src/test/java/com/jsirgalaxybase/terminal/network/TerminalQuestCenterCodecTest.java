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
            "minecraft:iron_ingot", 0, "铁锭",true,"{\"display:10\":{\"Name:8\":\"炉渣\"}}");
        TerminalQuestCenterSectionSnapshot.Detail detail = new TerminalQuestCenterSectionSnapshot.Detail(
            quest, "建造一座高炉", "前置任务 1 项", "不可重复",
            Arrays.asList(new TerminalQuestCenterSectionSnapshot.Task("task-a", "物品任务", "必需目标", 32, 64,
                true, true, true, "bq_standard:retrieval", Arrays.asList(
                    new TerminalQuestCenterSectionSnapshot.Component(3, "item", "item.3", "gregtech:ingotIron",
                        "铁锭", 2, 64, 32, 64, "ingotIron", "{\"display:10\":{\"Name:8\":\"高级铁锭\"}}")))),
            Arrays.asList(new TerminalQuestCenterSectionSnapshot.Reward("reward-a", "选择奖励", "bq_standard:choice",
                "minecraft:diamond", 0, "钻石", 4, true, false,Arrays.asList(
                    new TerminalQuestCenterSectionSnapshot.Choice(2,"minecraft:diamond",0,"钻石",4,"{\"display:10\":{\"Name:8\":\"钻石\"}}"),
                    new TerminalQuestCenterSectionSnapshot.Choice(5,"",0,"未注册奖励",8,"")),5,"bq_standard:choice",
                    Arrays.asList(new TerminalQuestCenterSectionSnapshot.Component(0,"item","item.0","minecraft:diamond",
                        "钻石",0,4,0,4,"","{\"display:10\":{\"Name:8\":\"钻石\"}}")),
                    "{\"display:10\":{\"Name:8\":\"奖励钻石\"}}")),2,7,Arrays.asList(
                new TerminalQuestCenterSectionSnapshot.Prerequisite("quest-pre","焦炉","COMPLETED",true)));
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
        assertEquals("{\"display:10\":{\"Name:8\":\"炉渣\"}}", decoded.getQuests().get(0).getIconNbt());
        assertTrue(decoded.getQuests().get(0).isTracked());
        assertNotNull(decoded.getDetail());
        assertEquals(32, decoded.getDetail().getTasks().get(0).getValue());
        assertTrue(decoded.getDetail().getTasks().get(0).isConfirmable());
        assertTrue(decoded.getDetail().getTasks().get(0).isSubmittable());
        assertTrue(decoded.getDetail().getTasks().get(0).isDetectable());
        assertEquals("bq_standard:retrieval", decoded.getDetail().getTasks().get(0).getTypeId());
        TerminalQuestCenterSectionSnapshot.Component taskComponent=decoded.getDetail().getTasks().get(0).getComponents().get(0);
        assertEquals(3,taskComponent.getSourceIndex());
        assertEquals("gregtech:ingotIron",taskComponent.getIdentity());
        assertEquals("{\"display:10\":{\"Name:8\":\"高级铁锭\"}}",taskComponent.getNbt());
        assertEquals(2,decoded.getDetail().getDefinitionVersion());
        assertEquals(7,decoded.getDetail().getCycle());
        assertEquals("quest-pre",decoded.getDetail().getPrerequisites().get(0).getId());
        assertTrue(decoded.getDetail().getPrerequisites().get(0).isNavigable());
        assertEquals("minecraft:diamond", decoded.getDetail().getRewards().get(0).getItemRegistry());
        assertTrue(decoded.getDetail().getRewards().get(0).isClaimable());
        assertEquals(2,decoded.getDetail().getRewards().get(0).getChoices().size());
        assertEquals(5,decoded.getDetail().getRewards().get(0).getChoices().get(1).getSourceIndex());
        assertEquals("",decoded.getDetail().getRewards().get(0).getChoices().get(1).getItemRegistry());
        assertEquals(5,decoded.getDetail().getRewards().get(0).getSelectedChoiceIndex());
        assertEquals("{\"display:10\":{\"Name:8\":\"奖励钻石\"}}",decoded.getDetail().getRewards().get(0).getItemNbt());
        assertEquals(0,buffer.readableBytes());
    }

    @Test
    public void detailWriterRejectsOversizedVectorsInsteadOfSilentlyDroppingEntries() {
        List<TerminalQuestCenterSectionSnapshot.Task> tasks=new ArrayList<TerminalQuestCenterSectionSnapshot.Task>();
        for(int i=0;i<65;i++)tasks.add(new TerminalQuestCenterSectionSnapshot.Task("t"+i,"任务","详情",0,1));
        TerminalQuestCenterSectionSnapshot.Quest quest=new TerminalQuestCenterSectionSnapshot.Quest(
            "q","c","任务","","AVAILABLE",0,65,"",0,"");
        TerminalQuestCenterSectionSnapshot.Detail detail=new TerminalQuestCenterSectionSnapshot.Detail(
            quest,"","","",tasks,java.util.Collections.<TerminalQuestCenterSectionSnapshot.Reward>emptyList());
        TerminalQuestCenterSectionSnapshot original=new TerminalQuestCenterSectionSnapshot("READY","",
            TerminalQuestCenterSectionSnapshot.View.DETAIL,"c","q","all","",
            java.util.Collections.<TerminalQuestCenterSectionSnapshot.Chapter>emptyList(),0,6,0,
            java.util.Collections.<TerminalQuestCenterSectionSnapshot.Quest>emptyList(),0,7,0,detail);
        try {
            OpenTerminalApprovedMessage.writeQuestCenter(Unpooled.buffer(),original);
            throw new AssertionError("oversized quest detail must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("quest tasks count exceeds"));
        }
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

    @Test public void roundTripsParticipantMembershipDirectory() {
        TerminalQuestCenterSectionSnapshot.Member owner=new TerminalQuestCenterSectionSnapshot.Member(
            "550e8400-e29b-41d4-a716-446655440010","OWNER",1000L,7L);
        TerminalQuestCenterSectionSnapshot.Member member=new TerminalQuestCenterSectionSnapshot.Member(
            "550e8400-e29b-41d4-a716-446655440011","MEMBER",2000L,8L);
        TerminalQuestCenterSectionSnapshot.Participant participant=new TerminalQuestCenterSectionSnapshot.Participant(
            "TEAM","550e8400-e29b-41d4-a716-446655440012","银河工业组","OWNER",9L,2,
            Arrays.asList(owner,member));
        TerminalQuestCenterSectionSnapshot original=TerminalQuestCenterSectionSnapshot.unavailable("test")
            .withParticipants(Arrays.asList(participant));
        ByteBuf buffer=Unpooled.buffer();OpenTerminalApprovedMessage.writeQuestCenter(buffer,original);
        TerminalQuestCenterSectionSnapshot decoded=OpenTerminalApprovedMessage.readQuestCenter(buffer);
        assertEquals(1,decoded.getParticipants().size());
        assertEquals(2,decoded.getParticipants().get(0).getMembers().size());
        assertEquals("MEMBER",decoded.getParticipants().get(0).getMembers().get(1).getRole());
        assertEquals(8L,decoded.getParticipants().get(0).getMembers().get(1).getMembershipVersion());
    }

    @Test
    public void roundTripsBoundedManagementProjection() {
        List<TerminalQuestCenterSectionSnapshot.Definition> rows=new ArrayList<TerminalQuestCenterSectionSnapshot.Definition>();
        for(int i=0;i<60;i++)rows.add(new TerminalQuestCenterSectionSnapshot.Definition("q-"+i,i+1,"任务 "+i,
            "DRAFT","hash-"+i,0L,i,2));
        TerminalQuestCenterSectionSnapshot.Management management=new TerminalQuestCenterSectionSnapshot.Management(
            "steel","DRAFT",2,20,80L,rows);
        management=management.withHealthRows(java.util.Arrays.asList("Quest 3744 · Task 5620", "revision 9 · 奖励失败 2"));
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
        assertEquals(management.getHealthRows(),decoded.getManagement().getHealthRows());
        assertEquals(0,buffer.readableBytes());
    }

    @Test public void roundTripsManagedDefinitionDetailAndParameterMaps(){TerminalQuestCenterSectionSnapshot.Definition summary=new TerminalQuestCenterSectionSnapshot.Definition("550e8400-e29b-41d4-a716-446655440000",2,"钢材补给","DRAFT","hash",0,1,1);java.util.Map<String,String> parameters=new java.util.LinkedHashMap<String,String>();parameters.put("item.count","1");parameters.put("item.0.registryName","minecraft:iron_ingot");TerminalQuestCenterSectionSnapshot.DefinitionDetail detail=new TerminalQuestCenterSectionSnapshot.DefinitionDetail(summary,"说明","AND","OR",java.util.Collections.singletonList("550e8400-e29b-41d4-a716-446655440001"),java.util.Collections.singletonList(new TerminalQuestCenterSectionSnapshot.Element("steel","bq_standard:retrieval",false,parameters)),java.util.Collections.singletonList(new TerminalQuestCenterSectionSnapshot.Element("reward","bq_standard:item",false,parameters)),java.util.Collections.singletonMap("behavior.visibility","NORMAL"));TerminalQuestCenterSectionSnapshot.Field field=new TerminalQuestCenterSectionSnapshot.Field("item.count","数量","LONG",true,Long.valueOf(1),Long.valueOf(1000000),java.util.Collections.<String>emptyList());TerminalQuestCenterSectionSnapshot.ElementType taskType=new TerminalQuestCenterSectionSnapshot.ElementType("bq_standard:retrieval","物品提交","TASK",java.util.Collections.singletonList(field));TerminalQuestCenterSectionSnapshot.Migration migration=new TerminalQuestCenterSectionSnapshot.Migration("bq-1234","abcdef","PREVIEW",10L,0L,12,2,3,1,4,"ERROR|UNSUPPORTED_TASK");TerminalQuestCenterSectionSnapshot.Management management=new TerminalQuestCenterSectionSnapshot.Management("","DRAFT",0,20,1,java.util.Collections.singletonList(summary),detail,java.util.Collections.singletonList(taskType),java.util.Collections.<TerminalQuestCenterSectionSnapshot.ElementType>emptyList(),java.util.Collections.singletonList(migration));TerminalQuestCenterSectionSnapshot original=new TerminalQuestCenterSectionSnapshot("READY","",TerminalQuestCenterSectionSnapshot.View.ADMIN,"","","all","",java.util.Collections.<TerminalQuestCenterSectionSnapshot.Chapter>emptyList(),0,6,0,java.util.Collections.<TerminalQuestCenterSectionSnapshot.Quest>emptyList(),0,7,0,null,management);ByteBuf buffer=Unpooled.buffer();OpenTerminalApprovedMessage.writeQuestCenter(buffer,original);TerminalQuestCenterSectionSnapshot decoded=OpenTerminalApprovedMessage.readQuestCenter(buffer);assertNotNull(decoded.getManagement().getDetail());assertEquals("OR",decoded.getManagement().getDetail().getTaskLogic());assertEquals("minecraft:iron_ingot",decoded.getManagement().getDetail().getTasks().get(0).getParameters().get("item.0.registryName"));assertEquals("NORMAL",decoded.getManagement().getDetail().getOptions().get("behavior.visibility"));assertEquals("LONG",decoded.getManagement().getTaskTypes().get(0).getFields().get(0).getType());assertEquals(Long.valueOf(1000000),decoded.getManagement().getTaskTypes().get(0).getFields().get(0).getMaximum());assertEquals("bq-1234",decoded.getManagement().getMigrations().get(0).getBatchId());assertEquals(12,decoded.getManagement().getMigrations().get(0).getCreated());assertEquals(2,decoded.getManagement().getMigrations().get(0).getVersioned());assertEquals(1,decoded.getManagement().getMigrations().get(0).getErrorCount());assertEquals("ERROR|UNSUPPORTED_TASK",decoded.getManagement().getMigrations().get(0).getDiagnostic());}

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

package com.jsirgalaxybase.quest.bqimport;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestChapterEntry;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestDefinitionValidator;
import com.jsirgalaxybase.quest.core.QuestEditorValidationIssue;
import com.jsirgalaxybase.quest.core.StandardQuestEditorTypeRegistry;

/** Builds a deterministic, fail-closed plan without writing source files or a database. */
public final class BqMigrationPlanner {
    public BqMigrationManifest plan(Path source) throws Exception {
        List<BqImportedQuest> quests = new BqQuestDefinitionImporter().readDirectory(source);
        List<BqImportedChapter> chapters = new BqQuestChapterImporter().readDirectory(source);
        Collections.sort(quests,new java.util.Comparator<BqImportedQuest>(){public int compare(BqImportedQuest a,BqImportedQuest b){return a.getDefinition().getId().compareTo(b.getDefinition().getId());}});
        Collections.sort(chapters,new java.util.Comparator<BqImportedChapter>(){public int compare(BqImportedChapter a,BqImportedChapter b){int value=Integer.compare(a.getDisplayOrder(),b.getDisplayOrder());return value==0?a.getDefinition().getId().compareTo(b.getDefinition().getId()):value;}});
        List<BqMigrationIssue> issues = validate(quests, chapters);
        validateChapterOrder(source,chapters,issues);
        List<String> fingerprints = new ArrayList<String>();
        for (BqImportedQuest quest : quests) fingerprints.add("Q|" + quest.getDefinition().getId() + "|" + sha256(quest.getRawJson()));
        for (BqImportedChapter chapter : chapters) {
            StringBuilder value = new StringBuilder("C|").append(chapter.getDefinition().getId()).append('|')
                .append(chapter.getDefinition().getName()).append('|').append(chapter.getDisplayOrder());
            for (QuestChapterEntry entry : chapter.getDefinition().getEntries()) value.append('|').append(entry.getQuestId())
                .append('@').append(entry.getX()).append(',').append(entry.getY()).append(',')
                .append(entry.getWidth()).append(',').append(entry.getHeight());
            fingerprints.add(value.toString());
        }
        Collections.sort(fingerprints);
        String sourceHash = sha256(join(fingerprints));
        return new BqMigrationManifest("bq-" + sourceHash.substring(0, 16), sourceHash, quests, chapters, issues);
    }

    private static List<BqMigrationIssue> validate(List<BqImportedQuest> quests, List<BqImportedChapter> chapters) {
        List<BqMigrationIssue> issues = new ArrayList<BqMigrationIssue>();
        Map<UUID, QuestDefinition> definitions = new HashMap<UUID, QuestDefinition>();
        for (BqImportedQuest imported : quests) {
            QuestDefinition definition = imported.getDefinition();
            if (definitions.put(definition.getId(), definition) != null) error(issues, "DUPLICATE_QUEST_UUID", definition.getId(), "quest UUID appears more than once");
            for (com.jsirgalaxybase.quest.core.TaskDefinition task : definition.getTasks()) {if (!BqStandardCodec.supportedTaskTypes().contains(task.getTypeId())) error(issues, "UNSUPPORTED_TASK", definition.getId(), task.getTypeId());else validateTask(definition.getId(),task,issues);}
            for (com.jsirgalaxybase.quest.core.RewardDefinition reward : definition.getRewards()) {if (!BqStandardCodec.supportedRewardTypes().contains(reward.getTypeId())) error(issues, "UNSUPPORTED_REWARD", definition.getId(), reward.getTypeId());else validateReward(definition.getId(),reward,issues);}
            for (QuestEditorValidationIssue issue : new QuestDefinitionValidator().validate(
                definition, StandardQuestEditorTypeRegistry.create())) {
                error(issues, "INVALID_ELEMENT_PARAMETER", definition.getId() + "/" + issue.getFieldKey(),
                    issue.getCode() + ": " + issue.getMessage());
            }
        }
        for (QuestDefinition definition : definitions.values()) for (UUID prerequisite : definition.getPrerequisites())
            if (!definitions.containsKey(prerequisite)) error(issues, "MISSING_PREREQUISITE", definition.getId(), prerequisite.toString());
        detectCycles(definitions, issues);
        Set<UUID> chapterIds = new HashSet<UUID>();
        Set<UUID> placed = new HashSet<UUID>();
        for (BqImportedChapter chapter : chapters) {
            UUID id = chapter.getDefinition().getId();
            if (!chapterIds.add(id)) error(issues, "DUPLICATE_CHAPTER_UUID", id, "chapter UUID appears more than once");
            validateResource(id, "icon", chapter.getDefinition().getIconReference(), issues);
            validateResource(id, "background", chapter.getDefinition().getBackgroundReference(), issues);
            for (QuestChapterEntry entry : chapter.getDefinition().getEntries()) {
                if (!definitions.containsKey(entry.getQuestId())) error(issues, "DANGLING_CHAPTER_ENTRY", id, entry.getQuestId().toString());
                if (!placed.add(entry.getQuestId())) error(issues, "DUPLICATE_CHAPTER_PLACEMENT", entry.getQuestId(), "quest appears more than once in chapter catalog");
            }
        }
        for (UUID id : definitions.keySet()) if (!placed.contains(id)) issues.add(new BqMigrationIssue(BqMigrationIssue.Severity.WARNING, "UNPLACED_QUEST", id.toString(), "quest is not present in a chapter"));
        return issues;
    }

    private static void detectCycles(Map<UUID, QuestDefinition> definitions, List<BqMigrationIssue> issues) {
        Set<UUID> visited = new HashSet<UUID>(), active = new HashSet<UUID>();
        for (UUID id : definitions.keySet()) visit(id, definitions, visited, active, issues);
    }
    private static void validateChapterOrder(Path source,List<BqImportedChapter> chapters,List<BqMigrationIssue> issues)throws java.io.IOException{
        Set<UUID> chapterIds=new HashSet<UUID>();for(BqImportedChapter chapter:chapters){chapterIds.add(chapter.getDefinition().getId());if(chapter.getDisplayOrder()==Integer.MAX_VALUE)error(issues,"MISSING_CHAPTER_ORDER",chapter.getDefinition().getId(),"chapter is absent from QuestLinesOrder.txt");}
        for(String line:java.nio.file.Files.readAllLines(source.resolve("QuestLinesOrder.txt"),StandardCharsets.UTF_8)){String value=line.trim();if(value.isEmpty())continue;int delimiter=value.indexOf(':');if(delimiter>0){UUID id=BqQuestChapterImporter.decodeUuid(value.substring(0,delimiter).trim());if(!chapterIds.contains(id))error(issues,"DANGLING_CHAPTER_ORDER",id,"order entry has no chapter directory");}}
    }
    private static void validateTask(UUID questId,com.jsirgalaxybase.quest.core.TaskDefinition task,List<BqMigrationIssue> issues){
        Map<String,String> values=task.getParameters();String subject=questId+"/"+task.getKey();
        if(!values.containsKey("bq.raw"))error(issues,"INVALID_TASK_PARAMETER",subject,"missing preserved BQ payload");
        String target=values.get("target");if(target==null||positive(target)<1)error(issues,"INVALID_TASK_PARAMETER",subject,"target must be positive");
        String type=task.getTypeId();if(("bq_standard:retrieval".equals(type)||"bq_standard:optional_retrieval".equals(type)||"bq_standard:crafting".equals(type))&&positive(values.get("item.count"))<1)error(issues,"INVALID_TASK_PARAMETER",subject,"at least one item is required");
        if("bq_standard:fluid".equals(type)&&positive(values.get("fluid.count"))<1)error(issues,"INVALID_TASK_PARAMETER",subject,"at least one fluid is required");
        if("bq_standard:block_break".equals(type)&&positive(values.get("block.count"))<1)error(issues,"INVALID_TASK_PARAMETER",subject,"at least one block is required");
    }
    private static void validateReward(UUID questId,com.jsirgalaxybase.quest.core.RewardDefinition reward,List<BqMigrationIssue> issues){
        Map<String,String> values=reward.getParameters();String subject=questId+"/"+reward.getKey();if(!values.containsKey("bq.raw"))error(issues,"INVALID_REWARD_PARAMETER",subject,"missing preserved BQ payload");
        if(("bq_standard:item".equals(reward.getTypeId())||"bq_standard:choice".equals(reward.getTypeId()))&&positive(values.get("item.count"))<1)error(issues,"INVALID_REWARD_PARAMETER",subject,"at least one item is required");
        if("bq_standard:questcompletion".equals(reward.getTypeId()))try{UUID.fromString(values.get("questUuid"));}catch(RuntimeException invalid){error(issues,"INVALID_REWARD_PARAMETER",subject,"questUuid is invalid");}
    }
    private static long positive(String value){try{return Math.max(0L,Long.parseLong(value));}catch(RuntimeException invalid){return 0L;}}
    private static void validateResource(UUID chapterId,String field,String value,List<BqMigrationIssue> issues){if(value==null||value.isEmpty())return;if(value.indexOf('\0')>=0||value.contains("..")||value.startsWith("/")||value.startsWith("\\")||value.contains("://"))error(issues,"INVALID_RESOURCE_REFERENCE",chapterId+"/"+field,"unsafe resource reference");}
    private static void visit(UUID id, Map<UUID, QuestDefinition> definitions, Set<UUID> visited, Set<UUID> active, List<BqMigrationIssue> issues) {
        if (visited.contains(id)) return;
        if (!active.add(id)) { error(issues, "PREREQUISITE_CYCLE", id, "cycle detected"); return; }
        QuestDefinition definition = definitions.get(id);
        if (definition != null) for (UUID prerequisite : definition.getPrerequisites()) if (definitions.containsKey(prerequisite)) visit(prerequisite, definitions, visited, active, issues);
        active.remove(id); visited.add(id);
    }
    private static void error(List<BqMigrationIssue> issues, String code, Object subject, String message) { issues.add(new BqMigrationIssue(BqMigrationIssue.Severity.ERROR, code, String.valueOf(subject), message)); }
    private static String join(List<String> values) { StringBuilder out = new StringBuilder(); for (String value : values) out.append(value).append('\n'); return out.toString(); }
    private static String sha256(String value) { try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format("%02x", b & 255)); return out.toString(); } catch (Exception impossible) { throw new IllegalStateException(impossible); } }
}

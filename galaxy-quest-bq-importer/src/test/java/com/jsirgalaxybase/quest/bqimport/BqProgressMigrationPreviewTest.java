package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.*;
import java.nio.file.Paths;
import java.util.*;
import org.junit.Test;
import com.jsirgalaxybase.quest.core.*;

public class BqProgressMigrationPreviewTest {
    private final UUID quest = new UUID(0, 1);
    private BqImportedQuestProgress progress(long value) {
        return new BqImportedQuestProgress(quest, false, false, 0,
            Collections.singletonList(new BqImportedTaskProgress("task-0", "bq_standard:hunt", false,
                Collections.singletonList(value), "{}")), "{}");
    }
    private BqMigrationManifest manifest(RepeatPolicy repeat, boolean choice) {
        TaskDefinition task = new TaskDefinition("task-0", "bq_standard:hunt", false,
            Collections.singletonMap("target", "10"));
        QuestDefinition definition = new QuestDefinition(quest, 1, "Q", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(task), choice
                ? Collections.singletonList(new RewardDefinition("reward-0", "bq_standard:choice", Collections.<String,String>emptyMap()))
                : Collections.<RewardDefinition>emptyList(), repeat);
        return new BqMigrationManifest("test", "hash", Collections.singletonList(new BqImportedQuest(
            definition, Paths.get("q.json"), "{}", Collections.<BqStandardTaskSpec>emptyList(),
            Collections.<BqStandardRewardSpec>emptyList())), Collections.<BqImportedChapter>emptyList(),
            Collections.<BqMigrationIssue>emptyList());
    }
    private List<BqImportedPlayerProgress> player(long id, long value) {
        return Collections.singletonList(new BqImportedPlayerProgress(new UUID(0, id), Paths.get("p.json"),
            Collections.singletonList(progress(value))));
    }
    @Test public void deterministicMultiSourceRelationsNeverSelectOrMerge() {
        Map<String,List<BqImportedPlayerProgress>> sources = new LinkedHashMap<String,List<BqImportedPlayerProgress>>();
        sources.put("s2", player(2, 7)); sources.put("lobby", player(2, 3));
        String first = new BqProgressMigrationPreview().report(manifest(RepeatPolicy.never(), false), sources);
        Map<String,List<BqImportedPlayerProgress>> reversed = new TreeMap<String,List<BqImportedPlayerProgress>>(sources);
        assertEquals(first, new BqProgressMigrationPreview().report(manifest(RepeatPolicy.never(), false), reversed));
        assertTrue(first.contains("RIGHT_AHEAD")); assertTrue(first.contains("REVIEW_ONLY"));
        assertTrue(first.contains("\"applyAllowed\": false"));
    }
    @Test public void repeatAndChoiceRemainExplicitlyBlocked() {
        String report = new BqProgressMigrationPreview().report(manifest(RepeatPolicy.after(100), true),
            Collections.singletonMap("s2", player(2, 3)));
        assertTrue(report.contains("REPEAT_CYCLE_UNRESOLVED")); assertTrue(report.contains("CHOICE_SELECTION_UNMAPPED"));
        assertTrue(report.contains("BLOCKED"));
    }
    @Test public void differentPlayersAreNotCompared() {
        Map<String,List<BqImportedPlayerProgress>> sources = new TreeMap<String,List<BqImportedPlayerProgress>>();
        sources.put("lobby", player(2, 3)); sources.put("s2", player(3, 7));
        String report = new BqProgressMigrationPreview().report(manifest(RepeatPolicy.never(), false), sources);
        assertFalse(report.contains("RIGHT_AHEAD"));
        assertEquals(2, com.google.gson.JsonParser.parseString(report).getAsJsonObject().getAsJsonArray("entries").size());
    }
    @Test public void missingDefinitionIsBlocked() {
        BqMigrationManifest empty = new BqMigrationManifest("test", "hash", Collections.<BqImportedQuest>emptyList(),
            Collections.<BqImportedChapter>emptyList(), Collections.<BqMigrationIssue>emptyList());
        assertTrue(new BqProgressMigrationPreview().report(empty, Collections.singletonMap("s2", player(2,3)))
            .contains("MISSING_DEFINITION"));
    }
    @Test(expected = BqProgressShapeException.class) public void negativeValuesAreRejected() {
        new BqProgressMapper().toCore(manifest(RepeatPolicy.never(), false).getQuests().get(0).getDefinition(), progress(-1));
    }
    @Test(expected = BqProgressShapeException.class) public void duplicateTaskIsRejected() {
        BqImportedTaskProgress task = progress(3).getTasks().get(0);
        new BqProgressMapper().toCore(manifest(RepeatPolicy.never(), false).getQuests().get(0).getDefinition(),
            new BqImportedQuestProgress(quest, false, false, 0, Arrays.asList(task, task), "{}"));
    }
    @Test public void completionAndClaimFlagsRemainEvidenceWithoutGranting() {
        BqImportedQuestProgress completed = new BqImportedQuestProgress(quest, true, true, 42,
            progress(10).getTasks(), "{}");
        BqImportedPlayerProgress player = new BqImportedPlayerProgress(new UUID(0,2), Paths.get("p.json"),
            Collections.singletonList(completed));
        String report = new BqProgressMigrationPreview().report(manifest(RepeatPolicy.never(), false),
            Collections.singletonMap("s2", Collections.singletonList(player)));
        assertTrue(report.contains("\"claimed\": true")); assertTrue(report.contains("\"timestamp\": 42"));
        assertTrue(report.contains("\"applyAllowed\": false"));
    }
    @Test public void claimedWithoutCompletionIsBlocked() {
        BqImportedPlayerProgress player = new BqImportedPlayerProgress(new UUID(0,2), Paths.get("p.json"),
            Collections.singletonList(new BqImportedQuestProgress(quest, false, true, 42, progress(3).getTasks(), "{}")));
        assertTrue(new BqProgressMigrationPreview().report(manifest(RepeatPolicy.never(), false),
            Collections.singletonMap("s2", Collections.singletonList(player))).contains("CLAIM_WITHOUT_COMPLETION"));
    }
}

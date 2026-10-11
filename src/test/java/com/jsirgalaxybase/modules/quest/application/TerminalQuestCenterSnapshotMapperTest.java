package com.jsirgalaxybase.modules.quest.application;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestBehavior;
import com.jsirgalaxybase.quest.core.QuestCenterEntry;
import com.jsirgalaxybase.quest.core.QuestCenterPage;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestRewardSummary;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot;

public class TerminalQuestCenterSnapshotMapperTest {
    @Test
    public void keepsCraftingStatisticsAutomaticInsteadOfExposingManualDetect() {
        QuestDefinition personal = quest(QuestBehavior.DEFAULT, "true");
        assertNotDetectable(personal, QuestStatus.IN_PROGRESS, true);
        assertNotDetectable(personal, QuestStatus.LOCKED, true);
        assertNotDetectable(personal, QuestStatus.COMPLETED, true);
        assertNotDetectable(personal, QuestStatus.IN_PROGRESS, false);
        assertNotDetectable(quest(QuestBehavior.builder().globalShare(true).build(), "true"),
            QuestStatus.IN_PROGRESS, true);
        assertNotDetectable(quest(QuestBehavior.builder().global(true).globalShare(true).build(), "true"),
            QuestStatus.IN_PROGRESS, true);
        assertNotDetectable(quest(QuestBehavior.builder()
            .participantScope(com.jsirgalaxybase.quest.core.QuestParticipantScope.PARTY).build(), "true"),
            QuestStatus.IN_PROGRESS, true);
        assertNotDetectable(quest(QuestBehavior.DEFAULT, "false"), QuestStatus.IN_PROGRESS, true);
    }

    @Test
    public void repeatCraftingQuestStillUsesAutomaticStatisticsRefresh() {
        QuestDefinition definition = quest(QuestBehavior.DEFAULT, "true");
        UUID player = UUID.randomUUID();
        QuestProgressSnapshot progress = new QuestProgressSnapshot(player, definition.getId(), definition.getVersion(),
            QuestStatus.COMPLETED, Collections.singletonMap("craft",
                new com.jsirgalaxybase.quest.core.TaskProgress("craft", 4L, 4L, true)), 1L);
        QuestCenterEntry entry = new QuestCenterEntry(definition, progress, QuestRewardSummary.EMPTY,
            QuestStatus.COMPLETED);
        QuestCenterPage page = new QuestCenterPage(ParticipantId.player(player), "chapter",
            Collections.<com.jsirgalaxybase.quest.core.QuestCenterChapterSummary>emptyList(), 0, 6, 0,
            Collections.singletonList(entry), 0, 7, 1);
        TerminalQuestCenterSectionSnapshot snapshot = new TerminalQuestCenterSnapshotMapper().map(page,
            "all", "", definition.getId().toString(), true, "", false);
        assertFalse(snapshot.getDetail().getTasks().get(0).isDetectable());
    }

    @Test
    public void checkboxConfirmationIsHiddenWhileLockedUnlessLockedProgressIsEnabled() {
        QuestDefinition locked = checkboxQuest(QuestBehavior.DEFAULT);
        assertConfirmable(locked, QuestStatus.LOCKED, false);
        assertConfirmable(locked, QuestStatus.AVAILABLE, true);
        assertConfirmable(checkboxQuest(QuestBehavior.builder().progressWhileLocked(true).build()),
            QuestStatus.LOCKED, true);
    }

    @Test
    public void exposesManualSubmissionOnlyForConsumingInventoryTasks() {
        assertSubmittable(retrievalQuest(true, false), true, true);
        assertSubmittable(retrievalQuest(false, false), true, false);
        assertSubmittable(retrievalQuest(true, true), true, false);
        assertSubmittable(retrievalQuest(true, false), false, false);
    }

    private static void assertNotDetectable(QuestDefinition definition, QuestStatus status, boolean runtimeAvailable) {
        QuestCenterEntry entry = new QuestCenterEntry(definition, null, QuestRewardSummary.EMPTY, status);
        QuestCenterPage page = new QuestCenterPage(ParticipantId.player(UUID.randomUUID()), "chapter",
            Collections.<com.jsirgalaxybase.quest.core.QuestCenterChapterSummary>emptyList(), 0, 6, 0,
            Collections.singletonList(entry), 0, 7, 1);
        TerminalQuestCenterSectionSnapshot snapshot = new TerminalQuestCenterSnapshotMapper().map(page,
            "all", "", definition.getId().toString(), true, "", false);
        assertFalse(snapshot.getDetail().getTasks().get(0).isDetectable());
    }

    private static QuestDefinition quest(QuestBehavior behavior, String allowStatistics) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "1");
        parameters.put("item.0.registryName", "minecraft:iron_ingot");
        parameters.put("item.0.meta", "0");
        parameters.put("item.0.amount", "4");
        parameters.put("allowCraftedFromStatistics", allowStatistics);
        return new QuestDefinition(UUID.randomUUID(), 1, "Craft", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Arrays.asList(new TaskDefinition("craft", "bq_standard:crafting", false,
                parameters)), Collections.<com.jsirgalaxybase.quest.core.RewardDefinition>emptyList(),
            RepeatPolicy.after(0L), behavior);
    }

    private static void assertConfirmable(QuestDefinition definition, QuestStatus status, boolean expected) {
        QuestCenterEntry entry = new QuestCenterEntry(definition, null, QuestRewardSummary.EMPTY, status);
        QuestCenterPage page = new QuestCenterPage(ParticipantId.player(UUID.randomUUID()), "chapter",
            Collections.<com.jsirgalaxybase.quest.core.QuestCenterChapterSummary>emptyList(), 0, 6, 0,
            Collections.singletonList(entry), 0, 7, 1);
        TerminalQuestCenterSectionSnapshot snapshot = new TerminalQuestCenterSnapshotMapper().map(page,
            "all", "", definition.getId().toString(), true, "");
        if (expected) assertTrue(snapshot.getDetail().getTasks().get(0).isConfirmable());
        else assertFalse(snapshot.getDetail().getTasks().get(0).isConfirmable());
    }

    private static void assertSubmittable(QuestDefinition definition, boolean runtimeAvailable, boolean expected) {
        QuestCenterEntry entry = new QuestCenterEntry(definition, null, QuestRewardSummary.EMPTY,
            QuestStatus.IN_PROGRESS);
        QuestCenterPage page = new QuestCenterPage(ParticipantId.player(UUID.randomUUID()), "chapter",
            Collections.<com.jsirgalaxybase.quest.core.QuestCenterChapterSummary>emptyList(), 0, 6, 0,
            Collections.singletonList(entry), 0, 7, 1);
        TerminalQuestCenterSectionSnapshot snapshot = new TerminalQuestCenterSnapshotMapper().map(page,
            "all", "", definition.getId().toString(), true, "", runtimeAvailable);
        if (expected) assertTrue(snapshot.getDetail().getTasks().get(0).isSubmittable());
        else assertFalse(snapshot.getDetail().getTasks().get(0).isSubmittable());
    }

    private static QuestDefinition retrievalQuest(boolean consume, boolean autoConsume) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("consume", Boolean.toString(consume));
        parameters.put("autoConsume", Boolean.toString(autoConsume));
        parameters.put("item.count", "1");
        parameters.put("item.0.registryName", "minecraft:iron_ingot");
        parameters.put("item.0.meta", "0");
        parameters.put("item.0.amount", "4");
        return new QuestDefinition(UUID.randomUUID(), 1, "Retrieval", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(new TaskDefinition("retrieval",
                "bq_standard:retrieval", false, parameters)),
            Collections.<com.jsirgalaxybase.quest.core.RewardDefinition>emptyList(), RepeatPolicy.never(),
            QuestBehavior.DEFAULT);
    }

    private static QuestDefinition checkboxQuest(QuestBehavior behavior) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", "galaxy:task_confirmed");
        return new QuestDefinition(UUID.randomUUID(), 1, "Checkbox", "", QuestLogic.AND, QuestLogic.AND,
            Collections.singleton(UUID.randomUUID()), Collections.singletonList(
                new TaskDefinition("check", "bq_standard:checkbox", false, parameters)),
            Collections.<com.jsirgalaxybase.quest.core.RewardDefinition>emptyList(), RepeatPolicy.never(), behavior);
    }
}

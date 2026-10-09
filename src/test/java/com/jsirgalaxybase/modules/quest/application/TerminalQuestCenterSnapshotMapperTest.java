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
    public void exposesCraftingDetectOnlyForAvailablePlayerCraftingTasks() {
        QuestDefinition personal = quest(QuestBehavior.DEFAULT, "true");
        assertDetectable(personal, QuestStatus.IN_PROGRESS, true, true);
        assertDetectable(personal, QuestStatus.LOCKED, true, false);
        assertDetectable(personal, QuestStatus.COMPLETED, true, false);
        assertDetectable(personal, QuestStatus.IN_PROGRESS, false, false);
        assertDetectable(quest(QuestBehavior.builder().globalShare(true).build(), "true"),
            QuestStatus.IN_PROGRESS, true, true);
        assertDetectable(quest(QuestBehavior.builder().global(true).globalShare(true).build(), "true"),
            QuestStatus.IN_PROGRESS, true, false);
        assertDetectable(quest(QuestBehavior.builder()
            .participantScope(com.jsirgalaxybase.quest.core.QuestParticipantScope.PARTY).build(), "true"),
            QuestStatus.IN_PROGRESS, true, false);
        assertDetectable(quest(QuestBehavior.DEFAULT, "false"), QuestStatus.IN_PROGRESS, true, false);
    }

    @Test
    public void exposesCraftingDetectAgainWhenARepeatCycleIsAvailable() {
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
            "all", "", definition.getId().toString(), true, "", false, true);
        assertTrue(snapshot.getDetail().getTasks().get(0).isDetectable());
    }

    @Test
    public void checkboxConfirmationIsHiddenWhileLockedUnlessLockedProgressIsEnabled() {
        QuestDefinition locked = checkboxQuest(QuestBehavior.DEFAULT);
        assertConfirmable(locked, QuestStatus.LOCKED, false);
        assertConfirmable(locked, QuestStatus.AVAILABLE, true);
        assertConfirmable(checkboxQuest(QuestBehavior.builder().progressWhileLocked(true).build()),
            QuestStatus.LOCKED, true);
    }

    private static void assertDetectable(QuestDefinition definition, QuestStatus status, boolean runtimeAvailable,
        boolean expected) {
        QuestCenterEntry entry = new QuestCenterEntry(definition, null, QuestRewardSummary.EMPTY, status);
        QuestCenterPage page = new QuestCenterPage(ParticipantId.player(UUID.randomUUID()), "chapter",
            Collections.<com.jsirgalaxybase.quest.core.QuestCenterChapterSummary>emptyList(), 0, 6, 0,
            Collections.singletonList(entry), 0, 7, 1);
        TerminalQuestCenterSectionSnapshot snapshot = new TerminalQuestCenterSnapshotMapper().map(page,
            "all", "", definition.getId().toString(), true, "", false, runtimeAvailable);
        boolean detectable = snapshot.getDetail().getTasks().get(0).isDetectable();
        if (expected) assertTrue(detectable); else assertFalse(detectable);
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

    private static QuestDefinition checkboxQuest(QuestBehavior behavior) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", "galaxy:task_confirmed");
        return new QuestDefinition(UUID.randomUUID(), 1, "Checkbox", "", QuestLogic.AND, QuestLogic.AND,
            Collections.singleton(UUID.randomUUID()), Collections.singletonList(
                new TaskDefinition("check", "bq_standard:checkbox", false, parameters)),
            Collections.<com.jsirgalaxybase.quest.core.RewardDefinition>emptyList(), RepeatPolicy.never(), behavior);
    }
}

package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

public class QuestFactProjectorTest {
    @Test
    public void projectsMatchingFactsAndExposesStableDeduplicationKey() {
        UUID player = UUID.randomUUID();
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("target", "64");
        parameters.put("factType", "minecraft:item_obtained");
        parameters.put("subject", "minecraft:iron_ingot:0");
        TaskDefinition task = new TaskDefinition("iron", "count", false, parameters);
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Iron", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
        QuestProgressSnapshot initial = new QuestEngine().evaluate(player, definition, null,
            Collections.<String, TaskProgress>emptyMap(), Collections.<UUID>emptySet(), 1L).getProgress();
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("subject", "minecraft:iron_ingot:0");
        attributes.put("amount", "16");
        GameplayFact fact = new GameplayFact("s2", "craft:42", player, "minecraft:item_obtained", 2L, attributes);
        Map<String, TaskProgress> observed = new QuestFactProjector(registry).project(definition, initial, fact);

        assertEquals("s2:craft:42", fact.getFactKey());
        assertEquals(16L, observed.get("iron").getValue());
    }

    @Test
    public void partyProgressAcceptsFactsFromAnySnapshottedMember() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ParticipantId party = ParticipantId.party(UUID.randomUUID());
        TaskDefinition task = new TaskDefinition("meeting", "count", false,
            map("target", "2", "factType", "minecraft:meeting", "subject", "station"));
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Meeting", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
        ParticipantMembership membership = new ParticipantMembership(party,
            new HashSet<UUID>(Arrays.asList(first, second)));
        QuestProgressSnapshot initial = new QuestEngine().evaluate(membership, definition, null,
            Collections.<String, TaskProgress>emptyMap(), Collections.<UUID>emptySet(), 1L).getProgress();
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        GameplayFact fact = new GameplayFact("s1", "meeting:1", second, "minecraft:meeting", 2L,
            map("subject", "station", "amount", "1"));
        assertEquals(1L, new QuestFactProjector(registry).project(definition, initial, membership, fact)
            .get("meeting").getValue());
    }

    @Test
    public void singleMemberInventoryDoesNotOverwriteSharedRetrievalProgress() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ParticipantId party = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership membership = new ParticipantMembership(party,
            new HashSet<UUID>(Arrays.asList(first, second)));
        TaskDefinition task = new TaskDefinition("items", "bq_standard:retrieval", false,
            map("factType", "galaxy:inventory_observed", "progressMode", "item-vector", "item.count", "1",
                "item.0.amount", "10", "consume", "false", "groupDetect", "false"));
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Shared retrieval", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
        TaskProgress stored = new TaskProgress("items", Arrays.asList(3L), Arrays.asList(10L), false);
        QuestProgressSnapshot current = new QuestProgressSnapshot(party, definition.getId(), definition.getVersion(),
            QuestStatus.IN_PROGRESS, Collections.singletonMap("items", stored), 0L, 0);
        TaskEvaluatorRegistry registry = BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch();
        GameplayFact inventory = new GameplayFact("s1", "inventory:" + first, first,
            "galaxy:inventory_observed", 10L, map("inventory.count", "1", "inventory.0.amount", "10",
                "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0"));

        Map<String, TaskProgress> projected = new QuestFactProjector(registry)
            .project(definition, current, membership, inventory);
        assertEquals(Arrays.asList(3L), projected.get("items").getValues());
    }

    @Test
    public void groupInventorySnapshotUpdatesOnlyItsMatchingSharedParticipant() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ParticipantId party = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership membership = new ParticipantMembership(party,
            new HashSet<UUID>(Arrays.asList(first, second)));
        TaskDefinition task = new TaskDefinition("items", "bq_standard:retrieval", false,
            map("factType", "galaxy:inventory_observed", "progressMode", "item-vector", "item.count", "1",
                "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "10",
                "consume", "false", "groupDetect", "true"));
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Shared retrieval", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.PARTY).build());
        TaskProgress stored = new TaskProgress("items", Arrays.asList(3L), Arrays.asList(10L), false);
        QuestProgressSnapshot current = new QuestProgressSnapshot(party, definition.getId(), definition.getVersion(),
            QuestStatus.IN_PROGRESS, Collections.singletonMap("items", stored), 0L, 0);
        GameplayFact aggregate = new GameplayFact("s1", "party-inventory:" + party.getId(), first,
            "galaxy:inventory_observed", 11L, map("inventory.groupSnapshot", "true",
                "inventory.participantType", "PARTY", "inventory.participantId", party.getId().toString(),
                "inventory.count", "1", "inventory.0.registryName", "minecraft:iron_ingot",
                "inventory.0.meta", "0", "inventory.0.amount", "7"));

        Map<String, TaskProgress> projected = new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry
            .firstEventDrivenBatch()).project(definition, current, membership, aggregate);
        assertEquals(Arrays.asList(7L), projected.get("items").getValues());

        ParticipantMembership anotherParty = new ParticipantMembership(ParticipantId.party(UUID.randomUUID()),
            new HashSet<UUID>(Arrays.asList(first, second)));
        QuestProgressSnapshot otherProgress = new QuestProgressSnapshot(anotherParty.getParticipantId(),
            definition.getId(), definition.getVersion(), QuestStatus.IN_PROGRESS,
            Collections.singletonMap("items", stored), 0L, 0);
        assertEquals(Arrays.asList(3L), new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry
            .firstEventDrivenBatch()).project(definition, otherProgress, anotherParty, aggregate)
            .get("items").getValues());
    }

    @Test
    public void groupInventorySnapshotCannotUpdatePersonalRetrievalProgress() {
        UUID player = UUID.randomUUID();
        TaskDefinition task = new TaskDefinition("items", "bq_standard:retrieval", false,
            map("factType", "galaxy:inventory_observed", "progressMode", "item-vector", "item.count", "1",
                "item.0.amount", "10", "consume", "false", "groupDetect", "true"));
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Personal retrieval", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
        QuestProgressSnapshot current = new QuestProgressSnapshot(ParticipantId.player(player), definition.getId(),
            definition.getVersion(), QuestStatus.IN_PROGRESS,
            Collections.singletonMap("items", new TaskProgress("items", Arrays.asList(2L), Arrays.asList(10L), false)),
            0L, 0);
        GameplayFact aggregate = new GameplayFact("s1", "team-inventory", player, "galaxy:inventory_observed",
            11L, map("inventory.groupSnapshot", "true", "inventory.participantType", "TEAM",
                "inventory.participantId", UUID.randomUUID().toString(), "inventory.count", "0"));

        assertEquals(Arrays.asList(2L), new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry
            .firstEventDrivenBatch()).project(definition, current, ParticipantMembership.player(player), aggregate)
            .get("items").getValues());
    }

    @Test
    public void bqPartyInventorySnapshotUpdatesOnlyGroupDetectPersonalRetrieval() {
        UUID player = UUID.randomUUID();
        ParticipantId party = ParticipantId.party(UUID.randomUUID());
        GameplayFact aggregate = new GameplayFact("s1", "bq-party-inventory", player,
            "galaxy:inventory_observed", 12L, map("inventory.bqPartySnapshot", "true",
                "inventory.bqPartyId", party.getId().toString(), "inventory.count", "1",
                "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0",
                "inventory.0.amount", "7"));
        TaskDefinition shared = new TaskDefinition("shared", "bq_standard:retrieval", false,
            map("factType", "galaxy:inventory_observed", "progressMode", "item-vector", "item.count", "1",
                "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "10",
                "consume", "false", "groupDetect", "true", "bq.raw", "{}"));
        TaskDefinition personal = new TaskDefinition("personal", "bq_standard:optional_retrieval", false,
            map("factType", "galaxy:inventory_observed", "progressMode", "item-vector", "item.count", "1",
                "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "10",
                "consume", "false", "groupDetect", "false"));
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Retrieval", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Arrays.asList(shared, personal),
            Collections.<RewardDefinition>emptyList());
        Map<String, TaskProgress> progress = new LinkedHashMap<String, TaskProgress>();
        progress.put("shared", new TaskProgress("shared", Arrays.asList(0L), Arrays.asList(10L), false));
        progress.put("personal", new TaskProgress("personal", Arrays.asList(2L), Arrays.asList(10L), false));
        QuestProgressSnapshot current = new QuestProgressSnapshot(ParticipantId.player(player), definition.getId(),
            definition.getVersion(), QuestStatus.IN_PROGRESS, progress, 0L, 0);

        GameplayFact singlePlayer = new GameplayFact("s1", "single-inventory", player,
            "galaxy:inventory_observed", 11L, map("inventory.count", "1", "inventory.0.registryName",
                "minecraft:iron_ingot", "inventory.0.meta", "0", "inventory.0.amount", "7"));
        Map<String, TaskProgress> personalProjection = new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry
            .firstEventDrivenBatch()).project(definition, current, ParticipantMembership.player(player), singlePlayer);
        assertEquals(Arrays.asList(0L), personalProjection.get("shared").getValues());
        assertEquals(Arrays.asList(7L), personalProjection.get("personal").getValues());

        Map<String, TaskProgress> projected = new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry
            .firstEventDrivenBatch()).project(definition, current, ParticipantMembership.player(player), aggregate);

        assertEquals(Arrays.asList(7L), projected.get("shared").getValues());
        assertEquals(Arrays.asList(2L), projected.get("personal").getValues());
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }
}

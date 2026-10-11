package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

public class AuthenticatedCraftingStatisticsDetectionServiceTest {
    @Test
    public void explicitDetectScopesStatisticsToTheSelectedQuestAndTask() {
        Fixture fixture = new Fixture();
        QuestDefinition target = quest("target", RepeatPolicy.never(), QuestBehavior.DEFAULT,
            crafting("selected", true, item("minecraft:iron_ingot", 2), item("minecraft:gold_ingot", 3)),
            crafting("sibling", true, item("minecraft:iron_ingot", 8)));
        QuestDefinition other = quest("other", RepeatPolicy.never(), QuestBehavior.DEFAULT,
            crafting("same-item", true, item("minecraft:iron_ingot", 1)));
        fixture.publish(target);
        fixture.publish(other);
        fixture.statistics.put("minecraft:iron_ingot", Long.valueOf(7L));
        fixture.statistics.put("minecraft:gold_ingot", Long.valueOf(5L));

        List<FactProcessingResult> results = fixture.service().detect(fixture.player, target.getId(), "selected", 100L);

        assertEquals(2, results.size());
        assertEquals(FactProcessingStatus.ACCEPTED, results.get(0).getStatus());
        assertEquals(FactProcessingStatus.ACCEPTED, results.get(1).getStatus());
        QuestProgressSnapshot progress = fixture.runtime.progress(fixture.player, target.getId());
        assertEquals("BQ keeps absolute crafting statistics even after the task target is exceeded",
            Arrays.asList(Long.valueOf(7L), Long.valueOf(5L)),
            progress.getTasks().get("selected").getValues());
        assertEquals(Collections.singletonList(Long.valueOf(0L)), progress.getTasks().get("sibling").getValues());
        assertEquals(null, fixture.runtime.progress(fixture.player, other.getId()));
        assertTrue(fixture.runtime.entitlements.isEmpty());
    }

    @Test
    public void explicitDetectUsesAbsoluteLifetimeStatisticsAgainInAnAvailableRepeatCycle() {
        Fixture fixture = new Fixture();
        QuestDefinition definition = quest("repeat", RepeatPolicy.after(10L), QuestBehavior.DEFAULT,
            crafting("craft", true, item("minecraft:iron_ingot", 1)));
        fixture.publish(definition);
        fixture.statistics.put("minecraft:iron_ingot", Long.valueOf(4L));

        assertEquals(FactProcessingStatus.ACCEPTED,
            fixture.service().detect(fixture.player, definition.getId(), "craft", 100L).get(0).getStatus());
        assertTrue(fixture.service().detect(fixture.player, definition.getId(), "craft", 109L).isEmpty());
        assertEquals(FactProcessingStatus.ACCEPTED,
            fixture.service().detect(fixture.player, definition.getId(), "craft", 110L).get(0).getStatus());

        QuestProgressSnapshot progress = fixture.runtime.progress(fixture.player, definition.getId());
        assertEquals(1, progress.getCycle());
        assertEquals(2, fixture.runtime.entitlements.size());
    }

    @Test
    public void earlyRepeatDetectionDoesNotConsumeFactKeyBeforeRewardClaimAndClaimAnchoredCooldown() {
        Fixture fixture = new Fixture();
        QuestDefinition definition = quest("repeat-claim-anchor", RepeatPolicy.after(10L), QuestBehavior.DEFAULT,
            crafting("craft", true, item("minecraft:iron_ingot", 1)));
        fixture.publish(definition);
        fixture.statistics.put("minecraft:iron_ingot", Long.valueOf(4L));

        assertEquals(FactProcessingStatus.ACCEPTED,
            fixture.service().detect(fixture.player, definition.getId(), "craft", 100L).get(0).getStatus());
        fixture.runtime.claimableRewards = true;
        assertTrue(fixture.service().detect(fixture.player, definition.getId(), "craft", 110L).isEmpty());
        assertEquals(1, fixture.runtime.facts.size());

        fixture.runtime.claimableRewards = false;
        fixture.runtime.repeatAnchorMillis = Long.valueOf(120L);
        fixture.runtime.pendingConsumption = true;
        assertTrue(fixture.service().detect(fixture.player, definition.getId(), "craft", 129L).isEmpty());
        assertTrue(fixture.service().detect(fixture.player, definition.getId(), "craft", 130L).isEmpty());
        assertEquals(1, fixture.runtime.facts.size());
        fixture.runtime.pendingConsumption = false;
        assertEquals(FactProcessingStatus.ACCEPTED,
            fixture.service().detect(fixture.player, definition.getId(), "craft", 130L).get(0).getStatus());
        assertEquals(2, fixture.runtime.facts.size());
        assertEquals(1, fixture.runtime.progress(fixture.player, definition.getId()).getCycle());
    }

    @Test
    public void repeatedDetectOfTheSameLifetimeStatisticIsIdempotentAcrossClicks() {
        Fixture fixture = new Fixture();
        QuestDefinition definition = quest("partial", RepeatPolicy.never(), QuestBehavior.DEFAULT,
            crafting("craft", true, item("minecraft:iron_ingot", 10)));
        fixture.publish(definition);
        fixture.statistics.put("minecraft:iron_ingot", Long.valueOf(4L));

        assertEquals(FactProcessingStatus.ACCEPTED,
            fixture.service().detect(fixture.player, definition.getId(), "craft", 100L).get(0).getStatus());
        assertEquals(FactProcessingStatus.DUPLICATE,
            fixture.service().detect(fixture.player, definition.getId(), "craft", 200L).get(0).getStatus());
        assertEquals(Collections.singletonList(Long.valueOf(4L)),
            fixture.runtime.progress(fixture.player, definition.getId()).getTasks().get("craft").getValues());
    }

    @Test
    public void detectAllowsGlobalShareOnPersonalQuestsButRejectsGlobalAndUnsupportedTasks() {
        Fixture fixture = new Fixture();
        QuestBehavior shareFlag = QuestBehavior.builder().globalShare(true).build();
        QuestDefinition personalQuest = quest("personal-share-flag", RepeatPolicy.never(), shareFlag,
            crafting("craft", true, item("minecraft:iron_ingot", 1)));
        QuestBehavior global = QuestBehavior.builder().global(true).globalShare(true).build();
        QuestDefinition globalQuest = quest("global", RepeatPolicy.never(), global,
            crafting("craft", true, item("minecraft:iron_ingot", 1)));
        QuestDefinition disabled = quest("disabled", RepeatPolicy.never(), QuestBehavior.DEFAULT,
            crafting("craft", false, item("minecraft:iron_ingot", 1)));
        fixture.publish(personalQuest);
        fixture.publish(globalQuest);
        fixture.publish(disabled);
        fixture.statistics.put("minecraft:iron_ingot", Long.valueOf(1L));

        assertEquals(FactProcessingStatus.ACCEPTED, fixture.service()
            .detect(fixture.player, personalQuest.getId(), "craft", 100L).get(0).getStatus());
        assertTrue(fixture.service().detect(fixture.player, globalQuest.getId(), "craft", 100L).isEmpty());
        assertTrue(fixture.service().detect(fixture.player, disabled.getId(), "craft", 100L).isEmpty());
        assertEquals(1, fixture.runtime.facts.size());
    }

    @Test
    public void explicitDetectDoesNotUpdateUntilQuestPrerequisitesAreSatisfied() {
        Fixture fixture = new Fixture();
        UUID prerequisite = UUID.randomUUID();
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Locked", "", QuestLogic.AND,
            QuestLogic.AND, Collections.singleton(prerequisite), Collections.singletonList(
                crafting("craft", true, item("minecraft:iron_ingot", 1))), Collections.<RewardDefinition>emptyList());
        fixture.publish(definition);
        fixture.statistics.put("minecraft:iron_ingot", Long.valueOf(1L));

        assertTrue(fixture.service().detect(fixture.player, definition.getId(), "craft", 100L).isEmpty());
        assertTrue(fixture.runtime.facts.isEmpty());
        fixture.completed.add(prerequisite);
        assertEquals(FactProcessingStatus.ACCEPTED,
            fixture.service().detect(fixture.player, definition.getId(), "craft", 101L).get(0).getStatus());
        assertTrue(fixture.runtime.progress(fixture.player, definition.getId()).getTasks().get("craft").isComplete());
    }

    @Test
    public void automaticBqDetectReevaluatesCompletedLockedTaskAfterPrerequisiteUnlocks() {
        Fixture fixture = new Fixture();
        UUID prerequisite = UUID.randomUUID();
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "1");
        parameters.put("item.0.registryName", "minecraft:iron_ingot");
        parameters.put("item.0.meta", "0");
        parameters.put("item.0.amount", "1");
        parameters.put("progressMode", "item-vector");
        parameters.put("allowCraft", "true");
        parameters.put("allowSmelt", "true");
        parameters.put("allowAnvil", "false");
        parameters.put("allowCraftedFromStatistics", "true");
        parameters.put("bq.raw", "{}");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false, parameters);
        RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item",
            Collections.<String, String>emptyMap());
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Locked BQ Crafting", "",
            QuestLogic.AND, QuestLogic.AND, Collections.singleton(prerequisite), Collections.singletonList(task),
            Collections.singletonList(reward));
        fixture.publish(definition);
        fixture.statistics.put("minecraft:iron_ingot", Long.valueOf(1L));
        TaskProgress alreadyDetected = new TaskProgress("craft", Collections.singletonList(Long.valueOf(1L)),
            Collections.singletonList(Long.valueOf(1L)), true);
        fixture.runtime.saveProgress(new QuestProgressSnapshot(ParticipantId.player(fixture.player),
            definition.getId(), definition.getVersion(), QuestStatus.LOCKED,
            Collections.singletonMap("craft", alreadyDetected), 0L, 0));

        assertTrue("periodic BQ Detect must not complete while this player is still locked",
            fixture.service().detectAutomatically(fixture.player, definition.getId(), "craft", 100L).isEmpty());
        assertTrue(fixture.runtime.entitlements.isEmpty());

        fixture.completed.add(prerequisite);
        Map<String, String> probeAttributes = new LinkedHashMap<String, String>();
        probeAttributes.put("registryName", "minecraft:iron_ingot");
        probeAttributes.put("channel", "craft-statistics");
        probeAttributes.put("targetQuestId", definition.getId().toString());
        probeAttributes.put("targetQuestVersion", "1");
        probeAttributes.put("targetTaskKey", "craft");
        probeAttributes.put(BqPartyCraftingStatisticsScope.PERSONAL_ONLINE_SNAPSHOT, "true");
        probeAttributes.put(BqPartyCraftingStatisticsScope.AUTOMATIC_TASK_SNAPSHOT, "true");
        assertEquals("the now-unlocked player has a targeted imported-BQ request", 1,
            fixture.planner.requestsFor(new GameplayFact("s2", "unlock-probe", fixture.player,
                "galaxy:item_produced", 160L, probeAttributes)).size());
        List<FactProcessingResult> results = fixture.service().detectAutomatically(fixture.player,
            definition.getId(), "craft", 160L);

        assertEquals(1, results.size());
        assertEquals(FactProcessingStatus.ACCEPTED, results.get(0).getStatus());
        assertEquals(QuestStatus.COMPLETED, fixture.runtime.progress(fixture.player, definition.getId()).getStatus());
        assertEquals("the unlocked reevaluation produces its reward once", 1, fixture.runtime.entitlements.size());
        assertTrue("the later unlocked reevaluation uses its own stable idempotency key",
            fixture.runtime.facts.iterator().next().startsWith("s2:craft-stat-auto-unlock:"));
        assertTrue(fixture.service().detectAutomatically(fixture.player, definition.getId(), "craft", 220L).isEmpty());
        assertEquals(1, fixture.runtime.facts.size());
    }

    private static QuestDefinition quest(String name, RepeatPolicy repeat, QuestBehavior behavior,
        TaskDefinition... tasks) {
        return new QuestDefinition(UUID.randomUUID(), 1, name, "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Arrays.asList(tasks), Collections.singletonList(new RewardDefinition(
                "reward", "bq_standard:item", Collections.<String, String>emptyMap())), repeat, behavior);
    }

    private static TaskDefinition crafting(String key, boolean allowStatistics, Map<String, String> items) {
        return crafting(key, allowStatistics, Collections.singletonList(items));
    }

    private static TaskDefinition crafting(String key, boolean allowStatistics, Map<String, String> first,
        Map<String, String> second) {
        return crafting(key, allowStatistics, Arrays.asList(first, second));
    }

    private static TaskDefinition crafting(String key, boolean allowStatistics, List<Map<String, String>> items) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", Integer.toString(items.size()));
        for (int index = 0; index < items.size(); index++) {
            for (Map.Entry<String, String> value : items.get(index).entrySet()) {
                if (value.getKey().startsWith("item.0.")) {
                    parameters.put("item." + index + "." + value.getKey().substring("item.0.".length()),
                        value.getValue());
                }
            }
        }
        parameters.put("progressMode", "item-vector");
        parameters.put("allowCraft", "true");
        parameters.put("allowSmelt", "true");
        parameters.put("allowAnvil", "false");
        parameters.put("allowCraftedFromStatistics", Boolean.toString(allowStatistics));
        return new TaskDefinition(key, "bq_standard:crafting", false, parameters);
    }

    private static Map<String, String> item(String registryName, long amount) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "1");
        parameters.put("item.0.registryName", registryName);
        parameters.put("item.0.meta", "0");
        parameters.put("item.0.amount", Long.toString(amount));
        return parameters;
    }

    private static final class Fixture {
        private final UUID player = UUID.randomUUID();
        private final List<StoredQuestDefinition> definitions = new ArrayList<StoredQuestDefinition>();
        private final Map<String, Long> statistics = new HashMap<String, Long>();
        private final Set<UUID> completed = new HashSet<UUID>();
        private final MemoryRuntimeRepository runtime = new MemoryRuntimeRepository();
        private final QuestEvaluationPlanner planner = new QuestEvaluationPlanner(new PublishedQuestCatalog() {
            @Override public List<StoredQuestDefinition> findAllPublished() {
                return Collections.unmodifiableList(definitions);
            }
        }, new QuestCompletionQuery() {
            @Override public Set<UUID> findCompletedQuestIds(ParticipantId participantId) {
                return Collections.unmodifiableSet(completed);
            }
        }, new ParticipantMembershipResolver() {
            @Override public List<ParticipantMembership> resolve(UUID playerId) {
                return Collections.singletonList(ParticipantMembership.player(playerId));
            }
        }, new ScopedQuestAssignmentPolicy());

        private Fixture() {
            TaskEvaluatorRegistry evaluators = BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch();
            runtime.runtime = new QuestRuntimeService(runtime, new DirectTransaction(),
                new QuestFactProjector(evaluators), new QuestEngine());
        }

        private void publish(QuestDefinition definition) {
            definitions.add(new StoredQuestDefinition(definition, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L));
        }

        private AuthenticatedCraftingStatisticsDetectionService service() {
            return new AuthenticatedCraftingStatisticsDetectionService(new DefinitionRepository(definitions), runtime,
                runtime.runtime, planner, (playerId, registryName) -> {
                    Long value = statistics.get(registryName);
                    return value == null ? OptionalLong.empty() : OptionalLong.of(value.longValue());
                }, "s2");
        }
    }

    private static final class MemoryRuntimeRepository implements QuestRuntimeRepository {
        private QuestRuntimeService runtime;
        private final Map<String, QuestProgressSnapshot> progress = new HashMap<String, QuestProgressSnapshot>();
        private final Set<String> facts = new HashSet<String>();
        private final Set<RewardEntitlement> entitlements = new HashSet<RewardEntitlement>();
        private boolean claimableRewards;
        private boolean pendingConsumption;
        private Long repeatAnchorMillis;
        private String key(ParticipantId participant, UUID questId, int version) {
            return participant.asStableKey() + ':' + questId + ':' + version;
        }
        private QuestProgressSnapshot progress(UUID player, UUID quest) {
            return progress.get(key(ParticipantId.player(player), quest, 1));
        }
        @Override public Optional<QuestProgressSnapshot> findProgress(ParticipantId participant, UUID questId,
            int version) { return Optional.ofNullable(progress.get(key(participant, questId, version))); }
        @Override public boolean hasPendingConsumptionSubmission(ParticipantId participant, UUID questId,
            int version, int cycle) { return pendingConsumption; }
        @Override public boolean hasClaimableRewardEntitlement(ParticipantId participant, UUID questId,
            int version, int cycle) { return claimableRewards; }
        @Override public long findRepeatCycleAnchorMillis(ParticipantId participant, UUID questId, int version,
            int cycle, long fallbackCompletedAt) {
            return repeatAnchorMillis == null ? fallbackCompletedAt : repeatAnchorMillis.longValue();
        }
        @Override public boolean recordFactIfAbsent(GameplayFact fact) { return facts.add(fact.getFactKey()); }
        @Override public void saveProgress(QuestProgressSnapshot value) {
            progress.put(key(value.getParticipantId(), value.getQuestId(), value.getDefinitionVersion()), value);
        }
        @Override public void insertEntitlementsIfAbsent(Set<RewardEntitlement> values) { entitlements.addAll(values); }
    }

    private static final class DefinitionRepository implements QuestDefinitionRepository {
        private final List<StoredQuestDefinition> values;
        private DefinitionRepository(List<StoredQuestDefinition> values) { this.values = values; }
        @Override public Optional<StoredQuestDefinition> findPublished(UUID questId) {
            for (StoredQuestDefinition value : values) if (value.getDefinition().getId().equals(questId)) {
                return Optional.of(value);
            }
            return Optional.empty();
        }
        @Override public List<StoredQuestDefinition> findAllPublished() { return values; }
        @Override public StoredQuestDefinition createDraft(QuestDefinition definition) { throw new UnsupportedOperationException(); }
        @Override public StoredQuestDefinition updateDraft(QuestDefinition definition, String hash) { throw new UnsupportedOperationException(); }
        @Override public boolean publish(UUID id, int version, String hash, long at) { throw new UnsupportedOperationException(); }
        @Override public boolean retire(UUID id, int version) { throw new UnsupportedOperationException(); }
        @Override public Optional<StoredQuestDefinition> find(UUID id, int version) { return Optional.empty(); }
    }

    private static final class DirectTransaction implements QuestTransaction {
        @Override public <T> T inTransaction(Work<T> work) { return work.execute(); }
    }
}

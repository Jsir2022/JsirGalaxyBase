package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.InventoryObservationAttributeCodec;
import com.jsirgalaxybase.quest.core.InventoryObservationEntry;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMembership;
import com.jsirgalaxybase.quest.core.ParticipantMembershipResolver;
import com.jsirgalaxybase.quest.core.ParticipantType;
import com.jsirgalaxybase.quest.core.QuestClock;
import com.jsirgalaxybase.quest.core.QuestObservationPlan;
import com.jsirgalaxybase.quest.core.QuestObservationPlanProvider;
import com.jsirgalaxybase.quest.core.QuestTransaction;
import com.jsirgalaxybase.quest.postgres.QuestInventorySnapshotLease;

public class BqPartyInventorySnapshotFactSinkTest {

    @Test
    public void claimsSessionAndPublishesAggregateFromFreshPartyMemberLeases() {
        Fixture fixture = new Fixture(true);
        UUID sourceSession = UUID.randomUUID();
        UUID peerSession = UUID.randomUUID();
        fixture.store.leases.put(fixture.peer, lease(fixture.peer, "lobby", peerSession, 3L));
        GameplayFact sourceInventory = inventory(fixture.source, sourceSession, 4L);

        assertTrue(fixture.sink.beginInventorySession(sourceInventory));

        assertEquals(1, fixture.store.claims);
        assertEquals(1, fixture.store.expiredCleanups);
        assertFalse(fixture.store.leases.get(fixture.source).getAttributes().containsKey("inventory.sessionId"));
        assertEquals(2, fixture.delegate.facts.size());
        assertSame(sourceInventory, fixture.delegate.facts.get(0));

        GameplayFact aggregate = fixture.delegate.facts.get(1);
        assertEquals("true", aggregate.getAttributes().get("inventory.bqPartySnapshot"));
        assertEquals(fixture.party.getId().toString(), aggregate.getAttributes().get("inventory.bqPartyId"));
        assertEquals("1", aggregate.getAttributes().get("inventory.count"));
        assertEquals("minecraft:iron_ingot", aggregate.getAttributes().get("inventory.0.registryName"));
        assertEquals("7", aggregate.getAttributes().get("inventory.0.amount"));
    }

    @Test
    public void rejectsInventoryTickFromReplacedLoginSessionBeforePublishingFacts() {
        Fixture fixture = new Fixture(true);
        UUID currentSession = UUID.randomUUID();
        UUID staleSession = UUID.randomUUID();
        fixture.store.leases.put(fixture.source, lease(fixture.source, "s2", currentSession, 2L));

        fixture.sink.accept(inventory(fixture.source, staleSession, 9L));

        assertEquals(0, fixture.store.claims);
        assertEquals(0, fixture.store.expiredCleanups);
        assertTrue(fixture.delegate.facts.isEmpty());
        assertEquals(currentSession, fixture.store.leases.get(fixture.source).getSessionId());
    }

    @Test
    public void soloInventorySessionPublishesOnlyItsPersonalFactWithoutCreatingPartyLease() {
        Fixture fixture = new Fixture(false);
        GameplayFact sourceInventory = inventory(fixture.source, UUID.randomUUID(), 4L);

        assertTrue(fixture.sink.beginInventorySession(sourceInventory));

        assertEquals(0, fixture.store.claims);
        assertEquals(1, fixture.delegate.facts.size());
        assertSame(sourceInventory, fixture.delegate.facts.get(0));
    }

    private static GameplayFact inventory(UUID player, UUID session, long amount) {
        Map<String, String> attributes = new LinkedHashMap<String, String>(
            new InventoryObservationAttributeCodec().encode(Collections.singletonList(
                new InventoryObservationEntry("minecraft:iron_ingot", 0, amount, "ingotIron", "", "", false))));
        attributes.put("inventory.sessionId", session.toString());
        return new GameplayFact("s2", "inventory-" + session, player, "galaxy:inventory_observed", 1000L,
            attributes);
    }

    private static QuestInventorySnapshotLease lease(UUID player, String server, UUID session, long amount) {
        Map<String, String> attributes = new InventoryObservationAttributeCodec().encode(Collections.singletonList(
            new InventoryObservationEntry("minecraft:iron_ingot", 0, amount, "ingotIron", "", "", false)));
        return new QuestInventorySnapshotLease(player, server, session, attributes);
    }

    private static final class Fixture {
        private final UUID source = UUID.randomUUID();
        private final UUID peer = UUID.randomUUID();
        private final ParticipantId party = ParticipantId.party(UUID.randomUUID());
        private final ParticipantMembership partyMembership = new ParticipantMembership(party,
            new LinkedHashSet<UUID>(Arrays.asList(source, peer)));
        private final FakeStore store = new FakeStore();
        private final RecordingSink delegate = new RecordingSink();
        private final BqPartyInventorySnapshotFactSink sink;

        private Fixture(final boolean hasParty) {
            ParticipantMembershipResolver memberships = new ParticipantMembershipResolver() {
                @Override public List<ParticipantMembership> resolve(UUID playerId) {
                    if (!source.equals(playerId) && !peer.equals(playerId)) return Collections.emptyList();
                    if (!hasParty) return Collections.singletonList(ParticipantMembership.player(playerId));
                    return Arrays.asList(ParticipantMembership.player(playerId), partyMembership);
                }
                @Override public ParticipantMembership resolveParticipant(ParticipantId participantId) {
                    return hasParty && party.equals(participantId) ? partyMembership : null;
                }
            };
            QuestTransaction transaction = new QuestTransaction() {
                @Override public <T> T inTransaction(Work<T> work) { return work.execute(); }
            };
            QuestObservationPlanProvider plans = new QuestObservationPlanProvider() {
                @Override public QuestObservationPlan current() {
                    return new QuestObservationPlan(true, true, false, false, -1, false,
                        Collections.emptyList(), Collections.emptyList());
                }
            };
            QuestClock clock = new QuestClock() {
                @Override public long currentTimeMillis() { return 1000L; }
            };
            sink = new BqPartyInventorySnapshotFactSink(delegate, transaction, store, memberships,
                new MinecraftQuestFactFactory("s2"), plans, clock);
        }
    }

    private static final class RecordingSink implements QuestFactSink {
        private final List<GameplayFact> facts = new ArrayList<GameplayFact>();
        @Override public void accept(GameplayFact fact) { facts.add(fact); }
    }

    private static final class FakeStore implements BqPartyInventorySnapshotStore {
        private final Map<UUID, QuestInventorySnapshotLease> leases =
            new LinkedHashMap<UUID, QuestInventorySnapshotLease>();
        private int claims;
        private int expiredCleanups;

        @Override public void claimSession(UUID playerId, String sourceServer, UUID sessionId,
            Map<String, String> attributes, long leaseMillis) {
            claims++;
            leases.put(playerId, new QuestInventorySnapshotLease(playerId, sourceServer, sessionId, attributes));
        }

        @Override public boolean publish(UUID playerId, String sourceServer, UUID sessionId,
            Map<String, String> attributes, long leaseMillis) {
            QuestInventorySnapshotLease current = leases.get(playerId);
            if (current != null && !current.getSessionId().equals(sessionId)) return false;
            leases.put(playerId, new QuestInventorySnapshotLease(playerId, sourceServer, sessionId, attributes));
            return true;
        }

        @Override public boolean lockActiveParty(ParticipantId target) {
            return target != null && target.getType() == ParticipantType.PARTY;
        }

        @Override public Map<UUID, QuestInventorySnapshotLease> findActive(List<UUID> playerIds) {
            Map<UUID, QuestInventorySnapshotLease> result = new LinkedHashMap<UUID, QuestInventorySnapshotLease>();
            for (UUID playerId : playerIds) if (leases.containsKey(playerId)) result.put(playerId, leases.get(playerId));
            return result;
        }

        @Override public int deleteSession(UUID playerId, String sourceServer, UUID sessionId) {
            QuestInventorySnapshotLease lease = leases.get(playerId);
            if (lease == null || !lease.getSourceServer().equals(sourceServer)
                || !lease.getSessionId().equals(sessionId)) return 0;
            leases.remove(playerId);
            return 1;
        }

        @Override public int deleteExpired() {
            expiredCleanups++;
            return 0;
        }
    }
}

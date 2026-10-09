package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.quest.core.BqPartyInventorySnapshotAggregator;
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
import com.jsirgalaxybase.quest.postgres.JdbcBqPartyInventorySnapshotRepository;
import com.jsirgalaxybase.quest.postgres.QuestInventorySnapshotLease;

/** Converts fresh, server-owned player snapshots into bounded BQ PARTY retrieval facts. */
public final class BqPartyInventorySnapshotFactSink implements QuestFactSink {
    public static final long INVENTORY_LEASE_MILLIS = 15000L;
    private static final long CLEANUP_INTERVAL_MILLIS = 60000L;
    private static final int MAX_PARTY_MEMBERS = 512;

    private final QuestFactSink delegate;
    private final QuestTransaction transaction;
    private final BqPartyInventorySnapshotStore snapshots;
    private final ParticipantMembershipResolver memberships;
    private final MinecraftQuestFactFactory facts;
    private final QuestObservationPlanProvider observationPlans;
    private final QuestClock clock;
    private final AtomicLong lastCleanupAt = new AtomicLong(Long.MIN_VALUE);

    public BqPartyInventorySnapshotFactSink(QuestFactSink delegate, QuestTransaction transaction,
        JdbcBqPartyInventorySnapshotRepository snapshots, ParticipantMembershipResolver memberships,
        MinecraftQuestFactFactory facts, QuestObservationPlanProvider observationPlans, QuestClock clock) {
        this(delegate, transaction, adapt(snapshots), memberships, facts, observationPlans, clock);
    }

    BqPartyInventorySnapshotFactSink(QuestFactSink delegate, QuestTransaction transaction,
        BqPartyInventorySnapshotStore snapshots, ParticipantMembershipResolver memberships,
        MinecraftQuestFactFactory facts, QuestObservationPlanProvider observationPlans, QuestClock clock) {
        if (delegate == null || transaction == null || snapshots == null || memberships == null || facts == null
            || observationPlans == null || clock == null) throw new IllegalArgumentException("dependencies are required");
        this.delegate = delegate;
        this.transaction = transaction;
        this.snapshots = snapshots;
        this.memberships = memberships;
        this.facts = facts;
        this.observationPlans = observationPlans;
        this.clock = clock;
    }

    private static BqPartyInventorySnapshotStore adapt(final JdbcBqPartyInventorySnapshotRepository repository) {
        if (repository == null) throw new IllegalArgumentException("dependencies are required");
        return new BqPartyInventorySnapshotStore() {
            @Override public void claimSession(UUID playerId, String sourceServer, UUID sessionId,
                Map<String, String> attributes, long leaseMillis) {
                repository.claimSession(playerId, sourceServer, sessionId, attributes, leaseMillis);
            }
            @Override public boolean publish(UUID playerId, String sourceServer, UUID sessionId,
                Map<String, String> attributes, long leaseMillis) {
                return repository.publish(playerId, sourceServer, sessionId, attributes, leaseMillis);
            }
            @Override public boolean lockActiveParty(ParticipantId party) {
                return repository.lockActiveParty(party);
            }
            @Override public Map<UUID, QuestInventorySnapshotLease> findActive(List<UUID> playerIds) {
                return repository.findActive(playerIds);
            }
            @Override public int deleteSession(UUID playerId, String sourceServer, UUID sessionId) {
                return repository.deleteSession(playerId, sourceServer, sessionId);
            }
            @Override public int deleteExpired() { return repository.deleteExpired(); }
        };
    }

    @Override public void accept(final GameplayFact fact) {
        if (!isPartyInventoryObservation(fact) || !observesBqPartyInventory()) {
            delegate.accept(fact);
            return;
        }
        final UUID sessionId = sessionId(fact);
        if (sessionId == null) {
            delegate.accept(fact);
            return;
        }
        transaction.inTransaction(() -> process(fact, sessionId, false));
    }

    @Override public boolean beginInventorySession(final GameplayFact fact) {
        if (!isPartyInventoryObservation(fact) || !observesBqPartyInventory()) {
            delegate.accept(fact);
            return true;
        }
        final UUID sessionId = sessionId(fact);
        if (sessionId == null) throw new IllegalArgumentException("new BQ inventory session has no session token");
        return transaction.inTransaction(() -> process(fact, sessionId, true));
    }

    @Override public void endInventorySession(final UUID playerId, final UUID sessionId) {
        if (playerId == null || sessionId == null) return;
        transaction.inTransaction(() -> snapshots.deleteSession(playerId, facts.getSourceServer(), sessionId));
    }

    @Override public void reconcileLockedProgress(UUID playerId, long now) {
        delegate.reconcileLockedProgress(playerId, now);
    }

    private boolean process(GameplayFact fact, UUID sessionId, boolean claimSession) {
        List<ParticipantId> parties = partyIds(memberships.resolve(fact.getPlayerId()));
        List<ParticipantId> locked = new ArrayList<ParticipantId>();
        for (ParticipantId party : parties) if (snapshots.lockActiveParty(party)) locked.add(party);

        List<ParticipantId> currentParties = partyIds(memberships.resolve(fact.getPlayerId()));
        currentParties.retainAll(locked);
        Map<String, String> leaseAttributes = new LinkedHashMap<String, String>(fact.getAttributes());
        leaseAttributes.remove("inventory.sessionId");
        if (!currentParties.isEmpty()) {
            if (claimSession) {
                snapshots.claimSession(fact.getPlayerId(), fact.getSourceServer(), sessionId, leaseAttributes,
                    INVENTORY_LEASE_MILLIS);
            } else if (!snapshots.publish(fact.getPlayerId(), fact.getSourceServer(), sessionId,
                leaseAttributes, INVENTORY_LEASE_MILLIS)) {
                return false;
            }
        }

        cleanupExpiredIfDue();
        delegate.accept(fact);
        for (ParticipantId party : currentParties) publishPartyAggregate(fact, party, sessionId);
        return true;
    }

    private void publishPartyAggregate(GameplayFact trigger, ParticipantId party, UUID triggerSession) {
        ParticipantMembership membership = memberships.resolveParticipant(party);
        if (membership == null || !membership.contains(trigger.getPlayerId())) return;
        if (membership.getPlayerIds().size() > MAX_PARTY_MEMBERS) {
            GalaxyBase.LOG.warn("Skipping BQ PARTY inventory observation for oversized party {}", party.getId());
            return;
        }
        List<UUID> memberOrder = new ArrayList<UUID>(membership.getPlayerIds());
        Map<UUID, QuestInventorySnapshotLease> active = snapshots.findActive(memberOrder);
        QuestInventorySnapshotLease triggerLease = active.get(trigger.getPlayerId());
        if (triggerLease == null || !triggerSession.equals(triggerLease.getSessionId())
            || !trigger.getSourceServer().equals(triggerLease.getSourceServer())) return;

        Map<UUID, List<InventoryObservationEntry>> decoded = new HashMap<UUID, List<InventoryObservationEntry>>();
        InventoryObservationAttributeCodec codec = new InventoryObservationAttributeCodec();
        for (UUID memberId : memberOrder) {
            QuestInventorySnapshotLease lease = active.get(memberId);
            if (lease != null) decoded.put(memberId, codec.decode(lease.getAttributes()));
        }
        List<InventoryObservationEntry> aggregate = new BqPartyInventorySnapshotAggregator()
            .aggregate(memberOrder, decoded);
        delegate.accept(facts.bqPartyInventoryObserved(trigger, party.getId(), aggregate));
    }

    private void cleanupExpiredIfDue() {
        long now = clock.currentTimeMillis();
        long previous = lastCleanupAt.get();
        if (previous != Long.MIN_VALUE && now - previous < CLEANUP_INTERVAL_MILLIS) return;
        if (lastCleanupAt.compareAndSet(previous, now)) snapshots.deleteExpired();
    }

    private boolean observesBqPartyInventory() {
        QuestObservationPlan plan = observationPlans.current();
        return plan != null && plan.observesBqPartyInventory();
    }

    private static boolean isPartyInventoryObservation(GameplayFact fact) {
        return fact != null && "galaxy:inventory_observed".equals(fact.getTypeId());
    }

    private static UUID sessionId(GameplayFact fact) {
        String value = fact.getAttributes().get("inventory.sessionId");
        if (value == null || value.trim().isEmpty()) return null;
        try { return UUID.fromString(value); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("invalid inventory session id", invalid); }
    }

    private static List<ParticipantId> partyIds(List<ParticipantMembership> resolved) {
        if (resolved == null || resolved.isEmpty()) return Collections.emptyList();
        List<ParticipantId> result = new ArrayList<ParticipantId>();
        for (ParticipantMembership membership : resolved) {
            if (membership != null && membership.getParticipantId().getType() == ParticipantType.PARTY) {
                result.add(membership.getParticipantId());
            }
        }
        Collections.sort(result, new Comparator<ParticipantId>() {
            @Override public int compare(ParticipantId left, ParticipantId right) {
                return left.getId().compareTo(right.getId());
            }
        });
        return result;
    }
}

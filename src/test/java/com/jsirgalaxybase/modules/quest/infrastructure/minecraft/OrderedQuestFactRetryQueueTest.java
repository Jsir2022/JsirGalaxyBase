package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestEvaluationRequest;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMembership;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;

public class OrderedQuestFactRetryQueueTest {
    @Test
    public void failedFactKeepsItsKeyAndBlocksLaterFactsUntilFifoRetrySucceeds() {
        OrderedQuestFactRetryQueue queue = new OrderedQuestFactRetryQueue();
        RecordingSink sink = new RecordingSink();
        GameplayFact first = fact("first");
        GameplayFact second = fact("second");
        sink.failNext = true;

        OrderedQuestFactRetryQueue.SubmissionResult submitted = queue.submit(first, sink);
        assertNotNull(submitted.getFailure());
        assertFalse(submitted.isRejected());
        assertEquals(1, queue.pendingCount());
        assertEquals("server:first", first.getFactKey());

        queue.submit(second, sink);
        assertTrue(sink.accepted.isEmpty());
        assertEquals(2, queue.pendingCount());
        OrderedQuestFactRetryQueue.FlushResult flushed = queue.retry(sink, 2);
        assertNull(flushed.getFailure());
        assertEquals(2, flushed.getAccepted());
        assertEquals(list("server:first", "server:second"), sink.accepted);
        assertTrue(queue.isEmpty());
        assertEquals(0L, queue.retainedBytes());
    }

    @Test
    public void retryUsesTheEventTimeQuestAndPartyRosterSnapshotAfterMembershipChanges() {
        OrderedQuestFactRetryQueue queue = new OrderedQuestFactRetryQueue();
        RecordingSink sink = new RecordingSink();
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000012");
        UUID laterMember = UUID.fromString("00000000-0000-0000-0000-000000000013");
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Frozen", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(),
            Collections.singletonList(new TaskDefinition("craft", "bq_standard:crafting", false,
                Collections.<String, String>emptyMap())), Collections.emptyList(), RepeatPolicy.never());
        QuestEvaluationRequest eventRequest = new QuestEvaluationRequest(definition,
            ParticipantMembership.player(first), new ParticipantMembership(
                new ParticipantId(com.jsirgalaxybase.quest.core.ParticipantType.PARTY, UUID.randomUUID()),
                new java.util.LinkedHashSet<UUID>(java.util.Arrays.asList(first, second))),
            Collections.<UUID>emptySet());
        List<QuestEvaluationRequest> mutableCapture = new ArrayList<QuestEvaluationRequest>(
            Collections.singletonList(eventRequest));
        sink.failNextSnapshot = true;

        OrderedQuestFactRetryQueue.SubmissionResult firstTry = queue.submit(craftingFact("craft"), sink,
            mutableCapture);
        mutableCapture.clear();
        assertNotNull(firstTry.getFailure());
        assertEquals(1, queue.pendingCount());

        // The live Party has changed before retry, but the queued request still owns the original roster/version.
        ParticipantMembership changedParty = new ParticipantMembership(eventRequest.getFactMembership()
            .getParticipantId(), Collections.singleton(laterMember));
        assertTrue(changedParty.contains(laterMember));
        assertEquals(2, eventRequest.getFactMembership().getPlayerIds().size());
        OrderedQuestFactRetryQueue.FlushResult retried = queue.retry(sink, 1);

        assertNull(retried.getFailure());
        assertEquals(1, sink.frozenRequests.size());
        assertSame(eventRequest, sink.frozenRequests.get(0).get(0));
        assertEquals(2, sink.frozenRequests.get(0).get(0).getFactMembership().getPlayerIds().size());
        assertEquals(1, sink.frozenRequests.get(0).get(0).getDefinition().getVersion());
    }

    @Test
    public void snapshotAwareDeliveryCannotFallBackToCurrentMembershipResolution() {
        final List<String> accepted = new ArrayList<String>();
        QuestFactSink legacySink = new QuestFactSink() {
            @Override public void accept(GameplayFact fact) { accepted.add(fact.getFactKey()); }
        };
        boolean rejected = false;
        try {
            legacySink.accept(fact("snapshot-required"), Collections.<QuestEvaluationRequest>emptyList());
        } catch (UnsupportedOperationException expected) {
            rejected = true;
        }

        assertTrue("a sink must explicitly implement event-time request handling", rejected);
        assertTrue("the legacy acceptance method must not receive the event", accepted.isEmpty());
    }

    @Test
    public void failedInventorySessionRetriesTheOriginalOperationBeforeLaterFacts() {
        OrderedQuestFactRetryQueue queue = new OrderedQuestFactRetryQueue();
        RecordingSink sink = new RecordingSink();
        GameplayFact login = fact("login-session");
        sink.failBeginNext = true;

        OrderedQuestFactRetryQueue.SubmissionResult submitted = queue.submitInventorySession(login, sink);
        assertNotNull(submitted.getFailure());
        queue.submit(fact("later"), sink);

        OrderedQuestFactRetryQueue.FlushResult flushed = queue.retry(sink, 2);
        assertNull(flushed.getFailure());
        assertEquals(list("server:login-session"), sink.begunSessions);
        assertEquals(list("server:later"), sink.accepted);
        assertEquals(2, flushed.getAccepted());
    }

    @Test
    public void transientFailureLeavesHeadQueuedAndRetryBudgetBoundsWork() {
        OrderedQuestFactRetryQueue queue = new OrderedQuestFactRetryQueue();
        RecordingSink sink = new RecordingSink();
        sink.failNext = true;
        queue.submit(fact("one"), sink);
        queue.submit(fact("two"), sink);
        queue.submit(fact("three"), sink);
        sink.failNext = true;

        OrderedQuestFactRetryQueue.FlushResult failure = queue.retry(sink, 2);
        assertNotNull(failure.getFailure());
        assertEquals(0, failure.getAccepted());
        assertEquals(3, queue.pendingCount());

        OrderedQuestFactRetryQueue.FlushResult bounded = queue.retry(sink, 2);
        assertEquals(2, bounded.getAccepted());
        assertEquals(1, queue.pendingCount());
    }

    @Test
    public void queueRejectsNewestFactAtCapacityAndReportsOverflow() {
        OrderedQuestFactRetryQueue queue = new OrderedQuestFactRetryQueue();
        RecordingSink sink = new RecordingSink();
        sink.failNext = true;
        queue.submit(fact("head"), sink);
        for (int i = 1; i < OrderedQuestFactRetryQueue.CAPACITY; i++) queue.submit(fact("queued-" + i), sink);

        OrderedQuestFactRetryQueue.SubmissionResult overflow = queue.submit(fact("overflow"), sink);
        assertTrue(overflow.isRejected());
        assertEquals(OrderedQuestFactRetryQueue.CAPACITY, queue.pendingCount());
        assertEquals(1L, queue.drainRejectedCount());
        assertEquals(0L, queue.drainRejectedCount());
    }

    @Test
    public void queueAlsoBoundsEstimatedPayloadMemory() {
        OrderedQuestFactRetryQueue queue = new OrderedQuestFactRetryQueue();
        RecordingSink sink = new RecordingSink();
        sink.failNext = true;
        GameplayFact oversized = new GameplayFact("server", "large", UUID.fromString(
            "00000000-0000-0000-0000-000000000001"), "test:fact", 1L,
            Collections.singletonMap("payload", new String(new char[4_200_000])));

        OrderedQuestFactRetryQueue.SubmissionResult rejected = queue.submit(oversized, sink);

        assertTrue(rejected.isRejected());
        assertEquals(0, queue.pendingCount());
        assertEquals(0L, queue.retainedBytes());
        assertEquals(1L, queue.drainRejectedCount());
    }

    @Test
    public void queueAlsoBoundsTheFrozenQuestSnapshotMemory() {
        OrderedQuestFactRetryQueue queue = new OrderedQuestFactRetryQueue();
        RecordingSink sink = new RecordingSink();
        sink.failNextSnapshot = true;
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000021");
        QuestDefinition oversizedDefinition = new QuestDefinition(UUID.randomUUID(), 1, "Large snapshot",
            new String(new char[4_200_000]), QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(),
            Collections.singletonList(new TaskDefinition("craft", "bq_standard:crafting", false,
                Collections.<String, String>emptyMap())), Collections.emptyList(), RepeatPolicy.never());
        QuestEvaluationRequest request = new QuestEvaluationRequest(oversizedDefinition,
            ParticipantMembership.player(player), Collections.<UUID>emptySet());

        OrderedQuestFactRetryQueue.SubmissionResult rejected = queue.submit(craftingFact("large-snapshot"), sink,
            Collections.singletonList(request));

        assertTrue(rejected.isRejected());
        assertEquals(0, queue.pendingCount());
        assertEquals(0L, queue.retainedBytes());
        assertEquals(1L, queue.drainRejectedCount());
    }

    @Test
    public void clearingAtServerShutdownDropsVolatileFactsAndOverflowState() {
        OrderedQuestFactRetryQueue queue = new OrderedQuestFactRetryQueue();
        RecordingSink sink = new RecordingSink();
        sink.failNext = true;
        queue.submit(fact("pending"), sink);

        queue.clear();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.pendingCount());
        assertEquals(0L, queue.retainedBytes());
        assertEquals(0L, queue.drainRejectedCount());
    }

    private static GameplayFact fact(String id) {
        return new GameplayFact("server", id, UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "test:fact", 1L, Collections.<String, String>emptyMap());
    }

    private static GameplayFact craftingFact(String id) {
        return new GameplayFact("server", id, UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "galaxy:item_produced", 1L, Collections.singletonMap("channel", "craft"));
    }

    private static List<String> list(String... values) {
        List<String> result = new ArrayList<String>();
        Collections.addAll(result, values);
        return result;
    }

    private static final class RecordingSink implements QuestFactSink {
        private final List<String> accepted = new ArrayList<String>();
        private final List<String> begunSessions = new ArrayList<String>();
        private final List<List<QuestEvaluationRequest>> frozenRequests =
            new ArrayList<List<QuestEvaluationRequest>>();
        private boolean failNext;
        private boolean failNextSnapshot;
        private boolean failBeginNext;
        @Override public void accept(GameplayFact fact) {
            if (failNext) { failNext = false; throw new IllegalStateException("simulated database failure"); }
            accepted.add(fact.getFactKey());
        }
        @Override public void accept(GameplayFact fact, List<QuestEvaluationRequest> eventTimeRequests) {
            if (failNextSnapshot) {
                failNextSnapshot = false;
                throw new IllegalStateException("simulated event-time snapshot persistence failure");
            }
            accepted.add(fact.getFactKey());
            frozenRequests.add(eventTimeRequests);
        }
        @Override public boolean beginInventorySession(GameplayFact fact) {
            if (failBeginNext) {
                failBeginNext = false;
                throw new IllegalStateException("simulated login snapshot database failure");
            }
            begunSessions.add(fact.getFactKey());
            return true;
        }
    }
}

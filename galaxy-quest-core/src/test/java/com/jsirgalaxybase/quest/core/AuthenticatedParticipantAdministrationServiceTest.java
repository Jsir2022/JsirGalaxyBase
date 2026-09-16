package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

public class AuthenticatedParticipantAdministrationServiceTest {
    private final UUID actor=UUID.randomUUID();
    private final ParticipantId team=new ParticipantId(ParticipantType.TEAM,UUID.randomUUID());

    @Test public void identityAndRevisionAreForwardedInsideTransaction(){
        RecordingRepository repository=new RecordingRepository();CountingTransaction transaction=new CountingTransaction();
        AuthenticatedParticipantAdministrationService service=new AuthenticatedParticipantAdministrationService(repository,transaction);
        assertEquals(ParticipantMutationStatus.APPLIED,service.create(team," Builders ",actor,10L));
        assertEquals("Builders",repository.name);assertEquals(actor,repository.actor);assertEquals(1,transaction.calls);
        UUID member=UUID.randomUUID();assertEquals(ParticipantMutationStatus.APPLIED,
            service.addMember(team,actor,member,ParticipantMemberRole.ADMIN,4L,11L));
        assertEquals(member,repository.member);assertEquals(4L,repository.revision);assertEquals(2,transaction.calls);
        assertEquals(ParticipantMutationStatus.APPLIED,service.removeMember(team,actor,member,5L,12L));
        assertEquals(5L,repository.revision);assertEquals(3,transaction.calls);
        assertEquals(ParticipantMutationStatus.APPLIED,service.transferOwnership(team,actor,member,6L,13L));
        assertEquals(member,repository.member);assertEquals(6L,repository.revision);assertEquals(4,transaction.calls);
    }

    @Test public void rejectsOwnerEscalationAndInvalidNamesBeforeRepository(){
        RecordingRepository repository=new RecordingRepository();AuthenticatedParticipantAdministrationService service=
            new AuthenticatedParticipantAdministrationService(repository,new CountingTransaction());
        assertEquals(ParticipantMutationStatus.INVALID,service.create(team," ",actor,1L));
        assertEquals(ParticipantMutationStatus.INVALID,service.addMember(team,actor,UUID.randomUUID(),ParticipantMemberRole.OWNER,0L,1L));
        assertEquals(ParticipantMutationStatus.INVALID,service.transferOwnership(team,actor,actor,0L,1L));
        assertEquals(0,repository.calls);
    }

    private static final class RecordingRepository implements ParticipantAdministrationRepository{
        int calls;String name;UUID actor,member;long revision;
        public ParticipantMutationStatus create(ParticipantId participant,String displayName,UUID owner,long changedAt){calls++;name=displayName;actor=owner;return ParticipantMutationStatus.APPLIED;}
        public ParticipantMutationStatus addMember(ParticipantId participant,UUID authenticatedActor,UUID playerId,ParticipantMemberRole role,long expectedRevision,long changedAt){calls++;actor=authenticatedActor;member=playerId;revision=expectedRevision;return ParticipantMutationStatus.APPLIED;}
        public ParticipantMutationStatus removeMember(ParticipantId participant,UUID authenticatedActor,UUID playerId,long expectedRevision,long changedAt){calls++;actor=authenticatedActor;member=playerId;revision=expectedRevision;return ParticipantMutationStatus.APPLIED;}
        public ParticipantMutationStatus transferOwnership(ParticipantId participant,UUID authenticatedOwner,UUID newOwner,long expectedRevision,long changedAt){calls++;actor=authenticatedOwner;member=newOwner;revision=expectedRevision;return ParticipantMutationStatus.APPLIED;}
    }
    private static final class CountingTransaction implements QuestTransaction{int calls;public <T>T inTransaction(Work<T> work){calls++;return work.execute();}}
}

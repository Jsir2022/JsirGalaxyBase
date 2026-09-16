package com.jsirgalaxybase.quest.core;

import java.util.Objects;

/** Authenticated player's bounded view of one active cross-server quest subject. */
public final class ParticipantDirectoryEntry {
    private final ParticipantId participantId;
    private final String displayName;
    private final ParticipantMemberRole role;
    private final long revision;
    private final int memberCount;

    public ParticipantDirectoryEntry(ParticipantId participantId,String displayName,ParticipantMemberRole role,
        long revision,int memberCount){this.participantId=Objects.requireNonNull(participantId,"participantId");
        this.displayName=displayName==null?"":displayName;this.role=Objects.requireNonNull(role,"role");
        if(revision<0L||memberCount<1)throw new IllegalArgumentException("revision and memberCount are invalid");
        this.revision=revision;this.memberCount=memberCount;}
    public ParticipantId getParticipantId(){return participantId;}public String getDisplayName(){return displayName;}
    public ParticipantMemberRole getRole(){return role;}public long getRevision(){return revision;}
    public int getMemberCount(){return memberCount;}
}

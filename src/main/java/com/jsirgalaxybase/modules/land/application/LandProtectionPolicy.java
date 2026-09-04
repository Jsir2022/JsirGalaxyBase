package com.jsirgalaxybase.modules.land.application;

import java.util.Optional;

import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.LandProtectionAction;
import com.jsirgalaxybase.modules.land.domain.LandProtectionDecision;
import com.jsirgalaxybase.modules.land.domain.LandProtectionReason;
import com.jsirgalaxybase.modules.land.domain.PersonalLandTitle;

public final class LandProtectionPolicy {

    private final LandProtectionIndex index;
    private final boolean allowFakePlayers;

    public LandProtectionPolicy(LandProtectionIndex index, boolean allowFakePlayers) {
        if (index == null) throw new IllegalArgumentException("index must not be null");
        this.index = index;
        this.allowFakePlayers = allowFakePlayers;
    }

    public LandProtectionDecision decide(String playerRef, boolean fakePlayer, LandChunkKey chunkKey,
        LandProtectionAction action) {
        Optional<PersonalLandTitle> found = index.find(chunkKey);
        if (!found.isPresent()) {
            return new LandProtectionDecision(action, chunkKey, true, LandProtectionReason.UNCLAIMED, null);
        }
        PersonalLandTitle title = found.get();
        if (fakePlayer) {
            return new LandProtectionDecision(action, chunkKey, allowFakePlayers,
                allowFakePlayers ? LandProtectionReason.FAKE_PLAYER_ALLOWED : LandProtectionReason.DENIED_FAKE_PLAYER,
                title);
        }
        if (playerRef != null && title.getOwnerPlayerRef().equals(playerRef)) {
            return new LandProtectionDecision(action, chunkKey, true, LandProtectionReason.OWNER, title);
        }
        return new LandProtectionDecision(action, chunkKey, false,
            playerRef == null ? LandProtectionReason.DENIED_ENVIRONMENT : LandProtectionReason.DENIED_NOT_OWNER,
            title);
    }
}

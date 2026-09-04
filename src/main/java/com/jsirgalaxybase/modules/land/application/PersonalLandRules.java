package com.jsirgalaxybase.modules.land.application;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.jsirgalaxybase.modules.land.domain.LandActionResult;
import com.jsirgalaxybase.modules.land.domain.LandChunkKey;

public final class PersonalLandRules {

    private final int maxClaimsPerPlayer;
    private final Set<Integer> blockedDimensions;
    private final Set<LandChunkKey> reservedChunks;

    public PersonalLandRules(int maxClaimsPerPlayer, Set<Integer> blockedDimensions,
        Set<LandChunkKey> reservedChunks) {
        if (maxClaimsPerPlayer <= 0) throw new IllegalArgumentException("maxClaimsPerPlayer must be positive");
        this.maxClaimsPerPlayer = maxClaimsPerPlayer;
        this.blockedDimensions = immutableCopy(blockedDimensions);
        this.reservedChunks = immutableCopy(reservedChunks);
    }

    public static PersonalLandRules allowAll(int maxClaimsPerPlayer) {
        return new PersonalLandRules(maxClaimsPerPlayer, Collections.<Integer>emptySet(),
            Collections.<LandChunkKey>emptySet());
    }

    public LandActionResult evaluateClaim(LandChunkKey chunkKey, int currentClaims) {
        if (blockedDimensions.contains(chunkKey.getDimensionId())) return LandActionResult.DIMENSION_BLOCKED;
        if (reservedChunks.contains(chunkKey)) return LandActionResult.RESERVED;
        if (currentClaims >= maxClaimsPerPlayer) return LandActionResult.LIMIT_REACHED;
        return LandActionResult.SUCCESS;
    }

    public int getMaxClaimsPerPlayer() {
        return maxClaimsPerPlayer;
    }

    public boolean isReserved(LandChunkKey chunkKey) {
        return chunkKey != null && reservedChunks.contains(chunkKey);
    }

    public boolean isDimensionBlocked(int dimensionId) {
        return blockedDimensions.contains(Integer.valueOf(dimensionId));
    }

    private static <T> Set<T> immutableCopy(Set<T> values) {
        return values == null || values.isEmpty() ? Collections.<T>emptySet()
            : Collections.unmodifiableSet(new HashSet<T>(values));
    }
}

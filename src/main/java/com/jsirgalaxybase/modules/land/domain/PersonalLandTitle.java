/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Derived from the GTNH ServerUtilities ClaimedChunk state model.
 * Team ownership and mutable global state were replaced with a versioned personal title.
 */
package com.jsirgalaxybase.modules.land.domain;

import java.util.Objects;

public final class PersonalLandTitle {

    private final long titleId;
    private final LandChunkKey chunkKey;
    private final String ownerPlayerRef;
    private final LandTitleStatus status;
    private final long version;

    public PersonalLandTitle(long titleId, LandChunkKey chunkKey, String ownerPlayerRef, LandTitleStatus status,
        long version) {
        if (titleId <= 0L) throw new IllegalArgumentException("titleId must be positive");
        if (version <= 0L) throw new IllegalArgumentException("version must be positive");
        this.titleId = titleId;
        this.chunkKey = Objects.requireNonNull(chunkKey, "chunkKey");
        this.ownerPlayerRef = requireText(ownerPlayerRef, "ownerPlayerRef");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
    }

    public long getTitleId() {
        return titleId;
    }

    public LandChunkKey getChunkKey() {
        return chunkKey;
    }

    public String getOwnerPlayerRef() {
        return ownerPlayerRef;
    }

    public LandTitleStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public PersonalLandTitle withStatus(LandTitleStatus newStatus) {
        return new PersonalLandTitle(titleId, chunkKey, ownerPlayerRef, newStatus, version + 1L);
    }

    public PersonalLandTitle transferTo(String newOwnerPlayerRef) {
        return new PersonalLandTitle(titleId, chunkKey, newOwnerPlayerRef, LandTitleStatus.ACTIVE, version + 1L);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}

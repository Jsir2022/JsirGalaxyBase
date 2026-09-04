/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Derived from the indexed chunk lookup behavior in GTNH ServerUtilities ClaimedChunks.
 */
package com.jsirgalaxybase.modules.land.application;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.PersonalLandTitle;

public final class LandProtectionIndex {

    private volatile Map<LandChunkKey, PersonalLandTitle> snapshot = Collections.emptyMap();

    public Optional<PersonalLandTitle> find(LandChunkKey chunkKey) {
        return Optional.ofNullable(snapshot.get(chunkKey));
    }

    public boolean mayModify(String playerRef, LandChunkKey chunkKey) {
        PersonalLandTitle title = snapshot.get(chunkKey);
        return title == null || (playerRef != null && title.getOwnerPlayerRef().equals(playerRef));
    }

    public synchronized void replace(Collection<PersonalLandTitle> titles) {
        Map<LandChunkKey, PersonalLandTitle> next = new HashMap<LandChunkKey, PersonalLandTitle>();
        if (titles != null) {
            for (PersonalLandTitle title : titles) next.put(title.getChunkKey(), title);
        }
        snapshot = Collections.unmodifiableMap(next);
    }

    public synchronized void put(PersonalLandTitle title) {
        Map<LandChunkKey, PersonalLandTitle> next = new HashMap<LandChunkKey, PersonalLandTitle>(snapshot);
        next.put(title.getChunkKey(), title);
        snapshot = Collections.unmodifiableMap(next);
    }

    public synchronized void remove(LandChunkKey chunkKey) {
        Map<LandChunkKey, PersonalLandTitle> next = new HashMap<LandChunkKey, PersonalLandTitle>(snapshot);
        next.remove(chunkKey);
        snapshot = Collections.unmodifiableMap(next);
    }

    public int size() {
        return snapshot.size();
    }
}

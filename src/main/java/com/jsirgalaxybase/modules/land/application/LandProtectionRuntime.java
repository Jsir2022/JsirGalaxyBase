package com.jsirgalaxybase.modules.land.application;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.LandProtectionAction;
import com.jsirgalaxybase.modules.land.domain.LandProtectionDecision;
import com.jsirgalaxybase.modules.land.domain.LandProtectionMode;

public final class LandProtectionRuntime {

    private final LandProtectionPolicy policy;
    private final LandProtectionMode mode;
    private final EnumMap<LandProtectionAction, AtomicLong> deniedCounts =
        new EnumMap<LandProtectionAction, AtomicLong>(LandProtectionAction.class);
    private final ConcurrentHashMap<String, ShadowNotice> latestShadowNotices =
        new ConcurrentHashMap<String, ShadowNotice>();

    public LandProtectionRuntime(LandProtectionPolicy policy, LandProtectionMode mode) {
        if (policy == null || mode == null) throw new IllegalArgumentException("land protection runtime is incomplete");
        this.policy = policy;
        this.mode = mode;
        for (LandProtectionAction action : LandProtectionAction.values()) deniedCounts.put(action, new AtomicLong());
    }

    public LandProtectionDecision evaluate(String playerRef, boolean fakePlayer, LandChunkKey chunkKey,
        LandProtectionAction action) {
        LandProtectionDecision decision = policy.decide(playerRef, fakePlayer, chunkKey, action);
        if (!decision.isAllowed()) {
            deniedCounts.get(action).incrementAndGet();
            if (mode == LandProtectionMode.SHADOW && playerRef != null && !playerRef.trim().isEmpty() && !fakePlayer) {
                latestShadowNotices.put(playerRef, new ShadowNotice(action.name(), chunkKey.toString()));
            }
        }
        return decision;
    }

    public boolean shouldCancel(LandProtectionDecision decision) {
        return mode == LandProtectionMode.ENFORCE && !decision.isAllowed();
    }

    public LandProtectionMode getMode() {
        return mode;
    }

    public Map<LandProtectionAction, Long> deniedCountSnapshot() {
        EnumMap<LandProtectionAction, Long> result = new EnumMap<LandProtectionAction, Long>(LandProtectionAction.class);
        for (Map.Entry<LandProtectionAction, AtomicLong> entry : deniedCounts.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return Collections.unmodifiableMap(result);
    }

    public ShadowNotice getLatestShadowNotice(String playerRef) {
        return playerRef == null ? null : latestShadowNotices.get(playerRef);
    }

    public static final class ShadowNotice {
        private final String action;
        private final String chunk;

        ShadowNotice(String action, String chunk) {
            this.action = action;
            this.chunk = chunk;
        }

        public String getAction() { return action; }
        public String getChunk() { return chunk; }
    }
}

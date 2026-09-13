package com.jsirgalaxybase.quest.core;

public interface CommandRewardPolicy {
    boolean allows(String normalizedCommand, boolean viaPlayer);
}

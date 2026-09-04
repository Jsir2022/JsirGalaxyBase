package com.jsirgalaxybase.modules.land.application;

import java.util.HashSet;
import java.util.Set;

import com.jsirgalaxybase.config.ModConfiguration;
import com.jsirgalaxybase.modules.land.domain.LandChunkKey;

public final class PersonalLandRulesFactory {

    private PersonalLandRulesFactory() {}

    public static PersonalLandRules fromConfiguration(ModConfiguration configuration, String localServerId) {
        Set<Integer> blockedDimensions = new HashSet<Integer>();
        for (int dimension : configuration.getLandBlockedDimensions()) blockedDimensions.add(dimension);
        Set<LandChunkKey> reservedChunks = new HashSet<LandChunkKey>();
        for (String value : configuration.getLandReservedChunks()) {
            if (value == null || value.trim().isEmpty()) continue;
            String[] parts = value.trim().split(":", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException(
                    "landReservedChunks entry must use dimension:chunkX:chunkZ: " + value);
            }
            try {
                reservedChunks.add(new LandChunkKey(localServerId, Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("landReservedChunks entry contains a non-integer: " + value);
            }
        }
        return new PersonalLandRules(configuration.getLandMaxClaimsPerPlayer(), blockedDimensions, reservedChunks);
    }
}

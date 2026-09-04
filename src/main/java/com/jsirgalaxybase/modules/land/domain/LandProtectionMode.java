package com.jsirgalaxybase.modules.land.domain;

import java.util.Locale;

public enum LandProtectionMode {
    SHADOW,
    ENFORCE;

    public static LandProtectionMode parseStrict(String value) {
        if (value == null) throw new IllegalArgumentException("landProtectionMode must not be null");
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("landProtectionMode must be SHADOW or ENFORCE: " + value);
        }
    }
}

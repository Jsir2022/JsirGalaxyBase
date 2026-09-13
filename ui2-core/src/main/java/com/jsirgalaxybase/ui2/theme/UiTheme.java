package com.jsirgalaxybase.ui2.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UiTheme {
    private final Map<String, Integer> colors;
    private final Map<String, Integer> spacing;
    private final Map<String, Integer> radii;
    private final Map<String, Integer> typography;
    private final Map<String, Integer> controls;
    private final Map<String, Integer> motion;
    private final Map<String, Integer> elevation;

    public UiTheme(Map<String, Integer> colors, Map<String, Integer> spacing, Map<String, Integer> radii) {
        this(colors, spacing, radii, defaults(11), defaults(24), defaults(140), defaults(2));
    }

    public UiTheme(Map<String, Integer> colors, Map<String, Integer> spacing, Map<String, Integer> radii,
        Map<String, Integer> typography, Map<String, Integer> controls, Map<String, Integer> motion,
        Map<String, Integer> elevation) {
        this.colors = immutable(colors, "colors");
        this.spacing = immutable(spacing, "spacing");
        this.radii = immutable(radii, "radii");
        this.typography = immutable(typography, "typography");
        this.controls = immutable(controls, "controls");
        this.motion = immutable(motion, "motion");
        this.elevation = immutable(elevation, "elevation");
    }

    private static Map<String, Integer> defaults(int value) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        result.put("default", value);
        return result;
    }

    private static Map<String, Integer> immutable(Map<String, Integer> values, String name) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException(name + " must not be empty");
        return Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(values));
    }

    public int color(String name) { return required(colors, name); }
    public int spacing(String name) { return required(spacing, name); }
    public int radius(String name) { return required(radii, name); }
    public int typography(String name) { return required(typography, name); }
    public int control(String name) { return required(controls, name); }
    public int motion(String name) { return required(motion, name); }
    public int elevation(String name) { return required(elevation, name); }
    public Map<String, Integer> getColors() { return colors; }
    public Map<String, Integer> getSpacing() { return spacing; }
    public Map<String, Integer> getRadii() { return radii; }
    public Map<String, Integer> getTypography() { return typography; }
    public Map<String, Integer> getControls() { return controls; }
    public Map<String, Integer> getMotion() { return motion; }
    public Map<String, Integer> getElevation() { return elevation; }

    private static int required(Map<String, Integer> values, String name) {
        Integer value = values.get(name);
        if (value == null) throw new IllegalArgumentException("unknown theme token: " + name);
        return value.intValue();
    }
}

package com.jsirgalaxybase.ui2.core;

import java.util.Locale;

import com.jsirgalaxybase.ui2.motion.UiClock;
import com.jsirgalaxybase.ui2.resource.ResourceResolver;
import com.jsirgalaxybase.ui2.text.TextMeasurer;
import com.jsirgalaxybase.ui2.theme.UiTheme;

public final class UiContext {
    private final UiTheme theme;
    private final Locale locale;
    private final float density;
    private final UiClock clock;
    private final TextMeasurer textMeasurer;
    private final ResourceResolver resources;

    public UiContext(UiTheme theme, Locale locale, float density, UiClock clock) {
        this(theme, locale, density, clock, new TextMeasurer() {
            @Override public com.jsirgalaxybase.ui2.geometry.UiSize measure(String text,
                com.jsirgalaxybase.ui2.text.TextStyle style, int maxWidth) {
                int width = Math.min(Math.max(0, maxWidth), (text == null ? 0 : text.length()) * style.getSize());
                return new com.jsirgalaxybase.ui2.geometry.UiSize(width, style.getLineHeight());
            }
        }, ResourceResolver.EMPTY);
    }

    public UiContext(UiTheme theme, Locale locale, float density, UiClock clock,
        TextMeasurer textMeasurer, ResourceResolver resources) {
        if (theme == null || locale == null || clock == null || density <= 0F) throw new IllegalArgumentException("invalid context");
        if (textMeasurer == null || resources == null) throw new IllegalArgumentException("platform services are required");
        this.theme = theme;
        this.locale = locale;
        this.density = density;
        this.clock = clock;
        this.textMeasurer = textMeasurer;
        this.resources = resources;
    }

    public UiTheme getTheme() { return theme; }
    public Locale getLocale() { return locale; }
    public float getDensity() { return density; }
    public UiClock getClock() { return clock; }
    public TextMeasurer getTextMeasurer() { return textMeasurer; }
    public ResourceResolver getResources() { return resources; }
}

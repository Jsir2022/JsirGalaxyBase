package com.jsirgalaxybase.terminal.client.ui2;

import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.terminal.client.settings.TerminalAppearance;
import com.jsirgalaxybase.terminal.client.settings.TerminalWindowSize;

/** Single responsive window contract shared by terminal Screen and Container documents. */
public final class TerminalWindowMetrics {
    private static final int MIN_WIDTH = 350;
    private static final int MIN_HEIGHT = 193;
    private static final int MAX_WIDTH = 620;
    private static final int MAX_HEIGHT = 340;

    private final UiRect bounds;
    private final int navigationWidth;

    private TerminalWindowMetrics(UiRect bounds, int navigationWidth) {
        this.bounds = bounds;
        this.navigationWidth = navigationWidth;
    }

    public static TerminalWindowMetrics compute(UiSize viewport) {
        return compute(viewport, TerminalAppearance.INSTANCE.preferences().getWindowSize());
    }

    public static TerminalWindowMetrics compute(UiSize viewport, TerminalWindowSize profile) {
        int viewportWidth = viewport == null ? 0 : Math.max(1, viewport.getWidth());
        int viewportHeight = viewport == null ? 0 : Math.max(1, viewport.getHeight());
        TerminalWindowSize actual=profile==null?TerminalWindowSize.STANDARD:profile;
        int width = fit(viewportWidth, MIN_WIDTH, MAX_WIDTH, actual.getWidthRatio(), 8);
        int height = fit(viewportHeight, MIN_HEIGHT, MAX_HEIGHT, actual.getHeightRatio(), 8);
        int x = Math.max(0, (viewportWidth - width) / 2);
        int y = Math.max(0, (viewportHeight - height) / 2);
        int navigation = width < 400 ? 56 : width < 520 ? 64 : 72;
        return new TerminalWindowMetrics(new UiRect(x, y, width, height), navigation);
    }

    private static int fit(int available, int minimum, int maximum, float ratio, int margin) {
        int capped = Math.min(maximum, Math.max(1, available - margin));
        if (available <= minimum) return available;
        return Math.min(capped, Math.max(Math.min(minimum, capped), Math.round(available * ratio)));
    }

    public UiRect getBounds() { return bounds; }
    public int getNavigationWidth() { return navigationWidth; }
    public int getMainWidth() { return Math.max(0, bounds.getWidth() - navigationWidth); }
    public Insets viewportInsets(UiSize viewport) {
        int width = viewport == null ? bounds.getRight() : viewport.getWidth();
        int height = viewport == null ? bounds.getBottom() : viewport.getHeight();
        return new Insets(bounds.getY(), Math.max(0, width - bounds.getRight()),
            Math.max(0, height - bounds.getBottom()), bounds.getX());
    }
}

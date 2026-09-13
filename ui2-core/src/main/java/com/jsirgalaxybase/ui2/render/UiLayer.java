package com.jsirgalaxybase.ui2.render;

public final class UiLayer {
    public static final int BACKGROUND = 0;
    public static final int CONTENT = 100;
    public static final int NATIVE = 200;
    /** Item-count and focus chrome painted after native items but before native tooltips. */
    public static final int NATIVE_OVERLAY = 250;
    public static final int TOOLTIP = 300;
    public static final int MODAL = 400;
    public static final int TOAST = 500;

    private UiLayer() {}
}

package com.jsirgalaxybase.ui2.lab;

public final class UiLabState {
    private final UiLabPage page;
    private final boolean debug;
    private final boolean reducedMotion;
    private final boolean modal;

    public UiLabState(UiLabPage page, boolean debug, boolean reducedMotion, boolean modal) {
        this.page = page == null ? UiLabPage.OVERVIEW : page;
        this.debug = debug; this.reducedMotion = reducedMotion; this.modal = modal;
    }
    public static UiLabState initial() { return new UiLabState(UiLabPage.OVERVIEW, false, false, false); }
    public UiLabPage getPage() { return page; }
    public boolean isDebug() { return debug; }
    public boolean isReducedMotion() { return reducedMotion; }
    public boolean isModal() { return modal; }
    public UiLabState withPage(UiLabPage value) { return new UiLabState(value, debug, reducedMotion, false); }
    public UiLabState withDebug(boolean value) { return new UiLabState(page, value, reducedMotion, modal); }
    public UiLabState withReducedMotion(boolean value) { return new UiLabState(page, debug, value, modal); }
    public UiLabState withModal(boolean value) { return new UiLabState(page, debug, reducedMotion, value); }
}

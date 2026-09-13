package com.jsirgalaxybase.ui2.lab;

public final class UiLabAction {
    public enum Type { SELECT_PAGE, TOGGLE_DEBUG, TOGGLE_MOTION, OPEN_MODAL, CLOSE_MODAL }
    private final Type type;
    private final UiLabPage page;
    private UiLabAction(Type type, UiLabPage page) { this.type = type; this.page = page; }
    public static UiLabAction select(UiLabPage page) { return new UiLabAction(Type.SELECT_PAGE, page); }
    public static UiLabAction of(Type type) { return new UiLabAction(type, null); }
    public Type getType() { return type; }
    public UiLabPage getPage() { return page; }
}

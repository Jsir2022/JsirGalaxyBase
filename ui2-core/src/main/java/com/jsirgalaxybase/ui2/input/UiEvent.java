package com.jsirgalaxybase.ui2.input;

public final class UiEvent {
    public enum Type { POINTER_DOWN, POINTER_UP, POINTER_MOVE, SCROLL, KEY_DOWN, TEXT_INPUT }
    public enum Phase { CAPTURE, TARGET, BUBBLE }

    private final Type type;
    private final int x;
    private final int y;
    private final int button;
    private final int delta;
    private final UiKeyCode keyCode;
    private final char typedChar;
    private Phase phase = Phase.TARGET;

    private UiEvent(Type type, int x, int y, int button, int delta, UiKeyCode keyCode, char typedChar) {
        this.type = type; this.x = x; this.y = y; this.button = button; this.delta = delta;
        this.keyCode = keyCode; this.typedChar = typedChar;
    }
    public static UiEvent pointer(Type type, int x, int y, int button) { return new UiEvent(type, x, y, button, 0, UiKeyCode.UNKNOWN, '\0'); }
    public static UiEvent scroll(int x, int y, int delta) { return new UiEvent(Type.SCROLL, x, y, 0, delta, UiKeyCode.UNKNOWN, '\0'); }
    public static UiEvent key(UiKeyCode keyCode) { return new UiEvent(Type.KEY_DOWN, 0, 0, 0, 0, keyCode == null ? UiKeyCode.UNKNOWN : keyCode, '\0'); }
    public static UiEvent text(char value) { return new UiEvent(Type.TEXT_INPUT, 0, 0, 0, 0, UiKeyCode.UNKNOWN, value); }
    public Type getType() { return type; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getButton() { return button; }
    public int getDelta() { return delta; }
    public UiKeyCode getKeyCode() { return keyCode; }
    public char getTypedChar() { return typedChar; }
    public Phase getPhase() { return phase; }
    void setPhase(Phase phase) { this.phase = phase; }
}

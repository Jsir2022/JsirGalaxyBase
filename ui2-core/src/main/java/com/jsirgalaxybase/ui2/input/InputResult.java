package com.jsirgalaxybase.ui2.input;

public enum InputResult {
    PASS(false, false, false),
    CONSUMED(true, false, false),
    CAPTURE_POINTER(true, true, false),
    RELEASE_POINTER(true, false, true);

    private final boolean consumed;
    private final boolean capture;
    private final boolean release;
    InputResult(boolean consumed, boolean capture, boolean release) {
        this.consumed = consumed; this.capture = capture; this.release = release;
    }
    public boolean isConsumed() { return consumed; }
    public boolean shouldCapture() { return capture; }
    public boolean shouldRelease() { return release; }
}

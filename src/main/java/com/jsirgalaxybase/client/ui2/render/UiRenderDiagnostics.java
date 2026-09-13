package com.jsirgalaxybase.client.ui2.render;

/** Last-frame diagnostics displayed by the UI Lab; no business code depends on these values. */
public final class UiRenderDiagnostics {
    public static final UiRenderDiagnostics INSTANCE = new UiRenderDiagnostics();
    private volatile int commandCount;
    private volatile int maximumClipDepth;
    private volatile long frameNanos;
    private volatile long renderNanos;
    private UiRenderDiagnostics() {}
    void record(int commands, int clipDepth, long nanos) {
        commandCount = commands; maximumClipDepth = clipDepth; renderNanos = Math.max(0L, nanos);
    }
    public void recordFrame(long nanos) { frameNanos = Math.max(0L, nanos); }
    public int getCommandCount() { return commandCount; }
    public int getMaximumClipDepth() { return maximumClipDepth; }
    public long getFrameNanos() { return frameNanos; }
    public long getRenderNanos() { return renderNanos; }
}

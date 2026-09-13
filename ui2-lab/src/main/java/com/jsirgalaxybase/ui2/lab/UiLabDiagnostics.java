package com.jsirgalaxybase.ui2.lab;

/** Host-provided diagnostics; standalone Java2D uses the neutral value. */
public interface UiLabDiagnostics {
    UiLabDiagnostics NONE = new UiLabDiagnostics() {
        @Override public String summary(boolean reducedMotion) {
            return "Backend Java2D · DrawList · " + (reducedMotion ? "reduced motion" : "smooth motion");
        }
    };
    String summary(boolean reducedMotion);
}

package com.jsirgalaxybase.ui2.state;

public abstract class UiSelector<S, R> {
    private S lastState;
    private R lastValue;
    private boolean initialized;

    public final R select(S state) {
        if (!initialized || state != lastState) {
            lastState = state;
            lastValue = compute(state);
            initialized = true;
        }
        return lastValue;
    }

    protected abstract R compute(S state);
}

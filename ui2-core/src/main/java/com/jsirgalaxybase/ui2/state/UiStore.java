package com.jsirgalaxybase.ui2.state;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public final class UiStore<S, A> {
    public interface Listener<S> { void onStateChanged(S state); }

    private final UiReducer<S, A> reducer;
    private final List<UiEffect<S, A>> effects;
    private final List<Listener<S>> listeners = new ArrayList<Listener<S>>();
    private final Deque<A> pendingActions = new ArrayDeque<A>();
    private S state;
    private boolean flushing;
    private static final int MAX_ACTIONS_PER_FLUSH = 1000;

    public UiStore(S initialState, UiReducer<S, A> reducer) { this(initialState, reducer, Collections.<UiEffect<S, A>>emptyList()); }
    public UiStore(S initialState, UiReducer<S, A> reducer, List<UiEffect<S, A>> effects) {
        if (initialState == null || reducer == null) throw new IllegalArgumentException("state and reducer are required");
        this.state = initialState;
        this.reducer = reducer;
        this.effects = new ArrayList<UiEffect<S, A>>(effects);
    }

    public S getState() { return state; }
    public void addListener(Listener<S> listener) { if (listener != null) listeners.add(listener); }
    public void dispatch(A action) {
        if (action == null) return;
        pendingActions.addLast(action);
        if (flushing) return;
        flushing = true;
        S frameStart = state;
        int processed = 0;
        A lastAction = action;
        try {
            while (!pendingActions.isEmpty()) {
                if (++processed > MAX_ACTIONS_PER_FLUSH) {
                    pendingActions.clear();
                    throw new IllegalStateException("UI action flush exceeded " + MAX_ACTIONS_PER_FLUSH
                        + " actions at " + String.valueOf(lastAction)
                        + "; an effect is probably dispatching in a loop");
                }
                A currentAction = pendingActions.removeFirst();
                lastAction = currentAction;
                S previous = state;
                S next = reducer.reduce(previous, currentAction);
                if (next == null) throw new IllegalStateException("reducer returned null for " + currentAction);
                state = next;
                for (UiEffect<S, A> effect : effects) effect.afterDispatch(previous, next, currentAction);
            }
        } finally {
            flushing = false;
        }
        if (frameStart != state) {
            for (Listener<S> listener : new ArrayList<Listener<S>>(listeners)) listener.onStateChanged(state);
        }
    }
}

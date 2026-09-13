package com.jsirgalaxybase.ui2.state;

public interface UiReducer<S, A> {
    S reduce(S state, A action);
}

package com.jsirgalaxybase.ui2.state;

public interface UiEffect<S, A> {
    void afterDispatch(S previous, S current, A action);
}

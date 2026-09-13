package com.jsirgalaxybase.ui2.lab;

import com.jsirgalaxybase.ui2.state.UiReducer;

public final class UiLabReducer implements UiReducer<UiLabState, UiLabAction> {
    @Override public UiLabState reduce(UiLabState state, UiLabAction action) {
        if (action == null) return state;
        switch (action.getType()) {
            case SELECT_PAGE: return state.withPage(action.getPage());
            case TOGGLE_DEBUG: return state.withDebug(!state.isDebug());
            case TOGGLE_MOTION: return state.withReducedMotion(!state.isReducedMotion());
            case OPEN_MODAL: return state.withModal(true);
            case CLOSE_MODAL: return state.withModal(false);
            default: return state;
        }
    }
}

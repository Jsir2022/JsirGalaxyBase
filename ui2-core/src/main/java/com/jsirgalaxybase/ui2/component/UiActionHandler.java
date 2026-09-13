package com.jsirgalaxybase.ui2.component;

import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;

/** Platform-neutral component action callback. Business effects remain outside painting. */
public interface UiActionHandler {
    InputResult handle(UiEvent event);
}

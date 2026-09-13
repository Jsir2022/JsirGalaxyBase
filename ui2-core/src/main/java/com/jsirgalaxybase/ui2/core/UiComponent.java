package com.jsirgalaxybase.ui2.core;

public interface UiComponent<P> {
    UiElement render(P props, UiContext context);
}

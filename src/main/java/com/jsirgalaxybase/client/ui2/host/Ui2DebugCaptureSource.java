package com.jsirgalaxybase.client.ui2.host;

import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.render.DrawList;

/** Read-only bridge for developer screenshots; it never mutates UI state. */
public interface Ui2DebugCaptureSource {
    DrawList ui2DebugDrawList();
    UiNode ui2DebugRoot();
    int ui2DebugWidth();
    int ui2DebugHeight();
}

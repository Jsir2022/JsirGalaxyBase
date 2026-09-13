package com.jsirgalaxybase.ui2.lab;

import java.util.Locale;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.debug.UiDebugSnapshot;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.state.UiStore;
import com.jsirgalaxybase.ui2.theme.DarkIndustrialTheme;

/** Compatibility facade used by the standalone exporter and the Minecraft Lab. */
public final class UiLabScene {
    public DrawList render(UiLabPage page, int width, int height, boolean debug, boolean reducedMotion) {
        return capture(page, width, height, debug, reducedMotion).getDrawList();
    }

    public UiLabCapture capture(UiLabPage page, int width, int height, boolean debug, boolean reducedMotion) {
        UiLabState state = new UiLabState(page, debug, reducedMotion, false);
        UiStore<UiLabState, UiLabAction> store = new UiStore<UiLabState, UiLabAction>(state, new UiLabReducer());
        UiRuntime runtime = new UiRuntime(new UiLabDocument(store),
            new UiContext(DarkIndustrialTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0, reducedMotion)));
        runtime.setViewport(width, height);
        DrawList result = runtime.frame();
        String snapshot = UiDebugSnapshot.capture(runtime.getRoot(), result, width, height);
        runtime.close();
        return new UiLabCapture(result, snapshot);
    }
}

package com.jsirgalaxybase.ui2.lab;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.state.UiStore;
import com.jsirgalaxybase.ui2.theme.DarkIndustrialTheme;

public class UiLabDocumentTest {
    @Test public void everyPageRendersAtBothAcceptanceSizes() {
        UiLabScene scene = new UiLabScene();
        int[][] sizes = {{350, 193}, {620, 340}};
        for (UiLabPage page : UiLabPage.values()) for (int[] size : sizes) {
            DrawList list = scene.render(page, size[0], size[1], true, false);
            assertTrue(page.name(), list.size() > 10);
            assertTrue(page.name(), !list.snapshot().contains("literal\\n"));
        }
    }

    @Test public void overlayDialogOpensAndEscapeClosesExactlyOnceAcrossRebuild() {
        final UiStore<UiLabState, UiLabAction> store = new UiStore<UiLabState, UiLabAction>(
            new UiLabState(UiLabPage.OVERLAY, false, false, false), new UiLabReducer());
        final UiRuntime runtime = new UiRuntime(new UiLabDocument(store), new UiContext(
            DarkIndustrialTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0, false)));
        store.addListener(new UiStore.Listener<UiLabState>() {
            @Override public void onStateChanged(UiLabState state) { runtime.invalidate(); }
        });
        runtime.setViewport(350, 193);
        runtime.frame();
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, 90, 75, 0)));
        assertTrue(store.getState().isModal());
        runtime.frame();
        assertTrue(runtime.hasModal());
        runtime.invalidate();
        runtime.frame();
        assertTrue(runtime.hasModal());
        assertTrue(runtime.dispatch(UiEvent.key(UiKeyCode.ESCAPE)));
        assertFalse(store.getState().isModal());
        runtime.frame();
        assertFalse(runtime.hasModal());
        runtime.close();
    }
}

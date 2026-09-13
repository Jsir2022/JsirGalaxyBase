package com.jsirgalaxybase.client.ui2.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiRect;

public class MinecraftClipStackTest {
    @Test public void nestedClipsIntersectAndCloseRestoresHostBaseline() {
        RecordingOps ops = new RecordingOps(true, new UiRect(0, 0, 400, 300));
        MinecraftClipStack stack = new MinecraftClipStack(2, 400, ops);
        stack.push(new UiRect(10, 20, 100, 80));
        UiRect first = ops.box;
        stack.push(new UiRect(50, 30, 100, 100));
        assertTrue(ops.box.getWidth() < first.getWidth());
        assertEquals(2, stack.depth());
        stack.close();
        assertEquals(new UiRect(0, 0, 400, 300), ops.box);
        assertTrue(ops.enabled);
    }

    @Test public void closeDisablesScissorWhenHostHadNone() {
        RecordingOps ops = new RecordingOps(false, null);
        MinecraftClipStack stack = new MinecraftClipStack(1, 200, ops);
        stack.push(new UiRect(0, 0, 20, 20));
        assertTrue(ops.enabled);
        stack.close();
        assertFalse(ops.enabled);
    }

    private static final class RecordingOps implements MinecraftClipStack.GlOps {
        private boolean enabled;
        private UiRect box;
        private RecordingOps(boolean enabled, UiRect box) { this.enabled = enabled; this.box = box; }
        @Override public boolean enabled() { return enabled; }
        @Override public UiRect box() { return box; }
        @Override public void enable() { enabled = true; }
        @Override public void disable() { enabled = false; }
        @Override public void scissor(UiRect value) { box = value; }
    }
}

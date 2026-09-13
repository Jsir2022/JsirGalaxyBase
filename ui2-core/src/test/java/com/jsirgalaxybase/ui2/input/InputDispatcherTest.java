package com.jsirgalaxybase.ui2.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiRect;

public class InputDispatcherTest {
    @Test public void eventsTravelCaptureTargetBubbleAndFocusCycles() {
        final StringBuilder phases = new StringBuilder();
        UiInputHandler recorder = new UiInputHandler() {
            @Override public InputResult handle(UiInputNode target, UiEvent event) {
                phases.append(target.getKey()).append(':').append(event.getPhase()).append(';'); return InputResult.PASS;
            }
        };
        UiInputNode root = new UiInputNode("root", new UiRect(0, 0, 100, 100), false, recorder);
        UiInputNode first = new UiInputNode("first", new UiRect(5, 5, 30, 20), true, recorder);
        UiInputNode second = new UiInputNode("second", new UiRect(40, 5, 30, 20), true, recorder);
        root.add(first).add(second);
        InputDispatcher dispatcher = new InputDispatcher(root);
        dispatcher.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, 10, 10, 0));
        assertEquals("root:CAPTURE;first:TARGET;root:BUBBLE;", phases.toString());
        assertSame(first, dispatcher.getFocused());
        dispatcher.dispatch(UiEvent.key(UiKeyCode.TAB));
        assertSame(second, dispatcher.getFocused());
    }

    @Test public void pointerCaptureReceivesReleaseOutsideBounds() {
        UiInputNode drag = new UiInputNode("drag", new UiRect(5, 5, 20, 20), true, new UiInputHandler() {
            @Override public InputResult handle(UiInputNode target, UiEvent event) {
                return event.getType() == UiEvent.Type.POINTER_DOWN ? InputResult.CAPTURE_POINTER : InputResult.CONSUMED;
            }
        });
        InputDispatcher dispatcher = new InputDispatcher(new UiInputNode("root", new UiRect(0, 0, 100, 100), false, null).add(drag));
        assertTrue(dispatcher.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, 10, 10, 0)));
        assertSame(drag, dispatcher.getPointerCapture());
        assertTrue(dispatcher.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_UP, 500, 500, 0)));
        assertNull(dispatcher.getPointerCapture());
    }

    @Test public void modalBlocksRootAndEscapeClosesIt() {
        final int[] rootClicks = {0};
        UiInputNode root = new UiInputNode("root", new UiRect(0, 0, 100, 100), false, new UiInputHandler() {
            @Override public InputResult handle(UiInputNode target, UiEvent event) { rootClicks[0]++; return InputResult.CONSUMED; }
        });
        InputDispatcher dispatcher = new InputDispatcher(root);
        dispatcher.openModal(new UiInputNode("modal", new UiRect(20, 20, 40, 40), false, null));
        assertTrue(dispatcher.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, 2, 2, 0)));
        assertEquals(0, rootClicks[0]);
        assertTrue(dispatcher.dispatch(UiEvent.key(UiKeyCode.ESCAPE)));
        assertFalse(dispatcher.hasModal());
    }

    @Test public void rebuiltTreesPreserveFocusAndModalByStableKey() {
        UiInputNode root = new UiInputNode("root", new UiRect(0, 0, 100, 100), false, null)
            .add(new UiInputNode("field", new UiRect(0, 0, 20, 20), true, null));
        InputDispatcher dispatcher = new InputDispatcher(root);
        dispatcher.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, 5, 5, 0));
        assertEquals("field", dispatcher.getFocused().getKey());
        dispatcher.setRoot(new UiInputNode("root", new UiRect(0, 0, 100, 100), false, null)
            .add(new UiInputNode("field", new UiRect(0, 0, 20, 20), true, null)));
        assertEquals("field", dispatcher.getFocused().getKey());

        dispatcher.syncModal(new UiInputNode("dialog", new UiRect(0, 0, 100, 100), false, null)
            .add(new UiInputNode("close", new UiRect(10, 10, 20, 20), true, null)));
        assertEquals("close", dispatcher.getFocused().getKey());
        dispatcher.syncModal(new UiInputNode("dialog", new UiRect(0, 0, 100, 100), false, null)
            .add(new UiInputNode("close", new UiRect(10, 10, 20, 20), true, null)));
        assertEquals("close", dispatcher.getFocused().getKey());
        assertTrue(dispatcher.hasModal());
    }
}

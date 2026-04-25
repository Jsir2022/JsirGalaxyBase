package com.jsirgalaxybase.client.gui.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VerticalScrollPanelTest {

    @Test
    public void scrollOffsetIsClampedWithinViewportBounds() {
        VerticalScrollPanel panel = new VerticalScrollPanel(2, 2);
        panel.addScrollableChild(new DummyClickablePanel(), 20);
        panel.addScrollableChild(new DummyClickablePanel(), 20);
        panel.addScrollableChild(new DummyClickablePanel(), 20);
        panel.addScrollableChild(new DummyClickablePanel(), 20);
        panel.setBounds(new GuiRect(0, 0, 120, 50));

        panel.setScrollOffset(999);
        assertEquals(panel.getMaxScrollOffset(), panel.getScrollOffset());

        panel.setScrollOffset(-12);
        assertEquals(0, panel.getScrollOffset());
    }

    @Test
    public void wheelInputOutsideViewportDoesNotChangeOffset() {
        VerticalScrollPanel panel = new VerticalScrollPanel(0, 0);
        panel.addScrollableChild(new DummyClickablePanel(), 20);
        panel.addScrollableChild(new DummyClickablePanel(), 20);
        panel.addScrollableChild(new DummyClickablePanel(), 20);
        panel.setBounds(new GuiRect(0, 0, 100, 30));

        boolean handled = panel.mouseScrolled(null, 120, 120, -120);

        assertFalse(handled);
        assertEquals(0, panel.getScrollOffset());
    }

    @Test
    public void clickDispatchStillTargetsVisibleChildAfterScroll() {
        VerticalScrollPanel panel = new VerticalScrollPanel(0, 0);
        DummyClickablePanel first = new DummyClickablePanel();
        DummyClickablePanel second = new DummyClickablePanel();
        DummyClickablePanel third = new DummyClickablePanel();
        panel.addScrollableChild(first, 20);
        panel.addScrollableChild(second, 20);
        panel.addScrollableChild(third, 20);
        panel.setBounds(new GuiRect(0, 0, 100, 30));
        panel.setScrollOffset(20);

        assertTrue(panel.mouseClicked(null, 10, 10, 0));
        assertTrue(panel.mouseReleased(null, 10, 10, 0));

        assertEquals(0, first.getClickCount());
        assertEquals(1, second.getClickCount());
        assertEquals(0, third.getClickCount());
    }

    private static final class DummyClickablePanel extends AbstractGuiPanel {

        private boolean pressed;
        private int clickCount;

        @Override
        public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {}

        @Override
        public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            pressed = mouseButton == 0 && contains(mouseX, mouseY);
            return pressed;
        }

        @Override
        public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            boolean clicked = pressed && mouseButton == 0 && contains(mouseX, mouseY);
            pressed = false;
            if (clicked) {
                clickCount++;
            }
            return clicked;
        }

        int getClickCount() {
            return clickCount;
        }
    }
}
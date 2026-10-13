package com.jsirgalaxybase.ui2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.theme.DarkIndustrialTheme;

public class UiRuntimeTest {
    @Test public void viewportIsDeliveredBeforeTreeBuildAndAfterResize() {
        ViewportDocument document = new ViewportDocument();
        UiRuntime runtime = new UiRuntime(document,
            new UiContext(DarkIndustrialTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0, false)));
        runtime.setViewport(350, 193);
        runtime.frame();
        assertEquals(new UiSize(350, 193), document.builtFor);
        runtime.setViewport(620, 340);
        runtime.frame();
        assertEquals(new UiSize(620, 340), document.builtFor);
        assertEquals(2, document.viewportChanges);
        runtime.close();
    }

    @Test public void invalidationReconcilesStableNodeAndCloseUnmountsIt() {
        CountingDocument document = new CountingDocument();
        UiRuntime runtime = new UiRuntime(document,
            new UiContext(DarkIndustrialTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0, false)));
        runtime.setViewport(350, 193);
        DrawList first = runtime.frame();
        UiNode node = runtime.getRoot();
        assertEquals(1, document.builds);
        assertSame(first, runtime.frame());
        assertEquals(1, document.builds);
        runtime.invalidate();
        runtime.frame();
        assertSame(node, runtime.getRoot());
        assertEquals(new UiRect(0, 0, 350, 193), node.getBounds());
        runtime.close();
        assertFalse(node.isMounted());
    }

    private static class CountingDocument implements UiDocument {
        private int builds;
        @Override public UiElement build(UiContext context) { builds++; return UiComponents.surface("root"); }
        @Override public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
            return LayoutSpec.of("root", LayoutKind.STACK).preferred(viewport.getWidth(), viewport.getHeight()).build();
        }
        @Override public DrawList paint(UiNode root, UiContext context) {
            return new DrawList().add(DrawCommand.surface(0, root.getBounds(), context.getTheme().color("background"), 0));
        }
        @Override public UiInputNode input(UiNode root, UiContext context) { return null; }
    }

    private static final class ViewportDocument extends CountingDocument {
        private UiSize viewport;
        private UiSize builtFor;
        private int viewportChanges;
        @Override public void onViewportChanged(UiSize value, UiContext context) {
            viewport=value;
            viewportChanges++;
        }
        @Override public UiElement build(UiContext context) {
            builtFor=viewport;
            return super.build(context);
        }
    }
}

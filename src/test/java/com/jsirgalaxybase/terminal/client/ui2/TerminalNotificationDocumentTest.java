package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.settings.TerminalNotificationAction;
import com.jsirgalaxybase.terminal.client.settings.TerminalNotificationReducer;
import com.jsirgalaxybase.terminal.client.settings.TerminalNotificationUiState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.state.UiStore;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;

public class TerminalNotificationDocumentTest {
    @Test public void emptyNotificationPageFitsProductionSizes() {
        for (int[] size : new int[][] {{350,193},{427,240},{620,340}}) {
            UiRuntime runtime = runtime(size[0], size[1]);
            UiRect window = find(runtime, "terminal-window");
            for (DrawCommand command : runtime.frame().ordered()) {
                if (command.getKind() != DrawCommand.Kind.TEXT) continue;
                UiRect bounds = command.getBounds();
                assertTrue(bounds.getX() >= window.getX());
                assertTrue(bounds.getY() >= window.getY());
                assertTrue(bounds.getRight() <= window.getRight());
                assertTrue(bounds.getBottom() <= window.getBottom());
            }
        }
    }

    @Test public void helpModalConsumesEscapeAndClosesOnce() {
        UiStore<TerminalNotificationUiState, TerminalNotificationAction> store = store();
        TerminalNotificationDocument document = document(store);
        UiRuntime runtime = new UiRuntime(document, context());
        runtime.setViewport(427,240);
        runtime.frame();
        document.openHelp();
        runtime.invalidate();
        runtime.frame();
        assertTrue(runtime.hasModal());
        assertTrue(runtime.dispatch(UiEvent.key(UiKeyCode.ESCAPE)));
        runtime.invalidate();
        runtime.frame();
        assertFalse(runtime.hasModal());
    }

    @Test public void registryOnlyClaimsMigratedRoutes() {
        assertTrue(TerminalPageRegistry.supports("home"));
        assertTrue(TerminalPageRegistry.supports("notifications"));
        assertTrue(TerminalPageRegistry.supports("market"));
        assertTrue(TerminalPageRegistry.supports("market_standardized"));
        assertTrue(TerminalPageRegistry.supports("market_custom"));
        assertTrue(TerminalPageRegistry.supports("market_exchange"));
        assertTrue(TerminalPageRegistry.supports("market_account_center"));
        assertTrue(TerminalPageRegistry.supports("bank"));
        assertTrue(TerminalPageRegistry.supports("server_tools"));
    }

    private static UiRuntime runtime(int width, int height) {
        UiRuntime runtime = new UiRuntime(document(store()), context());
        runtime.setViewport(width, height);
        runtime.frame();
        return runtime;
    }

    private static TerminalNotificationDocument document(
        UiStore<TerminalNotificationUiState, TerminalNotificationAction> store) {
        return new TerminalNotificationDocument(store, TerminalHomeScreenModel.placeholder(), actions(),
            new TerminalNotificationDocument.TargetHandler() {
                @Override public void open(String pageId, String recordId) {}
            });
    }

    private static UiStore<TerminalNotificationUiState, TerminalNotificationAction> store() {
        return new UiStore<TerminalNotificationUiState, TerminalNotificationAction>(
            TerminalNotificationUiState.initial(), new TerminalNotificationReducer());
    }

    private static UiContext context() {
        return new UiContext(GlassTerminalTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0,false));
    }

    private static TerminalAppShell.Actions actions() {
        return new TerminalAppShell.Actions() {
            @Override public void navigate(String pageId) {}
            @Override public void refresh() {}
            @Override public void help() {}
            @Override public void back() {}
            @Override public void close() {}
        };
    }

    private static UiRect find(UiRuntime runtime, String key) {
        return find(runtime.getRoot(), key).getBounds();
    }

    private static com.jsirgalaxybase.ui2.core.UiNode find(com.jsirgalaxybase.ui2.core.UiNode node, String key) {
        if (node.getElement().getKey() != null && key.equals(node.getElement().getKey().getValue())) return node;
        for (com.jsirgalaxybase.ui2.core.UiNode child : node.getChildren()) {
            try { return find(child, key); } catch (AssertionError ignored) {}
        }
        throw new AssertionError("missing node " + key);
    }
}

package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.client.component.TerminalCustomMarketSectionState;
import com.jsirgalaxybase.terminal.client.component.TerminalExchangeMarketSectionState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiDocument;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;

public class TerminalAdvancedMarketDocumentsTest {
    @Test public void customAndExchangeFitAllProductionWindows() {
        for (int[] size : new int[][] {{350,193},{427,240},{620,340}}) {
            assertFits(standard(), size[0], size[1]);
            assertFits(custom(), size[0], size[1]);
            assertFits(exchange(), size[0], size[1]);
        }
    }

    @Test public void registryClaimsEveryMarketRoute() {
        assertTrue(TerminalPageRegistry.supports("market"));
        assertTrue(TerminalPageRegistry.supports("market_standardized"));
        assertTrue(TerminalPageRegistry.supports("market_custom"));
        assertTrue(TerminalPageRegistry.supports("market_exchange"));
        assertTrue(TerminalPageRegistry.supports("market_account_center"));
    }

    @Test public void standardHelpClosesWithEscapeAndDoesNotTrapTerminal() {
        final boolean[] invalidated = {false};
        TerminalStandardMarketDocument document = new TerminalStandardMarketDocument(
            new com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState(),
            TerminalHomeScreenModel.placeholder().withSelectedPageId("market_standardized"),
            shell(), market(), new Runnable() { @Override public void run() { invalidated[0] = true; } });
        UiRuntime runtime = new UiRuntime(document, context());
        runtime.setViewport(427, 240);
        document.openHelp();
        runtime.frame();
        assertTrue(runtime.hasModal());
        assertTrue(runtime.dispatch(UiEvent.key(UiKeyCode.ESCAPE)));
        assertTrue(invalidated[0]);
        runtime.invalidate();
        runtime.frame();
        assertTrue(!runtime.hasModal());
    }

    private static void assertFits(UiDocument document, int width, int height) {
        UiRuntime runtime = new UiRuntime(document, context());
        runtime.setViewport(width, height);
        runtime.frame();
        UiRect window = find(runtime.getRoot(), "terminal-window").getBounds();
        for (DrawCommand command : runtime.frame().ordered()) {
            if (command.getKind() != DrawCommand.Kind.TEXT) continue;
            UiRect bounds = command.getBounds();
            assertTrue(bounds.getX() >= window.getX());
            assertTrue(bounds.getY() >= window.getY());
            assertTrue(bounds.getRight() <= window.getRight());
            assertTrue(bounds.getBottom() <= window.getBottom());
        }
    }

    private static TerminalCustomMarketDocument custom() {
        return new TerminalCustomMarketDocument(new TerminalCustomMarketSectionState(),
            TerminalHomeScreenModel.placeholder().withSelectedPageId("market_custom"), shell(), market(), noop());
    }

    private static TerminalStandardMarketDocument standard() {
        return new TerminalStandardMarketDocument(
            new com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState(),
            TerminalHomeScreenModel.placeholder().withSelectedPageId("market_standardized"),
            shell(), market(), noop());
    }

    private static TerminalExchangeMarketDocument exchange() {
        return new TerminalExchangeMarketDocument(new TerminalExchangeMarketSectionState(),
            TerminalHomeScreenModel.placeholder().withSelectedPageId("market_exchange"), shell(), market(), noop());
    }

    private static TerminalStandardMarketDocument.Actions market() {
        return new TerminalStandardMarketDocument.Actions() {
            @Override public void sendMarket(TerminalActionType action, String payload) {}
        };
    }

    private static TerminalAppShell.Actions shell() {
        return new TerminalAppShell.Actions() {
            @Override public void navigate(String pageId) {}
            @Override public void refresh() {}
            @Override public void help() {}
            @Override public void back() {}
            @Override public void close() {}
        };
    }

    private static Runnable noop() { return new Runnable() { @Override public void run() {} }; }
    private static UiContext context() { return new UiContext(GlassTerminalTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0, false)); }
    private static com.jsirgalaxybase.ui2.core.UiNode find(com.jsirgalaxybase.ui2.core.UiNode node, String key) {
        if (node.getElement().getKey() != null && key.equals(node.getElement().getKey().getValue())) return node;
        for (com.jsirgalaxybase.ui2.core.UiNode child : node.getChildren()) {
            try { return find(child, key); } catch (AssertionError ignored) {}
        }
        throw new AssertionError("missing node " + key);
    }
}

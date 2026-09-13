package com.jsirgalaxybase.terminal.client.ui2;

import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.terminal.client.settings.TerminalAppearance;
import com.jsirgalaxybase.terminal.client.settings.TerminalPreferences;
import com.jsirgalaxybase.terminal.client.settings.TerminalSettingsAction;
import com.jsirgalaxybase.terminal.client.settings.TerminalSettingsState;
import com.jsirgalaxybase.terminal.client.settings.TerminalThemeRegistry;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.state.UiStore;
import com.jsirgalaxybase.ui2.terminal.TerminalActionPort;
import com.jsirgalaxybase.ui2.terminal.TerminalSettingsPort;
import com.jsirgalaxybase.ui2.terminal.TerminalSettingsVisualDocument;
import com.jsirgalaxybase.ui2.terminal.TerminalSettingsVisualModel;
import com.jsirgalaxybase.ui2.terminal.TerminalWindowProfile;

/** Minecraft state adapter around the platform-neutral production settings document. */
public final class TerminalSettingsDocument extends TerminalSettingsVisualDocument {
    private final Bridge bridge;

    public TerminalSettingsDocument(UiStore<TerminalSettingsState, TerminalSettingsAction> store,
        TerminalHomeScreenModel shellModel, TerminalAppShell.Actions shellActions) {
        this(new Bridge(store, shellModel), shellActions);
    }

    private TerminalSettingsDocument(Bridge bridge, final TerminalAppShell.Actions actions) {
        super(bridge, new TerminalActionPort() {
            @Override public void navigate(String pageId) { actions.navigate(pageId); }
            @Override public void refresh() { actions.refresh(); }
            @Override public void help() { actions.help(); }
            @Override public void back() { actions.back(); }
            @Override public void close() { actions.close(); }
        });
        this.bridge = bridge;
    }

    public void updateShellModel(TerminalHomeScreenModel value) { bridge.shellModel = value; }

    private static final class Bridge implements TerminalSettingsPort {
        private final UiStore<TerminalSettingsState, TerminalSettingsAction> store;
        private TerminalHomeScreenModel shellModel;

        private Bridge(UiStore<TerminalSettingsState, TerminalSettingsAction> store,
            TerminalHomeScreenModel shellModel) {
            if (store == null) throw new IllegalArgumentException("settings store is required");
            this.store = store;
            this.shellModel = shellModel;
        }

        @Override
        public TerminalSettingsVisualModel current() {
            TerminalSettingsState state = store.getState();
            TerminalPreferences preferences = state.getPreferences();
            List<TerminalSettingsVisualModel.ThemeChoice> themes =
                new ArrayList<TerminalSettingsVisualModel.ThemeChoice>();
            for (TerminalThemeRegistry.Entry entry : TerminalAppearance.INSTANCE.themes().entries()) {
                themes.add(new TerminalSettingsVisualModel.ThemeChoice(entry.getId(), entry.getLabel()));
            }
            return new TerminalSettingsVisualModel(TerminalVisualModelAdapter.shell(shellModel), themes,
                preferences.getThemeId(), TerminalWindowProfile.valueOf(preferences.getWindowSize().name()),
                preferences.isReducedMotion(), state.isHelpOpen());
        }

        @Override public void selectTheme(String id) { store.dispatch(TerminalSettingsAction.value(TerminalSettingsAction.Type.THEME, id)); }
        @Override public void selectWindow(TerminalWindowProfile profile) { store.dispatch(TerminalSettingsAction.value(TerminalSettingsAction.Type.WINDOW, profile.name())); }
        @Override public void toggleMotion() { store.dispatch(TerminalSettingsAction.of(TerminalSettingsAction.Type.TOGGLE_MOTION)); }
        @Override public void reset() { store.dispatch(TerminalSettingsAction.of(TerminalSettingsAction.Type.RESET)); }
        @Override public void openHelp() { store.dispatch(TerminalSettingsAction.of(TerminalSettingsAction.Type.OPEN_HELP)); }
        @Override public void closeHelp() { store.dispatch(TerminalSettingsAction.of(TerminalSettingsAction.Type.CLOSE_HELP)); }
    }
}

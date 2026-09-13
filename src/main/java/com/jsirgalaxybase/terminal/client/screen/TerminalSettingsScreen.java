package com.jsirgalaxybase.terminal.client.screen;

import net.minecraft.client.gui.GuiScreen;

import com.jsirgalaxybase.terminal.client.settings.TerminalAppearance;
import com.jsirgalaxybase.terminal.client.settings.TerminalPreferences;
import com.jsirgalaxybase.terminal.client.settings.TerminalSettingsAction;
import com.jsirgalaxybase.terminal.client.settings.TerminalSettingsReducer;
import com.jsirgalaxybase.terminal.client.settings.TerminalSettingsState;
import com.jsirgalaxybase.terminal.client.ui2.TerminalSettingsDocument;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiDocument;
import com.jsirgalaxybase.ui2.state.UiStore;

public final class TerminalSettingsScreen extends TerminalScreenV2Base {
    private final UiStore<TerminalSettingsState,TerminalSettingsAction> store;
    private TerminalSettingsDocument document;private TerminalPreferences applied;private boolean rebuildPending;
    public TerminalSettingsScreen(GuiScreen parent,TerminalHomeScreenModel model){
        super(parent,model);applied=TerminalAppearance.INSTANCE.preferences();
        store=new UiStore<TerminalSettingsState,TerminalSettingsAction>(new TerminalSettingsState(applied,false),new TerminalSettingsReducer());
        store.addListener(new UiStore.Listener<TerminalSettingsState>(){@Override public void onStateChanged(TerminalSettingsState state){
            if(!state.getPreferences().equals(applied)){applied=state.getPreferences();TerminalAppearance.INSTANCE.apply(applied);rebuildPending=true;}else invalidateUi();
        }});
    }
    public void applyShellModel(TerminalHomeScreenModel model){updateShellModel(model);if(document!=null)document.updateShellModel(shellModel());invalidateUi();}
    @Override public void updateScreen(){super.updateScreen();if(rebuildPending){rebuildPending=false;initGui();}}
    @Override protected UiDocument createDocument(){document=new TerminalSettingsDocument(store,shellModel(),shellActions());return document;}
    @Override protected void refreshPage(){invalidateUi();}
    @Override protected void showHelp(){store.dispatch(TerminalSettingsAction.of(TerminalSettingsAction.Type.OPEN_HELP));}
}

package com.jsirgalaxybase.client.ui2.lab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.jsirgalaxybase.client.ui2.MinecraftUiPlatform;
import com.jsirgalaxybase.client.ui2.host.ModernScreenHost;
import com.jsirgalaxybase.ui2.core.UiDocument;
import com.jsirgalaxybase.ui2.lab.UiLabAction;
import com.jsirgalaxybase.ui2.lab.UiLabDocument;
import com.jsirgalaxybase.ui2.lab.UiLabDiagnostics;
import com.jsirgalaxybase.ui2.lab.UiLabPage;
import com.jsirgalaxybase.ui2.lab.UiLabReducer;
import com.jsirgalaxybase.ui2.lab.UiLabState;
import com.jsirgalaxybase.ui2.state.UiStore;

/** Client-only platform laboratory. It never enters the production terminal route. */
public final class Ui2LabScreen extends ModernScreenHost {
    private final UiStore<UiLabState, UiLabAction> store = new UiStore<UiLabState, UiLabAction>(UiLabState.initial(), new UiLabReducer());
    private final UiLabDocument document = new UiLabDocument(store, new UiLabDiagnostics() {
        @Override public String summary(boolean reducedMotion) {
            return MinecraftUiPlatform.INSTANCE.diagnosticSummary(reducedMotion);
        }
    });

    public Ui2LabScreen(GuiScreen parent) {
        super(parent);
        store.addListener(new UiStore.Listener<UiLabState>() { @Override public void onStateChanged(UiLabState state) { invalidateUi(); } });
    }
    @Override protected UiDocument createDocument(){return document;}
    @Override protected boolean isReducedMotion(){return store.getState().isReducedMotion();}
    @Override protected boolean handleUnhandledKey(char typedChar,int keyCode){
        char key=Character.toLowerCase(typedChar);
        if(key=='d'){store.dispatch(UiLabAction.of(UiLabAction.Type.TOGGLE_DEBUG));return true;}
        if(key=='m'){store.dispatch(UiLabAction.of(UiLabAction.Type.TOGGLE_MOTION));return true;}
        if(key=='c'&&store.getState().getPage()==UiLabPage.INVENTORY){Minecraft.getMinecraft().displayGuiScreen(new Ui2LabContainerScreen(this));return true;}
        return false;
    }
}

package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.terminal.TerminalBankActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.state.UiStore;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;

public class TerminalBankDocumentTest {
    @Test public void draftSanitizesInputAndReducerBuildsCompletePayload() {
        UiStore<BankUiState,BankUiAction> store=store();
        for(char c:"Player_1!".toCharArray())store.dispatch(BankUiAction.input(BankUiState.Field.TARGET,c));
        for(char c:"12x34".toCharArray())store.dispatch(BankUiAction.input(BankUiState.Field.AMOUNT,c));
        assertEquals("Player_1",store.getState().getTarget());
        assertEquals("1234",store.getState().getAmount());
        assertTrue(store.getState().isComplete());
        assertEquals(1234L,store.getState().payload().parseAmount());
        store.dispatch(BankUiAction.backspace(BankUiState.Field.AMOUNT));
        assertEquals("123",store.getState().getAmount());
    }

    @Test public void bankPageFitsAllProductionWindows() {
        for(int[] size:new int[][]{{350,193},{427,240},{620,340}}){
            UiRuntime runtime=new UiRuntime(document(store()),context());runtime.setViewport(size[0],size[1]);
            UiRect window=find(runtime,"terminal-window");
            for(DrawCommand command:runtime.frame().ordered()){
                if(command.getKind()!=DrawCommand.Kind.TEXT)continue;
                UiRect bounds=command.getBounds();
                assertTrue(bounds.getX()>=window.getX());assertTrue(bounds.getY()>=window.getY());
                assertTrue(bounds.getRight()<=window.getRight());assertTrue(bounds.getBottom()<=window.getBottom());
            }
        }
    }

    @Test public void confirmationStateIsExplicitAndCancelDoesNotSubmit() {
        UiStore<BankUiState,BankUiAction> store=store();
        store.dispatch(BankUiAction.of(BankUiAction.Type.OPEN_CONFIRM));
        assertTrue(store.getState().isConfirmOpen());
        store.dispatch(BankUiAction.of(BankUiAction.Type.CLOSE_CONFIRM));
        assertFalse(store.getState().isConfirmOpen());
    }

    private static UiStore<BankUiState,BankUiAction> store(){return new UiStore<BankUiState,BankUiAction>(BankUiState.initial(TerminalBankSectionModel.TransferFormModel.placeholder()),new BankUiReducer());}
    private static TerminalBankDocument document(UiStore<BankUiState,BankUiAction> store){return new TerminalBankDocument(store,TerminalHomeScreenModel.placeholder().withSelectedPageId("bank"),actions(),new TerminalBankDocument.Actions(){public void openAccount(TerminalBankActionPayload payload){}public void confirmTransfer(TerminalBankActionPayload payload){}});}
    private static TerminalAppShell.Actions actions(){return new TerminalAppShell.Actions(){public void navigate(String pageId){}public void refresh(){}public void help(){}public void back(){}public void close(){}};}
    private static UiContext context(){return new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,false));}
    private static UiRect find(UiRuntime runtime,String key){runtime.frame();return find(runtime.getRoot(),key).getBounds();}
    private static com.jsirgalaxybase.ui2.core.UiNode find(com.jsirgalaxybase.ui2.core.UiNode node,String key){if(node.getElement().getKey()!=null&&key.equals(node.getElement().getKey().getValue()))return node;for(com.jsirgalaxybase.ui2.core.UiNode child:node.getChildren()){try{return find(child,key);}catch(AssertionError ignored){}}throw new AssertionError("missing "+key);}
}

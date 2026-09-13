package com.jsirgalaxybase.terminal.client.ui2;

import java.util.List;

import com.jsirgalaxybase.terminal.TerminalBankActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.component.ComponentUiDocument;
import com.jsirgalaxybase.ui2.component.StandardWidgets;
import com.jsirgalaxybase.ui2.component.UiActionHandler;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiInputHandler;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;
import com.jsirgalaxybase.ui2.state.UiStore;

/** Modern bank view; only its form draft is local. All balances and writes remain server-owned. */
public final class TerminalBankDocument extends ComponentUiDocument implements TerminalPageDocument {
    public interface Actions {
        void openAccount(TerminalBankActionPayload payload);
        void confirmTransfer(TerminalBankActionPayload payload);
    }

    private final UiStore<BankUiState, BankUiAction> store;
    private final TerminalAppShell.Actions shellActions;
    private final Actions actions;
    private TerminalHomeScreenModel model;
    private int width, height;

    public TerminalBankDocument(UiStore<BankUiState, BankUiAction> store, TerminalHomeScreenModel model,
        TerminalAppShell.Actions shellActions, Actions actions) {
        super(StandardWidgets.create());
        this.store = store; this.model = model; this.shellActions = shellActions; this.actions = actions;
    }

    @Override public void update(TerminalHomeScreenModel value) { model = value; }
    @Override public void openHelp() { store.dispatch(BankUiAction.of(BankUiAction.Type.OPEN_HELP)); }

    @Override public UiElement build(UiContext context) {
        TerminalBankSectionModel bank = bank();
        BankUiState state = store.getState();
        UiElement account = card("bank-account", "账户", bank.getAccountStatus().getAccountLabel() + " · "
            + bank.getAccountStatus().getAccountNo(), bank.getAccountStatus().getServiceState(),
            button("bank-open", bank.getAccountStatus().isOpened() ? "账户已存在" : "立即开户",
                bank.getAccountStatus().isOpenAllowed(), new Runnable() {
                    @Override public void run() { actions.openAccount(store.getState().payload()); }
                }));
        UiElement balance = card("bank-balance", "余额", bank.getBalanceSummary().getPlayerBalance(),
            "公开储备 " + bank.getBalanceSummary().getExchangeBalance() + " · "
                + bank.getBalanceSummary().getExchangeStatus(), null);
        UiElement transfer = UiElement.type("Card").key("bank-transfer")
            .child(label("bank-transfer-title", "玩家转账", "caption", 1, true, "text"))
            .child(field("bank-target", BankUiState.Field.TARGET, state.getTarget(), "收款玩家名"))
            .child(field("bank-amount", BankUiState.Field.AMOUNT, state.getAmount(), "金额"))
            .child(field("bank-comment", BankUiState.Field.COMMENT, state.getComment(), "备注（可选）"))
            .child(button("bank-confirm", "核对转账", bank.getTransferForm().isTransferEnabled() && state.isComplete(),
                new Runnable() { @Override public void run() {
                    store.dispatch(BankUiAction.of(BankUiAction.Type.OPEN_CONFIRM));
                }})).build();
        String ledger = join(bank.getPlayerLedgerLines(), " · ", 2);
        UiElement feedback = card("bank-feedback", bank.getActionFeedback().getTitle(),
            bank.getActionFeedback().getBody(), "近期流水 · " + ledger, null);
        UiElement base = UiElement.type("Column").key("bank-base")
            .child(label("bank-title", "银河银行", "section", 1, true, "text"))
            .child(UiElement.type("Row").key("bank-summary").child(account).child(balance).build())
            .child(transfer).child(feedback).build();
        UiElement.Builder root = UiElement.type("Stack").key("bank-page-root").child(base);
        if (state.isConfirmOpen()) root.child(UiElement.type("Dialog").key("bank-confirm-dialog")
            .prop(StandardWidgets.TEXT, "确认转账")
            .prop(StandardWidgets.DETAIL, "向 " + state.getTarget() + " 转账 " + state.getAmount()
                + " STARCOIN。确认后由服务端再次校验余额与账户状态。")
            .prop(StandardWidgets.CANCEL_TEXT, "取消").prop(StandardWidgets.CONFIRM_TEXT, "确认转账").build());
        if (state.isHelpOpen()) root.child(UiElement.type("Dialog").key("bank-help")
            .prop(StandardWidgets.TEXT, "银行帮助")
            .prop(StandardWidgets.DETAIL, "余额与流水来自服务端。客户端只保存尚未提交的表单；提交时服务端会重新校验。")
            .build());
        return TerminalAppShell.build(model, root.build(), shellActions, "银河银行");
    }

    @Override public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
        width = viewport.getWidth(); height = viewport.getHeight();
        boolean compact = TerminalWindowMetrics.compute(viewport).getBounds().getHeight() <= 220;
        LayoutSpec account = cardLayout("bank-account", "bank-account-title", "bank-account-value",
            "bank-account-detail", "bank-open", compact ? 40 : 48);
        LayoutSpec balance = cardLayout("bank-balance", "bank-balance-title", "bank-balance-value",
            "bank-balance-detail", null, compact ? 40 : 48);
        LayoutSpec summary = LayoutSpec.of("bank-summary", LayoutKind.ROW).preferred(0, compact ? 40 : 48).gap(3)
            .child(account).child(balance).build();
        LayoutSpec transfer;
        if (compact) {
            transfer = LayoutSpec.of("bank-transfer", LayoutKind.ROW).preferred(0,24)
                .padding(new Insets(3,4,3,4)).gap(2)
                .child(LayoutSpec.of("bank-transfer-title", LayoutKind.LEAF).preferred(38,18).build())
                .child(LayoutSpec.of("bank-target", LayoutKind.LEAF).flex(2F).build())
                .child(LayoutSpec.of("bank-amount", LayoutKind.LEAF).preferred(46,18).build())
                .child(LayoutSpec.of("bank-comment", LayoutKind.LEAF).flex(2F).build())
                .child(LayoutSpec.of("bank-confirm", LayoutKind.LEAF).preferred(60,18).build()).build();
        } else {
            transfer = LayoutSpec.of("bank-transfer", LayoutKind.COLUMN).preferred(0,92)
                .padding(new Insets(3,4,3,4)).gap(2)
                .child(LayoutSpec.of("bank-transfer-title", LayoutKind.LEAF).preferred(0,10).build())
                .child(LayoutSpec.of("bank-target", LayoutKind.LEAF).preferred(0,16).build())
                .child(LayoutSpec.of("bank-amount", LayoutKind.LEAF).preferred(0,16).build())
                .child(LayoutSpec.of("bank-comment", LayoutKind.LEAF).preferred(0,16).build())
                .child(LayoutSpec.of("bank-confirm", LayoutKind.LEAF).preferred(70,18).build()).build();
        }
        LayoutSpec feedback = LayoutSpec.of("bank-feedback", LayoutKind.COLUMN).flex(1F)
            .padding(new Insets(3,4,3,4)).gap(1)
            .child(LayoutSpec.of("bank-feedback-title", LayoutKind.LEAF).preferred(0,10).build())
            .child(LayoutSpec.of("bank-feedback-value", LayoutKind.LEAF).preferred(0,12).build())
            .child(LayoutSpec.of("bank-feedback-detail", LayoutKind.LEAF).flex(1F).build()).build();
        LayoutSpec base = LayoutSpec.of("bank-base", LayoutKind.COLUMN).padding(new Insets(3,4,3,4)).gap(2)
            .child(LayoutSpec.of("bank-title", LayoutKind.LEAF).preferred(0,14).build())
            .child(summary).child(transfer).child(feedback).build();
        LayoutSpec.Builder page = LayoutSpec.of("bank-page-root", LayoutKind.STACK).flex(1F).child(base);
        if (store.getState().isConfirmOpen()) page.child(LayoutSpec.of("bank-confirm-dialog", LayoutKind.LEAF).build());
        if (store.getState().isHelpOpen()) page.child(LayoutSpec.of("bank-help", LayoutKind.LEAF).build());
        return TerminalAppShell.layout(viewport, model, page.build());
    }

    @Override public UiInputNode modalInput(UiNode root, UiContext context) {
        final boolean confirm = store.getState().isConfirmOpen();
        if (!confirm && !store.getState().isHelpOpen()) return null;
        return new UiInputNode(confirm ? "bank-confirm-dialog" : "bank-help", new UiRect(0,0,width,height), true,
            new UiInputHandler() {
                @Override public InputResult handle(UiInputNode target, UiEvent event) {
                    if (event.getPhase() != UiEvent.Phase.TARGET) return InputResult.PASS;
                    if (event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.ENTER && confirm) {
                        actions.confirmTransfer(store.getState().payload());
                        store.dispatch(BankUiAction.of(BankUiAction.Type.CLOSE_CONFIRM));
                        return InputResult.CONSUMED;
                    }
                    if (event.getType() == UiEvent.Type.POINTER_DOWN && confirm) {
                        int dialogWidth=Math.min(230,Math.max(80,width-40));int dialogHeight=Math.min(90,Math.max(50,height-30));
                        int dialogX=(width-dialogWidth)/2,dialogY=(height-dialogHeight)/2;
                        int buttonY=dialogY+dialogHeight-22,half=Math.max(0,(dialogWidth-28)/2);
                        UiRect confirmBounds=new UiRect(dialogX+10+half+8,buttonY,half,16);
                        if(confirmBounds.contains(event.getX(),event.getY())){
                            actions.confirmTransfer(store.getState().payload());
                        }
                        store.dispatch(BankUiAction.of(BankUiAction.Type.CLOSE_CONFIRM));
                        return InputResult.CONSUMED;
                    }
                    if ((event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.ESCAPE)
                        || event.getType() == UiEvent.Type.POINTER_DOWN) {
                        store.dispatch(BankUiAction.of(confirm ? BankUiAction.Type.CLOSE_CONFIRM : BankUiAction.Type.CLOSE_HELP));
                        return InputResult.CONSUMED;
                    }
                    return InputResult.PASS;
                }
            });
    }

    private UiElement field(String key, final BankUiState.Field field, String value, String placeholder) {
        return UiElement.type("TextField").key(key).prop(StandardWidgets.TEXT, value)
            .prop(StandardWidgets.PLACEHOLDER, placeholder)
            .prop(StandardWidgets.FOCUSED, store.getState().getField() == field)
            .prop(StandardWidgets.ACTION, new UiActionHandler() {
                @Override public InputResult handle(UiEvent event) {
                    if (event.getType() == UiEvent.Type.POINTER_DOWN) store.dispatch(BankUiAction.focus(field));
                    else if (event.getType() == UiEvent.Type.TEXT_INPUT && !Character.isISOControl(event.getTypedChar()))
                        store.dispatch(BankUiAction.input(field, event.getTypedChar()));
                    else if (event.getType() == UiEvent.Type.KEY_DOWN
                        && (event.getKeyCode() == UiKeyCode.BACKSPACE || event.getKeyCode() == UiKeyCode.DELETE))
                        store.dispatch(BankUiAction.backspace(field));
                    return InputResult.CONSUMED;
                }
            }).build();
    }

    private static UiElement card(String key, String title, String value, String detail, UiElement action) {
        UiElement.Builder card = UiElement.type("Card").key(key)
            .child(label(key+"-title", title, "caption", 1, true, "text"))
            .child(label(key+"-value", value, "caption", 1, false, "text"))
            .child(label(key+"-detail", detail, "caption", 1, false, "textMuted"));
        if (action != null) card.child(action);
        return card.build();
    }
    private static UiElement button(String key, String text, boolean enabled, final Runnable run) {
        return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT,text)
            .prop(StandardWidgets.ENABLED,enabled).prop(StandardWidgets.ACTION,new UiActionHandler(){
                @Override public InputResult handle(UiEvent event){run.run();return InputResult.CONSUMED;}
            }).build();
    }
    private static UiElement label(String key,String text,String role,int lines,boolean bold,String tone){
        return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.TEXT_ROLE,role)
            .prop(StandardWidgets.MAX_LINES,lines).prop(StandardWidgets.BOLD,bold).prop(StandardWidgets.TONE,tone).build();
    }
    private static LayoutSpec cardLayout(String key,String title,String value,String detail,String action,int height){
        LayoutSpec.Builder result=LayoutSpec.of(key,LayoutKind.COLUMN).flex(1F).preferred(0,height)
            .padding(new Insets(3,4,3,4)).gap(1).child(LayoutSpec.of(title,LayoutKind.LEAF).preferred(0,10).build())
            .child(LayoutSpec.of(value,LayoutKind.LEAF).preferred(0,11).build())
            .child(LayoutSpec.of(detail,LayoutKind.LEAF).flex(1F).build());
        if(action!=null)result.child(LayoutSpec.of(action,LayoutKind.LEAF).preferred(54,14).build());
        return result.build();
    }
    private TerminalBankSectionModel bank(){TerminalHomeScreenModel.PageSnapshotModel page=model.getPageSnapshot("bank");return page.hasBankSectionModel()?page.getBankSectionModel():TerminalBankSectionModel.placeholder();}
    private static String join(List<String> values,String separator,int limit){StringBuilder out=new StringBuilder();for(int i=0;i<values.size()&&i<limit;i++){if(i>0)out.append(separator);out.append(values.get(i));}return out.toString();}
}

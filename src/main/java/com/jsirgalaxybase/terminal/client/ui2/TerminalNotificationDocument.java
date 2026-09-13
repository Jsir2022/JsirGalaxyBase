package com.jsirgalaxybase.terminal.client.ui2;

import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.terminal.client.settings.TerminalNotificationAction;
import com.jsirgalaxybase.terminal.client.settings.TerminalNotificationUiState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalNotificationCenterModel;
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
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;
import com.jsirgalaxybase.ui2.state.UiStore;

/** Player-scoped modern notification center with bounded local filtering and pagination. */
public final class TerminalNotificationDocument extends ComponentUiDocument implements TerminalPageDocument {
    public interface TargetHandler { void open(String pageId,String recordId); }
    private static final int PAGE_SIZE=2;
    private final UiStore<TerminalNotificationUiState,TerminalNotificationAction> store;
    private final TerminalAppShell.Actions shellActions;private final TargetHandler targets;
    private TerminalHomeScreenModel model;private int width,height;
    public TerminalNotificationDocument(UiStore<TerminalNotificationUiState,TerminalNotificationAction> store,TerminalHomeScreenModel model,TerminalAppShell.Actions shellActions,TargetHandler targets){
        super(StandardWidgets.create());if(store==null||shellActions==null||targets==null)throw new IllegalArgumentException("notification dependencies are required");this.store=store;this.model=model;this.shellActions=shellActions;this.targets=targets;
    }
    @Override public void update(TerminalHomeScreenModel value){model=value;clampPage();}
    @Override public void openHelp(){store.dispatch(TerminalNotificationAction.of(TerminalNotificationAction.Type.OPEN_HELP));}
    @Override public UiElement build(UiContext context){
        TerminalNotificationCenterModel center=center();List<TerminalNotificationCenterModel.EntryModel> filtered=filtered();int page=Math.min(store.getState().getPage(),pages(filtered)-1),start=page*PAGE_SIZE,end=Math.min(filtered.size(),start+PAGE_SIZE);
        UiElement filters=UiElement.type("Row").key("notifications-filters")
            .child(filter("notifications-all","全部",TerminalNotificationUiState.Severity.ALL)).child(filter("notifications-error","错误",TerminalNotificationUiState.Severity.ERROR))
            .child(filter("notifications-warning","警告",TerminalNotificationUiState.Severity.WARNING)).child(filter("notifications-success","成功",TerminalNotificationUiState.Severity.SUCCESS))
            .child(filter("notifications-info","信息",TerminalNotificationUiState.Severity.INFO)).build();
        UiElement.Builder list=UiElement.type("Column").key("notifications-list");
        if(start>=end)list.child(UiElement.type("EmptyState").key("notifications-empty").prop(StandardWidgets.TEXT,"当前筛选下没有通知").build());
        for(int i=start;i<end;i++){final TerminalNotificationCenterModel.EntryModel entry=filtered.get(i);String tone=tone(entry.getSeverityName());list.child(UiElement.type("Card").key("notification-row-"+i).prop(StandardWidgets.TONE,tone).prop(StandardWidgets.ACTION,new UiActionHandler(){@Override public InputResult handle(UiEvent event){targets.open(entry.getTargetPageId(),entry.getTargetRecordId());return InputResult.CONSUMED;}})
            .child(label("notification-title-"+i,entry.getTitle(),"body",1,true,tone))
            .child(label("notification-body-"+i,entry.getBody(),"caption",1,false,"textMuted"))
            .child(label("notification-meta-"+i,source(entry)+" · "+entry.getSeverityName()+(entry.getOccurrences()>1?" · ×"+entry.getOccurrences():""),"caption",1,false,"textMuted")).build());}
        UiElement pager=UiElement.type("Row").key("notifications-pager")
            .child(button("notifications-prev","‹",page>0,TerminalNotificationAction.page(page-1)))
            .child(label("notifications-page",(page+1)+" / "+pages(filtered),"caption",1,false,"text"))
            .child(button("notifications-next","›",page+1<pages(filtered),TerminalNotificationAction.page(page+1))).build();
        UiElement base=UiElement.type("Column").key("notifications-base")
            .child(label("notifications-title","通知中心","section",1,true,"text"))
            .child(label("notifications-status",center.getServiceState()+" · 保留 "+center.getRetainedEntries()+" 条","caption",1,false,"textMuted"))
            .child(filters).child(list.build()).child(pager).build();
        UiElement.Builder pageRoot=UiElement.type("Stack").key("notifications-page-root").child(base);
        if(store.getState().isHelpOpen())pageRoot.child(UiElement.type("Dialog").key("notifications-help").prop(StandardWidgets.TEXT,"通知中心").prop(StandardWidgets.DETAIL,"这里只显示当前玩家的服务端通知。选择卡片可进入对应业务页面；筛选和分页只在客户端进行。").build());
        return TerminalAppShell.build(model,pageRoot.build(),shellActions,"银河通知中心");
    }
    @Override public LayoutSpec layout(UiElement root,UiSize viewport,UiContext context){
        width=viewport.getWidth();height=viewport.getHeight();boolean compact=TerminalWindowMetrics.compute(viewport).getBounds().getHeight()<=220;int row=compact?42:48;
        LayoutSpec.Builder list=LayoutSpec.of("notifications-list",LayoutKind.COLUMN).flex(1F).gap(2);List<TerminalNotificationCenterModel.EntryModel> entries=filtered();int page=Math.min(store.getState().getPage(),pages(entries)-1),start=page*PAGE_SIZE,end=Math.min(entries.size(),start+PAGE_SIZE);
        if(start>=end)list.child(LayoutSpec.of("notifications-empty",LayoutKind.LEAF).flex(1F).build());
        for(int i=start;i<end;i++)list.child(LayoutSpec.of("notification-row-"+i,LayoutKind.COLUMN).preferred(0,row).padding(new Insets(3,5,3,5)).gap(1).child(LayoutSpec.of("notification-title-"+i,LayoutKind.LEAF).preferred(0,14).build()).child(LayoutSpec.of("notification-body-"+i,LayoutKind.LEAF).preferred(0,12).build()).child(LayoutSpec.of("notification-meta-"+i,LayoutKind.LEAF).preferred(0,12).build()).build());
        LayoutSpec base=LayoutSpec.of("notifications-base",LayoutKind.COLUMN).padding(new Insets(4,5,4,5)).gap(2)
            .child(LayoutSpec.of("notifications-title",LayoutKind.LEAF).preferred(0,16).build()).child(LayoutSpec.of("notifications-status",LayoutKind.LEAF).preferred(0,12).build())
            .child(fiveFilters(compact?16:19)).child(list.build()).child(LayoutSpec.of("notifications-pager",LayoutKind.ROW).preferred(0,18).gap(3).child(LayoutSpec.of("notifications-prev",LayoutKind.LEAF).preferred(26,18).build()).child(LayoutSpec.of("notifications-page",LayoutKind.LEAF).flex(1F).build()).child(LayoutSpec.of("notifications-next",LayoutKind.LEAF).preferred(26,18).build()).build()).build();
        LayoutSpec.Builder pageLayout = LayoutSpec.of("notifications-page-root", LayoutKind.STACK).flex(1F)
            .child(base);
        if (store.getState().isHelpOpen()) {
            pageLayout.child(LayoutSpec.of("notifications-help", LayoutKind.LEAF).build());
        }
        return TerminalAppShell.layout(viewport, model, pageLayout.build());
    }
    @Override public UiInputNode modalInput(UiNode root,UiContext context){if(!store.getState().isHelpOpen())return null;return new UiInputNode("notifications-help",new UiRect(0,0,width,height),false,new UiInputHandler(){@Override public InputResult handle(UiInputNode target,UiEvent event){if((event.getPhase()==UiEvent.Phase.TARGET&&event.getType()==UiEvent.Type.POINTER_DOWN)||(event.getType()==UiEvent.Type.KEY_DOWN&&event.getKeyCode()==com.jsirgalaxybase.ui2.input.UiKeyCode.ESCAPE)){store.dispatch(TerminalNotificationAction.of(TerminalNotificationAction.Type.CLOSE_HELP));return InputResult.CONSUMED;}return InputResult.PASS;}});}
    private UiElement filter(String key,String text,TerminalNotificationUiState.Severity severity){return UiElement.type("Tab").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.SELECTED,store.getState().getSeverity()==severity).prop(StandardWidgets.ACTION,action(TerminalNotificationAction.severity(severity))).build();}
    private UiElement button(String key,String text,boolean enabled,TerminalNotificationAction action){return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.ENABLED,enabled).prop(StandardWidgets.ACTION,action(action)).build();}
    private UiActionHandler action(final TerminalNotificationAction action){return new UiActionHandler(){@Override public InputResult handle(UiEvent event){store.dispatch(action);return InputResult.CONSUMED;}};}
    private static UiElement label(String key,String text,String role,int lines,boolean bold,String tone){return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.TEXT_ROLE,role).prop(StandardWidgets.MAX_LINES,lines).prop(StandardWidgets.BOLD,bold).prop(StandardWidgets.TONE,tone).build();}
    private LayoutSpec fiveFilters(int height){return LayoutSpec.of("notifications-filters",LayoutKind.ROW).preferred(0,height).gap(2).child(LayoutSpec.of("notifications-all",LayoutKind.LEAF).flex(1F).build()).child(LayoutSpec.of("notifications-error",LayoutKind.LEAF).flex(1F).build()).child(LayoutSpec.of("notifications-warning",LayoutKind.LEAF).flex(1F).build()).child(LayoutSpec.of("notifications-success",LayoutKind.LEAF).flex(1F).build()).child(LayoutSpec.of("notifications-info",LayoutKind.LEAF).flex(1F).build()).build();}
    private TerminalNotificationCenterModel center(){TerminalHomeScreenModel.PageSnapshotModel page=model.getPageSnapshot("notifications");return page.hasNotificationCenterModel()?page.getNotificationCenterModel():TerminalNotificationCenterModel.empty();}
    private List<TerminalNotificationCenterModel.EntryModel> filtered(){List<TerminalNotificationCenterModel.EntryModel> result=new ArrayList<TerminalNotificationCenterModel.EntryModel>();for(TerminalNotificationCenterModel.EntryModel entry:center().getEntries())if(store.getState().getSeverity()==TerminalNotificationUiState.Severity.ALL||store.getState().getSeverity().name().equalsIgnoreCase(entry.getSeverityName()))result.add(entry);return result;}
    private void clampPage(){int maximum=pages(filtered())-1;if(store.getState().getPage()>maximum)store.dispatch(TerminalNotificationAction.page(maximum));}
    private static int pages(List<?> values){return Math.max(1,(values.size()+PAGE_SIZE-1)/PAGE_SIZE);}
    private static String source(TerminalNotificationCenterModel.EntryModel entry){String value=entry.getSourceId();return value==null||value.isEmpty()?"终端":value;}
    private static String tone(String severity){if("ERROR".equalsIgnoreCase(severity))return "danger";if("WARNING".equalsIgnoreCase(severity))return "warning";if("SUCCESS".equalsIgnoreCase(severity))return "success";return "accent";}
}

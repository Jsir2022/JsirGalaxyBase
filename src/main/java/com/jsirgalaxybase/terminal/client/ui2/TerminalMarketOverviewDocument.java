package com.jsirgalaxybase.terminal.client.ui2;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.ui.TerminalPage;
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

/** Modern market launchpad; each card routes to the existing server-backed workflow. */
public final class TerminalMarketOverviewDocument extends ComponentUiDocument implements TerminalPageDocument {
    private final TerminalAppShell.Actions shellActions;private final TerminalNotificationDocument.TargetHandler routes;
    private TerminalHomeScreenModel model;private boolean helpOpen;private int width,height;
    public TerminalMarketOverviewDocument(TerminalHomeScreenModel model,TerminalAppShell.Actions shellActions,TerminalNotificationDocument.TargetHandler routes){super(StandardWidgets.create());this.model=model;this.shellActions=shellActions;this.routes=routes;}
    @Override public void update(TerminalHomeScreenModel value){model=value;}
    @Override public void openHelp(){helpOpen=true;}
    @Override public UiElement build(UiContext context){TerminalMarketSectionModel market=model.getPageSnapshot("market").getMarketSectionModel();if(market==null)market=TerminalMarketSectionModel.placeholder("market");UiElement cards=UiElement.type("Stack").key("market-overview-cards")
        .child(routeCard("market-overview-standard","标准商品","连续交易、订单簿与限价/市价订单",TerminalPage.MARKET_STANDARDIZED.getId(),"accent"))
        .child(routeCard("market-overview-custom","定制商品","单件挂牌、购买与交付",TerminalPage.MARKET_CUSTOM.getId(),"success"))
        .child(routeCard("market-overview-exchange","汇率市场","货币兑换与公开报价",TerminalPage.MARKET_EXCHANGE.getId(),"warning"))
        .child(routeCard("market-overview-account","订单与交付","委托、成交、待收货与恢复",TerminalPage.MARKET_ACCOUNT_CENTER.getId(),"text"))
        .build();UiElement base=UiElement.type("Column").key("market-overview-base").child(label("market-overview-title","银河市场","section",true,"text"))
        .child(label("market-overview-status",market.getServiceState()+" · "+market.getSummaryNotice(),"caption",false,"textMuted"))
        .child(cards).build();UiElement.Builder page=UiElement.type("Stack").key("market-overview-root").child(base);if(helpOpen)page.child(UiElement.type("Dialog").key("market-overview-help").prop(StandardWidgets.TEXT,"市场导航").prop(StandardWidgets.DETAIL,"选择一个市场工作区。交易、资金冻结、仓储交付和恢复仍全部由服务端权威处理。").build());return TerminalAppShell.build(model,page.build(),shellActions,"银河市场");}
    private UiElement routeCard(String key,String title,String detail,final String pageId,String tone){return UiElement.type("Card").key(key).prop(StandardWidgets.TONE,tone).prop(StandardWidgets.ACTION,new UiActionHandler(){public InputResult handle(UiEvent event){routes.open(pageId,"");return InputResult.CONSUMED;}}).child(label(key+"-title",title,"body",true,tone)).child(label(key+"-detail",detail,"caption",false,"textMuted")).build();}
    @Override public LayoutSpec layout(UiElement root,UiSize viewport,UiContext context){width=viewport.getWidth();height=viewport.getHeight();boolean compact=TerminalWindowMetrics.compute(viewport).getBounds().getHeight()<=220;LayoutSpec.Builder cards=LayoutSpec.of("market-overview-cards",LayoutKind.GRID).columns(2).flex(1F).gap(compact?2:4);String[] keys={"market-overview-standard","market-overview-custom","market-overview-exchange","market-overview-account"};for(String key:keys)cards.child(LayoutSpec.of(key,LayoutKind.COLUMN).padding(new Insets(5,6,5,6)).gap(2).child(LayoutSpec.of(key+"-title",LayoutKind.LEAF).preferred(0,14).build()).child(LayoutSpec.of(key+"-detail",LayoutKind.LEAF).flex(1F).build()).build());LayoutSpec base=LayoutSpec.of("market-overview-base",LayoutKind.COLUMN).padding(new Insets(5,6,5,6)).gap(compact?2:4).child(LayoutSpec.of("market-overview-title",LayoutKind.LEAF).preferred(0,16).build()).child(LayoutSpec.of("market-overview-status",LayoutKind.LEAF).preferred(0,12).build()).child(cards.build()).build();LayoutSpec.Builder page=LayoutSpec.of("market-overview-root",LayoutKind.STACK).flex(1F).child(base);if(helpOpen)page.child(LayoutSpec.of("market-overview-help",LayoutKind.LEAF).build());return TerminalAppShell.layout(viewport,model,page.build());}
    @Override public UiInputNode modalInput(UiNode root,UiContext context){if(!helpOpen)return null;return new UiInputNode("market-overview-help",new UiRect(0,0,width,height),false,new UiInputHandler(){public InputResult handle(UiInputNode target,UiEvent event){if((event.getType()==UiEvent.Type.KEY_DOWN&&event.getKeyCode()==com.jsirgalaxybase.ui2.input.UiKeyCode.ESCAPE)||event.getType()==UiEvent.Type.POINTER_DOWN){helpOpen=false;return InputResult.CONSUMED;}return InputResult.PASS;}});}
    private static UiElement label(String key,String text,String role,boolean bold,String tone){return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.TEXT_ROLE,role).prop(StandardWidgets.MAX_LINES,1).prop(StandardWidgets.BOLD,bold).prop(StandardWidgets.TONE,tone).build();}
}

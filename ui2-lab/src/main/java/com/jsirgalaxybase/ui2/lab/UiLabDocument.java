package com.jsirgalaxybase.ui2.lab;

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
import com.jsirgalaxybase.ui2.render.UiLayer;
import com.jsirgalaxybase.ui2.state.UiStore;

/** UI Lab rendered through the same production widget registry used by business pages. */
public final class UiLabDocument extends ComponentUiDocument {
    private final UiStore<UiLabState,UiLabAction> store;
    private final UiLabDiagnostics diagnostics;
    private int width;
    private int height;

    public UiLabDocument(UiStore<UiLabState,UiLabAction> store){this(store,UiLabDiagnostics.NONE);}
    public UiLabDocument(UiStore<UiLabState,UiLabAction> store,UiLabDiagnostics diagnostics){
        super(StandardWidgets.create());
        if(store==null)throw new IllegalArgumentException("store is required");
        this.store=store;this.diagnostics=diagnostics==null?UiLabDiagnostics.NONE:diagnostics;
    }

    @Override public UiElement build(UiContext context){
        UiLabState state=store.getState();
        UiElement top=UiElement.type("Surface").key("lab-top")
            .child(label("lab-title","Galaxy UI 2 Lab"))
            .child(UiElement.type("Label").key("lab-metrics")
                .prop(StandardWidgets.TEXT,"响应式 · "+(state.isReducedMotion()?"减少动效":"平滑动效"))
                .prop(StandardWidgets.TEXT_ROLE,"caption").prop(StandardWidgets.MAX_LINES,1).build()).build();
        UiElement.Builder nav=UiElement.type("Surface").key("lab-nav");
        for(UiLabPage page:UiLabPage.values())nav.child(UiElement.type("Tab").key("nav-"+page.name())
            .prop(StandardWidgets.TEXT,page.getLabel()).prop(StandardWidgets.SELECTED,page==state.getPage())
            .prop(StandardWidgets.ACTION,action(UiLabAction.select(page))).build());
        nav.child(label("lab-hint-f8","F8 UI Lab")).child(label("lab-hint-esc","Esc 返回"));
        UiElement body=UiElement.type("Card").key("lab-body").child(page(state)).build();
        UiElement workspace=UiElement.type("Row").key("lab-workspace").child(nav.build()).child(body).build();
        UiElement base=UiElement.type("Column").key("lab-base").child(top).child(workspace).build();
        UiElement.Builder root=UiElement.type("Root").key("root").child(base);
        if(state.isModal())root.child(UiElement.type("Dialog").key("lab-dialog")
            .prop(StandardWidgets.TEXT,"确认执行此操作？")
            .prop(StandardWidgets.DETAIL,"模态层已阻断底层控件与原生槽位。").build());
        return root.build();
    }

    private UiElement page(UiLabState state){
        UiElement.Builder page=UiElement.type("Column").key("lab-page")
            .child(label("page-title",title(state.getPage())))
            .child(label("page-subtitle",subtitle(state.getPage())));
        switch(state.getPage()){
            case OVERVIEW:
                page.child(UiElement.type("Row").key("overview-cards")
                    .child(badge("overview-components","组件 · 现代","accent"))
                    .child(badge("overview-state","状态 · READY","success"))
                    .child(badge("overview-font","字体 · CJK","warning")).build())
                    .child(label("overview-numeric","12,800.50 · 平滑中文与等宽数字"))
                    .child(label("overview-diagnostics",diagnostics.summary(state.isReducedMotion())));break;
            case CONTROLS:
                page.child(UiElement.type("Row").key("controls-row")
                    .child(button("control-primary","主要操作",UiLabAction.of(UiLabAction.Type.TOGGLE_DEBUG),"accent",true))
                    .child(button("control-danger","危险操作",UiLabAction.of(UiLabAction.Type.TOGGLE_DEBUG),"danger",true))
                    .child(button("control-disabled","已禁用",UiLabAction.of(UiLabAction.Type.TOGGLE_DEBUG),"surface",false)).build())
                    .child(UiElement.type("Card").key("control-field").child(label("control-field-text","搜索商品、订单或玩家…")).build())
                    .child(label("control-hint","Tab 焦点 · Enter 确认 · Esc 返回"));break;
            case DATA:
                page.child(label("data-head","商品    方向    数量      状态"))
                    .child(dataRow("data-row-0","铁锭   买入   1.28K   已成交","text"))
                    .child(dataRow("data-row-1","金锭   卖出   256     等待中","warning"))
                    .child(dataRow("data-row-2","钢锭   买入   64      已撤销","text"))
                    .child(UiElement.type("Pager").key("data-pager").prop(StandardWidgets.TEXT,"‹  1 / 3  ›").build());break;
            case OVERLAY:
                page.child(button("open-modal","打开确认框",UiLabAction.of(UiLabAction.Type.OPEN_MODAL),"accent",true))
                    .child(label("overlay-hint","Dialog 打开后阻断底层控件与原生槽位。"))
                    .child(UiElement.type("Card").key("overlay-menu").prop(StandardWidgets.LAYER,UiLayer.TOOLTIP)
                        .child(layerLabel("overlay-detail","查看详情 · 撤销操作",UiLayer.TOOLTIP)).build())
                    .child(UiElement.type("Toast").key("overlay-toast").prop(StandardWidgets.LAYER,UiLayer.TOAST)
                        .child(layerLabel("overlay-toast-label","操作已完成",UiLayer.TOAST)).build());break;
            case INVENTORY:
            default:
                page.child(UiElement.type("Row").key("inventory-panes")
                    .child(UiElement.type("Card").key("inventory-left")
                        .child(label("inventory-left-title","Base Vault · 玩家背包"))
                        .child(slots("inventory-vault","vault:region",9,45)).build())
                    .child(UiElement.type("Card").key("inventory-right")
                        .child(label("inventory-right-title","AE2 Bay · Cell"))
                        .child(slots("inventory-cell","cell:region",5,25)).build()).build());break;
        }
        if(state.isDebug())page.child(label("lab-debug","DEBUG · 组件边界与命中区已开启"));
        return page.build();
    }

    @Override public LayoutSpec layout(UiElement root,UiSize viewport,UiContext context){
        width=viewport.getWidth();height=viewport.getHeight();int top=height>=260?32:24;int nav=width>=500?100:72;int gap=width>=500?9:5;
        LayoutSpec topSpec=LayoutSpec.of("lab-top",LayoutKind.ROW).preferred(width,top).padding(new Insets(3,7,3,9)).gap(4)
            .child(LayoutSpec.of("lab-title",LayoutKind.LEAF).flex(1F).build())
            .child(LayoutSpec.of("lab-metrics",LayoutKind.LEAF).preferred(Math.min(150,width/3),0).build()).build();
        LayoutSpec.Builder navSpec=LayoutSpec.of("lab-nav",LayoutKind.COLUMN).preferred(nav,0).padding(new Insets(gap,gap,gap,gap)).gap(3);
        for(UiLabPage page:UiLabPage.values())navSpec.child(LayoutSpec.of("nav-"+page.name(),LayoutKind.LEAF).preferred(0,22).build());
        navSpec.child(LayoutSpec.of("lab-hint-f8",LayoutKind.LEAF).flex(1F).build())
            .child(LayoutSpec.of("lab-hint-esc",LayoutKind.LEAF).preferred(0,12).build());
        LayoutSpec body=LayoutSpec.of("lab-body",LayoutKind.STACK).flex(1F).padding(new Insets(gap+2,gap+2,gap+2,gap+2)).child(pageLayout(store.getState().getPage(),gap)).build();
        LayoutSpec workspace=LayoutSpec.of("lab-workspace",LayoutKind.ROW).flex(1F).gap(gap).child(navSpec.build()).child(body).build();
        LayoutSpec base=LayoutSpec.of("lab-base",LayoutKind.COLUMN).preferred(width,height).child(topSpec).child(workspace).build();
        LayoutSpec.Builder result=LayoutSpec.of("root",LayoutKind.STACK).preferred(width,height).child(base);
        if(store.getState().isModal())result.child(LayoutSpec.of("lab-dialog",LayoutKind.LEAF).build());
        return result.build();
    }

    private LayoutSpec pageLayout(UiLabPage page,int gap){
        LayoutSpec.Builder result=LayoutSpec.of("lab-page",LayoutKind.COLUMN).gap(3)
            .child(LayoutSpec.of("page-title",LayoutKind.LEAF).preferred(0,20).build())
            .child(LayoutSpec.of("page-subtitle",LayoutKind.LEAF).preferred(0,14).build());
        switch(page){
            case OVERVIEW:
                result.child(LayoutSpec.of("overview-cards",LayoutKind.ROW).preferred(0,48).gap(gap)
                    .child(LayoutSpec.of("overview-components",LayoutKind.LEAF).flex(1F).build())
                    .child(LayoutSpec.of("overview-state",LayoutKind.LEAF).flex(1F).build())
                    .child(LayoutSpec.of("overview-font",LayoutKind.LEAF).flex(1F).build()).build())
                    .child(LayoutSpec.of("overview-numeric",LayoutKind.LEAF).preferred(0,16).build())
                    .child(LayoutSpec.of("overview-diagnostics",LayoutKind.LEAF).preferred(0,13).build());break;
            case CONTROLS:
                result.child(LayoutSpec.of("controls-row",LayoutKind.ROW).preferred(0,24).gap(5)
                    .child(LayoutSpec.of("control-primary",LayoutKind.LEAF).flex(1F).build())
                    .child(LayoutSpec.of("control-danger",LayoutKind.LEAF).flex(1F).build())
                    .child(LayoutSpec.of("control-disabled",LayoutKind.LEAF).flex(1F).build()).build())
                    .child(LayoutSpec.of("control-field",LayoutKind.STACK).preferred(0,26).padding(new Insets(5,7,5,7))
                        .child(LayoutSpec.of("control-field-text",LayoutKind.LEAF).build()).build())
                    .child(LayoutSpec.of("control-hint",LayoutKind.LEAF).preferred(0,16).build());break;
            case DATA:
                result.child(LayoutSpec.of("data-head",LayoutKind.LEAF).preferred(0,14).build());
                for(int i=0;i<3;i++)result.child(LayoutSpec.of("data-row-"+i,LayoutKind.LEAF).preferred(0,19).build());
                result.child(LayoutSpec.of("data-pager",LayoutKind.LEAF).preferred(96,20).build());break;
            case OVERLAY:
                result.child(LayoutSpec.of("open-modal",LayoutKind.LEAF).preferred(104,24).build())
                    .child(LayoutSpec.of("overlay-hint",LayoutKind.LEAF).preferred(0,16).build())
                    .child(LayoutSpec.of("overlay-menu",LayoutKind.STACK).preferred(Math.min(180,Math.max(80,(width-100)/2)),30).padding(new Insets(5,7,5,7))
                        .child(LayoutSpec.of("overlay-detail",LayoutKind.LEAF).build()).build())
                    .child(LayoutSpec.of("overlay-toast",LayoutKind.STACK).preferred(150,24).padding(new Insets(4,7,4,7))
                        .child(LayoutSpec.of("overlay-toast-label",LayoutKind.LEAF).build()).build());break;
            case INVENTORY:
            default:
                int left=Math.max(90,(width-(width>=500?100:72)-gap*4)*53/100);
                result.child(LayoutSpec.of("inventory-panes",LayoutKind.ROW).flex(1F).gap(gap)
                    .child(LayoutSpec.of("inventory-left",LayoutKind.COLUMN).preferred(left,0).padding(new Insets(4,4,4,4))
                        .child(LayoutSpec.of("inventory-left-title",LayoutKind.LEAF).preferred(0,16).build())
                        .child(LayoutSpec.of("inventory-vault",LayoutKind.LEAF).flex(1F).build()).build())
                    .child(LayoutSpec.of("inventory-right",LayoutKind.COLUMN).flex(1F).padding(new Insets(4,4,4,4))
                        .child(LayoutSpec.of("inventory-right-title",LayoutKind.LEAF).preferred(0,16).build())
                        .child(LayoutSpec.of("inventory-cell",LayoutKind.LEAF).flex(1F).build()).build()).build());break;
        }
        if(store.getState().isDebug())result.child(LayoutSpec.of("lab-debug",LayoutKind.LEAF).preferred(0,12).build());
        return result.build();
    }

    @Override public UiInputNode modalInput(UiNode root,UiContext context){
        if(!store.getState().isModal())return null;
        int w=Math.min(230,Math.max(80,width-40)),h=Math.min(88,Math.max(50,height-30));
        UiRect dialog=new UiRect((width-w)/2,(height-h)/2,w,h);
        UiInputNode modal=new UiInputNode("lab-dialog",new UiRect(0,0,width,height),false,new UiInputHandler(){
            @Override public InputResult handle(UiInputNode target,UiEvent event){
                if((event.getPhase()==UiEvent.Phase.TARGET&&event.getType()==UiEvent.Type.POINTER_DOWN)
                    ||(event.getType()==UiEvent.Type.KEY_DOWN&&event.getKeyCode()==UiKeyCode.ESCAPE)){
                    store.dispatch(UiLabAction.of(UiLabAction.Type.CLOSE_MODAL));return InputResult.CONSUMED;
                }
                return InputResult.PASS;
            }});
        modal.add(new UiInputNode("lab-dialog-body",dialog,false,new UiInputHandler(){
            @Override public InputResult handle(UiInputNode target,UiEvent event){return event.getType()==UiEvent.Type.POINTER_DOWN?InputResult.CONSUMED:InputResult.PASS;}
        }));
        return modal;
    }

    private UiActionHandler action(final UiLabAction action){return new UiActionHandler(){@Override public InputResult handle(UiEvent event){store.dispatch(action);return InputResult.CONSUMED;}};}
    private UiElement button(String key,String text,UiLabAction action,String tone,boolean enabled){return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.TONE,tone).prop(StandardWidgets.ENABLED,enabled).prop(StandardWidgets.ACTION,action(action)).build();}
    private static UiElement label(String key,String text){return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT,text).build();}
    private static UiElement layerLabel(String key,String text,int layer){return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.LAYER,layer).build();}
    private static UiElement badge(String key,String text,String tone){return UiElement.type("Badge").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.TONE,tone).build();}
    private static UiElement dataRow(String key,String text,String tone){return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.TONE,tone).build();}
    private static UiElement slots(String key,String id,int columns,int count){return UiElement.type("NativeSlotRegion").key(key).prop(StandardWidgets.EXTERNAL_ID,id).prop(StandardWidgets.COLUMNS,columns).prop(StandardWidgets.COUNT,count).build();}
    private static String title(UiLabPage p){return p==UiLabPage.OVERVIEW?"现代终端设计系统":p==UiLabPage.CONTROLS?"控件与焦点":p==UiLabPage.DATA?"结构化数据":p==UiLabPage.OVERLAY?"浮层与反馈":"个人资产";}
    private static String subtitle(UiLabPage p){return p==UiLabPage.OVERVIEW?"Vue 式状态与组件，Minecraft 原生渲染":p==UiLabPage.CONTROLS?"Hover · Pressed · Focused · Disabled":p==UiLabPage.DATA?"表格、列表、分页与异步状态":p==UiLabPage.OVERLAY?"底层内容在模态打开时不可操作":"Vault 与背包在左，Bay 与 Cell 在右";}
}

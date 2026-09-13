package com.jsirgalaxybase.ui2.terminal;

import com.jsirgalaxybase.ui2.component.StandardWidgets;
import com.jsirgalaxybase.ui2.component.UiActionHandler;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;

/** Production terminal chrome shared by live and headless hosts. */
public final class TerminalVisualShell {
    public static final String SETTINGS_PAGE_ID="settings";
    private TerminalVisualShell() {}

    public static UiElement build(TerminalVisualModel model,UiElement content,final TerminalActionPort actions,String titleOverride,String localPageId){
        if(model==null||actions==null)throw new IllegalArgumentException("shell model and actions are required");
        final boolean settingsSelected=SETTINGS_PAGE_ID.equals(localPageId);
        UiElement.Builder nav=UiElement.type("Surface").key("terminal-nav").prop(StandardWidgets.TONE,"surface").child(label("terminal-brand","银河终端"));
        for(final TerminalVisualModel.NavItem item:model.getNavigation())nav.child(UiElement.type("NavTab").key("terminal-nav-"+item.getPageId())
            .prop(StandardWidgets.TEXT,item.getLabel()).prop(StandardWidgets.SELECTED,item.isSelected()&&!settingsSelected)
            .prop(StandardWidgets.ENABLED,item.isEnabled()).prop(StandardWidgets.ACTION,new UiActionHandler(){@Override public InputResult handle(UiEvent event){actions.navigate(item.getPageId());return InputResult.CONSUMED;}}).build());
        nav.child(UiElement.type("Stack").key("terminal-nav-spacer").build()).child(UiElement.type("NavTab").key("terminal-nav-settings")
            .prop(StandardWidgets.TEXT,"设置").prop(StandardWidgets.SELECTED,settingsSelected).prop(StandardWidgets.ENABLED,true)
            .prop(StandardWidgets.ACTION,new UiActionHandler(){@Override public InputResult handle(UiEvent event){actions.navigate(SETTINGS_PAGE_ID);return InputResult.CONSUMED;}}).build());
        UiElement top=UiElement.type("Surface").key("terminal-top").prop(StandardWidgets.TONE,"surface")
            .child(label("terminal-title",titleOverride==null?model.getTitle():titleOverride))
            .child(button("terminal-refresh","R",new Runnable(){@Override public void run(){actions.refresh();}}))
            .child(button("terminal-help","?",new Runnable(){@Override public void run(){actions.help();}}))
            .child(button("terminal-back","‹",new Runnable(){@Override public void run(){actions.back();}}))
            .child(button("terminal-close","×",new Runnable(){@Override public void run(){actions.close();}})).build();
        UiElement main=UiElement.type("Column").key("terminal-main").child(top).child(content).build();
        return UiElement.type("Root").key("terminal-viewport").child(UiElement.type("Surface").key("terminal-window")
            .prop(StandardWidgets.TONE,"window").child(nav.build()).child(main).build()).build();
    }

    public static LayoutSpec layout(UiSize viewport,TerminalVisualModel model,LayoutSpec content,TerminalWindowProfile profile){
        int width=viewport==null?0:viewport.getWidth(),height=viewport==null?0:viewport.getHeight();
        TerminalVisualMetrics metrics=TerminalVisualMetrics.compute(viewport,profile);int ww=metrics.getBounds().getWidth(),wh=metrics.getBounds().getHeight(),nav=metrics.getNavigationWidth();
        LayoutSpec.Builder navSpec=LayoutSpec.of("terminal-nav",LayoutKind.COLUMN).preferred(nav,height).gap(2).padding(new Insets(4,4,4,4)).child(LayoutSpec.of("terminal-brand",LayoutKind.LEAF).preferred(nav-8,20).build());
        int count=Math.max(1,model.getNavigation().size()+1),navHeight=Math.max(12,Math.min(16,(wh-28-Math.max(0,count-1)*2)/count));
        for(TerminalVisualModel.NavItem item:model.getNavigation())navSpec.child(LayoutSpec.of("terminal-nav-"+item.getPageId(),LayoutKind.LEAF).preferred(nav-8,navHeight).build());
        navSpec.child(LayoutSpec.of("terminal-nav-spacer",LayoutKind.LEAF).flex(1F).build()).child(LayoutSpec.of("terminal-nav-settings",LayoutKind.LEAF).preferred(nav-8,navHeight).build());
        int topHeight=wh<=220?18:22,topInset=wh<=220?1:2;
        LayoutSpec top=LayoutSpec.of("terminal-top",LayoutKind.ROW).preferred(0,topHeight).gap(2).padding(new Insets(topInset,3,topInset,3))
            .child(LayoutSpec.of("terminal-title",LayoutKind.LEAF).flex(1F).build()).child(LayoutSpec.of("terminal-refresh",LayoutKind.LEAF).preferred(22,topHeight-topInset*2).build())
            .child(LayoutSpec.of("terminal-help",LayoutKind.LEAF).preferred(22,topHeight-topInset*2).build()).child(LayoutSpec.of("terminal-back",LayoutKind.LEAF).preferred(22,topHeight-topInset*2).build())
            .child(LayoutSpec.of("terminal-close",LayoutKind.LEAF).preferred(22,topHeight-topInset*2).build()).build();
        LayoutSpec main=LayoutSpec.of("terminal-main",LayoutKind.COLUMN).flex(1F).child(top).child(content).build();
        LayoutSpec window=LayoutSpec.of("terminal-window",LayoutKind.ROW).preferred(ww,wh).child(navSpec.build()).child(main).build();
        return LayoutSpec.of("terminal-viewport",LayoutKind.STACK).preferred(width,height).padding(metrics.viewportInsets(viewport)).child(window).build();
    }
    private static UiElement label(String key,String text){return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT,text).build();}
    private static UiElement button(String key,String text,final Runnable action){return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.ENABLED,true).prop(StandardWidgets.ACTION,new UiActionHandler(){@Override public InputResult handle(UiEvent event){action.run();return InputResult.CONSUMED;}}).build();}
}

package com.jsirgalaxybase.ui2.terminal;

import com.jsirgalaxybase.ui2.component.ComponentUiDocument;
import com.jsirgalaxybase.ui2.component.StandardWidgets;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;

/** Shared read-only document for career, public-service and policy pages. */
public class SummaryVisualDocument extends ComponentUiDocument {
    private SummaryVisualModel model;private TerminalWindowProfile profile;private final TerminalActionPort actions;
    public SummaryVisualDocument(SummaryVisualModel model,TerminalWindowProfile profile){this(model,profile,TerminalActionPort.NONE);}
    public SummaryVisualDocument(SummaryVisualModel model,TerminalWindowProfile profile,TerminalActionPort actions){super(StandardWidgets.create());if(model==null)throw new IllegalArgumentException("model is required");this.model=model;this.profile=profile==null?TerminalWindowProfile.STANDARD:profile;this.actions=actions==null?TerminalActionPort.NONE:actions;}
    public final void updateVisualModel(SummaryVisualModel value){if(value==null)throw new IllegalArgumentException("model is required");model=value;}
    public final void updateWindowProfile(TerminalWindowProfile value){profile=value==null?TerminalWindowProfile.STANDARD:value;}
    @Override public UiElement build(UiContext context){UiElement.Builder cards=UiElement.type("Stack").key("summary-cards");int count=Math.min(6,model.getSections().size());if(count==0)cards.child(UiElement.type("EmptyState").key("summary-empty").prop(StandardWidgets.TEXT,"当前没有可展示内容").build());for(int i=0;i<count;i++){HomeVisualModel.Section s=model.getSections().get(i);cards.child(UiElement.type("Card").key("summary-card-"+i).child(label("summary-title-"+i,s.getTitle(),"body",1,true)).child(label("summary-subtitle-"+i,s.getSummary(),"caption",1,false)).child(label("summary-detail-"+i,s.getDetail(),"caption",3,false)).build());}UiElement body=UiElement.type("Column").key("summary-body").child(label("summary-page-title",model.getTitle(),"section",1,true)).child(label("summary-page-lead",model.getLead(),"caption",2,false)).child(cards.build()).build();return TerminalVisualShell.build(model.getShell(),body,actions,model.getTitle(),model.getPageId());}
    @Override public LayoutSpec layout(UiElement root,UiSize viewport,UiContext context){TerminalVisualMetrics metrics=TerminalVisualMetrics.compute(viewport,profile);boolean compact=metrics.getBounds().getHeight()<=220;int count=Math.min(6,model.getSections().size());LayoutKind kind=metrics.getMainWidth()>=280?LayoutKind.GRID:LayoutKind.COLUMN;LayoutSpec.Builder cards=LayoutSpec.of("summary-cards",kind).flex(1).gap(compact?2:4);if(kind==LayoutKind.GRID)cards.columns(2);if(count==0)cards.child(LayoutSpec.of("summary-empty",LayoutKind.LEAF).flex(1).build());for(int i=0;i<count;i++)cards.child(LayoutSpec.of("summary-card-"+i,LayoutKind.COLUMN).flex(1).padding(new Insets(4,5,4,5)).gap(1).child(LayoutSpec.of("summary-title-"+i,LayoutKind.LEAF).preferred(0,13).build()).child(LayoutSpec.of("summary-subtitle-"+i,LayoutKind.LEAF).preferred(0,11).build()).child(LayoutSpec.of("summary-detail-"+i,LayoutKind.LEAF).flex(1).build()).build());LayoutSpec body=LayoutSpec.of("summary-body",LayoutKind.COLUMN).flex(1).padding(new Insets(4,5,4,5)).gap(2).child(LayoutSpec.of("summary-page-title",LayoutKind.LEAF).preferred(0,16).build()).child(LayoutSpec.of("summary-page-lead",LayoutKind.LEAF).preferred(0,compact?18:24).build()).child(cards.build()).build();return TerminalVisualShell.layout(viewport,model.getShell(),body,profile);}
    private static UiElement label(String key,String text,String role,int lines,boolean bold){return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.TEXT_ROLE,role).prop(StandardWidgets.MAX_LINES,lines).prop(StandardWidgets.BOLD,bold).build();}
}

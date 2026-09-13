package com.jsirgalaxybase.ui2.component;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.geometry.SlotGridGeometry;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.render.UiLayer;
import com.jsirgalaxybase.ui2.text.TextStyle;
import com.jsirgalaxybase.ui2.text.TextFlow;
import com.jsirgalaxybase.ui2.text.TextFit;

/** Theme-driven production widgets shared by Lab and business documents. */
public final class StandardWidgets {
    public static final String ACTION = "action";
    public static final String ENABLED = "enabled";
    public static final String TEXT = "text";
    public static final String TONE = "tone";
    public static final String SELECTED = "selected";
    public static final String EXTERNAL_ID = "externalId";
    public static final String COLUMNS = "columns";
    public static final String COUNT = "count";
    public static final String LAYER = "layer";
    public static final String QUANTITY = "quantity";
    public static final String DETAIL = "detail";
    public static final String TEXT_ROLE = "textRole";
    public static final String MAX_LINES = "maxLines";
    public static final String BOLD = "bold";
    public static final String PLACEHOLDER = "placeholder";
    public static final String FOCUSED = "focused";
    public static final String CONFIRM_TEXT = "confirmText";
    public static final String CANCEL_TEXT = "cancelText";

    private StandardWidgets() {}

    public static UiWidgetRegistry create() {
        UiWidgetRegistry result = new UiWidgetRegistry();
        result.register(new Basic("Root", Basic.Kind.ROOT));
        for (String type : new String[] { "Row", "Column", "Stack", "Tabs", "List" }) {
            result.register(new Basic(type, Basic.Kind.NONE));
        }
        result.register(new Basic("Surface", Basic.Kind.SURFACE));
        result.register(new Basic("Card", Basic.Kind.CARD));
        result.register(new Basic("Divider", Basic.Kind.DIVIDER));
        result.register(new Basic("Label", Basic.Kind.LABEL));
        result.register(new Basic("Badge", Basic.Kind.BADGE));
        result.register(new Basic("Button", Basic.Kind.BUTTON));
        result.register(new Basic("Tab", Basic.Kind.TAB));
        result.register(new Basic("NavTab", Basic.Kind.NAV_TAB));
        result.register(new Basic("LoadingState", Basic.Kind.LOADING));
        result.register(new Basic("EmptyState", Basic.Kind.EMPTY));
        result.register(new Basic("ErrorState", Basic.Kind.ERROR));
        result.register(new Basic("Dialog", Basic.Kind.DIALOG));
        result.register(new Basic("Toast", Basic.Kind.TOAST));
        result.register(new Basic("Pager", Basic.Kind.PAGER));
        result.register(new TextField());
        result.register(new NativeSlots());
        result.register(new ExternalItem());
        result.register(new ExternalSurface());
        return result;
    }

    private static final class TextField implements UiWidgetAdapter {
        @Override public String type() { return "TextField"; }

        @Override public void paint(UiNode node, UiContext context, DrawList target) {
            UiElement element = node.getElement();
            UiRect bounds = node.getBounds();
            boolean enabled = bool(element, ENABLED, true);
            boolean focused = bool(element, FOCUSED, false);
            int radius = context.getTheme().radius("md");
            target.add(DrawCommand.surface(UiLayer.CONTENT, bounds,
                context.getTheme().color(enabled ? "surfaceRaised" : "surface"), radius));
            target.add(DrawCommand.border(UiLayer.CONTENT, bounds,
                context.getTheme().color(focused ? "accent" : "border"), radius));
            String value = string(element, TEXT);
            boolean placeholder = value.isEmpty();
            TextStyle style = style(context, "body", false, TextStyle.Align.LEFT);
            UiRect textBounds = inset(bounds, 5, Math.max(1, (bounds.getHeight() - style.getLineHeight()) / 2));
            TextStyle fitted=TextFit.singleLine(placeholder?string(element,PLACEHOLDER):value,style,
                context.getTextMeasurer(),textBounds.getWidth(),textBounds.getHeight());
            flowText(target, UiLayer.CONTENT, textBounds,
                context.getTheme().color(placeholder || !enabled ? "textMuted" : "text"),
                placeholder ? string(element, PLACEHOLDER) : value, fitted, 1, context);
            if (focused && enabled) {
                int caretX = textBounds.getX() + Math.min(textBounds.getWidth(),
                    context.getTextMeasurer().measure(value, fitted, Integer.MAX_VALUE).getWidth());
                target.add(DrawCommand.line(UiLayer.CONTENT,
                    new UiRect(caretX, textBounds.getY(), 1, fitted.getLineHeight()),
                    context.getTheme().color("accent")));
            }
        }

        @Override public UiInputNode input(UiNode node, UiContext context) {
            final UiElement element = node.getElement();
            final Object handler = element.getProp(ACTION);
            if (!(handler instanceof UiActionHandler) || !bool(element, ENABLED, true)) return null;
            return new UiInputNode(key(node), node.getBounds(), true,
                new com.jsirgalaxybase.ui2.input.UiInputHandler() {
                    @Override public InputResult handle(UiInputNode target, UiEvent event) {
                        if (event.getPhase() != UiEvent.Phase.TARGET) return InputResult.PASS;
                        if (event.getType() == UiEvent.Type.POINTER_DOWN && event.getButton() == 0) {
                            return ((UiActionHandler) handler).handle(event);
                        }
                        if (event.getType() == UiEvent.Type.TEXT_INPUT) {
                            return ((UiActionHandler) handler).handle(event);
                        }
                        if (event.getType() == UiEvent.Type.KEY_DOWN
                            && (event.getKeyCode() == com.jsirgalaxybase.ui2.input.UiKeyCode.BACKSPACE
                                || event.getKeyCode() == com.jsirgalaxybase.ui2.input.UiKeyCode.DELETE
                                || event.getKeyCode() == com.jsirgalaxybase.ui2.input.UiKeyCode.HOME
                                || event.getKeyCode() == com.jsirgalaxybase.ui2.input.UiKeyCode.END)) {
                            return ((UiActionHandler) handler).handle(event);
                        }
                        return InputResult.PASS;
                    }
                });
        }
    }

    private static final class Basic implements UiWidgetAdapter {
        private enum Kind { NONE, ROOT, SURFACE, CARD, DIVIDER, LABEL, BADGE, BUTTON, TAB, NAV_TAB,
            LOADING, EMPTY, ERROR, DIALOG, TOAST, PAGER }
        private final String type;
        private final Kind kind;
        private Basic(String type, Kind kind) { this.type = type; this.kind = kind; }
        @Override public String type() { return type; }
        @Override public void paint(UiNode node, UiContext context, DrawList target) {
            UiRect b = node.getBounds(); UiElement e = node.getElement();
            int layer = integer(e, LAYER, kind == Kind.DIALOG ? UiLayer.MODAL
                : kind == Kind.TOAST ? UiLayer.TOAST : UiLayer.CONTENT);
            int radius = context.getTheme().radius(kind == Kind.DIALOG ? "lg" : "md");
            if (kind == Kind.ROOT) target.add(DrawCommand.surface(UiLayer.BACKGROUND,b,context.getTheme().color("background"),0));
            else if (kind == Kind.SURFACE) {
                String surfaceTone = tone(e, "surface");
                if ("window".equals(surfaceTone)) {
                    target.add(DrawCommand.shadow(layer, b, 0x78000000, 7, 4));
                }
                target.add(DrawCommand.surface(layer, b, context.getTheme().color(surfaceTone), radius));
                if ("window".equals(surfaceTone)) {
                    target.add(DrawCommand.border(layer, b, context.getTheme().color("border"), radius));
                }
            }
            else if (kind == Kind.DIALOG) {
                target.add(DrawCommand.surface(layer,b,0xAA030A12,0));
                int width=Math.min(230,Math.max(80,b.getWidth()-40)); int height=Math.min(90,Math.max(50,b.getHeight()-30));
                UiRect dialog=new UiRect(b.getX()+(b.getWidth()-width)/2,b.getY()+(b.getHeight()-height)/2,width,height);
                target.add(DrawCommand.shadow(layer,dialog,0x88000000,6,5));
                target.add(DrawCommand.surface(layer,dialog,context.getTheme().color("surfaceRaised"),radius));
                target.add(DrawCommand.border(layer,dialog,context.getTheme().color("accent"),radius));
                flowText(target,layer,inset(dialog,12,10),context.getTheme().color("text"),string(e,TEXT),
                    style(context,"body",false,TextStyle.Align.LEFT),1,context);
                boolean hasActions=!string(e,CONFIRM_TEXT).isEmpty()||!string(e,CANCEL_TEXT).isEmpty();
                flowText(target,layer,new UiRect(dialog.getX()+12,dialog.getY()+32,Math.max(0,dialog.getWidth()-24),Math.max(0,dialog.getHeight()-(hasActions?58:40))),context.getTheme().color("textMuted"),string(e,DETAIL),
                    style(context,"caption",false,TextStyle.Align.LEFT),3,context);
                if(hasActions){
                    int buttonY=dialog.getBottom()-22;int half=Math.max(0,(dialog.getWidth()-28)/2);
                    UiRect cancel=new UiRect(dialog.getX()+10,buttonY,half,16);
                    UiRect confirm=new UiRect(cancel.getRight()+8,buttonY,half,16);
                    target.add(DrawCommand.surface(layer,cancel,context.getTheme().color("surface"),context.getTheme().radius("sm")));
                    target.add(DrawCommand.border(layer,cancel,context.getTheme().color("border"),context.getTheme().radius("sm")));
                    target.add(DrawCommand.surface(layer,confirm,context.getTheme().color("selectedSurface"),context.getTheme().radius("sm")));
                    target.add(DrawCommand.border(layer,confirm,context.getTheme().color("accent"),context.getTheme().radius("sm")));
                    flowText(target,layer,inset(cancel,3,2),context.getTheme().color("textMuted"),string(e,CANCEL_TEXT),style(context,"caption",false,TextStyle.Align.CENTER),1,context);
                    flowText(target,layer,inset(confirm,3,2),context.getTheme().color("text"),string(e,CONFIRM_TEXT),style(context,"caption",false,TextStyle.Align.CENTER),1,context);
                }
            } else if (kind == Kind.CARD || kind == Kind.TOAST) {
                target.add(DrawCommand.surface(layer, b, context.getTheme().color("surfaceRaised"), radius));
                target.add(DrawCommand.border(layer, b, context.getTheme().color(tone(e, "border")), radius));
            } else if (kind == Kind.DIVIDER) target.add(DrawCommand.line(layer, b, context.getTheme().color("border")));
            else if (kind == Kind.LABEL) {
                String role=string(e,TEXT_ROLE); if(role.isEmpty())role="body";
                TextStyle labelStyle=style(context,role,bool(e,BOLD,false),TextStyle.Align.LEFT);
                flowText(target,layer,b,context.getTheme().color(tone(e,"text")),string(e,TEXT),labelStyle,
                    integer(e,MAX_LINES,1),context);
            }
            else if (kind == Kind.BADGE) {
                target.add(DrawCommand.surface(layer, b, context.getTheme().color(tone(e, "surface")), radius));
                flowText(target,layer,inset(b,5,2),context.getTheme().color("text"),string(e,TEXT),
                    style(context,"caption",false,TextStyle.Align.LEFT),1,context);
            } else if (kind == Kind.BUTTON || kind == Kind.TAB || kind == Kind.NAV_TAB || kind == Kind.PAGER) {
                boolean enabled = bool(e, ENABLED, true), selected = bool(e, SELECTED, false);
                String fill = !enabled ? "surface" : selected ? "selectedSurface" : "surfaceRaised";
                target.add(DrawCommand.surface(layer, b, context.getTheme().color(fill), radius));
                target.add(DrawCommand.border(layer, b, context.getTheme().color(selected ? "accent" : tone(e, "border")), radius));
                if (selected) target.add(DrawCommand.surface(layer,
                    new UiRect(b.getX() + 2, b.getBottom() - 2, Math.max(0, b.getWidth() - 4), 1),
                    context.getTheme().color("accent"), 0));
                TextStyle buttonStyle=style(context,kind==Kind.NAV_TAB?"caption":"body",false,TextStyle.Align.CENTER);
                flowText(target, layer, inset(b, 4, Math.max(1, (b.getHeight() - buttonStyle.getLineHeight()) / 2)),
                    context.getTheme().color(enabled ? "text" : "textMuted"), string(e, TEXT),
                    buttonStyle,1,context);
            } else if (kind == Kind.LOADING || kind == Kind.EMPTY || kind == Kind.ERROR) {
                String fallback = kind == Kind.LOADING ? "正在加载…" : kind == Kind.EMPTY ? "暂无内容" : "加载失败";
                flowText(target, layer, inset(b, 6, 5), context.getTheme().color(kind == Kind.ERROR ? "danger" : "textMuted"),
                    string(e, TEXT).isEmpty() ? fallback : string(e, TEXT),style(context,"body",false,TextStyle.Align.LEFT),2,context);
            }
        }
        @Override public UiInputNode input(UiNode node, UiContext context) {
            final UiElement element = node.getElement();
            final Object handler = element.getProp(ACTION);
            if (!(handler instanceof UiActionHandler) || !bool(element, ENABLED, true)) return null;
            return new UiInputNode(key(node), node.getBounds(), true,
                new com.jsirgalaxybase.ui2.input.UiInputHandler() {
                    @Override public InputResult handle(UiInputNode target, UiEvent event) {
                        boolean clickableCardBubble = kind == Kind.CARD && event.getPhase() == UiEvent.Phase.BUBBLE;
                        if (event.getPhase() != UiEvent.Phase.TARGET && !clickableCardBubble) return InputResult.PASS;
                        if (event.getType() == UiEvent.Type.POINTER_DOWN
                            && kind != Kind.NONE && event.getButton() != 0) return InputResult.PASS;
                        if (event.getType() == UiEvent.Type.KEY_DOWN
                            && event.getKeyCode() != com.jsirgalaxybase.ui2.input.UiKeyCode.ENTER
                            && event.getKeyCode() != com.jsirgalaxybase.ui2.input.UiKeyCode.SPACE) return InputResult.PASS;
                        if (event.getType() != UiEvent.Type.POINTER_DOWN && event.getType() != UiEvent.Type.KEY_DOWN) return InputResult.PASS;
                        return ((UiActionHandler) handler).handle(event);
                    }
                });
        }
    }

    private static final class NativeSlots implements UiWidgetAdapter {
        @Override public String type() { return "NativeSlotRegion"; }
        @Override public void paint(UiNode node, UiContext context, DrawList target) {
            UiElement e=node.getElement(); int columns=Math.max(1,integer(e,COLUMNS,1));
            int count=Math.max(0,integer(e,COUNT,0)); String id=string(e,EXTERNAL_ID);
            for(UiRect cell:SlotGridGeometry.cells(node.getBounds(),columns,count)){
                target.add(DrawCommand.surface(UiLayer.CONTENT,cell,context.getTheme().color("slot"),3));
                target.add(DrawCommand.border(UiLayer.CONTENT,cell,context.getTheme().color("slotBorder"),3));
            }
            if(!id.isEmpty())target.add(DrawCommand.externalRegion(UiLayer.NATIVE,node.getBounds(),id));
        }
        @Override public UiInputNode input(UiNode node,UiContext context){return null;}
    }

    private static final class ExternalItem implements UiWidgetAdapter {
        private static final TextStyle COUNT = new TextStyle("numeric",8,true,10,TextStyle.Align.RIGHT);
        @Override public String type(){return "ExternalItem";}
        @Override public void paint(UiNode node,UiContext context,DrawList target){UiElement element=node.getElement();String id=string(element,EXTERNAL_ID);if(id.isEmpty())return;UiRect bounds=node.getBounds();target.add(DrawCommand.surface(UiLayer.CONTENT,bounds,context.getTheme().color("slot"),3));target.add(DrawCommand.border(UiLayer.CONTENT,bounds,context.getTheme().color("slotBorder"),3));target.add(DrawCommand.externalRegion(UiLayer.NATIVE,bounds,id));String quantity=string(element,QUANTITY);if(!quantity.isEmpty())target.add(DrawCommand.text(UiLayer.NATIVE_OVERLAY,new UiRect(bounds.getX()+1,bounds.getBottom()-10,Math.max(0,bounds.getWidth()-2),10),0xFFFFFFFF,quantity,COUNT));}
        @Override public UiInputNode input(UiNode node,UiContext context){return new Basic("ExternalItem",Basic.Kind.NONE).input(node,context);}
    }

    /** Platform-owned visual region embedded between UI content and overlays. */
    private static final class ExternalSurface implements UiWidgetAdapter {
        @Override public String type() { return "ExternalSurface"; }
        @Override public void paint(UiNode node, UiContext context, DrawList target) {
            String id = string(node.getElement(), EXTERNAL_ID);
            if (!id.isEmpty()) target.add(DrawCommand.externalRegion(UiLayer.NATIVE, node.getBounds(), id));
        }
        @Override public UiInputNode input(UiNode node, UiContext context) { return null; }
    }

    private static TextStyle style(UiContext context,String role,boolean bold,TextStyle.Align align){
        String token="caption".equals(role)?"caption":"title".equals(role)?"title":"section".equals(role)?"section":"body";
        int size=context.getTheme().typography(token);
        int line="title".equals(token)?context.getTheme().typography("lineTitle"):
            "section".equals(token)?Math.max(size,context.getTheme().typography("lineBody")+2):
            "caption".equals(token)?Math.max(size,context.getTheme().typography("lineBody")-2):context.getTheme().typography("lineBody");
        return new TextStyle(token,size,bold,line,align);
    }
    private static void flowText(DrawList list,int layer,UiRect bounds,int color,String value,TextStyle style,int maxLines,UiContext context){
        int visible=Math.min(Math.max(0,maxLines),bounds.getHeight()/Math.max(1,style.getLineHeight()));
        TextStyle fitted=TextFit.block(value,style,context.getTextMeasurer(),bounds.getWidth(),bounds.getHeight(),maxLines);
        visible=Math.min(Math.max(0,maxLines),bounds.getHeight()/Math.max(1,fitted.getLineHeight()));
        TextFlow.Result flow=TextFlow.layout(value,fitted,context.getTextMeasurer(),bounds.getWidth(),visible);
        list.add(DrawCommand.clipPush(layer,bounds));
        int y=bounds.getY();
        for(String line:flow.getLines()){
            list.add(DrawCommand.text(layer,new UiRect(bounds.getX(),y,bounds.getWidth(),fitted.getLineHeight()),color,line,fitted));
            y+=fitted.getLineHeight();
        }
        list.add(DrawCommand.clipPop(layer));
    }
    private static UiRect inset(UiRect b,int x,int y){return new UiRect(b.getX()+x,b.getY()+y,Math.max(0,b.getWidth()-x*2),Math.max(0,b.getHeight()-y*2));}
    private static String string(UiElement e,String name){Object value=e.getProp(name);return value==null?"":String.valueOf(value);}
    private static String tone(UiElement e,String fallback){String value=string(e,TONE);return value.isEmpty()?fallback:value;}
    private static int integer(UiElement e,String name,int fallback){Object value=e.getProp(name);return value instanceof Number?((Number)value).intValue():fallback;}
    private static boolean bool(UiElement e,String name,boolean fallback){Object value=e.getProp(name);return value instanceof Boolean?((Boolean)value).booleanValue():fallback;}
    private static String key(UiNode node){return node.getElement().getKey()==null?node.getElement().getType():node.getElement().getKey().getValue();}
}

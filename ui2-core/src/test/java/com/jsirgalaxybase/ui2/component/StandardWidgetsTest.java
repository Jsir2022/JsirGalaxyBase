package com.jsirgalaxybase.ui2.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.theme.DarkIndustrialTheme;

public class StandardWidgetsTest {
    @Test public void componentDocumentPaintsAndDispatchesRegisteredButton(){
        final AtomicInteger clicks=new AtomicInteger();
        ComponentUiDocument document=new ComponentUiDocument(StandardWidgets.create()){
            @Override public UiElement build(UiContext context){return UiElement.type("Surface").key("root")
                .child(UiElement.type("Button").key("action").prop(StandardWidgets.TEXT,"执行")
                    .prop(StandardWidgets.ACTION,new UiActionHandler(){@Override public InputResult handle(UiEvent event){clicks.incrementAndGet();return InputResult.CONSUMED;}}).build()).build();}
            @Override public LayoutSpec layout(UiElement root,UiSize viewport,UiContext context){return LayoutSpec.of("root",LayoutKind.COLUMN).preferred(viewport.getWidth(),viewport.getHeight()).child(LayoutSpec.of("action",LayoutKind.LEAF).preferred(80,20).build()).build();}
        };
        UiRuntime runtime=new UiRuntime(document,new UiContext(DarkIndustrialTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,false)));
        runtime.setViewport(100,40);DrawList list=runtime.frame();
        assertTrue(has(list,DrawCommand.Kind.SURFACE));assertTrue(has(list,DrawCommand.Kind.BORDER));assertTrue(has(list,DrawCommand.Kind.TEXT));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,5,5,0)));assertEquals(1,clicks.get());
        assertTrue(runtime.dispatch(UiEvent.key(com.jsirgalaxybase.ui2.input.UiKeyCode.ENTER)));
        assertEquals(2,clicks.get());
        assertTrue(!runtime.dispatch(UiEvent.key(com.jsirgalaxybase.ui2.input.UiKeyCode.LEFT)));
        assertEquals(2,clicks.get());
    }

    @Test public void componentCompositionIsStableAcrossNativeAndContentLayers(){
        ComponentUiDocument document=new ComponentUiDocument(StandardWidgets.create()){
            @Override public UiElement build(UiContext context){return UiElement.type("Row").key("root")
                .child(UiElement.type("NativeSlotRegion").key("slots").prop(StandardWidgets.EXTERNAL_ID,"native:test")
                    .prop(StandardWidgets.COLUMNS,1).prop(StandardWidgets.COUNT,1).build())
                .child(UiElement.type("Label").key("after").prop(StandardWidgets.TEXT,"后续内容").build()).build();}
            @Override public LayoutSpec layout(UiElement root,UiSize viewport,UiContext context){return LayoutSpec.of("root",LayoutKind.ROW)
                .preferred(viewport.getWidth(),viewport.getHeight()).child(LayoutSpec.of("slots",LayoutKind.LEAF).preferred(20,20).build())
                .child(LayoutSpec.of("after",LayoutKind.LEAF).flex(1F).build()).build();}
        };
        UiRuntime runtime=new UiRuntime(document,new UiContext(DarkIndustrialTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,false)));
        runtime.setViewport(100,24);DrawList list=runtime.frame();int previous=Integer.MIN_VALUE;boolean nativeRegion=false;
        for(DrawCommand command:list.ordered()){assertTrue(command.getLayer()>=previous);previous=command.getLayer();if(command.getKind()==DrawCommand.Kind.EXTERNAL_REGION)nativeRegion=true;}
        assertTrue(nativeRegion);
    }

    @Test public void clickableCardReceivesClickFromPassiveChild() {
        final AtomicInteger clicks = new AtomicInteger();
        ComponentUiDocument document = new ComponentUiDocument(StandardWidgets.create()) {
            @Override public UiElement build(UiContext context) {
                return UiElement.type("Card").key("card")
                    .prop(StandardWidgets.ACTION, new UiActionHandler() {
                        @Override public InputResult handle(UiEvent event) {
                            clicks.incrementAndGet();
                            return InputResult.CONSUMED;
                        }
                    })
                    .child(UiElement.type("ExternalSurface").key("passive")
                        .prop(StandardWidgets.EXTERNAL_ID, "chart").build()).build();
            }
            @Override public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
                return LayoutSpec.of("card", LayoutKind.STACK).preferred(100, 40)
                    .child(LayoutSpec.of("passive", LayoutKind.LEAF).build()).build();
            }
        };
        UiRuntime runtime = new UiRuntime(document,
            new UiContext(DarkIndustrialTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0, false)));
        runtime.setViewport(100, 40);
        runtime.frame();
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, 20, 20, 0)));
        assertEquals(1, clicks.get());
    }

    @Test public void textFieldAcceptsTextAndEditingKeysButDoesNotTurnSpaceIntoButtonActivation() {
        final StringBuilder events = new StringBuilder();
        ComponentUiDocument document = new ComponentUiDocument(StandardWidgets.create()) {
            @Override public UiElement build(UiContext context) {
                return UiElement.type("TextField").key("field")
                    .prop(StandardWidgets.TEXT, "abc")
                    .prop(StandardWidgets.PLACEHOLDER, "输入内容")
                    .prop(StandardWidgets.FOCUSED, true)
                    .prop(StandardWidgets.ACTION, new UiActionHandler() {
                        @Override public InputResult handle(UiEvent event) {
                            events.append(event.getType()).append(':').append(event.getTypedChar()).append(';');
                            return InputResult.CONSUMED;
                        }
                    }).build();
            }
            @Override public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
                return LayoutSpec.of("field", LayoutKind.LEAF).preferred(100,20).build();
            }
        };
        UiRuntime runtime = new UiRuntime(document,
            new UiContext(DarkIndustrialTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0,false)));
        runtime.setViewport(100,20);
        runtime.frame();
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, 4, 4, 0)));
        assertTrue(runtime.dispatch(UiEvent.text('中')));
        assertTrue(runtime.dispatch(UiEvent.key(com.jsirgalaxybase.ui2.input.UiKeyCode.BACKSPACE)));
        assertTrue(!runtime.dispatch(UiEvent.key(com.jsirgalaxybase.ui2.input.UiKeyCode.SPACE)));
        assertTrue(events.toString().contains("TEXT_INPUT:中"));
        assertTrue(events.toString().contains("KEY_DOWN"));
    }

    @Test public void fixedThemeButtonTextShrinksAndStaysInsideItsAssignedBounds(){
        ComponentUiDocument document=new ComponentUiDocument(StandardWidgets.create()){
            @Override public UiElement build(UiContext context){return UiElement.type("Button").key("button")
                .prop(StandardWidgets.TEXT,"Minecraft / 资源包字体").build();}
            @Override public LayoutSpec layout(UiElement root,UiSize viewport,UiContext context){return LayoutSpec.of("button",LayoutKind.LEAF).preferred(80,16).build();}
        };
        UiRuntime runtime=new UiRuntime(document,new UiContext(DarkIndustrialTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,false)));
        runtime.setViewport(80,16);DrawList list=runtime.frame();
        for(DrawCommand command:list.ordered()){
            if(command.getKind()!=DrawCommand.Kind.TEXT)continue;
            assertTrue(command.getBounds().getX()>=0&&command.getBounds().getY()>=0);
            assertTrue(command.getBounds().getRight()<=80&&command.getBounds().getBottom()<=16);
            assertTrue(command.getTextStyle().getSize()<DarkIndustrialTheme.create().typography("body"));
        }
    }

    @Test public void multilineLabelUsesItsSemanticSizeAsAnUpperBound(){
        ComponentUiDocument document=new ComponentUiDocument(StandardWidgets.create()){
            @Override public UiElement build(UiContext context){return UiElement.type("Label").key("label")
                .prop(StandardWidgets.TEXT,"一二三四五六七八九十").prop(StandardWidgets.MAX_LINES,2).build();}
            @Override public LayoutSpec layout(UiElement root,UiSize viewport,UiContext context){return LayoutSpec.of("label",LayoutKind.LEAF).preferred(42,20).build();}
        };
        UiRuntime runtime=new UiRuntime(document,new UiContext(DarkIndustrialTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,false)));
        runtime.setViewport(42,20);DrawList list=runtime.frame();
        for(DrawCommand command:list.ordered())if(command.getKind()==DrawCommand.Kind.TEXT){
            assertTrue(command.getTextStyle().getSize()<DarkIndustrialTheme.create().typography("body"));
            assertTrue(command.getBounds().getBottom()<=20);
        }
    }
    private static boolean has(DrawList list,DrawCommand.Kind kind){for(DrawCommand command:list.ordered())if(command.getKind()==kind)return true;return false;}
}

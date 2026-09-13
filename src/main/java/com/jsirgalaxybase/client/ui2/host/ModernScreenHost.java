package com.jsirgalaxybase.client.ui2.host;

import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.jsirgalaxybase.client.ui2.MinecraftKeyMapper;
import com.jsirgalaxybase.client.ui2.MinecraftUiPlatform;
import com.jsirgalaxybase.client.ui2.SystemUiClock;
import com.jsirgalaxybase.client.ui2.render.MinecraftUiRenderer;
import com.jsirgalaxybase.client.ui2.render.UiRenderDiagnostics;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiDocument;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.render.UiLayer;
import com.jsirgalaxybase.ui2.theme.DarkIndustrialTheme;
import com.jsirgalaxybase.ui2.theme.UiTheme;

public abstract class ModernScreenHost extends GuiScreen implements Ui2DebugCaptureSource {
    private final GuiScreen parent;
    private UiRuntime runtime;
    private DrawList current;

    protected ModernScreenHost(GuiScreen parent) { this.parent = parent; }

    @Override public void initGui() {
        super.initGui(); Keyboard.enableRepeatEvents(true); MinecraftUiPlatform.INSTANCE.initialize();
        if (runtime != null) runtime.close();
        runtime = new UiRuntime(createDocument(), new UiContext(uiTheme(), Locale.CHINA,
            density(), new SystemUiClock(isReducedMotion()), MinecraftUiPlatform.INSTANCE.fonts(),
            com.jsirgalaxybase.ui2.resource.ResourceResolver.EMPTY));
        runtime.setViewport(width, height);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        long frameStarted = System.nanoTime();
        DrawList list = runtime.frame(); current = list;
        UiRenderDiagnostics.INSTANCE.recordFrame(System.nanoTime() - frameStarted);
        MinecraftUiRenderer renderer = new MinecraftUiRenderer(MinecraftUiPlatform.INSTANCE.fonts(), scaleFactor(), mc.displayHeight);
        renderer.render(list, Integer.MIN_VALUE, UiLayer.NATIVE,true,uiClipBounds());
        drawExternalContent(list, mouseX, mouseY, partialTicks);
        renderer.render(list, UiLayer.NATIVE_OVERLAY, Integer.MAX_VALUE, false,uiClipBounds());
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!runtime.hasModal() && handleExternalPointer(UiEvent.Type.POINTER_DOWN, mouseX, mouseY, mouseButton)) return;
        runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, mouseX, mouseY, mouseButton));
    }
    @Override protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        if (!runtime.hasModal() && handleExternalPointer(UiEvent.Type.POINTER_UP, mouseX, mouseY, mouseButton)) return;
        runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_UP, mouseX, mouseY, mouseButton));
    }
    @Override public void handleMouseInput() {
        super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel==0)return;
        int x=Mouse.getEventX()*width/mc.displayWidth,y=height-Mouse.getEventY()*height/mc.displayHeight-1;
        if(!runtime.hasModal()&&handleExternalScroll(x,y,wheel))return;
        runtime.dispatch(UiEvent.scroll(x,y,wheel));
    }
    @Override protected void keyTyped(char typedChar, int keyCode) {
        if(!runtime.hasModal()&&handleExternalKey(typedChar,keyCode))return;
        UiKeyCode semantic=MinecraftKeyMapper.map(keyCode);
        boolean consumed=semantic!=UiKeyCode.UNKNOWN&&runtime.dispatch(UiEvent.key(semantic));
        if(!consumed&&typedChar!='\0')consumed=runtime.dispatch(UiEvent.text(typedChar));
        if(!consumed)consumed=handleUnhandledKey(typedChar,keyCode);
        if(!consumed&&keyCode==Keyboard.KEY_ESCAPE)mc.displayGuiScreen(parent);
    }
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);if(runtime!=null)runtime.close();super.onGuiClosed();}
    @Override public boolean doesGuiPauseGame(){return false;}
    protected final void invalidateUi(){if(runtime!=null)runtime.invalidate();}
    protected final UiRuntime getUiRuntime(){return runtime;}
    protected final DrawList currentDrawList(){return current;}
    protected void drawExternalContent(DrawList list,int mouseX,int mouseY,float partialTicks){}
    protected boolean handleExternalPointer(UiEvent.Type type,int x,int y,int button){return false;}
    protected boolean handleExternalScroll(int x,int y,int delta){return false;}
    protected boolean handleExternalKey(char typedChar,int keyCode){return false;}
    protected boolean handleUnhandledKey(char typedChar,int keyCode){return false;}
    protected boolean isReducedMotion(){return false;}
    protected com.jsirgalaxybase.ui2.geometry.UiRect uiClipBounds(){return null;}
    protected UiTheme uiTheme(){return DarkIndustrialTheme.create();}
    protected abstract UiDocument createDocument();
    @Override public final DrawList ui2DebugDrawList(){return current;}
    @Override public final com.jsirgalaxybase.ui2.core.UiNode ui2DebugRoot(){return runtime==null?null:runtime.getRoot();}
    @Override public final int ui2DebugWidth(){return width;}
    @Override public final int ui2DebugHeight(){return height;}
    private float density(){return scaleFactor();}
    private int scaleFactor(){return new ScaledResolution(mc,mc.displayWidth,mc.displayHeight).getScaleFactor();}
}

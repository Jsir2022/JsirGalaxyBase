package com.jsirgalaxybase.client.ui2.host;

import java.util.Locale;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

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

public abstract class ModernContainerHost<C extends Container> extends GuiContainer implements Ui2DebugCaptureSource {
    private final GuiScreen parent;
    private UiRuntime runtime;
    private DrawList current;
    protected final NativeSlotBridge slotBridge=new NativeSlotBridge();
    protected final NativeItemBridge itemBridge=new NativeItemBridge();
    protected ModernContainerHost(C container,GuiScreen parent){super(container);this.parent=parent;}
    @SuppressWarnings("unchecked") protected final C container(){return(C)inventorySlots;}

    @Override public void initGui(){xSize=width;ySize=height;super.initGui();guiLeft=0;guiTop=0;Keyboard.enableRepeatEvents(true);MinecraftUiPlatform.INSTANCE.initialize();if(runtime!=null)runtime.close();runtime=new UiRuntime(createDocument(),new UiContext(uiTheme(),Locale.CHINA,scaleFactor(),new SystemUiClock(isReducedMotion()),MinecraftUiPlatform.INSTANCE.fonts(),com.jsirgalaxybase.ui2.resource.ResourceResolver.EMPTY));runtime.setViewport(width,height);}
    @Override protected final void drawGuiContainerBackgroundLayer(float partialTicks,int mouseX,int mouseY){long started=System.nanoTime();current=runtime.frame();UiRenderDiagnostics.INSTANCE.recordFrame(System.nanoTime()-started);MinecraftUiRenderer renderer=renderer();renderer.render(current,Integer.MIN_VALUE,UiLayer.NATIVE,true,uiClipBounds());syncNativeSlots(current);drawNativeSlotBackgrounds();}
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY){if(current!=null){drawExternalContent(current,mouseX,mouseY);renderer().render(current,UiLayer.NATIVE_OVERLAY,UiLayer.TOOLTIP,false,uiClipBounds());}}
    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){super.drawScreen(mouseX,mouseY,partialTicks);if(current==null)current=runtime.frame();renderer().render(current,UiLayer.TOOLTIP,Integer.MAX_VALUE,false,uiClipBounds());if(!runtime.hasModal())drawExternalTooltips(current,mouseX,mouseY);}
    @Override protected void mouseClicked(int x,int y,int button){boolean modal=runtime.hasModal();boolean consumed=runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,x,y,button));if(modal||runtime.hasModal()||consumed)return;super.mouseClicked(x,y,button);}
    @Override protected void mouseMovedOrUp(int x,int y,int button){boolean modal=runtime.hasModal();boolean consumed=runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_UP,x,y,button));if(modal||runtime.hasModal()||consumed)return;super.mouseMovedOrUp(x,y,button);}
    @Override public void handleMouseInput(){super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel==0)return;int x=Mouse.getEventX()*width/mc.displayWidth,y=height-Mouse.getEventY()*height/mc.displayHeight-1;runtime.dispatch(UiEvent.scroll(x,y,wheel));}
    @Override protected void keyTyped(char typedChar,int keyCode){UiKeyCode semantic=MinecraftKeyMapper.map(keyCode);boolean consumed=semantic!=UiKeyCode.UNKNOWN&&runtime.dispatch(UiEvent.key(semantic));if(!consumed&&typedChar!='\0')consumed=runtime.dispatch(UiEvent.text(typedChar));if(!consumed)consumed=handleUnhandledKey(typedChar,keyCode);if(!consumed&&keyCode==Keyboard.KEY_ESCAPE){if(parent==null)mc.thePlayer.closeScreen();else mc.displayGuiScreen(parent);}else if(!consumed)super.keyTyped(typedChar,keyCode);}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);if(runtime!=null)runtime.close();super.onGuiClosed();}
    @Override public boolean doesGuiPauseGame(){return false;}
    protected final void invalidateUi(){if(runtime!=null)runtime.invalidate();}
    protected final DrawList currentDrawList(){return current;}
    protected final UiRuntime uiRuntime(){return runtime;}
    protected void drawExternalContent(DrawList list,int mouseX,int mouseY){}
    protected void drawExternalTooltips(DrawList list,int mouseX,int mouseY){}
    protected boolean handleUnhandledKey(char typedChar,int keyCode){return false;}
    protected boolean isReducedMotion(){return false;}
    protected com.jsirgalaxybase.ui2.geometry.UiRect uiClipBounds(){return null;}
    protected UiTheme uiTheme(){return DarkIndustrialTheme.create();}
    protected abstract UiDocument createDocument();
    protected abstract void syncNativeSlots(DrawList list);
    protected abstract void drawNativeSlotBackgrounds();
    @Override public final DrawList ui2DebugDrawList(){return current;}
    @Override public final com.jsirgalaxybase.ui2.core.UiNode ui2DebugRoot(){return runtime==null?null:runtime.getRoot();}
    @Override public final int ui2DebugWidth(){return width;}
    @Override public final int ui2DebugHeight(){return height;}
    private int scaleFactor(){return new ScaledResolution(mc,mc.displayWidth,mc.displayHeight).getScaleFactor();}
    private MinecraftUiRenderer renderer(){return new MinecraftUiRenderer(MinecraftUiPlatform.INSTANCE.fonts(),scaleFactor(),mc.displayHeight);}
}

package com.jsirgalaxybase.ui2.terminal;

import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;

public final class TerminalVisualMetrics {
    private final UiRect bounds; private final int navigationWidth;
    private TerminalVisualMetrics(UiRect bounds,int navigationWidth){this.bounds=bounds;this.navigationWidth=navigationWidth;}
    public static TerminalVisualMetrics compute(UiSize viewport,TerminalWindowProfile profile){
        int vw=viewport==null?1:Math.max(1,viewport.getWidth()),vh=viewport==null?1:Math.max(1,viewport.getHeight());
        TerminalWindowProfile actual=profile==null?TerminalWindowProfile.STANDARD:profile;
        int width=fit(vw,350,620,actual.getWidthRatio(),8),height=fit(vh,193,340,actual.getHeightRatio(),8);
        return new TerminalVisualMetrics(new UiRect(Math.max(0,(vw-width)/2),Math.max(0,(vh-height)/2),width,height),width<400?56:width<520?64:72);
    }
    private static int fit(int available,int minimum,int maximum,float ratio,int margin){int capped=Math.min(maximum,Math.max(1,available-margin));if(available<=minimum)return available;return Math.min(capped,Math.max(Math.min(minimum,capped),Math.round(available*ratio)));}
    public UiRect getBounds(){return bounds;} public int getNavigationWidth(){return navigationWidth;}
    public int getMainWidth(){return Math.max(0,bounds.getWidth()-navigationWidth);}
    public Insets viewportInsets(UiSize viewport){int w=viewport==null?bounds.getRight():viewport.getWidth(),h=viewport==null?bounds.getBottom():viewport.getHeight();return new Insets(bounds.getY(),Math.max(0,w-bounds.getRight()),Math.max(0,h-bounds.getBottom()),bounds.getX());}
}

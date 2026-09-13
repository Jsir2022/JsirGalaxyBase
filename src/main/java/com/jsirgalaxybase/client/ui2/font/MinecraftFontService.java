package com.jsirgalaxybase.client.ui2.font;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.text.TextMeasurer;
import com.jsirgalaxybase.ui2.text.TextStyle;

/** Synchronous UI2 adapter for Minecraft's active, resource-pack-aware FontRenderer. */
public final class MinecraftFontService implements TextMeasurer {
    @Override public UiSize measure(String text,TextStyle style,int maxWidth){
        String value=text==null?"":text;TextStyle actual=style==null?TextStyle.BODY:style;
        FontRenderer renderer=Minecraft.getMinecraft().fontRenderer;
        int width=FontMeasurementBounds.width(renderer.getStringWidth(rendered(value,actual)),
            scale(actual,renderer),maxWidth);
        return new UiSize(width,Math.max(0,actual.getLineHeight()));
    }

    public void render(String text,UiRect bounds,int argb,TextStyle style){
        String value=text==null?"":text;TextStyle actual=style==null?TextStyle.BODY:style;
        if(value.isEmpty()||bounds==null)return;
        FontRenderer renderer=Minecraft.getMinecraft().fontRenderer;
        float scale=scale(actual,renderer);String rendered=rendered(value,actual);
        float width=FontMeasurementBounds.drawWidth(renderer.getStringWidth(rendered),scale),x=bounds.getX();
        if(actual.getAlign()==TextStyle.Align.CENTER)x+=(bounds.getWidth()-width)/2F;
        else if(actual.getAlign()==TextStyle.Align.RIGHT)x+=bounds.getWidth()-width;
        float height=renderer.FONT_HEIGHT*scale;
        float y=bounds.getY()+Math.max(0F,(bounds.getHeight()-height)/2F);
        GL11.glPushMatrix();
        try{
            GL11.glTranslatef(x,y,0F);GL11.glScalef(scale,scale,1F);
            renderer.drawString(rendered,0,0,argb,false);
        }finally{
            GL11.glPopMatrix();GL11.glColor4f(1F,1F,1F,1F);
        }
    }

    private static float scale(TextStyle style,FontRenderer renderer){
        return Math.max(0.25F,style.getSize()/(float)Math.max(1,renderer.FONT_HEIGHT));
    }
    private static String rendered(String value,TextStyle style){return style.isBold()?"\u00a7l"+value:value;}
}

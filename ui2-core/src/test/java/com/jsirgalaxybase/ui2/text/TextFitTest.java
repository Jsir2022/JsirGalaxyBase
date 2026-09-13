package com.jsirgalaxybase.ui2.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiSize;

public class TextFitTest {
    private static final TextMeasurer MEASURER=new TextMeasurer(){
        @Override public UiSize measure(String text,TextStyle style,int maxWidth){
            int width=(text==null?0:text.length())*style.getSize();
            return new UiSize(Math.min(Math.max(0,maxWidth),width),style.getLineHeight());
        }
    };

    @Test public void keepsThemeSizeWhenTextFits(){
        TextStyle source=new TextStyle("body",10,false,12,TextStyle.Align.LEFT);
        assertEquals(source,TextFit.singleLine("abc",source,MEASURER,30,12));
    }

    @Test public void shrinksOnlyAsMuchAsRequiredWithinReadableFloor(){
        TextStyle source=new TextStyle("body",10,false,12,TextStyle.Align.LEFT);
        TextStyle fitted=TextFit.singleLine("abcd",source,MEASURER,32,12);
        assertEquals(8,fitted.getSize());
        assertTrue(fitted.getLineHeight()<=12);
    }

    @Test public void neverShrinksBelowReadableFloor(){
        TextStyle source=new TextStyle("title",17,true,21,TextStyle.Align.CENTER);
        TextStyle fitted=TextFit.singleLine("very long text",source,MEASURER,10,8);
        assertEquals(12,fitted.getSize());
    }

    @Test public void multilineKeepsItsSemanticCeilingWhenContentFits(){
        TextStyle source=new TextStyle("body",10,false,12,TextStyle.Align.LEFT);
        assertEquals(source,TextFit.block("abcd",source,MEASURER,20,24,2));
    }

    @Test public void multilineShrinksOnlyWhenTheWholeBlockWouldOverflow(){
        TextStyle source=new TextStyle("body",10,false,12,TextStyle.Align.LEFT);
        TextStyle fitted=TextFit.block("abcdef",source,MEASURER,24,20,2);
        assertEquals(8,fitted.getSize());
        assertTrue(fitted.getSize()<=source.getSize());
    }
}

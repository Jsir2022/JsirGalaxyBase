package com.jsirgalaxybase.ui2.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiSize;

public class TextFlowTest {
    private static final TextStyle STYLE=new TextStyle("body",10,false,12,TextStyle.Align.LEFT);
    private static final TextMeasurer MONO=new TextMeasurer(){
        @Override public UiSize measure(String text,TextStyle style,int maxWidth){
            int width=(text==null?0:text.codePointCount(0,text.length()))*5;
            return new UiSize(Math.min(Math.max(0,maxWidth),width),style.getLineHeight());
        }
    };

    @Test public void wrapsChineseWithoutDependingOnSpaces(){
        TextFlow.Result result=TextFlow.layout("公共任务与福利服务",STYLE,MONO,20,4);
        assertEquals(3,result.getLines().size());
        assertFalse(result.isTruncated());
    }

    @Test public void truncatesLastVisibleLineWithEllipsis(){
        TextFlow.Result result=TextFlow.layout("abcdefghijk",STYLE,MONO,20,2);
        assertEquals(2,result.getLines().size());
        assertEquals("efg\u2026",result.getLines().get(1));
        assertTrue(result.isTruncated());
    }

    @Test public void exactLastLineIsNotMarkedTruncated(){
        TextFlow.Result result=TextFlow.layout("abcdefgh",STYLE,MONO,20,2);
        assertEquals("efgh",result.getLines().get(1));
        assertFalse(result.isTruncated());
    }

    @Test public void keepsSurrogatePairsWhole(){
        TextFlow.Result result=TextFlow.layout("A\ud83d\ude80BC",STYLE,MONO,10,3);
        assertEquals("A\ud83d\ude80",result.getLines().get(0));
        assertEquals("BC",result.getLines().get(1));
    }

    @Test public void overwideSingleGlyphFallsBackToEllipsisWithoutEscaping(){
        TextStyle huge=new TextStyle("body",10,false,12,TextStyle.Align.LEFT);
        TextMeasurer wide=new TextMeasurer(){@Override public UiSize measure(String text,TextStyle style,int maxWidth){return new UiSize("…".equals(text)?5:(text==null?0:text.length())*20,style.getLineHeight());}};
        TextFlow.Result result=TextFlow.layout("宽",huge,wide,10,1);
        assertTrue(result.isTruncated());
        assertEquals("…",result.getLines().get(0));
    }
}

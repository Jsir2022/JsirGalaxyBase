package com.jsirgalaxybase.ui2.text;

import com.jsirgalaxybase.ui2.geometry.UiSize;

/** Resolves the largest readable fixed-theme style that fits a constrained single-line control. */
public final class TextFit {
    private TextFit() {}

    public static TextStyle singleLine(String text, TextStyle preferred, TextMeasurer measurer,
        int maxWidth, int maxHeight) {
        return block(text,preferred,measurer,maxWidth,maxHeight,1);
    }

    /** Treats the declared style as a ceiling and only shrinks when the complete block overflows. */
    public static TextStyle block(String text,TextStyle preferred,TextMeasurer measurer,
        int maxWidth,int maxHeight,int maxLines){
        if (preferred == null || measurer == null) throw new IllegalArgumentException("style and measurer are required");
        int width=Math.max(0,maxWidth),height=Math.max(0,maxHeight),lines=Math.max(0,maxLines);
        int minimum=Math.min(preferred.getSize(),Math.max(7,Math.round(preferred.getSize()*0.72F)));
        for(int size=preferred.getSize();size>=minimum;size--){
            TextStyle candidate=scaled(preferred,size);
            int visible=Math.min(lines,height/Math.max(1,candidate.getLineHeight()));
            if(visible<=0)continue;
            TextFlow.Result flow=TextFlow.layout(text,candidate,measurer,width,visible);
            if(!flow.isTruncated()&&flow.height(candidate)<=height)return candidate;
        }
        return scaled(preferred,minimum);
    }

    private static TextStyle scaled(TextStyle source,int size){
        int line=Math.max(size,Math.round(source.getLineHeight()*size/(float)source.getSize()));
        return new TextStyle(source.getToken(),size,source.isBold(),line,source.getAlign());
    }
}

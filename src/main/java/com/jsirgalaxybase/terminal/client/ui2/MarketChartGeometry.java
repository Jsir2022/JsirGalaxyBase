package com.jsirgalaxybase.terminal.client.ui2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.ui2.geometry.UiRect;

/** Saturating, renderer-independent market chart projection. */
final class MarketChartGeometry {
    static final class Point {
        final int x, openY, highY, lowY, closeY, volumeTop;
        final TerminalMarketSectionModel.PricePointModel source;
        Point(int x,int openY,int highY,int lowY,int closeY,int volumeTop,TerminalMarketSectionModel.PricePointModel source){this.x=x;this.openY=openY;this.highY=highY;this.lowY=lowY;this.closeY=closeY;this.volumeTop=volumeTop;this.source=source;}
    }
    static final class Projection {
        final UiRect priceArea,volumeArea;final List<Point> points;final long minimum,maximum;
        Projection(UiRect p,UiRect v,List<Point> pts,long min,long max){priceArea=p;volumeArea=v;points=Collections.unmodifiableList(pts);minimum=min;maximum=max;}
    }
    private MarketChartGeometry(){}
    static Projection project(List<TerminalMarketSectionModel.PricePointModel> values,UiRect bounds){
        int pad=8,volume=Math.max(10,bounds.getHeight()/5);UiRect p=new UiRect(bounds.getX()+pad,bounds.getY()+4,Math.max(0,bounds.getWidth()-pad-22),Math.max(0,bounds.getHeight()-volume-12));UiRect v=new UiRect(p.getX(),p.getBottom()+3,p.getWidth(),Math.max(0,volume-5));
        List<TerminalMarketSectionModel.PricePointModel> safe=values==null?Collections.<TerminalMarketSectionModel.PricePointModel>emptyList():values;long min=Long.MAX_VALUE,max=0,maxVolume=1;
        for(TerminalMarketSectionModel.PricePointModel q:safe){if(q==null||q.isEmpty())continue;min=Math.min(min,q.getLow());max=Math.max(max,q.getHigh());maxVolume=Math.max(maxVolume,q.getQuantity());}
        if(min==Long.MAX_VALUE){min=0;max=1;}if(max<=min)max=min==Long.MAX_VALUE?Long.MAX_VALUE:min+1;
        List<Point> result=new ArrayList<Point>();int count=safe.size();
        for(int i=0;i<count;i++){TerminalMarketSectionModel.PricePointModel q=safe.get(i);int x=p.getX()+(count<=1?p.getWidth()/2:(int)((long)i*Math.max(0,p.getWidth()-1)/(count-1)));int volumeHeight=(int)Math.round((q.getQuantity()/(double)maxVolume)*v.getHeight());result.add(new Point(x,y(q.getOpen(),min,max,p),y(q.getHigh(),min,max,p),y(q.getLow(),min,max,p),y(q.getPrice(),min,max,p),v.getBottom()-volumeHeight,q));}
        return new Projection(p,v,result,min,max);
    }
    private static int y(long value,long min,long max,UiRect b){if(b.getHeight()<=1)return b.getY();double span=(double)max-(double)min;double ratio=span<=0D?0D:((double)value-(double)min)/span;ratio=Math.max(0D,Math.min(1D,ratio));return b.getBottom()-1-(int)Math.round(ratio*(b.getHeight()-1));}
}

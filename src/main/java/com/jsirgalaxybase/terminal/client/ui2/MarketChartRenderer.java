package com.jsirgalaxybase.terminal.client.ui2;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.ui2.geometry.UiRect;

/** Minecraft backend for UI2 market charts and hover quote cards. */
final class MarketChartRenderer {
    void draw(UiRect bounds,List<TerminalMarketSectionModel.PricePointModel> values,int mouseX,int mouseY,boolean compact){
        drawInternal(bounds,values,mouseX,mouseY,compact,null,null);
    }
    void drawDetail(UiRect bounds,List<TerminalMarketSectionModel.PricePointModel> values,int mouseX,int mouseY,
        boolean compact,String range,String referencePrice){
        drawInternal(bounds,values,mouseX,mouseY,compact,range,referencePrice);
    }
    private void drawInternal(UiRect bounds,List<TerminalMarketSectionModel.PricePointModel> values,int mouseX,int mouseY,
        boolean compact,String range,String referencePrice){
        if(bounds==null)return;
        Gui.drawRect(bounds.getX(),bounds.getY(),bounds.getRight(),bounds.getBottom(),0xE806101A);
        UiRect plot=bounds;
        if(range!=null){
            font("价格走势 · "+range,bounds.getX()+8,bounds.getY()+5,0xFF9FB0C2);
            plot=new UiRect(bounds.getX(),bounds.getY()+15,bounds.getWidth(),Math.max(0,bounds.getHeight()-15));
        }
        MarketChartGeometry.Projection p=MarketChartGeometry.project(values,plot);
        grid(p.priceArea);
        if(p.points.isEmpty()){
            int y=p.priceArea.getY()+p.priceArea.getHeight()/2;
            Gui.drawRect(p.priceArea.getX(),y,p.priceArea.getRight(),y+1,0x405C7894);
            center(plot,"暂无成交数据"+(referencePrice==null||referencePrice.isEmpty()?"":" · 参考 "+referencePrice));
            return;
        }
        boolean sparse=trades(p.points)<3;if(sparse)line(p.points,0xFF48A8ED);else candles(p.points);
        volumes(p.points,p.volumeArea);if(bounds.contains(mouseX,mouseY))crosshair(p,mouseX,mouseY,compact);
    }
    void hoverCard(UiRect anchor,TerminalMarketSectionModel.CatalogProductModel product,int mouseX,int mouseY){if(anchor==null||product==null||!anchor.contains(mouseX,mouseY))return;int w=150,h=92,x=Math.min(Minecraft.getMinecraft().currentScreen.width-w-4,mouseX+8),y=Math.min(Minecraft.getMinecraft().currentScreen.height-h-4,mouseY+8);x=Math.max(4,x);y=Math.max(4,y);Gui.drawRect(x,y,x+w,y+h,0xF208111B);Gui.drawRect(x,y,x+w,y+1,0xFF3D9DE0);font(product.getDisplayName(),x+7,y+6,0xFFFFFFFF);font("最新  "+product.getMarketSummary().getLatestTrade()+"   "+product.getMarketSummary().getDayChange(),x+7,y+20,color(product.getMarketSummary().getDayChange()));font("买一  "+product.getMarketSummary().getBestBid(),x+7,y+33,0xFF65D488);font("卖一  "+product.getMarketSummary().getBestAsk(),x+78,y+33,0xFFE36A68);font("24h量 "+product.getMarketSummary().getVolume24h()+"  可卖 "+product.getMarketSummary().getAvailable(),x+7,y+46,0xFFAAB8C8);draw(new UiRect(x+6,y+59,w-12,27),product.getMarketSummary().getPricePoints(),-1,-1,true);}
    private void grid(UiRect b){for(int i=1;i<4;i++){int y=b.getY()+i*b.getHeight()/4;Gui.drawRect(b.getX(),y,b.getRight(),y+1,0x283A526B);}for(int i=1;i<6;i++){int x=b.getX()+i*b.getWidth()/6;Gui.drawRect(x,b.getY(),x+1,b.getBottom(),0x183A526B);}}
    private void candles(List<MarketChartGeometry.Point> pts){for(MarketChartGeometry.Point p:pts){int c=p.source.getPrice()>=p.source.getOpen()?0xFF49C77A:0xFFE35D61;Gui.drawRect(p.x,p.highY,p.x+1,p.lowY+1,c);Gui.drawRect(p.x-1,Math.min(p.openY,p.closeY),p.x+2,Math.max(p.openY,p.closeY)+1,c);}}
    private void line(List<MarketChartGeometry.Point> pts,int color){for(int i=1;i<pts.size();i++)segment(pts.get(i-1).x,pts.get(i-1).closeY,pts.get(i).x,pts.get(i).closeY,color);if(pts.size()==1)Gui.drawRect(pts.get(0).x-1,pts.get(0).closeY-1,pts.get(0).x+2,pts.get(0).closeY+2,color);}
    private void segment(int x0,int y0,int x1,int y1,int color){int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,err=dx+dy;for(;;){Gui.drawRect(x0,y0,x0+1,y0+1,color);if(x0==x1&&y0==y1)break;int twice=err*2;if(twice>=dy){err+=dy;x0+=sx;}if(twice<=dx){err+=dx;y0+=sy;}}}
    private void volumes(List<MarketChartGeometry.Point> pts,UiRect area){for(MarketChartGeometry.Point p:pts)Gui.drawRect(p.x-1,p.volumeTop,p.x+2,area.getBottom(),p.source.getPrice()>=p.source.getOpen()?0xAA3BA765:0xAABD4B50);}
    private void crosshair(MarketChartGeometry.Projection p,int mx,int my,boolean compact){if(p.points.isEmpty()||!p.priceArea.contains(mx,my))return;MarketChartGeometry.Point q=p.points.get(0);int distance=Math.abs(q.x-mx);for(MarketChartGeometry.Point v:p.points)if(Math.abs(v.x-mx)<distance){q=v;distance=Math.abs(v.x-mx);}Gui.drawRect(q.x,p.priceArea.getY(),q.x+1,p.volumeArea.getBottom(),0x88FFFFFF);Gui.drawRect(p.priceArea.getX(),q.closeY,p.priceArea.getRight(),q.closeY+1,0x66FFFFFF);if(compact)return;int x=Math.min(p.priceArea.getRight()-82,q.x+5),y=Math.max(p.priceArea.getY()+2,q.closeY-30);Gui.drawRect(x,y,x+80,y+29,0xE008111B);font("开 "+q.source.getOpen()+" 高 "+q.source.getHigh(),x+4,y+4,0xFFE7EDF5);font("低 "+q.source.getLow()+" 收 "+q.source.getPrice(),x+4,y+14,0xFFE7EDF5);}
    private int trades(List<MarketChartGeometry.Point> pts){int n=0;for(MarketChartGeometry.Point p:pts)if(p.source.isTrade())n++;return n;}
    private void center(UiRect b,String s){int w=Minecraft.getMinecraft().fontRenderer.getStringWidth(s);font(s,b.getX()+(b.getWidth()-w)/2,b.getY()+b.getHeight()/2-4,0xFF8293A6);}
    private void font(String s,int x,int y,int c){Minecraft.getMinecraft().fontRenderer.drawString(s,x,y,c,false);}private int color(String s){return s!=null&&s.startsWith("-")?0xFFE36A68:0xFF65D488;}
}

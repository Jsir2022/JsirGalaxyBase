package com.jsirgalaxybase.ui2.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Renderer-neutral standard market snapshot used by catalog and detail documents. */
public final class MarketVisualModel {
    public enum View { CATALOG, DETAIL }
    private final TerminalVisualModel shell; private final View view; private final List<Product> products;
    private final String selectedId, range, freshness;
    public MarketVisualModel(TerminalVisualModel shell,View view,List<Product> products,String selectedId,String range,String freshness){if(shell==null)throw new IllegalArgumentException("shell is required");this.shell=shell;this.view=view==null?View.CATALOG:view;this.products=products==null?Collections.<Product>emptyList():Collections.unmodifiableList(new ArrayList<Product>(products));this.selectedId=selectedId==null?"":selectedId;this.range=range==null?"24h":range;this.freshness=freshness==null?"最新":freshness;}
    public TerminalVisualModel getShell(){return shell;}public View getView(){return view;}public List<Product> getProducts(){return products;}public String getSelectedId(){return selectedId;}public String getRange(){return range;}public String getFreshness(){return freshness;}
    public Product selected(){for(Product p:products)if(p.getItem().getId().equals(selectedId))return p;return products.isEmpty()?null:products.get(0);}
    public static final class Product{private final VisualItem item;private final long latest,change,volume;private final List<Point> points;public Product(VisualItem item,long latest,long change,long volume,List<Point> points){if(item==null)throw new IllegalArgumentException("item is required");this.item=item;this.latest=latest;this.change=change;this.volume=volume;this.points=points==null?Collections.<Point>emptyList():Collections.unmodifiableList(new ArrayList<Point>(points));}public VisualItem getItem(){return item;}public long getLatest(){return latest;}public long getChange(){return change;}public long getVolume(){return volume;}public List<Point> getPoints(){return points;}}
    public static final class Point{private final long time,value,volume;public Point(long time,long value,long volume){this.time=time;this.value=value;this.volume=Math.max(0,volume);}public long getTime(){return time;}public long getValue(){return value;}public long getVolume(){return volume;}}
}

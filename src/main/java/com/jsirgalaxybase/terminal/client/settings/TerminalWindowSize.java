package com.jsirgalaxybase.terminal.client.settings;

public enum TerminalWindowSize {
    COMPACT(0.78F,0.74F), STANDARD(0.90F,0.88F), LARGE(0.96F,0.94F);
    private final float widthRatio,heightRatio;
    TerminalWindowSize(float widthRatio,float heightRatio){this.widthRatio=widthRatio;this.heightRatio=heightRatio;}
    public float getWidthRatio(){return widthRatio;}
    public float getHeightRatio(){return heightRatio;}
}

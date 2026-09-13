package com.jsirgalaxybase.ui2.terminal;

public enum TerminalWindowProfile {
    COMPACT(0.78F, 0.74F), STANDARD(0.90F, 0.88F), LARGE(0.96F, 0.94F);
    private final float widthRatio, heightRatio;
    TerminalWindowProfile(float widthRatio, float heightRatio) { this.widthRatio=widthRatio;this.heightRatio=heightRatio; }
    public float getWidthRatio(){return widthRatio;} public float getHeightRatio(){return heightRatio;}
}

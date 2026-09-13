package com.jsirgalaxybase.terminal.client.settings;

import java.util.Objects;

/** Immutable client-only terminal appearance and accessibility preferences. */
public final class TerminalPreferences {
    public static final String DEFAULT_THEME="glass_mono";
    private final String themeId;
    private final TerminalWindowSize windowSize;
    private final boolean reducedMotion;
    private final int tooltipDelayMillis;

    public TerminalPreferences(String themeId,TerminalWindowSize windowSize,
        boolean reducedMotion,int tooltipDelayMillis){
        if(themeId==null||themeId.trim().isEmpty()||windowSize==null)throw new IllegalArgumentException("invalid terminal preferences");
        this.themeId=themeId.trim();this.windowSize=windowSize;
        this.reducedMotion=reducedMotion;this.tooltipDelayMillis=Math.max(0,Math.min(2000,tooltipDelayMillis));
    }
    public static TerminalPreferences defaults(){return new TerminalPreferences(DEFAULT_THEME,TerminalWindowSize.STANDARD,false,350);}
    public String getThemeId(){return themeId;}
    public TerminalWindowSize getWindowSize(){return windowSize;}
    public boolean isReducedMotion(){return reducedMotion;}
    public int getTooltipDelayMillis(){return tooltipDelayMillis;}
    public TerminalPreferences withTheme(String value){return new TerminalPreferences(value,windowSize,reducedMotion,tooltipDelayMillis);}
    public TerminalPreferences withWindowSize(TerminalWindowSize value){return new TerminalPreferences(themeId,value,reducedMotion,tooltipDelayMillis);}
    public TerminalPreferences withReducedMotion(boolean value){return new TerminalPreferences(themeId,windowSize,value,tooltipDelayMillis);}
    public TerminalPreferences withTooltipDelay(int value){return new TerminalPreferences(themeId,windowSize,reducedMotion,value);}
    @Override public boolean equals(Object value){if(this==value)return true;if(!(value instanceof TerminalPreferences))return false;TerminalPreferences other=(TerminalPreferences)value;return reducedMotion==other.reducedMotion&&tooltipDelayMillis==other.tooltipDelayMillis&&themeId.equals(other.themeId)&&windowSize==other.windowSize;}
    @Override public int hashCode(){return Objects.hash(themeId,windowSize,reducedMotion,tooltipDelayMillis);}
}

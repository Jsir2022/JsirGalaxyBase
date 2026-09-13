package com.jsirgalaxybase.ui2.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TerminalSettingsVisualModel {
    private final TerminalVisualModel shell;private final List<ThemeChoice> themes;private final String themeId;
    private final TerminalWindowProfile windowProfile;private final boolean reducedMotion,helpOpen;
    public TerminalSettingsVisualModel(TerminalVisualModel shell,List<ThemeChoice> themes,String themeId,TerminalWindowProfile windowProfile,boolean reducedMotion,boolean helpOpen){if(shell==null)throw new IllegalArgumentException("shell is required");this.shell=shell;this.themes=Collections.unmodifiableList(new ArrayList<ThemeChoice>(themes==null?Collections.<ThemeChoice>emptyList():themes));this.themeId=themeId==null?"":themeId;this.windowProfile=windowProfile==null?TerminalWindowProfile.STANDARD:windowProfile;this.reducedMotion=reducedMotion;this.helpOpen=helpOpen;}
    public TerminalVisualModel getShell(){return shell;}public List<ThemeChoice> getThemes(){return themes;}public String getThemeId(){return themeId;}public TerminalWindowProfile getWindowProfile(){return windowProfile;}public boolean isReducedMotion(){return reducedMotion;}public boolean isHelpOpen(){return helpOpen;}
    public static final class ThemeChoice{private final String id,label;public ThemeChoice(String id,String label){if(id==null||id.trim().isEmpty())throw new IllegalArgumentException("id is required");this.id=id;this.label=label==null?id:label;}public String getId(){return id;}public String getLabel(){return label;}}
}

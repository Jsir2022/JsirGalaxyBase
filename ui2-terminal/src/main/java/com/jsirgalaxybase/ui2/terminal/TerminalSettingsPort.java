package com.jsirgalaxybase.ui2.terminal;

public interface TerminalSettingsPort {
    TerminalSettingsVisualModel current();
    void selectTheme(String id);
    void selectWindow(TerminalWindowProfile profile);
    void toggleMotion();
    void reset();
    void openHelp();
    void closeHelp();
}

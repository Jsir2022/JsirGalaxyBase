package com.jsirgalaxybase.ui2.terminal;

/** Side-effect boundary shared by live Minecraft hosts and no-op preview scenarios. */
public interface TerminalActionPort {
    void navigate(String pageId);
    void refresh();
    void help();
    void back();
    void close();

    TerminalActionPort NONE = new TerminalActionPort() {
        @Override public void navigate(String pageId) {}
        @Override public void refresh() {}
        @Override public void help() {}
        @Override public void back() {}
        @Override public void close() {}
    };
}

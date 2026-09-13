package com.jsirgalaxybase.ui2.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete read-only data required by the production home document. */
public final class HomeVisualModel {
    private final TerminalVisualModel shell;
    private final String title, lead, status;
    private final List<Section> sections;

    public HomeVisualModel(TerminalVisualModel shell, String title, String lead, String status, List<Section> sections) {
        if (shell == null) throw new IllegalArgumentException("shell is required");
        this.shell = shell; this.title = safe(title); this.lead = safe(lead); this.status = safe(status);
        this.sections = Collections.unmodifiableList(new ArrayList<Section>(sections == null
            ? Collections.<Section>emptyList() : sections));
    }
    private static String safe(String value) { return value == null ? "" : value; }
    public TerminalVisualModel getShell() { return shell; }
    public String getTitle() { return title; }
    public String getLead() { return lead; }
    public String getStatus() { return status; }
    public List<Section> getSections() { return sections; }

    public static final class Section {
        private final String title, summary, detail;
        public Section(String title, String summary, String detail) {
            this.title = safe(title); this.summary = safe(summary); this.detail = safe(detail);
        }
        public String getTitle() { return title; }
        public String getSummary() { return summary; }
        public String getDetail() { return detail; }
    }
}

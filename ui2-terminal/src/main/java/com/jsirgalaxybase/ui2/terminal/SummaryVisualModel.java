package com.jsirgalaxybase.ui2.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SummaryVisualModel {
    private final TerminalVisualModel shell;private final String pageId,title,lead;private final List<HomeVisualModel.Section> sections;
    public SummaryVisualModel(TerminalVisualModel shell,String pageId,String title,String lead,List<HomeVisualModel.Section> sections){if(shell==null)throw new IllegalArgumentException("shell is required");this.shell=shell;this.pageId=pageId==null?"":pageId;this.title=title==null?"":title;this.lead=lead==null?"":lead;this.sections=sections==null?Collections.<HomeVisualModel.Section>emptyList():Collections.unmodifiableList(new ArrayList<HomeVisualModel.Section>(sections));}
    public TerminalVisualModel getShell(){return shell;}public String getPageId(){return pageId;}public String getTitle(){return title;}public String getLead(){return lead;}public List<HomeVisualModel.Section> getSections(){return sections;}
}

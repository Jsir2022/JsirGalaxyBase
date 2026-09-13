package com.jsirgalaxybase.terminal.client.ui2;

import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.settings.TerminalAppearance;
import com.jsirgalaxybase.ui2.terminal.HomeVisualModel;
import com.jsirgalaxybase.ui2.terminal.TerminalVisualModel;
import com.jsirgalaxybase.ui2.terminal.TerminalWindowProfile;
import com.jsirgalaxybase.ui2.terminal.SummaryVisualModel;

/** Converts network-backed client view models into platform-neutral UI2 values. */
public final class TerminalVisualModelAdapter {
    private TerminalVisualModelAdapter() {}
    public static TerminalVisualModel shell(TerminalHomeScreenModel value){TerminalHomeScreenModel model=value==null?TerminalHomeScreenModel.placeholder():value;List<TerminalVisualModel.NavItem> nav=new ArrayList<TerminalVisualModel.NavItem>();for(TerminalHomeScreenModel.NavItemModel item:model.getNavItems())nav.add(new TerminalVisualModel.NavItem(item.getPageId(),item.getLabel(),item.isEnabled(),item.isSelected()));return new TerminalVisualModel(model.getTerminalTitle(),model.getSelectedPageId(),nav);}
    public static HomeVisualModel home(TerminalHomeScreenModel value){TerminalHomeScreenModel model=value==null?TerminalHomeScreenModel.placeholder():value;TerminalHomeScreenModel.PageSnapshotModel page=model.getPageSnapshot("home");List<HomeVisualModel.Section> sections=new ArrayList<HomeVisualModel.Section>();for(TerminalHomeScreenModel.SectionModel section:page.getSections())sections.add(new HomeVisualModel.Section(section.getTitle(),section.getSummary(),section.getDetail()));String status=model.getStatusBand().getHeadline()+" · "+model.getStatusBand().getBadgeLabel()+" "+model.getStatusBand().getBadgeValue();return new HomeVisualModel(shell(model),page.getTitle(),page.getLead(),status,sections);}
    public static TerminalWindowProfile windowProfile(){return TerminalWindowProfile.valueOf(TerminalAppearance.INSTANCE.preferences().getWindowSize().name());}
    public static SummaryVisualModel summary(String pageId,TerminalHomeScreenModel value){TerminalHomeScreenModel model=value==null?TerminalHomeScreenModel.placeholder():value;TerminalHomeScreenModel.PageSnapshotModel page=model.getPageSnapshot(pageId);List<HomeVisualModel.Section> sections=new ArrayList<HomeVisualModel.Section>();for(TerminalHomeScreenModel.SectionModel section:page.getSections())sections.add(new HomeVisualModel.Section(section.getTitle(),section.getSummary(),section.getDetail()));return new SummaryVisualModel(shell(model),pageId,page.getTitle(),page.getLead(),sections);}
}

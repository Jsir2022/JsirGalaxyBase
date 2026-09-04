package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalNotificationCenterModel;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

/** Client-local filters for the already player-scoped notification snapshot. */
public final class TerminalNotificationCenterState {

    public enum SourceFilter { ALL, MARKET, TRANSFER, LAND, BANK, OTHER }
    public enum SeverityFilter { ALL, ERROR, WARNING, SUCCESS, INFO }

    private SourceFilter source = SourceFilter.ALL;
    private SeverityFilter severity = SeverityFilter.ALL;
    private int pageIndex;

    public SourceFilter getSource() { return source; }
    public SeverityFilter getSeverity() { return severity; }
    public void setSource(SourceFilter value) { source = value == null ? SourceFilter.ALL : value; pageIndex = 0; }
    public void setSeverity(SeverityFilter value) { severity = value == null ? SeverityFilter.ALL : value; pageIndex = 0; }
    public int getPageIndex() { return pageIndex; }
    public void setPageIndex(int value) { pageIndex = Math.max(0, value); }

    public List<TerminalNotificationCenterModel.EntryModel> filtered(TerminalNotificationCenterModel model) {
        List<TerminalNotificationCenterModel.EntryModel> result =
            new ArrayList<TerminalNotificationCenterModel.EntryModel>();
        if (model == null) return result;
        for (TerminalNotificationCenterModel.EntryModel entry : model.getEntries()) {
            if (entry != null && matchesSource(entry) && matchesSeverity(entry)) result.add(entry);
        }
        return result;
    }

    private boolean matchesSource(TerminalNotificationCenterModel.EntryModel entry) {
        if (source == SourceFilter.ALL) return true;
        String value = entry.getSourceId() == null ? "" : entry.getSourceId().toLowerCase(java.util.Locale.ROOT);
        if (source == SourceFilter.MARKET) return value.contains("market") || value.contains("vault");
        if (source == SourceFilter.TRANSFER) return value.contains("server") || value.contains("transfer") || value.contains("tpa");
        if (source == SourceFilter.LAND) return value.contains("land") || value.contains("property");
        if (source == SourceFilter.BANK) return value.contains("bank");
        return !(value.contains("market") || value.contains("vault") || value.contains("server") || value.contains("transfer")
            || value.contains("tpa") || value.contains("land") || value.contains("property") || value.contains("bank"));
    }

    private boolean matchesSeverity(TerminalNotificationCenterModel.EntryModel entry) {
        if (severity == SeverityFilter.ALL) return true;
        TerminalNotificationSeverity actual = TerminalNotificationSeverity.fromName(entry.getSeverityName());
        return severity == SeverityFilter.ERROR ? actual == TerminalNotificationSeverity.ERROR
            : severity == SeverityFilter.WARNING ? actual == TerminalNotificationSeverity.WARNING
                : severity == SeverityFilter.SUCCESS ? actual == TerminalNotificationSeverity.SUCCESS
                    : actual == TerminalNotificationSeverity.INFO;
    }
}

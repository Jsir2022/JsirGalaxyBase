package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TerminalOpenApproval {

    private final String selectedPageId;
    private final String terminalTitle;
    private final String terminalSubtitle;
    private final StatusBand statusBand;
    private final List<NavItem> navItems;
    private final List<PageSnapshot> pageSnapshots;
    private final List<NotificationEntry> notifications;
    private final String sessionToken;

    public TerminalOpenApproval(String selectedPageId, String terminalTitle, String terminalSubtitle,
        StatusBand statusBand, List<NavItem> navItems, List<PageSnapshot> pageSnapshots,
        List<NotificationEntry> notifications, String sessionToken) {
        this.selectedPageId = normalize(selectedPageId, "home");
        this.terminalTitle = normalize(terminalTitle, "银河终端");
        this.terminalSubtitle = normalize(terminalSubtitle, "服务端授权完成");
        this.statusBand = statusBand == null ? StatusBand.placeholder() : statusBand;
        this.navItems = freeze(navItems, Collections.singletonList(NavItem.placeholder("home", "首页", "总览", true)));
        this.pageSnapshots = freeze(pageSnapshots, Collections.singletonList(PageSnapshot.placeholder("home", "制度总览", "当前玩家制度摘要")));
        this.notifications = freeze(
            notifications,
            Collections.singletonList(NotificationEntry.placeholder("终端首页壳已接入", "当前为 phase 4 首页壳。", "INFO")));
        this.sessionToken = normalize(sessionToken, "terminal-session");
    }

    public String getSelectedPageId() {
        return selectedPageId;
    }

    public String getTerminalTitle() {
        return terminalTitle;
    }

    public String getTerminalSubtitle() {
        return terminalSubtitle;
    }

    public StatusBand getStatusBand() {
        return statusBand;
    }

    public List<NavItem> getNavItems() {
        return navItems;
    }

    public List<PageSnapshot> getPageSnapshots() {
        return pageSnapshots;
    }

    public List<NotificationEntry> getNotifications() {
        return notifications;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    private static <T> List<T> freeze(List<T> source, List<T> fallback) {
        List<T> resolved = source == null || source.isEmpty() ? fallback : source;
        return Collections.unmodifiableList(new ArrayList<T>(resolved));
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public static final class StatusBand {

        private final String eyebrow;
        private final String headline;
        private final String detail;
        private final String badgeLabel;
        private final String badgeValue;

        public StatusBand(String eyebrow, String headline, String detail, String badgeLabel, String badgeValue) {
            this.eyebrow = normalize(eyebrow, "当前页");
            this.headline = normalize(headline, "首页壳已接入");
            this.detail = normalize(detail, "当前没有状态带摘要。");
            this.badgeLabel = normalize(badgeLabel, "会话");
            this.badgeValue = normalize(badgeValue, "ready");
        }

        public static StatusBand placeholder() {
            return new StatusBand("当前页", "首页壳已接入", "当前没有状态带摘要。", "会话", "ready");
        }

        public String getEyebrow() {
            return eyebrow;
        }

        public String getHeadline() {
            return headline;
        }

        public String getDetail() {
            return detail;
        }

        public String getBadgeLabel() {
            return badgeLabel;
        }

        public String getBadgeValue() {
            return badgeValue;
        }
    }

    public static final class NavItem {

        private final String pageId;
        private final String label;
        private final String subtitle;
        private final boolean enabled;
        private final boolean selected;

        public NavItem(String pageId, String label, String subtitle, boolean enabled, boolean selected) {
            this.pageId = normalize(pageId, "home");
            this.label = normalize(label, "首页");
            this.subtitle = normalize(subtitle, "总览");
            this.enabled = enabled;
            this.selected = selected;
        }

        public static NavItem placeholder(String pageId, String label, String subtitle, boolean selected) {
            return new NavItem(pageId, label, subtitle, true, selected);
        }

        public String getPageId() {
            return pageId;
        }

        public String getLabel() {
            return label;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isSelected() {
            return selected;
        }
    }

    public static final class PageSnapshot {

        private final String pageId;
        private final String title;
        private final String lead;
        private final List<Section> sections;
        private final TerminalBankSectionSnapshot bankSectionSnapshot;
        private final TerminalMarketSectionSnapshot marketSectionSnapshot;
        private final TerminalCustomMarketSectionSnapshot customMarketSectionSnapshot;
        private final TerminalExchangeMarketSectionSnapshot exchangeMarketSectionSnapshot;

        public PageSnapshot(String pageId, String title, String lead, List<Section> sections) {
            this(pageId, title, lead, sections, null, null);
        }

        public PageSnapshot(String pageId, String title, String lead, List<Section> sections,
            TerminalBankSectionSnapshot bankSectionSnapshot) {
            this(pageId, title, lead, sections, bankSectionSnapshot, null);
        }

        public PageSnapshot(String pageId, String title, String lead, List<Section> sections,
            TerminalBankSectionSnapshot bankSectionSnapshot, TerminalMarketSectionSnapshot marketSectionSnapshot) {
            this(pageId, title, lead, sections, bankSectionSnapshot, marketSectionSnapshot, null, null);
        }

        public PageSnapshot(String pageId, String title, String lead, List<Section> sections,
            TerminalBankSectionSnapshot bankSectionSnapshot, TerminalMarketSectionSnapshot marketSectionSnapshot,
            TerminalCustomMarketSectionSnapshot customMarketSectionSnapshot,
            TerminalExchangeMarketSectionSnapshot exchangeMarketSectionSnapshot) {
            this.pageId = normalize(pageId, "home");
            this.title = normalize(title, "制度总览");
            this.lead = normalize(lead, "当前玩家制度摘要");
            this.sections = freeze(sections, Collections.singletonList(Section.placeholder()));
            this.bankSectionSnapshot = "bank".equalsIgnoreCase(this.pageId)
                ? (bankSectionSnapshot == null ? TerminalBankSectionSnapshot.placeholder() : bankSectionSnapshot)
                : bankSectionSnapshot;
            this.marketSectionSnapshot = "market".equalsIgnoreCase(this.pageId)
                ? (marketSectionSnapshot == null ? TerminalMarketSectionSnapshot.placeholder("market") : marketSectionSnapshot)
                : marketSectionSnapshot;
            this.customMarketSectionSnapshot = customMarketSectionSnapshot;
            this.exchangeMarketSectionSnapshot = exchangeMarketSectionSnapshot;
        }

        public static PageSnapshot placeholder(String pageId, String title, String lead) {
            return new PageSnapshot(
                pageId,
                title,
                lead,
                Collections.singletonList(Section.placeholder()),
                "bank".equalsIgnoreCase(normalize(pageId, "home")) ? TerminalBankSectionSnapshot.placeholder() : null,
                "market".equalsIgnoreCase(normalize(pageId, "home")) ? TerminalMarketSectionSnapshot.placeholder("market") : null,
                null,
                null);
        }

        public String getPageId() {
            return pageId;
        }

        public String getTitle() {
            return title;
        }

        public String getLead() {
            return lead;
        }

        public List<Section> getSections() {
            return sections;
        }

        public TerminalBankSectionSnapshot getBankSectionSnapshot() {
            return bankSectionSnapshot;
        }

        public TerminalMarketSectionSnapshot getMarketSectionSnapshot() {
            return marketSectionSnapshot;
        }

        public TerminalCustomMarketSectionSnapshot getCustomMarketSectionSnapshot() {
            return customMarketSectionSnapshot;
        }

        public TerminalExchangeMarketSectionSnapshot getExchangeMarketSectionSnapshot() {
            return exchangeMarketSectionSnapshot;
        }
    }

    public static final class Section {

        private final String sectionId;
        private final String title;
        private final String summary;
        private final String detail;

        public Section(String sectionId, String title, String summary, String detail) {
            this.sectionId = normalize(sectionId, "home_overview");
            this.title = normalize(title, "首页摘要");
            this.summary = normalize(summary, "当前没有首页摘要。");
            this.detail = normalize(detail, "当前没有更多说明。");
        }

        public static Section placeholder() {
            return new Section("home_overview", "首页摘要", "当前没有首页摘要。", "当前没有更多说明。");
        }

        public String getSectionId() {
            return sectionId;
        }

        public String getTitle() {
            return title;
        }

        public String getSummary() {
            return summary;
        }

        public String getDetail() {
            return detail;
        }
    }

    public static final class NotificationEntry {

        private final String title;
        private final String body;
        private final String severityName;

        public NotificationEntry(String title, String body, String severityName) {
            this.title = normalize(title, "终端通知");
            this.body = normalize(body, "当前没有通知内容。");
            this.severityName = normalize(severityName, "INFO");
        }

        public static NotificationEntry placeholder(String title, String body, String severityName) {
            return new NotificationEntry(title, body, severityName);
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }

        public String getSeverityName() {
            return severityName;
        }
    }
}

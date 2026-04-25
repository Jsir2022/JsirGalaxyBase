package com.jsirgalaxybase.terminal.client.viewmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public class TerminalHomeScreenModel {

    private final String selectedPageId;
    private final String terminalTitle;
    private final String terminalSubtitle;
    private final StatusBandModel statusBand;
    private final List<NavItemModel> navItems;
    private final List<PageSnapshotModel> pageSnapshots;
    private final List<NotificationModel> notifications;
    private final String sessionToken;
    private final String selectedNavigationPageId;
    private final String selectedSectionPageId;

    public TerminalHomeScreenModel(String selectedPageId, String terminalTitle, String terminalSubtitle,
        StatusBandModel statusBand, List<NavItemModel> navItems, List<PageSnapshotModel> pageSnapshots,
        List<NotificationModel> notifications, String sessionToken) {
        this.selectedPageId = normalizePageId(selectedPageId);
        this.terminalTitle = normalize(terminalTitle, "银河终端");
        this.terminalSubtitle = normalize(terminalSubtitle, "客户端终端首页");
        this.statusBand = statusBand == null ? StatusBandModel.placeholder() : statusBand;
        this.selectedNavigationPageId = resolveNavigationSelectionId(this.selectedPageId, navItems);
        this.selectedSectionPageId = TerminalPage.fromId(this.selectedPageId).toTopLevelPageId();
        this.navItems = freeze(normalizeNavItems(navItems, this.selectedNavigationPageId), defaultNavItems(this.selectedNavigationPageId));
        this.pageSnapshots = freeze(normalizePageSnapshots(pageSnapshots), defaultPageSnapshots());
        this.notifications = freeze(
            notifications,
            Collections.singletonList(NotificationModel.placeholder("终端通知", "当前没有通知内容。", "INFO")));
        this.sessionToken = normalize(sessionToken, "terminal-session");
    }

    public static TerminalHomeScreenModel placeholder() {
        return new TerminalHomeScreenModel(
            "home",
            "银河终端",
            "客户端首页壳",
            new StatusBandModel("当前页", "首页壳已接入", "当前为 phase 4 section 宿主占位模型。", "会话", "ready"),
            defaultNavItems("home"),
            defaultPageSnapshots(),
            Collections.singletonList(
                new NotificationModel("终端首页壳已接入", "当前模型已经具备 section 宿主、导航与最小 snapshot 落点。", "INFO")),
            "terminal-session");
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

    public StatusBandModel getStatusBand() {
        return statusBand;
    }

    public List<NavItemModel> getNavItems() {
        return navItems;
    }

    public List<PageSnapshotModel> getPageSnapshots() {
        return pageSnapshots;
    }

    public List<NotificationModel> getNotifications() {
        return notifications;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getSelectedNavigationPageId() {
        return selectedNavigationPageId;
    }

    public String getSelectedSectionPageId() {
        return selectedSectionPageId;
    }

    public NavItemModel getSelectedNavItem() {
        for (NavItemModel navItem : navItems) {
            if (selectedNavigationPageId.equals(navItem.getPageId())) {
                return navItem;
            }
        }
        TerminalPage selectedPage = TerminalPage.fromId(selectedPageId);
        return new NavItemModel(
            selectedPage.toTopLevelPageId(),
            selectedPage.getLabel(),
            selectedPage.getSubtitle(),
            true,
            true);
    }

    public PageSnapshotModel getSelectedPageSnapshot() {
        return getPageSnapshot(selectedSectionPageId);
    }

    public PageSnapshotModel getPageSnapshot(String pageId) {
        String normalizedPageId = TerminalPage.fromId(pageId).toTopLevelPageId();
        for (PageSnapshotModel pageSnapshot : pageSnapshots) {
            if (normalizedPageId.equals(pageSnapshot.getPageId())) {
                return pageSnapshot;
            }
        }
        return PageSnapshotModel.placeholder(TerminalPage.fromId(normalizedPageId));
    }

    public TerminalHomeScreenModel withSelectedPageId(String selectedPageId) {
        return new TerminalHomeScreenModel(
            selectedPageId,
            terminalTitle,
            terminalSubtitle,
            statusBand,
            navItems,
            pageSnapshots,
            notifications,
            sessionToken);
    }

    private static List<NavItemModel> defaultNavItems(String selectedPageId) {
        List<NavItemModel> defaults = new ArrayList<NavItemModel>();
        defaults.add(new NavItemModel("home", "首页", "总览", true, "home".equals(selectedPageId)));
        defaults.add(new NavItemModel("career", "职业", "职业页", true, "career".equals(selectedPageId)));
        defaults.add(new NavItemModel("public_service", "公共", "公共页", true, "public_service".equals(selectedPageId)));
        defaults.add(new NavItemModel("market", "市场", "总入口", true, "market".equals(selectedPageId)));
        defaults.add(new NavItemModel("bank", "银行", "银行页", true, "bank".equals(selectedPageId)));
        return defaults;
    }

    private static List<PageSnapshotModel> defaultPageSnapshots() {
        List<PageSnapshotModel> defaults = new ArrayList<PageSnapshotModel>();
        defaults.add(PageSnapshotModel.placeholder(TerminalPage.HOME));
        defaults.add(PageSnapshotModel.placeholder(TerminalPage.CAREER));
        defaults.add(PageSnapshotModel.placeholder(TerminalPage.PUBLIC_SERVICE));
        defaults.add(PageSnapshotModel.placeholder(TerminalPage.MARKET));
        defaults.add(PageSnapshotModel.placeholder(TerminalPage.BANK));
        return defaults;
    }

    private static List<NavItemModel> normalizeNavItems(List<NavItemModel> navItems, String selectedNavigationPageId) {
        List<NavItemModel> source = navItems == null || navItems.isEmpty() ? defaultNavItems(selectedNavigationPageId) : navItems;
        List<NavItemModel> normalized = new ArrayList<NavItemModel>(source.size());
        for (NavItemModel navItem : source) {
            if (navItem == null) {
                continue;
            }
            normalized.add(navItem.withSelected(selectedNavigationPageId.equals(navItem.getPageId())));
        }
        if (normalized.isEmpty()) {
            normalized.addAll(defaultNavItems(selectedNavigationPageId));
        }
        return normalized;
    }

    private static List<PageSnapshotModel> normalizePageSnapshots(List<PageSnapshotModel> pageSnapshots) {
        List<PageSnapshotModel> normalized = new ArrayList<PageSnapshotModel>();
        TerminalPage[] topLevelPages = new TerminalPage[] {
            TerminalPage.HOME,
            TerminalPage.CAREER,
            TerminalPage.PUBLIC_SERVICE,
            TerminalPage.MARKET,
            TerminalPage.BANK };
        for (TerminalPage page : topLevelPages) {
            normalized.add(findPageSnapshot(pageSnapshots, page));
        }
        return normalized;
    }

    private static PageSnapshotModel findPageSnapshot(List<PageSnapshotModel> pageSnapshots, TerminalPage targetPage) {
        if (pageSnapshots != null) {
            for (PageSnapshotModel pageSnapshot : pageSnapshots) {
                if (pageSnapshot != null && targetPage.toTopLevelPageId().equals(pageSnapshot.getPageId())) {
                    return pageSnapshot;
                }
            }
        }
        return PageSnapshotModel.placeholder(targetPage);
    }

    private static String resolveNavigationSelectionId(String selectedPageId, List<NavItemModel> navItems) {
        String topLevelPageId = TerminalPage.fromId(selectedPageId).toTopLevelPageId();
        if (navItems != null) {
            for (NavItemModel navItem : navItems) {
                if (navItem != null && topLevelPageId.equals(navItem.getPageId())) {
                    return topLevelPageId;
                }
            }
        }
        return topLevelPageId;
    }

    private static String normalizePageId(String selectedPageId) {
        return TerminalPage.fromId(normalize(selectedPageId, "home")).getId();
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

    public static final class StatusBandModel {

        private final String eyebrow;
        private final String headline;
        private final String detail;
        private final String badgeLabel;
        private final String badgeValue;

        public StatusBandModel(String eyebrow, String headline, String detail, String badgeLabel, String badgeValue) {
            this.eyebrow = normalize(eyebrow, "当前页");
            this.headline = normalize(headline, "首页壳已接入");
            this.detail = normalize(detail, "当前没有状态带摘要。");
            this.badgeLabel = normalize(badgeLabel, "会话");
            this.badgeValue = normalize(badgeValue, "ready");
        }

        public static StatusBandModel placeholder() {
            return new StatusBandModel("当前页", "首页壳已接入", "当前没有状态带摘要。", "会话", "ready");
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

    public static final class NavItemModel {

        private final String pageId;
        private final String label;
        private final String subtitle;
        private final boolean enabled;
        private final boolean selected;

        public NavItemModel(String pageId, String label, String subtitle, boolean enabled, boolean selected) {
            this.pageId = TerminalPage.fromId(normalize(pageId, "home")).toTopLevelPageId();
            this.label = normalize(label, "首页");
            this.subtitle = normalize(subtitle, "总览");
            this.enabled = enabled;
            this.selected = selected;
        }

        public static NavItemModel placeholder(String pageId, String label, String subtitle, boolean selected) {
            return new NavItemModel(pageId, label, subtitle, true, selected);
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

        public NavItemModel withSelected(boolean selected) {
            return new NavItemModel(pageId, label, subtitle, enabled, selected);
        }
    }

    public static final class PageSnapshotModel {

        private final String pageId;
        private final String title;
        private final String lead;
        private final List<SectionModel> sections;
        private final TerminalBankSectionModel bankSectionModel;
        private final TerminalMarketSectionModel marketSectionModel;
        private final TerminalCustomMarketSectionModel customMarketSectionModel;
        private final TerminalExchangeMarketSectionModel exchangeMarketSectionModel;

        public PageSnapshotModel(String pageId, String title, String lead, List<SectionModel> sections) {
            this(pageId, title, lead, sections, null, null);
        }

        public PageSnapshotModel(String pageId, String title, String lead, List<SectionModel> sections,
            TerminalBankSectionModel bankSectionModel) {
            this(pageId, title, lead, sections, bankSectionModel, null);
        }

        public PageSnapshotModel(String pageId, String title, String lead, List<SectionModel> sections,
            TerminalBankSectionModel bankSectionModel, TerminalMarketSectionModel marketSectionModel) {
            this(pageId, title, lead, sections, bankSectionModel, marketSectionModel, null, null);
        }

        public PageSnapshotModel(String pageId, String title, String lead, List<SectionModel> sections,
            TerminalBankSectionModel bankSectionModel, TerminalMarketSectionModel marketSectionModel,
            TerminalCustomMarketSectionModel customMarketSectionModel,
            TerminalExchangeMarketSectionModel exchangeMarketSectionModel) {
            this.pageId = TerminalPage.fromId(normalize(pageId, "home")).toTopLevelPageId();
            TerminalPage page = TerminalPage.fromId(this.pageId);
            this.title = normalize(title, page.getTitle());
            this.lead = normalize(lead, page.getLead());
            this.sections = freeze(sections, Collections.singletonList(SectionModel.placeholder()));
            this.bankSectionModel = page == TerminalPage.BANK
                ? (bankSectionModel == null ? TerminalBankSectionModel.placeholder() : bankSectionModel)
                : bankSectionModel;
            this.marketSectionModel = page == TerminalPage.MARKET
                ? (marketSectionModel == null ? TerminalMarketSectionModel.placeholder(TerminalPage.MARKET.getId())
                    : marketSectionModel)
                : marketSectionModel;
            this.customMarketSectionModel = page == TerminalPage.MARKET ? customMarketSectionModel : customMarketSectionModel;
            this.exchangeMarketSectionModel = page == TerminalPage.MARKET ? exchangeMarketSectionModel : exchangeMarketSectionModel;
        }

        public static PageSnapshotModel placeholder(TerminalPage page) {
            TerminalPage resolvedPage = page == null ? TerminalPage.HOME : TerminalPage.fromId(page.getId());
            return new PageSnapshotModel(
                resolvedPage.toTopLevelPageId(),
                resolvedPage.getTitle(),
                resolvedPage.getLead(),
                Collections.singletonList(
                    new SectionModel(
                        resolvedPage.toTopLevelPageId() + "_placeholder",
                        resolvedPage.getLabel() + " 宿主已接线",
                        "当前页面已具备 section 宿主落点。",
                        "后续真实业务页会直接挂到这个 section 宿主上。")),
                    resolvedPage == TerminalPage.BANK ? TerminalBankSectionModel.placeholder() : null,
                    resolvedPage == TerminalPage.MARKET ? TerminalMarketSectionModel.placeholder(TerminalPage.MARKET.getId()) : null,
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

        public List<SectionModel> getSections() {
            return sections;
        }

        public TerminalBankSectionModel getBankSectionModel() {
            return bankSectionModel;
        }

        public boolean hasBankSectionModel() {
            return bankSectionModel != null;
        }

        public TerminalMarketSectionModel getMarketSectionModel() {
            return marketSectionModel;
        }

        public boolean hasMarketSectionModel() {
            return marketSectionModel != null;
        }

        public TerminalCustomMarketSectionModel getCustomMarketSectionModel() {
            return customMarketSectionModel;
        }

        public boolean hasCustomMarketSectionModel() {
            return customMarketSectionModel != null;
        }

        public TerminalExchangeMarketSectionModel getExchangeMarketSectionModel() {
            return exchangeMarketSectionModel;
        }

        public boolean hasExchangeMarketSectionModel() {
            return exchangeMarketSectionModel != null;
        }
    }

    public static final class SectionModel {

        private final String sectionId;
        private final String title;
        private final String summary;
        private final String detail;

        public SectionModel(String sectionId, String title, String summary, String detail) {
            this.sectionId = normalize(sectionId, "home_overview");
            this.title = normalize(title, "首页摘要");
            this.summary = normalize(summary, "当前没有首页摘要。");
            this.detail = normalize(detail, "当前没有更多说明。");
        }

        public static SectionModel placeholder() {
            return new SectionModel("home_overview", "首页摘要", "当前没有首页摘要。", "当前没有更多说明。");
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

    public static final class NotificationModel {

        private final String title;
        private final String body;
        private final String severityName;

        public NotificationModel(String title, String body, String severityName) {
            this.title = normalize(title, "终端通知");
            this.body = normalize(body, "当前没有通知内容。");
            this.severityName = normalize(severityName, "INFO");
        }

        public static NotificationModel placeholder(String title, String body, String severityName) {
            return new NotificationModel(title, body, severityName);
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

        public TerminalNotificationSeverity getSeverity() {
            return TerminalNotificationSeverity.fromName(severityName);
        }
    }
}

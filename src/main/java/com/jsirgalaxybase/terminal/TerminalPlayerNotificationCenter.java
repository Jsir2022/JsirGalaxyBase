package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

/**
 * Bounded server-side presentation feed. It never replaces market, ticket, land or recovery records; those remain
 * their own business truth and supply new entries when the terminal next receives an actionable result.
 */
final class TerminalPlayerNotificationCenter {

    private static final int MAX_PLAYERS = 128;
    private final LinkedHashMap<String, TerminalNotificationFeed> feeds = new LinkedHashMap<String, TerminalNotificationFeed>();

    synchronized List<TerminalOpenApproval.NotificationEntry> recordAndPage(EntityPlayer player,
        List<TerminalOpenApproval.NotificationEntry> candidates) {
        TerminalNotificationFeed feed = feedFor(player);
        List<TerminalOpenApproval.NotificationEntry> response = new ArrayList<TerminalOpenApproval.NotificationEntry>();
        if (candidates != null) {
            for (TerminalOpenApproval.NotificationEntry candidate : candidates) {
                if (candidate != null) response.add(candidate);
                if (!isImportant(candidate)) continue;
                feed.record(new TerminalNotificationFeed.Entry(key(candidate), candidate.getSourceId(),
                    candidate.getTargetPageId(), candidate.getTargetRecordId(), candidate.getTitle(), candidate.getBody(),
                    candidate.getSeverityName()));
            }
        }
        for (TerminalOpenApproval.NotificationEntry historical : toEntries(feed.page(0, TerminalNotificationFeed.MAX_PAGE_SIZE))) {
            if (!containsSame(response, historical)) response.add(historical);
        }
        return response;
    }

    synchronized TerminalNotificationFeed.Page page(EntityPlayer player, int pageIndex, int pageSize) {
        return feedFor(player).page(pageIndex, pageSize);
    }

    synchronized TerminalNotificationCenterSnapshot snapshot(EntityPlayer player) {
        List<TerminalNotificationCenterSnapshot.Entry> entries =
            new ArrayList<TerminalNotificationCenterSnapshot.Entry>();
        for (TerminalNotificationFeed.Entry entry : feedFor(player).entriesNewestFirst()) {
            entries.add(new TerminalNotificationCenterSnapshot.Entry(entry.getSourceId(), entry.getTargetPageId(),
                entry.getTargetRecordId(), entry.getTitle(), entry.getBody(), entry.getSeverityName(),
                entry.getOccurrences()));
        }
        return new TerminalNotificationCenterSnapshot(
            entries.isEmpty() ? "当前没有可显示的重要通知。" : "保留最近 " + entries.size() + " 条重要通知。",
            entries, entries.size());
    }

    synchronized void clearAnonymousForTestOrPreview() {
        feeds.remove("terminal-anonymous");
    }

    private TerminalNotificationFeed feedFor(EntityPlayer player) {
        String playerKey = player == null || player.getUniqueID() == null ? "terminal-anonymous"
            : player.getUniqueID().toString();
        TerminalNotificationFeed feed = feeds.remove(playerKey);
        if (feed == null) feed = new TerminalNotificationFeed();
        feeds.put(playerKey, feed);
        while (feeds.size() > MAX_PLAYERS) {
            Iterator<Map.Entry<String, TerminalNotificationFeed>> iterator = feeds.entrySet().iterator();
            if (!iterator.hasNext()) break;
            iterator.next(); iterator.remove();
        }
        return feed;
    }

    private static boolean isImportant(TerminalOpenApproval.NotificationEntry entry) {
        if (entry == null) return false;
        TerminalNotificationSeverity severity = TerminalNotificationSeverity.fromName(entry.getSeverityName());
        return severity != TerminalNotificationSeverity.INFO || !empty(entry.getTargetPageId());
    }

    private static String key(TerminalOpenApproval.NotificationEntry entry) {
        return safe(entry.getSourceId()) + "|" + safe(entry.getTargetPageId()) + "|" + safe(entry.getTargetRecordId())
            + "|" + safe(entry.getTitle()) + "|" + safe(entry.getBody());
    }

    private static List<TerminalOpenApproval.NotificationEntry> toEntries(TerminalNotificationFeed.Page page) {
        List<TerminalOpenApproval.NotificationEntry> entries = new ArrayList<TerminalOpenApproval.NotificationEntry>();
        for (TerminalNotificationFeed.Entry entry : page.getEntries()) {
            entries.add(new TerminalOpenApproval.NotificationEntry(entry.getTitle(), entry.getBody(), entry.getSeverityName(),
                entry.getSourceId(), entry.getTargetPageId(), entry.getTargetRecordId()));
        }
        return entries;
    }

    private static boolean containsSame(List<TerminalOpenApproval.NotificationEntry> entries,
        TerminalOpenApproval.NotificationEntry target) {
        String targetKey = key(target);
        for (TerminalOpenApproval.NotificationEntry entry : entries) {
            if (targetKey.equals(key(entry))) return true;
        }
        return false;
    }

    private static boolean empty(String value) { return value == null || value.trim().isEmpty(); }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}

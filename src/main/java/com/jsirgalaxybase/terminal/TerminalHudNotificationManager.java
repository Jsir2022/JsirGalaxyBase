package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jsirgalaxybase.terminal.ui.TerminalNotification;

public final class TerminalHudNotificationManager {

    private static final int MAX_QUEUE_SIZE = 6;
    private static final List<ActiveNotification> ACTIVE_NOTIFICATIONS = new ArrayList<ActiveNotification>();

    private TerminalHudNotificationManager() {}

    public static synchronized void push(TerminalNotification notification) {
        if (notification == null || notification.getBody() == null || notification.getBody().trim().isEmpty()) {
            return;
        }

        Iterator<ActiveNotification> iterator = ACTIVE_NOTIFICATIONS.iterator();
        while (iterator.hasNext()) {
            ActiveNotification activeNotification = iterator.next();
            if (sameNotification(activeNotification.notification, notification)) {
                iterator.remove();
            }
        }

        ACTIVE_NOTIFICATIONS.add(new ActiveNotification(notification));
        while (ACTIVE_NOTIFICATIONS.size() > MAX_QUEUE_SIZE) {
            ActiveNotification removed = ACTIVE_NOTIFICATIONS.remove(0);
            invokeOnClose(removed.notification);
        }
    }

    public static synchronized List<NotificationView> pollVisible(long now, int limit) {
        purgeExpired(now);
        List<NotificationView> visible = new ArrayList<NotificationView>();
        for (int i = 0; i < ACTIVE_NOTIFICATIONS.size() && visible.size() < limit; i++) {
            ActiveNotification activeNotification = ACTIVE_NOTIFICATIONS.get(i);
            if (activeNotification.startedAtMillis < 0L) {
                activeNotification.startedAtMillis = now;
            }
            visible.add(new NotificationView(activeNotification.notification, now - activeNotification.startedAtMillis));
        }
        return visible;
    }

    private static boolean sameNotification(TerminalNotification first, TerminalNotification second) {
        return first.getSeverity() == second.getSeverity() && first.getTitle().equals(second.getTitle())
            && first.getBody().equals(second.getBody());
    }

    private static void purgeExpired(long now) {
        Iterator<ActiveNotification> iterator = ACTIVE_NOTIFICATIONS.iterator();
        while (iterator.hasNext()) {
            ActiveNotification activeNotification = iterator.next();
            if (activeNotification.startedAtMillis < 0L) {
                continue;
            }
            if (now - activeNotification.startedAtMillis >= activeNotification.notification.getAutoCloseMillis()) {
                iterator.remove();
                invokeOnClose(activeNotification.notification);
            }
        }
    }

    private static void invokeOnClose(TerminalNotification notification) {
        if (notification.getOnClose() != null) {
            notification.getOnClose().run();
        }
    }

    static synchronized void clearForTest() {
        ACTIVE_NOTIFICATIONS.clear();
    }

    private static final class ActiveNotification {

        private final TerminalNotification notification;
        private long startedAtMillis = -1L;

        private ActiveNotification(TerminalNotification notification) {
            this.notification = notification;
        }
    }

    public static final class NotificationView {

        private final TerminalNotification notification;
        private final long ageMillis;

        private NotificationView(TerminalNotification notification, long ageMillis) {
            this.notification = notification;
            this.ageMillis = ageMillis;
        }

        public TerminalNotification getNotification() {
            return notification;
        }

        public long getAgeMillis() {
            return ageMillis;
        }
    }
}
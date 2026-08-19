package com.jsirgalaxybase.terminal.client;

import net.minecraft.client.Minecraft;

import com.jsirgalaxybase.terminal.TerminalHudNotificationManager;
import com.jsirgalaxybase.terminal.client.screen.TerminalHomeScreen;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel.NotificationModel;
import com.jsirgalaxybase.terminal.ui.TerminalNotification;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TerminalClientScreenController {

    public static final TerminalClientScreenController INSTANCE = new TerminalClientScreenController();

    private TerminalHomeScreenModel pendingHomeScreen;
    private long pendingRequestSequence;

    private TerminalClientScreenController() {}

    public synchronized void queueHomeScreen(TerminalHomeScreenModel model) {
        queueHomeScreen(model, 0L);
    }

    public synchronized void queueHomeScreen(TerminalHomeScreenModel model, long requestSequence) {
        if (model != null) {
            if (pendingHomeScreen != null && requestSequence < pendingRequestSequence) {
                return;
            }
            pendingHomeScreen = model;
            pendingRequestSequence = Math.max(0L, requestSequence);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        PendingHomeScreen pending = drainPendingHomeScreen();
        if (pending == null) {
            return;
        }
        TerminalHomeScreenModel model = pending.model;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) {
            queueHomeScreen(model, pending.requestSequence);
            return;
        }
        pushActionNotifications(model, pending.requestSequence);

        if (minecraft.currentScreen instanceof TerminalHomeScreen) {
            ((TerminalHomeScreen) minecraft.currentScreen).applyModel(model, pending.requestSequence);
            return;
        }

        minecraft.displayGuiScreen(new TerminalHomeScreen(minecraft.currentScreen, model));
    }

    private long lastNotificationSequence = Long.MIN_VALUE;

    private void pushActionNotifications(TerminalHomeScreenModel model, long requestSequence) {
        if (model == null || model.getNotifications() == null) {
            return;
        }
        if (requestSequence > 0L && requestSequence == lastNotificationSequence) return;
        if (requestSequence > 0L) lastNotificationSequence = requestSequence;
        for (NotificationModel notification : model.getNotifications()) {
            if (notification == null || notification.getSeverity() == TerminalNotificationSeverity.INFO) {
                continue;
            }
            TerminalNotificationSeverity severity = notification.getSeverity();
            final String notificationTitle = notification.getTitle();
            final String notificationBody = notification.getBody();
            long duration = severity == TerminalNotificationSeverity.ERROR ? 7000L
                : severity == TerminalNotificationSeverity.WARNING ? 5500L : 4200L;
            TerminalHudNotificationManager.push(TerminalNotification.builder()
                .severity(severity)
                .title(safeNotificationTitle(notification.getTitle(), notification.getBody()))
                .body(safeNotificationBody(notification.getBody()))
                .autoCloseMillis(duration)
                .onClick(new Runnable() {
                    @Override public void run() { focusMarketNotification(notificationTitle, notificationBody); }
                })
                .build());
        }
    }

    private void focusMarketNotification(String title, String body) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!(minecraft.currentScreen instanceof TerminalHomeScreen)) return;
        String text = (title == null ? "" : title) + " " + (body == null ? "" : body);
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState.AccountCenterTab tab;
        String prefix = "";
        if (lower.contains("vault") || lower.contains("custody") || text.contains("收货")
            || text.contains("恢复") || text.contains("返还失败")) {
            tab = com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState.AccountCenterTab.ASSETS_AND_DELIVERY;
            prefix = text.contains("收货") || lower.contains("custody") ? "C" : "";
        } else if (text.contains("撤单") || text.contains("已撤销")) {
            tab = com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState.AccountCenterTab.HISTORY;
        } else if (text.contains("成交")) {
            tab = com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState.AccountCenterTab.FILLS;
        } else {
            tab = com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState.AccountCenterTab.OPEN_ORDERS;
        }
        ((TerminalHomeScreen) minecraft.currentScreen).openAccountCenterFocused(tab, prefix + extractRecordId(text));
    }

    private String extractRecordId(String text) {
        String[] markers = { "orderId=", "custodyId=", "收货编号 " };
        for (String marker : markers) {
            int start = text.indexOf(marker);
            if (start < 0) continue;
            start += marker.length();
            StringBuilder digits = new StringBuilder();
            while (start < text.length() && Character.isDigit(text.charAt(start))) digits.append(text.charAt(start++));
            if (digits.length() > 0) return digits.toString();
        }
        return "";
    }

    private String safeNotificationTitle(String title, String body) {
        return containsBackendDetail(body) ? "操作未完成" : title;
    }

    private String safeNotificationBody(String body) {
        if (containsBackendDetail(body)) {
            return "服务端暂时无法完成此操作，请稍后刷新重试；资产状态不会由客户端自行改写。";
        }
        return body;
    }

    private boolean containsBackendDetail(String value) {
        String normalized = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("jdbc") || normalized.contains("sql") || normalized.contains("exception")
            || normalized.contains("fromaccountid") || normalized.contains("toaccountid")
            || normalized.contains("stacktrace") || normalized.contains("java.");
    }

    private synchronized PendingHomeScreen drainPendingHomeScreen() {
        PendingHomeScreen pending = pendingHomeScreen == null ? null
            : new PendingHomeScreen(pendingHomeScreen, pendingRequestSequence);
        pendingHomeScreen = null;
        pendingRequestSequence = 0L;
        return pending;
    }

    private static final class PendingHomeScreen {
        private final TerminalHomeScreenModel model;
        private final long requestSequence;

        private PendingHomeScreen(TerminalHomeScreenModel model, long requestSequence) {
            this.model = model;
            this.requestSequence = requestSequence;
        }
    }
}

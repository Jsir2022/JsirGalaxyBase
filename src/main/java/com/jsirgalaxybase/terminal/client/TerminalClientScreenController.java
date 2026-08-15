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
            long duration = severity == TerminalNotificationSeverity.ERROR ? 7000L
                : severity == TerminalNotificationSeverity.WARNING ? 5500L : 4200L;
            TerminalHudNotificationManager.push(TerminalNotification.builder()
                .severity(severity)
                .title(safeNotificationTitle(notification.getTitle(), notification.getBody()))
                .body(safeNotificationBody(notification.getBody()))
                .autoCloseMillis(duration)
                .build());
        }
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

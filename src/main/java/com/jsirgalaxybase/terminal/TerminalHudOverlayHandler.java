package com.jsirgalaxybase.terminal;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.jsirgalaxybase.client.gui.framework.CanvasScreen;
import com.jsirgalaxybase.terminal.client.screen.TerminalHomeScreen;
import com.jsirgalaxybase.terminal.ui.TerminalNotification;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TerminalHudOverlayHandler {

    public static final TerminalHudOverlayHandler INSTANCE = new TerminalHudOverlayHandler();

    private static final int MAX_VISIBLE = 3;

    private TerminalHudOverlayHandler() {}

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.theWorld == null || !canRenderOver(minecraft.currentScreen)) {
            return;
        }

        if (minecraft.currentScreen instanceof TerminalHomeScreen) return;

        List<TerminalHudNotificationManager.NotificationView> notifications = TerminalHudNotificationManager.pollVisible(
            System.currentTimeMillis(),
            MAX_VISIBLE);
        if (notifications.isEmpty()) {
            return;
        }

        int screenWidth = event.resolution.getScaledWidth();
        int width = Math.min(320, Math.max(180, screenWidth / 3));
        width = Math.min(width, Math.max(120, screenWidth - 20));
        int x = screenWidth - width - 10;
        int y = 10;
        for (TerminalHudNotificationManager.NotificationView notificationView : notifications) {
            int height = drawNotification(minecraft.fontRenderer, notificationView, x, y, width);
            y += height + 6;
        }
    }

    public void drawTerminalNotifications(FontRenderer fontRenderer, int screenWidth, int screenHeight) {
        if (fontRenderer == null) return;
        List<TerminalHudNotificationManager.NotificationView> notifications = TerminalHudNotificationManager.pollVisible(
            System.currentTimeMillis(), MAX_VISIBLE);
        if (notifications.isEmpty()) return;
        int terminalInset = Math.max(8, screenWidth / 20);
        int width = Math.min(280, Math.max(170, screenWidth / 4));
        width = Math.min(width, Math.max(120, screenWidth - terminalInset * 2));
        int x = screenWidth - terminalInset - width;
        int y = Math.max(28, screenHeight / 12);
        for (TerminalHudNotificationManager.NotificationView view : notifications) {
            int height = drawNotification(fontRenderer, view, x, y, width);
            y += height + 5;
        }
    }

    public boolean clickTerminalNotification(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        List<TerminalHudNotificationManager.NotificationView> notifications = TerminalHudNotificationManager.pollVisible(
            System.currentTimeMillis(), MAX_VISIBLE);
        if (notifications.isEmpty()) return false;
        int terminalInset = Math.max(8, screenWidth / 20);
        int width = Math.min(280, Math.max(170, screenWidth / 4));
        width = Math.min(width, Math.max(120, screenWidth - terminalInset * 2));
        int x = screenWidth - terminalInset - width;
        int y = Math.max(28, screenHeight / 12);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        for (TerminalHudNotificationManager.NotificationView view : notifications) {
            List lines = font.listFormattedStringToWidth(view.getNotification().getBody(), width - 18);
            int height = 18 + Math.max(Math.min(4, lines.size()), 1) * 10;
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                return TerminalHudNotificationManager.click(view);
            }
            y += height + 5;
        }
        return false;
    }

    private int drawNotification(FontRenderer fontRenderer, TerminalHudNotificationManager.NotificationView notificationView,
        int x, int y, int width) {
        TerminalNotification notification = notificationView.getNotification();
        TerminalNotificationSeverity severity = notification.getSeverity();
        List lines = fontRenderer.listFormattedStringToWidth(notification.getBody(), width - 18);
        int visibleLines = Math.min(4, lines.size());
        int height = 18 + Math.max(visibleLines, 1) * 10;
        int alpha = computeAlpha(notification, notificationView.getAgeMillis());
        int background = applyAlpha(severity.getBackgroundColor(), alpha);
        int accent = applyAlpha(severity.getAccentColor(), alpha);
        int shadow = applyAlpha(0xFF0E1318, alpha);
        int titleColor = applyAlpha(0xFFE8EDF2, alpha);
        int bodyColor = applyAlpha(0xFFD0D7DE, alpha);

        Gui.drawRect(x, y, x + width, y + height, shadow);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, background);
        Gui.drawRect(x + 1, y + 1, x + 5, y + height - 1, accent);

        String title = fontRenderer.trimStringToWidth(notification.getTitle(), width - 18);
        fontRenderer.drawStringWithShadow(title, x + 10, y + 4, titleColor);
        for (int i = 0; i < visibleLines; i++) {
            String line = (String) lines.get(i);
            fontRenderer.drawStringWithShadow(line, x + 10, y + 16 + i * 9, bodyColor);
        }
        return height;
    }

    private boolean canRenderOver(GuiScreen currentScreen) {
        if (currentScreen == null) {
            return true;
        }
        return currentScreen instanceof CanvasScreen;
    }

    private int computeAlpha(TerminalNotification notification, long ageMillis) {
        long duration = Math.max(1000L, notification.getAutoCloseMillis());
        long fadeStart = (long) (duration * 0.80d);
        if (ageMillis <= fadeStart) {
            return 220;
        }
        long fadeDuration = Math.max(1L, duration - fadeStart);
        long fadeAge = Math.min(fadeDuration, ageMillis - fadeStart);
        return 220 - (int) ((fadeAge * 140L) / fadeDuration);
    }

    private int applyAlpha(int color, int alpha) {
        int safeAlpha = Math.max(32, Math.min(255, alpha));
        return (safeAlpha << 24) | (color & 0x00FFFFFF);
    }
}

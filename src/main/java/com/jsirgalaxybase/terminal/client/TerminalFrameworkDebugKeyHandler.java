package com.jsirgalaxybase.terminal.client;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;

import com.jsirgalaxybase.client.ui2.lab.Ui2LabScreen;
import com.jsirgalaxybase.client.ui2.host.Ui2DebugCaptureSource;
import com.jsirgalaxybase.ui2.debug.UiDebugSnapshot;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TerminalFrameworkDebugKeyHandler {

    private static final KeyBinding OPEN_FRAMEWORK_TEST_KEY = new KeyBinding(
        "key.jsirgalaxybase.terminalFrameworkTest",
        Keyboard.KEY_F8,
        "key.categories.jsirgalaxybase");
    private static final KeyBinding CAPTURE_UI2_KEY = new KeyBinding(
        "key.jsirgalaxybase.captureUi2Debug",
        Keyboard.KEY_F9,
        "key.categories.jsirgalaxybase");

    private static boolean registered;

    public TerminalFrameworkDebugKeyHandler() {
        if (!registered) {
            registered = true;
            ClientRegistry.registerKeyBinding(OPEN_FRAMEWORK_TEST_KEY);
            ClientRegistry.registerKeyBinding(CAPTURE_UI2_KEY);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) {
            return;
        }
        if (OPEN_FRAMEWORK_TEST_KEY.isPressed()) {
            minecraft.displayGuiScreen(new Ui2LabScreen(minecraft.currentScreen));
        }
        if (CAPTURE_UI2_KEY.isPressed()) capture(minecraft);
    }

    private static void capture(Minecraft minecraft) {
        if (!(minecraft.currentScreen instanceof Ui2DebugCaptureSource)) {
            minecraft.thePlayer.addChatMessage(new ChatComponentText("UI2 调试截图仅适用于现代终端页面"));
            return;
        }
        Ui2DebugCaptureSource source = (Ui2DebugCaptureSource) minecraft.currentScreen;
        if (source.ui2DebugDrawList() == null) {
            minecraft.thePlayer.addChatMessage(new ChatComponentText("UI2 页面尚未完成首帧渲染"));
            return;
        }
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS").format(new Date());
        String screen = minecraft.currentScreen.getClass().getSimpleName().replaceAll("[^A-Za-z0-9_-]", "_");
        String base = "ui2-debug-" + screen + "-" + stamp;
        File screenshots = new File(minecraft.mcDataDir, "screenshots");
        if (!screenshots.exists()) screenshots.mkdirs();
        try {
            String snapshot = UiDebugSnapshot.capture(source.ui2DebugRoot(), source.ui2DebugDrawList(),
                source.ui2DebugWidth(), source.ui2DebugHeight());
            Files.write(new File(screenshots, base + ".ui2.txt").toPath(), snapshot.getBytes(StandardCharsets.UTF_8));
            IChatComponent result = ScreenShotHelper.saveScreenshot(minecraft.mcDataDir, base + ".png",
                minecraft.displayWidth, minecraft.displayHeight, minecraft.getFramebuffer());
            minecraft.thePlayer.addChatMessage(result);
        } catch (Exception exception) {
            minecraft.thePlayer.addChatMessage(new ChatComponentText("UI2 调试截图失败: " + exception.getMessage()));
        }
    }
}

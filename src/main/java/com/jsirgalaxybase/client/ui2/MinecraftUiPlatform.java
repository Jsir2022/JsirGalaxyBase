package com.jsirgalaxybase.client.ui2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

import com.jsirgalaxybase.client.ui2.font.MinecraftFontService;
import com.jsirgalaxybase.client.ui2.render.UiRenderDiagnostics;
import com.jsirgalaxybase.terminal.client.settings.TerminalAppearance;

/** Shared client resources for every UI2 host. */
public final class MinecraftUiPlatform implements IResourceManagerReloadListener {
    public static final MinecraftUiPlatform INSTANCE = new MinecraftUiPlatform();
    private MinecraftFontService fonts;
    private boolean registered;
    private MinecraftUiPlatform() {}

    public synchronized void initialize() {
        if (registered) return;
        registered = true;
        IResourceManager resources = Minecraft.getMinecraft().getResourceManager();
        if (resources instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) resources).registerReloadListener(this);
        }
    }
    public synchronized MinecraftFontService fonts() {
        if (fonts == null) fonts = new MinecraftFontService();
        return fonts;
    }
    public synchronized String diagnosticSummary(boolean reducedMotion) {
        UiRenderDiagnostics render = UiRenderDiagnostics.INSTANCE;
        return "GL2-AA · Minecraft FontRenderer"
            + " · cmd" + render.getCommandCount() + "/clip" + render.getMaximumClipDepth()
            + " · f" + (render.getFrameNanos() / 1000L) + "/r"
            + (render.getRenderNanos() / 1000L) + "us · "
            + (reducedMotion ? "reduced" : "smooth");
    }
    @Override public synchronized void onResourceManagerReload(IResourceManager manager) {
        TerminalAppearance.INSTANCE.reloadThemes(manager);
    }
}

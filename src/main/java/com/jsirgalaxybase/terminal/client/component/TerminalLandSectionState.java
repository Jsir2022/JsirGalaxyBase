package com.jsirgalaxybase.terminal.client.component;

import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.jsirgalaxybase.terminal.TerminalLandActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalLandSectionModel;

public final class TerminalLandSectionState {

    private TerminalLandActionPayload.Tab tab = TerminalLandActionPayload.Tab.NEARBY;
    private int selectedChunkX;
    private int selectedChunkZ;
    private int pageIndex;
    private long selectedVersion;
    private int viewportChunkX;
    private int viewportChunkZ;
    private TerminalLandActionPayload.Zoom zoom = TerminalLandActionPayload.Zoom.MEDIUM;
    private boolean selectedTerrainLoaded;
    private boolean ownershipOverlayVisible = true;
    private boolean contextRequested;
    private int contextChunkX;
    private int contextChunkZ;
    private int contextScreenX;
    private int contextScreenY;

    public void applyModel(TerminalLandSectionModel model) {
        if (model == null) return;
        try {
            tab = TerminalLandActionPayload.Tab.valueOf(model.getTab());
        } catch (IllegalArgumentException ignored) {
            tab = TerminalLandActionPayload.Tab.NEARBY;
        }
        selectedChunkX = model.getSelectedChunkX();
        selectedChunkZ = model.getSelectedChunkZ();
        selectedVersion = model.getSelectedVersion();
        pageIndex = model.getPageIndex();
        selectedTerrainLoaded = Minecraft.getMinecraft().theWorld != null
            && Minecraft.getMinecraft().theWorld.getChunkProvider() != null
            && Minecraft.getMinecraft().theWorld.getChunkProvider()
                .chunkExists(model.getSelectedChunkX(), model.getSelectedChunkZ());
        viewportChunkX = model.getViewportChunkX();
        viewportChunkZ = model.getViewportChunkZ();
        try {
            zoom = TerminalLandActionPayload.Zoom.valueOf(model.getZoom());
        } catch (IllegalArgumentException ignored) {
            zoom = TerminalLandActionPayload.Zoom.MEDIUM;
        }
    }

    public void select(int chunkX, int chunkZ, long version) {
        select(chunkX, chunkZ, version, selectedTerrainLoaded);
    }

    public void select(int chunkX, int chunkZ, long version, boolean terrainLoaded) {
        selectedChunkX = chunkX;
        selectedChunkZ = chunkZ;
        selectedVersion = Math.max(0L, version);
        selectedTerrainLoaded = terrainLoaded;
    }

    public void selectTab(TerminalLandActionPayload.Tab value) {
        tab = value == null ? TerminalLandActionPayload.Tab.NEARBY : value;
        pageIndex = 0;
    }

    public void setPageIndex(int value) { pageIndex = Math.max(0, value); }
    public TerminalLandActionPayload.Tab getTab() { return tab; }
    public int getSelectedChunkX() { return selectedChunkX; }
    public int getSelectedChunkZ() { return selectedChunkZ; }
    public int getPageIndex() { return pageIndex; }
    public long getSelectedVersion() { return selectedVersion; }
    public int getViewportChunkX() { return viewportChunkX; }
    public int getViewportChunkZ() { return viewportChunkZ; }
    public TerminalLandActionPayload.Zoom getZoom() { return zoom; }
    public boolean isSelectedTerrainLoaded() { return selectedTerrainLoaded; }
    public boolean isOwnershipOverlayVisible() { return ownershipOverlayVisible; }

    public void toggleOwnershipOverlay() {
        ownershipOverlayVisible = !ownershipOverlayVisible;
    }

    public void requestContext(int chunkX, int chunkZ, int screenX, int screenY) {
        contextRequested = true;
        contextChunkX = chunkX;
        contextChunkZ = chunkZ;
        contextScreenX = screenX;
        contextScreenY = screenY;
    }

    public boolean consumeContextRequest(TerminalLandSectionModel model) {
        if (!contextRequested || model == null || model.getSelectedChunkX() != contextChunkX
            || model.getSelectedChunkZ() != contextChunkZ) return false;
        contextRequested = false;
        return true;
    }

    public void clearContextRequest() { contextRequested = false; }
    public int getContextScreenX() { return contextScreenX; }
    public int getContextScreenY() { return contextScreenY; }

    public void setViewport(int chunkX, int chunkZ, TerminalLandActionPayload.Zoom value) {
        viewportChunkX = chunkX;
        viewportChunkZ = chunkZ;
        zoom = value == null ? TerminalLandActionPayload.Zoom.MEDIUM : value;
    }

    public void centerOnPlayer(int chunkX, int chunkZ) {
        setViewport(chunkX, chunkZ, TerminalLandActionPayload.Zoom.MEDIUM);
    }

    public TerminalLandActionPayload toPayload() {
        return new TerminalLandActionPayload(tab, selectedChunkX, selectedChunkZ, pageIndex, "", selectedVersion,
            viewportChunkX, viewportChunkZ, zoom);
    }

    public TerminalLandActionPayload toActionPayload(String prefix) {
        return new TerminalLandActionPayload(tab, selectedChunkX, selectedChunkZ, pageIndex,
            prefix + "-" + UUID.randomUUID().toString(), selectedVersion, viewportChunkX, viewportChunkZ, zoom);
    }
}

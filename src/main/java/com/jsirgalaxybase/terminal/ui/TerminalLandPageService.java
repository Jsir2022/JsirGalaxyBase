package com.jsirgalaxybase.terminal.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.land.application.PersonalLandService;
import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.PersonalLandActionReceipt;
import com.jsirgalaxybase.modules.land.domain.PersonalLandTitle;
import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalLandActionPayload;
import com.jsirgalaxybase.terminal.TerminalLandMapCellSnapshot;
import com.jsirgalaxybase.terminal.TerminalLandSectionSnapshot;

public final class TerminalLandPageService {

    public static final int GRID_RADIUS = 7;
    public static final int GRID_SIZE = GRID_RADIUS * 2 + 1;
    public static final int OWNED_PAGE_SIZE = 4;
    public static final int MAX_VIEWPORT_OFFSET = 16;

    public TerminalLandSectionSnapshot createSnapshot(PersonalLandService service, String serverId,
        String protectionMode, int maxClaims, String playerRef, int dimensionId, int centerChunkX,
        int centerChunkZ, TerminalActionType actionType, TerminalLandActionPayload payload) {
        if (service == null || serverId == null || playerRef == null || playerRef.trim().isEmpty()) {
            return TerminalLandSectionSnapshot.unavailable();
        }
        TerminalLandActionPayload safePayload = payload == null ? TerminalLandActionPayload.empty() : payload;
        int viewportX = actionType == TerminalActionType.SELECT_PAGE ? centerChunkX
            : clampAround(centerChunkX, safePayload.getViewportChunkX(), MAX_VIEWPORT_OFFSET);
        int viewportZ = actionType == TerminalActionType.SELECT_PAGE ? centerChunkZ
            : clampAround(centerChunkZ, safePayload.getViewportChunkZ(), MAX_VIEWPORT_OFFSET);
        TerminalLandActionPayload.Zoom zoom = safePayload.getZoom();
        LandChunkKey requestedKey = new LandChunkKey(serverId, dimensionId, safePayload.getSelectedChunkX(),
            safePayload.getSelectedChunkZ());
        Optional<PersonalLandTitle> requestedTitle = service.find(requestedKey);
        boolean requestedNearby = withinGrid(centerChunkX, safePayload.getSelectedChunkX())
            && withinGrid(centerChunkZ, safePayload.getSelectedChunkZ());
        boolean nearby = actionType != TerminalActionType.SELECT_PAGE
            && requestedNearby;
        boolean ownedListSelection = safePayload.getTab() == TerminalLandActionPayload.Tab.MINE
            && requestedTitle.isPresent() && playerRef.equals(requestedTitle.get().getOwnerPlayerRef());
        int selectedX = nearby || ownedListSelection ? safePayload.getSelectedChunkX() : centerChunkX;
        int selectedZ = nearby || ownedListSelection ? safePayload.getSelectedChunkZ() : centerChunkZ;
        LandChunkKey selectedKey = new LandChunkKey(serverId, dimensionId, selectedX, selectedZ);
        String feedbackCode = "NONE";
        if (actionType == TerminalActionType.LAND_CLAIM || actionType == TerminalActionType.LAND_UNCLAIM) {
            if (actionType == TerminalActionType.LAND_CLAIM && !requestedNearby) {
                feedbackCode = "OUT_OF_RANGE";
            } else if (!safePayload.hasRequestId()) {
                feedbackCode = "INVALID_REQUEST";
            } else {
                PersonalLandActionReceipt receipt = actionType == TerminalActionType.LAND_CLAIM
                    ? service.claim(safePayload.getRequestId(), playerRef, selectedKey)
                    : service.unclaim(safePayload.getRequestId(), playerRef, selectedKey,
                        safePayload.getExpectedVersion());
                feedbackCode = receipt.getResult().name();
            }
        } else if (actionType == TerminalActionType.REFRESH_PAGE) {
            feedbackCode = "REFRESHED";
        }

        List<PersonalLandTitle> owned = new ArrayList<PersonalLandTitle>();
        for (PersonalLandTitle title : service.listOwned(playerRef)) {
            if (serverId.equals(title.getChunkKey().getServerId())) owned.add(title);
        }
        Collections.sort(owned, Comparator.comparing(PersonalLandTitle::getChunkKey));
        int totalEntries = owned.size();
        int totalPages = totalEntries == 0 ? 0 : (totalEntries + OWNED_PAGE_SIZE - 1) / OWNED_PAGE_SIZE;
        int pageIndex = totalPages == 0 ? 0 : Math.min(safePayload.getPageIndex(), totalPages - 1);
        int start = Math.min(totalEntries, pageIndex * OWNED_PAGE_SIZE);
        int end = Math.min(totalEntries, start + OWNED_PAGE_SIZE);
        List<Long> titleIds = new ArrayList<Long>();
        List<Integer> chunkXs = new ArrayList<Integer>();
        List<Integer> chunkZs = new ArrayList<Integer>();
        List<Long> versions = new ArrayList<Long>();
        for (PersonalLandTitle title : owned.subList(start, end)) {
            titleIds.add(Long.valueOf(title.getTitleId()));
            chunkXs.add(Integer.valueOf(title.getChunkKey().getChunkX()));
            chunkZs.add(Integer.valueOf(title.getChunkKey().getChunkZ()));
            versions.add(Long.valueOf(title.getVersion()));
        }

        int viewportRadius = zoom.getRadius();
        List<TerminalLandMapCellSnapshot> mapCells = new ArrayList<TerminalLandMapCellSnapshot>();
        int minZ = saturatingAdd(viewportZ, -viewportRadius);
        int maxZ = saturatingAdd(viewportZ, viewportRadius);
        int minX = saturatingAdd(viewportX, -viewportRadius);
        int maxX = saturatingAdd(viewportX, viewportRadius);
        for (int z = minZ;; z++) {
            for (int x = minX;; x++) {
                LandChunkKey key = new LandChunkKey(serverId, dimensionId, x, z);
                Optional<PersonalLandTitle> found = service.getProtectionIndex().find(key);
                if (found.isPresent()) {
                    PersonalLandTitle title = found.get();
                    mapCells.add(new TerminalLandMapCellSnapshot(x, z,
                        playerRef.equals(title.getOwnerPlayerRef()) ? "OWNED" : "OTHER",
                        title.getTitleId(), title.getVersion()));
                } else if (service.isReserved(key)) {
                    mapCells.add(new TerminalLandMapCellSnapshot(x, z, "RESERVED", 0L, 0L));
                }
                if (x == maxX) break;
            }
            if (z == maxZ) break;
        }

        Optional<PersonalLandTitle> selected = service.find(selectedKey);
        boolean ownedByPlayer = selected.isPresent() && playerRef.equals(selected.get().getOwnerPlayerRef());
        boolean selectedReserved = !selected.isPresent() && service.isReserved(selectedKey);
        boolean dimensionBlocked = service.isDimensionBlocked(dimensionId);
        String selectedState = selected.isPresent() ? (ownedByPlayer ? "OWNED" : "OTHER")
            : selectedReserved ? "RESERVED" : dimensionBlocked ? "BLOCKED" : "UNCLAIMED";
        boolean selectedNearby = withinGrid(centerChunkX, selectedX) && withinGrid(centerChunkZ, selectedZ);
        return new TerminalLandSectionSnapshot("READY", serverId, protectionMode, dimensionId,
            centerChunkX, centerChunkZ, selectedX, selectedZ, totalEntries, maxClaims,
            safePayload.getTab().name(), viewportX, viewportZ, zoom.name(), mapCells,
            titleIds, chunkXs, chunkZs, versions,
            pageIndex, totalPages, totalEntries, selected.isPresent() ? selected.get().getTitleId() : 0L,
            selected.isPresent() ? selected.get().getVersion() : 0L, selectedState,
            !selected.isPresent() && !selectedReserved && !dimensionBlocked && selectedNearby
                && totalEntries < maxClaims,
            ownedByPlayer, feedbackCode);
    }

    private static boolean withinGrid(int center, int selected) {
        return Math.abs((long) center - (long) selected) <= GRID_RADIUS;
    }

    static int clampAround(int center, int requested, int radius) {
        long delta = (long) requested - (long) center;
        if (delta < -radius) return (int) Math.max(Integer.MIN_VALUE, (long) center - radius);
        if (delta > radius) return (int) Math.min(Integer.MAX_VALUE, (long) center + radius);
        return requested;
    }

    private static int saturatingAdd(int value, int delta) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) value + delta));
    }
}

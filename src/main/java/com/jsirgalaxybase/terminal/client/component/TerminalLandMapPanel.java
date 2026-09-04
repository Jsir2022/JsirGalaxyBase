package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.client.gui.framework.AbstractGuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.terminal.TerminalLandActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalLandSectionModel;

public final class TerminalLandMapPanel extends AbstractGuiPanel {

    private static final int TOOL_SIZE = 13;
    private static final int TOOL_GAP = 2;
    private static final int STATUS_HEIGHT = 13;
    private static final int CONTEXT_WIDTH = 108;
    private static final int CONTEXT_ROW_HEIGHT = 18;

    public interface Handler {
        void select(int chunkX, int chunkZ, long version, boolean terrainLoaded);
        void viewport(int chunkX, int chunkZ, TerminalLandActionPayload.Zoom zoom);
        void refresh();
        void contextAction(boolean unclaim);
    }

    private final TerminalLandSectionModel model;
    private final TerminalLandSectionState state;
    private final Handler handler;
    private final Map<Long, TerminalLandSectionModel.MapCellModel> cells =
        new HashMap<Long, TerminalLandSectionModel.MapCellModel>();
    private int pressX;
    private int pressY;
    private boolean pressed;
    private boolean contextOpen;
    private int contextX;
    private int contextY;

    public TerminalLandMapPanel(TerminalLandSectionModel model, TerminalLandSectionState state, Handler handler) {
        this.model = model == null ? TerminalLandSectionModel.unavailable() : model;
        this.state = state == null ? new TerminalLandSectionState() : state;
        this.handler = handler;
        for (TerminalLandSectionModel.MapCellModel cell : this.model.getMapCells()) {
            cells.put(key(cell.getChunkX(), cell.getChunkZ()), cell);
        }
        if (this.state.consumeContextRequest(this.model)) {
            contextOpen = true;
            contextX = this.state.getContextScreenX();
            contextY = this.state.getContextScreenY();
        }
    }

    @Override
    public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible()) return;
        GuiRect map = getBounds();
        Gui.drawRect(map.getX(), map.getY(), map.getRight(), map.getBottom(), 0xFF0D1820);
        Gui.drawRect(map.getX(), map.getY(), map.getRight(), map.getY() + 1, 0xFF7995AA);
        Gui.drawRect(map.getX(), map.getBottom() - 1, map.getRight(), map.getBottom(), 0xFF7995AA);
        Gui.drawRect(map.getX(), map.getY(), map.getX() + 1, map.getBottom(), 0xFF7995AA);
        Gui.drawRect(map.getRight() - 1, map.getY(), map.getRight(), map.getBottom(), 0xFF7995AA);

        TerminalLandActionPayload.Zoom zoom = zoom();
        int radius = zoom.getRadius();
        WorldClient world = Minecraft.getMinecraft().theWorld;
        boolean drawOwnership = state.isOwnershipOverlayVisible() && !Keyboard.isKeyDown(Keyboard.KEY_TAB);
        for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
            int rawTop = LandMapViewport.cellTop(map, zoom, offsetZ);
            int rawBottom = LandMapViewport.cellBottom(map, zoom, offsetZ);
            if (rawBottom <= map.getY() || rawTop >= map.getBottom()) continue;
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                int rawLeft = LandMapViewport.cellLeft(map, zoom, offsetX);
                int rawRight = LandMapViewport.cellRight(map, zoom, offsetX);
                if (rawRight <= map.getX() || rawLeft >= map.getRight()) continue;
                int chunkX = safeOffset(model.getViewportChunkX(), offsetX);
                int chunkZ = safeOffset(model.getViewportChunkZ(), offsetZ);
                int left = Math.max(map.getX(), rawLeft);
                int top = Math.max(map.getY(), rawTop);
                int right = Math.min(map.getRight(), rawRight);
                int bottom = Math.min(map.getBottom(), rawBottom);
                ResourceLocation texture = ClientLandTerrainCache.INSTANCE.textureFor(world, chunkX, chunkZ);
                if (texture == null) drawUnknown(left, top, right, bottom);
                else drawTile(texture, rawLeft, rawTop, rawRight, rawBottom, left, top, right, bottom);
                if (drawOwnership) drawClaim(chunkX, chunkZ, left, top, right, bottom);
                Gui.drawRect(left, top, right, Math.min(bottom, top + 1), 0x263B5668);
                Gui.drawRect(left, top, Math.min(right, left + 1), bottom, 0x263B5668);
                if (chunkX == model.getCenterChunkX() && chunkZ == model.getCenterChunkZ()) {
                    drawBorder(left, top, right, bottom, 0xFFE6F4FF, 1);
                }
                if (chunkX == model.getSelectedChunkX() && chunkZ == model.getSelectedChunkZ()) {
                    drawBorder(left, top, right, bottom, 0xFFFFC83D, 2);
                }
            }
        }
        drawPlayer(map, zoom);
        drawTools(map, mouseX, mouseY);
        drawStatusBar(map, mouseX, mouseY);
        if (contextOpen) drawContextMenu(map, mouseX, mouseY);
        else drawHover(map, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        GuiRect map = getBounds();
        if (!map.contains(mouseX, mouseY)) {
            contextOpen = false;
            return false;
        }
        if (mouseButton == 0 && contextOpen && handleContextClick(mouseX, mouseY)) return true;
        if (mouseButton == 0) {
            int tool = toolAt(map, mouseX, mouseY);
            if (tool >= 0) {
                activateTool(tool);
                contextOpen = false;
                return true;
            }
            contextOpen = false;
            pressX = mouseX;
            pressY = mouseY;
            pressed = true;
            return true;
        }
        if (mouseButton == 1) {
            int[] chunk = LandMapViewport.chunkAt(map, model.getViewportChunkX(), model.getViewportChunkZ(),
                zoom(), mouseX, mouseY);
            if (chunk == null) return false;
            TerminalLandSectionModel.MapCellModel cell = cells.get(key(chunk[0], chunk[1]));
            if (chunk[0] == model.getSelectedChunkX() && chunk[1] == model.getSelectedChunkZ()) {
                openContext(map, mouseX, mouseY);
            } else {
                boolean loaded = isLoaded(Minecraft.getMinecraft().theWorld, chunk[0], chunk[1]);
                state.requestContext(chunk[0], chunk[1], mouseX, mouseY);
                if (handler != null) handler.select(chunk[0], chunk[1], cell == null ? 0L : cell.getVersion(), loaded);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !pressed) return false;
        pressed = false;
        int deltaX = mouseX - pressX;
        int deltaY = mouseY - pressY;
        boolean dragging = Math.abs(deltaX) >= LandMapViewport.DRAG_THRESHOLD
            || Math.abs(deltaY) >= LandMapViewport.DRAG_THRESHOLD;
        GuiRect map = getBounds();
        if (dragging) {
            int[] center = LandMapViewport.panCenter(model.getViewportChunkX(), model.getViewportChunkZ(), map,
                zoom(), deltaX, deltaY);
            notifyViewport(center[0], center[1], zoom());
            return true;
        }
        int[] chunk = LandMapViewport.chunkAt(map, model.getViewportChunkX(), model.getViewportChunkZ(),
            zoom(), mouseX, mouseY);
        if (chunk == null) return false;
        TerminalLandSectionModel.MapCellModel mapCell = cells.get(key(chunk[0], chunk[1]));
        boolean loaded = isLoaded(Minecraft.getMinecraft().theWorld, chunk[0], chunk[1]);
        if (handler != null) handler.select(chunk[0], chunk[1], mapCell == null ? 0L : mapCell.getVersion(), loaded);
        return true;
    }

    @Override
    public boolean mouseScrolled(GuiScene scene, int mouseX, int mouseY, int wheelDelta) {
        GuiRect map = getBounds();
        if (!map.contains(mouseX, mouseY)) return false;
        contextOpen = false;
        TerminalLandActionPayload.Zoom next = LandMapViewport.stepZoom(zoom(), wheelDelta > 0 ? 1 : -1);
        if (next == zoom()) return true;
        int[] center = LandMapViewport.zoomCenter(model.getViewportChunkX(), model.getViewportChunkZ(), map,
            zoom(), next, mouseX, mouseY);
        notifyViewport(center[0], center[1], next);
        return true;
    }

    @Override
    public boolean keyTyped(GuiScene scene, char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE && contextOpen) {
            contextOpen = false;
            return true;
        }
        if (keyCode == Keyboard.KEY_F) {
            centerOnPlayer();
            return true;
        }
        if (keyCode == Keyboard.KEY_R) {
            refreshTerrain();
            if (handler != null) handler.refresh();
            return true;
        }
        if (keyCode == Keyboard.KEY_ADD || keyCode == Keyboard.KEY_EQUALS || typedChar == '+') {
            changeZoom(1);
            return true;
        }
        if (keyCode == Keyboard.KEY_SUBTRACT || keyCode == Keyboard.KEY_MINUS || typedChar == '-') {
            changeZoom(-1);
            return true;
        }
        return false;
    }

    public void changeZoom(int direction) {
        TerminalLandActionPayload.Zoom next = LandMapViewport.stepZoom(zoom(), direction);
        if (next != zoom()) notifyViewport(model.getViewportChunkX(), model.getViewportChunkZ(), next);
    }

    public void centerOnPlayer() {
        notifyViewport(model.getCenterChunkX(), model.getCenterChunkZ(), TerminalLandActionPayload.Zoom.MEDIUM);
    }

    public void refreshTerrain() {
        ClientLandTerrainCache.INSTANCE.invalidateVisible(Minecraft.getMinecraft().theWorld,
            model.getViewportChunkX(), model.getViewportChunkZ(), zoom().getRadius());
    }

    boolean isContextOpen() { return contextOpen; }

    private void notifyViewport(int x, int z, TerminalLandActionPayload.Zoom value) {
        contextOpen = false;
        int clampedX = LandMapViewport.clampAround(model.getCenterChunkX(), x, 16);
        int clampedZ = LandMapViewport.clampAround(model.getCenterChunkZ(), z, 16);
        if (handler != null) handler.viewport(clampedX, clampedZ, value);
    }

    private void drawClaim(int chunkX, int chunkZ, int left, int top, int right, int bottom) {
        TerminalLandSectionModel.MapCellModel cell = cells.get(key(chunkX, chunkZ));
        if (cell == null) return;
        int fill = "OWNED".equals(cell.getState()) ? 0x6650D6B1
            : "RESERVED".equals(cell.getState()) ? 0x776F73D8 : 0x66E66A4E;
        Gui.drawRect(left, top, right, bottom, fill);
        if ("RESERVED".equals(cell.getState())) {
            for (int offset = -Math.max(1, bottom - top); offset < right - left; offset += 5) {
                int x = left + offset;
                for (int y = top; y < bottom; y++) {
                    int drawX = x + (y - top);
                    if (drawX >= left && drawX < right) Gui.drawRect(drawX, y, drawX + 1, y + 1, 0xAA9A9DFF);
                }
            }
        }
        if (!sameClaim(cell, chunkX - 1, chunkZ)) Gui.drawRect(left, top, Math.min(right, left + 2), bottom, 0xCCFFFFFF);
        if (!sameClaim(cell, chunkX + 1, chunkZ)) Gui.drawRect(Math.max(left, right - 2), top, right, bottom, 0xCCFFFFFF);
        if (!sameClaim(cell, chunkX, chunkZ - 1)) Gui.drawRect(left, top, right, Math.min(bottom, top + 2), 0xCCFFFFFF);
        if (!sameClaim(cell, chunkX, chunkZ + 1)) Gui.drawRect(left, Math.max(top, bottom - 2), right, bottom, 0xCCFFFFFF);
    }

    private boolean sameClaim(TerminalLandSectionModel.MapCellModel cell, int x, int z) {
        TerminalLandSectionModel.MapCellModel neighbor = cells.get(key(x, z));
        if (neighbor == null || !cell.getState().equals(neighbor.getState())) return false;
        return cell.getTitleId() == 0L || cell.getTitleId() == neighbor.getTitleId();
    }

    private void drawHover(GuiRect map, int mouseX, int mouseY) {
        if (!map.contains(mouseX, mouseY) || toolAt(map, mouseX, mouseY) >= 0
            || mouseY >= map.getBottom() - STATUS_HEIGHT) return;
        int[] chunk = LandMapViewport.chunkAt(map, model.getViewportChunkX(), model.getViewportChunkZ(),
            zoom(), mouseX, mouseY);
        if (chunk == null) return;
        TerminalLandSectionModel.MapCellModel cell = cells.get(key(chunk[0], chunk[1]));
        boolean loaded = isLoaded(Minecraft.getMinecraft().theWorld, chunk[0], chunk[1]);
        List<String> lines = tooltipLines(chunk[0], chunk[1], cell, loaded);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int width = 0;
        for (String line : lines) width = Math.max(width, font.getStringWidth(line));
        width = tooltipWidth(map.getWidth(), width + 10);
        int lineStep = 9;
        int height = lines.size() * lineStep + 7;
        int x = mouseX + 10;
        if (x + width > map.getRight() - 4) x = mouseX - width - 10;
        x = Math.max(map.getX() + 4, Math.min(x, map.getRight() - width - 4));
        int y = mouseY + 10;
        if (y + height > map.getBottom() - STATUS_HEIGHT - 4) y = mouseY - height - 10;
        y = Math.max(map.getY() + 4, y);
        Gui.drawRect(x, y, x + width, y + height, 0xF013232E);
        Gui.drawRect(x, y, x + 4, y + height, 0xFFFFC83D);
        Gui.drawRect(x, y, x + width, y + 1, 0xFF8DA9BC);
        for (int index = 0; index < lines.size(); index++) {
            font.drawStringWithShadow(font.trimStringToWidth(lines.get(index), width - 13),
                x + 7, y + 4 + index * lineStep, index == 0 ? 0xFFEAF6FF : 0xFFB7C9D7);
        }
    }

    private List<String> tooltipLines(int chunkX, int chunkZ, TerminalLandSectionModel.MapCellModel cell,
        boolean loaded) {
        List<String> lines = new ArrayList<String>();
        lines.add(tr("jsirgalaxybase.land.hover.title", Integer.valueOf(chunkX), Integer.valueOf(chunkZ), stateLabel(cell)));
        long minX = (long) chunkX * 16L;
        long minZ = (long) chunkZ * 16L;
        lines.add(tr("jsirgalaxybase.land.hover.blocks", Long.valueOf(minX), Long.valueOf(minX + 15L),
            Long.valueOf(minZ), Long.valueOf(minZ + 15L)));
        long distance = Math.max(Math.abs((long) chunkX - model.getCenterChunkX()),
            Math.abs((long) chunkZ - model.getCenterChunkZ()));
        lines.add(tr("jsirgalaxybase.land.hover.terrain", Long.valueOf(distance),
            loaded ? biomeLabel(chunkX, chunkZ) : tr("jsirgalaxybase.land.map.unloaded.short")));
        String owner = cell == null || "RESERVED".equals(cell.getState()) ? "—"
            : "OWNED".equals(cell.getState()) ? tr("jsirgalaxybase.land.owner.self")
                : tr("jsirgalaxybase.land.owner.other");
        lines.add(tr("jsirgalaxybase.land.hover.owner", owner,
            cell == null || cell.getTitleId() <= 0L ? "—" : "#" + cell.getTitleId(),
            cell == null || cell.getVersion() <= 0L ? "—" : "v" + cell.getVersion()));
        lines.add(tr("jsirgalaxybase.land.hover.market.disabled"));
        lines.add(operationHint(chunkX, chunkZ, cell, loaded));
        return lines;
    }

    private String operationHint(int chunkX, int chunkZ, TerminalLandSectionModel.MapCellModel cell, boolean loaded) {
        if (cell != null) {
            if ("OWNED".equals(cell.getState())) return tr("jsirgalaxybase.land.hover.manage");
            if ("RESERVED".equals(cell.getState())) return tr("jsirgalaxybase.land.feedback.reserved");
            return tr("jsirgalaxybase.land.feedback.not_owner");
        }
        boolean selected = chunkX == model.getSelectedChunkX() && chunkZ == model.getSelectedChunkZ();
        if (selected && "BLOCKED".equals(model.getSelectedState())) {
            return tr("jsirgalaxybase.land.feedback.dimension_blocked");
        }
        long distance = Math.max(Math.abs((long) chunkX - model.getCenterChunkX()),
            Math.abs((long) chunkZ - model.getCenterChunkZ()));
        if (!loaded) return tr("jsirgalaxybase.land.map.unloaded");
        if (distance > 7L) return tr("jsirgalaxybase.land.feedback.out_of_range");
        if (model.getUsedClaims() >= model.getMaxClaims()) return tr("jsirgalaxybase.land.feedback.limit_reached");
        if (!selected) {
            return tr("jsirgalaxybase.land.hover.inspect");
        }
        return model.isCanClaim() ? tr("jsirgalaxybase.land.hover.claimable")
            : tr("jsirgalaxybase.land.feedback." + model.getFeedbackCode().toLowerCase());
    }

    private void drawPlayer(GuiRect map, TerminalLandActionPayload.Zoom zoom) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null) return;
        double cell = LandMapViewport.cellSize(map, zoom);
        double chunkX = minecraft.thePlayer.posX / 16D;
        double chunkZ = minecraft.thePlayer.posZ / 16D;
        double x = map.getX() + map.getWidth() / 2D + (chunkX - model.getViewportChunkX()) * cell;
        double y = map.getY() + map.getHeight() / 2D + (chunkZ - model.getViewportChunkZ()) * cell;
        if (x < map.getX() || x >= map.getRight() || y < map.getY() || y >= map.getBottom()) return;
        int size = playerMarkerSize(cell, zoom);
        drawDirectionArrow(x, y, size, minecraft.thePlayer.rotationYaw);
        drawPin(x, y, size);
        drawPlayerHead(minecraft, (int) Math.round(x), (int) Math.round(y - size * 0.15D), size);
    }

    private static void drawDirectionArrow(double x, double y, int size, float yaw) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, 0D);
        GL11.glRotatef(yaw + 180F, 0F, 0F, 1F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_TRIANGLES);
        tessellator.setColorRGBA(165, 235, 255, 255);
        tessellator.addVertex(0D, -size * 0.78D, 0D);
        tessellator.addVertex(-size * 0.16D, -size * 0.42D, 0D);
        tessellator.addVertex(size * 0.16D, -size * 0.42D, 0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private static void drawPin(double x, double y, int size) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, 0D);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_TRIANGLES);
        tessellator.setColorRGBA(35, 142, 208, 255);
        tessellator.addVertex(-size * 0.18D, size * 0.22D, 0D);
        tessellator.addVertex(size * 0.18D, size * 0.22D, 0D);
        tessellator.addVertex(0D, size * 0.58D, 0D);
        tessellator.draw();
        int half = size / 2;
        Gui.drawRect(-half, -half, half, half / 2, 0xFF238ED0);
        Gui.drawRect(-half + 2, -half + 2, half - 2, half / 2 - 2, 0xFFD5F3FF);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private static void drawPlayerHead(Minecraft minecraft, int x, int y, int size) {
        int head = Math.max(5, Math.round(size * 0.48F));
        int left = x - head / 2;
        int top = y - head / 2;
        if (minecraft.thePlayer instanceof AbstractClientPlayer) {
            ResourceLocation skin = ((AbstractClientPlayer) minecraft.thePlayer).getLocationSkin();
            if (skin != null) {
                minecraft.getTextureManager().bindTexture(skin);
                GL11.glColor4f(1F, 1F, 1F, 1F);
                drawSkinRegion(left, top, head, 8D / 64D, 8D / 64D, 16D / 64D, 16D / 64D);
                drawSkinRegion(left, top, head, 40D / 64D, 8D / 64D, 48D / 64D, 16D / 64D);
                return;
            }
        }
        Gui.drawRect(left, top, left + head, top + head, 0xFFD9A06F);
        Gui.drawRect(left, top, left + head, top + Math.max(2, head / 4), 0xFF3D2A22);
        Gui.drawRect(left + head / 4, top + head / 2, left + head / 4 + 2, top + head / 2 + 2, 0xFF213C55);
        Gui.drawRect(left + head * 3 / 4 - 2, top + head / 2, left + head * 3 / 4, top + head / 2 + 2, 0xFF213C55);
    }

    private static void drawSkinRegion(int left, int top, int size, double u0, double v0, double u1, double v1) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(left, top + size, 0D, u0, v1);
        tessellator.addVertexWithUV(left + size, top + size, 0D, u1, v1);
        tessellator.addVertexWithUV(left + size, top, 0D, u1, v0);
        tessellator.addVertexWithUV(left, top, 0D, u0, v0);
        tessellator.draw();
    }

    private void drawTools(GuiRect map, int mouseX, int mouseY) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String[] labels = { "+", "−", "⌖", "↻", tr("jsirgalaxybase.land.map.layer.short") };
        int x = map.getX() + 8;
        int y = map.getY() + 8;
        Gui.drawRect(x - 2, y - 2, x + TOOL_SIZE + 2,
            y + labels.length * (TOOL_SIZE + TOOL_GAP) - TOOL_GAP + 2, 0xD912242F);
        for (int index = 0; index < labels.length; index++) {
            int top = y + index * (TOOL_SIZE + TOOL_GAP);
            boolean hover = mouseX >= x && mouseX < x + TOOL_SIZE && mouseY >= top && mouseY < top + TOOL_SIZE;
            boolean active = index != 4 || state.isOwnershipOverlayVisible();
            Gui.drawRect(x, top, x + TOOL_SIZE, top + TOOL_SIZE,
                hover ? 0xFF477FA8 : active ? 0xFF315F83 : 0xFF233746);
            Gui.drawRect(x, top, x + TOOL_SIZE, top + 1, 0xFF8EB4CD);
            int textX = x + (TOOL_SIZE - font.getStringWidth(labels[index])) / 2;
            font.drawStringWithShadow(labels[index], textX, top + Math.max(2, (TOOL_SIZE - 8) / 2),
                active ? 0xFFEAF6FF : 0xFF738999);
        }
    }

    private void drawStatusBar(GuiRect map, int mouseX, int mouseY) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int top = map.getBottom() - STATUS_HEIGHT;
        Gui.drawRect(map.getX() + 5, top, map.getRight() - 5, map.getBottom() - 4, 0xD912242F);
        int legendX = map.getX() + 10;
        legendX = drawLegend(font, legendX, top + 3, 0xFF50D6B1, tr("jsirgalaxybase.land.legend.mine"));
        legendX = drawLegend(font, legendX, top + 3, 0xFFE66A4E, tr("jsirgalaxybase.land.legend.other"));
        legendX = drawLegend(font, legendX, top + 3, 0xFF817EE3, tr("jsirgalaxybase.land.legend.reserved"));
        drawLegend(font, legendX, top + 3, 0xFFFFC83D, tr("jsirgalaxybase.land.legend.selected"));
        int[] chunk = LandMapViewport.chunkAt(map, model.getViewportChunkX(), model.getViewportChunkZ(),
            zoom(), mouseX, mouseY);
        String pointer = chunk == null ? "—" : "[" + chunk[0] + "," + chunk[1] + "]";
        String status = tr("jsirgalaxybase.land.map.status", tr("jsirgalaxybase.land.zoom." + zoom().name().toLowerCase()),
            Integer.valueOf(LandMapViewport.visibleColumns(map, zoom())),
            Integer.valueOf(LandMapViewport.visibleRows(map, zoom())), pointer);
        int width = font.getStringWidth(status);
        font.drawStringWithShadow(status, Math.max(map.getX() + 10, map.getRight() - width - 10), top + 3, 0xFF91A8B8);
    }

    private static int drawLegend(FontRenderer font, int x, int y, int color, String label) {
        Gui.drawRect(x, y + 2, x + 6, y + 8, color);
        font.drawStringWithShadow(label, x + 9, y, 0xFFB8CAD7);
        return x + 13 + font.getStringWidth(label);
    }

    private void drawContextMenu(GuiRect map, int mouseX, int mouseY) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int x = Math.max(map.getX() + 4, Math.min(contextX, map.getRight() - CONTEXT_WIDTH - 4));
        int height = CONTEXT_ROW_HEIGHT * 2 + 6;
        int y = Math.max(map.getY() + 4, Math.min(contextY, map.getBottom() - height - 4));
        contextX = x;
        contextY = y;
        Gui.drawRect(x, y, x + CONTEXT_WIDTH, y + height, 0xF51A2C38);
        Gui.drawRect(x, y, x + CONTEXT_WIDTH, y + 1, 0xFF8EABBE);
        boolean actionEnabled = model.isCanClaim() || model.isCanUnclaim();
        String action = model.isCanUnclaim() ? tr("jsirgalaxybase.land.unclaim")
            : model.isCanClaim() ? tr("jsirgalaxybase.land.claim") : tr("jsirgalaxybase.land.context.unavailable");
        drawContextRow(font, x, y + 3, tr("jsirgalaxybase.land.context.details"), true, mouseX, mouseY);
        drawContextRow(font, x, y + 3 + CONTEXT_ROW_HEIGHT, action, actionEnabled, mouseX, mouseY);
    }

    private static void drawContextRow(FontRenderer font, int x, int y, String text, boolean enabled,
        int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x + 2 && mouseX < x + CONTEXT_WIDTH - 2
            && mouseY >= y && mouseY < y + CONTEXT_ROW_HEIGHT;
        if (hover) Gui.drawRect(x + 2, y, x + CONTEXT_WIDTH - 2, y + CONTEXT_ROW_HEIGHT, 0xFF315F83);
        font.drawStringWithShadow(font.trimStringToWidth(text, CONTEXT_WIDTH - 12), x + 6, y + 5,
            enabled ? 0xFFE4F1FA : 0xFF718391);
    }

    private boolean handleContextClick(int mouseX, int mouseY) {
        int height = CONTEXT_ROW_HEIGHT * 2 + 6;
        if (mouseX < contextX || mouseX >= contextX + CONTEXT_WIDTH || mouseY < contextY
            || mouseY >= contextY + height) {
            contextOpen = false;
            return false;
        }
        int row = (mouseY - contextY - 3) / CONTEXT_ROW_HEIGHT;
        if (row <= 0) {
            contextOpen = false;
            return true;
        }
        if (model.isCanClaim() || model.isCanUnclaim()) {
            contextOpen = false;
            if (handler != null) handler.contextAction(model.isCanUnclaim());
        }
        return true;
    }

    private void openContext(GuiRect map, int mouseX, int mouseY) {
        contextOpen = true;
        contextX = Math.max(map.getX() + 4, Math.min(mouseX, map.getRight() - CONTEXT_WIDTH - 4));
        contextY = Math.max(map.getY() + 4, Math.min(mouseY, map.getBottom() - CONTEXT_ROW_HEIGHT * 2 - 10));
    }

    private int toolAt(GuiRect map, int mouseX, int mouseY) {
        int x = map.getX() + 8;
        int y = map.getY() + 8;
        if (mouseX < x || mouseX >= x + TOOL_SIZE || mouseY < y) return -1;
        int stride = TOOL_SIZE + TOOL_GAP;
        int index = (mouseY - y) / stride;
        if (index < 0 || index >= 5 || (mouseY - y) % stride >= TOOL_SIZE) return -1;
        return index;
    }

    private void activateTool(int tool) {
        if (tool == 0) changeZoom(1);
        else if (tool == 1) changeZoom(-1);
        else if (tool == 2) centerOnPlayer();
        else if (tool == 3) {
            refreshTerrain();
            if (handler != null) handler.refresh();
        } else if (tool == 4) state.toggleOwnershipOverlay();
    }

    private String biomeLabel(int chunkX, int chunkZ) {
        WorldClient world = Minecraft.getMinecraft().theWorld;
        if (!isLoaded(world, chunkX, chunkZ)) return tr("jsirgalaxybase.land.map.unloaded.short");
        try {
            Chunk chunk = world.getChunkProvider().provideChunk(chunkX, chunkZ);
            BiomeGenBase biome = chunk.getBiomeGenForWorldCoords(8, 8, world.getWorldChunkManager());
            if (biome == null) return tr("jsirgalaxybase.land.biome.unknown", Integer.valueOf(-1));
            String translationKey = "biome." + biome.biomeName + ".name";
            String localized = I18n.format(translationKey);
            return translationKey.equals(localized)
                ? tr("jsirgalaxybase.land.biome.unknown", Integer.valueOf(biome.biomeID)) : localized;
        } catch (RuntimeException ignored) {
            return tr("jsirgalaxybase.land.biome.unknown", Integer.valueOf(-1));
        }
    }

    private String stateLabel(TerminalLandSectionModel.MapCellModel cell) {
        String value = cell == null ? "unclaimed" : cell.getState().toLowerCase();
        return tr("jsirgalaxybase.land.state." + value);
    }

    private static void drawUnknown(int left, int top, int right, int bottom) {
        Gui.drawRect(left, top, right, bottom, 0xFF17242D);
        for (int offset = -Math.max(1, bottom - top); offset < right - left; offset += 6) {
            for (int y = top; y < bottom; y++) {
                int x = left + offset + y - top;
                if (x >= left && x < right) Gui.drawRect(x, y, x + 1, y + 1, 0xFF263B49);
            }
        }
    }

    private static void drawTile(ResourceLocation texture, int rawLeft, int rawTop, int rawRight, int rawBottom,
        int left, int top, int right, int bottom) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        double width = Math.max(1D, rawRight - rawLeft);
        double height = Math.max(1D, rawBottom - rawTop);
        double u0 = (left - rawLeft) / width;
        double u1 = (right - rawLeft) / width;
        double v0 = (top - rawTop) / height;
        double v1 = (bottom - rawTop) / height;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(left, bottom, 0D, u0, v1);
        tessellator.addVertexWithUV(right, bottom, 0D, u1, v1);
        tessellator.addVertexWithUV(right, top, 0D, u1, v0);
        tessellator.addVertexWithUV(left, top, 0D, u0, v0);
        tessellator.draw();
    }

    private static void drawBorder(int left, int top, int right, int bottom, int color, int width) {
        if (right <= left || bottom <= top) return;
        Gui.drawRect(left, top, right, Math.min(bottom, top + width), color);
        Gui.drawRect(left, Math.max(top, bottom - width), right, bottom, color);
        Gui.drawRect(left, top, Math.min(right, left + width), bottom, color);
        Gui.drawRect(Math.max(left, right - width), top, right, bottom, color);
    }

    private TerminalLandActionPayload.Zoom zoom() {
        try { return TerminalLandActionPayload.Zoom.valueOf(model.getZoom()); }
        catch (IllegalArgumentException ignored) { return TerminalLandActionPayload.Zoom.MEDIUM; }
    }

    static int playerMarkerSize(double cellSize, TerminalLandActionPayload.Zoom zoom) {
        TerminalLandActionPayload.Zoom value = zoom == null ? TerminalLandActionPayload.Zoom.MEDIUM : zoom;
        double ratio = value == TerminalLandActionPayload.Zoom.FAR ? 0.75D : 0.90D;
        int minimum = value == TerminalLandActionPayload.Zoom.FAR ? 6 : 8;
        int maximum = value == TerminalLandActionPayload.Zoom.FAR ? 12 : 20;
        return Math.max(minimum, Math.min(maximum, (int) Math.round(Math.max(1D, cellSize) * ratio)));
    }

    static int tooltipWidth(int mapWidth, int contentWidth) {
        int responsiveMaximum = Math.max(88, Math.min(132, mapWidth * 42 / 100));
        return Math.max(88, Math.min(responsiveMaximum, contentWidth));
    }

    private static boolean isLoaded(WorldClient world, int chunkX, int chunkZ) {
        return world != null && world.getChunkProvider() != null
            && world.getChunkProvider().chunkExists(chunkX, chunkZ);
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

    private static int safeOffset(int value, int offset) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) value + offset));
    }

    private static String tr(String key, Object... args) { return I18n.format(key, args); }
}

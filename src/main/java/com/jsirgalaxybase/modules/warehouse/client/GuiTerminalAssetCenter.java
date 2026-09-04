package com.jsirgalaxybase.modules.warehouse.client;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.client.gui.framework.AbstractGuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft.BaseVaultSortRequestMessage;
import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseCellInspector;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterContainer;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterRequestMessage;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterTab;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalCellContentActionMessage;
import com.jsirgalaxybase.terminal.TerminalHudOverlayHandler;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityRow;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityType;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentAction;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentEntry;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;
import com.jsirgalaxybase.terminal.client.TerminalClientScreenController;
import com.jsirgalaxybase.terminal.client.TerminalRouteCoordinator;
import com.jsirgalaxybase.terminal.client.component.TerminalPanelFactory;
import com.jsirgalaxybase.terminal.client.component.TerminalPopupFactory;
import com.jsirgalaxybase.terminal.client.component.TerminalShellPanels;
import com.jsirgalaxybase.terminal.client.screen.TerminalContainerScreenBase;
import com.jsirgalaxybase.terminal.client.screen.TerminalHomeLayout;
import com.jsirgalaxybase.terminal.client.screen.TerminalShellFrame;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

/** Native Vault/Bay/backpack surface hosted inside the shared terminal shell. */
public final class GuiTerminalAssetCenter extends TerminalContainerScreenBase<GuiTerminalAssetCenter.ClientContainer> {

    private static final int SLOT_BORDER = 0xFF345168;
    private static final int SLOT_FILL = 0xFF09131D;
    private static final int ACCENT = 0xFF3D8BD1;
    private static final int GREEN = 0xFF70E67C;
    private static final int ORANGE = 0xFFFFB52E;

    private TerminalAssetCenterTab activeTab;
    private TerminalAssetCenterContentLayout contentLayout;
    private AssetActivitySnapshot activitySnapshot = AssetActivitySnapshot.empty();
    private TerminalCellContentSnapshot cellSnapshot = TerminalCellContentSnapshot.empty();
    private int cellPage;

    public GuiTerminalAssetCenter(InventoryPlayer inventory, int initialTabCode) {
        this(new ClientContainer(inventory), TerminalAssetCenterTab.fromCode(initialTabCode));
    }

    private GuiTerminalAssetCenter(ClientContainer container, TerminalAssetCenterTab initialTab) {
        super(container, (GuiScreen) null,
            TerminalClientScreenController.INSTANCE.getLastHomeScreen(TerminalPage.WAREHOUSE.getId()));
        activeTab = initialTab == null ? TerminalAssetCenterTab.STORAGE : initialTab;
    }

    @Override protected PanelContainer buildRootPanel() {
        TerminalHomeLayout shell = getTerminalLayout();
        contentLayout = TerminalAssetCenterContentLayout.compute(shell.getBodyBounds(), shell.getPanelBounds());
        getTypedContainer().layoutSlots(activeTab, contentLayout);
        PanelContainer root = new PanelContainer();
        root.setBounds(new GuiRect(0, 0, width, height));
        final TerminalPanelFactory panels = new TerminalPanelFactory();
        TerminalShellFrame.addBackdrop(root, panels, width, height, shell);
        root.addChild(new AssetContentPanel(shell.getBodyBounds()));
        TerminalShellFrame.addStatusBand(root, panels, shell, getShellModel(),
            new Runnable() { @Override public void run() { requestSnapshot(); } },
            new Runnable() { @Override public void run() { openHelp(); } },
            new Runnable() { @Override public void run() { TerminalRouteCoordinator.openTerminalPage(TerminalPage.HOME.getId()); } },
            new Runnable() { @Override public void run() { TerminalRouteCoordinator.closeTerminal(); } }, null);
        TerminalShellFrame.addNavigation(root, panels, shell, getShellModel(),
            new TerminalShellPanels.NavigationHandler() {
                @Override public void open(TerminalHomeScreenModel.NavItemModel item) {
                    if (item != null && item.isEnabled()
                        && !TerminalPage.WAREHOUSE.getId().equals(item.getPageId())) {
                        TerminalRouteCoordinator.openTerminalPage(item.getPageId());
                    }
                }
            });
        requestSnapshot();
        return root;
    }

    public void applyActivitySnapshot(AssetActivitySnapshot snapshot) {
        activitySnapshot = snapshot == null ? AssetActivitySnapshot.empty() : snapshot;
    }

    public void applySnapshot(AssetActivitySnapshot activity, TerminalCellContentSnapshot cell) {
        applyActivitySnapshot(activity);
        cellSnapshot = cell == null ? TerminalCellContentSnapshot.empty() : cell;
        cellPage = cellSnapshot.getPageIndex();
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!hasOpenPopup() && activeTab == TerminalAssetCenterTab.STORAGE) drawCellTooltip(mouseX, mouseY);
        if (!hasOpenPopup()) TerminalHudOverlayHandler.INSTANCE.drawTerminalNotifications(fontRendererObj, width, height);
    }

    @Override protected void drawNativeContainerBackground(float partialTicks, int mouseX, int mouseY) {
        if (activeTab != TerminalAssetCenterTab.STORAGE) return;
        for (Object value : inventorySlots.inventorySlots) {
            Slot slot = (Slot) value;
            if (slot.xDisplayPosition < 0 || slot.yDisplayPosition < 0) continue;
            int x = guiLeft + slot.xDisplayPosition;
            int y = guiTop + slot.yDisplayPosition;
            Gui.drawRect(x - 1, y - 1, x + 17, y + 17, SLOT_BORDER);
            Gui.drawRect(x, y, x + 16, y + 16, SLOT_FILL);
        }
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) { }

    @Override protected void handleMouseClick(Slot slot, int slotId, int button, int mode) {
        if (activeTab != TerminalAssetCenterTab.STORAGE || slotId < 0 || !getTypedContainer().isVisibleSlot(slotId)) return;
        super.handleMouseClick(slot, slotId, button, mode);
    }

    private void applyTab(TerminalAssetCenterTab tab) {
        activeTab = tab == null ? TerminalAssetCenterTab.STORAGE : tab;
        getTypedContainer().layoutSlots(activeTab, contentLayout);
        requestSnapshot();
    }

    private void requestSnapshot() {
        int pageSize = contentLayout == null ? 20 : contentLayout.getCellPageSize();
        TerminalNetwork.CHANNEL.sendToServer(new TerminalAssetCenterRequestMessage(activeTab, cellPage, pageSize));
    }

    private void openHelp() {
        openPopup(TerminalPopupFactory.createInfoPopup(width, height, "银河资产中心",
            "存储管理直接显示 Base Vault、AE2 Cell Bay 与玩家背包。Shift 点击背包物品始终进入 Base Vault；Cell 需要手动拖入 Bay。",
            "资产动态只显示市场成交、交付、返还和恢复等系统业务事件。",
            new Runnable() { @Override public void run() { closePopup(); } }));
    }

    private final class AssetContentPanel extends AbstractGuiPanel {
        private AssetContentPanel(GuiRect bounds) { setBounds(bounds); }

        @Override public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect body = getBounds();
            Gui.drawRect(body.getX(), body.getY(), body.getRight(), body.getBottom(),
                scene.getTheme().color(ThemeColorKey.PANEL_FILL));
            drawTab(contentLayout.getStorageTab(), "存储管理", activeTab == TerminalAssetCenterTab.STORAGE, mouseX, mouseY);
            drawTab(contentLayout.getActivityTab(), "资产动态", activeTab == TerminalAssetCenterTab.ACTIVITY, mouseX, mouseY);
            if (activeTab == TerminalAssetCenterTab.STORAGE) drawStorage(scene);
            else drawActivity(scene);
        }

        @Override public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int button) {
            if (button == 0 && contentLayout.getStorageTab().contains(mouseX, mouseY)) {
                applyTab(TerminalAssetCenterTab.STORAGE); return true;
            }
            if (button == 0 && contentLayout.getActivityTab().contains(mouseX, mouseY)) {
                applyTab(TerminalAssetCenterTab.ACTIVITY); return true;
            }
            GuiRect sort = sortBounds();
            if (button == 0 && activeTab == TerminalAssetCenterTab.STORAGE && sort.contains(mouseX, mouseY)) {
                if (Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null)
                    TerminalNetwork.CHANNEL.sendToServer(new BaseVaultSortRequestMessage());
                return true;
            }
            if (activeTab == TerminalAssetCenterTab.STORAGE && cellSnapshot.isCellPresent()) {
                if (button == 0 && contentLayout.getDepositButton().contains(mouseX, mouseY)) {
                    sendCellAction(TerminalCellContentAction.INJECT_CURSOR, null);
                    return true;
                }
                List<TerminalCellContentEntry> entries = cellSnapshot.getEntries();
                for (int index = 0; index < entries.size(); index++) {
                    if (contentLayout.cellEntryBounds(index).contains(mouseX, mouseY)) {
                        sendCellAction(button == 1 ? TerminalCellContentAction.EXTRACT_ONE
                            : TerminalCellContentAction.EXTRACT_STACK, entries.get(index).getStack());
                        return true;
                    }
                }
                if (button == 0 && contentLayout.getPreviousButton().contains(mouseX, mouseY)
                    && cellPage > 0) { cellPage--; requestSnapshot(); return true; }
                if (button == 0 && contentLayout.getNextButton().contains(mouseX, mouseY)
                    && cellPage + 1 < cellSnapshot.getTotalPages()) { cellPage++; requestSnapshot(); return true; }
            }
            return false;
        }

        private void drawStorage(GuiScene scene) {
            Minecraft mc = Minecraft.getMinecraft();
            GuiRect left = contentLayout.getLeftPane();
            GuiRect right = contentLayout.getRightPane();
            Gui.drawRect(left.getX(), left.getY(), left.getRight(), left.getBottom(), 0xFF101C28);
            Gui.drawRect(right.getX(), right.getY(), right.getRight(), right.getBottom(), 0xFF101C28);
            GuiRect bay = contentLayout.getBayBand();
            ItemStack cell = bayCell();
            String bayText = cell == null ? "Bay：拖入 Cell" : trim(cell.getDisplayName(), Math.max(24, bay.getWidth() - 29));
            mc.fontRenderer.drawString(bayText, bay.getX() + 23, bay.getY() + 3, cell == null ? ORANGE : GREEN);
            GuiRect sort = sortBounds();
            drawTab(sort, "整理 Vault", false, -1, -1);
            int panelX = getTerminalLayout().getPanelBounds().getX();
            int panelY = getTerminalLayout().getPanelBounds().getY();
            mc.fontRenderer.drawString("Base Vault · 27 格", panelX + contentLayout.getVaultX(),
                panelY + contentLayout.getVaultY() - 10, scene.getTheme().color(ThemeColorKey.TEXT_PRIMARY));
            mc.fontRenderer.drawString("玩家背包", panelX + contentLayout.getPlayerX(),
                panelY + contentLayout.getPlayerY() - 10, scene.getTheme().color(ThemeColorKey.TEXT_PRIMARY));
            drawCellContents(scene, cell);
        }

        private void drawCellContents(GuiScene scene, ItemStack cell) {
            Minecraft mc = Minecraft.getMinecraft();
            GuiRect right = contentLayout.getRightPane();
            if (cell == null || !cellSnapshot.isCellPresent()) {
                mc.fontRenderer.drawString("插入 Cell 后显示内容", right.getX() + 6,
                    right.getY() + 30, scene.getTheme().color(ThemeColorKey.TEXT_SECONDARY));
                return;
            }
            GuiRect deposit = contentLayout.getDepositButton();
            drawTab(deposit, mc.thePlayer.inventory.getItemStack() == null ? "拿起物品后存入" : "存入手持物品",
                false, -1, -1);
            int index = 0;
            for (TerminalCellContentEntry entry : cellSnapshot.getEntries()) {
                GuiRect bounds = contentLayout.cellEntryBounds(index++);
                Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), SLOT_BORDER);
                Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getRight() - 1, bounds.getBottom() - 1, SLOT_FILL);
                ItemStack stack = entry.getStack();
                itemRender.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack,
                    bounds.getX() + 1, bounds.getY() + 1);
                String quantity = compact(entry.getQuantity());
                mc.fontRenderer.drawStringWithShadow(quantity,
                    bounds.getRight() - mc.fontRenderer.getStringWidth(quantity), bounds.getBottom() - 8, 0xFFFFFFFF);
            }
            GuiRect previous = contentLayout.getPreviousButton(); GuiRect next = contentLayout.getNextButton();
            drawTab(previous, "‹", false, -1, -1); drawTab(next, "›", false, -1, -1);
            String page = (cellSnapshot.getPageIndex() + 1) + "/" + cellSnapshot.getTotalPages();
            mc.fontRenderer.drawString(page, right.getX() + (right.getWidth() - mc.fontRenderer.getStringWidth(page)) / 2,
                right.getBottom() - 12, scene.getTheme().color(ThemeColorKey.TEXT_SECONDARY));
            String facts = compact(cellSnapshot.getUsedBytes()) + "/" + compact(cellSnapshot.getTotalBytes())
                + "B · " + cellSnapshot.getStoredTypes() + "/" + cellSnapshot.getTotalTypes() + "类";
            if (!cellSnapshot.getFeedback().isEmpty() && !"SUCCESS".equals(cellSnapshot.getFeedback())) {
                mc.fontRenderer.drawString(trim(cellFeedback(cellSnapshot.getFeedback()), right.getWidth() - 4),
                    right.getX() + 2, contentLayout.getCellGrid().getBottom() + 1, ORANGE);
            } else mc.fontRenderer.drawString(trim(facts, right.getWidth() - 4), right.getX() + 2,
                contentLayout.getCellGrid().getBottom() + 1, scene.getTheme().color(ThemeColorKey.TEXT_SECONDARY));
        }

        private void drawActivity(GuiScene scene) {
            Minecraft mc = Minecraft.getMinecraft();
            int y = contentLayout.getBayBand().getY() + 3;
            mc.fontRenderer.drawString("系统资产业务动态", getBounds().getX() + 5, y,
                scene.getTheme().color(ThemeColorKey.TEXT_PRIMARY));
            y += 14;
            if (activitySnapshot.getRows().isEmpty()) {
                mc.fontRenderer.drawString("暂无市场成交、交付、返还或恢复动态", getBounds().getX() + 7, y,
                    scene.getTheme().color(ThemeColorKey.TEXT_SECONDARY));
                return;
            }
            for (AssetActivityRow row : activitySnapshot.getRows()) {
                if (y + 18 >= getBounds().getBottom()) break;
                ItemStack stack = row.getItemStack();
                int textX = getBounds().getX() + 7;
                if (stack != null) {
                    itemRender.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, textX, y - 2);
                    textX += 20;
                }
                String first = activityLabel(row.getType()) + " · "
                    + (stack == null ? row.getReference() : stack.getDisplayName())
                    + (row.getQuantity() > 0L ? " ×" + compact(row.getQuantity()) : "");
                mc.fontRenderer.drawString(trim(first, getBounds().getRight() - textX - 6), textX, y,
                    row.getType() == AssetActivityType.DELIVERY_FAILED
                        || row.getType() == AssetActivityType.RECOVERY_REQUIRED ? ORANGE
                            : scene.getTheme().color(ThemeColorKey.TEXT_PRIMARY));
                String second = formatTime(row) + (row.getAmount() > 0L ? " · " + compact(row.getAmount()) + " GT" : "")
                    + (row.getFee() > 0L ? " · 费 " + compact(row.getFee()) : "") + " · " + row.getReference();
                mc.fontRenderer.drawString(trim(second, getBounds().getRight() - textX - 6), textX, y + 9,
                    scene.getTheme().color(ThemeColorKey.TEXT_SECONDARY));
                y += 21;
            }
        }

        private GuiRect sortBounds() {
            GuiRect left = contentLayout.getLeftPane();
            return new GuiRect(left.getRight() - 61, left.getY() + 1, 59, 11);
        }

        private void drawTab(GuiRect bounds, String label, boolean selected, int mouseX, int mouseY) {
            boolean hover = bounds.contains(mouseX, mouseY);
            int border = selected ? ACCENT : hover ? 0xFF74B9F2 : 0xFF2A4051;
            int fill = selected ? 0xFF174B78 : hover ? 0xFF244863 : 0xFF0A141E;
            Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), border);
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getRight() - 1, bounds.getBottom() - 1, fill);
            Minecraft.getMinecraft().fontRenderer.drawString(label,
                bounds.getX() + Math.max(2, (bounds.getWidth() - Minecraft.getMinecraft().fontRenderer.getStringWidth(label)) / 2),
                bounds.getY() + Math.max(2, (bounds.getHeight() - 8) / 2), 0xFFE3F0F7);
        }
    }

    private ItemStack bayCell() {
        return ((Slot) inventorySlots.inventorySlots.get(TerminalAssetCenterContainer.BAY_SLOT_INDEX)).getStack();
    }

    private void sendCellAction(TerminalCellContentAction action, ItemStack target) {
        TerminalNetwork.CHANNEL.sendToServer(new TerminalCellContentActionMessage(
            TerminalCellContentActionMessage.nextRequestId(), cellSnapshot.getBayVersion(), action, target));
    }

    private void drawCellTooltip(int mouseX, int mouseY) {
        if (!cellSnapshot.isCellPresent() || contentLayout == null) return;
        List<TerminalCellContentEntry> entries = cellSnapshot.getEntries();
        for (int index = 0; index < entries.size(); index++) {
            if (!contentLayout.cellEntryBounds(index).contains(mouseX, mouseY)) continue;
            TerminalCellContentEntry entry = entries.get(index);
            List<String> lines = new ArrayList<String>();
            lines.add(entry.getStack().getDisplayName());
            lines.add("Cell 数量：" + compact(entry.getQuantity()));
            lines.add("左键取一组，右键取一个；光标必须为空");
            drawHoveringText(lines, substrSafe(mouseX + 8), mouseY, fontRendererObj);
            return;
        }
    }

    private int substrSafe(int x) { return Math.min(width - 8, Math.max(8, x)); }

    private String cellFeedback(String code) {
        if ("CURSOR_NOT_EMPTY".equals(code)) return "请先放下光标物品";
        if ("CURSOR_EMPTY".equals(code)) return "请先从背包拿起物品";
        if ("NO_CELL".equals(code)) return "Cell 已被取出";
        if ("CELL_FULL".equals(code)) return "Cell 空间或类型已满";
        if ("ITEM_NOT_FOUND".equals(code)) return "Cell 中已无该物品";
        if ("VERSION_CONFLICT".equals(code)) return "Cell 状态已更新，请重试";
        if ("REQUEST_CONFLICT".equals(code)) return "请求冲突";
        if ("RUNTIME_UNAVAILABLE".equals(code)) return "Cell 服务暂不可用";
        return code;
    }

    private String compactCellCapacity(ItemStack cell) {
        TerminalWarehouseCellInspector.CellFacts facts = TerminalWarehouseCellInspector.inspect(cell);
        return facts.isValid() ? facts.getUsedBytes() + "/" + facts.getTotalBytes() + " Bytes" : "状态待同步";
    }

    private String trim(String value, int width) {
        return Minecraft.getMinecraft().fontRenderer.trimStringToWidth(value == null ? "" : value, Math.max(16, width));
    }

    private String activityLabel(AssetActivityType type) {
        if (type == AssetActivityType.STANDARD_BUY_DEPOSITED) return "标准市场买入入库";
        if (type == AssetActivityType.STANDARD_SELL_SETTLED) return "标准市场卖出结算";
        if (type == AssetActivityType.CUSTOM_BUY_DELIVERED) return "定制市场买入交付";
        if (type == AssetActivityType.CUSTOM_SELL_SETTLED) return "定制市场卖出结算";
        if (type == AssetActivityType.ASSET_RETURNED) return "撤单资产返还";
        if (type == AssetActivityType.DELIVERY_FAILED) return "交付失败";
        if (type == AssetActivityType.RECOVERY_COMPLETED) return "恢复完成";
        return "需要恢复";
    }

    private String formatTime(AssetActivityRow row) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.ROOT).format(new Date(row.getCreatedAt().toEpochMilli()));
    }

    private String compact(long value) {
        if (value < 1000L) return String.valueOf(value);
        if (value < 1000000L) return trimDecimal(value / 1000D) + "K";
        if (value < 1000000000L) return trimDecimal(value / 1000000D) + "M";
        if (value < 1000000000000L) return trimDecimal(value / 1000000000D) + "G";
        return trimDecimal(value / 1000000000000D) + "T";
    }

    private String trimDecimal(double value) {
        return value >= 100D ? String.format(Locale.ROOT, "%.0f", value)
            : value >= 10D ? String.format(Locale.ROOT, "%.1f", value)
                : String.format(Locale.ROOT, "%.2f", value);
    }

    static final class ClientContainer extends Container {
        private final InventoryBasic vault = new InventoryBasic("Base Vault", true, 27);
        private final InventoryBasic bay = new InventoryBasic("AE2 Cell Bay", true, 1);
        private TerminalAssetCenterTab currentTab = TerminalAssetCenterTab.STORAGE;

        private ClientContainer(InventoryPlayer inventory) {
            for (int index = 0; index < 27; index++) addSlotToContainer(new Slot(vault, index, -1000, -1000));
            addSlotToContainer(new Slot(bay, 0, -1000, -1000) {
                @Override public int getSlotStackLimit() { return 1; }
                @Override public boolean isItemValid(ItemStack stack) { return TerminalWarehouseCellInspector.isValidCell(stack); }
            });
            for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
                addSlotToContainer(new Slot(inventory, column + row * 9 + 9, -1000, -1000));
            for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(inventory, column, -1000, -1000));
        }

        private void layoutSlots(TerminalAssetCenterTab tab, TerminalAssetCenterContentLayout layout) {
            currentTab = tab == null ? TerminalAssetCenterTab.STORAGE : tab;
            for (Object value : inventorySlots) { Slot slot = (Slot) value; slot.xDisplayPosition = -1000; slot.yDisplayPosition = -1000; }
            if (!currentTab.exposesInventory() || layout == null) return;
            for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
                Slot slot = (Slot) inventorySlots.get(column + row * 9);
                slot.xDisplayPosition = layout.getVaultX() + column * 18;
                slot.yDisplayPosition = layout.getVaultY() + row * 18;
            }
            Slot cell = (Slot) inventorySlots.get(TerminalAssetCenterContainer.BAY_SLOT_INDEX);
            cell.xDisplayPosition = layout.getBaySlotX();
            cell.yDisplayPosition = layout.getBaySlotY();
            for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
                Slot slot = (Slot) inventorySlots.get(TerminalAssetCenterContainer.PLAYER_SLOT_START + row * 9 + column);
                slot.xDisplayPosition = layout.getPlayerX() + column * 18;
                slot.yDisplayPosition = layout.getPlayerY() + row * 18;
            }
            for (int column = 0; column < 9; column++) {
                Slot slot = (Slot) inventorySlots.get(TerminalAssetCenterContainer.PLAYER_SLOT_START + 27 + column);
                slot.xDisplayPosition = layout.getPlayerX() + column * 18;
                slot.yDisplayPosition = layout.getPlayerY() + 54;
            }
        }

        private boolean isVisibleSlot(int slotId) {
            return currentTab == TerminalAssetCenterTab.STORAGE && slotId >= 0 && slotId < inventorySlots.size();
        }

        @Override public boolean canInteractWith(EntityPlayer player) { return true; }

        @Override public ItemStack transferStackInSlot(EntityPlayer player, int slotId) {
            if (!isVisibleSlot(slotId)) return null;
            Slot slot = (Slot) inventorySlots.get(slotId);
            if (slot == null || !slot.getHasStack()) return null;
            ItemStack source = slot.getStack(); ItemStack original = source.copy();
            if (slotId < TerminalAssetCenterContainer.VAULT_SLOT_COUNT
                || slotId == TerminalAssetCenterContainer.BAY_SLOT_INDEX) {
                if (!mergeItemStack(source, TerminalAssetCenterContainer.PLAYER_SLOT_START, inventorySlots.size(), true)) return null;
            } else if (slotId >= TerminalAssetCenterContainer.PLAYER_SLOT_START) {
                if (!mergeItemStack(source, 0, TerminalAssetCenterContainer.VAULT_SLOT_COUNT, false)) return null;
            } else return null;
            if (source.stackSize == 0) slot.putStack(null); else slot.onSlotChanged();
            if (source.stackSize == original.stackSize) return null;
            slot.onPickupFromSlot(player, source);
            return original;
        }
    }
}

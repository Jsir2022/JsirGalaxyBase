package com.jsirgalaxybase.terminal.client.component;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.ModalPopupPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.client.TerminalNumberFormat;

/**
 * Read-only terminal picker for Base Vault assets. It never moves stacks: the selected slot and
 * quantity are sent back through the normal market action request and revalidated server-side.
 */
public final class VaultAssetPickerPopup extends ModalPopupPanel {

    public interface SelectionHandler {
        void select(TerminalMarketSectionModel.VaultAssetModel asset, int quantity);
        void cancel();
    }

    private final List<TerminalMarketSectionModel.VaultAssetModel> assets;
    private final Predicate<TerminalMarketSectionModel.VaultAssetModel> selectable;
    private final SelectionHandler handler;
    private TerminalMarketSectionModel.VaultAssetModel selected;
    private int quantity = 1;
    private final ButtonPanel confirm;
    private final ButtonPanel cancel;
    private final ButtonPanel decrease;
    private final ButtonPanel increase;

    public VaultAssetPickerPopup(int screenWidth, int screenHeight, List<TerminalMarketSectionModel.VaultAssetModel> assets,
        Predicate<TerminalMarketSectionModel.VaultAssetModel> selectable, SelectionHandler handler) {
        this.assets = assets == null ? Collections.<TerminalMarketSectionModel.VaultAssetModel>emptyList() : assets;
        this.selectable = selectable == null ? value -> true : selectable;
        this.handler = handler;
        int popupWidth = Math.min(390, Math.max(280, screenWidth - 76));
        int popupHeight = Math.min(290, Math.max(218, screenHeight - 92));
        confirm = new ButtonPanel(() -> "选择", () -> {
            if (selected != null && this.selectable.test(selected) && this.handler != null) {
                this.handler.select(selected, quantity);
            }
        }, () -> Boolean.valueOf(selected != null && selectable.test(selected)), 0.86F);
        cancel = new ButtonPanel(() -> "取消", () -> { if (this.handler != null) this.handler.cancel(); }, null, 0.86F);
        decrease = new ButtonPanel(() -> "-", () -> quantity = Math.max(1, quantity - 1), () -> Boolean.valueOf(selected != null && quantity > 1), 0.86F);
        increase = new ButtonPanel(() -> "+", () -> quantity = Math.min(selected == null ? 1 : selected.getQuantity(), quantity + 1), () -> Boolean.valueOf(selected != null && quantity < selected.getQuantity()), 0.86F);
        addChild(confirm);
        addChild(cancel);
        addChild(decrease);
        addChild(increase);
        setBounds(new GuiRect((screenWidth - popupWidth) / 2, (screenHeight - popupHeight) / 2, popupWidth, popupHeight));
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        if (bounds == null) return;
        int buttonY = bounds.getBottom() - 25;
        int x = bounds.getRight() - 144;
        confirm.setBounds(new GuiRect(x, buttonY, 62, 17));
        cancel.setBounds(new GuiRect(x + 68, buttonY, 62, 17));
        decrease.setBounds(new GuiRect(bounds.getX() + 16, buttonY, 20, 17));
        increase.setBounds(new GuiRect(bounds.getX() + 74, buttonY, 20, 17));
    }

    @Override
    protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        GuiRect bounds = getBounds();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRenderer == null) return;
        minecraft.fontRenderer.drawStringWithShadow("个人仓选择器 (只读)", bounds.getX() + 14, bounds.getY() + 12,
            scene.getTheme().color(ThemeColorKey.TEXT_PRIMARY));
        minecraft.fontRenderer.drawStringWithShadow("选择后仅提交市场请求；不会在此窗口搬运或整理物品。",
            bounds.getX() + 14, bounds.getY() + 26, scene.getTheme().color(ThemeColorKey.TEXT_SECONDARY));

        int cell = 30;
        int gridX = bounds.getX() + 14;
        int gridY = bounds.getY() + 45;
        for (int slot = 0; slot < 27; slot++) {
            int x = gridX + (slot % 9) * (cell + 3);
            int y = gridY + (slot / 9) * (cell + 3);
            TerminalMarketSectionModel.VaultAssetModel asset = find(slot);
            boolean isSelected = asset != null && selected != null && asset.getSlotIndex() == selected.getSlotIndex();
            int border = isSelected ? 0xFF4A9DFF : 0xFF324152;
            int fill = asset == null ? 0xFF111A22 : selectable.test(asset) ? 0xFF19252F : 0xFF242326;
            Gui.drawRect(x, y, x + cell, y + cell, border);
            Gui.drawRect(x + 2, y + 2, x + cell - 2, y + cell - 2, fill);
            if (asset != null) {
                ItemStack stack = TerminalMarketVisuals.resolveItemStack(asset.getRegistryName() + "@" + asset.getMeta());
                if (!TerminalMarketClientIconRenderer.drawItemIcon(x + 6, y + 5, 18, stack)) {
                    TerminalMarketVisuals.drawFallbackItemBadge(x + 7, y + 6, 16, asset.getDisplayName());
                }
                String compactQuantity = TerminalNumberFormat.compactQuantity(asset.getQuantity());
                float quantityScale = 0.60F;
                int quantityWidth = MarketCompactText.width(minecraft.fontRenderer, compactQuantity, quantityScale);
                MarketCompactText.draw(minecraft.fontRenderer, compactQuantity,
                    Math.max(x + 2, x + cell - 3 - quantityWidth), y + 21,
                    scene.getTheme().color(ThemeColorKey.TEXT_PRIMARY), quantityScale);
            }
        }
        String selection = selected == null ? "未选择格位" : ("第 " + (selected.getSlotIndex() + 1) + " 格: "
            + selected.getDisplayName() + " | " + (selectable.test(selected) ? "可选择" : selected.getStandardizedReason()));
        minecraft.fontRenderer.drawStringWithShadow(selection, bounds.getX() + 14, bounds.getY() + 150,
            scene.getTheme().color(ThemeColorKey.TEXT_SECONDARY));
        minecraft.fontRenderer.drawStringWithShadow("数量 " + TerminalNumberFormat.exact(quantity),
            bounds.getX() + 42, bounds.getBottom() - 20,
            scene.getTheme().color(ThemeColorKey.TEXT_PRIMARY));
    }

    @Override
    protected boolean onContainerClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return false;
        GuiRect bounds = getBounds();
        int gridX = bounds.getX() + 14;
        int gridY = bounds.getY() + 45;
        for (int slot = 0; slot < 27; slot++) {
            int x = gridX + (slot % 9) * 33;
            int y = gridY + (slot / 9) * 33;
            if (mouseX >= x && mouseX < x + 30 && mouseY >= y && mouseY < y + 30) {
                TerminalMarketSectionModel.VaultAssetModel asset = find(slot);
                if (asset != null) {
                    selected = asset;
                    quantity = Math.max(1, Math.min(asset.getQuantity(), quantity));
                }
                return true;
            }
        }
        return false;
    }

    private TerminalMarketSectionModel.VaultAssetModel find(int slotIndex) {
        for (TerminalMarketSectionModel.VaultAssetModel asset : assets) {
            if (asset != null && asset.getSlotIndex() == slotIndex) return asset;
        }
        return null;
    }
}

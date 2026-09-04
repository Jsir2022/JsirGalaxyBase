package com.jsirgalaxybase.terminal.client.component;

import java.util.List;
import java.util.function.Supplier;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.client.gui.theme.ThemeTextureKey;
import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

/** Compact asset-center page. Dragging the actual Cell remains a native Container concern. */
public final class TerminalWarehouseSection extends TexturedCanvasPanel {
    private final TerminalHomeScreenModel model;
    private final TerminalHomeScreenModel.PageSnapshotModel snapshot;
    private final TerminalPanelFactory panels;
    private final LabelPanel title;
    private final LabelPanel hint;
    private final TexturedCanvasPanel vaultCard;
    private final TexturedCanvasPanel bayCard;
    private final TexturedCanvasPanel auditCard;
    private final LabelPanel vaultTitle;
    private final LabelPanel vaultSummary;
    private final LabelPanel vaultDetail;
    private final LabelPanel bayTitle;
    private final LabelPanel baySummary;
    private final LabelPanel bayDetail;
    private final LabelPanel auditTitle;
    private final LabelPanel auditSummary;
    private final LabelPanel auditDetail;
    private final ButtonPanel vaultButton;
    private final ButtonPanel bayButton;

    public TerminalWarehouseSection(TerminalPanelFactory panels, TerminalHomeScreenModel model,
        TerminalHomeScreenModel.PageSnapshotModel snapshot) {
        super(ThemeTextureKey.PANEL_BACKGROUND, ThemeColorKey.PANEL_FILL, ThemeColorKey.PANEL_BORDER);
        this.panels = panels; this.model = model; this.snapshot = snapshot;
        title = label(() -> "银河资产中心", ThemeColorKey.TEXT_PRIMARY, false, 0.92F);
        hint = label(() -> "个人 Base Vault 与 AE2 存储单元在此统一查看；存储单元不接入外部 AE 网络。", ThemeColorKey.TEXT_SECONDARY, false, 0.66F);
        vaultCard = card(); bayCard = card(); auditCard = card();
        vaultTitle = label(() -> "Base Vault", ThemeColorKey.TEXT_PRIMARY, false, 0.82F);
        vaultSummary = label(() -> section("warehouse_base_vault", 1), ThemeColorKey.TEXT_PRIMARY, false, 0.72F);
        vaultDetail = label(() -> section("warehouse_base_vault", 2), ThemeColorKey.TEXT_SECONDARY, false, 0.60F);
        bayTitle = label(() -> "AE2 存储单元  ·  1 个 Bay", ThemeColorKey.TEXT_PRIMARY, false, 0.82F);
        baySummary = label(() -> section("warehouse_bay", 1), ThemeColorKey.TEXT_PRIMARY, false, 0.72F);
        bayDetail = label(() -> section("warehouse_bay", 2), ThemeColorKey.TEXT_SECONDARY, false, 0.60F);
        auditTitle = label(() -> "最近操作", ThemeColorKey.TEXT_PRIMARY, false, 0.72F);
        auditSummary = label(() -> section("warehouse_audit_", 0), ThemeColorKey.TEXT_PRIMARY, false, 0.64F);
        auditDetail = label(() -> section("warehouse_audit_", 1), ThemeColorKey.TEXT_SECONDARY, false, 0.60F);
        vaultButton = panels.createButton(new GuiRect(0, 0, 1, 1), () -> "打开 Base Vault", () -> send(TerminalActionType.VAULT_OPEN), () -> Boolean.TRUE);
        bayButton = panels.createButton(new GuiRect(0, 0, 1, 1), () -> "插入 / 管理存储单元", () -> send(TerminalActionType.WAREHOUSE_OPEN_BAY), () -> Boolean.TRUE);
        addChild(title); addChild(hint); addChild(vaultCard); addChild(bayCard); addChild(auditCard); addChild(vaultButton); addChild(bayButton);
        vaultCard.addChild(vaultTitle); vaultCard.addChild(vaultSummary); vaultCard.addChild(vaultDetail);
        bayCard.addChild(bayTitle); bayCard.addChild(baySummary); bayCard.addChild(bayDetail);
        auditCard.addChild(auditTitle); auditCard.addChild(auditSummary); auditCard.addChild(auditDetail);
    }

    @Override public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        TerminalWarehouseSectionLayout layout = TerminalWarehouseSectionLayout.compute(bounds);
        title.setBounds(layout.title); hint.setBounds(layout.hint);
        vaultCard.setBounds(layout.vaultCard); bayCard.setBounds(layout.bayCard);
        layoutCard(vaultCard, vaultTitle, vaultSummary, vaultDetail); layoutCard(bayCard, bayTitle, baySummary, bayDetail);
        vaultButton.setBounds(layout.vaultButton); bayButton.setBounds(layout.bayButton);
        auditCard.setBounds(layout.auditCard); layoutCard(auditCard, auditTitle, auditSummary, auditDetail);
    }

    private LabelPanel label(Supplier<String> text, ThemeColorKey color, boolean centered, float scale) { return new LabelPanel(text, color, centered, scale); }
    private TexturedCanvasPanel card() { return new TexturedCanvasPanel(ThemeTextureKey.PANEL_BACKGROUND, ThemeColorKey.PANEL_FILL, ThemeColorKey.PANEL_BORDER); }
    private void layoutCard(TexturedCanvasPanel card, LabelPanel cardTitle, LabelPanel summary, LabelPanel detail) {
        GuiRect b = card.getBounds(); int x = b.getX() + 4, width = Math.max(24, b.getWidth() - 8); cardTitle.setBounds(new GuiRect(x, b.getY() + 3, width, 11)); summary.setBounds(new GuiRect(x, b.getY() + 16, width, 12)); detail.setBounds(new GuiRect(x, b.getY() + 29, width, Math.max(8, b.getHeight() - 32))); }
    private String section(String prefix, int field) {
        List<TerminalHomeScreenModel.SectionModel> sections = snapshot == null ? java.util.Collections.<TerminalHomeScreenModel.SectionModel>emptyList() : snapshot.getSections();
        for (TerminalHomeScreenModel.SectionModel section : sections) if (section.getSectionId().startsWith(prefix)) return field == 0 ? section.getTitle() : field == 1 ? section.getSummary() : section.getDetail();
        return field == 0 ? "暂无操作" : field == 1 ? "—" : "";
    }
    private void send(TerminalActionType action) { if (model != null) TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(model.getSessionToken(), TerminalPage.WAREHOUSE.getId(), action.getId(), "terminal_asset_center")); }
}

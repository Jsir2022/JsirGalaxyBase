package com.jsirgalaxybase.terminal.client.component;

import java.util.function.Supplier;

import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.client.gui.theme.ThemeTextureKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;

public class TerminalHomeSection extends TexturedCanvasPanel {

    private final TerminalHomeScreenModel.SectionModel model;
    private final LabelPanel titleLabel;
    private final LabelPanel summaryLabel;
    private final LabelPanel detailLabel;

    public TerminalHomeSection(TerminalHomeScreenModel.SectionModel model) {
        super(ThemeTextureKey.PANEL_BACKGROUND, ThemeColorKey.PANEL_FILL, ThemeColorKey.PANEL_BORDER);
        this.model = model == null ? TerminalHomeScreenModel.SectionModel.placeholder() : model;
        this.titleLabel = new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return TerminalHomeSection.this.model.getTitle();
            }
        }, ThemeColorKey.TEXT_PRIMARY, false);
        this.summaryLabel = new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return TerminalHomeSection.this.model.getSummary();
            }
        }, ThemeColorKey.TEXT_PRIMARY, false);
        this.detailLabel = new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return TerminalHomeSection.this.model.getDetail();
            }
        }, ThemeColorKey.TEXT_SECONDARY, false);
        addChild(titleLabel);
        addChild(summaryLabel);
        addChild(detailLabel);
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        GuiRect sectionBounds = getBounds();
        int innerX = sectionBounds.getX() + 10;
        int innerY = sectionBounds.getY() + 8;
        int innerWidth = Math.max(32, sectionBounds.getWidth() - 20);
        titleLabel.setBounds(new GuiRect(innerX, innerY, innerWidth, 12));
        summaryLabel.setBounds(new GuiRect(innerX, innerY + 14, innerWidth, 18));
        detailLabel.setBounds(new GuiRect(innerX, innerY + 32, innerWidth, Math.max(16, sectionBounds.getHeight() - 40)));
    }
}

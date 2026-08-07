package com.jsirgalaxybase.terminal.client.component;

import java.util.function.Supplier;

import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.client.gui.theme.ThemeTextureKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;

public class TerminalHomeSection extends TexturedCanvasPanel {

    private static final float TERMINAL_SECTION_TEXT_SCALE = 0.78F;

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
        }, ThemeColorKey.TEXT_PRIMARY, false, TERMINAL_SECTION_TEXT_SCALE);
        this.summaryLabel = new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return TerminalHomeSection.this.model.getSummary();
            }
        }, ThemeColorKey.TEXT_PRIMARY, false, TERMINAL_SECTION_TEXT_SCALE);
        this.detailLabel = new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return TerminalHomeSection.this.model.getDetail();
            }
        }, ThemeColorKey.TEXT_SECONDARY, false, TERMINAL_SECTION_TEXT_SCALE);
        addChild(titleLabel);
        addChild(summaryLabel);
        addChild(detailLabel);
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        GuiRect sectionBounds = getBounds();
        int innerX = sectionBounds.getX() + 3;
        int innerY = sectionBounds.getY() + 3;
        int innerWidth = Math.max(32, sectionBounds.getWidth() - 6);
        int titleHeight = TerminalLayoutMetrics.labelHeight(model.getTitle(), innerWidth, 1);
        int summaryHeight = TerminalLayoutMetrics.labelHeight(model.getSummary(), innerWidth, 1);
        titleLabel.setBounds(new GuiRect(innerX, innerY, innerWidth, titleHeight));
        summaryLabel.setBounds(new GuiRect(innerX, innerY + titleHeight, innerWidth, summaryHeight));
        detailLabel.setBounds(new GuiRect(innerX, innerY + titleHeight + summaryHeight, innerWidth,
            Math.max(12, sectionBounds.getBottom() - innerY - titleHeight - summaryHeight - 3)));
    }
}

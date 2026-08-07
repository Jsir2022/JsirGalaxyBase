package com.jsirgalaxybase.terminal.client.component;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.framework.VerticalScrollPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.client.gui.theme.ThemeTextureKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

public final class TerminalPanelFactory {

    private static final float TERMINAL_TEXT_SCALE = 0.78F;
    private static final float TERMINAL_BUTTON_TEXT_SCALE = 0.82F;

    public TexturedCanvasPanel createSurface(GuiRect bounds, ThemeColorKey fillColorKey) {
        TexturedCanvasPanel panel = new TexturedCanvasPanel(
            ThemeTextureKey.PANEL_BACKGROUND,
            fillColorKey,
            ThemeColorKey.PANEL_BORDER);
        panel.setBounds(bounds);
        return panel;
    }

    public LabelPanel createLabel(GuiRect bounds, Supplier<String> textSupplier, ThemeColorKey colorKey,
        boolean centered) {
        LabelPanel label = new LabelPanel(textSupplier, colorKey, centered, TERMINAL_TEXT_SCALE);
        label.setBounds(bounds);
        return label;
    }

    public ButtonPanel createButton(GuiRect bounds, Supplier<String> labelSupplier, Runnable onClick,
        Supplier<Boolean> enabledSupplier) {
        ButtonPanel button = new ButtonPanel(labelSupplier, onClick, enabledSupplier, TERMINAL_BUTTON_TEXT_SCALE);
        button.setBounds(bounds);
        return button;
    }

    public PanelContainer createBadge(GuiRect bounds, Supplier<String> labelSupplier, Supplier<String> valueSupplier) {
        TexturedCanvasPanel badge = createSurface(bounds, ThemeColorKey.PANEL_FILL);
        badge.addChild(createLabel(
            new GuiRect(bounds.getX() + 8, bounds.getY() + 6, bounds.getWidth() - 16, 10),
            labelSupplier,
            ThemeColorKey.TEXT_SECONDARY,
            false));
        badge.addChild(createLabel(
            new GuiRect(bounds.getX() + 8, bounds.getY() + 18, bounds.getWidth() - 16, bounds.getHeight() - 24),
            valueSupplier,
            ThemeColorKey.TEXT_PRIMARY,
            false));
        return badge;
    }

    public PanelContainer createNavigationItem(GuiRect bounds, TerminalHomeScreenModel.NavItemModel model,
        Runnable onClick) {
        NavigationItemPanel panel = new NavigationItemPanel(model, onClick);
        panel.setBounds(bounds);
        return panel;
    }

    public PanelContainer createNotificationCard(GuiRect bounds, TerminalHomeScreenModel.NotificationModel model) {
        NotificationCardPanel panel = new NotificationCardPanel(model);
        panel.setBounds(bounds);
        return panel;
    }

    public VerticalScrollPanel createScrollPanel(GuiRect bounds, int padding, int gap) {
        VerticalScrollPanel panel = new VerticalScrollPanel(padding, gap);
        panel.setBounds(bounds);
        return panel;
    }

    private static final class NavigationItemPanel extends PanelContainer {

        private static final ResourceLocation CLICK_SOUND = new ResourceLocation("gui.button.press");

        private final TerminalHomeScreenModel.NavItemModel model;
        private final Runnable onClick;
        private final LabelPanel titleLabel;
        private final TerminalIconKind iconKind;
        private boolean pressed;

        private NavigationItemPanel(TerminalHomeScreenModel.NavItemModel model, Runnable onClick) {
            this.model = model == null ? TerminalHomeScreenModel.NavItemModel.placeholder("home", "首页", "总览", true) : model;
            this.onClick = onClick;
            this.iconKind = TerminalIconPainter.navIconFor(this.model.getPageId(), this.model.getLabel());
            this.titleLabel = new LabelPanel(new Supplier<String>() {
                @Override
                public String get() {
                    return NavigationItemPanel.this.model.getLabel();
                }
            }, ThemeColorKey.TEXT_PRIMARY, false, 0.74F);
            addChild(titleLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            GuiRect itemBounds = getBounds();
            int innerX = itemBounds.getX() + 27;
            int innerWidth = Math.max(24, itemBounds.getWidth() - 31);
            int textHeight = Math.min(10, Math.max(8, itemBounds.getHeight() - 6));
            titleLabel.setBounds(new GuiRect(innerX, itemBounds.getY() + Math.max(2, (itemBounds.getHeight() - textHeight) / 2), innerWidth, textHeight));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            boolean hovered = contains(mouseX, mouseY) && model.isEnabled();
            int border = scene.getTheme().color(ThemeColorKey.PANEL_BORDER);
            int fill = model.isSelected() ? 0xFF20394D : hovered ? 0xFF1B3041 : 0xFF101923;
            RoundedRectPainter.draw(bounds, 0xFF243240, fill);
            if (model.isSelected()) {
                Gui.drawRect(bounds.getX(), bounds.getY() + 2, bounds.getX() + 3, bounds.getBottom() - 2,
                    scene.getTheme().color(ThemeColorKey.BUTTON_FILL_HOVER));
            } else if (hovered) {
                Gui.drawRect(bounds.getX() + 2, bounds.getY() + 1, bounds.getRight() - 2, bounds.getY() + 2, border);
            }
            int iconColor = model.isEnabled()
                ? model.isSelected() ? TerminalIconPainter.ICON_PRIMARY : TerminalIconPainter.ICON_SECONDARY
                : TerminalIconPainter.ICON_MUTED;
            int iconSize = Math.min(14, Math.max(11, bounds.getHeight() - 8));
            TerminalIconPainter.draw(iconKind, bounds.getX() + 7, bounds.getY() + Math.max(3, (bounds.getHeight() - iconSize) / 2),
                iconSize, iconColor);
        }

        @Override
        public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            if (!model.isEnabled()) {
                return false;
            }
            pressed = mouseButton == 0 && contains(mouseX, mouseY);
            return pressed;
        }

        @Override
        public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            boolean shouldClick = pressed && mouseButton == 0 && contains(mouseX, mouseY) && model.isEnabled();
            pressed = false;
            if (!shouldClick) {
                return false;
            }
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null) {
                minecraft.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(CLICK_SOUND, 1.0F));
            }
            if (onClick != null) {
                onClick.run();
            }
            return true;
        }
    }

    private static final class NotificationCardPanel extends PanelContainer {

        private final TerminalHomeScreenModel.NotificationModel model;
        private final LabelPanel titleLabel;
        private final LabelPanel bodyLabel;

        private NotificationCardPanel(TerminalHomeScreenModel.NotificationModel model) {
            this.model = model == null ? TerminalHomeScreenModel.NotificationModel.placeholder("终端通知", "当前没有通知内容。", "INFO")
                : model;
            this.titleLabel = new LabelPanel(new Supplier<String>() {
                @Override
                public String get() {
                    return NotificationCardPanel.this.model.getTitle();
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            this.bodyLabel = new LabelPanel(new Supplier<String>() {
                @Override
                public String get() {
                    return NotificationCardPanel.this.model.getBody();
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            addChild(titleLabel);
            addChild(bodyLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            GuiRect cardBounds = getBounds();
            int innerX = cardBounds.getX() + 10;
            int innerWidth = Math.max(20, cardBounds.getWidth() - 18);
            titleLabel.setBounds(new GuiRect(innerX, cardBounds.getY() + 5, innerWidth - 4, 10));
            bodyLabel.setBounds(new GuiRect(innerX, cardBounds.getY() + 16, innerWidth - 4, cardBounds.getHeight() - 20));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            TerminalNotificationSeverity severity = model.getSeverity();
            Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), 0xFF10161D);
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getRight() - 1, bounds.getBottom() - 1,
                severity.getBackgroundColor());
            Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getX() + 5, bounds.getBottom() - 1,
                severity.getAccentColor());
        }
    }
}

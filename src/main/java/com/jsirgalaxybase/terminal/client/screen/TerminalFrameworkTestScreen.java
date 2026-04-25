package com.jsirgalaxybase.terminal.client.screen;

import net.minecraft.client.gui.GuiScreen;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.CanvasScreen;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.ModalPopupPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.client.gui.theme.ThemeTextureKey;

public class TerminalFrameworkTestScreen extends CanvasScreen {

    public TerminalFrameworkTestScreen(GuiScreen parentScreen) {
        super(parentScreen);
    }

    @Override
    protected PanelContainer buildRootPanel() {
        PanelContainer root = new PanelContainer();
        root.setBounds(new GuiRect(0, 0, width, height));

        int panelWidth = Math.min(332, Math.max(240, width - 40));
        int panelHeight = Math.min(206, Math.max(160, height - 40));
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        TexturedCanvasPanel mainPanel = new TexturedCanvasPanel(
            ThemeTextureKey.PANEL_BACKGROUND,
            ThemeColorKey.PANEL_FILL,
            ThemeColorKey.PANEL_BORDER);
        mainPanel.setBounds(new GuiRect(panelX, panelY, panelWidth, panelHeight));
        root.addChild(mainPanel);

        LabelPanel title = new LabelPanel(
            new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    return "Terminal Framework Test";
                }
            },
            ThemeColorKey.TEXT_PRIMARY,
            true);
        title.setBounds(new GuiRect(panelX + 16, panelY + 14, panelWidth - 32, 14));
        mainPanel.addChild(title);

        LabelPanel description = new LabelPanel(
            new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    return "Phase 1 placeholder screen validating root screen, panel tree, themed textures, button callbacks, and popup hosting without touching the live terminal open chain.";
                }
            },
            ThemeColorKey.TEXT_SECONDARY,
            false);
        description.setBounds(new GuiRect(panelX + 16, panelY + 36, panelWidth - 32, 44));
        mainPanel.addChild(description);

        TexturedCanvasPanel swatchPanel = new TexturedCanvasPanel(
            ThemeTextureKey.PANEL_BACKGROUND,
            ThemeColorKey.PANEL_ACCENT,
            ThemeColorKey.PANEL_BORDER);
        swatchPanel.setBounds(new GuiRect(panelX + 16, panelY + 92, 104, 48));
        mainPanel.addChild(swatchPanel);

        LabelPanel swatchLabel = new LabelPanel(
            new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    return "Theme swatch";
                }
            },
            ThemeColorKey.TEXT_PRIMARY,
            true);
        swatchLabel.setBounds(new GuiRect(panelX + 22, panelY + 108, 92, 12));
        mainPanel.addChild(swatchLabel);

        LabelPanel themeInfo = new LabelPanel(
            new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    return "Theme: " + getTheme().getId() + "\nTexture path: assets/jsirgalaxybase/textures/gui/framework/*\nPopup and button states are driven by the new registry.";
                }
            },
            ThemeColorKey.TEXT_SECONDARY,
            false);
        themeInfo.setBounds(new GuiRect(panelX + 132, panelY + 92, panelWidth - 148, 48));
        mainPanel.addChild(themeInfo);

        ButtonPanel popupButton = new ButtonPanel(
            new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    return "Open Popup";
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    openFrameworkPopup();
                }
            },
            null);
        popupButton.setBounds(new GuiRect(panelX + 16, panelY + panelHeight - 40, 140, 22));
        mainPanel.addChild(popupButton);

        ButtonPanel closeButton = new ButtonPanel(
            new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    return "Close Test Screen";
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closeScreen();
                }
            },
            null);
        closeButton.setBounds(new GuiRect(panelX + panelWidth - 156, panelY + panelHeight - 40, 140, 22));
        mainPanel.addChild(closeButton);
        return root;
    }

    private void openFrameworkPopup() {
        final int popupWidth = Math.min(236, Math.max(180, width - 72));
        final int popupHeight = 116;
        final int popupX = (width - popupWidth) / 2;
        final int popupY = (height - popupHeight) / 2;

        ModalPopupPanel popupPanel = new ModalPopupPanel();
        popupPanel.setBounds(new GuiRect(popupX, popupY, popupWidth, popupHeight));

        LabelPanel popupTitle = new LabelPanel(
            new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    return "Popup Layer Active";
                }
            },
            ThemeColorKey.TEXT_PRIMARY,
            true);
        popupTitle.setBounds(new GuiRect(popupX + 12, popupY + 12, popupWidth - 24, 12));
        popupPanel.addChild(popupTitle);

        LabelPanel popupBody = new LabelPanel(
            new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    return "This popup is hosted by the new root screen, rendered on a higher layer, and dismissed without involving the old ModularUI terminal pipeline.";
                }
            },
            ThemeColorKey.TEXT_SECONDARY,
            false);
        popupBody.setBounds(new GuiRect(popupX + 12, popupY + 32, popupWidth - 24, 40));
        popupPanel.addChild(popupBody);

        ButtonPanel dismissButton = new ButtonPanel(
            new java.util.function.Supplier<String>() {
                @Override
                public String get() {
                    return "Dismiss Popup";
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            },
            null);
        dismissButton.setBounds(new GuiRect(popupX + (popupWidth - 120) / 2, popupY + popupHeight - 34, 120, 20));
        popupPanel.addChild(dismissButton);
        openPopup(popupPanel);
    }
}
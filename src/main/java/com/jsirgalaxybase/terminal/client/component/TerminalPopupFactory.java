package com.jsirgalaxybase.terminal.client.component;

import java.util.List;
import java.util.function.Supplier;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.ModalPopupPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;

public final class TerminalPopupFactory {

    private TerminalPopupFactory() {}

    public static ModalPopupPanel createInfoPopup(int screenWidth, int screenHeight, final String title,
        final String body, final String footer, Runnable closeAction) {
        int popupWidth = Math.min(320, Math.max(220, screenWidth - 84));
        int popupHeight = Math.min(176, Math.max(132, screenHeight - 120));
        int popupX = (screenWidth - popupWidth) / 2;
        int popupY = (screenHeight - popupHeight) / 2;

        ModalPopupPanel popupPanel = new ModalPopupPanel();
        popupPanel.setBounds(new GuiRect(popupX, popupY, popupWidth, popupHeight));
        popupPanel.addChild(new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return title == null ? "终端说明" : title;
            }
        }, ThemeColorKey.TEXT_PRIMARY, true));
        popupPanel.getChildren().get(0).setBounds(new GuiRect(popupX + 12, popupY + 12, popupWidth - 24, 12));

        popupPanel.addChild(new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return body == null ? "当前没有说明内容。" : body;
            }
        }, ThemeColorKey.TEXT_SECONDARY, false));
        popupPanel.getChildren().get(1).setBounds(new GuiRect(popupX + 12, popupY + 34, popupWidth - 24, popupHeight - 74));

        popupPanel.addChild(new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return footer == null ? "按 ESC 或点击关闭。" : footer;
            }
        }, ThemeColorKey.TEXT_SECONDARY, false));
        popupPanel.getChildren().get(2).setBounds(new GuiRect(popupX + 12, popupY + popupHeight - 44, popupWidth - 144, 18));

        ButtonPanel closeButton = new ButtonPanel(new Supplier<String>() {
            @Override
            public String get() {
                return "关闭说明";
            }
        }, closeAction, null);
        closeButton.setBounds(new GuiRect(popupX + popupWidth - 118, popupY + popupHeight - 34, 102, 20));
        popupPanel.addChild(closeButton);
        return popupPanel;
    }

    public static ModalPopupPanel createConfirmationPopup(int screenWidth, int screenHeight, final String title,
        final String body, List<String> detailLines, final String confirmLabel, final String cancelLabel,
        Runnable confirmAction, Runnable cancelAction) {
        int popupWidth = Math.min(360, Math.max(260, screenWidth - 84));
        int popupHeight = Math.min(220, Math.max(164, screenHeight - 108));
        int popupX = (screenWidth - popupWidth) / 2;
        int popupY = (screenHeight - popupHeight) / 2;
        StringBuilder detailBuilder = new StringBuilder();
        if (detailLines != null) {
            for (String detailLine : detailLines) {
                if (detailLine == null || detailLine.trim().isEmpty()) {
                    continue;
                }
                if (detailBuilder.length() > 0) {
                    detailBuilder.append("\n");
                }
                detailBuilder.append(detailLine.trim());
            }
        }

        ModalPopupPanel popupPanel = new ModalPopupPanel();
        popupPanel.setBounds(new GuiRect(popupX, popupY, popupWidth, popupHeight));
        popupPanel.addChild(new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return title == null ? "确认操作" : title;
            }
        }, ThemeColorKey.TEXT_PRIMARY, true));
        popupPanel.getChildren().get(0).setBounds(new GuiRect(popupX + 12, popupY + 12, popupWidth - 24, 12));
        popupPanel.addChild(new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return body == null ? "请确认当前操作。" : body;
            }
        }, ThemeColorKey.TEXT_SECONDARY, false));
        popupPanel.getChildren().get(1).setBounds(new GuiRect(popupX + 12, popupY + 34, popupWidth - 24, 34));
        popupPanel.addChild(new LabelPanel(new Supplier<String>() {
            @Override
            public String get() {
                return detailBuilder.length() == 0 ? "当前没有详情。" : detailBuilder.toString();
            }
        }, ThemeColorKey.TEXT_SECONDARY, false));
        popupPanel.getChildren().get(2).setBounds(new GuiRect(popupX + 12, popupY + 72, popupWidth - 24, popupHeight - 118));

        ButtonPanel confirmButton = new ButtonPanel(new Supplier<String>() {
            @Override
            public String get() {
                return confirmLabel == null ? "确认" : confirmLabel;
            }
        }, confirmAction, null);
        confirmButton.setBounds(new GuiRect(popupX + popupWidth - 228, popupY + popupHeight - 34, 102, 20));
        popupPanel.addChild(confirmButton);

        ButtonPanel cancelButton = new ButtonPanel(new Supplier<String>() {
            @Override
            public String get() {
                return cancelLabel == null ? "取消" : cancelLabel;
            }
        }, cancelAction, null);
        cancelButton.setBounds(new GuiRect(popupX + popupWidth - 118, popupY + popupHeight - 34, 102, 20));
        popupPanel.addChild(cancelButton);
        return popupPanel;
    }
}
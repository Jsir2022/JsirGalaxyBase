package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.ModalPopupPanel;

public class TerminalPopupFactoryTest {

    @Test
    public void confirmationPopupUsesCanvasModalLifecycle() {
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            400,
            300,
            "确认转账",
            "确认后提交真实转账。",
            Arrays.asList("目标玩家: Test", "金额: 100 STARCOIN"),
            "确认",
            "取消",
            new Runnable() {
                @Override
                public void run() {}
            },
            new Runnable() {
                @Override
                public void run() {}
            });

        assertTrue(popup instanceof ModalPopupPanel);
        assertEquals(5, popup.getChildren().size());
    }
}
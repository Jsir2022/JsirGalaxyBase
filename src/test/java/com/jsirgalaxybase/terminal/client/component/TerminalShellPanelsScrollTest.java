package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.VerticalScrollPanel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public class TerminalShellPanelsScrollTest {

    @Test
    public void navigationRailWrapsNavItemsInScrollPanel() {
        PanelContainer rail = TerminalShellPanels.createNavigationRail(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 112, 120),
            createHomeModel(10, 2, 1),
            null);

        VerticalScrollPanel scrollPanel = findFirstScrollPanel(rail);

        assertTrue(scrollPanel != null);
        assertTrue(scrollPanel.getMaxScrollOffset() > 0);
        assertTrue(scrollPanel.getBounds().getBottom() <= rail.getBounds().getBottom());
    }

    @Test
    public void plainSectionBodyUsesScrollPanelForSectionsAndNotifications() {
        PanelContainer body = TerminalShellPanels.createSectionBody(
            new TerminalPanelFactory(),
            new GuiRect(0, 0, 320, 180),
            createHomeModel(5, 7, 5),
            null,
            null,
            new TerminalBankSectionState(),
            null,
            new TerminalMarketSectionState(),
            null);

        VerticalScrollPanel scrollPanel = findFirstScrollPanel(body);

        assertTrue(scrollPanel != null);
        assertTrue(scrollPanel.getMaxScrollOffset() > 0);
        assertTrue(scrollPanel.getBounds().getBottom() <= body.getBounds().getBottom() - 24);
    }

    @Test
    public void evenSectionHeightSubtractsGapBeforeDivision() {
        int height = TerminalShellPanels.computeEvenSectionHeight(160, 4, 6, 54);

        assertEquals(35, height);
        assertTrue(TerminalShellPanels.computeStackHeight(4, height, 6) <= 160);
    }

    private static TerminalHomeScreenModel createHomeModel(int navCount, int sectionCount, int notificationCount) {
        List<TerminalHomeScreenModel.NavItemModel> navItems = new ArrayList<TerminalHomeScreenModel.NavItemModel>();
        for (int index = 0; index < navCount; index++) {
            navItems.add(new TerminalHomeScreenModel.NavItemModel(
                index == 0 ? "home" : "career",
                "入口" + index,
                "子标题 " + index,
                true,
                index == 0));
        }

        List<TerminalHomeScreenModel.SectionModel> sections = new ArrayList<TerminalHomeScreenModel.SectionModel>();
        for (int index = 0; index < sectionCount; index++) {
            sections.add(new TerminalHomeScreenModel.SectionModel(
                "section_" + index,
                "Section " + index,
                "summary " + index,
                "detail " + index));
        }

        List<TerminalHomeScreenModel.NotificationModel> notifications = new ArrayList<TerminalHomeScreenModel.NotificationModel>();
        for (int index = 0; index < notificationCount; index++) {
            notifications.add(new TerminalHomeScreenModel.NotificationModel(
                "通知 " + index,
                "这是一条较长的通知内容，用来验证普通首页正文交给滚动容器处理。",
                "INFO"));
        }

        return new TerminalHomeScreenModel(
            "home",
            "银河终端 / Test",
            "phase 10 layout",
            TerminalHomeScreenModel.StatusBandModel.placeholder(),
            navItems,
            Collections.singletonList(new TerminalHomeScreenModel.PageSnapshotModel(
                "home",
                "首页",
                "普通 section 页",
                sections)),
            notifications,
            "session-layout");
    }

    private static VerticalScrollPanel findFirstScrollPanel(PanelContainer container) {
        for (GuiPanel child : container.getChildren()) {
            if (child instanceof VerticalScrollPanel) {
                return (VerticalScrollPanel) child;
            }
            if (child instanceof PanelContainer) {
                VerticalScrollPanel nested = findFirstScrollPanel((PanelContainer) child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}

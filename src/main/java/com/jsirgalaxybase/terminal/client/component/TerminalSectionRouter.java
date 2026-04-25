package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public final class TerminalSectionRouter {

    private TerminalSectionRouter() {}

    public static String resolveSectionPageId(String selectedPageId) {
        return TerminalPage.fromId(selectedPageId).toTopLevelPageId();
    }

    public static TerminalHomeScreenModel.PageSnapshotModel resolveSnapshot(TerminalHomeScreenModel model) {
        if (model == null) {
            return TerminalHomeScreenModel.placeholder().getSelectedPageSnapshot();
        }
        return model.getPageSnapshot(resolveSectionPageId(model.getSelectedPageId()));
    }
}

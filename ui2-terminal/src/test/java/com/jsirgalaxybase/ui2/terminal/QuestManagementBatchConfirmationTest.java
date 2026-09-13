package com.jsirgalaxybase.ui2.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;

public class QuestManagementBatchConfirmationTest {
    @Test public void modalConsumesEscapeAndEnterBeforeBatchAction() {
        RecordingActions actions = new RecordingActions();
        QuestManagementVisualDocument document = new QuestManagementVisualDocument(model(), TerminalActionPort.NONE,
            actions, TerminalWindowProfile.STANDARD);
        document.selectBatchForPreview("one", "two");
        assertTrue(document.beginBatchConfirmationForPreview());
        UiRuntime runtime = new UiRuntime(document, new UiContext(GlassTerminalTheme.create(), Locale.CHINA, 1F,
            new FixedUiClock(0, true)));
        runtime.setViewport(427, 240);
        runtime.frame();
        assertTrue(runtime.hasModal());
        assertTrue(runtime.dispatch(UiEvent.key(UiKeyCode.ESCAPE)));
        runtime.frame();
        assertFalse(runtime.hasModal());
        assertTrue(document.beginBatchConfirmationForPreview());
        runtime.invalidate();
        runtime.frame();
        assertTrue(runtime.dispatch(UiEvent.key(UiKeyCode.ENTER)));
        assertTrue(actions.called);
        runtime.close();
    }

    private static QuestManagementVisualModel model() {
        TerminalVisualModel shell = new TerminalVisualModel("终端", "career", Arrays.asList(
            new TerminalVisualModel.NavItem("career", "职业", true, true)));
        List<QuestManagementVisualModel.Definition> rows = Arrays.asList(
            new QuestManagementVisualModel.Definition("one", 2, "任务一", "PUBLISHED", "h1", 1, 1, 1),
            new QuestManagementVisualModel.Definition("two", 3, "任务二", "PUBLISHED", "h2", 1, 1, 1));
        return new QuestManagementVisualModel(shell, "", "PUBLISHED", "", 0, 20, 2, rows);
    }

    private static final class RecordingActions implements QuestManagementActionPort {
        private boolean called;
        public void back() {} public void openChapters() {} public void filter(String lifecycle) {}
        public void page(int page) {} public void select(String id, int version) {} public void closeDetail() {}
        public void createDraft(String template, String name) {} public void publish(String id, int version, String hash) {}
        public void retire(String id, int version) {}
        public void batchRetire(List<String> ids, List<Integer> versions) { called = true; }
        public void saveBasics(String id, int version, String hash, String name, String description, String prerequisiteLogic, String taskLogic) {}
        public void savePrerequisites(String id, int version, String hash, List<String> original, List<String> selected) {}
        public void saveOptions(String id, int version, String hash, java.util.Map<String,String> original, java.util.Map<String,String> selected) {}
        public void saveElement(String id, int version, String hash, boolean task, String originalKey, String key, String typeId, boolean optional, java.util.Map<String,String> parameters) {}
        public void deleteElement(String id, int version, String hash, boolean task, String key) {}
        public void moveElement(String id, int version, String hash, boolean task, String key, int index) {}
    }
}

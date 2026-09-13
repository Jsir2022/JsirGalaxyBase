package com.jsirgalaxybase.ui2.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.lab.UiLabPage;
import com.jsirgalaxybase.ui2.lab.UiLabScene;
import com.jsirgalaxybase.ui2.terminal.TerminalVisualScenario;

public class UiLabDemoTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test public void everyPageFitsBothAcceptanceViewports() {
        UiLabScene scene = new UiLabScene();
        int[][] sizes = { {350, 193}, {427, 240}, {620, 340} };
        for (UiLabPage page : UiLabPage.values()) for (int[] size : sizes) {
            DrawList list = scene.render(page, size[0], size[1], true, false);
            assertTrue(page.name(), list.size() > 10);
            UiRect viewport = new UiRect(0, 0, size[0], size[1]);
            for (DrawCommand command : list.ordered()) {
                if (command.getBounds() != null) assertTrue(page + " " + command.snapshot(), viewport.contains(command.getBounds()));
            }
            String snapshot = list.snapshot();
            assertTrue(snapshot.contains("Galaxy UI 2 Lab"));
            assertFalse(snapshot.contains("\\n"));
        }
    }

    @Test public void java2dRendererProducesNonEmptyArgbImage() {
        DrawList list = new UiLabScene().render(UiLabPage.OVERVIEW, 350, 193, false, false);
        BufferedImage image = new Java2dRenderer().render(list, 350, 193);
        assertEquals(350, image.getWidth());
        assertEquals(193, image.getHeight());
        assertTrue(image.getRGB(0, 0) != 0);
        assertTrue(image.getRGB(175, 96) != image.getRGB(0, 0));
    }

    @Test public void exporterWritesPngAndStructuralSnapshotsForAllPages() throws Exception {
        File output = temporary.newFolder("ui-lab");
        UiLabExporter.exportAll(output);
        File[] pngs = output.listFiles((dir, name) -> name.endsWith(".png"));
        File[] snapshots = output.listFiles((dir, name) -> name.endsWith(".drawlist.txt"));
        File[] audits = output.listFiles((dir, name) -> name.endsWith(".ui2.txt"));
        int expected = (UiLabPage.values().length + TerminalVisualScenario.defaults().size()) * 3;
        assertEquals(expected, pngs == null ? 0 : pngs.length);
        assertEquals(expected, snapshots == null ? 0 : snapshots.length);
        assertEquals(expected, audits == null ? 0 : audits.length);
        assertTrue(new File(output, "index.html").isFile());
        assertTrue(new File(output, "terminal-home-glass-427x240.png").isFile());
        BufferedImage compact = ImageIO.read(new File(output, "inventory-350x193.png"));
        assertEquals(350, compact.getWidth());
        assertEquals(193, compact.getHeight());
    }

    @Test public void structuralCaptureContainsNodeTreeAndCleanAuditSummary() {
        String snapshot = new UiLabScene().capture(UiLabPage.OVERVIEW, 427, 240, false, false)
            .getDebugSnapshot();
        assertTrue(snapshot.contains("nodes\n"));
        assertTrue(snapshot.contains("Root|root|"));
        assertTrue(snapshot.contains("SUMMARY|issues=0"));
    }

    @Test public void reducedMotionAndDebugStateAreRepresentedDeterministically() {
        UiLabScene scene = new UiLabScene();
        String normal = scene.render(UiLabPage.CONTROLS, 350, 193, false, false).snapshot();
        String reduced = scene.render(UiLabPage.CONTROLS, 350, 193, true, true).snapshot();
        assertFalse(normal.equals(reduced));
        assertTrue(reduced.contains("减少动效"));
        assertTrue(reduced.contains("DEBUG"));
    }
}

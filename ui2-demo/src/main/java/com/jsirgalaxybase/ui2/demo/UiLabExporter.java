package com.jsirgalaxybase.ui2.demo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.debug.UiDebugSnapshot;
import com.jsirgalaxybase.ui2.lab.UiLabPage;
import com.jsirgalaxybase.ui2.lab.UiLabCapture;
import com.jsirgalaxybase.ui2.lab.UiLabScene;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.terminal.TerminalVisualScenario;

public final class UiLabExporter {
    private UiLabExporter() {}

    public static void main(String[] args) throws Exception {
        File output = new File(args.length == 0 ? "build/ui-lab" : args[0]);
        exportAll(output);
        System.out.println("Galaxy UI Lab rendered to " + output.getAbsolutePath());
    }

    public static void exportAll(File output) throws IOException {
        if (!output.exists() && !output.mkdirs()) throw new IOException("cannot create output directory: " + output);
        UiLabScene scene = new UiLabScene();
        Java2dRenderer renderer = new Java2dRenderer();
        int[][] sizes = { {350, 193}, {427, 240}, {620, 340} };
        StringBuilder report = new StringBuilder();
        report.append("<!doctype html><meta charset=\"utf-8\"><title>Galaxy UI2 visual report</title>")
            .append("<style>body{font-family:sans-serif;background:#111;color:#eee;margin:24px}")
            .append("main{display:grid;grid-template-columns:repeat(auto-fit,minmax(380px,1fr));gap:20px}")
            .append("article{background:#1b1f24;padding:12px;border-radius:8px;overflow:auto}")
            .append("img{max-width:100%;image-rendering:auto;border:1px solid #485566}</style>")
            .append("<h1>Galaxy UI2 visual report</h1><p>PNG, DrawList and component-tree evidence.</p><main>");
        for (UiLabPage page : UiLabPage.values()) for (int[] size : sizes) {
            UiLabCapture capture = scene.capture(page, size[0], size[1], false, false);
            DrawList list = capture.getDrawList();
            BufferedImage image = renderer.render(list, size[0], size[1]);
            String base = page.name().toLowerCase() + "-" + size[0] + "x" + size[1];
            ImageIO.write(image, "png", new File(output, base + ".png"));
            Files.write(new File(output, base + ".drawlist.txt").toPath(), list.snapshot().getBytes(StandardCharsets.UTF_8));
            Files.write(new File(output, base + ".ui2.txt").toPath(), capture.getDebugSnapshot().getBytes(StandardCharsets.UTF_8));
            report.append("<article><h2>").append(base).append("</h2><img src=\"").append(base)
                .append(".png\"><p><a href=\"").append(base).append(".ui2.txt\">layout audit</a> · <a href=\"")
                .append(base).append(".drawlist.txt\">DrawList</a></p></article>");
        }
        for (TerminalVisualScenario scenario : TerminalVisualScenario.defaults()) for (int[] size : sizes) {
            UiRuntime runtime = new UiRuntime(scenario.createDocument(), new UiContext(
                scenario.getTheme(), Locale.CHINA, 1F, new FixedUiClock(0, true)));
            runtime.setViewport(size[0], size[1]);
            DrawList list = runtime.frame();
            String snapshot = UiDebugSnapshot.capture(runtime.getRoot(), list, size[0], size[1]);
            String base = scenario.getId() + "-" + size[0] + "x" + size[1];
            ImageIO.write(renderer.render(list, size[0], size[1]), "png", new File(output, base + ".png"));
            Files.write(new File(output, base + ".drawlist.txt").toPath(), list.snapshot().getBytes(StandardCharsets.UTF_8));
            Files.write(new File(output, base + ".ui2.txt").toPath(), snapshot.getBytes(StandardCharsets.UTF_8));
            report.append("<article><h2>").append(base).append("</h2><img src=\"").append(base)
                .append(".png\"><p><a href=\"").append(base).append(".ui2.txt\">layout audit</a> · <a href=\"")
                .append(base).append(".drawlist.txt\">DrawList</a></p></article>");
            runtime.close();
        }
        report.append("</main>");
        Files.write(new File(output, "index.html").toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
    }
}

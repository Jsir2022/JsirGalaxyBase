package com.jsirgalaxybase.terminal.network;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jsirgalaxybase.modules.quest.application.TerminalQuestCenterSnapshotMapper;
import com.jsirgalaxybase.quest.bqimport.BqImportedQuest;
import com.jsirgalaxybase.quest.bqimport.BqMigrationManifest;
import com.jsirgalaxybase.quest.bqimport.BqMigrationPlanner;
import com.jsirgalaxybase.quest.bqimport.BqStandardRewardSpec;
import com.jsirgalaxybase.quest.bqimport.BqStandardTaskSpec;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestCenterChapterSummary;
import com.jsirgalaxybase.quest.core.QuestCenterEntry;
import com.jsirgalaxybase.quest.core.QuestCenterPage;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestRewardSummary;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot;
import com.jsirgalaxybase.terminal.client.ui2.QuestCenterVisualModelAdapter;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.debug.UiDebugSnapshot;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;
import com.jsirgalaxybase.ui2.demo.Java2dRenderer;
import com.jsirgalaxybase.ui2.terminal.QuestCenterActionPort;
import com.jsirgalaxybase.ui2.terminal.QuestCenterVisualDocument;
import com.jsirgalaxybase.ui2.terminal.QuestCenterVisualModel;
import com.jsirgalaxybase.ui2.terminal.TerminalActionPort;
import com.jsirgalaxybase.ui2.terminal.TerminalVisualModel;
import com.jsirgalaxybase.ui2.terminal.TerminalWindowProfile;
import com.jsirgalaxybase.ui2.terminal.VisualItem;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** Read-only fixed-source audit for definition -> server snapshot -> codec -> platform-neutral view. */
public final class BqQuestBookVisualCoverageMain {
    private static final UUID AUDIT_PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final TerminalVisualModel SHELL = new TerminalVisualModel("Quest audit", "career",
        Collections.<TerminalVisualModel.NavItem>emptyList());

    private BqQuestBookVisualCoverageMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException(
            "Usage: BqQuestBookVisualCoverageMain <read-only DefaultQuests directory> [gallery output directory]");
        Path source = Paths.get(args[0]);
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(source);
        if (!manifest.isApplicable()) throw new IllegalStateException(
            "source manifest contains blockers; refusing to claim visual coverage: " + manifest.getIssues());

        Map<UUID, QuestCenterEntry> entries = new HashMap<UUID, QuestCenterEntry>();
        for (BqImportedQuest imported : manifest.getQuests()) {
            QuestDefinition definition = imported.getDefinition();
            entries.put(definition.getId(), new QuestCenterEntry(definition, null, QuestRewardSummary.EMPTY,
                QuestStatus.AVAILABLE));
        }

        Totals totals = new Totals();
        VisualCandidates visualCandidates = new VisualCandidates();
        for (BqImportedQuest imported : manifest.getQuests()) {
            QuestDefinition definition = imported.getDefinition();
            QuestCenterEntry entry = entries.get(definition.getId());
            List<QuestCenterEntry> visiblePrerequisites = new ArrayList<QuestCenterEntry>();
            for (UUID id : definition.getPrerequisites()) {
                QuestCenterEntry prerequisite = entries.get(id);
                if (prerequisite == null) throw failure(definition, "unresolved prerequisite " + id);
                visiblePrerequisites.add(prerequisite);
            }
            QuestCenterPage page = new QuestCenterPage(ParticipantId.player(AUDIT_PLAYER), "source-audit",
                Collections.<QuestCenterChapterSummary>emptyList(), 0, 1, 0,
                Collections.singletonList(entry), 0, 1, 1, entry, visiblePrerequisites);
            TerminalQuestCenterSectionSnapshot mapped = new TerminalQuestCenterSnapshotMapper().map(page,
                "all", "", definition.getId().toString(), true, "", false);
            validateDefinitionMapping(imported, mapped);

            ByteBuf buffer = Unpooled.buffer();
            TerminalQuestCenterSectionSnapshot decoded;
            try {
                OpenTerminalApprovedMessage.writeQuestCenter(buffer, mapped);
                totals.maxEncodedQuestBytes = Math.max(totals.maxEncodedQuestBytes, buffer.readableBytes());
                decoded = OpenTerminalApprovedMessage.readQuestCenter(buffer);
                if (buffer.isReadable()) throw failure(definition, "quest snapshot codec left trailing bytes");
            } finally {
                buffer.release();
            }
            if (decoded == null || decoded.getDetail() == null) throw failure(definition,
                "network round-trip removed quest detail");
            validateSnapshotRoundTrip(definition, mapped, decoded);

            QuestCenterVisualModel visual = QuestCenterVisualModelAdapter.fromWire(SHELL, decoded);
            validateVisualModel(definition, decoded, visual);
            totals.add(imported, mapped, decoded, visual);
            visualCandidates.observe(imported, visual);
        }
        VisualAudit visualAudit = auditSourceVisualBoundaries(visualCandidates);
        int galleryFrames = args.length == 2
            ? exportSourceBoundaryGallery(new File(args[1]), visualCandidates) : 0;

        System.out.println("sourceHash=" + manifest.getSourceHash());
        System.out.println("quests=" + manifest.getQuests().size());
        System.out.println("chapters=" + manifest.getChapters().size());
        System.out.println("taskOccurrences=" + totals.tasks + " rewardOccurrences=" + totals.rewards);
        System.out.println("taskComponents=" + totals.taskComponents + " rewardComponents=" + totals.rewardComponents
            + " choiceOptions=" + totals.choices + " prerequisiteLinks=" + totals.prerequisites);
        System.out.println("typedIcons=" + totals.typedIcons + " iconsWithNbt=" + totals.iconsWithNbt
            + " iconNbtBytes=" + totals.iconNbtBytes + " maxDescriptionUtf8Bytes=" + totals.maxDescriptionBytes);
        System.out.println("maxPerQuestTasks=" + totals.maxTasks + " maxPerQuestRewards=" + totals.maxRewards
            + " maxPerQuestComponents=" + totals.maxComponents + " maxPerRewardChoices=" + totals.maxChoices
            + " maxQuestSnapshotBytes=" + totals.maxEncodedQuestBytes);
        System.out.println("sourceLayoutBoundaryQuests=" + visualCandidates.size()
            + " layoutFrames=" + visualAudit.frames + " pageTransitions=" + visualAudit.pageTransitions
            + " viewports=350x193,427x240,620x340");
        System.out.println("sourceBoundaryCandidates=" + visualCandidates.summary());
        if (args.length == 2) System.out.println("sourceBoundaryGallery=" + new File(args[1]).getAbsolutePath()
            + " frames=" + galleryFrames);
        System.out.println("result=PASS (read-only source; no player progress or database access)");
    }

    private static int exportSourceBoundaryGallery(File output, VisualCandidates candidates) throws Exception {
        Files.createDirectories(output.toPath());
        Java2dRenderer renderer = new Java2dRenderer();
        StringBuilder html = new StringBuilder("<!doctype html><meta charset=\"utf-8\"><title>BQ source boundary gallery</title>")
            .append("<style>body{font-family:sans-serif;background:#111;color:#eee;margin:24px}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(380px,1fr));gap:18px}article{background:#1b1f24;padding:12px;border-radius:8px}img{max-width:100%;image-rendering:auto;border:1px solid #485566}small{color:#9aa9b8}</style>")
            .append("<h1>DefaultQuests UI2 boundary gallery</h1><p>Read-only fixed-source definitions rendered through the production QuestCenterVisualDocument. Each image links to the matching layout and DrawList audit.</p><main>");
        int frames = 0;
        int[][] viewports = { {350, 193}, {427, 240}, {620, 340} };
        for (Map.Entry<String, Candidate> entry : candidates.values.entrySet()) {
            String id = entry.getValue().questId;
            String prefix = entry.getKey() + "-" + id;
            for (int[] viewport : viewports) {
                int width = viewport[0], height = viewport[1];
                String size = width + "x" + height;
                QuestCenterVisualModel model = entry.getValue().model;

                UiRuntime taskRuntime = auditRuntime(model, width, height);
                List<QuestCenterVisualModel.TaskRow> tasks = model.getDetail().getTasks();
                for (int i = 1; i < tasks.size(); i++) click(prefix, taskRuntime, "quest-task-next");
                if (!tasks.isEmpty()) {
                    QuestCenterVisualModel.TaskRow task = tasks.get(tasks.size() - 1);
                    int pages = pageCount(task.getComponents().size(), detailPageSize(height));
                    for (int page = 1; page < pages; page++)
                        click(prefix, taskRuntime, "quest-task-components-pager-next");
                    if (!task.getComponents().isEmpty()) requireNode(prefix, taskRuntime,
                        "quest-task-components-label-" + task.getComponents().get(task.getComponents().size() - 1).getSourceIndex());
                }
                frames += exportGalleryFrame(output, html, renderer, prefix + "-task-last-" + size,
                    entry.getKey() + " · last task/component · " + size, taskRuntime, width, height);
                taskRuntime.close();

                UiRuntime rewardRuntime = auditRuntime(model, width, height);
                click(prefix, rewardRuntime, "quest-detail-tab-1");
                List<QuestCenterVisualModel.RewardRow> rewards = model.getDetail().getRewards();
                for (int i = 1; i < rewards.size(); i++) click(prefix, rewardRuntime, "quest-reward-next");
                if (!rewards.isEmpty()) {
                    QuestCenterVisualModel.RewardRow reward = rewards.get(rewards.size() - 1);
                    int pages = pageCount(reward.getComponents().size(), detailPageSize(height));
                    for (int page = 1; reward.getChoices().isEmpty() && page < pages; page++)
                        click(prefix, rewardRuntime, "quest-reward-components-pager-next");
                    if (!reward.getComponents().isEmpty() && reward.getChoices().isEmpty())
                        requireNode(prefix, rewardRuntime, "quest-reward-components-label-"
                            + reward.getComponents().get(reward.getComponents().size() - 1).getSourceIndex());
                }
                frames += exportGalleryFrame(output, html, renderer, prefix + "-reward-last-" + size,
                    entry.getKey() + " · last reward · " + size, rewardRuntime, width, height);
                rewardRuntime.close();

                int choiceRewardIndex = choiceRewardIndex(rewards);
                if (choiceRewardIndex >= 0) {
                    UiRuntime choiceRuntime = auditRuntime(model, width, height);
                    click(prefix, choiceRuntime, "quest-detail-tab-1");
                    for (int i = 0; i < choiceRewardIndex; i++) click(prefix, choiceRuntime, "quest-reward-next");
                    QuestCenterVisualModel.RewardRow choiceReward = rewards.get(choiceRewardIndex);
                    int pages = pageCount(choiceReward.getChoices().size(), detailPageSize(height));
                    for (int page = 1; page < pages; page++) click(prefix, choiceRuntime, "quest-choice-pager-next");
                    int sourceIndex = choiceReward.getChoices().get(choiceReward.getChoices().size() - 1).getSourceIndex();
                    requireNode(prefix, choiceRuntime, "quest-choice-" + choiceRewardIndex + "-" + sourceIndex);
                    frames += exportGalleryFrame(output, html, renderer, prefix + "-choice-last-" + size,
                        entry.getKey() + " · last Choice (source index " + sourceIndex + ") · " + size,
                        choiceRuntime, width, height);
                    choiceRuntime.close();
                }

                UiRuntime prerequisiteRuntime = auditRuntime(model, width, height);
                click(prefix, prerequisiteRuntime, "quest-detail-tab-2");
                List<QuestCenterVisualModel.PrerequisiteRow> prerequisites = model.getDetail().getPrerequisites();
                int prerequisitePages = pageCount(prerequisites.size(), 3);
                for (int page = 1; page < prerequisitePages; page++)
                    click(prefix, prerequisiteRuntime, "quest-prerequisite-pager-next");
                if (!prerequisites.isEmpty()) requireNode(prefix, prerequisiteRuntime,
                    "quest-prerequisite-" + (prerequisites.size() - 1));
                frames += exportGalleryFrame(output, html, renderer, prefix + "-prerequisite-last-" + size,
                    entry.getKey() + " · last visible prerequisite · " + size,
                    prerequisiteRuntime, width, height);
                prerequisiteRuntime.close();

                UiRuntime descriptionRuntime = auditRuntime(model, width, height);
                click(prefix, descriptionRuntime, "quest-detail-tab-3");
                int descriptionLines = parseTrailingCount(text(descriptionRuntime, "quest-description-title"));
                int descriptionPages = pageCount(descriptionLines, height <= 220 ? 7 : 12);
                for (int page = 1; page < descriptionPages; page++)
                    click(prefix, descriptionRuntime, "quest-description-pager-next");
                if (descriptionLines > 0 && text(descriptionRuntime, "quest-description-text").isEmpty())
                    throw new IllegalStateException("visual gallery " + prefix + " has an empty final description page");
                frames += exportGalleryFrame(output, html, renderer, prefix + "-description-last-" + size,
                    entry.getKey() + " · last description page · " + size, descriptionRuntime, width, height);
                descriptionRuntime.close();
            }
        }
        html.append("</main>");
        Files.write(new File(output, "index.html").toPath(), html.toString().getBytes(StandardCharsets.UTF_8));
        if (frames == 0) throw new IllegalStateException("source-boundary gallery produced no frames");
        return frames;
    }

    private static UiRuntime auditRuntime(QuestCenterVisualModel model, int width, int height) {
        UiRuntime runtime = new UiRuntime(new QuestCenterVisualDocument(model,
            TerminalActionPort.NONE, QuestCenterActionPort.NONE, TerminalWindowProfile.STANDARD),
            new UiContext(GlassTerminalTheme.create(), java.util.Locale.CHINA, 1F,
                new FixedUiClock(0, false)));
        runtime.setViewport(width, height);
        runtime.frame();
        return runtime;
    }

    private static int exportGalleryFrame(File output, StringBuilder html, Java2dRenderer renderer,
        String base, String title, UiRuntime runtime, int width, int height) throws Exception {
        DrawList drawList = runtime.frame();
        String audit = UiDebugSnapshot.capture(runtime.getRoot(), drawList, width, height);
        if (!audit.contains("SUMMARY|issues=0|"))
            throw new IllegalStateException("visual gallery frame failed layout audit: " + base);
        requireNodeBounds(base, runtime.getRoot(), new UiRect(0, 0, width, height));
        BufferedImage image = renderer.render(drawList, width, height);
        if (image.getWidth() != width || image.getHeight() != height || !ImageIO.write(image, "png",
            new File(output, base + ".png"))) throw new IllegalStateException("could not export source gallery PNG: " + base);
        Files.write(new File(output, base + ".drawlist.txt").toPath(),
            drawList.snapshot().getBytes(StandardCharsets.UTF_8));
        Files.write(new File(output, base + ".ui2.txt").toPath(), audit.getBytes(StandardCharsets.UTF_8));
        html.append("<article><h2>").append(title).append("</h2><img src=\"").append(base)
            .append(".png\"><p><small><a href=\"").append(base).append(".ui2.txt\">layout audit</a> · <a href=\"")
            .append(base).append(".drawlist.txt\">DrawList</a></small></p></article>");
        return 1;
    }

    private static int choiceRewardIndex(List<QuestCenterVisualModel.RewardRow> rewards) {
        int selected = -1, mostChoices = 0;
        for (int index = 0; index < rewards.size(); index++) {
            int count = rewards.get(index).getChoices().size();
            if (count > mostChoices) { selected = index; mostChoices = count; }
        }
        return selected;
    }

    private static VisualAudit auditSourceVisualBoundaries(VisualCandidates candidates) {
        VisualAudit result = new VisualAudit();
        int[][] viewports = { { 350, 193 }, { 427, 240 }, { 620, 340 } };
        for (Map.Entry<String, Candidate> candidate : candidates.values.entrySet()) {
            String label = candidate.getKey() + "[" + candidate.getValue().questId + "="
                + candidate.getValue().metric + "]";
            for (int[] viewport : viewports) {
                QuestCenterVisualModel model = candidate.getValue().model;
                QuestCenterVisualDocument document = new QuestCenterVisualDocument(model,
                    TerminalActionPort.NONE, QuestCenterActionPort.NONE, TerminalWindowProfile.STANDARD);
                UiRuntime runtime = new UiRuntime(document,
                    new UiContext(GlassTerminalTheme.create(), java.util.Locale.CHINA, 1F,
                        new FixedUiClock(0, false)));
                try {
                    runtime.setViewport(viewport[0], viewport[1]);
                    auditFrame(label, runtime, viewport[0], viewport[1]); result.frames++;
                    auditTasks(label, runtime, model, viewport[0], viewport[1], result);
                    click(label, runtime, "quest-detail-tab-1");
                    auditFrame(label, runtime, viewport[0], viewport[1]); result.frames++;
                    auditRewards(label, runtime, model, viewport[0], viewport[1], result);
                    click(label, runtime, "quest-detail-tab-2");
                    auditFrame(label, runtime, viewport[0], viewport[1]); result.frames++;
                    auditPrerequisites(label, runtime, model, viewport[0], viewport[1], result);
                    click(label, runtime, "quest-detail-tab-3");
                    auditDescription(label, runtime, model, viewport[0], viewport[1], result);
                } finally {
                    runtime.close();
                }
            }
        }
        return result;
    }

    private static void auditTasks(String name, UiRuntime runtime, QuestCenterVisualModel model,
        int width, int height, VisualAudit totals) {
        List<QuestCenterVisualModel.TaskRow> tasks = model.getDetail().getTasks();
        for (int taskIndex = 0; taskIndex < tasks.size(); taskIndex++) {
            if (taskIndex > 0) click(name, runtime, "quest-task-next");
            requirePage(name, runtime, "quest-task-page", taskIndex + 1, tasks.size());
            QuestCenterVisualModel.TaskRow task = tasks.get(taskIndex);
            auditComponentPages(name, runtime, task.getComponents(), "quest-task-components", height, width, totals);
        }
    }

    private static void auditRewards(String name, UiRuntime runtime, QuestCenterVisualModel model,
        int width, int height, VisualAudit totals) {
        List<QuestCenterVisualModel.RewardRow> rewards = model.getDetail().getRewards();
        for (int rewardIndex = 0; rewardIndex < rewards.size(); rewardIndex++) {
            if (rewardIndex > 0) click(name, runtime, "quest-reward-next");
            requirePage(name, runtime, "quest-reward-page", rewardIndex + 1, rewards.size());
            QuestCenterVisualModel.RewardRow reward = rewards.get(rewardIndex);
            if (reward.getChoices().isEmpty()) {
                auditComponentPages(name, runtime, reward.getComponents(), "quest-reward-components", height, width, totals);
                continue;
            }
            int size = detailPageSize(height), pages = pageCount(reward.getChoices().size(), size);
            for (int page = 0; page < pages; page++) {
                if (page > 0) click(name, runtime, "quest-choice-pager-next");
                int from = page * size, to = Math.min(reward.getChoices().size(), from + size);
                for (int i = from; i < to; i++) {
                    int sourceIndex = reward.getChoices().get(i).getSourceIndex();
                    requireNode(name, runtime, "quest-choice-" + rewardIndex + "-" + sourceIndex);
                }
                if (page > 0) totals.pageTransitions++;
                auditFrame(name, runtime, width, height); totals.frames++;
            }
        }
    }

    private static void auditComponentPages(String name, UiRuntime runtime,
        List<QuestCenterVisualModel.ComponentRow> components, String key, int height, int width,
        VisualAudit totals) {
        if (components.isEmpty()) return;
        int size = detailPageSize(height), pages = pageCount(components.size(), size);
        for (int page = 0; page < pages; page++) {
            if (page > 0) click(name, runtime, key + "-pager-next");
            int from = page * size, to = Math.min(components.size(), from + size);
            for (int i = from; i < to; i++)
                requireNode(name, runtime, key + "-label-" + components.get(i).getSourceIndex());
            if (page > 0) totals.pageTransitions++;
            auditFrame(name, runtime, width, height); totals.frames++;
        }
    }

    private static void auditPrerequisites(String name, UiRuntime runtime, QuestCenterVisualModel model,
        int width, int height, VisualAudit totals) {
        List<QuestCenterVisualModel.PrerequisiteRow> rows = model.getDetail().getPrerequisites();
        int size = 3, pages = pageCount(rows.size(), size);
        for (int page = 0; page < pages; page++) {
            if (page > 0) click(name, runtime, "quest-prerequisite-pager-next");
            int from = page * size, to = Math.min(rows.size(), from + size);
            for (int i = from; i < to; i++) requireNode(name, runtime, "quest-prerequisite-" + i);
            if (page > 0) totals.pageTransitions++;
            auditFrame(name, runtime, width, height); totals.frames++;
        }
    }

    private static void auditDescription(String name, UiRuntime runtime, QuestCenterVisualModel model,
        int width, int height, VisualAudit totals) {
        String title = text(runtime, "quest-description-title");
        int lines = parseTrailingCount(title);
        int size = height <= 220 ? 7 : 12, pages = pageCount(lines, size);
        for (int page = 0; page < pages; page++) {
            if (page > 0) click(name, runtime, "quest-description-pager-next");
            requireNode(name, runtime, "quest-description-text");
            if (page > 0) totals.pageTransitions++;
            auditFrame(name, runtime, width, height); totals.frames++;
        }
    }

    private static void requirePage(String name, UiRuntime runtime, String key, int page, int pages) {
        String value = text(runtime, key);
        if (!(page + " / " + pages).equals(value)) throw new IllegalStateException(
            "visual audit " + name + " expected " + key + "=" + page + " / " + pages + " but got " + value);
    }

    private static int parseTrailingCount(String value) {
        int unit = value.lastIndexOf(" 行");
        int separator = unit < 0 ? -1 : value.lastIndexOf(' ', unit - 1);
        try { return Integer.parseInt(value.substring(separator + 1, unit)); }
        catch (RuntimeException invalid) { throw new IllegalStateException("invalid description page summary: " + value); }
    }

    private static String text(UiRuntime runtime, String key) {
        UiNode node = find(runtime.getRoot(), key);
        if (node == null) return "";
        Object value = node.getElement().getProp("text");
        return value == null ? "" : String.valueOf(value);
    }

    private static UiNode find(UiNode node, String key) {
        if (node == null) return null;
        if (node.getElement().getKey() != null && key.equals(node.getElement().getKey().getValue())) return node;
        for (UiNode child : node.getChildren()) {
            UiNode found = find(child, key);
            if (found != null) return found;
        }
        return null;
    }

    private static void requireNode(String name, UiRuntime runtime, String key) {
        if (find(runtime.getRoot(), key) == null) throw new IllegalStateException(
            "visual audit " + name + " cannot reach " + key);
    }

    private static void click(String name, UiRuntime runtime, String key) {
        UiNode node = find(runtime.getRoot(), key);
        if (node == null) throw new IllegalStateException("visual audit " + name + " missing control " + key);
        UiRect bounds = node.getBounds();
        int x = bounds.getX() + Math.max(0, bounds.getWidth() / 2);
        int y = bounds.getY() + Math.max(0, bounds.getHeight() / 2);
        if (!runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, x, y, 0)))
            throw new IllegalStateException("visual audit " + name + " could not activate " + key + " at " + bounds);
        runtime.frame();
    }

    private static void auditFrame(String name, UiRuntime runtime, int width, int height) {
        String report = UiDebugSnapshot.capture(runtime.getRoot(), runtime.frame(), width, height);
        int start = report.indexOf("SUMMARY|issues=");
        int end = start < 0 ? -1 : report.indexOf('\n', start);
        String summary = start < 0 ? "" : report.substring(start, end < 0 ? report.length() : end);
        if (!summary.startsWith("SUMMARY|issues=0|")) throw new IllegalStateException(
            "visual audit " + name + " has layout/render warnings: " + summary);
        requireNodeBounds(name, runtime.getRoot(), new UiRect(0, 0, width, height));
    }

    private static void requireNodeBounds(String name, UiNode node, UiRect viewport) {
        if (node == null) return;
        if (!viewport.contains(node.getBounds())) throw new IllegalStateException(
            "visual audit " + name + " node outside viewport: " + node.getElement().getKey() + " " + node.getBounds());
        for (UiNode child : node.getChildren()) requireNodeBounds(name, child, viewport);
    }

    private static int detailPageSize(int height) { return height <= 220 ? 1 : height <= 280 ? 2 : 4; }
    private static int pageCount(int count, int size) { return Math.max(1, (count + size - 1) / size); }

    private static void validateDefinitionMapping(BqImportedQuest imported,
        TerminalQuestCenterSectionSnapshot snapshot) {
        QuestDefinition definition = imported.getDefinition();
        TerminalQuestCenterSectionSnapshot.Detail detail = snapshot.getDetail();
        if (detail == null || !definition.getId().toString().equals(detail.getSummary().getId()))
            throw failure(definition, "mapper lost the selected quest identity/detail");
        if (!definition.getDescription().equals(detail.getDescription()))
            throw failure(definition, "mapper changed the quest description");
        if (detail.getTasks().size() != definition.getTasks().size())
            throw failure(definition, "task occurrence count changed in mapper");
        if (detail.getRewards().size() != definition.getRewards().size())
            throw failure(definition, "reward occurrence count changed in mapper");

        Map<String, BqStandardTaskSpec> taskSpecs = new HashMap<String, BqStandardTaskSpec>();
        for (BqStandardTaskSpec spec : imported.getTaskSpecs()) taskSpecs.put(spec.getKey(), spec);
        for (int i = 0; i < definition.getTasks().size(); i++) {
            TaskDefinition task = definition.getTasks().get(i);
            TerminalQuestCenterSectionSnapshot.Task row = detail.getTasks().get(i);
            if (!task.getKey().equals(row.getKey()) || !task.getTypeId().equals(row.getTypeId()))
                throw failure(definition, "task identity/order changed at index " + i);
            BqStandardTaskSpec spec = taskSpecs.get(task.getKey());
            if (spec == null) throw failure(definition, "source task spec missing for " + task.getKey());
            if (task.getTypeId().endsWith(":retrieval") || task.getTypeId().endsWith(":optional_retrieval")
                || task.getTypeId().endsWith(":crafting")) {
                validateItemVector(definition, "task " + task.getKey(), spec.getItems(), row.getComponents(), "item");
            } else if (task.getTypeId().endsWith(":fluid")) {
                if (spec.getFluids().size() != row.getComponents().size())
                    throw failure(definition, "fluid component count changed for " + task.getKey());
                for (int j = 0; j < spec.getFluids().size(); j++) {
                    if (!spec.getFluids().get(j).getFluidName().equals(row.getComponents().get(j).getIdentity())
                        || spec.getFluids().get(j).getAmount() != row.getComponents().get(j).getQuantity())
                        throw failure(definition, "fluid identity/amount changed for " + task.getKey() + "[" + j + "]");
                }
            } else if (task.getTypeId().endsWith(":block_break")) {
                if (spec.getBlocks().size() != row.getComponents().size())
                    throw failure(definition, "block component count changed for " + task.getKey());
                for (int j = 0; j < spec.getBlocks().size(); j++) {
                    if (!spec.getBlocks().get(j).getRegistryName().equals(row.getComponents().get(j).getIdentity())
                        || spec.getBlocks().get(j).getMeta() != row.getComponents().get(j).getMetadata()
                        || spec.getBlocks().get(j).getAmount() != row.getComponents().get(j).getQuantity())
                        throw failure(definition, "block identity/meta/amount changed for " + task.getKey() + "[" + j + "]");
                }
            }
        }

        Map<String, BqStandardRewardSpec> rewardSpecs = new HashMap<String, BqStandardRewardSpec>();
        for (BqStandardRewardSpec spec : imported.getRewardSpecs()) rewardSpecs.put(spec.getKey(), spec);
        for (int i = 0; i < definition.getRewards().size(); i++) {
            RewardDefinition reward = definition.getRewards().get(i);
            TerminalQuestCenterSectionSnapshot.Reward row = detail.getRewards().get(i);
            if (!reward.getKey().equals(row.getKey()) || !reward.getTypeId().equals(row.getTypeId()))
                throw failure(definition, "reward identity/order changed at index " + i);
            BqStandardRewardSpec spec = rewardSpecs.get(reward.getKey());
            if (spec == null) throw failure(definition, "source reward spec missing for " + reward.getKey());
            if ("bq_standard:item".equals(reward.getTypeId()))
                validateItemVector(definition, "reward " + reward.getKey(), spec.getItems(), row.getComponents(), "item");
            if ("bq_standard:choice".equals(reward.getTypeId())) {
                if (spec.getItems().size() != row.getChoices().size())
                    throw failure(definition, "Choice count changed for " + reward.getKey());
                for (int j = 0; j < row.getChoices().size(); j++) {
                    TerminalQuestCenterSectionSnapshot.Choice choice = row.getChoices().get(j);
                    if (choice.getSourceIndex() != j) throw failure(definition,
                        "Choice original slot changed for " + reward.getKey() + "[" + j + "]");
                    if (!spec.getItems().get(j).getRegistryName().equals(choice.getItemRegistry())
                        || spec.getItems().get(j).getDamage() != choice.getItemMeta()
                        || spec.getItems().get(j).getCount() != choice.getQuantity())
                        throw failure(definition, "Choice identity/meta/count changed for " + reward.getKey() + "[" + j + "]");
                }
            }
        }

        String iconText = definition.getBehavior().getIconReference();
        if (iconText != null && iconText.trim().startsWith("{")) {
            JsonObject icon = new JsonParser().parse(iconText).getAsJsonObject();
            JsonElement rawId = icon.get("id:8");
            if (rawId != null && !rawId.isJsonNull() && !rawId.getAsString().trim().isEmpty()) {
                String expectedId = rawId.getAsString();
                int expectedMeta = icon.has("Damage:2") ? icon.get("Damage:2").getAsInt() : 0;
                if (!expectedId.equals(detail.getSummary().getIconRegistry())
                    || expectedMeta != detail.getSummary().getIconMeta())
                    throw failure(definition, "typed quest icon identity/meta changed");
                JsonElement tag = icon.get("tag:10");
                if (tag != null && !tag.isJsonNull() && !detail.getSummary().getIconNbt().equals(tag.toString()))
                    throw failure(definition, "typed quest icon NBT changed");
            }
        }
    }

    private static void validateItemVector(QuestDefinition definition, String label,
        List<com.jsirgalaxybase.quest.bqimport.BqItemStackSpec> source,
        List<TerminalQuestCenterSectionSnapshot.Component> mapped, String kind) {
        if (source.size() != mapped.size()) throw failure(definition, label + " component count changed");
        for (int i = 0; i < source.size(); i++) {
            com.jsirgalaxybase.quest.bqimport.BqItemStackSpec expected = source.get(i);
            TerminalQuestCenterSectionSnapshot.Component actual = mapped.get(i);
            if (actual.getSourceIndex() != i || !kind.equals(actual.getKind())
                || !expected.getRegistryName().equals(actual.getIdentity())
                || expected.getDamage() != actual.getMetadata() || expected.getCount() != actual.getQuantity())
                throw failure(definition, label + " identity/meta/count changed at component " + i);
        }
    }

    private static void validateSnapshotRoundTrip(QuestDefinition definition,
        TerminalQuestCenterSectionSnapshot before, TerminalQuestCenterSectionSnapshot after) {
        TerminalQuestCenterSectionSnapshot.Detail a = before.getDetail(), b = after.getDetail();
        if (!a.getDescription().equals(b.getDescription()) || a.getTasks().size() != b.getTasks().size()
            || a.getRewards().size() != b.getRewards().size()
            || a.getPrerequisites().size() != b.getPrerequisites().size())
            throw failure(definition, "detail fields changed during network round-trip");
        for (int i = 0; i < a.getTasks().size(); i++) {
            TerminalQuestCenterSectionSnapshot.Task x = a.getTasks().get(i), y = b.getTasks().get(i);
            if (!x.getKey().equals(y.getKey()) || !x.getTypeId().equals(y.getTypeId())
                || x.getComponents().size() != y.getComponents().size())
                throw failure(definition, "task identity/vector changed during network round-trip at " + i);
        }
        for (int i = 0; i < a.getRewards().size(); i++) {
            TerminalQuestCenterSectionSnapshot.Reward x = a.getRewards().get(i), y = b.getRewards().get(i);
            if (!x.getKey().equals(y.getKey()) || x.getChoices().size() != y.getChoices().size()
                || x.getComponents().size() != y.getComponents().size())
                throw failure(definition, "reward vector changed during network round-trip at " + i);
            for (int j = 0; j < x.getChoices().size(); j++)
                if (x.getChoices().get(j).getSourceIndex() != y.getChoices().get(j).getSourceIndex())
                    throw failure(definition, "Choice slot changed during network round-trip");
        }
        for (int i = 0; i < a.getPrerequisites().size(); i++)
            if (!a.getPrerequisites().get(i).getId().equals(b.getPrerequisites().get(i).getId()))
                throw failure(definition, "prerequisite identity changed during network round-trip");
        if (!a.getSummary().getIconRegistry().equals(b.getSummary().getIconRegistry())
            || a.getSummary().getIconMeta() != b.getSummary().getIconMeta()
            || !a.getSummary().getIconNbt().equals(b.getSummary().getIconNbt()))
            throw failure(definition, "quest icon changed during network round-trip");
    }

    private static void validateVisualModel(QuestDefinition definition,
        TerminalQuestCenterSectionSnapshot wire, QuestCenterVisualModel visual) {
        QuestCenterVisualModel.QuestDetail detail = visual.getDetail();
        if (detail == null || detail.getTasks().size() != wire.getDetail().getTasks().size()
            || detail.getRewards().size() != wire.getDetail().getRewards().size()
            || !definition.getDescription().equals(detail.getDescription()))
            throw failure(definition, "platform-neutral model changed description/task/reward counts");
        for (int i = 0; i < detail.getTasks().size(); i++) {
            TerminalQuestCenterSectionSnapshot.Task source = wire.getDetail().getTasks().get(i);
            QuestCenterVisualModel.TaskRow row = detail.getTasks().get(i);
            if (!source.getKey().equals(row.getKey()) || !source.getTypeId().equals(row.getTypeId())
                || source.getComponents().size() != row.getComponents().size())
                throw failure(definition, "visual task mapping changed at " + i);
        }
        for (int i = 0; i < detail.getRewards().size(); i++) {
            TerminalQuestCenterSectionSnapshot.Reward source = wire.getDetail().getRewards().get(i);
            QuestCenterVisualModel.RewardRow row = detail.getRewards().get(i);
            if (!source.getKey().equals(row.getKey()) || source.getTypeId().equals(row.getTypeId()) == false
                || source.getComponents().size() != row.getComponents().size()
                || source.getChoices().size() != row.getChoices().size())
                throw failure(definition, "visual reward/Choice mapping changed at " + i);
            for (int j = 0; j < row.getChoices().size(); j++)
                if (source.getChoices().get(j).getSourceIndex() != row.getChoices().get(j).getSourceIndex())
                    throw failure(definition, "visual Choice index changed at " + i + "/" + j);
        }
        TerminalQuestCenterSectionSnapshot.Quest source = wire.getDetail().getSummary();
        VisualItem icon = detail.getSummary().getIcon();
        if (!source.getIconRegistry().isEmpty() && (icon == null
            || !source.getIconRegistry().equals(icon.getRegistryName())
            || source.getIconMeta() != icon.getMetadata()))
            throw failure(definition, "platform-neutral model lost quest icon identity/meta");
    }

    private static IllegalStateException failure(QuestDefinition definition, String message) {
        return new IllegalStateException("quest " + definition.getId() + " (" + definition.getName() + "): " + message);
    }

    private static final class Totals {
        private long tasks, rewards, taskComponents, rewardComponents, choices, prerequisites, typedIcons,
            iconsWithNbt, iconNbtBytes;
        private int maxTasks, maxRewards, maxComponents, maxChoices, maxDescriptionBytes, maxEncodedQuestBytes;

        private void add(BqImportedQuest imported, TerminalQuestCenterSectionSnapshot mapped,
            TerminalQuestCenterSectionSnapshot decoded, QuestCenterVisualModel visual) {
            QuestDefinition definition = imported.getDefinition();
            TerminalQuestCenterSectionSnapshot.Detail detail = decoded.getDetail();
            tasks += detail.getTasks().size(); rewards += detail.getRewards().size();
            prerequisites += detail.getPrerequisites().size();
            int components = 0, choicesForQuest = 0;
            for (TerminalQuestCenterSectionSnapshot.Task task : detail.getTasks()) {
                taskComponents += task.getComponents().size(); components += task.getComponents().size();
            }
            for (TerminalQuestCenterSectionSnapshot.Reward reward : detail.getRewards()) {
                rewardComponents += reward.getComponents().size(); components += reward.getComponents().size();
                choices += reward.getChoices().size(); choicesForQuest += reward.getChoices().size();
            }
            maxTasks = Math.max(maxTasks, detail.getTasks().size());
            maxRewards = Math.max(maxRewards, detail.getRewards().size());
            maxComponents = Math.max(maxComponents, components);
            maxChoices = Math.max(maxChoices, choicesForQuest);
            maxDescriptionBytes = Math.max(maxDescriptionBytes,
                definition.getDescription().getBytes(StandardCharsets.UTF_8).length);
            String icon = definition.getBehavior().getIconReference();
            if (icon != null && icon.trim().startsWith("{")) typedIcons++;
            if (!detail.getSummary().getIconNbt().isEmpty()) {
                iconsWithNbt++;
                iconNbtBytes += detail.getSummary().getIconNbt().getBytes(StandardCharsets.UTF_8).length;
            }
            if (mapped.getDetail() == null || visual.getDetail() == null)
                throw failure(definition, "unexpected missing audit detail");
        }
    }

    private static final class VisualCandidates {
        private final Map<String, Candidate> values = new LinkedHashMap<String, Candidate>();

        private void observe(BqImportedQuest imported, QuestCenterVisualModel model) {
            QuestDefinition definition = imported.getDefinition();
            QuestCenterVisualModel.QuestDetail detail = model.getDetail();
            int tasks = detail.getTasks().size(), rewards = detail.getRewards().size();
            int choices = 0, components = 0;
            for (QuestCenterVisualModel.TaskRow task : detail.getTasks()) components += task.getComponents().size();
            for (QuestCenterVisualModel.RewardRow reward : detail.getRewards()) {
                choices += reward.getChoices().size();
                components += reward.getComponents().size();
            }
            int descriptionBytes = definition.getDescription().getBytes(StandardCharsets.UTF_8).length;
            int prerequisites = detail.getPrerequisites().size();
            record("max-tasks", tasks, definition, model);
            record("max-rewards", rewards, definition, model);
            record("max-choice-options", choices, definition, model);
            record("max-component-vector", components, definition, model);
            record("max-description-bytes", descriptionBytes, definition, model);
            record("max-prerequisites", prerequisites, definition, model);
        }

        private void record(String key, int value, QuestDefinition definition, QuestCenterVisualModel model) {
            Candidate previous = values.get(key);
            if (previous == null || value > previous.metric)
                values.put(key, new Candidate(definition.getId().toString(), value, model));
        }

        private int size() { return values.size(); }
        private String summary() {
            StringBuilder result = new StringBuilder();
            for (Map.Entry<String, Candidate> entry : values.entrySet()) {
                if (result.length() > 0) result.append(',');
                result.append(entry.getKey()).append(':').append(entry.getValue().questId)
                    .append('=').append(entry.getValue().metric);
            }
            return result.toString();
        }
    }

    private static final class Candidate {
        private final String questId;
        private final int metric;
        private final QuestCenterVisualModel model;
        private Candidate(String questId, int metric, QuestCenterVisualModel model) {
            this.questId = questId; this.metric = metric; this.model = model;
        }
    }

    private static final class VisualAudit {
        private int frames, pageTransitions;
    }
}

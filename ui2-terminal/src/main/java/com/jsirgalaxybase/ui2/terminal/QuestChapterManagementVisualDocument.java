package com.jsirgalaxybase.ui2.terminal;

import com.jsirgalaxybase.ui2.component.ComponentUiDocument;
import com.jsirgalaxybase.ui2.component.StandardWidgets;
import com.jsirgalaxybase.ui2.component.UiActionHandler;
import com.jsirgalaxybase.ui2.component.UiWidgetAdapter;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.render.UiLayer;
import com.jsirgalaxybase.ui2.text.TextStyle;

/** Production chapter list/detail and bounded spatial placement editor. */
public final class QuestChapterManagementVisualDocument extends ComponentUiDocument {
    private QuestChapterManagementVisualModel model;
    private final TerminalActionPort shell;
    private final QuestChapterManagementActionPort actions;
    private final TerminalWindowProfile profile;
    private final Runnable invalidation;
    private String selectedQuestId = "";
    private final java.util.LinkedHashSet<String> selectedQuestIds = new java.util.LinkedHashSet<String>();
    private boolean selectionMode;
    private CreateState create;
    private EditState edit;
    private String candidateQuery = "";
    private int candidatePage;
    private QuestChapterManagementVisualModel.Entry dragging;
    private int dragStartX, dragStartY, dragCurrentX, dragCurrentY;
    private boolean resizing;
    private double canvasZoom = 1D;
    private int canvasPanX, canvasPanY;
    private boolean panning;

    public QuestChapterManagementVisualDocument(QuestChapterManagementVisualModel model, TerminalActionPort shell,
        QuestChapterManagementActionPort actions, TerminalWindowProfile profile, Runnable invalidation) {
        super(StandardWidgets.create());
        if (model == null) throw new IllegalArgumentException("model is required");
        this.model = model;
        this.shell = shell == null ? TerminalActionPort.NONE : shell;
        this.actions = actions == null ? QuestChapterManagementActionPort.NONE : actions;
        this.profile = profile == null ? TerminalWindowProfile.STANDARD : profile;
        this.invalidation = invalidation == null ? new Runnable() { public void run() {} } : invalidation;
        widgets().register(new ChapterCanvas());
    }

    public void update(QuestChapterManagementVisualModel value) {
        if (value == null) throw new IllegalArgumentException("model is required");
        model = value;
        if (entry(selectedQuestId) == null) selectedQuestId = "";
        selectedQuestIds.retainAll(entryIds());
        if (edit != null && (value.getDetail() == null || !edit.id.equals(value.getDetail().getSummary().getId())
            || !edit.hash.equals(value.getDetail().getSummary().getHash()))) edit = null;
        candidatePage = Math.min(candidatePage, candidatePages() - 1);
    }

    public void beginCreateForPreview() { create = new CreateState(); invalidation.run(); }
    public boolean beginEditForPreview(int page) { if (!beginEdit()) return false; edit.page = Math.max(0, Math.min(1, page)); return true; }
    public boolean beginEditForPreview() { return beginEdit(); }
    public boolean showResourcePageForPreview() { if (!beginEdit()) return false; edit.page = 1; return true; }
    public void selectEntriesForPreview(java.util.List<String> ids) {
        selectedQuestIds.clear(); if (ids != null) for (String id : ids) if (entry(id) != null) selectedQuestIds.add(id);
        selectedQuestId = selectedQuestIds.isEmpty() ? "" : new java.util.ArrayList<String>(selectedQuestIds).get(selectedQuestIds.size() - 1);
        selectionMode = true; invalidation.run();
    }

    @Override public UiElement build(UiContext context) {
        UiElement content = create != null ? creator() : model.getDetail() == null ? list() : edit == null ? detail() : editor();
        return TerminalVisualShell.build(model.getShell(), content, shell,
            model.getDetail() == null ? "章节管理" : "章节布局", "career");
    }

    private UiElement list() {
        UiElement.Builder rows = UiElement.type("Card").key("chapter-admin-list")
            .child(label("chapter-admin-title", "任务章节 · " + model.getTotal() + " 项", true));
        if (model.getChapters().isEmpty()) rows.child(UiElement.type("EmptyState").key("chapter-admin-empty")
            .prop(StandardWidgets.TEXT, model.getMessage().isEmpty() ? "没有符合条件的章节" : model.getMessage()).build());
        for (int i = 0; i < model.getChapters().size(); i++) {
            final QuestChapterManagementVisualModel.Chapter value = model.getChapters().get(i);
            rows.child(UiElement.type("Row").key("chapter-admin-row-" + value.getId())
                .prop(StandardWidgets.ACTION, handler(new Runnable() { public void run() {
                    actions.select(value.getId(), value.getVersion());
                }})).child(label("chapter-admin-name-" + i, value.getName(), true))
                .child(label("chapter-admin-version-" + i, "v" + value.getVersion(), false))
                .child(UiElement.type("Badge").key("chapter-admin-life-" + i)
                    .prop(StandardWidgets.TEXT, value.getLifecycle()).build())
                .child(label("chapter-admin-count-" + i, value.getEntryCount() + " 个任务", false))
                .child(button("chapter-admin-up-" + i, "↑", new Runnable() { public void run() {
                    int index = model.getChapters().indexOf(value); if (index > 0) actions.moveBefore(value.getId(), model.getChapters().get(index - 1).getId());
                }}, i > 0))
                .child(button("chapter-admin-down-" + i, "↓", new Runnable() { public void run() {
                    int index = model.getChapters().indexOf(value); if (index + 1 < model.getChapters().size()) actions.moveBefore(model.getChapters().get(index + 1).getId(), value.getId());
                }}, i + 1 < model.getChapters().size())).build());
        }
        rows.child(UiElement.type("Row").key("chapter-admin-pager")
            .child(button("chapter-admin-prev", "‹", new Runnable() { public void run() { actions.page(model.getPage() - 1); } }, model.getPage() > 0))
            .child(label("chapter-admin-page", (model.getPage() + 1) + " / " + model.getPages(), false))
            .child(button("chapter-admin-next", "›", new Runnable() { public void run() { actions.page(model.getPage() + 1); } }, model.getPage() + 1 < model.getPages())).build());
        UiElement tools = UiElement.type("Row").key("chapter-admin-tools")
            .child(button("chapter-admin-back", "‹ 任务管理", new Runnable() { public void run() { actions.back(); } }, true))
            .child(button("chapter-admin-create", "＋ 新建章节", new Runnable() { public void run() { beginCreateForPreview(); } }, true))
            .child(filter("chapter-admin-all", "全部", ""))
            .child(filter("chapter-admin-draft", "草稿", "DRAFT"))
            .child(filter("chapter-admin-published", "已发布", "PUBLISHED")).build();
        return UiElement.type("Column").key("chapter-admin").child(tools).child(rows.build()).build();
    }

    private UiElement creator() {
        return UiElement.type("Column").key("chapter-create").child(UiElement.type("Card").key("chapter-create-card")
            .child(label("chapter-create-title", "创建章节草稿", true))
            .child(UiElement.type("TextField").key("chapter-create-name").prop(StandardWidgets.TEXT, create.name)
                .prop(StandardWidgets.PLACEHOLDER, "章节名称").prop(StandardWidgets.FOCUSED, create.focused)
                .prop(StandardWidgets.ACTION, new UiActionHandler() { public InputResult handle(UiEvent event) {
                    if (event.getType() == UiEvent.Type.POINTER_DOWN) create.focused = true;
                    else if (event.getType() == UiEvent.Type.TEXT_INPUT && !Character.isISOControl(event.getTypedChar())
                        && create.name.length() < 96) create.name += event.getTypedChar();
                    else if (event.getType() == UiEvent.Type.KEY_DOWN && (event.getKeyCode() == UiKeyCode.BACKSPACE
                        || event.getKeyCode() == UiKeyCode.DELETE) && !create.name.isEmpty())
                        create.name = create.name.substring(0, create.name.length() - 1);
                    invalidation.run(); return InputResult.CONSUMED;
                }}).build())
            .child(label("chapter-create-hint", "章节身份、版本和初始布局由服务器生成", false))
            .child(UiElement.type("Row").key("chapter-create-actions")
                .child(button("chapter-create-cancel", "取消", new Runnable() { public void run() { create = null; invalidation.run(); } }, true))
                .child(button("chapter-create-submit", "创建草稿", new Runnable() { public void run() {
                    String name = create.name.trim(); create = null; actions.create(name); invalidation.run();
                } }, !create.name.trim().isEmpty())).build()).build()).build();
    }

    private UiElement detail() {
        final QuestChapterManagementVisualModel.Detail detail = model.getDetail();
        final QuestChapterManagementVisualModel.Chapter summary = detail.getSummary();
        UiElement.Builder header = UiElement.type("Row").key("chapter-detail-header")
            .child(button("chapter-detail-back", "‹ 章节列表", new Runnable() { public void run() { actions.closeDetail(); } }, true))
            .child(label("chapter-detail-name", summary.getName() + " · v" + summary.getVersion(), true))
            .child(UiElement.type("Badge").key("chapter-detail-life").prop(StandardWidgets.TEXT, summary.getLifecycle()).build());
        if ("DRAFT".equals(summary.getLifecycle())) header.child(button("chapter-detail-publish", "发布",
            new Runnable() { public void run() { actions.publish(summary.getId(), summary.getVersion(), summary.getHash()); } }, true));
        if ("DRAFT".equals(summary.getLifecycle())) header.child(button("chapter-detail-edit", "编辑",
            new Runnable() { public void run() { beginEdit(); } }, true));
        if ("DRAFT".equals(summary.getLifecycle())) header.child(button("chapter-detail-select-mode",
            selectionMode ? "完成选择" : "多选", new Runnable() { public void run() {
                selectionMode = !selectionMode; invalidation.run();
            } }, true));
        if ("DRAFT".equals(summary.getLifecycle()) && !copySources().isEmpty()) header.child(button(
            "chapter-detail-copy", "复制 " + copySources().size(), new Runnable() { public void run() {
                QuestChapterManagementVisualModel.Entry origin = entry(copySources().get(0));
                if (origin != null) actions.cloneEntries(summary.getId(), summary.getVersion(), summary.getHash(),
                    copySources(), origin.getX() + Math.max(8, origin.getWidth()), origin.getY() + Math.max(8, origin.getHeight()));
            } }, true));
        if ("PUBLISHED".equals(summary.getLifecycle())) header.child(button("chapter-detail-retire", "退役",
            new Runnable() { public void run() { actions.retire(summary.getId(), summary.getVersion()); } }, true));
        UiElement canvas = UiElement.type("ChapterCanvas").key("chapter-layout-canvas").build();
        UiElement.Builder side = UiElement.type("Card").key("chapter-detail-side")
            .child(label("chapter-detail-info", detail.getVisibility() + " · 背景 " + detail.getBackgroundSize(), true))
            .child(label("chapter-detail-description", detail.getDescription(), false))
            .child(label("chapter-detail-dependencies", dependencySummary(detail), false));
        final QuestChapterManagementVisualModel.Entry selected = entry(selectedQuestId);
        if ("DRAFT".equals(summary.getLifecycle()) && copySources().size() >= 2) side
            .child(label("chapter-align-title", "多选对齐 · " + copySources().size(), true))
            .child(UiElement.type("Row").key("chapter-align-horizontal")
                .child(alignButton("chapter-align-left", "左", "LEFT"))
                .child(alignButton("chapter-align-center-x", "中", "CENTER_X"))
                .child(alignButton("chapter-align-right", "右", "RIGHT")).build())
            .child(UiElement.type("Row").key("chapter-align-vertical")
                .child(alignButton("chapter-align-top", "上", "TOP"))
                .child(alignButton("chapter-align-center-y", "中", "CENTER_Y"))
                .child(alignButton("chapter-align-bottom", "下", "BOTTOM")).build())
            .child(button("chapter-remove-selected","批量移出 "+copySources().size(),new Runnable(){public void run(){
                actions.removeEntries(summary.getId(),summary.getVersion(),summary.getHash(),copySources());
            }},true));
        else if (selected == null) side.child(label("chapter-detail-hint", "选择画布中的任务以调整位置", false));
        else {
            side.child(label("chapter-detail-selected", selected.getName(), true));
            side.child(UiElement.type("Row").key("chapter-detail-move-x")
                .child(moveButton("chapter-left", "←", selected, -8, 0)).child(moveButton("chapter-right", "→", selected, 8, 0)).build());
            side.child(UiElement.type("Row").key("chapter-detail-move-y")
                .child(moveButton("chapter-up", "↑", selected, 0, -8)).child(moveButton("chapter-down", "↓", selected, 0, 8)).build());
            side.child(button("chapter-grow", "扩大", new Runnable() { public void run() { actions.resize(summary.getId(),
                summary.getVersion(), summary.getHash(), selected.getQuestId(), selected.getWidth() + 8, selected.getHeight() + 8); } }, "DRAFT".equals(summary.getLifecycle())));
            side.child(button("chapter-remove", "移出章节", new Runnable() { public void run() { actions.remove(summary.getId(),
                summary.getVersion(), summary.getHash(), selected.getQuestId()); } }, "DRAFT".equals(summary.getLifecycle())));
        }
        if (selected == null && "DRAFT".equals(summary.getLifecycle()) && !detail.getCandidates().isEmpty()) {
            side.child(UiElement.type("TextField").key("chapter-candidate-query").prop(StandardWidgets.TEXT, candidateQuery)
                .prop(StandardWidgets.PLACEHOLDER, "搜索候选任务").prop(StandardWidgets.ACTION, new UiActionHandler() {
                    public InputResult handle(UiEvent event) { String changed = candidateQuery;
                        if (event.getType() == UiEvent.Type.TEXT_INPUT && !Character.isISOControl(event.getTypedChar()) && changed.length() < 64) changed += event.getTypedChar();
                        else if (event.getType() == UiEvent.Type.KEY_DOWN && (event.getKeyCode() == UiKeyCode.BACKSPACE || event.getKeyCode() == UiKeyCode.DELETE) && !changed.isEmpty()) changed = changed.substring(0, changed.length() - 1);
                        candidateQuery = changed; candidatePage = 0; invalidation.run(); return InputResult.CONSUMED; }
                }).build());
            java.util.List<QuestChapterManagementVisualModel.QuestCandidate> candidates = filteredCandidates();
            int from = Math.min(candidates.size(), candidatePage * 3), to = Math.min(candidates.size(), from + 3);
            if (from == to) side.child(label("chapter-candidate-empty", "没有匹配的候选任务", false));
            for (int i = from; i < to; i++) { final QuestChapterManagementVisualModel.QuestCandidate candidate = candidates.get(i);
                side.child(button("chapter-candidate-" + candidate.getId(), "＋ " + candidate.getName(), new Runnable() { public void run() {
                    actions.place(summary.getId(), summary.getVersion(), summary.getHash(), candidate.getId(), 0, 0, 24, 24);
                } }, true)); }
            side.child(UiElement.type("Row").key("chapter-candidate-pager")
                .child(button("chapter-candidate-prev", "‹", new Runnable() { public void run() { candidatePage = Math.max(0, candidatePage - 1); invalidation.run(); } }, candidatePage > 0))
                .child(label("chapter-candidate-page", (candidatePage + 1) + " / " + candidatePages(), false))
                .child(button("chapter-candidate-next", "›", new Runnable() { public void run() { candidatePage = Math.min(candidatePages() - 1, candidatePage + 1); invalidation.run(); } }, candidatePage + 1 < candidatePages())).build());
        }
        return UiElement.type("Column").key("chapter-detail").child(header.build())
            .child(UiElement.type("Row").key("chapter-detail-body").child(canvas).child(side.build()).build()).build();
    }

    private boolean beginEdit() {
        if (model.getDetail() == null || !"DRAFT".equals(model.getDetail().getSummary().getLifecycle())) return false;
        edit = new EditState(model.getDetail()); invalidation.run(); return true;
    }

    private UiElement editor() {
        final QuestChapterManagementVisualModel.Chapter summary = model.getDetail().getSummary();
        UiElement.Builder card = UiElement.type("Card").key("chapter-editor-card")
            .child(label("chapter-editor-title", "编辑章节 · " + summary.getName() + " · " + (edit.page + 1) + " / 2", true));
        if (edit.page == 0) card
            .child(editField("chapter-editor-name", "章节名称", 0, 96))
            .child(editField("chapter-editor-description", "章节说明", 1, 4096))
            .child(button("chapter-editor-visibility", "可见性 · " + edit.visibility, new Runnable() { public void run() {
                String[] values = {"NORMAL", "HIDDEN", "SECRET", "UNLOCKED", "COMPLETED", "CHAIN", "ALWAYS"};
                int index = java.util.Arrays.asList(values).indexOf(edit.visibility);
                edit.visibility = values[(index + 1 + values.length) % values.length]; invalidation.run();
            } }, true));
        else card
            .child(editField("chapter-editor-icon", "图标资源", 2, 256))
            .child(editField("chapter-editor-background", "背景资源", 3, 256))
            .child(UiElement.type("Row").key("chapter-editor-size")
                .child(button("chapter-editor-size-less", "－", new Runnable() { public void run() {
                    edit.backgroundSize = Math.max(16, edit.backgroundSize - 16); invalidation.run();
                } }, edit.backgroundSize > 16))
                .child(label("chapter-editor-size-value", "背景尺寸 " + edit.backgroundSize, false))
                .child(button("chapter-editor-size-more", "＋", new Runnable() { public void run() {
                    edit.backgroundSize = Math.min(4096, edit.backgroundSize + 16); invalidation.run();
                } }, edit.backgroundSize < 4096)).build());
        card.child(UiElement.type("Row").key("chapter-editor-pages")
            .child(button("chapter-editor-prev", "‹", new Runnable() { public void run() { edit.page = 0; invalidation.run(); } }, edit.page > 0))
            .child(button("chapter-editor-next", "›", new Runnable() { public void run() { edit.page = 1; invalidation.run(); } }, edit.page < 1)).build())
            .child(UiElement.type("Row").key("chapter-editor-actions")
                .child(button("chapter-editor-cancel", "取消", new Runnable() { public void run() { edit = null; invalidation.run(); } }, true))
                .child(button("chapter-editor-save", "保存", new Runnable() { public void run() {
                    actions.saveBasics(summary.getId(), summary.getVersion(), summary.getHash(), edit.name.trim(),
                        edit.description, edit.icon, edit.background, edit.backgroundSize, edit.visibility);
                } }, !edit.name.trim().isEmpty())).build());
        return UiElement.type("Column").key("chapter-editor").child(card.build()).build();
    }

    private UiElement editField(String key, String placeholder, final int target, final int max) {
        String value = edit.value(target);
        return UiElement.type("TextField").key(key).prop(StandardWidgets.TEXT, value)
            .prop(StandardWidgets.PLACEHOLDER, placeholder).prop(StandardWidgets.FOCUSED, edit.focus == target)
            .prop(StandardWidgets.ACTION, new UiActionHandler() { public InputResult handle(UiEvent event) {
                String changed = edit.value(target);
                if (event.getType() == UiEvent.Type.POINTER_DOWN) edit.focus = target;
                else if (event.getType() == UiEvent.Type.TEXT_INPUT && !Character.isISOControl(event.getTypedChar())
                    && changed.length() < max) changed += event.getTypedChar();
                else if (event.getType() == UiEvent.Type.KEY_DOWN && (event.getKeyCode() == UiKeyCode.BACKSPACE
                    || event.getKeyCode() == UiKeyCode.DELETE) && !changed.isEmpty()) changed = changed.substring(0, changed.length() - 1);
                edit.value(target, changed); invalidation.run(); return InputResult.CONSUMED;
            }}).build();
    }

    private UiElement moveButton(String key, String text, final QuestChapterManagementVisualModel.Entry selected,
        final int dx, final int dy) {
        final QuestChapterManagementVisualModel.Chapter summary = model.getDetail().getSummary();
        return button(key, text, new Runnable() { public void run() { actions.move(summary.getId(), summary.getVersion(),
            summary.getHash(), selected.getQuestId(), selected.getX() + dx, selected.getY() + dy); } },
            "DRAFT".equals(summary.getLifecycle()));
    }

    private UiElement alignButton(String key,String text,final String alignment){
        final QuestChapterManagementVisualModel.Chapter summary=model.getDetail().getSummary();
        return button(key,text,new Runnable(){public void run(){actions.align(summary.getId(),summary.getVersion(),
            summary.getHash(),copySources(),alignment);}},true);
    }

    private java.util.List<QuestChapterManagementVisualModel.QuestCandidate> filteredCandidates() {
        java.util.List<QuestChapterManagementVisualModel.QuestCandidate> result = new java.util.ArrayList<QuestChapterManagementVisualModel.QuestCandidate>();
        if (model.getDetail() == null) return result; String query = candidateQuery.trim().toLowerCase(java.util.Locale.ROOT);
        for (QuestChapterManagementVisualModel.QuestCandidate value : model.getDetail().getCandidates())
            if (query.isEmpty() || value.getName().toLowerCase(java.util.Locale.ROOT).contains(query) || value.getId().toLowerCase(java.util.Locale.ROOT).contains(query)) result.add(value);
        return result;
    }
    private int candidatePages() { return Math.max(1, (filteredCandidates().size() + 2) / 3); }

    @Override public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
        boolean compact = TerminalVisualMetrics.compute(viewport, profile).getBounds().getHeight() <= 220;
        LayoutSpec body = create != null ? createLayout(compact) : model.getDetail() == null ? listLayout(compact)
            : edit == null ? detailLayout(compact) : editorLayout(compact);
        return TerminalVisualShell.layout(viewport, model.getShell(), body, profile);
    }

    private LayoutSpec listLayout(boolean compact) {
        LayoutSpec tools = LayoutSpec.of("chapter-admin-tools", LayoutKind.ROW).preferred(0, compact ? 17 : 21).gap(2)
            .child(LayoutSpec.of("chapter-admin-back", LayoutKind.LEAF).preferred(62, 0).build())
            .child(LayoutSpec.of("chapter-admin-create", LayoutKind.LEAF).preferred(62, 0).build())
            .child(LayoutSpec.of("chapter-admin-all", LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("chapter-admin-draft", LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("chapter-admin-published", LayoutKind.LEAF).flex(1).build()).build();
        LayoutSpec.Builder card = LayoutSpec.of("chapter-admin-list", LayoutKind.COLUMN).flex(1)
            .padding(new Insets(3, 3, 3, 3)).gap(2)
            .child(LayoutSpec.of("chapter-admin-title", LayoutKind.LEAF).preferred(0, 13).build());
        if (model.getChapters().isEmpty()) card.child(LayoutSpec.of("chapter-admin-empty", LayoutKind.LEAF).flex(1).build());
        for (int i = 0; i < model.getChapters().size(); i++) card.child(LayoutSpec.of(
            "chapter-admin-row-" + model.getChapters().get(i).getId(), LayoutKind.ROW).preferred(0, compact ? 16 : 19).gap(2)
            .child(LayoutSpec.of("chapter-admin-name-" + i, LayoutKind.LEAF).flex(2).build())
            .child(LayoutSpec.of("chapter-admin-version-" + i, LayoutKind.LEAF).preferred(22, 0).build())
            .child(LayoutSpec.of("chapter-admin-life-" + i, LayoutKind.LEAF).preferred(48, 0).build())
            .child(LayoutSpec.of("chapter-admin-count-" + i, LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("chapter-admin-up-" + i, LayoutKind.LEAF).preferred(14, 0).build())
            .child(LayoutSpec.of("chapter-admin-down-" + i, LayoutKind.LEAF).preferred(14, 0).build()).build());
        card.child(LayoutSpec.of("chapter-admin-pager", LayoutKind.ROW).preferred(0, 17).gap(2)
            .child(LayoutSpec.of("chapter-admin-prev", LayoutKind.LEAF).preferred(20, 0).build())
            .child(LayoutSpec.of("chapter-admin-page", LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("chapter-admin-next", LayoutKind.LEAF).preferred(20, 0).build()).build());
        return LayoutSpec.of("chapter-admin", LayoutKind.COLUMN).flex(1).padding(new Insets(3, 3, 3, 3)).gap(3)
            .child(tools).child(card.build()).build();
    }

    private LayoutSpec detailLayout(boolean compact) {
        LayoutSpec.Builder header = LayoutSpec.of("chapter-detail-header", LayoutKind.ROW).preferred(0, compact ? 17 : 21).gap(3)
            .child(LayoutSpec.of("chapter-detail-back", LayoutKind.LEAF).preferred(64, 0).build())
            .child(LayoutSpec.of("chapter-detail-name", LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("chapter-detail-life", LayoutKind.LEAF).preferred(48, 0).build());
        if (model.getDetail() != null && "DRAFT".equals(model.getDetail().getSummary().getLifecycle()))
            header.child(LayoutSpec.of("chapter-detail-publish", LayoutKind.LEAF).preferred(38, 0).build());
        if (model.getDetail() != null && "DRAFT".equals(model.getDetail().getSummary().getLifecycle()))
            header.child(LayoutSpec.of("chapter-detail-edit", LayoutKind.LEAF).preferred(38, 0).build());
        if (model.getDetail() != null && "DRAFT".equals(model.getDetail().getSummary().getLifecycle()))
            header.child(LayoutSpec.of("chapter-detail-select-mode", LayoutKind.LEAF).preferred(38, 0).build());
        if (model.getDetail() != null && "DRAFT".equals(model.getDetail().getSummary().getLifecycle()) && !copySources().isEmpty())
            header.child(LayoutSpec.of("chapter-detail-copy", LayoutKind.LEAF).preferred(42, 0).build());
        if (model.getDetail() != null && "PUBLISHED".equals(model.getDetail().getSummary().getLifecycle()))
            header.child(LayoutSpec.of("chapter-detail-retire", LayoutKind.LEAF).preferred(38, 0).build());
        LayoutSpec.Builder side = LayoutSpec.of("chapter-detail-side", LayoutKind.COLUMN).preferred(compact ? 105 : 135, 0)
            .padding(new Insets(3, 3, 3, 3)).gap(2)
            .child(LayoutSpec.of("chapter-detail-info", LayoutKind.LEAF).preferred(0, 13).build())
            .child(LayoutSpec.of("chapter-detail-description", LayoutKind.LEAF).preferred(0, compact ? 20 : 30).build())
            .child(LayoutSpec.of("chapter-detail-dependencies", LayoutKind.LEAF).preferred(0, compact ? 12 : 14).build());
        QuestChapterManagementVisualModel.Entry selected = entry(selectedQuestId);
        if (model.getDetail() != null && "DRAFT".equals(model.getDetail().getSummary().getLifecycle())
            && copySources().size() >= 2) side
            .child(LayoutSpec.of("chapter-align-title", LayoutKind.LEAF).preferred(0, 14).build())
            .child(LayoutSpec.of("chapter-align-horizontal", LayoutKind.ROW).preferred(0, 18).gap(2)
                .child(LayoutSpec.of("chapter-align-left", LayoutKind.LEAF).flex(1).build())
                .child(LayoutSpec.of("chapter-align-center-x", LayoutKind.LEAF).flex(1).build())
                .child(LayoutSpec.of("chapter-align-right", LayoutKind.LEAF).flex(1).build()).build())
            .child(LayoutSpec.of("chapter-align-vertical", LayoutKind.ROW).preferred(0, 18).gap(2)
                .child(LayoutSpec.of("chapter-align-top", LayoutKind.LEAF).flex(1).build())
                .child(LayoutSpec.of("chapter-align-center-y", LayoutKind.LEAF).flex(1).build())
                .child(LayoutSpec.of("chapter-align-bottom", LayoutKind.LEAF).flex(1).build()).build())
            .child(LayoutSpec.of("chapter-remove-selected",LayoutKind.LEAF).preferred(0,18).build());
        else if (selected == null) side.child(LayoutSpec.of("chapter-detail-hint", LayoutKind.LEAF).flex(1).build());
        else side.child(LayoutSpec.of("chapter-detail-selected", LayoutKind.LEAF).preferred(0, 14).build())
            .child(LayoutSpec.of("chapter-detail-move-x", LayoutKind.ROW).preferred(0, 18).gap(2)
                .child(LayoutSpec.of("chapter-left", LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of("chapter-right", LayoutKind.LEAF).flex(1).build()).build())
            .child(LayoutSpec.of("chapter-detail-move-y", LayoutKind.ROW).preferred(0, 18).gap(2)
                .child(LayoutSpec.of("chapter-up", LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of("chapter-down", LayoutKind.LEAF).flex(1).build()).build())
            .child(LayoutSpec.of("chapter-grow", LayoutKind.LEAF).preferred(0, 18).build())
            .child(LayoutSpec.of("chapter-remove", LayoutKind.LEAF).preferred(0, 18).build());
        if (model.getDetail() != null && "DRAFT".equals(model.getDetail().getSummary().getLifecycle())
            && entry(selectedQuestId) == null && !model.getDetail().getCandidates().isEmpty()) {
            side.child(LayoutSpec.of("chapter-candidate-query", LayoutKind.LEAF).preferred(0, compact ? 16 : 19).build());
            java.util.List<QuestChapterManagementVisualModel.QuestCandidate> candidates = filteredCandidates();
            int from = Math.min(candidates.size(), candidatePage * 3), to = Math.min(candidates.size(), from + 3);
            if (from == to) side.child(LayoutSpec.of("chapter-candidate-empty", LayoutKind.LEAF).flex(1).build());
            for (int i = from; i < to; i++) side.child(LayoutSpec.of("chapter-candidate-" + candidates.get(i).getId(), LayoutKind.LEAF).preferred(0, compact ? 15 : 18).build());
            side.child(LayoutSpec.of("chapter-candidate-pager", LayoutKind.ROW).preferred(0, compact ? 15 : 18).gap(2)
                .child(LayoutSpec.of("chapter-candidate-prev", LayoutKind.LEAF).preferred(20, 0).build())
                .child(LayoutSpec.of("chapter-candidate-page", LayoutKind.LEAF).flex(1).build())
                .child(LayoutSpec.of("chapter-candidate-next", LayoutKind.LEAF).preferred(20, 0).build()).build());
        }
        return LayoutSpec.of("chapter-detail", LayoutKind.COLUMN).flex(1).padding(new Insets(3, 3, 3, 3)).gap(3)
            .child(header.build()).child(LayoutSpec.of("chapter-detail-body", LayoutKind.ROW).flex(1).gap(3)
                .child(LayoutSpec.of("chapter-layout-canvas", LayoutKind.LEAF).flex(1).build()).child(side.build()).build()).build();
    }

    private LayoutSpec createLayout(boolean compact) {
        return LayoutSpec.of("chapter-create", LayoutKind.COLUMN).flex(1).padding(new Insets(3, 3, 3, 3))
            .child(LayoutSpec.of("chapter-create-card", LayoutKind.COLUMN).flex(1).padding(new Insets(4, 4, 4, 4)).gap(3)
                .child(LayoutSpec.of("chapter-create-title", LayoutKind.LEAF).preferred(0, compact ? 13 : 16).build())
                .child(LayoutSpec.of("chapter-create-name", LayoutKind.LEAF).preferred(0, compact ? 17 : 21).build())
                .child(LayoutSpec.of("chapter-create-hint", LayoutKind.LEAF).flex(1).build())
                .child(LayoutSpec.of("chapter-create-actions", LayoutKind.ROW).preferred(0, compact ? 17 : 21).gap(3)
                    .child(LayoutSpec.of("chapter-create-cancel", LayoutKind.LEAF).flex(1).build())
                    .child(LayoutSpec.of("chapter-create-submit", LayoutKind.LEAF).flex(1).build()).build()).build()).build();
    }

    private LayoutSpec editorLayout(boolean compact) {
        LayoutSpec.Builder card = LayoutSpec.of("chapter-editor-card", LayoutKind.COLUMN).flex(1)
            .padding(new Insets(4, 4, 4, 4)).gap(3)
            .child(LayoutSpec.of("chapter-editor-title", LayoutKind.LEAF).preferred(0, compact ? 13 : 16).build());
        if (edit.page == 0) card
            .child(LayoutSpec.of("chapter-editor-name", LayoutKind.LEAF).preferred(0, compact ? 17 : 21).build())
            .child(LayoutSpec.of("chapter-editor-description", LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("chapter-editor-visibility", LayoutKind.LEAF).preferred(0, compact ? 17 : 21).build());
        else card
            .child(LayoutSpec.of("chapter-editor-icon", LayoutKind.LEAF).preferred(0, compact ? 17 : 21).build())
            .child(LayoutSpec.of("chapter-editor-background", LayoutKind.LEAF).preferred(0, compact ? 17 : 21).build())
            .child(LayoutSpec.of("chapter-editor-size", LayoutKind.ROW).flex(1).gap(3)
                .child(LayoutSpec.of("chapter-editor-size-less", LayoutKind.LEAF).preferred(24, 0).build())
                .child(LayoutSpec.of("chapter-editor-size-value", LayoutKind.LEAF).flex(1).build())
                .child(LayoutSpec.of("chapter-editor-size-more", LayoutKind.LEAF).preferred(24, 0).build()).build());
        card.child(LayoutSpec.of("chapter-editor-pages", LayoutKind.ROW).preferred(0, compact ? 17 : 21).gap(3)
            .child(LayoutSpec.of("chapter-editor-prev", LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("chapter-editor-next", LayoutKind.LEAF).flex(1).build()).build())
            .child(LayoutSpec.of("chapter-editor-actions", LayoutKind.ROW).preferred(0, compact ? 17 : 21).gap(3)
                .child(LayoutSpec.of("chapter-editor-cancel", LayoutKind.LEAF).flex(1).build())
                .child(LayoutSpec.of("chapter-editor-save", LayoutKind.LEAF).flex(1).build()).build());
        return LayoutSpec.of("chapter-editor", LayoutKind.COLUMN).flex(1).padding(new Insets(3, 3, 3, 3)).child(card.build()).build();
    }

    private QuestChapterManagementVisualModel.Entry entry(String id) {
        if (model.getDetail() != null) for (QuestChapterManagementVisualModel.Entry value : model.getDetail().getEntries())
            if (value.getQuestId().equals(id)) return value;
        return null;
    }
    private static String dependencySummary(QuestChapterManagementVisualModel.Detail detail) {
        int internal = 0, external = 0, missing = 0;
        for (QuestChapterManagementVisualModel.Dependency value : detail.getDependencies()) {
            if ("INTERNAL".equals(value.getKind())) internal++;
            else if ("EXTERNAL".equals(value.getKind())) external++;
            else if ("MISSING".equals(value.getKind())) missing++;
        }
        return "前置：内部 " + internal + " · 外部 " + external + " · 缺失 " + missing;
    }
    private java.util.Set<String> entryIds() {
        java.util.Set<String> result = new java.util.LinkedHashSet<String>();
        if (model.getDetail() != null) for (QuestChapterManagementVisualModel.Entry value : model.getDetail().getEntries())
            result.add(value.getQuestId());
        return result;
    }
    private java.util.List<String> copySources() {
        java.util.List<String> result = new java.util.ArrayList<String>(selectedQuestIds);
        if (result.isEmpty() && !selectedQuestId.isEmpty()) result.add(selectedQuestId);
        return result;
    }

    private final class ChapterCanvas implements UiWidgetAdapter {
        @Override public String type() { return "ChapterCanvas"; }
        @Override public void paint(UiNode node, UiContext context, DrawList target) {
            UiRect bounds = node.getBounds();
            target.add(DrawCommand.surface(UiLayer.CONTENT, bounds, context.getTheme().color("surface"), context.getTheme().radius("sm")));
            target.add(DrawCommand.border(UiLayer.CONTENT, bounds, context.getTheme().color("border"), context.getTheme().radius("sm")));
            if (model.getDetail() == null) return;
            target.add(DrawCommand.clipPush(UiLayer.CONTENT, bounds));
            CanvasTransform transform = transform(bounds);
            paintGrid(target, context, bounds, transform);
            paintDependencies(target, context, transform);
            for (QuestChapterManagementVisualModel.Entry value : model.getDetail().getEntries()) {
                UiRect placed = transform.rect(value);
                if (dragging != null && dragging.getQuestId().equals(value.getQuestId())) placed = resizing
                    ? new UiRect(placed.getX(), placed.getY(), Math.max(8, placed.getWidth() + dragCurrentX - dragStartX),
                        Math.max(8, placed.getHeight() + dragCurrentY - dragStartY))
                    : new UiRect(placed.getX() + dragCurrentX - dragStartX, placed.getY() + dragCurrentY - dragStartY,
                        placed.getWidth(), placed.getHeight());
                boolean selected = value.getQuestId().equals(selectedQuestId);
                target.add(DrawCommand.surface(UiLayer.CONTENT, placed,
                    context.getTheme().color("surfaceRaised"), 3));
                target.add(DrawCommand.border(UiLayer.CONTENT, placed,
                    context.getTheme().color(selected ? "accent" : "border"), 3));
                target.add(DrawCommand.text(UiLayer.CONTENT, new UiRect(placed.getX() + 3, placed.getY() + 2,
                    Math.max(1, placed.getWidth() - 6), Math.max(1, placed.getHeight() - 4)),
                    context.getTheme().color("text"), value.getName(),
                    new TextStyle("caption", 9, false, 11, TextStyle.Align.LEFT)));
            }
            target.add(DrawCommand.clipPop(UiLayer.CONTENT));
        }
        @Override public UiInputNode input(UiNode node, UiContext context) {
            final UiRect bounds = node.getBounds();
            return new UiInputNode("chapter-layout-canvas", bounds, true,
                new com.jsirgalaxybase.ui2.input.UiInputHandler() {
                    @Override public InputResult handle(UiInputNode target, UiEvent event) {
                        if (event.getPhase() != UiEvent.Phase.TARGET) return InputResult.PASS;
                        CanvasTransform transform = transform(bounds);
                        if (event.getType() == UiEvent.Type.POINTER_DOWN) {
                            if (event.getButton() == 2) {
                                panning = true; dragging = null;
                                dragStartX = dragCurrentX = event.getX(); dragStartY = dragCurrentY = event.getY();
                                return InputResult.CAPTURE_POINTER;
                            }
                            selectedQuestId = ""; dragging = null;
                            java.util.List<QuestChapterManagementVisualModel.Entry> values = model.getDetail().getEntries();
                            for (int i = values.size() - 1; i >= 0; i--) if (contains(transform.rect(values.get(i)), event.getX(), event.getY())) {
                                selectedQuestId = values.get(i).getQuestId(); dragging = values.get(i); break;
                            }
                            if (selectionMode) {
                                if (!selectedQuestId.isEmpty() && !selectedQuestIds.add(selectedQuestId)) selectedQuestIds.remove(selectedQuestId);
                                dragging = null; resizing = false; invalidation.run(); return InputResult.CONSUMED;
                            }
                            dragStartX = dragCurrentX = event.getX(); dragStartY = dragCurrentY = event.getY(); invalidation.run();
                            resizing = event.getButton() == 1;
                            return dragging != null && "DRAFT".equals(model.getDetail().getSummary().getLifecycle())
                                ? InputResult.CAPTURE_POINTER : InputResult.CONSUMED;
                        }
                        if (event.getType() == UiEvent.Type.POINTER_MOVE && panning) {
                            canvasPanX += event.getX() - dragCurrentX; canvasPanY += event.getY() - dragCurrentY;
                            dragCurrentX = event.getX(); dragCurrentY = event.getY(); invalidation.run();
                            return InputResult.CONSUMED;
                        }
                        if (event.getType() == UiEvent.Type.POINTER_UP && panning) {
                            panning = false; return InputResult.RELEASE_POINTER;
                        }
                        if (event.getType() == UiEvent.Type.POINTER_MOVE && dragging != null) {
                            dragCurrentX = event.getX(); dragCurrentY = event.getY(); invalidation.run(); return InputResult.CONSUMED;
                        }
                        if (event.getType() == UiEvent.Type.POINTER_UP && dragging != null) {
                            QuestChapterManagementVisualModel.Chapter chapter = model.getDetail().getSummary();
                            int dx = transform.logicalDelta(event.getX() - dragStartX), dy = transform.logicalDelta(event.getY() - dragStartY);
                            QuestChapterManagementVisualModel.Entry moved = dragging; dragging = null; invalidation.run();
                            if (resizing && (dx != 0 || dy != 0)) actions.resize(chapter.getId(), chapter.getVersion(), chapter.getHash(),
                                moved.getQuestId(), Math.max(1, moved.getWidth() + dx), Math.max(1, moved.getHeight() + dy));
                            else if (!resizing && (dx != 0 || dy != 0)) actions.move(chapter.getId(), chapter.getVersion(), chapter.getHash(),
                                moved.getQuestId(), moved.getX() + dx, moved.getY() + dy);
                            resizing = false;
                            return InputResult.RELEASE_POINTER;
                        }
                        if (event.getType() == UiEvent.Type.SCROLL) {
                            double oldScale = transform.scale;
                            double factor = event.getDelta() > 0 ? 1.2D : event.getDelta() < 0 ? 1D / 1.2D : 1D;
                            canvasZoom = Math.max(0.5D, Math.min(3D, canvasZoom * factor));
                            CanvasTransform changed = transform(bounds);
                            canvasPanX += (int)Math.round((event.getX() - bounds.getX() - 4 - canvasPanX)
                                * (1D - changed.scale / oldScale));
                            canvasPanY += (int)Math.round((event.getY() - bounds.getY() - 4 - canvasPanY)
                                * (1D - changed.scale / oldScale));
                            invalidation.run(); return InputResult.CONSUMED;
                        }
                        if (event.getType() == UiEvent.Type.KEY_DOWN && !selectedQuestId.isEmpty()
                            && "DRAFT".equals(model.getDetail().getSummary().getLifecycle())) {
                            int dx = event.getKeyCode() == com.jsirgalaxybase.ui2.input.UiKeyCode.LEFT ? -1
                                : event.getKeyCode() == com.jsirgalaxybase.ui2.input.UiKeyCode.RIGHT ? 1 : 0;
                            int dy = event.getKeyCode() == com.jsirgalaxybase.ui2.input.UiKeyCode.UP ? -1
                                : event.getKeyCode() == com.jsirgalaxybase.ui2.input.UiKeyCode.DOWN ? 1 : 0;
                            QuestChapterManagementVisualModel.Entry selected = entry(selectedQuestId);
                            if (selected != null && (dx != 0 || dy != 0)) {
                                QuestChapterManagementVisualModel.Chapter chapter = model.getDetail().getSummary();
                                actions.move(chapter.getId(), chapter.getVersion(), chapter.getHash(), selected.getQuestId(),
                                    selected.getX() + dx, selected.getY() + dy);
                                return InputResult.CONSUMED;
                            }
                        }
                        return InputResult.PASS;
                    }
                });
        }
    }

    private void paintGrid(DrawList target, UiContext context, UiRect bounds, CanvasTransform transform) {
        int color = context.getTheme().color("border");
        int step = 16;
        while (step * transform.scale < 7D) step *= 2;
        int left = transform.logicalX(bounds.getX()), right = transform.logicalX(bounds.getX() + bounds.getWidth());
        int top = transform.logicalY(bounds.getY()), bottom = transform.logicalY(bounds.getY() + bounds.getHeight());
        int firstX = Math.floorDiv(Math.min(left, right), step) * step;
        int firstY = Math.floorDiv(Math.min(top, bottom), step) * step;
        for (int x = firstX, count = 0; x <= Math.max(left, right) && count < 128; x += step, count++) {
            int px = transform.pixelX(x); target.add(DrawCommand.line(UiLayer.CONTENT,
                new UiRect(px, bounds.getY(), 1, bounds.getHeight()), color));
        }
        for (int y = firstY, count = 0; y <= Math.max(top, bottom) && count < 128; y += step, count++) {
            int py = transform.pixelY(y); target.add(DrawCommand.line(UiLayer.CONTENT,
                new UiRect(bounds.getX(), py, bounds.getWidth(), 1), color));
        }
    }

    private void paintDependencies(DrawList target, UiContext context, CanvasTransform transform) {
        if (model.getDetail() == null) return;
        java.util.Map<String, QuestChapterManagementVisualModel.Entry> entries = new java.util.LinkedHashMap<String, QuestChapterManagementVisualModel.Entry>();
        for (QuestChapterManagementVisualModel.Entry entry : model.getDetail().getEntries()) entries.put(entry.getQuestId(), entry);
        for (QuestChapterManagementVisualModel.Dependency dependency : model.getDetail().getDependencies()) {
            QuestChapterManagementVisualModel.Entry from = entries.get(dependency.getPrerequisiteQuestId());
            QuestChapterManagementVisualModel.Entry to = entries.get(dependency.getQuestId());
            if (from == null || to == null) continue; // External/missing prerequisites remain visible in the inspector, never faked on-canvas.
            UiRect first = transform.rect(from), second = transform.rect(to);
            int startX = first.getX() + first.getWidth() / 2, startY = first.getY() + first.getHeight() / 2;
            int endX = second.getX() + second.getWidth() / 2, endY = second.getY() + second.getHeight() / 2;
            int middleX = startX + (endX - startX) / 2;
            int color = context.getTheme().color("INTERNAL".equals(dependency.getKind()) ? "accent" : "border");
            segment(target, startX, startY, middleX, startY, color);
            segment(target, middleX, startY, middleX, endY, color);
            segment(target, middleX, endY, endX, endY, color);
        }
    }

    private static void segment(DrawList target, int x1, int y1, int x2, int y2, int color) {
        int x = Math.min(x1, x2), y = Math.min(y1, y2);
        target.add(DrawCommand.line(UiLayer.CONTENT, new UiRect(x, y, Math.max(1, Math.abs(x2 - x1) + 1),
            Math.max(1, Math.abs(y2 - y1) + 1)), color));
    }

    private CanvasTransform transform(UiRect bounds) { return new CanvasTransform(bounds, model.getDetail().getEntries(),
        canvasZoom, canvasPanX, canvasPanY); }
    private static boolean contains(UiRect value, int x, int y) { return x >= value.getX() && y >= value.getY()
        && x < (long) value.getX() + value.getWidth() && y < (long) value.getY() + value.getHeight(); }
    private static final class CanvasTransform {
        private final UiRect bounds; private final int minX, minY, panX, panY; private final double scale;
        private CanvasTransform(UiRect bounds, java.util.List<QuestChapterManagementVisualModel.Entry> values,
            double zoom, int panX, int panY) {
            this.bounds = bounds; int left = 0, top = 0, right = 128, bottom = 128;
            for (QuestChapterManagementVisualModel.Entry value : values) { left = Math.min(left, value.getX()); top = Math.min(top, value.getY());
                right = Math.max(right, value.getX() + value.getWidth()); bottom = Math.max(bottom, value.getY() + value.getHeight()); }
            minX = left; minY = top; this.panX = panX; this.panY = panY;
            scale = Math.max(0.1D, Math.min((bounds.getWidth() - 8D) / Math.max(1, right - left),
                (bounds.getHeight() - 8D) / Math.max(1, bottom - top)) * zoom);
        }
        private int pixelX(int logical) { return bounds.getX() + 4 + panX + (int)Math.round((logical - minX) * scale); }
        private int pixelY(int logical) { return bounds.getY() + 4 + panY + (int)Math.round((logical - minY) * scale); }
        private int logicalX(int pixel) { return minX + (int)Math.floor((pixel - bounds.getX() - 4 - panX) / scale); }
        private int logicalY(int pixel) { return minY + (int)Math.floor((pixel - bounds.getY() - 4 - panY) / scale); }
        private UiRect rect(QuestChapterManagementVisualModel.Entry value) { return new UiRect(pixelX(value.getX()),
            pixelY(value.getY()), Math.max(8, (int) Math.round(value.getWidth() * scale)),
            Math.max(8, (int) Math.round(value.getHeight() * scale))); }
        private int logicalDelta(int pixels) { return (int) Math.round(pixels / scale); }
    }

    private UiElement filter(String key, String text, final String value) { return button(key, text,
        new Runnable() { public void run() { actions.filter(value); } }, true); }
    private static UiElement label(String key, String text, boolean bold) { return UiElement.type("Label").key(key)
        .prop(StandardWidgets.TEXT, text).prop(StandardWidgets.TEXT_ROLE, "caption").prop(StandardWidgets.MAX_LINES, 2)
        .prop(StandardWidgets.BOLD, bold).build(); }
    private static UiElement button(String key, String text, final Runnable run, boolean enabled) { return UiElement.type("Button")
        .key(key).prop(StandardWidgets.TEXT, text).prop(StandardWidgets.ENABLED, enabled).prop(StandardWidgets.ACTION, handler(run)).build(); }
    private static UiActionHandler handler(final Runnable run) { return new UiActionHandler() {
        public InputResult handle(UiEvent event) { run.run(); return InputResult.CONSUMED; }
    }; }
    private static final class CreateState { private String name = ""; private boolean focused; }
    private static final class EditState {
        private final String id, hash;
        private String name, description, icon, background, visibility;
        private int backgroundSize, focus = -1, page;
        private EditState(QuestChapterManagementVisualModel.Detail value) {
            QuestChapterManagementVisualModel.Chapter summary = value.getSummary();
            id = summary.getId(); hash = summary.getHash(); name = summary.getName();
            description = value.getDescription(); icon = value.getIcon(); background = value.getBackground();
            backgroundSize = value.getBackgroundSize(); visibility = value.getVisibility().isEmpty() ? "NORMAL" : value.getVisibility();
        }
        private String value(int target) { return target == 0 ? name : target == 1 ? description : target == 2 ? icon : background; }
        private void value(int target, String value) { if (target == 0) name = value; else if (target == 1) description = value;
            else if (target == 2) icon = value; else background = value; }
    }
}

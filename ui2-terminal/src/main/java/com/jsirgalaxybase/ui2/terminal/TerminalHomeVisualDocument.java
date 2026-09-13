package com.jsirgalaxybase.ui2.terminal;

import com.jsirgalaxybase.ui2.component.ComponentUiDocument;
import com.jsirgalaxybase.ui2.component.StandardWidgets;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiInputHandler;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;

/** Actual production home document; platform hosts only adapt its model and actions. */
public class TerminalHomeVisualDocument extends ComponentUiDocument {
    private HomeVisualModel model;
    private final TerminalActionPort actions;
    private TerminalWindowProfile windowProfile;
    private boolean helpOpen;
    private int width;
    private int height;

    public TerminalHomeVisualDocument(HomeVisualModel model, TerminalActionPort actions) {
        this(model, actions, TerminalWindowProfile.STANDARD);
    }

    public TerminalHomeVisualDocument(HomeVisualModel model, TerminalActionPort actions,
        TerminalWindowProfile windowProfile) {
        super(StandardWidgets.create());
        if (model == null) throw new IllegalArgumentException("model is required");
        this.model = model;
        this.actions = actions == null ? TerminalActionPort.NONE : actions;
        this.windowProfile = windowProfile == null ? TerminalWindowProfile.STANDARD : windowProfile;
    }

    public final void updateVisualModel(HomeVisualModel value) {
        if (value == null) throw new IllegalArgumentException("model is required");
        model = value;
    }

    public final void updateWindowProfile(TerminalWindowProfile value) {
        windowProfile = value == null ? TerminalWindowProfile.STANDARD : value;
    }

    public final void openVisualHelp() { helpOpen = true; }

    @Override
    public UiElement build(UiContext context) {
        UiElement.Builder hero = UiElement.type("Card").key("home-hero")
            .child(label("home-title", model.getTitle(), "section", 1, true))
            .child(label("home-lead", model.getLead(), "caption", 1, false))
            .child(label("home-status", model.getStatus(), "caption", 1, false));
        UiElement.Builder sections = UiElement.type("Stack").key("home-sections");
        int count = Math.min(4, model.getSections().size());
        for (int i = 0; i < count; i++) {
            HomeVisualModel.Section section = model.getSections().get(i);
            sections.child(UiElement.type("Card").key("home-section-" + i)
                .child(label("home-section-title-" + i, section.getTitle(), "caption", 1, true))
                .child(label("home-section-summary-" + i, section.getSummary(), "caption", 1, false))
                .child(label("home-section-detail-" + i, section.getDetail(), "caption", 2, false)).build());
        }
        UiElement.Builder page = UiElement.type("Stack").key("home-page")
            .child(UiElement.type("Column").key("home-base").child(hero.build()).child(sections.build()).build());
        if (helpOpen) page.child(UiElement.type("Dialog").key("home-help")
            .prop(StandardWidgets.TEXT, "银河终端")
            .prop(StandardWidgets.DETAIL, "首页汇总当前制度状态。左侧导航进入具体业务，R 刷新，Esc 关闭终端。")
            .build());
        return TerminalVisualShell.build(model.getShell(), page.build(), actions, "银河终端", null);
    }

    private static UiElement label(String key, String value, String role, int maxLines, boolean bold) {
        return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT, value)
            .prop(StandardWidgets.TEXT_ROLE, role).prop(StandardWidgets.MAX_LINES, maxLines)
            .prop(StandardWidgets.BOLD, bold).build();
    }

    @Override
    public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
        width = viewport.getWidth();
        height = viewport.getHeight();
        TerminalVisualMetrics metrics = TerminalVisualMetrics.compute(viewport, windowProfile);
        boolean compact = metrics.getBounds().getHeight() <= 220;
        int count = Math.min(4, model.getSections().size());
        LayoutKind kind = metrics.getMainWidth() >= 280 ? LayoutKind.GRID : LayoutKind.COLUMN;
        LayoutSpec.Builder sections = LayoutSpec.of("home-sections", kind).flex(1F).gap(compact ? 2 : 4);
        if (kind == LayoutKind.GRID) sections.columns(2);
        for (int i = 0; i < count; i++) {
            sections.child(LayoutSpec.of("home-section-" + i, LayoutKind.COLUMN)
                .preferred(0, compact ? 56 : 66).padding(new Insets(4, 5, 4, 5)).gap(1)
                .child(LayoutSpec.of("home-section-title-" + i, LayoutKind.LEAF).preferred(0, 12).build())
                .child(LayoutSpec.of("home-section-summary-" + i, LayoutKind.LEAF).preferred(0, 12).build())
                .child(LayoutSpec.of("home-section-detail-" + i, LayoutKind.LEAF).flex(1F).build()).build());
        }
        LayoutSpec hero = LayoutSpec.of("home-hero", LayoutKind.COLUMN).preferred(0, compact ? 48 : 52)
            .padding(new Insets(5, 6, 5, 6)).gap(1)
            .child(LayoutSpec.of("home-title", LayoutKind.LEAF).preferred(0, 16).build())
            .child(LayoutSpec.of("home-lead", LayoutKind.LEAF).preferred(0, 12).build())
            .child(LayoutSpec.of("home-status", LayoutKind.LEAF).preferred(0, 12).build()).build();
        LayoutSpec base = LayoutSpec.of("home-base", LayoutKind.COLUMN).padding(new Insets(4, 4, 4, 4))
            .gap(compact ? 2 : 4).child(hero).child(sections.build()).build();
        LayoutSpec.Builder page = LayoutSpec.of("home-page", LayoutKind.STACK).flex(1F).child(base);
        if (helpOpen) page.child(LayoutSpec.of("home-help", LayoutKind.LEAF).build());
        return TerminalVisualShell.layout(viewport, model.getShell(), page.build(), windowProfile);
    }

    @Override
    public UiInputNode modalInput(UiNode root, UiContext context) {
        if (!helpOpen) return null;
        return new UiInputNode("home-help", new UiRect(0, 0, width, height), false, new UiInputHandler() {
            @Override
            public InputResult handle(UiInputNode target, UiEvent event) {
                if ((event.getPhase() == UiEvent.Phase.TARGET && event.getType() == UiEvent.Type.POINTER_DOWN)
                    || (event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.ESCAPE)) {
                    helpOpen = false;
                    return InputResult.CONSUMED;
                }
                return InputResult.PASS;
            }
        });
    }
}

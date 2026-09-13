package com.jsirgalaxybase.ui2.debug;

import java.util.LinkedHashSet;
import java.util.Set;

import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;

/** Deterministic, renderer-neutral evidence used by visual regression reports. */
public final class UiDebugSnapshot {
    private UiDebugSnapshot() {}

    public static String capture(UiNode root, DrawList drawList, int width, int height) {
        if (drawList == null) throw new IllegalArgumentException("drawList is required");
        drawList.validateBalanced();
        StringBuilder output = new StringBuilder();
        output.append("viewport|").append(width).append('x').append(height).append('\n');
        output.append("nodes\n");
        appendNode(output, root, 0);
        output.append("audit\n");
        appendAudit(output, drawList, width, height);
        output.append("drawlist\n").append(drawList.snapshot());
        return output.toString();
    }

    private static void appendNode(StringBuilder output, UiNode node, int depth) {
        if (node == null) return;
        for (int i = 0; i < depth; i++) output.append("  ");
        output.append(node.getElement().getType()).append('|');
        output.append(node.getElement().getKey() == null ? "-" : node.getElement().getKey().getValue());
        output.append('|').append(node.getBounds()).append('|');
        output.append(node.isMounted() ? "mounted" : "unmounted").append('\n');
        for (UiNode child : node.getChildren()) appendNode(output, child, depth + 1);
    }

    private static void appendAudit(StringBuilder output, DrawList drawList, int width, int height) {
        UiRect viewport = new UiRect(0, 0, Math.max(0, width), Math.max(0, height));
        Set<String> externalIds = new LinkedHashSet<String>();
        int issueCount = 0;
        int visibleCount = 0;
        for (DrawCommand command : drawList.ordered()) {
            UiRect bounds = command.getBounds();
            if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) visibleCount++;
            if (bounds != null && !viewport.contains(bounds)
                && command.getKind() != DrawCommand.Kind.SHADOW
                && command.getKind() != DrawCommand.Kind.TRANSFORM_PUSH) {
                output.append("WARN|OUTSIDE_VIEWPORT|").append(command.snapshot()).append('\n');
                issueCount++;
            }
            if (command.getKind() == DrawCommand.Kind.TEXT && bounds != null && command.getTextStyle() != null
                && bounds.getHeight() < command.getTextStyle().getLineHeight()) {
                output.append("WARN|TEXT_HEIGHT|").append(command.snapshot()).append('\n');
                issueCount++;
            }
            if (command.getKind() == DrawCommand.Kind.EXTERNAL_REGION
                && !externalIds.add(command.getExternalId())) {
                output.append("WARN|DUPLICATE_EXTERNAL_ID|").append(command.getExternalId()).append('\n');
                issueCount++;
            }
        }
        output.append("SUMMARY|issues=").append(issueCount)
            .append("|commands=").append(drawList.size())
            .append("|visible=").append(visibleCount)
            .append("|externalRegions=").append(externalIds.size()).append('\n');
    }
}

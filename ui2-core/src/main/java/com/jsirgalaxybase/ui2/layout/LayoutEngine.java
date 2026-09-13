package com.jsirgalaxybase.ui2.layout;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jsirgalaxybase.ui2.geometry.Constraints;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;

public final class LayoutEngine {
    public UiSize measure(LayoutSpec spec, Constraints constraints) {
        if (spec == null || constraints == null) throw new IllegalArgumentException("spec and constraints are required");
        Insets padding = spec.getPadding();
        int width = spec.getPreferredSize().getWidth();
        int height = spec.getPreferredSize().getHeight();
        List<LayoutSpec> children = spec.getChildren();
        if (!children.isEmpty()) {
            int maxWidth = 0, maxHeight = 0, sumWidth = 0, sumHeight = 0;
            for (LayoutSpec child : children) {
                UiSize childSize = child.getPreferredSize();
                maxWidth = Math.max(maxWidth, childSize.getWidth());
                maxHeight = Math.max(maxHeight, childSize.getHeight());
                sumWidth += childSize.getWidth();
                sumHeight += childSize.getHeight();
            }
            int gaps = spec.getGap() * Math.max(0, children.size() - 1);
            if (spec.getKind() == LayoutKind.ROW) { width = Math.max(width, sumWidth + gaps); height = Math.max(height, maxHeight); }
            else if (spec.getKind() == LayoutKind.COLUMN || spec.getKind() == LayoutKind.SCROLL) {
                width = Math.max(width, maxWidth); height = Math.max(height, sumHeight + gaps);
            } else { width = Math.max(width, maxWidth); height = Math.max(height, maxHeight); }
        }
        long paddedWidth = (long) width + padding.horizontal();
        long paddedHeight = (long) height + padding.vertical();
        return constraints.constrain(new UiSize(safe(paddedWidth), safe(paddedHeight)));
    }

    public LayoutResult layout(LayoutSpec spec, UiRect bounds) {
        Map<String, UiRect> output = new LinkedHashMap<String, UiRect>();
        layoutInto(spec, bounds, output);
        return new LayoutResult(output);
    }

    private void layoutInto(LayoutSpec spec, UiRect bounds, Map<String, UiRect> output) {
        if (output.put(spec.getKey(), bounds) != null) throw new IllegalArgumentException("duplicate layout key: " + spec.getKey());
        Insets pad = spec.getPadding();
        UiRect inner = inset(bounds, pad);
        List<LayoutSpec> children = spec.getChildren();
        if (children.isEmpty()) return;
        switch (spec.getKind()) {
            case ROW: layoutLinear(children, inner, spec.getGap(), true, output); break;
            case COLUMN: layoutLinear(children, inner, spec.getGap(), false, output); break;
            case GRID: layoutGrid(children, inner, spec.getGap(), spec.getColumns(), output); break;
            case SCROLL: layoutScroll(children, inner, spec.getGap(), output); break;
            case STACK:
            case LEAF:
            default: for (LayoutSpec child : children) layoutInto(child, inner, output);
        }
    }

    private void layoutLinear(List<LayoutSpec> children, UiRect bounds, int gap, boolean row, Map<String, UiRect> output) {
        int available = (row ? bounds.getWidth() : bounds.getHeight()) - gap * Math.max(0, children.size() - 1);
        available = Math.max(0, available);
        int fixed = 0;
        float totalFlex = 0F;
        for (LayoutSpec child : children) {
            if (child.getFlex() > 0F) totalFlex += child.getFlex();
            else fixed += row ? child.getPreferredSize().getWidth() : child.getPreferredSize().getHeight();
        }
        int flexible = Math.max(0, available - fixed);
        int cursor = row ? bounds.getX() : bounds.getY();
        int remainingFlex = flexible;
        float remainingWeight = totalFlex;
        for (int i = 0; i < children.size(); i++) {
            LayoutSpec child = children.get(i);
            int length;
            if (child.getFlex() > 0F && remainingWeight > 0F) {
                length = i == children.size() - 1 ? remainingFlex : Math.round(remainingFlex * child.getFlex() / remainingWeight);
                remainingFlex -= length;
                remainingWeight -= child.getFlex();
            } else length = row ? child.getPreferredSize().getWidth() : child.getPreferredSize().getHeight();
            length = Math.max(0, Math.min(length, row ? bounds.getRight() - cursor : bounds.getBottom() - cursor));
            UiRect childBounds = row ? new UiRect(cursor, bounds.getY(), length, bounds.getHeight())
                : new UiRect(bounds.getX(), cursor, bounds.getWidth(), length);
            layoutInto(child, childBounds, output);
            cursor += length + gap;
        }
    }

    private void layoutGrid(List<LayoutSpec> children, UiRect bounds, int gap, int columns, Map<String, UiRect> output) {
        int rows = (children.size() + columns - 1) / columns;
        int cellWidth = Math.max(0, (bounds.getWidth() - gap * Math.max(0, columns - 1)) / columns);
        int cellHeight = rows == 0 ? 0 : Math.max(0, (bounds.getHeight() - gap * Math.max(0, rows - 1)) / rows);
        for (int i = 0; i < children.size(); i++) {
            int col = i % columns, row = i / columns;
            int x = bounds.getX() + col * (cellWidth + gap);
            int y = bounds.getY() + row * (cellHeight + gap);
            int width = col == columns - 1 ? Math.max(0, bounds.getRight() - x) : cellWidth;
            int height = row == rows - 1 ? Math.max(0, bounds.getBottom() - y) : cellHeight;
            layoutInto(children.get(i), new UiRect(x, y, width, height), output);
        }
    }

    private void layoutScroll(List<LayoutSpec> children, UiRect bounds, int gap, Map<String, UiRect> output) {
        int cursor = bounds.getY();
        for (LayoutSpec child : children) {
            int height = Math.max(0, child.getPreferredSize().getHeight());
            layoutInto(child, new UiRect(bounds.getX(), cursor, bounds.getWidth(), height), output);
            cursor += height + gap;
        }
    }

    private static UiRect inset(UiRect bounds, Insets padding) {
        int width = Math.max(0, bounds.getWidth() - padding.horizontal());
        int height = Math.max(0, bounds.getHeight() - padding.vertical());
        return new UiRect(bounds.getX() + padding.getLeft(), bounds.getY() + padding.getTop(), width, height);
    }

    private static int safe(long value) { return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, value); }
}

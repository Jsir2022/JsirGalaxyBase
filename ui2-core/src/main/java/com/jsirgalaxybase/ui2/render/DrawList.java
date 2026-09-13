package com.jsirgalaxybase.ui2.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DrawList {
    private final List<DrawCommand> commands = new ArrayList<DrawCommand>();
    private int clipDepth;
    private int transformDepth;
    private int lastLayer = Integer.MIN_VALUE;

    public DrawList add(DrawCommand command) {
        if (command == null) throw new IllegalArgumentException("command is required");
        if (command.getLayer() < lastLayer) throw new IllegalArgumentException("draw layers must be appended in order");
        if (command.getKind() == DrawCommand.Kind.CLIP_PUSH) clipDepth++;
        if (command.getKind() == DrawCommand.Kind.CLIP_POP && --clipDepth < 0) throw new IllegalStateException("unbalanced clip pop");
        if (command.getKind() == DrawCommand.Kind.TRANSFORM_PUSH) transformDepth++;
        if (command.getKind() == DrawCommand.Kind.TRANSFORM_POP && --transformDepth < 0) throw new IllegalStateException("unbalanced transform pop");
        commands.add(command);
        lastLayer = command.getLayer();
        return this;
    }

    public List<DrawCommand> ordered() {
        return Collections.unmodifiableList(new ArrayList<DrawCommand>(commands));
    }

    public DrawList layers(int minimumInclusive, int maximumExclusive) {
        DrawList result = new DrawList();
        for (DrawCommand command : commands) {
            if (command.getLayer() >= minimumInclusive && command.getLayer() < maximumExclusive) result.add(command);
        }
        return result;
    }

    public void validateBalanced() {
        if (clipDepth != 0) throw new IllegalStateException("unbalanced clip stack: " + clipDepth);
        if (transformDepth != 0) throw new IllegalStateException("unbalanced transform stack: " + transformDepth);
    }

    public String snapshot() {
        validateBalanced();
        StringBuilder output = new StringBuilder();
        for (DrawCommand command : ordered()) output.append(command.snapshot()).append('\n');
        return output.toString();
    }

    public int size() { return commands.size(); }
}

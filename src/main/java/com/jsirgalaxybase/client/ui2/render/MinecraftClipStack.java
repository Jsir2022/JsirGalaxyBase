// SPDX-License-Identifier: LGPL-3.0-or-later
// Informed by Qz-UILib 4.1.3-LTS c1c9874ca20d2f1526f3ce3769dcb704e46e4258 and
// 4.8.0 7937cd042910c8182d899d41aa5692d1c8b4a98a
// src/main/java/club/heiqi/uilib/ui/render/ClipStack.java; rewritten for UI2 rectangular DrawCommands.
package com.jsirgalaxybase.client.ui2.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.ui2.geometry.UiRect;

/** Nested framebuffer-space scissor stack that restores the host baseline idempotently. */
final class MinecraftClipStack implements AutoCloseable {
    interface GlOps {
        boolean enabled();
        UiRect box();
        void enable();
        void disable();
        void scissor(UiRect box);
    }
    private static final class RealGlOps implements GlOps {
        @Override public boolean enabled() { return GL11.glIsEnabled(GL11.GL_SCISSOR_TEST); }
        @Override public UiRect box() {
            IntBuffer values = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, values);
            return new UiRect(values.get(0), values.get(1), Math.max(0, values.get(2)), Math.max(0, values.get(3)));
        }
        @Override public void enable() { GL11.glEnable(GL11.GL_SCISSOR_TEST); }
        @Override public void disable() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }
        @Override public void scissor(UiRect box) {
            GL11.glScissor(box.getX(), box.getY(), box.getWidth(), box.getHeight());
        }
    }

    private final GlOps gl;
    private final int scaleFactor;
    private final int displayHeight;
    private final boolean baselineEnabled;
    private final UiRect baseline;
    private final Deque<UiRect> stack = new ArrayDeque<UiRect>();

    MinecraftClipStack(int scaleFactor, int displayHeight) {
        this(scaleFactor, displayHeight, new RealGlOps());
    }
    MinecraftClipStack(int scaleFactor, int displayHeight, GlOps gl) {
        this.scaleFactor = Math.max(1, scaleFactor);
        this.displayHeight = Math.max(0, displayHeight);
        this.gl = gl;
        this.baselineEnabled = gl.enabled();
        this.baseline = baselineEnabled ? gl.box() : null;
    }
    void push(UiRect logical) {
        UiRect resolved = ScissorMath.toFramebuffer(logical, scaleFactor, displayHeight);
        if (!stack.isEmpty()) resolved = ScissorMath.intersect(stack.peek(), resolved);
        else if (baselineEnabled) resolved = ScissorMath.intersect(baseline, resolved);
        stack.push(resolved); apply();
    }
    void pop() { if (!stack.isEmpty()) stack.pop(); apply(); }
    int depth() { return stack.size(); }
    private void apply() {
        if (!stack.isEmpty()) { gl.enable(); gl.scissor(stack.peek()); }
        else if (baselineEnabled) { gl.enable(); gl.scissor(baseline); }
        else gl.disable();
    }
    @Override public void close() { stack.clear(); apply(); }
}

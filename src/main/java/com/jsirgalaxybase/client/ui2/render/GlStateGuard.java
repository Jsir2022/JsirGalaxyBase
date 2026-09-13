package com.jsirgalaxybase.client.ui2.render;

import org.lwjgl.opengl.GL11;

/** Restores all fixed-function state changed by the UI2 renderer. */
public final class GlStateGuard implements AutoCloseable {
    private boolean closed;
    public GlStateGuard() {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}

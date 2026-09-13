package com.jsirgalaxybase.client.ui2.render;

import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.client.ui2.font.MinecraftFontService;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.render.UiRenderer;

/** Interprets platform-neutral UI2 commands through isolated Minecraft rendering backends. */
public final class MinecraftUiRenderer implements UiRenderer {
    private final MinecraftShapeBackend shapes;
    private final MinecraftTextBackend text;
    private final MinecraftImageBackend images = new MinecraftImageBackend();
    private final int scaleFactor;
    private final int displayHeight;
    private int maximumClipDepth;

    public MinecraftUiRenderer(MinecraftFontService fonts, int scaleFactor, int displayHeight) {
        if (fonts == null || scaleFactor < 1 || displayHeight < 0) {
            throw new IllegalArgumentException("invalid renderer context");
        }
        this.scaleFactor = scaleFactor;
        this.displayHeight = displayHeight;
        this.shapes = new MinecraftShapeBackend(scaleFactor);
        this.text = new MinecraftTextBackend(fonts);
    }

    @Override public void render(DrawList drawList) {
        render(drawList, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public void render(DrawList drawList, int minimumLayer, int maximumLayer) {
        render(drawList, minimumLayer, maximumLayer, true);
    }

    public void render(DrawList drawList, int minimumLayer, int maximumLayer,
        boolean uploadFonts) {
        render(drawList,minimumLayer,maximumLayer,uploadFonts,null);
    }

    public void render(DrawList drawList, int minimumLayer, int maximumLayer,
        boolean uploadFonts, UiRect hardClip) {
        if (drawList == null) return;
        long started = System.nanoTime();
        maximumClipDepth = 0;
        try (GlStateGuard ignored = new GlStateGuard();
             MinecraftClipStack clips = new MinecraftClipStack(scaleFactor, displayHeight)) {
            if(hardClip!=null)clips.push(hardClip);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            for (DrawCommand command : drawList.ordered()) {
                if (command.getLayer() < minimumLayer || command.getLayer() >= maximumLayer) continue;
                draw(command, clips);
                maximumClipDepth = Math.max(maximumClipDepth, clips.depth());
            }
            if(hardClip!=null)clips.pop();
        } finally {
            UiRenderDiagnostics.INSTANCE.record(drawList.size(), maximumClipDepth,
                System.nanoTime() - started);
        }
    }

    private void draw(DrawCommand command, MinecraftClipStack clips) {
        UiRect bounds = command.getBounds();
        switch (command.getKind()) {
            case SURFACE:
                shapes.surface(bounds, command.getColor(), command.getColor(), command.getRadius()); break;
            case GRADIENT_SURFACE:
                shapes.surface(bounds, command.getColor(), command.getSecondaryColor(), command.getRadius()); break;
            case SHADOW:
                shapes.shadow(bounds, command.getColor(), command.getRadius(), command.getElevation()); break;
            case BORDER:
                shapes.border(bounds, command.getColor(), command.getRadius()); break;
            case LINE:
                shapes.line(bounds, command.getColor()); break;
            case CHART_LINE:
                shapes.chartLine(command.getText(), command.getColor()); break;
            case CHART_VOLUME:
                shapes.chartVolume(command.getText(), command.getColor()); break;
            case TEXT:
                text.draw(command.getText(), bounds, command.getColor(), command.getTextStyle()); break;
            case ICON:
                shapes.surface(bounds, command.getColor(), command.getColor(),
                    Math.min(bounds.getWidth(), bounds.getHeight()) / 2); break;
            case IMAGE:
                images.draw(bounds, command.getColor(), command.getResourceId()); break;
            case EXTERNAL_REGION:
                break;
            case CLIP_PUSH:
                clips.push(bounds); break;
            case CLIP_POP:
                clips.pop(); break;
            case TRANSFORM_PUSH:
                GL11.glPushMatrix(); GL11.glTranslatef(bounds.getX(), bounds.getY(), 0F); break;
            case TRANSFORM_POP:
                GL11.glPopMatrix(); break;
            default:
                break;
        }
    }
}

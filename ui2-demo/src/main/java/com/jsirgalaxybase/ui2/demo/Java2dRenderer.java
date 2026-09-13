package com.jsirgalaxybase.ui2.demo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;

public final class Java2dRenderer {
    private final Font baseFont = loadBaseFont();

    public BufferedImage render(DrawList list, int width, int height) {
        list.validateBalanced();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(baseFont.deriveFont(Font.PLAIN, height < 220 ? 10F : 12F));
        graphics.setClip(0, 0, width, height);
        Deque<Shape> clips = new ArrayDeque<Shape>();
        Deque<AffineTransform> transforms = new ArrayDeque<AffineTransform>();
        try {
            for (DrawCommand command : list.ordered()) draw(graphics, command, clips, transforms);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static Font loadBaseFont() {
        String configured = System.getenv("UI2_PREVIEW_FONT");
        if (configured != null && !configured.trim().isEmpty()) {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, new File(configured));
            } catch (Exception exception) {
                throw new IllegalStateException("cannot load UI2_PREVIEW_FONT=" + configured, exception);
            }
        }
        String[] candidates = { "Noto Sans CJK SC", "Droid Sans Fallback", "Microsoft YaHei", "SansSerif" };
        for (String candidate : candidates) {
            Font font = new Font(candidate, Font.PLAIN, 12);
            if (font.canDisplay('资') && font.canDisplay('产')) return font;
        }
        throw new IllegalStateException("no CJK preview font available; set UI2_PREVIEW_FONT to an external TTF file");
    }

    private void draw(Graphics2D graphics, DrawCommand command, Deque<Shape> clips, Deque<AffineTransform> transforms) {
        UiRect rect = command.getBounds();
        switch (command.getKind()) {
            case SURFACE:
                graphics.setColor(new Color(command.getColor(), true));
                graphics.fillRoundRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), command.getRadius() * 2, command.getRadius() * 2);
                break;
            case GRADIENT_SURFACE:
                graphics.setPaint(new GradientPaint(rect.getX(), rect.getY(), new Color(command.getColor(), true),
                    rect.getX(), rect.getBottom(), new Color(command.getSecondaryColor(), true)));
                graphics.fillRoundRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), command.getRadius() * 2, command.getRadius() * 2);
                break;
            case SHADOW:
                graphics.setColor(new Color(command.getColor(), true));
                int offset = Math.max(1, command.getElevation());
                graphics.fillRoundRect(rect.getX() + offset, rect.getY() + offset, rect.getWidth(), rect.getHeight(), command.getRadius() * 2, command.getRadius() * 2);
                break;
            case BORDER:
                graphics.setColor(new Color(command.getColor(), true)); graphics.setStroke(new BasicStroke(1F));
                graphics.drawRoundRect(rect.getX(), rect.getY(), Math.max(0, rect.getWidth() - 1), Math.max(0, rect.getHeight() - 1), command.getRadius() * 2, command.getRadius() * 2);
                break;
            case LINE:
                graphics.setColor(new Color(command.getColor(), true));
                graphics.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                break;
            case CHART_LINE:
                drawChartLine(graphics, command);
                break;
            case CHART_VOLUME:
                drawChartVolume(graphics, command);
                break;
            case TEXT:
                if (command.getTextStyle() != null) graphics.setFont(graphics.getFont().deriveFont(
                    command.getTextStyle().isBold() ? Font.BOLD : Font.PLAIN, command.getTextStyle().getSize()));
                graphics.setColor(new Color(command.getColor(), true));
                int textX = rect.getX();
                int textWidth = graphics.getFontMetrics().stringWidth(command.getText());
                if (command.getTextStyle() != null && command.getTextStyle().getAlign() == com.jsirgalaxybase.ui2.text.TextStyle.Align.CENTER) textX += (rect.getWidth() - textWidth) / 2;
                else if (command.getTextStyle() != null && command.getTextStyle().getAlign() == com.jsirgalaxybase.ui2.text.TextStyle.Align.RIGHT) textX += rect.getWidth() - textWidth;
                graphics.drawString(command.getText(), textX, rect.getY() + Math.min(rect.getHeight() - 1, graphics.getFontMetrics().getAscent()));
                break;
            case ICON:
                graphics.setColor(new Color(command.getColor(), true));
                graphics.fillOval(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                break;
            case IMAGE:
                graphics.setColor(new Color(command.getColor(), true));
                graphics.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                break;
            case EXTERNAL_REGION:
                graphics.setColor(new Color(0x334D9FFF, true));
                graphics.drawRoundRect(rect.getX(), rect.getY(), Math.max(0, rect.getWidth() - 1), Math.max(0, rect.getHeight() - 1), 4, 4);
                break;
            case CLIP_PUSH:
                clips.push(graphics.getClip()); graphics.clipRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                break;
            case CLIP_POP:
                graphics.setClip(clips.pop());
                break;
            case TRANSFORM_PUSH:
                transforms.push(graphics.getTransform());
                graphics.translate(rect.getX(), rect.getY());
                break;
            case TRANSFORM_POP:
                graphics.setTransform(transforms.pop());
                break;
            default: break;
        }
    }

    private static void drawChartLine(Graphics2D graphics, DrawCommand command) {
        String[] values = command.getText().split(";");
        Path2D.Float path = new Path2D.Float(); boolean started = false;
        for (String value : values) {
            String[] pair = value.split(","); if (pair.length != 2) continue;
            try { float x=Float.parseFloat(pair[0]),y=Float.parseFloat(pair[1]);if(!started){path.moveTo(x,y);started=true;}else path.lineTo(x,y); } catch(NumberFormatException ignored) {}
        }
        graphics.setColor(new Color(command.getColor(),true)); graphics.setStroke(new BasicStroke(1.35F)); graphics.draw(path);
    }

    private static void drawChartVolume(Graphics2D graphics, DrawCommand command) {
        graphics.setColor(new Color(command.getColor(),true));
        for(String value:command.getText().split(";")){String[] p=value.split(",");if(p.length!=4)continue;try{graphics.fillRect(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Math.max(1,Integer.parseInt(p[2])),Math.max(1,Integer.parseInt(p[3])));}catch(NumberFormatException ignored){}}
    }
}

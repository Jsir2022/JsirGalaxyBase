package com.jsirgalaxybase.client.gui.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;

public class LabelPanel extends AbstractGuiPanel {

    private final Supplier<String> textSupplier;
    private final ThemeColorKey colorKey;
    private final boolean centered;

    public LabelPanel(Supplier<String> textSupplier, ThemeColorKey colorKey, boolean centered) {
        this.textSupplier = textSupplier;
        this.colorKey = colorKey;
        this.centered = centered;
    }

    @Override
    public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRenderer == null) {
            return;
        }

        FontRenderer fontRenderer = minecraft.fontRenderer;
        GuiRect bounds = getBounds();
        int availableWidth = Math.max(8, bounds.getWidth() - 8);
        String text = textSupplier == null ? "" : textSupplier.get();
        List<String> lines = safeWrap(fontRenderer, text == null ? "" : text, availableWidth);
        int color = scene.getTheme().color(colorKey);
        int lineY = bounds.getY() + 2;
        for (String line : lines) {
            int drawX = centered
                ? bounds.getX() + Math.max(0, (bounds.getWidth() - fontRenderer.getStringWidth(line)) / 2)
                : bounds.getX() + 4;
            fontRenderer.drawStringWithShadow(line, drawX, lineY, color);
            lineY += 10;
            if (lineY > bounds.getBottom() - 10) {
                break;
            }
        }
    }

    private List<String> safeWrap(FontRenderer fontRenderer, String text, int availableWidth) {
        List<String> lines = new ArrayList<String>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            String remaining = paragraph;
            while (!remaining.isEmpty()) {
                String fitted = fontRenderer.trimStringToWidth(remaining, availableWidth);
                int consumed = fitted == null ? 0 : fitted.length();
                if (consumed <= 0) {
                    lines.add(remaining.substring(0, 1));
                    remaining = remaining.substring(1).trim();
                    continue;
                }

                if (consumed >= remaining.length()) {
                    lines.add(remaining);
                    break;
                }

                int split = consumed;
                while (split > 0 && split < remaining.length() && remaining.charAt(split - 1) != ' ') {
                    split--;
                }
                if (split <= 0) {
                    split = consumed;
                }

                String line = remaining.substring(0, split).trim();
                lines.add(line.isEmpty() ? remaining.substring(0, consumed) : line);
                remaining = remaining.substring(split).trim();
            }
        }
        return lines;
    }
}
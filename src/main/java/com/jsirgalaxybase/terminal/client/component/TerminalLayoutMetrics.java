package com.jsirgalaxybase.terminal.client.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

final class TerminalLayoutMetrics {

    private static final float TERMINAL_TEXT_SCALE = 0.78F;
    static final int TEXT_LINE_HEIGHT = 8;
    static final int TEXT_VERTICAL_PADDING = 2;
    static final int BUTTON_HEIGHT = 16;
    static final int BUTTON_GAP = 3;
    static final int CARD_PADDING = 4;
    static final int CARD_ROW_GAP = 2;
    static final int SCROLLBAR_RESERVE = 7;

    private TerminalLayoutMetrics() {}

    static int labelHeight(String text, int width) {
        return labelHeight(text, width, 1);
    }

    static int labelHeight(String text, int width, int minLines) {
        int safeLines = Math.max(minLines, wrappedLineCount(text, width));
        return safeLines * TEXT_LINE_HEIGHT + TEXT_VERTICAL_PADDING;
    }

    static int rowHeight(String text, int width) {
        return Math.max(18, labelHeight(text, width, 1));
    }

    static int cardHeaderHeight() {
        return TEXT_LINE_HEIGHT + TEXT_VERTICAL_PADDING;
    }

    static int contentWidth(int outerWidth, int horizontalPadding) {
        return Math.max(24, outerWidth - horizontalPadding * 2 - SCROLLBAR_RESERVE);
    }

    static int wrappedLineCount(String text, int width) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
        int availableWidth = Math.max(8, Math.round((width - 8) / TERMINAL_TEXT_SCALE));
        FontRenderer fontRenderer = getFontRenderer();
        int lines = 0;
        String[] paragraphs = normalized.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.length() == 0) {
                lines++;
                continue;
            }
            if (fontRenderer != null) {
                lines += countFontRendererLines(fontRenderer, paragraph, availableWidth);
            } else {
                lines += countEstimatedLines(paragraph, availableWidth);
            }
        }
        return Math.max(1, lines);
    }

    static int stackHeight(int rowCount, int rowHeight, int gap) {
        int count = Math.max(0, rowCount);
        if (count == 0) {
            return 0;
        }
        return count * Math.max(1, rowHeight) + Math.max(0, count - 1) * Math.max(0, gap);
    }

    private static int countFontRendererLines(FontRenderer fontRenderer, String paragraph, int availableWidth) {
        int lines = 0;
        String remaining = paragraph;
        while (remaining.length() > 0) {
            String fitted = fontRenderer.trimStringToWidth(remaining, availableWidth);
            int consumed = fitted == null ? 0 : fitted.length();
            if (consumed <= 0) {
                consumed = 1;
            }
            lines++;
            if (consumed >= remaining.length()) {
                break;
            }
            remaining = remaining.substring(consumed).trim();
        }
        return Math.max(1, lines);
    }

    private static int countEstimatedLines(String paragraph, int availableWidth) {
        int lineWidth = 0;
        int lines = 1;
        for (int index = 0; index < paragraph.length(); index++) {
            int charWidth = estimateCharWidth(paragraph.charAt(index));
            if (lineWidth > 0 && lineWidth + charWidth > availableWidth) {
                lines++;
                lineWidth = 0;
            }
            lineWidth += charWidth;
        }
        return lines;
    }

    private static int estimateCharWidth(char value) {
        if (value <= 0x7F) {
            return value == ' ' ? 4 : 6;
        }
        return 9;
    }

    private static FontRenderer getFontRenderer() {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            return minecraft == null ? null : minecraft.fontRenderer;
        } catch (LinkageError error) {
            return null;
        } catch (RuntimeException exception) {
            return null;
        }
    }
}

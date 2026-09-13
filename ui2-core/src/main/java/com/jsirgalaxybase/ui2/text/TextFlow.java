package com.jsirgalaxybase.ui2.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.ui2.geometry.UiSize;

/** Measurement-driven wrapping and ellipsis shared by every rendering backend. */
public final class TextFlow {
    private static final String ELLIPSIS = "\u2026";

    private TextFlow() {}

    public static Result layout(String text, TextStyle style, TextMeasurer measurer,
        int maxWidth, int maxLines) {
        if (style == null || measurer == null) throw new IllegalArgumentException("style and measurer are required");
        int width = Math.max(0, maxWidth);
        int limit = Math.max(0, maxLines);
        String value = text == null ? "" : text;
        if (width == 0 || limit == 0) return new Result(Collections.<String>emptyList(), !value.isEmpty());

        List<String> lines = new ArrayList<String>();
        boolean truncated = false;
        int paragraphStart = 0;
        while (paragraphStart <= value.length() && lines.size() < limit) {
            int newline = value.indexOf('\n', paragraphStart);
            int paragraphEnd = newline < 0 ? value.length() : newline;
            String paragraph = value.substring(paragraphStart, paragraphEnd);
            if (paragraph.isEmpty()) {
                lines.add("");
            } else {
                int offset = 0;
                while (offset < paragraph.length() && lines.size() < limit) {
                    int end = fittingEnd(paragraph, offset, style, measurer, width);
                    if (end <= offset) {
                        int forcedEnd=offset+Character.charCount(paragraph.codePointAt(offset));
                        String forced=paragraph.substring(offset,forcedEnd);
                        if(measurer.measure(forced,style,Integer.MAX_VALUE).getWidth()>width){
                            lines.add("");truncated=true;offset=paragraph.length();break;
                        }
                        end=forcedEnd;
                    }
                    int breakAt = preferredBreak(paragraph, offset, end);
                    if (breakAt > offset && end < paragraph.length()) end = breakAt;
                    lines.add(trimLineEnd(paragraph.substring(offset, end)));
                    offset = skipLineStartWhitespace(paragraph, end);
                }
                if (offset < paragraph.length()) truncated = true;
            }
            if (newline < 0) break;
            paragraphStart = newline + 1;
            if (lines.size() >= limit && paragraphStart <= value.length()) truncated = true;
        }
        if (truncated && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, ellipsize(lines.get(last), style, measurer, width));
        }
        return new Result(lines, truncated);
    }

    private static int fittingEnd(String value, int start, TextStyle style, TextMeasurer measurer, int maxWidth) {
        int offset = start;
        int lastFit = start;
        while (offset < value.length()) {
            offset += Character.charCount(value.codePointAt(offset));
            UiSize measured = measurer.measure(value.substring(start, offset), style, Integer.MAX_VALUE);
            if (measured.getWidth() > maxWidth) break;
            lastFit = offset;
        }
        return lastFit;
    }

    private static int preferredBreak(String value, int start, int end) {
        for (int index = end; index > start; index--) {
            char previous = value.charAt(index - 1);
            if (Character.isWhitespace(previous) || previous == '-' || previous == '/' || previous == '\u00b7') {
                return index;
            }
        }
        return end;
    }

    private static int skipLineStartWhitespace(String value, int start) {
        int offset = start;
        while (offset < value.length()) {
            int codePoint = value.codePointAt(offset);
            if (!Character.isWhitespace(codePoint)) break;
            offset += Character.charCount(codePoint);
        }
        return offset;
    }

    private static String trimLineEnd(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) end--;
        return value.substring(0, end);
    }

    private static String ellipsize(String value, TextStyle style, TextMeasurer measurer, int maxWidth) {
        if (measurer.measure(ELLIPSIS, style, Integer.MAX_VALUE).getWidth() > maxWidth) return "";
        String current = value == null ? "" : value;
        while (!current.isEmpty()
            && measurer.measure(current + ELLIPSIS, style, Integer.MAX_VALUE).getWidth() > maxWidth) {
            int codePoint = current.codePointBefore(current.length());
            current = current.substring(0, current.length() - Character.charCount(codePoint));
        }
        return current + ELLIPSIS;
    }

    public static final class Result {
        private final List<String> lines;
        private final boolean truncated;
        private Result(List<String> lines, boolean truncated) {
            this.lines = Collections.unmodifiableList(new ArrayList<String>(lines));
            this.truncated = truncated;
        }
        public List<String> getLines() { return lines; }
        public boolean isTruncated() { return truncated; }
        public int height(TextStyle style) { return lines.size() * style.getLineHeight(); }
    }
}

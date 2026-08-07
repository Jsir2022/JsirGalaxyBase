package com.jsirgalaxybase.terminal.client.component;

import net.minecraft.client.gui.Gui;

final class TerminalIconPainter {

    static final int ICON_PRIMARY = 0xFFE8F2FA;
    static final int ICON_SECONDARY = 0xFF89A3B8;
    static final int ICON_MUTED = 0xFF5E7488;
    static final int ICON_GREEN = 0xFF69D86A;

    private TerminalIconPainter() {}

    static TerminalIconKind navIconFor(String pageId, String label) {
        String value = ((pageId == null ? "" : pageId) + " " + (label == null ? "" : label)).toLowerCase();
        if (value.contains("market") || value.contains("市场")) {
            return TerminalIconKind.MARKET;
        }
        if (value.contains("bank") || value.contains("银行")) {
            return TerminalIconKind.BANK;
        }
        if (value.contains("server") || value.contains("warp") || value.contains("传送")) {
            return TerminalIconKind.WARP;
        }
        if (value.contains("job") || value.contains("career") || value.contains("职业")) {
            return TerminalIconKind.JOB;
        }
        if (value.contains("public") || value.contains("公共")) {
            return TerminalIconKind.PUBLIC;
        }
        return TerminalIconKind.HOME;
    }

    static void draw(TerminalIconKind kind, int x, int y, int size, int color) {
        int s = Math.max(8, size);
        switch (kind) {
            case HOME:
                drawHome(x, y, s, color);
                break;
            case JOB:
                drawJob(x, y, s, color);
                break;
            case PUBLIC:
                drawPublic(x, y, s, color);
                break;
            case MARKET:
                drawMarket(x, y, s, color);
                break;
            case WARP:
                drawWarp(x, y, s, color);
                break;
            case BANK:
                drawBank(x, y, s, color);
                break;
            case HELP:
                drawHelp(x, y, s, color);
                break;
            case REFRESH:
                drawRefresh(x, y, s, color);
                break;
            case BACK:
                drawBack(x, y, s, color);
                break;
            case CLOSE:
                drawClose(x, y, s, color);
                break;
            case SIGNAL:
                drawSignal(x, y, s);
                break;
            case INFO:
            default:
                drawInfo(x, y, s, color);
                break;
        }
    }

    private static void drawHome(int x, int y, int s, int color) {
        drawPattern(new String[] {
            ".....#.....",
            "....###....",
            "...#####...",
            "..##...##..",
            "...#...#...",
            "...#...#...",
            "...#...#...",
            "...##.##...",
            "...##.##...",
            "...#####..."}, x, y, s, color);
    }

    private static void drawJob(int x, int y, int s, int color) {
        drawPattern(new String[] {
            "...#####...",
            "...#...#...",
            "..#######..",
            ".#.......#.",
            ".#...#...#.",
            ".#########.",
            ".#.......#.",
            ".#.......#.",
            ".#########."}, x, y, s, color);
    }

    private static void drawPublic(int x, int y, int s, int color) {
        drawPattern(new String[] {
            "...##.##...",
            "..#..#..#..",
            "..###.###..",
            ".#########.",
            ".#...#...#.",
            ".#...#...#.",
            ".#########.",
            ".#...#...#.",
            ".#########."}, x, y, s, color);
    }

    private static void drawMarket(int x, int y, int s, int color) {
        drawPattern(new String[] {
            "...#...#...",
            "..#.....#..",
            ".#########.",
            ".#.......#.",
            ".#.#.#.#.#.",
            ".#.......#.",
            ".#########.",
            "..#.....#..",
            "..#.....#.."}, x, y, s, color);
    }

    private static void drawWarp(int x, int y, int s, int color) {
        drawPattern(new String[] {
            ".....#.....",
            "..#..#..#..",
            "...#.#.#...",
            "....###....",
            "###########",
            "....###....",
            "...#.#.#...",
            "..#..#..#..",
            ".....#....."}, x, y, s, color);
    }

    private static void drawBank(int x, int y, int s, int color) {
        drawPattern(new String[] {
            ".....#.....",
            "....###....",
            "...#####...",
            "..#######..",
            "...#.#.#...",
            "...#.#.#...",
            "...#.#.#...",
            "...#.#.#...",
            "..#######.."}, x, y, s, color);
    }

    private static void drawHelp(int x, int y, int s, int color) {
        drawPattern(new String[] {
            ".#####.",
            "#.....#",
            ".....#.",
            "....#..",
            "...#...",
            "...#...",
            ".......",
            "...#..."}, x, y, s, color);
    }

    private static void drawInfo(int x, int y, int s, int color) {
        drawPattern(new String[] {
            "...#...",
            ".......",
            "..##...",
            "...#...",
            "...#...",
            "...#...",
            "..###.."}, x, y, s, color);
    }

    private static void drawRefresh(int x, int y, int s, int color) {
        drawPattern(new String[] {
            "..####.",
            ".#....#",
            "#......",
            "#..###.",
            "....#.#",
            ".#....#",
            "..####."}, x, y, s, color);
    }

    private static void drawBack(int x, int y, int s, int color) {
        drawPattern(new String[] {
            "...#....",
            "..##....",
            ".###....",
            "########",
            ".###....",
            "..##....",
            "...#...."}, x, y, s, color);
    }

    private static void drawClose(int x, int y, int s, int color) {
        drawPattern(new String[] {
            "#.....#",
            ".#...#.",
            "..#.#..",
            "...#...",
            "..#.#..",
            ".#...#.",
            "#.....#"}, x, y, s, color);
    }

    private static void drawSignal(int x, int y, int s) {
        int bar = Math.max(1, s / 8);
        for (int i = 0; i < 4; i++) {
            int h = 3 + i * Math.max(2, s / 7);
            int bx = x + 2 + i * (bar + 2);
            Gui.drawRect(bx, y + s - h, bx + bar, y + s, ICON_GREEN);
        }
    }

    private static void h(int x, int y, int width, int color) {
        if (width > 0) {
            Gui.drawRect(x, y, x + width, y + 1, color);
        }
    }

    private static void v(int x, int y, int height, int color) {
        if (height > 0) {
            Gui.drawRect(x, y, x + 1, y + height, color);
        }
    }

    private static void p(int x, int y, int color) {
        Gui.drawRect(x, y, x + 1, y + 1, color);
    }

    private static void r(int x, int y, int width, int height, int color) {
        if (width > 0 && height > 0) {
            Gui.drawRect(x, y, x + width, y + height, color);
        }
    }

    private static void drawPattern(String[] rows, int x, int y, int size, int color) {
        if (rows == null || rows.length == 0 || size <= 0) {
            return;
        }
        int patternHeight = rows.length;
        int patternWidth = 0;
        for (String row : rows) {
            patternWidth = Math.max(patternWidth, row == null ? 0 : row.length());
        }
        if (patternWidth <= 0) {
            return;
        }
        for (int py = 0; py < patternHeight; py++) {
            String row = rows[py] == null ? "" : rows[py];
            for (int px = 0; px < row.length(); px++) {
                if (row.charAt(px) != '#') {
                    continue;
                }
                int left = x + px * size / patternWidth;
                int top = y + py * size / patternHeight;
                int right = x + Math.max(left - x + 1, (px + 1) * size / patternWidth);
                int bottom = y + Math.max(top - y + 1, (py + 1) * size / patternHeight);
                Gui.drawRect(left, top, right, bottom, color);
            }
        }
    }
}

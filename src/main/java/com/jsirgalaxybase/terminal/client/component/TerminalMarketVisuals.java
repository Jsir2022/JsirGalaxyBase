package com.jsirgalaxybase.terminal.client.component;

import java.lang.reflect.Method;

import net.minecraft.client.gui.Gui;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;

final class TerminalMarketVisuals {

    enum StatusIconKind {
        INVENTORY,
        DELIVERY,
        ORDERS
    }

    static final int COLOR_BLUE = 0xFF3268C0;
    static final int COLOR_BROWN = 0xFF82603B;
    static final int COLOR_GOLD = 0xFFD7A938;
    static final int COLOR_GREEN = 0xFF62C86C;
    static final int COLOR_RED = 0xFFC94F4F;
    static final int COLOR_SILVER = 0xFFB7C0C8;
    static final int COLOR_PURPLE = 0xFF8E65D8;
    private static final String CLIENT_ICON_RENDERER =
        "com.jsirgalaxybase.terminal.client.component.TerminalMarketClientIconRenderer";
    private static Method drawItemIconMethod;
    private static boolean drawItemIconUnavailable;

    private TerminalMarketVisuals() {}

    static int colorForItem(String value) {
        String normalized = value == null ? "" : value.toLowerCase();
        if (normalized.contains("redstone") || normalized.contains("红石")) {
            return COLOR_RED;
        }
        if (normalized.contains("emerald") || normalized.contains("绿") || normalized.contains("晶")) {
            return COLOR_GREEN;
        }
        if (normalized.contains("gold") || normalized.contains("coin") || normalized.contains("starcoin")
            || normalized.contains("金币") || normalized.contains("硬币")) {
            return COLOR_GOLD;
        }
        if (normalized.contains("quantum") || normalized.contains("紫") || normalized.contains("护甲")) {
            return COLOR_PURPLE;
        }
        if (normalized.contains("wood") || normalized.contains("crate") || normalized.contains("箱")
            || normalized.contains("木")) {
            return COLOR_BROWN;
        }
        if (normalized.contains("iron") || normalized.contains("steel") || normalized.contains("ingot")
            || normalized.contains("锭") || normalized.contains("铁") || normalized.contains("钢")) {
            return COLOR_SILVER;
        }
        return COLOR_BLUE;
    }

    static void drawItemBadge(int x, int y, int size, String seed) {
        drawItemIconOrBadge(x, y, size, seed, seed);
    }

    static void drawItemIconOrBadge(int x, int y, int size, String iconRef, String fallbackSeed) {
        if (drawItemIcon(x, y, size, iconRef)) {
            return;
        }
        drawFallbackItemBadge(x, y, size, fallbackSeed);
    }

    static void drawFallbackItemBadge(int x, int y, int size, String seed) {
        int color = colorForItem(seed);
        RoundedRectPainter.draw(x, y, x + size, y + size, 0xFF0C131B, 0xFF25303D);
        Gui.drawRect(x + 4, y + 4, x + size - 4, y + size - 4, darken(color));
        Gui.drawRect(x + 6, y + 6, x + size - 7, y + size - 7, color);
        Gui.drawRect(x + size - 9, y + 4, x + size - 4, y + 9, lighten(color));
    }

    static ItemStack resolveItemStack(String iconRef) {
        ParsedItemRef parsed = ParsedItemRef.parse(iconRef);
        if (parsed.registryName.isEmpty()) {
            return null;
        }
        Object object = Item.itemRegistry.getObject(parsed.registryName);
        if (!(object instanceof Item)) {
            return null;
        }
        return new ItemStack((Item) object, Math.max(1, parsed.stackSize), Math.max(0, parsed.meta));
    }

    static String resolveLocalizedItemName(String iconRef, String fallback) {
        ItemStack stack = resolveItemStack(iconRef);
        if (stack != null) {
            try {
                String localized = EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName());
                if (localized != null && !localized.trim().isEmpty()) {
                    return localized.trim();
                }
            } catch (RuntimeException ignored) {
                // Broken third-party localization must not make the market unusable.
            }
        }
        String safeFallback = fallback == null ? "" : fallback.trim();
        return safeFallback.isEmpty() ? "--" : safeFallback;
    }

    static void drawMarketIcon(int x, int y, int size, int kind) {
        if (kind == 1) {
            drawCrate(x, y, size, COLOR_BROWN);
        } else if (kind == 2) {
            drawCoins(x, y, size);
        } else {
            drawCrate(x, y, size, COLOR_BLUE);
        }
    }

    static void drawCoin(int x, int y, int size, int color) {
        Gui.drawRect(x + size / 5, y, x + size * 4 / 5, y + size, darken(color));
        Gui.drawRect(x, y + size / 5, x + size, y + size * 4 / 5, darken(color));
        Gui.drawRect(x + 2, y + size / 5 + 2, x + size - 2, y + size * 4 / 5 - 1, color);
        Gui.drawRect(x + size / 3, y + 3, x + size * 2 / 3, y + size - 3, lighten(color));
    }

    private static boolean drawItemIcon(int x, int y, int size, String iconRef) {
        ItemStack stack = resolveItemStack(iconRef);
        if (stack == null) {
            return false;
        }
        Method method = resolveDrawItemIconMethod();
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(size), stack));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Method resolveDrawItemIconMethod() {
        if (drawItemIconUnavailable) {
            return null;
        }
        if (drawItemIconMethod != null) {
            return drawItemIconMethod;
        }
        try {
            Class<?> rendererClass = Class.forName(CLIENT_ICON_RENDERER);
            drawItemIconMethod = rendererClass.getDeclaredMethod("drawItemIcon",
                Integer.TYPE, Integer.TYPE, Integer.TYPE, ItemStack.class);
            drawItemIconMethod.setAccessible(true);
            return drawItemIconMethod;
        } catch (Exception ignored) {
            drawItemIconUnavailable = true;
            return null;
        }
    }

    static void drawStatusDot(int x, int y, boolean active) {
        int color = active ? COLOR_GREEN : 0xFF68727D;
        Gui.drawRect(x, y + 2, x + 8, y + 6, darken(color));
        Gui.drawRect(x + 2, y, x + 6, y + 8, darken(color));
        Gui.drawRect(x + 2, y + 2, x + 6, y + 6, color);
    }

    static void drawStatusIcon(int x, int y, boolean active, StatusIconKind kind) {
        int color = active ? COLOR_GREEN : 0xFF68727D;
        int dark = darken(color);
        if (kind == StatusIconKind.DELIVERY) {
            Gui.drawRect(x + 1, y + 6, x + 9, y + 9, dark);
            Gui.drawRect(x + 4, y, x + 6, y + 6, color);
            Gui.drawRect(x + 2, y + 3, x + 8, y + 5, color);
            return;
        }
        if (kind == StatusIconKind.ORDERS) {
            Gui.drawRect(x + 1, y, x + 8, y + 9, dark);
            Gui.drawRect(x + 3, y + 2, x + 7, y + 3, color);
            Gui.drawRect(x + 3, y + 4, x + 7, y + 5, color);
            Gui.drawRect(x + 3, y + 6, x + 6, y + 7, color);
            return;
        }
        Gui.drawRect(x, y + 2, x + 9, y + 9, dark);
        Gui.drawRect(x + 1, y + 3, x + 8, y + 5, color);
        Gui.drawRect(x + 4, y + 3, x + 5, y + 9, dark);
    }

    static void drawAccentFrame(int x, int y, int width, int height, int border, int fill) {
        RoundedRectPainter.draw(x, y, x + width, y + height, border, fill);
    }

    private static void drawCrate(int x, int y, int size, int color) {
        Gui.drawRect(x + size / 6, y + size / 4, x + size * 5 / 6, y + size * 5 / 6, darken(color));
        Gui.drawRect(x + size / 4, y + size / 3, x + size * 3 / 4, y + size * 3 / 4, color);
        Gui.drawRect(x + size / 3, y + size / 7, x + size * 2 / 3, y + size / 3, lighten(color));
        Gui.drawRect(x + size / 2 - 1, y + size / 3, x + size / 2 + 2, y + size * 3 / 4, lighten(color));
    }

    private static void drawCoins(int x, int y, int size) {
        int coin = Math.max(10, size / 3);
        drawCoin(x + size / 8, y + size / 2, coin, COLOR_GOLD);
        drawCoin(x + size / 2 - coin / 2, y + size / 4, coin, COLOR_GOLD);
        drawCoin(x + size * 5 / 8, y + size / 2, coin, 0xFFC69024);
    }

    private static int lighten(int color) {
        return mix(color, 0xFFFFFFFF, 0.22F);
    }

    private static int darken(int color) {
        return mix(color, 0xFF000000, 0.28F);
    }

    private static int mix(int a, int b, float ratio) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int rr = Math.round(ar * (1.0F - ratio) + br * ratio);
        int rg = Math.round(ag * (1.0F - ratio) + bg * ratio);
        int rb = Math.round(ab * (1.0F - ratio) + bb * ratio);
        return 0xFF000000 | (rr << 16) | (rg << 8) | rb;
    }

    private static final class ParsedItemRef {

        private final String registryName;
        private final int meta;
        private final int stackSize;

        private ParsedItemRef(String registryName, int meta, int stackSize) {
            this.registryName = registryName;
            this.meta = meta;
            this.stackSize = stackSize;
        }

        private static ParsedItemRef parse(String value) {
            String normalized = value == null ? "" : value.trim();
            if (normalized.isEmpty() || "--".equals(normalized)) {
                return new ParsedItemRef("", 0, 1);
            }
            normalized = trimDisplayPrefix(normalized);
            int stackSize = parseTrailingStackSize(normalized);
            normalized = stripTrailingStackSize(normalized);
            int meta = 0;
            int metaDelimiter = findMetaDelimiter(normalized);
            if (metaDelimiter >= 0) {
                meta = parseInt(normalized.substring(metaDelimiter + 1).replace("@", "").replace("#", "").replace(":", "").trim(), 0);
                normalized = normalized.substring(0, metaDelimiter).trim();
            }
            return new ParsedItemRef(normalized, meta, stackSize);
        }

        private static String trimDisplayPrefix(String value) {
            int pipe = value.indexOf('|');
            if (pipe >= 0 && pipe + 1 < value.length()) {
                String right = value.substring(pipe + 1).trim();
                if (right.indexOf(':') >= 0) {
                    return right;
                }
            }
            return value;
        }

        private static int findMetaDelimiter(String value) {
            int at = value.lastIndexOf('@');
            int hash = value.lastIndexOf('#');
            int colon = value.lastIndexOf(':');
            int best = Math.max(at, hash);
            if (colon > 0 && colon > best && colon + 1 < value.length()
                && allDigits(value.substring(colon + 1).trim())) {
                best = colon;
            }
            return best;
        }

        private static int parseTrailingStackSize(String value) {
            int marker = value.lastIndexOf(" x");
            if (marker < 0 || marker + 2 >= value.length()) {
                return 1;
            }
            return parseInt(value.substring(marker + 2).trim(), 1);
        }

        private static String stripTrailingStackSize(String value) {
            int marker = value.lastIndexOf(" x");
            if (marker < 0 || marker + 2 >= value.length()) {
                return value;
            }
            String suffix = value.substring(marker + 2).trim();
            return allDigits(suffix) ? value.substring(0, marker).trim() : value;
        }

        private static boolean allDigits(String value) {
            if (value == null || value.isEmpty()) {
                return false;
            }
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isDigit(value.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        private static int parseInt(String value, int fallback) {
            try {
                return Integer.parseInt(value);
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
    }
}

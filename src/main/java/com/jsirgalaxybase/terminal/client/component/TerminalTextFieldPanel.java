package com.jsirgalaxybase.terminal.client.component;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import org.lwjgl.input.Keyboard;

import com.jsirgalaxybase.client.gui.framework.AbstractGuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;

public final class TerminalTextFieldPanel extends AbstractGuiPanel {

    private final Supplier<String> textSupplier;
    private final Consumer<String> changeConsumer;
    private final Supplier<Boolean> focusedSupplier;
    private final Runnable focusAction;
    private final String placeholder;
    private final int maxLength;
    private final Predicate<Character> charFilter;

    public TerminalTextFieldPanel(Supplier<String> textSupplier, Consumer<String> changeConsumer,
        Supplier<Boolean> focusedSupplier, Runnable focusAction, String placeholder, int maxLength,
        Predicate<Character> charFilter) {
        this.textSupplier = textSupplier;
        this.changeConsumer = changeConsumer;
        this.focusedSupplier = focusedSupplier;
        this.focusAction = focusAction;
        this.placeholder = placeholder == null ? "" : placeholder;
        this.maxLength = maxLength <= 0 ? 32 : maxLength;
        this.charFilter = charFilter;
    }

    @Override
    public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (!isVisible()) {
            return;
        }

        GuiRect bounds = getBounds();
        boolean focused = isFocused();
        int borderColor = scene.getTheme().color(focused ? ThemeColorKey.PANEL_ACCENT : ThemeColorKey.PANEL_BORDER);
        int fillColor = scene.getTheme().color(focused ? ThemeColorKey.BUTTON_FILL_PRESSED : ThemeColorKey.PANEL_FILL);
        Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), borderColor);
        Gui.drawRect(bounds.getX() + 1, bounds.getY() + 1, bounds.getRight() - 1, bounds.getBottom() - 1, fillColor);

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRenderer == null) {
            return;
        }

        FontRenderer fontRenderer = minecraft.fontRenderer;
        String value = currentValue();
        boolean showPlaceholder = value.isEmpty();
        String displayText = showPlaceholder ? placeholder : value + (focused ? "_" : "");
        int color = scene.getTheme().color(showPlaceholder ? ThemeColorKey.TEXT_SECONDARY : ThemeColorKey.TEXT_PRIMARY);
        String trimmed = fontRenderer.trimStringToWidth(displayText, Math.max(8, bounds.getWidth() - 8));
        fontRenderer.drawStringWithShadow(trimmed, bounds.getX() + 4, bounds.getY() + Math.max(1, (bounds.getHeight() - 8) / 2), color);
    }

    @Override
    public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
        if (!isVisible()) {
            return false;
        }
        if (mouseButton == 0 && contains(mouseX, mouseY)) {
            if (focusAction != null) {
                focusAction.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyTyped(GuiScene scene, char typedChar, int keyCode) {
        if (!isVisible() || !isFocused()) {
            return false;
        }

        String value = currentValue();
        if (keyCode == Keyboard.KEY_BACK) {
            if (!value.isEmpty()) {
                change(value.substring(0, value.length() - 1));
            }
            return true;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_TAB) {
            return true;
        }
        if (Character.isISOControl(typedChar)) {
            return false;
        }
        if (value.length() >= maxLength) {
            return true;
        }
        if (charFilter != null && !charFilter.test(Character.valueOf(typedChar))) {
            return true;
        }

        change(value + typedChar);
        return true;
    }

    private boolean isFocused() {
        return focusedSupplier != null && Boolean.TRUE.equals(focusedSupplier.get());
    }

    private String currentValue() {
        String value = textSupplier == null ? "" : textSupplier.get();
        return value == null ? "" : value;
    }

    private void change(String value) {
        if (changeConsumer != null) {
            changeConsumer.accept(value == null ? "" : value);
        }
    }
}
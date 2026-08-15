package com.jsirgalaxybase.terminal.client.component;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.ModalPopupPanel;

/** Small input-only step between choosing a Vault item and confirming a custom listing. */
public final class CustomListingPricePopup extends ModalPopupPanel {

    private String priceText = "";
    private boolean priceFocused;
    private final TerminalTextFieldPanel priceField;
    private final ButtonPanel confirmButton;
    private final ButtonPanel cancelButton;
    private final Consumer<String> confirmAction;

    public CustomListingPricePopup(int screenWidth, int screenHeight, Consumer<String> confirmAction, Runnable cancelAction) {
        this.confirmAction = confirmAction;
        priceField = new TerminalTextFieldPanel(() -> priceText, value -> priceText = value,
            () -> Boolean.valueOf(priceFocused), () -> priceFocused = true, "挂牌价格", 18,
            value -> Boolean.valueOf(value.charValue() >= '0' && value.charValue() <= '9'));
        confirmButton = new ButtonPanel(() -> "下一步", this::confirm,
            () -> Boolean.valueOf(parsePrice() > 0L), 0.88F);
        cancelButton = new ButtonPanel(() -> "取消", cancelAction, () -> Boolean.TRUE, 0.88F);
        addChild(priceField);
        addChild(confirmButton);
        addChild(cancelButton);

        int popupWidth = Math.min(270, Math.max(220, screenWidth - 120));
        int popupHeight = 126;
        setBounds(new GuiRect((screenWidth - popupWidth) / 2, (screenHeight - popupHeight) / 2,
            popupWidth, popupHeight));
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        if (priceField == null) {
            return;
        }
        int x = bounds.getX() + 12;
        int contentWidth = bounds.getWidth() - 24;
        int gap = 5;
        int half = (contentWidth - gap) / 2;
        priceField.setBounds(new GuiRect(x, bounds.getY() + 48, contentWidth, 17));
        confirmButton.setBounds(new GuiRect(x, bounds.getBottom() - 29, half, 18));
        cancelButton.setBounds(new GuiRect(x + half + gap, bounds.getBottom() - 29,
            contentWidth - half - gap, 18));
    }

    @Override
    protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        super.drawSelf(scene, mouseX, mouseY, partialTicks);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        if (font == null) {
            return;
        }
        GuiRect bounds = getBounds();
        int x = bounds.getX() + 12;
        draw(font, "设置挂牌价格", x, bounds.getY() + 12, 0xFFF0C75E, bounds.getWidth() - 24);
        draw(font, "价格由服务端按 STARCOIN 正整数校验", x, bounds.getY() + 28,
            0xFFBFCBDA, bounds.getWidth() - 24);
    }

    private void confirm() {
        long value = parsePrice();
        if (value > 0L && confirmAction != null) {
            confirmAction.accept(String.valueOf(value));
        }
    }

    private long parsePrice() {
        try {
            return Long.parseLong(priceText == null || priceText.isEmpty() ? "0" : priceText);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static void draw(FontRenderer font, String text, int x, int y, int color, int width) {
        font.drawStringWithShadow(font.trimStringToWidth(text, Math.max(8, width)), x, y, color);
    }
}

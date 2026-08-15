package com.jsirgalaxybase.terminal.client.component;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.ModalPopupPanel;

/** Focused order ticket. The detail workstation remains read-only until this modal is opened. */
public final class MarketOrderEntryPopup extends ModalPopupPanel {

    private final TerminalMarketSectionState state;
    private final TerminalMarketSectionState.OrderSide side;
    private final String productName;
    private final String marketPrice;
    private final String accountSummary;
    private final long availableAsset;
    private final Runnable submitAction;
    private TerminalMarketSectionState.OrderType orderType;
    private boolean quantityFocused;
    private boolean priceFocused;

    private final ButtonPanel marketButton;
    private final ButtonPanel limitButton;
    private final TerminalTextFieldPanel quantityField;
    private final ButtonPanel maximumButton;
    private final TerminalTextFieldPanel priceField;
    private final ButtonPanel submitButton;
    private final ButtonPanel cancelButton;

    public MarketOrderEntryPopup(int screenWidth, int screenHeight, TerminalMarketSectionState state,
        TerminalMarketSectionState.OrderSide side, TerminalMarketSectionState.OrderType initialType,
        String productName, String marketPrice, String accountSummary, Runnable submitAction, Runnable cancelAction) {
        this(screenWidth, screenHeight, state, side, initialType, productName, marketPrice, accountSummary, 0L,
            submitAction, cancelAction);
    }

    public MarketOrderEntryPopup(int screenWidth, int screenHeight, TerminalMarketSectionState state,
        TerminalMarketSectionState.OrderSide side, TerminalMarketSectionState.OrderType initialType,
        String productName, String marketPrice, String accountSummary, long availableAsset,
        Runnable submitAction, Runnable cancelAction) {
        this.state = state;
        this.side = side == null ? TerminalMarketSectionState.OrderSide.BUY : side;
        this.orderType = initialType == null ? TerminalMarketSectionState.OrderType.MARKET : initialType;
        this.productName = productName == null ? "--" : productName;
        this.marketPrice = compactPrice(marketPrice);
        this.accountSummary = accountSummary == null ? "" : accountSummary;
        this.availableAsset = Math.max(0L, availableAsset);
        this.submitAction = submitAction;

        seedDraft();
        marketButton = new ButtonPanel(() -> "市价", () -> switchType(TerminalMarketSectionState.OrderType.MARKET),
            () -> Boolean.TRUE, 0.85F);
        limitButton = new ButtonPanel(() -> "限价", () -> switchType(TerminalMarketSectionState.OrderType.LIMIT),
            () -> Boolean.TRUE, 0.85F);
        quantityField = new TerminalTextFieldPanel(this::quantityText, this::setQuantityText,
            () -> Boolean.valueOf(quantityFocused), () -> focus(true), "数量", 18,
            value -> Boolean.valueOf(value.charValue() >= '0' && value.charValue() <= '9'));
        maximumButton = new ButtonPanel(() -> "最大", this::applyMaximumQuantity,
            () -> Boolean.valueOf(maximumQuantity() > 0L), 0.82F);
        priceField = new TerminalTextFieldPanel(this::priceText, this::setPriceText,
            () -> Boolean.valueOf(priceFocused), () -> focus(false), "限价", 18,
            value -> Boolean.valueOf(value.charValue() >= '0' && value.charValue() <= '9'));
        submitButton = new ButtonPanel(() -> this.side == TerminalMarketSectionState.OrderSide.BUY ? "确认买入" : "确认卖出",
            () -> submit(), () -> Boolean.valueOf(isComplete()), 0.88F);
        cancelButton = new ButtonPanel(() -> "取消", cancelAction, () -> Boolean.TRUE, 0.88F);
        addChild(marketButton);
        addChild(limitButton);
        addChild(quantityField);
        addChild(maximumButton);
        addChild(priceField);
        addChild(submitButton);
        addChild(cancelButton);

        int popupWidth = Math.min(304, Math.max(248, screenWidth - 110));
        int popupHeight = Math.min(178, Math.max(154, screenHeight - 126));
        setBounds(new GuiRect((screenWidth - popupWidth) / 2, (screenHeight - popupHeight) / 2,
            popupWidth, popupHeight));
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        if (marketButton == null) { return; }
        int x = bounds.getX() + 12;
        int width = bounds.getWidth() - 24;
        int gap = 5;
        int half = (width - gap) / 2;
        marketButton.setBounds(new GuiRect(x, bounds.getY() + 48, half, 16));
        limitButton.setBounds(new GuiRect(x + half + gap, bounds.getY() + 48, width - half - gap, 16));
        int maximumWidth = 42;
        quantityField.setBounds(new GuiRect(x, bounds.getY() + 72, Math.max(32, half - maximumWidth - gap), 16));
        maximumButton.setBounds(new GuiRect(x + Math.max(32, half - maximumWidth - gap) + gap,
            bounds.getY() + 72, maximumWidth, 16));
        priceField.setVisible(orderType == TerminalMarketSectionState.OrderType.LIMIT);
        priceField.setBounds(orderType == TerminalMarketSectionState.OrderType.LIMIT
            ? new GuiRect(x + half + gap, bounds.getY() + 72, width - half - gap, 16)
            : new GuiRect(0, 0, 0, 0));
        int buttonY = bounds.getBottom() - 30;
        submitButton.setBounds(new GuiRect(x, buttonY, half, 18));
        cancelButton.setBounds(new GuiRect(x + half + gap, buttonY, width - half - gap, 18));
    }

    @Override
    protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        super.drawSelf(scene, mouseX, mouseY, partialTicks);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        if (font == null) { return; }
        GuiRect b = getBounds();
        int x = b.getX() + 12;
        int color = side == TerminalMarketSectionState.OrderSide.BUY ? 0xFF62D478 : 0xFFE56A64;
        draw(font, (side == TerminalMarketSectionState.OrderSide.BUY ? "买入 " : "卖出 ") + productName,
            x, b.getY() + 12, color, b.getWidth() - 24);
        draw(font, "盘口参考 " + marketPrice, x, b.getY() + 28, 0xFFBFCBDA, b.getWidth() - 24);
        long quantity = parse(quantityText());
        long price = orderType == TerminalMarketSectionState.OrderType.MARKET ? parse(marketPrice) : parse(priceText());
        draw(font, "预估 " + saturatedMultiply(quantity, price) + "  " + accountSummary,
            x, b.getY() + 96, 0xFFF0C75E, b.getWidth() - 24);
        draw(font, "价格、手续费与可用资产由服务端最终复核", x, b.getY() + 110,
            0xFF718396, b.getWidth() - 24);
    }

    private void switchType(TerminalMarketSectionState.OrderType value) {
        orderType = value;
        state.setOrderType(value);
        if (value == TerminalMarketSectionState.OrderType.LIMIT && priceText().isEmpty()) {
            setPriceText(marketPrice);
        }
        focus(true);
        setBounds(getBounds());
    }

    private void seedDraft() {
        state.setOrderSide(side);
        state.setOrderType(orderType);
        if (quantityText().isEmpty()) { setQuantityText("1"); }
        if (orderType == TerminalMarketSectionState.OrderType.LIMIT && priceText().isEmpty()) {
            setPriceText(marketPrice);
        }
    }

    private void focus(boolean quantity) {
        quantityFocused = quantity;
        priceFocused = !quantity;
    }

    private String quantityText() {
        if (side == TerminalMarketSectionState.OrderSide.BUY) {
            return orderType == TerminalMarketSectionState.OrderType.MARKET
                ? state.getInstantBuyQuantityText() : state.getLimitBuyQuantityText();
        }
        return orderType == TerminalMarketSectionState.OrderType.MARKET
            ? state.getInstantSellQuantityText() : state.getLimitSellQuantityText();
    }

    private void setQuantityText(String value) {
        if (side == TerminalMarketSectionState.OrderSide.BUY) {
            if (orderType == TerminalMarketSectionState.OrderType.MARKET) state.setInstantBuyQuantityText(value);
            else state.setLimitBuyQuantityText(value);
        } else if (orderType == TerminalMarketSectionState.OrderType.MARKET) state.setInstantSellQuantityText(value);
        else state.setLimitSellQuantityText(value);
    }

    private String priceText() {
        return side == TerminalMarketSectionState.OrderSide.BUY
            ? state.getLimitBuyPriceText() : state.getLimitSellPriceText();
    }

    private void setPriceText(String value) {
        if (side == TerminalMarketSectionState.OrderSide.BUY) state.setLimitBuyPriceText(value);
        else state.setLimitSellPriceText(value);
    }

    private boolean isComplete() {
        return parse(quantityText()) > 0L
            && (orderType == TerminalMarketSectionState.OrderType.MARKET || parse(priceText()) > 0L);
    }

    void applyMaximumQuantity() {
        long maximum = maximumQuantity();
        if (maximum > 0L) { setQuantityText(String.valueOf(maximum)); }
    }

    long maximumQuantity() {
        if (side == TerminalMarketSectionState.OrderSide.SELL) { return availableAsset; }
        long price = orderType == TerminalMarketSectionState.OrderType.MARKET ? parse(marketPrice) : parse(priceText());
        return price <= 0L ? 0L : availableAsset / price;
    }

    void submit() {
        state.setOrderSide(side);
        state.setOrderType(orderType);
        if (isComplete() && submitAction != null) { submitAction.run(); }
    }

    private static String compactPrice(String value) {
        long parsed = parse(value);
        return parsed <= 0L ? "--" : String.valueOf(parsed);
    }

    private static long parse(String value) {
        if (value == null || value.isEmpty()) { return 0L; }
        StringBuilder digits = new StringBuilder();
        boolean started = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current >= '0' && current <= '9') {
                digits.append(current);
                started = true;
            } else if (started && current != ',') {
                break;
            }
        }
        if (digits.length() == 0) { return 0L; }
        try { return Long.parseLong(digits.toString()); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    private static long saturatedMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) { return 0L; }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static void draw(FontRenderer font, String text, int x, int y, int color, int width) {
        font.drawStringWithShadow(font.trimStringToWidth(text == null ? "" : text, Math.max(8, width)), x, y, color);
    }
}

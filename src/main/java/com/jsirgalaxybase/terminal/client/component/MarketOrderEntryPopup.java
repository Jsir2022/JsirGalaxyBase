package com.jsirgalaxybase.terminal.client.component;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

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
    private final String bidPrice;
    private final String lastPrice;
    private final String askPrice;
    private final String accountSummary;
    private final String assetRoute;
    private final long availableAsset;
    private final Runnable submitAction;
    private TerminalMarketSectionState.OrderType orderType;
    private boolean quantityFocused;
    private boolean priceFocused;

    private final ButtonPanel marketButton;
    private final ButtonPanel limitButton;
    private final TerminalTextFieldPanel quantityField;
    private final ButtonPanel quarterButton;
    private final ButtonPanel halfButton;
    private final ButtonPanel maximumButton;
    private final TerminalTextFieldPanel priceField;
    private final ButtonPanel bidButton;
    private final ButtonPanel lastButton;
    private final ButtonPanel askButton;
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
        this(screenWidth, screenHeight, state, side, initialType, productName, marketPrice, marketPrice, marketPrice,
            accountSummary, "个人 Base Vault -> 市场", availableAsset, submitAction, cancelAction);
    }

    public MarketOrderEntryPopup(int screenWidth, int screenHeight, TerminalMarketSectionState state,
        TerminalMarketSectionState.OrderSide side, TerminalMarketSectionState.OrderType initialType,
        String productName, String bidPrice, String lastPrice, String askPrice, String accountSummary,
        String assetRoute, long availableAsset, Runnable submitAction, Runnable cancelAction) {
        this.state = state;
        this.side = side == null ? TerminalMarketSectionState.OrderSide.BUY : side;
        this.orderType = initialType == null ? TerminalMarketSectionState.OrderType.MARKET : initialType;
        this.productName = productName == null ? "--" : productName;
        this.bidPrice = compactPrice(bidPrice);
        this.lastPrice = compactPrice(lastPrice);
        this.askPrice = compactPrice(askPrice);
        this.marketPrice = this.side == TerminalMarketSectionState.OrderSide.BUY ? this.askPrice : this.bidPrice;
        this.accountSummary = accountSummary == null ? "" : accountSummary;
        this.assetRoute = assetRoute == null ? "" : assetRoute;
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
        quarterButton = new ButtonPanel(() -> "25%", () -> applyQuantityFraction(1, 4),
            () -> Boolean.valueOf(maximumQuantity() > 0L), 0.78F);
        halfButton = new ButtonPanel(() -> "50%", () -> applyQuantityFraction(1, 2),
            () -> Boolean.valueOf(maximumQuantity() > 0L), 0.78F);
        maximumButton = new ButtonPanel(() -> "最大", this::applyMaximumQuantity,
            () -> Boolean.valueOf(maximumQuantity() > 0L), 0.82F);
        priceField = new TerminalTextFieldPanel(this::priceText, this::setPriceText,
            () -> Boolean.valueOf(priceFocused), () -> focus(false), "限价", 18,
            value -> Boolean.valueOf(value.charValue() >= '0' && value.charValue() <= '9'));
        bidButton = new ButtonPanel(() -> "买一", () -> applyPrice(bidPrice),
            () -> Boolean.valueOf(parse(bidPrice) > 0L), 0.76F);
        lastButton = new ButtonPanel(() -> "最新", () -> applyPrice(lastPrice),
            () -> Boolean.valueOf(parse(lastPrice) > 0L), 0.76F);
        askButton = new ButtonPanel(() -> "卖一", () -> applyPrice(askPrice),
            () -> Boolean.valueOf(parse(askPrice) > 0L), 0.76F);
        submitButton = new ButtonPanel(() -> this.side == TerminalMarketSectionState.OrderSide.BUY ? "确认买入" : "确认卖出",
            () -> submit(), () -> Boolean.valueOf(isComplete()), 0.88F);
        cancelButton = new ButtonPanel(() -> "取消", cancelAction, () -> Boolean.TRUE, 0.88F);
        addChild(marketButton);
        addChild(limitButton);
        addChild(quantityField);
        addChild(quarterButton);
        addChild(halfButton);
        addChild(maximumButton);
        addChild(priceField);
        addChild(bidButton);
        addChild(lastButton);
        addChild(askButton);
        addChild(submitButton);
        addChild(cancelButton);

        int popupWidth = Math.min(320, Math.max(264, screenWidth - 104));
        int popupHeight = Math.min(220, Math.max(198, screenHeight - 88));
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
        marketButton.setBounds(new GuiRect(x, bounds.getY() + 50, half, 16));
        limitButton.setBounds(new GuiRect(x + half + gap, bounds.getY() + 50, width - half - gap, 16));
        quantityField.setBounds(new GuiRect(x, bounds.getY() + 77, half, 16));
        int shortcutWidth = Math.max(28, (half - gap * 2) / 3);
        quarterButton.setBounds(new GuiRect(x, bounds.getY() + 97, shortcutWidth, 15));
        halfButton.setBounds(new GuiRect(x + shortcutWidth + gap, bounds.getY() + 97, shortcutWidth, 15));
        maximumButton.setBounds(new GuiRect(x + (shortcutWidth + gap) * 2, bounds.getY() + 97,
            Math.max(0, half - (shortcutWidth + gap) * 2), 15));
        priceField.setVisible(orderType == TerminalMarketSectionState.OrderType.LIMIT);
        priceField.setBounds(orderType == TerminalMarketSectionState.OrderType.LIMIT
            ? new GuiRect(x + half + gap, bounds.getY() + 77, width - half - gap, 16)
            : new GuiRect(0, 0, 0, 0));
        int priceShortcutX = x + half + gap;
        int priceShortcutWidth = Math.max(24, (width - half - gap * 3) / 3);
        boolean limit = orderType == TerminalMarketSectionState.OrderType.LIMIT;
        bidButton.setVisible(limit);
        lastButton.setVisible(limit);
        askButton.setVisible(limit);
        bidButton.setBounds(new GuiRect(priceShortcutX, bounds.getY() + 97, priceShortcutWidth, 15));
        lastButton.setBounds(new GuiRect(priceShortcutX + priceShortcutWidth + gap, bounds.getY() + 97,
            priceShortcutWidth, 15));
        askButton.setBounds(new GuiRect(priceShortcutX + (priceShortcutWidth + gap) * 2, bounds.getY() + 97,
            Math.max(0, width - half - gap - (priceShortcutWidth + gap) * 2), 15));
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
        int right = b.getRight() - 12;
        int color = side == TerminalMarketSectionState.OrderSide.BUY ? 0xFF62D478 : 0xFFE56A64;
        Gui.drawRect(x - 4, b.getY() + 8, right + 4, b.getY() + 42, 0xAA17232E);
        Gui.drawRect(x - 4, b.getY() + 70, right + 4, b.getY() + 116, 0x66121C25);
        Gui.drawRect(x - 4, b.getY() + 120, right + 4, b.getBottom() - 35, 0x66121C25);
        Gui.drawRect(x - 4, b.getY() + 8, x - 1, b.getY() + 42, color);
        draw(font, (side == TerminalMarketSectionState.OrderSide.BUY ? "买入 " : "卖出 ") + productName,
            x, b.getY() + 12, color, b.getWidth() - 24);
        draw(font, "买一 " + bidPrice + "  最新 " + lastPrice + "  卖一 " + askPrice,
            x, b.getY() + 28, 0xFFBFCBDA, b.getWidth() - 24);
        draw(font, "订单参数", x, b.getY() + 68, 0xFF71879B, b.getWidth() - 24);
        long quantity = parse(quantityText());
        long price = orderType == TerminalMarketSectionState.OrderType.MARKET ? parse(marketPrice) : parse(priceText());
        draw(font, "结算预览", x, b.getY() + 118, 0xFF71879B, b.getWidth() - 24);
        draw(font, "预估总额 " + saturatedMultiply(quantity, price) + "  手续费: 服务端复核",
            x, b.getY() + 132, 0xFFF0C75E, b.getWidth() - 24);
        draw(font, accountSummary + (assetRoute.isEmpty() ? "" : "  " + assetRoute),
            x, b.getY() + 146, 0xFFBFCBDA, b.getWidth() - 24);
        String reason = disabledReason();
        draw(font, reason.isEmpty() ? "提交后由服务端重新核验价格、余额与库存" : reason,
            x, b.getY() + 160, reason.isEmpty() ? 0xFF718396 : 0xFFE56A64, b.getWidth() - 24);
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
        return disabledReason().isEmpty();
    }

    void applyMaximumQuantity() {
        long maximum = maximumQuantity();
        if (maximum > 0L) { setQuantityText(String.valueOf(maximum)); }
    }

    void applyQuantityFraction(int numerator, int denominator) {
        long maximum = maximumQuantity();
        if (maximum <= 0L || numerator <= 0 || denominator <= 0) return;
        long value = Math.max(1L, maximum / denominator * numerator);
        setQuantityText(String.valueOf(Math.min(maximum, value)));
    }

    void applyPrice(String value) {
        if (orderType == TerminalMarketSectionState.OrderType.LIMIT && parse(value) > 0L) {
            setPriceText(compactPrice(value));
            focus(false);
        }
    }

    String disabledReason() {
        if (parse(quantityText()) <= 0L) return "请输入大于 0 的数量";
        if (orderType == TerminalMarketSectionState.OrderType.LIMIT && parse(priceText()) <= 0L) {
            return "请输入限价，或选择买一 / 最新 / 卖一";
        }
        if (maximumQuantity() <= 0L) {
            return side == TerminalMarketSectionState.OrderSide.BUY ? "银行可用余额不足" : "账户仓可卖库存不足";
        }
        if (parse(quantityText()) > maximumQuantity()) {
            return side == TerminalMarketSectionState.OrderSide.BUY ? "数量超过可用余额" : "数量超过账户仓可卖库存";
        }
        return "";
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

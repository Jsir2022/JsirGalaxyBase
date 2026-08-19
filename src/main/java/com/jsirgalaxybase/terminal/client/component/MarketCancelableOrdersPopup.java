package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.ModalPopupPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

/** Current-product, read-only order selector. Cancellation still requires a second confirmation. */
public final class MarketCancelableOrdersPopup extends ModalPopupPanel {

    public interface Handler {
        void select(String orderId);
        void close();
    }

    private final String productName;
    private final List<Row> rows;
    private final List<ButtonPanel> cancelButtons = new ArrayList<ButtonPanel>();
    private final ButtonPanel closeButton;
    private int scrollOffset;

    public MarketCancelableOrdersPopup(int screenWidth, int screenHeight, TerminalMarketSectionModel model,
        final Handler handler) {
        this(new GuiRect(0, 0, screenWidth, screenHeight), model, handler);
    }

    public MarketCancelableOrdersPopup(GuiRect terminalBounds, TerminalMarketSectionModel model,
        final Handler handler) {
        productName = TerminalMarketVisuals.resolveSelectedProductName(model);
        rows = buildRows(model);
        GuiRect host = terminalBounds == null ? new GuiRect(0, 0, 1, 1) : terminalBounds;
        int inset = Math.min(10, Math.max(3, Math.min(host.getWidth(), host.getHeight()) / 30));
        int availableWidth = Math.max(1, host.getWidth() - inset * 2);
        int availableHeight = Math.max(1, host.getHeight() - inset * 2);
        int popupWidth = Math.min(560, Math.max(Math.min(360, availableWidth), availableWidth * 9 / 10));
        int desiredRows = Math.min(5, Math.max(1, rows.size()));
        int popupHeight = Math.min(availableHeight, Math.max(Math.min(154, availableHeight), 106 + desiredRows * 32));
        setBounds(new GuiRect(host.getX() + (host.getWidth() - popupWidth) / 2,
            host.getY() + (host.getHeight() - popupHeight) / 2, popupWidth, popupHeight));
        for (final Row row : rows) {
            ButtonPanel button = new ButtonPanel(() -> "撤单", () -> {
                if (handler != null) handler.select(row.orderId);
            }, () -> Boolean.TRUE, 0.86F);
            cancelButtons.add(button);
            addChild(button);
        }
        closeButton = new ButtonPanel(() -> "关闭", () -> { if (handler != null) handler.close(); }, null, 0.86F);
        addChild(closeButton);
        setBounds(getBounds());
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        if (bounds == null || closeButton == null) return;
        int rowY = bounds.getY() + 66;
        int rowHeight = 32;
        int visibleRows = visibleRowCount(bounds);
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, rows.size() - visibleRows)));
        for (int index = 0; index < cancelButtons.size(); index++) {
            boolean visible = index >= scrollOffset && index < scrollOffset + visibleRows;
            cancelButtons.get(index).setVisible(visible);
            cancelButtons.get(index).setBounds(new GuiRect(bounds.getRight() - 58,
                rowY + (index - scrollOffset) * rowHeight + 6, 44, 18));
        }
        closeButton.setBounds(new GuiRect(bounds.getRight() - 92, bounds.getBottom() - 28, 76, 18));
    }

    @Override
    protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        super.drawSelf(scene, mouseX, mouseY, partialTicks);
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRenderer == null) return;
        GuiRect bounds = getBounds();
        int primary = scene.getTheme().color(ThemeColorKey.TEXT_PRIMARY);
        int secondary = scene.getTheme().color(ThemeColorKey.TEXT_SECONDARY);
        minecraft.fontRenderer.drawStringWithShadow(productName + " · 当前委托", bounds.getX() + 14,
            bounds.getY() + 12, primary);
        minecraft.fontRenderer.drawString("仅列出 OPEN / PARTIALLY_FILLED 且剩余量大于 0 的本人委托",
            bounds.getX() + 14, bounds.getY() + 29, secondary);
        minecraft.fontRenderer.drawString("方向  类型  委托价       成交/总量       剩余       时间",
            bounds.getX() + 14, bounds.getY() + 49, secondary);
        if (rows.isEmpty()) {
            minecraft.fontRenderer.drawString("当前商品没有可撤委托。", bounds.getX() + 18,
                bounds.getY() + 78, secondary);
            return;
        }
        int rowY = bounds.getY() + 66;
        int visibleRows = visibleRowCount(bounds);
        for (int visible = 0; visible < visibleRows && scrollOffset + visible < rows.size(); visible++) {
            int index = scrollOffset + visible;
            Row row = rows.get(index);
            int y = rowY + visible * 32;
            Gui.drawRect(bounds.getX() + 12, y, bounds.getRight() - 12, y + 28,
                index % 2 == 0 ? 0xFF111922 : 0xFF151F29);
            minecraft.fontRenderer.drawString(trim(row.side + "  限价  " + row.price + "  " + row.filled
                + "/" + row.total + "  " + row.remaining + "  " + row.time, bounds.getWidth() - 92),
                bounds.getX() + 18, y + 10, primary);
        }
        if (rows.size() > visibleRows) {
            minecraft.fontRenderer.drawString((scrollOffset + 1) + "-"
                + Math.min(rows.size(), scrollOffset + visibleRows) + " / " + rows.size(),
                bounds.getX() + 16, bounds.getBottom() - 23, secondary);
        }
    }

    @Override
    protected boolean onContainerScrolled(GuiScene scene, int mouseX, int mouseY, int wheelDelta) {
        GuiRect bounds = getBounds();
        GuiRect table = new GuiRect(bounds.getX() + 12, bounds.getY() + 66,
            Math.max(0, bounds.getWidth() - 24), Math.max(0, bounds.getHeight() - 104));
        if (!table.contains(mouseX, mouseY) || rows.size() <= visibleRowCount(bounds)) return false;
        int next = scrollOffset + (wheelDelta < 0 ? 1 : -1);
        scrollOffset = Math.max(0, Math.min(next, rows.size() - visibleRowCount(bounds)));
        setBounds(bounds);
        return true;
    }

    private int visibleRowCount(GuiRect bounds) {
        return Math.max(1, Math.max(0, bounds.getHeight() - 104) / 32);
    }

    private String trim(String value, int width) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft == null || minecraft.fontRenderer == null ? value
            : minecraft.fontRenderer.trimStringToWidth(value, Math.max(40, width));
    }

    private static List<Row> buildRows(TerminalMarketSectionModel model) {
        if (model == null) return Collections.emptyList();
        List<Row> result = new ArrayList<Row>();
        for (int index = 0; index < model.getMyOrderLines().size() && index < model.getMyOrderIds().size(); index++) {
            if (index >= model.getMyOrderCancelableFlags().size()
                || !"1".equals(model.getMyOrderCancelableFlags().get(index))) continue;
            String id = safe(model.getMyOrderIds().get(index));
            String[] parts = safe(model.getMyOrderLines().get(index)).split("\\|");
            if (id.isEmpty() || parts.length < 9) continue;
            String status = part(parts, 7).toUpperCase(java.util.Locale.ROOT);
            long remaining = number(part(parts, 6));
            if (remaining <= 0L || !("OPEN".equals(status) || "PARTIALLY_FILLED".equals(status))) continue;
            result.add(new Row(id, "BUY".equalsIgnoreCase(part(parts, 2)) ? "买入" : "卖出",
                label(part(parts, 3)), label(part(parts, 5)), label(part(parts, 4)),
                label(part(parts, 6)), part(parts, 8)));
        }
        return Collections.unmodifiableList(result);
    }

    private static String part(String[] values, int index) {
        return index < values.length ? safe(values[index]) : "";
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private static String label(String value) {
        String normalized = safe(value);
        int separator = normalized.indexOf(' ');
        return separator < 0 ? normalized : normalized.substring(separator + 1).trim();
    }
    private static long number(String value) {
        try { return Long.parseLong(label(value).replace(",", "")); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    private static final class Row {
        final String orderId, side, price, filled, total, remaining, time;
        Row(String orderId, String side, String price, String filled, String total, String remaining, String time) {
            this.orderId = orderId; this.side = side; this.price = price; this.filled = filled;
            this.total = total; this.remaining = remaining; this.time = time;
        }
    }
}

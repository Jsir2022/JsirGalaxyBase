package com.jsirgalaxybase.terminal.client.ui2;

import java.util.Collections;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiDocument;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.render.DrawList;

/** Common contract for documents hosted by the non-Container terminal application. */
public interface TerminalPageDocument extends UiDocument {
    void update(TerminalHomeScreenModel model);
    void openHelp();
    default boolean refresh() { return false; }
    default void tick() {}
    default Map<String, ItemStack> externalItems() { return Collections.emptyMap(); }
    default void drawExternal(DrawList list, int mouseX, int mouseY, float partialTicks) {}
    default boolean externalPointer(UiEvent.Type type, int x, int y, int button) { return false; }
    default boolean externalScroll(int x, int y, int delta) { return false; }
    default boolean externalKey(char typedChar, int keyCode) { return false; }
    default void close() {}
}

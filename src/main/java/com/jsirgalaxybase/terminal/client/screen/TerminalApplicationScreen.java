package com.jsirgalaxybase.terminal.client.screen;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.client.ui2.host.NativeItemBridge;
import com.jsirgalaxybase.client.ui2.render.GlStateGuard;

import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalBankActionPayload;
import com.jsirgalaxybase.terminal.TerminalServerToolsActionPayload;
import com.jsirgalaxybase.terminal.TerminalLandActionPayload;
import com.jsirgalaxybase.terminal.TerminalQuestActionPayload;
import com.jsirgalaxybase.terminal.TerminalHudOverlayHandler;
import com.jsirgalaxybase.terminal.client.TerminalRouteCoordinator;
import com.jsirgalaxybase.terminal.client.ui2.TerminalPageDocument;
import com.jsirgalaxybase.terminal.client.ui2.TerminalPageRegistry;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;
import com.jsirgalaxybase.ui2.core.UiDocument;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.render.DrawList;

/** One stable UI2 host for all non-Container terminal routes migrated to UI2. */
public final class TerminalApplicationScreen extends TerminalScreenV2Base {
    private final NativeItemBridge itemBridge = new NativeItemBridge();
    private TerminalPageDocument document;
    private String documentPageId;

    public TerminalApplicationScreen(GuiScreen parent, TerminalHomeScreenModel model) {
        super(parent, model);
    }

    public void applyModel(TerminalHomeScreenModel model, long requestSequence) {
        String previousPage = documentPageId;
        updateShellModel(model);
        String nextPage = shellModel().getSelectedPageId();
        if (document == null || !nextPage.equals(previousPage)) {
            initGui();
            return;
        }
        document.update(shellModel());
        invalidateUi();
    }

    @Override protected UiDocument createDocument() {
        documentPageId = shellModel().getSelectedPageId();
        if (document != null) document.close();
        document = TerminalPageRegistry.create(documentPageId, shellModel(), shellActions(),
            new TerminalPageRegistry.BusinessActions() {
                @Override public void open(String pageId, String recordId) {
                    TerminalRouteCoordinator.openTerminalPage(pageId);
                }
                @Override public void openAccount(TerminalBankActionPayload payload) {
                    sendBankAction(TerminalActionType.BANK_OPEN_ACCOUNT, payload);
                }
                @Override public void confirmTransfer(TerminalBankActionPayload payload) {
                    sendBankAction(TerminalActionType.BANK_CONFIRM_TRANSFER, payload);
                }
                @Override public void send(TerminalActionType action, TerminalServerToolsActionPayload payload) {
                    sendServerToolsAction(action, payload);
                }
                @Override public void sendMarket(TerminalActionType action, String payload) {
                    TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
                        shellModel().getSelectedPageId(), action.getId(), payload));
                }
                @Override public void sendLand(TerminalActionType action, TerminalLandActionPayload payload) {
                    TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
                        com.jsirgalaxybase.terminal.ui.TerminalPage.PROPERTY.getId(), action.getId(),
                        (payload == null ? TerminalLandActionPayload.empty() : payload).encode()));
                }
                @Override public void sendQuest(TerminalActionType action, TerminalQuestActionPayload payload) {
                    TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
                        com.jsirgalaxybase.terminal.ui.TerminalPage.CAREER.getId(), action.getId(),
                        (payload == null ? TerminalQuestActionPayload.empty() : payload).encode()));
                }
                @Override public void sendQuestEdit(com.jsirgalaxybase.terminal.TerminalQuestDraftEditPayload payload) {
                    if(payload==null)return;
                    TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
                        com.jsirgalaxybase.terminal.ui.TerminalPage.CAREER.getId(),TerminalActionType.QUEST_ADMIN_EDIT.getId(),payload.encode()));
                }
                @Override public void sendQuestBatchRetire(com.jsirgalaxybase.terminal.TerminalQuestBatchRetirePayload payload) {
                    if(payload==null)return;
                    TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
                        com.jsirgalaxybase.terminal.ui.TerminalPage.CAREER.getId(),
                        TerminalActionType.QUEST_ADMIN_BATCH_RETIRE.getId(),payload.encode()));
                }
                @Override public void sendQuestChapterEdit(com.jsirgalaxybase.terminal.TerminalQuestChapterEditPayload payload) {
                    if(payload==null)return;
                    TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
                        com.jsirgalaxybase.terminal.ui.TerminalPage.CAREER.getId(),
                        TerminalActionType.QUEST_CHAPTER_ADMIN_EDIT.getId(),payload.encode()));
                }
                @Override public void sendQuestChapterClone(com.jsirgalaxybase.terminal.TerminalQuestChapterClonePayload payload) {
                    if(payload==null)return;
                    TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
                        com.jsirgalaxybase.terminal.ui.TerminalPage.CAREER.getId(),
                        TerminalActionType.QUEST_CHAPTER_ADMIN_CLONE.getId(),payload.encode()));
                }
                @Override public void sendQuestChapterAlignment(com.jsirgalaxybase.terminal.TerminalQuestChapterAlignmentPayload payload) {
                    if(payload==null)return;
                    TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
                        com.jsirgalaxybase.terminal.ui.TerminalPage.CAREER.getId(),
                        TerminalActionType.QUEST_CHAPTER_ADMIN_ALIGN.getId(),payload.encode()));
                }
                @Override public void sendQuestChapterOrder(com.jsirgalaxybase.terminal.TerminalQuestChapterOrderPayload payload) {
                    if(payload==null)return;
                    TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
                        com.jsirgalaxybase.terminal.ui.TerminalPage.CAREER.getId(),
                        TerminalActionType.QUEST_CHAPTER_ADMIN_MOVE_BEFORE.getId(),payload.encode()));
                }
            }, new TerminalPageRegistry.Invalidation() {
                @Override public void invalidate() { invalidateUi(); }
            });
        return document;
    }

    private void sendBankAction(TerminalActionType action, TerminalBankActionPayload payload) {
        TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
            shellModel().getSelectedPageId(), action.getId(),
            (payload == null ? TerminalBankActionPayload.empty() : payload).encode()));
    }

    private void sendServerToolsAction(TerminalActionType action, TerminalServerToolsActionPayload payload) {
        TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
            com.jsirgalaxybase.terminal.ui.TerminalPage.SERVER_TOOLS.getId(), action.getId(),
            (payload == null ? TerminalServerToolsActionPayload.empty() : payload).encode()));
    }

    @Override protected void refreshPage() {
        if (document != null && document.refresh()) return;
        if (com.jsirgalaxybase.terminal.ui.TerminalPage.fromId(shellModel().getSelectedPageId()).isBankPage()) {
            sendBankAction(TerminalActionType.BANK_REFRESH, TerminalBankActionPayload.empty());
            return;
        }
        if (com.jsirgalaxybase.terminal.ui.TerminalPage.SERVER_TOOLS.getId()
            .equals(shellModel().getSelectedPageId())) {
            sendServerToolsAction(TerminalActionType.SERVER_TOOLS_REFRESH, TerminalServerToolsActionPayload.empty());
            return;
        }
        TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel().getSessionToken(),
            shellModel().getSelectedPageId(), TerminalActionType.REFRESH_PAGE.getId(), "manual_refresh"));
    }

    @Override protected void showHelp() {
        if (document != null) {
            document.openHelp();
            invalidateUi();
        }
    }

    @Override public void updateScreen() {
        super.updateScreen();
        if (document != null) document.tick();
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (document != null && !getUiRuntime().hasModal()) {
            net.minecraft.item.ItemStack hovered = itemBridge.hovered(currentDrawList(),
                document.externalItems(), mouseX, mouseY);
            if (hovered != null) renderToolTip(hovered, mouseX, mouseY);
        }
        TerminalHudOverlayHandler.INSTANCE.drawTerminalNotifications(fontRendererObj, width, height);
    }

    @Override protected void drawExternalContent(DrawList list, int mouseX, int mouseY, float partialTicks) {
        if (document == null) return;
        // Charts and other UI-owned external surfaces must not inherit the fixed-function
        // lighting/colour state used by RenderItem. Draw them first and isolate both stages.
        try (GlStateGuard ignored = new GlStateGuard()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            document.drawExternal(list, mouseX, mouseY, partialTicks);
        }
        try (GlStateGuard ignored = new GlStateGuard()) {
            itemBridge.draw(list, document.externalItems(), itemRender, fontRendererObj, mc.getTextureManager());
        }
    }

    @Override protected boolean handleExternalPointer(UiEvent.Type type, int x, int y, int button) {
        return document != null && document.externalPointer(type, x, y, button);
    }

    @Override protected boolean handleExternalScroll(int x, int y, int delta) {
        return document != null && document.externalScroll(x, y, delta);
    }

    @Override protected boolean handleExternalKey(char typedChar, int keyCode) {
        return document != null && document.externalKey(typedChar, keyCode);
    }

    @Override public void onGuiClosed() {
        if (document != null) document.close();
        super.onGuiClosed();
    }
}

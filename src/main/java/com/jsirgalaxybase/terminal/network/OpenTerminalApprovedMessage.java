package com.jsirgalaxybase.terminal.network;

import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.terminal.TerminalBankSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalCustomMarketSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalExchangeMarketSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalMarketSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalOpenApproval;
import com.jsirgalaxybase.terminal.client.TerminalClientScreenController;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class OpenTerminalApprovedMessage implements IMessage {

    private String selectedPageId;
    private String terminalTitle;
    private String terminalSubtitle;
    private String statusEyebrow;
    private String statusHeadline;
    private String statusDetail;
    private String statusBadgeLabel;
    private String statusBadgeValue;
    private List<TerminalHomeScreenModel.NavItemModel> navItems = new ArrayList<TerminalHomeScreenModel.NavItemModel>();
    private List<TerminalHomeScreenModel.PageSnapshotModel> pageSnapshots = new ArrayList<TerminalHomeScreenModel.PageSnapshotModel>();
    private List<TerminalHomeScreenModel.NotificationModel> notifications = new ArrayList<TerminalHomeScreenModel.NotificationModel>();
    private String sessionToken;

    public OpenTerminalApprovedMessage() {}

    public OpenTerminalApprovedMessage(TerminalOpenApproval approval) {
        this(
            approval.getSelectedPageId(),
            approval.getTerminalTitle(),
            approval.getTerminalSubtitle(),
            approval.getStatusBand().getEyebrow(),
            approval.getStatusBand().getHeadline(),
            approval.getStatusBand().getDetail(),
            approval.getStatusBand().getBadgeLabel(),
            approval.getStatusBand().getBadgeValue(),
            toNavItemModels(approval.getNavItems()),
            toPageSnapshotModels(approval.getPageSnapshots()),
            toNotificationModels(approval.getNotifications()),
            approval.getSessionToken());
    }

    public OpenTerminalApprovedMessage(String selectedPageId, String terminalTitle, String terminalSubtitle,
        String statusEyebrow, String statusHeadline, String statusDetail, String statusBadgeLabel,
        String statusBadgeValue, List<TerminalHomeScreenModel.NavItemModel> navItems,
        List<TerminalHomeScreenModel.PageSnapshotModel> pageSnapshots,
        List<TerminalHomeScreenModel.NotificationModel> notifications, String sessionToken) {
        this.selectedPageId = selectedPageId;
        this.terminalTitle = terminalTitle;
        this.terminalSubtitle = terminalSubtitle;
        this.statusEyebrow = statusEyebrow;
        this.statusHeadline = statusHeadline;
        this.statusDetail = statusDetail;
        this.statusBadgeLabel = statusBadgeLabel;
        this.statusBadgeValue = statusBadgeValue;
        this.navItems = navItems == null ? new ArrayList<TerminalHomeScreenModel.NavItemModel>() : navItems;
        this.pageSnapshots = pageSnapshots == null ? new ArrayList<TerminalHomeScreenModel.PageSnapshotModel>() : pageSnapshots;
        this.notifications = notifications == null ? new ArrayList<TerminalHomeScreenModel.NotificationModel>() : notifications;
        this.sessionToken = sessionToken;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        selectedPageId = ByteBufUtils.readUTF8String(buf);
        terminalTitle = ByteBufUtils.readUTF8String(buf);
        terminalSubtitle = ByteBufUtils.readUTF8String(buf);
        statusEyebrow = ByteBufUtils.readUTF8String(buf);
        statusHeadline = ByteBufUtils.readUTF8String(buf);
        statusDetail = ByteBufUtils.readUTF8String(buf);
        statusBadgeLabel = ByteBufUtils.readUTF8String(buf);
        statusBadgeValue = ByteBufUtils.readUTF8String(buf);
        navItems = readNavItems(buf);
        pageSnapshots = readPageSnapshots(buf);
        notifications = readNotifications(buf);
        sessionToken = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, safe(selectedPageId));
        ByteBufUtils.writeUTF8String(buf, safe(terminalTitle));
        ByteBufUtils.writeUTF8String(buf, safe(terminalSubtitle));
        ByteBufUtils.writeUTF8String(buf, safe(statusEyebrow));
        ByteBufUtils.writeUTF8String(buf, safe(statusHeadline));
        ByteBufUtils.writeUTF8String(buf, safe(statusDetail));
        ByteBufUtils.writeUTF8String(buf, safe(statusBadgeLabel));
        ByteBufUtils.writeUTF8String(buf, safe(statusBadgeValue));
        writeNavItems(buf, navItems);
        writePageSnapshots(buf, pageSnapshots);
        writeNotifications(buf, notifications);
        ByteBufUtils.writeUTF8String(buf, safe(sessionToken));
    }

    public TerminalHomeScreenModel toScreenModel() {
        return new TerminalHomeScreenModel(
            selectedPageId,
            terminalTitle,
            terminalSubtitle,
            new TerminalHomeScreenModel.StatusBandModel(
                statusEyebrow,
                statusHeadline,
                statusDetail,
                statusBadgeLabel,
                statusBadgeValue),
            navItems,
            pageSnapshots,
            notifications,
            sessionToken);
    }

    static void writePageSnapshots(ByteBuf buf, List<TerminalHomeScreenModel.PageSnapshotModel> snapshots) {
        List<TerminalHomeScreenModel.PageSnapshotModel> safeItems = snapshots == null
            ? new ArrayList<TerminalHomeScreenModel.PageSnapshotModel>() : snapshots;
        buf.writeInt(safeItems.size());
        for (TerminalHomeScreenModel.PageSnapshotModel snapshot : safeItems) {
            ByteBufUtils.writeUTF8String(buf, safe(snapshot.getPageId()));
            ByteBufUtils.writeUTF8String(buf, safe(snapshot.getTitle()));
            ByteBufUtils.writeUTF8String(buf, safe(snapshot.getLead()));
            writeSections(buf, snapshot.getSections());
            writeBankSection(buf, snapshot.getBankSectionModel());
            writeMarketSection(buf, snapshot.getMarketSectionModel());
            writeCustomMarketSection(buf, snapshot.getCustomMarketSectionModel());
            writeExchangeMarketSection(buf, snapshot.getExchangeMarketSectionModel());
        }
    }

    static List<TerminalHomeScreenModel.PageSnapshotModel> readPageSnapshots(ByteBuf buf) {
        int size = buf.readInt();
        List<TerminalHomeScreenModel.PageSnapshotModel> items = new ArrayList<TerminalHomeScreenModel.PageSnapshotModel>(size);
        for (int i = 0; i < size; i++) {
            items.add(new TerminalHomeScreenModel.PageSnapshotModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                readSections(buf),
                readBankSection(buf),
                readMarketSection(buf),
                readCustomMarketSection(buf),
                readExchangeMarketSection(buf)));
        }
        return items;
    }

    static void writeCustomMarketSection(ByteBuf buf, TerminalCustomMarketSectionModel model) {
        buf.writeBoolean(model != null);
        if (model == null) {
            return;
        }
        ByteBufUtils.writeUTF8String(buf, safe(model.getServiceState()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getBrowserHint()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getScopeLabel()));
        writeStringList(buf, model.getActiveListingLines());
        writeStringList(buf, model.getActiveListingIds());
        writeStringList(buf, model.getSellingListingLines());
        writeStringList(buf, model.getSellingListingIds());
        writeStringList(buf, model.getPendingListingLines());
        writeStringList(buf, model.getPendingListingIds());
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedListingId()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedPrice()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedCounterparty()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedItemIdentity()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTradeSummary()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedActionHint()));
        buf.writeBoolean(model.isCanBuy());
        buf.writeBoolean(model.isCanCancel());
        buf.writeBoolean(model.isCanClaim());
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getBody()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getSeverityName()));
    }

    static TerminalCustomMarketSectionModel readCustomMarketSection(ByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        return new TerminalCustomMarketSectionModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            new TerminalCustomMarketSectionModel.ActionFeedbackModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)));
    }

    static void writeExchangeMarketSection(ByteBuf buf, TerminalExchangeMarketSectionModel model) {
        buf.writeBoolean(model != null);
        if (model == null) {
            return;
        }
        ByteBufUtils.writeUTF8String(buf, safe(model.getServiceState()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getBrowserHint()));
        writeStringList(buf, model.getTargetCodes());
        writeStringList(buf, model.getTargetLabels());
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTargetCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTargetTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTargetSummary()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getHeldSummary()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getInputRegistryName()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getPairCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getInputAssetCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getOutputAssetCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getRuleVersion()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getLimitStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getReasonCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getNotes()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getInputQuantity()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getNominalFaceValue()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getEffectiveExchangeValue()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getContributionValue()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getDiscountStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getRateDisplay()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getExecutionHint()));
        buf.writeBoolean(model.isExecutable());
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getBody()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getSeverityName()));
    }

    static TerminalExchangeMarketSectionModel readExchangeMarketSection(ByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        return new TerminalExchangeMarketSectionModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            readStringList(buf),
            readStringList(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean(),
            new TerminalExchangeMarketSectionModel.ActionFeedbackModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)));
    }

    static void writeMarketSection(ByteBuf buf, TerminalMarketSectionModel marketSectionModel) {
        buf.writeBoolean(marketSectionModel != null);
        if (marketSectionModel == null) {
            return;
        }

        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getRoutePageId()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getServiceState()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getBrowserHint()));
        writeStringList(buf, marketSectionModel.getProductKeys());
        writeStringList(buf, marketSectionModel.getProductLabels());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSelectedProductKey()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSelectedProductName()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSelectedProductUnit()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLatestTradePrice()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getHighestBid()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLowestAsk()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getVolume24h()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getTurnover24h()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSourceAvailable()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLockedEscrowQuantity()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getClaimableQuantity()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getFrozenFunds()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSummaryNotice()));
        writeStringList(buf, marketSectionModel.getAskLines());
        writeStringList(buf, marketSectionModel.getBidLines());
        writeStringList(buf, marketSectionModel.getMyOrderLines());
        writeStringList(buf, marketSectionModel.getClaimLines());
        writeStringList(buf, marketSectionModel.getClaimIds());
        writeStringList(buf, marketSectionModel.getRuleLines());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitBuyDraft().getSelectedProductKey()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitBuyDraft().getPriceText()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitBuyDraft().getQuantityText()));
        buf.writeBoolean(marketSectionModel.getLimitBuyDraft().isSubmitEnabled());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getActionFeedback().getTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getActionFeedback().getBody()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getActionFeedback().getSeverityName()));
    }

    static TerminalMarketSectionModel readMarketSection(ByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        return new TerminalMarketSectionModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            readStringList(buf),
            readStringList(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            new TerminalMarketSectionModel.LimitBuyDraftModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                buf.readBoolean()),
            new TerminalMarketSectionModel.ActionFeedbackModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)));
    }

    static void writeBankSection(ByteBuf buf, TerminalBankSectionModel bankSectionModel) {
        buf.writeBoolean(bankSectionModel != null);
        if (bankSectionModel == null) {
            return;
        }

        TerminalBankSectionModel.AccountStatusModel accountStatus = bankSectionModel.getAccountStatus();
        buf.writeBoolean(accountStatus.isOpened());
        ByteBufUtils.writeUTF8String(buf, safe(accountStatus.getServiceState()));
        ByteBufUtils.writeUTF8String(buf, safe(accountStatus.getAccountLabel()));
        ByteBufUtils.writeUTF8String(buf, safe(accountStatus.getPlayerStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(accountStatus.getAccountNo()));
        ByteBufUtils.writeUTF8String(buf, safe(accountStatus.getUpdatedAt()));
        buf.writeBoolean(accountStatus.isOpenAllowed());

        TerminalBankSectionModel.BalanceSummaryModel balanceSummary = bankSectionModel.getBalanceSummary();
        ByteBufUtils.writeUTF8String(buf, safe(balanceSummary.getPlayerBalance()));
        ByteBufUtils.writeUTF8String(buf, safe(balanceSummary.getExchangeBalance()));
        ByteBufUtils.writeUTF8String(buf, safe(balanceSummary.getExchangeStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(balanceSummary.getTransferHint()));
        buf.writeBoolean(balanceSummary.isTransferAllowed());

        TerminalBankSectionModel.TransferFormModel transferForm = bankSectionModel.getTransferForm();
        ByteBufUtils.writeUTF8String(buf, safe(transferForm.getTargetPlayerName()));
        ByteBufUtils.writeUTF8String(buf, safe(transferForm.getAmountText()));
        ByteBufUtils.writeUTF8String(buf, safe(transferForm.getComment()));
        buf.writeBoolean(transferForm.isTransferEnabled());

        TerminalBankSectionModel.ActionFeedbackModel actionFeedback = bankSectionModel.getActionFeedback();
        ByteBufUtils.writeUTF8String(buf, safe(actionFeedback.getTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(actionFeedback.getBody()));
        ByteBufUtils.writeUTF8String(buf, safe(actionFeedback.getSeverityName()));

        List<String> ledgerLines = bankSectionModel.getPlayerLedgerLines();
        buf.writeInt(ledgerLines.size());
        for (String ledgerLine : ledgerLines) {
            ByteBufUtils.writeUTF8String(buf, safe(ledgerLine));
        }
    }

    static void writeStringList(ByteBuf buf, List<String> values) {
        List<String> safeValues = values == null ? new ArrayList<String>() : values;
        buf.writeInt(safeValues.size());
        for (String value : safeValues) {
            ByteBufUtils.writeUTF8String(buf, safe(value));
        }
    }

    static List<String> readStringList(ByteBuf buf) {
        int size = buf.readInt();
        List<String> values = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            values.add(ByteBufUtils.readUTF8String(buf));
        }
        return values;
    }

    static TerminalBankSectionModel readBankSection(ByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }

        TerminalBankSectionModel.AccountStatusModel accountStatus = new TerminalBankSectionModel.AccountStatusModel(
            buf.readBoolean(),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean());
        TerminalBankSectionModel.BalanceSummaryModel balanceSummary = new TerminalBankSectionModel.BalanceSummaryModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean());
        TerminalBankSectionModel.TransferFormModel transferForm = new TerminalBankSectionModel.TransferFormModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean());
        TerminalBankSectionModel.ActionFeedbackModel actionFeedback = new TerminalBankSectionModel.ActionFeedbackModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf));
        int ledgerSize = buf.readInt();
        List<String> ledgerLines = new ArrayList<String>(ledgerSize);
        for (int i = 0; i < ledgerSize; i++) {
            ledgerLines.add(ByteBufUtils.readUTF8String(buf));
        }
        return new TerminalBankSectionModel(accountStatus, balanceSummary, transferForm, actionFeedback, ledgerLines);
    }

    static void writeNavItems(ByteBuf buf, List<TerminalHomeScreenModel.NavItemModel> items) {
        List<TerminalHomeScreenModel.NavItemModel> safeItems = items == null ? new ArrayList<TerminalHomeScreenModel.NavItemModel>() : items;
        buf.writeInt(safeItems.size());
        for (TerminalHomeScreenModel.NavItemModel item : safeItems) {
            ByteBufUtils.writeUTF8String(buf, safe(item.getPageId()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getLabel()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getSubtitle()));
            buf.writeBoolean(item.isEnabled());
            buf.writeBoolean(item.isSelected());
        }
    }

    static List<TerminalHomeScreenModel.NavItemModel> readNavItems(ByteBuf buf) {
        int size = buf.readInt();
        List<TerminalHomeScreenModel.NavItemModel> items = new ArrayList<TerminalHomeScreenModel.NavItemModel>(size);
        for (int i = 0; i < size; i++) {
            items.add(new TerminalHomeScreenModel.NavItemModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                buf.readBoolean(),
                buf.readBoolean()));
        }
        return items;
    }

    static void writeSections(ByteBuf buf, List<TerminalHomeScreenModel.SectionModel> sections) {
        List<TerminalHomeScreenModel.SectionModel> safeSections = sections == null
            ? new ArrayList<TerminalHomeScreenModel.SectionModel>() : sections;
        buf.writeInt(safeSections.size());
        for (TerminalHomeScreenModel.SectionModel section : safeSections) {
            ByteBufUtils.writeUTF8String(buf, safe(section.getSectionId()));
            ByteBufUtils.writeUTF8String(buf, safe(section.getTitle()));
            ByteBufUtils.writeUTF8String(buf, safe(section.getSummary()));
            ByteBufUtils.writeUTF8String(buf, safe(section.getDetail()));
        }
    }

    static List<TerminalHomeScreenModel.SectionModel> readSections(ByteBuf buf) {
        int size = buf.readInt();
        List<TerminalHomeScreenModel.SectionModel> sections = new ArrayList<TerminalHomeScreenModel.SectionModel>(size);
        for (int i = 0; i < size; i++) {
            sections.add(new TerminalHomeScreenModel.SectionModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)));
        }
        return sections;
    }

    static void writeNotifications(ByteBuf buf, List<TerminalHomeScreenModel.NotificationModel> items) {
        List<TerminalHomeScreenModel.NotificationModel> safeItems = items == null
            ? new ArrayList<TerminalHomeScreenModel.NotificationModel>() : items;
        buf.writeInt(safeItems.size());
        for (TerminalHomeScreenModel.NotificationModel item : safeItems) {
            ByteBufUtils.writeUTF8String(buf, safe(item.getTitle()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getBody()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getSeverityName()));
        }
    }

    static List<TerminalHomeScreenModel.NotificationModel> readNotifications(ByteBuf buf) {
        int size = buf.readInt();
        List<TerminalHomeScreenModel.NotificationModel> items = new ArrayList<TerminalHomeScreenModel.NotificationModel>(size);
        for (int i = 0; i < size; i++) {
            items.add(new TerminalHomeScreenModel.NotificationModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)));
        }
        return items;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static List<TerminalHomeScreenModel.NavItemModel> toNavItemModels(List<TerminalOpenApproval.NavItem> items) {
        List<TerminalHomeScreenModel.NavItemModel> models = new ArrayList<TerminalHomeScreenModel.NavItemModel>();
        if (items == null) {
            return models;
        }
        for (TerminalOpenApproval.NavItem item : items) {
            models.add(new TerminalHomeScreenModel.NavItemModel(
                item.getPageId(),
                item.getLabel(),
                item.getSubtitle(),
                item.isEnabled(),
                item.isSelected()));
        }
        return models;
    }

    private static List<TerminalHomeScreenModel.PageSnapshotModel> toPageSnapshotModels(
        List<TerminalOpenApproval.PageSnapshot> snapshots) {
        List<TerminalHomeScreenModel.PageSnapshotModel> models = new ArrayList<TerminalHomeScreenModel.PageSnapshotModel>();
        if (snapshots == null) {
            return models;
        }
        for (TerminalOpenApproval.PageSnapshot snapshot : snapshots) {
            models.add(new TerminalHomeScreenModel.PageSnapshotModel(
                snapshot.getPageId(),
                snapshot.getTitle(),
                snapshot.getLead(),
                toSectionModels(snapshot.getSections()),
                toBankSectionModel(snapshot.getBankSectionSnapshot()),
                toMarketSectionModel(snapshot.getMarketSectionSnapshot()),
                toCustomMarketSectionModel(snapshot.getCustomMarketSectionSnapshot()),
                toExchangeMarketSectionModel(snapshot.getExchangeMarketSectionSnapshot())));
        }
        return models;
    }

    private static TerminalCustomMarketSectionModel toCustomMarketSectionModel(
        TerminalCustomMarketSectionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new TerminalCustomMarketSectionModel(
            snapshot.getServiceState(),
            snapshot.getBrowserHint(),
            snapshot.getScopeLabel(),
            snapshot.getActiveListingLines(),
            snapshot.getActiveListingIds(),
            snapshot.getSellingListingLines(),
            snapshot.getSellingListingIds(),
            snapshot.getPendingListingLines(),
            snapshot.getPendingListingIds(),
            snapshot.getSelectedListingId(),
            snapshot.getSelectedTitle(),
            snapshot.getSelectedPrice(),
            snapshot.getSelectedStatus(),
            snapshot.getSelectedCounterparty(),
            snapshot.getSelectedItemIdentity(),
            snapshot.getSelectedTradeSummary(),
            snapshot.getSelectedActionHint(),
            snapshot.isCanBuy(),
            snapshot.isCanCancel(),
            snapshot.isCanClaim(),
            new TerminalCustomMarketSectionModel.ActionFeedbackModel(
                snapshot.getActionFeedback().getTitle(),
                snapshot.getActionFeedback().getBody(),
                snapshot.getActionFeedback().getSeverityName()));
    }

    private static TerminalExchangeMarketSectionModel toExchangeMarketSectionModel(
        TerminalExchangeMarketSectionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new TerminalExchangeMarketSectionModel(
            snapshot.getServiceState(),
            snapshot.getBrowserHint(),
            snapshot.getTargetCodes(),
            snapshot.getTargetLabels(),
            snapshot.getSelectedTargetCode(),
            snapshot.getSelectedTargetTitle(),
            snapshot.getSelectedTargetSummary(),
            snapshot.getHeldSummary(),
            snapshot.getInputRegistryName(),
            snapshot.getPairCode(),
            snapshot.getInputAssetCode(),
            snapshot.getOutputAssetCode(),
            snapshot.getRuleVersion(),
            snapshot.getLimitStatus(),
            snapshot.getReasonCode(),
            snapshot.getNotes(),
            snapshot.getInputQuantity(),
            snapshot.getNominalFaceValue(),
            snapshot.getEffectiveExchangeValue(),
            snapshot.getContributionValue(),
            snapshot.getDiscountStatus(),
            snapshot.getRateDisplay(),
            snapshot.getExecutionHint(),
            snapshot.isExecutable(),
            new TerminalExchangeMarketSectionModel.ActionFeedbackModel(
                snapshot.getActionFeedback().getTitle(),
                snapshot.getActionFeedback().getBody(),
                snapshot.getActionFeedback().getSeverityName()));
    }

    private static TerminalBankSectionModel toBankSectionModel(TerminalBankSectionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new TerminalBankSectionModel(
            new TerminalBankSectionModel.AccountStatusModel(
                snapshot.getAccountStatus().isOpened(),
                snapshot.getAccountStatus().getServiceState(),
                snapshot.getAccountStatus().getAccountLabel(),
                snapshot.getAccountStatus().getPlayerStatus(),
                snapshot.getAccountStatus().getAccountNo(),
                snapshot.getAccountStatus().getUpdatedAt(),
                snapshot.getAccountStatus().isOpenAllowed()),
            new TerminalBankSectionModel.BalanceSummaryModel(
                snapshot.getBalanceSummary().getPlayerBalance(),
                snapshot.getBalanceSummary().getExchangeBalance(),
                snapshot.getBalanceSummary().getExchangeStatus(),
                snapshot.getBalanceSummary().getTransferHint(),
                snapshot.getBalanceSummary().isTransferAllowed()),
            new TerminalBankSectionModel.TransferFormModel(
                snapshot.getTransferForm().getTargetPlayerName(),
                snapshot.getTransferForm().getAmountText(),
                snapshot.getTransferForm().getComment(),
                snapshot.getTransferForm().isTransferEnabled()),
            new TerminalBankSectionModel.ActionFeedbackModel(
                snapshot.getActionFeedback().getTitle(),
                snapshot.getActionFeedback().getBody(),
                snapshot.getActionFeedback().getSeverityName()),
            snapshot.getPlayerLedgerLines());
    }

    private static TerminalMarketSectionModel toMarketSectionModel(TerminalMarketSectionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new TerminalMarketSectionModel(
            snapshot.getRoutePageId(),
            snapshot.getServiceState(),
            snapshot.getBrowserHint(),
            snapshot.getProductKeys(),
            snapshot.getProductLabels(),
            snapshot.getSelectedProductKey(),
            snapshot.getSelectedProductName(),
            snapshot.getSelectedProductUnit(),
            snapshot.getLatestTradePrice(),
            snapshot.getHighestBid(),
            snapshot.getLowestAsk(),
            snapshot.getVolume24h(),
            snapshot.getTurnover24h(),
            snapshot.getSourceAvailable(),
            snapshot.getLockedEscrowQuantity(),
            snapshot.getClaimableQuantity(),
            snapshot.getFrozenFunds(),
            snapshot.getSummaryNotice(),
            snapshot.getAskLines(),
            snapshot.getBidLines(),
            snapshot.getMyOrderLines(),
            snapshot.getClaimLines(),
            snapshot.getClaimIds(),
            snapshot.getRuleLines(),
            new TerminalMarketSectionModel.LimitBuyDraftModel(
                snapshot.getLimitBuyDraft().getSelectedProductKey(),
                snapshot.getLimitBuyDraft().getPriceText(),
                snapshot.getLimitBuyDraft().getQuantityText(),
                snapshot.getLimitBuyDraft().isSubmitEnabled()),
            new TerminalMarketSectionModel.ActionFeedbackModel(
                snapshot.getActionFeedback().getTitle(),
                snapshot.getActionFeedback().getBody(),
                snapshot.getActionFeedback().getSeverityName()));
    }

    private static List<TerminalHomeScreenModel.SectionModel> toSectionModels(List<TerminalOpenApproval.Section> sections) {
        List<TerminalHomeScreenModel.SectionModel> models = new ArrayList<TerminalHomeScreenModel.SectionModel>();
        if (sections == null) {
            return models;
        }
        for (TerminalOpenApproval.Section section : sections) {
            models.add(new TerminalHomeScreenModel.SectionModel(
                section.getSectionId(),
                section.getTitle(),
                section.getSummary(),
                section.getDetail()));
        }
        return models;
    }

    private static List<TerminalHomeScreenModel.NotificationModel> toNotificationModels(
        List<TerminalOpenApproval.NotificationEntry> items) {
        List<TerminalHomeScreenModel.NotificationModel> models = new ArrayList<TerminalHomeScreenModel.NotificationModel>();
        if (items == null) {
            return models;
        }
        for (TerminalOpenApproval.NotificationEntry item : items) {
            models.add(new TerminalHomeScreenModel.NotificationModel(
                item.getTitle(),
                item.getBody(),
                item.getSeverityName()));
        }
        return models;
    }

    public static class Handler implements IMessageHandler<OpenTerminalApprovedMessage, IMessage> {

        @Override
        public IMessage onMessage(OpenTerminalApprovedMessage message, MessageContext ctx) {
            TerminalClientScreenController.INSTANCE.queueHomeScreen(message.toScreenModel());
            return null;
        }
    }
}

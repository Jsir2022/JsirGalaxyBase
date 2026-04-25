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

public class TerminalSnapshotMessage implements IMessage {

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

    public TerminalSnapshotMessage() {}

    public TerminalSnapshotMessage(TerminalOpenApproval approval) {
        this.selectedPageId = approval.getSelectedPageId();
        this.terminalTitle = approval.getTerminalTitle();
        this.terminalSubtitle = approval.getTerminalSubtitle();
        this.statusEyebrow = approval.getStatusBand().getEyebrow();
        this.statusHeadline = approval.getStatusBand().getHeadline();
        this.statusDetail = approval.getStatusBand().getDetail();
        this.statusBadgeLabel = approval.getStatusBand().getBadgeLabel();
        this.statusBadgeValue = approval.getStatusBand().getBadgeValue();
        this.navItems = toNavItemModels(approval.getNavItems());
        this.pageSnapshots = toPageSnapshotModels(approval.getPageSnapshots());
        this.notifications = toNotificationModels(approval.getNotifications());
        this.sessionToken = approval.getSessionToken();
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
        navItems = OpenTerminalApprovedMessage.readNavItems(buf);
        pageSnapshots = OpenTerminalApprovedMessage.readPageSnapshots(buf);
        notifications = OpenTerminalApprovedMessage.readNotifications(buf);
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
        OpenTerminalApprovedMessage.writeNavItems(buf, navItems);
        OpenTerminalApprovedMessage.writePageSnapshots(buf, pageSnapshots);
        OpenTerminalApprovedMessage.writeNotifications(buf, notifications);
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static class Handler implements IMessageHandler<TerminalSnapshotMessage, IMessage> {

        @Override
        public IMessage onMessage(TerminalSnapshotMessage message, MessageContext ctx) {
            TerminalClientScreenController.INSTANCE.queueHomeScreen(message.toScreenModel());
            return null;
        }
    }
}

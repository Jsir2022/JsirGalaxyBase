package com.jsirgalaxybase.terminal.client.viewmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

public final class TerminalBankSectionModel {

    private final AccountStatusModel accountStatus;
    private final BalanceSummaryModel balanceSummary;
    private final TransferFormModel transferForm;
    private final ActionFeedbackModel actionFeedback;
    private final List<String> playerLedgerLines;

    public TerminalBankSectionModel(AccountStatusModel accountStatus, BalanceSummaryModel balanceSummary,
        TransferFormModel transferForm, ActionFeedbackModel actionFeedback, List<String> playerLedgerLines) {
        this.accountStatus = accountStatus == null ? AccountStatusModel.placeholder() : accountStatus;
        this.balanceSummary = balanceSummary == null ? BalanceSummaryModel.placeholder() : balanceSummary;
        this.transferForm = transferForm == null ? TransferFormModel.placeholder() : transferForm;
        this.actionFeedback = actionFeedback == null ? ActionFeedbackModel.placeholder() : actionFeedback;
        this.playerLedgerLines = freeze(playerLedgerLines, Collections.singletonList("当前没有个人流水摘要。"));
    }

    public static TerminalBankSectionModel placeholder() {
        return new TerminalBankSectionModel(
            AccountStatusModel.placeholder(),
            BalanceSummaryModel.placeholder(),
            TransferFormModel.placeholder(),
            ActionFeedbackModel.placeholder(),
            Collections.singletonList("当前没有个人流水摘要。"));
    }

    public AccountStatusModel getAccountStatus() {
        return accountStatus;
    }

    public BalanceSummaryModel getBalanceSummary() {
        return balanceSummary;
    }

    public TransferFormModel getTransferForm() {
        return transferForm;
    }

    public ActionFeedbackModel getActionFeedback() {
        return actionFeedback;
    }

    public List<String> getPlayerLedgerLines() {
        return playerLedgerLines;
    }

    private static <T> List<T> freeze(List<T> source, List<T> fallback) {
        List<T> resolved = source == null || source.isEmpty() ? fallback : source;
        return Collections.unmodifiableList(new ArrayList<T>(resolved));
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public static final class AccountStatusModel {

        private final boolean opened;
        private final String serviceState;
        private final String accountLabel;
        private final String playerStatus;
        private final String accountNo;
        private final String updatedAt;
        private final boolean openAllowed;

        public AccountStatusModel(boolean opened, String serviceState, String accountLabel, String playerStatus,
            String accountNo, String updatedAt, boolean openAllowed) {
            this.opened = opened;
            this.serviceState = normalize(serviceState, "银行服务状态未知");
            this.accountLabel = normalize(accountLabel, opened ? "已开户" : "未开户");
            this.playerStatus = normalize(playerStatus, "当前没有账户状态。");
            this.accountNo = normalize(accountNo, "未分配");
            this.updatedAt = normalize(updatedAt, "无更新记录");
            this.openAllowed = openAllowed;
        }

        public static AccountStatusModel placeholder() {
            return new AccountStatusModel(false, "银行 section 宿主已接入", "未开户", "当前暂无账户状态。", "未分配", "无更新记录", true);
        }

        public boolean isOpened() {
            return opened;
        }

        public String getServiceState() {
            return serviceState;
        }

        public String getAccountLabel() {
            return accountLabel;
        }

        public String getPlayerStatus() {
            return playerStatus;
        }

        public String getAccountNo() {
            return accountNo;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public boolean isOpenAllowed() {
            return openAllowed;
        }
    }

    public static final class BalanceSummaryModel {

        private final String playerBalance;
        private final String exchangeBalance;
        private final String exchangeStatus;
        private final String transferHint;
        private final boolean transferAllowed;

        public BalanceSummaryModel(String playerBalance, String exchangeBalance, String exchangeStatus,
            String transferHint, boolean transferAllowed) {
            this.playerBalance = normalize(playerBalance, "未开户");
            this.exchangeBalance = normalize(exchangeBalance, "不可用");
            this.exchangeStatus = normalize(exchangeStatus, "公开账户未就绪");
            this.transferHint = normalize(transferHint, "当前没有转账说明。");
            this.transferAllowed = transferAllowed;
        }

        public static BalanceSummaryModel placeholder() {
            return new BalanceSummaryModel("未开户", "不可用", "公开账户未就绪", "后续银行摘要会显示在这里。", false);
        }

        public String getPlayerBalance() {
            return playerBalance;
        }

        public String getExchangeBalance() {
            return exchangeBalance;
        }

        public String getExchangeStatus() {
            return exchangeStatus;
        }

        public String getTransferHint() {
            return transferHint;
        }

        public boolean isTransferAllowed() {
            return transferAllowed;
        }
    }

    public static final class TransferFormModel {

        private final String targetPlayerName;
        private final String amountText;
        private final String comment;
        private final boolean transferEnabled;

        public TransferFormModel(String targetPlayerName, String amountText, String comment, boolean transferEnabled) {
            this.targetPlayerName = normalize(targetPlayerName, "");
            this.amountText = normalize(amountText, "");
            this.comment = normalize(comment, "");
            this.transferEnabled = transferEnabled;
        }

        public static TransferFormModel placeholder() {
            return new TransferFormModel("", "", "", false);
        }

        public String getTargetPlayerName() {
            return targetPlayerName;
        }

        public String getAmountText() {
            return amountText;
        }

        public String getComment() {
            return comment;
        }

        public boolean isTransferEnabled() {
            return transferEnabled;
        }
    }

    public static final class ActionFeedbackModel {

        private final String title;
        private final String body;
        private final String severityName;

        public ActionFeedbackModel(String title, String body, String severityName) {
            this.title = normalize(title, "银行动作反馈");
            this.body = normalize(body, "开户、刷新与转账反馈会显示在这里。");
            this.severityName = normalize(severityName, TerminalNotificationSeverity.INFO.name());
        }

        public static ActionFeedbackModel placeholder() {
            return new ActionFeedbackModel("银行动作反馈", "开户、刷新与转账反馈会显示在这里。", TerminalNotificationSeverity.INFO.name());
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }

        public String getSeverityName() {
            return severityName;
        }

        public TerminalNotificationSeverity getSeverity() {
            return TerminalNotificationSeverity.fromName(severityName);
        }
    }
}
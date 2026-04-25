package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.component.TerminalBankSectionState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public class TerminalBankActionMessageFactoryTest {

    @Test
    public void confirmTransferBuildsBankActionMessageOnNewActionSnapshotChain() {
        TerminalHomeScreenModel screenModel = createBankScreenModel(true);
        TerminalBankSectionState state = new TerminalBankSectionState();
        state.applyModel(screenModel.getSelectedPageSnapshot().getBankSectionModel().getTransferForm());

        TerminalActionMessage message = TerminalBankActionMessageFactory.createConfirmTransferMessage(
            screenModel,
            screenModel.getSelectedPageSnapshot().getBankSectionModel(),
            state);

        assertNotNull(message);
        assertEquals("session-bank-test", message.getSessionToken());
        assertEquals("bank", message.getPageId());
        assertEquals(TerminalActionType.BANK_CONFIRM_TRANSFER.getId(), message.getActionType());
        assertTrue(message.getPayload().contains("UmVjZWl2ZXI="));
    }

    @Test
    public void confirmTransferDoesNotBuildMessageWhenDraftIsIncomplete() {
        TerminalHomeScreenModel screenModel = createBankScreenModel(false);
        TerminalBankSectionState state = new TerminalBankSectionState();
        state.applyModel(screenModel.getSelectedPageSnapshot().getBankSectionModel().getTransferForm());

        TerminalActionMessage message = TerminalBankActionMessageFactory.createConfirmTransferMessage(
            screenModel,
            screenModel.getSelectedPageSnapshot().getBankSectionModel(),
            state);

        assertNull(message);
    }

    private static TerminalHomeScreenModel createBankScreenModel(boolean completeDraft) {
        TerminalHomeScreenModel.PageSnapshotModel home = TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.HOME);
        TerminalHomeScreenModel.PageSnapshotModel career = TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.CAREER);
        TerminalHomeScreenModel.PageSnapshotModel publicService = TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.PUBLIC_SERVICE);
        TerminalHomeScreenModel.PageSnapshotModel market = TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.MARKET);
        TerminalHomeScreenModel.PageSnapshotModel bank = new TerminalHomeScreenModel.PageSnapshotModel(
            "bank",
            "银河银行",
            "银行完整业务页",
            Arrays.asList(new TerminalHomeScreenModel.SectionModel("bank_account_state", "开户状态", "已开户", "当前账户可转账")),
            new TerminalBankSectionModel(
                new TerminalBankSectionModel.AccountStatusModel(true, "银行服务在线", "已开户", "ACTIVE / 按需开户", "ACC-001", "04-19 10:00", false),
                new TerminalBankSectionModel.BalanceSummaryModel("1000 / STARCOIN", "99999 / STARCOIN", "ACTIVE / 公开透明", "可向已开户玩家转账", true),
                new TerminalBankSectionModel.TransferFormModel(completeDraft ? "Receiver" : "", completeDraft ? "250" : "", "phase5 test", completeDraft),
                new TerminalBankSectionModel.ActionFeedbackModel("银行动作反馈", "等待确认提交。", "INFO"),
                Arrays.asList("04-19 09:00 | 入账 +1000 | 结余 1000")));

        return new TerminalHomeScreenModel(
            "bank",
            "银河终端 / Test",
            "phase 5 银行业务页",
            new TerminalHomeScreenModel.StatusBandModel("当前页", "银河银行", "银行完整业务页已迁入新壳。", "贡献", "0"),
            Arrays.asList(
                new TerminalHomeScreenModel.NavItemModel("home", "首页", "总览", true, false),
                new TerminalHomeScreenModel.NavItemModel("career", "职业", "职业页", true, false),
                new TerminalHomeScreenModel.NavItemModel("public_service", "公共", "公共页", true, false),
                new TerminalHomeScreenModel.NavItemModel("market", "市场", "总入口", true, false),
                new TerminalHomeScreenModel.NavItemModel("bank", "银行", "银行页", true, true)),
            Arrays.asList(home, career, publicService, market, bank),
            Arrays.asList(new TerminalHomeScreenModel.NotificationModel("银行页已迁入新壳", "当前可以直接走转账确认 popup。", "INFO")),
            "session-bank-test");
    }
}
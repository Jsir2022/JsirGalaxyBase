package com.jsirgalaxybase.terminal.client.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class TerminalBankSectionModelTest {

    @Test
    public void preservesOpenedAccountMappingAndBalanceSummary() {
        TerminalBankSectionModel model = new TerminalBankSectionModel(
            new TerminalBankSectionModel.AccountStatusModel(true, "银行服务在线", "已开户", "ACTIVE / 按需开户", "ACC-001", "04-18 12:00", false),
            new TerminalBankSectionModel.BalanceSummaryModel("1,250 / STARCOIN", "99,999 / STARCOIN", "ACTIVE / 公开透明", "可向已开户玩家转账", true),
            new TerminalBankSectionModel.TransferFormModel("Target", "250", "phase5", true),
            new TerminalBankSectionModel.ActionFeedbackModel("成功", "开户完成", "SUCCESS"),
            Arrays.asList("L1", "L2"));

        assertTrue(model.getAccountStatus().isOpened());
        assertEquals("ACC-001", model.getAccountStatus().getAccountNo());
        assertEquals("1,250 / STARCOIN", model.getBalanceSummary().getPlayerBalance());
        assertTrue(model.getBalanceSummary().isTransferAllowed());
        assertEquals("Target", model.getTransferForm().getTargetPlayerName());
        assertEquals("SUCCESS", model.getActionFeedback().getSeverityName());
    }

    @Test
    public void preservesUnopenedStateWithoutInventingTransferCapability() {
        TerminalBankSectionModel model = new TerminalBankSectionModel(
            new TerminalBankSectionModel.AccountStatusModel(false, "银行服务在线", "未开户", "未开户 / 按需开户 / 余额 0", "未分配", "无更新记录", true),
            new TerminalBankSectionModel.BalanceSummaryModel("未开户", "99,999 / STARCOIN", "ACTIVE / 公开透明", "请先开户后再转账", false),
            new TerminalBankSectionModel.TransferFormModel("", "", "", false),
            new TerminalBankSectionModel.ActionFeedbackModel("银行动作反馈", "开户、刷新与转账反馈会显示在这里。", "INFO"),
            Arrays.asList("尚未开户，暂无个人流水"));

        assertFalse(model.getAccountStatus().isOpened());
        assertEquals("未开户", model.getBalanceSummary().getPlayerBalance());
        assertFalse(model.getBalanceSummary().isTransferAllowed());
        assertTrue(model.getAccountStatus().isOpenAllowed());
    }
}
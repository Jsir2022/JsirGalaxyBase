package com.jsirgalaxybase.terminal.ui;

import net.minecraft.entity.player.EntityPlayer;

public final class TerminalHomeSnapshotProvider {

    public static final TerminalHomeSnapshotProvider INSTANCE = new TerminalHomeSnapshotProvider();

    private TerminalHomeSnapshotProvider() {}

    public TerminalHomeSnapshot create(EntityPlayer player) {
        String playerName = player == null ? "访客" : player.getCommandSenderName();
        return new TerminalHomeSnapshot(
            "后勤见习",
            1280,
            "友善",
            "钢材、焦煤、盘点",
            "钢紧、红稳、建材补"
                + (playerName.isEmpty() ? "" : " | " + playerName));
    }

    public String[] createCareerPageLines(EntityPlayer player) {
        TerminalHomeSnapshot snapshot = create(player);
        return new String[] {
            "当前职业：" + snapshot.getCareer(),
            "阶段定位：以工业后勤、仓储统配与物资保障为主。",
            "职业入口下一步将接职业等级、资格、晋升条件与权限摘要。",
            "当前展示仍为只读版本，用于先稳定终端分页结构。"
        };
    }

    public String[] createPublicServicePageLines(EntityPlayer player) {
        TerminalHomeSnapshot snapshot = create(player);
        return new String[] {
            "公共任务面板：" + snapshot.getPublicTasks(),
            "当前重点：把公共工程、公共订单和福利申请收拢到统一入口。",
            "后续这里会扩成任务列表、提交入口、进度条和奖励摘要。",
            "现阶段先验证分页切换和只读同步。"
        };
    }

    public String[] createMarketPageLines(EntityPlayer player) {
        TerminalHomeSnapshot snapshot = create(player);
        return new String[] {
            snapshot.getMarketSummary(),
            "市场页下一步将接价格看板、订单流、托管库存和成交摘要。",
            "当前终端骨架已预留正式市场页位置，后续不需要再换入口。",
            "玩家身份会继续沿用服务端权威快照。"
        };
    }
}
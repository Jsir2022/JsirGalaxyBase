package com.jsirgalaxybase.terminal.ui;

public enum TerminalPage {

    HOME("home", 0, "首页", "总览", "制度总览", "当前玩家制度摘要"),
    CAREER("career", 1, "职业", "职业页", "职业与资格", "资格、阶段与进阶方向"),
    PUBLIC_SERVICE("public_service", 2, "公共", "公共页", "公共任务与服务", "任务、福利与服务入口"),
    MARKET("market", 3, "市场", "总入口", "市场总入口", "标准商品 / 定制商品 / 汇率市场入口"),
    MARKET_STANDARDIZED("market_standardized", 4, "标市", "标准商品页", "标准商品市场", "统一目录、订单簿、托管库存与 CLAIMABLE"),
    MARKET_CUSTOM("market_custom", 5, "定制", "定制商品页", "定制商品市场", "单件挂牌、浏览、购买、pending 与 claim 入口"),
    MARKET_EXCHANGE("market_exchange", 6, "汇率", "汇率市场页", "汇率市场", "quote / exchange 兼容入口与规则边界说明"),
    BANK("bank", 7, "银行", "银行页", "银河银行", "个人账户、公开储备与子页入口"),
    BANK_ACCOUNT("bank_account", 8, "账户", "账户页", "个人账户信息", "余额、账户状态与开户策略"),
    BANK_TRANSFER("bank_transfer", 9, "转账", "转账页", "转账服务", "转账规则与后续正式操作入口"),
    BANK_EXCHANGE("bank_exchange", 10, "exchange", "公开页", "Exchange 公开页", "兑换储备余额与近期公开账本"),
    BANK_LEDGER("bank_ledger", 11, "流水", "流水页", "个人流水", "个人最近账本变化与空状态提示");

    final String id;
    final int index;
    final String label;
    final String subtitle;
    final String title;
    final String lead;

    TerminalPage(String id, int index, String label, String subtitle, String title, String lead) {
        this.id = id;
        this.index = index;
        this.label = label;
        this.subtitle = subtitle;
        this.title = title;
        this.lead = lead;
    }

    public String getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public String getLabel() {
        return label;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getTitle() {
        return title;
    }

    public String getLead() {
        return lead;
    }

    public boolean isBankPage() {
        return this == BANK || this == BANK_ACCOUNT || this == BANK_TRANSFER || this == BANK_EXCHANGE
            || this == BANK_LEDGER;
    }

    public boolean isMarketPage() {
        return this == MARKET || this == MARKET_STANDARDIZED || this == MARKET_CUSTOM || this == MARKET_EXCHANGE;
    }

    public String toTopLevelPageId() {
        if (isBankPage()) {
            return BANK.getId();
        }
        if (isMarketPage()) {
            return MARKET.getId();
        }
        return getId();
    }

    public static TerminalPage byIndex(int index) {
        for (TerminalPage page : values()) {
            if (page.index == index) {
                return page;
            }
        }
        return HOME;
    }

    public static TerminalPage fromId(String id) {
        if (id != null) {
            String normalized = id.trim();
            for (TerminalPage page : values()) {
                if (page.id.equalsIgnoreCase(normalized)) {
                    return page;
                }
            }
        }
        return HOME;
    }
}
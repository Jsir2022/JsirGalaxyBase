package com.jsirgalaxybase.terminal;

public enum TerminalActionType {
    OPEN_SHELL("open_shell"),
    SELECT_PAGE("select_page"),
    VAULT_OPEN("vault_open"),
    REFRESH_PAGE("refresh_page"),
    MARKET_REFRESH("market_refresh"),
    MARKET_CONFIRM_DEPOSIT_HELD("market_confirm_deposit_held"),
    MARKET_CONFIRM_LIMIT_BUY("market_confirm_limit_buy"),
    MARKET_CONFIRM_LIMIT_SELL("market_confirm_limit_sell"),
    MARKET_CONFIRM_INSTANT_BUY("market_confirm_instant_buy"),
    MARKET_CONFIRM_INSTANT_SELL("market_confirm_instant_sell"),
    MARKET_CONFIRM_ORDER("market_confirm_order"),
    MARKET_CANCEL_ORDER("market_cancel_order"),
    MARKET_CLAIM_ASSET("market_claim_asset"),
    MARKET_CUSTOM_REFRESH("market_custom_refresh"),
    MARKET_CUSTOM_SELECT_LISTING("market_custom_select_listing"),
    MARKET_CUSTOM_PUBLISH_HELD("market_custom_publish_held"),
    MARKET_CUSTOM_BUY_LISTING("market_custom_buy_listing"),
    MARKET_CUSTOM_CANCEL_LISTING("market_custom_cancel_listing"),
    MARKET_CUSTOM_CLAIM_LISTING("market_custom_claim_listing"),
    MARKET_EXCHANGE_REFRESH_QUOTE("market_exchange_refresh_quote"),
    MARKET_EXCHANGE_SELECT_TARGET("market_exchange_select_target"),
    MARKET_EXCHANGE_CONFIRM("market_exchange_confirm"),
    SERVER_TOOLS_REFRESH("server_tools_refresh"),
    SERVER_TOOLS_SELECT_WARP("server_tools_select_warp"),
    SERVER_TOOLS_CONFIRM_WARP("server_tools_confirm_warp"),
    BANK_REFRESH("bank_refresh"),
    BANK_OPEN_ACCOUNT("bank_open_account"),
    BANK_CONFIRM_TRANSFER("bank_confirm_transfer"),
    UNKNOWN("unknown");

    private final String id;

    TerminalActionType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static TerminalActionType fromId(String actionType) {
        if (actionType != null) {
            String normalized = actionType.trim();
            for (TerminalActionType value : values()) {
                if (value.id.equalsIgnoreCase(normalized)) {
                    return value;
                }
            }
        }
        return UNKNOWN;
    }
}

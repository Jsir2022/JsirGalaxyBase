package com.jsirgalaxybase.terminal;

public enum TerminalActionType {
    OPEN_SHELL("open_shell"),
    SELECT_PAGE("select_page"),
    VAULT_OPEN("vault_open"),
    WAREHOUSE_OPEN_BAY("warehouse_open_bay"),
    REFRESH_PAGE("refresh_page"),
    QUEST_SELECT_CHAPTER("quest_select_chapter"),
    QUEST_SELECT_QUEST("quest_select_quest"),
    QUEST_CHANGE_FILTER("quest_change_filter"),
    QUEST_CHANGE_QUERY("quest_change_query"),
    QUEST_CHANGE_CHAPTER_PAGE("quest_change_chapter_page"),
    QUEST_CHANGE_QUEST_PAGE("quest_change_quest_page"),
    QUEST_BACK("quest_back"),
    QUEST_CLAIM("quest_claim"),
    QUEST_SELECT_REWARD_CHOICE("quest_select_reward_choice"),
    QUEST_TOGGLE_TRACKING("quest_toggle_tracking"),
    QUEST_PARTICIPANT_CREATE("quest_participant_create"),
    QUEST_PARTICIPANT_ADD_MEMBER("quest_participant_add_member"),
    QUEST_PARTICIPANT_REMOVE_MEMBER("quest_participant_remove_member"),
    QUEST_PARTICIPANT_LEAVE("quest_participant_leave"),
    QUEST_PARTICIPANT_TRANSFER_OWNER("quest_participant_transfer_owner"),
    QUEST_ADMIN_OPEN("quest_admin_open"),
    QUEST_ADMIN_FILTER("quest_admin_filter"),
    QUEST_ADMIN_PAGE("quest_admin_page"),
    QUEST_ADMIN_SELECT("quest_admin_select"),
    QUEST_ADMIN_CREATE("quest_admin_create"),
    QUEST_ADMIN_EDIT("quest_admin_edit"),
    QUEST_ADMIN_PUBLISH("quest_admin_publish"),
    QUEST_ADMIN_RETIRE("quest_admin_retire"),
    QUEST_ADMIN_BATCH_RETIRE("quest_admin_batch_retire"),
    QUEST_CHAPTER_ADMIN_OPEN("quest_chapter_admin_open"),
    QUEST_CHAPTER_ADMIN_FILTER("quest_chapter_admin_filter"),
    QUEST_CHAPTER_ADMIN_PAGE("quest_chapter_admin_page"),
    QUEST_CHAPTER_ADMIN_SELECT("quest_chapter_admin_select"),
    QUEST_CHAPTER_ADMIN_CREATE("quest_chapter_admin_create"),
    QUEST_CHAPTER_ADMIN_EDIT("quest_chapter_admin_edit"),
    QUEST_CHAPTER_ADMIN_CLONE("quest_chapter_admin_clone"),
    QUEST_CHAPTER_ADMIN_ALIGN("quest_chapter_admin_align"),
    QUEST_CHAPTER_ADMIN_MOVE_BEFORE("quest_chapter_admin_move_before"),
    QUEST_CHAPTER_ADMIN_PUBLISH("quest_chapter_admin_publish"),
    QUEST_CHAPTER_ADMIN_RETIRE("quest_chapter_admin_retire"),
    MARKET_REFRESH("market_refresh"),
    MARKET_REFRESH_HISTORY("market_refresh_history"),
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
    SERVER_TOOLS_CONFIRM_QUICK("server_tools_confirm_quick"),
    SERVER_TOOLS_SELECT_HOME("server_tools_select_home"),
    SERVER_TOOLS_CONFIRM_HOME("server_tools_confirm_home"),
    SERVER_TOOLS_SET_HOME("server_tools_set_home"),
    SERVER_TOOLS_DELETE_HOME("server_tools_delete_home"),
    SERVER_TOOLS_TPA_REQUEST("server_tools_tpa_request"),
    SERVER_TOOLS_TPA_ACCEPT("server_tools_tpa_accept"),
    SERVER_TOOLS_TPA_DENY("server_tools_tpa_deny"),
    SERVER_TOOLS_TPA_CANCEL("server_tools_tpa_cancel"),
    LAND_SELECT("land_select"),
    LAND_SELECT_TAB("land_select_tab"),
    LAND_CHANGE_PAGE("land_change_page"),
    LAND_CLAIM("land_claim"),
    LAND_UNCLAIM("land_unclaim"),
    LAND_VIEWPORT("land_viewport"),
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

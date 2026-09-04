package com.jsirgalaxybase.config;

import java.io.File;
import java.util.Locale;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class ModConfiguration {

    private static final String BANKING_CATEGORY = "banking";
    private static final String ITEM_POLICY_CATEGORY = "item_policy";
    private static final String GLOBAL_ENTRY_CATEGORY = "global_entry";
    private static final String LAND_CATEGORY = "land";
    private static final String WAREHOUSE_CATEGORY = "warehouse";
    private static final String TERMINAL_CATEGORY = "terminal";
    private static final int DEFAULT_TERMINAL_ACCENT_COLOR = 0x529BED;
    private static final float DEFAULT_TERMINAL_PANEL_WIDTH_RATIO = 0.72f;
    private static final float DEFAULT_TERMINAL_PANEL_HEIGHT_RATIO = 0.44f;
    private static final float DEFAULT_TERMINAL_NAVIGATION_WIDTH_RATIO = 0.07f;

    private final File minecraftDirectory;
    private final boolean autoDumpItemsOnClientStart;
    private final String itemDumpDirectory;
    private final int terminalAccentColor;
    private final float terminalPanelWidthRatio;
    private final float terminalPanelHeightRatio;
    private final float terminalNavigationWidthRatio;
    private final boolean bankingPostgresEnabled;
    private final String bankingJdbcUrl;
    private final String bankingJdbcUsername;
    private final String bankingJdbcPassword;
    private final String bankingSourceServerId;
    private final boolean landEnabled;
    private final String landProtectionMode;
    private final int landMaxClaimsPerPlayer;
    private final int[] landBlockedDimensions;
    private final String[] landReservedChunks;
    private final boolean landAllowFakePlayers;
    private final boolean warehouseEnabled;
    private final boolean itemPolicyEnabled;
    private final String[] itemPolicyRules;
    private final String globalHubTarget;
    private final String[] targetServerRtpProfiles;

    private ModConfiguration(File minecraftDirectory, boolean autoDumpItemsOnClientStart, String itemDumpDirectory,
        int terminalAccentColor, float terminalPanelWidthRatio, float terminalPanelHeightRatio,
        float terminalNavigationWidthRatio, boolean bankingPostgresEnabled, String bankingJdbcUrl,
        String bankingJdbcUsername, String bankingJdbcPassword, String bankingSourceServerId, boolean landEnabled,
        String landProtectionMode, int landMaxClaimsPerPlayer, int[] landBlockedDimensions,
        String[] landReservedChunks, boolean landAllowFakePlayers, boolean warehouseEnabled, boolean itemPolicyEnabled, String[] itemPolicyRules,
        String globalHubTarget, String[] targetServerRtpProfiles) {
        this.minecraftDirectory = minecraftDirectory;
        this.autoDumpItemsOnClientStart = autoDumpItemsOnClientStart;
        this.itemDumpDirectory = itemDumpDirectory;
        this.terminalAccentColor = terminalAccentColor;
        this.terminalPanelWidthRatio = terminalPanelWidthRatio;
        this.terminalPanelHeightRatio = terminalPanelHeightRatio;
        this.terminalNavigationWidthRatio = terminalNavigationWidthRatio;
        this.bankingPostgresEnabled = bankingPostgresEnabled;
        this.bankingJdbcUrl = bankingJdbcUrl;
        this.bankingJdbcUsername = bankingJdbcUsername;
        this.bankingJdbcPassword = bankingJdbcPassword;
        this.bankingSourceServerId = bankingSourceServerId;
        this.landEnabled = landEnabled;
        this.landProtectionMode = landProtectionMode;
        this.landMaxClaimsPerPlayer = landMaxClaimsPerPlayer;
        this.landBlockedDimensions = landBlockedDimensions.clone();
        this.landReservedChunks = landReservedChunks.clone();
        this.landAllowFakePlayers = landAllowFakePlayers;
        this.warehouseEnabled = warehouseEnabled;
        this.itemPolicyEnabled = itemPolicyEnabled;
        this.itemPolicyRules = itemPolicyRules.clone();
        this.globalHubTarget = globalHubTarget;
        this.targetServerRtpProfiles = targetServerRtpProfiles.clone();
    }

    /**
     * Kept for the existing server-side configuration test fixture and older
     * internal bootstrap adapters. Warehouse remains opt-in when that legacy
     * construction path is used.
     */
    @SuppressWarnings("unused")
    private ModConfiguration(File minecraftDirectory, boolean autoDumpItemsOnClientStart, String itemDumpDirectory,
        int terminalAccentColor, float terminalPanelWidthRatio, float terminalPanelHeightRatio,
        float terminalNavigationWidthRatio, boolean bankingPostgresEnabled, String bankingJdbcUrl,
        String bankingJdbcUsername, String bankingJdbcPassword, String bankingSourceServerId, boolean landEnabled,
        String landProtectionMode, int landMaxClaimsPerPlayer, int[] landBlockedDimensions,
        String[] landReservedChunks, boolean landAllowFakePlayers, boolean itemPolicyEnabled, String[] itemPolicyRules,
        String globalHubTarget, String[] targetServerRtpProfiles) {
        this(minecraftDirectory, autoDumpItemsOnClientStart, itemDumpDirectory, terminalAccentColor,
            terminalPanelWidthRatio, terminalPanelHeightRatio, terminalNavigationWidthRatio, bankingPostgresEnabled,
            bankingJdbcUrl, bankingJdbcUsername, bankingJdbcPassword, bankingSourceServerId, landEnabled,
            landProtectionMode, landMaxClaimsPerPlayer, landBlockedDimensions, landReservedChunks, landAllowFakePlayers,
            false, itemPolicyEnabled, itemPolicyRules, globalHubTarget, targetServerRtpProfiles);
    }

    public static ModConfiguration load(File configFile, boolean client) {
        final Configuration clientConfiguration = new Configuration(configFile);
        final Configuration serverConfiguration = client ? null
            : new Configuration(new File(configFile.getParentFile(), "jsirgalaxybase-server.cfg"));
        final File minecraftDirectory = configFile.getParentFile().getParentFile();

        final boolean autoDumpItemsOnClientStart = clientConfiguration.getBoolean(
            "autoDumpItemsOnClientStart",
            Configuration.CATEGORY_GENERAL,
            true,
            "When true, the client exports registry and NEI item lists after startup.");
        final String itemDumpDirectory = clientConfiguration.getString(
            "itemDumpDirectory",
            Configuration.CATEGORY_GENERAL,
            "jsirgalaxybase/item_dumps",
            "Relative path under the Minecraft directory where item dump files are written.");
        final int terminalAccentColor = parseHexColor(
            clientConfiguration.getString(
                "terminalAccentColor",
                TERMINAL_CATEGORY,
                "#529BED",
                "Client-only terminal accent color in #RRGGBB format."),
            DEFAULT_TERMINAL_ACCENT_COLOR);
        final float terminalPanelWidthRatio = readClampedRatio(
            clientConfiguration,
            TERMINAL_CATEGORY,
            "terminalPanelWidthRatio",
            "0.72",
            "Client-only terminal width ratio relative to screen size. Recommended 0.68 - 0.80.",
            DEFAULT_TERMINAL_PANEL_WIDTH_RATIO,
            0.68f,
            0.80f);
        final float terminalPanelHeightRatio = readClampedRatio(
            clientConfiguration,
            TERMINAL_CATEGORY,
            "terminalPanelHeightRatio",
            "0.44",
            "Client-only terminal height ratio relative to screen size. Recommended 0.40 - 0.54.",
            DEFAULT_TERMINAL_PANEL_HEIGHT_RATIO,
            0.40f,
            0.54f);
        final float terminalNavigationWidthRatio = readClampedRatio(
            clientConfiguration,
            TERMINAL_CATEGORY,
            "terminalNavigationWidthRatio",
            "0.07",
            "Client-only navigation width ratio inside terminal layout. Recommended 0.06 - 0.09.",
            DEFAULT_TERMINAL_NAVIGATION_WIDTH_RATIO,
            0.06f,
            0.09f);

        final boolean bankingPostgresEnabled = serverConfiguration == null ? false : serverConfiguration.getBoolean(
            "bankingPostgresEnabled",
            BANKING_CATEGORY,
            false,
            "When true, the institution core prepares PostgreSQL-backed banking infrastructure on the server side.");
        final String bankingJdbcUrl = serverConfiguration == null ? "" : serverConfiguration.getString(
            "bankingJdbcUrl",
            BANKING_CATEGORY,
            "jdbc:postgresql://db-host:5432/jsirgalaxybase",
            "Server-only PostgreSQL JDBC URL for banking and institutional data.");
        final String bankingJdbcUsername = serverConfiguration == null ? "" : serverConfiguration.getString(
            "bankingJdbcUsername",
            BANKING_CATEGORY,
            "",
            "Server-only PostgreSQL username used by the banking infrastructure.");
        final String bankingJdbcPassword = serverConfiguration == null ? "" : serverConfiguration.getString(
            "bankingJdbcPassword",
            BANKING_CATEGORY,
            "",
            "Server-only PostgreSQL password used by the banking infrastructure.");
        final String bankingSourceServerId = serverConfiguration == null ? "client-disabled" : serverConfiguration.getString(
            "bankingSourceServerId",
            BANKING_CATEGORY,
            "local-dev",
            "Logical source_server_id written into banking transactions on this server.");
        final boolean landEnabled = serverConfiguration != null && serverConfiguration.getBoolean(
            "landEnabled",
            LAND_CATEGORY,
            false,
            "Enables the JGB personal-land PostgreSQL runtime and Forge protection handler.");
        final String landProtectionMode = serverConfiguration == null ? "SHADOW" : serverConfiguration.getString(
            "landProtectionMode",
            LAND_CATEGORY,
            "SHADOW",
            "SHADOW records denied decisions without cancelling events; ENFORCE actively protects claimed chunks.");
        final int landMaxClaimsPerPlayer = serverConfiguration == null ? 4 : serverConfiguration.getInt(
            "landMaxClaimsPerPlayer",
            LAND_CATEGORY,
            4,
            1,
            10000,
            "Maximum number of active personal chunk titles per player.");
        final int[] landBlockedDimensions = serverConfiguration == null ? new int[0]
            : serverConfiguration.get(LAND_CATEGORY, "landBlockedDimensions", new int[0],
                "Dimension IDs where personal land cannot be claimed.").getIntList();
        final String[] landReservedChunks = serverConfiguration == null ? new String[0]
            : serverConfiguration.getStringList(
                "landReservedChunks",
                LAND_CATEGORY,
                new String[0],
                "Reserved local chunks in dimension:chunkX:chunkZ format.");
        final boolean landAllowFakePlayers = serverConfiguration != null && serverConfiguration.getBoolean(
            "landAllowFakePlayers",
            LAND_CATEGORY,
            false,
                "When true, all fake players may modify claimed chunks. Keep false until per-title automation grants exist.");
        final boolean warehouseEnabled = serverConfiguration != null && serverConfiguration.getBoolean(
            "warehouseEnabled",
            WAREHOUSE_CATEGORY,
            false,
            "Enables the personal AE2 Warehouse Drive ownership and audit runtime. Requires AE2 and the Warehouse Drive migration.");
        final boolean itemPolicyEnabled = serverConfiguration != null && serverConfiguration.getBoolean(
            "itemPolicyEnabled",
            ITEM_POLICY_CATEGORY,
            false,
            "Enables server-authoritative item admission rules and denied-operation audit. Disabled by default.");
        final String[] itemPolicyRules = serverConfiguration == null ? new String[0]
            : serverConfiguration.getStringList(
                "itemPolicyRules",
                ITEM_POLICY_CATEGORY,
                new String[0],
                "Deny rules: rule-id|SCOPE,SCOPE|modid:item[:meta]. Valid scopes: MARKET_CUSTODY, CUSTOM_MARKET_ESCROW, BASE_VAULT, LAND_AUTOMATION.");
        final String globalHubTarget = serverConfiguration == null ? "" : serverConfiguration.getString(
            "globalHubTarget", GLOBAL_ENTRY_CATEGORY, "",
            "Optional global Hub target: serverId|dimension|x|y|z|yaw|pitch. Empty keeps /spawn local.");
        final String[] targetServerRtpProfiles = serverConfiguration == null ? new String[0]
            : serverConfiguration.getStringList("targetServerRtpProfiles", GLOBAL_ENTRY_CATEGORY, new String[0],
                "Target RTP profiles: serverId|dimension|centerX|fallbackY|centerZ|minDistance|maxDistance.");

        if (clientConfiguration.hasCategory(BANKING_CATEGORY)) {
            clientConfiguration.removeCategory(clientConfiguration.getCategory(BANKING_CATEGORY));
        }
        if (clientConfiguration.hasCategory(LAND_CATEGORY)) {
            clientConfiguration.removeCategory(clientConfiguration.getCategory(LAND_CATEGORY));
        }
        if (clientConfiguration.hasCategory(ITEM_POLICY_CATEGORY)) {
            clientConfiguration.removeCategory(clientConfiguration.getCategory(ITEM_POLICY_CATEGORY));
        }
        if (clientConfiguration.hasCategory(WAREHOUSE_CATEGORY)) {
            clientConfiguration.removeCategory(clientConfiguration.getCategory(WAREHOUSE_CATEGORY));
        }
        if (clientConfiguration.hasCategory(GLOBAL_ENTRY_CATEGORY)) {
            clientConfiguration.removeCategory(clientConfiguration.getCategory(GLOBAL_ENTRY_CATEGORY));
        }
        if (clientConfiguration.hasChanged()) {
            clientConfiguration.save();
        }
        if (serverConfiguration != null && serverConfiguration.hasChanged()) {
            serverConfiguration.save();
        }

        return new ModConfiguration(
            minecraftDirectory,
            autoDumpItemsOnClientStart,
            itemDumpDirectory,
            terminalAccentColor,
            terminalPanelWidthRatio,
            terminalPanelHeightRatio,
            terminalNavigationWidthRatio,
            bankingPostgresEnabled,
            bankingJdbcUrl,
            bankingJdbcUsername,
            bankingJdbcPassword,
            bankingSourceServerId,
            landEnabled,
            landProtectionMode,
            landMaxClaimsPerPlayer,
            landBlockedDimensions,
            landReservedChunks,
            landAllowFakePlayers,
            warehouseEnabled,
            itemPolicyEnabled,
            itemPolicyRules,
            globalHubTarget,
            targetServerRtpProfiles);
    }

    private static float parseRatio(String value, float fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float clampRatio(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static float readClampedRatio(Configuration configuration, String category, String key, String defaultValue,
        String comment, float fallback, float min, float max) {
        Property property = configuration.get(category, key, defaultValue, comment);
        float clamped = clampRatio(parseRatio(property.getString(), fallback), min, max);
        String normalized = String.format(Locale.ROOT, "%.2f", clamped);
        if (!normalized.equals(property.getString())) {
            property.set(normalized);
        }
        return clamped;
    }

    private static int parseHexColor(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public File getMinecraftDirectory() {
        return minecraftDirectory;
    }

    public boolean isAutoDumpItemsOnClientStart() {
        return autoDumpItemsOnClientStart;
    }

    public String getItemDumpDirectory() {
        return itemDumpDirectory;
    }

    public int getTerminalAccentColor() {
        return terminalAccentColor;
    }

    public float getTerminalPanelWidthRatio() {
        return terminalPanelWidthRatio;
    }

    public float getTerminalPanelHeightRatio() {
        return terminalPanelHeightRatio;
    }

    public float getTerminalNavigationWidthRatio() {
        return terminalNavigationWidthRatio;
    }

    public boolean isBankingPostgresEnabled() {
        return bankingPostgresEnabled;
    }

    public String getBankingJdbcUrl() {
        return bankingJdbcUrl;
    }

    public String getBankingJdbcUsername() {
        return bankingJdbcUsername;
    }

    public String getBankingJdbcPassword() {
        return bankingJdbcPassword;
    }

    public String getBankingSourceServerId() {
        return bankingSourceServerId;
    }

    public boolean isLandEnabled() {
        return landEnabled;
    }

    public String getLandProtectionMode() {
        return landProtectionMode;
    }

    public int getLandMaxClaimsPerPlayer() {
        return landMaxClaimsPerPlayer;
    }

    public int[] getLandBlockedDimensions() {
        return landBlockedDimensions.clone();
    }

    public String[] getLandReservedChunks() {
        return landReservedChunks.clone();
    }

    public boolean isLandAllowFakePlayers() {
        return landAllowFakePlayers;
    }

    public boolean isWarehouseEnabled() { return warehouseEnabled; }

    public boolean isItemPolicyEnabled() {
        return itemPolicyEnabled;
    }

    public String[] getItemPolicyRules() {
        return itemPolicyRules.clone();
    }

    public String getGlobalHubTarget() { return globalHubTarget; }

    public String[] getTargetServerRtpProfiles() { return targetServerRtpProfiles.clone(); }
}

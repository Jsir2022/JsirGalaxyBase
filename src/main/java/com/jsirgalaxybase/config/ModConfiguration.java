package com.jsirgalaxybase.config;

import java.io.File;
import java.util.Locale;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class ModConfiguration {

    private static final String BANKING_CATEGORY = "banking";
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

    private ModConfiguration(File minecraftDirectory, boolean autoDumpItemsOnClientStart, String itemDumpDirectory,
        int terminalAccentColor, float terminalPanelWidthRatio, float terminalPanelHeightRatio,
        float terminalNavigationWidthRatio, boolean bankingPostgresEnabled, String bankingJdbcUrl,
        String bankingJdbcUsername, String bankingJdbcPassword, String bankingSourceServerId) {
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

        if (clientConfiguration.hasCategory(BANKING_CATEGORY)) {
            clientConfiguration.removeCategory(clientConfiguration.getCategory(BANKING_CATEGORY));
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
            bankingSourceServerId);
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
}

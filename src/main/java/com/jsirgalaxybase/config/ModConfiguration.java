package com.jsirgalaxybase.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class ModConfiguration {

    private final File minecraftDirectory;
    private final boolean autoDumpItemsOnClientStart;
    private final String itemDumpDirectory;
    private final boolean bankingPostgresEnabled;
    private final String bankingJdbcUrl;
    private final String bankingJdbcUsername;
    private final String bankingJdbcPassword;
    private final String bankingSourceServerId;

    private ModConfiguration(File minecraftDirectory, boolean autoDumpItemsOnClientStart, String itemDumpDirectory,
        boolean bankingPostgresEnabled, String bankingJdbcUrl, String bankingJdbcUsername,
        String bankingJdbcPassword, String bankingSourceServerId) {
        this.minecraftDirectory = minecraftDirectory;
        this.autoDumpItemsOnClientStart = autoDumpItemsOnClientStart;
        this.itemDumpDirectory = itemDumpDirectory;
        this.bankingPostgresEnabled = bankingPostgresEnabled;
        this.bankingJdbcUrl = bankingJdbcUrl;
        this.bankingJdbcUsername = bankingJdbcUsername;
        this.bankingJdbcPassword = bankingJdbcPassword;
        this.bankingSourceServerId = bankingSourceServerId;
    }

    public static ModConfiguration load(File configFile) {
        final Configuration configuration = new Configuration(configFile);
        final File minecraftDirectory = configFile.getParentFile().getParentFile();

        final boolean autoDumpItemsOnClientStart = configuration.getBoolean(
            "autoDumpItemsOnClientStart",
            Configuration.CATEGORY_GENERAL,
            true,
            "When true, the client exports registry and NEI item lists after startup.");
        final String itemDumpDirectory = configuration.getString(
            "itemDumpDirectory",
            Configuration.CATEGORY_GENERAL,
            "jsirgalaxybase/item_dumps",
            "Relative path under the Minecraft directory where item dump files are written.");
        final boolean bankingPostgresEnabled = configuration.getBoolean(
            "bankingPostgresEnabled",
            "banking",
            false,
            "When true, the institution core prepares PostgreSQL-backed banking infrastructure on the server side.");
        final String bankingJdbcUrl = configuration.getString(
            "bankingJdbcUrl",
            "banking",
            "jdbc:postgresql://127.0.0.1:5432/jsirgalaxybase",
            "PostgreSQL JDBC URL for banking and institutional data.");
        final String bankingJdbcUsername = configuration.getString(
            "bankingJdbcUsername",
            "banking",
            "postgres",
            "PostgreSQL username used by the banking infrastructure.");
        final String bankingJdbcPassword = configuration.getString(
            "bankingJdbcPassword",
            "banking",
            "",
            "PostgreSQL password used by the banking infrastructure.");
        final String bankingSourceServerId = configuration.getString(
            "bankingSourceServerId",
            "banking",
            "local-dev",
            "Logical source_server_id written into banking transactions on this server.");

        if (configuration.hasChanged()) {
            configuration.save();
        }

        return new ModConfiguration(minecraftDirectory, autoDumpItemsOnClientStart, itemDumpDirectory,
            bankingPostgresEnabled, bankingJdbcUrl, bankingJdbcUsername, bankingJdbcPassword, bankingSourceServerId);
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
package com.jsirgalaxybase.terminal.ui;

public class TerminalBankSnapshot {

    private final String serviceState;
    private final String playerBalance;
    private final String playerStatus;
    private final String playerAccountNo;
    private final String playerUpdatedAt;
    private final String transferState;
    private final String[] playerLedgerLines;
    private final String exchangeBalance;
    private final String exchangeStatus;
    private final String exchangeAccountNo;
    private final String exchangeUpdatedAt;
    private final String[] exchangeLedgerLines;

    public TerminalBankSnapshot(String serviceState, String playerBalance, String playerStatus, String playerAccountNo,
        String playerUpdatedAt, String transferState, String[] playerLedgerLines, String exchangeBalance,
        String exchangeStatus, String exchangeAccountNo, String exchangeUpdatedAt, String[] exchangeLedgerLines) {
        this.serviceState = serviceState;
        this.playerBalance = playerBalance;
        this.playerStatus = playerStatus;
        this.playerAccountNo = playerAccountNo;
        this.playerUpdatedAt = playerUpdatedAt;
        this.transferState = transferState;
        this.playerLedgerLines = playerLedgerLines == null ? new String[0] : playerLedgerLines.clone();
        this.exchangeBalance = exchangeBalance;
        this.exchangeStatus = exchangeStatus;
        this.exchangeAccountNo = exchangeAccountNo;
        this.exchangeUpdatedAt = exchangeUpdatedAt;
        this.exchangeLedgerLines = exchangeLedgerLines == null ? new String[0] : exchangeLedgerLines.clone();
    }

    public String getServiceState() {
        return serviceState;
    }

    public String getPlayerBalance() {
        return playerBalance;
    }

    public String getPlayerStatus() {
        return playerStatus;
    }

    public String getPlayerAccountNo() {
        return playerAccountNo;
    }

    public String getPlayerUpdatedAt() {
        return playerUpdatedAt;
    }

    public String getTransferState() {
        return transferState;
    }

    public String[] getPlayerLedgerLines() {
        return playerLedgerLines.clone();
    }

    public String getExchangeBalance() {
        return exchangeBalance;
    }

    public String getExchangeStatus() {
        return exchangeStatus;
    }

    public String getExchangeAccountNo() {
        return exchangeAccountNo;
    }

    public String getExchangeUpdatedAt() {
        return exchangeUpdatedAt;
    }

    public String[] getExchangeLedgerLines() {
        return exchangeLedgerLines.clone();
    }
}
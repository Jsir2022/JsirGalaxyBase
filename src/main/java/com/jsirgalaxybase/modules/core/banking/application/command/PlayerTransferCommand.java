package com.jsirgalaxybase.modules.core.banking.application.command;

public class PlayerTransferCommand {

    private final String requestId;
    private final long fromAccountId;
    private final long toAccountId;
    private final String sourceServerId;
    private final String operatorRef;
    private final String playerRef;
    private final long amount;
    private final String comment;
    private final String businessRef;
    private final String extraJson;

    public PlayerTransferCommand(String requestId, long fromAccountId, long toAccountId, String sourceServerId,
        String operatorRef, String playerRef, long amount, String comment, String businessRef, String extraJson) {
        this.requestId = requestId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.sourceServerId = sourceServerId;
        this.operatorRef = operatorRef;
        this.playerRef = playerRef;
        this.amount = amount;
        this.comment = comment;
        this.businessRef = businessRef;
        this.extraJson = extraJson;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getFromAccountId() {
        return fromAccountId;
    }

    public long getToAccountId() {
        return toAccountId;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public String getOperatorRef() {
        return operatorRef;
    }

    public String getPlayerRef() {
        return playerRef;
    }

    public long getAmount() {
        return amount;
    }

    public String getComment() {
        return comment;
    }

    public String getBusinessRef() {
        return businessRef;
    }

    public String getExtraJson() {
        return extraJson;
    }
}
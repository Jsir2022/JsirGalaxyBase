package com.jsirgalaxybase.modules.core.banking.domain;

import java.time.Instant;

public class BankTransaction {

    private final long transactionId;
    private final String requestId;
    private final BankTransactionType transactionType;
    private final BankBusinessType businessType;
    private final String businessRef;
    private final String sourceServerId;
    private final String operatorType;
    private final String operatorRef;
    private final String playerRef;
    private final String comment;
    private final String extraJson;
    private final Instant createdAt;

    public BankTransaction(long transactionId, String requestId, BankTransactionType transactionType,
        BankBusinessType businessType, String businessRef, String sourceServerId, String operatorType,
        String operatorRef, String playerRef, String comment, String extraJson, Instant createdAt) {
        this.transactionId = transactionId;
        this.requestId = requestId;
        this.transactionType = transactionType;
        this.businessType = businessType;
        this.businessRef = businessRef;
        this.sourceServerId = sourceServerId;
        this.operatorType = operatorType;
        this.operatorRef = operatorRef;
        this.playerRef = playerRef;
        this.comment = comment;
        this.extraJson = extraJson;
        this.createdAt = createdAt;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public BankTransactionType getTransactionType() {
        return transactionType;
    }

    public BankBusinessType getBusinessType() {
        return businessType;
    }

    public String getBusinessRef() {
        return businessRef;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public String getOperatorType() {
        return operatorType;
    }

    public String getOperatorRef() {
        return operatorRef;
    }

    public String getPlayerRef() {
        return playerRef;
    }

    public String getComment() {
        return comment;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
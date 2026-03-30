package com.jsirgalaxybase.modules.core.banking.application.command;

import com.jsirgalaxybase.modules.core.banking.domain.BankBusinessType;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransactionType;

public class InternalTransferCommand {

    private final String requestId;
    private final BankTransactionType transactionType;
    private final BankBusinessType businessType;
    private final long fromAccountId;
    private final long toAccountId;
    private final String sourceServerId;
    private final String operatorType;
    private final String operatorRef;
    private final String playerRef;
    private final long amount;
    private final String comment;
    private final String businessRef;
    private final String extraJson;

    public InternalTransferCommand(String requestId, BankTransactionType transactionType,
        BankBusinessType businessType, long fromAccountId, long toAccountId, String sourceServerId,
        String operatorType, String operatorRef, String playerRef, long amount, String comment, String businessRef,
        String extraJson) {
        this.requestId = requestId;
        this.transactionType = transactionType;
        this.businessType = businessType;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.sourceServerId = sourceServerId;
        this.operatorType = operatorType;
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

    public BankTransactionType getTransactionType() {
        return transactionType;
    }

    public BankBusinessType getBusinessType() {
        return businessType;
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

    public String getOperatorType() {
        return operatorType;
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
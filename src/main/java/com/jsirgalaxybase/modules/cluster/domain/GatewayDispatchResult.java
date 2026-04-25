package com.jsirgalaxybase.modules.cluster.domain;

public class GatewayDispatchResult {

    public enum Status {
        COMPLETED_LOCAL,
        PENDING_REMOTE,
        FAILED
    }

    private final Status status;
    private final String message;
    private final TransferTicket ticket;

    private GatewayDispatchResult(Status status, String message, TransferTicket ticket) {
        this.status = status;
        this.message = message;
        this.ticket = ticket;
    }

    public static GatewayDispatchResult completedLocal(String message) {
        return new GatewayDispatchResult(Status.COMPLETED_LOCAL, message, null);
    }

    public static GatewayDispatchResult pendingRemote(String message, TransferTicket ticket) {
        return new GatewayDispatchResult(Status.PENDING_REMOTE, message, ticket);
    }

    public static GatewayDispatchResult failed(String message, TransferTicket ticket) {
        return new GatewayDispatchResult(Status.FAILED, message, ticket);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public TransferTicket getTicket() {
        return ticket;
    }
}
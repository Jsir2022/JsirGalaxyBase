package com.jsirgalaxybase.modules.servertools.domain;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

public class TeleportDispatchPlan {

    private final String requestId;
    private final String subjectPlayerUuid;
    private final String subjectPlayerName;
    private final String sourceServerId;
    private final TeleportKind teleportKind;
    private final TeleportTarget target;

    public TeleportDispatchPlan(String requestId, String subjectPlayerUuid, String subjectPlayerName,
        String sourceServerId, TeleportKind teleportKind, TeleportTarget target) {
        this.requestId = requestId;
        this.subjectPlayerUuid = subjectPlayerUuid;
        this.subjectPlayerName = subjectPlayerName;
        this.sourceServerId = sourceServerId;
        this.teleportKind = teleportKind;
        this.target = target;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSubjectPlayerUuid() {
        return subjectPlayerUuid;
    }

    public String getSubjectPlayerName() {
        return subjectPlayerName;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public TeleportKind getTeleportKind() {
        return teleportKind;
    }

    public TeleportTarget getTarget() {
        return target;
    }
}
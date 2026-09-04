package com.jsirgalaxybase.modules.land.domain;

import java.util.Objects;

public final class PersonalLandActionReceipt {

    private final String requestId;
    private final String semanticsKey;
    private final LandActionType actionType;
    private final LandActionResult result;
    private final PersonalLandTitle beforeTitle;
    private final PersonalLandTitle afterTitle;

    public PersonalLandActionReceipt(String requestId, String semanticsKey, LandActionType actionType,
        LandActionResult result, PersonalLandTitle beforeTitle, PersonalLandTitle afterTitle) {
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.semanticsKey = Objects.requireNonNull(semanticsKey, "semanticsKey");
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        this.result = Objects.requireNonNull(result, "result");
        this.beforeTitle = beforeTitle;
        this.afterTitle = afterTitle;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSemanticsKey() {
        return semanticsKey;
    }

    public LandActionType getActionType() {
        return actionType;
    }

    public LandActionResult getResult() {
        return result;
    }

    public PersonalLandTitle getBeforeTitle() {
        return beforeTitle;
    }

    public PersonalLandTitle getAfterTitle() {
        return afterTitle;
    }

    public boolean isSuccess() {
        return result == LandActionResult.SUCCESS;
    }
}

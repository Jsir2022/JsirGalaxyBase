package com.jsirgalaxybase.quest.core;

import java.util.Objects;

/** Immutable behavior and presentation options that affect quest execution or discovery. */
public final class QuestBehavior {
    public static final QuestBehavior DEFAULT = builder().build();

    private final QuestVisibility visibility;
    private final String iconReference;
    private final boolean main;
    private final boolean silent;
    private final boolean autoClaim;
    private final boolean progressWhileLocked;
    private final boolean simultaneous;
    private final boolean global;
    private final boolean globalShare;
    private final String updateSound;
    private final String completeSound;
    private final QuestParticipantScope participantScope;

    private QuestBehavior(Builder builder) {
        visibility = Objects.requireNonNull(builder.visibility, "visibility");
        iconReference = safe(builder.iconReference);
        main = builder.main;
        silent = builder.silent;
        autoClaim = builder.autoClaim;
        progressWhileLocked = builder.progressWhileLocked;
        simultaneous = builder.simultaneous;
        global = builder.global;
        globalShare = builder.globalShare;
        updateSound = safe(builder.updateSound);
        completeSound = safe(builder.completeSound);
        participantScope = Objects.requireNonNull(builder.participantScope, "participantScope");
    }

    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() {
        return builder().visibility(visibility).iconReference(iconReference).main(main).silent(silent)
            .autoClaim(autoClaim).progressWhileLocked(progressWhileLocked).simultaneous(simultaneous)
            .global(global).globalShare(globalShare).updateSound(updateSound).completeSound(completeSound)
            .participantScope(participantScope);
    }

    public QuestVisibility getVisibility() { return visibility; }
    public String getIconReference() { return iconReference; }
    public boolean isMain() { return main; }
    public boolean isSilent() { return silent; }
    public boolean isAutoClaim() { return autoClaim; }
    public boolean isProgressWhileLocked() { return progressWhileLocked; }
    public boolean isSimultaneous() { return simultaneous; }
    public boolean isGlobal() { return global; }
    public boolean isGlobalShare() { return globalShare; }
    public String getUpdateSound() { return updateSound; }
    public String getCompleteSound() { return completeSound; }
    public QuestParticipantScope getParticipantScope() { return participantScope; }

    private static String safe(String value) { return value == null ? "" : value; }

    public static final class Builder {
        private QuestVisibility visibility = QuestVisibility.NORMAL;
        private String iconReference = "";
        private boolean main;
        private boolean silent;
        private boolean autoClaim;
        private boolean progressWhileLocked;
        private boolean simultaneous;
        private boolean global;
        private boolean globalShare;
        private String updateSound = "";
        private String completeSound = "";
        private QuestParticipantScope participantScope = QuestParticipantScope.PLAYER;

        public Builder visibility(QuestVisibility value) { visibility = value; return this; }
        public Builder iconReference(String value) { iconReference = value; return this; }
        public Builder main(boolean value) { main = value; return this; }
        public Builder silent(boolean value) { silent = value; return this; }
        public Builder autoClaim(boolean value) { autoClaim = value; return this; }
        public Builder progressWhileLocked(boolean value) { progressWhileLocked = value; return this; }
        public Builder simultaneous(boolean value) { simultaneous = value; return this; }
        public Builder global(boolean value) { global = value; return this; }
        public Builder globalShare(boolean value) { globalShare = value; return this; }
        public Builder updateSound(String value) { updateSound = value; return this; }
        public Builder completeSound(String value) { completeSound = value; return this; }
        public Builder participantScope(QuestParticipantScope value) { participantScope = value; return this; }
        public QuestBehavior build() { return new QuestBehavior(this); }
    }
}

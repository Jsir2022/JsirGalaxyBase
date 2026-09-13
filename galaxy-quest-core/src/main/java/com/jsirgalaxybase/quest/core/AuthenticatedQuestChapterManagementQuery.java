package com.jsirgalaxybase.quest.core;

import java.util.UUID;

public final class AuthenticatedQuestChapterManagementQuery {
    public enum Status { SUCCESS, FORBIDDEN, NOT_FOUND }

    public static final class Result {
        private final Status status;
        private final QuestChapterManagementPage page;
        private final StoredQuestChapter chapter;

        private Result(Status status, QuestChapterManagementPage page, StoredQuestChapter chapter) {
            this.status = status;
            this.page = page;
            this.chapter = chapter;
        }

        public Status getStatus() { return status; }
        public QuestChapterManagementPage getPage() { return page; }
        public StoredQuestChapter getChapter() { return chapter; }
    }

    private final QuestChapterManagementQuery query;
    private final QuestEditorAuthorization authorization;

    public AuthenticatedQuestChapterManagementQuery(QuestChapterManagementQuery query,
        QuestEditorAuthorization authorization) {
        if (query == null || authorization == null) throw new IllegalArgumentException("dependencies are required");
        this.query = query;
        this.authorization = authorization;
    }

    public Result load(QuestEditorActor actor, QuestDefinitionManagementRequest request) {
        if (!allowed(actor)) return new Result(Status.FORBIDDEN, null, null);
        if (request == null) throw new IllegalArgumentException("request is required");
        return new Result(Status.SUCCESS, query.load(request), null);
    }

    public Result find(QuestEditorActor actor, UUID id, int version) {
        if (!allowed(actor)) return new Result(Status.FORBIDDEN, null, null);
        if (id == null || version < 1) throw new IllegalArgumentException("valid chapter identity is required");
        java.util.Optional<StoredQuestChapter> found = query.find(id, version);
        return found.isPresent()
            ? new Result(Status.SUCCESS, null, found.get())
            : new Result(Status.NOT_FOUND, null, null);
    }

    private boolean allowed(QuestEditorActor actor) {
        return actor != null && authorization.canManageDefinitions(actor);
    }
}

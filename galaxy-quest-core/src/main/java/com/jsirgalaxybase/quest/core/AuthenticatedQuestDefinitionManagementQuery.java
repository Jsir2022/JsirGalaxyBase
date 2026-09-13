package com.jsirgalaxybase.quest.core;

public final class AuthenticatedQuestDefinitionManagementQuery {
    public enum Status { SUCCESS, FORBIDDEN }

    public static final class Result {
        private final Status status;
        private final QuestDefinitionManagementPage page;
        private Result(Status status, QuestDefinitionManagementPage page) { this.status = status; this.page = page; }
        public Status getStatus() { return status; }
        public QuestDefinitionManagementPage getPage() { return page; }
    }
    public static final class DefinitionResult {
        public enum Status { SUCCESS, FORBIDDEN, NOT_FOUND }
        private final Status status;private final StoredQuestDefinition definition;
        private DefinitionResult(Status status,StoredQuestDefinition definition){this.status=status;this.definition=definition;}
        public Status getStatus(){return status;}public StoredQuestDefinition getDefinition(){return definition;}
    }

    private final QuestDefinitionManagementQuery query;
    private final QuestEditorAuthorization authorization;

    public AuthenticatedQuestDefinitionManagementQuery(QuestDefinitionManagementQuery query,
        QuestEditorAuthorization authorization) {
        if (query == null || authorization == null) throw new IllegalArgumentException("dependencies are required");
        this.query = query;
        this.authorization = authorization;
    }

    public Result load(QuestEditorActor actor, QuestDefinitionManagementRequest request) {
        if (actor == null || !authorization.canManageDefinitions(actor)) return new Result(Status.FORBIDDEN, null);
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return new Result(Status.SUCCESS, query.load(request));
    }

    public DefinitionResult find(QuestEditorActor actor,java.util.UUID questId,int version){
        if(actor==null||!authorization.canManageDefinitions(actor))return new DefinitionResult(DefinitionResult.Status.FORBIDDEN,null);
        if(questId==null||version<1)throw new IllegalArgumentException("quest and version are required");
        java.util.Optional<StoredQuestDefinition> found=query.find(questId,version);
        return found.isPresent()?new DefinitionResult(DefinitionResult.Status.SUCCESS,found.get()):new DefinitionResult(DefinitionResult.Status.NOT_FOUND,null);
    }
}

package com.jsirgalaxybase.quest.core;
import java.util.Collections;import java.util.List;
public final class AuthenticatedQuestContentMigrationQuery {
    private final QuestContentMigrationQuery query;private final QuestEditorAuthorization authorization;
    public AuthenticatedQuestContentMigrationQuery(QuestContentMigrationQuery query,QuestEditorAuthorization authorization){if(query==null||authorization==null)throw new IllegalArgumentException("dependencies are required");this.query=query;this.authorization=authorization;}
    public List<QuestContentMigrationSummary> listRecent(QuestEditorActor actor,int limit){if(actor==null||!authorization.canManageDefinitions(actor))return Collections.emptyList();return query.listRecent(Math.max(1,Math.min(20,limit)));}
}

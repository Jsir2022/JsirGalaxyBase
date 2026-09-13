package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.UUID;
import java.util.Optional;

import org.junit.Test;

public class AuthenticatedQuestDefinitionManagementQueryTest {
    @Test
    public void refusesUnauthorizedActorsWithoutCallingStorage() {
        CountingQuery query = new CountingQuery();
        AuthenticatedQuestDefinitionManagementQuery service = service(query, false);
        AuthenticatedQuestDefinitionManagementQuery.Result result = service.load(actor(), request());
        assertEquals(AuthenticatedQuestDefinitionManagementQuery.Status.FORBIDDEN, result.getStatus());
        assertEquals(0, query.calls);
    }

    @Test
    public void returnsBoundedPageForAuthorizedActor() {
        CountingQuery query = new CountingQuery();
        AuthenticatedQuestDefinitionManagementQuery.Result result = service(query, true).load(actor(), request());
        assertEquals(AuthenticatedQuestDefinitionManagementQuery.Status.SUCCESS, result.getStatus());
        assertEquals(1, query.calls);
        assertEquals(51L, result.getPage().getTotal());
        assertTrue(result.getPage().hasNext());
        assertFalse(result.getPage().hasPrevious());
    }

    @Test
    public void requestBoundsSearchPageAndPageSize() {
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < 100; i++) query.append('x');
        QuestDefinitionManagementRequest request = new QuestDefinitionManagementRequest(query.toString(), null,
            -4, 500);
        assertEquals(80, request.getQuery().length());
        assertEquals(0, request.getPage());
        assertEquals(50, request.getPageSize());
    }

    @Test public void detailLookupUsesSameAuthorizationBoundary(){CountingQuery query=new CountingQuery();UUID id=UUID.randomUUID();assertEquals(AuthenticatedQuestDefinitionManagementQuery.DefinitionResult.Status.FORBIDDEN,service(query,false).find(actor(),id,1).getStatus());assertEquals(0,query.findCalls);assertEquals(AuthenticatedQuestDefinitionManagementQuery.DefinitionResult.Status.NOT_FOUND,service(query,true).find(actor(),id,1).getStatus());assertEquals(1,query.findCalls);}

    private static AuthenticatedQuestDefinitionManagementQuery service(CountingQuery query, final boolean allowed) {
        return new AuthenticatedQuestDefinitionManagementQuery(query, new QuestEditorAuthorization() {
            @Override public boolean canManageDefinitions(QuestEditorActor actor) { return allowed; }
        });
    }

    private static QuestEditorActor actor() { return new QuestEditorActor(UUID.randomUUID(), "operator"); }
    private static QuestDefinitionManagementRequest request() {
        return new QuestDefinitionManagementRequest("", QuestDefinitionLifecycle.DRAFT, 0, 20);
    }

    private static final class CountingQuery implements QuestDefinitionManagementQuery {
        int calls;
        int findCalls;
        @Override public QuestDefinitionManagementPage load(QuestDefinitionManagementRequest request) {
            calls++;
            return new QuestDefinitionManagementPage(Collections.<StoredQuestDefinition>emptyList(),
                request.getPage(), request.getPageSize(), 51L);
        }
        @Override public Optional<StoredQuestDefinition> find(UUID id,int version){findCalls++;return Optional.empty();}
    }
}

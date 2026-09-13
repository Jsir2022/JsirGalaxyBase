package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

public class AuthenticatedQuestChapterManagementQueryTest {
    @Test
    public void refusesUnauthorizedActorsBeforeCallingStorage() {
        CountingQuery query = new CountingQuery();
        AuthenticatedQuestChapterManagementQuery service = service(query, false);
        assertEquals(AuthenticatedQuestChapterManagementQuery.Status.FORBIDDEN,
            service.load(actor(), request()).getStatus());
        assertEquals(AuthenticatedQuestChapterManagementQuery.Status.FORBIDDEN,
            service.find(actor(), UUID.randomUUID(), 1).getStatus());
        assertEquals(0, query.loadCalls);
        assertEquals(0, query.findCalls);
    }

    @Test
    public void returnsBoundedPageAndAuthorizedDetail() {
        CountingQuery query = new CountingQuery();
        AuthenticatedQuestChapterManagementQuery service = service(query, true);
        AuthenticatedQuestChapterManagementQuery.Result page = service.load(actor(), request());
        assertEquals(AuthenticatedQuestChapterManagementQuery.Status.SUCCESS, page.getStatus());
        assertEquals(51L, page.getPage().getTotal());
        assertFalse(page.getPage().hasPrevious());

        UUID id = UUID.randomUUID();
        query.chapter = new StoredQuestChapter(new QuestChapterDefinition(id, 1, "Chapter", "", "", "",
            Collections.<QuestChapterEntry>emptyList()), QuestDefinitionLifecycle.DRAFT, "hash", 0L);
        AuthenticatedQuestChapterManagementQuery.Result detail = service.find(actor(), id, 1);
        assertEquals(AuthenticatedQuestChapterManagementQuery.Status.SUCCESS, detail.getStatus());
        assertNotNull(detail.getChapter());
    }

    private static AuthenticatedQuestChapterManagementQuery service(CountingQuery query, final boolean allowed) {
        return new AuthenticatedQuestChapterManagementQuery(query, new QuestEditorAuthorization() {
            @Override public boolean canManageDefinitions(QuestEditorActor actor) { return allowed; }
        });
    }

    private static QuestEditorActor actor() { return new QuestEditorActor(UUID.randomUUID(), "operator"); }

    private static QuestDefinitionManagementRequest request() {
        return new QuestDefinitionManagementRequest("", QuestDefinitionLifecycle.DRAFT, 0, 20);
    }

    private static final class CountingQuery implements QuestChapterManagementQuery {
        private int loadCalls;
        private int findCalls;
        private StoredQuestChapter chapter;

        @Override
        public QuestChapterManagementPage load(QuestDefinitionManagementRequest request) {
            loadCalls++;
            return new QuestChapterManagementPage(Collections.<StoredQuestChapter>emptyList(),
                request.getPage(), request.getPageSize(), 51L);
        }

        @Override
        public Optional<StoredQuestChapter> find(UUID chapterId, int version) {
            findCalls++;
            return chapter == null ? Optional.<StoredQuestChapter>empty() : Optional.of(chapter);
        }
    }
}

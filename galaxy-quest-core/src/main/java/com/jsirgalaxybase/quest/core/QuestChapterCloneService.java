package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-authoritative equivalent of BetterQuesting's selected-task copy tool. It never trusts client positions,
 * source definitions, or IDs: source layouts and definitions are read from repositories before one transaction
 * creates drafts and rewrites the target chapter.
 */
public final class QuestChapterCloneService {
    private final QuestChapterRepository chapters;
    private final QuestDefinitionRepository definitions;
    private final QuestTransaction transaction;
    private final QuestEditorAuthorization authorization;
    private final QuestDefinitionValidator validator = new QuestDefinitionValidator();
    private final QuestEditorTypeRegistry types;
    private final QuestDefinitionBatchCloner cloner;

    public QuestChapterCloneService(QuestChapterRepository chapters, QuestDefinitionRepository definitions,
        QuestTransaction transaction, QuestEditorAuthorization authorization, QuestEditorTypeRegistry types) {
        this(chapters, definitions, transaction, authorization, types, new QuestDefinitionBatchCloner.IdSource() {
            @Override public UUID next() { return UUID.randomUUID(); }
        });
    }

    QuestChapterCloneService(QuestChapterRepository chapters, QuestDefinitionRepository definitions,
        QuestTransaction transaction, QuestEditorAuthorization authorization, QuestEditorTypeRegistry types,
        QuestDefinitionBatchCloner.IdSource ids) {
        if (chapters == null || definitions == null || transaction == null || authorization == null || types == null) {
            throw new IllegalArgumentException("dependencies are required");
        }
        this.chapters = chapters; this.definitions = definitions; this.transaction = transaction;
        this.authorization = authorization; this.types = types; this.cloner = new QuestDefinitionBatchCloner(ids);
    }

    public QuestChapterCloneResult cloneIntoChapter(QuestEditorActor actor, QuestChapterCloneRequest request) {
        if (actor == null || !authorization.canManageDefinitions(actor)) {
            return QuestChapterCloneResult.status(QuestChapterCloneResult.Status.FORBIDDEN);
        }
        if (request == null) return QuestChapterCloneResult.status(QuestChapterCloneResult.Status.INVALID);
        Optional<StoredQuestChapter> stored = chapters.find(request.getChapterId(), request.getChapterVersion());
        if (!stored.isPresent()) return QuestChapterCloneResult.status(QuestChapterCloneResult.Status.NOT_FOUND);
        if (stored.get().getLifecycle() != QuestDefinitionLifecycle.DRAFT) {
            return QuestChapterCloneResult.status(QuestChapterCloneResult.Status.CONFLICT);
        }

        List<QuestDefinition> sourceDefinitions = new ArrayList<QuestDefinition>();
        Map<UUID, QuestChapterEntry> sourceEntries = entries(stored.get().getDefinition());
        for (QuestDefinitionCloneRequest.Source source : request.getSources().getSources()) {
            if (!sourceEntries.containsKey(source.getQuestId())) return QuestChapterCloneResult.status(QuestChapterCloneResult.Status.INVALID);
            Optional<StoredQuestDefinition> definition = definitions.find(source.getQuestId(), source.getVersion());
            if (!definition.isPresent()) return QuestChapterCloneResult.status(QuestChapterCloneResult.Status.NOT_FOUND);
            sourceDefinitions.add(definition.get().getDefinition());
        }

        final QuestDefinitionBatchCloner.Result plan;
        final QuestChapterDefinition revised;
        try {
            plan = cloner.clone(sourceDefinitions);
            for (QuestDefinition definition : plan.getDefinitions()) if (!validator.validate(definition, types).isEmpty()) {
                return QuestChapterCloneResult.status(QuestChapterCloneResult.Status.INVALID);
            }
            QuestChapterEntry origin = sourceEntries.get(request.getSources().getSources().get(0).getQuestId());
            QuestChapterDraftEditor editor = QuestChapterDraftEditor.edit(stored.get().getDefinition());
            for (QuestDefinitionCloneRequest.Source source : request.getSources().getSources()) {
                QuestChapterEntry entry = sourceEntries.get(source.getQuestId());
                UUID cloneId = plan.getRemappedIds().get(source.getQuestId());
                editor.place(new QuestChapterEntry(cloneId, safeAdd(request.getAnchorX(), entry.getX() - origin.getX()),
                    safeAdd(request.getAnchorY(), entry.getY() - origin.getY()), entry.getWidth(), entry.getHeight()));
            }
            revised = editor.build(); validateLayout(revised);
        } catch (RuntimeException invalid) {
            return QuestChapterCloneResult.status(QuestChapterCloneResult.Status.INVALID);
        }

        try {
            StoredQuestChapter changed = transaction.inTransaction(new QuestTransaction.Work<StoredQuestChapter>() {
                @Override public StoredQuestChapter execute() {
                    for (QuestDefinition definition : plan.getDefinitions()) definitions().createDraft(definition);
                    return chapters.updateDraft(revised, request.getExpectedChapterHash());
                }
                private QuestDefinitionRepository definitions() { return QuestChapterCloneService.this.definitions; }
            });
            return QuestChapterCloneResult.success(changed, plan.getRemappedIds());
        } catch (QuestDefinitionConflictException conflict) {
            return QuestChapterCloneResult.status(QuestChapterCloneResult.Status.CONFLICT);
        }
    }

    private static Map<UUID, QuestChapterEntry> entries(QuestChapterDefinition chapter) {
        Map<UUID, QuestChapterEntry> result = new LinkedHashMap<UUID, QuestChapterEntry>();
        for (QuestChapterEntry entry : chapter.getEntries()) result.put(entry.getQuestId(), entry);
        return result;
    }

    private static int safeAdd(int left, int right) {
        long result = (long)left + right;
        if (result < -1000000L || result > 1000000L) throw new IllegalArgumentException("chapter placement exceeds bounds");
        return (int)result;
    }

    private static void validateLayout(QuestChapterDefinition value) {
        if (value.getEntries().size() > 4096) throw new IllegalArgumentException("too many chapter entries");
        for (QuestChapterEntry entry : value.getEntries()) if (Math.abs((long)entry.getX()) > 1000000L
            || Math.abs((long)entry.getY()) > 1000000L || entry.getWidth() > 1024 || entry.getHeight() > 1024) {
            throw new IllegalArgumentException("chapter placement exceeds bounds");
        }
    }
}

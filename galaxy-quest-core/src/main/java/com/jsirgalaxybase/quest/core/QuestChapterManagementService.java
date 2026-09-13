package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative lifecycle and semantic editing boundary for chapter definitions. */
public final class QuestChapterManagementService {
    private final QuestChapterRepository chapters;
    private final QuestDefinitionRepository quests;
    private final QuestTransaction transaction;
    private final QuestEditorAuthorization authorization;

    public QuestChapterManagementService(QuestChapterRepository chapters, QuestDefinitionRepository quests,
        QuestTransaction transaction, QuestEditorAuthorization authorization) {
        if (chapters == null || quests == null || transaction == null || authorization == null) {
            throw new IllegalArgumentException("dependencies are required");
        }
        this.chapters = chapters;
        this.quests = quests;
        this.transaction = transaction;
        this.authorization = authorization;
    }

    public QuestChapterManagementResult create(QuestEditorActor actor, String name) {
        if (!allowed(actor)) return status(QuestChapterManagementResult.Status.FORBIDDEN);
        if (name == null || name.trim().isEmpty() || name.trim().length() > 256) {
            return QuestChapterManagementResult.invalid("name must contain 1 to 256 characters");
        }
        final QuestChapterDefinition value = new QuestChapterDefinition(UUID.randomUUID(), 1, name.trim(), "", "",
            "", Collections.<QuestChapterEntry>emptyList());
        try {
            return QuestChapterManagementResult.success(transaction.inTransaction(
                new QuestTransaction.Work<StoredQuestChapter>() {
                    @Override public StoredQuestChapter execute() { return chapters.createDraft(value); }
                }));
        } catch (QuestDefinitionConflictException conflict) {
            return status(QuestChapterManagementResult.Status.CONFLICT);
        }
    }

    public QuestChapterManagementResult apply(QuestEditorActor actor, QuestChapterEditRequest request) {
        if (!allowed(actor)) return status(QuestChapterManagementResult.Status.FORBIDDEN);
        if (request == null) return QuestChapterManagementResult.invalid("request is required");
        Optional<StoredQuestChapter> stored = chapters.find(request.getChapterId(), request.getVersion());
        if (!stored.isPresent()) return status(QuestChapterManagementResult.Status.NOT_FOUND);
        if (stored.get().getLifecycle() != QuestDefinitionLifecycle.DRAFT) {
            return status(QuestChapterManagementResult.Status.CONFLICT);
        }
        QuestChapterDraftEditor editor = QuestChapterDraftEditor.edit(stored.get().getDefinition());
        try {
            for (QuestChapterEditOperation operation : request.getOperations()) apply(editor, operation);
            final QuestChapterDefinition result = editor.build();
            validate(result);
            return QuestChapterManagementResult.success(transaction.inTransaction(
                new QuestTransaction.Work<StoredQuestChapter>() {
                    @Override public StoredQuestChapter execute() {
                        return chapters.updateDraft(result, request.getExpectedHash());
                    }
                }));
        } catch (QuestDefinitionConflictException conflict) {
            return status(QuestChapterManagementResult.Status.CONFLICT);
        } catch (RuntimeException invalid) {
            return QuestChapterManagementResult.invalid(invalid.getMessage());
        }
    }

    public QuestChapterManagementResult publish(QuestEditorActor actor, final UUID id, final int version,
        final String hash, final long at) {
        if (!allowed(actor)) return status(QuestChapterManagementResult.Status.FORBIDDEN);
        if (id == null || version < 1 || hash == null || hash.isEmpty() || at < 0L) {
            return QuestChapterManagementResult.invalid("valid chapter identity, hash, and timestamp are required");
        }
        Optional<StoredQuestChapter> stored = chapters.find(id, version);
        if (!stored.isPresent()) return status(QuestChapterManagementResult.Status.NOT_FOUND);
        try {
            validate(stored.get().getDefinition());
            for (QuestChapterEntry entry : stored.get().getDefinition().getEntries()) {
                if (!quests.findPublished(entry.getQuestId()).isPresent()) {
                    return QuestChapterManagementResult.invalid("placed quest is not published: " + entry.getQuestId());
                }
            }
            Boolean changed = transaction.inTransaction(new QuestTransaction.Work<Boolean>() {
                @Override public Boolean execute() { return Boolean.valueOf(chapters.publish(id, version, hash, at)); }
            });
            if (!changed.booleanValue()) return status(QuestChapterManagementResult.Status.CONFLICT);
            Optional<StoredQuestChapter> published = chapters.find(id, version);
            return published.isPresent()
                ? QuestChapterManagementResult.success(published.get())
                : status(QuestChapterManagementResult.Status.NOT_FOUND);
        } catch (RuntimeException invalid) {
            return QuestChapterManagementResult.invalid(invalid.getMessage());
        }
    }

    public QuestChapterManagementResult retire(QuestEditorActor actor, final UUID id, final int version) {
        if (!allowed(actor)) return status(QuestChapterManagementResult.Status.FORBIDDEN);
        if (id == null || version < 1) return QuestChapterManagementResult.invalid("valid chapter identity is required");
        Boolean changed = transaction.inTransaction(new QuestTransaction.Work<Boolean>() {
            @Override public Boolean execute() { return Boolean.valueOf(chapters.retire(id, version)); }
        });
        if (!changed.booleanValue()) return status(QuestChapterManagementResult.Status.CONFLICT);
        Optional<StoredQuestChapter> stored = chapters.find(id, version);
        return stored.isPresent()
            ? QuestChapterManagementResult.success(stored.get())
            : status(QuestChapterManagementResult.Status.NOT_FOUND);
    }

    private static void apply(QuestChapterDraftEditor editor, QuestChapterEditOperation operation) {
        if (operation == null) throw new IllegalArgumentException("chapter operation is required");
        switch (operation.getKind()) {
            case SET_NAME: editor.name(operation.getText()); break;
            case SET_DESCRIPTION: editor.description(operation.getText()); break;
            case SET_ICON: editor.iconReference(operation.getText()); break;
            case SET_BACKGROUND: editor.backgroundReference(operation.getText()); break;
            case SET_BACKGROUND_SIZE: editor.backgroundSize(operation.getFirst()); break;
            case SET_VISIBILITY: editor.visibility(QuestVisibility.valueOf(operation.getText())); break;
            case PLACE:
                editor.place(new QuestChapterEntry(operation.getQuestId(), operation.getFirst(), operation.getSecond(),
                    operation.getThird(), operation.getFourth()));
                break;
            case MOVE: editor.move(operation.getQuestId(), operation.getFirst(), operation.getSecond()); break;
            case RESIZE: editor.resize(operation.getQuestId(), operation.getFirst(), operation.getSecond()); break;
            case REMOVE: editor.remove(operation.getQuestId()); break;
            default: throw new IllegalArgumentException("unsupported chapter operation");
        }
    }

    private static void validate(QuestChapterDefinition value) {
        if (value.getDescription().length() > 4096 || value.getIconReference().length() > 256
            || value.getBackgroundReference().length() > 256) {
            throw new IllegalArgumentException("chapter text exceeds limit");
        }
        if (value.getBackgroundSize() > 4096) throw new IllegalArgumentException("background size exceeds limit");
        if (value.getEntries().size() > 4096) throw new IllegalArgumentException("too many chapter entries");
        for (QuestChapterEntry entry : value.getEntries()) {
            if (Math.abs((long) entry.getX()) > 1000000L || Math.abs((long) entry.getY()) > 1000000L
                || entry.getWidth() > 1024 || entry.getHeight() > 1024) {
                throw new IllegalArgumentException("chapter placement exceeds bounds");
            }
        }
    }

    private static QuestChapterManagementResult status(QuestChapterManagementResult.Status value) {
        return QuestChapterManagementResult.status(value);
    }

    private boolean allowed(QuestEditorActor actor) {
        return actor != null && authorization.canManageDefinitions(actor);
    }
}

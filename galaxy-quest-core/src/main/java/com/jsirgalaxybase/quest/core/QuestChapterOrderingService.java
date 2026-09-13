package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Atomic, complete-catalog reorder command. It never infers order from canvas coordinates. */
public final class QuestChapterOrderingService {
    private final QuestChapterCatalogRepository catalog;
    private final QuestTransaction transaction;
    private final QuestEditorAuthorization authorization;

    public QuestChapterOrderingService(QuestChapterCatalogRepository catalog, QuestTransaction transaction,
        QuestEditorAuthorization authorization) {
        if (catalog == null || transaction == null || authorization == null) throw new IllegalArgumentException("dependencies are required");
        this.catalog = catalog; this.transaction = transaction; this.authorization = authorization;
    }

    public QuestChapterOrderingResult reorder(QuestEditorActor actor, final List<UUID> orderedChapterIds) {
        if (actor == null || !authorization.canManageDefinitions(actor)) return QuestChapterOrderingResult.status(QuestChapterOrderingResult.Status.FORBIDDEN, "not authorized");
        if (orderedChapterIds == null || orderedChapterIds.size() > 4096) return QuestChapterOrderingResult.status(QuestChapterOrderingResult.Status.INVALID, "order must contain at most 4096 chapters");
        final List<UUID> requested = new ArrayList<UUID>(orderedChapterIds);
        if (new HashSet<UUID>(requested).size() != requested.size() || requested.contains(null)) return QuestChapterOrderingResult.status(QuestChapterOrderingResult.Status.INVALID, "chapter order contains duplicate or null identity");
        try {
            return transaction.inTransaction(new QuestTransaction.Work<QuestChapterOrderingResult>() {
                @Override public QuestChapterOrderingResult execute() {
                    List<QuestChapterCatalogEntry> current = catalog.listOrdered();
                    Set<UUID> expected = new HashSet<UUID>(); for (QuestChapterCatalogEntry entry : current) expected.add(entry.getChapterId());
                    if (!expected.equals(new HashSet<UUID>(requested))) return QuestChapterOrderingResult.status(
                        QuestChapterOrderingResult.Status.INVALID, "order must contain every known chapter exactly once");
                    catalog.replaceOrder(requested);
                    return QuestChapterOrderingResult.success(catalog.listOrdered());
                }
            });
        } catch (RuntimeException invalid) { return QuestChapterOrderingResult.status(QuestChapterOrderingResult.Status.INVALID, invalid.getMessage()); }
    }

    /** Moves one catalog identity before another; a null target appends it. */
    public QuestChapterOrderingResult moveBefore(QuestEditorActor actor, final UUID chapterId, final UUID beforeChapterId) {
        if (actor == null || !authorization.canManageDefinitions(actor)) return QuestChapterOrderingResult.status(QuestChapterOrderingResult.Status.FORBIDDEN, "not authorized");
        if (chapterId == null || chapterId.equals(beforeChapterId)) return QuestChapterOrderingResult.status(QuestChapterOrderingResult.Status.INVALID, "distinct chapter identities are required");
        try {
            return transaction.inTransaction(new QuestTransaction.Work<QuestChapterOrderingResult>() {
                @Override public QuestChapterOrderingResult execute() {
                    List<QuestChapterCatalogEntry> current = catalog.listOrdered(); List<UUID> reordered = new ArrayList<UUID>();
                    for (QuestChapterCatalogEntry entry : current) reordered.add(entry.getChapterId());
                    if (!reordered.remove(chapterId) || (beforeChapterId != null && !reordered.contains(beforeChapterId))) return QuestChapterOrderingResult.status(QuestChapterOrderingResult.Status.INVALID, "chapter is not present in the catalog");
                    if (beforeChapterId == null) reordered.add(chapterId); else reordered.add(reordered.indexOf(beforeChapterId), chapterId);
                    catalog.replaceOrder(reordered);
                    return QuestChapterOrderingResult.success(catalog.listOrdered());
                }
            });
        } catch (RuntimeException invalid) { return QuestChapterOrderingResult.status(QuestChapterOrderingResult.Status.INVALID, invalid.getMessage()); }
    }
}

package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestChapterDefinition;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.StoredQuestChapter;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.postgres.JdbcQuestChapterRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestContentMigrationRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestDefinitionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestTransaction;
import com.jsirgalaxybase.quest.postgres.QuestContentHashes;
import com.jsirgalaxybase.quest.postgres.QuestContentMigrationBatch;
import com.jsirgalaxybase.quest.postgres.QuestContentMigrationItem;

/** Transactional bridge kept in the offline importer module, never in the Base runtime jar. */
public final class BqMigrationDatabaseService {
    private final JdbcQuestTransaction transaction; private final JdbcQuestDefinitionRepository definitions;
    private final JdbcQuestChapterRepository chapters; private final JdbcQuestContentMigrationRepository migrations;
    public BqMigrationDatabaseService(JdbcQuestTransaction transaction,JdbcQuestDefinitionRepository definitions,JdbcQuestChapterRepository chapters,JdbcQuestContentMigrationRepository migrations){
        this.transaction=transaction;this.definitions=definitions;this.chapters=chapters;this.migrations=migrations;
    }
    public BqMigrationDryRun dryRun(BqMigrationManifest manifest){return plan(manifest);}
    /** Explicitly records an immutable preview for UI2. Unlike dryRun, this is a database mutation. */
    public QuestContentMigrationBatch stage(final BqMigrationManifest manifest){return transaction.inTransaction(()->{
        migrations.lock();Optional<QuestContentMigrationBatch> existing=migrations.find(manifest.getBatchId());if(existing.isPresent())return existing.get();
        BqMigrationDryRun planned=plan(manifest);QuestContentMigrationBatch batch=batch(manifest,manifest.isApplicable()?QuestContentMigrationBatch.Status.PREVIEW:QuestContentMigrationBatch.Status.BLOCKED,"",planned.getItems());migrations.insert(batch);return batch;
    });}
    public QuestContentMigrationBatch apply(final BqMigrationManifest manifest,final long now){
        if(!manifest.isApplicable())throw new IllegalArgumentException("blocked migration manifest");
        Optional<QuestContentMigrationBatch> prior=migrations.find(manifest.getBatchId());
        if(prior.isPresent()&&prior.get().getStatus()==QuestContentMigrationBatch.Status.APPLIED)return prior.get();
        if(prior.isPresent()&&prior.get().getStatus()!=QuestContentMigrationBatch.Status.PREVIEW)throw new IllegalStateException("migration batch is not applicable: "+prior.get().getStatus());
        return transaction.inTransaction(()->{
            migrations.lock();Optional<QuestContentMigrationBatch> concurrent=migrations.find(manifest.getBatchId());if(concurrent.isPresent()&&concurrent.get().getStatus()==QuestContentMigrationBatch.Status.APPLIED)return concurrent.get();if(concurrent.isPresent()&&concurrent.get().getStatus()!=QuestContentMigrationBatch.Status.PREVIEW)throw new IllegalStateException("migration batch is not applicable: "+concurrent.get().getStatus());
            BqMigrationDryRun planned=plan(manifest);List<QuestContentMigrationItem> applied=new ArrayList<QuestContentMigrationItem>();
            for(BqImportedQuest imported:manifest.getQuests()){
                QuestContentMigrationItem item=find(planned,QuestContentMigrationItem.Kind.QUEST,imported.getDefinition().getId());
                if(item.getAction()!=QuestContentMigrationItem.Action.UNCHANGED){QuestDefinition value=definitionVersion(imported.getDefinition(),item.getVersion());StoredQuestDefinition draft=definitions.createDraft(value);if(!definitions.publish(value.getId(),value.getVersion(),draft.getContentHash(),now))throw new IllegalStateException("quest publish conflict: "+value.getId());}
                applied.add(item);
            }
            List<UUID> previous=new ArrayList<UUID>();for(com.jsirgalaxybase.quest.core.QuestChapterCatalogEntry entry:chapters.listOrdered())previous.add(entry.getChapterId());
            List<BqImportedChapter> ordered=new ArrayList<BqImportedChapter>(manifest.getChapters());Collections.sort(ordered,Comparator.comparingInt(BqImportedChapter::getDisplayOrder));
            for(BqImportedChapter imported:ordered){QuestContentMigrationItem item=find(planned,QuestContentMigrationItem.Kind.CHAPTER,imported.getDefinition().getId());
                if(item.getAction()!=QuestContentMigrationItem.Action.UNCHANGED){QuestChapterDefinition value=chapterVersion(imported.getDefinition(),item.getVersion());StoredQuestChapter draft=chapters.createDraft(value);if(!chapters.publish(value.getId(),value.getVersion(),draft.getContentHash(),now))throw new IllegalStateException("chapter publish conflict: "+value.getId());}applied.add(item);}
            List<UUID> desired=new ArrayList<UUID>();for(BqImportedChapter imported:ordered)if(!desired.contains(imported.getDefinition().getId()))desired.add(imported.getDefinition().getId());for(UUID id:previous)if(!desired.contains(id))desired.add(id);
            chapters.replaceOrder(desired);
            QuestContentMigrationBatch batch=batch(manifest,QuestContentMigrationBatch.Status.APPLIED,join(previous),applied);if(concurrent.isPresent())migrations.promoteApplied(batch);else migrations.insert(batch);return batch;
        });
    }
    public void rollback(final String batchId){transaction.inTransaction(()->{QuestContentMigrationBatch batch=migrations.find(batchId).orElseThrow(()->new IllegalArgumentException("unknown migration batch"));if(batch.getStatus()==QuestContentMigrationBatch.Status.ROLLED_BACK)return null;if(batch.getStatus()!=QuestContentMigrationBatch.Status.APPLIED)throw new IllegalStateException("only applied migration batches can be rolled back");
        for(QuestContentMigrationItem item:batch.getItems())if(item.getAction()!=QuestContentMigrationItem.Action.UNCHANGED){if(item.getKind()==QuestContentMigrationItem.Kind.QUEST){StoredQuestDefinition latest=definitions.findPublished(item.getId()).orElseThrow(()->new IllegalStateException("published quest missing"));if(latest.getDefinition().getVersion()!=item.getVersion())throw new IllegalStateException("later quest version depends on migration");}else{StoredQuestChapter latest=chapters.findPublished(item.getId()).orElseThrow(()->new IllegalStateException("published chapter missing"));if(latest.getDefinition().getVersion()!=item.getVersion())throw new IllegalStateException("later chapter version depends on migration");}}
        java.util.Set<UUID> createdQuests=new java.util.HashSet<UUID>(),createdChapters=new java.util.HashSet<UUID>();for(QuestContentMigrationItem item:batch.getItems())if(item.getAction()==QuestContentMigrationItem.Action.CREATED){if(item.getKind()==QuestContentMigrationItem.Kind.QUEST)createdQuests.add(item.getId());else createdChapters.add(item.getId());}
        for(StoredQuestDefinition later:definitions.findAllPublished())if(!createdQuests.contains(later.getDefinition().getId()))for(UUID prerequisite:later.getDefinition().getPrerequisites())if(createdQuests.contains(prerequisite))throw new IllegalStateException("later quest depends on migration: "+later.getDefinition().getId());
        for(StoredQuestChapter later:chapters.findAllPublished())if(!createdChapters.contains(later.getDefinition().getId()))for(com.jsirgalaxybase.quest.core.QuestChapterEntry entry:later.getDefinition().getEntries())if(createdQuests.contains(entry.getQuestId()))throw new IllegalStateException("later chapter depends on migration: "+later.getDefinition().getId());
        for(QuestContentMigrationItem item:batch.getItems())if(item.getAction()!=QuestContentMigrationItem.Action.UNCHANGED){boolean retired=item.getKind()==QuestContentMigrationItem.Kind.QUEST?definitions.retire(item.getId(),item.getVersion()):chapters.retire(item.getId(),item.getVersion());if(!retired)throw new IllegalStateException("migration item could not be retired");}migrations.restoreChapterCatalog(batch.getPreviousChapterOrder(),batch.getItems());migrations.markRolledBack(batchId);return null;});}
    private BqMigrationDryRun plan(BqMigrationManifest manifest){List<QuestContentMigrationItem> items=new ArrayList<QuestContentMigrationItem>();for(BqImportedQuest imported:manifest.getQuests()){QuestDefinition source=imported.getDefinition();Optional<StoredQuestDefinition> current=definitions.findPublished(source.getId());int version=current.isPresent()?current.get().getDefinition().getVersion():1;String comparable=QuestContentHashes.definition(definitionVersion(source,version));boolean same=current.isPresent()&&current.get().getContentHash().equals(comparable);int target=same?version:(current.isPresent()?version+1:1);QuestContentMigrationItem.Action action=same?QuestContentMigrationItem.Action.UNCHANGED:(current.isPresent()?QuestContentMigrationItem.Action.VERSIONED:QuestContentMigrationItem.Action.CREATED);items.add(new QuestContentMigrationItem(QuestContentMigrationItem.Kind.QUEST,source.getId(),target,action,QuestContentHashes.definition(definitionVersion(source,target))));}
        for(BqImportedChapter imported:manifest.getChapters()){QuestChapterDefinition source=imported.getDefinition();Optional<StoredQuestChapter> current=chapters.findPublished(source.getId());int version=current.isPresent()?current.get().getDefinition().getVersion():1;String comparable=QuestContentHashes.chapter(chapterVersion(source,version));boolean same=current.isPresent()&&current.get().getContentHash().equals(comparable);int target=same?version:(current.isPresent()?version+1:1);QuestContentMigrationItem.Action action=same?QuestContentMigrationItem.Action.UNCHANGED:(current.isPresent()?QuestContentMigrationItem.Action.VERSIONED:QuestContentMigrationItem.Action.CREATED);items.add(new QuestContentMigrationItem(QuestContentMigrationItem.Kind.CHAPTER,source.getId(),target,action,QuestContentHashes.chapter(chapterVersion(source,target))));}return new BqMigrationDryRun(manifest.getBatchId(),manifest.isApplicable(),items);}
    private static QuestContentMigrationItem find(BqMigrationDryRun plan,QuestContentMigrationItem.Kind kind,UUID id){for(QuestContentMigrationItem item:plan.getItems())if(item.getKind()==kind&&item.getId().equals(id))return item;throw new IllegalStateException("missing migration item");}
    private static QuestDefinition definitionVersion(QuestDefinition d,int version){return new QuestDefinition(d.getId(),version,d.getName(),d.getDescription(),d.getPrerequisiteLogic(),d.getTaskLogic(),d.getPrerequisites(),d.getTasks(),d.getRewards(),d.getRepeatPolicy(),d.getBehavior());}
    private static QuestChapterDefinition chapterVersion(QuestChapterDefinition d,int version){return new QuestChapterDefinition(d.getId(),version,d.getName(),d.getDescription(),d.getIconReference(),d.getBackgroundReference(),d.getBackgroundSize(),d.getVisibility(),d.getEntries());}
    private static QuestContentMigrationBatch batch(BqMigrationManifest manifest,QuestContentMigrationBatch.Status status,String previous,List<QuestContentMigrationItem> items){int errors=0,warnings=0;for(BqMigrationIssue issue:manifest.getIssues())if(issue.getSeverity()==BqMigrationIssue.Severity.ERROR)errors++;else warnings++;return new QuestContentMigrationBatch(manifest.getBatchId(),manifest.getSourceHash(),status,manifest.toText(),previous,errors,warnings,items);}
    private static String join(List<UUID> ids){StringBuilder out=new StringBuilder();for(UUID id:ids){if(out.length()>0)out.append(',');out.append(id);}return out.toString();}
}

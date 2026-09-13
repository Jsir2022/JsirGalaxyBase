package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Applies a white-listed group alignment in one authorized optimistic chapter write. */
public final class QuestChapterAlignmentService {
    private final QuestChapterRepository chapters; private final QuestTransaction transaction; private final QuestEditorAuthorization authorization;
    public QuestChapterAlignmentService(QuestChapterRepository chapters,QuestTransaction transaction,QuestEditorAuthorization authorization){
        if(chapters==null||transaction==null||authorization==null)throw new IllegalArgumentException("dependencies are required");this.chapters=chapters;this.transaction=transaction;this.authorization=authorization;
    }
    public QuestChapterManagementResult align(QuestEditorActor actor,final QuestChapterAlignmentRequest request){
        if(actor==null||!authorization.canManageDefinitions(actor))return QuestChapterManagementResult.status(QuestChapterManagementResult.Status.FORBIDDEN);
        if(request==null)return QuestChapterManagementResult.invalid("alignment request is required");
        Optional<StoredQuestChapter> stored=chapters.find(request.getChapterId(),request.getVersion());
        if(!stored.isPresent())return QuestChapterManagementResult.status(QuestChapterManagementResult.Status.NOT_FOUND);
        if(stored.get().getLifecycle()!=QuestDefinitionLifecycle.DRAFT)return QuestChapterManagementResult.status(QuestChapterManagementResult.Status.CONFLICT);
        try{
            final QuestChapterDraftEditor editor=QuestChapterDraftEditor.edit(stored.get().getDefinition());
            List<QuestChapterEntry> selected=new ArrayList<QuestChapterEntry>();
            for(java.util.UUID id:request.getQuestIds()){
                QuestChapterEntry entry=find(stored.get().getDefinition(),id); if(entry==null)throw new IllegalArgumentException("selected quest is not placed: "+id); selected.add(entry);
            }
            int target=target(selected,request.getAlignment());
            for(QuestChapterEntry entry:selected){int x=entry.getX(),y=entry.getY(); switch(request.getAlignment()){
                case LEFT:x=target;break; case RIGHT:x=target-entry.getWidth();break; case CENTER_X:x=target-entry.getWidth()/2;break;
                case TOP:y=target;break; case BOTTOM:y=target-entry.getHeight();break; case CENTER_Y:y=target-entry.getHeight()/2;break; default:throw new IllegalArgumentException("unsupported alignment");}
                editor.move(entry.getQuestId(),x,y);
            }
            final QuestChapterDefinition changed=editor.build();
            return QuestChapterManagementResult.success(transaction.inTransaction(new QuestTransaction.Work<StoredQuestChapter>(){public StoredQuestChapter execute(){return chapters.updateDraft(changed,request.getExpectedHash());}}));
        }catch(QuestDefinitionConflictException conflict){return QuestChapterManagementResult.status(QuestChapterManagementResult.Status.CONFLICT);
        }catch(RuntimeException invalid){return QuestChapterManagementResult.invalid(invalid.getMessage());}
    }
    private static QuestChapterEntry find(QuestChapterDefinition value,java.util.UUID id){for(QuestChapterEntry entry:value.getEntries())if(entry.getQuestId().equals(id))return entry;return null;}
    private static int target(List<QuestChapterEntry> values,QuestChapterAlignment mode){
        long minimum=Long.MAX_VALUE,maximum=Long.MIN_VALUE;
        for(QuestChapterEntry value:values){long low,high;if(mode==QuestChapterAlignment.LEFT||mode==QuestChapterAlignment.RIGHT||mode==QuestChapterAlignment.CENTER_X){low=value.getX();high=low+value.getWidth();}else{low=value.getY();high=low+value.getHeight();}minimum=Math.min(minimum,low);maximum=Math.max(maximum,high);}
        long result=mode==QuestChapterAlignment.LEFT||mode==QuestChapterAlignment.TOP?minimum
            :mode==QuestChapterAlignment.RIGHT||mode==QuestChapterAlignment.BOTTOM?maximum:minimum+(maximum-minimum)/2L;
        if(result<Integer.MIN_VALUE||result>Integer.MAX_VALUE)throw new IllegalArgumentException("alignment coordinate exceeds bounds");return (int)result;
    }
}

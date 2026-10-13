package com.jsirgalaxybase.ui2.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.ui2.component.ComponentUiDocument;
import com.jsirgalaxybase.ui2.component.StandardWidgets;
import com.jsirgalaxybase.ui2.component.UiActionHandler;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiInputHandler;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;
import com.jsirgalaxybase.ui2.text.TextFlow;
import com.jsirgalaxybase.ui2.text.TextStyle;

/** Player-facing quest browse/detail document shared by Java2D and Minecraft hosts. */
public class QuestCenterVisualDocument extends ComponentUiDocument {
    private static final int MAX_QUERY_LENGTH=96;
    private static final int MAX_DESCRIPTION_LINES=512;
    private QuestCenterVisualModel model;
    private final TerminalActionPort shellActions;
    private final QuestCenterActionPort actions;
    private final Runnable invalidation;
    private TerminalWindowProfile profile;
    private boolean participantManagementOpen;
    private int selectedParticipant;
    private String createType="PARTY",createName="",targetPlayer="",targetRole="MEMBER";
    private int focusedField=-1,height;
    private String queryDraft,submittedQuery="",descriptionCacheText="";
    private boolean queryDirty,queryAwaitingSnapshot;
    private int detailTab,detailTaskIndex,detailTaskComponentPage,detailRewardIndex,detailRewardComponentPage,
        detailChoicePage,detailPrerequisitePage,detailDescriptionPage,
        descriptionCacheWidth=-1,descriptionCacheSize=-1,descriptionCacheLineHeight=-1;
    private List<String> descriptionLines=Collections.emptyList();
    private String detailStateQuestId="";
    private PendingParticipantAction pendingParticipantAction;

    public QuestCenterVisualDocument(QuestCenterVisualModel model, TerminalActionPort shellActions,
        QuestCenterActionPort actions, TerminalWindowProfile profile) {
        this(model,shellActions,actions,profile,new Runnable(){public void run(){}});
    }

    public QuestCenterVisualDocument(QuestCenterVisualModel model, TerminalActionPort shellActions,
        QuestCenterActionPort actions, TerminalWindowProfile profile,Runnable invalidation) {
        super(StandardWidgets.create());
        if (model == null) throw new IllegalArgumentException("model is required");
        this.model=model;this.shellActions=shellActions==null?TerminalActionPort.NONE:shellActions;
        this.actions=actions==null?QuestCenterActionPort.NONE:actions;
        this.profile=profile==null?TerminalWindowProfile.STANDARD:profile;
        this.invalidation=invalidation==null?new Runnable(){public void run(){}}:invalidation;
        this.queryDraft=model.getQuery();
        this.detailStateQuestId=model.getSelectedQuestId();this.detailTab=defaultDetailTab(model);
    }

    public final void updateVisualModel(QuestCenterVisualModel value) {
        if (value == null) throw new IllegalArgumentException("model is required");
        String nextQuest=value.getSelectedQuestId();
        if(!nextQuest.equals(detailStateQuestId)){detailStateQuestId=nextQuest;detailTab=defaultDetailTab(value);detailTaskIndex=0;
            detailTaskComponentPage=0;detailRewardIndex=0;detailRewardComponentPage=0;detailChoicePage=0;detailPrerequisitePage=0;detailDescriptionPage=0;}
        if(queryAwaitingSnapshot&&value.getQuery().equals(submittedQuery)){
            queryAwaitingSnapshot=false;
            if(!queryDirty)queryDraft=value.getQuery();
        }else if(!queryDirty&&!queryAwaitingSnapshot)queryDraft=value.getQuery();
        model=value;
    }

    @Override public UiElement build(UiContext context) {
        UiElement content=participantManagementOpen?participantManagement()
            :model.getView()==QuestCenterVisualModel.View.DETAIL&&model.getDetail()!=null?detail():browse();
        if(pendingParticipantAction!=null)content=UiElement.type("Stack").key("quest-participant-confirm-stack")
            .child(content).child(UiElement.type("Dialog").key("quest-participant-confirm")
                .prop(StandardWidgets.TEXT,pendingParticipantAction.title)
                .prop(StandardWidgets.DETAIL,pendingParticipantAction.detail)
                .prop(StandardWidgets.CANCEL_TEXT,"取消").prop(StandardWidgets.CONFIRM_TEXT,"确认").build()).build();
        return TerminalVisualShell.build(model.getShell(),content,shellActions,"任务中心","career");
    }

    @Override public void onViewportChanged(UiSize viewport,UiContext context){
        height=viewport.getHeight();
        if(model.getView()!=QuestCenterVisualModel.View.DETAIL||model.getDetail()==null)return;
        String text=model.getDetail().getDescription();
        int width=Math.max(1,TerminalVisualMetrics.compute(viewport,profile).getMainWidth()-8);
        TextStyle style=captionStyle(context);
        if(!text.equals(descriptionCacheText)||width!=descriptionCacheWidth||style.getSize()!=descriptionCacheSize
            ||style.getLineHeight()!=descriptionCacheLineHeight){
            if(text.isEmpty())descriptionLines=Collections.emptyList();
            else{
                TextFlow.Result flow=TextFlow.layout(text,style,context.getTextMeasurer(),width,MAX_DESCRIPTION_LINES);
                descriptionLines=new ArrayList<String>(flow.getLines());
            }
            descriptionCacheText=text;descriptionCacheWidth=width;descriptionCacheSize=style.getSize();
            descriptionCacheLineHeight=style.getLineHeight();detailDescriptionPage=0;
        }
    }

    private UiElement browse() {
        UiElement search=UiElement.type("Row").key("quest-search-row")
            .child(queryField())
            .child(button("quest-search-submit","搜索",new Runnable(){public void run(){submitQuery(queryDraft);}},true))
            .child(button("quest-search-clear","清空",new Runnable(){public void run(){queryDraft="";queryDirty=false;submitQuery("");}},!queryDraft.isEmpty())).build();
        UiElement tools=UiElement.type("Column").key("quest-tools").child(search)
            .child(UiElement.type("Row").key("quest-filter-row")
            .child(filter("quest-filter-all","全部","all"))
            .child(filter("quest-filter-active","进行中","active"))
            .child(filter("quest-filter-claim","待领取","claimable"))
            .child(button("quest-participant-open","主体管理",new Runnable(){public void run(){participantManagementOpen=true;selectedParticipant=0;invalidation.run();}},true))
            .child(button("quest-admin-open","任务管理",new Runnable(){public void run(){actions.openManagement();}},true)).build()).build();
        UiElement.Builder participants=UiElement.type("Row").key("quest-participants")
            .child(label("quest-participant-personal","个人进度","caption",1,true));
        for(int i=0;i<model.getParticipants().size();i++){
            QuestCenterVisualModel.Participant value=model.getParticipants().get(i);
            participants.child(badge("quest-participant-"+i,value.getType()+" · "+value.getName()+" · "+value.getMemberCount()+"人 / "+value.getRole(),"border"));
        }
        UiElement.Builder chapters=UiElement.type("Card").key("quest-chapters")
            .child(label("quest-chapters-title","任务章节","caption",1,true));
        for(int i=0;i<model.getChapters().size();i++){
            final QuestCenterVisualModel.Chapter chapter=model.getChapters().get(i);
            chapters.child(UiElement.type("Button").key(chapterKey(chapter))
                .prop(StandardWidgets.TEXT,chapter.getName()+"  "+chapter.getCompleted()+"/"+chapter.getTotal())
                .prop(StandardWidgets.SELECTED,chapter.getId().equals(model.getSelectedChapterId()))
                .prop(StandardWidgets.ACTION,handler(new Runnable(){public void run(){actions.selectChapter(chapter.getId());}})).build());
        }
        chapters.child(pager("quest-chapter-pager",model.getChapterPageIndex(),model.getChapterTotalPages(),
            model.hasPreviousChapterPage(),model.hasNextChapterPage(),true));
        UiElement.Builder quests=UiElement.type("Card").key("quest-list")
            .child(label("quest-list-title","当前章节","caption",1,true));
        if(model.getLoadState()==QuestCenterVisualModel.LoadState.LOADING) quests.child(state("quest-state","LoadingState","正在加载跨服任务进度…"));
        else if(model.getLoadState()==QuestCenterVisualModel.LoadState.ERROR) quests.child(state("quest-state","ErrorState",model.getMessage().isEmpty()?"任务读取失败":model.getMessage()));
        else if(model.getLoadState()==QuestCenterVisualModel.LoadState.EMPTY||model.getQuests().isEmpty()) quests.child(state("quest-state","EmptyState","当前筛选没有任务"));
        else for(int i=0;i<model.getQuests().size();i++) quests.child(questCard(model.getQuests().get(i)));
        quests.child(pager("quest-list-pager",model.getQuestPageIndex(),model.getQuestTotalPages(),
            model.hasPreviousQuestPage(),model.hasNextQuestPage(),false));
        return UiElement.type("Column").key("quest-browse").child(tools).child(participants.build())
            .child(UiElement.type("Row").key("quest-browser").child(chapters.build()).child(quests.build()).build()).build();
    }

    private UiElement pager(String key,final int page,int total,boolean previous,boolean next,final boolean chapter){
        return UiElement.type("Row").key(key)
            .child(button(key+"-previous","‹",new Runnable(){public void run(){if(chapter)actions.changeChapterPage(page-1);else actions.changeQuestPage(page-1);}},previous))
            .child(label(key+"-label",(page+1)+" / "+Math.max(1,total),"caption",1,false))
            .child(button(key+"-next","›",new Runnable(){public void run(){if(chapter)actions.changeChapterPage(page+1);else actions.changeQuestPage(page+1);}},next)).build();
    }

    private UiElement questCard(final QuestCenterVisualModel.QuestSummary quest){
        UiElement.Builder row=UiElement.type("Card").key(questKey(quest))
            .prop(StandardWidgets.ACTION,handler(new Runnable(){public void run(){actions.selectQuest(quest.getId());}}));
        if(quest.getIcon()!=null)row.child(UiElement.type("ExternalItem").key(questIconKey(quest))
            .prop(StandardWidgets.EXTERNAL_ID,"quest:icon:"+quest.getId()).build());
        row.child(label(questNameKey(quest),quest.getName(),"caption",1,true))
            .child(label(questSubtitleKey(quest),quest.getSubtitle(),"caption",1,false))
            .child(label("quest-track-"+quest.getId(),quest.isTracked()?"★":"","caption",1,true))
            .child(badge(questStatusKey(quest),status(quest),tone(quest.getState())));
        return row.build();
    }

    private UiElement detail(){
        final QuestCenterVisualModel.QuestDetail detail=model.getDetail();
        final QuestCenterVisualModel.QuestSummary summary=detail.getSummary();
        UiElement.Builder heading=UiElement.type("Card").key("quest-detail-heading");
        UiElement.Builder headingRow=UiElement.type("Row").key("quest-detail-heading-row")
            .child(button("quest-detail-back","‹ 列表",new Runnable(){public void run(){actions.backToBrowse();}},true));
        if(summary.getIcon()!=null)headingRow.child(UiElement.type("ExternalItem").key("quest-detail-icon")
            .prop(StandardWidgets.EXTERNAL_ID,"quest:detail:icon").build());
        heading.child(headingRow.child(label("quest-detail-title",summary.getName(),"section",1,true))
            .child(badge("quest-detail-status",status(summary),tone(summary.getState())))
            .child(button("quest-detail-track",summary.isTracked()?"★":"☆",new Runnable(){public void run(){actions.toggleTracking(summary.getId());}},true)).build())
            .child(label("quest-detail-meta","版本 "+detail.getDefinitionVersion()+" · "+detail.getPrerequisiteText()+" · "+detail.getRepeatText()
                +(detail.getCycle()>0?" · 周期 "+detail.getCycle():""),"caption",1,false));
        UiElement tabs=UiElement.type("Row").key("quest-detail-tabs")
            .child(detailTabButton(0,"任务目标")).child(detailTabButton(1,"奖励")).child(detailTabButton(2,"前置"))
            .child(detailTabButton(3,"任务说明")).build();
        UiElement body=detailTab==1?rewardDetail(detail,summary):detailTab==2?prerequisiteDetail(detail)
            :detailTab==3?descriptionDetail():taskDetail(detail,summary);
        return UiElement.type("Column").key("quest-detail").child(heading.build()).child(tabs).child(body).build();
    }

    private UiElement descriptionDetail(){
        UiElement.Builder pane=UiElement.type("Card").key("quest-description-page")
            .child(label("quest-description-title","任务说明 · "+descriptionLines.size()+" 行","caption",1,true));
        if(descriptionLines.isEmpty())return pane.child(state("quest-description-empty","EmptyState","此任务没有额外说明")).build();
        int pageSize=descriptionPageSize();int pages=pageCount(descriptionLines.size(),pageSize);
        int page=Math.max(0,Math.min(detailDescriptionPage,pages-1));detailDescriptionPage=page;
        int from=page*pageSize,to=Math.min(descriptionLines.size(),from+pageSize);
        pane.child(label("quest-description-text",joinLines(descriptionLines,from,to),"caption",pageSize,false));
        if(pages>1)pane.child(pageControls("quest-description-pager",page,pages,
            new Runnable(){public void run(){detailDescriptionPage=Math.max(0,detailDescriptionPage-1);invalidation.run();}},
            new Runnable(){public void run(){detailDescriptionPage=Math.min(pages-1,detailDescriptionPage+1);invalidation.run();}}));
        return pane.build();
    }

    private UiElement queryField(){
        return UiElement.type("TextField").key("quest-search-input").prop(StandardWidgets.TEXT,queryDraft)
            .prop(StandardWidgets.PLACEHOLDER,"搜索任务名称或说明（最多 96 字）")
            .prop(StandardWidgets.ACTION,new UiActionHandler(){public InputResult handle(UiEvent event){
                if(event.getType()==UiEvent.Type.POINTER_DOWN)return InputResult.CONSUMED;
                if(event.getType()==UiEvent.Type.TEXT_INPUT){
                    char value=event.getTypedChar();
                    if(!Character.isISOControl(value)&&queryDraft.length()<MAX_QUERY_LENGTH){queryDraft+=value;queryDirty=true;queryAwaitingSnapshot=false;invalidation.run();}
                    return InputResult.CONSUMED;
                }
                if(event.getType()==UiEvent.Type.KEY_DOWN
                    &&(event.getKeyCode()==UiKeyCode.BACKSPACE||event.getKeyCode()==UiKeyCode.DELETE)){
                    if(!queryDraft.isEmpty()){int end=queryDraft.offsetByCodePoints(queryDraft.length(),-1);queryDraft=queryDraft.substring(0,end);queryDirty=true;queryAwaitingSnapshot=false;invalidation.run();}
                    return InputResult.CONSUMED;
                }
                if(event.getType()==UiEvent.Type.KEY_DOWN&&event.getKeyCode()==UiKeyCode.ENTER){
                    submitQuery(queryDraft);
                    return InputResult.CONSUMED;
                }
                return InputResult.PASS;
            }}).build();
    }

    private void submitQuery(String value){
        String query=value==null?"":value.trim();
        if(query.length()>MAX_QUERY_LENGTH)query=query.substring(0,MAX_QUERY_LENGTH);
        queryDraft=query;queryDirty=false;submittedQuery=query;queryAwaitingSnapshot=true;
        actions.changeQuery(query);invalidation.run();
    }

    private UiElement detailTabButton(final int tab,String text){
        return UiElement.type("Button").key("quest-detail-tab-"+tab).prop(StandardWidgets.TEXT,text)
            .prop(StandardWidgets.SELECTED,detailTab==tab).prop(StandardWidgets.ACTION,handler(new Runnable(){public void run(){detailTab=tab;invalidation.run();}})).build();
    }

    private UiElement taskDetail(final QuestCenterVisualModel.QuestDetail detail,final QuestCenterVisualModel.QuestSummary summary){
        List<QuestCenterVisualModel.TaskRow> rows=detail.getTasks();
        UiElement.Builder pane=UiElement.type("Card").key("quest-detail-tasks")
            .child(label("quest-task-title","任务目标 · "+rows.size(),"caption",1,true));
        if(rows.isEmpty())return pane.child(state("quest-task-empty","EmptyState","该任务没有配置目标")).build();
        final int index=Math.max(0,Math.min(detailTaskIndex,rows.size()-1));detailTaskIndex=index;
        final QuestCenterVisualModel.TaskRow task=rows.get(index);
        pane.child(UiElement.type("Row").key("quest-task-pager")
            .child(button("quest-task-previous","‹",new Runnable(){public void run(){detailTaskIndex=Math.max(0,index-1);detailTaskComponentPage=0;invalidation.run();}},index>0))
            .child(label("quest-task-page",(index+1)+" / "+rows.size(),"caption",1,false))
            .child(button("quest-task-next","›",new Runnable(){public void run(){detailTaskIndex=Math.min(rows.size()-1,index+1);detailTaskComponentPage=0;invalidation.run();}},index+1<rows.size())).build())
            .child(label("quest-task-name",(task.isComplete()?"✓ ":"○ ")+task.getLabel()+" · "+task.getTypeId(),"caption",1,true))
            .child(label("quest-task-progress","进度 "+task.getValue()+" / "+task.getTarget(),"caption",1,false));
        if(task.getComponents().isEmpty())pane.child(label("quest-task-detail",task.getDetail(),"caption",2,false));
        addComponentPage(pane,task.getComponents(),detailTaskComponentPage,"quest-task-components",
            "quest:task:"+task.getKey()+":",true);
        UiElement.Builder operations=UiElement.type("Row").key("quest-task-actions");
        if(task.isConfirmable()&&!task.isComplete())operations.child(button("quest-task-confirm-"+index,"确认目标",
            new Runnable(){public void run(){actions.confirmTask(summary.getId(),task.getKey());}},true));
        boolean hasConsumable=false;for(QuestCenterVisualModel.TaskRow row:rows)hasConsumable|=row.isSubmittable()&&!row.isComplete();
        if(hasConsumable)operations.child(button("quest-detect-consumptions","检测消耗目标",
            new Runnable(){public void run(){actions.detectConsumptions(summary.getId());}},true));
        UiElement operationElement=operations.build();
        if(!operationElement.getChildren().isEmpty())pane.child(operationElement);
        return pane.build();
    }

    private UiElement rewardDetail(final QuestCenterVisualModel.QuestDetail detail,final QuestCenterVisualModel.QuestSummary summary){
        List<QuestCenterVisualModel.RewardRow> rows=detail.getRewards();
        UiElement.Builder pane=UiElement.type("Card").key("quest-detail-rewards")
            .child(label("quest-reward-title","任务奖励 · "+rows.size(),"caption",1,true));
        if(rows.isEmpty())return pane.child(state("quest-reward-empty","EmptyState","该任务没有配置奖励")).build();
        final int index=Math.max(0,Math.min(detailRewardIndex,rows.size()-1));detailRewardIndex=index;
        final QuestCenterVisualModel.RewardRow reward=rows.get(index);
        pane.child(UiElement.type("Row").key("quest-reward-pager")
            .child(button("quest-reward-previous","‹",new Runnable(){public void run(){detailRewardIndex=Math.max(0,index-1);detailRewardComponentPage=0;detailChoicePage=0;invalidation.run();}},index>0))
            .child(label("quest-reward-page",(index+1)+" / "+rows.size(),"caption",1,false))
            .child(button("quest-reward-next","›",new Runnable(){public void run(){detailRewardIndex=Math.min(rows.size()-1,index+1);detailRewardComponentPage=0;detailChoicePage=0;invalidation.run();}},index+1<rows.size())).build());
        UiElement.Builder summaryRow=UiElement.type("Row").key("quest-reward-summary");
        if(reward.getItem()!=null)summaryRow.child(UiElement.type("ExternalItem").key("quest-reward-item-"+index)
            .prop(StandardWidgets.EXTERNAL_ID,"quest:reward:"+index)
            .prop(StandardWidgets.QUANTITY,AssetCenterVisualDocument.compact(reward.getItem().getQuantity())).build());
        summaryRow.child(label("quest-reward-name",reward.getLabel(),"caption",1,true))
            .child(label("quest-reward-detail",reward.getDetail(),"caption",1,false));pane.child(summaryRow.build());
        if(reward.getChoices().isEmpty()&&!reward.getComponents().isEmpty())addComponentPage(pane,reward.getComponents(),detailRewardComponentPage,
            "quest-reward-components","quest:reward-component:"+index+":",false);
        if(!reward.getChoices().isEmpty()){
            pane.child(label("quest-choice-title","可选奖励 · "+reward.getChoices().size(),"caption",1,true));
            int pageSize=choicePageSize();int pages=pageCount(reward.getChoices().size(),pageSize);
            int page=Math.max(0,Math.min(detailChoicePage,pages-1));detailChoicePage=page;
            int from=page*pageSize,to=Math.min(reward.getChoices().size(),from+pageSize);
            for(int optionIndex=from;optionIndex<to;optionIndex++){
                final QuestCenterVisualModel.ChoiceOption option=reward.getChoices().get(optionIndex);
                final int sourceIndex=option.getSourceIndex();
                UiElement.Builder choice=UiElement.type("Row").key("quest-choice-row-"+index+"-"+sourceIndex);
                if(option.getItem()!=null)choice.child(UiElement.type("ExternalItem").key("quest-choice-item-"+index+"-"+sourceIndex)
                    .prop(StandardWidgets.EXTERNAL_ID,"quest:choice:"+index+":"+sourceIndex)
                    .prop(StandardWidgets.QUANTITY,AssetCenterVisualDocument.compact(option.getItem().getQuantity())).build());
                choice.child(UiElement.type("Button").key("quest-choice-"+index+"-"+sourceIndex)
                    .prop(StandardWidgets.TEXT,sourceIndex+". "+option.getLabel()+" ×"+AssetCenterVisualDocument.compact(option.getQuantity()))
                    .prop(StandardWidgets.SELECTED,reward.getSelectedChoiceIndex()==sourceIndex)
                    .prop(StandardWidgets.ENABLED,reward.isClaimable())
                    .prop(StandardWidgets.ACTION,handler(new Runnable(){public void run(){actions.selectRewardChoice(summary.getId(),reward.getKey(),sourceIndex);}})).build());
                pane.child(choice.build());
            }
            if(pages>1)pane.child(pageControls("quest-choice-pager",page,pages,new Runnable(){public void run(){detailChoicePage=Math.max(0,detailChoicePage-1);invalidation.run();}},new Runnable(){public void run(){detailChoicePage=Math.min(pages-1,detailChoicePage+1);invalidation.run();}}));
        }
        boolean canClaim=summary.getState()==QuestCenterVisualModel.QuestState.CLAIMABLE;
        boolean hasUnselectedChoice=hasUnselectedChoice(rows);
        String claimLabel=!canClaim?summary.getState()==QuestCenterVisualModel.QuestState.COMPLETED?"奖励已领取":"完成后可领取"
            :hasUnselectedChoice?"请先选择奖励":"领取全部奖励";
        pane.child(button("quest-claim",claimLabel,new Runnable(){public void run(){actions.claim(summary.getId());}},canClaim&&!hasUnselectedChoice));
        if(canClaim&&hasUnselectedChoice)pane.child(button("quest-claim-random-choice","随机选择并领取",
            new Runnable(){public void run(){actions.forceClaimWithRandomChoices(summary.getId());}},true));
        return pane.build();
    }

    private UiElement prerequisiteDetail(final QuestCenterVisualModel.QuestDetail detail){
        List<QuestCenterVisualModel.PrerequisiteRow> rows=detail.getPrerequisites();
        UiElement.Builder pane=UiElement.type("Card").key("quest-detail-prerequisites")
            .child(label("quest-prerequisite-title","前置任务 · "+rows.size(),"caption",1,true));
        if(rows.isEmpty())return pane.child(state("quest-prerequisite-empty","EmptyState",detail.getPrerequisiteText())).build();
        int pageSize=prerequisitePageSize();int pages=pageCount(rows.size(),pageSize);
        int page=Math.max(0,Math.min(detailPrerequisitePage,pages-1));detailPrerequisitePage=page;
        int from=page*pageSize,to=Math.min(rows.size(),from+pageSize);
        for(int i=from;i<to;i++){
            final QuestCenterVisualModel.PrerequisiteRow row=rows.get(i);
            pane.child(button("quest-prerequisite-"+i,row.getName()+" · "+row.getState(),
                new Runnable(){public void run(){if(row.isNavigable())actions.selectPrerequisite(row.getId());}},row.isNavigable()));
        }
        if(pages>1)pane.child(pageControls("quest-prerequisite-pager",page,pages,
            new Runnable(){public void run(){detailPrerequisitePage=Math.max(0,detailPrerequisitePage-1);invalidation.run();}},
            new Runnable(){public void run(){detailPrerequisitePage=Math.min(pages-1,detailPrerequisitePage+1);invalidation.run();}}));
        return pane.build();
    }

    private void addComponentPage(UiElement.Builder parent,List<QuestCenterVisualModel.ComponentRow> components,int requestedPage,
        String key,String externalPrefix,boolean task){
        if(components==null||components.isEmpty())return;
        int pageSize=componentPageSize();int pages=pageCount(components.size(),pageSize);
        int page=Math.max(0,Math.min(requestedPage,pages-1));
        if(task)detailTaskComponentPage=page;else detailRewardComponentPage=page;
        int from=page*pageSize,to=Math.min(components.size(),from+pageSize);
        for(int index=from;index<to;index++){
            QuestCenterVisualModel.ComponentRow component=components.get(index);
            UiElement.Builder row=UiElement.type("Row").key(key+"-"+component.getSourceIndex());
            if(component.getItem()!=null)row.child(UiElement.type("ExternalItem").key(key+"-item-"+component.getSourceIndex())
                .prop(StandardWidgets.EXTERNAL_ID,externalPrefix+component.getSourceIndex())
                .prop(StandardWidgets.QUANTITY,AssetCenterVisualDocument.compact(component.getQuantity())).build());
            String name=component.getDisplayName().isEmpty()?component.getIdentity():component.getDisplayName();
            if(name.isEmpty())name=component.getKey();
            String detail=name+" ×"+component.getQuantity();
            if(task&&component.getTarget()>0)detail+=" · "+component.getProgress()+"/"+component.getTarget();
            if(!component.getOreDictionary().isEmpty())detail+=" · ore:"+component.getOreDictionary();
            if(component.getItem()==null&&"item".equals(component.getKind()))detail+=" · 物品未注册";
            row.child(label(key+"-label-"+component.getSourceIndex(),"#"+component.getSourceIndex()+" "+detail,"caption",1,false));
            parent.child(row.build());
        }
        if(pages>1){
            final int last=pages-1;
            parent.child(pageControls(key+"-pager",page,pages,
                new Runnable(){public void run(){if(task)detailTaskComponentPage=Math.max(0,detailTaskComponentPage-1);else detailRewardComponentPage=Math.max(0,detailRewardComponentPage-1);invalidation.run();}},
                new Runnable(){public void run(){if(task)detailTaskComponentPage=Math.min(last,detailTaskComponentPage+1);else detailRewardComponentPage=Math.min(last,detailRewardComponentPage+1);invalidation.run();}}));
        }
    }

    private UiElement pageControls(String key,int page,int pages,Runnable previous,Runnable next){
        return UiElement.type("Row").key(key).child(button(key+"-previous","‹",previous,page>0))
            .child(label(key+"-label",(page+1)+" / "+pages,"caption",1,false))
            .child(button(key+"-next","›",next,page+1<pages)).build();
    }
    private int componentPageSize(){return detailPageSize();}
    private int choicePageSize(){return detailPageSize();}
    private int detailPageSize(){return height<=220?1:height<=280?2:4;}
    private int prerequisitePageSize(){return 3;}
    private int descriptionPageSize(){return height<=220?7:12;}
    private static int pageCount(int count,int size){return Math.max(1,(count+size-1)/size);}
    private static TextStyle captionStyle(UiContext context){
        int size=context.getTheme().typography("caption");
        return new TextStyle("caption",size,false,Math.max(size,context.getTheme().typography("lineBody")-2),TextStyle.Align.LEFT);
    }
    private static String joinLines(List<String> lines,int from,int to){
        StringBuilder result=new StringBuilder();
        for(int i=from;i<to;i++){if(i>from)result.append('\n');result.append(lines.get(i));}
        return result.toString();
    }
    private static int defaultDetailTab(QuestCenterVisualModel model){
        QuestCenterVisualModel.QuestDetail detail=model==null?null:model.getDetail();
        return detail!=null&&detail.getTasks().isEmpty()&&!detail.getRewards().isEmpty()?1:0;
    }

    private UiElement participantManagement(){
        UiElement header=UiElement.type("Row").key("quest-participant-head")
            .child(button("quest-participant-back","‹ 返回任务",new Runnable(){public void run(){participantManagementOpen=false;pendingParticipantAction=null;invalidation.run();}},true))
            .child(label("quest-participant-title","跨服任务主体","section",1,true)).build();
        UiElement create=UiElement.type("Card").key("quest-participant-create")
            .child(button("quest-participant-type",createType,new Runnable(){public void run(){createType="PARTY".equals(createType)?"TEAM":"TEAM".equals(createType)?"PUBLIC":"PARTY";invalidation.run();}},true))
            .child(textField("quest-participant-name",createName,"主体名称",0,128))
            .child(button("quest-participant-create-submit","创建",new Runnable(){public void run(){if(!createName.trim().isEmpty())actions.createParticipant(createType,createName);}},!createName.trim().isEmpty())).build();
        UiElement.Builder selectors=UiElement.type("Row").key("quest-participant-selectors");
        for(int i=0;i<model.getParticipants().size();i++){
            final int index=i;QuestCenterVisualModel.Participant participant=model.getParticipants().get(i);
            selectors.child(UiElement.type("Button").key("quest-participant-select-"+i)
                .prop(StandardWidgets.TEXT,participant.getType()+" · "+participant.getName())
                .prop(StandardWidgets.SELECTED,i==selectedParticipant)
                .prop(StandardWidgets.ACTION,handler(new Runnable(){public void run(){selectedParticipant=index;invalidation.run();}})).build());
        }
        UiElement body;
        QuestCenterVisualModel.Participant selected=selectedParticipant();
        if(selected==null)body=UiElement.type("EmptyState").key("quest-participant-empty")
            .prop(StandardWidgets.TEXT,"你尚未加入任何 PARTY / TEAM / PUBLIC 主体，可在上方创建。").build();
        else{
            UiElement.Builder members=UiElement.type("Card").key("quest-participant-members")
                .child(label("quest-participant-summary",selected.getType()+" · "+selected.getName()+" · "+selected.getMemberCount()+" 人 · 你的角色 "+selected.getRole(),"caption",1,true));
            int shown=Math.min(8,selected.getMembers().size());
            for(int i=0;i<shown;i++){
                final QuestCenterVisualModel.Member member=selected.getMembers().get(i);final QuestCenterVisualModel.Participant group=selected;
                String shortId=member.getPlayerId().length()>12?member.getPlayerId().substring(0,8)+"…":member.getPlayerId();
                UiElement.Builder row=UiElement.type("Row").key("quest-participant-member-"+i)
                    .child(label("quest-participant-member-label-"+i,member.getRole()+" · "+shortId,"caption",1,false));
                boolean canAdmin="OWNER".equals(group.getRole())||"ADMIN".equals(group.getRole());
                boolean targetOwner="OWNER".equals(member.getRole());
                if("OWNER".equals(group.getRole())&&!targetOwner)row.child(button("quest-participant-transfer-"+i,"转让",new Runnable(){public void run(){pendingParticipantAction=PendingParticipantAction.transfer(group,member.getPlayerId());invalidation.run();}},true));
                if(canAdmin&&!targetOwner)row.child(button("quest-participant-remove-"+i,"移除",new Runnable(){public void run(){pendingParticipantAction=PendingParticipantAction.remove(group,member.getPlayerId());invalidation.run();}},true));
                members.child(row.build());
            }
            if(selected.getMembers().size()>shown)members.child(label("quest-participant-member-more","另有 "+(selected.getMembers().size()-shown)+" 名成员，请在宽窗查看完整目录。","caption",1,false));
            UiElement manage=UiElement.type("Card").key("quest-participant-manage")
                .child(textField("quest-participant-target",targetPlayer,"玩家名或 UUID",1,64))
                .child(button("quest-participant-role",targetRole,new Runnable(){public void run(){targetRole="MEMBER".equals(targetRole)?"ADMIN":"MEMBER";invalidation.run();}},true))
                .child(button("quest-participant-add","添加成员",new Runnable(){public void run(){QuestCenterVisualModel.Participant group=selectedParticipant();if(group!=null&&!targetPlayer.trim().isEmpty())actions.addParticipantMember(group.getType(),group.getId(),group.getRevision(),targetPlayer,targetRole);}},("OWNER".equals(selected.getRole())||"ADMIN".equals(selected.getRole()))&&!targetPlayer.trim().isEmpty()))
                .child(button("quest-participant-leave","离开主体",new Runnable(){public void run(){QuestCenterVisualModel.Participant group=selectedParticipant();if(group!=null){pendingParticipantAction=PendingParticipantAction.leave(group);invalidation.run();}}},true)).build();
            body=UiElement.type("Row").key("quest-participant-body").child(members.build()).child(manage).build();
        }
        return UiElement.type("Column").key("quest-participant-management").child(header).child(create)
            .child(selectors.build()).child(body).build();
    }

    private QuestCenterVisualModel.Participant selectedParticipant(){
        if(model.getParticipants().isEmpty())return null;
        selectedParticipant=Math.max(0,Math.min(selectedParticipant,model.getParticipants().size()-1));
        return model.getParticipants().get(selectedParticipant);
    }

    private UiElement textField(String key,String value,String placeholder,final int field,final int maximum){
        return UiElement.type("TextField").key(key).prop(StandardWidgets.TEXT,value)
            .prop(StandardWidgets.PLACEHOLDER,placeholder).prop(StandardWidgets.FOCUSED,focusedField==field)
            .prop(StandardWidgets.ACTION,new UiActionHandler(){public InputResult handle(UiEvent event){String current=field==0?createName:targetPlayer;if(event.getType()==UiEvent.Type.POINTER_DOWN)focusedField=field;else if(event.getType()==UiEvent.Type.TEXT_INPUT&&!Character.isISOControl(event.getTypedChar())&&current.length()<maximum)current+=event.getTypedChar();else if(event.getType()==UiEvent.Type.KEY_DOWN&&(event.getKeyCode()==UiKeyCode.BACKSPACE||event.getKeyCode()==UiKeyCode.DELETE)&&!current.isEmpty())current=current.substring(0,current.length()-1);if(field==0)createName=current;else targetPlayer=current;invalidation.run();return InputResult.CONSUMED;}}).build();
    }

    @Override public LayoutSpec layout(UiElement root,UiSize viewport,UiContext context){
        height=viewport.getHeight();
        TerminalVisualMetrics metrics=TerminalVisualMetrics.compute(viewport,profile);boolean compact=metrics.getBounds().getHeight()<=220;
        LayoutSpec body=participantManagementOpen?participantManagementLayout(compact)
            :model.getView()==QuestCenterVisualModel.View.DETAIL&&model.getDetail()!=null?detailLayout(compact):browseLayout(compact);
        if(pendingParticipantAction!=null)body=LayoutSpec.of("quest-participant-confirm-stack",LayoutKind.STACK).flex(1)
            .child(body).child(LayoutSpec.of("quest-participant-confirm",LayoutKind.LEAF).build()).build();
        return TerminalVisualShell.layout(viewport,model.getShell(),body,profile);
    }

    private LayoutSpec browseLayout(boolean compact){
        LayoutSpec search=LayoutSpec.of("quest-search-row",LayoutKind.ROW).preferred(0,compact?16:20).gap(2)
            .child(LayoutSpec.of("quest-search-input",LayoutKind.LEAF).preferred(0,compact?16:20).flex(1).build())
            .child(LayoutSpec.of("quest-search-submit",LayoutKind.LEAF).preferred(compact?30:38,compact?16:20).build())
            .child(LayoutSpec.of("quest-search-clear",LayoutKind.LEAF).preferred(compact?24:32,compact?16:20).build()).build();
        LayoutSpec tools=LayoutSpec.of("quest-tools",LayoutKind.COLUMN).preferred(0,compact?31:38).gap(1).child(search)
            .child(LayoutSpec.of("quest-filter-row",LayoutKind.ROW).preferred(0,compact?14:17).gap(2)
            .child(LayoutSpec.of("quest-filter-all",LayoutKind.LEAF).preferred(36,0).build())
            .child(LayoutSpec.of("quest-filter-active",LayoutKind.LEAF).preferred(46,0).build())
            .child(LayoutSpec.of("quest-filter-claim",LayoutKind.LEAF).preferred(46,0).build())
            .child(LayoutSpec.of("quest-participant-open",LayoutKind.LEAF).preferred(compact?48:58,0).build())
            .child(LayoutSpec.of("quest-admin-open",LayoutKind.LEAF).preferred(compact?48:58,0).build()).build()).build();
        LayoutSpec.Builder participants=LayoutSpec.of("quest-participants",LayoutKind.ROW).preferred(0,compact?14:17).gap(2)
            .child(LayoutSpec.of("quest-participant-personal",LayoutKind.LEAF).preferred(compact?42:52,0).build());
        for(int i=0;i<model.getParticipants().size();i++)participants.child(LayoutSpec.of("quest-participant-"+i,LayoutKind.LEAF).flex(1).build());
        LayoutSpec.Builder chapters=LayoutSpec.of("quest-chapters",LayoutKind.COLUMN).preferred(compact?86:104,0).padding(new Insets(3,3,3,3)).gap(2)
            .child(LayoutSpec.of("quest-chapters-title",LayoutKind.LEAF).preferred(0,12).build());
        int chapterCount=model.getChapters().size();
        for(int i=0;i<chapterCount;i++)chapters.child(LayoutSpec.of(chapterKey(model.getChapters().get(i)),LayoutKind.LEAF).preferred(0,compact?15:17).build());
        chapters.child(pagerLayout("quest-chapter-pager",compact));
        LayoutSpec.Builder quests=LayoutSpec.of("quest-list",LayoutKind.COLUMN).flex(1).padding(new Insets(3,3,3,3)).gap(2)
            .child(LayoutSpec.of("quest-list-title",LayoutKind.LEAF).preferred(0,12).build());
        if(model.getLoadState()!=QuestCenterVisualModel.LoadState.READY||model.getQuests().isEmpty())quests.child(LayoutSpec.of("quest-state",LayoutKind.LEAF).flex(1).build());
        else for(int i=0;i<model.getQuests().size();i++){
            QuestCenterVisualModel.QuestSummary q=model.getQuests().get(i);LayoutSpec.Builder row=LayoutSpec.of(questKey(q),LayoutKind.ROW).preferred(0,compact?18:22).padding(new Insets(2,3,2,3)).gap(3);
            if(q.getIcon()!=null)row.child(LayoutSpec.of(questIconKey(q),LayoutKind.LEAF).preferred(compact?14:18,compact?14:18).build());
            row.child(LayoutSpec.of(questNameKey(q),LayoutKind.LEAF).flex(2).build()).child(LayoutSpec.of(questSubtitleKey(q),LayoutKind.LEAF).flex(2).build()).child(LayoutSpec.of("quest-track-"+q.getId(),LayoutKind.LEAF).preferred(10,0).build()).child(LayoutSpec.of(questStatusKey(q),LayoutKind.LEAF).preferred(52,0).build());quests.child(row.build());
        }
        quests.child(pagerLayout("quest-list-pager",compact));
        LayoutSpec browser=LayoutSpec.of("quest-browser",LayoutKind.ROW).flex(1).gap(3).child(chapters.build()).child(quests.build()).build();
        return LayoutSpec.of("quest-browse",LayoutKind.COLUMN).flex(1).padding(new Insets(3,3,3,3)).gap(3).child(tools).child(participants.build()).child(browser).build();
    }

    private LayoutSpec participantManagementLayout(boolean compact){
        LayoutSpec head=LayoutSpec.of("quest-participant-head",LayoutKind.ROW).preferred(0,compact?17:21).gap(3)
            .child(LayoutSpec.of("quest-participant-back",LayoutKind.LEAF).preferred(compact?54:68,0).build())
            .child(LayoutSpec.of("quest-participant-title",LayoutKind.LEAF).flex(1).build()).build();
        LayoutSpec create=LayoutSpec.of("quest-participant-create",LayoutKind.ROW).preferred(0,compact?22:26)
            .padding(new Insets(2,3,2,3)).gap(3)
            .child(LayoutSpec.of("quest-participant-type",LayoutKind.LEAF).preferred(45,0).build())
            .child(LayoutSpec.of("quest-participant-name",LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-participant-create-submit",LayoutKind.LEAF).preferred(44,0).build()).build();
        LayoutSpec.Builder selectors=LayoutSpec.of("quest-participant-selectors",LayoutKind.ROW).preferred(0,compact?17:20).gap(2);
        for(int i=0;i<model.getParticipants().size();i++)selectors.child(LayoutSpec.of("quest-participant-select-"+i,LayoutKind.LEAF).flex(1).build());
        LayoutSpec body;
        QuestCenterVisualModel.Participant selected=selectedParticipant();
        if(selected==null)body=LayoutSpec.of("quest-participant-empty",LayoutKind.LEAF).flex(1).build();
        else{
            LayoutSpec.Builder members=LayoutSpec.of("quest-participant-members",LayoutKind.COLUMN).flex(2).padding(new Insets(3,4,3,4)).gap(2)
                .child(LayoutSpec.of("quest-participant-summary",LayoutKind.LEAF).preferred(0,12).build());
            int shown=Math.min(8,selected.getMembers().size());
            for(int i=0;i<shown;i++){QuestCenterVisualModel.Member member=selected.getMembers().get(i);LayoutSpec.Builder row=LayoutSpec.of("quest-participant-member-"+i,LayoutKind.ROW).preferred(0,compact?15:18).gap(2)
                .child(LayoutSpec.of("quest-participant-member-label-"+i,LayoutKind.LEAF).flex(1).build());
                boolean canAdmin="OWNER".equals(selected.getRole())||"ADMIN".equals(selected.getRole()),targetOwner="OWNER".equals(member.getRole());
                if("OWNER".equals(selected.getRole())&&!targetOwner)row.child(LayoutSpec.of("quest-participant-transfer-"+i,LayoutKind.LEAF).preferred(36,0).build());
                if(canAdmin&&!targetOwner)row.child(LayoutSpec.of("quest-participant-remove-"+i,LayoutKind.LEAF).preferred(36,0).build());members.child(row.build());}
            if(selected.getMembers().size()>shown)members.child(LayoutSpec.of("quest-participant-member-more",LayoutKind.LEAF).preferred(0,12).build());
            LayoutSpec manage=LayoutSpec.of("quest-participant-manage",LayoutKind.COLUMN).flex(1).padding(new Insets(4,4,4,4)).gap(3)
                .child(LayoutSpec.of("quest-participant-target",LayoutKind.LEAF).preferred(0,compact?16:19).build())
                .child(LayoutSpec.of("quest-participant-role",LayoutKind.LEAF).preferred(0,compact?16:19).build())
                .child(LayoutSpec.of("quest-participant-add",LayoutKind.LEAF).preferred(0,compact?17:21).build())
                .child(LayoutSpec.of("quest-participant-leave",LayoutKind.LEAF).preferred(0,compact?17:21).build()).build();
            body=LayoutSpec.of("quest-participant-body",LayoutKind.ROW).flex(1).gap(3).child(members.build()).child(manage).build();
        }
        return LayoutSpec.of("quest-participant-management",LayoutKind.COLUMN).flex(1).padding(new Insets(3,3,3,3)).gap(3)
            .child(head).child(create).child(selectors.build()).child(body).build();
    }

    private static LayoutSpec pagerLayout(String key,boolean compact){return LayoutSpec.of(key,LayoutKind.ROW).preferred(0,compact?15:17).gap(2)
        .child(LayoutSpec.of(key+"-previous",LayoutKind.LEAF).preferred(18,0).build())
        .child(LayoutSpec.of(key+"-label",LayoutKind.LEAF).flex(1).build())
        .child(LayoutSpec.of(key+"-next",LayoutKind.LEAF).preferred(18,0).build()).build();}

    private LayoutSpec detailLayout(boolean compact){
        QuestCenterVisualModel.QuestDetail detail=model.getDetail();QuestCenterVisualModel.QuestSummary summary=detail.getSummary();
        int small=compact?12:15,normal=compact?14:18;
        LayoutSpec headingRow=LayoutSpec.of("quest-detail-heading-row",LayoutKind.ROW).preferred(0,normal).gap(2)
            .child(LayoutSpec.of("quest-detail-back",LayoutKind.LEAF).preferred(compact?28:40,0).build())
            .child(LayoutSpec.of("quest-detail-icon",LayoutKind.LEAF).preferred(compact?14:18,0).build())
            .child(LayoutSpec.of("quest-detail-title",LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-detail-status",LayoutKind.LEAF).preferred(compact?36:48,0).build())
            .child(LayoutSpec.of("quest-detail-track",LayoutKind.LEAF).preferred(compact?14:18,0).build()).build();
        LayoutSpec heading=LayoutSpec.of("quest-detail-heading",LayoutKind.COLUMN).preferred(0,compact?32:38)
            .padding(new Insets(2,2,2,2)).gap(1).child(headingRow)
            .child(LayoutSpec.of("quest-detail-meta",LayoutKind.LEAF).preferred(0,small).build()).build();
        LayoutSpec tabs=LayoutSpec.of("quest-detail-tabs",LayoutKind.ROW).preferred(0,normal).gap(2)
            .child(LayoutSpec.of("quest-detail-tab-0",LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-detail-tab-1",LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-detail-tab-2",LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-detail-tab-3",LayoutKind.LEAF).flex(1).build()).build();
        LayoutSpec body;
        if(detailTab==2){
            LayoutSpec.Builder prerequisites=LayoutSpec.of("quest-detail-prerequisites",LayoutKind.COLUMN).flex(1)
                .padding(new Insets(2,2,2,2)).gap(1)
                .child(LayoutSpec.of("quest-prerequisite-title",LayoutKind.LEAF).preferred(0,small).build());
            int count=detail.getPrerequisites().size(),pageSize=prerequisitePageSize(),pages=pageCount(count,pageSize);
            int page=Math.max(0,Math.min(detailPrerequisitePage,pages-1)),from=page*pageSize,to=Math.min(count,from+pageSize);
            for(int i=from;i<to;i++)prerequisites.child(LayoutSpec.of("quest-prerequisite-"+i,LayoutKind.LEAF).preferred(0,normal).build());
            if(pages>1)addPageLayout(prerequisites,"quest-prerequisite-pager",compact);
            if(count==0)prerequisites.child(LayoutSpec.of("quest-prerequisite-empty",LayoutKind.LEAF).flex(1).build());
            body=prerequisites.build();
        }else if(detailTab==1){
            LayoutSpec.Builder rewards=LayoutSpec.of("quest-detail-rewards",LayoutKind.COLUMN).flex(1)
                .padding(new Insets(2,2,2,2)).gap(1)
                .child(LayoutSpec.of("quest-reward-title",LayoutKind.LEAF).preferred(0,small).build());
            int rewardCount=detail.getRewards().size(),rewardPages=pageCount(rewardCount,1);
            int rewardIndex=Math.max(0,Math.min(detailRewardIndex,rewardCount-1));
            if(rewardCount>0){
                rewards.child(LayoutSpec.of("quest-reward-pager",LayoutKind.ROW).preferred(0,normal).gap(2)
                    .child(LayoutSpec.of("quest-reward-previous",LayoutKind.LEAF).preferred(compact?16:20,0).build())
                    .child(LayoutSpec.of("quest-reward-page",LayoutKind.LEAF).flex(1).build())
                    .child(LayoutSpec.of("quest-reward-next",LayoutKind.LEAF).preferred(compact?16:20,0).build()).build());
                QuestCenterVisualModel.RewardRow reward=detail.getRewards().get(rewardIndex);
                LayoutSpec.Builder summaryRow=LayoutSpec.of("quest-reward-summary",LayoutKind.ROW).preferred(0,normal).gap(2);
                if(reward.getItem()!=null)summaryRow.child(LayoutSpec.of("quest-reward-item-"+rewardIndex,LayoutKind.LEAF).preferred(compact?14:18,0).build());
                summaryRow.child(LayoutSpec.of("quest-reward-name",LayoutKind.LEAF).flex(1).build())
                    .child(LayoutSpec.of("quest-reward-detail",LayoutKind.LEAF).flex(2).build());rewards.child(summaryRow.build());
                if(reward.getChoices().isEmpty()&&!reward.getComponents().isEmpty()){
                    int size=componentPageSize(),pages=pageCount(reward.getComponents().size(),size),page=Math.max(0,Math.min(detailRewardComponentPage,pages-1));
                    int from=page*size,to=Math.min(reward.getComponents().size(),from+size);
                    for(int i=from;i<to;i++){int sourceIndex=reward.getComponents().get(i).getSourceIndex();LayoutSpec.Builder row=LayoutSpec.of("quest-reward-components-"+sourceIndex,LayoutKind.ROW).preferred(0,normal).gap(2);if(reward.getComponents().get(i).getItem()!=null)row.child(LayoutSpec.of("quest-reward-components-item-"+sourceIndex,LayoutKind.LEAF).preferred(compact?14:18,0).build());row.child(LayoutSpec.of("quest-reward-components-label-"+sourceIndex,LayoutKind.LEAF).flex(1).build());rewards.child(row.build());}
                    if(pages>1)addPageLayout(rewards,"quest-reward-components-pager",compact);
                }
                if(!reward.getChoices().isEmpty()){
                    rewards.child(LayoutSpec.of("quest-choice-title",LayoutKind.LEAF).preferred(0,small).build());
                    int size=choicePageSize(),pages=pageCount(reward.getChoices().size(),size),page=Math.max(0,Math.min(detailChoicePage,pages-1));
                    int from=page*size,to=Math.min(reward.getChoices().size(),from+size);
                    for(int i=from;i<to;i++){int sourceIndex=reward.getChoices().get(i).getSourceIndex();LayoutSpec.Builder row=LayoutSpec.of("quest-choice-row-"+rewardIndex+"-"+sourceIndex,LayoutKind.ROW).preferred(0,normal).gap(2);if(reward.getChoices().get(i).getItem()!=null)row.child(LayoutSpec.of("quest-choice-item-"+rewardIndex+"-"+sourceIndex,LayoutKind.LEAF).preferred(compact?14:18,0).build());row.child(LayoutSpec.of("quest-choice-"+rewardIndex+"-"+sourceIndex,LayoutKind.LEAF).flex(1).build());rewards.child(row.build());}
                    if(pages>1)addPageLayout(rewards,"quest-choice-pager",compact);
                }
                rewards.child(LayoutSpec.of("quest-claim",LayoutKind.LEAF).preferred(0,normal).build());
                if(summary.getState()==QuestCenterVisualModel.QuestState.CLAIMABLE&&hasUnselectedChoice(detail.getRewards()))rewards.child(LayoutSpec.of("quest-claim-random-choice",LayoutKind.LEAF).preferred(0,normal).build());
            }else rewards.child(LayoutSpec.of("quest-reward-empty",LayoutKind.LEAF).flex(1).build());
            body=rewards.build();
        }else if(detailTab==3){
            int pageSize=descriptionPageSize(),pages=pageCount(descriptionLines.size(),pageSize);
            LayoutSpec.Builder description=LayoutSpec.of("quest-description-page",LayoutKind.COLUMN).flex(1)
                .padding(new Insets(2,2,2,2)).gap(1)
                .child(LayoutSpec.of("quest-description-title",LayoutKind.LEAF).preferred(0,small).build());
            if(descriptionLines.isEmpty())description.child(LayoutSpec.of("quest-description-empty",LayoutKind.LEAF).flex(1).build());
            else{
                description.child(LayoutSpec.of("quest-description-text",LayoutKind.LEAF).flex(1).build());
                if(pages>1)addPageLayout(description,"quest-description-pager",compact);
            }
            body=description.build();
        }else{
            LayoutSpec.Builder tasks=LayoutSpec.of("quest-detail-tasks",LayoutKind.COLUMN).flex(1)
                .padding(new Insets(2,2,2,2)).gap(1)
                .child(LayoutSpec.of("quest-task-title",LayoutKind.LEAF).preferred(0,small).build());
            int taskCount=detail.getTasks().size(),taskIndex=Math.max(0,Math.min(detailTaskIndex,taskCount-1));
            if(taskCount>0){
                tasks.child(LayoutSpec.of("quest-task-pager",LayoutKind.ROW).preferred(0,normal).gap(2)
                    .child(LayoutSpec.of("quest-task-previous",LayoutKind.LEAF).preferred(compact?16:20,0).build())
                    .child(LayoutSpec.of("quest-task-page",LayoutKind.LEAF).flex(1).build())
                    .child(LayoutSpec.of("quest-task-next",LayoutKind.LEAF).preferred(compact?16:20,0).build()).build());
                QuestCenterVisualModel.TaskRow task=detail.getTasks().get(taskIndex);
                tasks.child(LayoutSpec.of("quest-task-name",LayoutKind.LEAF).preferred(0,small).build())
                    .child(LayoutSpec.of("quest-task-progress",LayoutKind.LEAF).preferred(0,small).build());
                if(task.getComponents().isEmpty())tasks.child(LayoutSpec.of("quest-task-detail",LayoutKind.LEAF).preferred(0,normal).build());
                else{
                    int size=componentPageSize(),pages=pageCount(task.getComponents().size(),size),page=Math.max(0,Math.min(detailTaskComponentPage,pages-1));
                    int from=page*size,to=Math.min(task.getComponents().size(),from+size);
                    for(int i=from;i<to;i++){int sourceIndex=task.getComponents().get(i).getSourceIndex();LayoutSpec.Builder row=LayoutSpec.of("quest-task-components-"+sourceIndex,LayoutKind.ROW).preferred(0,normal).gap(2);if(task.getComponents().get(i).getItem()!=null)row.child(LayoutSpec.of("quest-task-components-item-"+sourceIndex,LayoutKind.LEAF).preferred(compact?14:18,0).build());row.child(LayoutSpec.of("quest-task-components-label-"+sourceIndex,LayoutKind.LEAF).flex(1).build());tasks.child(row.build());}
                    if(pages>1)addPageLayout(tasks,"quest-task-components-pager",compact);
                }
                LayoutSpec.Builder actionsRow=LayoutSpec.of("quest-task-actions",LayoutKind.ROW).preferred(0,normal).gap(2);
                boolean hasActions=false;
                if(task.isConfirmable()&&!task.isComplete()){actionsRow.child(LayoutSpec.of("quest-task-confirm-"+taskIndex,LayoutKind.LEAF).flex(1).build());hasActions=true;}
                boolean hasConsumableTasks=false;for(QuestCenterVisualModel.TaskRow value:detail.getTasks())hasConsumableTasks|=value.isSubmittable()&&!value.isComplete();
                if(hasConsumableTasks){actionsRow.child(LayoutSpec.of("quest-detect-consumptions",LayoutKind.LEAF).flex(1).build());hasActions=true;}
                if(hasActions)tasks.child(actionsRow.build());
            }else tasks.child(LayoutSpec.of("quest-task-empty",LayoutKind.LEAF).flex(1).build());
            body=tasks.build();
        }
        return LayoutSpec.of("quest-detail",LayoutKind.COLUMN).flex(1).padding(new Insets(2,2,2,2)).gap(2)
            .child(heading).child(tabs).child(body).build();
    }

    private static void addPageLayout(LayoutSpec.Builder parent,String key,boolean compact){
        parent.child(LayoutSpec.of(key,LayoutKind.ROW).preferred(0,compact?11:14).gap(2)
            .child(LayoutSpec.of(key+"-previous",LayoutKind.LEAF).preferred(compact?16:20,0).build())
            .child(LayoutSpec.of(key+"-label",LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of(key+"-next",LayoutKind.LEAF).preferred(compact?16:20,0).build()).build());
    }

    @Override public UiInputNode modalInput(UiNode root,UiContext context){
        if(pendingParticipantAction==null)return null;
        final UiNode node=find(root,"quest-participant-confirm");
        if(node==null)return null;
        return new UiInputNode("quest-participant-confirm",node.getBounds(),true,new UiInputHandler(){
            public InputResult handle(UiInputNode target,UiEvent event){
                if(event.getPhase()!=UiEvent.Phase.TARGET)return InputResult.PASS;
                if(event.getType()==UiEvent.Type.KEY_DOWN&&event.getKeyCode()==UiKeyCode.ENTER){confirmParticipantAction();return InputResult.CONSUMED;}
                if(event.getType()==UiEvent.Type.KEY_DOWN&&event.getKeyCode()==UiKeyCode.ESCAPE){pendingParticipantAction=null;invalidation.run();return InputResult.CONSUMED;}
                if(event.getType()==UiEvent.Type.POINTER_DOWN){UiRect b=node.getBounds();int dw=Math.min(230,Math.max(80,b.getWidth()-40)),dh=Math.min(90,Math.max(50,b.getHeight()-30)),dx=b.getX()+(b.getWidth()-dw)/2,dy=b.getY()+(b.getHeight()-dh)/2,half=Math.max(0,(dw-28)/2);if(new UiRect(dx+10+half+8,dy+dh-22,half,16).contains(event.getX(),event.getY()))confirmParticipantAction();else{pendingParticipantAction=null;invalidation.run();}return InputResult.CONSUMED;}
                return InputResult.PASS;
            }});
    }

    private void confirmParticipantAction(){
        PendingParticipantAction pending=pendingParticipantAction;pendingParticipantAction=null;
        if(pending==null)return;
        if(pending.kind==1)actions.removeParticipantMember(pending.participant.getType(),pending.participant.getId(),pending.participant.getRevision(),pending.player);
        else if(pending.kind==2)actions.leaveParticipant(pending.participant.getType(),pending.participant.getId(),pending.participant.getRevision());
        else if(pending.kind==3)actions.transferParticipantOwner(pending.participant.getType(),pending.participant.getId(),pending.participant.getRevision(),pending.player);
        invalidation.run();
    }

    private static UiNode find(UiNode node,String key){if(node==null)return null;if(node.getElement().getKey()!=null&&key.equals(node.getElement().getKey().getValue()))return node;for(UiNode child:node.getChildren()){UiNode found=find(child,key);if(found!=null)return found;}return null;}

    private static final class PendingParticipantAction{
        private final int kind;private final QuestCenterVisualModel.Participant participant;private final String player,title,detail;
        private PendingParticipantAction(int kind,QuestCenterVisualModel.Participant participant,String player,String title,String detail){this.kind=kind;this.participant=participant;this.player=player;this.title=title;this.detail=detail;}
        private static PendingParticipantAction remove(QuestCenterVisualModel.Participant p,String player){return new PendingParticipantAction(1,p,player,"确认移除成员","成员 "+player+" 将停止参与 "+p.getName()+" 的后续共享进度；已经冻结给他的奖励不会被收回。");}
        private static PendingParticipantAction leave(QuestCenterVisualModel.Participant p){return new PendingParticipantAction(2,p,"","确认离开主体","离开 "+p.getName()+" 后不再参与后续共享进度；既有个人奖励归属保持不变。所有者必须先转让所有权。");}
        private static PendingParticipantAction transfer(QuestCenterVisualModel.Participant p,String player){return new PendingParticipantAction(3,p,player,"确认转让所有权","将 "+p.getName()+" 的所有权转让给 "+player+"；你将降级为管理员。");}
    }

    void openParticipantManagementForPreview(){participantManagementOpen=true;invalidation.run();}
    boolean requestLeaveForPreview(){QuestCenterVisualModel.Participant participant=selectedParticipant();if(participant==null)return false;pendingParticipantAction=PendingParticipantAction.leave(participant);invalidation.run();return true;}

    private UiElement filter(String key,String text,final String value){return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.SELECTED,value.equals(model.getFilter())).prop(StandardWidgets.ACTION,handler(new Runnable(){public void run(){actions.changeFilter(value);}})).build();}
    private static UiElement state(String key,String type,String text){return UiElement.type(type).key(key).prop(StandardWidgets.TEXT,text).build();}
    private static UiElement badge(String key,String text,String tone){return UiElement.type("Badge").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.TONE,tone).build();}
    private static UiElement label(String key,String text,String role,int lines,boolean bold){return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.TEXT_ROLE,role).prop(StandardWidgets.MAX_LINES,lines).prop(StandardWidgets.BOLD,bold).build();}
    private static UiElement button(String key,String text,final Runnable run,boolean enabled){return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT,text).prop(StandardWidgets.ENABLED,enabled).prop(StandardWidgets.ACTION,handler(run)).build();}
    private static UiActionHandler handler(final Runnable run){return new UiActionHandler(){public InputResult handle(UiEvent event){run.run();return InputResult.CONSUMED;}};}
    private static String status(QuestCenterVisualModel.QuestSummary q){switch(q.getState()){case LOCKED:return "未解锁";case IN_PROGRESS:return q.getCompletedTasks()+"/"+q.getTotalTasks();case CLAIMABLE:return "待领取";case COMPLETED:return "已完成";default:return "可开始";}}
    private static String tone(QuestCenterVisualModel.QuestState state){return state==QuestCenterVisualModel.QuestState.CLAIMABLE?"accent":state==QuestCenterVisualModel.QuestState.COMPLETED?"success":state==QuestCenterVisualModel.QuestState.LOCKED?"textMuted":"border";}
    private static String chapterKey(QuestCenterVisualModel.Chapter chapter){return "quest-chapter-"+chapter.getId();}
    private static String questKey(QuestCenterVisualModel.QuestSummary quest){return "quest-row-"+quest.getId();}
    private static String questIconKey(QuestCenterVisualModel.QuestSummary quest){return "quest-icon-"+quest.getId();}
    private static String questNameKey(QuestCenterVisualModel.QuestSummary quest){return "quest-name-"+quest.getId();}
    private static String questSubtitleKey(QuestCenterVisualModel.QuestSummary quest){return "quest-subtitle-"+quest.getId();}
    private static String questStatusKey(QuestCenterVisualModel.QuestSummary quest){return "quest-status-"+quest.getId();}
    private static boolean hasUnselectedChoice(List<QuestCenterVisualModel.RewardRow> rewards){
        for(QuestCenterVisualModel.RewardRow reward:rewards){
            if(reward.requiresChoice()&&reward.getSelectedChoiceIndex()<0)return true;
        }
        return false;
    }
}

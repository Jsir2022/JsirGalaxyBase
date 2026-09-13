package com.jsirgalaxybase.ui2.terminal;

import java.util.List;

import com.jsirgalaxybase.ui2.component.ComponentUiDocument;
import com.jsirgalaxybase.ui2.component.StandardWidgets;
import com.jsirgalaxybase.ui2.component.UiActionHandler;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;

/** Player-facing quest browse/detail document shared by Java2D and Minecraft hosts. */
public class QuestCenterVisualDocument extends ComponentUiDocument {
    private QuestCenterVisualModel model;
    private final TerminalActionPort shellActions;
    private final QuestCenterActionPort actions;
    private TerminalWindowProfile profile;

    public QuestCenterVisualDocument(QuestCenterVisualModel model, TerminalActionPort shellActions,
        QuestCenterActionPort actions, TerminalWindowProfile profile) {
        super(StandardWidgets.create());
        if (model == null) throw new IllegalArgumentException("model is required");
        this.model=model;this.shellActions=shellActions==null?TerminalActionPort.NONE:shellActions;
        this.actions=actions==null?QuestCenterActionPort.NONE:actions;
        this.profile=profile==null?TerminalWindowProfile.STANDARD:profile;
    }

    public final void updateVisualModel(QuestCenterVisualModel value) {
        if (value == null) throw new IllegalArgumentException("model is required");
        model=value;
    }

    @Override public UiElement build(UiContext context) {
        UiElement content=model.getView()==QuestCenterVisualModel.View.DETAIL&&model.getDetail()!=null?detail():browse();
        return TerminalVisualShell.build(model.getShell(),content,shellActions,"任务中心","career");
    }

    private UiElement browse() {
        UiElement tools=UiElement.type("Row").key("quest-tools")
            .child(label("quest-search",model.getQuery().isEmpty()?"搜索任务…":"搜索: "+model.getQuery(),"caption",1,false))
            .child(filter("quest-filter-all","全部","all"))
            .child(filter("quest-filter-active","进行中","active"))
            .child(filter("quest-filter-claim","待领取","claimable"))
            .child(button("quest-admin-open","任务管理",new Runnable(){public void run(){actions.openManagement();}},true)).build();
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
        return UiElement.type("Column").key("quest-browse").child(tools)
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
        QuestCenterVisualModel.QuestSummary summary=detail.getSummary();
        UiElement.Builder heading=UiElement.type("Card").key("quest-detail-heading")
            .child(button("quest-detail-back","‹ 任务列表",new Runnable(){public void run(){actions.backToBrowse();}},true));
        if(summary.getIcon()!=null)heading.child(UiElement.type("ExternalItem").key("quest-detail-icon")
            .prop(StandardWidgets.EXTERNAL_ID,"quest:detail:icon").build());
        heading.child(label("quest-detail-title",summary.getName(),"section",1,true))
            .child(badge("quest-detail-status",status(summary),tone(summary.getState())))
            .child(button("quest-detail-track",summary.isTracked()?"★ 已追踪":"☆ 追踪",new Runnable(){public void run(){actions.toggleTracking(detail.getSummary().getId());}},true))
            .child(label("quest-detail-description",detail.getDescription(),"caption",2,false))
            .child(label("quest-detail-prereq",detail.getPrerequisiteText()+" · "+detail.getRepeatText(),"caption",1,false));
        UiElement.Builder tasks=UiElement.type("Card").key("quest-detail-tasks")
            .child(label("quest-task-title","任务目标","caption",1,true));
        for(int i=0;i<Math.min(6,detail.getTasks().size());i++){
            QuestCenterVisualModel.TaskRow task=detail.getTasks().get(i);
            tasks.child(UiElement.type("Row").key("quest-task-"+i)
                .child(label("quest-task-name-"+i,(task.isComplete()?"✓ ":"○ ")+task.getLabel(),"caption",1,task.isComplete()))
                .child(label("quest-task-detail-"+i,task.getDetail(),"caption",1,false))
                .child(label("quest-task-progress-"+i,task.getValue()+" / "+task.getTarget(),"caption",1,false)).build());
        }
        UiElement.Builder rewards=UiElement.type("Card").key("quest-detail-rewards")
            .child(label("quest-reward-title","任务奖励","caption",1,true));
        List<QuestCenterVisualModel.RewardRow> rewardRows=detail.getRewards();
        for(int i=0;i<Math.min(4,rewardRows.size());i++){
            QuestCenterVisualModel.RewardRow reward=rewardRows.get(i);
            UiElement.Builder row=UiElement.type("Row").key("quest-reward-"+i);
            if(reward.getItem()!=null)row.child(UiElement.type("ExternalItem").key("quest-reward-item-"+i)
                .prop(StandardWidgets.EXTERNAL_ID,"quest:reward:"+i).prop(StandardWidgets.QUANTITY,
                    AssetCenterVisualDocument.compact(reward.getItem().getQuantity())).build());
            row.child(label("quest-reward-name-"+i,reward.getLabel(),"caption",1,true))
                .child(label("quest-reward-detail-"+i,reward.getDetail(),"caption",1,false));
            rewards.child(row.build());
            for(int optionIndex=0;optionIndex<Math.min(4,reward.getChoices().size());optionIndex++){
                final int selectedOption=optionIndex;final QuestCenterVisualModel.RewardRow choiceReward=reward;
                QuestCenterVisualModel.ChoiceOption option=reward.getChoices().get(optionIndex);
                rewards.child(UiElement.type("Button").key("quest-choice-"+i+"-"+optionIndex)
                    .prop(StandardWidgets.TEXT,(optionIndex+1)+". "+option.getLabel()+" ×"+option.getItem().getQuantity())
                    .prop(StandardWidgets.SELECTED,reward.getSelectedChoiceIndex()==optionIndex)
                    .prop(StandardWidgets.ENABLED,reward.getSelectedChoiceIndex()<0)
                    .prop(StandardWidgets.ACTION,handler(new Runnable(){public void run(){actions.selectRewardChoice(
                        detail.getSummary().getId(),choiceReward.getKey(),selectedOption);}})).build());
            }
        }
        boolean canClaim=summary.getState()==QuestCenterVisualModel.QuestState.CLAIMABLE;
        rewards.child(button("quest-claim",canClaim?"领取全部奖励":summary.getState()==QuestCenterVisualModel.QuestState.COMPLETED?"奖励已领取":"完成后可领取",
            new Runnable(){public void run(){actions.claim(detail.getSummary().getId());}},canClaim));
        return UiElement.type("Column").key("quest-detail").child(heading.build())
            .child(UiElement.type("Row").key("quest-detail-columns").child(tasks.build()).child(rewards.build()).build()).build();
    }

    @Override public LayoutSpec layout(UiElement root,UiSize viewport,UiContext context){
        TerminalVisualMetrics metrics=TerminalVisualMetrics.compute(viewport,profile);boolean compact=metrics.getBounds().getHeight()<=220;
        LayoutSpec body=model.getView()==QuestCenterVisualModel.View.DETAIL&&model.getDetail()!=null?detailLayout(compact):browseLayout(compact);
        return TerminalVisualShell.layout(viewport,model.getShell(),body,profile);
    }

    private LayoutSpec browseLayout(boolean compact){
        LayoutSpec tools=LayoutSpec.of("quest-tools",LayoutKind.ROW).preferred(0,compact?16:20).gap(2)
            .child(LayoutSpec.of("quest-search",LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-filter-all",LayoutKind.LEAF).preferred(36,0).build())
            .child(LayoutSpec.of("quest-filter-active",LayoutKind.LEAF).preferred(46,0).build())
            .child(LayoutSpec.of("quest-filter-claim",LayoutKind.LEAF).preferred(46,0).build())
            .child(LayoutSpec.of("quest-admin-open",LayoutKind.LEAF).preferred(compact?48:58,0).build()).build();
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
        return LayoutSpec.of("quest-browse",LayoutKind.COLUMN).flex(1).padding(new Insets(3,3,3,3)).gap(3).child(tools).child(browser).build();
    }

    private static LayoutSpec pagerLayout(String key,boolean compact){return LayoutSpec.of(key,LayoutKind.ROW).preferred(0,compact?15:17).gap(2)
        .child(LayoutSpec.of(key+"-previous",LayoutKind.LEAF).preferred(18,0).build())
        .child(LayoutSpec.of(key+"-label",LayoutKind.LEAF).flex(1).build())
        .child(LayoutSpec.of(key+"-next",LayoutKind.LEAF).preferred(18,0).build()).build();}

    private LayoutSpec detailLayout(boolean compact){
        QuestCenterVisualModel.QuestDetail detail=model.getDetail();QuestCenterVisualModel.QuestSummary summary=detail.getSummary();
        LayoutSpec.Builder heading=LayoutSpec.of("quest-detail-heading",LayoutKind.ROW).preferred(0,compact?48:58).padding(new Insets(4,4,4,4)).gap(3)
            .child(LayoutSpec.of("quest-detail-back",LayoutKind.LEAF).preferred(compact?42:48,compact?16:20).build());
        if(summary.getIcon()!=null)heading.child(LayoutSpec.of("quest-detail-icon",LayoutKind.LEAF).preferred(compact?20:24,compact?20:24).build());
        heading.child(LayoutSpec.of("quest-detail-title",LayoutKind.LEAF).preferred(compact?60:90,18).build())
            .child(LayoutSpec.of("quest-detail-status",LayoutKind.LEAF).preferred(compact?42:48,18).build())
            .child(LayoutSpec.of("quest-detail-track",LayoutKind.LEAF).preferred(compact?44:58,18).build())
            .child(LayoutSpec.of("quest-detail-description",LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-detail-prereq",LayoutKind.LEAF).preferred(compact?40:70,18).build());
        LayoutSpec.Builder tasks=LayoutSpec.of("quest-detail-tasks",LayoutKind.COLUMN).flex(3).padding(new Insets(3,3,3,3)).gap(2)
            .child(LayoutSpec.of("quest-task-title",LayoutKind.LEAF).preferred(0,12).build());
        for(int i=0;i<Math.min(6,detail.getTasks().size());i++)tasks.child(LayoutSpec.of("quest-task-"+i,LayoutKind.ROW).preferred(0,compact?17:20).gap(2)
            .child(LayoutSpec.of("quest-task-name-"+i,LayoutKind.LEAF).flex(2).build()).child(LayoutSpec.of("quest-task-detail-"+i,LayoutKind.LEAF).flex(2).build()).child(LayoutSpec.of("quest-task-progress-"+i,LayoutKind.LEAF).preferred(48,0).build()).build());
        LayoutSpec.Builder rewards=LayoutSpec.of("quest-detail-rewards",LayoutKind.COLUMN).flex(2).padding(new Insets(3,3,3,3)).gap(2)
            .child(LayoutSpec.of("quest-reward-title",LayoutKind.LEAF).preferred(0,12).build());
        for(int i=0;i<Math.min(4,detail.getRewards().size());i++){QuestCenterVisualModel.RewardRow reward=detail.getRewards().get(i);LayoutSpec.Builder row=LayoutSpec.of("quest-reward-"+i,LayoutKind.ROW).preferred(0,compact?18:22).gap(2);if(reward.getItem()!=null)row.child(LayoutSpec.of("quest-reward-item-"+i,LayoutKind.LEAF).preferred(compact?16:20,compact?16:20).build());row.child(LayoutSpec.of("quest-reward-name-"+i,LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of("quest-reward-detail-"+i,LayoutKind.LEAF).flex(1).build());rewards.child(row.build());for(int optionIndex=0;optionIndex<Math.min(4,reward.getChoices().size());optionIndex++)rewards.child(LayoutSpec.of("quest-choice-"+i+"-"+optionIndex,LayoutKind.LEAF).preferred(0,compact?16:19).build());}
        rewards.child(LayoutSpec.of("quest-claim",LayoutKind.LEAF).preferred(0,compact?18:22).build());
        return LayoutSpec.of("quest-detail",LayoutKind.COLUMN).flex(1).padding(new Insets(3,3,3,3)).gap(3).child(heading.build())
            .child(LayoutSpec.of("quest-detail-columns",LayoutKind.ROW).flex(1).gap(3).child(tasks.build()).child(rewards.build()).build()).build();
    }

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
}

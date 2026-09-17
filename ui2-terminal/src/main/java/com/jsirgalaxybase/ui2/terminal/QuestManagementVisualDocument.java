package com.jsirgalaxybase.ui2.terminal;

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

public final class QuestManagementVisualDocument extends ComponentUiDocument {
    private QuestManagementVisualModel model;
    private final TerminalActionPort shell;
    private final QuestManagementActionPort actions;
    private final TerminalWindowProfile profile;
    private final Runnable invalidation;
    private int width, height;
    private EditState edit;
    private CreateState create;
    private boolean batchMode;
    private boolean batchConfirmOpen;
    private final java.util.LinkedHashMap<String,Integer> selectedBatch = new java.util.LinkedHashMap<String,Integer>();

    public QuestManagementVisualDocument(QuestManagementVisualModel model, TerminalActionPort shell,
        QuestManagementActionPort actions, TerminalWindowProfile profile) {
        this(model,shell,actions,profile,new Runnable(){public void run(){}});
    }

    public QuestManagementVisualDocument(QuestManagementVisualModel model, TerminalActionPort shell,
        QuestManagementActionPort actions, TerminalWindowProfile profile,Runnable invalidation) {
        super(StandardWidgets.create());
        if (model == null) throw new IllegalArgumentException("model is required");
        this.model = model;
        this.shell = shell == null ? TerminalActionPort.NONE : shell;
        this.actions = actions == null ? QuestManagementActionPort.NONE : actions;
        this.profile = profile == null ? TerminalWindowProfile.STANDARD : profile;
        this.invalidation=invalidation==null?new Runnable(){public void run(){}}:invalidation;
    }

    public void update(QuestManagementVisualModel value) {
        if (value == null) throw new IllegalArgumentException("model is required");
        model = value;
        java.util.Iterator<String> selected = selectedBatch.keySet().iterator();
        while (selected.hasNext()) { String id = selected.next(); boolean present=false; for (QuestManagementVisualModel.Definition definition : value.getDefinitions()) if (id.equals(definition.getId()) && "PUBLISHED".equals(definition.getLifecycle())) { present=true; break; } if (!present) selected.remove(); }
        if (batchConfirmOpen && selectedBatch.size() < 2) batchConfirmOpen = false;
        if(edit!=null&&(value.getDetail()==null||!edit.id.equals(value.getDetail().getSummary().getId())
            ||!edit.hash.equals(value.getDetail().getSummary().getHash())))edit=null;
    }

    /** Opens a local edit session for a draft; useful to hosts and deterministic visual scenarios. */
    public boolean beginEdit(){if(model.getDetail()==null||!"DRAFT".equals(model.getDetail().getSummary().getLifecycle()))return false;edit=new EditState(model.getDetail());invalidation.run();return true;}
    public boolean beginFirstElementEdit(boolean task){if(!beginEdit())return false;java.util.List<QuestManagementVisualModel.Element> values=task?model.getDetail().getTasks():model.getDetail().getRewards();if(values.isEmpty()){edit=null;return false;}beginElementEdit(task,values.get(0));return true;}
    public boolean beginNewElementForPreview(boolean task){if(!beginEdit())return false;return beginNewElement(task);}
    public boolean beginPrerequisiteEditForPreview(){if(!beginEdit())return false;edit.prerequisitePicker=true;invalidation.run();return true;}
    public boolean beginBehaviorEditForPreview(){if(!beginEdit())return false;edit.behaviorPicker=true;invalidation.run();return true;}
    public boolean beginBehaviorEditForPreview(int page){if(!beginBehaviorEditForPreview())return false;edit.behaviorPage=Math.max(0,Math.min(2,page));return true;}
    public void beginCreateForPreview(){create=new CreateState();invalidation.run();}
    /** Opens the batch confirmation for deterministic hosts without dispatching a destructive action. */
    public boolean beginBatchConfirmationForPreview(){if(selectedBatch.size()<2)return false;batchConfirmOpen=true;invalidation.run();return true;}
    /** Selects published rows for deterministic visual/input scenarios; production clicks use the same map. */
    void selectBatchForPreview(String... ids) {
        selectedBatch.clear();
        if (ids != null) for (String id : ids) for (QuestManagementVisualModel.Definition definition : model.getDefinitions())
            if (id != null && id.equals(definition.getId()) && "PUBLISHED".equals(definition.getLifecycle())) {
                selectedBatch.put(id, Integer.valueOf(definition.getVersion())); break;
            }
        batchMode = !selectedBatch.isEmpty(); invalidation.run();
    }

    @Override public UiElement build(UiContext context) {
        UiElement content = create!=null?creator():model.getDetail() == null ? list() : edit==null?detail():editor();
        if (batchConfirmOpen) content = UiElement.type("Stack").key("quest-admin-stack")
            .child(content).child(batchConfirmation()).build();
        return TerminalVisualShell.build(model.getShell(), content, shell,
            model.getDetail() == null ? "任务管理" : "定义详情", "career");
    }

    private UiElement list() {
        UiElement tools = UiElement.type("Row").key("quest-admin-tools")
            .child(button("quest-admin-back", "‹ 玩家任务", new Runnable() { public void run() { actions.back(); } }, true))
            .child(button("quest-admin-chapters", "章节", new Runnable(){public void run(){actions.openChapters();}}, true))
            .child(button("quest-admin-create", "＋ 新建", new Runnable(){public void run(){create=new CreateState();invalidation.run();}}, true))
            .child(button("quest-admin-batch-mode", batchMode ? "取消多选" : "多选退役", new Runnable(){public void run(){batchMode=!batchMode;if(!batchMode)selectedBatch.clear();invalidation.run();}}, true))
            .child(button("quest-admin-batch-retire", "退役 " + selectedBatch.size() + " 项", new Runnable(){public void run(){
                if(selectedBatch.size()<2)return; batchConfirmOpen=true;invalidation.run();
            }}, selectedBatch.size()>=2))
            .child(label("quest-admin-query", model.getQuery().isEmpty() ? "名称或 UUID" : "搜索: " + model.getQuery(), false))
            .child(filter("quest-admin-all", "全部", ""))
            .child(filter("quest-admin-draft", "草稿", "DRAFT"))
            .child(filter("quest-admin-published", "已发布", "PUBLISHED"))
            .child(filter("quest-admin-retired", "已退役", "RETIRED")).build();
        UiElement.Builder rows = UiElement.type("Card").key("quest-admin-list")
            .child(label("quest-admin-title", "任务定义 · " + model.getTotal() + " 项", true));
        if (model.getDefinitions().isEmpty()) {
            rows.child(UiElement.type("EmptyState").key("quest-admin-empty")
                .prop(StandardWidgets.TEXT, model.getMessage().isEmpty() ? "没有符合条件的任务定义" : model.getMessage()).build());
        } else for (int i = 0; i < model.getDefinitions().size(); i++) {
            final QuestManagementVisualModel.Definition definition = model.getDefinitions().get(i);
            rows.child(UiElement.type("Row").key(rowKey(definition))
                .prop(StandardWidgets.ACTION, handler(new Runnable() { public void run() {
                    if(batchMode && "PUBLISHED".equals(definition.getLifecycle())) { if(selectedBatch.containsKey(definition.getId())) selectedBatch.remove(definition.getId()); else selectedBatch.put(definition.getId(),Integer.valueOf(definition.getVersion())); invalidation.run(); }
                    else actions.select(definition.getId(), definition.getVersion());
                }}))
                .prop(StandardWidgets.SELECTED, Boolean.valueOf(batchMode && selectedBatch.containsKey(definition.getId())))
                .child(label("quest-admin-name-" + i, definition.getName(), true))
                .child(label("quest-admin-version-" + i, "v" + definition.getVersion(), false))
                .child(UiElement.type("Badge").key("quest-admin-life-" + i)
                    .prop(StandardWidgets.TEXT, definition.getLifecycle())
                    .prop(StandardWidgets.TONE, "PUBLISHED".equals(definition.getLifecycle()) ? "success" : "accent").build())
                .child(label("quest-admin-count-" + i,
                    definition.getTasks() + " 目标 · " + definition.getRewards() + " 奖励", false)).build());
        }
        rows.child(UiElement.type("Row").key("quest-admin-pager")
            .child(button("quest-admin-prev", "‹", new Runnable() { public void run() { actions.page(model.getPage() - 1); } }, model.getPage() > 0))
            .child(label("quest-admin-page", (model.getPage() + 1) + " / " + model.getPages(), false))
            .child(button("quest-admin-next", "›", new Runnable() { public void run() { actions.page(model.getPage() + 1); } }, model.getPage() + 1 < model.getPages())).build());
        UiElement.Builder content=UiElement.type("Column").key("quest-admin").child(tools);
        if(!model.getMigrations().isEmpty()){QuestManagementVisualModel.Migration migration=model.getMigrations().get(0);content.child(label("quest-migration-latest","迁移 "+migration.getBatchId()+" · "+migration.getStatus()+" · 新增 "+migration.getCreated()+" · 变更 "+migration.getVersioned()+" · 相同 "+migration.getUnchanged()+" · 阻塞 "+migration.getErrorCount()+" · 警告 "+migration.getWarningCount()+" · "+shortHash(migration.getSourceHash()),false));String diagnostic=firstDiagnostic(migration.getDiagnostic());if(!diagnostic.isEmpty())content.child(label("quest-migration-diagnostic",diagnostic,false));}
        return content.child(rows.build()).build();
    }

    private static String shortHash(String value){return value==null?"":value.substring(0,Math.min(12,value.length()));}
    private static String firstDiagnostic(String value){if(value==null)return "";for(String line:value.split("\\r?\\n"))if(line.startsWith("ERROR|")||line.startsWith("WARNING|"))return line.replace("|"," · ");return "";}

    private UiElement batchConfirmation() {
        return UiElement.type("Dialog").key("quest-admin-batch-confirm")
            .prop(StandardWidgets.TEXT, "确认批量退役")
            .prop(StandardWidgets.DETAIL, "将退役 " + selectedBatch.size()
                + " 个已发布任务定义。服务端会再次检查外部依赖、章节放置、精确版本并以单事务提交。")
            .prop(StandardWidgets.CANCEL_TEXT, "取消")
            .prop(StandardWidgets.CONFIRM_TEXT, "确认退役").build();
    }

    @Override public UiInputNode modalInput(UiNode root, UiContext context) {
        if (!batchConfirmOpen) return null;
        return new UiInputNode("quest-admin-batch-confirm", new UiRect(0, 0, width, height), true,
            new UiInputHandler() {
                public InputResult handle(UiInputNode target, UiEvent event) {
                    if (event.getPhase() != UiEvent.Phase.TARGET) return InputResult.PASS;
                    if (event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.ESCAPE) {
                        batchConfirmOpen = false; invalidation.run(); return InputResult.CONSUMED;
                    }
                    if (event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.ENTER) {
                        confirmBatchRetire(); return InputResult.CONSUMED;
                    }
                    if (event.getType() == UiEvent.Type.POINTER_DOWN) {
                        if (batchConfirmHit(event)) confirmBatchRetire();
                        else { batchConfirmOpen = false; invalidation.run(); }
                        return InputResult.CONSUMED;
                    }
                    return InputResult.PASS;
                }
            });
    }

    private void confirmBatchRetire() {
        if (selectedBatch.size() < 2) { batchConfirmOpen = false; invalidation.run(); return; }
        actions.batchRetire(new java.util.ArrayList<String>(selectedBatch.keySet()),
            new java.util.ArrayList<Integer>(selectedBatch.values()));
        selectedBatch.clear(); batchMode = false; batchConfirmOpen = false; invalidation.run();
    }

    private boolean batchConfirmHit(UiEvent event) {
        int dialogWidth = Math.min(260, Math.max(100, width - 32));
        int dialogHeight = Math.min(100, Math.max(60, height - 24));
        int x = (width - dialogWidth) / 2, y = (height - dialogHeight) / 2;
        int half = Math.max(0, (dialogWidth - 28) / 2);
        return new UiRect(x + 14 + half, y + dialogHeight - 24, half, 18)
            .contains(event.getX(), event.getY());
    }

    private UiElement creator(){return UiElement.type("Column").key("quest-admin-creator").child(UiElement.type("Card").key("quest-admin-create-card")
        .child(label("quest-admin-create-title","创建任务草稿",true))
        .child(UiElement.type("TextField").key("quest-admin-create-name").prop(StandardWidgets.TEXT,create.name).prop(StandardWidgets.PLACEHOLDER,"任务名称").prop(StandardWidgets.FOCUSED,create.focused).prop(StandardWidgets.ACTION,new UiActionHandler(){public InputResult handle(UiEvent event){if(event.getType()==UiEvent.Type.POINTER_DOWN)create.focused=true;else if(event.getType()==UiEvent.Type.TEXT_INPUT&&!Character.isISOControl(event.getTypedChar())&&create.name.length()<96)create.name+=event.getTypedChar();else if(event.getType()==UiEvent.Type.KEY_DOWN&&(event.getKeyCode()==UiKeyCode.BACKSPACE||event.getKeyCode()==UiKeyCode.DELETE)&&!create.name.isEmpty())create.name=create.name.substring(0,create.name.length()-1);invalidation.run();return InputResult.CONSUMED;}}).build())
        .child(button("quest-admin-template-empty","空白任务",new Runnable(){public void run(){create.template="EMPTY";invalidation.run();}},true))
        .child(button("quest-admin-template-manual","人工确认目标",new Runnable(){public void run(){create.template="MANUAL_CHECK";invalidation.run();}},true))
        .child(button("quest-admin-template-xp","经验目标",new Runnable(){public void run(){create.template="EXPERIENCE";invalidation.run();}},true))
        .child(label("quest-admin-create-selected","当前模板 · "+create.template,false))
        .child(UiElement.type("Row").key("quest-admin-create-actions")
            .child(button("quest-admin-create-cancel","取消",new Runnable(){public void run(){create=null;invalidation.run();}},true))
            .child(button("quest-admin-create-submit","创建草稿",new Runnable(){public void run(){actions.createDraft(create.template,create.name);create=null;invalidation.run();}},!create.name.trim().isEmpty())).build()).build()).build();}

    private UiElement detail() {
        final QuestManagementVisualModel.Detail detail = model.getDetail();
        final QuestManagementVisualModel.Definition summary = detail.getSummary();
        UiElement.Builder actionsRow=UiElement.type("Row").key("quest-admin-detail-actions")
            .child(button("quest-admin-detail-back", "‹ 定义列表", new Runnable() { public void run() { actions.closeDetail(); } }, true))
            .child(label("quest-admin-detail-name", summary.getName() + " · v" + summary.getVersion(), true))
            .child(UiElement.type("Badge").key("quest-admin-detail-life").prop(StandardWidgets.TEXT, summary.getLifecycle())
                .prop(StandardWidgets.TONE, "PUBLISHED".equals(summary.getLifecycle()) ? "success" : "accent").build());
        if ("DRAFT".equals(summary.getLifecycle())) actionsRow.child(button("quest-admin-detail-publish", "发布",
            new Runnable() { public void run() { actions.publish(summary.getId(), summary.getVersion(), summary.getHash()); } }, true));
        if ("DRAFT".equals(summary.getLifecycle())) actionsRow.child(button("quest-admin-detail-edit", "编辑",
            new Runnable() { public void run() { beginEdit(); } }, true));
        final boolean impactKnown=detail.getOptions().containsKey("impact.safeToRetire");
        final boolean safeToRetire=impactKnown&&Boolean.parseBoolean(detail.getOptions().get("impact.safeToRetire"));
        if ("PUBLISHED".equals(summary.getLifecycle())) actionsRow.child(button("quest-admin-detail-retire", "退役",
            new Runnable() { public void run() { actions.retire(summary.getId(), summary.getVersion()); } }, safeToRetire));
        UiElement.Builder heading = UiElement.type("Card").key("quest-admin-detail-heading").child(actionsRow.build())
            .child(label("quest-admin-detail-meta", "前置 " + detail.getPrerequisiteLogic() + " · 目标 "
                + detail.getTaskLogic() + " · 前置任务 " + detail.getPrerequisites().size()+" · "+detail.getDescription(), false));
        UiElement.Builder tasks = elementCard("quest-admin-detail-tasks", "任务目标", detail.getTasks(), true);
        UiElement.Builder rewards = elementCard("quest-admin-detail-rewards", "任务奖励", detail.getRewards(), false);
        UiElement.Builder impact=UiElement.type("Card").key("quest-admin-detail-impact")
            .child(label("quest-admin-detail-impact-title","变更影响预检",true))
            .child(label("quest-admin-detail-impact-counts",impactKnown
                ?"直接依赖 "+option(detail,"impact.direct","0")+" · 间接依赖 "+option(detail,"impact.transitive","0")
                    +" · 章节放置 "+option(detail,"impact.placements","0")
                :"服务端影响预检不可用，禁止退役",false));
        String impactSummary=option(detail,"impact.summary","");
        if(!impactSummary.isEmpty())impact.child(label("quest-admin-detail-impact-summary","涉及："+impactSummary,false));
        if(Boolean.parseBoolean(option(detail,"impact.truncated","false")))impact.child(label(
            "quest-admin-detail-impact-truncated","影响结果已截断，必须先缩小或修复关系图",false));
        return UiElement.type("Column").key("quest-admin-detail").child(heading.build()).child(impact.build())
            .child(UiElement.type("Row").key("quest-admin-detail-columns").child(tasks.build()).child(rewards.build()).build()).build();
    }

    private static String option(QuestManagementVisualModel.Detail detail,String key,String fallback){String value=detail.getOptions().get(key);return value==null?fallback:value;}

    private UiElement editor(){if(edit.element!=null)return elementEditor();if(edit.prerequisitePicker)return prerequisiteEditor();if(edit.behaviorPicker)return behaviorEditor();final QuestManagementVisualModel.Detail source=model.getDetail();final QuestManagementVisualModel.Definition summary=source.getSummary();
        return UiElement.type("Column").key("quest-admin-editor")
            .child(UiElement.type("Card").key("quest-admin-editor-card")
                .child(label("quest-admin-editor-title","编辑草稿 · "+summary.getName()+" · v"+summary.getVersion(),true))
                .child(field("quest-admin-editor-name","任务名称",0,256))
                .child(field("quest-admin-editor-description","任务说明",1,4096))
                .child(UiElement.type("Row").key("quest-admin-editor-prerequisite")
                    .child(label("quest-admin-editor-prerequisite-label","前置逻辑",false))
                    .child(choice("quest-admin-editor-prerequisite-and","AND",true,"AND".equals(edit.prerequisiteLogic)))
                    .child(choice("quest-admin-editor-prerequisite-or","OR",true,"OR".equals(edit.prerequisiteLogic))).build())
                .child(UiElement.type("Row").key("quest-admin-editor-task")
                    .child(label("quest-admin-editor-task-label","目标逻辑",false))
                    .child(choice("quest-admin-editor-task-and","AND",false,"AND".equals(edit.taskLogic)))
                    .child(choice("quest-admin-editor-task-or","OR",false,"OR".equals(edit.taskLogic))).build())
                .child(button("quest-admin-editor-prerequisites","前置任务 · "+edit.prerequisites.size(),new Runnable(){public void run(){edit.prerequisitePicker=true;invalidation.run();}},true))
                .child(button("quest-admin-editor-behavior","重复与行为选项",new Runnable(){public void run(){edit.behaviorPicker=true;invalidation.run();}},true))
                .child(UiElement.type("Row").key("quest-admin-editor-actions")
                    .child(button("quest-admin-editor-cancel","取消",new Runnable(){public void run(){edit=null;invalidation.run();}},true))
                    .child(button("quest-admin-editor-save","保存",new Runnable(){public void run(){actions.saveBasics(summary.getId(),summary.getVersion(),summary.getHash(),edit.name,edit.description,edit.prerequisiteLogic,edit.taskLogic);}},!edit.name.trim().isEmpty())).build()).build()).build();
    }

    private UiElement field(String key,String placeholder,final int target,final int max){String value=target==0?edit.name:edit.description;return UiElement.type("TextField").key(key).prop(StandardWidgets.TEXT,value).prop(StandardWidgets.PLACEHOLDER,placeholder).prop(StandardWidgets.FOCUSED,edit.focus==target).prop(StandardWidgets.ACTION,new UiActionHandler(){public InputResult handle(UiEvent event){String current=target==0?edit.name:edit.description;if(event.getType()==UiEvent.Type.POINTER_DOWN)edit.focus=target;else if(event.getType()==UiEvent.Type.TEXT_INPUT&&!Character.isISOControl(event.getTypedChar())&&current.length()<max)current+=event.getTypedChar();else if(event.getType()==UiEvent.Type.KEY_DOWN&&(event.getKeyCode()==UiKeyCode.BACKSPACE||event.getKeyCode()==UiKeyCode.DELETE)&&!current.isEmpty())current=current.substring(0,current.length()-1);if(target==0)edit.name=current;else edit.description=current;invalidation.run();return InputResult.CONSUMED;}}).build();}
    private UiElement choice(String key,final String value,final boolean prerequisite,boolean selected){return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT,value).prop(StandardWidgets.SELECTED,selected).prop(StandardWidgets.ACTION,handler(new Runnable(){public void run(){if(prerequisite)edit.prerequisiteLogic=value;else edit.taskLogic=value;invalidation.run();}})).build();}
    private UiElement prerequisiteEditor(){final QuestManagementVisualModel.Definition summary=model.getDetail().getSummary();UiElement.Builder card=UiElement.type("Card").key("quest-prerequisite-picker")
        .child(label("quest-prerequisite-title","选择已发布的前置任务 · 已选 "+edit.prerequisites.size(),true));int shown=0;
        for(final QuestManagementVisualModel.Definition candidate:model.getDefinitions()){if(candidate.getId().equals(summary.getId())||shown>=8)continue;shown++;final boolean selected=edit.prerequisites.contains(candidate.getId());card.child(button("quest-prerequisite-"+candidate.getId(),(selected?"✓ ":"○ ")+candidate.getName()+" · v"+candidate.getVersion(),new Runnable(){public void run(){if(!edit.prerequisites.remove(candidate.getId()))edit.prerequisites.add(candidate.getId());invalidation.run();}},true));}
        if(shown==0)card.child(UiElement.type("EmptyState").key("quest-prerequisite-empty").prop(StandardWidgets.TEXT,"没有可用的已发布任务").build());
        card.child(UiElement.type("Row").key("quest-prerequisite-actions")
            .child(button("quest-prerequisite-cancel","返回",new Runnable(){public void run(){edit.prerequisites.clear();edit.prerequisites.addAll(edit.originalPrerequisites);edit.prerequisitePicker=false;invalidation.run();}},true))
            .child(button("quest-prerequisite-save","保存前置",new Runnable(){public void run(){actions.savePrerequisites(summary.getId(),summary.getVersion(),summary.getHash(),new java.util.ArrayList<String>(edit.originalPrerequisites),new java.util.ArrayList<String>(edit.prerequisites));edit.prerequisitePicker=false;invalidation.run();}},true)).build());
        return UiElement.type("Column").key("quest-prerequisite-editor").child(card.build()).build();}
    private UiElement behaviorEditor(){final QuestManagementVisualModel.Definition summary=model.getDetail().getSummary();UiElement.Builder card=UiElement.type("Card").key("quest-behavior-picker").child(label("quest-behavior-title","重复与行为选项 · "+(edit.behaviorPage+1)+" / 4",true));
        if(edit.behaviorPage==0){card.child(button("quest-behavior-repeat","重复周期 · "+repeatLabel(edit.option("repeat.cooldownMillis")),new Runnable(){public void run(){String value=edit.option("repeat.cooldownMillis");edit.option("repeat.cooldownMillis","-1".equals(value)?"3600000":"3600000".equals(value)?"86400000":"86400000".equals(value)?"604800000":"-1");invalidation.run();}},true));card.child(optionToggle("quest-behavior-relative","固定周期边界","repeat.relative"));card.child(button("quest-behavior-visibility","可见性 · "+edit.option("behavior.visibility"),new Runnable(){public void run(){String[] values={"NORMAL","HIDDEN","SECRET","UNLOCKED","COMPLETED","CHAIN","ALWAYS"};int index=java.util.Arrays.asList(values).indexOf(edit.option("behavior.visibility"));edit.option("behavior.visibility",values[(index+1+values.length)%values.length]);invalidation.run();}},true));card.child(optionToggle("quest-behavior-main","主线任务","behavior.main"));}
        else if(edit.behaviorPage==1){card.child(optionToggle("quest-behavior-auto","自动领取","behavior.autoClaim"));card.child(optionToggle("quest-behavior-simultaneous","目标须同时满足","behavior.simultaneous"));card.child(optionToggle("quest-behavior-locked","锁定时允许进度","behavior.progressWhileLocked"));card.child(optionToggle("quest-behavior-silent","静默任务","behavior.silent"));}
        else if(edit.behaviorPage==2){card.child(button("quest-behavior-scope","进度主体 · "+edit.option("behavior.participantScope"),new Runnable(){public void run(){String[] values={"PLAYER","PARTY","TEAM","PUBLIC"};int index=java.util.Arrays.asList(values).indexOf(edit.option("behavior.participantScope"));edit.option("behavior.participantScope",values[(index+1+values.length)%values.length]);invalidation.run();}},true));card.child(label("quest-behavior-scope-help","完成时冻结当前主体成员；后来加入者不能补领，离队者保留已生成资格。",false));card.child(optionToggle("quest-behavior-global","BQ 全局兼容标记","behavior.global"));card.child(optionToggle("quest-behavior-share","BQ 共享兼容标记","behavior.globalShare"));}
        else{card.child(optionField("quest-behavior-icon","图标资源","behavior.icon"));card.child(optionField("quest-behavior-update-sound","进度音效","behavior.updateSound"));card.child(optionField("quest-behavior-complete-sound","完成音效","behavior.completeSound"));}
        card.child(UiElement.type("Row").key("quest-behavior-pager").child(button("quest-behavior-prev","‹",new Runnable(){public void run(){edit.behaviorPage=Math.max(0,edit.behaviorPage-1);invalidation.run();}},edit.behaviorPage>0)).child(button("quest-behavior-next","›",new Runnable(){public void run(){edit.behaviorPage=Math.min(3,edit.behaviorPage+1);invalidation.run();}},edit.behaviorPage<3)).build());
        card.child(UiElement.type("Row").key("quest-behavior-actions").child(button("quest-behavior-cancel","取消",new Runnable(){public void run(){edit.options.clear();edit.options.putAll(edit.originalOptions);edit.behaviorPicker=false;invalidation.run();}},true)).child(button("quest-behavior-save","保存选项",new Runnable(){public void run(){actions.saveOptions(summary.getId(),summary.getVersion(),summary.getHash(),new java.util.LinkedHashMap<String,String>(edit.originalOptions),new java.util.LinkedHashMap<String,String>(edit.options));edit.behaviorPicker=false;invalidation.run();}},true)).build());return UiElement.type("Column").key("quest-behavior-editor").child(card.build()).build();}
    private UiElement optionToggle(String key,String label,final String option){return button(key,label+" · "+(Boolean.parseBoolean(edit.option(option))?"是":"否"),new Runnable(){public void run(){edit.option(option,String.valueOf(!Boolean.parseBoolean(edit.option(option))));invalidation.run();}},true);}
    private UiElement optionField(String key,String placeholder,final String option){return UiElement.type("TextField").key(key).prop(StandardWidgets.TEXT,edit.option(option)).prop(StandardWidgets.PLACEHOLDER,placeholder).prop(StandardWidgets.FOCUSED,edit.focus==key.hashCode()).prop(StandardWidgets.ACTION,new UiActionHandler(){public InputResult handle(UiEvent event){String value=edit.option(option);if(event.getType()==UiEvent.Type.POINTER_DOWN)edit.focus=key.hashCode();else if(event.getType()==UiEvent.Type.TEXT_INPUT&&!Character.isISOControl(event.getTypedChar())&&value.length()<256)value+=event.getTypedChar();else if(event.getType()==UiEvent.Type.KEY_DOWN&&(event.getKeyCode()==UiKeyCode.BACKSPACE||event.getKeyCode()==UiKeyCode.DELETE)&&!value.isEmpty())value=value.substring(0,value.length()-1);edit.option(option,value);invalidation.run();return InputResult.CONSUMED;}}).build();}
    private static String repeatLabel(String value){if("-1".equals(value))return "不重复";if("3600000".equals(value))return "1 小时";if("86400000".equals(value))return "1 天";if("604800000".equals(value))return "7 天";return value+" ms";}
    private void beginElementEdit(boolean task,QuestManagementVisualModel.Element value){if(edit==null)edit=new EditState(model.getDetail());java.util.List<QuestManagementVisualModel.Element> values=task?model.getDetail().getTasks():model.getDetail().getRewards();edit.element=new QuestElementEditState(task,value,task?model.getTaskTypes():model.getRewardTypes(),values.indexOf(value),values.size());invalidation.run();}
    private boolean beginNewElement(boolean task){java.util.List<QuestManagementVisualModel.ElementType> types=task?model.getTaskTypes():model.getRewardTypes();if(types.isEmpty())return false;if(edit==null)edit=new EditState(model.getDetail());java.util.List<QuestManagementVisualModel.Element> values=task?model.getDetail().getTasks():model.getDetail().getRewards();edit.element=new QuestElementEditState(task,types,(task?"task":"reward")+(values.size()+1),values.size());invalidation.run();return true;}
    private UiElement elementEditor(){final QuestElementEditState element=edit.element;final QuestManagementVisualModel.Definition summary=model.getDetail().getSummary();UiElement.Builder card=UiElement.type("Card").key("quest-element-editor-card")
        .child(label("quest-element-editor-title",(element.task?"编辑目标":"编辑奖励")+" · "+element.typeId,true))
        .child(elementTextField("quest-element-editor-key","稳定 key","",64,true))
        .child(button("quest-element-editor-type","类型 · "+(element.descriptor==null?element.typeId:element.descriptor.getName()),new Runnable(){public void run(){element.cycleType();invalidation.run();}},element.originalKey.isEmpty()));
        if(element.task)card.child(button("quest-element-editor-optional",element.optional?"可选目标：是":"可选目标：否",new Runnable(){public void run(){element.optional=!element.optional;invalidation.run();}},true));
        if(element.descriptor==null)card.child(UiElement.type("ErrorState").key("quest-element-editor-missing").prop(StandardWidgets.TEXT,"服务器未提供该类型的字段描述，不能安全编辑参数。" ).build());
        else{int start=element.page*4,end=Math.min(start+4,element.descriptor.getFields().size());for(int i=start;i<end;i++)card.child(elementField(element.descriptor.getFields().get(i),i));}
        card.child(UiElement.type("Row").key("quest-element-editor-pager")
            .child(button("quest-element-editor-prev","‹",new Runnable(){public void run(){element.page=Math.max(0,element.page-1);invalidation.run();}},element.page>0))
            .child(label("quest-element-editor-page",(element.page+1)+" / "+element.pages(),false))
            .child(button("quest-element-editor-next","›",new Runnable(){public void run(){element.page=Math.min(element.pages()-1,element.page+1);invalidation.run();}},element.page+1<element.pages())).build())
        .child(UiElement.type("Row").key("quest-element-editor-actions")
            .child(button("quest-element-editor-cancel","返回",new Runnable(){public void run(){edit.element=null;invalidation.run();}},true))
            .child(button("quest-element-editor-save",element.originalKey.isEmpty()?"新增元素":"保存元素",new Runnable(){public void run(){actions.saveElement(summary.getId(),summary.getVersion(),summary.getHash(),element.task,element.originalKey,element.key,element.typeId,element.optional,new java.util.LinkedHashMap<String,String>(element.parameters));}},element.complete())).build());
        if(!element.originalKey.isEmpty())card.child(UiElement.type("Row").key("quest-element-editor-order")
            .child(button("quest-element-editor-up","上移",new Runnable(){public void run(){actions.moveElement(summary.getId(),summary.getVersion(),summary.getHash(),element.task,element.originalKey,element.index-1);}},element.index>0))
            .child(button("quest-element-editor-down","下移",new Runnable(){public void run(){actions.moveElement(summary.getId(),summary.getVersion(),summary.getHash(),element.task,element.originalKey,element.index+1);}},element.index+1<element.total))
            .child(button("quest-element-editor-delete",element.confirmDelete?"确认删除":"删除",new Runnable(){public void run(){if(!element.confirmDelete){element.confirmDelete=true;invalidation.run();}else actions.deleteElement(summary.getId(),summary.getVersion(),summary.getHash(),element.task,element.originalKey);}},true)).build());
        return UiElement.type("Column").key("quest-admin-element-editor").child(card.build()).build();}
    private UiElement elementField(final QuestManagementVisualModel.Field field,final int index){final QuestElementEditState element=edit.element;String label=field.getLabel()+(field.isRequired()?" *":"");if("BOOLEAN".equals(field.getType()))return button("quest-element-field-"+index,label+"："+(Boolean.parseBoolean(element.value(field.getKey()))?"是":"否"),new Runnable(){public void run(){element.value(field.getKey(),String.valueOf(!Boolean.parseBoolean(element.value(field.getKey()))));invalidation.run();}},true);if("ENUM".equals(field.getType()))return button("quest-element-field-"+index,label+"："+element.value(field.getKey()),new Runnable(){public void run(){java.util.List<String> options=field.getOptions();if(options.isEmpty())return;int current=options.indexOf(element.value(field.getKey()));element.value(field.getKey(),options.get((current+1)%options.size()));invalidation.run();}},true);return elementTextField("quest-element-field-"+index,label,field.getKey(),2048,false);}
    private UiElement elementTextField(String key,String placeholder,final String parameter,final int max,final boolean stableKey){final QuestElementEditState element=edit.element;String value=stableKey?element.key:element.value(parameter);return UiElement.type("TextField").key(key).prop(StandardWidgets.TEXT,value).prop(StandardWidgets.PLACEHOLDER,placeholder).prop(StandardWidgets.FOCUSED,element.focus==key.hashCode()).prop(StandardWidgets.ACTION,new UiActionHandler(){public InputResult handle(UiEvent event){String current=stableKey?element.key:element.value(parameter);if(event.getType()==UiEvent.Type.POINTER_DOWN)element.focus=key.hashCode();else if(event.getType()==UiEvent.Type.TEXT_INPUT&&!Character.isISOControl(event.getTypedChar())&&current.length()<max)current+=event.getTypedChar();else if(event.getType()==UiEvent.Type.KEY_DOWN&&(event.getKeyCode()==UiKeyCode.BACKSPACE||event.getKeyCode()==UiKeyCode.DELETE)&&!current.isEmpty())current=current.substring(0,current.length()-1);if(stableKey)element.key=current;else element.value(parameter,current);invalidation.run();return InputResult.CONSUMED;}}).build();}

    private UiElement.Builder elementCard(String key, String title, java.util.List<QuestManagementVisualModel.Element> elements,
        boolean tasks) {
        UiElement.Builder card = UiElement.type("Card").key(key)
            .child(label(key + "-title", title + " · " + elements.size(), true));
        final boolean createTask=tasks;
        if("DRAFT".equals(model.getDetail().getSummary().getLifecycle())&&!(tasks?model.getTaskTypes():model.getRewardTypes()).isEmpty())
            card.child(button(key+"-add","＋ 新增",new Runnable(){public void run(){beginNewElement(createTask);}},true));
        for (int i = 0; i < Math.min(6, elements.size()); i++) {
            QuestManagementVisualModel.Element element = elements.get(i);
            final QuestManagementVisualModel.Element selected=element;final boolean taskElement=tasks;
            if("DRAFT".equals(model.getDetail().getSummary().getLifecycle()))card.child(button(key+"-row-"+i,element.getKey()+" · "+element.getTypeId()+(tasks&&element.isOptional()?" · 可选":""),new Runnable(){public void run(){beginElementEdit(taskElement,selected);}},true));
            else card.child(label(key + "-row-" + i, element.getKey() + " · " + element.getTypeId()
                + (tasks && element.isOptional() ? " · 可选" : ""), false));
        }
        return card;
    }

    @Override public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
        width = viewport.getWidth();
        height = viewport.getHeight();
        boolean compact = TerminalVisualMetrics.compute(viewport, profile).getBounds().getHeight() <= 220;
        LayoutSpec body = create!=null?creatorLayout(compact):model.getDetail() == null ? listLayout(compact) : edit==null?detailLayout(compact):edit.element!=null?elementEditorLayout(compact):edit.prerequisitePicker?prerequisiteEditorLayout(compact):edit.behaviorPicker?behaviorEditorLayout(compact):editorLayout(compact);
        if (batchConfirmOpen) body = LayoutSpec.of("quest-admin-stack", LayoutKind.STACK).flex(1)
            .child(body).child(LayoutSpec.of("quest-admin-batch-confirm", LayoutKind.LEAF).build()).build();
        return TerminalVisualShell.layout(viewport, model.getShell(), body, profile);
    }

    private LayoutSpec listLayout(boolean compact) {
        LayoutSpec.Builder toolsBuilder = LayoutSpec.of("quest-admin-tools", LayoutKind.ROW).preferred(0, compact ? 17 : 21).gap(2)
            .child(LayoutSpec.of("quest-admin-back", LayoutKind.LEAF).preferred(compact ? 52 : 64, 0).build())
            .child(LayoutSpec.of("quest-admin-chapters",LayoutKind.LEAF).preferred(compact?34:42,0).build())
            .child(LayoutSpec.of("quest-admin-create",LayoutKind.LEAF).preferred(compact?38:46,0).build())
            .child(LayoutSpec.of("quest-admin-batch-mode",LayoutKind.LEAF).preferred(compact?34:42,0).build())
            .child(LayoutSpec.of("quest-admin-batch-retire",LayoutKind.LEAF).preferred(compact?34:42,0).build())
            .child(LayoutSpec.of("quest-admin-query", LayoutKind.LEAF).flex(1).build());
        toolsBuilder.child(LayoutSpec.of("quest-admin-all", LayoutKind.LEAF).preferred(compact ? 14 : 34, 0).build())
            .child(LayoutSpec.of("quest-admin-draft", LayoutKind.LEAF).preferred(compact ? 14 : 36, 0).build())
            .child(LayoutSpec.of("quest-admin-published", LayoutKind.LEAF).preferred(compact ? 14 : 42, 0).build())
            .child(LayoutSpec.of("quest-admin-retired", LayoutKind.LEAF).preferred(compact ? 14 : 42, 0).build());
        LayoutSpec tools = toolsBuilder.build();
        LayoutSpec.Builder list = LayoutSpec.of("quest-admin-list", LayoutKind.COLUMN).flex(1)
            .padding(new Insets(3, 3, 3, 3)).gap(2)
            .child(LayoutSpec.of("quest-admin-title", LayoutKind.LEAF).preferred(0, 12).build());
        if (model.getDefinitions().isEmpty()) list.child(LayoutSpec.of("quest-admin-empty", LayoutKind.LEAF).flex(1).build());
        else for (int i = 0; i < model.getDefinitions().size(); i++) {
            QuestManagementVisualModel.Definition definition = model.getDefinitions().get(i);
            list.child(LayoutSpec.of(rowKey(definition), LayoutKind.ROW).preferred(0, compact ? 16 : 19).gap(3)
                .child(LayoutSpec.of("quest-admin-name-" + i, LayoutKind.LEAF).flex(2).build())
                .child(LayoutSpec.of("quest-admin-version-" + i, LayoutKind.LEAF).preferred(24, 0).build())
                .child(LayoutSpec.of("quest-admin-life-" + i, LayoutKind.LEAF).preferred(48, 0).build())
                .child(LayoutSpec.of("quest-admin-count-" + i, LayoutKind.LEAF).flex(1).build()).build());
        }
        list.child(LayoutSpec.of("quest-admin-pager", LayoutKind.ROW).preferred(0, compact ? 15 : 18).gap(2)
            .child(LayoutSpec.of("quest-admin-prev", LayoutKind.LEAF).preferred(18, 0).build())
            .child(LayoutSpec.of("quest-admin-page", LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-admin-next", LayoutKind.LEAF).preferred(18, 0).build()).build());
        LayoutSpec.Builder root=LayoutSpec.of("quest-admin", LayoutKind.COLUMN).flex(1).padding(new Insets(3, 3, 3, 3)).gap(3).child(tools);
        if(!model.getMigrations().isEmpty()){root.child(LayoutSpec.of("quest-migration-latest",LayoutKind.LEAF).preferred(0,compact?10:13).build());if(!firstDiagnostic(model.getMigrations().get(0).getDiagnostic()).isEmpty())root.child(LayoutSpec.of("quest-migration-diagnostic",LayoutKind.LEAF).preferred(0,compact?10:13).build());}
        return root.child(list.build()).build();
    }

    private LayoutSpec creatorLayout(boolean compact){return LayoutSpec.of("quest-admin-creator",LayoutKind.COLUMN).flex(1).padding(new Insets(3,3,3,3)).child(LayoutSpec.of("quest-admin-create-card",LayoutKind.COLUMN).flex(1).padding(new Insets(4,4,4,4)).gap(3)
        .child(LayoutSpec.of("quest-admin-create-title",LayoutKind.LEAF).preferred(0,14).build()).child(LayoutSpec.of("quest-admin-create-name",LayoutKind.LEAF).preferred(0,compact?18:22).build())
        .child(LayoutSpec.of("quest-admin-template-empty",LayoutKind.LEAF).preferred(0,compact?18:22).build()).child(LayoutSpec.of("quest-admin-template-manual",LayoutKind.LEAF).preferred(0,compact?18:22).build()).child(LayoutSpec.of("quest-admin-template-xp",LayoutKind.LEAF).preferred(0,compact?18:22).build())
        .child(LayoutSpec.of("quest-admin-create-selected",LayoutKind.LEAF).preferred(0,14).build()).child(LayoutSpec.of("quest-admin-create-actions",LayoutKind.ROW).preferred(0,compact?18:22).gap(3).child(LayoutSpec.of("quest-admin-create-cancel",LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of("quest-admin-create-submit",LayoutKind.LEAF).flex(1).build()).build()).build()).build();}

    private LayoutSpec detailLayout(boolean compact) {
        QuestManagementVisualModel.Detail detail = model.getDetail();
        QuestManagementVisualModel.Definition summary = detail.getSummary();
        LayoutSpec.Builder actionsRow=LayoutSpec.of("quest-admin-detail-actions",LayoutKind.ROW).preferred(0,18).gap(3)
            .child(LayoutSpec.of("quest-admin-detail-back", LayoutKind.LEAF).preferred(compact ? 52 : 64, 0).build())
            .child(LayoutSpec.of("quest-admin-detail-name", LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-admin-detail-life", LayoutKind.LEAF).preferred(48, 0).build());
        if ("DRAFT".equals(summary.getLifecycle())) actionsRow.child(LayoutSpec.of("quest-admin-detail-publish", LayoutKind.LEAF).preferred(38, 0).build());
        if ("DRAFT".equals(summary.getLifecycle())) actionsRow.child(LayoutSpec.of("quest-admin-detail-edit", LayoutKind.LEAF).preferred(38, 0).build());
        if ("PUBLISHED".equals(summary.getLifecycle())) actionsRow.child(LayoutSpec.of("quest-admin-detail-retire", LayoutKind.LEAF).preferred(38, 0).build());
        LayoutSpec.Builder heading = LayoutSpec.of("quest-admin-detail-heading", LayoutKind.COLUMN)
            .preferred(0, compact ? 42 : 50).padding(new Insets(3, 3, 3, 3)).gap(3)
            .child(actionsRow.build()).child(LayoutSpec.of("quest-admin-detail-meta",LayoutKind.LEAF).flex(1).build());
        LayoutSpec tasks = elementLayout("quest-admin-detail-tasks", detail.getTasks(), compact);
        LayoutSpec rewards = elementLayout("quest-admin-detail-rewards", detail.getRewards(), compact);
        return LayoutSpec.of("quest-admin-detail", LayoutKind.COLUMN).flex(1).padding(new Insets(3, 3, 3, 3)).gap(3)
            .child(heading.build()).child(LayoutSpec.of("quest-admin-detail-impact",LayoutKind.COLUMN)
                .preferred(0,compact?32:42).padding(new Insets(3,3,3,3)).gap(2)
                .child(LayoutSpec.of("quest-admin-detail-impact-title",LayoutKind.LEAF).preferred(0,12).build())
                .child(LayoutSpec.of("quest-admin-detail-impact-counts",LayoutKind.LEAF).preferred(0,12).build())
                .child(LayoutSpec.of("quest-admin-detail-impact-summary",LayoutKind.LEAF).flex(1).build())
                .child(LayoutSpec.of("quest-admin-detail-impact-truncated",LayoutKind.LEAF).flex(1).build()).build())
            .child(LayoutSpec.of("quest-admin-detail-columns", LayoutKind.ROW).flex(1).gap(3)
                .child(tasks).child(rewards).build()).build();
    }

    private LayoutSpec editorLayout(boolean compact){return LayoutSpec.of("quest-admin-editor",LayoutKind.COLUMN).flex(1).padding(new Insets(3,3,3,3))
        .child(LayoutSpec.of("quest-admin-editor-card",LayoutKind.COLUMN).flex(1).padding(new Insets(4,4,4,4)).gap(3)
            .child(LayoutSpec.of("quest-admin-editor-title",LayoutKind.LEAF).preferred(0,14).build())
            .child(LayoutSpec.of("quest-admin-editor-name",LayoutKind.LEAF).preferred(0,compact?18:22).build())
            .child(LayoutSpec.of("quest-admin-editor-description",LayoutKind.LEAF).preferred(0,compact?22:30).build())
            .child(choiceLayout("quest-admin-editor-prerequisite","quest-admin-editor-prerequisite-label","quest-admin-editor-prerequisite-and","quest-admin-editor-prerequisite-or",compact))
            .child(choiceLayout("quest-admin-editor-task","quest-admin-editor-task-label","quest-admin-editor-task-and","quest-admin-editor-task-or",compact))
            .child(LayoutSpec.of("quest-admin-editor-prerequisites",LayoutKind.LEAF).preferred(0,compact?18:22).build())
            .child(LayoutSpec.of("quest-admin-editor-behavior",LayoutKind.LEAF).preferred(0,compact?18:22).build())
            .child(LayoutSpec.of("quest-admin-editor-actions",LayoutKind.ROW).preferred(0,compact?18:22).gap(3)
                .child(LayoutSpec.of("quest-admin-editor-cancel",LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of("quest-admin-editor-save",LayoutKind.LEAF).flex(1).build()).build()).build()).build();}
    private static LayoutSpec choiceLayout(String row,String label,String and,String or,boolean compact){return LayoutSpec.of(row,LayoutKind.ROW).preferred(0,compact?18:22).gap(3).child(LayoutSpec.of(label,LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of(and,LayoutKind.LEAF).preferred(45,0).build()).child(LayoutSpec.of(or,LayoutKind.LEAF).preferred(45,0).build()).build();}
    private LayoutSpec prerequisiteEditorLayout(boolean compact){LayoutSpec.Builder card=LayoutSpec.of("quest-prerequisite-picker",LayoutKind.COLUMN).flex(1).padding(new Insets(4,4,4,4)).gap(2).child(LayoutSpec.of("quest-prerequisite-title",LayoutKind.LEAF).preferred(0,14).build());int shown=0;QuestManagementVisualModel.Definition summary=model.getDetail().getSummary();for(QuestManagementVisualModel.Definition candidate:model.getDefinitions()){if(candidate.getId().equals(summary.getId())||shown>=8)continue;shown++;card.child(LayoutSpec.of("quest-prerequisite-"+candidate.getId(),LayoutKind.LEAF).preferred(0,compact?15:19).build());}if(shown==0)card.child(LayoutSpec.of("quest-prerequisite-empty",LayoutKind.LEAF).flex(1).build());card.child(LayoutSpec.of("quest-prerequisite-actions",LayoutKind.ROW).preferred(0,compact?17:21).gap(3).child(LayoutSpec.of("quest-prerequisite-cancel",LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of("quest-prerequisite-save",LayoutKind.LEAF).flex(1).build()).build());return LayoutSpec.of("quest-prerequisite-editor",LayoutKind.COLUMN).flex(1).padding(new Insets(3,3,3,3)).child(card.build()).build();}
    private LayoutSpec behaviorEditorLayout(boolean compact){LayoutSpec.Builder card=LayoutSpec.of("quest-behavior-picker",LayoutKind.COLUMN).flex(1).padding(new Insets(4,4,4,4)).gap(2).child(LayoutSpec.of("quest-behavior-title",LayoutKind.LEAF).preferred(0,14).build());String[] keys=edit.behaviorPage==0?new String[]{"repeat","relative","visibility","main"}:edit.behaviorPage==1?new String[]{"auto","simultaneous","locked","silent"}:new String[]{"global","share","icon","update-sound","complete-sound"};for(String key:keys)card.child(LayoutSpec.of("quest-behavior-"+key,LayoutKind.LEAF).preferred(0,compact?16:20).build());card.child(LayoutSpec.of("quest-behavior-pager",LayoutKind.ROW).preferred(0,compact?16:20).gap(3).child(LayoutSpec.of("quest-behavior-prev",LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of("quest-behavior-next",LayoutKind.LEAF).flex(1).build()).build()).child(LayoutSpec.of("quest-behavior-actions",LayoutKind.ROW).preferred(0,compact?17:21).gap(3).child(LayoutSpec.of("quest-behavior-cancel",LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of("quest-behavior-save",LayoutKind.LEAF).flex(1).build()).build());return LayoutSpec.of("quest-behavior-editor",LayoutKind.COLUMN).flex(1).padding(new Insets(3,3,3,3)).child(card.build()).build();}
    private LayoutSpec elementEditorLayout(boolean compact){QuestElementEditState element=edit.element;LayoutSpec.Builder card=LayoutSpec.of("quest-element-editor-card",LayoutKind.COLUMN).flex(1).padding(new Insets(4,4,4,4)).gap(2)
        .child(LayoutSpec.of("quest-element-editor-title",LayoutKind.LEAF).preferred(0,12).build()).child(LayoutSpec.of("quest-element-editor-key",LayoutKind.LEAF).preferred(0,compact?17:20).build()).child(LayoutSpec.of("quest-element-editor-type",LayoutKind.LEAF).preferred(0,12).build());
        if(element.task)card.child(LayoutSpec.of("quest-element-editor-optional",LayoutKind.LEAF).preferred(0,compact?17:20).build());
        if(element.descriptor==null)card.child(LayoutSpec.of("quest-element-editor-missing",LayoutKind.LEAF).flex(1).build());else{int start=element.page*4,end=Math.min(start+4,element.descriptor.getFields().size());for(int i=start;i<end;i++)card.child(LayoutSpec.of("quest-element-field-"+i,LayoutKind.LEAF).preferred(0,compact?17:20).build());}
        card.child(LayoutSpec.of("quest-element-editor-pager",LayoutKind.ROW).preferred(0,compact?16:19).gap(2).child(LayoutSpec.of("quest-element-editor-prev",LayoutKind.LEAF).preferred(20,0).build()).child(LayoutSpec.of("quest-element-editor-page",LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of("quest-element-editor-next",LayoutKind.LEAF).preferred(20,0).build()).build())
            .child(LayoutSpec.of("quest-element-editor-actions",LayoutKind.ROW).preferred(0,compact?17:20).gap(3).child(LayoutSpec.of("quest-element-editor-cancel",LayoutKind.LEAF).flex(1).build()).child(LayoutSpec.of("quest-element-editor-save",LayoutKind.LEAF).flex(1).build()).build());
        if(!element.originalKey.isEmpty())card.child(LayoutSpec.of("quest-element-editor-order",LayoutKind.ROW).preferred(0,compact?17:20).gap(3)
            .child(LayoutSpec.of("quest-element-editor-up",LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-element-editor-down",LayoutKind.LEAF).flex(1).build())
            .child(LayoutSpec.of("quest-element-editor-delete",LayoutKind.LEAF).flex(1).build()).build());
        return LayoutSpec.of("quest-admin-element-editor",LayoutKind.COLUMN).flex(1).padding(new Insets(3,3,3,3)).child(card.build()).build();}

    private static final class EditState{private final String id,hash;private String name,description,prerequisiteLogic,taskLogic;private int focus=-1,behaviorPage;private QuestElementEditState element;private boolean prerequisitePicker,behaviorPicker;private final java.util.LinkedHashSet<String> originalPrerequisites,prerequisites;private final java.util.LinkedHashMap<String,String> originalOptions,options;private EditState(QuestManagementVisualModel.Detail value){QuestManagementVisualModel.Definition summary=value.getSummary();id=summary.getId();hash=summary.getHash();name=summary.getName();description=value.getDescription();prerequisiteLogic=value.getPrerequisiteLogic();taskLogic=value.getTaskLogic();originalPrerequisites=new java.util.LinkedHashSet<String>(value.getPrerequisites());prerequisites=new java.util.LinkedHashSet<String>(value.getPrerequisites());originalOptions=new java.util.LinkedHashMap<String,String>(value.getOptions());options=new java.util.LinkedHashMap<String,String>(value.getOptions());}private String option(String key){String value=options.get(key);return value==null?defaultOption(key):value;}private void option(String key,String value){options.put(key,value);}private static String defaultOption(String key){if("repeat.cooldownMillis".equals(key))return "-1";if("behavior.visibility".equals(key))return "NORMAL";if("repeat.relative".equals(key))return "true";return "false";}}
    private static final class CreateState{private String name="",template="EMPTY";private boolean focused;}

    private LayoutSpec elementLayout(String key, java.util.List<QuestManagementVisualModel.Element> elements,
        boolean compact) {
        LayoutSpec.Builder card = LayoutSpec.of(key, LayoutKind.COLUMN).flex(1).padding(new Insets(3, 3, 3, 3)).gap(2)
            .child(LayoutSpec.of(key + "-title", LayoutKind.LEAF).preferred(0, 12).build());
        boolean tasks=key.endsWith("tasks");
        if("DRAFT".equals(model.getDetail().getSummary().getLifecycle())&&!(tasks?model.getTaskTypes():model.getRewardTypes()).isEmpty())
            card.child(LayoutSpec.of(key+"-add",LayoutKind.LEAF).preferred(0,compact?15:18).build());
        for (int i = 0; i < Math.min(6, elements.size()); i++)
            card.child(LayoutSpec.of(key + "-row-" + i, LayoutKind.LEAF).preferred(0, compact ? 15 : 18).build());
        return card.build();
    }

    private UiElement filter(String key, String text, final String value) {
        return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT, text)
            .prop(StandardWidgets.SELECTED, value.equals(model.getLifecycle()))
            .prop(StandardWidgets.ACTION, handler(new Runnable() { public void run() { actions.filter(value); } })).build();
    }
    private static UiElement label(String key, String text, boolean bold) { return UiElement.type("Label").key(key)
        .prop(StandardWidgets.TEXT, text).prop(StandardWidgets.TEXT_ROLE, "caption").prop(StandardWidgets.MAX_LINES, 1)
        .prop(StandardWidgets.BOLD, bold).build(); }
    private static UiElement button(String key, String text, final Runnable run, boolean enabled) { return UiElement.type("Button")
        .key(key).prop(StandardWidgets.TEXT, text).prop(StandardWidgets.ENABLED, enabled)
        .prop(StandardWidgets.ACTION, handler(run)).build(); }
    private static UiActionHandler handler(final Runnable run) { return new UiActionHandler() {
        public InputResult handle(UiEvent event) { run.run(); return InputResult.CONSUMED; }
    }; }
    private static String rowKey(QuestManagementVisualModel.Definition value) {
        return "quest-admin-row-" + value.getId() + "-" + value.getVersion();
    }
}

package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative command facade for the quest definition editing lifecycle. */
public final class QuestDraftManagementService {
    private final QuestDefinitionRepository definitions;
    private final QuestTransaction transaction;
    private final QuestEditorAuthorization authorization;
    private final QuestDefinitionValidator validator;
    private final QuestEditorTypeRegistry types;
    private final QuestDefinitionGraphValidator graphValidator;
    private final QuestDefinitionBatchCloner cloner;

    public QuestDraftManagementService(QuestDefinitionRepository definitions, QuestTransaction transaction,
        QuestEditorAuthorization authorization, QuestEditorTypeRegistry types) {
        this(definitions, transaction, authorization, types, new QuestDefinitionBatchCloner.IdSource() {
            @Override public UUID next() { return UUID.randomUUID(); }
        });
    }

    QuestDraftManagementService(QuestDefinitionRepository definitions, QuestTransaction transaction,
        QuestEditorAuthorization authorization, QuestEditorTypeRegistry types,
        QuestDefinitionBatchCloner.IdSource cloneIds) {
        if (definitions == null || transaction == null || authorization == null || types == null) {
            throw new IllegalArgumentException("dependencies are required");
        }
        this.definitions = definitions;
        this.transaction = transaction;
        this.authorization = authorization;
        this.types = types;
        this.validator = new QuestDefinitionValidator();
        this.graphValidator = new QuestDefinitionGraphValidator();
        this.cloner = new QuestDefinitionBatchCloner(cloneIds);
    }

    public QuestDraftManagementResult createDraft(QuestEditorActor actor, final QuestDefinition definition) {
        if (!allowed(actor)) return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.FORBIDDEN);
        if (definition == null) return invalidField("definition", "required", "Definition is required");
        List<QuestEditorValidationIssue> issues = validator.validate(definition, types);
        if (!issues.isEmpty()) return QuestDraftManagementResult.invalid(issues);
        try {
            return QuestDraftManagementResult.success(transaction.inTransaction(
                new QuestTransaction.Work<StoredQuestDefinition>() {
                    @Override public StoredQuestDefinition execute() { return definitions.createDraft(definition); }
                }));
        } catch (QuestDefinitionConflictException conflict) {
            return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.CONFLICT);
        }
    }

    public QuestDraftManagementResult createFromTemplate(QuestEditorActor actor,QuestDraftTemplate template,String name){
        if(!allowed(actor))return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.FORBIDDEN);
        if(template==null)return invalidField("template","required","Template is required");
        if(blank(name)||name.trim().length()>256)return invalidField("name","invalid","Name must contain 1 to 256 characters");
        return createDraft(actor,template.create(UUID.randomUUID(),name.trim()));
    }

    public QuestDefinitionCloneResult cloneDrafts(QuestEditorActor actor, QuestDefinitionCloneRequest request) {
        if (!allowed(actor)) return QuestDefinitionCloneResult.status(QuestDefinitionCloneResult.Status.FORBIDDEN);
        if (request == null) return QuestDefinitionCloneResult.status(QuestDefinitionCloneResult.Status.INVALID);
        List<QuestDefinition> sources = new java.util.ArrayList<QuestDefinition>();
        for (QuestDefinitionCloneRequest.Source source : request.getSources()) {
            Optional<StoredQuestDefinition> stored = definitions.find(source.getQuestId(), source.getVersion());
            if (!stored.isPresent()) return QuestDefinitionCloneResult.status(QuestDefinitionCloneResult.Status.NOT_FOUND);
            sources.add(stored.get().getDefinition());
        }
        final QuestDefinitionBatchCloner.Result plan;
        try {
            plan = cloner.clone(sources);
            for (QuestDefinition definition : plan.getDefinitions()) {
                if (!validator.validate(definition, types).isEmpty()) {
                    return QuestDefinitionCloneResult.status(QuestDefinitionCloneResult.Status.INVALID);
                }
            }
        } catch (IllegalArgumentException invalid) {
            return QuestDefinitionCloneResult.status(QuestDefinitionCloneResult.Status.INVALID);
        }
        try {
            List<StoredQuestDefinition> created = transaction.inTransaction(
                new QuestTransaction.Work<List<StoredQuestDefinition>>() {
                    @Override public List<StoredQuestDefinition> execute() {
                        List<StoredQuestDefinition> result = new java.util.ArrayList<StoredQuestDefinition>();
                        for (QuestDefinition definition : plan.getDefinitions()) result.add(definitions.createDraft(definition));
                        return result;
                    }
                });
            return QuestDefinitionCloneResult.success(plan.getRemappedIds(), created);
        } catch (QuestDefinitionConflictException conflict) {
            return QuestDefinitionCloneResult.status(QuestDefinitionCloneResult.Status.CONFLICT);
        }
    }

    public QuestDraftManagementResult updateDraft(QuestEditorActor actor, final QuestDefinition definition,
        final String expectedContentHash) {
        if (!allowed(actor)) return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.FORBIDDEN);
        if (definition == null) return invalidField("definition", "required", "Definition is required");
        if (blank(expectedContentHash)) return invalidField("contentHash", "required", "Expected content hash is required");
        List<QuestEditorValidationIssue> issues = validator.validate(definition, types);
        if (!issues.isEmpty()) return QuestDraftManagementResult.invalid(issues);
        try {
            return QuestDraftManagementResult.success(transaction.inTransaction(
                new QuestTransaction.Work<StoredQuestDefinition>() {
                    @Override public StoredQuestDefinition execute() {
                        return definitions.updateDraft(definition, expectedContentHash);
                    }
                }));
        } catch (QuestDefinitionConflictException conflict) {
            return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.CONFLICT);
        }
    }

    public QuestDraftManagementResult applyEdit(QuestEditorActor actor,QuestDraftEditRequest request){
        if(!allowed(actor))return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.FORBIDDEN);
        if(request==null)return invalidField("edit","required","Edit request is required");
        Optional<StoredQuestDefinition> stored=definitions.find(request.getQuestId(),request.getVersion());
        if(!stored.isPresent())return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.NOT_FOUND);
        if(stored.get().getLifecycle()!=QuestDefinitionLifecycle.DRAFT)return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.CONFLICT);
        QuestDraftEditor editor=QuestDraftEditor.edit(stored.get().getDefinition());
        try{for(QuestDraftEditOperation operation:request.getOperations())apply(editor,operation);}
        catch(IllegalArgumentException invalid){return invalidField("edit","invalid_operation",invalid.getMessage());}
        catch(IndexOutOfBoundsException invalid){return invalidField("edit","invalid_operation",invalid.getMessage());}
        return updateDraft(actor,editor.build(),request.getExpectedContentHash());
    }

    private static void apply(QuestDraftEditor editor,QuestDraftEditOperation operation){
        switch(operation.getKind()){
            case SET_NAME:editor.name(operation.getText());break;
            case SET_DESCRIPTION:editor.description(operation.getText());break;
            case SET_PREREQUISITE_LOGIC:editor.prerequisiteLogic(QuestLogic.valueOf(operation.getText()));break;
            case SET_TASK_LOGIC:editor.taskLogic(QuestLogic.valueOf(operation.getText()));break;
            case SET_OPTION:applyOption(editor,operation.getKey(),operation.getText());break;
            case ADD_PREREQUISITE:editor.addPrerequisite(operation.getQuestId());break;
            case REMOVE_PREREQUISITE:editor.removePrerequisite(operation.getQuestId());break;
            case ADD_TASK:editor.addTask(operation.getTask());break;
            case REPLACE_TASK:editor.replaceTask(operation.getKey(),operation.getTask());break;
            case REMOVE_TASK:editor.removeTask(operation.getKey());break;
            case MOVE_TASK:editor.moveTask(operation.getKey(),operation.getIndex());break;
            case ADD_REWARD:editor.addReward(operation.getReward());break;
            case REPLACE_REWARD:editor.replaceReward(operation.getKey(),operation.getReward());break;
            case REMOVE_REWARD:editor.removeReward(operation.getKey());break;
            case MOVE_REWARD:editor.moveReward(operation.getKey(),operation.getIndex());break;
            default:throw new IllegalArgumentException("unsupported edit operation");
        }
    }

    private static void applyOption(QuestDraftEditor editor,String key,String value){
        if("repeat.cooldownMillis".equals(key)){long cooldown=Long.parseLong(value);if(cooldown< -1L)throw new IllegalArgumentException("repeat cooldown must be -1 or non-negative");editor.repeatPolicy(cooldown<0?RepeatPolicy.never():RepeatPolicy.after(cooldown,editor.getRepeatPolicy().isRelative()));return;}
        if("repeat.relative".equals(key)){boolean relative=strictBoolean(value);RepeatPolicy current=editor.getRepeatPolicy();editor.repeatPolicy(current.isRepeatable()?RepeatPolicy.after(current.getCooldownMillis(),relative):RepeatPolicy.never());return;}
        QuestBehavior current=editor.getBehavior();QuestBehavior.Builder builder=current.toBuilder();
        if("behavior.visibility".equals(key))builder.visibility(QuestVisibility.valueOf(value));
        else if("behavior.icon".equals(key))builder.iconReference(boundedOption(value));
        else if("behavior.main".equals(key))builder.main(strictBoolean(value));
        else if("behavior.silent".equals(key))builder.silent(strictBoolean(value));
        else if("behavior.autoClaim".equals(key))builder.autoClaim(strictBoolean(value));
        else if("behavior.progressWhileLocked".equals(key))builder.progressWhileLocked(strictBoolean(value));
        else if("behavior.simultaneous".equals(key))builder.simultaneous(strictBoolean(value));
        else if("behavior.global".equals(key))builder.global(strictBoolean(value));
        else if("behavior.globalShare".equals(key))builder.globalShare(strictBoolean(value));
        else if("behavior.updateSound".equals(key))builder.updateSound(boundedOption(value));
        else if("behavior.completeSound".equals(key))builder.completeSound(boundedOption(value));
        else throw new IllegalArgumentException("unknown quest option: "+key);
        editor.behavior(builder.build());
    }

    private static boolean strictBoolean(String value){if("true".equalsIgnoreCase(value))return true;if("false".equalsIgnoreCase(value))return false;throw new IllegalArgumentException("boolean option must be true or false");}
    private static String boundedOption(String value){String result=value==null?"":value.trim();if(result.length()>256)throw new IllegalArgumentException("quest option exceeds 256 characters");return result;}

    public QuestDraftManagementResult publish(QuestEditorActor actor, final UUID questId, final int version,
        final String expectedContentHash, final long publishedAt) {
        if (!allowed(actor)) return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.FORBIDDEN);
        if (questId == null || version < 1 || blank(expectedContentHash) || publishedAt < 0L) {
            return invalidField("publish", "invalid", "Quest, version, content hash and publish time are required");
        }
        Optional<StoredQuestDefinition> candidate = definitions.find(questId, version);
        if (!candidate.isPresent()) return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.NOT_FOUND);
        List<QuestEditorValidationIssue> issues = validator.validate(candidate.get().getDefinition(), types);
        issues.addAll(graphValidator.validateForPublish(candidate.get().getDefinition(), definitions));
        if (!issues.isEmpty()) return QuestDraftManagementResult.invalid(issues);
        Boolean published = transaction.inTransaction(new QuestTransaction.Work<Boolean>() {
            @Override public Boolean execute() {
                return Boolean.valueOf(definitions.publish(questId, version, expectedContentHash, publishedAt));
            }
        });
        if (!published.booleanValue()) return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.CONFLICT);
        Optional<StoredQuestDefinition> stored = definitions.find(questId, version);
        return stored.isPresent() ? QuestDraftManagementResult.success(stored.get())
            : QuestDraftManagementResult.status(QuestDraftManagementResult.Status.NOT_FOUND);
    }

    public QuestDraftManagementResult retire(QuestEditorActor actor, final UUID questId, final int version) {
        if (!allowed(actor)) return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.FORBIDDEN);
        if (questId == null || version < 1) {
            return invalidField("retire", "invalid", "Quest and version are required");
        }
        Optional<StoredQuestDefinition> candidate = definitions.find(questId, version);
        if (!candidate.isPresent()) return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.NOT_FOUND);
        Boolean retired = transaction.inTransaction(new QuestTransaction.Work<Boolean>() {
            @Override public Boolean execute() { return Boolean.valueOf(definitions.retire(questId, version)); }
        });
        if (!retired.booleanValue()) return QuestDraftManagementResult.status(QuestDraftManagementResult.Status.CONFLICT);
        Optional<StoredQuestDefinition> stored = definitions.find(questId, version);
        return stored.isPresent() ? QuestDraftManagementResult.success(stored.get())
            : QuestDraftManagementResult.status(QuestDraftManagementResult.Status.NOT_FOUND);
    }

    public List<QuestElementTypeDescriptor> listTypes(QuestElementKind kind){return types.list(kind);}

    private boolean allowed(QuestEditorActor actor) {
        return actor != null && authorization.canManageDefinitions(actor);
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }

    private static QuestDraftManagementResult invalidField(String field, String code, String message) {
        return QuestDraftManagementResult.invalid(java.util.Collections.singletonList(
            new QuestEditorValidationIssue(field, code, message)));
    }
}

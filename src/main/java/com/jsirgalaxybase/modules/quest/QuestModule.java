package com.jsirgalaxybase.modules.quest;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.quest.core.QuestCenterQuery;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestClaimService;
import com.jsirgalaxybase.quest.core.AuthenticatedRewardChoiceService;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestTrackingService;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestConsumptionService;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestConsumptionDetectService;
import com.jsirgalaxybase.quest.core.AuthenticatedCraftingStatisticsDetectionService;
import com.jsirgalaxybase.quest.core.ConsumptionRequestAuthorizer;
import com.jsirgalaxybase.quest.core.ConsumptionSubmissionService;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestDefinitionManagementQuery;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestChapterManagementQuery;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestChapterDependencyQuery;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestDefinitionImpactQuery;
import com.jsirgalaxybase.quest.core.QuestChapterManagementService;
import com.jsirgalaxybase.quest.core.QuestChapterCloneService;
import com.jsirgalaxybase.quest.core.QuestChapterOrderingService;
import com.jsirgalaxybase.quest.core.QuestChapterAlignmentService;
import com.jsirgalaxybase.quest.core.QuestDraftManagementService;
import com.jsirgalaxybase.quest.core.QuestEditorAuthorization;
import com.jsirgalaxybase.quest.core.StandardQuestEditorTypeRegistry;
import com.jsirgalaxybase.quest.core.QuestRuntimeService;
import com.jsirgalaxybase.quest.core.QuestFactProjector;
import com.jsirgalaxybase.quest.core.QuestEngine;
import com.jsirgalaxybase.quest.core.RewardSuppressionPolicy;
import com.jsirgalaxybase.quest.core.QuestEvaluationPlanner;
import com.jsirgalaxybase.quest.core.RevisionAwarePublishedQuestCatalog;
import com.jsirgalaxybase.quest.core.QuestClock;
import com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorRegistry;
import com.jsirgalaxybase.quest.core.ScopedQuestAssignmentPolicy;
import com.jsirgalaxybase.quest.core.QuestObservationPlan;
import com.jsirgalaxybase.quest.core.QuestObservationPlanProvider;
import com.jsirgalaxybase.quest.core.QuestRuntimeTypeExtension;
import com.jsirgalaxybase.quest.core.QuestRuntimeTypeRegistry;
import com.jsirgalaxybase.quest.core.BqHuntPartyAssignmentPolicy;
import com.jsirgalaxybase.quest.core.BqPartyCraftingStatisticsMembers;
import com.jsirgalaxybase.quest.postgres.JdbcQuestCenterQuery;
import com.jsirgalaxybase.quest.postgres.JdbcQuestConnectionManager;
import com.jsirgalaxybase.quest.postgres.JdbcQuestDefinitionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestChapterRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestRuntimeRepository;
import com.jsirgalaxybase.quest.postgres.JdbcParticipantMembershipResolver;
import com.jsirgalaxybase.quest.postgres.JdbcBqPartyInventorySnapshotRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestTransaction;
import com.jsirgalaxybase.quest.postgres.JdbcRewardClaimRepository;
import com.jsirgalaxybase.quest.postgres.JdbcRewardChoiceSelectionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestTrackingRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestCompletionQuery;
import com.jsirgalaxybase.quest.postgres.JdbcConsumptionSubmissionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcConsumptionDetectBatchRepository;
import com.jsirgalaxybase.quest.postgres.JdbcRewardDeliveryRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestCompletionRewardPort;
import com.jsirgalaxybase.terminal.TerminalService;
import com.jsirgalaxybase.modules.quest.application.TrackedQuestSnapshotFactory;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.TrackedQuestSyncController;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftQuestEditorAuthorization;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.QuestGameplayEventHandler;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.QuestConsumptionRecoveryHandler;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.RuntimeQuestFactSink;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.BqPartyInventorySnapshotFactSink;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftQuestFactFactory;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftItemRewardDeliveryHandler;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftXpRewardDeliveryHandler;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftScoreboardRewardDeliveryHandler;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftCommandRewardExecutor;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftQuestOnlinePlayerDirectory;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.OnlineServerQuestPlayerResolver;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.QuestPlayerResolver;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.QuestRewardDeliveryController;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.WorldSaveQuestPlayerDataFlusher;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.server.MinecraftServer;

/** Disabled-by-default server composition root for the PostgreSQL quest read model. */
public class QuestModule extends ModModule {
    private boolean requested;
    private QuestCenterQuery questCenterQuery;
    private com.jsirgalaxybase.quest.core.ParticipantDirectoryQuery participantDirectoryQuery;
    private com.jsirgalaxybase.quest.core.AuthenticatedParticipantAdministrationService participantAdministrationService;
    private AuthenticatedQuestClaimService questClaimService;
    private AuthenticatedRewardChoiceService rewardChoiceService;
    private com.jsirgalaxybase.quest.core.AuthenticatedCheckboxConfirmationService checkboxConfirmationService;
    private AuthenticatedCraftingStatisticsDetectionService craftingStatisticsDetectionService;
    private AuthenticatedQuestConsumptionService questConsumptionService;
    private AuthenticatedQuestConsumptionDetectService questConsumptionDetectService;
    private AuthenticatedQuestTrackingService questTrackingService;
    private QuestDraftManagementService draftManagementService;
    private com.jsirgalaxybase.quest.core.QuestDefinitionBatchRetirementService batchRetirementService;
    private AuthenticatedQuestDefinitionManagementQuery definitionManagementQuery;
    private AuthenticatedQuestChapterManagementQuery chapterManagementQuery;
    private AuthenticatedQuestChapterDependencyQuery chapterDependencyQuery;
    private AuthenticatedQuestDefinitionImpactQuery definitionImpactQuery;
    private com.jsirgalaxybase.quest.core.AuthenticatedQuestContentMigrationQuery contentMigrationQuery;
    private QuestChapterManagementService chapterManagementService;
    private QuestChapterCloneService chapterCloneService;
    private QuestChapterOrderingService chapterOrderingService;
    private QuestChapterAlignmentService chapterAlignmentService;
    private QuestRuntimeService questRuntimeService;
    private QuestRuntimeTypeRegistry questTypeRegistry;
    private QuestGameplayEventHandler gameplayEventHandler;
    private QuestConsumptionRecoveryHandler consumptionRecoveryHandler;
    private boolean gameplayEventsRegistered;
    private boolean consumptionRecoveryRegistered;
    private boolean trackingSyncRegistered;
    private QuestRewardDeliveryController rewardDeliveryController;
    private boolean rewardDeliveryRegistered;
    private String unavailableReason = "quest PostgreSQL read runtime is disabled";

    public QuestModule() { super("quest", "Cross-server Quest Platform", "core"); }

    @Override
    public void preInit(ModuleContext context,FMLPreInitializationEvent event) {
        requested=!context.isClient()&&context.getConfiguration().isQuestPostgresReadEnabled();
        if(requested)GalaxyBase.LOG.info("Quest PostgreSQL read runtime requested; schema validation is deferred to dedicated server start");
    }

    @Override
    public void serverStarting(ModuleContext context,FMLServerStartingEvent event) {
        if (consumptionRecoveryRegistered && consumptionRecoveryHandler != null) {
            unregisterConsumptionRecovery(consumptionRecoveryHandler);
            consumptionRecoveryRegistered = false;
        }
        if (rewardDeliveryRegistered && rewardDeliveryController != null) {
            unregisterRewardDelivery(rewardDeliveryController);
            rewardDeliveryRegistered = false;
        }
        if (gameplayEventsRegistered && gameplayEventHandler != null) {
            unregisterGameplayEvents(gameplayEventHandler);
            gameplayEventsRegistered = false;
        }
        TerminalService.installQuestCenterQuery(null);
        TerminalService.installQuestParticipantDirectoryQuery(null);
        TerminalService.installQuestParticipantAdministrationService(null);
        TerminalService.installQuestClaimService(null);
        TerminalService.installRewardChoiceService(null);
        TerminalService.installCheckboxConfirmationService(null);
        TerminalService.installQuestConsumptionService(null);
        TerminalService.installQuestConsumptionDetectService(null);
        TerminalService.installQuestTrackingService(null);
        TerminalService.installQuestDefinitionManagementQuery(null);
        TerminalService.installQuestDraftManagementService(null);
        TerminalService.installQuestBatchRetirementService(null);
        TerminalService.installQuestChapterManagementQuery(null);
        TerminalService.installQuestChapterDependencyQuery(null);
        TerminalService.installQuestDefinitionImpactQuery(null);
        TerminalService.installQuestContentMigrationQuery(null);
        TerminalService.installQuestChapterManagementService(null);
        TerminalService.installQuestChapterCloneService(null);
        TerminalService.installQuestChapterOrderingService(null);
        TerminalService.installQuestChapterAlignmentService(null);
        questCenterQuery=null;participantDirectoryQuery=null;participantAdministrationService=null; batchRetirementService=null;
        questClaimService=null;rewardChoiceService=null;checkboxConfirmationService=null;craftingStatisticsDetectionService=null;questTrackingService=null;draftManagementService=null;definitionManagementQuery=null;
        questConsumptionService=null;
        questConsumptionDetectService=null;
        chapterManagementQuery=null;chapterDependencyQuery=null;definitionImpactQuery=null;contentMigrationQuery=null;chapterManagementService=null;chapterCloneService=null;chapterOrderingService=null;chapterAlignmentService=null;
        questRuntimeService=null;questTypeRegistry=null;gameplayEventHandler=null;consumptionRecoveryHandler=null;rewardDeliveryController=null;
        if(!requested)return;
        if(!isDedicatedServer()){
            unavailableReason="quest PostgreSQL read runtime requires a dedicated server";
            GalaxyBase.LOG.warn(unavailableReason);return;
        }
        InstitutionCoreModule institution=context.getModuleManager().findModule(InstitutionCoreModule.class);
        BankingInfrastructure banking=institution==null?null:institution.getBankingInfrastructure();
        JdbcConnectionManager shared=banking==null?null:banking.getSharedConnectionManager();
        if(shared==null){
            unavailableReason="quest PostgreSQL read runtime requires the shared banking JDBC connection";
            GalaxyBase.LOG.warn(unavailableReason);return;
        }
        boolean noRewards=context.getConfiguration().isQuestNoRewardsEnabled();
        try{
            questTypeRegistry=createQuestRuntimeTypeRegistry();
            questCenterQuery=createAndValidateQuery(shared);
            if(noRewards)validateRewardSuppressionSchema(shared);
            participantDirectoryQuery=createParticipantDirectoryQuery(shared);
            participantAdministrationService=createParticipantAdministrationService(shared);
            questClaimService=createClaimService(shared,noRewards);
            rewardChoiceService=createChoiceService(shared);
            questTrackingService=createTrackingService(shared);
            QuestEditorAuthorization editorAuthorization=createEditorAuthorization();
            draftManagementService=createDraftManagementService(shared,editorAuthorization);
            batchRetirementService=createBatchRetirementService(shared,editorAuthorization);
            definitionManagementQuery=createDefinitionManagementQuery(shared,editorAuthorization);
            chapterManagementQuery=createChapterManagementQuery(shared,editorAuthorization);
            chapterDependencyQuery=createChapterDependencyQuery(shared,editorAuthorization);
            definitionImpactQuery=createDefinitionImpactQuery(shared,editorAuthorization);
            contentMigrationQuery=createContentMigrationQuery(shared,editorAuthorization);
            chapterManagementService=createChapterManagementService(shared,editorAuthorization);
            chapterCloneService=createChapterCloneService(shared,editorAuthorization);
            chapterOrderingService=createChapterOrderingService(shared,editorAuthorization);
            chapterAlignmentService=createChapterAlignmentService(shared,editorAuthorization);
            JdbcQuestConnectionManager runtimeManager=new JdbcQuestConnectionManager(new SharedDataSource(shared));
            questRuntimeService=createQuestRuntimeService(runtimeManager,noRewards);
            checkboxConfirmationService=createCheckboxConfirmationService(runtimeManager,questRuntimeService,
                context.getConfiguration().getBankingSourceServerId());
            craftingStatisticsDetectionService=createCraftingStatisticsDetectionService(runtimeManager,questRuntimeService,
                context.getConfiguration().getBankingSourceServerId());
            questConsumptionService=createQuestConsumptionService(runtimeManager,questRuntimeService,
                context.getConfiguration().getBankingSourceServerId());
            questConsumptionDetectService=createQuestConsumptionDetectService(runtimeManager,questRuntimeService,
                context.getConfiguration().getBankingSourceServerId());
            consumptionRecoveryHandler=new QuestConsumptionRecoveryHandler(questConsumptionService);
            gameplayEventHandler=createGameplayEventHandler(runtimeManager, questRuntimeService,
                context.getConfiguration().getBankingSourceServerId(), craftingStatisticsDetectionService);
            rewardDeliveryController=createRewardDeliveryController(runtimeManager,
                context.getConfiguration().getBankingSourceServerId(),
                context.getConfiguration().getQuestCommandRewardAllowlist());
            TerminalService.installQuestCenterQuery(questCenterQuery);
            TerminalService.installQuestParticipantDirectoryQuery(participantDirectoryQuery);
            TerminalService.installQuestParticipantAdministrationService(participantAdministrationService);
            TerminalService.installQuestClaimService(questClaimService);
            TerminalService.installRewardChoiceService(rewardChoiceService);
            TerminalService.installCheckboxConfirmationService(checkboxConfirmationService);
            TerminalService.installQuestConsumptionService(questConsumptionService);
            TerminalService.installQuestConsumptionDetectService(questConsumptionDetectService);
            TerminalService.installQuestTrackingService(questTrackingService);
            TerminalService.installQuestDefinitionManagementQuery(definitionManagementQuery);
            TerminalService.installQuestDraftManagementService(draftManagementService);
            TerminalService.installQuestBatchRetirementService(batchRetirementService);
            TerminalService.installQuestChapterManagementQuery(chapterManagementQuery);
            TerminalService.installQuestChapterDependencyQuery(chapterDependencyQuery);
            TerminalService.installQuestDefinitionImpactQuery(definitionImpactQuery);
            TerminalService.installQuestContentMigrationQuery(contentMigrationQuery);
            TerminalService.installQuestChapterManagementService(chapterManagementService);
            TerminalService.installQuestChapterCloneService(chapterCloneService);
            TerminalService.installQuestChapterOrderingService(chapterOrderingService);
            TerminalService.installQuestChapterAlignmentService(chapterAlignmentService);
            if(!trackingSyncRegistered){registerTrackingSync(questCenterQuery);trackingSyncRegistered=true;}
            if(!gameplayEventsRegistered){registerGameplayEvents(gameplayEventHandler);gameplayEventsRegistered=true;}
            if(!consumptionRecoveryRegistered){registerConsumptionRecovery(consumptionRecoveryHandler);consumptionRecoveryRegistered=true;}
            if(!rewardDeliveryRegistered){registerRewardDelivery(rewardDeliveryController);rewardDeliveryRegistered=true;}
            unavailableReason="";
            GalaxyBase.LOG.info("Quest PostgreSQL read runtime enabled after read-only schema validation");
        }catch(RuntimeException failure){
            checkboxConfirmationService=null;
            TerminalService.installCheckboxConfirmationService(null);
            craftingStatisticsDetectionService=null;
            questConsumptionService=null;
            questConsumptionDetectService=null;
            if(consumptionRecoveryRegistered&&consumptionRecoveryHandler!=null){unregisterConsumptionRecovery(consumptionRecoveryHandler);consumptionRecoveryRegistered=false;}
            consumptionRecoveryHandler=null;
            TerminalService.installQuestConsumptionService(null);
            TerminalService.installQuestConsumptionDetectService(null);
            questCenterQuery=null;participantDirectoryQuery=null;participantAdministrationService=null;batchRetirementService=null;questClaimService=null;rewardChoiceService=null;questTrackingService=null;draftManagementService=null;definitionManagementQuery=null;chapterManagementQuery=null;chapterDependencyQuery=null;definitionImpactQuery=null;contentMigrationQuery=null;chapterManagementService=null;chapterCloneService=null;chapterOrderingService=null;chapterAlignmentService=null;TerminalService.installQuestCenterQuery(null);TerminalService.installQuestParticipantDirectoryQuery(null);TerminalService.installQuestParticipantAdministrationService(null);TerminalService.installQuestClaimService(null);TerminalService.installRewardChoiceService(null);TerminalService.installQuestTrackingService(null);TerminalService.installQuestDefinitionManagementQuery(null);TerminalService.installQuestDraftManagementService(null);TerminalService.installQuestBatchRetirementService(null);TerminalService.installQuestChapterManagementQuery(null);TerminalService.installQuestChapterDependencyQuery(null);TerminalService.installQuestDefinitionImpactQuery(null);TerminalService.installQuestContentMigrationQuery(null);TerminalService.installQuestChapterManagementService(null);TerminalService.installQuestChapterCloneService(null);TerminalService.installQuestChapterOrderingService(null);TerminalService.installQuestChapterAlignmentService(null);
            questRuntimeService=null;questTypeRegistry=null;gameplayEventHandler=null;rewardDeliveryController=null;
            unavailableReason="quest PostgreSQL read runtime failed schema validation: "+safe(failure.getMessage());
            GalaxyBase.LOG.error(unavailableReason,failure);
        }
    }

    protected boolean isDedicatedServer(){MinecraftServer server=MinecraftServer.getServer();return server!=null&&server.isDedicatedServer();}
    protected void registerTrackingSync(QuestCenterQuery query){FMLCommonHandler.instance().bus().register(new TrackedQuestSyncController(new TrackedQuestSnapshotFactory(query)));}
    protected void registerGameplayEvents(QuestGameplayEventHandler handler){
        FMLCommonHandler.instance().bus().register(handler);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(handler);
    }
    protected void unregisterGameplayEvents(QuestGameplayEventHandler handler){
        FMLCommonHandler.instance().bus().unregister(handler);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(handler);
    }
    protected void registerConsumptionRecovery(QuestConsumptionRecoveryHandler handler){
        FMLCommonHandler.instance().bus().register(handler);
    }
    protected void unregisterConsumptionRecovery(QuestConsumptionRecoveryHandler handler){
        FMLCommonHandler.instance().bus().unregister(handler);
    }
    protected void registerRewardDelivery(QuestRewardDeliveryController controller){
        FMLCommonHandler.instance().bus().register(controller);
    }
    protected void unregisterRewardDelivery(QuestRewardDeliveryController controller){
        FMLCommonHandler.instance().bus().unregister(controller);
    }
    protected QuestCenterQuery createAndValidateQuery(JdbcConnectionManager shared){
        JdbcQuestCenterQuery query=new JdbcQuestCenterQuery(new JdbcQuestConnectionManager(new SharedDataSource(shared)));
        query.validateSchema();return query;
    }
    protected void validateRewardSuppressionSchema(JdbcConnectionManager shared){
        new JdbcQuestCenterQuery(new JdbcQuestConnectionManager(new SharedDataSource(shared)))
            .validateSuppressedRewardStatus();
    }
    protected AuthenticatedQuestClaimService createClaimService(JdbcConnectionManager shared){
        return createClaimService(shared,false);
    }
    protected AuthenticatedQuestClaimService createClaimService(JdbcConnectionManager shared,boolean noRewards){
        JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new AuthenticatedQuestClaimService(new JdbcQuestDefinitionRepository(manager),
            new JdbcQuestRuntimeRepository(manager),new JdbcRewardClaimRepository(manager),new JdbcQuestTransaction(manager),
            new RewardSuppressionPolicy(noRewards),new JdbcRewardChoiceSelectionRepository(manager));
    }
    protected com.jsirgalaxybase.quest.core.ParticipantDirectoryQuery createParticipantDirectoryQuery(JdbcConnectionManager shared){JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(new SharedDataSource(shared));return new com.jsirgalaxybase.quest.postgres.JdbcParticipantDirectoryQuery(manager);}
    protected com.jsirgalaxybase.quest.core.AuthenticatedParticipantAdministrationService createParticipantAdministrationService(JdbcConnectionManager shared){JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(new SharedDataSource(shared));return new com.jsirgalaxybase.quest.core.AuthenticatedParticipantAdministrationService(new com.jsirgalaxybase.quest.postgres.JdbcParticipantAdministrationRepository(manager),new JdbcQuestTransaction(manager));}
    protected AuthenticatedRewardChoiceService createChoiceService(JdbcConnectionManager shared){
        JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new AuthenticatedRewardChoiceService(new JdbcQuestDefinitionRepository(manager),
            new JdbcQuestRuntimeRepository(manager),new JdbcRewardChoiceSelectionRepository(manager),new JdbcQuestTransaction(manager));
    }
    protected com.jsirgalaxybase.quest.core.AuthenticatedCheckboxConfirmationService createCheckboxConfirmationService(
        JdbcQuestConnectionManager manager,QuestRuntimeService runtime,String sourceServer){
        JdbcQuestDefinitionRepository definitions=new JdbcQuestDefinitionRepository(manager);
        QuestEvaluationPlanner planner=new QuestEvaluationPlanner(definitions,new JdbcQuestCompletionQuery(manager),
            new JdbcParticipantMembershipResolver(manager),new ScopedQuestAssignmentPolicy(),
            createPartyActivityQuery(manager,sourceServer));
        return new com.jsirgalaxybase.quest.core.AuthenticatedCheckboxConfirmationService(definitions,
            new JdbcQuestRuntimeRepository(manager),runtime,planner,sourceServer);
    }
    protected AuthenticatedCraftingStatisticsDetectionService createCraftingStatisticsDetectionService(
        JdbcQuestConnectionManager manager, QuestRuntimeService runtime, String sourceServer) {
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository progress = new JdbcQuestRuntimeRepository(manager);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions, new JdbcQuestCompletionQuery(manager),
            new JdbcParticipantMembershipResolver(manager), new ScopedQuestAssignmentPolicy(),
            createPartyActivityQuery(manager,sourceServer));
        return new AuthenticatedCraftingStatisticsDetectionService(definitions, progress, runtime, planner,
            new com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftCraftingStatisticReader(
                new OnlineServerQuestPlayerResolver()), sourceServer);
    }
    protected AuthenticatedQuestConsumptionService createQuestConsumptionService(JdbcQuestConnectionManager manager,
        QuestRuntimeService runtime,String sourceServer){
        JdbcQuestDefinitionRepository definitions=new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository progress=new JdbcQuestRuntimeRepository(manager);
        JdbcQuestCompletionQuery completions=new JdbcQuestCompletionQuery(manager);
        JdbcParticipantMembershipResolver memberships=new JdbcParticipantMembershipResolver(manager);
        ScopedQuestAssignmentPolicy assignments=new ScopedQuestAssignmentPolicy();
        QuestEvaluationPlanner planner=new QuestEvaluationPlanner(definitions,completions,memberships,assignments,
            createPartyActivityQuery(manager,sourceServer));
        ConsumptionRequestAuthorizer authorizer=new ConsumptionRequestAuthorizer(definitions,progress,completions,
            new QuestEngine(),memberships,assignments);
        ConsumptionSubmissionService submissions=new ConsumptionSubmissionService(
            new JdbcConsumptionSubmissionRepository(manager),
            new com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftPlayerConsumptionPort(
                new OnlineServerQuestPlayerResolver(),new WorldSaveQuestPlayerDataFlusher()),runtime,planner,
            new JdbcQuestTransaction(manager));
        return new AuthenticatedQuestConsumptionService(submissions,authorizer,sourceServer);
    }
    protected AuthenticatedQuestConsumptionDetectService createQuestConsumptionDetectService(
        JdbcQuestConnectionManager manager, QuestRuntimeService runtime, String sourceServer) {
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository progress = new JdbcQuestRuntimeRepository(manager);
        JdbcQuestCompletionQuery completions = new JdbcQuestCompletionQuery(manager);
        JdbcParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(manager);
        ScopedQuestAssignmentPolicy assignments = new ScopedQuestAssignmentPolicy();
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions, completions, memberships, assignments,
            createPartyActivityQuery(manager, sourceServer));
        ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(definitions, progress, completions,
            new QuestEngine(), memberships, assignments);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        ConsumptionSubmissionService submissions = new ConsumptionSubmissionService(
            new JdbcConsumptionSubmissionRepository(manager),
            new com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftPlayerConsumptionPort(
                new OnlineServerQuestPlayerResolver(), new WorldSaveQuestPlayerDataFlusher()), runtime, planner,
            transaction);
        return new AuthenticatedQuestConsumptionDetectService(sourceServer,
            new JdbcConsumptionDetectBatchRepository(manager), authorizer, submissions, runtime, planner, transaction);
    }
    protected AuthenticatedQuestTrackingService createTrackingService(JdbcConnectionManager shared){
        JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new AuthenticatedQuestTrackingService(new JdbcQuestDefinitionRepository(manager),
            new JdbcQuestTrackingRepository(manager),new JdbcQuestTransaction(manager));
    }
    protected QuestEditorAuthorization createEditorAuthorization(){return new MinecraftQuestEditorAuthorization();}
    /**
     * Explicit Base-owned extensions.  The default is empty so a plain Base
     * installation remains self-contained; modules may override this hook to
     * contribute a type and its evaluator as one atomic registration unit.
     */
    protected Collection<QuestRuntimeTypeExtension> questTypeExtensions(){
        return Collections.<QuestRuntimeTypeExtension>emptyList();
    }
    protected QuestRuntimeTypeRegistry createQuestRuntimeTypeRegistry(){
        return QuestRuntimeTypeRegistry.withExtensions(questTypeExtensions());
    }
    protected QuestDraftManagementService createDraftManagementService(JdbcConnectionManager shared,QuestEditorAuthorization authorization){
        JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new QuestDraftManagementService(new JdbcQuestDefinitionRepository(manager),new JdbcQuestTransaction(manager),
            authorization,questTypeRegistry==null?StandardQuestEditorTypeRegistry.create():questTypeRegistry.getEditorTypes());
    }
    protected com.jsirgalaxybase.quest.core.QuestDefinitionBatchRetirementService createBatchRetirementService(JdbcConnectionManager shared,QuestEditorAuthorization authorization){
        JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new com.jsirgalaxybase.quest.core.QuestDefinitionBatchRetirementService(new JdbcQuestDefinitionRepository(manager),
            new JdbcQuestChapterRepository(manager),new JdbcQuestTransaction(manager),authorization);
    }
    protected AuthenticatedQuestDefinitionManagementQuery createDefinitionManagementQuery(JdbcConnectionManager shared,QuestEditorAuthorization authorization){
        JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new AuthenticatedQuestDefinitionManagementQuery(new JdbcQuestDefinitionRepository(manager),authorization);
    }
    protected AuthenticatedQuestChapterManagementQuery createChapterManagementQuery(JdbcConnectionManager shared,
        QuestEditorAuthorization authorization) {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new AuthenticatedQuestChapterManagementQuery(new JdbcQuestChapterRepository(manager), authorization);
    }
    protected AuthenticatedQuestChapterDependencyQuery createChapterDependencyQuery(JdbcConnectionManager shared,
        QuestEditorAuthorization authorization) {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new AuthenticatedQuestChapterDependencyQuery(new JdbcQuestChapterRepository(manager),
            new JdbcQuestDefinitionRepository(manager), authorization);
    }
    protected AuthenticatedQuestDefinitionImpactQuery createDefinitionImpactQuery(JdbcConnectionManager shared,
        QuestEditorAuthorization authorization) {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new AuthenticatedQuestDefinitionImpactQuery(new JdbcQuestDefinitionRepository(manager),
            new JdbcQuestChapterRepository(manager), authorization);
    }
    protected com.jsirgalaxybase.quest.core.AuthenticatedQuestContentMigrationQuery createContentMigrationQuery(JdbcConnectionManager shared,QuestEditorAuthorization authorization){JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(new SharedDataSource(shared));return new com.jsirgalaxybase.quest.core.AuthenticatedQuestContentMigrationQuery(new com.jsirgalaxybase.quest.postgres.JdbcQuestContentMigrationRepository(manager),authorization);}
    protected QuestChapterManagementService createChapterManagementService(JdbcConnectionManager shared,
        QuestEditorAuthorization authorization) {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new QuestChapterManagementService(new JdbcQuestChapterRepository(manager),
            new JdbcQuestDefinitionRepository(manager), new JdbcQuestTransaction(manager), authorization);
    }
    protected QuestChapterCloneService createChapterCloneService(JdbcConnectionManager shared,
        QuestEditorAuthorization authorization) {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new QuestChapterCloneService(new JdbcQuestChapterRepository(manager),
            new JdbcQuestDefinitionRepository(manager), new JdbcQuestTransaction(manager), authorization,
            questTypeRegistry==null?StandardQuestEditorTypeRegistry.create():questTypeRegistry.getEditorTypes());
    }
    protected QuestChapterOrderingService createChapterOrderingService(JdbcConnectionManager shared,
        QuestEditorAuthorization authorization) {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(new SharedDataSource(shared));
        return new QuestChapterOrderingService(new JdbcQuestChapterRepository(manager), new JdbcQuestTransaction(manager), authorization);
    }
    protected QuestChapterAlignmentService createChapterAlignmentService(JdbcConnectionManager shared,
        QuestEditorAuthorization authorization){JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(new SharedDataSource(shared));return new QuestChapterAlignmentService(new JdbcQuestChapterRepository(manager),new JdbcQuestTransaction(manager),authorization);}
    protected QuestRuntimeService createQuestRuntimeService(JdbcQuestConnectionManager manager){
        return createQuestRuntimeService(manager,false);
    }
    protected QuestRuntimeService createQuestRuntimeService(JdbcQuestConnectionManager manager,boolean noRewards){
        return new QuestRuntimeService(new JdbcQuestRuntimeRepository(manager),new JdbcQuestTransaction(manager),
            new QuestFactProjector(questTypeRegistry==null?BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch():
                questTypeRegistry.getEvaluators()),new QuestEngine(),new RewardSuppressionPolicy(noRewards));
    }
    protected QuestGameplayEventHandler createGameplayEventHandler(JdbcQuestConnectionManager manager, QuestRuntimeService runtime,
        String sourceServer, AuthenticatedCraftingStatisticsDetectionService craftingStatisticsDetectionService){
        JdbcParticipantMembershipResolver memberships=new JdbcParticipantMembershipResolver(manager);
        JdbcQuestDefinitionRepository definitionRepository=new JdbcQuestDefinitionRepository(manager);
        RevisionAwarePublishedQuestCatalog definitions=new RevisionAwarePublishedQuestCatalog(definitionRepository,
            definitionRepository);
        QuestEvaluationPlanner planner=new QuestEvaluationPlanner(definitions,
            new JdbcQuestCompletionQuery(manager),memberships,
            new BqHuntPartyAssignmentPolicy(),createPartyActivityQuery(manager,sourceServer));
        QuestObservationPlanProvider observations=new QuestObservationPlanProvider(){
            private volatile QuestObservationPlan cached=QuestObservationPlan.empty();
            private volatile long nextRefresh;
            @Override public QuestObservationPlan current(){
                long now=System.currentTimeMillis();
                if(now>=nextRefresh){
                    synchronized(this){
                        if(now>=nextRefresh){
                            try{cached=QuestObservationPlan.fromPublished(definitions.findAllPublished());}
                            catch(RuntimeException ignored){/* retain last known plan during transient database failures */}
                            nextRefresh=now+5000L;
                        }
                    }
                }
                return cached;
            }
        };
        QuestClock clock=new QuestClock(){@Override public long currentTimeMillis(){return System.currentTimeMillis();}};
        MinecraftQuestFactFactory facts=new MinecraftQuestFactFactory(sourceServer);
        RuntimeQuestFactSink runtimeSink=new RuntimeQuestFactSink(runtime,planner,clock);
        BqPartyInventorySnapshotFactSink inventorySink=new BqPartyInventorySnapshotFactSink(runtimeSink,
            new JdbcQuestTransaction(manager),new JdbcBqPartyInventorySnapshotRepository(manager),memberships,
            facts,observations,clock);
        QuestPlayerResolver statisticPlayers = new OnlineServerQuestPlayerResolver();
        com.jsirgalaxybase.quest.core.QuestOnlinePlayerDirectory onlinePlayers =
            new MinecraftQuestOnlinePlayerDirectory();
        QuestGameplayEventHandler.CraftingStatisticFactProducer statisticFacts =
            new QuestGameplayEventHandler.CraftingStatisticFactProducer() {
                @Override public java.util.Map<String, String> outputAttributes(net.minecraft.item.ItemStack trigger,
                    int amount, String channel) {
                    return facts.itemProducedAttributes(trigger, amount, channel);
                }

                @Override public com.jsirgalaxybase.quest.core.GameplayFact create(
                    net.minecraft.entity.player.EntityPlayer triggerPlayer, java.util.UUID observedPlayerId,
                    String registryName, int meta, net.minecraft.item.ItemStack trigger, String triggerChannel,
                    boolean bqPartyOnlineSnapshot) {
                    net.minecraft.entity.player.EntityPlayerMP observed = statisticPlayers.findOnline(observedPlayerId);
                    if (observed == null) return null;
                    return facts.craftingStatisticObserved(observed, registryName, meta, trigger, triggerChannel,
                        bqPartyOnlineSnapshot);
                }
            };
        QuestGameplayEventHandler.CraftingStatisticMemberResolver statisticMembers =
            new QuestGameplayEventHandler.CraftingStatisticMemberResolver() {
                @Override public List<java.util.UUID> localOnlineMembers(
                    net.minecraft.entity.player.EntityPlayer triggerPlayer) {
                    if (triggerPlayer == null) return Collections.emptyList();
                    return BqPartyCraftingStatisticsMembers.localOnlineMembers(triggerPlayer.getUniqueID(),
                        memberships.resolve(triggerPlayer.getUniqueID()), onlinePlayers);
                }
            };
        QuestGameplayEventHandler.AutomaticCraftingStatisticsDetector automaticCraftingStatisticsDetector =
            new QuestGameplayEventHandler.AutomaticCraftingStatisticsDetector() {
                @Override public void detect(java.util.UUID playerId,
                    com.jsirgalaxybase.quest.core.CraftingStatisticsDetectionTarget target, long now) {
                    craftingStatisticsDetectionService.detectAutomatically(playerId, target.getQuestId(),
                        target.getTaskKey(), now);
                }
            };
        return new QuestGameplayEventHandler(inventorySink,facts,observations,clock,statisticFacts,statisticMembers,
            automaticCraftingStatisticsDetector, new QuestGameplayEventHandler.CraftingEventEligibilitySnapshotter() {
                @Override public List<com.jsirgalaxybase.quest.core.QuestEvaluationRequest> capture(
                    com.jsirgalaxybase.quest.core.GameplayFact fact) {
                    return planner.requestsFor(fact);
                }
            });
    }

    protected com.jsirgalaxybase.quest.core.QuestPartyActivityQuery createPartyActivityQuery(
        JdbcQuestConnectionManager manager, String sourceServer) {
        JdbcQuestCompletionQuery completions = new JdbcQuestCompletionQuery(manager);
        return new com.jsirgalaxybase.quest.core.BqPartyActiveQuestQuery(sourceServer, completions,
            new JdbcQuestRuntimeRepository(manager), new MinecraftQuestOnlinePlayerDirectory());
    }
    protected QuestRewardDeliveryController createRewardDeliveryController(JdbcQuestConnectionManager manager,
        String sourceServer, String[] commandAllowlist) {
        com.jsirgalaxybase.quest.core.RewardDeliveryWorker worker =
            new com.jsirgalaxybase.quest.core.RewardDeliveryWorker(
                new JdbcRewardDeliveryRepository(manager), new JdbcQuestTransaction(manager),
                new com.jsirgalaxybase.quest.core.RewardDeliveryRouter(
                    createRewardDeliveryHandlers(manager, commandAllowlist)),
                new QuestClock(){@Override public long currentTimeMillis(){return System.currentTimeMillis();}},
                30000L, 5000L, 10);
        String workerId = "quest-reward:" + (sourceServer == null || sourceServer.trim().isEmpty()
            ? "unknown" : sourceServer.trim());
        return new QuestRewardDeliveryController(worker, workerId);
    }

    protected List<com.jsirgalaxybase.quest.core.RewardDeliveryHandler> createRewardDeliveryHandlers(
        JdbcQuestConnectionManager manager, String[] commandAllowlist) {
        QuestPlayerResolver players = new OnlineServerQuestPlayerResolver();
        WorldSaveQuestPlayerDataFlusher flusher = new WorldSaveQuestPlayerDataFlusher();
        List<com.jsirgalaxybase.quest.core.RewardDeliveryHandler> handlers =
            new ArrayList<com.jsirgalaxybase.quest.core.RewardDeliveryHandler>();
        handlers.add(new MinecraftItemRewardDeliveryHandler("bq_standard:item", players, flusher));
        handlers.add(new MinecraftItemRewardDeliveryHandler("bq_standard:choice", players, flusher));
        handlers.add(new MinecraftXpRewardDeliveryHandler(players, flusher));
        handlers.add(new MinecraftScoreboardRewardDeliveryHandler(players, flusher));
        handlers.add(new com.jsirgalaxybase.quest.core.QuestCompletionRewardDeliveryHandler(
            new JdbcQuestCompletionRewardPort(new JdbcQuestDefinitionRepository(manager),
                new JdbcQuestRuntimeRepository(manager), new JdbcQuestTransaction(manager),
                new JdbcParticipantMembershipResolver(manager)),
            new QuestClock(){@Override public long currentTimeMillis(){return System.currentTimeMillis();}}));
        handlers.add(new com.jsirgalaxybase.quest.core.CommandRewardDeliveryHandler(
            new com.jsirgalaxybase.quest.core.AllowlistedCommandRewardPolicy(
                new java.util.LinkedHashSet<String>(Arrays.asList(commandAllowlist == null
                    ? new String[0] : commandAllowlist))),
            new MinecraftCommandRewardExecutor(players, flusher),
            participant -> {
                net.minecraft.entity.player.EntityPlayerMP player = players.findOnline(participant.getId());
                return player == null ? "" : player.getCommandSenderName();
            }));
        return handlers;
    }
    public boolean isReadRuntimeActive(){return questCenterQuery!=null;}
    public String getUnavailableReason(){return unavailableReason;}
    public QuestCenterQuery getQuestCenterQuery(){return questCenterQuery;}
    public com.jsirgalaxybase.quest.core.ParticipantDirectoryQuery getParticipantDirectoryQuery(){return participantDirectoryQuery;}
    public com.jsirgalaxybase.quest.core.AuthenticatedParticipantAdministrationService getParticipantAdministrationService(){return participantAdministrationService;}
    public AuthenticatedQuestClaimService getQuestClaimService(){return questClaimService;}
    public AuthenticatedRewardChoiceService getRewardChoiceService(){return rewardChoiceService;}
    public AuthenticatedQuestConsumptionService getQuestConsumptionService(){return questConsumptionService;}
    public AuthenticatedQuestTrackingService getQuestTrackingService(){return questTrackingService;}
    public QuestDraftManagementService getDraftManagementService(){return draftManagementService;}
    public AuthenticatedQuestDefinitionManagementQuery getDefinitionManagementQuery(){return definitionManagementQuery;}
    public AuthenticatedQuestChapterManagementQuery getChapterManagementQuery(){return chapterManagementQuery;}
    public AuthenticatedQuestChapterDependencyQuery getChapterDependencyQuery(){return chapterDependencyQuery;}
    public AuthenticatedQuestDefinitionImpactQuery getDefinitionImpactQuery(){return definitionImpactQuery;}
    public QuestChapterManagementService getChapterManagementService(){return chapterManagementService;}
    public QuestChapterCloneService getChapterCloneService(){return chapterCloneService;}
    public QuestChapterOrderingService getChapterOrderingService(){return chapterOrderingService;}
    public QuestChapterAlignmentService getChapterAlignmentService(){return chapterAlignmentService;}
    public QuestRuntimeService getQuestRuntimeService(){return questRuntimeService;}
    public QuestRewardDeliveryController getRewardDeliveryController(){return rewardDeliveryController;}
    public QuestRuntimeTypeRegistry getQuestTypeRegistry(){return questTypeRegistry;}
    private static String safe(String value){return value==null||value.trim().isEmpty()?"unknown database error":value;}

    private static final class SharedDataSource implements DataSource {
        private final JdbcConnectionManager shared;
        private SharedDataSource(JdbcConnectionManager shared){this.shared=shared;}
        @Override public Connection getConnection()throws SQLException{return shared.openConnection();}
        @Override public Connection getConnection(String username,String password)throws SQLException{throw new SQLException("shared quest data source does not accept alternate credentials");}
        @Override public PrintWriter getLogWriter()throws SQLException{return java.sql.DriverManager.getLogWriter();}
        @Override public void setLogWriter(PrintWriter out)throws SQLException{java.sql.DriverManager.setLogWriter(out);}
        @Override public void setLoginTimeout(int seconds)throws SQLException{java.sql.DriverManager.setLoginTimeout(seconds);}
        @Override public int getLoginTimeout()throws SQLException{return java.sql.DriverManager.getLoginTimeout();}
        @Override public Logger getParentLogger(){return Logger.getLogger("com.jsirgalaxybase.quest");}
        @Override public <T>T unwrap(Class<T> type)throws SQLException{if(type.isInstance(this))return type.cast(this);throw new SQLException("Not a wrapper for "+type.getName());}
        @Override public boolean isWrapperFor(Class<?> type){return type.isInstance(this);}
    }
}

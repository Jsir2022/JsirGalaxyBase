package com.jsirgalaxybase.modules.quest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import com.jsirgalaxybase.config.ModConfiguration;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.module.ModuleManager;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.QuestGameplayEventHandler;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.QuestConsumptionRecoveryHandler;
import com.jsirgalaxybase.modules.quest.infrastructure.minecraft.QuestRewardDeliveryController;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestClaimService;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestCenterQuery;
import com.jsirgalaxybase.quest.core.QuestCenterSnapshot;
import com.jsirgalaxybase.quest.core.QuestRuntimeService;
import com.jsirgalaxybase.terminal.TerminalService;

public class QuestModuleTest {
    @After public void resetTerminalQuery(){TerminalService.installQuestCenterQuery(null);TerminalService.installQuestConsumptionService(null);TerminalService.installQuestClaimService(null);TerminalService.installRewardChoiceService(null);TerminalService.installQuestTrackingService(null);TerminalService.installQuestDefinitionManagementQuery(null);TerminalService.installQuestDraftManagementService(null);TerminalService.installQuestChapterManagementQuery(null);TerminalService.installQuestChapterDependencyQuery(null);TerminalService.installQuestChapterManagementService(null);TerminalService.installQuestChapterCloneService(null);TerminalService.installQuestChapterOrderingService(null);TerminalService.installQuestChapterAlignmentService(null);TerminalService.installQuestParticipantDirectoryQuery(null);TerminalService.installQuestParticipantAdministrationService(null);}

    @Test
    public void disabledConfigurationDoesNotPrepareQuestDatabase() throws Exception {
        ModuleManager modules=new ModuleManager();TestQuestModule quest=new TestQuestModule();modules.addModule(quest);
        ModuleContext context=new ModuleContext(false,configuration(false),modules);
        quest.preInit(context,null);quest.serverStarting(context,null);
        assertFalse(quest.isReadRuntimeActive());assertFalse(quest.createCalled);
    }

    @Test
    public void requestedRuntimeFailsClosedWithoutSharedBankingConnection() throws Exception {
        ModuleManager modules=new ModuleManager();modules.addModule(new InstitutionCoreModule());
        TestQuestModule quest=new TestQuestModule();modules.addModule(quest);
        ModuleContext context=new ModuleContext(false,configuration(true),modules);
        quest.preInit(context,null);quest.serverStarting(context,null);
        assertFalse(quest.isReadRuntimeActive());assertFalse(quest.createCalled);
        assertTrue(quest.getUnavailableReason().contains("shared banking JDBC"));
    }

    @Test
    public void requestedRuntimeInstallsValidatedReadQueryAfterInstitutionModule() throws Exception {
        final JdbcConnectionManager shared=new JdbcConnectionManager(new NoConnectionDataSource());
        BankingInfrastructure banking=new BankingInfrastructure(null,null,null,null,null,null,shared);
        ModuleManager modules=new ModuleManager();modules.addModule(new FixedInstitutionModule(banking));
        TestQuestModule quest=new TestQuestModule();modules.addModule(quest);
        ModuleContext context=new ModuleContext(false,configuration(true),modules);

        quest.preInit(context,null);quest.serverStarting(context,null);

        assertTrue(quest.createCalled);assertTrue(quest.isReadRuntimeActive());
        assertSame(quest.query,quest.getQuestCenterQuery());assertTrue(quest.getUnavailableReason().isEmpty());
        assertNotNull(quest.getQuestRuntimeService());
        assertNotNull(quest.getQuestTypeRegistry());
        assertTrue(quest.getQuestTypeRegistry().getEvaluators().snapshot().keySet().containsAll(bqTaskTypes()));
        assertNotNull(quest.getQuestTypeRegistry().getEditorTypes().require(
            "bq_standard:retrieval", com.jsirgalaxybase.quest.core.QuestElementKind.TASK));
        assertNotNull(quest.getDraftManagementService());assertNotNull(quest.getDefinitionManagementQuery());
        assertNotNull(quest.getChapterManagementQuery());assertNotNull(quest.getChapterManagementService());
        assertNotNull(quest.getChapterAlignmentService());
        assertNotNull(quest.getParticipantAdministrationService());
        assertNotNull(quest.getRewardDeliveryController());
        assertTrue(quest.gameplayEventsRegisteredForTest);
    }

    @Test
    public void baseCompositionRootRegistersEveryImportedTaskEvaluatorAndRewardHandler() throws Exception {
        TestQuestModule quest = new TestQuestModule();
        com.jsirgalaxybase.quest.postgres.JdbcQuestConnectionManager manager =
            new com.jsirgalaxybase.quest.postgres.JdbcQuestConnectionManager(new NoConnectionDataSource());

        Set<String> rewardTypes = new LinkedHashSet<String>();
        for (com.jsirgalaxybase.quest.core.RewardDeliveryHandler handler :
            quest.createHandlersForTest(manager)) rewardTypes.add(handler.getTypeId());

        assertEquals("the Base worker must build one unambiguous route for every configured reward handler",
            new LinkedHashSet<String>(java.util.Arrays.asList("bq_standard:choice", "bq_standard:command",
                "bq_standard:item", "bq_standard:questcompletion", "bq_standard:scoreboard", "bq_standard:xp")),
            rewardTypes);
        new com.jsirgalaxybase.quest.core.RewardDeliveryRouter(quest.createHandlersForTest(manager));
        assertTrue("all imported rewards must have a concrete Base delivery handler",
            rewardTypes.containsAll(new LinkedHashSet<String>(java.util.Arrays.asList(
                "bq_standard:item", "bq_standard:choice", "bq_standard:questcompletion", "bq_standard:xp"))));
    }

    private static Set<String> bqTaskTypes() {
        return new LinkedHashSet<String>(java.util.Arrays.asList(
            "bq_standard:block_break", "bq_standard:checkbox", "bq_standard:crafting", "bq_standard:fluid",
            "bq_standard:hunt", "bq_standard:location", "bq_standard:meeting",
            "bq_standard:optional_retrieval", "bq_standard:retrieval"));
    }

    @Test
    public void requestedRuntimeAppliesNoRewardsSettingToAutomaticAndManualRewards() throws Exception {
        final JdbcConnectionManager shared=new JdbcConnectionManager(new NoConnectionDataSource());
        BankingInfrastructure banking=new BankingInfrastructure(null,null,null,null,null,null,shared);
        ModuleManager modules=new ModuleManager();modules.addModule(new FixedInstitutionModule(banking));
        TestQuestModule quest=new TestQuestModule();modules.addModule(quest);
        ModuleContext context=new ModuleContext(false,configuration(true,true),modules);

        quest.preInit(context,null);quest.serverStarting(context,null);

        assertTrue(quest.isReadRuntimeActive());
        assertTrue(quest.claimNoRewards);
        assertTrue(quest.runtimeNoRewards);
        assertTrue(quest.suppressionSchemaValidated);
    }

    @Test
    public void noRewardsRuntimeFailsClosedUntilSuppressionMigrationIsInstalled() throws Exception {
        final JdbcConnectionManager shared=new JdbcConnectionManager(new NoConnectionDataSource());
        BankingInfrastructure banking=new BankingInfrastructure(null,null,null,null,null,null,shared);
        ModuleManager modules=new ModuleManager();modules.addModule(new FixedInstitutionModule(banking));
        TestQuestModule quest=new TestQuestModule();quest.suppressionSchemaValid=false;modules.addModule(quest);
        ModuleContext context=new ModuleContext(false,configuration(true,true),modules);

        quest.preInit(context,null);quest.serverStarting(context,null);

        assertFalse(quest.isReadRuntimeActive());
        assertTrue(quest.getUnavailableReason().contains("migration missing"));
    }

    private static ModConfiguration configuration(boolean enabled)throws Exception{
        return configuration(enabled,false);
    }

    private static ModConfiguration configuration(boolean enabled,boolean noRewards)throws Exception{
        Constructor<ModConfiguration> constructor=ModConfiguration.class.getDeclaredConstructor(File.class,
            boolean.class,String.class,int.class,float.class,float.class,float.class,boolean.class,String.class,
            String.class,String.class,String.class,boolean.class,String.class,int.class,int[].class,String[].class,
            boolean.class,boolean.class,boolean.class,String[].class,String.class,String[].class,boolean.class,
            String[].class,boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(new File("."),false,"items",0x529BED,0.72f,0.44f,0.07f,true,
            "jdbc:postgresql://example.invalid/db","test","test","test",false,"SHADOW",4,new int[0],
            new String[0],false,false,false,new String[0],"",new String[0],enabled,new String[0],noRewards);
    }

    private static final class TestQuestModule extends QuestModule{
        private final QuestCenterQuery query=new QuestCenterQuery(){public QuestCenterSnapshot load(ParticipantId id){return new QuestCenterSnapshot(id,Collections.emptyList(),Collections.emptyList());}};
        private boolean createCalled;
        private boolean claimNoRewards;
        private boolean runtimeNoRewards;
        private boolean suppressionSchemaValid=true;
        private boolean suppressionSchemaValidated;
        private boolean gameplayEventsRegisteredForTest;
        private java.util.List<com.jsirgalaxybase.quest.core.RewardDeliveryHandler> createHandlersForTest(
            com.jsirgalaxybase.quest.postgres.JdbcQuestConnectionManager manager) {
            return createRewardDeliveryHandlers(manager, new String[0]);
        }
        @Override protected boolean isDedicatedServer(){return true;}
        @Override protected QuestCenterQuery createAndValidateQuery(JdbcConnectionManager shared){createCalled=true;return query;}
        @Override protected void validateRewardSuppressionSchema(JdbcConnectionManager shared){
            suppressionSchemaValidated=true;
            if(!suppressionSchemaValid)throw new IllegalStateException("migration missing");
        }
        @Override protected AuthenticatedQuestClaimService createClaimService(JdbcConnectionManager shared,boolean noRewards){
            claimNoRewards=noRewards;return super.createClaimService(shared,noRewards);
        }
        @Override protected QuestRuntimeService createQuestRuntimeService(
            com.jsirgalaxybase.quest.postgres.JdbcQuestConnectionManager manager,boolean noRewards){
            runtimeNoRewards=noRewards;return super.createQuestRuntimeService(manager,noRewards);
        }
        @Override protected void registerTrackingSync(QuestCenterQuery query){}
        @Override protected void registerGameplayEvents(QuestGameplayEventHandler handler){gameplayEventsRegisteredForTest=true;}
        @Override protected void registerConsumptionRecovery(QuestConsumptionRecoveryHandler handler){}
        @Override protected void registerRewardDelivery(QuestRewardDeliveryController controller){}
        @Override protected void unregisterRewardDelivery(QuestRewardDeliveryController controller){}
    }
    private static final class FixedInstitutionModule extends InstitutionCoreModule{
        private final BankingInfrastructure banking;private FixedInstitutionModule(BankingInfrastructure banking){this.banking=banking;}
        @Override public BankingInfrastructure getBankingInfrastructure(){return banking;}
    }
    private static final class NoConnectionDataSource implements javax.sql.DataSource{
        public java.sql.Connection getConnection()throws java.sql.SQLException{throw new java.sql.SQLException("not used");}
        public java.sql.Connection getConnection(String u,String p)throws java.sql.SQLException{throw new java.sql.SQLException("not used");}
        public java.io.PrintWriter getLogWriter(){return null;}public void setLogWriter(java.io.PrintWriter out){}
        public void setLoginTimeout(int seconds){}public int getLoginTimeout(){return 0;}
        public java.util.logging.Logger getParentLogger(){return java.util.logging.Logger.getAnonymousLogger();}
        public <T>T unwrap(Class<T> type)throws java.sql.SQLException{throw new java.sql.SQLException("not wrapped");}
        public boolean isWrapperFor(Class<?> type){return false;}
    }
}

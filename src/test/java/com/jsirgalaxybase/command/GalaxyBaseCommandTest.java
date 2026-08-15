package com.jsirgalaxybase.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.module.ModuleManager;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.market.application.MarketRecoveryService;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationType;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;

public class GalaxyBaseCommandTest {

    @Test
    public void rootTabCompletionIncludesServerToolsRoutes() {
        ModuleManager moduleManager = new ModuleManager();
        moduleManager.addModule(new ServerToolsModule());
        GalaxyBaseCommand command = new GalaxyBaseCommand(moduleManager);

        List<String> rootSuggestions = command.addTabCompletionOptions(null, new String[] { "s" });
        List<String> serverToolsSuggestions = command.addTabCompletionOptions(null,
            new String[] { "servertools", "w" });

        assertTrue(rootSuggestions.contains("servertools"));
        assertTrue(rootSuggestions.contains("st"));
        assertEquals(1, serverToolsSuggestions.size());
        assertEquals("warp", serverToolsSuggestions.get(0));
    }

    @Test
    public void marketCustomTabCompletionIncludesManualDeliveryRecovery() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());

        List<String> suggestions = command.addTabCompletionOptions(null,
            new String[] { "market", "custom", "rec" });

        assertTrue(suggestions.contains("recover"));
    }

    @Test
    public void marketUsageKeepsPlayerOperationsOnTerminalUi() throws Exception {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingReplySink reply = new RecordingReplySink();
        Method method = GalaxyBaseCommand.class.getDeclaredMethod("sendMarketUsage", GalaxyBaseCommand.ReplySink.class);
        method.setAccessible(true);

        method.invoke(command, reply);

        assertTrue(reply.contains("玩家市场操作统一通过银河终端 UI"));
        assertTrue(reply.contains("/jsirgalaxybase market recover"));
        assertTrue(reply.contains("/jsirgalaxybase market custom recover"));
        assertFalse(reply.contains("market sell"));
        assertFalse(reply.contains("market buy"));
        assertFalse(reply.contains("market quote"));
        assertFalse(reply.contains("market exchange"));
        assertFalse(reply.contains("custom list hand"));
    }

    @Test
    public void recoverCommandUsesModuleRecoveryScanAndPrintsSummary() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        List<MarketOperationLog> recovered = Collections.singletonList(new MarketOperationLog(51L, "req-recover",
            MarketOperationType.BUY_ORDER_CREATE, MarketOperationStatus.COMPLETED, "test-server", "player-a",
            "playerRef=player-a", 71L, 0L, 0L, "recovered", Instant.now(), Instant.now()));
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(recovered);
        RecordingReplySink reply = new RecordingReplySink();

        command.processMarketRecoveryCommand(reply, new String[] { "market", "recover", "15" },
            institutionCoreModule, true);

        assertEquals(15, institutionCoreModule.lastRecoveryLimit);
        assertTrue(reply.contains("市场恢复扫描已处理 1 条操作"));
        assertTrue(reply.contains("op=51"));
    }

    private static final class RecordingReplySink implements GalaxyBaseCommand.ReplySink {

        private final List<String> messages = new ArrayList<String>();

        @Override
        public void send(String message) {
            messages.add(message);
        }

        private boolean contains(String text) {
            for (String message : messages) {
                if (message.contains(text)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class RecordingInstitutionCoreModule extends InstitutionCoreModule {

        private final List<MarketOperationLog> recoveryResults;
        private int lastRecoveryLimit;

        private RecordingInstitutionCoreModule(List<MarketOperationLog> recoveryResults) {
            this.recoveryResults = recoveryResults;
        }

        @Override
        public MarketRecoveryService getMarketRecoveryService() {
            return new MarketRecoveryService(null, null, null);
        }

        @Override
        public List<MarketOperationLog> scanMarketRecovery(int limit) {
            lastRecoveryLimit = limit;
            return recoveryResults;
        }
    }
}

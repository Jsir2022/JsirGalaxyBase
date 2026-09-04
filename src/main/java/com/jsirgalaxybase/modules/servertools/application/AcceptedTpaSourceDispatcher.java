package com.jsirgalaxybase.modules.servertools.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;

/**
 * Pure source-server coordinator for accepted TPA requests.
 *
 * <p>The coordinator deliberately has no Forge player or proxy dependency. The runtime supplies a dispatcher that
 * owns the current live connection, while this class owns source-server selection, deterministic plan construction,
 * and per-process duplicate suppression. Persistent cross-restart idempotency remains the responsibility of the
 * existing cluster transfer ticket keyed by the deterministic TPA request id.</p>
 */
public final class AcceptedTpaSourceDispatcher {

    private final PlayerTeleportService playerTeleportService;
    private final Set<String> dispatchedRequestIds = Collections.synchronizedSet(new HashSet<String>());

    public AcceptedTpaSourceDispatcher(PlayerTeleportService playerTeleportService) {
        if (playerTeleportService == null) {
            throw new IllegalArgumentException("playerTeleportService must not be null");
        }
        this.playerTeleportService = playerTeleportService;
    }

    /**
     * Finds accepted requests owned by this source-server actor and dispatches each one at most once per process.
     */
    public List<DispatchAttempt> dispatchAcceptedRequests(TeleportActor requester, Instant now,
        DispatchExecutor dispatchExecutor) {
        if (requester == null || now == null || dispatchExecutor == null) {
            throw new IllegalArgumentException("requester, now and dispatchExecutor must not be null");
        }
        List<TpaRequest> requests = playerTeleportService.listAcceptedTpaRequestsForRequester(
            requester.getSourceServerId(), requester.getPlayerUuid(), now);
        List<DispatchAttempt> attempts = new ArrayList<DispatchAttempt>();
        for (TpaRequest request : requests) {
            if (!markDispatched(request.getRequestId())) {
                continue;
            }
            try {
                TeleportDispatchPlan plan = playerTeleportService.prepareAcceptedTpaTeleport(requester, request, now);
                attempts.add(DispatchAttempt.completed(request, dispatchExecutor.dispatch(plan)));
            } catch (RuntimeException exception) {
                attempts.add(DispatchAttempt.failed(request, exception));
            }
        }
        return attempts;
    }

    private boolean markDispatched(String requestId) {
        return dispatchedRequestIds.add(requestId);
    }

    public interface DispatchExecutor {

        GatewayDispatchResult dispatch(TeleportDispatchPlan plan);
    }

    public static final class DispatchAttempt {

        private final TpaRequest request;
        private final GatewayDispatchResult gatewayResult;
        private final RuntimeException failure;

        private DispatchAttempt(TpaRequest request, GatewayDispatchResult gatewayResult, RuntimeException failure) {
            this.request = request;
            this.gatewayResult = gatewayResult;
            this.failure = failure;
        }

        private static DispatchAttempt completed(TpaRequest request, GatewayDispatchResult gatewayResult) {
            return new DispatchAttempt(request, gatewayResult, null);
        }

        private static DispatchAttempt failed(TpaRequest request, RuntimeException failure) {
            return new DispatchAttempt(request, null, failure);
        }

        public TpaRequest getRequest() {
            return request;
        }

        public GatewayDispatchResult getGatewayResult() {
            return gatewayResult;
        }

        public RuntimeException getFailure() {
            return failure;
        }

        public boolean isFailed() {
            return failure != null || gatewayResult == null
                || gatewayResult.getStatus() == GatewayDispatchResult.Status.FAILED;
        }
    }
}

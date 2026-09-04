package com.jsirgalaxybase.modules.servertools.infrastructure;

import java.time.Instant;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.port.ArrivalTargetResolver;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.application.ServerToolsException;
import com.jsirgalaxybase.modules.servertools.domain.GlobalEntryRules;
import com.jsirgalaxybase.modules.servertools.domain.TargetServerRtpProfile;
import com.jsirgalaxybase.modules.servertools.port.RtpCandidateFinder;

/**
 * Resolves remote RTP only after the ticket reaches its configured target
 * server. This prevents source servers from loading or inspecting remote
 * worlds, while the resulting coordinates are stored under the ticket id.
 */
public final class TargetServerRtpArrivalResolver implements ArrivalTargetResolver {

    private final String localServerId;
    private final GlobalEntryRules rules;
    private final RtpCandidateFinder candidateFinder;
    private final PlayerTeleportService teleportService;

    public TargetServerRtpArrivalResolver(String localServerId, GlobalEntryRules rules,
        RtpCandidateFinder candidateFinder, PlayerTeleportService teleportService) {
        this.localServerId = localServerId;
        this.rules = rules;
        this.candidateFinder = candidateFinder;
        this.teleportService = teleportService;
    }

    @Override
    public TeleportTarget resolve(TransferTicket ticket, EntityPlayerMP player) {
        if (ticket == null || !"RTP".equals(ticket.getTeleportKind())) return ticket == null ? null : ticket.getTarget();
        if (!localServerId.equals(ticket.getTarget().getServerId())) {
            throw new ServerToolsException("RTP ticket target does not belong to this server");
        }
        TargetServerRtpProfile profile = rules.findRtpProfile(localServerId).orElse(null);
        if (profile == null) throw new ServerToolsException("Target server has no configured RTP profile");
        TeleportTarget target = candidateFinder.findSafeTarget(profile, player);
        if (target == null || !localServerId.equals(target.getServerId())) {
            throw new ServerToolsException("Target RTP candidate is not local to this server");
        }
        return target;
    }

    @Override
    public void afterSuccessfulRestore(TransferTicket ticket, TeleportTarget resolvedTarget) {
        if (ticket == null || !"RTP".equals(ticket.getTeleportKind())) return;
        teleportService.recordResolvedTargetServerRandomTeleport(ticket.getRequestId(), ticket.getPlayerUuid(),
            ticket.getSourceServerId(), resolvedTarget, Instant.now());
    }
}

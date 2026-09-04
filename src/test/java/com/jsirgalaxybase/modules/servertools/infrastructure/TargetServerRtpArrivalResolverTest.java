package com.jsirgalaxybase.modules.servertools.infrastructure;

import static org.junit.Assert.assertEquals;

import java.time.Instant;

import org.junit.Test;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicketStatus;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.GlobalEntryRules;
import com.jsirgalaxybase.modules.servertools.domain.RandomTeleportRecord;
import com.jsirgalaxybase.modules.servertools.domain.TargetServerRtpProfile;

public class TargetServerRtpArrivalResolverTest {

    @Test
    public void remoteRtpResolvesOnTargetAndAuditsWithTicketRequestId() {
        RecordingService service = new RecordingService();
        TeleportTarget resolved = new TeleportTarget("s2", 7, 123.5D, 80.1D, -42.5D, 15.0F, 0.0F);
        TargetServerRtpArrivalResolver resolver = new TargetServerRtpArrivalResolver("s2",
            GlobalEntryRules.parse("", new String[] { "s2|7|0|80|0|256|2048" }),
            new FixedFinder(resolved), service);

        TeleportTarget target = resolver.resolve(ticket("RTP", "s2"), null);

        assertEquals(resolved.getX(), target.getX(), 0.0001D);
        assertEquals(null, service.requestId);
        resolver.afterSuccessfulRestore(ticket("RTP", "s2"), target);
        assertEquals("rtp-request", service.requestId);
        assertEquals("player-uuid", service.playerUuid);
        assertEquals("lobby", service.sourceServerId);
        assertEquals("s2", service.target.getServerId());
    }

    @Test
    public void nonRtpTicketPassesThroughWithoutAudit() {
        RecordingService service = new RecordingService();
        TeleportTarget original = ticket("HOME", "s2").getTarget();
        TargetServerRtpArrivalResolver resolver = new TargetServerRtpArrivalResolver("s2",
            GlobalEntryRules.parse("", new String[0]), new FixedFinder(new TeleportTarget("s2", 0, 1, 2, 3, 0, 0)), service);
        TransferTicket ticket = ticket("HOME", "s2");
        assertEquals(original.getX(), resolver.resolve(ticket, null).getX(), 0.0001D);
        resolver.afterSuccessfulRestore(ticket, original);
        assertEquals(null, service.requestId);
    }

    private static TransferTicket ticket(String kind, String targetServer) {
        Instant now = Instant.now();
        return new TransferTicket("ticket", "rtp-request", "player-uuid", "Player", kind, "lobby",
            new TeleportTarget(targetServer, 7, 0.5D, 80, 0.5D, 0, 0), TransferTicketStatus.DISPATCHED,
            "waiting", now, now.plusSeconds(30), now);
    }

    private static final class FixedFinder implements com.jsirgalaxybase.modules.servertools.port.RtpCandidateFinder {
        private final TeleportTarget result;
        private FixedFinder(TeleportTarget result) { this.result = result; }
        @Override public TeleportTarget findSafeTarget(TargetServerRtpProfile profile, EntityPlayerMP player) { return result; }
    }

    private static final class RecordingService extends PlayerTeleportService {
        private String requestId, playerUuid, sourceServerId;
        private TeleportTarget target;
        private RecordingService() { super(null, null); }
        @Override public RandomTeleportRecord recordResolvedTargetServerRandomTeleport(String requestId, String playerUuid,
            String sourceServerId, TeleportTarget target, Instant now) {
            this.requestId = requestId; this.playerUuid = playerUuid; this.sourceServerId = sourceServerId; this.target = target;
            return new RandomTeleportRecord(1L, requestId, playerUuid, sourceServerId, target, now);
        }
    }
}

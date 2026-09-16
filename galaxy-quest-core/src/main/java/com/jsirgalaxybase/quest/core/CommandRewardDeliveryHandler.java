package com.jsirgalaxybase.quest.core;

import java.util.Objects;

/** BQ variable semantics behind an explicit allowlist and entitlement-idempotent execution port. */
public final class CommandRewardDeliveryHandler implements RewardDeliveryHandler {
    private final CommandRewardPolicy policy;
    private final CommandRewardExecutor executor;
    private final ParticipantDisplayResolver participants;

    public CommandRewardDeliveryHandler(CommandRewardPolicy policy, CommandRewardExecutor executor,
        ParticipantDisplayResolver participants) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.participants = Objects.requireNonNull(participants, "participants");
    }
    @Override public String getTypeId() { return "bq_standard:command"; }
    @Override public RewardDeliveryOutcome deliver(RewardDeliveryLease lease) {
        if (lease.getParticipantId().getType() != ParticipantType.PLAYER) return RewardDeliveryOutcome.failed(
            "unsupported-participant:" + lease.getParticipantId().getType(), false);
        String template = lease.getReward().getParameters().get("command");
        if (template == null) return RewardDeliveryOutcome.failed("invalid-reward:command", false);
        String name = participants.displayName(lease.getParticipantId());
        if (name == null || name.isEmpty()) return RewardDeliveryOutcome.deferred("participant-unavailable");
        String command = normalize(template.replace("VAR_NAME", name)
            .replace("VAR_UUID", lease.getParticipantId().getId().toString()));
        boolean viaPlayer = bool(lease.getReward().getParameters().get("viaPlayer"), false);
        if (!policy.allows(command, viaPlayer)) return RewardDeliveryOutcome.failed("command-not-allowlisted", false);
        return executor.execute(new CommandRewardInvocation(lease.getEntitlementKey(), lease.getParticipantId(),
            command, viaPlayer));
    }
    static String normalize(String value) {
        String command = value == null ? "" : value.trim();
        while (command.startsWith("/")) command = command.substring(1).trim();
        return command;
    }
    private static boolean bool(String value, boolean fallback) {
        return value == null ? fallback : "1".equals(value) || Boolean.parseBoolean(value);
    }
}

package com.jsirgalaxybase.modules.servertools.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

/** Server-only global Hub and target-server RTP configuration. */
public final class GlobalEntryRules {

    private final TeleportTarget hubTarget;
    private final Map<String, TargetServerRtpProfile> rtpProfiles;

    private GlobalEntryRules(TeleportTarget hubTarget, Map<String, TargetServerRtpProfile> rtpProfiles) {
        this.hubTarget = hubTarget;
        this.rtpProfiles = Collections.unmodifiableMap(new LinkedHashMap<String, TargetServerRtpProfile>(rtpProfiles));
    }

    public static GlobalEntryRules parse(String hubDefinition, String[] rtpDefinitions) {
        TeleportTarget hub = text(hubDefinition).isEmpty() ? null : parseHub(hubDefinition);
        Map<String, TargetServerRtpProfile> profiles = new LinkedHashMap<String, TargetServerRtpProfile>();
        if (rtpDefinitions != null) for (String definition : rtpDefinitions) {
            if (text(definition).isEmpty()) throw new IllegalArgumentException("targetServerRtpProfiles must not contain blank entries");
            TargetServerRtpProfile profile = parseRtp(definition);
            if (profiles.put(profile.getServerId(), profile) != null) {
                throw new IllegalArgumentException("duplicate target-server RTP profile: " + profile.getServerId());
            }
        }
        return new GlobalEntryRules(hub, profiles);
    }

    public Optional<TeleportTarget> getHubTarget() { return Optional.ofNullable(hubTarget); }
    public Optional<TargetServerRtpProfile> findRtpProfile(String serverId) {
        return Optional.ofNullable(rtpProfiles.get(text(serverId)));
    }
    public Map<String, TargetServerRtpProfile> getRtpProfiles() { return rtpProfiles; }

    private static TeleportTarget parseHub(String definition) {
        String[] values = split(definition, 7, "globalHubTarget");
        return new TeleportTarget(values[0], integer(values[1], "hub dimension"), number(values[2], "hub x"),
            number(values[3], "hub y"), number(values[4], "hub z"), (float) number(values[5], "hub yaw"),
            (float) number(values[6], "hub pitch"));
    }

    private static TargetServerRtpProfile parseRtp(String definition) {
        String[] values = split(definition, 7, "targetServerRtpProfiles");
        return new TargetServerRtpProfile(values[0], integer(values[1], "rtp dimension"), number(values[2], "rtp centerX"),
            number(values[3], "rtp fallbackY"), number(values[4], "rtp centerZ"), number(values[5], "rtp minDistance"),
            number(values[6], "rtp maxDistance"));
    }

    private static String[] split(String definition, int size, String field) {
        String[] values = text(definition).split("\\|", -1);
        if (values.length != size) throw new IllegalArgumentException(field + " requires " + size + " pipe-separated values");
        for (int i = 0; i < values.length; i++) {
            values[i] = text(values[i]);
            if (values[i].isEmpty()) throw new IllegalArgumentException(field + " contains a blank value");
        }
        return values;
    }

    private static int integer(String text, String field) {
        try { return Integer.parseInt(text); } catch (NumberFormatException exception) { throw new IllegalArgumentException("invalid " + field, exception); }
    }
    private static double number(String text, String field) {
        try { double value = Double.parseDouble(text); if (Double.isNaN(value) || Double.isInfinite(value)) throw new NumberFormatException(); return value; }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("invalid " + field, exception); }
    }
    private static String text(String value) { return value == null ? "" : value.trim(); }
}

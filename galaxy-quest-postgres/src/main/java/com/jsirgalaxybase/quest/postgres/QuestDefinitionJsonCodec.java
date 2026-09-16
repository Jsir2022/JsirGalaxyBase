package com.jsirgalaxybase.quest.postgres;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestBehavior;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestParticipantScope;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.QuestVisibility;

final class QuestDefinitionJsonCodec {
    private final Gson gson = new Gson();

    Encoded encode(QuestDefinition definition) {
        JsonObject root = new JsonObject();
        root.addProperty("id", definition.getId().toString());
        root.addProperty("version", definition.getVersion());
        root.addProperty("name", definition.getName());
        root.addProperty("description", definition.getDescription());
        root.addProperty("prerequisiteLogic", definition.getPrerequisiteLogic().name());
        root.addProperty("taskLogic", definition.getTaskLogic().name());
        root.addProperty("repeatCooldownMillis", definition.getRepeatPolicy().getCooldownMillis());
        root.addProperty("repeatRelative", definition.getRepeatPolicy().isRelative());
        root.add("behavior", behavior(definition.getBehavior()));
        JsonArray prerequisites = new JsonArray();
        List<UUID> sorted = new ArrayList<UUID>(definition.getPrerequisites());
        Collections.sort(sorted, Comparator.comparing(UUID::toString));
        for (UUID prerequisite : sorted) prerequisites.add(prerequisite.toString());
        root.add("prerequisites", prerequisites);
        JsonArray tasks = new JsonArray();
        for (TaskDefinition task : definition.getTasks()) {
            JsonObject value = new JsonObject();
            value.addProperty("key", task.getKey());
            value.addProperty("type", task.getTypeId());
            value.addProperty("optional", task.isOptional());
            value.add("parameters", gson.toJsonTree(new TreeMap<String, String>(task.getParameters())));
            tasks.add(value);
        }
        root.add("tasks", tasks);
        JsonArray rewards = new JsonArray();
        for (RewardDefinition reward : definition.getRewards()) {
            JsonObject value = new JsonObject();
            value.addProperty("key", reward.getKey());
            value.addProperty("type", reward.getTypeId());
            value.add("parameters", gson.toJsonTree(new TreeMap<String, String>(reward.getParameters())));
            rewards.add(value);
        }
        root.add("rewards", rewards);
        String json = gson.toJson(root);
        return new Encoded(json, sha256(json));
    }

    QuestDefinition decode(String json) {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        Set<UUID> prerequisites = new LinkedHashSet<UUID>();
        for (JsonElement value : root.getAsJsonArray("prerequisites")) prerequisites.add(UUID.fromString(value.getAsString()));
        List<TaskDefinition> tasks = new ArrayList<TaskDefinition>();
        for (JsonElement element : root.getAsJsonArray("tasks")) {
            JsonObject value = element.getAsJsonObject();
            tasks.add(new TaskDefinition(value.get("key").getAsString(), value.get("type").getAsString(),
                value.get("optional").getAsBoolean(), stringMap(value.getAsJsonObject("parameters"))));
        }
        List<RewardDefinition> rewards = new ArrayList<RewardDefinition>();
        for (JsonElement element : root.getAsJsonArray("rewards")) {
            JsonObject value = element.getAsJsonObject();
            rewards.add(new RewardDefinition(value.get("key").getAsString(), value.get("type").getAsString(),
                stringMap(value.getAsJsonObject("parameters"))));
        }
        long cooldown = root.get("repeatCooldownMillis").getAsLong();
        boolean relative = !root.has("repeatRelative") || root.get("repeatRelative").getAsBoolean();
        return new QuestDefinition(UUID.fromString(root.get("id").getAsString()), root.get("version").getAsInt(),
            root.get("name").getAsString(), root.get("description").getAsString(),
            QuestLogic.valueOf(root.get("prerequisiteLogic").getAsString()),
            QuestLogic.valueOf(root.get("taskLogic").getAsString()), prerequisites, tasks, rewards,
            cooldown < 0 ? RepeatPolicy.never() : RepeatPolicy.after(cooldown, relative),
            root.has("behavior") ? behavior(root.getAsJsonObject("behavior")) : QuestBehavior.DEFAULT);
    }

    private static JsonObject behavior(QuestBehavior value) {
        JsonObject object = new JsonObject();
        object.addProperty("visibility", value.getVisibility().name());
        object.addProperty("iconReference", value.getIconReference());
        object.addProperty("main", value.isMain());
        object.addProperty("silent", value.isSilent());
        object.addProperty("autoClaim", value.isAutoClaim());
        object.addProperty("progressWhileLocked", value.isProgressWhileLocked());
        object.addProperty("simultaneous", value.isSimultaneous());
        object.addProperty("global", value.isGlobal());
        object.addProperty("globalShare", value.isGlobalShare());
        object.addProperty("updateSound", value.getUpdateSound());
        object.addProperty("completeSound", value.getCompleteSound());
        object.addProperty("participantScope", value.getParticipantScope().name());
        return object;
    }

    private static QuestBehavior behavior(JsonObject object) {
        return QuestBehavior.builder()
            .visibility(QuestVisibility.valueOf(string(object, "visibility", QuestVisibility.NORMAL.name())))
            .iconReference(string(object, "iconReference", ""))
            .main(bool(object, "main")).silent(bool(object, "silent")).autoClaim(bool(object, "autoClaim"))
            .progressWhileLocked(bool(object, "progressWhileLocked")).simultaneous(bool(object, "simultaneous"))
            .global(bool(object, "global")).globalShare(bool(object, "globalShare"))
            .updateSound(string(object, "updateSound", "")).completeSound(string(object, "completeSound", ""))
            .participantScope(QuestParticipantScope.valueOf(string(object, "participantScope",
                QuestParticipantScope.PLAYER.name())))
            .build();
    }

    private static boolean bool(JsonObject object, String key) {
        return object.has(key) && object.get(key).getAsBoolean();
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) ? object.get(key).getAsString() : fallback;
    }

    private static Map<String, String> stringMap(JsonObject object) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) result.put(entry.getKey(), entry.getValue().getAsString());
        return result;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte b : digest) result.append(String.format("%02x", b & 0xff));
            return result.toString();
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    static final class Encoded {
        final String json;
        final String hash;
        Encoded(String json, String hash) { this.json = json; this.hash = hash; }
    }
}

package com.jsirgalaxybase.quest.bqimport;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class BqPlayerProgressImporter {
    private final Gson gson = new Gson();

    public BqImportedPlayerProgress read(Path source) throws IOException {
        UUID playerId = playerId(source);
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            root = gson.fromJson(reader, JsonObject.class);
        }
        List<BqImportedQuestProgress> result = new ArrayList<BqImportedQuestProgress>();
        for (JsonObject quest : BqTypedJson.indexedObjects(root, "questProgress")) {
            UUID questId = BqQuestDefinitionImporter.uuid(quest, "questIDHigh", "questIDLow");
            boolean completed = false;
            boolean claimed = false;
            long timestamp = 0L;
            for (JsonObject completion : BqTypedJson.indexedObjects(quest, "completed")) {
                if (playerId.toString().equals(BqTypedJson.string(completion, "uuid", ""))) {
                    completed = true;
                    claimed = BqTypedJson.number(completion, "claimed", 0L) != 0L;
                    timestamp = BqTypedJson.number(completion, "timestamp", 0L);
                    break;
                }
            }
            JsonObject rawTasks = BqTypedJson.object(quest, "tasks");
            result.add(new BqImportedQuestProgress(questId, completed, claimed, timestamp,
                tasks(playerId, rawTasks), gson.toJson(rawTasks)));
        }
        return new BqImportedPlayerProgress(playerId, source, result);
    }

    private List<BqImportedTaskProgress> tasks(UUID playerId, JsonObject rawTasks) {
        List<BqImportedTaskProgress> result = new ArrayList<BqImportedTaskProgress>();
        int fallbackIndex = 0;
        for (JsonObject task : BqTypedJson.indexedObjects(rawTasks)) {
            int index = (int) BqTypedJson.number(task, "index", fallbackIndex++);
            String typeId = BqTypedJson.string(task, "taskID", "betterquesting:unknown");
            boolean complete = containsUuid(BqTypedJson.object(task, "completeUsers"), playerId);
            List<Long> values = new ArrayList<Long>();
            JsonObject playerProgress = findPlayerProgress(task, playerId);
            if (playerProgress != null) {
                JsonElement data = BqTypedJson.find(playerProgress, "data");
                if (data != null && data.isJsonArray()) {
                    JsonArray array = data.getAsJsonArray();
                    for (JsonElement value : array) if (value.isJsonPrimitive()) values.add(value.getAsLong());
                } else {
                    JsonElement value = BqTypedJson.find(playerProgress, "value");
                    if (value != null && value.isJsonPrimitive()) values.add(value.getAsLong());
                }
            }
            result.add(new BqImportedTaskProgress("task-" + index, typeId, complete, values, gson.toJson(task)));
        }
        return result;
    }

    private static JsonObject findPlayerProgress(JsonObject task, UUID playerId) {
        for (JsonObject progress : BqTypedJson.indexedObjects(task, "userProgress")) {
            if (playerId.toString().equals(BqTypedJson.string(progress, "uuid", ""))) return progress;
        }
        return null;
    }

    private static boolean containsUuid(JsonObject values, UUID playerId) {
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            JsonElement value = entry.getValue();
            if (value != null && value.isJsonPrimitive() && playerId.toString().equals(value.getAsString())) return true;
        }
        return false;
    }

    private static UUID playerId(Path source) {
        String name = source.getFileName().toString();
        if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
        return UUID.fromString(name);
    }
}

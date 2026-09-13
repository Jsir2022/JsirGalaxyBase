package com.jsirgalaxybase.quest.postgres;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jsirgalaxybase.quest.core.QuestChapterDefinition;
import com.jsirgalaxybase.quest.core.QuestChapterEntry;
import com.jsirgalaxybase.quest.core.QuestVisibility;

final class QuestChapterJsonCodec {
    private final Gson gson = new Gson();

    Encoded encode(QuestChapterDefinition definition) {
        JsonObject root = new JsonObject();
        root.addProperty("id", definition.getId().toString());
        root.addProperty("version", definition.getVersion());
        root.addProperty("name", definition.getName());
        root.addProperty("description", definition.getDescription());
        root.addProperty("iconReference", definition.getIconReference());
        root.addProperty("backgroundReference", definition.getBackgroundReference());
        root.addProperty("backgroundSize", definition.getBackgroundSize());
        root.addProperty("visibility", definition.getVisibility().name());
        JsonArray entries = new JsonArray();
        for (QuestChapterEntry entry : definition.getEntries()) {
            JsonObject value = new JsonObject();
            value.addProperty("questId", entry.getQuestId().toString());
            value.addProperty("x", entry.getX());
            value.addProperty("y", entry.getY());
            value.addProperty("width", entry.getWidth());
            value.addProperty("height", entry.getHeight());
            entries.add(value);
        }
        root.add("entries", entries);
        String json = gson.toJson(root);
        return new Encoded(json, sha256(json));
    }

    QuestChapterDefinition decode(String json) {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        List<QuestChapterEntry> entries = new ArrayList<QuestChapterEntry>();
        for (JsonElement element : root.getAsJsonArray("entries")) {
            JsonObject value = element.getAsJsonObject();
            entries.add(new QuestChapterEntry(UUID.fromString(value.get("questId").getAsString()),
                value.get("x").getAsInt(), value.get("y").getAsInt(), value.get("width").getAsInt(),
                value.get("height").getAsInt()));
        }
        return new QuestChapterDefinition(UUID.fromString(root.get("id").getAsString()),
            root.get("version").getAsInt(), root.get("name").getAsString(), root.get("description").getAsString(),
            root.get("iconReference").getAsString(), root.get("backgroundReference").getAsString(),
            root.has("backgroundSize") ? root.get("backgroundSize").getAsInt() : 256,
            root.has("visibility") ? QuestVisibility.valueOf(root.get("visibility").getAsString())
                : QuestVisibility.NORMAL,
            entries);
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

package com.jsirgalaxybase.quest.bqimport;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;

/** Offline evidence only. Relations describe snapshots, never an automatic merge or grant policy. */
public final class BqProgressMigrationPreview {
    public String report(BqMigrationManifest manifest,
        Map<String, List<BqImportedPlayerProgress>> sources) {
        Map<UUID, QuestDefinition> definitions = new TreeMap<UUID, QuestDefinition>();
        for (BqImportedQuest quest : manifest.getQuests()) {
            if (definitions.put(quest.getDefinition().getId(), quest.getDefinition()) != null)
                throw new IllegalArgumentException("duplicate definition UUID");
        }
        Map<String, Map<String, BqImportedQuestProgress>> candidates =
            new TreeMap<String, Map<String, BqImportedQuestProgress>>();
        for (Map.Entry<String, List<BqImportedPlayerProgress>> source : new TreeMap<String,
            List<BqImportedPlayerProgress>>(sources).entrySet()) {
            if (!source.getKey().matches("[A-Za-z0-9_-]{1,64}"))
                throw new IllegalArgumentException("invalid source ID");
            java.util.Set<UUID> players = new java.util.HashSet<UUID>();
            for (BqImportedPlayerProgress player : source.getValue()) {
                if (!players.add(player.getPlayerId())) throw new IllegalArgumentException("duplicate player");
                for (BqImportedQuestProgress quest : player.getQuests()) {
                    String key = player.getPlayerId() + "/" + quest.getQuestId();
                    Map<String, BqImportedQuestProgress> values = candidates.get(key);
                    if (values == null) { values = new TreeMap<String, BqImportedQuestProgress>(); candidates.put(key, values); }
                    if (values.put(source.getKey(), quest) != null)
                        throw new IllegalArgumentException("duplicate quest progress");
                }
            }
        }
        JsonObject root = new JsonObject();
        root.addProperty("schema", "bq-progress-preview-1");
        root.addProperty("definitionHash", manifest.getSourceHash());
        root.addProperty("definitionsApplicable", manifest.isApplicable());
        root.addProperty("applyAllowed", false);
        JsonArray entries = new JsonArray();
        for (Map.Entry<String, Map<String, BqImportedQuestProgress>> entry : candidates.entrySet()) {
            JsonObject row = new JsonObject();
            String[] identity = entry.getKey().split("/");
            row.addProperty("player", identity[0]); row.addProperty("quest", identity[1]);
            QuestDefinition definition = definitions.get(UUID.fromString(identity[1]));
            JsonArray issues = new JsonArray();
            if (!manifest.isApplicable()) issues.add("DEFINITION_MANIFEST_INVALID");
            if (definition == null) issues.add("MISSING_DEFINITION");
            else {
                row.addProperty("definitionVersion", definition.getVersion());
                if (definition.getRepeatPolicy().isRepeatable()) issues.add("REPEAT_CYCLE_UNRESOLVED");
                for (RewardDefinition reward : definition.getRewards()) {
                    if ("bq_standard:choice".equals(reward.getTypeId())) {
                        issues.add("CHOICE_SELECTION_UNMAPPED"); break;
                    }
                }
            }
            JsonArray snapshots = new JsonArray();
            JsonArray relations = new JsonArray();
            for (Map.Entry<String, BqImportedQuestProgress> snapshot : entry.getValue().entrySet()) {
                BqImportedQuestProgress progress = snapshot.getValue();
                JsonObject value = new JsonObject(); value.addProperty("source", snapshot.getKey());
                value.addProperty("completed", progress.isCompleted()); value.addProperty("claimed", progress.isClaimed());
                value.addProperty("timestamp", progress.getTimestamp());
                if (progress.isClaimed() && !progress.isCompleted()) issues.add("CLAIM_WITHOUT_COMPLETION");
                if (progress.getTimestamp() < 0) issues.add("INVALID_TIMESTAMP");
                if (definition != null) {
                    try {
                        Map<String, TaskProgress> mapped = new BqProgressMapper().toCore(definition, progress);
                        JsonObject tasks = new JsonObject();
                        for (Map.Entry<String, TaskProgress> task : mapped.entrySet()) {
                            JsonObject t = new JsonObject(); t.addProperty("complete", task.getValue().isComplete());
                            JsonArray vector = new JsonArray();
                            for (Long count : task.getValue().getValues()) vector.add(count);
                            t.add("values", vector); tasks.add(task.getKey(), t);
                        }
                        value.add("mappedTasks", tasks);
                    } catch (BqProgressShapeException invalid) { issues.add("PROGRESS_SHAPE_INVALID:" + snapshot.getKey()); }
                }
                snapshots.add(value);
                for (Map.Entry<String, BqImportedQuestProgress> other : entry.getValue().entrySet()) {
                    if (snapshot.getKey().compareTo(other.getKey()) >= 0) continue;
                    JsonObject relation = new JsonObject(); relation.addProperty("left", snapshot.getKey());
                    relation.addProperty("right", other.getKey());
                    relation.addProperty("relation", new BqProgressConflictAnalyzer().relation(progress, other.getValue()).name());
                    relations.add(relation);
                }
            }
            row.add("issues", issues); row.add("snapshots", snapshots); row.add("relations", relations);
            row.addProperty("status", issues.size() > 0 ? "BLOCKED" : "REVIEW_ONLY"); entries.add(row);
        }
        root.add("entries", entries);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n";
    }
}

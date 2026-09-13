package com.jsirgalaxybase.quest.bqimport;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestBehavior;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestVisibility;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;

public final class BqQuestDefinitionImporter {
    private final Gson gson = new Gson();
    private final BqStandardCodec standardCodec = new BqStandardCodec(gson);

    public BqImportedQuest read(Path source) throws IOException {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            root = gson.fromJson(reader, JsonObject.class);
        }
        UUID id = uuid(root, "questIDHigh", "questIDLow");
        JsonObject properties = BqTypedJson.object(BqTypedJson.object(root, "properties"), "betterquesting");
        String name = BqTypedJson.string(properties, "name", source.getFileName().toString());
        String description = BqTypedJson.string(properties, "desc", "");
        QuestLogic prerequisiteLogic = logic(BqTypedJson.string(properties, "questLogic", "AND"));
        QuestLogic taskLogic = logic(BqTypedJson.string(properties, "taskLogic", "AND"));
        Set<UUID> prerequisites = new LinkedHashSet<UUID>();
        for (JsonObject prerequisite : BqTypedJson.indexedObjects(root, "preRequisites")) {
            prerequisites.add(uuid(prerequisite, "questIDHigh", "questIDLow"));
        }

        List<TaskDefinition> tasks = new ArrayList<TaskDefinition>();
        List<BqStandardTaskSpec> taskSpecs = new ArrayList<BqStandardTaskSpec>();
        for (JsonObject task : BqTypedJson.indexedObjects(root, "tasks")) {
            BqStandardTaskSpec spec = standardCodec.task(task, tasks.size());
            taskSpecs.add(spec);
            String type = spec.getTypeId();
            Map<String, String> parameters = standardCodec.taskParameters(spec, gson.toJson(task));
            tasks.add(new TaskDefinition(spec.getKey(), type, type.contains("optional"), parameters));
        }

        List<RewardDefinition> rewards = new ArrayList<RewardDefinition>();
        List<BqStandardRewardSpec> rewardSpecs = new ArrayList<BqStandardRewardSpec>();
        for (JsonObject reward : BqTypedJson.indexedObjects(root, "rewards")) {
            BqStandardRewardSpec spec = standardCodec.reward(reward, rewards.size());
            rewardSpecs.add(spec);
            rewards.add(new RewardDefinition(spec.getKey(), spec.getTypeId(),
                standardCodec.rewardParameters(spec, gson.toJson(reward))));
        }

        long repeatTime = BqTypedJson.number(properties, "repeatTime", -1L);
        RepeatPolicy repeat = repeatTime < 0 ? RepeatPolicy.never() : RepeatPolicy.after(ticksToMillis(repeatTime),
            BqTypedJson.bool(properties, "repeat_relative", true));
        JsonObject icon = BqTypedJson.object(properties, "icon");
        QuestBehavior behavior = QuestBehavior.builder()
            .visibility(visibility(BqTypedJson.string(properties, "visibility", "NORMAL")))
            .iconReference(icon.size() == 0 ? "" : gson.toJson(icon))
            .main(BqTypedJson.bool(properties, "isMain", false))
            .silent(BqTypedJson.bool(properties, "isSilent", false))
            .autoClaim(BqTypedJson.bool(properties, "autoClaim", false))
            .progressWhileLocked(BqTypedJson.bool(properties, "lockedProgress", false))
            .simultaneous(BqTypedJson.bool(properties, "simultaneous", false))
            .global(BqTypedJson.bool(properties, "isGlobal", false))
            .globalShare(BqTypedJson.bool(properties, "globalShare", false))
            .updateSound(BqTypedJson.string(properties, "snd_update", ""))
            .completeSound(BqTypedJson.string(properties, "snd_complete", ""))
            .build();
        QuestDefinition definition = new QuestDefinition(id, 1, name, description, prerequisiteLogic, taskLogic,
            prerequisites, tasks, rewards, repeat, behavior);
        return new BqImportedQuest(definition, source, gson.toJson(root), taskSpecs, rewardSpecs);
    }

    public List<BqImportedQuest> readDirectory(Path defaultQuests) throws IOException {
        Path quests = defaultQuests.resolve("Quests");
        if (!Files.isDirectory(quests)) throw new IOException("Missing BetterQuesting Quests directory: " + quests);
        List<Path> files = new ArrayList<Path>();
        try (java.util.stream.Stream<Path> stream = Files.walk(quests)) {
            stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                .forEach(files::add);
        }
        Collections.sort(files);
        List<BqImportedQuest> result = new ArrayList<BqImportedQuest>();
        for (Path file : files) result.add(read(file));
        return result;
    }

    private static QuestLogic logic(String raw) {
        return "OR".equalsIgnoreCase(raw) ? QuestLogic.OR : QuestLogic.AND;
    }

    private static QuestVisibility visibility(String raw) {
        try { return QuestVisibility.valueOf(raw.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException invalid) { return QuestVisibility.NORMAL; }
    }

    static UUID uuid(JsonObject object, String highName, String lowName) {
        return new UUID(BqTypedJson.number(object, highName, 0L), BqTypedJson.number(object, lowName, 0L));
    }

    private static long ticksToMillis(long ticks) {
        if (ticks > Long.MAX_VALUE / 50L) return Long.MAX_VALUE;
        return ticks * 50L;
    }
}

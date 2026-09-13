package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class BqStandardCodecTest {
    private final BqStandardCodec codec = new BqStandardCodec(new Gson());

    @Test
    public void compatibilityMatrixCoversEveryTypeUsedByCurrentGtnhQuestBook() {
        assertEquals(new LinkedHashSet<String>(Arrays.asList(
            "bq_standard:block_break", "bq_standard:checkbox", "bq_standard:crafting", "bq_standard:fluid",
            "bq_standard:hunt", "bq_standard:interact_entity", "bq_standard:interact_item",
            "bq_standard:location", "bq_standard:meeting", "bq_standard:optional_retrieval",
            "bq_standard:retrieval", "bq_standard:scoreboard", "bq_standard:xp")),
            BqStandardCodec.supportedTaskTypes());
        assertEquals(new LinkedHashSet<String>(Arrays.asList(
            "bq_standard:choice", "bq_standard:command", "bq_standard:item", "bq_standard:questcompletion",
            "bq_standard:scoreboard", "bq_standard:xp")),
            BqStandardCodec.supportedRewardTypes());
    }

    @Test
    public void decodesRetrievalItemsAndMatchingOptions() {
        JsonObject task = new Gson().fromJson("{\"index:3\":2,\"taskID:8\":\"bq_standard:retrieval\","
            + "\"partialMatch:1\":1,\"ignoreNBT:1\":0,\"requiredItems:9\":{\"0:10\":{"
            + "\"id:8\":\"gregtech:item\",\"Damage:2\":7,\"Count:3\":64,\"OreDict:8\":\"ingotIron\","
            + "\"tag:10\":{\"grade:8\":\"pure\"}}}}", JsonObject.class);
        BqStandardTaskSpec spec = codec.task(task, 0);
        assertEquals("task-2", spec.getKey());
        assertEquals("1", spec.getOptions().get("partialMatch"));
        assertEquals(1, spec.getItems().size());
        assertEquals("gregtech:item", spec.getItems().get(0).getRegistryName());
        assertEquals(64, spec.getItems().get(0).getCount());
        assertTrue(spec.getItems().get(0).getTagJson().contains("grade:8"));
        Map<String, String> parameters = codec.taskParameters(spec, new Gson().toJson(task));
        assertEquals("pure", com.jsirgalaxybase.quest.core.PortableNbt.decode(
            parameters.get("item.0.nbtPortable")).getFields().get("grade").getScalar());
    }

    @Test
    public void decodesFluidBlockAndQuestCompletionReward() {
        JsonObject fluid = new Gson().fromJson("{\"taskID:8\":\"bq_standard:fluid\",\"requiredFluids:9\":{"
            + "\"0:10\":{\"FluidName:8\":\"water\",\"Amount:3\":1000}}}", JsonObject.class);
        assertEquals(1000, codec.task(fluid, 0).getFluids().get(0).getAmount());
        JsonObject block = new Gson().fromJson("{\"taskID:8\":\"bq_standard:block_break\",\"blocks:9\":{"
            + "\"0:10\":{\"blockID:8\":\"minecraft:log\",\"meta:3\":-1,\"amount:3\":3}}}",
            JsonObject.class);
        assertEquals(-1, codec.task(block, 0).getBlocks().get(0).getMeta());
        JsonObject reward = new Gson().fromJson("{\"rewardID:8\":\"bq_standard:questcompletion\","
            + "\"questID:8\":\"AAAAAAAAAAAAAAAAAAAABw==\"}", JsonObject.class);
        assertEquals(new UUID(0L, 7L).toString(), codec.reward(reward, 0).getOptions().get("questUuid"));
    }

    @Test
    public void mapsEntityLocationChoiceAndXpSemanticsWithoutInventingValues() {
        Gson gson = new Gson();
        JsonObject hunt = gson.fromJson("{\"taskID:8\":\"bq_standard:hunt\",\"target:8\":\"Zombie\","
            + "\"required:3\":10,\"damageType:8\":\"player\",\"ignoreNBT:1\":0,\"subtypes:1\":1,"
            + "\"targetNBT:10\":{\"CustomName:8\":\"Boss\"}}", JsonObject.class);
        BqStandardTaskSpec huntSpec = codec.task(hunt, 0);
        Map<String, String> huntParams = codec.taskParameters(huntSpec, gson.toJson(hunt));
        assertEquals("scalar", huntParams.get("progressMode"));
        assertEquals("10", huntParams.get("target"));
        assertEquals("Zombie", huntParams.get("subject"));
        assertTrue(huntParams.get("targetNBT.raw").contains("CustomName:8"));
        assertEquals("Boss", com.jsirgalaxybase.quest.core.PortableNbt.decode(
            huntParams.get("targetNBT.portable")).getFields().get("CustomName").getScalar());

        JsonObject location = gson.fromJson("{\"taskID:8\":\"bq_standard:location\",\"dimension:3\":1,"
            + "\"posX:3\":4,\"posY:3\":70,\"posZ:3\":-8,\"range:3\":12,\"invert:1\":1,"
            + "\"taxiCabDist:1\":1}", JsonObject.class);
        Map<String, String> locationParams = codec.taskParameters(codec.task(location, 0), gson.toJson(location));
        assertEquals("boolean", locationParams.get("progressMode"));
        assertEquals("galaxy:location_observed", locationParams.get("factType"));
        assertEquals("1", locationParams.get("dimension"));
        assertEquals("-8", locationParams.get("posZ"));
        assertEquals("1", locationParams.get("invert"));

        JsonObject meeting = gson.fromJson("{\"taskID:8\":\"bq_standard:meeting\",\"target:8\":\"Villager\","
            + "\"range:3\":4,\"amount:3\":2}", JsonObject.class);
        Map<String, String> meetingParams = codec.taskParameters(codec.task(meeting, 1), gson.toJson(meeting));
        assertEquals("galaxy:nearby_entities_observed", meetingParams.get("factType"));
        assertEquals("Villager", meetingParams.get("subject"));
        assertEquals("2", meetingParams.get("amount"));

        JsonObject choice = gson.fromJson("{\"rewardID:8\":\"bq_standard:choice\",\"choices:9\":{"
            + "\"0:10\":{\"id:8\":\"minecraft:diamond\",\"Count:3\":3}}}", JsonObject.class);
        Map<String, String> choiceParams = codec.rewardParameters(codec.reward(choice, 0), gson.toJson(choice));
        assertEquals("player-required", choiceParams.get("selectionMode"));
        assertEquals("minecraft:diamond", choiceParams.get("item.0.registryName"));
        assertEquals("3", choiceParams.get("item.0.amount"));

        JsonObject xp = gson.fromJson("{\"rewardID:8\":\"bq_standard:xp\",\"amount:3\":10,"
            + "\"isLevels:1\":1}", JsonObject.class);
        Map<String, String> xpParams = codec.rewardParameters(codec.reward(xp, 0), gson.toJson(xp));
        assertEquals("10", xpParams.get("amount"));
        assertEquals("levels", xpParams.get("xpUnit"));
    }

    @Test public void mapsLessCommonStandardTasksAndRewardsWithoutDroppingTheirRawConfiguration() {
        Gson gson = new Gson();
        JsonObject interact = gson.fromJson("{\"taskID:8\":\"bq_standard:interact_entity\","
            + "\"targetID:8\":\"Villager\",\"requiredUses:3\":4,\"onHit:1\":1,"
            + "\"targetNBT:10\":{\"Profession:3\":2}}", JsonObject.class);
        Map<String, String> interactParams = codec.taskParameters(codec.task(interact, 0), gson.toJson(interact));
        assertEquals("galaxy:entity_interacted", interactParams.get("factType"));
        assertEquals("Villager", interactParams.get("subject"));
        assertEquals("4", interactParams.get("target"));
        assertTrue(interactParams.get("bq.raw").contains("Profession:3"));
        assertEquals("2", com.jsirgalaxybase.quest.core.PortableNbt.decode(
            interactParams.get("targetNBT.portable")).getFields().get("Profession").getScalar());

        JsonObject scoreboard = gson.fromJson("{\"taskID:8\":\"bq_standard:scoreboard\","
            + "\"scoreName:8\":\"Machine Score\",\"target:3\":-2,\"operation:8\":\"LESS_OR_EQUAL\"}",
            JsonObject.class);
        Map<String, String> scoreParams = codec.taskParameters(codec.task(scoreboard, 0), gson.toJson(scoreboard));
        assertEquals("galaxy:score_observed", scoreParams.get("factType"));
        assertEquals("Machine_Score", scoreParams.get("subject"));
        assertEquals("LESS_OR_EQUAL", scoreParams.get("operation"));
        assertEquals("-2", scoreParams.get("scoreTarget"));
        assertEquals("1", scoreParams.get("target"));

        JsonObject xpTask = gson.fromJson("{\"taskID:8\":\"bq_standard:xp\",\"amount:3\":16,"
            + "\"isLevels:1\":1,\"consume:1\":0}", JsonObject.class);
        Map<String, String> xpParams = codec.taskParameters(codec.task(xpTask, 0), gson.toJson(xpTask));
        assertEquals("272", xpParams.get("target"));
        assertEquals("levels", xpParams.get("xpUnit"));
        assertEquals("galaxy:xp_observed", xpParams.get("factType"));

        JsonObject command = gson.fromJson("{\"rewardID:8\":\"bq_standard:command\","
            + "\"command:8\":\"say VAR_NAME\",\"viaPlayer:1\":0}", JsonObject.class);
        Map<String, String> commandParams = codec.rewardParameters(codec.reward(command, 0), gson.toJson(command));
        assertEquals("say VAR_NAME", commandParams.get("command"));
        assertTrue(commandParams.get("bq.raw").contains("viaPlayer:1"));
    }
}

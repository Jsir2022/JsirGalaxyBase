package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.bqimport.BqQuestDefinitionImporter;
import com.jsirgalaxybase.quest.core.ConsumptionIntent;
import com.jsirgalaxybase.quest.core.ConsumptionRequest;
import com.jsirgalaxybase.quest.core.ConsumptionRequestAuthorizer;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestCompletionQuery;
import com.jsirgalaxybase.quest.core.QuestEngine;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestRuntimeRepository;
import com.jsirgalaxybase.quest.core.QuestDefinitionLifecycle;
import com.jsirgalaxybase.quest.core.RewardEntitlement;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.RegistryNamespaced;
import net.minecraft.util.RegistrySimple;

/** Verifies the imported BQ consume contract survives authorization and reaches the Minecraft item matcher. */
public class BqConsumingRetrievalImportTest {
    @Test
    public void importedTypedRetrievalIsAuthorizedAndMatchedByMinecraftConsumptionAdapter() throws Exception {
        String registryName = "galaxybase:bq_retrieval_test_" + UUID.randomUUID().toString().replace("-", "");
        Path source = Files.createTempFile("bq-consuming-retrieval", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":17,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Synthetic retrieval\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\","
            + "\"consume:1\":1,\"autoConsume:1\":0,\"ignoreNBT:1\":0,\"partialMatch:1\":1,"
            + "\"requiredItems:9\":{\"0:10\":{\"id:8\":\"" + registryName + "\","
            + "\"Damage:2\":2,\"Count:3\":5,\"tag:10\":{\"grade:3\":2}}}}},\"rewards:9\":{}}";
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));
        try {
            final com.jsirgalaxybase.quest.core.QuestDefinition definition =
                new BqQuestDefinitionImporter().read(source).getDefinition();
            QuestRuntimeRepository progress = new EmptyProgressRepository();
            QuestCompletionQuery completions = participant -> Collections.<UUID>emptySet();
            ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(
                () -> Collections.singletonList(new StoredQuestDefinition(definition,
                    QuestDefinitionLifecycle.PUBLISHED, "synthetic", 1L)), progress, completions, new QuestEngine());
            UUID player = UUID.randomUUID();
            ConsumptionRequest request = authorizer.authorize(new ConsumptionIntent("retrieval-submit", "s2",
                player, definition.getId(), "task-0", 10L), 10L);

            assertEquals("item", request.getKind());
            assertEquals(Arrays.asList(5L), request.getAmounts());
            assertEquals("5", request.getResourceParameters().get("item.0.amount"));
            assertEquals(registryName, request.getResourceParameters().get("item.0.registryName"));
            assertEquals("false", request.getResourceParameters().get("ignoreNBT"));
            assertEquals("true", request.getResourceParameters().get("partialMatch"));

            Item item = registerItem(registryName);
            ItemStack held = new ItemStack(item, 5, 2);
            NBTTagCompound actualTag = new NBTTagCompound();
            actualTag.setInteger("grade", 2);
            actualTag.setString("additionalActualData", "allowed-by-partial-match");
            held.setTagCompound(actualTag);
            MinecraftPlayerConsumptionPort adapter = new MinecraftPlayerConsumptionPort(id -> null, playerState -> {});

            MinecraftPlayerConsumptionPort.ItemPlan plan = adapter.planItems(Collections.singletonList(held), request);

            assertNotNull(plan);
            assertEquals(Collections.singletonList(5L), plan.appliedAmounts);
            assertNull(plan.inventory.get(0));
            assertEquals("the inventory snapshot must not be mutated while planning", 5, held.stackSize);
        } finally {
            Files.deleteIfExists(source);
        }
    }

    private static Item registerItem(String registryName) throws Exception {
        Item item = new Item();
        item.setHasSubtypes(true);
        Field identityMapField = RegistryNamespaced.class.getDeclaredField("underlyingIntegerMap");
        identityMapField.setAccessible(true);
        ObjectIntIdentityMap identityMap = (ObjectIntIdentityMap) identityMapField.get(Item.itemRegistry);
        int id = Short.MAX_VALUE;
        while (identityMap.func_148744_b(id)) id--;
        identityMap.func_148746_a(item, id);
        Field registryObjectsField = RegistrySimple.class.getDeclaredField("registryObjects");
        registryObjectsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Object> registryObjects = (Map<Object, Object>) registryObjectsField.get(Item.itemRegistry);
        registryObjects.put(registryName, item);
        return item;
    }

    private static final class EmptyProgressRepository implements QuestRuntimeRepository {
        @Override public Optional<QuestProgressSnapshot> findProgress(ParticipantId participantId, UUID questId,
            int definitionVersion) { return Optional.empty(); }
        @Override public boolean hasPendingConsumptionSubmission(ParticipantId participantId, UUID questId,
            int definitionVersion, int cycle) { return false; }
        @Override public boolean hasClaimableRewardEntitlement(ParticipantId participantId, UUID questId,
            int definitionVersion, int cycle) { return false; }
        @Override public long findRepeatCycleAnchorMillis(ParticipantId participantId, UUID questId,
            int definitionVersion, int cycle, long fallbackCompletedAt) { return fallbackCompletedAt; }
        @Override public boolean recordFactIfAbsent(com.jsirgalaxybase.quest.core.GameplayFact fact) { return false; }
        @Override public void saveProgress(QuestProgressSnapshot progress) { }
        @Override public void insertEntitlementsIfAbsent(Set<RewardEntitlement> entitlements) { }
    }
}

package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Assume;
import org.junit.After;
import org.junit.Test;

import com.jsirgalaxybase.quest.bqimport.BqImportedQuest;
import com.jsirgalaxybase.quest.bqimport.BqMigrationManifest;
import com.jsirgalaxybase.quest.bqimport.BqMigrationPlanner;
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
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.RewardEntitlement;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.RegistryNamespaced;
import net.minecraft.util.RegistrySimple;

/** Verifies the imported BQ consume contract survives authorization and reaches the Minecraft item matcher. */
public class BqConsumingRetrievalImportTest {
    private final Map<String, Item> testRegisteredItems = new LinkedHashMap<String, Item>();

    @After
    public void removeItemsRegisteredByTest() throws Exception {
        if (testRegisteredItems.isEmpty()) return;
        Field registryObjectsField = RegistrySimple.class.getDeclaredField("registryObjects");
        registryObjectsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Object> registryObjects = (Map<Object, Object>) registryObjectsField.get(Item.itemRegistry);
        Field identityMapField = RegistryNamespaced.class.getDeclaredField("underlyingIntegerMap");
        identityMapField.setAccessible(true);
        ObjectIntIdentityMap identityMap = (ObjectIntIdentityMap) identityMapField.get(Item.itemRegistry);
        Field idsField = ObjectIntIdentityMap.class.getDeclaredField("field_148749_a");
        idsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Integer> ids = (Map<Object, Integer>) idsField.get(identityMap);
        Field objectsField = ObjectIntIdentityMap.class.getDeclaredField("field_148748_b");
        objectsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> objects = (List<Object>) objectsField.get(identityMap);
        for (Map.Entry<String, Item> entry : testRegisteredItems.entrySet()) {
            registryObjects.remove(entry.getKey(), entry.getValue());
            Integer id = ids.remove(entry.getValue());
            if (id != null && id.intValue() >= 0 && id.intValue() < objects.size()
                && objects.get(id.intValue()) == entry.getValue()) objects.set(id.intValue(), null);
        }
        testRegisteredItems.clear();
    }

    @Test
    public void pinnedDefaultQuestsConsumingRetrievalProfilesReachMinecraftInventoryMatcher() throws Exception {
        String source = System.getenv("BQ_TEST_DEFAULT_QUESTS");
        Assume.assumeTrue("read-only pinned DefaultQuests mount is required", source != null && !source.trim().isEmpty());
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(Paths.get(source));
        assertEquals("9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc",
            manifest.getSourceHash());

        Map<String, BqImportedQuest> representatives = new LinkedHashMap<String, BqImportedQuest>();
        Map<String, TaskDefinition> representativeTasks = new LinkedHashMap<String, TaskDefinition>();
        for (BqImportedQuest imported : manifest.getQuests()) {
            for (TaskDefinition task : imported.getDefinition().getTasks()) {
                if (!"bq_standard:retrieval".equals(task.getTypeId())
                    || !"true".equals(task.getParameters().get("consume"))
                    || "true".equals(task.getParameters().get("autoConsume"))) continue;
                String profile = task.getParameters().get("ignoreNBT") + ";"
                    + task.getParameters().get("partialMatch");
                if (!representatives.containsKey(profile)) {
                    representatives.put(profile, imported);
                    representativeTasks.put(profile, task);
                }
            }
        }
        assertEquals(new LinkedHashSet<String>(Arrays.asList("false;true", "true;true")),
            representatives.keySet());

        for (Map.Entry<String, BqImportedQuest> entry : representatives.entrySet()) {
            BqImportedQuest imported = entry.getValue();
            QuestDefinition definition = imported.getDefinition();
            TaskDefinition task = representativeTasks.get(entry.getKey());
            StoredQuestDefinition published = new StoredQuestDefinition(definition,
                QuestDefinitionLifecycle.PUBLISHED, "pinned-source-profile", 1L);
            ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(
                () -> Collections.singletonList(published), new EmptyProgressRepository(),
                participant -> definition.getPrerequisites(), new QuestEngine());
            UUID player = UUID.randomUUID();
            ConsumptionRequest request = authorizer.authorize(new ConsumptionIntent(
                "pinned-source:" + task.getKey(), "s2", player, definition.getId(), task.getKey(), 100L), 100L);

            assertEquals(task.getParameters().get("ignoreNBT"), request.getResourceParameters().get("ignoreNBT"));
            assertEquals("true", request.getResourceParameters().get("partialMatch"));
            List<ItemStack> inventory = new java.util.ArrayList<ItemStack>();
            for (int requirement = 0; requirement < request.getAmounts().size(); requirement++) {
                String prefix = "item." + requirement + ".";
                String registryName = request.getResourceParameters().get(prefix + "registryName");
                int amount = Math.toIntExact(request.getAmounts().get(requirement));
                int meta = Integer.parseInt(request.getResourceParameters().get(prefix + "meta"));
                ItemStack stack = new ItemStack(registerOrFindItem(registryName), amount, meta);
                NBTTagCompound required = BqTypedNbtCodec.compound(request.getResourceParameters().get(prefix + "nbt"));
                if (!required.hasNoTags() || "true".equals(request.getResourceParameters().get("ignoreNBT"))) {
                    required.setString("n8_profile_extra", "partial-match");
                    stack.setTagCompound(required);
                }
                inventory.add(stack);
            }

            List<ItemStack> original = new java.util.ArrayList<ItemStack>();
            for (ItemStack stack : inventory) original.add(stack.copy());
            MinecraftPlayerConsumptionPort adapter = new MinecraftPlayerConsumptionPort(id -> null, playerState -> {});
            MinecraftPlayerConsumptionPort.ItemPlan plan = adapter.planItems(inventory, request);

            assertNotNull("the fixed source profile must match a real ItemStack inventory snapshot", plan);
            assertEquals(request.getAmounts(), plan.appliedAmounts);
            for (int slot = 0; slot < inventory.size(); slot++) {
                assertEquals("planning must leave the observed inventory snapshot untouched",
                    original.get(slot).stackSize, inventory.get(slot).stackSize);
            }
        }
    }

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

    private Item registerItem(String registryName) throws Exception {
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
        // The object registry is process-global; test cleanup removes entries created here.
        // Record through the current test instance so fixed source names do not leak to other tests.
        testRegisteredItems.put(registryName, item);
        return item;
    }

    @SuppressWarnings("unchecked")
    private Item registerOrFindItem(String registryName) throws Exception {
        Field registryObjectsField = RegistrySimple.class.getDeclaredField("registryObjects");
        registryObjectsField.setAccessible(true);
        Map<Object, Object> registryObjects = (Map<Object, Object>) registryObjectsField.get(Item.itemRegistry);
        Object existing = registryObjects.get(registryName);
        return existing instanceof Item ? (Item) existing : registerItem(registryName);
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

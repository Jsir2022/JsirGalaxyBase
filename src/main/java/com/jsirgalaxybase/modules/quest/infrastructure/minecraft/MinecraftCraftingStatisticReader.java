package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.OptionalLong;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.CraftingStatisticReader;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;

/** Reads vanilla's server-owned lifetime craft counter for an online authenticated player. */
public final class MinecraftCraftingStatisticReader implements CraftingStatisticReader {
    private final QuestPlayerResolver players;

    public MinecraftCraftingStatisticReader(QuestPlayerResolver players) {
        if (players == null) throw new IllegalArgumentException("player resolver is required");
        this.players = players;
    }

    @Override
    public OptionalLong craftedCount(UUID authenticatedPlayerId, String registryName) {
        if (authenticatedPlayerId == null || registryName == null || registryName.trim().isEmpty()) {
            return OptionalLong.empty();
        }
        EntityPlayerMP player = players.findOnline(authenticatedPlayerId);
        if (player == null) return OptionalLong.empty();
        Object value = Item.itemRegistry.getObject(registryName);
        if (!(value instanceof Item)) return OptionalLong.empty();
        int itemId = Item.getIdFromItem((Item) value);
        if (itemId < 0 || itemId >= StatList.objectCraftStats.length) return OptionalLong.empty();
        StatBase statistic = StatList.objectCraftStats[itemId];
        if (statistic == null) return OptionalLong.empty();
        int count = player.func_147099_x().writeStat(statistic);
        return count <= 0 ? OptionalLong.empty() : OptionalLong.of(count);
    }
}

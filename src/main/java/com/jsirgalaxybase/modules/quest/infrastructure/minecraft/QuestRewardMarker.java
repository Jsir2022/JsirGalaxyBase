package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

final class QuestRewardMarker {
    static final String ROOT = "GalaxyQuestRewards";

    private QuestRewardMarker() {}

    static NBTTagCompound persisted(EntityPlayer player) {
        NBTTagCompound entity = player.getEntityData();
        if (!entity.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            entity.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return entity.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    static String key(String entitlementKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(entitlementKey.getBytes(Charset.forName("UTF-8")));
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}

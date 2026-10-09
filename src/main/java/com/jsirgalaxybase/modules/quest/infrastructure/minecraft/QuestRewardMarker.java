package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

final class QuestRewardMarker {
    static final String ROOT = "GalaxyQuestRewards";
    static final String PROGRESS_ROOT = "GalaxyQuestRewardProgressV1";

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

    static int granted(NBTTagCompound progress, int partIndex) {
        String key = Integer.toString(partIndex);
        if (!progress.hasKey(key)) return 0;
        if (!progress.hasKey(key, 3)) throw new IllegalArgumentException("invalid reward progress tag " + key);
        int count = progress.getInteger(key);
        if (count < 0) throw new IllegalArgumentException("negative reward progress " + key);
        return count;
    }

    static void setGranted(NBTTagCompound progress, int partIndex, int count) {
        if (partIndex < 0 || count < 0) throw new IllegalArgumentException("reward progress must be non-negative");
        String key = Integer.toString(partIndex);
        if (count == 0) progress.removeTag(key);
        else progress.setInteger(key, count);
    }
}

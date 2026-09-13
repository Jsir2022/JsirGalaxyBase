package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

/** Decodes BetterQuesting's typed JSON NBT and applies its reward string substitutions. */
final class BqTypedNbtCodec {
    private static final Field TAG_LIST = tagListField();

    private BqTypedNbtCodec() {}

    static NBTTagCompound compound(String json) {
        if (json == null || json.trim().isEmpty() || "{}".equals(json.trim())) return new NBTTagCompound();
        JsonElement parsed = new JsonParser().parse(json);
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("item NBT is not a compound");
        return (NBTTagCompound) tag(parsed, 10);
    }

    static NBTTagCompound replacePlayerVariables(NBTTagCompound source, String name, String uuid) {
        if (source == null) return null;
        return (NBTTagCompound) replace(source.copy(), "VAR_NAME", name, "VAR_UUID", uuid);
    }

    @SuppressWarnings("unchecked")
    private static NBTBase replace(NBTBase value, String key1, String replacement1, String key2,
        String replacement2) {
        if (value instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) value;
            for (String key : (java.util.Set<String>) compound.func_150296_c()) {
                compound.setTag(key, replace(compound.getTag(key), key1, replacement1, key2, replacement2));
            }
        } else if (value instanceof NBTTagList) {
            List<NBTBase> values = listValues((NBTTagList) value);
            for (int i = 0; i < values.size(); i++) {
                values.set(i, replace(values.get(i), key1, replacement1, key2, replacement2));
            }
        } else if (value instanceof NBTTagString) {
            String text = ((NBTTagString) value).func_150285_a_();
            return new NBTTagString(text.replace(key1, replacement1).replace(key2, replacement2));
        }
        return value;
    }

    private static NBTBase tag(JsonElement value, int type) {
        switch (type) {
            case 1: return new NBTTagByte(value.getAsByte());
            case 2: return new NBTTagShort(value.getAsShort());
            case 3: return new NBTTagInt(value.getAsInt());
            case 4: return new NBTTagLong(value.getAsLong());
            case 5: return new NBTTagFloat(value.getAsFloat());
            case 6: return new NBTTagDouble(value.getAsDouble());
            case 7:
                byte[] bytes = new byte[value.getAsJsonArray().size()];
                for (int i = 0; i < bytes.length; i++) bytes[i] = value.getAsJsonArray().get(i).getAsByte();
                return new NBTTagByteArray(bytes);
            case 8: return new NBTTagString(value.getAsString());
            case 9:
                NBTTagList list = new NBTTagList();
                for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                    list.appendTag(tag(entry.getValue(), suffixType(entry.getKey())));
                }
                return list;
            case 10:
                NBTTagCompound compound = new NBTTagCompound();
                for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                    compound.setTag(logicalName(entry.getKey()), tag(entry.getValue(), suffixType(entry.getKey())));
                }
                return compound;
            case 11:
                int[] ints = new int[value.getAsJsonArray().size()];
                for (int i = 0; i < ints.length; i++) ints[i] = value.getAsJsonArray().get(i).getAsInt();
                return new NBTTagIntArray(ints);
            default: throw new IllegalArgumentException("unsupported NBT type " + type);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<NBTBase> listValues(NBTTagList list) {
        try {
            return (List<NBTBase>) TAG_LIST.get(list);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("cannot inspect NBT list", failure);
        }
    }

    private static Field tagListField() {
        for (String name : new String[] { "tagList", "field_74747_a" }) {
            try {
                Field field = NBTTagList.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new ExceptionInInitializerError("NBTTagList backing field not found");
    }

    private static int suffixType(String key) {
        int split = key.lastIndexOf(':');
        if (split < 0) throw new IllegalArgumentException("NBT key has no type suffix: " + key);
        return Integer.parseInt(key.substring(split + 1));
    }

    private static String logicalName(String key) {
        int split = key.lastIndexOf(':');
        return split < 0 ? key : key.substring(0, split);
    }
}

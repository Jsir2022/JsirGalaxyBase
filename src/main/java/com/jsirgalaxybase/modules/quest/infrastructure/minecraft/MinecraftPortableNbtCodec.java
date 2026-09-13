package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jsirgalaxybase.quest.core.PortableNbt;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/** Minecraft 1.7.10 NBT adapter. The quest core remains free of platform classes. */
public final class MinecraftPortableNbtCodec {
    private static final java.lang.reflect.Field LIST_VALUES = listValuesField();
    private MinecraftPortableNbtCodec() {}

    public static String encode(NBTBase tag) { return convert(tag).encode(); }

    @SuppressWarnings("unchecked")
    static PortableNbt convert(NBTBase tag) {
        if (tag == null) return PortableNbt.compound(java.util.Collections.<String, PortableNbt>emptyMap());
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            Map<String, PortableNbt> fields = new LinkedHashMap<String, PortableNbt>();
            for (String key : (Set<String>) compound.func_150296_c()) fields.put(key, convert(compound.getTag(key)));
            return PortableNbt.compound(fields);
        }
        if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tag;
            List<PortableNbt> values = new ArrayList<PortableNbt>();
            for (NBTBase value : listValues(list)) values.add(convert(value));
            return PortableNbt.list(values);
        }
        if (tag instanceof NBTTagByteArray) {
            List<PortableNbt> values = new ArrayList<PortableNbt>();
            for (byte value : ((NBTTagByteArray) tag).func_150292_c()) values.add(PortableNbt.number(Byte.toString(value)));
            return PortableNbt.byteArray(values);
        }
        if (tag instanceof NBTTagIntArray) {
            List<PortableNbt> values = new ArrayList<PortableNbt>();
            for (int value : ((NBTTagIntArray) tag).func_150302_c()) values.add(PortableNbt.number(Integer.toString(value)));
            return PortableNbt.intArray(values);
        }
        if (tag instanceof NBTTagString) return PortableNbt.string(((NBTTagString) tag).func_150285_a_());
        if (tag instanceof NBTBase.NBTPrimitive) {
            NBTBase.NBTPrimitive number = (NBTBase.NBTPrimitive) tag;
            switch (tag.getId()) {
                case 1: return PortableNbt.number(Byte.toString(number.func_150290_f()));
                case 2: return PortableNbt.number(Short.toString(number.func_150289_e()));
                case 3: return PortableNbt.number(Integer.toString(number.func_150287_d()));
                case 4: return PortableNbt.number(Long.toString(number.func_150291_c()));
                case 5: return PortableNbt.number(Float.toString(number.func_150288_h()));
                case 6: return PortableNbt.number(Double.toString(number.func_150286_g()));
                default: break;
            }
        }
        throw new IllegalArgumentException("Unsupported Minecraft NBT type " + tag.getId());
    }

    private static java.lang.reflect.Field listValuesField() {
        for (String name : new String[] { "tagList", "field_74747_a" }) {
            try {
                java.lang.reflect.Field field = NBTTagList.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException ignored) { }
        }
        throw new ExceptionInInitializerError("Unable to locate NBTTagList backing values");
    }

    @SuppressWarnings("unchecked")
    private static List<NBTBase> listValues(NBTTagList list) {
        try { return (List<NBTBase>) LIST_VALUES.get(list); }
        catch (IllegalAccessException inaccessible) { throw new IllegalStateException("Unable to read NBTTagList", inaccessible); }
    }
}

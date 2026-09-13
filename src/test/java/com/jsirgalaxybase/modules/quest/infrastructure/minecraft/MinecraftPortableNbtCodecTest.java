package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.*;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.PortableNbt;
import com.jsirgalaxybase.quest.core.PortableNbtMatcher;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class MinecraftPortableNbtCodecTest {
    @Test public void preservesLongPrecisionAndMatchesNumbersAcrossNbtWidths() {
        NBTTagCompound actual = new NBTTagCompound();
        actual.setLong("large", 9007199254740993L);
        actual.setInteger("grade", 2);
        PortableNbt decoded = PortableNbt.decode(MinecraftPortableNbtCodec.encode(actual));
        assertEquals("9007199254740993", decoded.getFields().get("large").getScalar());
        assertEquals("2", decoded.getFields().get("grade").getScalar());
    }

    @Test public void canonicalCompoundAndUnorderedListMatchImportedShape() {
        NBTTagCompound actual = new NBTTagCompound();
        NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString("second"));
        lore.appendTag(new NBTTagString("first"));
        actual.setTag("lore", lore);
        java.util.Map<String, PortableNbt> fields = new java.util.LinkedHashMap<String, PortableNbt>();
        fields.put("lore", PortableNbt.list(java.util.Arrays.asList(PortableNbt.string("first"))));
        assertTrue(PortableNbtMatcher.matches(PortableNbt.compound(fields).encode(),
            MinecraftPortableNbtCodec.encode(actual), true));
    }
}

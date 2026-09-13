package com.jsirgalaxybase.client.ui2.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.render.UiLayer;

public class NativeItemBridgeTest {
    @Test public void stableExternalRegionReturnsDefensiveHoveredStack(){
        NativeItemBridge bridge=new NativeItemBridge();
        DrawList list=new DrawList().add(DrawCommand.externalRegion(UiLayer.NATIVE,new UiRect(10,20,16,16),"cell:0"));
        ItemStack source=new ItemStack(new Item().setUnlocalizedName("bridge_test"),7);
        ItemStack hovered=bridge.hovered(list,Collections.singletonMap("cell:0",source),12,22);
        assertNotNull(hovered);assertEquals(7,hovered.stackSize);assertNotSame(source,hovered);
        assertNull(bridge.hovered(list,Collections.singletonMap("cell:0",source),2,2));
    }
}

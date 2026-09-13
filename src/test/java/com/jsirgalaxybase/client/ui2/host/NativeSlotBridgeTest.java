package com.jsirgalaxybase.client.ui2.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;

public class NativeSlotBridgeTest {
    @Test public void extractsStableRegionsAndMovesOverflowSlotsOutOfHitArea(){NativeSlotBridge bridge=new NativeSlotBridge();DrawList list=new DrawList().add(DrawCommand.externalRegion(200,new UiRect(10,20,36,20),"vault:region"));assertEquals(new UiRect(10,20,36,20),bridge.regions(list).get("vault:region"));InventoryBasic inventory=new InventoryBasic("test",false,3);Slot a=new Slot(inventory,0,0,0),b=new Slot(inventory,1,0,0),c=new Slot(inventory,2,0,0);bridge.layoutGrid(Arrays.asList(a,b,c),new UiRect(10,20,36,20),2,0,0);assertEquals(11,a.xDisplayPosition);assertEquals(22,a.yDisplayPosition);assertEquals(29,b.xDisplayPosition);assertEquals(22,b.yDisplayPosition);assertTrue(c.xDisplayPosition<0);}
}

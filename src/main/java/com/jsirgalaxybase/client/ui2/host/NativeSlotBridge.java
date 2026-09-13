package com.jsirgalaxybase.client.ui2.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.inventory.Slot;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.SlotGridGeometry;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;

public final class NativeSlotBridge {
    public Map<String, UiRect> regions(DrawList list) {
        Map<String,UiRect> result=new LinkedHashMap<String,UiRect>();
        if(list!=null)for(DrawCommand command:list.ordered())if(command.getKind()==DrawCommand.Kind.EXTERNAL_REGION)result.put(command.getExternalId(),command.getBounds());
        return Collections.unmodifiableMap(result);
    }
    public void layoutGrid(List<Slot> slots, UiRect region, int columns, int guiLeft, int guiTop) {
        if(slots==null)return;
        if(region==null||columns<1){hide(slots);return;}
        List<UiRect> cells=SlotGridGeometry.cells(region,columns,slots.size());
        for(int index=0;index<slots.size();index++){
            Slot slot=slots.get(index);
            if(index<cells.size()){UiRect cell=cells.get(index);slot.xDisplayPosition=cell.getX()-guiLeft;slot.yDisplayPosition=cell.getY()-guiTop;}
            else{slot.xDisplayPosition=-10000;slot.yDisplayPosition=-10000;}
        }
    }
    public void hide(List<Slot> slots){if(slots!=null)for(Slot slot:slots){slot.xDisplayPosition=-10000;slot.yDisplayPosition=-10000;}}
    @SuppressWarnings("unchecked") public static List<Slot> slots(Object raw){return raw instanceof List<?>?new ArrayList<Slot>((List<Slot>)raw):Collections.<Slot>emptyList();}
}

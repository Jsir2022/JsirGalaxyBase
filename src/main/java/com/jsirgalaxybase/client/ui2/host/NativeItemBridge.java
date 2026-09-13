package com.jsirgalaxybase.client.ui2.host;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;

/** Renders platform ItemStacks into stable UI2 external regions without leaking them into ui2-core. */
public final class NativeItemBridge {
    public Map<String, UiRect> regions(DrawList list) {
        Map<String,UiRect> result=new LinkedHashMap<String,UiRect>();
        if(list!=null)for(DrawCommand command:list.ordered())if(command.getKind()==DrawCommand.Kind.EXTERNAL_REGION)
            result.put(command.getExternalId(),command.getBounds());
        return Collections.unmodifiableMap(result);
    }

    public void draw(DrawList list, Map<String, ItemStack> items, RenderItem renderer,
        FontRenderer font, TextureManager textures) {
        if (list == null || items == null || items.isEmpty() || renderer == null) return;
        Map<String,UiRect> regions=regions(list);
        for(Map.Entry<String,ItemStack> entry:items.entrySet()){
            UiRect bounds=regions.get(entry.getKey()); ItemStack stack=entry.getValue();
            if(bounds==null||stack==null||stack.getItem()==null)continue;
            int x=bounds.getX()+Math.max(0,(bounds.getWidth()-16)/2);
            int y=bounds.getY()+Math.max(0,(bounds.getHeight()-16)/2);
            renderer.renderItemAndEffectIntoGUI(font,textures,stack,x,y);
        }
    }

    public ItemStack hovered(DrawList list, Map<String, ItemStack> items, int x, int y) {
        if(items==null||items.isEmpty())return null;
        Map<String,UiRect> regions=regions(list);
        for(Map.Entry<String,ItemStack> entry:items.entrySet()){
            UiRect bounds=regions.get(entry.getKey());
            if(bounds!=null&&bounds.contains(x,y)&&entry.getValue()!=null)return entry.getValue().copy();
        }
        return null;
    }
}

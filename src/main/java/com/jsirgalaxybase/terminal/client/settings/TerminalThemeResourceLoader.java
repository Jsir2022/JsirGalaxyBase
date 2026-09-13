package com.jsirgalaxybase.terminal.client.settings;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.ui2.theme.UiTheme;

/** Loads data-only theme manifests from active Minecraft resource packs. */
final class TerminalThemeResourceLoader {
    private static final ResourceLocation INDEX=new ResourceLocation(GalaxyBase.MODID,"ui2/themes/index.json");
    private TerminalThemeResourceLoader(){}
    static void reload(IResourceManager resources,TerminalThemeRegistry registry){
        registry.resetBuiltIns();if(resources==null)return;
        try{
            List<IResource> indexes=resources.getAllResources(INDEX);
            for(IResource index:indexes){
                JsonObject root=read(index);JsonElement themes=root.get("themes");if(themes==null||!themes.isJsonArray())continue;
                for(JsonElement element:themes.getAsJsonArray()){
                    if(!element.isJsonObject())continue;
                    try{loadEntry(resources,registry,element.getAsJsonObject());}
                    catch(RuntimeException exception){GalaxyBase.LOG.warn("Ignoring invalid UI2 theme entry",exception);}
                }
            }
        }catch(Exception exception){GalaxyBase.LOG.debug("No external UI2 theme index loaded: {}",exception.toString());}
    }
    private static void loadEntry(IResourceManager resources,TerminalThemeRegistry registry,JsonObject entry){
        String id=required(entry,"id"),label=required(entry,"label"),base=required(entry,"base"),path=required(entry,"resource");
        TerminalThemeRegistry.Entry baseEntry=registry.resolveEntry(base);
        if(!baseEntry.getId().equals(base))throw new IllegalArgumentException("unknown base theme "+base);
        JsonObject definition;
        try{definition=read(resources,new ResourceLocation(GalaxyBase.MODID,path));}catch(Exception exception){throw new IllegalArgumentException("cannot read theme "+path,exception);}
        UiTheme source=baseEntry.getTheme();
        UiTheme theme=new UiTheme(map(definition,"colors",source.getColors(),true),map(definition,"spacing",source.getSpacing(),false),
            map(definition,"radii",source.getRadii(),false),map(definition,"typography",source.getTypography(),false),
            map(definition,"controls",source.getControls(),false),map(definition,"motion",source.getMotion(),false),
            map(definition,"elevation",source.getElevation(),false));
        registry.registerResource(id,label,theme);
    }
    private static Map<String,Integer> map(JsonObject root,String name,Map<String,Integer> fallback,boolean colors){
        Map<String,Integer> result=new LinkedHashMap<String,Integer>(fallback);JsonElement raw=root.get(name);if(raw==null)return result;
        if(!raw.isJsonObject())throw new IllegalArgumentException(name+" must be an object");
        for(Map.Entry<String,JsonElement> entry:raw.getAsJsonObject().entrySet()){
            int value=colors?parseColor(entry.getValue().getAsString()):entry.getValue().getAsInt();
            if(!colors&&value<0)throw new IllegalArgumentException(name+" values must be non-negative");
            result.put(entry.getKey(),value);
        }
        return result;
    }
    private static int parseColor(String value){String text=value==null?"":value.trim();if(text.startsWith("#"))text=text.substring(1);if(text.length()==6)text="FF"+text;if(text.length()!=8)throw new IllegalArgumentException("color must be #RRGGBB or #AARRGGBB");return(int)Long.parseLong(text,16);}
    private static String required(JsonObject object,String name){JsonElement value=object.get(name);if(value==null||value.getAsString().trim().isEmpty())throw new IllegalArgumentException("missing "+name);return value.getAsString().trim();}
    private static JsonObject read(IResourceManager resources,ResourceLocation location)throws Exception{
        return read(resources.getResource(location));
    }
    private static JsonObject read(IResource resource)throws Exception{
        InputStream stream=resource.getInputStream();
        try{return new JsonParser().parse(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();}
        finally{stream.close();}
    }
}

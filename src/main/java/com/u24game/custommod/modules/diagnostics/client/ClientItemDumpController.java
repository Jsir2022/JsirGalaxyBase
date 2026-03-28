package com.u24game.custommod.modules.diagnostics.client;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

import com.u24game.custommod.CustomMod;
import com.u24game.custommod.config.ModConfiguration;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientItemDumpController {

    private static final int MIN_TICKS_BEFORE_DUMP = 40;
    private static final int MAX_TICKS_WAITING_FOR_NEI = 20 * 60;
    private static final int WAIT_LOG_INTERVAL = 200;

    private final ModConfiguration configuration;
    private final boolean neiCallbackRegistered;
    private int tickCounter = 0;
    private boolean finished = false;

    public ClientItemDumpController(ModConfiguration configuration) {
        this.configuration = configuration;
        neiCallbackRegistered = registerNeiLoadCallback();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) {
            return;
        }

        tickCounter++;
        if (tickCounter < MIN_TICKS_BEFORE_DUMP) {
            return;
        }

        if (tickCounter == MIN_TICKS_BEFORE_DUMP) {
            CustomMod.LOG.info(
                "Item dump controller armed (neiLoaded={}, neiCallbackRegistered={})",
                Loader.isModLoaded("NotEnoughItems"),
                neiCallbackRegistered);
        }

        maybeExport("client-tick");
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!finished && tickCounter >= MIN_TICKS_BEFORE_DUMP) {
            maybeExport("gui-open");
        }
    }

    private void maybeExport(String trigger) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }

        final boolean neiLoaded = Loader.isModLoaded("NotEnoughItems");
        final boolean neiReady = !neiLoaded || ItemDumpExporter.isNeiItemListReady();
        final boolean timedOutWaitingForNei = tickCounter >= MAX_TICKS_WAITING_FOR_NEI;

        if (!neiReady && !timedOutWaitingForNei) {
            if (tickCounter % WAIT_LOG_INTERVAL == 0) {
                CustomMod.LOG.info("Waiting for NEI item list before export (trigger={}, tick={})", trigger, tickCounter);
            }
            return;
        }

        export(trigger, neiReady);
    }

    @SuppressWarnings("unchecked")
    private boolean registerNeiLoadCallback() {
        if (!Loader.isModLoaded("NotEnoughItems")) {
            return false;
        }

        try {
            final Class<?> itemListClass = Class.forName("codechicken.nei.ItemList");
            final Class<?> callbackClass = Class.forName("codechicken.nei.ItemList$ItemsLoadedCallback");
            final Object callbacks = itemListClass.getField("loadCallbacks").get(null);
            if (!(callbacks instanceof List)) {
                return false;
            }

            final InvocationHandler handler = new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    if ("itemsLoaded".equals(method.getName())) {
                        export("nei-callback", true);
                    } else if ("toString".equals(method.getName())) {
                        return "CustomModItemDumpControllerCallback";
                    }

                    return null;
                }
            };

            final Object callback = Proxy.newProxyInstance(
                callbackClass.getClassLoader(),
                new Class<?>[] { callbackClass },
                handler);
            ((List<Object>) callbacks).add(callback);
            CustomMod.LOG.info("Registered NEI itemsLoaded callback");
            return true;
        } catch (Exception e) {
            CustomMod.LOG.warn("Failed to register NEI itemsLoaded callback", e);
            return false;
        }
    }

    private synchronized void export(String trigger, boolean includeNei) {
        if (finished) {
            return;
        }

        finished = true;
        FMLCommonHandler.instance().bus().unregister(this);
        MinecraftForge.EVENT_BUS.unregister(this);

        try {
            final File outputDirectory = ItemDumpExporter.exportAll(
                configuration.getMinecraftDirectory(),
                configuration.getItemDumpDirectory(),
                includeNei);
            CustomMod.LOG.info("Item dump finished via {}: {}", trigger, outputDirectory.getAbsolutePath());
        } catch (Exception e) {
            CustomMod.LOG.error("Failed to export item dumps via " + trigger, e);
        }
    }
}
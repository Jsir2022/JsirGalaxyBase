// SPDX-License-Identifier: LGPL-3.0-or-later
// Terrain sampling derived from GTNewHorizons/ServerUtilities ThreadReloadChunkSelector.
package com.jsirgalaxybase.terminal.client.component;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

public final class ClientLandTerrainCache {

    public static final ClientLandTerrainCache INSTANCE = new ClientLandTerrainCache(256);
    private static final int TILE_PIXELS = 16;

    private final int maxEntries;
    private final AtomicLong generation = new AtomicLong();
    private final Set<TileKey> pending = java.util.Collections.newSetFromMap(
        new ConcurrentHashMap<TileKey, Boolean>());
    private final ConcurrentLinkedQueue<TilePixels> completed = new ConcurrentLinkedQueue<TilePixels>();
    private final LinkedHashMap<TileKey, TileTexture> textures =
        new LinkedHashMap<TileKey, TileTexture>(32, 0.75F, true);
    private final ExecutorService worker;

    ClientLandTerrainCache(int maxEntries) {
        this.maxEntries = Math.max(16, maxEntries);
        worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "JGB-LandTerrain");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    public ResourceLocation textureFor(final WorldClient world, final int chunkX, final int chunkZ) {
        drainCompleted(world);
        if (world == null) return null;
        final TileKey key = TileKey.of(world, chunkX, chunkZ);
        TileTexture texture = textures.get(key);
        if (texture != null) return texture.location;
        IChunkProvider provider = world.getChunkProvider();
        if (provider == null || !provider.chunkExists(chunkX, chunkZ) || !pending.add(key)) return null;
        final Chunk chunk = provider.provideChunk(chunkX, chunkZ);
        if (chunk == null) {
            pending.remove(key);
            return null;
        }
        final long requestedGeneration = generation.get();
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    int[] pixels = sample(chunk);
                    if (generation.get() == requestedGeneration) completed.add(new TilePixels(key, pixels));
                } finally {
                    pending.remove(key);
                }
            }
        });
        return null;
    }

    public void invalidateVisible(WorldClient world, int centerX, int centerZ, int radius) {
        if (world == null) return;
        Iterator<Map.Entry<TileKey, TileTexture>> iterator = textures.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TileKey, TileTexture> entry = iterator.next();
            TileKey key = entry.getKey();
            if (key.matchesWorld(world) && Math.abs((long) key.chunkX - centerX) <= radius
                && Math.abs((long) key.chunkZ - centerZ) <= radius) {
                delete(entry.getValue());
                iterator.remove();
            }
        }
    }

    public void clearAll() {
        generation.incrementAndGet();
        pending.clear();
        completed.clear();
        for (TileTexture texture : textures.values()) delete(texture);
        textures.clear();
    }

    int size() { return textures.size(); }

    private void drainCompleted(WorldClient currentWorld) {
        TilePixels result;
        while ((result = completed.poll()) != null) {
            if (currentWorld == null || !result.key.matchesWorld(currentWorld)) continue;
            DynamicTexture dynamic = new DynamicTexture(TILE_PIXELS, TILE_PIXELS);
            System.arraycopy(result.pixels, 0, dynamic.getTextureData(), 0, result.pixels.length);
            dynamic.updateDynamicTexture();
            ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("jgb_land_" + result.key.chunkX + "_" + result.key.chunkZ, dynamic);
            textures.put(result.key, new TileTexture(dynamic, location));
        }
        evictOverflow();
    }

    private void evictOverflow() {
        Iterator<Map.Entry<TileKey, TileTexture>> iterator = textures.entrySet().iterator();
        while (textures.size() > maxEntries && iterator.hasNext()) {
            Map.Entry<TileKey, TileTexture> entry = iterator.next();
            delete(entry.getValue());
            iterator.remove();
        }
    }

    private static void delete(TileTexture texture) {
        if (texture != null && texture.location != null) {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(texture.location);
        }
    }

    static int[] sample(Chunk chunk) {
        int[] pixels = new int[TILE_PIXELS * TILE_PIXELS];
        int[] heights = new int[pixels.length];
        Arrays.fill(pixels, 0xFF182631);
        for (int z = 0; z < TILE_PIXELS; z++) {
            for (int x = 0; x < TILE_PIXELS; x++) {
                int y = Math.max(1, chunk.getHeightValue(x, z));
                Block block = Blocks.air;
                int metadata = 0;
                while (y > 0) {
                    block = chunk.getBlock(x, y, z);
                    metadata = chunk.getBlockMetadata(x, y, z);
                    if (block != Blocks.air && block != Blocks.tallgrass) break;
                    y--;
                }
                heights[x + z * TILE_PIXELS] = y;
                int color = block == Blocks.air ? 0x182631 : block.getMapColor(metadata).colorValue;
                pixels[x + z * TILE_PIXELS] = 0xFF000000 | color;
            }
        }
        for (int z = 0; z < TILE_PIXELS; z++) {
            for (int x = 0; x < TILE_PIXELS; x++) {
                int index = x + z * TILE_PIXELS;
                int neighbor = heights[Math.max(0, x - 1) + z * TILE_PIXELS];
                if (z > 0) neighbor = (neighbor + heights[x + (z - 1) * TILE_PIXELS]) / 2;
                int delta = Integer.compare(heights[index], neighbor) * 18;
                pixels[index] = shade(pixels[index], delta);
            }
        }
        return pixels;
    }

    static int shade(int argb, int delta) {
        int red = Math.max(0, Math.min(255, ((argb >> 16) & 0xFF) + delta));
        int green = Math.max(0, Math.min(255, ((argb >> 8) & 0xFF) + delta));
        int blue = Math.max(0, Math.min(255, (argb & 0xFF) + delta));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static final class TileKey {
        private final int worldIdentity;
        private final int dimension;
        private final int chunkX;
        private final int chunkZ;

        private TileKey(int worldIdentity, int dimension, int chunkX, int chunkZ) {
            this.worldIdentity = worldIdentity;
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        static TileKey of(WorldClient world, int chunkX, int chunkZ) {
            return new TileKey(System.identityHashCode(world), world.provider.dimensionId, chunkX, chunkZ);
        }

        boolean matchesWorld(WorldClient world) {
            return world != null && worldIdentity == System.identityHashCode(world)
                && dimension == world.provider.dimensionId;
        }

        @Override public boolean equals(Object value) {
            if (!(value instanceof TileKey)) return false;
            TileKey other = (TileKey) value;
            return worldIdentity == other.worldIdentity && dimension == other.dimension
                && chunkX == other.chunkX && chunkZ == other.chunkZ;
        }

        @Override public int hashCode() {
            int result = worldIdentity;
            result = 31 * result + dimension;
            result = 31 * result + chunkX;
            return 31 * result + chunkZ;
        }
    }

    private static final class TilePixels {
        private final TileKey key;
        private final int[] pixels;
        private TilePixels(TileKey key, int[] pixels) { this.key = key; this.pixels = pixels; }
    }

    private static final class TileTexture {
        @SuppressWarnings("unused") private final DynamicTexture dynamic;
        private final ResourceLocation location;
        private TileTexture(DynamicTexture dynamic, ResourceLocation location) {
            this.dynamic = dynamic;
            this.location = location;
        }
    }
}

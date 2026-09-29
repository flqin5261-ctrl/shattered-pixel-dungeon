/*
 * Shattered Pixel Dungeon - Assist Edition
 * Infinite-world prototype state.
 */
package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class InfiniteWorldState implements Bundlable {

    public static final int WORLD_GEN_VERSION = 3;

    public int generatorVersion = WORLD_GEN_VERSION;
    public int centerChunkX = 0;
    public int centerChunkY = 0;
    public int heroWorldX = 12;
    public int heroWorldY = 12;
    public boolean heroWorldInitialized = false;

    private final HashMap<Long, Integer> terrainOverrides = new HashMap<>();
    private final HashSet<Long> generatedChunks = new HashSet<>();
    private final HashSet<Long> exploredChunks = new HashSet<>();
    // Exploration is stored as one compact bitset per 24x24 chunk instead of one
    // boxed Long per world cell. 576 cells = 9 longs (72 bytes) for each layer.
    private static final int CHUNK_SIZE = InfiniteWorldLevel.CHUNK_SIZE;
    private static final int CHUNK_CELL_COUNT = CHUNK_SIZE * CHUNK_SIZE;
    private static final int CHUNK_MASK_LONGS = (CHUNK_CELL_COUNT + 63) / 64;

    private final HashMap<Long, long[]> visitedChunks = new HashMap<>();
    private final HashMap<Long, long[]> mappedChunks = new HashMap<>();
    private final HashMap<Long, Integer> chestStates = new HashMap<>();

    public InfiniteWorldState() {}

    public Integer terrainOverride(long key) {
        return terrainOverrides.get(key);
    }

    public void markChunkGenerated(int chunkX, int chunkY) {
        generatedChunks.add(chunkKey(chunkX, chunkY));
    }

    public boolean chunkGenerated(int chunkX, int chunkY) {
        return generatedChunks.contains(chunkKey(chunkX, chunkY));
    }

    public int generatedChunkCount() {
        return generatedChunks.size();
    }

    public void markChunkExplored(int chunkX, int chunkY) {
        exploredChunks.add(chunkKey(chunkX, chunkY));
    }

    public boolean chunkExplored(int chunkX, int chunkY) {
        return exploredChunks.contains(chunkKey(chunkX, chunkY));
    }

    private static long chunkKey(int x, int y) {
        return ((long)x << 32) ^ (y & 0xFFFFFFFFL);
    }

    public void setTerrainOverride(long key, Integer value) {
        if (value == null) terrainOverrides.remove(key);
        else terrainOverrides.put(key, value);
    }

    public void markVisited(long key) {
        markExploredCell(visitedChunks, key);
    }

    public void markMapped(long key) {
        markExploredCell(mappedChunks, key);
    }

    public boolean wasVisited(long key) {
        return exploredCellSet(visitedChunks, key);
    }

    public boolean wasMapped(long key) {
        return exploredCellSet(mappedChunks, key);
    }

    public int visitedChunkCount() {
        return visitedChunks.size();
    }

    public int mappedChunkCount() {
        return mappedChunks.size();
    }

    private static void markExploredCell(HashMap<Long, long[]> chunks, long worldKey) {
        int wx = (int)(worldKey >> 32);
        int wy = (int)worldKey;
        int cx = Math.floorDiv(wx, CHUNK_SIZE);
        int cy = Math.floorDiv(wy, CHUNK_SIZE);
        int lx = Math.floorMod(wx, CHUNK_SIZE);
        int ly = Math.floorMod(wy, CHUNK_SIZE);
        int bit = lx + ly * CHUNK_SIZE;

        long key = chunkKey(cx, cy);
        long[] mask = chunks.get(key);
        if (mask == null) {
            mask = new long[CHUNK_MASK_LONGS];
            chunks.put(key, mask);
        }
        mask[bit >>> 6] |= 1L << (bit & 63);
    }

    private static boolean exploredCellSet(HashMap<Long, long[]> chunks, long worldKey) {
        int wx = (int)(worldKey >> 32);
        int wy = (int)worldKey;
        int cx = Math.floorDiv(wx, CHUNK_SIZE);
        int cy = Math.floorDiv(wy, CHUNK_SIZE);
        int lx = Math.floorMod(wx, CHUNK_SIZE);
        int ly = Math.floorMod(wy, CHUNK_SIZE);
        int bit = lx + ly * CHUNK_SIZE;

        long[] mask = chunks.get(chunkKey(cx, cy));
        return mask != null && (mask[bit >>> 6] & (1L << (bit & 63))) != 0;
    }

    private static long[] flattenMasks(HashMap<Long, long[]> chunks, long[] keys) {
        long[] flattened = new long[keys.length * CHUNK_MASK_LONGS];
        int i = 0;
        for (Map.Entry<Long, long[]> e : chunks.entrySet()) {
            keys[i] = e.getKey();
            System.arraycopy(e.getValue(), 0, flattened, i * CHUNK_MASK_LONGS, CHUNK_MASK_LONGS);
            i++;
        }
        return flattened;
    }

    private static void restoreMasks(HashMap<Long, long[]> chunks, long[] keys, long[] flattened) {
        chunks.clear();
        if (keys == null || flattened == null) return;
        int count = Math.min(keys.length, flattened.length / CHUNK_MASK_LONGS);
        for (int i = 0; i < count; i++) {
            long[] mask = new long[CHUNK_MASK_LONGS];
            System.arraycopy(flattened, i * CHUNK_MASK_LONGS, mask, 0, CHUNK_MASK_LONGS);
            chunks.put(keys[i], mask);
        }
    }

    public int chestState(long key) {
        Integer state = chestStates.get(key);
        return state == null ? 0 : state;
    }

    public void setChestState(long key, int state) {
        if (state <= 0) chestStates.remove(key);
        else chestStates.put(key, state);
    }

    private static final String GEN = "gen";
    private static final String CX = "cx";
    private static final String CY = "cy";
    private static final String HERO_WX = "hero_world_x";
    private static final String HERO_WY = "hero_world_y";
    private static final String HERO_WORLD_INIT = "hero_world_init";
    private static final String TERRAIN_KEYS = "terrain_keys";
    private static final String GENERATED_CHUNKS = "generated_chunks";
    private static final String EXPLORED_CHUNKS = "explored_chunks";
    private static final String TERRAIN_VALUES = "terrain_values";
    // Legacy v0.3.6 cell-per-key fields are kept read-only for migration.
    private static final String VISITED = "visited";
    private static final String MAPPED = "mapped";
    private static final String VISITED_CHUNK_KEYS = "visited_chunk_keys";
    private static final String VISITED_CHUNK_MASKS = "visited_chunk_masks";
    private static final String MAPPED_CHUNK_KEYS = "mapped_chunk_keys";
    private static final String MAPPED_CHUNK_MASKS = "mapped_chunk_masks";
    private static final String CHEST_KEYS = "chest_keys";
    private static final String CHEST_VALUES = "chest_values";

    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(GEN, generatorVersion);
        bundle.put(CX, centerChunkX);
        bundle.put(CY, centerChunkY);
        bundle.put(HERO_WX, heroWorldX);
        bundle.put(HERO_WY, heroWorldY);
        bundle.put(HERO_WORLD_INIT, heroWorldInitialized);

        long[] terrainKeys = new long[terrainOverrides.size()];
        int[] terrainValues = new int[terrainOverrides.size()];
        int i = 0;
        for (Map.Entry<Long, Integer> e : terrainOverrides.entrySet()) {
            terrainKeys[i] = e.getKey();
            terrainValues[i] = e.getValue();
            i++;
        }
        bundle.put(TERRAIN_KEYS, terrainKeys);
        bundle.put(TERRAIN_VALUES, terrainValues);

        long[] generated = new long[generatedChunks.size()];
        i = 0;
        for (Long key : generatedChunks) generated[i++] = key;
        bundle.put(GENERATED_CHUNKS, generated);

        long[] explored = new long[exploredChunks.size()];
        i = 0;
        for (Long key : exploredChunks) explored[i++] = key;
        bundle.put(EXPLORED_CHUNKS, explored);

        long[] visitedChunkKeys = new long[visitedChunks.size()];
        long[] visitedMasks = flattenMasks(visitedChunks, visitedChunkKeys);
        bundle.put(VISITED_CHUNK_KEYS, visitedChunkKeys);
        bundle.put(VISITED_CHUNK_MASKS, visitedMasks);

        long[] mappedChunkKeys = new long[mappedChunks.size()];
        long[] mappedMasks = flattenMasks(mappedChunks, mappedChunkKeys);
        bundle.put(MAPPED_CHUNK_KEYS, mappedChunkKeys);
        bundle.put(MAPPED_CHUNK_MASKS, mappedMasks);

        long[] chestKeys = new long[chestStates.size()];
        int[] chestValues = new int[chestStates.size()];
        i = 0;
        for (Map.Entry<Long, Integer> e : chestStates.entrySet()) {
            chestKeys[i] = e.getKey();
            chestValues[i] = e.getValue();
            i++;
        }
        bundle.put(CHEST_KEYS, chestKeys);
        bundle.put(CHEST_VALUES, chestValues);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        generatorVersion = bundle.contains(GEN) ? bundle.getInt(GEN) : WORLD_GEN_VERSION;
        centerChunkX = bundle.getInt(CX);
        centerChunkY = bundle.getInt(CY);
        heroWorldX = bundle.contains(HERO_WX) ? bundle.getInt(HERO_WX) : 12;
        heroWorldY = bundle.contains(HERO_WY) ? bundle.getInt(HERO_WY) : 12;
        heroWorldInitialized = bundle.contains(HERO_WORLD_INIT) && bundle.getBoolean(HERO_WORLD_INIT);

        terrainOverrides.clear();
        long[] terrainKeys = bundle.getLongArray(TERRAIN_KEYS);
        int[] terrainValues = bundle.getIntArray(TERRAIN_VALUES);
        if (terrainKeys != null && terrainValues != null) {
            for (int i = 0; i < Math.min(terrainKeys.length, terrainValues.length); i++) {
                terrainOverrides.put(terrainKeys[i], terrainValues[i]);
            }
        }

        generatedChunks.clear();
        long[] generated = bundle.getLongArray(GENERATED_CHUNKS);
        if (generated != null) for (long key : generated) generatedChunks.add(key);

        exploredChunks.clear();
        long[] explored = bundle.getLongArray(EXPLORED_CHUNKS);
        if (explored != null) for (long key : explored) exploredChunks.add(key);

        if (bundle.contains(VISITED_CHUNK_KEYS)) {
            restoreMasks(visitedChunks,
                    bundle.getLongArray(VISITED_CHUNK_KEYS),
                    bundle.getLongArray(VISITED_CHUNK_MASKS));
        } else {
            // One-time migration from v0.3.6 and earlier.
            visitedChunks.clear();
            long[] visitedKeys = bundle.getLongArray(VISITED);
            if (visitedKeys != null) for (long key : visitedKeys) markVisited(key);
        }

        if (bundle.contains(MAPPED_CHUNK_KEYS)) {
            restoreMasks(mappedChunks,
                    bundle.getLongArray(MAPPED_CHUNK_KEYS),
                    bundle.getLongArray(MAPPED_CHUNK_MASKS));
        } else {
            mappedChunks.clear();
            long[] mappedKeys = bundle.getLongArray(MAPPED);
            if (mappedKeys != null) for (long key : mappedKeys) markMapped(key);
        }

        chestStates.clear();
        long[] chestKeys = bundle.getLongArray(CHEST_KEYS);
        int[] chestValues = bundle.getIntArray(CHEST_VALUES);
        if (chestKeys != null && chestValues != null) {
            for (int i = 0; i < Math.min(chestKeys.length, chestValues.length); i++) {
                chestStates.put(chestKeys[i], chestValues[i]);
            }
        }
    }
}

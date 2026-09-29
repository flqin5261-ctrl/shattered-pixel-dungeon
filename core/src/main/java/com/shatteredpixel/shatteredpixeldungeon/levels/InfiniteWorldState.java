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

    public static final int WORLD_GEN_VERSION = 1;

    public int generatorVersion = WORLD_GEN_VERSION;
    public int centerChunkX = 0;
    public int centerChunkY = 0;

    private final HashMap<Long, Integer> terrainOverrides = new HashMap<>();
    private final HashSet<Long> visited = new HashSet<>();
    private final HashSet<Long> mapped = new HashSet<>();
    private final HashMap<Long, Integer> chestStates = new HashMap<>();

    public InfiniteWorldState() {}

    public Integer terrainOverride(long key) {
        return terrainOverrides.get(key);
    }

    public void setTerrainOverride(long key, Integer value) {
        if (value == null) terrainOverrides.remove(key);
        else terrainOverrides.put(key, value);
    }

    public void markVisited(long key) {
        visited.add(key);
    }

    public void markMapped(long key) {
        mapped.add(key);
    }

    public boolean wasVisited(long key) {
        return visited.contains(key);
    }

    public boolean wasMapped(long key) {
        return mapped.contains(key);
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
    private static final String TERRAIN_KEYS = "terrain_keys";
    private static final String TERRAIN_VALUES = "terrain_values";
    private static final String VISITED = "visited";
    private static final String MAPPED = "mapped";
    private static final String CHEST_KEYS = "chest_keys";
    private static final String CHEST_VALUES = "chest_values";

    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(GEN, generatorVersion);
        bundle.put(CX, centerChunkX);
        bundle.put(CY, centerChunkY);

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

        long[] visitedKeys = new long[visited.size()];
        i = 0;
        for (Long key : visited) visitedKeys[i++] = key;
        bundle.put(VISITED, visitedKeys);

        long[] mappedKeys = new long[mapped.size()];
        i = 0;
        for (Long key : mapped) mappedKeys[i++] = key;
        bundle.put(MAPPED, mappedKeys);

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

        terrainOverrides.clear();
        long[] terrainKeys = bundle.getLongArray(TERRAIN_KEYS);
        int[] terrainValues = bundle.getIntArray(TERRAIN_VALUES);
        if (terrainKeys != null && terrainValues != null) {
            for (int i = 0; i < Math.min(terrainKeys.length, terrainValues.length); i++) {
                terrainOverrides.put(terrainKeys[i], terrainValues[i]);
            }
        }

        visited.clear();
        long[] visitedKeys = bundle.getLongArray(VISITED);
        if (visitedKeys != null) for (long key : visitedKeys) visited.add(key);

        mapped.clear();
        long[] mappedKeys = bundle.getLongArray(MAPPED);
        if (mappedKeys != null) for (long key : mappedKeys) mapped.add(key);

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

/*
 * Shattered Pixel Dungeon - Assist Edition
 * Infinite-world prototype state.
 */
package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class InfiniteWorldState implements Bundlable {

    public static final int WORLD_GEN_VERSION = 18;

    public int generatorVersion = WORLD_GEN_VERSION;
    public int centerChunkX = 0;
    public int centerChunkY = 0;
    public int heroWorldX = 12;
    public int heroWorldY = 12;
    public boolean heroWorldInitialized = false;

    // Assist 0.6.17 starter cache. Coordinates are stored in world-space so the
    // two chests/key survive streaming-window rebuilds and can never duplicate.
    public boolean starterSuppliesInitialized = false;
    public int starterNormalChestWorldX = Integer.MIN_VALUE;
    public int starterNormalChestWorldY = Integer.MIN_VALUE;
    public int starterCrystalKeyWorldX = Integer.MIN_VALUE;
    public int starterCrystalKeyWorldY = Integer.MIN_VALUE;
    public int starterCrystalChestWorldX = Integer.MIN_VALUE;
    public int starterCrystalChestWorldY = Integer.MIN_VALUE;
    public final ArrayList<Item> starterNormalChestContents = new ArrayList<>();
    public final ArrayList<Item> starterCrystalChestContents = new ArrayList<>();

    // V12 progression state. heroActionValue counts positive Hero spend/spendConstant
    // time while inside Infinite World. The guaranteed artifact chest is a single
    // world object with an explicit lifecycle so Streaming and Save/Load cannot clone it.
    public float heroActionValue = 0f;
    public int artifactChestWorldX = 0;
    public int artifactChestWorldY = 0;
    public int artifactChestArtifactIndex = -1;
    // 0 = not spawned, 1 = closed crystal chest, 2 = opened heap, 3 = artifact taken.
    public int artifactChestState = 0;

    // 0.5.11 long-run progression / level-30 breakthrough state.
    public boolean breakthroughCompleted = false;
    public boolean breakthroughTrialActive = false;
    public int breakthroughWave = 0;
    public float breakthroughCountdown = -1f;
    public int breakthroughReturnDepth = 1;
    public int breakthroughReturnBranch = 0;
    public int breakthroughReturnPos = -1;
    public int breakthroughTrialGold = 0;
    public int breakthroughTrialEnergy = 0;
    public Bundle breakthroughHeroSnapshot = null;
    public Bundle breakthroughQuickslotSnapshot = null;

    // 0.6.6 level-60 endgame state. Genesis Echo is permanent once unlocked.
    public boolean genesisEchoUnlocked = false;
    public int genesisKillHpBonus = 0;
    public int genesisKillStrBonus = 0;

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
    // Generic persistent state for deterministic plants and loose world loot.
    // 0 = untouched/present, 1+ = object-specific consumed/changed state.
    private final HashMap<Long, Integer> objectStates = new HashMap<>();

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

    public int objectState(long key) {
        Integer state = objectStates.get(key);
        return state == null ? 0 : state;
    }

    public void setObjectState(long key, int state) {
        if (state <= 0) objectStates.remove(key);
        else objectStates.put(key, state);
    }

    private static final String GEN = "gen";
    private static final String CX = "cx";
    private static final String CY = "cy";
    private static final String HERO_WX = "hero_world_x";
    private static final String HERO_WY = "hero_world_y";
    private static final String HERO_WORLD_INIT = "hero_world_init";
    private static final String STARTER_SUPPLIES_INIT = "starter_supplies_init";
    private static final String STARTER_NORMAL_X = "starter_normal_x";
    private static final String STARTER_NORMAL_Y = "starter_normal_y";
    private static final String STARTER_KEY_X = "starter_key_x";
    private static final String STARTER_KEY_Y = "starter_key_y";
    private static final String STARTER_CRYSTAL_X = "starter_crystal_x";
    private static final String STARTER_CRYSTAL_Y = "starter_crystal_y";
    private static final String STARTER_NORMAL_CONTENTS = "starter_normal_contents";
    private static final String STARTER_CRYSTAL_CONTENTS = "starter_crystal_contents";
    private static final String HERO_ACTION_VALUE = "hero_action_value";
    private static final String ARTIFACT_CHEST_WX = "artifact_chest_wx";
    private static final String ARTIFACT_CHEST_WY = "artifact_chest_wy";
    private static final String ARTIFACT_CHEST_INDEX = "artifact_chest_index";
    private static final String ARTIFACT_CHEST_STATE = "artifact_chest_state";
    private static final String BREAKTHROUGH_COMPLETED = "breakthrough_completed";
    private static final String BREAKTHROUGH_ACTIVE = "breakthrough_active";
    private static final String BREAKTHROUGH_WAVE = "breakthrough_wave";
    private static final String BREAKTHROUGH_COUNTDOWN = "breakthrough_countdown";
    private static final String BREAKTHROUGH_RETURN_DEPTH = "breakthrough_return_depth";
    private static final String BREAKTHROUGH_RETURN_BRANCH = "breakthrough_return_branch";
    private static final String BREAKTHROUGH_RETURN_POS = "breakthrough_return_pos";
    private static final String BREAKTHROUGH_GOLD = "breakthrough_gold";
    private static final String BREAKTHROUGH_ENERGY = "breakthrough_energy";
    private static final String BREAKTHROUGH_HERO = "breakthrough_hero";
    private static final String BREAKTHROUGH_QUICKSLOT = "breakthrough_quickslot";
    private static final String GENESIS_ECHO_UNLOCKED = "genesis_echo_unlocked";
    private static final String GENESIS_KILL_HP = "genesis_kill_hp";
    private static final String GENESIS_KILL_STR = "genesis_kill_str";
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
    private static final String OBJECT_KEYS = "object_keys";
    private static final String OBJECT_VALUES = "object_values";

    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(GEN, generatorVersion);
        bundle.put(CX, centerChunkX);
        bundle.put(CY, centerChunkY);
        bundle.put(HERO_WX, heroWorldX);
        bundle.put(HERO_WY, heroWorldY);
        bundle.put(HERO_WORLD_INIT, heroWorldInitialized);
        bundle.put(STARTER_SUPPLIES_INIT, starterSuppliesInitialized);
        bundle.put(STARTER_NORMAL_X, starterNormalChestWorldX);
        bundle.put(STARTER_NORMAL_Y, starterNormalChestWorldY);
        bundle.put(STARTER_KEY_X, starterCrystalKeyWorldX);
        bundle.put(STARTER_KEY_Y, starterCrystalKeyWorldY);
        bundle.put(STARTER_CRYSTAL_X, starterCrystalChestWorldX);
        bundle.put(STARTER_CRYSTAL_Y, starterCrystalChestWorldY);
        bundle.put(STARTER_NORMAL_CONTENTS, starterNormalChestContents);
        bundle.put(STARTER_CRYSTAL_CONTENTS, starterCrystalChestContents);
        bundle.put(HERO_ACTION_VALUE, heroActionValue);
        bundle.put(ARTIFACT_CHEST_WX, artifactChestWorldX);
        bundle.put(ARTIFACT_CHEST_WY, artifactChestWorldY);
        bundle.put(ARTIFACT_CHEST_INDEX, artifactChestArtifactIndex);
        bundle.put(ARTIFACT_CHEST_STATE, artifactChestState);
        bundle.put(BREAKTHROUGH_COMPLETED, breakthroughCompleted);
        bundle.put(BREAKTHROUGH_ACTIVE, breakthroughTrialActive);
        bundle.put(BREAKTHROUGH_WAVE, breakthroughWave);
        bundle.put(BREAKTHROUGH_COUNTDOWN, breakthroughCountdown);
        bundle.put(BREAKTHROUGH_RETURN_DEPTH, breakthroughReturnDepth);
        bundle.put(BREAKTHROUGH_RETURN_BRANCH, breakthroughReturnBranch);
        bundle.put(BREAKTHROUGH_RETURN_POS, breakthroughReturnPos);
        bundle.put(BREAKTHROUGH_GOLD, breakthroughTrialGold);
        bundle.put(BREAKTHROUGH_ENERGY, breakthroughTrialEnergy);
        if (breakthroughHeroSnapshot != null) {
            bundle.put(BREAKTHROUGH_HERO, breakthroughHeroSnapshot);
        }
        if (breakthroughQuickslotSnapshot != null) {
            bundle.put(BREAKTHROUGH_QUICKSLOT, breakthroughQuickslotSnapshot);
        }
        bundle.put(GENESIS_ECHO_UNLOCKED, genesisEchoUnlocked);
        bundle.put(GENESIS_KILL_HP, genesisKillHpBonus);
        bundle.put(GENESIS_KILL_STR, genesisKillStrBonus);

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

        long[] objectKeys = new long[objectStates.size()];
        int[] objectValues = new int[objectStates.size()];
        i = 0;
        for (Map.Entry<Long, Integer> e : objectStates.entrySet()) {
            objectKeys[i] = e.getKey();
            objectValues[i] = e.getValue();
            i++;
        }
        bundle.put(OBJECT_KEYS, objectKeys);
        bundle.put(OBJECT_VALUES, objectValues);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        generatorVersion = bundle.contains(GEN) ? bundle.getInt(GEN) : WORLD_GEN_VERSION;
        centerChunkX = bundle.getInt(CX);
        centerChunkY = bundle.getInt(CY);
        heroWorldX = bundle.contains(HERO_WX) ? bundle.getInt(HERO_WX) : 12;
        heroWorldY = bundle.contains(HERO_WY) ? bundle.getInt(HERO_WY) : 12;
        heroWorldInitialized = bundle.contains(HERO_WORLD_INIT) && bundle.getBoolean(HERO_WORLD_INIT);
        starterSuppliesInitialized = bundle.contains(STARTER_SUPPLIES_INIT)
                && bundle.getBoolean(STARTER_SUPPLIES_INIT);
        starterNormalChestWorldX = bundle.contains(STARTER_NORMAL_X)
                ? bundle.getInt(STARTER_NORMAL_X) : Integer.MIN_VALUE;
        starterNormalChestWorldY = bundle.contains(STARTER_NORMAL_Y)
                ? bundle.getInt(STARTER_NORMAL_Y) : Integer.MIN_VALUE;
        starterCrystalKeyWorldX = bundle.contains(STARTER_KEY_X)
                ? bundle.getInt(STARTER_KEY_X) : Integer.MIN_VALUE;
        starterCrystalKeyWorldY = bundle.contains(STARTER_KEY_Y)
                ? bundle.getInt(STARTER_KEY_Y) : Integer.MIN_VALUE;
        starterCrystalChestWorldX = bundle.contains(STARTER_CRYSTAL_X)
                ? bundle.getInt(STARTER_CRYSTAL_X) : Integer.MIN_VALUE;
        starterCrystalChestWorldY = bundle.contains(STARTER_CRYSTAL_Y)
                ? bundle.getInt(STARTER_CRYSTAL_Y) : Integer.MIN_VALUE;
        starterNormalChestContents.clear();
        if (bundle.contains(STARTER_NORMAL_CONTENTS)) {
            for (Bundlable item : bundle.getCollection(STARTER_NORMAL_CONTENTS)) {
                if (item instanceof Item) starterNormalChestContents.add((Item)item);
            }
        }
        starterCrystalChestContents.clear();
        if (bundle.contains(STARTER_CRYSTAL_CONTENTS)) {
            for (Bundlable item : bundle.getCollection(STARTER_CRYSTAL_CONTENTS)) {
                if (item instanceof Item) starterCrystalChestContents.add((Item)item);
            }
        }
        heroActionValue = bundle.contains(HERO_ACTION_VALUE) ? bundle.getFloat(HERO_ACTION_VALUE) : 0f;
        artifactChestWorldX = bundle.contains(ARTIFACT_CHEST_WX) ? bundle.getInt(ARTIFACT_CHEST_WX) : 0;
        artifactChestWorldY = bundle.contains(ARTIFACT_CHEST_WY) ? bundle.getInt(ARTIFACT_CHEST_WY) : 0;
        artifactChestArtifactIndex = bundle.contains(ARTIFACT_CHEST_INDEX) ? bundle.getInt(ARTIFACT_CHEST_INDEX) : -1;
        artifactChestState = bundle.contains(ARTIFACT_CHEST_STATE) ? bundle.getInt(ARTIFACT_CHEST_STATE) : 0;
        breakthroughCompleted = bundle.contains(BREAKTHROUGH_COMPLETED)
                && bundle.getBoolean(BREAKTHROUGH_COMPLETED);
        breakthroughTrialActive = bundle.contains(BREAKTHROUGH_ACTIVE)
                && bundle.getBoolean(BREAKTHROUGH_ACTIVE);
        breakthroughWave = bundle.contains(BREAKTHROUGH_WAVE)
                ? bundle.getInt(BREAKTHROUGH_WAVE) : 0;
        breakthroughCountdown = bundle.contains(BREAKTHROUGH_COUNTDOWN)
                ? bundle.getFloat(BREAKTHROUGH_COUNTDOWN) : -1f;
        breakthroughReturnDepth = bundle.contains(BREAKTHROUGH_RETURN_DEPTH)
                ? bundle.getInt(BREAKTHROUGH_RETURN_DEPTH) : 1;
        breakthroughReturnBranch = bundle.contains(BREAKTHROUGH_RETURN_BRANCH)
                ? bundle.getInt(BREAKTHROUGH_RETURN_BRANCH) : 0;
        breakthroughReturnPos = bundle.contains(BREAKTHROUGH_RETURN_POS)
                ? bundle.getInt(BREAKTHROUGH_RETURN_POS) : -1;
        breakthroughTrialGold = bundle.contains(BREAKTHROUGH_GOLD)
                ? bundle.getInt(BREAKTHROUGH_GOLD) : 0;
        breakthroughTrialEnergy = bundle.contains(BREAKTHROUGH_ENERGY)
                ? bundle.getInt(BREAKTHROUGH_ENERGY) : 0;
        breakthroughHeroSnapshot = bundle.contains(BREAKTHROUGH_HERO)
                ? bundle.getBundle(BREAKTHROUGH_HERO) : null;
        breakthroughQuickslotSnapshot = bundle.contains(BREAKTHROUGH_QUICKSLOT)
                ? bundle.getBundle(BREAKTHROUGH_QUICKSLOT) : null;
        genesisEchoUnlocked = bundle.contains(GENESIS_ECHO_UNLOCKED)
                && bundle.getBoolean(GENESIS_ECHO_UNLOCKED);
        genesisKillHpBonus = bundle.contains(GENESIS_KILL_HP)
                ? bundle.getInt(GENESIS_KILL_HP) : 0;
        genesisKillStrBonus = bundle.contains(GENESIS_KILL_STR)
                ? bundle.getInt(GENESIS_KILL_STR) : 0;

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

        objectStates.clear();
        long[] objectKeys = bundle.getLongArray(OBJECT_KEYS);
        int[] objectValues = bundle.getIntArray(OBJECT_VALUES);
        if (objectKeys != null && objectValues != null) {
            for (int i = 0; i < Math.min(objectKeys.length, objectValues.length); i++) {
                objectStates.put(objectKeys[i], objectValues[i]);
            }
        }
    }
}

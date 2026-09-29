/*
 * Shattered Pixel Dungeon - Assist Edition
 * 0.3 infinite-world technical prototype.
 */
package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class InfiniteWorldLevel extends Level {

    public static final int CHUNK_SIZE = 24;
    public static final int WINDOW_CHUNKS = 3;
    public static final int MAP_SIZE = CHUNK_SIZE * WINDOW_CHUNKS;

    private boolean shifting;

    {
        color1 = 0x36585F;
        color2 = 0x7AA7A1;
    }

    @Override
    public String tilesTex() {
        return Assets.Environment.TILES_CITY;
    }

    @Override
    public String waterTex() {
        return Assets.Environment.WATER_CITY;
    }

    @Override
    public void playLevelMusic() {
        Music.INSTANCE.play(Assets.Music.CITY_1, true);
    }

    private InfiniteWorldState state() {
        if (Dungeon.infiniteWorldState == null) {
            Dungeon.infiniteWorldState = new InfiniteWorldState();
        }
        return Dungeon.infiniteWorldState;
    }

    @Override
    protected boolean build() {
        setSize(MAP_SIZE, MAP_SIZE);

        InfiniteWorldState state = state();
        int[] base = generateBaseWindow(state.centerChunkX, state.centerChunkY);
        System.arraycopy(base, 0, map, 0, map.length);
        applyTerrainOverrides();
        restoreExploration();

        transitions.clear();
        int spawn = centerCellForChunk(state.centerChunkX, state.centerChunkY);
        transitions.add(new LevelTransition(this, spawn, LevelTransition.Type.REGULAR_ENTRANCE));

        return true;
    }

    @Override
    protected void createMobs() {
        // 0.3 intentionally has no enemies. This version validates world streaming first.
    }

    @Override
    public Actor addRespawner() {
        return null;
    }

    @Override
    public Mob createMob() {
        return null;
    }

    @Override
    protected void createItems() {
        generateChestHeaps();
    }

    @Override
    public boolean activateTransition(Hero hero, LevelTransition transition) {
        // The only transition is a hidden spawn anchor. There are no floors or stairs here.
        return false;
    }

    @Override
    public int randomRespawnCell(Char ch) {
        return centerCellForChunk(state().centerChunkX, state().centerChunkY);
    }

    public boolean afterHeroMove(Hero hero) {
        if (shifting || hero == null || Dungeon.level != this) return false;

        int x = hero.pos % width();
        int y = hero.pos / width();

        int dx = 0;
        int dy = 0;
        if (x < CHUNK_SIZE) dx = -1;
        else if (x >= CHUNK_SIZE * 2) dx = 1;
        if (y < CHUNK_SIZE) dy = -1;
        else if (y >= CHUNK_SIZE * 2) dy = 1;

        if (dx == 0 && dy == 0) return false;

        shifting = true;

        InfiniteWorldState state = state();
        int oldCenterX = state.centerChunkX;
        int oldCenterY = state.centerChunkY;

        int localX = hero.pos % width();
        int localY = hero.pos / width();
        int worldX = (oldCenterX - 1) * CHUNK_SIZE + localX;
        int worldY = (oldCenterY - 1) * CHUNK_SIZE + localY;

        snapshotForSave();

        state.centerChunkX += dx;
        state.centerChunkY += dy;

        rebuildWindow();

        int newLocalX = worldX - (state.centerChunkX - 1) * CHUNK_SIZE;
        int newLocalY = worldY - (state.centerChunkY - 1) * CHUNK_SIZE;
        hero.pos = newLocalX + newLocalY * width();

        hero.interrupt();

        GLog.i("无界世界：区块 (" + state.centerChunkX + ", " + state.centerChunkY + ")");

        ShatteredPixelDungeon.runOnRenderThread(new Callback() {
            @Override
            public void call() {
                InterlevelScene.mode = InterlevelScene.Mode.NONE;
                ShatteredPixelDungeon.switchNoFade(GameScene.class);
                shifting = false;
            }
        });

        return true;
    }

    public void snapshotForSave() {
        InfiniteWorldState state = state();
        int[] base = generateBaseWindow(state.centerChunkX, state.centerChunkY);

        for (int cell = 0; cell < length(); cell++) {
            long key = worldKeyForLocalCell(cell);

            if (map[cell] != base[cell]) {
                state.setTerrainOverride(key, map[cell]);
            } else {
                state.setTerrainOverride(key, null);
            }

            if (visited[cell]) state.markVisited(key);
            if (mapped[cell]) state.markMapped(key);
        }

        snapshotChestStates();
    }

    private void rebuildWindow() {
        InfiniteWorldState state = state();

        int[] base = generateBaseWindow(state.centerChunkX, state.centerChunkY);
        System.arraycopy(base, 0, map, 0, map.length);
        applyTerrainOverrides();

        Arrays.fill(visited, false);
        Arrays.fill(mapped, false);
        Arrays.fill(heroFOV, false);
        restoreExploration();

        transitions = new ArrayList<>();
        int spawn = centerCellForChunk(state.centerChunkX, state.centerChunkY);
        transitions.add(new LevelTransition(this, spawn, LevelTransition.Type.REGULAR_ENTRANCE));

        heaps = new SparseArray<>();
        plants = new SparseArray<>();
        traps = new SparseArray<>();
        blobs = new HashMap<>();
        customTiles = new ArrayList<>();
        customTerrain = new ArrayList<>();
        customWalls = new ArrayList<>();

        buildFlagMaps();
        cleanWalls();
        PathFinder.setMapSize(width(), height());

        generateChestHeaps();
    }

    private void applyTerrainOverrides() {
        InfiniteWorldState state = state();
        for (int cell = 0; cell < length(); cell++) {
            Integer override = state.terrainOverride(worldKeyForLocalCell(cell));
            if (override != null) map[cell] = override;
        }
    }

    private void restoreExploration() {
        InfiniteWorldState state = state();
        for (int cell = 0; cell < length(); cell++) {
            long key = worldKeyForLocalCell(cell);
            visited[cell] = state.wasVisited(key);
            mapped[cell] = state.wasMapped(key);
        }
    }

    private void snapshotChestStates() {
        InfiniteWorldState state = state();
        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                if (!hasChest(cx, cy)) return;

                int lx = chestLocalX(cx, cy);
                int ly = chestLocalY(cx, cy);
                int cell = ox + lx + (oy + ly) * width();
                long key = encodeWorld((long)cx * CHUNK_SIZE + lx, (long)cy * CHUNK_SIZE + ly);

                Heap heap = heaps.get(cell);
                if (heap == null) {
                    state.setChestState(key, 2);
                } else if (heap.type == Heap.Type.CHEST) {
                    state.setChestState(key, 0);
                } else {
                    state.setChestState(key, 1);
                }
            }
        });
    }

    private void generateChestHeaps() {
        final InfiniteWorldState state = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                if (!hasChest(cx, cy)) return;

                int lx = chestLocalX(cx, cy);
                int ly = chestLocalY(cx, cy);
                int cell = ox + lx + (oy + ly) * width();
                long key = encodeWorld((long)cx * CHUNK_SIZE + lx, (long)cy * CHUNK_SIZE + ly);
                int chestState = state.chestState(key);

                if (chestState >= 2) return;

                Heap heap = new Heap();
                heap.pos = cell;
                heap.seen = visited[cell];
                heap.type = chestState == 0 ? Heap.Type.CHEST : Heap.Type.HEAP;
                heap.drop(chestItem(cx, cy));
                heaps.put(cell, heap);
            }
        });
    }

    private Item chestItem(int cx, int cy) {
        long h = hash(cx, cy, 7001);
        if ((h & 1L) == 0) return new PotionOfHealing();
        return new ScrollOfMagicMapping();
    }

    private int[] generateBaseWindow(int centerChunkX, int centerChunkY) {
        int[] result = new int[MAP_SIZE * MAP_SIZE];
        Arrays.fill(result, Terrain.WALL);

        for (int wy = -1; wy <= 1; wy++) {
            for (int wx = -1; wx <= 1; wx++) {
                int cx = centerChunkX + wx;
                int cy = centerChunkY + wy;
                int ox = (wx + 1) * CHUNK_SIZE;
                int oy = (wy + 1) * CHUNK_SIZE;
                generateChunk(result, cx, cy, ox, oy);
            }
        }

        return result;
    }

    private void generateChunk(int[] result, int cx, int cy, int ox, int oy) {
        int centerX = 9 + range(cx, cy, 11, 0, 5);
        int centerY = 9 + range(cx, cy, 12, 0, 5);

        int northX = edgeHorizontal(cx, cy);
        int southX = edgeHorizontal(cx, cy + 1);
        int westY = edgeVertical(cx, cy);
        int eastY = edgeVertical(cx + 1, cy);

        // A guaranteed central court keeps every edge connection reachable.
        carveRect(result, ox + centerX - 4, oy + centerY - 4,
                ox + centerX + 4, oy + centerY + 4, Terrain.EMPTY);

        carvePath(result, ox + centerX, oy + centerY, ox + northX, oy, cx, cy, 21);
        carvePath(result, ox + centerX, oy + centerY, ox + southX, oy + CHUNK_SIZE - 1, cx, cy, 22);
        carvePath(result, ox + centerX, oy + centerY, ox, oy + westY, cx, cy, 23);
        carvePath(result, ox + centerX, oy + centerY, ox + CHUNK_SIZE - 1, oy + eastY, cx, cy, 24);

        carveGateway(result, ox + northX, oy, true);
        carveGateway(result, ox + southX, oy + CHUNK_SIZE - 1, true);
        carveGateway(result, ox, oy + westY, false);
        carveGateway(result, ox + CHUNK_SIZE - 1, oy + eastY, false);

        int style = range(cx, cy, 31, 0, 3);
        switch (style) {
            case 0:
                decorateGarden(result, cx, cy, ox, oy, centerX, centerY);
                break;
            case 1:
                decorateWaterCourt(result, cx, cy, ox, oy, centerX, centerY);
                break;
            case 2:
                decorateArchive(result, cx, cy, ox, oy, centerX, centerY);
                break;
            default:
                decorateRuins(result, cx, cy, ox, oy, centerX, centerY);
                break;
        }

        // Add one ordinary door on a side corridor. Door state is persisted as terrain override.
        int doorChoice = range(cx, cy, 51, 0, 3);
        int doorX;
        int doorY;
        if (doorChoice == 0) {
            doorX = northX;
            doorY = 5;
        } else if (doorChoice == 1) {
            doorX = southX;
            doorY = CHUNK_SIZE - 6;
        } else if (doorChoice == 2) {
            doorX = 5;
            doorY = westY;
        } else {
            doorX = CHUNK_SIZE - 6;
            doorY = eastY;
        }
        int dc = ox + doorX + (oy + doorY) * MAP_SIZE;
        if (result[dc] == Terrain.EMPTY || result[dc] == Terrain.EMPTY_DECO) {
            result[dc] = Terrain.DOOR;
        }

        if (hasChest(cx, cy)) {
            int chestX = chestLocalX(cx, cy);
            int chestY = chestLocalY(cx, cy);
            carveRect(result, ox + chestX - 1, oy + chestY - 1,
                    ox + chestX + 1, oy + chestY + 1, Terrain.EMPTY);
            carvePath(result, ox + centerX, oy + centerY, ox + chestX, oy + chestY, cx, cy, 81);
            result[ox + chestX + (oy + chestY) * MAP_SIZE] = Terrain.EMPTY_DECO;
        }

        // World origin gets a recognizable spawn plaza.
        if (cx == 0 && cy == 0) {
            carveRect(result, ox + 7, oy + 7, ox + 16, oy + 16, Terrain.EMPTY);
            for (int x = 9; x <= 14; x++) {
                result[ox + x + (oy + 9) * MAP_SIZE] = Terrain.WATER;
                result[ox + x + (oy + 14) * MAP_SIZE] = Terrain.WATER;
            }
            result[ox + 11 + (oy + 11) * MAP_SIZE] = Terrain.EMPTY_DECO;
            result[ox + 12 + (oy + 12) * MAP_SIZE] = Terrain.EMPTY_DECO;
        }
    }

    private void decorateGarden(int[] map, int cx, int cy, int ox, int oy, int centerX, int centerY) {
        for (int y = centerY - 3; y <= centerY + 3; y++) {
            for (int x = centerX - 3; x <= centerX + 3; x++) {
                long h = hash(cx * CHUNK_SIZE + x, cy * CHUNK_SIZE + y, 101);
                int cell = ox + x + (oy + y) * MAP_SIZE;
                if (map[cell] == Terrain.EMPTY && Math.floorMod(h, 5) == 0) map[cell] = Terrain.HIGH_GRASS;
            }
        }
    }

    private void decorateWaterCourt(int[] map, int cx, int cy, int ox, int oy, int centerX, int centerY) {
        for (int x = centerX - 4; x <= centerX + 4; x++) {
            setIfFloor(map, ox + x, oy + centerY - 4, Terrain.WATER);
            setIfFloor(map, ox + x, oy + centerY + 4, Terrain.WATER);
        }
        for (int y = centerY - 4; y <= centerY + 4; y++) {
            setIfFloor(map, ox + centerX - 4, oy + y, Terrain.WATER);
            setIfFloor(map, ox + centerX + 4, oy + y, Terrain.WATER);
        }
        setIfFloor(map, ox + centerX, oy + centerY - 4, Terrain.EMPTY);
        setIfFloor(map, ox + centerX, oy + centerY + 4, Terrain.EMPTY);
        setIfFloor(map, ox + centerX - 4, oy + centerY, Terrain.EMPTY);
        setIfFloor(map, ox + centerX + 4, oy + centerY, Terrain.EMPTY);
    }

    private void decorateArchive(int[] map, int cx, int cy, int ox, int oy, int centerX, int centerY) {
        int left = Math.max(2, centerX - 5);
        int right = Math.min(CHUNK_SIZE - 3, centerX + 5);
        int top = Math.max(2, centerY - 5);
        int bottom = Math.min(CHUNK_SIZE - 3, centerY + 5);
        for (int x = left; x <= right; x += 2) {
            int c1 = ox + x + (oy + top) * MAP_SIZE;
            int c2 = ox + x + (oy + bottom) * MAP_SIZE;
            if (map[c1] == Terrain.EMPTY) map[c1] = Terrain.BOOKSHELF;
            if (map[c2] == Terrain.EMPTY) map[c2] = Terrain.BOOKSHELF;
        }
    }

    private void decorateRuins(int[] map, int cx, int cy, int ox, int oy, int centerX, int centerY) {
        for (int i = 0; i < 8; i++) {
            int x = 4 + range(cx, cy, 201 + i, 0, CHUNK_SIZE - 9);
            int y = 4 + range(cx, cy, 301 + i, 0, CHUNK_SIZE - 9);
            int cell = ox + x + (oy + y) * MAP_SIZE;
            if (map[cell] == Terrain.EMPTY) {
                map[cell] = (i % 3 == 0) ? Terrain.STATUE : Terrain.EMPTY_DECO;
            }
        }
    }

    private void carvePath(int[] map, int x1, int y1, int x2, int y2, int cx, int cy, int salt) {
        boolean horizontalFirst = (hash(cx, cy, salt) & 1L) == 0;
        if (horizontalFirst) {
            carveHorizontal(map, x1, x2, y1);
            carveVertical(map, y1, y2, x2);
        } else {
            carveVertical(map, y1, y2, x1);
            carveHorizontal(map, x1, x2, y2);
        }
    }

    private void carveHorizontal(int[] map, int x1, int x2, int y) {
        int from = Math.min(x1, x2);
        int to = Math.max(x1, x2);
        for (int x = from; x <= to; x++) {
            setFloor(map, x, y);
            setFloor(map, x, y - 1);
            setFloor(map, x, y + 1);
        }
    }

    private void carveVertical(int[] map, int y1, int y2, int x) {
        int from = Math.min(y1, y2);
        int to = Math.max(y1, y2);
        for (int y = from; y <= to; y++) {
            setFloor(map, x, y);
            setFloor(map, x - 1, y);
            setFloor(map, x + 1, y);
        }
    }

    private void carveGateway(int[] map, int x, int y, boolean horizontalEdge) {
        if (horizontalEdge) {
            setFloor(map, x - 1, y);
            setFloor(map, x, y);
            setFloor(map, x + 1, y);
        } else {
            setFloor(map, x, y - 1);
            setFloor(map, x, y);
            setFloor(map, x, y + 1);
        }
    }

    private void carveRect(int[] map, int left, int top, int right, int bottom, int terrain) {
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                if (x >= 0 && y >= 0 && x < MAP_SIZE && y < MAP_SIZE) {
                    map[x + y * MAP_SIZE] = terrain;
                }
            }
        }
    }

    private void setFloor(int[] map, int x, int y) {
        if (x >= 0 && y >= 0 && x < MAP_SIZE && y < MAP_SIZE) {
            map[x + y * MAP_SIZE] = Terrain.EMPTY;
        }
    }

    private void setIfFloor(int[] map, int x, int y, int terrain) {
        if (x >= 0 && y >= 0 && x < MAP_SIZE && y < MAP_SIZE) {
            int cell = x + y * MAP_SIZE;
            if (map[cell] == Terrain.EMPTY || map[cell] == Terrain.EMPTY_DECO
                    || map[cell] == Terrain.GRASS || map[cell] == Terrain.HIGH_GRASS) {
                map[cell] = terrain;
            }
        }
    }

    private int edgeHorizontal(int cx, int boundaryY) {
        return 5 + range(cx, boundaryY, 5001, 0, CHUNK_SIZE - 11);
    }

    private int edgeVertical(int boundaryX, int cy) {
        return 5 + range(boundaryX, cy, 5002, 0, CHUNK_SIZE - 11);
    }

    private boolean hasChest(int cx, int cy) {
        return Math.floorMod(hash(cx, cy, 6001), 3) == 0;
    }

    private int chestLocalX(int cx, int cy) {
        return 6 + range(cx, cy, 6101, 0, CHUNK_SIZE - 13);
    }

    private int chestLocalY(int cx, int cy) {
        return 6 + range(cx, cy, 6102, 0, CHUNK_SIZE - 13);
    }

    private int centerCellForChunk(int cx, int cy) {
        // Center chunk always occupies the middle third of the active window.
        int localX = CHUNK_SIZE + 12;
        int localY = CHUNK_SIZE + 12;
        return localX + localY * width();
    }

    private long worldKeyForLocalCell(int cell) {
        int lx = cell % width();
        int ly = cell / width();
        long wx = (long)(state().centerChunkX - 1) * CHUNK_SIZE + lx;
        long wy = (long)(state().centerChunkY - 1) * CHUNK_SIZE + ly;
        return encodeWorld(wx, wy);
    }

    private static long encodeWorld(long x, long y) {
        return (x << 32) ^ (y & 0xFFFFFFFFL);
    }

    private int range(int x, int y, int salt, int min, int max) {
        int span = max - min + 1;
        return min + (int)Math.floorMod(hash(x, y, salt), (long)span);
    }

    private long hash(int x, int y, int salt) {
        long z = Dungeon.seed;
        z ^= 0x9E3779B97F4A7C15L * (x + 0x632BE5ABL);
        z ^= 0xC2B2AE3D27D4EB4FL * (y + 0x85157AF5L);
        z ^= 0x165667B19E3779F9L * salt;
        z ^= (z >>> 33);
        z *= 0xff51afd7ed558ccdL;
        z ^= (z >>> 33);
        z *= 0xc4ceb9fe1a85ec53L;
        z ^= (z >>> 33);
        return z;
    }

    private interface ChunkVisitor {
        void visit(int cx, int cy, int ox, int oy);
    }

    private void forEachActiveChunk(ChunkVisitor visitor) {
        InfiniteWorldState state = state();
        for (int wy = -1; wy <= 1; wy++) {
            for (int wx = -1; wx <= 1; wx++) {
                visitor.visit(
                        state.centerChunkX + wx,
                        state.centerChunkY + wy,
                        (wx + 1) * CHUNK_SIZE,
                        (wy + 1) * CHUNK_SIZE);
            }
        }
    }
}

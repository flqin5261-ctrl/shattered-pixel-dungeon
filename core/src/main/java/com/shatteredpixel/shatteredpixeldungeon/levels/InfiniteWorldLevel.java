/*
 * Shattered Pixel Dungeon - Assist Edition
 * Infinite world prototype, generator v2.
 */
package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Arrays;

public class InfiniteWorldLevel extends Level {

    public static final int CHUNK_SIZE = 24;

    // Seven chunks are rendered at once. The active canvas is therefore 168x168,
    // but only nearby actors will be simulated when mobs are added later.
    public static final int WINDOW_CHUNKS = 7;
    private static final int HALF_WINDOW = WINDOW_CHUNKS / 2;
    private static final int MAP_SIZE = CHUNK_SIZE * WINDOW_CHUNKS;

    // Shift three chunks at once. This leaves a two-chunk safety margin and makes
    // streaming far less frequent than v0.3's every-chunk scene rebuild.
    private static final int SHIFT_STEP = 3;
    private static final int SHIFT_LOW = CHUNK_SIZE * 2;
    private static final int SHIFT_HIGH = CHUNK_SIZE * (WINDOW_CHUNKS - 2);

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

        InfiniteWorldState st = state();
        if (!st.heroWorldInitialized) {
            st.heroWorldX = 12;
            st.heroWorldY = 12;
            st.heroWorldInitialized = true;
        }
        st.markChunkExplored(st.centerChunkX, st.centerChunkY);

        int[] base = generateBaseWindow(st.centerChunkX, st.centerChunkY);
        System.arraycopy(base, 0, map, 0, map.length);
        applyTerrainOverrides();
        restoreExploration();

        transitions.clear();
        transitions.add(new LevelTransition(this, centerCell(), LevelTransition.Type.REGULAR_ENTRANCE));
        return true;
    }

    @Override
    protected void createMobs() {
        // Generator v2 still intentionally spawns no enemies.
        // Later versions will save/freeze mobs per world chunk and only simulate nearby chunks.
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
        // This world has no stairs/floors. The transition only exists as a spawn anchor.
        return false;
    }

    @Override
    public int randomRespawnCell(Char ch) {
        return centerCell();
    }

    /**
     * Called after the hero moves. The local 168x168 canvas is recentred in-place
     * when the hero reaches the outer streaming band. No GameScene recreation is used.
     */
    public boolean afterHeroMove(final Hero hero) {
        if (shifting || hero == null || Dungeon.level != this) return false;

        int x = hero.pos % width();
        int y = hero.pos / width();

        InfiniteWorldState st = state();
        int worldXNow = (st.centerChunkX - HALF_WINDOW) * CHUNK_SIZE + x;
        int worldYNow = (st.centerChunkY - HALF_WINDOW) * CHUNK_SIZE + y;
        st.heroWorldX = worldXNow;
        st.heroWorldY = worldYNow;
        st.heroWorldInitialized = true;
        st.markChunkExplored(Math.floorDiv(worldXNow, CHUNK_SIZE), Math.floorDiv(worldYNow, CHUNK_SIZE));

        int shiftX = 0;
        int shiftY = 0;

        if (x < SHIFT_LOW) shiftX = -SHIFT_STEP;
        else if (x >= SHIFT_HIGH) shiftX = SHIFT_STEP;

        if (y < SHIFT_LOW) shiftY = -SHIFT_STEP;
        else if (y >= SHIFT_HIGH) shiftY = SHIFT_STEP;

        if (shiftX == 0 && shiftY == 0) return false;

        shifting = true;

        int oldCenterX = st.centerChunkX;
        int oldCenterY = st.centerChunkY;

        int worldX = st.heroWorldX;
        int worldY = st.heroWorldY;

        // Preserve any cell-based action as an absolute-world target before the
        // local window is rebased. Otherwise a long path can resume toward the
        // old local cell, which looks like the hero is walking back to spawn.
        int curTargetWorldX = Integer.MIN_VALUE;
        int curTargetWorldY = Integer.MIN_VALUE;
        int lastTargetWorldX = Integer.MIN_VALUE;
        int lastTargetWorldY = Integer.MIN_VALUE;

        if (isCellAction(hero.curAction)) {
            curTargetWorldX = (oldCenterX - HALF_WINDOW) * CHUNK_SIZE + hero.curAction.dst % width();
            curTargetWorldY = (oldCenterY - HALF_WINDOW) * CHUNK_SIZE + hero.curAction.dst / width();
        }
        if (isCellAction(hero.lastAction)) {
            lastTargetWorldX = (oldCenterX - HALF_WINDOW) * CHUNK_SIZE + hero.lastAction.dst % width();
            lastTargetWorldY = (oldCenterY - HALF_WINDOW) * CHUNK_SIZE + hero.lastAction.dst / width();
        }

        snapshotForSave();

        st.centerChunkX += shiftX;
        st.centerChunkY += shiftY;

        rebuildWindow();

        int newLocalX = worldX - (st.centerChunkX - HALF_WINDOW) * CHUNK_SIZE;
        int newLocalY = worldY - (st.centerChunkY - HALF_WINDOW) * CHUNK_SIZE;

        // Absolute coordinates are authoritative. A bad rebase must never silently
        // fall back to the spawn/entrance position.
        if (newLocalX < 1 || newLocalX >= width()-1
                || newLocalY < 1 || newLocalY >= height()-1) {
            st.centerChunkX = oldCenterX;
            st.centerChunkY = oldCenterY;
            rebuildWindow();
            shifting = false;
            return false;
        }

        hero.pos = newLocalX + newLocalY * width();

        // Translate active/paused destinations into the new local window. If a target
        // falls outside the new canvas, cancel it instead of allowing stale local
        // coordinates to pull the hero back across the world.
        hero.curAction = rebaseCellAction(hero.curAction, curTargetWorldX, curTargetWorldY, st);
        hero.lastAction = rebaseCellAction(hero.lastAction, lastTargetWorldX, lastTargetWorldY, st);

        final int shiftedCellsX = shiftX * CHUNK_SIZE;
        final int shiftedCellsY = shiftY * CHUNK_SIZE;

        ShatteredPixelDungeon.runOnRenderThread(new Callback() {
            @Override
            public void call() {
                GameScene.refreshInfiniteWorldWindow(shiftedCellsX, shiftedCellsY);
                shifting = false;
            }
        });

        return true;
    }

    private boolean isCellAction(HeroAction action) {
        return action instanceof HeroAction.Move
                || action instanceof HeroAction.PickUp
                || action instanceof HeroAction.OpenChest
                || action instanceof HeroAction.Buy
                || action instanceof HeroAction.Unlock
                || action instanceof HeroAction.LvlTransition
                || action instanceof HeroAction.Mine
                || action instanceof HeroAction.Alchemy;
    }

    private HeroAction rebaseCellAction(HeroAction action, int worldX, int worldY, InfiniteWorldState st) {
        if (!isCellAction(action) || worldX == Integer.MIN_VALUE || worldY == Integer.MIN_VALUE) {
            return action;
        }

        int localX = worldX - (st.centerChunkX - HALF_WINDOW) * CHUNK_SIZE;
        int localY = worldY - (st.centerChunkY - HALF_WINDOW) * CHUNK_SIZE;

        if (localX < 0 || localX >= width() || localY < 0 || localY >= height()) {
            return null;
        }

        action.dst = localX + localY * width();
        return action;
    }

    public void snapshotForSave() {
        InfiniteWorldState st = state();

        if (Dungeon.hero != null) {
            int hx = Dungeon.hero.pos % width();
            int hy = Dungeon.hero.pos / width();
            st.heroWorldX = (st.centerChunkX - HALF_WINDOW) * CHUNK_SIZE + hx;
            st.heroWorldY = (st.centerChunkY - HALF_WINDOW) * CHUNK_SIZE + hy;
            st.heroWorldInitialized = true;
        }

        int[] base = generateBaseWindow(st.centerChunkX, st.centerChunkY);

        for (int cell = 0; cell < length(); cell++) {
            long key = worldKeyForLocalCell(cell);

            if (map[cell] != base[cell]) st.setTerrainOverride(key, map[cell]);
            else st.setTerrainOverride(key, null);

            if (visited[cell]) st.markVisited(key);
            if (mapped[cell]) st.markMapped(key);
        }

        snapshotChestStates();
    }

    private void rebuildWindow() {
        InfiniteWorldState st = state();

        int[] base = generateBaseWindow(st.centerChunkX, st.centerChunkY);
        System.arraycopy(base, 0, map, 0, map.length);
        applyTerrainOverrides();

        Arrays.fill(visited, false);
        Arrays.fill(mapped, false);
        Arrays.fill(heroFOV, false);
        restoreExploration();

        transitions.clear();
        transitions.add(new LevelTransition(this, centerCell(), LevelTransition.Type.REGULAR_ENTRANCE));

        // Keep the same container objects so GameScene tile layers retain valid references.
        heaps.clear();
        plants.clear();
        traps.clear();
        blobs.clear();
        customTiles.clear();
        customTerrain.clear();
        customWalls.clear();

        buildFlagMaps();
        cleanWalls();
        PathFinder.setMapSize(width(), height());

        generateChestHeaps();
    }

    private void applyTerrainOverrides() {
        InfiniteWorldState st = state();
        for (int cell = 0; cell < length(); cell++) {
            Integer override = st.terrainOverride(worldKeyForLocalCell(cell));
            if (override != null) map[cell] = override;
        }
    }

    private void restoreExploration() {
        InfiniteWorldState st = state();

        for (int cell = 0; cell < length(); cell++) {
            long worldKey = worldKeyForLocalCell(cell);
            int wx = worldXForLocalCell(cell);
            int wy = worldYForLocalCell(cell);
            int cx = Math.floorDiv(wx, CHUNK_SIZE);
            int cy = Math.floorDiv(wy, CHUNK_SIZE);

            // Generated only means "the generator has produced this chunk before".
            // It is deliberately NOT the same as explored. Only genuinely observed cells,
            // or cells revealed by mapping effects, remain visible when revisiting.
            mapped[cell] = st.wasMapped(worldKey);
            visited[cell] = st.wasVisited(worldKey);
        }
    }

    private void snapshotChestStates() {
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                if (!hasChest(cx, cy)) return;

                int lx = chestLocalX(cx, cy);
                int ly = chestLocalY(cx, cy);
                int cell = ox + lx + (oy + ly) * width();
                long key = encodeWorld((long)cx * CHUNK_SIZE + lx, (long)cy * CHUNK_SIZE + ly);

                Heap heap = heaps.get(cell);
                if (heap == null) st.setChestState(key, 2);
                else if (heap.type == Heap.Type.CHEST) st.setChestState(key, 0);
                else st.setChestState(key, 1);
            }
        });
    }

    private void generateChestHeaps() {
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                if (!hasChest(cx, cy)) return;

                int lx = chestLocalX(cx, cy);
                int ly = chestLocalY(cx, cy);
                int cell = ox + lx + (oy + ly) * width();
                long key = encodeWorld((long)cx * CHUNK_SIZE + lx, (long)cy * CHUNK_SIZE + ly);
                int chestState = st.chestState(key);

                if (chestState >= 2) return;

                Heap heap = new Heap();
                heap.pos = cell;
                heap.seen = mapped[cell] || visited[cell];
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

        InfiniteWorldState st = state();

        for (int wy = -HALF_WINDOW; wy <= HALF_WINDOW; wy++) {
            for (int wx = -HALF_WINDOW; wx <= HALF_WINDOW; wx++) {
                int cx = centerChunkX + wx;
                int cy = centerChunkY + wy;
                int ox = (wx + HALF_WINDOW) * CHUNK_SIZE;
                int oy = (wy + HALF_WINDOW) * CHUNK_SIZE;

                if (st.generatorVersion <= 1) generateChunkV1(result, cx, cy, ox, oy);
                else generateChunkV2(result, cx, cy, ox, oy);

                st.markChunkGenerated(cx, cy);
            }
        }

        return result;
    }

    // ------------------------------------------------------------------------
    // Generator v2: deterministic pseudo-random fragments with shared edges.
    // ------------------------------------------------------------------------

    private void generateChunkV2(int[] out, int cx, int cy, int ox, int oy) {
        int northX = edgeHorizontal(cx, cy);
        int southX = edgeHorizontal(cx, cy + 1);
        int westY = edgeVertical(cx, cy);
        int eastY = edgeVertical(cx + 1, cy);

        int hubCount = 3 + range(cx, cy, 101, 0, 3);
        int[] hx = new int[hubCount];
        int[] hy = new int[hubCount];

        for (int i = 0; i < hubCount; i++) {
            hx[i] = 4 + range(cx, cy, 110 + i * 2, 0, CHUNK_SIZE - 9);
            hy[i] = 4 + range(cx, cy, 111 + i * 2, 0, CHUNK_SIZE - 9);
            carveHub(out, cx, cy, ox, oy, hx[i], hy[i], i);
        }

        // Connect the hubs into a guaranteed tree first.
        for (int i = 1; i < hubCount; i++) {
            int parent = range(cx, cy, 180 + i, 0, i - 1);
            carveWanderPath(out,
                    ox + hx[i], oy + hy[i],
                    ox + hx[parent], oy + hy[parent],
                    cx, cy, 200 + i);
        }

        // Shared edge gateways are each connected to a pseudo-random hub.
        int nHub = range(cx, cy, 250, 0, hubCount - 1);
        int sHub = range(cx, cy, 251, 0, hubCount - 1);
        int wHub = range(cx, cy, 252, 0, hubCount - 1);
        int eHub = range(cx, cy, 253, 0, hubCount - 1);

        carveWanderPath(out, ox + northX, oy, ox + hx[nHub], oy + hy[nHub], cx, cy, 260);
        carveWanderPath(out, ox + southX, oy + CHUNK_SIZE - 1, ox + hx[sHub], oy + hy[sHub], cx, cy, 261);
        carveWanderPath(out, ox, oy + westY, ox + hx[wHub], oy + hy[wHub], cx, cy, 262);
        carveWanderPath(out, ox + CHUNK_SIZE - 1, oy + eastY, ox + hx[eHub], oy + hy[eHub], cx, cy, 263);

        carveGateway(out, ox + northX, oy, true);
        carveGateway(out, ox + southX, oy + CHUNK_SIZE - 1, true);
        carveGateway(out, ox, oy + westY, false);
        carveGateway(out, ox + CHUNK_SIZE - 1, oy + eastY, false);

        // Extra loops and side routes make the chunk topology much less predictable.
        int extraLinks = 1 + range(cx, cy, 280, 0, 2);
        for (int i = 0; i < extraLinks; i++) {
            int a = range(cx, cy, 281 + i * 2, 0, hubCount - 1);
            int b = range(cx, cy, 282 + i * 2, 0, hubCount - 1);
            if (a != b) {
                carveWanderPath(out, ox + hx[a], oy + hy[a], ox + hx[b], oy + hy[b],
                        cx, cy, 300 + i);
            }
        }

        // Large-area style changes only every ~4 chunks, giving coherent pseudo-biomes.
        int regionX = Math.floorDiv(cx, 4);
        int regionY = Math.floorDiv(cy, 4);
        int biome = range(regionX, regionY, 401, 0, 4);
        decorateBiome(out, cx, cy, ox, oy, biome);

        // 0-2 doors placed only on narrow-ish floor cells.
        int doorCount = range(cx, cy, 430, 0, 2);
        for (int i = 0; i < doorCount; i++) {
            placeSafeDoor(out, cx, cy, ox, oy, 440 + i);
        }

        if (hasChest(cx, cy)) {
            int chestX = chestLocalX(cx, cy);
            int chestY = chestLocalY(cx, cy);
            carveHub(out, cx, cy, ox, oy, chestX, chestY, 90);
            int nearest = nearestHub(hx, hy, chestX, chestY);
            carveWanderPath(out, ox + chestX, oy + chestY, ox + hx[nearest], oy + hy[nearest],
                    cx, cy, 500);
            out[ox + chestX + (oy + chestY) * MAP_SIZE] = Terrain.EMPTY_DECO;
        }

        if (cx == 0 && cy == 0) {
            carveOriginPlaza(out, ox, oy);
        }
    }

    private void carveHub(int[] out, int cx, int cy, int ox, int oy, int x, int y, int index) {
        int shape = range(cx, cy, 520 + index, 0, 4);
        int rx = 2 + range(cx, cy, 560 + index, 0, 3);
        int ry = 2 + range(cx, cy, 600 + index, 0, 3);

        switch (shape) {
            case 0:
                carveRect(out, ox + x - rx, oy + y - ry, ox + x + rx, oy + y + ry, Terrain.EMPTY);
                break;
            case 1:
                for (int dy = -ry; dy <= ry; dy++) {
                    for (int dx = -rx; dx <= rx; dx++) {
                        if (Math.abs(dx) * ry + Math.abs(dy) * rx <= rx * ry + Math.max(rx, ry)) {
                            setFloor(out, ox + x + dx, oy + y + dy);
                        }
                    }
                }
                break;
            case 2:
                carveRect(out, ox + x - rx, oy + y - 1, ox + x + rx, oy + y + 1, Terrain.EMPTY);
                carveRect(out, ox + x - 1, oy + y - ry, ox + x + 1, oy + y + ry, Terrain.EMPTY);
                break;
            case 3:
                carveRect(out, ox + x - rx, oy + y - ry, ox + x + 1, oy + y + ry, Terrain.EMPTY);
                carveRect(out, ox + x - rx, oy + y - 1, ox + x + rx, oy + y + ry, Terrain.EMPTY);
                break;
            default:
                // noisy organic blob
                for (int dy = -ry - 1; dy <= ry + 1; dy++) {
                    for (int dx = -rx - 1; dx <= rx + 1; dx++) {
                        long n = hash(cx * 31 + x + dx, cy * 31 + y + dy, 650 + index);
                        float nx = dx / (float)(rx + 1);
                        float ny = dy / (float)(ry + 1);
                        if (nx * nx + ny * ny < 1.15f && Math.floorMod(n, 7) != 0) {
                            setFloor(out, ox + x + dx, oy + y + dy);
                        }
                    }
                }
                break;
        }
    }

    private void carveWanderPath(int[] out, int x1, int y1, int x2, int y2, int cx, int cy, int salt) {
        int x = x1;
        int y = y1;
        int guard = 0;

        setFloor(out, x, y);

        while ((x != x2 || y != y2) && guard++ < 160) {
            boolean canX = x != x2;
            boolean canY = y != y2;

            boolean takeX;
            if (!canY) takeX = true;
            else if (!canX) takeX = false;
            else {
                long h = hash(cx * 131 + x, cy * 131 + y, salt + guard);
                takeX = Math.floorMod(h, 100) < 50;
            }

            if (takeX) x += Integer.compare(x2, x);
            else y += Integer.compare(y2, y);

            setFloor(out, x, y);

            // Randomly widen parts of the route to avoid identical 3-wide corridors everywhere.
            long h = hash(cx * 97 + x, cy * 97 + y, salt + guard * 3);
            int widen = (int)Math.floorMod(h, 5);
            if (widen <= 1) {
                setFloor(out, x + 1, y);
                setFloor(out, x - 1, y);
            } else if (widen == 2) {
                setFloor(out, x, y + 1);
                setFloor(out, x, y - 1);
            }
        }
    }

    private void decorateBiome(int[] out, int cx, int cy, int ox, int oy, int biome) {
        for (int y = 2; y < CHUNK_SIZE - 2; y++) {
            for (int x = 2; x < CHUNK_SIZE - 2; x++) {
                int cell = ox + x + (oy + y) * MAP_SIZE;
                if (out[cell] != Terrain.EMPTY && out[cell] != Terrain.EMPTY_DECO) continue;

                long h = hash(cx * CHUNK_SIZE + x, cy * CHUNK_SIZE + y, 800 + biome);
                int roll = (int)Math.floorMod(h, 100);

                switch (biome) {
                    case 0: // overgrown courts
                        if (roll < 18) out[cell] = Terrain.HIGH_GRASS;
                        else if (roll < 30) out[cell] = Terrain.GRASS;
                        break;
                    case 1: // flooded ruins
                        if (roll < 15 && floorNeighbours(out, ox + x, oy + y) >= 5) out[cell] = Terrain.WATER;
                        else if (roll < 22) out[cell] = Terrain.EMPTY_DECO;
                        break;
                    case 2: // archives
                        if (roll < 8 && floorNeighbours(out, ox + x, oy + y) >= 7) out[cell] = Terrain.BOOKSHELF;
                        else if (roll < 18) out[cell] = Terrain.EMPTY_DECO;
                        break;
                    case 3: // broken plazas
                        if (roll < 5 && floorNeighbours(out, ox + x, oy + y) >= 7) out[cell] = Terrain.STATUE;
                        else if (roll < 22) out[cell] = Terrain.EMPTY_DECO;
                        break;
                    default: // mixed old district
                        if (roll < 8) out[cell] = Terrain.GRASS;
                        else if (roll < 13 && floorNeighbours(out, ox + x, oy + y) >= 6) out[cell] = Terrain.WATER;
                        else if (roll < 20) out[cell] = Terrain.EMPTY_DECO;
                        break;
                }
            }
        }
    }

    private void placeSafeDoor(int[] out, int cx, int cy, int ox, int oy, int salt) {
        for (int attempt = 0; attempt < 18; attempt++) {
            int x = 3 + range(cx, cy, salt + attempt * 2, 0, CHUNK_SIZE - 7);
            int y = 3 + range(cx, cy, salt + attempt * 2 + 1, 0, CHUNK_SIZE - 7);
            int gx = ox + x;
            int gy = oy + y;
            int cell = gx + gy * MAP_SIZE;

            if (out[cell] != Terrain.EMPTY) continue;

            boolean lr = isFloorLike(out, gx - 1, gy) && isFloorLike(out, gx + 1, gy)
                    && !isFloorLike(out, gx, gy - 1) && !isFloorLike(out, gx, gy + 1);
            boolean ud = isFloorLike(out, gx, gy - 1) && isFloorLike(out, gx, gy + 1)
                    && !isFloorLike(out, gx - 1, gy) && !isFloorLike(out, gx + 1, gy);

            if (lr || ud) {
                out[cell] = Terrain.DOOR;
                return;
            }
        }
    }

    private void carveOriginPlaza(int[] out, int ox, int oy) {
        carveRect(out, ox + 6, oy + 6, ox + 17, oy + 17, Terrain.EMPTY);
        for (int x = 8; x <= 15; x++) {
            out[ox + x + (oy + 8) * MAP_SIZE] = Terrain.WATER;
            out[ox + x + (oy + 15) * MAP_SIZE] = Terrain.WATER;
        }
        for (int y = 9; y <= 14; y++) {
            out[ox + 8 + (oy + y) * MAP_SIZE] = Terrain.WATER;
            out[ox + 15 + (oy + y) * MAP_SIZE] = Terrain.WATER;
        }
        out[ox + 11 + (oy + 11) * MAP_SIZE] = Terrain.EMPTY_DECO;
        out[ox + 12 + (oy + 12) * MAP_SIZE] = Terrain.EMPTY_DECO;
    }

    private int nearestHub(int[] hx, int[] hy, int x, int y) {
        int best = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < hx.length; i++) {
            int d = Math.abs(hx[i] - x) + Math.abs(hy[i] - y);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private int floorNeighbours(int[] out, int x, int y) {
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (isFloorLike(out, x + dx, y + dy)) count++;
            }
        }
        return count;
    }

    private boolean isFloorLike(int[] out, int x, int y) {
        if (x < 0 || y < 0 || x >= MAP_SIZE || y >= MAP_SIZE) return false;
        int t = out[x + y * MAP_SIZE];
        return t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.GRASS
                || t == Terrain.HIGH_GRASS || t == Terrain.WATER || t == Terrain.DOOR;
    }

    // ------------------------------------------------------------------------
    // Generator v1 retained only so old 0.3 saves never mutate underneath players.
    // ------------------------------------------------------------------------

    private void generateChunkV1(int[] result, int cx, int cy, int ox, int oy) {
        int centerX = 9 + range(cx, cy, 11, 0, 5);
        int centerY = 9 + range(cx, cy, 12, 0, 5);

        int northX = edgeHorizontalV1(cx, cy);
        int southX = edgeHorizontalV1(cx, cy + 1);
        int westY = edgeVerticalV1(cx, cy);
        int eastY = edgeVerticalV1(cx + 1, cy);

        carveRect(result, ox + centerX - 4, oy + centerY - 4,
                ox + centerX + 4, oy + centerY + 4, Terrain.EMPTY);

        carvePathV1(result, ox + centerX, oy + centerY, ox + northX, oy, cx, cy, 21);
        carvePathV1(result, ox + centerX, oy + centerY, ox + southX, oy + CHUNK_SIZE - 1, cx, cy, 22);
        carvePathV1(result, ox + centerX, oy + centerY, ox, oy + westY, cx, cy, 23);
        carvePathV1(result, ox + centerX, oy + centerY, ox + CHUNK_SIZE - 1, oy + eastY, cx, cy, 24);

        carveGateway(result, ox + northX, oy, true);
        carveGateway(result, ox + southX, oy + CHUNK_SIZE - 1, true);
        carveGateway(result, ox, oy + westY, false);
        carveGateway(result, ox + CHUNK_SIZE - 1, oy + eastY, false);

        int style = range(cx, cy, 31, 0, 3);
        switch (style) {
            case 0:
                decorateGardenV1(result, cx, cy, ox, oy, centerX, centerY);
                break;
            case 1:
                decorateWaterCourtV1(result, ox, oy, centerX, centerY);
                break;
            case 2:
                decorateArchiveV1(result, ox, oy, centerX, centerY);
                break;
            default:
                decorateRuinsV1(result, cx, cy, ox, oy);
                break;
        }

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

        if (hasChestV1(cx, cy)) {
            int chestX = chestLocalXV1(cx, cy);
            int chestY = chestLocalYV1(cx, cy);
            carveRect(result, ox + chestX - 1, oy + chestY - 1,
                    ox + chestX + 1, oy + chestY + 1, Terrain.EMPTY);
            carvePathV1(result, ox + centerX, oy + centerY, ox + chestX, oy + chestY, cx, cy, 81);
            result[ox + chestX + (oy + chestY) * MAP_SIZE] = Terrain.EMPTY_DECO;
        }

        if (cx == 0 && cy == 0) carveOriginPlazaV1(result, ox, oy);
    }

    private void carveOriginPlazaV1(int[] result, int ox, int oy) {
        carveRect(result, ox + 7, oy + 7, ox + 16, oy + 16, Terrain.EMPTY);
        for (int x = 9; x <= 14; x++) {
            result[ox + x + (oy + 9) * MAP_SIZE] = Terrain.WATER;
            result[ox + x + (oy + 14) * MAP_SIZE] = Terrain.WATER;
        }
        result[ox + 11 + (oy + 11) * MAP_SIZE] = Terrain.EMPTY_DECO;
        result[ox + 12 + (oy + 12) * MAP_SIZE] = Terrain.EMPTY_DECO;
    }

    private void carvePathV1(int[] map, int x1, int y1, int x2, int y2, int cx, int cy, int salt) {
        boolean horizontalFirst = (hash(cx, cy, salt) & 1L) == 0;
        if (horizontalFirst) {
            carveHorizontal(map, x1, x2, y1);
            carveVertical(map, y1, y2, x2);
        } else {
            carveVertical(map, y1, y2, x1);
            carveHorizontal(map, x1, x2, y2);
        }
    }

    private void decorateGardenV1(int[] map, int cx, int cy, int ox, int oy, int centerX, int centerY) {
        for (int y = centerY - 3; y <= centerY + 3; y++) {
            for (int x = centerX - 3; x <= centerX + 3; x++) {
                long h = hash(cx * CHUNK_SIZE + x, cy * CHUNK_SIZE + y, 101);
                int cell = ox + x + (oy + y) * MAP_SIZE;
                if (map[cell] == Terrain.EMPTY && Math.floorMod(h, 5) == 0) map[cell] = Terrain.HIGH_GRASS;
            }
        }
    }

    private void decorateWaterCourtV1(int[] map, int ox, int oy, int centerX, int centerY) {
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

    private void decorateArchiveV1(int[] map, int ox, int oy, int centerX, int centerY) {
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

    private void decorateRuinsV1(int[] map, int cx, int cy, int ox, int oy) {
        for (int i = 0; i < 8; i++) {
            int x = 4 + range(cx, cy, 201 + i, 0, CHUNK_SIZE - 9);
            int y = 4 + range(cx, cy, 301 + i, 0, CHUNK_SIZE - 9);
            int cell = ox + x + (oy + y) * MAP_SIZE;
            if (map[cell] == Terrain.EMPTY) {
                map[cell] = (i % 3 == 0) ? Terrain.STATUE : Terrain.EMPTY_DECO;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Shared generator helpers.
    // ------------------------------------------------------------------------

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

    private int edgeHorizontalV1(int cx, int boundaryY) {
        return 5 + range(cx, boundaryY, 5001, 0, CHUNK_SIZE - 11);
    }

    private int edgeVerticalV1(int boundaryX, int cy) {
        return 5 + range(boundaryX, cy, 5002, 0, CHUNK_SIZE - 11);
    }

    private boolean hasChestV1(int cx, int cy) {
        return Math.floorMod(hash(cx, cy, 6001), 3) == 0;
    }

    private int chestLocalXV1(int cx, int cy) {
        return 6 + range(cx, cy, 6101, 0, CHUNK_SIZE - 13);
    }

    private int chestLocalYV1(int cx, int cy) {
        return 6 + range(cx, cy, 6102, 0, CHUNK_SIZE - 13);
    }

    private int edgeHorizontal(int cx, int boundaryY) {
        return 4 + range(cx, boundaryY, 5001, 0, CHUNK_SIZE - 9);
    }

    private int edgeVertical(int boundaryX, int cy) {
        return 4 + range(boundaryX, cy, 5002, 0, CHUNK_SIZE - 9);
    }

    private boolean hasChest(int cx, int cy) {
        return Math.floorMod(hash(cx, cy, 6001), 100) < 42;
    }

    private int chestLocalX(int cx, int cy) {
        return 5 + range(cx, cy, 6101, 0, CHUNK_SIZE - 11);
    }

    private int chestLocalY(int cx, int cy) {
        return 5 + range(cx, cy, 6102, 0, CHUNK_SIZE - 11);
    }

    private int centerCell() {
        int localX = HALF_WINDOW * CHUNK_SIZE + CHUNK_SIZE / 2;
        int localY = HALF_WINDOW * CHUNK_SIZE + CHUNK_SIZE / 2;
        return localX + localY * width();
    }

    private int worldXForLocalCell(int cell) {
        int lx = cell % width();
        return (state().centerChunkX - HALF_WINDOW) * CHUNK_SIZE + lx;
    }

    private int worldYForLocalCell(int cell) {
        int ly = cell / width();
        return (state().centerChunkY - HALF_WINDOW) * CHUNK_SIZE + ly;
    }

    private long worldKeyForLocalCell(int cell) {
        return encodeWorld(worldXForLocalCell(cell), worldYForLocalCell(cell));
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
        InfiniteWorldState st = state();
        for (int wy = -HALF_WINDOW; wy <= HALF_WINDOW; wy++) {
            for (int wx = -HALF_WINDOW; wx <= HALF_WINDOW; wx++) {
                visitor.visit(
                        st.centerChunkX + wx,
                        st.centerChunkY + wy,
                        (wx + HALF_WINDOW) * CHUNK_SIZE,
                        (wy + HALF_WINDOW) * CHUNK_SIZE);
            }
        }
    }
}

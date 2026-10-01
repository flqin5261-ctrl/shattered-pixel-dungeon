/*
 * Shattered Pixel Dungeon - Assist Edition
 * Infinite world prototype, generator v2.
 */
package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.InfiniteWorldShopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.*;
import com.shatteredpixel.shatteredpixeldungeon.items.journal.InfiniteWorldNote;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.*;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.*;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.InfiniteWorldAccentTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.InfiniteWorldUrbanSurfaceLayer;
import com.shatteredpixel.shatteredpixeldungeon.tiles.InfiniteWorldBackroomsMaterialLayer;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.InfiniteWorldDecorationLayer;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class InfiniteWorldLevel extends Level {

    public static boolean assistSpectatorActive() {
        return Dungeon.infiniteWorld
                && Dungeon.level instanceof InfiniteWorldLevel
                && SPDSettings.assistInfiniteSpectator();
    }

    public static final int CHUNK_SIZE = 24;

    // Seven chunks are rendered at once. The active canvas is therefore 168x168,
    // but only nearby actors will be simulated when mobs are added later.
    public static final int WINDOW_CHUNKS = 7;
    private static final int HALF_WINDOW = WINDOW_CHUNKS / 2;
    private static final int MAP_SIZE = CHUNK_SIZE * WINDOW_CHUNKS;

    // Shift three chunks at once. This leaves a two-chunk safety margin and makes
    // streaming far less frequent than v0.3's every-chunk scene rebuild.
    private static final int SHIFT_STEP = 3;
    // Keep a full one-chunk hysteresis band at each edge. After shifting 3 chunks,
    // the hero lands around y/x 72..96 instead of exactly on the opposite trigger.
    private static final int SHIFT_LOW = CHUNK_SIZE;
    private static final int SHIFT_HIGH = MAP_SIZE - CHUNK_SIZE;

    // V20 remains frozen for save compatibility.
    private static final int[] V20_BACKROOMS_CYCLE = {
            1, 0, 8, 2, 0, 15, 3, 0, 10, 4,
            17, 0, 5, 12, 0, 6, 18, 7, 0, 13,
            9, 0, 19, 11, 14, 0, 16, 20, 0
    };

    // V21 encounter order. Every type appears exactly once before any repeats.
    // Geometry controls spacing separately, so rarity can be tuned without losing
    // the guarantee that long exploration eventually visits all twenty themes.
    private static final int[] V21_BACKROOMS_ORDER = {
            1, 8, 2, 15, 3, 10, 4, 17, 5, 12,
            6, 18, 7, 13, 9, 19, 11, 14, 16, 20
    };

    private boolean shifting;
    private int streamingShiftsSinceCheckpoint;

    // Runtime-only throttles for per-step Infinite World housekeeping.
    private int lastProcessedHeroChunkX = Integer.MIN_VALUE;
    private int lastProcessedHeroChunkY = Integer.MIN_VALUE;
    private int lastPruneWorldX = Integer.MIN_VALUE;
    private int lastPruneWorldY = Integer.MIN_VALUE;

    // A long-range Genesis teleport may land inside the normal streaming trigger
    // band. Give the player a small safe grace band so the next ordinary step
    // does not immediately force a 7x7 window rebuild.
    private boolean genesisTeleportGrace;

    // Infinite World ecology scales with Hero progression: still sparse early,
    // but 0.8.5 raises mid/late dynamic pressure one step over 0.8.4.
    // Six is the normal late-game target; seven remains the hard safety cap.
    private static final int MOB_SPAWN_TARGET_CAP = 6;
    private static final int MOB_HARD_CAP = 7;
    private static final int MOB_SPAWN_MIN_DISTANCE = 14;
    private static final int MOB_SPAWN_MAX_DISTANCE = 28;
    private static final int MOB_DESPAWN_DISTANCE = 40;
    private static final int ELITE_HARD_CAP = 1;
    private static final int ELITE_CHANCE_PERCENT = 6;
    private static final float MOB_RESPAWN_MIN_TURNS = 32f;
    private static final float MOB_RESPAWN_MAX_TURNS = 50f;

    private InfiniteWorldMobEcology mobEcology;

    // Merchant outposts are intentionally much rarer than ordinary rooms.
    // A 7-chunk lattice is just wider than the six-chunk coordinate span of the
    // 7x7 active window, so at most one travelling merchant can be active at once.
    // It is also dense enough that a long straight exploration route can eventually
    // bring an outpost into the loaded window instead of missing every shop row.
    private static final int MERCHANT_SPACING_CHUNKS = 7;
    private static final int MERCHANT_SPACING_CHUNKS_V12 = 5;
    private static final int MERCHANT_SPACING_CHUNKS_V13 = 4;
    private static final int MERCHANT_SPACING_CHUNKS_V15 = 3;
    private static final int MERCHANT_STOCK_SLOTS = 6;
    private static final int MERCHANT_ROOM_THEME = 10;

    // V17 pacing guarantees: after 1000 positive hero action-time the run receives
    // exactly one visible crystal chest in the Hero-centered 3x3 neighbourhood.
    private static final float GUARANTEED_ARTIFACT_CHEST_ACTION_VALUE = 1000f;

    // Keep recently generated chunks in memory. The world itself is still seed-driven;
    // this cache only avoids rebuilding chunks when the player walks back and forth.
    private static final int CHUNK_CACHE_LIMIT = 128;
    private final LinkedHashMap<Long, int[]> chunkCache =
            new LinkedHashMap<Long, int[]>(CHUNK_CACHE_LIMIT, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
                    return size() > CHUNK_CACHE_LIMIT;
                }
            };

    // Exact unmodified terrain for the current 7x7 window. Reusing this avoids
    // regenerating all 49 chunks every time exploration state is snapshotted.
    private int[] baseWindow;

    {
        color1 = 0x36585F;
        color2 = 0x7AA7A1;
    }

    private int visualTheme() {
        // Keep old v2 worlds visually compatible. New v3 worlds pick one complete
        // upstream tileset from the world seed and keep it for the lifetime of the save.
        if (state().generatorVersion <= 2) return 3;
        return (int)Math.floorMod(Dungeon.seed ^ (Dungeon.seed >>> 32), 5L);
    }

    @Override
    public String tilesTex() {
        switch (visualTheme()) {
            case 0: return Assets.Environment.TILES_SEWERS;
            case 1: return Assets.Environment.TILES_PRISON;
            case 2: return Assets.Environment.TILES_CAVES;
            case 4: return Assets.Environment.TILES_HALLS;
            case 3:
            default:return Assets.Environment.TILES_CITY;
        }
    }

    @Override
    public String waterTex() {
        switch (visualTheme()) {
            case 0: return Assets.Environment.WATER_SEWERS;
            case 1: return Assets.Environment.WATER_PRISON;
            case 2: return Assets.Environment.WATER_CAVES;
            case 4: return Assets.Environment.WATER_HALLS;
            case 3:
            default:return Assets.Environment.WATER_CITY;
        }
    }

    @Override
    public void playLevelMusic() {
        // Infinite World is a long-running exploration mode; rotate through all
        // normal regional tracks instead of looping a single song forever.
        String[] tracks = new String[]{
                Assets.Music.SEWERS_1, Assets.Music.SEWERS_2, Assets.Music.SEWERS_3,
                Assets.Music.PRISON_1, Assets.Music.PRISON_2, Assets.Music.PRISON_3,
                Assets.Music.CAVES_1, Assets.Music.CAVES_2, Assets.Music.CAVES_3,
                Assets.Music.CITY_1, Assets.Music.CITY_2, Assets.Music.CITY_3,
                Assets.Music.HALLS_1, Assets.Music.HALLS_2, Assets.Music.HALLS_3
        };
        float[] chances = new float[tracks.length];
        Arrays.fill(chances, 1f);
        Music.INSTANCE.playTracks(tracks, chances, true);
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

        baseWindow = generateBaseWindow(st.centerChunkX, st.centerChunkY);
        System.arraycopy(baseWindow, 0, map, 0, map.length);
        applyTerrainOverrides();
        repairLegacyMerchantAccessInWindow();
        restoreExploration();
        rebuildAccentTiles();

        transitions.clear();
        transitions.add(new LevelTransition(this, centerCell(), LevelTransition.Type.REGULAR_ENTRANCE));
        return true;
    }

    @Override
    protected void createMobs() {
        // Enemies are still not pre-populated. Deterministic merchant NPCs are
        // different: they belong to fixed world outposts and are created with the level.
        ensureV11Shopkeeper(false);
    }

    @Override
    public Actor addRespawner() {
        if (mobEcology == null) {
            mobEcology = new InfiniteWorldMobEcology();
        }
        float min = mobRespawnMinTurns();
        float max = mobRespawnMaxTurns();
        Actor.addDelayed(mobEcology, min + Random.Float() * (max - min));
        return mobEcology;
    }

    @Override
    public int mobLimit() {
        return MOB_HARD_CAP;
    }

    private int mobEcologyStage() {
        int level = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
        if (level < 10) return 0;
        if (level < 30) return 1;
        if (level < 45) return 2;
        return 3;
    }

    private int dynamicMobTargetCap() {
        switch (mobEcologyStage()) {
            case 0: return 2;
            case 1: return 3;
            case 2: return 5;
            default:return MOB_SPAWN_TARGET_CAP;
        }
    }

    private float mobRespawnMinTurns() {
        switch (mobEcologyStage()) {
            case 0: return 42f;
            case 1: return 34f;
            case 2: return 27f;
            default:return 22f;
        }
    }

    private float mobRespawnMaxTurns() {
        switch (mobEcologyStage()) {
            case 0: return 62f;
            case 1: return 52f;
            case 2: return 40f;
            default:return 34f;
        }
    }

    private float mobSpawnChance(int count) {
        switch (mobEcologyStage()) {
            case 0:
                return count == 0 ? 0.48f : 0.24f;
            case 1:
                if (count == 0) return 0.65f;
                if (count == 1) return 0.47f;
                return 0.28f;
            case 2:
                if (count == 0) return 0.78f;
                if (count <= 1) return 0.60f;
                return 0.38f;
            default:
                if (count == 0) return 0.86f;
                if (count <= 2) return 0.70f;
                return 0.48f;
        }
    }

    @Override
    public Mob createMob() {
        Mob mob = createInfiniteWorldMob();
        if (Dungeon.hero != null) {
            InfiniteWorldProgression.applyDynamicScaling(mob, Dungeon.hero);
        }
        return mob;
    }

    @Override
    public int randomRespawnCell(Char ch) {
        return findInfiniteWorldMobSpawnCell(ch);
    }

    private class InfiniteWorldMobEcology extends Actor {
        {
            actPriority = BUFF_PRIO;
        }

        @Override
        protected boolean act() {
            if (Dungeon.level != InfiniteWorldLevel.this
                    || Dungeon.hero == null
                    || !Dungeon.hero.isAlive()) {
                Actor.remove(this);
                return true;
            }

            // Spectator test mode is for terrain/streaming QA. It must not cause
            // exploration to populate the window with new enemies.
            if (assistSpectatorActive()) {
                spend(mobRespawnMinTurns());
                return true;
            }

            pruneInfiniteWorldMobs();

            int count = activeEnemyCount();
            int targetCap = dynamicMobTargetCap();
            if (count < targetCap) {
                float chance = mobSpawnChance(count);

                int heroChunkX = Math.floorDiv(state().heroWorldX, CHUNK_SIZE);
                int heroChunkY = Math.floorDiv(state().heroWorldY, CHUNK_SIZE);
                int anomaly = v9AnomalyType(heroChunkX, heroChunkY);
                if (anomaly != 0) {
                    // Every Backrooms district now has its own ecology instead of
                    // sharing one generic spawn multiplier.
                    chance *= v19AnomalyMobFactor(anomaly);
                }

                if (Random.Float() < chance) {
                    spawnInfiniteWorldMob();
                }
            }

            float min = mobRespawnMinTurns();
            float max = mobRespawnMaxTurns();
            spend(min + Random.Float() * (max - min));
            return true;
        }
    }

    private float v19AnomalyMobFactor(int anomaly) {
        switch (anomaly) {
            case 1:  return 0.30f;
            case 2:  return 0.50f;
            case 3:  return 0.62f;
            case 4:  return 0.72f;
            case 5:  return 0.38f;
            case 6:  return 0.50f;
            case 7:  return 0.24f;
            case 8:  return 0.35f;
            case 9:  return 0.62f;
            case 10: return 0.42f;
            case 11: return 0.00f; // Level 10: canon source lists no hostile entities
            case 12: return 0.55f;
            case 13: return 0.00f; // Level 37: survival class 0, devoid of entities
            case 14: return 0.32f;
            case 15: return 0.28f;
            case 16: return 0.08f;
            case 17: return 0.78f;
            case 18: return 0.22f;
            case 19: return 0.12f;
            case 20: return 0.00f;
            default: return 0.45f;
        }
    }

    private boolean spawnInfiniteWorldMob() {
        if (Dungeon.hero == null || !Dungeon.hero.isAlive()) return false;
        if (activeEnemyCount() >= dynamicMobTargetCap()) return false;

        Mob mob = createMob();
        int cell = findInfiniteWorldMobSpawnCell(mob);
        if (cell < 0) return false;

        mob.pos = cell;
        mob.state = mob.WANDERING;
        maybeMakeInfiniteWorldElite(mob);

        GameScene.add(mob, Random.Float());
        return true;
    }

    private Mob createInfiniteWorldMob() {
        int heroLevel = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
        int dynamicLevel = Dungeon.hero == null ? heroLevel
                : InfiniteWorldProgression.dynamicMonsterLevel(Dungeon.hero);
        int worldChunkX = Math.floorDiv(state().heroWorldX, CHUNK_SIZE);
        int worldChunkY = Math.floorDiv(state().heroWorldY, CHUNK_SIZE);
        int worldDistance = Math.max(Math.abs(worldChunkX), Math.abs(worldChunkY));

        int tier = 0;
        if (dynamicLevel >= 7 || heroLevel >= 4 || worldDistance >= 10) tier = 1;
        if (dynamicLevel >= 16 || heroLevel >= 9 || worldDistance >= 24) tier = 2;
        if (dynamicLevel >= 30) tier = 3;
        if (dynamicLevel >= 45) tier = 4;

        int roll = Random.Int(100);

        if (tier >= 4 && roll < 45) {
            switch (Random.Int(5)) {
                case 0: return new Golem();
                case 1: return new Succubus();
                case 2: return new Scorpio();
                case 3: return new Warlock();
                default:return new Monk();
            }
        }

        if (tier >= 3 && roll < 38) {
            switch (Random.Int(5)) {
                case 0: return new Warlock();
                case 1: return new Monk();
                case 2: return new Golem();
                case 3: return new Spinner();
                default:return new Brute();
            }
        }

        if (tier >= 2 && roll < 28) {
            switch (Random.Int(5)) {
                case 0: return new Warlock();
                case 1: return new Monk();
                case 2: return new Spinner();
                case 3: return new Brute();
                default:return new Bat();
            }
        }

        if (tier >= 1 && roll < 58) {
            switch (Random.Int(5)) {
                case 0: return new Skeleton();
                case 1: return new DM100();
                case 2: return new Bat();
                case 3: return new Brute();
                default:return new Spinner();
            }
        }

        switch (Random.Int(5)) {
            case 0: return new Rat();
            case 1: return new Snake();
            case 2: return new Gnoll();
            case 3: return new Crab();
            default:return new Slime();
        }
    }

    private void maybeMakeInfiniteWorldElite(Mob mob) {
        if (mob == null || activeEliteCount() >= ELITE_HARD_CAP) return;

        int heroLevel = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
        int worldChunkX = Math.floorDiv(state().heroWorldX, CHUNK_SIZE);
        int worldChunkY = Math.floorDiv(state().heroWorldY, CHUNK_SIZE);
        int worldDistance = Math.max(Math.abs(worldChunkX), Math.abs(worldChunkY));

        // Don't surprise a brand-new character with an elite at the spawn area.
        if (heroLevel < 3 && worldDistance < 8) return;
        if (Random.Int(100) >= ELITE_CHANCE_PERCENT) return;

        // Blazing is intentionally excluded because detaching it creates fire,
        // which would violate reward/effect-free distance despawning. Giant is
        // excluded because it changes placement/open-space requirements.
        switch (Random.Int(4)) {
            case 0:
                Buff.affect(mob, ChampionEnemy.Projecting.class);
                break;
            case 1:
                Buff.affect(mob, ChampionEnemy.AntiMagic.class);
                break;
            case 2:
                Buff.affect(mob, ChampionEnemy.Blessed.class);
                break;
            default:
                Buff.affect(mob, ChampionEnemy.Growing.class);
                break;
        }
    }

    private int findInfiniteWorldMobSpawnCell(Char ch) {
        if (Dungeon.hero == null || ch == null) return -1;

        boolean[] walkable = BArray.or(passable, avoid, null);
        PathFinder.buildDistanceMap(Dungeon.hero.pos, walkable, MOB_SPAWN_MAX_DISTANCE);

        int hx = Dungeon.hero.pos % width();
        int hy = Dungeon.hero.pos / width();

        for (int tries = 0; tries < 80; tries++) {
            int x = hx + Random.Int(MOB_SPAWN_MAX_DISTANCE * 2 + 1) - MOB_SPAWN_MAX_DISTANCE;
            int y = hy + Random.Int(MOB_SPAWN_MAX_DISTANCE * 2 + 1) - MOB_SPAWN_MAX_DISTANCE;

            if (x <= 1 || x >= width() - 2 || y <= 1 || y >= height() - 2) continue;

            int cell = x + y * width();
            int dist = PathFinder.distance[cell];

            if (dist < MOB_SPAWN_MIN_DISTANCE || dist > MOB_SPAWN_MAX_DISTANCE) continue;
            if (heroFOV[cell]) continue;
            if (!passable[cell] || solid[cell] || pit[cell] || secret[cell]) continue;
            if (Actor.findChar(cell) != null) continue;
            if (nearV11Shopkeeper(cell, 8)) continue;
            if (heaps.get(cell) != null || traps.get(cell) != null || plants.get(cell) != null) continue;
            if (Char.hasProp(ch, Char.Property.LARGE) && !openSpace[cell]) continue;

            return cell;
        }

        return -1;
    }

    private boolean nearV11Shopkeeper(int cell, int radius) {
        int x = cell % width();
        int y = cell / width();
        for (Mob mob : mobs.toArray(new Mob[0])) {
            if (!(mob instanceof InfiniteWorldShopkeeper)) continue;
            int mx = mob.pos % width();
            int my = mob.pos / width();
            if (Math.max(Math.abs(mx - x), Math.abs(my - y)) <= radius) return true;
        }
        return false;
    }

    private int activeEnemyCount() {
        int count = 0;
        for (Mob mob : mobs.toArray(new Mob[0])) {
            if (mob.alignment == Char.Alignment.ENEMY
                    && !mob.properties().contains(Char.Property.BOSS)
                    && !mob.properties().contains(Char.Property.MINIBOSS)) {
                count++;
            }
        }
        return count;
    }

    private int activeEliteCount() {
        int count = 0;
        for (Mob mob : mobs.toArray(new Mob[0])) {
            if (mob.alignment == Char.Alignment.ENEMY
                    && !mob.buffs(ChampionEnemy.class).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void pruneInfiniteWorldMobs() {
        if (Dungeon.hero == null) return;

        int hx = Dungeon.hero.pos % width();
        int hy = Dungeon.hero.pos / width();

        ArrayList<Mob> managed = new ArrayList<>();
        for (Mob mob : mobs.toArray(new Mob[0])) {
            if (mob.alignment != Char.Alignment.ENEMY) continue;
            if (mob.properties().contains(Char.Property.BOSS)
                    || mob.properties().contains(Char.Property.MINIBOSS)) continue;

            int mx = mob.pos % width();
            int my = mob.pos / width();
            int dist = Math.max(Math.abs(mx - hx), Math.abs(my - hy));

            if (dist > MOB_DESPAWN_DISTANCE) {
                mob.despawnFromInfiniteWorld();
            } else {
                managed.add(mob);
            }
        }

        // Runtime effects can occasionally add enemies outside the ecology
        // controller. Keep an absolute cap by removing the farthest extras first.
        while (managed.size() > MOB_HARD_CAP) {
            Mob farthest = null;
            int farthestDist = -1;
            for (Mob mob : managed) {
                int mx = mob.pos % width();
                int my = mob.pos / width();
                int dist = Math.max(Math.abs(mx - hx), Math.abs(my - hy));
                if (dist > farthestDist) {
                    farthestDist = dist;
                    farthest = mob;
                }
            }
            if (farthest == null) break;
            farthest.despawnFromInfiniteWorld();
            managed.remove(farthest);
        }
    }

    private void rebaseInfiniteWorldMobs(int shiftedCellsX, int shiftedCellsY) {
        if (Dungeon.hero == null) return;

        int hx = Dungeon.hero.pos % width();
        int hy = Dungeon.hero.pos / width();

        for (Mob mob : mobs.toArray(new Mob[0])) {
            if (mob instanceof InfiniteWorldShopkeeper) {
                int oldX = mob.pos % width();
                int oldY = mob.pos / width();
                int newX = oldX - shiftedCellsX;
                int newY = oldY - shiftedCellsY;

                if (newX <= 0 || newX >= width() - 1
                        || newY <= 0 || newY >= height() - 1) {
                    mob.despawnFromInfiniteWorld();
                } else {
                    int newPos = newX + newY * width();
                    if (solid[newPos] || pit[newPos]) mob.despawnFromInfiniteWorld();
                    else mob.rebaseForInfiniteWorld(newPos);
                }
                continue;
            }

            if (mob.alignment != Char.Alignment.ENEMY) continue;
            if (mob.properties().contains(Char.Property.BOSS)
                    || mob.properties().contains(Char.Property.MINIBOSS)) continue;

            int oldX = mob.pos % width();
            int oldY = mob.pos / width();
            int newX = oldX - shiftedCellsX;
            int newY = oldY - shiftedCellsY;

            if (newX <= 0 || newX >= width() - 1
                    || newY <= 0 || newY >= height() - 1) {
                mob.despawnFromInfiniteWorld();
                continue;
            }

            int dist = Math.max(Math.abs(newX - hx), Math.abs(newY - hy));
            if (dist > MOB_DESPAWN_DISTANCE) {
                mob.despawnFromInfiniteWorld();
                continue;
            }

            int newPos = newX + newY * width();
            if (solid[newPos] || pit[newPos]) {
                mob.despawnFromInfiniteWorld();
                continue;
            }

            mob.rebaseForInfiniteWorld(newPos);
        }
    }

    @Override
    protected void createItems() {
        generateChestHeaps();
        generateV5Containers();
        generateV5Traps();
        generateV6LooseLoot();
        generateV6Plants();
        generateV7ThemedRoomContents();
        generateV18AnomalyContent();
        generateV9AnomalyNotes();
        restoreGuaranteedArtifactChest();
        generateStarterSupplies();
        rebuildV15DecorationProps();
    }

    @Override
    public boolean activateTransition(Hero hero, LevelTransition transition) {
        // This world has no stairs/floors. The transition only exists as a spawn anchor.
        return false;
    }

    /**
     * Called after the hero moves. The local 168x168 canvas is recentred in-place
     * when the hero reaches the outer streaming band. No GameScene recreation is used.
     */
    public void recordHeroMove(Hero hero) {
        if (hero == null || Dungeon.level != this) return;

        InfiniteWorldState st = state();
        int x = hero.pos % width();
        int y = hero.pos / width();

        st.heroWorldX = (st.centerChunkX - HALF_WINDOW) * CHUNK_SIZE + x;
        st.heroWorldY = (st.centerChunkY - HALF_WINDOW) * CHUNK_SIZE + y;
        st.heroWorldInitialized = true;

        int heroChunkX = Math.floorDiv(st.heroWorldX, CHUNK_SIZE);
        int heroChunkY = Math.floorDiv(st.heroWorldY, CHUNK_SIZE);
        st.markChunkExplored(heroChunkX, heroChunkY);

        InfiniteWorldProgression.onHeroAtBreakthroughGate(hero);
        if (InfiniteWorldProgression.breakthroughCompleted()) {
            InfiniteWorldProgression.ensureBreakthroughCertificate(hero);
        }

        boolean chunkChanged = heroChunkX != lastProcessedHeroChunkX
                || heroChunkY != lastProcessedHeroChunkY;
        if (chunkChanged) {
            lastProcessedHeroChunkX = heroChunkX;
            lastProcessedHeroChunkY = heroChunkY;

            // These checks are chunk-scoped; running them on every tile step was
            // wasted work and became especially noticeable after long teleports.
            if (state().generatorVersion >= 15) {
                ensureV15BackroomsInfo(hero);
            } else {
                ensureV12AnomalyNoteNearby(hero);
            }
            ensureV13MerchantDiscovery(hero);
        }

        // Mob pruning only needs coarse movement granularity. The hard cap is tiny,
        // so checking every four world cells (or on a chunk change) preserves
        // ecology behaviour while avoiding a full mob scan for every footstep.
        if (chunkChanged
                || lastPruneWorldX == Integer.MIN_VALUE
                || Math.max(Math.abs(st.heroWorldX - lastPruneWorldX),
                            Math.abs(st.heroWorldY - lastPruneWorldY)) >= 4) {
            pruneInfiniteWorldMobs();
            lastPruneWorldX = st.heroWorldX;
            lastPruneWorldY = st.heroWorldY;
        }
    }

    public void beginGenesisTeleportGrace(Hero hero) {
        if (hero == null || Dungeon.level != this) return;
        int x = hero.pos % width();
        int y = hero.pos / width();

        genesisTeleportGrace = (x < SHIFT_LOW || x >= SHIFT_HIGH
                || y < SHIFT_LOW || y >= SHIFT_HIGH);
    }


    public void settleHeroAfterSpectator(Hero hero) {
        if (hero == null || Dungeon.level != this) return;

        int current = hero.pos;
        if (current >= 0 && current < length()
                && insideMap(current)
                && !solid[current] && !pit[current]
                && (passable[current] || avoid[current])) {
            return;
        }

        int hx = current % width();
        int hy = current / width();
        int best = -1;
        long bestScore = Long.MAX_VALUE;

        for (int cell = 0; cell < length(); cell++) {
            if (!insideMap(cell) || solid[cell] || pit[cell]) continue;
            if (!(passable[cell] || avoid[cell])) continue;

            Char occupant = Actor.findChar(cell);
            if (occupant != null && occupant != hero) continue;

            int x = cell % width();
            int y = cell / width();
            int dist = Math.abs(x - hx) + Math.abs(y - hy);
            long score = (long)dist * 100_000L
                    + (visited[cell] ? 0L : 20_000L)
                    + Math.floorMod(hash(worldXForLocalCell(cell), worldYForLocalCell(cell), 28600), 10_000L);

            if (score < bestScore) {
                bestScore = score;
                best = cell;
            }
        }

        if (best >= 0) {
            hero.pos = best;
            if (hero.sprite != null) hero.sprite.place(best);
            recordHeroMove(hero);
            Dungeon.observe();
            GameScene.updateFog();
        }
    }

    public void recordHeroAction(float time) {
        if (time <= 0f || Dungeon.level != this || state().generatorVersion < 12) return;

        InfiniteWorldState st = state();
        st.heroActionValue += time;

        if (st.artifactChestState == 0
                && st.heroActionValue >= GUARANTEED_ARTIFACT_CHEST_ACTION_VALUE) {
            spawnGuaranteedArtifactChest();
        }
    }


    private void ensureV15BackroomsInfo(Hero hero) {
        if (hero == null || state().generatorVersion < 15) return;

        int cx = Math.floorDiv(state().heroWorldX, CHUNK_SIZE);
        int cy = Math.floorDiv(state().heroWorldY, CHUNK_SIZE);
        int anomaly = v9AnomalyType(cx, cy);
        if (anomaly == 0) return;

        String page = v9AnomalyNotePage(anomaly);
        if (Document.INFINITE_WORLD_NOTES.findPage(page)) {
            GLog.i(Messages.get(this, "backrooms_info_unlocked",
                    Document.INFINITE_WORLD_NOTES.pageTitle(page)));
            GameScene.flashForDocument(Document.INFINITE_WORLD_NOTES, page);
        }
    }

    private void ensureV12AnomalyNoteNearby(Hero hero) {
        if (hero == null || state().generatorVersion < 12) return;

        int cx = Math.floorDiv(state().heroWorldX, CHUNK_SIZE);
        int cy = Math.floorDiv(state().heroWorldY, CHUNK_SIZE);
        int anomaly = v9AnomalyType(cx, cy);
        if (anomaly == 0) return;

        String page = v9AnomalyNotePage(anomaly);
        if (Document.INFINITE_WORLD_NOTES.isPageFound(page) || activeAnomalyNoteExists(page)) return;

        int cell = findNearbyVisibleObjectCell(hero, 1, 4);
        if (cell < 0) cell = hero.pos;

        Heap heap = heaps.get(cell);
        if (heap != null && heap.type != Heap.Type.HEAP) {
            cell = findNearbyVisibleObjectCell(hero, 1, 7);
            if (cell < 0) return;
            heap = heaps.get(cell);
        }

        InfiniteWorldNote note = new InfiniteWorldNote();
        note.page(page);

        if (heap == null) {
            heap = new Heap();
            heap.pos = cell;
            heap.type = Heap.Type.HEAP;
            heap.seen = true;
            heap.drop(note);
            heaps.put(cell, heap);
            GameScene.add(heap);
        } else {
            heap.drop(note);
            heap.seen = true;
        }

        visited[cell] = true;
        mapped[cell] = true;
        long key = worldKeyForLocalCell(cell);
        state().markVisited(key);
        state().markMapped(key);
    }

    private boolean activeAnomalyNoteExists(String page) {
        for (Heap heap : heaps.valueList()) {
            for (Item item : heap.items) {
                if (item instanceof InfiniteWorldNote
                        && page.equals(((InfiniteWorldNote)item).page())) {
                    return true;
                }
            }
        }
        return false;
    }

    private int findNearbyVisibleObjectCell(Hero hero, int minDistance, int maxDistance) {
        if (hero == null) return -1;

        boolean[] walkable = BArray.or(passable, avoid, null);
        PathFinder.buildDistanceMap(hero.pos, walkable, maxDistance);

        int hx = hero.pos % width();
        int hy = hero.pos / width();
        int best = -1;
        long bestScore = Long.MAX_VALUE;

        for (int y = Math.max(1, hy - maxDistance); y <= Math.min(height() - 2, hy + maxDistance); y++) {
            for (int x = Math.max(1, hx - maxDistance); x <= Math.min(width() - 2, hx + maxDistance); x++) {
                int cell = x + y * width();
                int dist = PathFinder.distance[cell];
                if (dist < minDistance || dist > maxDistance) continue;
                if (!passable[cell] || solid[cell] || pit[cell] || secret[cell]) continue;
                if (Actor.findChar(cell) != null || traps.get(cell) != null || plants.get(cell) != null) continue;

                Heap heap = heaps.get(cell);
                if (heap != null && heap.type != Heap.Type.HEAP) continue;

                long score = (heroFOV[cell] ? 0L : 1_000_000L)
                        + (long)dist * 10_000L
                        + Math.floorMod(hash(worldXForLocalCell(cell), worldYForLocalCell(cell), 27100), 10_000L);
                if (score < bestScore) {
                    bestScore = score;
                    best = cell;
                }
            }
        }
        return best;
    }

    private int chooseGuaranteedArtifactIndex() {
        ArrayList<Integer> available = new ArrayList<>();
        for (int i = 0; i < Generator.Category.ARTIFACT.classes.length; i++) {
            if (i < Generator.Category.ARTIFACT.probs.length
                    && Generator.Category.ARTIFACT.probs[i] > 0f) {
                available.add(i);
            }
        }

        int chosen;
        if (!available.isEmpty()) {
            int pick = (int)Math.floorMod(
                    hash(state().heroWorldX, state().heroWorldY, 27200),
                    (long)available.size());
            chosen = available.get(pick);
            @SuppressWarnings("unchecked")
            Class<? extends Artifact> cls =
                    (Class<? extends Artifact>)Generator.Category.ARTIFACT.classes[chosen];
            Generator.removeArtifact(cls);
        } else {
            chosen = (int)Math.floorMod(
                    hash(state().heroWorldX, state().heroWorldY, 27201),
                    (long)Generator.Category.ARTIFACT.classes.length);
        }
        return chosen;
    }

    @SuppressWarnings("unchecked")
    private Artifact guaranteedArtifactItem(int index) {
        Class<?>[] classes = Generator.Category.ARTIFACT.classes;
        if (classes.length == 0) return null;
        if (index < 0 || index >= classes.length) index = 0;

        Artifact artifact = (Artifact)Reflection.newInstance((Class<? extends Artifact>)classes[index]);
        artifact.cursed = false;
        artifact.cursedKnown = true;
        return artifact;
    }

    private void spawnGuaranteedArtifactChest() {
        if (Dungeon.hero == null || state().artifactChestState != 0) return;

        // Keep the guaranteed chest inside the Hero-centered 3x3 cell neighbourhood.
        // If all eight adjacent cells are temporarily invalid, defer spawning and
        // retry on the next positive Hero action instead of placing it farther away.
        int cell = findNearbyVisibleObjectCell(Dungeon.hero, 1, 1);
        if (cell < 0) return;

        InfiniteWorldState st = state();
        st.artifactChestWorldX = worldXForLocalCell(cell);
        st.artifactChestWorldY = worldYForLocalCell(cell);
        st.artifactChestArtifactIndex = chooseGuaranteedArtifactIndex();
        st.artifactChestState = 1;

        Artifact artifact = guaranteedArtifactItem(st.artifactChestArtifactIndex);
        if (artifact == null) {
            st.artifactChestState = 0;
            st.artifactChestArtifactIndex = -1;
            return;
        }

        Heap heap = new Heap();
        heap.pos = cell;
        heap.type = Heap.Type.CRYSTAL_CHEST;
        heap.seen = true;
        heap.drop(artifact);
        heaps.put(cell, heap);
        GameScene.add(heap);

        visited[cell] = true;
        mapped[cell] = true;
        long key = worldKeyForLocalCell(cell);
        st.markVisited(key);
        st.markMapped(key);

        // The guaranteed chest must never become a key soft-lock. Give the player
        // one normal Infinite World crystal key at the moment the chest appears.
        Notes.add(new CrystalKey(Dungeon.depth));
        GameScene.updateKeyDisplay();
    }

    private int localCellForWorld(int worldX, int worldY) {
        int localX = worldX - (state().centerChunkX - HALF_WINDOW) * CHUNK_SIZE;
        int localY = worldY - (state().centerChunkY - HALF_WINDOW) * CHUNK_SIZE;
        if (localX < 0 || localX >= width() || localY < 0 || localY >= height()) return -1;
        return localX + localY * width();
    }

    private void restoreGuaranteedArtifactChest() {
        InfiniteWorldState st = state();
        if (st.generatorVersion < 12 || st.artifactChestState <= 0 || st.artifactChestState >= 3) return;

        int cell = localCellForWorld(st.artifactChestWorldX, st.artifactChestWorldY);
        if (cell < 0 || heaps.get(cell) != null) return;

        Artifact artifact = guaranteedArtifactItem(st.artifactChestArtifactIndex);
        if (artifact == null) return;

        Heap heap = new Heap();
        heap.pos = cell;
        heap.type = st.artifactChestState == 1 ? Heap.Type.CRYSTAL_CHEST : Heap.Type.HEAP;
        heap.seen = true;
        heap.drop(artifact);
        heaps.put(cell, heap);

        visited[cell] = true;
        mapped[cell] = true;
        long key = worldKeyForLocalCell(cell);
        st.markVisited(key);
        st.markMapped(key);
    }

    private void snapshotGuaranteedArtifactChest() {
        InfiniteWorldState st = state();
        if (st.generatorVersion < 12 || st.artifactChestState <= 0 || st.artifactChestState >= 3) return;

        int cell = localCellForWorld(st.artifactChestWorldX, st.artifactChestWorldY);
        if (cell < 0) return;

        Heap heap = heaps.get(cell);
        if (heap == null) {
            st.artifactChestState = 3;
            return;
        }

        boolean artifactPresent = false;
        Class<?>[] artifactClasses = Generator.Category.ARTIFACT.classes;
        Class<?> expected = st.artifactChestArtifactIndex >= 0
                && st.artifactChestArtifactIndex < artifactClasses.length
                ? artifactClasses[st.artifactChestArtifactIndex] : Artifact.class;
        for (Item item : heap.items) {
            if (expected.isInstance(item)) {
                artifactPresent = true;
                break;
            }
        }

        if (!artifactPresent) {
            st.artifactChestState = 3;
        } else {
            st.artifactChestState = heap.type == Heap.Type.CRYSTAL_CHEST ? 1 : 2;
        }
    }

    /**
     * Streaming is deliberately performed only after the current movement tween
     * has completed. CharSprite.onComplete() calls Hero.onMotionComplete() before
     * waking the actor thread, so the window can rebase here without destroying
     * a live tween or leaving Actor.process() blocked on sprite.wait().
     */
    public void afterHeroMotionComplete(Hero hero) {
        if (shifting || hero == null || Dungeon.level != this) return;

        int x = hero.pos % width();
        int y = hero.pos / width();

        int shiftX = 0;
        int shiftY = 0;

        // After Genesis teleport, defer the expensive streaming rebuild until the
        // hero is actually close to the hard map edge. This prevents the very next
        // normal movement step from hitching after a long teleport.
        int low = genesisTeleportGrace ? 8 : SHIFT_LOW;
        int high = genesisTeleportGrace ? MAP_SIZE - 8 : SHIFT_HIGH;

        if (x < low) shiftX = -SHIFT_STEP;
        else if (x >= high) shiftX = SHIFT_STEP;

        if (y < low) shiftY = -SHIFT_STEP;
        else if (y >= high) shiftY = SHIFT_STEP;

        if (shiftX == 0 && shiftY == 0) {
            if (genesisTeleportGrace
                    && x >= SHIFT_LOW && x < SHIFT_HIGH
                    && y >= SHIFT_LOW && y < SHIFT_HIGH) {
                genesisTeleportGrace = false;
            }
            return;
        }

        shifting = true;

        InfiniteWorldState st = state();
        int oldCenterX = st.centerChunkX;
        int oldCenterY = st.centerChunkY;

        // recordHeroMove() has already made these absolute coordinates authoritative.
        int worldX = st.heroWorldX;
        int worldY = st.heroWorldY;

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

        if (newLocalX < 1 || newLocalX >= width()-1
                || newLocalY < 1 || newLocalY >= height()-1) {
            st.centerChunkX = oldCenterX;
            st.centerChunkY = oldCenterY;
            rebuildWindow();
            shifting = false;
            return;
        }

        hero.pos = newLocalX + newLocalY * width();

        final int shiftedCellsX = shiftX * CHUNK_SIZE;
        final int shiftedCellsY = shiftY * CHUNK_SIZE;

        rebaseInfiniteWorldMobs(shiftedCellsX, shiftedCellsY);
        ensureV11Shopkeeper(true);
        ensureV13MerchantDiscovery(hero);

        hero.curAction = rebaseCellAction(hero.curAction, curTargetWorldX, curTargetWorldY, st);
        hero.lastAction = rebaseCellAction(hero.lastAction, lastTargetWorldX, lastTargetWorldY, st);

        // We are already on the render thread via CharSprite.onComplete().
        GameScene.refreshInfiniteWorldWindow(shiftedCellsX, shiftedCellsY);
        genesisTeleportGrace = false;
        shifting = false;
        checkpointAfterStreaming();
    }

    private void checkpointAfterStreaming() {
        streamingShiftsSinceCheckpoint++;
        int interval = assistSpectatorActive() ? 2 : 4;
        if (streamingShiftsSinceCheckpoint < interval) return;

        streamingShiftsSinceCheckpoint = 0;
        try {
            // snapshotForSave() alone only updates RAM. A process crash would
            // otherwise roll the player back to the last pause/exit save.
            Dungeon.saveAll();
        } catch (java.io.IOException e) {
            ShatteredPixelDungeon.reportException(e);
        }
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

        if (baseWindow == null || baseWindow.length != length()) {
            baseWindow = generateBaseWindow(st.centerChunkX, st.centerChunkY);
        }

        for (int cell = 0; cell < length(); cell++) {
            long key = worldKeyForLocalCell(cell);

            if (map[cell] != baseWindow[cell]) st.setTerrainOverride(key, map[cell]);
            else st.setTerrainOverride(key, null);

            if (visited[cell]) st.markVisited(key);
            if (mapped[cell]) st.markMapped(key);
        }

        snapshotChestStates();
        snapshotV5ContainerStates();
        snapshotV6LooseLootStates();
        snapshotV6PlantStates();
        snapshotV7ThemedRoomStates();
        snapshotV18AnomalyContent();
        snapshotV9AnomalyNotes();
        snapshotGuaranteedArtifactChest();
        snapshotStarterSupplies();
    }

    private void rebuildWindow() {
        InfiniteWorldState st = state();

        baseWindow = generateBaseWindow(st.centerChunkX, st.centerChunkY);
        System.arraycopy(baseWindow, 0, map, 0, map.length);
        applyTerrainOverrides();
        repairLegacyMerchantAccessInWindow();

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
        rebuildAccentTiles();

        buildFlagMaps();
        cleanWalls();
        PathFinder.setMapSize(width(), height());

        generateChestHeaps();
        generateV5Containers();
        generateV5Traps();
        generateV6LooseLoot();
        generateV6Plants();
        generateV7ThemedRoomContents();
        generateV18AnomalyContent();
        generateV9AnomalyNotes();
        restoreGuaranteedArtifactChest();
        generateStarterSupplies();
        rebuildV15DecorationProps();
    }


    private static final int STARTER_ORIGIN_WORLD_X = 12;
    private static final int STARTER_ORIGIN_WORLD_Y = 12;
    private static final int STARTER_KEY_SALT = 0x7A31;

    private void generateStarterSupplies() {
        InfiniteWorldState st = state();

        if (!st.starterSuppliesInitialized) {
            int origin = localCellForWorld(STARTER_ORIGIN_WORLD_X, STARTER_ORIGIN_WORLD_Y);
            if (origin < 0) return;

            ArrayList<Integer> used = new ArrayList<>();
            int normal = findStarterSupplyCell(origin, used, 0);
            if (normal < 0) return;
            used.add(normal);

            int key = findStarterSupplyCell(origin, used, 1);
            if (key < 0) return;
            used.add(key);

            int crystal = findStarterSupplyCell(origin, used, 2);
            if (crystal < 0) return;

            st.starterNormalChestWorldX = worldXForLocalCell(normal);
            st.starterNormalChestWorldY = worldYForLocalCell(normal);
            st.starterCrystalKeyWorldX = worldXForLocalCell(key);
            st.starterCrystalKeyWorldY = worldYForLocalCell(key);
            st.starterCrystalChestWorldX = worldXForLocalCell(crystal);
            st.starterCrystalChestWorldY = worldYForLocalCell(crystal);

            buildStarterContents(st);
            st.starterSuppliesInitialized = true;
        }

        restoreStarterNormalChest(st);
        restoreStarterCrystalKey(st);
        restoreStarterCrystalChest(st);
    }

    private void buildStarterContents(InfiniteWorldState st) {
        st.starterNormalChestContents.clear();
        st.starterCrystalChestContents.clear();

        Random.pushGenerator(Dungeon.seed ^ 0x6A09E667F3BCC909L);
        try {
            for (int i = 0; i < 5; i++) {
                Item item = Generator.randomUsingDefaults(Generator.Category.FOOD);
                if (item != null) st.starterNormalChestContents.add(starterUnknown(item));
            }
            for (int i = 0; i < 5; i++) {
                Item item = Generator.randomUsingDefaults(Generator.Category.SCROLL);
                if (item != null) st.starterNormalChestContents.add(starterUnknown(item));
            }
            for (int i = 0; i < 5; i++) {
                Item item = Generator.randomUsingDefaults(Generator.Category.POTION);
                if (item != null) st.starterNormalChestContents.add(starterUnknown(item));
            }

            Item ring = Generator.randomUsingDefaults(Generator.Category.RING);
            Item wand = Generator.randomUsingDefaults(Generator.Category.WAND);
            if (ring != null) st.starterCrystalChestContents.add(starterUnknown(ring));
            if (wand != null) st.starterCrystalChestContents.add(starterUnknown(wand));
        } finally {
            Random.popGenerator();
        }
    }

    private Item starterUnknown(Item item) {
        if (item instanceof Food) {
            ((Food)item).markStarterUnknown();
        } else {
            item.levelKnown = false;
            item.cursedKnown = false;
        }
        return item;
    }

    private void restoreStarterNormalChest(InfiniteWorldState st) {
        int cell = localCellForWorld(st.starterNormalChestWorldX, st.starterNormalChestWorldY);
        if (cell < 0) return;

        long key = encodeWorld(st.starterNormalChestWorldX, st.starterNormalChestWorldY);
        int chestState = st.chestState(key);
        if (chestState >= 2 || heaps.get(cell) != null) return;

        Heap heap = new Heap();
        heap.pos = cell;
        heap.seen = true;
        heap.type = chestState == 0 ? Heap.Type.CHEST : Heap.Type.HEAP;
        for (Item item : new ArrayList<>(st.starterNormalChestContents)) {
            if (item != null && item.quantity() > 0) heap.drop(item);
        }
        st.starterNormalChestContents.clear();
        st.starterNormalChestContents.addAll(heap.items);
        heaps.put(cell, heap);
        revealStarterCell(cell);
    }

    private void restoreStarterCrystalKey(InfiniteWorldState st) {
        int cell = localCellForWorld(st.starterCrystalKeyWorldX, st.starterCrystalKeyWorldY);
        if (cell < 0) return;

        long objectKey = starterKeyObjectKey(st);
        if (st.objectState(objectKey) != 0 || heaps.get(cell) != null) return;

        Heap heap = new Heap();
        heap.pos = cell;
        heap.seen = true;
        heap.type = Heap.Type.HEAP;
        heap.drop(new CrystalKey(Dungeon.depth));
        heaps.put(cell, heap);
        revealStarterCell(cell);
    }

    private void restoreStarterCrystalChest(InfiniteWorldState st) {
        int cell = localCellForWorld(st.starterCrystalChestWorldX, st.starterCrystalChestWorldY);
        if (cell < 0) return;

        long key = encodeWorld(st.starterCrystalChestWorldX, st.starterCrystalChestWorldY);
        int chestState = st.chestState(key);
        if (chestState >= 2 || heaps.get(cell) != null) return;

        Heap heap = new Heap();
        heap.pos = cell;
        heap.seen = true;
        heap.type = chestState == 0 ? Heap.Type.CRYSTAL_CHEST : Heap.Type.HEAP;
        for (Item item : new ArrayList<>(st.starterCrystalChestContents)) {
            if (item != null && item.quantity() > 0) heap.drop(item);
        }
        st.starterCrystalChestContents.clear();
        st.starterCrystalChestContents.addAll(heap.items);
        heaps.put(cell, heap);
        revealStarterCell(cell);
    }

    private void snapshotStarterSupplies() {
        InfiniteWorldState st = state();
        if (!st.starterSuppliesInitialized) return;

        snapshotStarterChest(st.starterNormalChestWorldX, st.starterNormalChestWorldY,
                Heap.Type.CHEST, st.starterNormalChestContents);
        snapshotStarterChest(st.starterCrystalChestWorldX, st.starterCrystalChestWorldY,
                Heap.Type.CRYSTAL_CHEST, st.starterCrystalChestContents);

        int keyCell = localCellForWorld(st.starterCrystalKeyWorldX, st.starterCrystalKeyWorldY);
        if (keyCell >= 0 && st.objectState(starterKeyObjectKey(st)) == 0) {
            Heap heap = heaps.get(keyCell);
            boolean present = false;
            if (heap != null) {
                for (Item item : heap.items) {
                    if (item instanceof CrystalKey) {
                        present = true;
                        break;
                    }
                }
            }
            if (!present) st.setObjectState(starterKeyObjectKey(st), 1);
        }
    }

    private void snapshotStarterChest(int worldX, int worldY, Heap.Type closedType,
                                      ArrayList<Item> contents) {
        int cell = localCellForWorld(worldX, worldY);
        if (cell < 0) return;

        long key = encodeWorld(worldX, worldY);
        Heap heap = heaps.get(cell);
        if (heap == null) {
            state().setChestState(key, 2);
            contents.clear();
            return;
        }

        state().setChestState(key, heap.type == closedType ? 0 : 1);
        contents.clear();
        contents.addAll(heap.items);
    }

    private long starterKeyObjectKey(InfiniteWorldState st) {
        long world = encodeWorld(st.starterCrystalKeyWorldX, st.starterCrystalKeyWorldY);
        long mix = 0x9E3779B97F4A7C15L * (STARTER_KEY_SALT + 0x632BE5AB);
        return world ^ Long.rotateLeft(mix, STARTER_KEY_SALT & 31);
    }

    private int findStarterSupplyCell(int origin, ArrayList<Integer> used, int ordinal) {
        int ox = origin % width();
        int oy = origin / width();

        for (int radius = 1; radius <= 9; radius++) {
            ArrayList<Integer> candidates = new ArrayList<>();
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                    int x = ox + dx;
                    int y = oy + dy;
                    if (x < 1 || y < 1 || x >= width()-1 || y >= height()-1) continue;
                    int cell = x + y * width();
                    if (used.contains(cell)) continue;
                    if (solid[cell] || pit[cell] || (!passable[cell] && !avoid[cell])) continue;
                    if (heaps.get(cell) != null || traps.get(cell) != null || plants.get(cell) != null) continue;
                    candidates.add(cell);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(Math.floorMod(ordinal, candidates.size()));
            }
        }

        // The starter cache is a hard guarantee. If an unusual generator layout
        // leaves no free nearby floor at all, reserve one of the immediate cells
        // and carve it into ordinary floor instead of silently omitting supplies.
        int[][] fallback = new int[][]{
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2}
        };
        for (int[] d : fallback) {
            int x = ox + d[0];
            int y = oy + d[1];
            if (x < 1 || y < 1 || x >= width()-1 || y >= height()-1) continue;
            int cell = x + y * width();
            if (used.contains(cell) || Actor.findChar(cell) != null) continue;

            heaps.remove(cell);
            traps.remove(cell);
            plants.remove(cell);
            Level.set(cell, Terrain.EMPTY, this);
            return cell;
        }
        return -1;
    }

    private void revealStarterCell(int cell) {
        visited[cell] = true;
        mapped[cell] = true;
        long key = worldKeyForLocalCell(cell);
        state().markVisited(key);
        state().markMapped(key);
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

    // Persist the small area whose exploration state just changed instead of waiting
    // for the next 7x7 window shift. This keeps long walks resilient while keeping
    // the per-step work bounded to the hero's current vision rectangle.
    public void syncExplorationArea(int left, int top, int areaWidth, int areaHeight) {
        InfiniteWorldState st = state();
        int right = Math.min(width(), left + areaWidth);
        int bottom = Math.min(height(), top + areaHeight);
        left = Math.max(0, left);
        top = Math.max(0, top);

        for (int y = top; y < bottom; y++) {
            int cell = left + y * width();
            for (int x = left; x < right; x++, cell++) {
                long key = worldKeyForLocalCell(cell);
                if (visited[cell]) st.markVisited(key);
                if (mapped[cell]) st.markMapped(key);
            }
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

                if (chestState >= 2 || solid[cell] || pit[cell]) return;

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
        if (state().generatorVersion >= 22) {
            return v22BalancedSupplyItem(cx, cy, 0, 7001);
        }
        if (state().generatorVersion >= 18) {
            int roll = range(cx, cy, 7001, 0, 99);
            if (roll < 16) return new PotionOfHealing();
            if (roll < 29) return v6RandomFood(cx, cy, 7010);
            if (roll < 43) return v6SafeScroll(cx, cy, 7020);
            if (roll < 56) return v6RandomPotion(cx, cy, 7030);
            if (roll < 67) return new Bomb();
            if (roll < 76) return new StoneOfBlink();
            if (roll < 83) return new Torch();
            if (roll < 90) return new Gold(8 + range(cx, cy, 7040, 0, 24));
            if (roll < 96) return v6EquipmentItem(cx, cy, 7050);
            // Mining tools remain obtainable in ordinary exploration.
            return new Pickaxe();
        }

        int roll = (int)Math.floorMod(hash(cx, cy, 7001), 8L);
        if (roll <= 2) return new PotionOfHealing();
        if (roll <= 4) return new ScrollOfTeleportation();
        if (roll <= 6) return new Bomb();
        return new Pickaxe();
    }

    private void generateV5Traps() {
        if (state().generatorVersion < 5) return;

        for (int cell = 0; cell < length(); cell++) {
            if (map[cell] != Terrain.SECRET_TRAP && map[cell] != Terrain.TRAP) continue;
            if (traps.get(cell) != null) continue;

            int wx = worldXForLocalCell(cell);
            int wy = worldYForLocalCell(cell);
            Trap trap;

            if (state().generatorVersion >= 18) {
                int cx = Math.floorDiv(wx, CHUNK_SIZE);
                int cy = Math.floorDiv(wy, CHUNK_SIZE);
                int anomaly = v9AnomalyType(cx, cy);
                trap = anomaly == 0
                        ? v18OrdinaryTrap(wx, wy)
                        : v18AnomalyTrap(wx, wy, anomaly);
            } else {
                int roll = (int)Math.floorMod(hash(wx, wy, 18100), 7L);
                switch (roll) {
                    case 0: trap = new TeleportationTrap(); break;
                    case 1: trap = new BurningTrap(); break;
                    case 2: trap = new ChillingTrap(); break;
                    case 3: trap = new PoisonDartTrap(); break;
                    case 4: trap = new OozeTrap(); break;
                    case 5: trap = new ConfusionTrap(); break;
                    default:trap = new GrippingTrap(); break;
                }
            }
            trap.set(cell);
            trap.visible = map[cell] == Terrain.TRAP;
            traps.put(cell, trap);
        }
    }

    private Trap v18OrdinaryTrap(int wx, int wy) {
        switch ((int)Math.floorMod(hash(wx, wy, 18120), 12L)) {
            case 0: return new TeleportationTrap();
            case 1: return new BurningTrap();
            case 2: return new ChillingTrap();
            case 3: return new PoisonDartTrap();
            case 4: return new OozeTrap();
            case 5: return new ConfusionTrap();
            case 6: return new GrippingTrap();
            case 7: return new ShockingTrap();
            case 8: return new FlashingTrap();
            case 9: return new CorrosionTrap();
            case 10:return new AlarmTrap();
            default:return new WornDartTrap();
        }
    }

    private Trap v18AnomalyTrap(int wx, int wy, int anomaly) {
        int roll = (int)Math.floorMod(hash(wx, wy, 18140 + anomaly), 4L);
        switch (anomaly) {
            case 1: // Level 0 — disorientation
                return roll < 2 ? new ConfusionTrap()
                        : (roll == 2 ? new TeleportationTrap() : new GrippingTrap());
            case 2: // Level 1 — storage/service
                return roll < 2 ? new WornDartTrap()
                        : (roll == 2 ? new AlarmTrap() : new PoisonDartTrap());
            case 3: // Level 2 — heat and pipes
                return roll < 2 ? new BurningTrap()
                        : (roll == 2 ? new CorrosionTrap() : new GrippingTrap());
            case 4: // Level 3 — electrical station
                return roll < 2 ? new ShockingTrap()
                        : (roll == 2 ? new FlashingTrap() : new StormTrap());
            case 5: // Level 4 — abandoned offices
                return roll < 2 ? new AlarmTrap()
                        : (roll == 2 ? new ConfusionTrap() : new TeleportationTrap());
            case 6: // Level 5 — hotel
                return roll < 2 ? new AlarmTrap()
                        : (roll == 2 ? new GrippingTrap() : new ConfusionTrap());
            case 7: // Level 6 — lights out
                return roll < 2 ? new GrippingTrap()
                        : (roll == 2 ? new TeleportationTrap() : new ConfusionTrap());
            case 8: // Level 7 — water
                return roll < 2 ? new ChillingTrap()
                        : (roll == 2 ? new FrostTrap() : new GeyserTrap());
            case 9: // Level 8 — caves
                return roll < 2 ? new GrippingTrap()
                        : (roll == 2 ? new RockfallTrap() : new PoisonDartTrap());
            case 10: // Level 9 — suburbs
                return roll < 2 ? new AlarmTrap()
                        : (roll == 2 ? new GrippingTrap() : new PoisonDartTrap());
            case 11: // Level 10 — fields
                return roll < 2 ? new GrippingTrap()
                        : (roll == 2 ? new PoisonDartTrap() : new WornDartTrap());
            case 12: // Level 11 — city
                return roll < 2 ? new AlarmTrap()
                        : (roll == 2 ? new ShockingTrap() : new PoisonDartTrap());
            case 13: // Level 37 — poolrooms
                return roll < 2 ? new GeyserTrap()
                        : (roll == 2 ? new ChillingTrap() : new FrostTrap());
            case 14: // Level 94 — Motion
                return roll < 2 ? new ConfusionTrap()
                        : (roll == 2 ? new TeleportationTrap() : new AlarmTrap());
            case 15: // Level 13
                return roll < 2 ? new AlarmTrap()
                        : (roll == 2 ? new TeleportationTrap() : new ConfusionTrap());
            case 16: // Level 18
                return new ConfusionTrap();
            case 17: // Level 34
                return roll < 2 ? new PoisonDartTrap()
                        : (roll == 2 ? new GrippingTrap() : new OozeTrap());
            case 18: // Level 40
                return roll < 2 ? new FlashingTrap()
                        : (roll == 2 ? new ConfusionTrap() : new AlarmTrap());
            case 19: // Level 48
                return new GeyserTrap();
            case 20: // Level 974
            default:
                return new ConfusionTrap();
        }
    }

    private void snapshotV5ContainerStates() {
        if (state().generatorVersion < 5) return;
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int count = v5ContainerCount(cx, cy);
                for (int index = 0; index < count; index++) {
                    int cell = v5ContainerCell(cx, cy, ox, oy, index);
                    if (cell < 0) continue;

                    long key = worldKeyForLocalCell(cell);
                    Heap heap = heaps.get(cell);
                    if (heap == null) {
                        st.setChestState(key, 2);
                    } else if (heap.type == v5ContainerType(cx, cy, index)) {
                        st.setChestState(key, 0);
                    } else {
                        st.setChestState(key, 1);
                    }
                }
            }
        });
    }

    private void generateV5Containers() {
        if (state().generatorVersion < 5) return;
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int count = v5ContainerCount(cx, cy);
                for (int index = 0; index < count; index++) {
                    int cell = v5ContainerCell(cx, cy, ox, oy, index);
                    if (cell < 0 || heaps.get(cell) != null || traps.get(cell) != null) continue;

                    long key = worldKeyForLocalCell(cell);
                    int containerState = st.chestState(key);
                    if (containerState >= 2) continue;

                    Heap heap = new Heap();
                    heap.pos = cell;
                    heap.seen = mapped[cell] || visited[cell];
                    Heap.Type originalType = v5ContainerType(cx, cy, index);
                    heap.type = containerState == 0 ? originalType : Heap.Type.HEAP;
                    heap.drop(v5ContainerItem(cx, cy, index, originalType));
                    heaps.put(cell, heap);
                }
            }
        });
    }

    private int v5ContainerCount(int cx, int cy) {
        if (state().generatorVersion >= 9) {
            if (v9AnomalyType(cx, cy) != 0) return 0;
            if (state().generatorVersion >= 18) {
                long roll = Math.floorMod(hash(cx, cy, 18200), 100L);
                if (roll < 7) return 2;
                return roll < 40 ? 1 : 0;
            }
            return Math.floorMod(hash(cx, cy, 18200), 100L) < 28 ? 1 : 0;
        }
        return 2 + range(cx, cy, 18200, 0, 3);
    }

    private int v5ContainerCell(int cx, int cy, int ox, int oy, int index) {
        // A container gets several deterministic candidate cells. This avoids forcing
        // floor carving just for loot and keeps compact chunks genuinely compact.
        for (int attempt = 0; attempt < 10; attempt++) {
            int salt = 18220 + index * 40 + attempt * 3;
            int x = 2 + range(cx, cy, salt, 0, CHUNK_SIZE - 5);
            int y = 2 + range(cx, cy, salt + 1, 0, CHUNK_SIZE - 5);
            int cell = ox + x + (oy + y) * width();

            if (state().generatorVersion >= 7
                    && (v7LocalCellInsideRoom(cx, cy, x, y)
                    || v8LocalCellReservedForRoomAccess(cx, cy, x, y))) continue;

            int t = map[cell];
            if ((t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.GRASS
                    || t == Terrain.EMBERS || t == Terrain.HIGH_GRASS)
                    && !solid[cell] && !pit[cell]) {
                return cell;
            }
        }
        return -1;
    }

    private Heap.Type v5ContainerType(int cx, int cy, int index) {
        int roll = range(cx, cy, 18400 + index, 0, 9);
        if (state().generatorVersion >= 6) {
            if (roll <= 4) return Heap.Type.CHEST;
            if (roll <= 6) return Heap.Type.TOMB;
            if (roll == 7) return Heap.Type.SKELETON;
            return Heap.Type.CRYSTAL_CHEST;
        }
        if (roll <= 4) return Heap.Type.CHEST;
        if (roll <= 6) return Heap.Type.TOMB;
        return Heap.Type.SKELETON;
    }

    private Item v5ContainerItem(int cx, int cy, int index, Heap.Type type) {
        if (state().generatorVersion >= 6 && type == Heap.Type.CRYSTAL_CHEST) {
            return v6EquipmentItem(cx, cy, 18600 + index);
        }

        if (state().generatorVersion >= 22) {
            return v22BalancedSupplyItem(cx, cy, index, 18500);
        }

        if (state().generatorVersion >= 18) {
            int roll = range(cx, cy, 18500 + index, 0, 99);
            if (roll < 14) return new PotionOfHealing();
            if (roll < 26) return v6RandomPotion(cx, cy, 18540 + index);
            if (roll < 39) return v6SafeScroll(cx, cy, 18550 + index);
            if (roll < 52) return v6RandomFood(cx, cy, 18560 + index);
            if (roll < 62) return new Bomb();
            if (roll < 71) return new StoneOfBlink();
            if (roll < 79) return new Torch();
            if (roll < 87) return new Gold(6 + range(cx, cy, 18570 + index, 0, 20));
            if (roll < 94) return new Pickaxe();
            return v6EquipmentItem(cx, cy, 18575 + index);
        }

        int roll = range(cx, cy, 18500 + index, 0, 10);
        switch (roll) {
            case 0: return new PotionOfHealing();
            case 1: return new PotionOfInvisibility();
            case 2: return new PotionOfLiquidFlame();
            case 3: return new PotionOfFrost();
            case 4: return new ScrollOfTeleportation();
            case 5: return new Bomb();
            case 6: return new StoneOfBlink();
            case 7: return new Torch();
            case 8: return v6RandomFood(cx, cy, 18580 + index);
            case 9: return v6SafeScroll(cx, cy, 18590 + index);
            default:return new Pickaxe();
        }
    }

    private void promoteV6LockedDoor(int[] out, int cx, int cy, int ox, int oy) {
        if (Math.floorMod(hash(cx, cy, 18800), 100L) >= 42) return;

        int chosen = -1;
        long best = Long.MAX_VALUE;
        for (int y = 2; y < CHUNK_SIZE - 2; y++) {
            for (int x = 2; x < CHUNK_SIZE - 2; x++) {
                int cell = ox + x + (oy + y) * MAP_SIZE;
                if (out[cell] != Terrain.DOOR) continue;
                long score = hash(cx * CHUNK_SIZE + x, cy * CHUNK_SIZE + y, 18801);
                if (score < best) {
                    best = score;
                    chosen = cell;
                }
            }
        }
        if (chosen >= 0) out[chosen] = Terrain.LOCKED_DOOR;
    }

    private void generateV6LooseLoot() {
        if (state().generatorVersion < 6) return;
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int count;
                if (state().generatorVersion >= 22) {
                    count = v22LooseLootCount(cx, cy);
                } else if (state().generatorVersion >= 9) {
                    if (v9AnomalyType(cx, cy) == 0) {
                        if (state().generatorVersion >= 18) {
                            long roll = Math.floorMod(hash(cx, cy, 19000), 100L);
                            count = roll < 6 ? 2 : (roll < 44 ? 1 : 0);
                        } else {
                            count = Math.floorMod(hash(cx, cy, 19000), 100L) < 30 ? 1 : 0;
                        }
                    } else {
                        count = 0;
                    }
                } else {
                    count = 1 + range(cx, cy, 19000, 0, 2);
                }
                for (int index = 0; index < count; index++) {
                    int cell = v6ObjectCell(cx, cy, ox, oy, index, 19100);
                    if (cell < 0) continue;
                    long key = v6ObjectKey(cell, 0x4100 + index);
                    if (st.objectState(key) != 0) continue;

                    if (heaps.get(cell) != null || traps.get(cell) != null) {
                        st.setObjectState(key, 1);
                        continue;
                    }

                    Heap heap = new Heap();
                    heap.pos = cell;
                    heap.seen = mapped[cell] || visited[cell];
                    heap.type = Heap.Type.HEAP;
                    heap.drop(v6LooseItem(cx, cy, index));
                    heaps.put(cell, heap);
                }

                // Keys are intentionally separate from random loot so an endless
                // world never runs out of ways to open remote locks.
                int ironChance = state().generatorVersion >= 18 ? 10
                        : (state().generatorVersion >= 9 ? 8 : 38);
                int crystalChance = state().generatorVersion >= 18 ? 6
                        : (state().generatorVersion >= 9 ? 5 : 32);
                if (v9AnomalyType(cx, cy) == 0
                        && Math.floorMod(hash(cx, cy, 19400), 100L) < ironChance) {
                    generateV6KeyHeap(cx, cy, ox, oy, 0, new IronKey(Dungeon.depth));
                }
                if (v9AnomalyType(cx, cy) == 0
                        && Math.floorMod(hash(cx, cy, 19401), 100L) < crystalChance) {
                    generateV6KeyHeap(cx, cy, ox, oy, 1, new CrystalKey(Dungeon.depth));
                }
            }
        });
    }

    private void generateV6KeyHeap(int cx, int cy, int ox, int oy, int index, Item keyItem) {
        int cell = v6ObjectCell(cx, cy, ox, oy, index, 19500);
        if (cell < 0) return;
        long stateKey = v6ObjectKey(cell, 0x4200 + index);
        if (state().objectState(stateKey) != 0) return;

        if (heaps.get(cell) != null || traps.get(cell) != null) {
            state().setObjectState(stateKey, 1);
            return;
        }

        Heap heap = new Heap();
        heap.pos = cell;
        heap.seen = mapped[cell] || visited[cell];
        heap.type = Heap.Type.HEAP;
        heap.drop(keyItem);
        heaps.put(cell, heap);
    }

    private void snapshotV6LooseLootStates() {
        if (state().generatorVersion < 6) return;
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int count;
                if (state().generatorVersion >= 22) {
                    count = v22LooseLootCount(cx, cy);
                } else if (state().generatorVersion >= 9) {
                    if (v9AnomalyType(cx, cy) == 0) {
                        if (state().generatorVersion >= 18) {
                            long roll = Math.floorMod(hash(cx, cy, 19000), 100L);
                            count = roll < 6 ? 2 : (roll < 44 ? 1 : 0);
                        } else {
                            count = Math.floorMod(hash(cx, cy, 19000), 100L) < 30 ? 1 : 0;
                        }
                    } else {
                        count = 0;
                    }
                } else {
                    count = 1 + range(cx, cy, 19000, 0, 2);
                }
                for (int index = 0; index < count; index++) {
                    int cell = v6ObjectCell(cx, cy, ox, oy, index, 19100);
                    if (cell < 0) continue;
                    long key = v6ObjectKey(cell, 0x4100 + index);
                    if (st.objectState(key) == 0) {
                        Heap heap = heaps.get(cell);
                        if (heap == null || heap.type != Heap.Type.HEAP) st.setObjectState(key, 1);
                    }
                }

                int ironChance = state().generatorVersion >= 9 ? 8 : 38;
                int crystalChance = state().generatorVersion >= 9 ? 5 : 32;
                if (v9AnomalyType(cx, cy) == 0
                        && Math.floorMod(hash(cx, cy, 19400), 100L) < ironChance) {
                    snapshotV6KeyState(cx, cy, ox, oy, 0);
                }
                if (v9AnomalyType(cx, cy) == 0
                        && Math.floorMod(hash(cx, cy, 19401), 100L) < crystalChance) {
                    snapshotV6KeyState(cx, cy, ox, oy, 1);
                }
            }
        });
    }

    private void snapshotV6KeyState(int cx, int cy, int ox, int oy, int index) {
        int cell = v6ObjectCell(cx, cy, ox, oy, index, 19500);
        if (cell < 0) return;
        long key = v6ObjectKey(cell, 0x4200 + index);
        if (state().objectState(key) == 0) {
            Heap heap = heaps.get(cell);
            if (heap == null || heap.type != Heap.Type.HEAP) state().setObjectState(key, 1);
        }
    }

    private void generateV6Plants() {
        if (state().generatorVersion < 6) return;
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int count;
                if (state().generatorVersion >= 9) {
                    count = v9AnomalyType(cx, cy) == 0
                            && Math.floorMod(hash(cx, cy, 19600), 100L) < 45 ? 1 : 0;
                } else {
                    count = 1 + range(cx, cy, 19600, 0, 3);
                }
                for (int index = 0; index < count; index++) {
                    int cell = v6ObjectCell(cx, cy, ox, oy, index, 19700);
                    if (cell < 0) continue;
                    long key = v6ObjectKey(cell, 0x4300 + index);
                    if (st.objectState(key) != 0) continue;

                    if (heaps.get(cell) != null || traps.get(cell) != null || plants.get(cell) != null) {
                        st.setObjectState(key, 1);
                        continue;
                    }

                    Plant.Seed seed = v6PlantSeed(cx, cy, index);
                    Plant plant = seed.couch(cell, InfiniteWorldLevel.this);
                    plants.put(cell, plant);
                }
            }
        });
    }

    private void snapshotV6PlantStates() {
        if (state().generatorVersion < 6) return;
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int count;
                if (state().generatorVersion >= 9) {
                    count = v9AnomalyType(cx, cy) == 0
                            && Math.floorMod(hash(cx, cy, 19600), 100L) < 45 ? 1 : 0;
                } else {
                    count = 1 + range(cx, cy, 19600, 0, 3);
                }
                for (int index = 0; index < count; index++) {
                    int cell = v6ObjectCell(cx, cy, ox, oy, index, 19700);
                    if (cell < 0) continue;
                    long key = v6ObjectKey(cell, 0x4300 + index);
                    if (st.objectState(key) == 0 && plants.get(cell) == null) {
                        st.setObjectState(key, 1);
                    }
                }
            }
        });
    }

    private int v6ObjectCell(int cx, int cy, int ox, int oy, int index, int saltBase) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int salt = saltBase + index * 50 + attempt * 3;
            int x = 2 + range(cx, cy, salt, 0, CHUNK_SIZE - 5);
            int y = 2 + range(cx, cy, salt + 1, 0, CHUNK_SIZE - 5);
            int cell = ox + x + (oy + y) * width();

            if (state().generatorVersion >= 7
                    && (v7LocalCellInsideRoom(cx, cy, x, y)
                    || v8LocalCellReservedForRoomAccess(cx, cy, x, y))) continue;

            int t = baseWindow != null && cell < baseWindow.length ? baseWindow[cell] : map[cell];

            if (t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.GRASS
                    || t == Terrain.EMBERS || t == Terrain.HIGH_GRASS) {
                return cell;
            }
        }
        return -1;
    }

    private boolean v7LocalCellInsideRoom(int cx, int cy, int x, int y) {
        if (state().generatorVersion < 7 || (cx == 0 && cy == 0)) return false;
        int count = v7RoomCount(cx, cy);
        for (int i = 0; i < count; i++) {
            int[] s = v7RoomSpec(cx, cy, i);
            if (x >= s[0] && x <= s[2] && y >= s[1] && y <= s[3]) return true;
        }
        return false;
    }

    private boolean v8LocalCellReservedForRoomAccess(int cx, int cy, int x, int y) {
        if (state().generatorVersion < 8 || (cx == 0 && cy == 0)) return false;
        int count = v7RoomCount(cx, cy);
        for (int i = 0; i < count; i++) {
            int[] s = v7RoomSpec(cx, cy, i);
            if ((x == s[4] && y == s[5]) || (x == s[6] && y == s[7])) return true;
        }
        return false;
    }

    private long v6ObjectKey(int cell, int salt) {
        long world = worldKeyForLocalCell(cell);
        long mix = 0x9E3779B97F4A7C15L * (salt + 0x632BE5AB);
        return world ^ Long.rotateLeft(mix, salt & 31);
    }

    private Item v6LooseItem(int cx, int cy, int index) {
        if (state().generatorVersion >= 22) {
            if (index > 0) return v22BonusSupplyItem(cx, cy, index);
            return v22BalancedSupplySequence(v22LooseSupplySequence(cx, cy));
        }

        int roll = range(cx, cy, 19800 + index, 0, 99);
        if (roll < 20) return v6RandomFood(cx, cy, 19850 + index);
        if (roll < 39) return v6EquipmentItem(cx, cy, 19860 + index);
        if (roll < 55) return v6SafeScroll(cx, cy, 19870 + index);
        if (roll < 67) return v6RandomPotion(cx, cy, 19880 + index);
        if (roll < 77) return v6SeedItem(cx, cy, 19890 + index);
        if (roll < 86) return new Bomb();
        if (roll < 93) return new StoneOfBlink();
        return new Torch();
    }

    private Item v6EquipmentItem(int cx, int cy, int salt) {
        Random.pushGenerator(hash(cx, cy, salt));
        try {
            int kind = range(cx, cy, salt + 1, 0, 99);
            int tier = range(cx, cy, salt + 2, 0, 4);
            if (kind < 44) return Generator.randomWeapon(tier, true);
            if (kind < 76) return Generator.randomArmor(tier);
            if (kind < 90) return Generator.randomUsingDefaults(Generator.Category.WAND);
            return Generator.randomUsingDefaults(Generator.Category.RING);
        } finally {
            Random.popGenerator();
        }
    }

    private Item v6RandomFood(int cx, int cy, int salt) {
        if (state().generatorVersion >= 22) {
            return v22FoodItem(v22VariantOrdinal(cx, cy, salt));
        }
        switch (range(cx, cy, salt, 0, 5)) {
            case 0: return new Food();
            case 1: return new SmallRation();
            case 2: return new Pasty();
            case 3: return new MysteryMeat();
            case 4: return new MeatPie();
            default:return new SupplyRation();
        }
    }

    private Item v6SafeScroll(int cx, int cy, int salt) {
        if (state().generatorVersion >= 22) {
            return v22CategoryItem(Generator.Category.SCROLL,
                    v22VariantOrdinal(cx, cy, salt), 0);
        }
        switch (range(cx, cy, salt, 0, 8)) {
            case 0: return new ScrollOfIdentify();
            case 1: return new ScrollOfRemoveCurse();
            case 2: return new ScrollOfMirrorImage();
            case 3: return new ScrollOfRecharging();
            case 4: return new ScrollOfTeleportation();
            case 5: return new ScrollOfLullaby();
            case 6: return new ScrollOfRage();
            case 7: return new ScrollOfTerror();
            default:return new ScrollOfTransmutation();
        }
    }

    private Item v6RandomPotion(int cx, int cy, int salt) {
        if (state().generatorVersion >= 22) {
            return v22CategoryItem(Generator.Category.POTION,
                    v22VariantOrdinal(cx, cy, salt), 0);
        }
        switch (range(cx, cy, salt, 0, 5)) {
            case 0: return new PotionOfHealing();
            case 1: return new PotionOfInvisibility();
            case 2: return new PotionOfLiquidFlame();
            case 3: return new PotionOfFrost();
            case 4: return new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste();
            default:return new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation();
        }
    }

    private Item v6SeedItem(int cx, int cy, int salt) {
        return v6PlantSeed(cx, cy, salt);
    }

    private Plant.Seed v6PlantSeed(int cx, int cy, int salt) {
        if (state().generatorVersion >= 22) {
            Item item = v22CategoryItem(Generator.Category.SEED,
                    v22VariantOrdinal(cx, cy, 19900 + salt), 1);
            return (Plant.Seed)item;
        }
        switch (range(cx, cy, 19900 + salt, 0, 10)) {
            case 0: return new Firebloom.Seed();
            case 1: return new Icecap.Seed();
            case 2: return new Sungrass.Seed();
            case 3: return new Earthroot.Seed();
            case 4: return new Fadeleaf.Seed();
            case 5: return new Sorrowmoss.Seed();
            case 6: return new Swiftthistle.Seed();
            case 7: return new Blindweed.Seed();
            case 8: return new Stormvine.Seed();
            case 9: return new Mageroyal.Seed();
            default:return new Starflower.Seed();
        }
    }

    /**
     * V22 resource coverage.
     *
     * Loot identity is no longer chosen by a chain of independent percentage
     * rolls. Each chunk has a stable square-spiral ordinal, then supply families
     * and sub-types advance through fixed complete cycles. This means a run can
     * still look varied in space, but bad luck cannot produce seven Blink stones
     * while every other runestone is absent.
     */
    private long v22SpiralOrdinal(int x, int y) {
        int r = Math.max(Math.abs(x), Math.abs(y));
        if (r == 0) return 0L;

        long side = 2L * r;
        long width = 2L * r + 1L;
        long max = width * width - 1L;

        if (y == -r) {
            return max - (r - x);
        } else if (x == -r) {
            return max - side - (y + r);
        } else if (y == r) {
            return max - 2L * side - (x + r);
        } else {
            return max - 3L * side - (r - y);
        }
    }

    private long v22VariantOrdinal(int cx, int cy, int salt) {
        long ordinal = v22SpiralOrdinal(cx, cy);
        return ordinal + (long)salt * 31L;
    }

    private int v22LooseLootCount(int cx, int cy) {
        if (v9AnomalyType(cx, cy) != 0) return 0;

        long ordinal = v22SpiralOrdinal(cx, cy);
        int slot = (int)Math.floorMod(ordinal, 5L);

        // Exactly 40% of ordinary chunks carry one loose supply item; every
        // 37th spiral slot gets a second. Density is fixed, not a chance roll.
        int count = slot < 2 ? 1 : 0;
        if (Math.floorMod(ordinal, 37L) == 0L) count++;
        return count;
    }

    @SuppressWarnings("unchecked")
    private Item v22CategoryItem(Generator.Category category, long ordinal, int firstClass) {
        Class<?>[] classes = category.classes;
        int available = classes.length - firstClass;
        if (available <= 0) return null;

        int index = firstClass + (int)Math.floorMod(ordinal, (long)available);
        Item item = (Item)Reflection.newInstance((Class<? extends Item>)classes[index]);
        return item == null ? null : item.random();
    }

    @SuppressWarnings("unchecked")
    private Item v22NormalCategoryItem(Generator.Category category, long ordinal) {
        Class<?>[] classes = category.classes;
        ArrayList<Class<?>> available = new ArrayList<>();

        for (int i = 0; i < classes.length; i++) {
            if (category.defaultProbs == null
                    || i >= category.defaultProbs.length
                    || category.defaultProbs[i] > 0f) {
                available.add(classes[i]);
            }
        }
        if (available.isEmpty()) return null;

        int index = (int)Math.floorMod(ordinal, (long)available.size());
        Item item = (Item)Reflection.newInstance((Class<? extends Item>)available.get(index));
        return item == null ? null : item.random();
    }

    private Item v22FoodItem(long ordinal) {
        switch ((int)Math.floorMod(ordinal, 6L)) {
            case 0: return new Food();
            case 1: return new SmallRation();
            case 2: return new Pasty();
            case 3: return new MysteryMeat();
            case 4: return new MeatPie();
            default:return new SupplyRation();
        }
    }

    private Item v22StoneItem(int cx, int cy, int salt) {
        return v22CategoryItem(Generator.Category.STONE,
                v22VariantOrdinal(cx, cy, salt), 0);
    }

    private Generator.Category v22MeleeTier(long ordinal) {
        switch ((int)Math.floorMod(ordinal, 5L)) {
            case 0: return Generator.Category.WEP_T1;
            case 1: return Generator.Category.WEP_T2;
            case 2: return Generator.Category.WEP_T3;
            case 3: return Generator.Category.WEP_T4;
            default:return Generator.Category.WEP_T5;
        }
    }

    private Generator.Category v22MissileTier(long ordinal) {
        switch ((int)Math.floorMod(ordinal, 5L)) {
            case 0: return Generator.Category.MIS_T1;
            case 1: return Generator.Category.MIS_T2;
            case 2: return Generator.Category.MIS_T3;
            case 3: return Generator.Category.MIS_T4;
            default:return Generator.Category.MIS_T5;
        }
    }

    private Item v22EquipmentItem(long ordinal) {
        int family = (int)Math.floorMod(ordinal, 5L);
        long variant = Math.floorDiv(ordinal, 5L);

        switch (family) {
            case 0:
                return v22NormalCategoryItem(v22MeleeTier(variant), variant / 5L);
            case 1: {
                // Only the five ordinary armor tiers are world loot; class armor
                // remains tied to its own progression systems.
                Class<?>[] armor = Generator.Category.ARMOR.classes;
                int idx = (int)Math.floorMod(variant, 5L);
                Item item = (Item)Reflection.newInstance((Class<? extends Item>)armor[idx]);
                return item == null ? null : item.random();
            }
            case 2:
                return v22CategoryItem(Generator.Category.WAND, variant, 0);
            case 3:
                return v22CategoryItem(Generator.Category.RING, variant, 0);
            default:
                return v22NormalCategoryItem(v22MissileTier(variant), variant / 5L);
        }
    }

    private long v22LooseSupplySequence(int cx, int cy) {
        long ordinal = v22SpiralOrdinal(cx, cy);
        long block = Math.floorDiv(ordinal, 5L);
        int remainder = (int)Math.floorMod(ordinal, 5L);

        // Main loose drops occur only at remainder 0/1, so map them back to a
        // gap-free sequence: 0,1,2,3,4,5... This is the key piece that prevents
        // spatial sparsity from biasing the item-family cycle.
        return block * 2L + Math.min(remainder, 1);
    }

    private Item v22BonusSupplyItem(int cx, int cy, int index) {
        long ordinal = v22SpiralOrdinal(cx, cy) + index;
        switch ((int)Math.floorMod(ordinal, 9L)) {
            case 0: return v22FoodItem(ordinal);
            case 1: return v22EquipmentItem(ordinal);
            case 2: return v22CategoryItem(Generator.Category.SCROLL, ordinal, 0);
            case 3: return v22CategoryItem(Generator.Category.POTION, ordinal, 0);
            case 4: return v22CategoryItem(Generator.Category.SEED, ordinal, 1);
            case 5: return new Bomb();
            case 6: return new Torch();
            case 7: return new Gold(10 + (int)Math.floorMod(ordinal, 20L));
            default:return new Pickaxe();
        }
    }

    private Item v22BalancedSupplyItem(int cx, int cy, int index, int salt) {
        long seq = v22SpiralOrdinal(cx, cy) + index + (long)salt;
        return v22BalancedSupplySequence(seq);
    }

    private Item v22BalancedSupplySequence(long seq) {
        int slot = (int)Math.floorMod(seq, 12L);
        long cycle = Math.floorDiv(seq, 12L);

        switch (slot) {
            case 0:
                return v22FoodItem(cycle);
            case 1:
                return v22EquipmentItem(cycle);
            case 2:
                return v22CategoryItem(Generator.Category.SCROLL, cycle, 0);
            case 3:
                return v22CategoryItem(Generator.Category.POTION, cycle, 0);
            case 4:
                return v22CategoryItem(Generator.Category.SEED, cycle, 1);
            case 5:
                return v22CategoryItem(Generator.Category.STONE, cycle * 2L, 0);
            case 6:
                return new Bomb();
            case 7:
                return new Torch();
            case 8:
                return new Gold(8 + (int)Math.floorMod(cycle, 25L));
            case 9:
                return v22CategoryItem(Generator.Category.STONE, cycle * 2L + 1L, 0);
            case 10:
                return new Pickaxe();
            default:
                return v22EquipmentItem(cycle + 1L);
        }
    }

    private long v15MerchantSiteKey(int cx, int cy) {
        long world = encodeWorld((long)cx * CHUNK_SIZE + 1L, (long)cy * CHUNK_SIZE + 1L);
        return world ^ 0x5A17B6C34D29E8F1L;
    }

    private boolean v15MerchantCandidateForTier(int cx, int cy, int tier) {
        int baseX = (int)Math.floorMod(hash(0, 0, 25000), (long)MERCHANT_SPACING_CHUNKS_V15);
        int baseY = (int)Math.floorMod(hash(0, 0, 25001), (long)MERCHANT_SPACING_CHUNKS_V15);

        // Tier 0 already uses a denser 3x3 lattice than V14's 4x4 lattice.
        if (Math.floorMod(cx - baseX, 3) == 0
                && Math.floorMod(cy - baseY, 3) == 0) {
            return true;
        }

        // Newly generated chunks gain extra independent sparse lattices as the
        // run matures. The result is locked per chunk, so revisiting old terrain
        // can never make a shop suddenly appear or disappear.
        if (tier >= 1) {
            int x = (int)Math.floorMod(hash(0, 0, 25010), 6L);
            int y = (int)Math.floorMod(hash(0, 0, 25011), 6L);
            if (Math.floorMod(cx - x, 6) == 0 && Math.floorMod(cy - y, 6) == 0) return true;
        }
        if (tier >= 2) {
            int x = (int)Math.floorMod(hash(0, 0, 25012), 5L);
            int y = (int)Math.floorMod(hash(0, 0, 25013), 5L);
            if (Math.floorMod(cx - x, 5) == 0 && Math.floorMod(cy - y, 5) == 0) return true;
        }
        if (tier >= 3) {
            int x = (int)Math.floorMod(hash(0, 0, 25014), 7L);
            int y = (int)Math.floorMod(hash(0, 0, 25015), 7L);
            if (Math.floorMod(cx - x, 7) == 0 && Math.floorMod(cy - y, 7) == 0) return true;
        }
        if (tier >= 4) {
            int x = (int)Math.floorMod(hash(0, 0, 25016), 4L);
            int y = (int)Math.floorMod(hash(0, 0, 25017), 4L);
            if (Math.floorMod(cx - x, 4) == 0 && Math.floorMod(cy - y, 4) == 0) return true;
        }

        return false;
    }

    private boolean isV11MerchantChunk(int cx, int cy) {
        if (state().generatorVersion < 11) return false;
        if (v9AnomalyType(cx, cy) != 0) return false;

        // Do not place a shop room directly on the guaranteed infinite road
        // network. V10 carves those roads after room generation, which would
        // otherwise cut through the shop shell.
        if (Math.floorMod(cx, 6) == 0 || Math.floorMod(cy, 6) == 0
                || v10SecondaryHorizontalRow(cy) || v10SecondaryVerticalColumn(cx)) {
            return false;
        }

        if (state().generatorVersion >= 15) {
            if (Math.max(Math.abs(cx), Math.abs(cy)) <= 1) return false;

            long siteKey = v15MerchantSiteKey(cx, cy);
            int locked = state().objectState(siteKey);
            if (locked == 1) return false;
            if (locked == 2) return true;

            boolean merchant = v15MerchantCandidateForTier(cx, cy, v15CurrentMerchantProgressTier());
            state().setObjectState(siteKey, merchant ? 2 : 1);
            return merchant;
        }

        int spacing = state().generatorVersion >= 13 ? MERCHANT_SPACING_CHUNKS_V13
                : (state().generatorVersion >= 12 ? MERCHANT_SPACING_CHUNKS_V12 : MERCHANT_SPACING_CHUNKS);
        int startBuffer = state().generatorVersion >= 13 ? 2
                : (state().generatorVersion >= 12 ? 3 : 4);
        if (Math.max(Math.abs(cx), Math.abs(cy)) <= startBuffer) return false;

        int offsetX = (int)Math.floorMod(hash(0, 0, 25000), (long)spacing);
        int offsetY = (int)Math.floorMod(hash(0, 0, 25001), (long)spacing);

        return Math.floorMod(cx - offsetX, spacing) == 0
                && Math.floorMod(cy - offsetY, spacing) == 0;
    }

    private void ensureV13MerchantDiscovery(Hero hero) {
        if (hero == null || state().generatorVersion < 13) return;

        int heroCx = Math.floorDiv(state().heroWorldX, CHUNK_SIZE);
        int heroCy = Math.floorDiv(state().heroWorldY, CHUNK_SIZE);
        boolean announced = false;

        for (Mob mob : mobs.toArray(new Mob[0])) {
            if (!(mob instanceof InfiniteWorldShopkeeper)) continue;

            InfiniteWorldShopkeeper shop = (InfiniteWorldShopkeeper)mob;
            int cx = shop.shopChunkX();
            int cy = shop.shopChunkY();
            int chunkDistance = Math.max(Math.abs(cx - heroCx), Math.abs(cy - heroCy));

            if (chunkDistance <= HALF_WINDOW) {
                revealV13MerchantOutpost(cx, cy);
            }

            if (chunkDistance <= 2) {
                if (state().generatorVersion >= 15) {
                    ensureV15MerchantProgressStock(cx, cy);
                }

                long hintKey = v11MerchantObjectKey(cx, cy, 0x63F1);
                if (state().objectState(hintKey) == 0) {
                    state().setObjectState(hintKey, 1);
                    if (!announced) {
                        GLog.i(Messages.get(this, "merchant_nearby"));
                        announced = true;
                    }
                }
            }
        }
    }

    private void revealV13MerchantOutpost(int cx, int cy) {
        int chunkLocalX = (cx - (state().centerChunkX - HALF_WINDOW)) * CHUNK_SIZE;
        int chunkLocalY = (cy - (state().centerChunkY - HALF_WINDOW)) * CHUNK_SIZE;
        if (chunkLocalX < 0 || chunkLocalX >= width()
                || chunkLocalY < 0 || chunkLocalY >= height()) return;

        int[] spec = v7RoomSpec(cx, cy, 0);
        int left = chunkLocalX + spec[0];
        int top = chunkLocalY + spec[1];
        int right = chunkLocalX + spec[2];
        int bottom = chunkLocalY + spec[3];

        for (int y = Math.max(0, top - 1); y <= Math.min(height() - 1, bottom + 1); y++) {
            for (int x = Math.max(0, left - 1); x <= Math.min(width() - 1, right + 1); x++) {
                int cell = x + y * width();
                mapped[cell] = true;
                state().markMapped(worldKeyForLocalCell(cell));
            }
        }

        for (int slot = 0; slot < MERCHANT_STOCK_SLOTS; slot++) {
            int cell = v11MerchantStockCell(cx, cy, chunkLocalX, chunkLocalY, slot);
            Heap heap = cell >= 0 ? heaps.get(cell) : null;
            if (heap != null && heap.type == Heap.Type.FOR_SALE) heap.seen = true;
        }

        // Do not reveal an isolated landmark with no clue how to reach it. V14
        // also maps the verified walkable access route from the shop to a shared
        // chunk gateway. Secret rooms stay hidden because SECRET_DOOR is not passable.
        ArrayList<Integer> access = merchantWalkPath(map, cx, cy, chunkLocalX, chunkLocalY);
        for (int cell : access) {
            mapped[cell] = true;
            state().markMapped(worldKeyForLocalCell(cell));
        }

        int fogLeft = Math.max(0, left - 2);
        int fogTop = Math.max(0, top - 2);
        int fogRight = Math.min(width(), right + 3);
        int fogBottom = Math.min(height(), bottom + 3);
        GameScene.updateFog(fogLeft, fogTop, fogRight - fogLeft, fogBottom - fogTop);
    }


    private void repairLegacyMerchantAccessInWindow() {
        if (state().generatorVersion < 11) return;

        for (int wy = -HALF_WINDOW; wy <= HALF_WINDOW; wy++) {
            for (int wx = -HALF_WINDOW; wx <= HALF_WINDOW; wx++) {
                int cx = state().centerChunkX + wx;
                int cy = state().centerChunkY + wy;
                if (!isV11MerchantChunk(cx, cy)) continue;

                int ox = (wx + HALF_WINDOW) * CHUNK_SIZE;
                int oy = (wy + HALF_WINDOW) * CHUNK_SIZE;
                ensureMerchantAccessInChunk(map, cx, cy, ox, oy);
            }
        }
    }

    private void ensureMerchantAccessInChunk(int[] terrain, int cx, int cy, int ox, int oy) {
        if (!isV11MerchantChunk(cx, cy)) return;

        int[] spec = v7RoomSpec(cx, cy, 0);
        int doorX = ox + spec[4];
        int doorY = oy + spec[5];
        int outsideX = ox + spec[6];
        int outsideY = oy + spec[7];

        int doorCell = doorX + doorY * MAP_SIZE;
        int outsideCell = outsideX + outsideY * MAP_SIZE;

        if ((Terrain.flags[terrain[doorCell]] & Terrain.PASSABLE) == 0) {
            terrain[doorCell] = Terrain.DOOR;
        }
        if ((Terrain.flags[terrain[outsideCell]] & Terrain.PASSABLE) == 0) {
            setFloor(terrain, outsideX, outsideY);
        }

        if (!merchantWalkPath(terrain, cx, cy, ox, oy).isEmpty()) return;

        ArrayList<Integer> digPath = merchantDigPath(cx, cy, ox, oy);
        if (digPath.isEmpty()) {
            digPath = merchantFallbackDigPath(cx, cy, ox, oy);
        }

        for (int cell : digPath) {
            int x = cell % MAP_SIZE;
            int y = cell / MAP_SIZE;
            setFloor(terrain, x, y);
        }

        // A later dig path must never leave the actual shop entrance blocked,
        // but preserve a player-opened/destroyed entrance if it is already passable.
        if ((Terrain.flags[terrain[doorCell]] & Terrain.PASSABLE) == 0) {
            terrain[doorCell] = Terrain.DOOR;
        }
        if ((Terrain.flags[terrain[outsideCell]] & Terrain.PASSABLE) == 0) {
            setFloor(terrain, outsideX, outsideY);
        }
    }

    private ArrayList<Integer> merchantWalkPath(int[] terrain, int cx, int cy, int ox, int oy) {
        int[] spec = v7RoomSpec(cx, cy, 0);
        int startX = spec[6];
        int startY = spec[7];

        int northX = edgeHorizontal(cx, cy);
        int southX = edgeHorizontal(cx, cy + 1);
        int westY = edgeVertical(cx, cy);
        int eastY = edgeVertical(cx + 1, cy);

        boolean[] targets = new boolean[CHUNK_SIZE * CHUNK_SIZE];
        targets[northX] = true;
        targets[southX + (CHUNK_SIZE - 1) * CHUNK_SIZE] = true;
        targets[westY * CHUNK_SIZE] = true;
        targets[(CHUNK_SIZE - 1) + eastY * CHUNK_SIZE] = true;

        int start = startX + startY * CHUNK_SIZE;
        int[] parent = new int[CHUNK_SIZE * CHUNK_SIZE];
        Arrays.fill(parent, -2);
        int[] queue = new int[CHUNK_SIZE * CHUNK_SIZE];
        int head = 0;
        int tail = 0;

        parent[start] = -1;
        queue[tail++] = start;

        int found = -1;
        final int[] dx = new int[]{1, 0, -1, 0};
        final int[] dy = new int[]{0, 1, 0, -1};

        while (head < tail) {
            int cur = queue[head++];
            if (targets[cur]) {
                found = cur;
                break;
            }

            int lx = cur % CHUNK_SIZE;
            int ly = cur / CHUNK_SIZE;

            for (int d = 0; d < 4; d++) {
                int nx = lx + dx[d];
                int ny = ly + dy[d];
                if (nx < 0 || ny < 0 || nx >= CHUNK_SIZE || ny >= CHUNK_SIZE) continue;

                int next = nx + ny * CHUNK_SIZE;
                if (parent[next] != -2) continue;

                int global = ox + nx + (oy + ny) * MAP_SIZE;
                int flags = Terrain.flags[terrain[global]];
                if ((flags & Terrain.PASSABLE) == 0) continue;

                parent[next] = cur;
                queue[tail++] = next;
            }
        }

        return reconstructMerchantPath(parent, found, ox, oy);
    }

    private ArrayList<Integer> merchantDigPath(int cx, int cy, int ox, int oy) {
        int[] spec = v7RoomSpec(cx, cy, 0);
        int start = spec[6] + spec[7] * CHUNK_SIZE;

        boolean[] blocked = new boolean[CHUNK_SIZE * CHUNK_SIZE];
        int roomCount = v7RoomCount(cx, cy);
        for (int roomIndex = 1; roomIndex < roomCount; roomIndex++) {
            int[] other = v7RoomSpec(cx, cy, roomIndex);
            for (int y = other[1]; y <= other[3]; y++) {
                for (int x = other[0]; x <= other[2]; x++) {
                    blocked[x + y * CHUNK_SIZE] = true;
                }
            }
        }

        int northX = edgeHorizontal(cx, cy);
        int southX = edgeHorizontal(cx, cy + 1);
        int westY = edgeVertical(cx, cy);
        int eastY = edgeVertical(cx + 1, cy);

        boolean[] targets = new boolean[CHUNK_SIZE * CHUNK_SIZE];
        targets[northX] = true;
        targets[southX + (CHUNK_SIZE - 1) * CHUNK_SIZE] = true;
        targets[westY * CHUNK_SIZE] = true;
        targets[(CHUNK_SIZE - 1) + eastY * CHUNK_SIZE] = true;

        int[] parent = new int[CHUNK_SIZE * CHUNK_SIZE];
        Arrays.fill(parent, -2);
        int[] queue = new int[CHUNK_SIZE * CHUNK_SIZE];
        int head = 0;
        int tail = 0;

        parent[start] = -1;
        queue[tail++] = start;

        int found = -1;
        int rotation = (int)Math.floorMod(hash(cx, cy, 25200), 4L);
        final int[] dx = new int[]{1, 0, -1, 0};
        final int[] dy = new int[]{0, 1, 0, -1};

        while (head < tail) {
            int cur = queue[head++];
            if (targets[cur]) {
                found = cur;
                break;
            }

            int lx = cur % CHUNK_SIZE;
            int ly = cur / CHUNK_SIZE;

            for (int step = 0; step < 4; step++) {
                int d = (rotation + step) & 3;
                int nx = lx + dx[d];
                int ny = ly + dy[d];
                if (nx < 0 || ny < 0 || nx >= CHUNK_SIZE || ny >= CHUNK_SIZE) continue;

                int next = nx + ny * CHUNK_SIZE;
                if (parent[next] != -2 || blocked[next]) continue;

                // Do not punch accidental extra gateways through the outer frame.
                if ((nx == 0 || ny == 0 || nx == CHUNK_SIZE - 1 || ny == CHUNK_SIZE - 1)
                        && !targets[next]) {
                    continue;
                }

                parent[next] = cur;
                queue[tail++] = next;
            }
        }

        return reconstructMerchantPath(parent, found, ox, oy);
    }

    private ArrayList<Integer> merchantFallbackDigPath(int cx, int cy, int ox, int oy) {
        int[] spec = v7RoomSpec(cx, cy, 0);
        int sx = spec[6];
        int sy = spec[7];

        int[][] targets = new int[][]{
                {edgeHorizontal(cx, cy), 0},
                {edgeHorizontal(cx, cy + 1), CHUNK_SIZE - 1},
                {0, edgeVertical(cx, cy)},
                {CHUNK_SIZE - 1, edgeVertical(cx + 1, cy)}
        };

        int best = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < targets.length; i++) {
            int dist = Math.abs(targets[i][0] - sx) + Math.abs(targets[i][1] - sy);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }

        int tx = targets[best][0];
        int ty = targets[best][1];
        ArrayList<Integer> result = new ArrayList<>();

        int x = sx;
        int y = sy;
        boolean xFirst = (hash(cx, cy, 25201) & 1L) == 0L;

        while (x != tx || y != ty) {
            if ((xFirst && x != tx) || y == ty) {
                x += Integer.compare(tx, x);
            } else {
                y += Integer.compare(ty, y);
            }
            result.add(ox + x + (oy + y) * MAP_SIZE);
        }
        return result;
    }

    private ArrayList<Integer> reconstructMerchantPath(int[] parent, int found, int ox, int oy) {
        ArrayList<Integer> result = new ArrayList<>();
        if (found < 0) return result;

        int cur = found;
        while (cur >= 0) {
            int lx = cur % CHUNK_SIZE;
            int ly = cur / CHUNK_SIZE;
            result.add(0, ox + lx + (oy + ly) * MAP_SIZE);
            cur = parent[cur];
        }
        return result;
    }

    private long v11MerchantObjectKey(int cx, int cy, int salt) {
        int[] spec = v7RoomSpec(cx, cy, 0);
        long wx = (long)cx * CHUNK_SIZE + (spec[0] + spec[2]) / 2;
        long wy = (long)cy * CHUNK_SIZE + (spec[1] + spec[3]) / 2;
        long world = encodeWorld(wx, wy);
        long mix = 0x9E3779B97F4A7C15L * (salt + 0x632BE5AB);
        return world ^ Long.rotateLeft(mix, salt & 31);
    }

    private boolean v11MerchantGone(int cx, int cy) {
        return state().objectState(v11MerchantObjectKey(cx, cy, 0x63F0)) != 0;
    }

    public void markInfiniteWorldMerchantGone(int cx, int cy) {
        if (!isV11MerchantChunk(cx, cy)) return;
        state().setObjectState(v11MerchantObjectKey(cx, cy, 0x63F0), 1);
    }

    private int v11MerchantCell(int cx, int cy, int ox, int oy) {
        int[] spec = v7RoomSpec(cx, cy, 0);
        int lx = (spec[0] + spec[2]) / 2;
        int ly = (spec[1] + spec[3]) / 2;
        return ox + lx + (oy + ly) * width();
    }

    private int v11MerchantStockCell(int cx, int cy, int ox, int oy, int slot) {
        int[] spec = v7RoomSpec(cx, cy, 0);
        int centerX = (spec[0] + spec[2]) / 2;
        int centerY = (spec[1] + spec[3]) / 2;

        ArrayList<Integer> candidates = new ArrayList<>();
        for (int ly = spec[1] + 1; ly <= spec[3] - 1; ly++) {
            for (int lx = spec[0] + 1; lx <= spec[2] - 1; lx++) {
                if (lx == centerX && ly == centerY) continue;
                int cell = ox + lx + (oy + ly) * width();
                if (cell < 0 || cell >= length()) continue;
                if (solid[cell] || pit[cell]) continue;
                candidates.add(cell);
            }
        }

        if (candidates.isEmpty()) return -1;
        int start = (int)Math.floorMod(hash(cx, cy, 25100), (long)candidates.size());
        return candidates.get(Math.floorMod(start + slot, candidates.size()));
    }

    private Item v11MerchantStockItem(int cx, int cy, int slot) {
        if (state().generatorVersion >= 15) {
            int progressTier = v15MerchantStoredProgressTier(cx, cy);
            if (progressTier < 0) progressTier = v15CurrentMerchantProgressTier();

            if (slot == 0) {
                Item bag = v15MerchantMissingBagOffer(cx, cy);
                if (bag != null) return bag;
                return new PotionOfHealing();
            }

            switch (slot) {
                case 1:
                    return progressTier >= 2 ? new Food() : new SmallRation();
                case 2:
                    if (progressTier == 0) {
                        return range(cx, cy, 25120, 0, 1) == 0
                                ? new PotionOfFrost() : new PotionOfLiquidFlame();
                    }
                    if (progressTier == 1) {
                        return v6RandomPotion(cx, cy, 25120);
                    }
                    return range(cx, cy, 25122, 0, 2) == 0
                            ? new PotionOfHealing() : v6RandomPotion(cx, cy, 25120);
                case 3:
                    if (progressTier <= 1) return v6SafeScroll(cx, cy, 25130);
                    if (progressTier >= 3 && range(cx, cy, 25131, 0, 3) == 0) {
                        return new ScrollOfTransmutation();
                    }
                    return range(cx, cy, 25132, 0, 2) == 0
                            ? new ScrollOfRemoveCurse() : v6SafeScroll(cx, cy, 25130);
                case 4:
                    return v11MerchantEquipment(cx, cy);
                default:
                    return v15MerchantSpecialItem(cx, cy, progressTier);
            }
        }

        switch (slot) {
            case 0:
                return new PotionOfHealing();
            case 1:
                return new SmallRation();
            case 2:
                return v6RandomPotion(cx, cy, 25120);
            case 3:
                return v6SafeScroll(cx, cy, 25130);
            case 4:
                return v11MerchantEquipment(cx, cy);
            default:
                switch (range(cx, cy, 25150, 0, 4)) {
                    case 0: return new Bomb();
                    case 1: return new Torch();
                    case 2: return state().generatorVersion >= 22
                            ? v22StoneItem(cx, cy, 25152) : new StoneOfBlink();
                    case 3: return new Pickaxe();
                    default:return new ScrollOfRemoveCurse();
                }
        }
    }

    private int v15CurrentMerchantProgressTier() {
        int heroLevel = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
        float action = state().heroActionValue;

        int levelTier = 0;
        if (heroLevel >= 4) levelTier = 1;
        if (heroLevel >= 7) levelTier = 2;
        if (heroLevel >= 11) levelTier = 3;
        if (heroLevel >= 15) levelTier = 4;

        int actionTier = 0;
        if (action >= 300f) actionTier = 1;
        if (action >= 800f) actionTier = 2;
        if (action >= 1600f) actionTier = 3;
        if (action >= 2800f) actionTier = 4;

        return Math.max(levelTier, actionTier);
    }

    private int v15MerchantStoredProgressTier(int cx, int cy) {
        int stored = state().objectState(v11MerchantObjectKey(cx, cy, 0x63F3));
        return stored <= 0 ? -1 : Math.min(4, stored - 1);
    }

    private int v15MissingBagMask() {
        if (Dungeon.hero == null) return 0;

        int mask = 0;
        if (Dungeon.hero.belongings.getItem(VelvetPouch.class) == null) mask |= 1;
        if (Dungeon.hero.belongings.getItem(ScrollHolder.class) == null) mask |= 2;
        if (Dungeon.hero.belongings.getItem(PotionBandolier.class) == null) mask |= 4;
        if (Dungeon.hero.belongings.getItem(MagicalHolster.class) == null) mask |= 8;
        return mask;
    }

    private Item v15BagByIndex(int index) {
        switch (index) {
            case 0: return new VelvetPouch();
            case 1: return new ScrollHolder();
            case 2: return new PotionBandolier();
            case 3: return new MagicalHolster();
            default:return null;
        }
    }

    private int v15ChooseMissingBagIndex(int cx, int cy, int missingMask) {
        ArrayList<Integer> missing = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if ((missingMask & (1 << i)) != 0) missing.add(i);
        }
        if (missing.isEmpty()) return -1;
        int pick = (int)Math.floorMod(hash(cx, cy, 25180), (long)missing.size());
        return missing.get(pick);
    }

    private Item v15MerchantMissingBagOffer(int cx, int cy) {
        int missingMask = v15MissingBagMask();
        if (missingMask == 0) return null;

        long key = v11MerchantObjectKey(cx, cy, 0x63F4);
        int stored = state().objectState(key);
        int bagIndex = stored - 1;

        // If this merchant was loaded before the player reached it, or the player
        // acquired that bag elsewhere, retarget the guaranteed offer to a bag that
        // is still actually missing. The choice locks again once the player is near.
        if (bagIndex < 0 || bagIndex > 3 || (missingMask & (1 << bagIndex)) == 0) {
            bagIndex = v15ChooseMissingBagIndex(cx, cy, missingMask);
        }

        return v15BagByIndex(bagIndex);
    }

    private Item v15MerchantSpecialItem(int cx, int cy, int progressTier) {
        if (state().generatorVersion >= 22) {
            return v22BalancedSupplyItem(cx, cy, Math.max(0, progressTier), 25150);
        }

        if (progressTier <= 0) {
            switch (range(cx, cy, 25150, 0, 4)) {
                case 0: return new Bomb();
                case 1: return new Torch();
                case 2: return new StoneOfBlink();
                case 3: return new Pickaxe();
                default:return new ScrollOfRemoveCurse();
            }
        }

        Random.pushGenerator(hash(cx, cy, 25170 + progressTier));
        try {
            int roll = range(cx, cy, 25171 + progressTier, 0, 99);

            if (progressTier == 1) {
                if (roll < 25) return Generator.randomUsingDefaults(Generator.Category.WAND);
                if (roll < 45) return new ScrollOfTransmutation();
                if (roll < 70) return state().generatorVersion >= 22
                        ? v22StoneItem(cx, cy, 25172 + progressTier) : new StoneOfBlink();
                return new ScrollOfRemoveCurse();
            }

            if (progressTier == 2) {
                if (roll < 35) return Generator.randomUsingDefaults(Generator.Category.WAND);
                if (roll < 60) return Generator.randomUsingDefaults(Generator.Category.RING);
                if (roll < 80) return new ScrollOfTransmutation();
                return state().generatorVersion >= 22
                        ? v22StoneItem(cx, cy, 25173 + progressTier) : new StoneOfBlink();
            }

            if (progressTier == 3) {
                if (roll < 40) return Generator.randomUsingDefaults(Generator.Category.RING);
                if (roll < 75) return Generator.randomUsingDefaults(Generator.Category.WAND);
                return new ScrollOfTransmutation();
            }

            if (roll < 45) return Generator.randomUsingDefaults(Generator.Category.RING);
            if (roll < 85) return Generator.randomUsingDefaults(Generator.Category.WAND);
            return new ScrollOfTransmutation();
        } finally {
            Random.popGenerator();
        }
    }

    private void ensureV15MerchantProgressStock(int cx, int cy) {
        if (state().generatorVersion < 15 || Dungeon.hero == null) return;

        long tierKey = v11MerchantObjectKey(cx, cy, 0x63F3);
        int storedTier = state().objectState(tierKey);
        if (storedTier == 0) {
            storedTier = v15CurrentMerchantProgressTier() + 1;
            state().setObjectState(tierKey, storedTier);
        }

        int missingMask = v15MissingBagMask();
        long bagKey = v11MerchantObjectKey(cx, cy, 0x63F4);
        int storedBag = state().objectState(bagKey);
        int bagIndex = storedBag - 1;

        if (missingMask != 0
                && (bagIndex < 0 || bagIndex > 3 || (missingMask & (1 << bagIndex)) == 0)) {
            bagIndex = v15ChooseMissingBagIndex(cx, cy, missingMask);
            if (bagIndex >= 0) state().setObjectState(bagKey, bagIndex + 1);
        }

        int ox = (cx - (state().centerChunkX - HALF_WINDOW)) * CHUNK_SIZE;
        int oy = (cy - (state().centerChunkY - HALF_WINDOW)) * CHUNK_SIZE;
        if (ox < 0 || oy < 0 || ox >= width() || oy >= height()) return;

        for (int slot = 0; slot < MERCHANT_STOCK_SLOTS; slot++) {
            int cell = v11MerchantStockCell(cx, cy, ox, oy, slot);
            if (cell < 0) continue;

            long consumedKey = v6ObjectKey(cell, 0x6300 + slot);
            if (state().objectState(consumedKey) != 0) continue;

            Heap heap = heaps.get(cell);
            if (heap == null || heap.type != Heap.Type.FOR_SALE) continue;

            heap.items.clear();
            Item replacement = v11MerchantStockItem(cx, cy, slot);
            if (replacement != null) heap.drop(replacement);
        }
    }

    private Item v11MerchantEquipment(int cx, int cy) {
        int tier;
        if (state().generatorVersion >= 15) {
            tier = v15MerchantStoredProgressTier(cx, cy);
            if (tier < 0) tier = v15CurrentMerchantProgressTier();
        } else {
            int worldDistance = Math.max(Math.abs(cx), Math.abs(cy));
            tier = Math.min(4, Math.max(0, worldDistance / 12));
        }

        Random.pushGenerator(hash(cx, cy, 25140));
        try {
            Item item;
            int kind = range(cx, cy, 25141, 0, 99);

            if (state().generatorVersion >= 15 && tier == 0) {
                item = kind < 58 ? Generator.randomWeapon(0, true) : Generator.randomArmor(0);
            } else if (kind < 46) {
                item = Generator.randomWeapon(tier, true);
            } else if (kind < 78) {
                item = Generator.randomArmor(tier);
            } else if (kind < 90) {
                item = Generator.randomUsingDefaults(Generator.Category.WAND);
            } else {
                item = Generator.randomUsingDefaults(Generator.Category.RING);
            }

            item.cursed = false;
            item.cursedKnown = true;
            if (item.isUpgradable()) {
                int itemLevel = 0;
                if (state().generatorVersion >= 15) {
                    int upgradeChance = tier >= 4 ? 45 : (tier >= 3 ? 25 : 0);
                    if (range(cx, cy, 25142, 0, 99) < upgradeChance) itemLevel = 1;
                }
                item.level(itemLevel);
                item.identify(false);
            }
            return item;
        } finally {
            Random.popGenerator();
        }
    }

    private void generateV11MerchantStock(int cx, int cy, int ox, int oy) {
        if (!isV11MerchantChunk(cx, cy) || v11MerchantGone(cx, cy)) return;

        for (int slot = 0; slot < MERCHANT_STOCK_SLOTS; slot++) {
            int cell = v11MerchantStockCell(cx, cy, ox, oy, slot);
            if (cell < 0) continue;

            long key = v6ObjectKey(cell, 0x6300 + slot);
            if (state().objectState(key) != 0) continue;
            if (heaps.get(cell) != null || traps.get(cell) != null || plants.get(cell) != null) continue;

            Heap heap = new Heap();
            heap.pos = cell;
            heap.seen = mapped[cell] || visited[cell];
            heap.type = Heap.Type.FOR_SALE;
            heap.drop(v11MerchantStockItem(cx, cy, slot));
            heaps.put(cell, heap);
        }
    }

    private void snapshotV11MerchantStock(int cx, int cy, int ox, int oy) {
        if (!isV11MerchantChunk(cx, cy)) return;

        for (int slot = 0; slot < MERCHANT_STOCK_SLOTS; slot++) {
            int cell = v11MerchantStockCell(cx, cy, ox, oy, slot);
            if (cell < 0) continue;

            long key = v6ObjectKey(cell, 0x6300 + slot);
            if (state().objectState(key) != 0) continue;

            Heap heap = heaps.get(cell);
            if (heap == null || heap.type != Heap.Type.FOR_SALE) {
                state().setObjectState(key, 1);
            }
        }
    }

    private InfiniteWorldShopkeeper findV11Shopkeeper(int cx, int cy) {
        for (Mob mob : mobs.toArray(new Mob[0])) {
            if (mob instanceof InfiniteWorldShopkeeper) {
                InfiniteWorldShopkeeper shop = (InfiniteWorldShopkeeper)mob;
                if (shop.shopChunkX() == cx && shop.shopChunkY() == cy) return shop;
            }
        }
        return null;
    }

    private void ensureV11Shopkeeper(boolean addToScene) {
        if (state().generatorVersion < 11) return;

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                if (!isV11MerchantChunk(cx, cy) || v11MerchantGone(cx, cy)) return;
                if (findV11Shopkeeper(cx, cy) != null) return;

                int cell = v11MerchantCell(cx, cy, ox, oy);
                Char occupant = Actor.findChar(cell);
                if (occupant instanceof Mob && occupant.alignment == Char.Alignment.ENEMY) {
                    ((Mob)occupant).despawnFromInfiniteWorld();
                } else if (occupant != null) {
                    return;
                }

                InfiniteWorldShopkeeper shop = new InfiniteWorldShopkeeper(cx, cy);
                shop.pos = cell;

                if (addToScene) GameScene.add(shop);
                else mobs.add(shop);
            }
        });
    }

    private int v7RoomCount(int cx, int cy) {
        return 2 + (Math.floorMod(hash(cx, cy, 20500), 100L) < 38 ? 1 : 0);
    }

    private int v7RoomSlotCell(int cx, int cy, int ox, int oy,
                               int roomIndex, int slot, int salt) {
        int[] s = v7RoomSpec(cx, cy, roomIndex);
        int iw = Math.max(1, s[2] - s[0] - 1);
        int ih = Math.max(1, s[3] - s[1] - 1);
        int area = iw * ih;
        if (area <= 0) return -1;

        int start = (int)Math.floorMod(hash(cx, cy, salt + roomIndex * 31), (long)area);
        int step = Math.max(1, area - 1);

        for (int attempt = 0; attempt < area; attempt++) {
            int idx = Math.floorMod(start + (slot + attempt) * step, area);
            int lx = s[0] + 1 + (idx % iw);
            int ly = s[1] + 1 + (idx / iw);
            int cell = ox + lx + (oy + ly) * width();

            if (cell < 0 || cell >= length()) continue;
            if (solid[cell] || pit[cell]) continue;
            if (map[cell] == Terrain.ALCHEMY || map[cell] == Terrain.MINE_CRYSTAL
                    || map[cell] == Terrain.STATUE || map[cell] == Terrain.STATUE_SP
                    || map[cell] == Terrain.BARRICADE || map[cell] == Terrain.BOOKSHELF) continue;
            return cell;
        }
        return -1;
    }

    private long v7RoomObjectKey(int cell, int roomIndex, int slot, int kind) {
        return v6ObjectKey(cell, 0x5100 + kind * 0x100 + roomIndex * 0x20 + slot);
    }

    private void generateV7ThemedRoomContents() {
        if (state().generatorVersion < 7) return;
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                if (cx == 0 && cy == 0) return;
                if (state().generatorVersion >= 9 && v9AnomalyType(cx, cy) != 0) return;

                int roomCount = v7RoomCount(cx, cy);
                for (int roomIndex = 0; roomIndex < roomCount; roomIndex++) {
                    int[] spec = v7RoomSpec(cx, cy, roomIndex);
                    int theme = spec[8];

                    if (theme == MERCHANT_ROOM_THEME) {
                        generateV11MerchantStock(cx, cy, ox, oy);
                        continue;
                    }

                    // Every locked room has its own visible iron key outside the
                    // door. This guarantees the player sees keys and can always
                    // open every locked room in an endless world.
                    if (spec[9] == Terrain.LOCKED_DOOR) {
                        int keyCell = ox + spec[6] + (oy + spec[7]) * width();
                        placeV7LooseItem(keyCell,
                                v7RoomObjectKey(keyCell, roomIndex, 0, 8),
                                new IronKey(Dungeon.depth));
                    }

                    // Crystal vaults also place a crystal key beside the door, so
                    // the chest is never generated without a corresponding key.
                    if (theme == 6) {
                        int keyCell = ox + spec[6] + (oy + spec[7]) * width();
                        placeV7LooseItem(keyCell,
                                v7RoomObjectKey(keyCell, roomIndex, 1, 8),
                                new CrystalKey(Dungeon.depth));
                    }

                    // V18 gives a genuinely hidden room a visible payoff instead
                    // of only scattering a few loose items on its floor.
                    if (state().generatorVersion >= 18 && spec.length > 10 && spec[10] == 1) {
                        generateV18SecretCacheChest(cx, cy, ox, oy, roomIndex, spec);
                    }

                    switch (theme) {
                        case 0:
                            generateV7PlantRoom(cx, cy, ox, oy, roomIndex);
                            break;
                        case 1:
                            generateV7ScrollRoom(cx, cy, ox, oy, roomIndex);
                            break;
                        case 2:
                            generateV7PotionRoom(cx, cy, ox, oy, roomIndex);
                            break;
                        case 3:
                            generateV7FoodRoom(cx, cy, ox, oy, roomIndex);
                            break;
                        case 4:
                            generateV7KeyRoom(cx, cy, ox, oy, roomIndex);
                            break;
                        case 5:
                            generateV7Armory(cx, cy, ox, oy, roomIndex);
                            break;
                        case 6:
                            generateV7CrystalVault(cx, cy, ox, oy, roomIndex);
                            break;
                        case 7:
                            generateV7Workshop(cx, cy, ox, oy, roomIndex);
                            break;
                        case 8:
                            generateV8GoldGarden(cx, cy, ox, oy, roomIndex);
                            break;
                        default:
                            generateV8Treasury(cx, cy, ox, oy, roomIndex);
                            break;
                    }

                    if (state().generatorVersion >= 8 && spec.length > 10 && spec[10] == 1) {
                        generateV8SecretBonus(cx, cy, ox, oy, roomIndex);
                    }
                }
            }
        });
    }

    private void generateV7PlantRoom(int cx, int cy, int ox, int oy, int roomIndex) {
        int plantCount = state().generatorVersion >= 9
                ? 7 + range(cx, cy, 21101 + roomIndex, 0, 2)
                : (state().generatorVersion >= 8 ? 12 : 7);
        for (int slot = 0; slot < plantCount; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21100);
            if (cell < 0) continue;
            long key = v7RoomObjectKey(cell, roomIndex, slot, 0);
            if (state().objectState(key) != 0 || plants.get(cell) != null
                    || heaps.get(cell) != null || traps.get(cell) != null) continue;

            Plant.Seed seed = v6PlantSeed(cx, cy, 21150 + roomIndex * 16 + slot);
            Plant plant = seed.couch(cell, this);
            plants.put(cell, plant);
        }
    }

    private void generateV7ScrollRoom(int cx, int cy, int ox, int oy, int roomIndex) {
        int count = state().generatorVersion >= 9
                ? 1 + range(cx, cy, 21200 + roomIndex, 0, 1)
                : 4 + range(cx, cy, 21200 + roomIndex, 0, 2);
        for (int slot = 0; slot < count; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21220);
            if (cell < 0) continue;
            placeV7LooseItem(cell,
                    v7RoomObjectKey(cell, roomIndex, slot, 1),
                    v6SafeScroll(cx, cy, 21260 + roomIndex * 16 + slot));
        }
    }

    private void generateV7PotionRoom(int cx, int cy, int ox, int oy, int roomIndex) {
        int count = state().generatorVersion >= 9
                ? 1 + range(cx, cy, 21300 + roomIndex, 0, 1)
                : 4 + range(cx, cy, 21300 + roomIndex, 0, 2);
        for (int slot = 0; slot < count; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21320);
            if (cell < 0) continue;
            placeV7LooseItem(cell,
                    v7RoomObjectKey(cell, roomIndex, slot, 2),
                    v6RandomPotion(cx, cy, 21360 + roomIndex * 16 + slot));
        }
    }

    private void generateV7FoodRoom(int cx, int cy, int ox, int oy, int roomIndex) {
        int count = state().generatorVersion >= 9
                ? 2 + range(cx, cy, 21400 + roomIndex, 0, 1)
                : 5 + range(cx, cy, 21400 + roomIndex, 0, 3);
        for (int slot = 0; slot < count; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21420);
            if (cell < 0) continue;
            placeV7LooseItem(cell,
                    v7RoomObjectKey(cell, roomIndex, slot, 3),
                    v6RandomFood(cx, cy, 21460 + roomIndex * 16 + slot));
        }
    }

    private void generateV7KeyRoom(int cx, int cy, int ox, int oy, int roomIndex) {
        if (state().generatorVersion >= 9) {
            Item[] keys = new Item[]{
                    new IronKey(Dungeon.depth),
                    new CrystalKey(Dungeon.depth)
            };
            for (int slot = 0; slot < keys.length; slot++) {
                int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21520);
                if (cell < 0) continue;
                placeV7LooseItem(cell,
                        v7RoomObjectKey(cell, roomIndex, slot, 4),
                        keys[slot]);
            }
            return;
        }

        Item[] keys = new Item[]{
                new IronKey(Dungeon.depth),
                new IronKey(Dungeon.depth),
                new CrystalKey(Dungeon.depth)
        };
        for (int slot = 0; slot < keys.length; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21520);
            if (cell < 0) continue;
            placeV7LooseItem(cell,
                    v7RoomObjectKey(cell, roomIndex, slot, 4),
                    keys[slot]);
        }

        int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, 4, 21520);
        if (cell >= 0) {
            placeV7LooseItem(cell,
                    v7RoomObjectKey(cell, roomIndex, 4, 4),
                    v6SafeScroll(cx, cy, 21580 + roomIndex));
        }
    }

    private void generateV7Armory(int cx, int cy, int ox, int oy, int roomIndex) {
        int count = state().generatorVersion >= 9
                ? 1 + range(cx, cy, 21600 + roomIndex, 0, 1)
                : 3 + range(cx, cy, 21600 + roomIndex, 0, 2);
        for (int slot = 0; slot < count; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21620);
            if (cell < 0) continue;
            placeV7LooseItem(cell,
                    v7RoomObjectKey(cell, roomIndex, slot, 5),
                    v6EquipmentItem(cx, cy, 21660 + roomIndex * 16 + slot));
        }
    }

    private void generateV7CrystalVault(int cx, int cy, int ox, int oy, int roomIndex) {
        int chestCount = state().generatorVersion >= 9 ? 1 : 2;
        for (int slot = 0; slot < chestCount; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21720);
            if (cell < 0) continue;
            long key = v7RoomObjectKey(cell, roomIndex, slot, 6);
            int state = state().chestState(key);
            if (state >= 2 || heaps.get(cell) != null || traps.get(cell) != null) continue;

            Heap heap = new Heap();
            heap.pos = cell;
            heap.seen = mapped[cell] || visited[cell];
            heap.type = state == 0 ? Heap.Type.CRYSTAL_CHEST : Heap.Type.HEAP;
            heap.drop(v6EquipmentItem(cx, cy, 21760 + roomIndex * 16 + slot));
            heaps.put(cell, heap);
        }
    }

    private void generateV7Workshop(int cx, int cy, int ox, int oy, int roomIndex) {
        Item[] items;
        if (state().generatorVersion >= 9) {
            items = new Item[]{ new Bomb(),
                    state().generatorVersion >= 22
                            ? v22StoneItem(cx, cy, 21821 + roomIndex)
                            : new StoneOfBlink() };
        } else {
            items = new Item[]{ new Bomb(), new Bomb(), new StoneOfBlink(), new Torch() };
        }
        for (int slot = 0; slot < items.length; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21820);
            if (cell < 0) continue;
            placeV7LooseItem(cell,
                    v7RoomObjectKey(cell, roomIndex, slot, 7),
                    items[slot]);
        }
    }

    private void generateV8GoldGarden(int cx, int cy, int ox, int oy, int roomIndex) {
        int count = state().generatorVersion >= 9
                ? 4 + range(cx, cy, 21900 + roomIndex, 0, 3)
                : 8 + range(cx, cy, 21900 + roomIndex, 0, 4);
        for (int slot = 0; slot < count; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21920);
            if (cell < 0) continue;
            int qty = state().generatorVersion >= 9
                    ? 5 + range(cx, cy, 21960 + roomIndex * 16 + slot, 0, 12)
                    : 8 + range(cx, cy, 21960 + roomIndex * 16 + slot, 0, 22);
            placeV7LooseItem(cell,
                    v7RoomObjectKey(cell, roomIndex, slot, 9),
                    new Gold(qty));
        }
    }

    private void generateV8Treasury(int cx, int cy, int ox, int oy, int roomIndex) {
        int count = state().generatorVersion >= 9
                ? 2 + range(cx, cy, 22000 + roomIndex, 0, 2)
                : 5 + range(cx, cy, 22000 + roomIndex, 0, 3);
        for (int slot = 0; slot < count; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 22020);
            if (cell < 0) continue;

            Item item;
            int roll = range(cx, cy, 22060 + roomIndex * 16 + slot, 0, 99);
            if (roll < 28) item = v6EquipmentItem(cx, cy, 22080 + roomIndex * 16 + slot);
            else if (roll < 48) item = v6SafeScroll(cx, cy, 22100 + roomIndex * 16 + slot);
            else if (roll < 68) item = v6RandomPotion(cx, cy, 22120 + roomIndex * 16 + slot);
            else if (roll < 82) item = new Gold(8 + range(cx, cy, 22140 + slot, 0, 18));
            else if (roll < 90) item = new Bomb();
            else item = new CrystalKey(Dungeon.depth);

            placeV7LooseItem(cell,
                    v7RoomObjectKey(cell, roomIndex, slot, 10),
                    item);
        }
    }

    private void generateV18SecretCacheChest(
            int cx, int cy, int ox, int oy, int roomIndex, int[] spec) {
        int cell = v18SecretCacheCell(cx, cy, ox, oy, roomIndex, spec);
        if (cell < 0) return;

        long key = v7RoomObjectKey(cell, roomIndex, 31, 12);
        int chestState = state().chestState(key);
        if (chestState >= 2 || heaps.get(cell) != null || traps.get(cell) != null) return;

        Heap heap = new Heap();
        heap.pos = cell;
        heap.seen = mapped[cell] || visited[cell];
        heap.type = chestState == 0 ? Heap.Type.CHEST : Heap.Type.HEAP;
        heap.drop(v18SecretCacheItem(cx, cy, roomIndex));
        heaps.put(cell, heap);
    }

    private void snapshotV18SecretCacheChest(
            int cx, int cy, int ox, int oy, int roomIndex, int[] spec) {
        int cell = v18SecretCacheCell(cx, cy, ox, oy, roomIndex, spec);
        if (cell < 0) return;

        long key = v7RoomObjectKey(cell, roomIndex, 31, 12);
        Heap heap = heaps.get(cell);
        if (heap == null) {
            state().setChestState(key, 2);
        } else if (heap.type == Heap.Type.CHEST) {
            state().setChestState(key, 0);
        } else {
            state().setChestState(key, 1);
        }
    }

    private int v18SecretCacheCell(
            int cx, int cy, int ox, int oy, int roomIndex, int[] spec) {
        int chosen = -1;
        long best = Long.MAX_VALUE;

        for (int y = spec[1] + 1; y <= spec[3] - 1; y++) {
            for (int x = spec[0] + 1; x <= spec[2] - 1; x++) {
                int cell = ox + x + (oy + y) * width();
                int t = baseWindow != null && cell < baseWindow.length ? baseWindow[cell] : map[cell];
                if (!(t == Terrain.EMPTY || t == Terrain.EMPTY_SP || t == Terrain.EMPTY_DECO
                        || t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.EMBERS)) {
                    continue;
                }
                if (solid[cell] || pit[cell]) continue;

                long score = hash(cx * CHUNK_SIZE + x, cy * CHUNK_SIZE + y,
                        22380 + roomIndex);
                if (score < best) {
                    best = score;
                    chosen = cell;
                }
            }
        }
        return chosen;
    }

    private Item v18SecretCacheItem(int cx, int cy, int roomIndex) {
        int salt = 22480 + roomIndex * 17;
        int roll = range(cx, cy, salt, 0, 99);
        if (roll < 34) return v6EquipmentItem(cx, cy, salt + 1);
        if (roll < 58) return v6SafeScroll(cx, cy, salt + 2);
        if (roll < 77) return v6RandomPotion(cx, cy, salt + 3);
        if (roll < 89) return new Gold(18 + range(cx, cy, salt + 4, 0, 32));
        if (roll < 95) return new Bomb();
        return state().generatorVersion >= 22
                ? v22StoneItem(cx, cy, salt + 5) : new StoneOfBlink();
    }

    private void generateV8SecretBonus(int cx, int cy, int ox, int oy, int roomIndex) {
        // Secret rooms remain richer than ordinary rooms, but V9 deliberately
        // avoids handing out an entire inventory every few chunks.
        int bonus = state().generatorVersion >= 9
                ? 2 + range(cx, cy, 22200 + roomIndex, 0, 1)
                : 3 + range(cx, cy, 22200 + roomIndex, 0, 2);
        for (int slot = 0; slot < bonus; slot++) {
            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot + 12, 22220);
            if (cell < 0) continue;

            Item item;
            int roll = range(cx, cy, 22260 + roomIndex * 16 + slot, 0, 99);
            if (roll < 30) item = new Gold(12 + range(cx, cy, 22280 + slot, 0, 24));
            else if (roll < 55) item = v6EquipmentItem(cx, cy, 22300 + roomIndex * 16 + slot);
            else if (roll < 75) item = v6SafeScroll(cx, cy, 22320 + roomIndex * 16 + slot);
            else item = v6RandomPotion(cx, cy, 22340 + roomIndex * 16 + slot);

            placeV7LooseItem(cell,
                    v7RoomObjectKey(cell, roomIndex, slot, 11),
                    item);
        }
    }

    private void placeV7LooseItem(int cell, long stateKey, Item item) {
        if (cell < 0 || cell >= length() || item == null) return;
        if (state().objectState(stateKey) != 0) return;

        Heap heap = heaps.get(cell);
        if (heap == null) {
            if (traps.get(cell) != null || plants.get(cell) != null) return;
            heap = new Heap();
            heap.pos = cell;
            heap.seen = mapped[cell] || visited[cell];
            heap.type = Heap.Type.HEAP;
            heaps.put(cell, heap);
        } else if (heap.type != Heap.Type.HEAP) {
            return;
        }
        heap.drop(item);
    }

    private void snapshotV7ThemedRoomStates() {
        if (state().generatorVersion < 7) return;
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                if (cx == 0 && cy == 0) return;
                if (state().generatorVersion >= 9 && v9AnomalyType(cx, cy) != 0) return;

                int roomCount = v7RoomCount(cx, cy);
                for (int roomIndex = 0; roomIndex < roomCount; roomIndex++) {
                    int[] spec = v7RoomSpec(cx, cy, roomIndex);
                    int theme = spec[8];

                    if (theme == MERCHANT_ROOM_THEME) {
                        snapshotV11MerchantStock(cx, cy, ox, oy);
                        continue;
                    }

                    if (state().generatorVersion >= 18 && spec.length > 10 && spec[10] == 1) {
                        snapshotV18SecretCacheChest(cx, cy, ox, oy, roomIndex, spec);
                    }

                    if (spec[9] == Terrain.LOCKED_DOOR) {
                        int cell = ox + spec[6] + (oy + spec[7]) * width();
                        snapshotV7LooseCell(cell, v7RoomObjectKey(cell, roomIndex, 0, 8));
                    }
                    if (theme == 6) {
                        int cell = ox + spec[6] + (oy + spec[7]) * width();
                        snapshotV7LooseCell(cell, v7RoomObjectKey(cell, roomIndex, 1, 8));
                    }

                    int count;
                    int kind;
                    int salt;
                    if (theme == 0) {
                        count = state().generatorVersion >= 9
                                ? 7 + range(cx, cy, 21101 + roomIndex, 0, 2)
                                : (state().generatorVersion >= 8 ? 12 : 7);
                        kind = 0; salt = 21100;
                        for (int slot = 0; slot < count; slot++) {
                            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, salt);
                            if (cell < 0) continue;
                            long key = v7RoomObjectKey(cell, roomIndex, slot, kind);
                            if (st.objectState(key) == 0 && plants.get(cell) == null) {
                                st.setObjectState(key, 1);
                            }
                        }
                    } else if (theme == 6) {
                        count = state().generatorVersion >= 9 ? 1 : 2;
                        for (int slot = 0; slot < count; slot++) {
                            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, 21720);
                            if (cell < 0) continue;
                            long key = v7RoomObjectKey(cell, roomIndex, slot, 6);
                            Heap heap = heaps.get(cell);
                            if (heap == null) st.setChestState(key, 2);
                            else if (heap.type == Heap.Type.CRYSTAL_CHEST) st.setChestState(key, 0);
                            else st.setChestState(key, 1);
                        }
                    } else {
                        if (theme == 1) {
                            count = state().generatorVersion >= 9 ? 1 + range(cx, cy, 21200 + roomIndex, 0, 1)
                                    : 4 + range(cx, cy, 21200 + roomIndex, 0, 2);
                            kind = 1; salt = 21220;
                        } else if (theme == 2) {
                            count = state().generatorVersion >= 9 ? 1 + range(cx, cy, 21300 + roomIndex, 0, 1)
                                    : 4 + range(cx, cy, 21300 + roomIndex, 0, 2);
                            kind = 2; salt = 21320;
                        } else if (theme == 3) {
                            count = state().generatorVersion >= 9 ? 2 + range(cx, cy, 21400 + roomIndex, 0, 1)
                                    : 5 + range(cx, cy, 21400 + roomIndex, 0, 3);
                            kind = 3; salt = 21420;
                        } else if (theme == 4) {
                            count = state().generatorVersion >= 9 ? 2 : 5;
                            kind = 4; salt = 21520;
                        } else if (theme == 5) {
                            count = state().generatorVersion >= 9 ? 1 + range(cx, cy, 21600 + roomIndex, 0, 1)
                                    : 3 + range(cx, cy, 21600 + roomIndex, 0, 2);
                            kind = 5; salt = 21620;
                        } else if (theme == 7) {
                            count = state().generatorVersion >= 9 ? 2 : 4;
                            kind = 7; salt = 21820;
                        } else if (theme == 8) {
                            count = state().generatorVersion >= 9 ? 4 + range(cx, cy, 21900 + roomIndex, 0, 3)
                                    : 8 + range(cx, cy, 21900 + roomIndex, 0, 4);
                            kind = 9; salt = 21920;
                        } else {
                            count = state().generatorVersion >= 9 ? 2 + range(cx, cy, 22000 + roomIndex, 0, 2)
                                    : 5 + range(cx, cy, 22000 + roomIndex, 0, 3);
                            kind = 10; salt = 22020;
                        }

                        for (int slot = 0; slot < count; slot++) {
                            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot, salt);
                            if (cell < 0) continue;
                            snapshotV7LooseCell(cell, v7RoomObjectKey(cell, roomIndex, slot, kind));
                        }
                    }

                    if (state().generatorVersion >= 8 && spec.length > 10 && spec[10] == 1) {
                        int bonus = state().generatorVersion >= 9
                                ? 2 + range(cx, cy, 22200 + roomIndex, 0, 1)
                                : 3 + range(cx, cy, 22200 + roomIndex, 0, 2);
                        for (int slot = 0; slot < bonus; slot++) {
                            int cell = v7RoomSlotCell(cx, cy, ox, oy, roomIndex, slot + 12, 22220);
                            if (cell < 0) continue;
                            snapshotV7LooseCell(cell, v7RoomObjectKey(cell, roomIndex, slot, 11));
                        }
                    }
                }
            }
        });
    }

    private void snapshotV7LooseCell(int cell, long stateKey) {
        if (state().objectState(stateKey) != 0) return;
        Heap heap = heaps.get(cell);
        if (heap == null || heap.type != Heap.Type.HEAP) {
            state().setObjectState(stateKey, 1);
        }
    }

    private void generateV18AnomalyContent() {
        if (state().generatorVersion < 18) return;
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int anomaly = v9AnomalyType(cx, cy);
                if (anomaly == 0) return;

                int looseChance = v18AnomalyLooseChance(anomaly);
                if (Math.floorMod(hash(cx, cy, 30500), 100L) < looseChance) {
                    int cell = v18AnomalyObjectCell(cx, cy, ox, oy, 30600);
                    if (cell >= 0) {
                        long key = v6ObjectKey(cell, 0x7100 + anomaly);
                        if (st.objectState(key) == 0
                                && heaps.get(cell) == null
                                && traps.get(cell) == null
                                && plants.get(cell) == null) {
                            Heap heap = new Heap();
                            heap.pos = cell;
                            heap.seen = mapped[cell] || visited[cell];
                            heap.type = Heap.Type.HEAP;
                            heap.drop(v18AnomalySupplyItem(cx, cy, anomaly, 30680));
                            heaps.put(cell, heap);
                        }
                    }
                }

                int containerChance = v18AnomalyContainerChance(anomaly);
                if (Math.floorMod(hash(cx, cy, 30700), 100L) < containerChance) {
                    int cell = v18AnomalyObjectCell(cx, cy, ox, oy, 30740);
                    if (cell >= 0) {
                        long key = v6ObjectKey(cell, 0x7200 + anomaly);
                        int chestState = st.chestState(key);
                        if (chestState < 2 && heaps.get(cell) == null && traps.get(cell) == null) {
                            Heap heap = new Heap();
                            heap.pos = cell;
                            heap.seen = mapped[cell] || visited[cell];
                            Heap.Type original = v18AnomalyContainerType(anomaly);
                            heap.type = chestState == 0 ? original : Heap.Type.HEAP;
                            heap.drop(v18AnomalySupplyItem(cx, cy, anomaly, 30820));
                            heaps.put(cell, heap);
                        }
                    }
                }
            }
        });
    }

    private void snapshotV18AnomalyContent() {
        if (state().generatorVersion < 18) return;
        final InfiniteWorldState st = state();

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int anomaly = v9AnomalyType(cx, cy);
                if (anomaly == 0) return;

                if (Math.floorMod(hash(cx, cy, 30500), 100L) < v18AnomalyLooseChance(anomaly)) {
                    int cell = v18AnomalyObjectCell(cx, cy, ox, oy, 30600);
                    if (cell >= 0) {
                        long key = v6ObjectKey(cell, 0x7100 + anomaly);
                        if (st.objectState(key) == 0) {
                            Heap heap = heaps.get(cell);
                            if (heap == null || heap.type != Heap.Type.HEAP) {
                                st.setObjectState(key, 1);
                            }
                        }
                    }
                }

                if (Math.floorMod(hash(cx, cy, 30700), 100L) < v18AnomalyContainerChance(anomaly)) {
                    int cell = v18AnomalyObjectCell(cx, cy, ox, oy, 30740);
                    if (cell >= 0) {
                        long key = v6ObjectKey(cell, 0x7200 + anomaly);
                        Heap heap = heaps.get(cell);
                        if (heap == null) {
                            st.setChestState(key, 2);
                        } else if (heap.type == v18AnomalyContainerType(anomaly)) {
                            st.setChestState(key, 0);
                        } else {
                            st.setChestState(key, 1);
                        }
                    }
                }
            }
        });
    }

    private int v18AnomalyLooseChance(int anomaly) {
        switch (anomaly) {
            case 1: return 24;
            case 6: return 34;
            case 7: return 24;
            case 8: return 20;
            case 13:return 20;
            case 15:return 38;
            case 16:return 50;
            case 17:return 24;
            case 18:return 36;
            case 19:return 44;
            case 20:return 55;
            default:return 40;
        }
    }

    private int v18AnomalyContainerChance(int anomaly) {
        switch (anomaly) {
            case 1: return 6;
            case 7: return 8;
            case 8: return 8;
            case 13:return 8;
            case 15:return 28;
            case 16:return 24;
            case 17:return 10;
            case 18:return 22;
            case 19:return 16;
            case 20:return 34;
            case 2: case 5: case 6: case 10: case 12:
                return 22;
            default:return 15;
        }
    }

    private Heap.Type v18AnomalyContainerType(int anomaly) {
        switch (anomaly) {
            case 1:
            case 13:
            case 16:
            case 19:
            case 20:
                return Heap.Type.HEAP;
            case 9:
                return Heap.Type.SKELETON;
            case 3:
                // Level 2 is an industrial maintenance corridor; a dungeon tomb
                // was visually out of place even though it functioned correctly.
                return Heap.Type.CHEST;
            default:
                return Heap.Type.CHEST;
        }
    }

    private int v18AnomalyObjectCell(int cx, int cy, int ox, int oy, int saltBase) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int lx = 2 + range(cx, cy, saltBase + attempt * 3, 0, CHUNK_SIZE - 5);
            int ly = 2 + range(cx, cy, saltBase + attempt * 3 + 1, 0, CHUNK_SIZE - 5);
            int cell = ox + lx + (oy + ly) * width();

            int t = baseWindow != null && cell < baseWindow.length ? baseWindow[cell] : map[cell];
            if ((t == Terrain.EMPTY || t == Terrain.EMPTY_SP || t == Terrain.EMPTY_DECO
                    || t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.EMBERS)
                    && !solid[cell] && !pit[cell]) {
                return cell;
            }
        }
        return -1;
    }

    private Item v18AnomalySupplyItem(int cx, int cy, int anomaly, int salt) {
        int roll = range(cx, cy, salt + anomaly * 17, 0, 4);
        switch (anomaly) {
            case 1: // Level 0
                if (roll == 0) return v6RandomFood(cx, cy, salt + 1);
                if (roll == 1) return v6SafeScroll(cx, cy, salt + 2);
                if (roll == 2) return new PotionOfHealing();
                if (roll == 3) return new Torch();
                return state().generatorVersion >= 22
                        ? v22StoneItem(cx, cy, salt + 41) : new StoneOfBlink();

            case 2: // Level 1
                if (roll == 0) return new Bomb();
                if (roll == 1) return new Torch();
                if (roll == 2) return v6RandomFood(cx, cy, salt + 3);
                if (roll == 3) return new Pickaxe();
                return new Gold(5 + range(cx, cy, salt + 4, 0, 18));

            case 3: // Level 2
                if (roll <= 1) return new PotionOfFrost();
                if (roll == 2) return new Bomb();
                if (roll == 3) return new Torch();
                return new ScrollOfTeleportation();

            case 4: // Level 3
                if (roll == 0) return new ScrollOfRecharging();
                if (roll == 1) return state().generatorVersion >= 22
                        ? v22StoneItem(cx, cy, salt + 42) : new StoneOfBlink();
                if (roll == 2) return new Bomb();
                if (roll == 3) return v6RandomPotion(cx, cy, salt + 5);
                return new Gold(8 + range(cx, cy, salt + 6, 0, 20));

            case 5: // Level 4
                if (roll <= 1) return v6SafeScroll(cx, cy, salt + 7);
                if (roll == 2) return v6RandomFood(cx, cy, salt + 8);
                if (roll == 3) return new PotionOfHealing();
                return new Gold(6 + range(cx, cy, salt + 9, 0, 18));

            case 6: // Level 5
                if (roll <= 1) return v6RandomFood(cx, cy, salt + 10);
                if (roll == 2) return new PotionOfHealing();
                if (roll == 3) return new PotionOfInvisibility();
                return v6SafeScroll(cx, cy, salt + 11);

            case 7: // Level 6
                if (roll <= 1) return new Torch();
                if (roll == 2) return v6RandomFood(cx, cy, salt + 12);
                if (roll == 3) return new PotionOfHealing();
                return state().generatorVersion >= 22
                        ? v22StoneItem(cx, cy, salt + 43) : new StoneOfBlink();

            case 8: // Level 7
                if (roll <= 1) return v6RandomFood(cx, cy, salt + 13);
                if (roll == 2) return new PotionOfFrost();
                if (roll == 3) return new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation();
                return new Torch();

            case 9: // Level 8
                if (roll == 0) return new Pickaxe();
                if (roll == 1) return new Torch();
                if (roll == 2) return new PotionOfHealing();
                if (roll == 3) return v6RandomFood(cx, cy, salt + 14);
                return v6SeedItem(cx, cy, salt + 15);

            case 10: // Level 9
                if (roll <= 1) return v6RandomFood(cx, cy, salt + 16);
                if (roll == 2) return new Gold(8 + range(cx, cy, salt + 17, 0, 22));
                if (roll == 3) return new PotionOfHealing();
                return new Bomb();

            case 11: // Level 10
                if (roll <= 1) return v6RandomFood(cx, cy, salt + 18);
                if (roll == 2) return v6SeedItem(cx, cy, salt + 19);
                if (roll == 3) return new PotionOfHealing();
                return new Torch();

            case 12: // Level 11
                if (roll <= 1) return new Gold(10 + range(cx, cy, salt + 20, 0, 30));
                if (roll == 2) return v6SafeScroll(cx, cy, salt + 21);
                if (roll == 3) return new Bomb();
                return v6RandomFood(cx, cy, salt + 22);

            case 13: // Level 37
                if (roll <= 1) return new PotionOfFrost();
                if (roll == 2) return new PotionOfHealing();
                if (roll == 3) return new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation();
                return v6SafeScroll(cx, cy, salt + 23);

            case 14: // Level 94
                if (roll == 0) return v6RandomFood(cx, cy, salt + 24);
                if (roll == 1) return v6RandomPotion(cx, cy, salt + 25);
                if (roll == 2) return v6SafeScroll(cx, cy, salt + 26);
                if (roll == 3) return new Gold(8 + range(cx, cy, salt + 27, 0, 22));
                return state().generatorVersion >= 22
                        ? v22StoneItem(cx, cy, salt + 44) : new StoneOfBlink();

            case 15: // Level 13 apartments
                if (roll <= 1) return v6RandomFood(cx, cy, salt + 28);
                if (roll == 2) return new PotionOfHealing();
                if (roll == 3) return new Gold(8 + range(cx, cy, salt + 29, 0, 20));
                return v6SafeScroll(cx, cy, salt + 30);

            case 16: // Level 18 memories
                if (roll <= 1) return v6RandomFood(cx, cy, salt + 31);
                if (roll == 2) return new PotionOfHealing();
                if (roll == 3) return v6RandomPotion(cx, cy, salt + 32);
                return state().generatorVersion >= 22
                        ? v22StoneItem(cx, cy, salt + 45) : new StoneOfBlink();

            case 17: // Level 34 sewer
                if (roll <= 1) return new Torch();
                if (roll == 2) return new PotionOfHealing();
                if (roll == 3) return new Pickaxe();
                return new Bomb();

            case 18: // Level 40 arcade
                if (roll == 0) return new Gold(10 + range(cx, cy, salt + 33, 0, 28));
                if (roll == 1) return v6RandomFood(cx, cy, salt + 34);
                if (roll == 2) return v6SafeScroll(cx, cy, salt + 35);
                if (roll == 3) return new PotionOfInvisibility();
                return state().generatorVersion >= 22
                        ? v22StoneItem(cx, cy, salt + 46) : new StoneOfBlink();

            case 19: // Level 48 beach
                if (roll <= 1) return v6RandomFood(cx, cy, salt + 36);
                if (roll == 2) return new PotionOfHealing();
                if (roll == 3) return v6SeedItem(cx, cy, salt + 37);
                return new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation();

            case 20: // Level 974
            default:
                if (roll <= 1) return v6RandomFood(cx, cy, salt + 38);
                if (roll == 2) return new PotionOfHealing();
                if (roll == 3) return v6RandomPotion(cx, cy, salt + 39);
                return v6SafeScroll(cx, cy, salt + 40);
        }
    }

    private void generateV9AnomalyNotes() {
        if (state().generatorVersion < 9 || state().generatorVersion >= 15) return;

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int anomaly = v9AnomalyType(cx, cy);
                if (anomaly == 0 || !v9IsAnomalyNoteAnchor(cx, cy)) return;

                String page = v9AnomalyNotePage(anomaly);
                if (state().generatorVersion >= 12
                        && Document.INFINITE_WORLD_NOTES.isPageFound(page)) return;

                int cell = ox + CHUNK_SIZE / 2 + (oy + CHUNK_SIZE / 2) * width();
                long key = v6ObjectKey(cell, 0x6100 + anomaly);
                if (state().objectState(key) != 0) return;

                // Anomaly chunks deliberately skip normal ambient loot, so the
                // central note position is reserved and deterministic.
                if (heaps.get(cell) != null || traps.get(cell) != null
                        || plants.get(cell) != null || solid[cell] || pit[cell]) {
                    return;
                }

                InfiniteWorldNote note = new InfiniteWorldNote();
                note.page(page);

                Heap heap = new Heap();
                heap.pos = cell;
                heap.seen = mapped[cell] || visited[cell];
                heap.type = Heap.Type.HEAP;
                heap.drop(note);
                heaps.put(cell, heap);
            }
        });
    }

    private void snapshotV9AnomalyNotes() {
        if (state().generatorVersion < 9 || state().generatorVersion >= 15) return;

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int anomaly = v9AnomalyType(cx, cy);
                if (anomaly == 0 || !v9IsAnomalyNoteAnchor(cx, cy)) return;

                int cell = ox + CHUNK_SIZE / 2 + (oy + CHUNK_SIZE / 2) * width();
                long key = v6ObjectKey(cell, 0x6100 + anomaly);
                if (state().objectState(key) != 0) return;

                String expectedPage = v9AnomalyNotePage(anomaly);
                Heap heap = heaps.get(cell);
                boolean noteStillPresent = false;
                if (heap != null) {
                    for (Item item : heap.items) {
                        if (item instanceof InfiniteWorldNote
                                && expectedPage.equals(((InfiniteWorldNote) item).page())) {
                            noteStillPresent = true;
                            break;
                        }
                    }
                }
                if (!noteStillPresent) state().setObjectState(key, 1);
            }
        });
    }

    private boolean v9IsAnomalyNoteAnchor(int cx, int cy) {
        return Math.floorMod(cx, 5) == 2 && Math.floorMod(cy, 5) == 2;
    }

    private String v9AnomalyNotePage(int anomaly) {
        if (state().generatorVersion >= 15) {
            switch (anomaly) {
                case 1:  return "Level_0";
                case 2:  return "Level_1";
                case 3:  return "Level_2";
                case 4:  return "Level_3";
                case 5:  return "Level_4";
                case 6:  return "Level_5";
                case 7:  return "Level_6";
                case 8:  return "Level_7";
                case 9:  return "Level_8";
                case 10: return "Level_9";
                case 11: return "Level_10";
                case 12: return "Level_11";
                case 13: return "Level_37";
                case 14: return "Level_94";
                case 15: return "Level_13";
                case 16: return "Level_18";
                case 17: return "Level_34";
                case 18: return "Level_40";
                case 19: return "Level_48";
                default: return "Level_974";
            }
        }

        switch (anomaly) {
            case 1: return "Liminal_Offices";
            case 2: return "Pool_Halls";
            case 3: return "Endless_Hall";
            case 4: return "Yellow_Maze";
            case 5: return "Service_Tunnels";
            default:return "Dark_Storage";
        }
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

                int[] cached = chunkCache.get(chunkKey(cx, cy));
                if (cached != null) {
                    pasteChunk(result, cached, ox, oy);
                } else {
                    if (st.generatorVersion <= 1) generateChunkV1(result, cx, cy, ox, oy);
                    else generateChunkV2(result, cx, cy, ox, oy);
                }

                st.markChunkGenerated(cx, cy);
            }
        }

        // Refresh every active cache entry from the final window so shared-edge
        // carving performed by a newly generated neighbour is retained exactly.
        for (int wy = -HALF_WINDOW; wy <= HALF_WINDOW; wy++) {
            for (int wx = -HALF_WINDOW; wx <= HALF_WINDOW; wx++) {
                int cx = centerChunkX + wx;
                int cy = centerChunkY + wy;
                int ox = (wx + HALF_WINDOW) * CHUNK_SIZE;
                int oy = (wy + HALF_WINDOW) * CHUNK_SIZE;
                chunkCache.put(chunkKey(cx, cy), extractChunk(result, ox, oy));
            }
        }

        return result;
    }

    private static long chunkKey(int cx, int cy) {
        return ((long)cx << 32) ^ (cy & 0xFFFFFFFFL);
    }

    private static int[] extractChunk(int[] source, int ox, int oy) {
        int[] chunk = new int[CHUNK_SIZE * CHUNK_SIZE];
        for (int y = 0; y < CHUNK_SIZE; y++) {
            System.arraycopy(source, ox + (oy + y) * MAP_SIZE,
                    chunk, y * CHUNK_SIZE, CHUNK_SIZE);
        }
        return chunk;
    }

    private static void pasteChunk(int[] target, int[] chunk, int ox, int oy) {
        for (int y = 0; y < CHUNK_SIZE; y++) {
            System.arraycopy(chunk, y * CHUNK_SIZE,
                    target, ox + (oy + y) * MAP_SIZE, CHUNK_SIZE);
        }
    }

    // ------------------------------------------------------------------------
    // Generator v2: deterministic pseudo-random fragments with shared edges.
    // ------------------------------------------------------------------------

    private void generateChunkV2(int[] out, int cx, int cy, int ox, int oy) {
        int northX = edgeHorizontal(cx, cy);
        int southX = edgeHorizontal(cx, cy + 1);
        int westY = edgeVertical(cx, cy);
        int eastY = edgeVertical(cx + 1, cy);

        if (state().generatorVersion >= 9) {
            int anomaly = v9AnomalyType(cx, cy);
            if (anomaly != 0) {
                generateV9AnomalyChunk(out, cx, cy, ox, oy, anomaly,
                        northX, southX, westY, eastY);
                if (state().generatorVersion >= 10) {
                    carveV10InfiniteNetwork(out, cx, cy, ox, oy);
                } else {
                    carveV9InfiniteBackbone(out, cx, cy, ox, oy);
                }
                if (state().generatorVersion >= 16) {
                    applyV16PhysicalScenery(out, cx, cy, ox, oy, anomaly);
                }
                return;
            }
        }

        int spaceProfile;
        if (state().generatorVersion >= 7) {
            int profileRoll = range(cx, cy, 94, 0, 99);
            if (profileRoll < 38) spaceProfile = 0;
            else if (profileRoll < 68) spaceProfile = 1;
            else if (profileRoll < 84) spaceProfile = 2;
            else if (profileRoll < 94) spaceProfile = 4;
            else spaceProfile = 3;
        } else {
            spaceProfile = state().generatorVersion >= 5 ? range(cx, cy, 95, 0, 4) : 2;
        }
        int hubCount;
        if (state().generatorVersion >= 5) {
            switch (spaceProfile) {
                case 0: hubCount = 5 + range(cx, cy, 101, 0, 2); break; // many tiny rooms
                case 1: hubCount = 4 + range(cx, cy, 101, 0, 2); break; // compact
                case 3: hubCount = 2 + range(cx, cy, 101, 0, 2); break; // broad chambers
                case 4: hubCount = 3 + range(cx, cy, 101, 0, 3); break; // mixed
                case 2:
                default:hubCount = 3 + range(cx, cy, 101, 0, 3); break;
            }
        } else {
            hubCount = 3 + range(cx, cy, 101, 0, 3);
        }

        int[] hx = new int[hubCount];
        int[] hy = new int[hubCount];

        for (int i = 0; i < hubCount; i++) {
            hx[i] = 4 + range(cx, cy, 110 + i * 2, 0, CHUNK_SIZE - 9);
            hy[i] = 4 + range(cx, cy, 111 + i * 2, 0, CHUNK_SIZE - 9);
            if (state().generatorVersion >= 5) {
                carveHubV5(out, cx, cy, ox, oy, hx[i], hy[i], i, spaceProfile);
            } else {
                carveHub(out, cx, cy, ox, oy, hx[i], hy[i], i);
            }
        }

        // Connect the hubs into a guaranteed tree first.
        for (int i = 1; i < hubCount; i++) {
            int parent = range(cx, cy, 180 + i, 0, i - 1);
            if (state().generatorVersion >= 5) {
                carveWanderPathV5(out,
                        ox + hx[i], oy + hy[i],
                        ox + hx[parent], oy + hy[parent],
                        cx, cy, 200 + i, spaceProfile);
            } else {
                carveWanderPath(out,
                        ox + hx[i], oy + hy[i],
                        ox + hx[parent], oy + hy[parent],
                        cx, cy, 200 + i);
            }
        }

        // Shared edge gateways are each connected to a pseudo-random hub.
        int nHub = range(cx, cy, 250, 0, hubCount - 1);
        int sHub = range(cx, cy, 251, 0, hubCount - 1);
        int wHub = range(cx, cy, 252, 0, hubCount - 1);
        int eHub = range(cx, cy, 253, 0, hubCount - 1);

        if (state().generatorVersion >= 5) {
            carveWanderPathV5(out, ox + northX, oy, ox + hx[nHub], oy + hy[nHub], cx, cy, 260, spaceProfile);
            carveWanderPathV5(out, ox + southX, oy + CHUNK_SIZE - 1, ox + hx[sHub], oy + hy[sHub], cx, cy, 261, spaceProfile);
            carveWanderPathV5(out, ox, oy + westY, ox + hx[wHub], oy + hy[wHub], cx, cy, 262, spaceProfile);
            carveWanderPathV5(out, ox + CHUNK_SIZE - 1, oy + eastY, ox + hx[eHub], oy + hy[eHub], cx, cy, 263, spaceProfile);
        } else {
            carveWanderPath(out, ox + northX, oy, ox + hx[nHub], oy + hy[nHub], cx, cy, 260);
            carveWanderPath(out, ox + southX, oy + CHUNK_SIZE - 1, ox + hx[sHub], oy + hy[sHub], cx, cy, 261);
            carveWanderPath(out, ox, oy + westY, ox + hx[wHub], oy + hy[wHub], cx, cy, 262);
            carveWanderPath(out, ox + CHUNK_SIZE - 1, oy + eastY, ox + hx[eHub], oy + hy[eHub], cx, cy, 263);
        }

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
                if (state().generatorVersion >= 5) {
                    carveWanderPathV5(out, ox + hx[a], oy + hy[a], ox + hx[b], oy + hy[b],
                            cx, cy, 300 + i, spaceProfile);
                } else {
                    carveWanderPath(out, ox + hx[a], oy + hy[a], ox + hx[b], oy + hy[b],
                            cx, cy, 300 + i);
                }
            }
        }

        // v4 adds much stronger silhouettes while preserving the shared gateways.
        // Old worlds retain their exact v3 generator.
        if (state().generatorVersion >= 4 && (cx != 0 || cy != 0)) {
            int landmarkChance = 48;
            if (state().generatorVersion >= 5) {
                landmarkChance = spaceProfile == 0 ? 18 : (spaceProfile == 1 ? 28 : (spaceProfile == 3 ? 72 : 48));
            }
            if (Math.floorMod(hash(cx, cy, 390), 100L) < landmarkChance) {
                carveStrangeLandmark(out, cx, cy, ox, oy);
            }
        }

        // Large-area styles form coherent districts, but v4 has eight distinct
        // families instead of five.
        int biomeRegionSize = state().generatorVersion >= 6 ? 3 : 4;
        int regionX = Math.floorDiv(cx, biomeRegionSize);
        int regionY = Math.floorDiv(cy, biomeRegionSize);
        int biomeMax = state().generatorVersion >= 4 ? 7 : 4;
        int biome = range(regionX, regionY, 401, 0, biomeMax);
        decorateBiome(out, cx, cy, ox, oy, biome);

        // v5 deliberately uses many more doors so dense chunks feel like actual
        // rooms instead of a single open cave.
        int doorCount = state().generatorVersion >= 5
                ? 2 + range(cx, cy, 430, 0, spaceProfile <= 1 ? 5 : 3)
                : range(cx, cy, 430, 0, 2);
        for (int i = 0; i < doorCount; i++) {
            placeSafeDoor(out, cx, cy, ox, oy, 440 + i);
        }

        if (state().generatorVersion >= 6 && state().generatorVersion < 7) {
            promoteV6LockedDoor(out, cx, cy, ox, oy);
        }

        if (state().generatorVersion >= 5) {
            scatterV5TerrainProps(out, cx, cy, ox, oy, spaceProfile);
        }

        if (hasChest(cx, cy)) {
            int chestX = chestLocalX(cx, cy);
            int chestY = chestLocalY(cx, cy);
            if (state().generatorVersion >= 5) {
                carveHubV5(out, cx, cy, ox, oy, chestX, chestY, 90, Math.min(spaceProfile, 2));
            } else {
                carveHub(out, cx, cy, ox, oy, chestX, chestY, 90);
            }
            int nearest = nearestHub(hx, hy, chestX, chestY);
            if (state().generatorVersion >= 5) {
                carveWanderPathV5(out, ox + chestX, oy + chestY, ox + hx[nearest], oy + hy[nearest],
                        cx, cy, 500, Math.min(spaceProfile, 2));
            } else {
                carveWanderPath(out, ox + chestX, oy + chestY, ox + hx[nearest], oy + hy[nearest],
                        cx, cy, 500);
            }
            out[ox + chestX + (oy + chestY) * MAP_SIZE] = Terrain.EMPTY_DECO;
        }

        if (state().generatorVersion >= 7 && (cx != 0 || cy != 0)) {
            carveV7ThemedRooms(out, cx, cy, ox, oy, hx, hy);
        }

        if (cx == 0 && cy == 0) {
            if (state().generatorVersion >= 3) carveOriginPlazaV3(out, ox, oy);
            else carveOriginPlaza(out, ox, oy);
        }

        if (state().generatorVersion >= 10) {
            carveV10InfiniteNetwork(out, cx, cy, ox, oy);
        } else if (state().generatorVersion >= 9) {
            carveV9InfiniteBackbone(out, cx, cy, ox, oy);
        }

        // V14's merchant access guarantee is deliberately the final terrain pass.
        // Earlier themed-room shells can overwrite corridors carved by room index 0,
        // so the shop must be validated only after every room/road layer is complete.
        if (state().generatorVersion >= 14 && isV11MerchantChunk(cx, cy)) {
            ensureMerchantAccessInChunk(out, cx, cy, ox, oy);
        }

        if (state().generatorVersion >= 16) {
            applyV16PhysicalScenery(out, cx, cy, ox, oy, 0);
        }
    }

    // ------------------------------------------------------------------------
    // Generator v9: guaranteed infinite backbone + rare liminal macro-zones.
    // ------------------------------------------------------------------------

    private int v9AnomalyType(int cx, int cy) {
        if (state().generatorVersion < 9) return 0;

        // V15 treats Backrooms-inspired districts as a normal part of exploration,
        // so they can start much closer to the origin. Older worlds retain their
        // original opening buffer for deterministic save compatibility.
        int startBuffer = state().generatorVersion >= 21 ? 4
                : (state().generatorVersion >= 15 ? 2 : 4);
        if (Math.abs(cx) <= startBuffer && Math.abs(cy) <= startBuffer) return 0;

        final int macro = state().generatorVersion >= 20 ? 3
                : (state().generatorVersion >= 15 ? 4 : 5);
        int mx = Math.floorDiv(cx, macro);
        int my = Math.floorDiv(cy, macro);

        if (state().generatorVersion >= 20) {
            int ax = Math.abs(mx);
            int ay = Math.abs(my);
            int sector;
            if (ay * 2 <= ax) {
                sector = mx >= 0 ? 0 : 4;
            } else if (ax * 2 <= ay) {
                sector = my >= 0 ? 2 : 6;
            } else if (mx >= 0 && my >= 0) {
                sector = 1;
            } else if (mx < 0 && my >= 0) {
                sector = 3;
            } else if (mx < 0) {
                sector = 5;
            } else {
                sector = 7;
            }

            int ring = Math.max(ax, ay);

            if (state().generatorVersion >= 21) {
                // V21 lowers Backrooms density from V20's 20/29 (~69%) to exactly
                // one macro-ring in four (25%). At macro size 3 this gives three
                // ordinary-world macro bands between Backrooms bands.
                //
                // The encounter order is ring-global rather than sector-specific:
                // every NEW Backrooms band advances exactly one slot through a
                // 20-entry permutation. This guarantees that no Backrooms type is
                // repeated until all other 19 themes have appeared once.
                int seedPhase = (int)Math.floorMod(hash(0, 0, 23100), 4L);
                int progression = ring + seedPhase;
                if (Math.floorMod(progression, 4) != 0) return 0;

                int anomalyOrdinal = Math.floorDiv(progression, 4);
                int typeOffset = (int)Math.floorMod(hash(0, 0, 23101), 20L);
                int orderIndex = Math.floorMod(
                        anomalyOrdinal + typeOffset,
                        V21_BACKROOMS_ORDER.length);
                return V21_BACKROOMS_ORDER[orderIndex];
            }

            // V20 deliberately balances discovery instead of leaving theme choice
            // to random collisions. Keep this exact mapping for old V20 saves.
            int seedOffset = (int)Math.floorMod(hash(0, 0, 23002), 29L);
            int cycleIndex = Math.floorMod(ring + seedOffset + sector * 7, 29);
            return V20_BACKROOMS_CYCLE[cycleIndex];
        }

        // V15-V19 keep their exact historic distribution for save compatibility.
        int anomalyChance = state().generatorVersion >= 19 ? 48
                : (state().generatorVersion >= 15 ? 42
                : (state().generatorVersion >= 13 ? 28
                : (state().generatorVersion >= 12 ? 15 : 8)));
        if (Math.floorMod(hash(mx, my, 23000), 100L) >= anomalyChance) return 0;

        int anomalyTypes = state().generatorVersion >= 19 ? 20
                : (state().generatorVersion >= 15 ? 14
                : (state().generatorVersion >= 13 ? 6 : 3));
        return 1 + (int)Math.floorMod(hash(mx, my, 23001), (long)anomalyTypes);
    }

    private void generateV9AnomalyChunk(int[] out, int cx, int cy, int ox, int oy,
                                        int anomaly, int northX, int southX,
                                        int westY, int eastY) {
        carveRect(out, ox, oy, ox + CHUNK_SIZE - 1, oy + CHUNK_SIZE - 1, Terrain.WALL);

        if (state().generatorVersion < 15) {
            // Preserve the exact six legacy layouts used by V9-V14 saves.
            if (anomaly == 1) {
                carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
                for (int x = 5; x < CHUNK_SIZE - 2; x += 6) {
                    for (int y = 1; y < CHUNK_SIZE - 1; y++) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL;
                    int gapA = 2 + range(cx, cy, 23100 + x, 0, CHUNK_SIZE - 5);
                    int gapB = 2 + range(cx, cy, 23120 + x, 0, CHUNK_SIZE - 5);
                    setFloor(out, ox + x, oy + gapA);
                    setFloor(out, ox + x, oy + gapB);
                }
                for (int y = 5; y < CHUNK_SIZE - 2; y += 6) {
                    for (int x = 1; x < CHUNK_SIZE - 1; x++) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL;
                    int gapA = 2 + range(cx, cy, 23140 + y, 0, CHUNK_SIZE - 5);
                    int gapB = 2 + range(cx, cy, 23160 + y, 0, CHUNK_SIZE - 5);
                    setFloor(out, ox + gapA, oy + y);
                    setFloor(out, ox + gapB, oy + y);
                }
            } else if (anomaly == 2) {
                carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY);
                for (int by = 3; by <= 15; by += 12) {
                    for (int bx = 3; bx <= 15; bx += 12) {
                        int w = 5 + range(cx + bx, cy + by, 23200, 0, 2);
                        int h = 5 + range(cx + bx, cy + by, 23201, 0, 2);
                        carveRect(out, ox + bx, oy + by,
                                Math.min(ox + CHUNK_SIZE - 3, ox + bx + w),
                                Math.min(oy + CHUNK_SIZE - 3, oy + by + h),
                                Terrain.WATER);
                    }
                }
                carveRect(out, ox + 11, oy + 1, ox + 12, oy + CHUNK_SIZE - 2, Terrain.EMPTY);
                carveRect(out, ox + 1, oy + 11, ox + CHUNK_SIZE - 2, oy + 12, Terrain.EMPTY);
            } else if (anomaly == 3) {
                carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
                for (int y = 4; y < CHUNK_SIZE - 3; y += 5) {
                    for (int x = 4; x < CHUNK_SIZE - 3; x += 5) {
                        if (((x + y + cx + cy) & 1) == 0) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.STATUE;
                    }
                }
            } else if (anomaly == 4) {
                carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
                for (int x = 4; x < CHUNK_SIZE - 2; x += 4) {
                    for (int y = 1; y < CHUNK_SIZE - 1; y++) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL;
                    int gap = 2 + range(cx, cy, 23400 + x, 0, CHUNK_SIZE - 5);
                    setFloor(out, ox + x, oy + gap);
                    if ((x & 4) == 0) setFloor(out, ox + x, oy + Math.max(2, CHUNK_SIZE - 3 - gap));
                }
                for (int y = 6; y < CHUNK_SIZE - 2; y += 8) {
                    for (int x = 1; x < CHUNK_SIZE - 1; x++) {
                        if ((x % 5) != 2) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL;
                    }
                }
            } else if (anomaly == 5) {
                for (int x = 3; x < CHUNK_SIZE - 2; x += 7) {
                    carveRect(out, ox + x, oy + 1, ox + Math.min(CHUNK_SIZE - 2, x + 1),
                            oy + CHUNK_SIZE - 2, Terrain.EMPTY_DECO);
                }
                for (int y = 4; y < CHUNK_SIZE - 2; y += 7) {
                    carveRect(out, ox + 1, oy + y, ox + CHUNK_SIZE - 2,
                            oy + Math.min(CHUNK_SIZE - 2, y + 1), Terrain.EMPTY_DECO);
                }
                for (int y = 3; y < CHUNK_SIZE - 3; y += 7) {
                    for (int x = 2; x < CHUNK_SIZE - 2; x += 7) {
                        if (Math.floorMod(hash(cx + x, cy + y, 23500), 3L) == 0) {
                            out[ox + x + (oy + y) * MAP_SIZE] = Terrain.EMBERS;
                        }
                    }
                }
            } else {
                carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
                for (int y = 3; y < CHUNK_SIZE - 5; y += 6) {
                    for (int x = 3; x < CHUNK_SIZE - 5; x += 6) {
                        carveRect(out, ox + x, oy + y, ox + x + 2, oy + y + 3, Terrain.WALL);
                        if (Math.floorMod(hash(cx + x, cy + y, 23600), 4L) == 0) {
                            out[ox + x + 1 + (oy + y + 4) * MAP_SIZE] = Terrain.STATUE;
                        }
                    }
                }
            }
        } else {
            // V15: fourteen Backrooms-inspired environments. These are normal
            // Infinite World terrain districts, not separate levels or portals.
            switch (anomaly) {
                case 1: // Level 0 - Threshold: repetitive yellow-office maze.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
                    for (int x = 4; x < CHUNK_SIZE - 2; x += 4) {
                        for (int y = 1; y < CHUNK_SIZE - 1; y++) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL;
                        int gapA = 2 + range(cx, cy, 24000 + x, 0, CHUNK_SIZE - 5);
                        int gapB = 2 + range(cx, cy, 24020 + x, 0, CHUNK_SIZE - 5);
                        setFloor(out, ox + x, oy + gapA);
                        setFloor(out, ox + x, oy + gapB);
                    }
                    for (int y = 5; y < CHUNK_SIZE - 2; y += 5) {
                        int from = 2 + range(cx, cy, 24040 + y, 0, 2);
                        for (int x = from; x < CHUNK_SIZE - 2; x += 7) {
                            carveRect(out, ox + x, oy + y, ox + Math.min(x + 3, CHUNK_SIZE - 2), oy + y, Terrain.WALL);
                        }
                    }
                    break;

                case 2: // Level 1 - Habitable Zone: concrete service/storage halls.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_DECO);
                    for (int y = 3; y < CHUNK_SIZE - 5; y += 7) {
                        for (int x = 3; x < CHUNK_SIZE - 5; x += 7) {
                            carveRect(out, ox + x, oy + y, ox + x + 3, oy + y + 3, Terrain.WALL_DECO);
                            if (((x + y + cx + cy) & 1) == 0) {
                                out[ox + x + 1 + (oy + y + 4) * MAP_SIZE] = Terrain.STATUE_SP;
                            }
                        }
                    }
                    carveRect(out, ox + 10, oy + 1, ox + 13, oy + CHUNK_SIZE - 2, Terrain.EMPTY);
                    break;

                case 3: // Level 2 - Abandoned Utility Halls: tight pipe corridors.
                    for (int x = 2; x < CHUNK_SIZE - 2; x += 5) {
                        carveRect(out, ox + x, oy + 1, ox + Math.min(x + 1, CHUNK_SIZE - 2), oy + CHUNK_SIZE - 2, Terrain.EMPTY_DECO);
                    }
                    for (int y = 3; y < CHUNK_SIZE - 2; y += 6) {
                        carveRect(out, ox + 1, oy + y, ox + CHUNK_SIZE - 2, oy + Math.min(y + 1, CHUNK_SIZE - 2), Terrain.EMPTY_DECO);
                    }
                    for (int y = 2; y < CHUNK_SIZE - 2; y += 6) {
                        int x = 2 + range(cx, cy, 24100 + y, 0, CHUNK_SIZE - 5);
                        out[ox + x + (oy + y) * MAP_SIZE] = Terrain.EMBERS;
                    }
                    break;

                case 4: // Level 3 - Electrical Station: dense machinery grid.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_DECO);
                    for (int x = 3; x < CHUNK_SIZE - 3; x += 5) {
                        for (int y = 2; y < CHUNK_SIZE - 2; y++) {
                            if ((y % 6) < 4) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL_DECO;
                        }
                    }
                    for (int y = 4; y < CHUNK_SIZE - 3; y += 6) {
                        carveRect(out, ox + 1, oy + y, ox + CHUNK_SIZE - 2, oy + y + 1, Terrain.EMPTY);
                        for (int x = 2; x < CHUNK_SIZE - 2; x += 6) {
                            if (Math.floorMod(hash(cx + x, cy + y, 24200), 2L) == 0) {
                                out[ox + x + (oy + y) * MAP_SIZE] = Terrain.EMBERS;
                            }
                        }
                    }
                    break;

                case 5: // Level 4 - Abandoned Office: broad office bays and partitions.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
                    for (int x = 6; x < CHUNK_SIZE - 2; x += 6) {
                        for (int y = 1; y < CHUNK_SIZE - 1; y++) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL;
                        setFloor(out, ox + x, oy + 3 + range(cx, cy, 24300 + x, 0, CHUNK_SIZE - 7));
                        setFloor(out, ox + x, oy + 3 + range(cx, cy, 24320 + x, 0, CHUNK_SIZE - 7));
                    }
                    for (int y = 6; y < CHUNK_SIZE - 2; y += 6) {
                        for (int x = 1; x < CHUNK_SIZE - 1; x++) {
                            if ((x % 6) > 2) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL;
                        }
                    }
                    break;

                case 6: // Level 5 - Terror Hotel: central hall with repeated side rooms.
                    carveRect(out, ox + 10, oy + 1, ox + 13, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
                    carveRect(out, ox + 1, oy + 10, ox + CHUNK_SIZE - 2, oy + 13, Terrain.EMPTY_SP);
                    for (int y = 2; y <= 15; y += 7) {
                        carveRect(out, ox + 2, oy + y, ox + 8, oy + y + 5, Terrain.EMPTY_DECO);
                        carveRect(out, ox + 15, oy + y, ox + 21, oy + y + 5, Terrain.EMPTY_DECO);
                        out[ox + 9 + (oy + y + 2) * MAP_SIZE] = Terrain.DOOR;
                        out[ox + 14 + (oy + y + 2) * MAP_SIZE] = Terrain.DOOR;
                    }
                    out[ox + 12 + (oy + 12) * MAP_SIZE] = Terrain.PEDESTAL;
                    break;

                case 7: // Level 6 - Lights Out: narrow, claustrophobic corridors.
                    for (int y = 2; y < CHUNK_SIZE - 2; y += 4) {
                        int startX = 1 + range(cx, cy, 24400 + y, 0, 3);
                        carveRect(out, ox + startX, oy + y, ox + CHUNK_SIZE - 2, oy + y, Terrain.EMPTY_SP);
                        int connectorX = 2 + range(cx, cy, 24420 + y, 0, CHUNK_SIZE - 5);
                        carveRect(out, ox + connectorX, oy + Math.max(1, y - 3),
                                ox + connectorX, oy + Math.min(CHUNK_SIZE - 2, y + 3), Terrain.EMPTY_SP);
                    }
                    break;

                case 8: // Level 7 - Thalassophobia: open ocean with sparse dry islands.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.WATER);
                    for (int i = 0; i < 5; i++) {
                        int ix = 3 + range(cx, cy, 24500 + i * 2, 0, CHUNK_SIZE - 8);
                        int iy = 3 + range(cx, cy, 24501 + i * 2, 0, CHUNK_SIZE - 8);
                        int r = 1 + range(cx, cy, 24520 + i, 0, 1);
                        carveRect(out, ox + ix - r, oy + iy - r, ox + ix + r, oy + iy + r, Terrain.EMPTY);
                    }
                    carveRect(out, ox + 11, oy + 1, ox + 12, oy + CHUNK_SIZE - 2, Terrain.WATER);
                    break;

                case 9: // Level 8 - Cave Systems: irregular chambers joined by winding tunnels.
                    for (int i = 0; i < 7; i++) {
                        int px = 3 + range(cx, cy, 24600 + i * 3, 0, CHUNK_SIZE - 7);
                        int py = 3 + range(cx, cy, 24601 + i * 3, 0, CHUNK_SIZE - 7);
                        int rw = 1 + range(cx, cy, 24602 + i * 3, 0, 2);
                        int rh = 1 + range(cx, cy, 24603 + i * 3, 0, 2);
                        carveRect(out, ox + px - rw, oy + py - rh, ox + px + rw, oy + py + rh, Terrain.EMPTY_DECO);
                        carveWanderPathV5(out, ox + px, oy + py,
                                ox + CHUNK_SIZE / 2, oy + CHUNK_SIZE / 2,
                                cx, cy, 24640 + i, 1);
                    }
                    for (int i = 0; i < 3; i++) {
                        int wx = 4 + range(cx, cy, 24670 + i, 0, CHUNK_SIZE - 9);
                        int wy = 4 + range(cx, cy, 24680 + i, 0, CHUNK_SIZE - 9);
                        out[ox + wx + (oy + wy) * MAP_SIZE] = Terrain.WATER;
                    }
                    break;

                case 10: // Level 9 - The Suburbs: streets bordered by small repeated houses.
                    carveRect(out, ox + 1, oy + 10, ox + CHUNK_SIZE - 2, oy + 13, Terrain.EMPTY);
                    carveRect(out, ox + 10, oy + 1, ox + 13, oy + CHUNK_SIZE - 2, Terrain.EMPTY);
                    for (int by = 2; by <= 15; by += 13) {
                        for (int bx = 2; bx <= 15; bx += 13) {
                            carveRect(out, ox + bx, oy + by, ox + bx + 6, oy + by + 6, Terrain.WALL);
                            carveRect(out, ox + bx + 1, oy + by + 1, ox + bx + 5, oy + by + 5, Terrain.EMPTY_SP);
                            int doorX = bx < 10 ? bx + 6 : bx;
                            int doorY = by + 3;
                            out[ox + doorX + (oy + doorY) * MAP_SIZE] = Terrain.DOOR;
                        }
                    }
                    break;

                case 11: // Level 10 - Bumper Crop: endless fields split by farm lanes.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.HIGH_GRASS);
                    for (int x = 4; x < CHUNK_SIZE - 2; x += 7) {
                        carveRect(out, ox + x, oy + 1, ox + x + 1, oy + CHUNK_SIZE - 2, Terrain.GRASS);
                    }
                    for (int y = 5; y < CHUNK_SIZE - 2; y += 8) {
                        carveRect(out, ox + 1, oy + y, ox + CHUNK_SIZE - 2, oy + y + 1, Terrain.EMPTY);
                    }
                    for (int i = 0; i < 4; i++) {
                        int tx = 3 + range(cx, cy, 24700 + i, 0, CHUNK_SIZE - 6);
                        int ty = 3 + range(cx, cy, 24710 + i, 0, CHUNK_SIZE - 6);
                        out[ox + tx + (oy + ty) * MAP_SIZE] = Terrain.STATUE_SP;
                    }
                    break;

                case 12: // Level 11 - The City That Never Sleeps: street grid and solid blocks.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.WALL);
                    for (int x = 5; x < CHUNK_SIZE; x += 8) {
                        carveRect(out, ox + x, oy + 1, ox + Math.min(x + 2, CHUNK_SIZE - 2), oy + CHUNK_SIZE - 2, Terrain.EMPTY);
                    }
                    for (int y = 5; y < CHUNK_SIZE; y += 8) {
                        carveRect(out, ox + 1, oy + y, ox + CHUNK_SIZE - 2, oy + Math.min(y + 2, CHUNK_SIZE - 2), Terrain.EMPTY);
                    }
                    for (int y = 2; y < CHUNK_SIZE - 2; y += 8) {
                        for (int x = 2; x < CHUNK_SIZE - 2; x += 8) {
                            if (Math.floorMod(hash(cx + x, cy + y, 24800), 3L) == 0) {
                                out[ox + x + (oy + y) * MAP_SIZE] = Terrain.STATUE;
                            }
                        }
                    }
                    break;

                case 13: // Level 37 - Sublimity / Poolrooms.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY);
                    for (int by = 2; by < CHUNK_SIZE - 4; by += 8) {
                        for (int bx = 2; bx < CHUNK_SIZE - 4; bx += 8) {
                            int w = 4 + range(cx + bx, cy + by, 24900, 0, 3);
                            int h = 4 + range(cx + bx, cy + by, 24901, 0, 3);
                            carveRect(out, ox + bx, oy + by,
                                    Math.min(ox + CHUNK_SIZE - 3, ox + bx + w),
                                    Math.min(oy + CHUNK_SIZE - 3, oy + by + h), Terrain.WATER);
                        }
                    }
                    carveRect(out, ox + 10, oy + 1, ox + 13, oy + CHUNK_SIZE - 2, Terrain.EMPTY);
                    carveRect(out, ox + 1, oy + 10, ox + CHUNK_SIZE - 2, oy + 13, Terrain.EMPTY);
                    break;

                case 14: // Level 94 - Motion: toy-like town blocks surrounded by grass.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.GRASS);
                    carveRect(out, ox + 10, oy + 1, ox + 13, oy + CHUNK_SIZE - 2, Terrain.EMPTY);
                    carveRect(out, ox + 1, oy + 10, ox + CHUNK_SIZE - 2, oy + 13, Terrain.EMPTY);
                    for (int by = 3; by <= 16; by += 13) {
                        for (int bx = 3; bx <= 16; bx += 13) {
                            carveRect(out, ox + bx, oy + by, ox + bx + 4, oy + by + 4, Terrain.WALL);
                            carveRect(out, ox + bx + 1, oy + by + 1, ox + bx + 3, oy + by + 3, Terrain.EMPTY_SP);
                            int dx = bx < 10 ? bx + 4 : bx;
                            out[ox + dx + (oy + by + 2) * MAP_SIZE] = Terrain.DOOR;
                        }
                    }
                    out[ox + 11 + (oy + 11) * MAP_SIZE] = Terrain.WELL;
                    break;

                case 15: // Level 13 - Apartment Complex.
                    carveRect(out, ox + 10, oy + 1, ox + 13, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
                    carveRect(out, ox + 1, oy + 10, ox + CHUNK_SIZE - 2, oy + 13, Terrain.EMPTY_SP);
                    for (int y = 2; y <= 16; y += 7) {
                        carveRect(out, ox + 2, oy + y, ox + 8, oy + Math.min(y + 5, CHUNK_SIZE - 2), Terrain.EMPTY_DECO);
                        carveRect(out, ox + 15, oy + y, ox + 21, oy + Math.min(y + 5, CHUNK_SIZE - 2), Terrain.EMPTY_DECO);
                        out[ox + 9 + (oy + Math.min(y + 2, CHUNK_SIZE - 2)) * MAP_SIZE] = Terrain.DOOR;
                        out[ox + 14 + (oy + Math.min(y + 2, CHUNK_SIZE - 2)) * MAP_SIZE] = Terrain.DOOR;
                    }
                    break;

                case 16: // Level 18 - Memories / daycare-like rooms.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_DECO);
                    for (int x = 6; x < CHUNK_SIZE - 3; x += 7) {
                        for (int y = 3; y < CHUNK_SIZE - 3; y++) {
                            if ((y % 7) < 4) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL_DECO;
                        }
                    }
                    for (int y = 7; y < CHUNK_SIZE - 3; y += 7) {
                        for (int x = 2; x < CHUNK_SIZE - 2; x++) {
                            if ((x % 8) > 3) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL_DECO;
                        }
                    }
                    carveRect(out, ox + 9, oy + 9, ox + 14, oy + 14, Terrain.EMPTY_SP);
                    break;

                case 17: // Level 34 - Sewer System.
                    carveRect(out, ox + 1, oy + 8, ox + CHUNK_SIZE - 2, oy + 15, Terrain.EMPTY_DECO);
                    carveRect(out, ox + 1, oy + 11, ox + CHUNK_SIZE - 2, oy + 12, Terrain.WATER);
                    carveRect(out, ox + 9, oy + 1, ox + 14, oy + CHUNK_SIZE - 2, Terrain.EMPTY_DECO);
                    carveRect(out, ox + 11, oy + 1, ox + 12, oy + CHUNK_SIZE - 2, Terrain.WATER);
                    break;

                case 18: // Level 40 - arcade hall.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_DECO);
                    for (int y = 4; y < CHUNK_SIZE - 3; y += 5) {
                        for (int x = 3; x < CHUNK_SIZE - 3; x += 5) {
                            if (((x + y + cx + cy) & 1) == 0) {
                                out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL_DECO;
                            }
                        }
                    }
                    carveRect(out, ox + 10, oy + 1, ox + 13, oy + CHUNK_SIZE - 2, Terrain.EMPTY);
                    carveRect(out, ox + 1, oy + 10, ox + CHUNK_SIZE - 2, oy + 13, Terrain.EMPTY);
                    break;

                case 19: // Level 48 - Sunset Beach.
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + 7, Terrain.GRASS);
                    carveRect(out, ox + 1, oy + 8, ox + CHUNK_SIZE - 2, oy + 15, Terrain.EMPTY);
                    carveRect(out, ox + 1, oy + 16, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.WATER);
                    // Sparse modern beach houses/hotel bungalows along the forest edge.
                    if (Math.floorMod(hash(cx, cy, 24980), 3L) != 0) {
                        int hx = 3 + range(cx, cy, 24981, 0, 8);
                        carveRect(out, ox + hx, oy + 2, ox + hx + 5, oy + 6, Terrain.WALL);
                        carveRect(out, ox + hx + 1, oy + 3, ox + hx + 4, oy + 5, Terrain.EMPTY_SP);
                        out[ox + hx + 2 + (oy + 6) * MAP_SIZE] = Terrain.DOOR;
                    }
                    break;

                case 20: // Level 974 - Kitty's House, pink domestic rooms.
                default:
                    carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
                    for (int x = 8; x <= 15; x += 7) {
                        for (int y = 1; y < CHUNK_SIZE - 1; y++) {
                            if (y != 6 && y != 17) out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL_DECO;
                        }
                        out[ox + x + (oy + 6) * MAP_SIZE] = Terrain.DOOR;
                        out[ox + x + (oy + 17) * MAP_SIZE] = Terrain.DOOR;
                    }
                    for (int x = 1; x < CHUNK_SIZE - 1; x++) {
                        if (x != 4 && x != 12 && x != 19) out[ox + x + (oy + 11) * MAP_SIZE] = Terrain.WALL_DECO;
                    }
                    out[ox + 4 + (oy + 11) * MAP_SIZE] = Terrain.DOOR;
                    out[ox + 12 + (oy + 11) * MAP_SIZE] = Terrain.DOOR;
                    out[ox + 19 + (oy + 11) * MAP_SIZE] = Terrain.DOOR;
                    break;
            }
        }

        if (state().generatorVersion >= 18) {
            polishV18AnomalyTerrain(out, cx, cy, ox, oy, anomaly);
        }

        setFloor(out, ox + CHUNK_SIZE / 2, oy + CHUNK_SIZE / 2);

        // Preserve the normal shared-edge contract so every Backrooms district
        // remains part of the ordinary walkable Infinite World network.
        int centerX = ox + CHUNK_SIZE / 2;
        int centerY = oy + CHUNK_SIZE / 2;
        carveWanderPathV5(out, ox + northX, oy, centerX, centerY, cx, cy, 23300, 1);
        carveWanderPathV5(out, ox + southX, oy + CHUNK_SIZE - 1, centerX, centerY, cx, cy, 23301, 1);
        carveWanderPathV5(out, ox, oy + westY, centerX, centerY, cx, cy, 23302, 1);
        carveWanderPathV5(out, ox + CHUNK_SIZE - 1, oy + eastY, centerX, centerY, cx, cy, 23303, 1);

        carveGateway(out, ox + northX, oy, true);
        carveGateway(out, ox + southX, oy + CHUNK_SIZE - 1, true);
        carveGateway(out, ox, oy + westY, false);
        carveGateway(out, ox + CHUNK_SIZE - 1, oy + eastY, false);
    }

    private void polishV18AnomalyTerrain(
            int[] out, int cx, int cy, int ox, int oy, int anomaly) {

        // Remove placeholder terrain that read as the wrong environment in V17.
        for (int ly = 1; ly < CHUNK_SIZE - 1; ly++) {
            for (int lx = 1; lx < CHUNK_SIZE - 1; lx++) {
                int cell = ox + lx + (oy + ly) * MAP_SIZE;
                int t = out[cell];

                if (anomaly == 2 && (t == Terrain.STATUE || t == Terrain.STATUE_SP)) {
                    out[cell] = Terrain.EMPTY_DECO;
                } else if (anomaly == 11 && (t == Terrain.STATUE || t == Terrain.STATUE_SP)) {
                    out[cell] = Terrain.GRASS;
                } else if (anomaly == 6 && t == Terrain.PEDESTAL) {
                    out[cell] = Terrain.EMPTY_DECO;
                }
            }
        }

        // V18 saves retain the old logical water cells for compatibility, but the
        // new material layer paints them as darker damp carpet. V19 worlds no
        // longer create literal water inside Level 0.
        if (anomaly == 1 && state().generatorVersion < 19) {
            for (int i = 0; i < 4; i++) {
                int lx = 3 + range(cx, cy, 30200 + i * 2, 0, CHUNK_SIZE - 7);
                int ly = 3 + range(cx, cy, 30201 + i * 2, 0, CHUNK_SIZE - 7);
                int cell = ox + lx + (oy + ly) * MAP_SIZE;
                if (out[cell] == Terrain.EMPTY_SP || out[cell] == Terrain.EMPTY_DECO) {
                    out[cell] = Terrain.WATER;
                }
            }
        }

        // A small deterministic trap budget gives each anomaly a mechanical
        // identity instead of making every special district safer than normal.
        int trapCount;
        switch (anomaly) {
            case 3: case 4: case 7: case 17:
                trapCount = 2; break;
            case 8: case 11: case 13: case 16: case 19: case 20:
                trapCount = 0; break;
            default:
                trapCount = 1; break;
        }

        for (int i = 0; i < trapCount; i++) {
            int cell = v18AnomalyTerrainCell(out, cx, cy, ox, oy, 30300 + i * 41);
            if (cell >= 0) {
                out[cell] = Math.floorMod(hash(cx, cy, 30320 + i), 100L) < 78
                        ? Terrain.SECRET_TRAP : Terrain.TRAP;
            }
        }
    }

    private int v18AnomalyTerrainCell(
            int[] out, int cx, int cy, int ox, int oy, int saltBase) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int lx = 2 + range(cx, cy, saltBase + attempt * 3, 0, CHUNK_SIZE - 5);
            int ly = 2 + range(cx, cy, saltBase + attempt * 3 + 1, 0, CHUNK_SIZE - 5);

            // Preserve the broad shared navigation lanes and chunk gateways.
            if ((lx >= 10 && lx <= 13) || (ly >= 10 && ly <= 13)) continue;

            int cell = ox + lx + (oy + ly) * MAP_SIZE;
            int t = out[cell];
            if (t == Terrain.EMPTY || t == Terrain.EMPTY_SP || t == Terrain.EMPTY_DECO
                    || t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.EMBERS) {
                return cell;
            }
        }
        return -1;
    }

    private void carveV9InfiniteBackbone(int[] out, int cx, int cy, int ox, int oy) {
        // A sparse world-scale grid guarantees that the component containing the
        // origin can extend forever even if ordinary side branches legitimately
        // dead-end. The grid is only every sixth chunk, so it does not force every
        // branch or every chunk to expose an exit.
        boolean verticalSpine = Math.floorMod(cx, 6) == 0;
        boolean horizontalSpine = Math.floorMod(cy, 6) == 0;

        if (verticalSpine) {
            carveRect(out, ox + 11, oy, ox + 12, oy + CHUNK_SIZE - 1, Terrain.EMPTY);
        }
        if (horizontalSpine) {
            carveRect(out, ox, oy + 11, ox + CHUNK_SIZE - 1, oy + 12, Terrain.EMPTY);
        }
    }


    private void carveV10InfiniteNetwork(int[] out, int cx, int cy, int ox, int oy) {
        // Keep the V9 primary grid as the guaranteed skeleton.
        carveV9InfiniteBackbone(out, cx, cy, ox, oy);

        // V10 adds secondary infinite branches. These are not short decorative
        // corridors: an entire selected chunk row becomes one endlessly extending
        // east-west route, and an entire selected chunk column becomes one
        // endlessly extending north-south route.
        //
        // One secondary row/column is selected deterministically in every 18-chunk
        // band, excluding the primary 6-chunk grid. This gives the world multiple
        // independent infinite routes without turning every chunk into a rigid grid.
        if (v10SecondaryHorizontalRow(cy)) {
            int westY = v10HorizontalEdgeY(cx, cy);
            int eastY = v10HorizontalEdgeY(cx + 1, cy);

            carveWanderPathV5(out,
                    ox, oy + westY,
                    ox + CHUNK_SIZE - 1, oy + eastY,
                    cx, cy, 24000, 1);

            carveGateway(out, ox, oy + westY, false);
            carveGateway(out, ox + CHUNK_SIZE - 1, oy + eastY, false);
        }

        if (v10SecondaryVerticalColumn(cx)) {
            int northX = v10VerticalEdgeX(cx, cy);
            int southX = v10VerticalEdgeX(cx, cy + 1);

            carveWanderPathV5(out,
                    ox + northX, oy,
                    ox + southX, oy + CHUNK_SIZE - 1,
                    cx, cy, 24010, 1);

            carveGateway(out, ox + northX, oy, true);
            carveGateway(out, ox + southX, oy + CHUNK_SIZE - 1, true);
        }
    }

    private boolean v10SecondaryHorizontalRow(int cy) {
        int band = Math.floorDiv(cy, 18);
        int local = Math.floorMod(cy, 18);
        int chosen = v10SecondaryOffset(band, true);
        return local == chosen;
    }

    private boolean v10SecondaryVerticalColumn(int cx) {
        int band = Math.floorDiv(cx, 18);
        int local = Math.floorMod(cx, 18);
        int chosen = v10SecondaryOffset(band, false);
        return local == chosen;
    }

    private int v10SecondaryOffset(int band, boolean horizontal) {
        int offset = (int)Math.floorMod(
                hash(horizontal ? band : 24050, horizontal ? 24050 : band,
                        horizontal ? 24051 : 24052),
                18L);

        // Primary spines occupy coordinates divisible by 6. Shift secondary
        // branches away from them so they are visibly separate routes.
        while (offset % 6 == 0) {
            offset = (offset + 1) % 18;
        }
        return offset;
    }

    private int v10HorizontalEdgeY(int edgeX, int branchCy) {
        // The same boundary coordinate is calculated by both neighboring chunks,
        // so the secondary branch cannot break at a chunk seam.
        return 3 + (int)Math.floorMod(hash(edgeX, branchCy, 24060), CHUNK_SIZE - 6L);
    }

    private int v10VerticalEdgeX(int branchCx, int edgeY) {
        return 3 + (int)Math.floorMod(hash(branchCx, edgeY, 24070), CHUNK_SIZE - 6L);
    }

    // ------------------------------------------------------------------------
    // Generator v7: true enclosed themed rooms with explicit doors.
    // ------------------------------------------------------------------------

    private void carveV7ThemedRooms(int[] out, int cx, int cy, int ox, int oy,
                                    int[] hx, int[] hy) {
        int roomCount = 2 + (Math.floorMod(hash(cx, cy, 20500), 100L) < 38 ? 1 : 0);

        boolean[] usedQuadrants = new boolean[4];
        for (int index = 0; index < roomCount; index++) {
            int[] spec = v7RoomSpec(cx, cy, index, usedQuadrants);
            int left = ox + spec[0];
            int top = oy + spec[1];
            int right = ox + spec[2];
            int bottom = oy + spec[3];
            int doorX = ox + spec[4];
            int doorY = oy + spec[5];
            int outsideX = ox + spec[6];
            int outsideY = oy + spec[7];
            int theme = spec[8];
            int doorTerrain = spec[9];

            // Solid shell + clean interior. This is deliberately stronger than the
            // previous open-area generator so the player reads these as real rooms.
            carveRect(out, left, top, right, bottom, Terrain.WALL);
            carveRect(out, left + 1, top + 1, right - 1, bottom - 1, Terrain.EMPTY);
            out[doorX + doorY * MAP_SIZE] = doorTerrain;

            decorateV7RoomTerrain(out, cx, cy, left, top, right, bottom, theme);

            // Create a narrow corridor from the door toward the chunk core.
            setFloor(out, outsideX, outsideY);
            carveWanderPathV5(out, outsideX, outsideY,
                    ox + CHUNK_SIZE / 2, oy + CHUNK_SIZE / 2,
                    cx, cy, 20600 + index, 0);

            // Link the core back into at least one original hub that is outside all
            // room rectangles. This preserves the pre-existing gateway network.
            int hub = -1;
            for (int h = 0; h < hx.length; h++) {
                if (!v7AnyRoomContains(cx, cy, roomCount, hx[h], hy[h])) {
                    hub = h;
                    break;
                }
            }
            if (hub >= 0) {
                carveWanderPathV5(out,
                        ox + CHUNK_SIZE / 2, oy + CHUNK_SIZE / 2,
                        ox + hx[hub], oy + hy[hub],
                        cx, cy, 20700 + index, 0);
            }
        }
    }

    // spec = left,top,right,bottom,doorX,doorY,outsideX,outsideY,theme,doorTerrain
    private int[] v7RoomSpec(int cx, int cy, int index) {
        return v7RoomSpec(cx, cy, index, null);
    }

    private int[] v7RoomSpec(int cx, int cy, int index, boolean[] usedQuadrants) {
        int q = range(cx, cy, 20800 + index, 0, 3);
        if (usedQuadrants != null) {
            for (int n = 0; n < 4 && usedQuadrants[q]; n++) q = (q + 1) & 3;
            usedQuadrants[q] = true;
        } else {
            // Runtime reconstruction uses the same deterministic sequence of unique
            // quadrants without needing the generated terrain.
            boolean[] used = new boolean[4];
            for (int i = 0; i <= index; i++) {
                int qi = range(cx, cy, 20800 + i, 0, 3);
                for (int n = 0; n < 4 && used[qi]; n++) qi = (qi + 1) & 3;
                used[qi] = true;
                if (i == index) q = qi;
            }
        }

        int w;
        int h;
        if (state().generatorVersion >= 8) {
            // Follow the upstream room scale more closely: most rooms are compact
            // 5-8 tile rectangles, with occasional slightly larger treasure rooms.
            w = 5 + range(cx, cy, 20820 + index * 2, 0, 3);
            h = 5 + range(cx, cy, 20821 + index * 2, 0, 3);
        } else {
            w = 6 + range(cx, cy, 20820 + index * 2, 0, 2);
            h = 6 + range(cx, cy, 20821 + index * 2, 0, 2);
        }
        int jitterX = range(cx, cy, 20840 + index * 2, 0, 1);
        int jitterY = range(cx, cy, 20841 + index * 2, 0, 1);

        int left;
        int top;
        if (q == 0 || q == 2) left = 2 + jitterX;
        else left = CHUNK_SIZE - 2 - w - jitterX;

        if (q == 0 || q == 1) top = 2 + jitterY;
        else top = CHUNK_SIZE - 2 - h - jitterY;

        int right = left + w - 1;
        int bottom = top + h - 1;

        boolean horizontalDoor = (hash(cx, cy, 20860 + index) & 1L) == 0;
        int doorX;
        int doorY;
        int outsideX;
        int outsideY;

        // Door always faces roughly toward chunk centre.
        if (q == 0) {
            if (horizontalDoor) {
                doorX = right; doorY = top + h / 2;
                outsideX = doorX + 1; outsideY = doorY;
            } else {
                doorX = left + w / 2; doorY = bottom;
                outsideX = doorX; outsideY = doorY + 1;
            }
        } else if (q == 1) {
            if (horizontalDoor) {
                doorX = left; doorY = top + h / 2;
                outsideX = doorX - 1; outsideY = doorY;
            } else {
                doorX = left + w / 2; doorY = bottom;
                outsideX = doorX; outsideY = doorY + 1;
            }
        } else if (q == 2) {
            if (horizontalDoor) {
                doorX = right; doorY = top + h / 2;
                outsideX = doorX + 1; outsideY = doorY;
            } else {
                doorX = left + w / 2; doorY = top;
                outsideX = doorX; outsideY = doorY - 1;
            }
        } else {
            if (horizontalDoor) {
                doorX = left; doorY = top + h / 2;
                outsideX = doorX - 1; outsideY = doorY;
            } else {
                doorX = left + w / 2; doorY = top;
                outsideX = doorX; outsideY = doorY - 1;
            }
        }

        int theme = state().generatorVersion >= 8
                ? range(cx, cy, 20900 + index, 0, 9)
                : range(cx, cy, 20900 + index, 0, 7);

        if (state().generatorVersion >= 11 && index == 0 && isV11MerchantChunk(cx, cy)) {
            theme = MERCHANT_ROOM_THEME;
        }

        int doorTerrain = Terrain.DOOR;
        int secret = 0;

        if (state().generatorVersion >= 8) {
            // Guarantee that every chunk exposes at least one ordinary unlocked
            // room. Remaining rooms mix regular, locked and hidden entrances.
            if (index > 0) {
                int doorRoll = range(cx, cy, 20920 + index, 0, 99);

                if (state().generatorVersion >= 9) {
                    // One deterministic chunk in each 3x3 macro-cell gets a forced
                    // secret room, so secret content is discoverable in normal play
                    // without making every corridor look suspicious.
                    int mx = Math.floorDiv(cx, 3);
                    int my = Math.floorDiv(cy, 3);
                    int secretLocalX = (int)Math.floorMod(hash(mx, my, 20910), 3L);
                    int secretLocalY = (int)Math.floorMod(hash(mx, my, 20911), 3L);
                    boolean forcedSecret = index == 1
                            && Math.floorMod(cx, 3) == secretLocalX
                            && Math.floorMod(cy, 3) == secretLocalY;

                    if (forcedSecret) {
                        doorTerrain = Terrain.SECRET_DOOR;
                        secret = 1;
                    } else if (doorRoll < 18 && theme != 4) {
                        doorTerrain = Terrain.LOCKED_DOOR;
                    } else if (doorRoll < 38) {
                        doorTerrain = Terrain.SECRET_DOOR;
                        secret = 1;
                    }
                } else {
                    if (doorRoll < 24 && theme != 4) {
                        doorTerrain = Terrain.LOCKED_DOOR;
                    } else if (doorRoll < 50) {
                        doorTerrain = Terrain.SECRET_DOOR;
                        secret = 1;
                    }
                }
            }

            // A key room must never require a key. A crystal vault may be hidden
            // but is not itself iron-locked; its chest still needs a crystal key.
            if (theme == 4 || theme == MERCHANT_ROOM_THEME) {
                doorTerrain = Terrain.DOOR;
                secret = 0;
            } else if (theme == 6 && doorTerrain == Terrain.LOCKED_DOOR) {
                doorTerrain = Terrain.DOOR;
            }
        } else {
            // v7 compatibility.
            if (theme != 0 && theme != 4 && theme != 6) {
                if (Math.floorMod(hash(cx, cy, 20920 + index), 100L) < 58) {
                    doorTerrain = Terrain.LOCKED_DOOR;
                }
            }
        }

        return new int[]{left, top, right, bottom, doorX, doorY,
                outsideX, outsideY, theme, doorTerrain, secret};
    }

    private boolean v7AnyRoomContains(int cx, int cy, int roomCount, int x, int y) {
        for (int i = 0; i < roomCount; i++) {
            int[] s = v7RoomSpec(cx, cy, i);
            if (x >= s[0] && x <= s[2] && y >= s[1] && y <= s[3]) return true;
        }
        return false;
    }

    private void decorateV7RoomTerrain(int[] out, int cx, int cy,
                                       int left, int top, int right, int bottom, int theme) {
        int innerLeft = left + 1;
        int innerTop = top + 1;
        int innerRight = right - 1;
        int innerBottom = bottom - 1;

        if (theme == 0) { // conservatory
            for (int y = innerTop; y <= innerBottom; y++) {
                for (int x = innerLeft; x <= innerRight; x++) {
                    if (((x + y) & 1) == 0) out[x + y * MAP_SIZE] = Terrain.HIGH_GRASS;
                    else if (Math.floorMod(hash(x, y, 20950), 100L) < 35) {
                        out[x + y * MAP_SIZE] = Terrain.GRASS;
                    }
                }
            }
        } else if (theme == 1) { // scroll archive
            for (int x = innerLeft; x <= innerRight; x += 2) {
                out[x + innerTop * MAP_SIZE] = Terrain.BOOKSHELF;
            }
        } else if (theme == 2) { // potion laboratory
            int cxm = (innerLeft + innerRight) / 2;
            int cym = (innerTop + innerBottom) / 2;
            out[cxm + cym * MAP_SIZE] = Terrain.ALCHEMY;
            out[innerLeft + innerTop * MAP_SIZE] = Terrain.EMBERS;
            out[innerRight + innerBottom * MAP_SIZE] = Terrain.EMBERS;
        } else if (theme == 3) { // pantry
            for (int x = innerLeft; x <= innerRight; x++) {
                if ((x & 1) == 0) out[x + innerTop * MAP_SIZE] = Terrain.EMPTY_DECO;
            }
        } else if (theme == 4) { // key room
            int cxm = (innerLeft + innerRight) / 2;
            int cym = (innerTop + innerBottom) / 2;
            out[cxm + cym * MAP_SIZE] = Terrain.PEDESTAL;
        } else if (theme == 5) { // armory
            out[innerLeft + innerTop * MAP_SIZE] = Terrain.STATUE;
            out[innerRight + innerTop * MAP_SIZE] = Terrain.STATUE;
        } else if (theme == 6) { // crystal vault
            int cxm = (innerLeft + innerRight) / 2;
            int cym = (innerTop + innerBottom) / 2;
            out[cxm + cym * MAP_SIZE] = Terrain.EMPTY_DECO;
            if (innerRight - innerLeft >= 3) {
                out[(cxm - 1) + innerTop * MAP_SIZE] = Terrain.MINE_CRYSTAL;
                out[(cxm + 1) + innerTop * MAP_SIZE] = Terrain.MINE_CRYSTAL;
            }
        } else if (theme == 7) { // trap workshop
            for (int y = innerTop; y <= innerBottom; y++) {
                for (int x = innerLeft; x <= innerRight; x++) {
                    int trapChance = state().generatorVersion >= 9 ? 10 : 22;
                    if (Math.floorMod(hash(x, y, 20970), 100L) < trapChance) {
                        out[x + y * MAP_SIZE] = Terrain.SECRET_TRAP;
                    }
                }
            }
        } else if (theme == 8) { // gold garden
            for (int y = innerTop; y <= innerBottom; y++) {
                for (int x = innerLeft; x <= innerRight; x++) {
                    out[x + y * MAP_SIZE] = ((x + y) & 1) == 0 ? Terrain.EMPTY_DECO : Terrain.GRASS;
                }
            }
        } else if (theme == MERCHANT_ROOM_THEME) { // travelling merchant outpost
            for (int y = innerTop; y <= innerBottom; y++) {
                for (int x = innerLeft; x <= innerRight; x++) {
                    out[x + y * MAP_SIZE] = ((x + y) & 1) == 0
                            ? Terrain.EMPTY_SP : Terrain.EMPTY_DECO;
                }
            }
        } else { // mixed treasury / secret cache
            int cxm = (innerLeft + innerRight) / 2;
            int cym = (innerTop + innerBottom) / 2;
            out[cxm + cym * MAP_SIZE] = Terrain.PEDESTAL;
            if (innerRight > innerLeft) {
                out[innerLeft + innerTop * MAP_SIZE] = Terrain.STATUE;
                out[innerRight + innerBottom * MAP_SIZE] = Terrain.STATUE;
            }
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

    private void carveHubV5(int[] out, int cx, int cy, int ox, int oy,
                            int x, int y, int index, int profile) {
        int shape = range(cx, cy, 1520 + index, 0, 5);
        int rx;
        int ry;

        switch (profile) {
            case 0: // tiny cells and closets
                rx = 1 + range(cx, cy, 1560 + index, 0, 1);
                ry = 1 + range(cx, cy, 1600 + index, 0, 1);
                break;
            case 1: // compact rooms
                rx = 1 + range(cx, cy, 1560 + index, 0, 2);
                ry = 1 + range(cx, cy, 1600 + index, 0, 2);
                break;
            case 3: // deliberately oversized halls
                rx = 4 + range(cx, cy, 1560 + index, 0, 2);
                ry = 4 + range(cx, cy, 1600 + index, 0, 2);
                break;
            case 4: // stretched / asymmetric rooms
                if (range(cx, cy, 1640 + index, 0, 1) == 0) {
                    rx = 1 + range(cx, cy, 1560 + index, 0, 2);
                    ry = 4 + range(cx, cy, 1600 + index, 0, 2);
                } else {
                    rx = 4 + range(cx, cy, 1560 + index, 0, 2);
                    ry = 1 + range(cx, cy, 1600 + index, 0, 2);
                }
                break;
            case 2:
            default:
                rx = 2 + range(cx, cy, 1560 + index, 0, 2);
                ry = 2 + range(cx, cy, 1600 + index, 0, 2);
                break;
        }

        switch (shape) {
            case 0:
                carveRect(out, ox + x - rx, oy + y - ry, ox + x + rx, oy + y + ry, Terrain.EMPTY);
                break;
            case 1:
                for (int dy = -ry; dy <= ry; dy++) {
                    for (int dx = -rx; dx <= rx; dx++) {
                        float nx = dx / (float)Math.max(1, rx);
                        float ny = dy / (float)Math.max(1, ry);
                        if (nx * nx + ny * ny <= 1.12f) setFloor(out, ox + x + dx, oy + y + dy);
                    }
                }
                break;
            case 2:
                carveRect(out, ox + x - rx, oy + y, ox + x + rx, oy + y, Terrain.EMPTY);
                carveRect(out, ox + x, oy + y - ry, ox + x, oy + y + ry, Terrain.EMPTY);
                break;
            case 3:
                carveRect(out, ox + x - rx, oy + y - 1, ox + x + rx, oy + y + 1, Terrain.EMPTY);
                carveRect(out, ox + x - 1, oy + y - ry, ox + x + 1, oy + y + ry, Terrain.EMPTY);
                break;
            case 4:
                carveRect(out, ox + x - rx, oy + y - ry, ox + x, oy + y + ry, Terrain.EMPTY);
                carveRect(out, ox + x, oy + y - 1, ox + x + rx, oy + y + ry, Terrain.EMPTY);
                break;
            default:
                for (int dy = -ry; dy <= ry; dy++) {
                    for (int dx = -rx; dx <= rx; dx++) {
                        long n = hash(cx * 31 + x + dx, cy * 31 + y + dy, 1660 + index);
                        if (Math.floorMod(n, 5L) != 0) setFloor(out, ox + x + dx, oy + y + dy);
                    }
                }
                break;
        }
    }

    private void carveWanderPathV5(int[] out, int x1, int y1, int x2, int y2,
                                   int cx, int cy, int salt, int profile) {
        int x = x1;
        int y = y1;
        int guard = 0;
        setFloor(out, x, y);

        while ((x != x2 || y != y2) && guard++ < 180) {
            boolean canX = x != x2;
            boolean canY = y != y2;
            boolean takeX;

            if (!canY) takeX = true;
            else if (!canX) takeX = false;
            else takeX = Math.floorMod(hash(cx * 137 + x, cy * 137 + y, salt + guard), 100L) < 50;

            if (takeX) x += Integer.compare(x2, x);
            else y += Integer.compare(y2, y);
            setFloor(out, x, y);

            int widenChance;
            switch (profile) {
                case 0: widenChance = 7; break;
                case 1: widenChance = 18; break;
                case 3: widenChance = 78; break;
                case 4: widenChance = 46; break;
                default:widenChance = 36; break;
            }

            long h = hash(cx * 101 + x, cy * 101 + y, salt + guard * 5);
            if (Math.floorMod(h, 100L) < widenChance) {
                if ((h & 1L) == 0) {
                    setFloor(out, x + 1, y);
                    setFloor(out, x - 1, y);
                } else {
                    setFloor(out, x, y + 1);
                    setFloor(out, x, y - 1);
                }
            }

            // Broad chunks occasionally explode into a short open pocket mid-corridor.
            if (profile == 3 && Math.floorMod(h, 29L) == 0) {
                carveRect(out, x - 2, y - 2, x + 2, y + 2, Terrain.EMPTY);
            }
        }
    }

    private void scatterV5TerrainProps(int[] out, int cx, int cy, int ox, int oy, int profile) {
        if (state().generatorVersion >= 9 && v9AnomalyType(cx, cy) != 0) return;

        int count = state().generatorVersion >= 9
                ? 8 + range(cx, cy, 17200, 0, profile <= 1 ? 6 : 4)
                : 11 + range(cx, cy, 17200, 0, profile <= 1 ? 13 : 9);

        for (int i = 0; i < count; i++) {
            int x = 2 + range(cx, cy, 17210 + i * 4, 0, CHUNK_SIZE - 5);
            int y = 2 + range(cx, cy, 17211 + i * 4, 0, CHUNK_SIZE - 5);
            int gx = ox + x;
            int gy = oy + y;
            int cell = gx + gy * MAP_SIZE;
            int t = out[cell];

            if (t != Terrain.EMPTY && t != Terrain.EMPTY_DECO && t != Terrain.GRASS
                    && t != Terrain.EMBERS && t != Terrain.HIGH_GRASS) continue;

            int neighbours = floorNeighbours(out, gx, gy);

            if (state().generatorVersion >= 9) {
                // V9 keeps the map visually busy but moves most strong rewards and
                // hazards into recognizable rooms. Ambient secret traps fall from
                // roughly one quarter of prop rolls to about one eighteenth.
                int kind = range(cx, cy, 17212 + i * 4, 0, 17);
                switch (kind) {
                    case 0:
                        if (neighbours >= 4) out[cell] = Terrain.SECRET_TRAP;
                        break;
                    case 1:
                        if (neighbours >= 6) out[cell] = Terrain.ALCHEMY;
                        break;
                    case 2:
                        if (neighbours >= 6) out[cell] = Terrain.BARRICADE;
                        break;
                    case 3:
                        if (neighbours >= 6) out[cell] = Terrain.BOOKSHELF;
                        break;
                    case 4:
                    case 5:
                        out[cell] = Terrain.HIGH_GRASS;
                        break;
                    case 6:
                    case 7:
                        out[cell] = Terrain.EMBERS;
                        break;
                    case 8:
                        if (neighbours >= 6) out[cell] = Terrain.MINE_CRYSTAL;
                        break;
                    case 9:
                        if (neighbours >= 7) out[cell] = Terrain.STATUE;
                        break;
                    case 10:
                    case 11:
                        out[cell] = Terrain.GRASS;
                        break;
                    default:
                        out[cell] = Terrain.EMPTY_DECO;
                        break;
                }
                continue;
            }

            int kind = range(cx, cy, 17212 + i * 4, 0, 11);
            switch (kind) {
                case 0:
                case 1:
                case 2:
                    // Hidden traps are reconstructed as Trap objects after flag-map creation.
                    if (neighbours >= 4) out[cell] = Terrain.SECRET_TRAP;
                    break;
                case 3:
                    if (neighbours >= 5) out[cell] = Terrain.ALCHEMY;
                    break;
                case 4:
                case 5:
                    if (neighbours >= 6) out[cell] = Terrain.BARRICADE;
                    break;
                case 6:
                    if (neighbours >= 6) out[cell] = Terrain.BOOKSHELF;
                    break;
                case 7:
                    out[cell] = Terrain.HIGH_GRASS;
                    break;
                case 8:
                    out[cell] = Terrain.EMBERS;
                    break;
                case 9:
                    if (neighbours >= 6) out[cell] = Terrain.MINE_CRYSTAL;
                    break;
                case 10:
                    if (neighbours >= 6) out[cell] = Terrain.STATUE;
                    break;
                default:
                    out[cell] = Terrain.EMPTY_DECO;
                    break;
            }
        }

        // Add a few interior divider objects to cramped chunks. V9 uses fewer
        // blockers because actual themed rooms already provide enclosure.
        if (profile <= 1) {
            int blockers = state().generatorVersion >= 9
                    ? 1 + range(cx, cy, 17500, 0, 2)
                    : 3 + range(cx, cy, 17500, 0, 5);
            for (int i = 0; i < blockers; i++) {
                int x = 4 + range(cx, cy, 17510 + i * 2, 0, CHUNK_SIZE - 9);
                int y = 4 + range(cx, cy, 17511 + i * 2, 0, CHUNK_SIZE - 9);
                int gx = ox + x;
                int gy = oy + y;
                int cell = gx + gy * MAP_SIZE;
                if (out[cell] == Terrain.EMPTY && floorNeighbours(out, gx, gy) == 8) {
                    out[cell] = (i & 1) == 0 ? Terrain.STATUE : Terrain.BOOKSHELF;
                }
            }
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
                        if (roll < 16) out[cell] = Terrain.HIGH_GRASS;
                        else if (roll < 32) out[cell] = Terrain.GRASS;
                        else if (state().generatorVersion >= 4 && roll < 35) out[cell] = Terrain.EMBERS;
                        break;
                    case 1: // flooded ruins
                        if (state().generatorVersion >= 3) {
                            if (roll < 6 && floorNeighbours(out, ox + x, oy + y) >= 6) out[cell] = Terrain.WATER;
                            else if (roll < 18) out[cell] = Terrain.EMPTY_DECO;
                        } else {
                            if (roll < 15 && floorNeighbours(out, ox + x, oy + y) >= 5) out[cell] = Terrain.WATER;
                            else if (roll < 22) out[cell] = Terrain.EMPTY_DECO;
                        }
                        break;
                    case 2: // impossible archives
                        if (roll < 8 && floorNeighbours(out, ox + x, oy + y) >= 7) out[cell] = Terrain.BOOKSHELF;
                        else if (roll < 21) out[cell] = Terrain.EMPTY_DECO;
                        else if (state().generatorVersion >= 4 && roll < 23
                                && floorNeighbours(out, ox + x, oy + y) >= 7) out[cell] = Terrain.STATUE;
                        break;
                    case 3: // broken plazas
                        if (roll < 6 && floorNeighbours(out, ox + x, oy + y) >= 7) out[cell] = Terrain.STATUE;
                        else if (roll < 25) out[cell] = Terrain.EMPTY_DECO;
                        else if (state().generatorVersion >= 4 && roll < 28) out[cell] = Terrain.PEDESTAL;
                        break;
                    case 4: // mixed old district
                        if (roll < 8) out[cell] = Terrain.GRASS;
                        else if (roll < 13 && floorNeighbours(out, ox + x, oy + y) >= 6) out[cell] = Terrain.WATER;
                        else if (roll < 22) out[cell] = Terrain.EMPTY_DECO;
                        break;
                    case 5: // ash shrines
                        if (roll < 18) out[cell] = Terrain.EMBERS;
                        else if (roll < 22 && floorNeighbours(out, ox + x, oy + y) >= 7) out[cell] = Terrain.STATUE_SP;
                        else if (roll < 27) out[cell] = Terrain.EMPTY_DECO;
                        else if (roll < 29) out[cell] = Terrain.PEDESTAL;
                        break;
                    case 6: // mineral crypt
                        if (roll < 5 && floorNeighbours(out, ox + x, oy + y) >= 7) out[cell] = Terrain.MINE_CRYSTAL;
                        else if (roll < 17) out[cell] = Terrain.EMPTY_DECO;
                        break;
                    case 7: // strange cloister: deliberately contradictory materials
                    default:
                        if (roll < 7 && floorNeighbours(out, ox + x, oy + y) >= 7) out[cell] = Terrain.BOOKSHELF;
                        else if (roll < 12 && floorNeighbours(out, ox + x, oy + y) >= 7) out[cell] = Terrain.STATUE;
                        else if (roll < 19) out[cell] = Terrain.HIGH_GRASS;
                        else if (roll < 23 && floorNeighbours(out, ox + x, oy + y) >= 6) out[cell] = Terrain.WATER;
                        else if (roll < 31) out[cell] = Terrain.EMPTY_DECO;
                        break;
                }
            }
        }

        // Gold-bearing wall seams use the native WALL_DECO mining path. They are
        // biased toward mineral districts but can rarely appear elsewhere.
        if (state().generatorVersion >= 4) {
            int veinChance = biome == 6 ? 13 : (biome == 5 ? 5 : 2);
            for (int y = 2; y < CHUNK_SIZE - 2; y++) {
                for (int x = 2; x < CHUNK_SIZE - 2; x++) {
                    int gx = ox + x;
                    int gy = oy + y;
                    int cell = gx + gy * MAP_SIZE;
                    if (out[cell] != Terrain.WALL) continue;
                    if (!adjacentToFloor(out, gx, gy)) continue;
                    if (Math.floorMod(hash(cx * CHUNK_SIZE + x, cy * CHUNK_SIZE + y, 980), 100L) < veinChance) {
                        out[cell] = Terrain.WALL_DECO;
                    }
                }
            }
        }
    }

    private void carveStrangeLandmark(int[] out, int cx, int cy, int ox, int oy) {
        int type = range(cx, cy, 910, 0, 5);
        // Keep the landmark inside its own chunk so cached chunks stay
        // independent of generation order at streaming-window edges.
        int mx = ox + 8 + range(cx, cy, 911, 0, 7);
        int my = oy + 8 + range(cx, cy, 912, 0, 7);

        switch (type) {
            case 0: { // oversized oval rotunda
                int rx = 5 + range(cx, cy, 913, 0, 2);
                int ry = 3 + range(cx, cy, 914, 0, 3);
                for (int dy = -ry; dy <= ry; dy++) {
                    for (int dx = -rx; dx <= rx; dx++) {
                        float nx = dx / (float)rx;
                        float ny = dy / (float)ry;
                        if (nx * nx + ny * ny <= 1.05f) setFloor(out, mx + dx, my + dy);
                    }
                }
                break;
            }
            case 1: // unnaturally long hall
                carveRect(out, ox + 2, my - 1, ox + CHUNK_SIZE - 3, my + 1, Terrain.EMPTY);
                carveRect(out, mx - 1, my - 4, mx + 1, my + 4, Terrain.EMPTY);
                break;
            case 2: // crooked ring with four deliberate wounds
                for (int dy = -6; dy <= 6; dy++) {
                    for (int dx = -6; dx <= 6; dx++) {
                        int d2 = dx * dx + dy * dy;
                        if (d2 >= 19 && d2 <= 39
                                && !((Math.abs(dx) <= 1 && Math.abs(dy) >= 5)
                                || (Math.abs(dy) <= 1 && Math.abs(dx) >= 5))) {
                            setFloor(out, mx + dx, my + dy);
                        }
                    }
                }
                setFloor(out, mx, my);
                break;
            case 3: // tiny dense cell nested in a larger cross
                carveRect(out, mx - 5, my - 1, mx + 5, my + 1, Terrain.EMPTY);
                carveRect(out, mx - 1, my - 5, mx + 1, my + 5, Terrain.EMPTY);
                carveRect(out, mx - 2, my - 2, mx + 2, my + 2, Terrain.EMPTY);
                break;
            case 4: // asymmetric fan
                for (int dy = -5; dy <= 5; dy++) {
                    int half = Math.max(1, 5 - Math.abs(dy));
                    carveRect(out, mx - 1, my + dy, mx + half, my + dy, Terrain.EMPTY);
                }
                break;
            default: // two overlapping chambers, intentionally off-axis
                carveRect(out, mx - 5, my - 3, mx + 1, my + 3, Terrain.EMPTY);
                carveRect(out, mx - 1, my - 1, mx + 5, my + 5, Terrain.EMPTY);
                break;
        }
    }

    private boolean adjacentToFloor(int[] out, int x, int y) {
        return isFloorLike(out, x + 1, y) || isFloorLike(out, x - 1, y)
                || isFloorLike(out, x, y + 1) || isFloorLike(out, x, y - 1);
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

    private void carveOriginPlazaV3(int[] out, int ox, int oy) {
        int variant = range(0, 0, 12001, 0, 3);

        // All variants keep the exact world spawn (12,12) open and connect naturally
        // to the procedural routes that were already carved through this chunk.
        carveRect(out, ox + 7, oy + 7, ox + 16, oy + 16, Terrain.EMPTY);

        switch (variant) {
            case 0: // small reflecting pools
                for (int x = 8; x <= 15; x++) {
                    if (x != 11 && x != 12) {
                        out[ox + x + (oy + 8) * MAP_SIZE] = Terrain.WATER;
                        out[ox + x + (oy + 15) * MAP_SIZE] = Terrain.WATER;
                    }
                }
                out[ox + 12 + (oy + 12) * MAP_SIZE] = Terrain.EMPTY_DECO;
                break;

            case 1: // overgrown cross court
                for (int x = 8; x <= 15; x++) {
                    if (x < 11 || x > 13) {
                        out[ox + x + (oy + 9) * MAP_SIZE] = Terrain.HIGH_GRASS;
                        out[ox + x + (oy + 14) * MAP_SIZE] = Terrain.GRASS;
                    }
                }
                for (int y = 9; y <= 14; y++) {
                    if (y < 11 || y > 13) {
                        out[ox + 9 + (oy + y) * MAP_SIZE] = Terrain.GRASS;
                        out[ox + 14 + (oy + y) * MAP_SIZE] = Terrain.HIGH_GRASS;
                    }
                }
                break;

            case 2: // broken ceremonial court
                out[ox + 8 + (oy + 8) * MAP_SIZE] = Terrain.STATUE;
                out[ox + 15 + (oy + 8) * MAP_SIZE] = Terrain.STATUE;
                out[ox + 8 + (oy + 15) * MAP_SIZE] = Terrain.STATUE;
                out[ox + 15 + (oy + 15) * MAP_SIZE] = Terrain.STATUE;
                out[ox + 10 + (oy + 11) * MAP_SIZE] = Terrain.EMPTY_DECO;
                out[ox + 13 + (oy + 13) * MAP_SIZE] = Terrain.EMPTY_DECO;
                break;

            default: // ruined archive court, without forcing every world into library visuals
                for (int x = 8; x <= 15; x += 2) {
                    out[ox + x + (oy + 8) * MAP_SIZE] = Terrain.BOOKSHELF;
                }
                for (int x = 9; x <= 15; x += 3) {
                    out[ox + x + (oy + 15) * MAP_SIZE] = Terrain.BOOKSHELF;
                }
                out[ox + 12 + (oy + 12) * MAP_SIZE] = Terrain.EMPTY_DECO;
                break;
        }

        out[ox + 12 + (oy + 12) * MAP_SIZE] = Terrain.EMPTY;
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
        if (state().generatorVersion >= 9) {
            if (v9AnomalyType(cx, cy) != 0) return false;
            if (state().generatorVersion >= 11 && isV11MerchantChunk(cx, cy)) return false;
            int chance = state().generatorVersion >= 18 ? 16 : 12;
            return Math.floorMod(hash(cx, cy, 6001), 100) < chance;
        }
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

    public int randomTeleportDestination(Char ch, boolean preferUnexploredChunk) {
        ArrayList<Integer> primary = new ArrayList<>();
        ArrayList<Integer> secondary = new ArrayList<>();
        ArrayList<Integer> fallback = new ArrayList<>();

        int fromX = ch.pos % width();
        int fromY = ch.pos / width();

        // Keep random teleports inside the central 5x5 chunks. This leaves one
        // loaded chunk of margin around the destination, so teleporting never
        // immediately trips a streaming boundary.
        int min = CHUNK_SIZE;
        int max = MAP_SIZE - CHUNK_SIZE;

        for (int y = min; y < max; y++) {
            for (int x = min; x < max; x++) {
                int cell = x + y * width();
                if ((!passable[cell] && !avoid[cell]) || secret[cell] || Actor.findChar(cell) != null) continue;
                if (Char.hasProp(ch, Char.Property.LARGE) && !openSpace[cell]) continue;

                int dist = Math.abs(x - fromX) + Math.abs(y - fromY);
                if (dist < 8) continue;

                int wx = worldXForLocalCell(cell);
                int wy = worldYForLocalCell(cell);
                int cx = Math.floorDiv(wx, CHUNK_SIZE);
                int cy = Math.floorDiv(wy, CHUNK_SIZE);
                boolean chunkKnown = state().chunkExplored(cx, cy);

                fallback.add(cell);
                if (preferUnexploredChunk) {
                    if (!chunkKnown && dist >= CHUNK_SIZE) primary.add(cell);
                    else if (!visited[cell]) secondary.add(cell);
                } else {
                    if (chunkKnown && visited[cell] && !heroFOV[cell]) primary.add(cell);
                    else if (chunkKnown && visited[cell]) secondary.add(cell);
                }
            }
        }

        if (!primary.isEmpty()) return Random.element(primary);
        if (!secondary.isEmpty()) return Random.element(secondary);
        if (!fallback.isEmpty()) return Random.element(fallback);
        return -1;
    }

    private void addBackroomsMaterialLayers(int cx, int cy, int localX, int localY, int anomaly) {
        InfiniteWorldBackroomsMaterialLayer ground =
                new InfiniteWorldBackroomsMaterialLayer(
                        anomaly,
                        (int)hash(cx, cy, 22560 + anomaly),
                        InfiniteWorldBackroomsMaterialLayer.MODE_GROUND);
        ground.setRect(localX, localY, CHUNK_SIZE, CHUNK_SIZE);
        customTiles.add(ground);

        InfiniteWorldBackroomsMaterialLayer wallsLayer =
                new InfiniteWorldBackroomsMaterialLayer(
                        anomaly,
                        (int)hash(cx, cy, 22580 + anomaly),
                        InfiniteWorldBackroomsMaterialLayer.MODE_WALLS);
        wallsLayer.setRect(localX, localY, CHUNK_SIZE, CHUNK_SIZE);
        customWalls.add(wallsLayer);
    }

    private void rebuildAccentTiles() {
        if (state().generatorVersion < 4 || customTiles == null) return;

        String[] textures = new String[]{
                Assets.Environment.TILES_SEWERS,
                Assets.Environment.TILES_PRISON,
                Assets.Environment.TILES_CAVES,
                Assets.Environment.TILES_CITY,
                Assets.Environment.TILES_HALLS
        };
        int base = visualTheme();

        for (int wy = -HALF_WINDOW; wy <= HALF_WINDOW; wy++) {
            for (int wx = -HALF_WINDOW; wx <= HALF_WINDOW; wx++) {
                int cx = state().centerChunkX + wx;
                int cy = state().centerChunkY + wy;
                int localX = (wx + HALF_WINDOW) * CHUNK_SIZE;
                int localY = (wy + HALF_WINDOW) * CHUNK_SIZE;

                if (state().generatorVersion >= 9) {
                    int anomaly = v9AnomalyType(cx, cy);
                    if (anomaly != 0) {
                        if (state().generatorVersion >= 18) {
                            addBackroomsMaterialLayers(cx, cy, localX, localY, anomaly);
                            continue;
                        }

                        int alt;
                        if (state().generatorVersion >= 15) {
                            switch (anomaly) {
                                case 1:  alt = 4; break; // Level 0: yellow-hall approximation
                                case 2:  alt = 1; break; // Level 1: concrete/storage
                                case 3:  alt = 0; break; // Level 2: utility pipes
                                case 4:  alt = 0; break; // Level 3: electrical/service
                                case 5:  alt = 3; break; // Level 4: offices
                                case 6:  alt = 4; break; // Level 5: hotel
                                case 7:  alt = 2; break; // Level 6: dark concrete
                                case 8:  alt = 0; break; // Level 7: ocean
                                case 9:  alt = 2; break; // Level 8: caves
                                case 10: alt = 3; break; // Level 9: suburbs
                                case 11: alt = 2; break; // Level 10: fields
                                case 12: alt = 3; break; // Level 11: city
                                case 13: alt = 0; break; // Level 37: poolrooms
                                default: alt = 3; break; // Level 94: toy-like town
                            }
                        } else {
                            switch (anomaly) {
                                case 1: alt = 3; break;
                                case 2: alt = 0; break;
                                case 3: alt = 4; break;
                                case 4: alt = 1; break;
                                case 5: alt = 2; break;
                                default:alt = 4; break;
                            }
                        }

                        InfiniteWorldAccentTilemap anomalyFloor =
                                new InfiniteWorldAccentTilemap(
                                        textures[alt],
                                        (int)hash(cx, cy, 22500 + anomaly),
                                        100,
                                        InfiniteWorldAccentTilemap.MODE_FLOOR);
                        anomalyFloor.setRect(localX, localY, CHUNK_SIZE, CHUNK_SIZE);
                        customTiles.add(anomalyFloor);

                        InfiniteWorldAccentTilemap anomalyWalls =
                                new InfiniteWorldAccentTilemap(
                                        textures[alt],
                                        (int)hash(cx, cy, 22510 + anomaly),
                                        100,
                                        InfiniteWorldAccentTilemap.MODE_WALLS);
                        anomalyWalls.setRect(localX, localY, CHUNK_SIZE, CHUNK_SIZE);
                        customWalls.add(anomalyWalls);

                        // Visual-only material correction is safe for existing
                        // V17 worlds; no terrain/collision state is changed.
                        if (state().generatorVersion >= 17) {
                            int primary = -1;
                            int alternate = -1;
                            int altPercent = 10;

                            switch (anomaly) {
                                case 1: // Level 0 — aged beige/yellow office floor
                                case 6: // Level 5 — hotel interior
                                case 14:// Level 94 — staged suburban interior
                                    primary = 109;
                                    alternate = 114;
                                    altPercent = 12;
                                    break;

                                case 2: // Level 1 — concrete service
                                case 3: // Level 2 — utility corridors
                                case 4: // Level 3 — electrical station
                                case 5: // Level 4 — offices
                                case 13:// Level 37 — tiled pool halls
                                    primary = 117;
                                    alternate = 122;
                                    altPercent = 10;
                                    break;

                                default:
                                    break;
                            }

                            if (primary >= 0) {
                                InfiniteWorldUrbanSurfaceLayer urbanSurface =
                                        new InfiniteWorldUrbanSurfaceLayer(
                                                primary,
                                                alternate,
                                                (int)hash(cx, cy, 22540 + anomaly),
                                                altPercent);
                                urbanSurface.setRect(localX, localY, CHUNK_SIZE, CHUNK_SIZE);
                                customTiles.add(urbanSurface);
                            }
                        }
                        continue;
                    }
                }

                if (state().generatorVersion < 6) {
                    // Preserve the sparse v4/v5 mixed-floor look exactly for old saves.
                    if (Math.floorMod(hash(cx, cy, 16001), 100L) >= 66) continue;

                    int alt = range(Math.floorDiv(cx, 2), Math.floorDiv(cy, 2), 16002, 0, 4);
                    if (alt == base) alt = (alt + 1 + range(cx, cy, 16003, 0, 2)) % 5;

                    InfiniteWorldAccentTilemap accent =
                            new InfiniteWorldAccentTilemap(textures[alt], (int)hash(cx, cy, 16004));
                    accent.setRect(localX, localY, CHUNK_SIZE, CHUNK_SIZE);
                    customTiles.add(accent);
                    continue;
                }

                // V6 uses coherent 2x2/3x3 "material districts". Most chunks in a
                // district are strongly reskinned, so walking can genuinely feel
                // like moving between sewers, prison, caves, city and halls.
                int macroSize = 2 + range(Math.floorDiv(cx, 5), Math.floorDiv(cy, 5), 16100, 0, 1);
                int mx = Math.floorDiv(cx, macroSize);
                int my = Math.floorDiv(cy, macroSize);
                int alt = range(mx, my, 16101, 0, 4);

                // Some districts intentionally keep the base world's material as a
                // visual breathing space; all others are forced to a foreign set.
                boolean keepBase = Math.floorMod(hash(mx, my, 16102), 100L) < 16;
                if (!keepBase && alt == base) {
                    alt = (alt + 1 + range(mx, my, 16103, 0, 3)) % 5;
                }
                if (keepBase && alt == base) continue;

                int floorCoverage = 86 + range(cx, cy, 16104, 0, 14);
                int wallCoverage = 82 + range(cx, cy, 16105, 0, 18);

                InfiniteWorldAccentTilemap floorAccent =
                        new InfiniteWorldAccentTilemap(
                                textures[alt],
                                (int)hash(cx, cy, 16106),
                                floorCoverage,
                                InfiniteWorldAccentTilemap.MODE_FLOOR);
                floorAccent.setRect(localX, localY, CHUNK_SIZE, CHUNK_SIZE);
                customTiles.add(floorAccent);

                InfiniteWorldAccentTilemap wallAccent =
                        new InfiniteWorldAccentTilemap(
                                textures[alt],
                                (int)hash(cx, cy, 16107),
                                wallCoverage,
                                InfiniteWorldAccentTilemap.MODE_WALLS);
                wallAccent.setRect(localX, localY, CHUNK_SIZE, CHUNK_SIZE);
                customWalls.add(wallAccent);
            }
        }

        if (state().generatorVersion >= 8) {
            // A second visual pass assigns each enclosed room its own upstream
            // environment material. This is intentionally room-scoped instead of
            // chunk-scoped, so two adjacent rooms can look like different regions.
            for (int wy = -HALF_WINDOW; wy <= HALF_WINDOW; wy++) {
                for (int wx = -HALF_WINDOW; wx <= HALF_WINDOW; wx++) {
                    int cx = state().centerChunkX + wx;
                    int cy = state().centerChunkY + wy;
                    if (cx == 0 && cy == 0) continue;
                    if (state().generatorVersion >= 9 && v9AnomalyType(cx, cy) != 0) continue;

                    int chunkX = (wx + HALF_WINDOW) * CHUNK_SIZE;
                    int chunkY = (wy + HALF_WINDOW) * CHUNK_SIZE;
                    int roomCount = v7RoomCount(cx, cy);

                    for (int roomIndex = 0; roomIndex < roomCount; roomIndex++) {
                        int[] s = v7RoomSpec(cx, cy, roomIndex);
                        int theme = s[8];

                        // Keep still-hidden rooms visually indistinguishable from
                        // surrounding walls. Their bespoke room tileset is only a
                        // reward after discovery, not a giveaway from a distance.
                        if (s.length > 10 && s[10] == 1) {
                            int doorCell = chunkX + s[4] + (chunkY + s[5]) * width();
                            if (map[doorCell] == Terrain.SECRET_DOOR) continue;
                        }

                        int alt = range(cx, cy, 22400 + roomIndex, 0, 4);
                        // Theme contributes to the room material so repeated room
                        // types tend to have a recognizable visual bias.
                        alt = (alt + theme) % 5;

                        int roomX = chunkX + s[0];
                        int roomY = chunkY + s[1];
                        int roomW = s[2] - s[0] + 1;
                        int roomH = s[3] - s[1] + 1;

                        InfiniteWorldAccentTilemap roomFloor =
                                new InfiniteWorldAccentTilemap(
                                        textures[alt],
                                        (int)hash(cx, cy, 22420 + roomIndex),
                                        100,
                                        InfiniteWorldAccentTilemap.MODE_FLOOR);
                        roomFloor.setRect(roomX, roomY, roomW, roomH);
                        customTiles.add(roomFloor);

                        InfiniteWorldAccentTilemap roomWalls =
                                new InfiniteWorldAccentTilemap(
                                        textures[alt],
                                        (int)hash(cx, cy, 22440 + roomIndex),
                                        100,
                                        InfiniteWorldAccentTilemap.MODE_WALLS);
                        roomWalls.setRect(roomX, roomY, roomW, roomH);
                        customWalls.add(roomWalls);
                    }
                }
            }
        }
    }


    @Override
    public void discover(int cell) {
        int oldTerrain = map[cell];
        super.discover(cell);

        if (oldTerrain == Terrain.SECRET_DOOR && map[cell] != oldTerrain) {
            refreshInfiniteWorldAccentOverlays();
        }
    }


    private static class V16DecorationPlacement {
        final int cell;
        final int kind;

        V16DecorationPlacement(int cell, int kind) {
            this.cell = cell;
            this.kind = kind;
        }
    }

    private void applyV16PhysicalScenery(int[] terrain, int cx, int cy, int ox, int oy, int anomaly) {
        ArrayList<V16DecorationPlacement> plan =
                v16DecorationPlan(terrain, cx, cy, ox, oy, anomaly, true);

        for (V16DecorationPlacement placement : plan) {
            if (InfiniteWorldDecorationLayer.isBlockingKind(placement.kind, state().generatorVersion)) {
                terrain[placement.cell] = Terrain.CUSTOM_DECO;
            }
        }
    }

    private void rebuildV15DecorationProps() {
        if (state().generatorVersion < 15 || customTiles == null || baseWindow == null) return;

        final InfiniteWorldDecorationLayer townLayer =
                new InfiniteWorldDecorationLayer(
                        InfiniteWorldDecorationLayer.SOURCE_TOWN, width(), height());
        final InfiniteWorldDecorationLayer dungeonLayer =
                new InfiniteWorldDecorationLayer(
                        InfiniteWorldDecorationLayer.SOURCE_DUNGEON, width(), height());
        final InfiniteWorldDecorationLayer urbanLayer =
                new InfiniteWorldDecorationLayer(
                        InfiniteWorldDecorationLayer.SOURCE_URBAN, width(), height());
        final InfiniteWorldDecorationLayer backroomsLayer =
                new InfiniteWorldDecorationLayer(
                        InfiniteWorldDecorationLayer.SOURCE_BACKROOMS, width(), height());

        forEachActiveChunk(new ChunkVisitor() {
            @Override
            public void visit(int cx, int cy, int ox, int oy) {
                int anomaly = v9AnomalyType(cx, cy);
                ArrayList<V16DecorationPlacement> plan =
                        v16DecorationPlan(baseWindow, cx, cy, ox, oy, anomaly,
                                state().generatorVersion >= 16);

                for (V16DecorationPlacement placement : plan) {
                    int cell = placement.cell;
                    int kind = placement.kind;
                    boolean blocking = InfiniteWorldDecorationLayer.isBlockingKind(kind, state().generatorVersion);

                    if (state().generatorVersion >= 16 && blocking) {
                        // Physical props are represented by invisible CUSTOM_DECO
                        // terrain in the deterministic base map. If a later terrain
                        // override removes that cell, do not resurrect the sprite.
                        if (map[cell] != Terrain.CUSTOM_DECO) continue;
                    } else {
                        if (solid[cell] || pit[cell] || water[cell] || secret[cell]) continue;
                        if (heaps.get(cell) != null || traps.get(cell) != null
                                || plants.get(cell) != null || Actor.findChar(cell) != null) {
                            continue;
                        }
                    }

                    int source = InfiniteWorldDecorationLayer.sourceForKind(kind);
                    InfiniteWorldDecorationLayer layer = source == InfiniteWorldDecorationLayer.SOURCE_DUNGEON
                            ? dungeonLayer
                            : (source == InfiniteWorldDecorationLayer.SOURCE_URBAN
                            ? urbanLayer
                            : (source == InfiniteWorldDecorationLayer.SOURCE_BACKROOMS
                            ? backroomsLayer : townLayer));
                    layer.put(cell, kind, InfiniteWorldLevel.this);
                }
            }
        });

        if (!townLayer.isEmpty()) customTiles.add(townLayer);
        if (!dungeonLayer.isEmpty()) customTiles.add(dungeonLayer);
        if (!urbanLayer.isEmpty()) customTiles.add(urbanLayer);
        if (!backroomsLayer.isEmpty()) customTiles.add(backroomsLayer);
    }

    private ArrayList<V16DecorationPlacement> v16DecorationPlan(
            int[] terrain, int cx, int cy, int ox, int oy, int anomaly, boolean physicalMode) {

        ArrayList<V16DecorationPlacement> result = new ArrayList<>();
        boolean[] claimed = new boolean[CHUNK_SIZE * CHUNK_SIZE];
        boolean[] hardClaimed = new boolean[CHUNK_SIZE * CHUNK_SIZE];

        int count = v16DecorationCount(cx, cy, anomaly);
        int previousCell = -1;
        int previousKind = -1;

        for (int slot = 0; slot < count; slot++) {
            int kind = v16DecorationKind(cx, cy, slot, anomaly);

            // Small clusters read much more like authored scenery than isolated
            // single sprites. Only prop families that make sense in groups repeat.
            if (slot % 3 != 0
                    && previousKind > 0
                    && InfiniteWorldDecorationLayer.isClusterFriendly(previousKind)
                    && range(cx, cy, 29220 + slot, 0, 99) < 58) {
                kind = previousKind;
            }

            boolean hard = physicalMode && InfiniteWorldDecorationLayer.isBlockingKind(kind, state().generatorVersion);
            if (hard && state().generatorVersion < 17
                    && (cx == 0 && cy == 0 || isV11MerchantChunk(cx, cy))) {
                // Legacy V16 kept these locations free of physical props by
                // substituting pass-through clutter. V17 no longer creates
                // pass-through decorations; its connectivity checks must approve
                // every prop as a real blocker instead.
                kind = v16NonBlockingFallbackKind(cx, cy, slot, anomaly);
                hard = false;
            }

            int preferred = slot % 3 == 0 ? -1 : previousCell;
            int cell = v16DecorationCell(terrain, cx, cy, ox, oy, slot, kind,
                    preferred, claimed, hardClaimed, hard);

            if (cell < 0) continue;

            int lx = cell % width() - ox;
            int ly = cell / width() - oy;
            int local = lx + ly * CHUNK_SIZE;
            claimed[local] = true;
            if (hard) hardClaimed[local] = true;

            result.add(new V16DecorationPlacement(cell, kind));
            previousCell = cell;
            previousKind = kind;
        }

        return result;
    }

    private int v16DecorationCount(int cx, int cy, int anomaly) {
        if (cx == 0 && cy == 0) return 3;

        // V17 saves can safely adopt the corrected prop palette because the
        // placement cells/collision remain unchanged; V18 alone changes counts.
        if (state().generatorVersion >= 17) {
            switch (anomaly) {
                case 1:  return 2 + range(cx, cy, 29080, 0, 2); // Level 0: sparse, repetitive
                case 2:  return 7 + range(cx, cy, 29081, 0, 3); // storage/service
                case 3:  return 8 + range(cx, cy, 29082, 0, 3); // hot utility
                case 4:  return 9 + range(cx, cy, 29083, 0, 3); // electrical machinery
                case 5:  return 2 + range(cx, cy, 29084, 0, 2); // Level 4: mostly empty office
                case 6:  return 7 + range(cx, cy, 29085, 0, 3); // hotel
                case 7:  return 2 + range(cx, cy, 29086, 0, 2); // lights out: intentionally bare
                case 8:  return 2 + range(cx, cy, 29087, 0, 1); // ocean: almost empty
                case 9:  return 7 + range(cx, cy, 29088, 0, 3); // caves
                case 10: return 9 + range(cx, cy, 29089, 0, 4); // suburbs
                case 11: return 8 + range(cx, cy, 29090, 0, 3); // fields
                case 12: return 10 + range(cx, cy, 29091, 0, 4); // city
                case 13: return 1 + range(cx, cy, 29092, 0, 2); // Poolrooms
                case 14: return 10 + range(cx, cy, 29093, 0, 4); // Motion
                case 15: return 7 + range(cx, cy, 29095, 0, 3); // apartments
                case 16: return 5 + range(cx, cy, 29096, 0, 3); // memories
                case 17: return 4 + range(cx, cy, 29097, 0, 2); // sewer
                case 18: return 8 + range(cx, cy, 29098, 0, 4); // arcade
                case 19: return 4 + range(cx, cy, 29099, 0, 3); // beach
                case 20: return 6 + range(cx, cy, 29112, 0, 3); // Kitty house
                default: return 3 + range(cx, cy, 29094, 0, 3);
            }
        }

        switch (anomaly) {
            case 1:  return 4 + range(cx, cy, 29100, 0, 2); // Level 0: intentionally sparse
            case 2:
            case 3:
            case 4:  return 6 + range(cx, cy, 29101, 0, 3); // industrial
            case 5:
            case 6:  return 6 + range(cx, cy, 29102, 0, 3); // office/hotel
            case 7:  return 4 + range(cx, cy, 29103, 0, 2); // lights out
            case 8:  return 3 + range(cx, cy, 29104, 0, 2); // ocean
            case 9:  return 7 + range(cx, cy, 29105, 0, 3); // caves
            case 10: return 8 + range(cx, cy, 29106, 0, 3); // suburbs
            case 11: return 9 + range(cx, cy, 29107, 0, 4); // fields
            case 12: return 8 + range(cx, cy, 29108, 0, 3); // city
            case 13: return 4 + range(cx, cy, 29109, 0, 2); // poolrooms
            case 14: return 9 + range(cx, cy, 29110, 0, 4); // Motion/town
            default: return 2 + range(cx, cy, 29111, 0, 2); // ordinary world
        }
    }

    private int v16DecorationCell(
            int[] terrain, int cx, int cy, int ox, int oy, int slot, int kind,
            int preferredCell, boolean[] claimed, boolean[] hardClaimed, boolean hard) {

        // Cluster members first search a compact 7x7 area around the previous prop.
        if (preferredCell >= 0) {
            int px = preferredCell % width() - ox;
            int py = preferredCell / width() - oy;
            int start = range(cx, cy, 29300 + slot * 7, 0, 48);

            for (int attempt = 0; attempt < 49; attempt++) {
                int idx = Math.floorMod(start + attempt * 17, 49);
                int lx = px - 3 + idx % 7;
                int ly = py - 3 + idx / 7;
                if (v16DecorationCandidateValid(terrain, cx, cy, ox, oy,
                        lx, ly, kind, claimed, hardClaimed, hard)) {
                    return ox + lx + (oy + ly) * width();
                }
            }
        }

        final int inner = CHUNK_SIZE - 4; // local 2..21
        final int area = inner * inner;
        int start = (int)Math.floorMod(hash(cx, cy, 29400 + slot * 11), (long)area);

        // 137 is coprime with 400, so every interior cell is considered once.
        for (int attempt = 0; attempt < area; attempt++) {
            int idx = Math.floorMod(start + attempt * 137, area);
            int lx = 2 + idx % inner;
            int ly = 2 + idx / inner;

            if (v16DecorationCandidateValid(terrain, cx, cy, ox, oy,
                    lx, ly, kind, claimed, hardClaimed, hard)) {
                return ox + lx + (oy + ly) * width();
            }
        }

        return -1;
    }

    private boolean v16DecorationCandidateValid(
            int[] terrain, int cx, int cy, int ox, int oy, int lx, int ly, int kind,
            boolean[] claimed, boolean[] hardClaimed, boolean hard) {

        if (lx < 2 || ly < 2 || lx >= CHUNK_SIZE - 2 || ly >= CHUNK_SIZE - 2) return false;

        int local = lx + ly * CHUNK_SIZE;
        if (claimed[local]) return false;

        int cell = ox + lx + (oy + ly) * width();
        if (cell < 0 || cell >= terrain.length) return false;

        int t = terrain[cell];
        if (!v16DecorationBaseTerrain(t)) return false;

        // Preserve the broad cross-lanes used by the global road network and many
        // Backrooms layouts. Visual clutter can be dense without obscuring navigation.
        if ((lx >= 10 && lx <= 13) || (ly >= 10 && ly <= 13)) return false;

        if (state().generatorVersion >= 7
                && v8LocalCellReservedForRoomAccess(cx, cy, lx, ly)) {
            return false;
        }

        if (hasChest(cx, cy)
                && Math.abs(lx - chestLocalX(cx, cy)) <= 1
                && Math.abs(ly - chestLocalY(cx, cy)) <= 1) {
            return false;
        }

        if (v16NearDoorOrCriticalTerrain(terrain, cell)) return false;

        if (hard && !v16BlockingDecorationSafe(terrain, ox, oy, lx, ly, hardClaimed)) {
            return false;
        }

        return true;
    }

    private boolean v16DecorationBaseTerrain(int terrain) {
        return terrain == Terrain.EMPTY
                || terrain == Terrain.EMPTY_SP
                || terrain == Terrain.EMPTY_DECO
                || terrain == Terrain.GRASS
                || terrain == Terrain.HIGH_GRASS
                || terrain == Terrain.EMBERS
                || terrain == Terrain.CUSTOM_DECO;
    }

    private boolean v16NearDoorOrCriticalTerrain(int[] terrain, int cell) {
        for (int off : PathFinder.NEIGHBOURS9) {
            int pos = cell + off;
            if (pos < 0 || pos >= terrain.length) continue;

            int t = terrain[pos];
            if (t == Terrain.DOOR || t == Terrain.OPEN_DOOR
                    || t == Terrain.LOCKED_DOOR || t == Terrain.HERO_LKD_DR
                    || t == Terrain.CRYSTAL_DOOR || t == Terrain.SECRET_DOOR
                    || t == Terrain.ENTRANCE || t == Terrain.ENTRANCE_SP
                    || t == Terrain.EXIT || t == Terrain.LOCKED_EXIT
                    || t == Terrain.UNLOCKED_EXIT || t == Terrain.PEDESTAL
                    || t == Terrain.WELL || t == Terrain.ALCHEMY) {
                return true;
            }
        }
        return false;
    }

    private boolean v16BlockingDecorationSafe(
            int[] terrain, int ox, int oy, int lx, int ly, boolean[] hardClaimed) {

        // If removing this floor tile would separate its immediate walkable
        // neighbours, it is a choke point and must remain clear.
        final int radius = 3;
        int minX = Math.max(1, lx - radius);
        int maxX = Math.min(CHUNK_SIZE - 2, lx + radius);
        int minY = Math.max(1, ly - radius);
        int maxY = Math.min(CHUNK_SIZE - 2, ly + radius);

        ArrayList<Integer> neighbours = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = lx + dx;
                int ny = ly + dy;
                if (nx < 1 || ny < 1 || nx >= CHUNK_SIZE - 1 || ny >= CHUNK_SIZE - 1) continue;
                if (v16PlanningPassable(terrain, ox, oy, nx, ny, hardClaimed, lx, ly)) {
                    neighbours.add(nx + ny * CHUNK_SIZE);
                }
            }
        }

        if (neighbours.size() <= 1) return true;

        int localW = maxX - minX + 1;
        int localH = maxY - minY + 1;
        boolean[] seen = new boolean[localW * localH];
        int[] queue = new int[localW * localH];
        int head = 0;
        int tail = 0;

        int first = neighbours.get(0);
        int firstX = first % CHUNK_SIZE;
        int firstY = first / CHUNK_SIZE;
        seen[(firstX - minX) + (firstY - minY) * localW] = true;
        queue[tail++] = first;

        while (head < tail) {
            int cur = queue[head++];
            int x = cur % CHUNK_SIZE;
            int y = cur / CHUNK_SIZE;

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx < minX || nx > maxX || ny < minY || ny > maxY) continue;
                    if (!v16PlanningPassable(terrain, ox, oy, nx, ny, hardClaimed, lx, ly)) continue;

                    int seenIndex = (nx - minX) + (ny - minY) * localW;
                    if (seen[seenIndex]) continue;
                    seen[seenIndex] = true;
                    queue[tail++] = nx + ny * CHUNK_SIZE;
                }
            }
        }

        for (int neighbour : neighbours) {
            int nx = neighbour % CHUNK_SIZE;
            int ny = neighbour / CHUNK_SIZE;
            if (!seen[(nx - minX) + (ny - minY) * localW]) return false;
        }

        return true;
    }

    private boolean v16PlanningPassable(
            int[] terrain, int ox, int oy, int lx, int ly,
            boolean[] hardClaimed, int blockedX, int blockedY) {

        if (lx == blockedX && ly == blockedY) return false;
        int local = lx + ly * CHUNK_SIZE;
        if (hardClaimed[local]) return false;

        int cell = ox + lx + (oy + ly) * width();
        int t = terrain[cell];

        // CUSTOM_DECO is treated as its pre-decoration floor while replaying the
        // deterministic plan from baseWindow.
        return t == Terrain.CUSTOM_DECO || (Terrain.flags[t] & Terrain.PASSABLE) != 0;
    }

    private int v16NonBlockingFallbackKind(int cx, int cy, int slot, int anomaly) {
        int roll = range(cx, cy, 29500 + slot, 0, 99);
        if (anomaly == 9) return roll < 70
                ? InfiniteWorldDecorationLayer.MUSHROOMS
                : InfiniteWorldDecorationLayer.RUBBLE;
        if (anomaly == 11 || anomaly == 14) return roll < 65
                ? InfiniteWorldDecorationLayer.FERN
                : InfiniteWorldDecorationLayer.MUSHROOMS;
        return roll < 50
                ? InfiniteWorldDecorationLayer.RUBBLE
                : InfiniteWorldDecorationLayer.MUSHROOMS;
    }

    private int v16DecorationKind(int cx, int cy, int slot, int anomaly) {
        int roll = range(cx, cy, 29600 + slot * 13, 0, 99);

        if (state().generatorVersion >= 18) {
            switch (anomaly) {
                case 1: // Level 0: sparse office objects only.
                    if (roll < 28) return InfiniteWorldDecorationLayer.FLOOR_LAMP;
                    if (roll < 54) return InfiniteWorldDecorationLayer.LOW_DESK;
                    if (roll < 78) return InfiniteWorldDecorationLayer.BACKROOMS_SHELF;
                    return InfiniteWorldDecorationLayer.OFFICE_CHAIR;

                case 2: // Level 1: concrete storage/service halls.
                    if (roll < 18) return InfiniteWorldDecorationLayer.BLUE_UTILITY;
                    if (roll < 34) return InfiniteWorldDecorationLayer.RED_UTILITY;
                    if (roll < 50) return InfiniteWorldDecorationLayer.ROAD_BARRIER;
                    if (roll < 65) return InfiniteWorldDecorationLayer.REFUSE_BIN;
                    if (roll < 80) return InfiniteWorldDecorationLayer.WOODEN_CRATE;
                    if (roll < 90) return InfiniteWorldDecorationLayer.IRON_RAIL;
                    return InfiniteWorldDecorationLayer.BOLLARD;

                case 3: // Level 2: hot maintenance corridors and pipe-like utilities.
                    if (roll < 22) return InfiniteWorldDecorationLayer.RED_UTILITY;
                    if (roll < 42) return InfiniteWorldDecorationLayer.BLUE_UTILITY;
                    if (roll < 58) return InfiniteWorldDecorationLayer.IRON_RAIL;
                    if (roll < 72) return InfiniteWorldDecorationLayer.ROAD_BARRIER;
                    if (roll < 84) return InfiniteWorldDecorationLayer.WARNING_SIGN;
                    if (roll < 93) return InfiniteWorldDecorationLayer.RUBBLE;
                    return InfiniteWorldDecorationLayer.BOLLARD;

                case 4: // Level 3: electrical station.
                    if (roll < 20) return InfiniteWorldDecorationLayer.BLUE_UTILITY;
                    if (roll < 37) return InfiniteWorldDecorationLayer.RED_UTILITY;
                    if (roll < 52) return InfiniteWorldDecorationLayer.WARNING_SIGN;
                    if (roll < 66) return InfiniteWorldDecorationLayer.ROAD_BARRIER;
                    if (roll < 79) return InfiniteWorldDecorationLayer.IRON_RAIL;
                    if (roll < 90) return InfiniteWorldDecorationLayer.BOLLARD;
                    return InfiniteWorldDecorationLayer.REFUSE_BIN;

                case 5: // Level 4: abandoned office.
                    if (roll < 24) return InfiniteWorldDecorationLayer.LOW_DESK;
                    if (roll < 43) return InfiniteWorldDecorationLayer.OFFICE_CHAIR;
                    if (roll < 60) return InfiniteWorldDecorationLayer.BACKROOMS_SHELF;
                    if (roll < 75) return InfiniteWorldDecorationLayer.SERVICE_COUNTER;
                    if (roll < 88) return InfiniteWorldDecorationLayer.FLOOR_LAMP;
                    return InfiniteWorldDecorationLayer.BLUE_LOCKER;

                case 6: // Level 5: hotel.
                    if (roll < 30) return InfiniteWorldDecorationLayer.BED;
                    if (roll < 50) return InfiniteWorldDecorationLayer.FLOOR_LAMP;
                    if (roll < 68) return InfiniteWorldDecorationLayer.LOW_DESK;
                    if (roll < 84) return InfiniteWorldDecorationLayer.BACKROOMS_SHELF;
                    return InfiniteWorldDecorationLayer.SERVICE_COUNTER;

                case 7: // Level 6: lights out — sparse, dark, no light-emitting props.
                    if (roll < 42) return InfiniteWorldDecorationLayer.RUBBLE;
                    if (roll < 72) return InfiniteWorldDecorationLayer.METAL_RAIL;
                    return InfiniteWorldDecorationLayer.BLUE_LOCKER;

                case 8: // Level 7: ocean — almost nothing artificial.
                    if (roll < 45) return InfiniteWorldDecorationLayer.ROCK;
                    if (roll < 78) return InfiniteWorldDecorationLayer.LOG;
                    return InfiniteWorldDecorationLayer.BARREL;

                case 9: // Level 8: caves; natural clutter only.
                    if (roll < 38) return InfiniteWorldDecorationLayer.ROCK;
                    if (roll < 64) return InfiniteWorldDecorationLayer.MUSHROOMS;
                    if (roll < 84) return InfiniteWorldDecorationLayer.RUBBLE;
                    return InfiniteWorldDecorationLayer.LOG;

                case 10: // Level 9: suburbs.
                    if (roll < 15) return InfiniteWorldDecorationLayer.STREET_LAMP;
                    if (roll < 28) return InfiniteWorldDecorationLayer.CURVED_STREET_LAMP;
                    if (roll < 40) return InfiniteWorldDecorationLayer.CITY_BENCH;
                    if (roll < 52) return InfiniteWorldDecorationLayer.URBAN_SIGN;
                    if (roll < 64) return InfiniteWorldDecorationLayer.REFUSE_BIN;
                    if (roll < 76) return InfiniteWorldDecorationLayer.FENCE_RAIL;
                    if (roll < 88) return InfiniteWorldDecorationLayer.ROUND_TREE;
                    return InfiniteWorldDecorationLayer.REFUSE_BAGS;

                case 11: // Level 10: fields.
                    if (roll < 24) return InfiniteWorldDecorationLayer.ROUND_TREE;
                    if (roll < 44) return InfiniteWorldDecorationLayer.FERN;
                    if (roll < 60) return InfiniteWorldDecorationLayer.FENCE_RAIL;
                    if (roll < 74) return InfiniteWorldDecorationLayer.FENCE_POST;
                    if (roll < 87) return InfiniteWorldDecorationLayer.LOG;
                    if (roll < 95) return InfiniteWorldDecorationLayer.BARREL;
                    return InfiniteWorldDecorationLayer.WOODEN_POST;

                case 12: // Level 11: city.
                    if (roll < 16) return InfiniteWorldDecorationLayer.STREET_LAMP;
                    if (roll < 30) return InfiniteWorldDecorationLayer.CURVED_STREET_LAMP;
                    if (roll < 43) return InfiniteWorldDecorationLayer.CITY_BENCH;
                    if (roll < 56) return InfiniteWorldDecorationLayer.REFUSE_BIN;
                    if (roll < 67) return InfiniteWorldDecorationLayer.BOLLARD;
                    if (roll < 78) return InfiniteWorldDecorationLayer.URBAN_SIGN;
                    if (roll < 88) return InfiniteWorldDecorationLayer.ROAD_BARRIER;
                    if (roll < 95) return InfiniteWorldDecorationLayer.BLUE_UTILITY;
                    return InfiniteWorldDecorationLayer.REFUSE_BAGS;

                case 13: // Level 37: Poolrooms — white tile and blue water carry the scene.
                    if (roll < 48) return InfiniteWorldDecorationLayer.BATH_FIXTURE;
                    if (roll < 76) return InfiniteWorldDecorationLayer.WAYFINDING_PILLAR;
                    return InfiniteWorldDecorationLayer.METAL_RAIL;

                case 14: // Level 94: stop-motion 1930s town, not a playground.
                    if (roll < 18) return InfiniteWorldDecorationLayer.ROUND_TREE;
                    if (roll < 34) return InfiniteWorldDecorationLayer.ROUND_TREE_AUTUMN;
                    if (roll < 48) return InfiniteWorldDecorationLayer.STREET_LAMP;
                    if (roll < 62) return InfiniteWorldDecorationLayer.FENCE_RAIL;
                    if (roll < 76) return InfiniteWorldDecorationLayer.BARREL;
                    if (roll < 90) return InfiniteWorldDecorationLayer.FERN;
                    return InfiniteWorldDecorationLayer.WAYFINDING_PILLAR;

                case 15: // Level 13 apartments.
                    if (roll < 24) return InfiniteWorldDecorationLayer.BED;
                    if (roll < 44) return InfiniteWorldDecorationLayer.LOW_DESK;
                    if (roll < 61) return InfiniteWorldDecorationLayer.FLOOR_LAMP;
                    if (roll < 76) return InfiniteWorldDecorationLayer.BACKROOMS_SHELF;
                    if (roll < 89) return InfiniteWorldDecorationLayer.OFFICE_CHAIR;
                    return InfiniteWorldDecorationLayer.SERVICE_COUNTER;

                case 16: // Level 18 memories/daycare.
                    if (roll < 30) return InfiniteWorldDecorationLayer.PLAY_BLOCKS;
                    if (roll < 50) return InfiniteWorldDecorationLayer.LOW_DESK;
                    if (roll < 68) return InfiniteWorldDecorationLayer.BACKROOMS_SHELF;
                    if (roll < 84) return InfiniteWorldDecorationLayer.FLOOR_LAMP;
                    return InfiniteWorldDecorationLayer.BED;

                case 17: // Level 34 sewer.
                    if (roll < 24) return InfiniteWorldDecorationLayer.LADDER;
                    if (roll < 44) return InfiniteWorldDecorationLayer.SAFETY_PYLON;
                    if (roll < 64) return InfiniteWorldDecorationLayer.METAL_RAIL;
                    if (roll < 80) return InfiniteWorldDecorationLayer.BLUE_LOCKER;
                    if (roll < 92) return InfiniteWorldDecorationLayer.RED_LOCKER;
                    return InfiniteWorldDecorationLayer.RUBBLE;

                case 18: // Level 40 arcade.
                    if (roll < 42) return InfiniteWorldDecorationLayer.ARCADE_CABINET;
                    if (roll < 58) return InfiniteWorldDecorationLayer.SERVICE_COUNTER;
                    if (roll < 70) return InfiniteWorldDecorationLayer.PLAY_BLOCKS;
                    if (roll < 82) return InfiniteWorldDecorationLayer.SIGNBOARD;
                    if (roll < 92) return InfiniteWorldDecorationLayer.REFUSE_BIN;
                    return InfiniteWorldDecorationLayer.FLOOR_LAMP;

                case 19: // Level 48 sunset beach.
                    if (roll < 28) return InfiniteWorldDecorationLayer.ROUND_TREE;
                    if (roll < 48) return InfiniteWorldDecorationLayer.FERN;
                    if (roll < 66) return InfiniteWorldDecorationLayer.LOG;
                    if (roll < 84) return InfiniteWorldDecorationLayer.ROCK;
                    return InfiniteWorldDecorationLayer.BARREL;

                case 20: // Level 974 Kitty's House.
                    if (roll < 25) return InfiniteWorldDecorationLayer.BED;
                    if (roll < 43) return InfiniteWorldDecorationLayer.FLOOR_LAMP;
                    if (roll < 60) return InfiniteWorldDecorationLayer.LOW_DESK;
                    if (roll < 75) return InfiniteWorldDecorationLayer.BACKROOMS_SHELF;
                    if (roll < 88) return InfiniteWorldDecorationLayer.PLAY_BLOCKS;
                    return InfiniteWorldDecorationLayer.OFFICE_CHAIR;

                default:
                    if (roll < 12) return InfiniteWorldDecorationLayer.ROUND_TREE;
                    if (roll < 22) return InfiniteWorldDecorationLayer.MUSHROOMS;
                    if (roll < 32) return InfiniteWorldDecorationLayer.BARREL;
                    if (roll < 42) return InfiniteWorldDecorationLayer.WOODEN_CRATE;
                    if (roll < 52) return InfiniteWorldDecorationLayer.ROCK;
                    if (roll < 62) return InfiniteWorldDecorationLayer.REFUSE_BIN;
                    if (roll < 72) return InfiniteWorldDecorationLayer.CITY_BENCH;
                    if (roll < 82) return InfiniteWorldDecorationLayer.URBAN_SIGN;
                    if (roll < 91) return InfiniteWorldDecorationLayer.RUBBLE;
                    return InfiniteWorldDecorationLayer.CAMPFIRE;
            }
        }

        switch (anomaly) {
            case 1: // Level 0: deliberately minimal, uncanny office clutter.
                if (roll < 30) return InfiniteWorldDecorationLayer.SIGN;
                if (roll < 55) return InfiniteWorldDecorationLayer.STOOL;
                if (roll < 75) return InfiniteWorldDecorationLayer.RUBBLE;
                return InfiniteWorldDecorationLayer.CUPBOARD;

            case 2: // Level 1
            case 3: // Level 2
            case 4: // Level 3
                if (roll < 18) return InfiniteWorldDecorationLayer.BARREL;
                if (roll < 35) return InfiniteWorldDecorationLayer.WOODEN_CRATE;
                if (roll < 48) return InfiniteWorldDecorationLayer.CUPBOARD;
                if (roll < 61) return InfiniteWorldDecorationLayer.IRON_RAIL;
                if (roll < 72) return InfiniteWorldDecorationLayer.WARNING_SIGN;
                if (roll < 84) return InfiniteWorldDecorationLayer.TORCH;
                if (roll < 93) return InfiniteWorldDecorationLayer.RUBBLE;
                return InfiniteWorldDecorationLayer.WEAPON_RACK;

            case 5: // Level 4 office
                if (roll < 22) return InfiniteWorldDecorationLayer.TABLE;
                if (roll < 40) return InfiniteWorldDecorationLayer.STOOL;
                if (roll < 57) return InfiniteWorldDecorationLayer.BOOKSHELF;
                if (roll < 72) return InfiniteWorldDecorationLayer.CUPBOARD;
                if (roll < 86) return InfiniteWorldDecorationLayer.SIGN;
                return InfiniteWorldDecorationLayer.WOODEN_CRATE;

            case 6: // Level 5 hotel
                if (roll < 22) return InfiniteWorldDecorationLayer.TABLE;
                if (roll < 38) return InfiniteWorldDecorationLayer.STOOL;
                if (roll < 54) return InfiniteWorldDecorationLayer.CUPBOARD;
                if (roll < 68) return InfiniteWorldDecorationLayer.BOOKSHELF;
                if (roll < 80) return InfiniteWorldDecorationLayer.SIGN;
                if (roll < 90) return InfiniteWorldDecorationLayer.EMPTY_TUB;
                return InfiniteWorldDecorationLayer.TORCH;

            case 7: // Level 6
                if (roll < 30) return InfiniteWorldDecorationLayer.RUBBLE;
                if (roll < 50) return InfiniteWorldDecorationLayer.CASKET;
                if (roll < 68) return InfiniteWorldDecorationLayer.GRAVESTONE;
                if (roll < 84) return InfiniteWorldDecorationLayer.TORCH;
                return InfiniteWorldDecorationLayer.STONE_CROSS;

            case 8: // Level 7 ocean
                if (roll < 28) return InfiniteWorldDecorationLayer.LOG;
                if (roll < 52) return InfiniteWorldDecorationLayer.BARREL;
                if (roll < 72) return InfiniteWorldDecorationLayer.ROCK;
                if (roll < 87) return InfiniteWorldDecorationLayer.SIGN;
                return InfiniteWorldDecorationLayer.WATER_TROUGH;

            case 9: // Level 8 caves
                if (roll < 34) return InfiniteWorldDecorationLayer.MUSHROOMS;
                if (roll < 57) return InfiniteWorldDecorationLayer.ROCK;
                if (roll < 73) return InfiniteWorldDecorationLayer.RUBBLE;
                if (roll < 86) return InfiniteWorldDecorationLayer.CAMPFIRE;
                return InfiniteWorldDecorationLayer.STONE_CROSS;

            case 10: // Level 9 suburbs
                if (roll < 18) return InfiniteWorldDecorationLayer.ROUND_TREE;
                if (roll < 32) return InfiniteWorldDecorationLayer.PINE_TREE_GREEN;
                if (roll < 45) return InfiniteWorldDecorationLayer.FENCE_RAIL;
                if (roll < 56) return InfiniteWorldDecorationLayer.FENCE_POST;
                if (roll < 68) return InfiniteWorldDecorationLayer.SIGN;
                if (roll < 80) return InfiniteWorldDecorationLayer.BARREL;
                if (roll < 90) return InfiniteWorldDecorationLayer.LOG;
                return InfiniteWorldDecorationLayer.WOODEN_CRATE;

            case 11: // Level 10 fields
                if (roll < 20) return InfiniteWorldDecorationLayer.ROUND_TREE;
                if (roll < 36) return InfiniteWorldDecorationLayer.PINE_TREE_GREEN;
                if (roll < 50) return InfiniteWorldDecorationLayer.FERN;
                if (roll < 63) return InfiniteWorldDecorationLayer.MUSHROOMS;
                if (roll < 75) return InfiniteWorldDecorationLayer.FENCE_RAIL;
                if (roll < 84) return InfiniteWorldDecorationLayer.FENCE_POST;
                if (roll < 92) return InfiniteWorldDecorationLayer.LOG;
                return InfiniteWorldDecorationLayer.SIGN;

            case 12: // Level 11 city
                if (roll < 18) return InfiniteWorldDecorationLayer.SIGN;
                if (roll < 32) return InfiniteWorldDecorationLayer.WARNING_SIGN;
                if (roll < 47) return InfiniteWorldDecorationLayer.BARREL;
                if (roll < 61) return InfiniteWorldDecorationLayer.WOODEN_CRATE;
                if (roll < 74) return InfiniteWorldDecorationLayer.IRON_RAIL;
                if (roll < 86) return InfiniteWorldDecorationLayer.TABLE;
                return InfiniteWorldDecorationLayer.ROCK;

            case 13: // Level 37 poolrooms
                if (roll < 24) return InfiniteWorldDecorationLayer.STONE_BASIN;
                if (roll < 46) return InfiniteWorldDecorationLayer.EMPTY_TUB;
                if (roll < 66) return InfiniteWorldDecorationLayer.WATER_TROUGH;
                if (roll < 82) return InfiniteWorldDecorationLayer.SIGN;
                return InfiniteWorldDecorationLayer.ROCK;

            case 14: // Level 94 Motion
                if (roll < 16) return InfiniteWorldDecorationLayer.ROUND_TREE;
                if (roll < 29) return InfiniteWorldDecorationLayer.ROUND_TREE_AUTUMN;
                if (roll < 42) return InfiniteWorldDecorationLayer.PINE_TREE_GREEN;
                if (roll < 54) return InfiniteWorldDecorationLayer.PINE_TREE_AUTUMN;
                if (roll < 65) return InfiniteWorldDecorationLayer.FENCE_RAIL;
                if (roll < 75) return InfiniteWorldDecorationLayer.FERN;
                if (roll < 84) return InfiniteWorldDecorationLayer.MUSHROOMS;
                if (roll < 92) return InfiniteWorldDecorationLayer.LOG;
                return InfiniteWorldDecorationLayer.SIGN;

            default: // ordinary Infinite World
                if (roll < 12) return InfiniteWorldDecorationLayer.ROUND_TREE;
                if (roll < 22) return InfiniteWorldDecorationLayer.PINE_TREE_GREEN;
                if (roll < 32) return InfiniteWorldDecorationLayer.MUSHROOMS;
                if (roll < 42) return InfiniteWorldDecorationLayer.FERN;
                if (roll < 52) return InfiniteWorldDecorationLayer.BARREL;
                if (roll < 62) return InfiniteWorldDecorationLayer.WOODEN_CRATE;
                if (roll < 71) return InfiniteWorldDecorationLayer.SIGN;
                if (roll < 80) return InfiniteWorldDecorationLayer.ROCK;
                if (roll < 88) return InfiniteWorldDecorationLayer.LOG;
                if (roll < 94) return InfiniteWorldDecorationLayer.RUBBLE;
                return InfiniteWorldDecorationLayer.CAMPFIRE;
        }
    }

    private void refreshInfiniteWorldAccentOverlays() {
        if (customTiles == null || customWalls == null) return;

        customTiles.clear();
        customWalls.clear();
        rebuildAccentTiles();
        rebuildV15DecorationProps();
        GameScene.refreshInfiniteWorldCustomOverlays();
    }

    public int stableTileVariance(int localPos) {
        if (localPos < 0 || localPos >= length()) return 50;
        int wx = worldXForLocalCell(localPos);
        int wy = worldYForLocalCell(localPos);
        return (int)Math.floorMod(hash(wx, wy, 15001), 100L);
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

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
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.BurningTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.ChillingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.ConfusionTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GrippingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.OozeTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.PoisonDartTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.TeleportationTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.InfiniteWorldAccentTilemap;
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

    private boolean shifting;

    // Infinite World ecology is deliberately sparse. The player should normally
    // see only a few enemies at a time, with exploration remaining the main loop.
    private static final int MOB_SPAWN_TARGET_CAP = 5;
    private static final int MOB_HARD_CAP = 6;
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

    // V12 pacing guarantees: after 400 positive hero action-time the run receives
    // exactly one visible, nearby crystal chest containing an artifact.
    private static final float GUARANTEED_ARTIFACT_CHEST_ACTION_VALUE = 400f;

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
        Actor.addDelayed(mobEcology, 20f + Random.Float() * 12f);
        return mobEcology;
    }

    @Override
    public int mobLimit() {
        return MOB_HARD_CAP;
    }

    @Override
    public Mob createMob() {
        return createInfiniteWorldMob();
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
                spend(MOB_RESPAWN_MIN_TURNS);
                return true;
            }

            pruneInfiniteWorldMobs();

            int count = activeEnemyCount();
            if (count < MOB_SPAWN_TARGET_CAP) {
                float chance;
                if (count == 0) chance = 0.82f;
                else if (count <= 2) chance = 0.58f;
                else chance = 0.32f;

                int heroChunkX = Math.floorDiv(state().heroWorldX, CHUNK_SIZE);
                int heroChunkY = Math.floorDiv(state().heroWorldY, CHUNK_SIZE);
                if (v9AnomalyType(heroChunkX, heroChunkY) != 0) {
                    // Liminal macro-regions are intentionally quieter and emptier.
                    chance *= 0.45f;
                }

                if (Random.Float() < chance) {
                    spawnInfiniteWorldMob();
                }
            }

            spend(MOB_RESPAWN_MIN_TURNS
                    + Random.Float() * (MOB_RESPAWN_MAX_TURNS - MOB_RESPAWN_MIN_TURNS));
            return true;
        }
    }

    private boolean spawnInfiniteWorldMob() {
        if (Dungeon.hero == null || !Dungeon.hero.isAlive()) return false;
        if (activeEnemyCount() >= MOB_SPAWN_TARGET_CAP) return false;

        Mob mob = createInfiniteWorldMob();
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
        int worldChunkX = Math.floorDiv(state().heroWorldX, CHUNK_SIZE);
        int worldChunkY = Math.floorDiv(state().heroWorldY, CHUNK_SIZE);
        int worldDistance = Math.max(Math.abs(worldChunkX), Math.abs(worldChunkY));

        int tier = 0;
        if (heroLevel >= 4 || worldDistance >= 10) tier = 1;
        if (heroLevel >= 9 || worldDistance >= 24) tier = 2;

        int roll = Random.Int(100);
        if (tier >= 2 && roll < 20) {
            switch (Random.Int(5)) {
                case 0: return new Warlock();
                case 1: return new Monk();
                case 2: return new Spinner();
                case 3: return new Brute();
                default:return new Bat();
            }
        }

        if (tier >= 1 && roll < 55) {
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
        generateV9AnomalyNotes();
        restoreGuaranteedArtifactChest();
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
        st.markChunkExplored(
                Math.floorDiv(st.heroWorldX, CHUNK_SIZE),
                Math.floorDiv(st.heroWorldY, CHUNK_SIZE));

        if (state().generatorVersion >= 15) {
            ensureV15BackroomsInfo(hero);
        } else {
            ensureV12AnomalyNoteNearby(hero);
        }
        ensureV13MerchantDiscovery(hero);
        pruneInfiniteWorldMobs();
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

        int cell = findNearbyVisibleObjectCell(Dungeon.hero, 2, 5);
        if (cell < 0) cell = findNearbyVisibleObjectCell(Dungeon.hero, 1, 7);
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

        if (x < SHIFT_LOW) shiftX = -SHIFT_STEP;
        else if (x >= SHIFT_HIGH) shiftX = SHIFT_STEP;

        if (y < SHIFT_LOW) shiftY = -SHIFT_STEP;
        else if (y >= SHIFT_HIGH) shiftY = SHIFT_STEP;

        if (shiftX == 0 && shiftY == 0) return;

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
        shifting = false;
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
        snapshotV9AnomalyNotes();
        snapshotGuaranteedArtifactChest();
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
        generateV9AnomalyNotes();
        restoreGuaranteedArtifactChest();
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
        int roll = (int)Math.floorMod(hash(cx, cy, 7001), 8L);
        if (roll <= 2) return new PotionOfHealing();
        if (roll <= 4) return new ScrollOfTeleportation();
        if (roll <= 6) return new Bomb();
        // The pickaxe is intentionally obtainable here because v4 mineral veins
        // use the game's native mining interaction.
        return new Pickaxe();
    }

    private void generateV5Traps() {
        if (state().generatorVersion < 5) return;

        for (int cell = 0; cell < length(); cell++) {
            if (map[cell] != Terrain.SECRET_TRAP && map[cell] != Terrain.TRAP) continue;
            if (traps.get(cell) != null) continue;

            int wx = worldXForLocalCell(cell);
            int wy = worldYForLocalCell(cell);
            int roll = (int)Math.floorMod(hash(wx, wy, 18100), 7L);
            Trap trap;
            switch (roll) {
                case 0: trap = new TeleportationTrap(); break;
                case 1: trap = new BurningTrap(); break;
                case 2: trap = new ChillingTrap(); break;
                case 3: trap = new PoisonDartTrap(); break;
                case 4: trap = new OozeTrap(); break;
                case 5: trap = new ConfusionTrap(); break;
                default:trap = new GrippingTrap(); break;
            }
            trap.set(cell);
            trap.visible = map[cell] == Terrain.TRAP;
            traps.put(cell, trap);
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
                if (state().generatorVersion >= 9) {
                    count = v9AnomalyType(cx, cy) == 0
                            && Math.floorMod(hash(cx, cy, 19000), 100L) < 30 ? 1 : 0;
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
                int ironChance = state().generatorVersion >= 9 ? 8 : 38;
                int crystalChance = state().generatorVersion >= 9 ? 5 : 32;
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
                if (state().generatorVersion >= 9) {
                    count = v9AnomalyType(cx, cy) == 0
                            && Math.floorMod(hash(cx, cy, 19000), 100L) < 30 ? 1 : 0;
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

        // V13 makes outposts substantially easier to encounter and pairs the denser
        // lattice with an explicit nearby-outpost discovery hint. Older worlds keep
        // their exact cadence for deterministic save compatibility.
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
                    case 2: return new StoneOfBlink();
                    case 3: return new Pickaxe();
                    default:return new ScrollOfRemoveCurse();
                }
        }
    }

    private Item v11MerchantEquipment(int cx, int cy) {
        int worldDistance = Math.max(Math.abs(cx), Math.abs(cy));
        int tier = Math.min(4, Math.max(0, worldDistance / 12));

        Random.pushGenerator(hash(cx, cy, 25140));
        try {
            Item item;
            int kind = range(cx, cy, 25141, 0, 99);
            if (kind < 46) item = Generator.randomWeapon(tier, true);
            else if (kind < 78) item = Generator.randomArmor(tier);
            else if (kind < 90) item = Generator.randomUsingDefaults(Generator.Category.WAND);
            else item = Generator.randomUsingDefaults(Generator.Category.RING);

            item.cursed = false;
            item.cursedKnown = true;
            if (item.isUpgradable()) {
                item.level(0);
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
            items = new Item[]{ new Bomb(), new StoneOfBlink() };
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
                default: return "Level_94";
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
    }

    // ------------------------------------------------------------------------
    // Generator v9: guaranteed infinite backbone + rare liminal macro-zones.
    // ------------------------------------------------------------------------

    private int v9AnomalyType(int cx, int cy) {
        if (state().generatorVersion < 9) return 0;

        // V15 treats Backrooms-inspired districts as a normal part of exploration,
        // so they can start much closer to the origin. Older worlds retain their
        // original opening buffer for deterministic save compatibility.
        int startBuffer = state().generatorVersion >= 15 ? 2 : 4;
        if (Math.abs(cx) <= startBuffer && Math.abs(cy) <= startBuffer) return 0;

        final int macro = state().generatorVersion >= 15 ? 4 : 5;
        int mx = Math.floorDiv(cx, macro);
        int my = Math.floorDiv(cy, macro);

        // V15 increases both frequency and variety: 42% of 4x4 macro-regions use
        // one of fourteen Backrooms-inspired environments. Older generators stay exact.
        int anomalyChance = state().generatorVersion >= 15 ? 42
                : (state().generatorVersion >= 13 ? 28
                : (state().generatorVersion >= 12 ? 15 : 8));
        if (Math.floorMod(hash(mx, my, 23000), 100L) >= anomalyChance) return 0;

        int anomalyTypes = state().generatorVersion >= 15 ? 14
                : (state().generatorVersion >= 13 ? 6 : 3);
        return 1 + (int)Math.floorMod(hash(mx, my, 23001), (long)anomalyTypes);
    }

    private void generateV9AnomalyChunk(int[] out, int cx, int cy, int ox, int oy,
                                        int anomaly, int northX, int southX,
                                        int westY, int eastY) {
        carveRect(out, ox, oy, ox + CHUNK_SIZE - 1, oy + CHUNK_SIZE - 1, Terrain.WALL);

        if (anomaly == 1) {
            // Repetitive liminal offices: mostly empty rooms separated by a rigid
            // wall lattice, with deterministic gaps that repeat across a 5x5 district.
            carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);

            for (int x = 5; x < CHUNK_SIZE - 2; x += 6) {
                for (int y = 1; y < CHUNK_SIZE - 1; y++) {
                    out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL;
                }
                int gapA = 2 + range(cx, cy, 23100 + x, 0, CHUNK_SIZE - 5);
                int gapB = 2 + range(cx, cy, 23120 + x, 0, CHUNK_SIZE - 5);
                setFloor(out, ox + x, oy + gapA);
                setFloor(out, ox + x, oy + gapB);
            }
            for (int y = 5; y < CHUNK_SIZE - 2; y += 6) {
                for (int x = 1; x < CHUNK_SIZE - 1; x++) {
                    out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL;
                }
                int gapA = 2 + range(cx, cy, 23140 + y, 0, CHUNK_SIZE - 5);
                int gapB = 2 + range(cx, cy, 23160 + y, 0, CHUNK_SIZE - 5);
                setFloor(out, ox + gapA, oy + y);
                setFloor(out, ox + gapB, oy + y);
            }

        } else if (anomaly == 2) {
            // Pool halls: broad empty floors, repeated pools and narrow dry lanes.
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

            // Repeating crosswalks keep the pools readable and traversable.
            carveRect(out, ox + 11, oy + 1, ox + 12, oy + CHUNK_SIZE - 2, Terrain.EMPTY);
            carveRect(out, ox + 1, oy + 11, ox + CHUNK_SIZE - 2, oy + 12, Terrain.EMPTY);

        } else if (anomaly == 3) {
            // Endless hall: deliberately oversized and sparse, with repeating pillars.
            carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
            for (int y = 4; y < CHUNK_SIZE - 3; y += 5) {
                for (int x = 4; x < CHUNK_SIZE - 3; x += 5) {
                    if (((x + y + cx + cy) & 1) == 0) {
                        out[ox + x + (oy + y) * MAP_SIZE] = Terrain.STATUE;
                    }
                }
            }
        } else if (anomaly == 4) {
            // Yellow maze: dense repeating partitions with subtly shifting gaps.
            carveRect(out, ox + 1, oy + 1, ox + CHUNK_SIZE - 2, oy + CHUNK_SIZE - 2, Terrain.EMPTY_SP);
            for (int x = 4; x < CHUNK_SIZE - 2; x += 4) {
                for (int y = 1; y < CHUNK_SIZE - 1; y++) {
                    out[ox + x + (oy + y) * MAP_SIZE] = Terrain.WALL;
                }
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
            // Service tunnels: a harsh, repetitive maintenance grid.
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
            // Dark storage: repeated solid storage blocks divided by narrow aisles.
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

        setFloor(out, ox + CHUNK_SIZE / 2, oy + CHUNK_SIZE / 2);

        // Preserve the normal shared edge contract so anomaly chunks always meet
        // conventional chunks cleanly.
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
            return Math.floorMod(hash(cx, cy, 6001), 100) < 12;
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
                        int alt;
                        switch (anomaly) {
                            case 1: alt = 3; break; // city
                            case 2: alt = 0; break; // sewers
                            case 3: alt = 4; break; // halls
                            case 4: alt = 1; break; // prison
                            case 5: alt = 2; break; // caves
                            default:alt = 4; break; // halls
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

    private void refreshInfiniteWorldAccentOverlays() {
        if (customTiles == null || customWalls == null) return;

        customTiles.clear();
        customWalls.clear();
        rebuildAccentTiles();
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

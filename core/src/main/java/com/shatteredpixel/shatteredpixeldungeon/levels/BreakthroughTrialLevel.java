/*
 * Shattered Pixel Dungeon - Assist Edition
 * Level-30 Infinite World breakthrough trial.
 */
package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class BreakthroughTrialLevel extends Level {

    private static final int MAP_W = 49;
    private static final int MAP_H = 49;
    private static final int TOTAL_WAVES = 10;
    private static final int MOBS_PER_WAVE = 10;
    private static final float BETWEEN_WAVE_COUNTDOWN = 30f;

    {
        color1 = 0x3F4558;
        color2 = 0x817A95;
    }

    @Override
    public String tilesTex() {
        return Assets.Environment.TILES_HALLS;
    }

    @Override
    public String waterTex() {
        return Assets.Environment.WATER_HALLS;
    }

    @Override
    public void playLevelMusic() {
        Music.INSTANCE.play(Assets.Music.HALLS_2, true);
    }

    @Override
    protected boolean build() {
        setSize(MAP_W, MAP_H);

        for (int y = 1; y < MAP_H - 1; y++) {
            for (int x = 1; x < MAP_W - 1; x++) {
                map[cell(x, y)] = Terrain.EMPTY;
            }
        }

        // Sparse cover islands: enough to break line-of-sight without turning the
        // trial into a corridor maze.
        for (int y = 9; y <= 39; y += 10) {
            for (int x = 9; x <= 39; x += 10) {
                if (x == 19 || x == 29 || y == 19 || y == 29) continue;
                map[cell(x, y)] = Terrain.STATUE;
            }
        }

        // Four small restorative corners provide tactical space between waves.
        fillRect(4, 4, 7, 7, Terrain.GRASS);
        fillRect(41, 4, 44, 7, Terrain.GRASS);
        fillRect(4, 41, 7, 44, Terrain.GRASS);
        fillRect(41, 41, 44, 44, Terrain.GRASS);

        int entrance = cell(MAP_W / 2, MAP_H / 2);
        map[entrance] = Terrain.ENTRANCE;
        transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.REGULAR_ENTRANCE));
        return true;
    }

    private int cell(int x, int y) {
        return x + y * width();
    }

    private void fillRect(int left, int top, int right, int bottom, int terrain) {
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) map[cell(x, y)] = terrain;
        }
    }

    @Override
    protected void createMobs() {
        // The TrialDirector owns every wave.
    }

    @Override
    public Mob createMob() {
        return null;
    }

    @Override
    public Actor addRespawner() {
        TrialDirector director = new TrialDirector();
        Actor.addDelayed(director, 1f);
        return director;
    }

    @Override
    protected void createItems() {
        int cx = width() / 2;
        int cy = height() / 2;

        drop(new PotionOfHealing(), cell(cx - 6, cy - 3));
        drop(new PotionOfHealing(), cell(cx + 6, cy + 3));
        drop(new PotionOfHealing(), cell(cx, cy + 7));
        drop(new SmallRation(), cell(cx - 7, cy + 5));
        drop(new SmallRation(), cell(cx + 7, cy - 5));
        drop(new PotionOfInvisibility(), cell(cx - 5, cy - 7));
        drop(new ScrollOfRecharging(), cell(cx + 5, cy + 7));
        drop(new ScrollOfRage(), cell(cx + 8, cy));
    }

    @Override
    public int randomRespawnCell(Char ch) {
        return cell(width() / 2, height() / 2);
    }

    @Override
    public boolean activateTransition(Hero hero, LevelTransition transition) {
        return false;
    }

    public void finishTrial(boolean success) {
        InfiniteWorldProgression.finishBreakthroughTrial(success);
    }

    private boolean hasLivingTrialEnemies() {
        for (Mob mob : mobs) {
            if (mob.alignment == Char.Alignment.ENEMY && mob.isAlive()) return true;
        }
        return false;
    }

    private void spawnWave(int wave) {
        InfiniteWorldState st = Dungeon.infiniteWorldState;
        if (st == null || Dungeon.hero == null) return;

        st.breakthroughWave = wave;
        st.breakthroughCountdown = -1f;

        ArrayList<Integer> used = new ArrayList<>();
        for (int i = 0; i < MOBS_PER_WAVE; i++) {
            Mob mob = mobForWave(wave, i);
            InfiniteWorldProgression.applyTrialScaling(mob, Dungeon.hero, wave);

            int pos = findWaveSpawnCell(used);
            if (pos < 0) continue;
            used.add(pos);

            mob.pos = pos;
            mob.state = mob.WANDERING;
            mob.aggro(Dungeon.hero);
            GameScene.add(mob);
        }

        GLog.w("突破试炼 · 第 " + wave + "/10 波：10只敌人已经进入试炼场！");
    }

    private int findWaveSpawnCell(ArrayList<Integer> used) {
        int hx = Dungeon.hero.pos % width();
        int hy = Dungeon.hero.pos / width();

        for (int attempt = 0; attempt < 300; attempt++) {
            int x = Random.IntRange(3, width() - 4);
            int y = Random.IntRange(3, height() - 4);
            int cell = cell(x, y);
            int dist = Math.max(Math.abs(x - hx), Math.abs(y - hy));
            if (dist < 11) continue;
            if (!passable[cell] || solid[cell] || pit[cell] || used.contains(cell)) continue;
            if (Actor.findChar(cell) != null) continue;
            return cell;
        }

        for (int y = 3; y < height() - 3; y++) {
            for (int x = 3; x < width() - 3; x++) {
                int cell = cell(x, y);
                int dist = Math.max(Math.abs(x - hx), Math.abs(y - hy));
                if (dist >= 9 && passable[cell] && !solid[cell]
                        && Actor.findChar(cell) == null && !used.contains(cell)) {
                    return cell;
                }
            }
        }
        return -1;
    }

    private Mob mobForWave(int wave, int index) {
        int roll = Random.Int(100);

        if (wave <= 2) {
            switch ((index + Random.Int(5)) % 5) {
                case 0: return new Rat();
                case 1: return new Snake();
                case 2: return new Gnoll();
                case 3: return new Crab();
                default:return new Slime();
            }
        }

        if (wave <= 4) {
            switch ((index + Random.Int(5)) % 5) {
                case 0: return new Crab();
                case 1: return new Slime();
                case 2: return new Skeleton();
                case 3: return new DM100();
                default:return new Bat();
            }
        }

        if (wave <= 6) {
            if (roll < 25) {
                return Random.Int(2) == 0 ? new Warlock() : new Monk();
            }
            switch ((index + Random.Int(5)) % 5) {
                case 0: return new Skeleton();
                case 1: return new DM100();
                case 2: return new Bat();
                case 3: return new Brute();
                default:return new Spinner();
            }
        }

        if (wave <= 8) {
            if (roll < 35) {
                switch (Random.Int(3)) {
                    case 0: return new Warlock();
                    case 1: return new Monk();
                    default:return new Golem();
                }
            }
            switch ((index + Random.Int(4)) % 4) {
                case 0: return new Brute();
                case 1: return new Spinner();
                case 2: return new Bat();
                default:return new DM100();
            }
        }

        if (wave == 9) {
            if (roll < 42) {
                switch (Random.Int(5)) {
                    case 0: return new Warlock();
                    case 1: return new Monk();
                    case 2: return new Golem();
                    case 3: return new Succubus();
                    default:return new Scorpio();
                }
            }
            return index % 2 == 0 ? new Brute() : new Spinner();
        }

        // Wave 10 deliberately mixes five late-game threats with five mid-game
        // enemies instead of ten late-game ranged enemies at once.
        if (index % 2 == 0) {
            switch ((index / 2) % 5) {
                case 0: return new Warlock();
                case 1: return new Monk();
                case 2: return new Golem();
                case 3: return new Succubus();
                default:return new Scorpio();
            }
        }
        switch ((index / 2) % 5) {
            case 0: return new Brute();
            case 1: return new Spinner();
            case 2: return new Bat();
            case 3: return new DM100();
            default:return new Skeleton();
        }
    }

    private class TrialDirector extends Actor {

        {
            actPriority = BUFF_PRIO;
        }

        @Override
        protected boolean act() {
            InfiniteWorldState st = Dungeon.infiniteWorldState;
            if (Dungeon.level != BreakthroughTrialLevel.this || st == null
                    || !st.breakthroughTrialActive) {
                Actor.remove(this);
                return true;
            }

            if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {
                spend(TICK);
                return true;
            }

            if (hasLivingTrialEnemies()) {
                spend(TICK);
                return true;
            }

            if (st.breakthroughWave >= TOTAL_WAVES) {
                Actor.remove(this);
                finishTrial(true);
                return true;
            }

            if (st.breakthroughWave == 0) {
                spawnWave(1);
                spend(TICK);
                return true;
            }

            if (st.breakthroughCountdown < 0f) {
                st.breakthroughCountdown = BETWEEN_WAVE_COUNTDOWN;
                GLog.i("本波已清空。下一波将在30个行动值后到来。");
            }

            int before = (int)Math.ceil(st.breakthroughCountdown);
            st.breakthroughCountdown = Math.max(0f, st.breakthroughCountdown - TICK);
            int remaining = (int)Math.ceil(st.breakthroughCountdown);

            if (remaining != before
                    && (remaining == 20 || remaining == 10 || remaining == 5
                    || remaining == 4 || remaining == 3 || remaining == 2 || remaining == 1)) {
                GLog.i("突破试炼：下一波倒计时 " + remaining + "。");
            }

            if (st.breakthroughCountdown <= 0f) {
                spawnWave(st.breakthroughWave + 1);
            }

            spend(TICK);
            return true;
        }
    }
}

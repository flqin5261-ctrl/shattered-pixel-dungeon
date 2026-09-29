/*
 * Shattered Pixel Dungeon - Assist Edition
 * Standalone Extra Challenge prototype level.
 *
 * Distributed under the GNU General Public License v3.0 or later,
 * consistent with the upstream project.
 */
package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;

public class ExtraChallengeLevel extends Level {

    private static final int MAP_W = 35;
    private static final int MAP_H = 35;

    {
        // City art is used as a palette only; the geometry is entirely hand-authored.
        color1 = 0x665A86;
        color2 = 0x9A82B8;
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
        Music.INSTANCE.play(Assets.Music.CITY_2, true);
    }

    @Override
    protected boolean build() {
        setSize(MAP_W, MAP_H);

        // ---- Outer loop: a broad rectangular promenade. ----
        fillRect(6, 7, 28, 9, Terrain.EMPTY);
        fillRect(6, 26, 28, 28, Terrain.EMPTY);
        fillRect(6, 7, 8, 28, Terrain.EMPTY);
        fillRect(26, 7, 28, 28, Terrain.EMPTY);

        // ---- Central court and four approach corridors. ----
        fillRect(12, 12, 22, 22, Terrain.EMPTY);
        fillRect(16, 9, 18, 12, Terrain.EMPTY);
        fillRect(16, 22, 18, 26, Terrain.EMPTY);
        fillRect(8, 16, 12, 18, Terrain.EMPTY);
        fillRect(22, 16, 26, 18, Terrain.EMPTY);

        // ---- North exit pavilion. It can only be reached through one locked gate. ----
        fillRect(14, 2, 20, 5, Terrain.EMPTY);
        fillRect(17, 5, 17, 7, Terrain.EMPTY);
        map[cell(17, 6)] = Terrain.LOCKED_DOOR;

        // ---- South entrance pavilion. ----
        fillRect(14, 29, 20, 32, Terrain.EMPTY);
        fillRect(16, 28, 18, 29, Terrain.EMPTY);

        // ---- West archive: shelves + an ordinary door and chest. ----
        fillRect(2, 13, 5, 21, Terrain.EMPTY);
        fillRect(5, 16, 8, 18, Terrain.EMPTY);
        map[cell(6, 17)] = Terrain.DOOR;
        for (int y = 14; y <= 20; y += 2) {
            map[cell(2, y)] = Terrain.BOOKSHELF;
            map[cell(4, y)] = Terrain.BOOKSHELF;
        }

        // ---- East garden: open grass/water study with the iron key on a pedestal. ----
        fillRect(29, 13, 32, 21, Terrain.EMPTY);
        fillRect(28, 16, 30, 18, Terrain.EMPTY);
        map[cell(28, 17)] = Terrain.DOOR;
        fillRect(30, 14, 31, 15, Terrain.WATER);
        fillRect(30, 19, 31, 20, Terrain.HIGH_GRASS);
        map[cell(31, 17)] = Terrain.PEDESTAL;

        // ---- Central "mirror" court: a water ring surrounding a grassy island. ----
        for (int x = 13; x <= 21; x++) {
            map[cell(x, 13)] = Terrain.WATER;
            map[cell(x, 21)] = Terrain.WATER;
        }
        for (int y = 13; y <= 21; y++) {
            map[cell(13, y)] = Terrain.WATER;
            map[cell(21, y)] = Terrain.WATER;
        }

        // Four bridges make the center a loop rather than a single corridor.
        map[cell(17, 13)] = Terrain.EMPTY;
        map[cell(17, 21)] = Terrain.EMPTY;
        map[cell(13, 17)] = Terrain.EMPTY;
        map[cell(21, 17)] = Terrain.EMPTY;

        fillRect(15, 15, 19, 19, Terrain.HIGH_GRASS);
        for (int x = 15; x <= 19; x++) map[cell(x, 17)] = Terrain.EMPTY;
        for (int y = 15; y <= 19; y++) map[cell(17, y)] = Terrain.EMPTY;
        map[cell(17, 17)] = Terrain.EMPTY_DECO;

        // Decorative breaks in the long promenade stop it from reading like a normal corridor.
        map[cell(10, 8)] = Terrain.STATUE;
        map[cell(24, 8)] = Terrain.STATUE;
        map[cell(10, 27)] = Terrain.STATUE;
        map[cell(24, 27)] = Terrain.STATUE;

        int entrance = cell(17, 31);
        int exit = cell(17, 3);

        map[entrance] = Terrain.ENTRANCE;
        map[exit] = Terrain.EXIT;

        transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.REGULAR_ENTRANCE));
        transitions.add(new LevelTransition(this, exit, LevelTransition.Type.REGULAR_EXIT));

        return true;
    }

    private int cell(int x, int y) {
        return y * width() + x;
    }

    private void fillRect(int left, int top, int right, int bottom, int terrain) {
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                map[cell(x, y)] = terrain;
            }
        }
    }

    @Override
    public Mob createMob() {
        return null;
    }

    @Override
    protected void createMobs() {
        // v0.2 prototype intentionally contains no enemies.
    }

    @Override
    public Actor addRespawner() {
        return null;
    }

    @Override
    protected void createItems() {
        // The gate key uses the original depth-aware key/door interaction.
        drop(new IronKey(Dungeon.depth), cell(31, 17));

        Heap archiveChest = drop(new PotionOfHealing(), cell(3, 17));
        archiveChest.type = Heap.Type.CHEST;

        Heap courtChest = drop(new ScrollOfMagicMapping(), cell(17, 17));
        courtChest.type = Heap.Type.CHEST;
    }

    @Override
    public int randomRespawnCell(Char ch) {
        return entrance();
    }

    @Override
    public boolean activateTransition(Hero hero, LevelTransition transition) {
        // The entrance transition is needed as a spawn anchor, but in this standalone
        // mode it must never behave like a staircase back to "floor 0".
        if (transition.type == LevelTransition.Type.REGULAR_ENTRANCE) {
            return false;
        }

        if (transition.type != LevelTransition.Type.REGULAR_EXIT) {
            return super.activateTransition(hero, transition);
        }

        // This mode is deliberately one floor only. Finishing it never falls through
        // into depth 2 of the original dungeon.
        Game.runOnRenderThread(new Callback() {
            @Override
            public void call() {
                GameScene.show(new WndOptions(
                        "额外挑战 · 镜庭",
                        "你已经抵达镜庭出口。这个测试挑战目前只有这一层。",
                        "完成并返回主界面",
                        "继续探索") {
                    @Override
                    protected void onSelect(int index) {
                        if (index == 0) {
                            Level.beforeTransition();
                            Dungeon.deleteGame(GamesInProgress.EXTRA_CHALLENGE_SLOT, true);
                            Dungeon.extraChallenge = false;
                            HeroSelectScene.extraChallengeMode = false;
                            GamesInProgress.curSlot = 0;
                            Game.switchScene(TitleScene.class);
                        }
                    }
                });
            }
        });
        return false;
    }
}

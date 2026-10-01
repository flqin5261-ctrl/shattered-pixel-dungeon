/*
 * Shattered Pixel Dungeon - Assist Edition
 * Seasonal visual-only ground overlay for Infinite World.
 */
package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldCycle;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldState;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

import java.util.Arrays;

/**
 * Uses the existing Assist materials atlas as a lightweight seasonal skin.
 * It never changes Terrain, passability, water state or world generation.
 */
public class InfiniteWorldSeasonTilemap extends CustomTilemap {

    private static final String SALT = "iw_season_salt";

    private int salt;

    public InfiniteWorldSeasonTilemap() {
        texture = Assets.Environment.ASSIST_BACKROOMS_MATERIALS;
    }

    public InfiniteWorldSeasonTilemap(int salt) {
        this.salt = salt;
        texture = Assets.Environment.ASSIST_BACKROOMS_MATERIALS;
    }

    @Override
    public Tilemap create() {
        texture = Assets.Environment.ASSIST_BACKROOMS_MATERIALS;
        Tilemap v = super.create();
        v.map(buildData(), tileW);
        return v;
    }

    public void refresh() {
        if (vis == null || !vis.alive) return;
        vis.map(buildData(), tileW);
        vis.flushMapUpdate();
    }

    private int[] buildData() {
        int[] data = new int[tileW * tileH];
        Arrays.fill(data, -1);

        if (Dungeon.level == null || Dungeon.infiniteWorldState == null) return data;

        InfiniteWorldState state = Dungeon.infiniteWorldState;
        InfiniteWorldCycle.ensureInitialized(state, Dungeon.seed);
        int season = InfiniteWorldCycle.season(state);
        int weather = state.worldWeather;

        for (int y = 0; y < tileH; y++) {
            for (int x = 0; x < tileW; x++) {
                int gx = tileX + x;
                int gy = tileY + y;
                if (gx < 0 || gy < 0 || gx >= Dungeon.level.width() || gy >= Dungeon.level.height()) continue;

                int pos = gx + gy * Dungeon.level.width();
                int terrain = Dungeon.level.map[pos];
                int visual = seasonalVisual(terrain, gx, gy, season, weather);
                if (visual >= 0) data[x + y * tileW] = visual;
            }
        }

        return data;
    }

    private int seasonalVisual(int terrain, int x, int y, int season, int weather) {
        boolean grass = terrain == Terrain.GRASS
                || terrain == Terrain.HIGH_GRASS
                || terrain == Terrain.FURROWED_GRASS;
        boolean floor = terrain == Terrain.EMPTY
                || terrain == Terrain.EMPTY_SP
                || terrain == Terrain.EMPTY_DECO
                || terrain == Terrain.CUSTOM_DECO_EMPTY;

        if (!grass && !floor) return -1;

        int roll = noise(x, y, 100);

        switch (season) {
            case InfiniteWorldCycle.SEASON_SPRING:
                if (grass && roll < 65) return variation(26, 27, x, y, 24);
                return -1;

            case InfiniteWorldCycle.SEASON_SUMMER:
                if (grass && roll < 78) return variation(27, 26, x, y, 18);
                return -1;

            case InfiniteWorldCycle.SEASON_AUTUMN:
                if (grass && roll < 92) return variation(18, 20, x, y, 30);
                if (floor && roll < 26) return variation(18, 20, x, y, 50);
                return -1;

            case InfiniteWorldCycle.SEASON_WINTER:
            default:
                int coverage;
                if (weather == InfiniteWorldCycle.WEATHER_SNOW) coverage = 96;
                else if (weather == InfiniteWorldCycle.WEATHER_RAIN) coverage = 32;
                else coverage = 64;

                if (roll < coverage) {
                    // Poolrooms white/cool material cells double as a snow-like
                    // visual skin without adding a second large texture atlas.
                    return variation(4, 5, x, y, 24);
                }
                return -1;
        }
    }

    private int variation(int primary, int alternate, int x, int y, int altPercent) {
        return noise(x + 19, y - 11, 100) < altPercent ? alternate : primary;
    }

    private int noise(int x, int y, int bound) {
        int h = salt;
        h ^= x * 0x45d9f3b;
        h = Integer.rotateLeft(h, 11);
        h ^= y * 0x119de1f3;
        h ^= h >>> 16;
        return Math.floorMod(h, bound);
    }

    @Override
    public String name(int tileX, int tileY) {
        return null;
    }

    @Override
    public String desc(int tileX, int tileY) {
        return null;
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(SALT, salt);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        salt = bundle.getInt(SALT);
        texture = Assets.Environment.ASSIST_BACKROOMS_MATERIALS;
    }
}

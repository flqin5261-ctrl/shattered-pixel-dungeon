/*
 * Shattered Pixel Dungeon - Assist Edition
 * CC0 urban floor overlay for Infinite World anomaly districts.
 */
package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

import java.util.Arrays;

/**
 * Visual-only 16x16 floor material from Kenney RPG Urban Pack (CC0).
 * It intentionally paints only ordinary floor terrain, leaving doors, water,
 * traps, grass, chasms and other gameplay-significant visuals untouched.
 */
public class InfiniteWorldUrbanSurfaceLayer extends CustomTilemap {

    private static final String PRIMARY = "iw_urban_primary";
    private static final String ALT = "iw_urban_alt";
    private static final String SALT = "iw_urban_salt";
    private static final String ALT_PERCENT = "iw_urban_alt_percent";

    private int primaryVisual = 109;
    private int altVisual = 114;
    private int salt;
    private int altPercent = 12;

    public InfiniteWorldUrbanSurfaceLayer() {
        texture = Assets.Environment.ASSIST_KENNEY_RPG_URBAN;
    }

    public InfiniteWorldUrbanSurfaceLayer(int primaryVisual, int altVisual,
                                          int salt, int altPercent) {
        this.primaryVisual = primaryVisual;
        this.altVisual = altVisual;
        this.salt = salt;
        this.altPercent = Math.max(0, Math.min(100, altPercent));
        texture = Assets.Environment.ASSIST_KENNEY_RPG_URBAN;
    }

    @Override
    public Tilemap create() {
        texture = Assets.Environment.ASSIST_KENNEY_RPG_URBAN;
        Tilemap v = super.create();

        int[] data = new int[tileW * tileH];
        Arrays.fill(data, -1);

        if (Dungeon.level != null) {
            for (int y = 0; y < tileH; y++) {
                for (int x = 0; x < tileW; x++) {
                    int gx = tileX + x;
                    int gy = tileY + y;
                    if (gx < 0 || gy < 0
                            || gx >= Dungeon.level.width() || gy >= Dungeon.level.height()) {
                        continue;
                    }

                    int pos = gx + gy * Dungeon.level.width();
                    int terrain = Dungeon.level.map[pos];
                    if (!ordinaryFloor(terrain)) continue;

                    int h = salt;
                    h ^= x * 0x45d9f3b;
                    h = Integer.rotateLeft(h, 11);
                    h ^= y * 0x119de1f3;
                    h ^= h >>> 16;

                    data[x + y * tileW] = Math.floorMod(h, 100) < altPercent
                            ? altVisual : primaryVisual;
                }
            }
        }

        v.map(data, tileW);
        return v;
    }

    private boolean ordinaryFloor(int terrain) {
        return terrain == Terrain.EMPTY
                || terrain == Terrain.EMPTY_SP
                || terrain == Terrain.EMPTY_DECO;
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
        bundle.put(PRIMARY, primaryVisual);
        bundle.put(ALT, altVisual);
        bundle.put(SALT, salt);
        bundle.put(ALT_PERCENT, altPercent);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        primaryVisual = bundle.getInt(PRIMARY);
        altVisual = bundle.getInt(ALT);
        salt = bundle.getInt(SALT);
        altPercent = bundle.getInt(ALT_PERCENT);
        texture = Assets.Environment.ASSIST_KENNEY_RPG_URBAN;
    }
}

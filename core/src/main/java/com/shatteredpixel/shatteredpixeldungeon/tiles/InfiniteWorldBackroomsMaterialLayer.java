/*
 * Shattered Pixel Dungeon - Assist Edition
 * Purpose-built visual material overlay for Infinite World Backrooms districts.
 */
package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

import java.util.Arrays;

public class InfiniteWorldBackroomsMaterialLayer extends CustomTilemap {

    public static final int MODE_GROUND = 0;
    public static final int MODE_WALLS = 1;

    private static final String SCHEME = "iw_br_scheme";
    private static final String SALT = "iw_br_salt";
    private static final String MODE = "iw_br_mode";

    private int scheme;
    private int salt;
    private int mode;

    public InfiniteWorldBackroomsMaterialLayer() {
        texture = Assets.Environment.ASSIST_BACKROOMS_MATERIALS;
    }

    public InfiniteWorldBackroomsMaterialLayer(int scheme, int salt, int mode) {
        this.scheme = scheme;
        this.salt = salt;
        this.mode = mode;
        texture = Assets.Environment.ASSIST_BACKROOMS_MATERIALS;
    }

    @Override
    public Tilemap create() {
        texture = Assets.Environment.ASSIST_BACKROOMS_MATERIALS;
        Tilemap v = super.create();
        v.map(buildData(), tileW);
        return v;
    }

    private int[] buildData() {
        int[] data = new int[tileW * tileH];
        Arrays.fill(data, -1);
        if (Dungeon.level == null) return data;

        for (int y = 0; y < tileH; y++) {
            for (int x = 0; x < tileW; x++) {
                int gx = tileX + x;
                int gy = tileY + y;
                if (gx < 0 || gy < 0 || gx >= Dungeon.level.width() || gy >= Dungeon.level.height()) continue;

                int pos = gx + gy * Dungeon.level.width();
                int terrain = Dungeon.level.map[pos];
                int visual = mode == MODE_WALLS
                        ? wallVisual(terrain, gx, gy)
                        : groundVisual(terrain, gx, gy);
                if (visual >= 0) data[x + y * tileW] = visual;
            }
        }
        return data;
    }

    private int groundVisual(int terrain, int x, int y) {
        if (terrain == Terrain.WATER) {
            switch (scheme) {
                case 1:  return variation(1, 0, x, y, 20); // damp yellow carpet, never red water
                case 8:  return variation(6, 7, x, y, 24);
                case 9:  return variation(15, 7, x, y, 18);
                case 13: return variation(6, 7, x, y, 18); // Poolrooms: bright blue
                case 17: return variation(15, 7, x, y, 12); // sewer channel
                case 19: return variation(6, 7, x, y, 20); // sunset beach
                default: return variation(6, 7, x, y, 15);
            }
        }

        if (terrain == Terrain.GRASS || terrain == Terrain.HIGH_GRASS || terrain == Terrain.FURROWED_GRASS) {
            switch (scheme) {
                case 10: return variation(26, 18, x, y, 18);
                case 11: return variation(18, 26, x, y, 28);
                case 14: return variation(26, 18, x, y, 22);
                case 19: return variation(26, 28, x, y, 12);
                default: return variation(26, 18, x, y, 12);
            }
        }

        if (terrain != Terrain.EMPTY && terrain != Terrain.EMPTY_SP
                && terrain != Terrain.EMPTY_DECO && terrain != Terrain.EMBERS
                && terrain != Terrain.CUSTOM_DECO_EMPTY) {
            return -1;
        }

        switch (scheme) {
            case 1:  return variation(0, 1, x, y, 18);
            case 2:  return variation(8, 12, x, y, 12);
            case 3:  return variation(11, 12, x, y, 14);
            case 4:  return variation(12, 9, x, y, 12);
            case 5:  return variation(8, 12, x, y, 16);
            case 6:  return variation(10, 20, x, y, 20);
            case 7:  return variation(14, 9, x, y, 10);
            case 8:  return variation(15, 17, x, y, 12);
            case 9:  return variation(16, 17, x, y, 20);
            case 10: return variation(19, 23, x, y, 15);
            case 11: return variation(20, 18, x, y, 15);
            case 12: return variation(19, 23, x, y, 18);
            case 13: return variation(4, 5, x, y, 16);
            case 14: return variation(18, 26, x, y, 18);
            case 15: return variation(10, 28, x, y, 18);
            case 16: return variation(22, 30, x, y, 24);
            case 17: return variation(11, 17, x, y, 12);
            case 18: return variation(24, 15, x, y, 18);
            case 19: return variation(28, 30, x, y, 15);
            case 20: return variation(21, 22, x, y, 22);
            default: return -1;
        }
    }

    private int wallVisual(int terrain, int x, int y) {
        if (terrain != Terrain.WALL && terrain != Terrain.WALL_DECO) return -1;

        switch (scheme) {
            case 1:  return variation(2, 3, x, y, 16);
            case 2:  return variation(8, 9, x, y, 12);
            case 3:  return variation(9, 11, x, y, 12);
            case 4:  return variation(9, 14, x, y, 10);
            case 5:  return variation(4, 5, x, y, 12);
            case 6:  return variation(20, 22, x, y, 14);
            case 7:  return variation(14, 9, x, y, 10);
            case 8:  return variation(15, 17, x, y, 12);
            case 9:  return variation(17, 16, x, y, 14);
            case 10: return variation(20, 28, x, y, 14);
            case 11: return variation(20, 18, x, y, 14);
            case 12: return variation(8, 9, x, y, 14);
            case 13: return variation(4, 5, x, y, 14);
            case 14: return variation(22, 28, x, y, 16);
            case 15: return variation(22, 28, x, y, 18);
            case 16: return variation(22, 4, x, y, 18);
            case 17: return variation(17, 11, x, y, 12);
            case 18: return variation(9, 14, x, y, 14);
            case 19: return variation(28, 20, x, y, 15);
            case 20: return variation(13, 21, x, y, 18);
            default: return -1;
        }
    }

    private int variation(int primary, int alternate, int x, int y, int altPercent) {
        int h = salt;
        h ^= x * 0x45d9f3b;
        h = Integer.rotateLeft(h, 11);
        h ^= y * 0x119de1f3;
        h ^= h >>> 16;
        return Math.floorMod(h, 100) < altPercent ? alternate : primary;
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
        bundle.put(SCHEME, scheme);
        bundle.put(SALT, salt);
        bundle.put(MODE, mode);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        scheme = bundle.getInt(SCHEME);
        salt = bundle.getInt(SALT);
        mode = bundle.getInt(MODE);
        texture = Assets.Environment.ASSIST_BACKROOMS_MATERIALS;
    }
}

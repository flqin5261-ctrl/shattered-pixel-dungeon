/*
 * Shattered Pixel Dungeon - Assist Edition
 * Mixed-material floor overlay for Infinite World.
 */
package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

import java.util.Arrays;

/**
 * Paints deterministic patches of ordinary floor using a foreign region tilesheet.
 * It intentionally avoids walls, water, doors and raised terrain so the overlay
 * cannot change collision or stitching logic; it is visual-only.
 */
public class InfiniteWorldAccentTilemap extends CustomTilemap {

    private String textureName;
    private int salt;

    public InfiniteWorldAccentTilemap() {
        // public zero-arg constructor required by Bundlable restore
    }

    public InfiniteWorldAccentTilemap(String textureName, int salt) {
        this.textureName = textureName;
        this.texture = textureName;
        this.salt = salt;
    }

    @Override
    public Tilemap create() {
        texture = textureName;
        Tilemap v = super.create();

        int[] data = new int[tileW * tileH];
        Arrays.fill(data, -1);

        if (Dungeon.level == null || textureName == null) {
            v.map(data, tileW);
            return v;
        }

        for (int y = 0; y < tileH; y++) {
            for (int x = 0; x < tileW; x++) {
                int gx = tileX + x;
                int gy = tileY + y;
                if (gx < 0 || gy < 0 || gx >= Dungeon.level.width() || gy >= Dungeon.level.height()) continue;

                int pos = gx + gy * Dungeon.level.width();
                int terrain = Dungeon.level.map[pos];

                // Keep this overlay deliberately conservative. These floor types do
                // not carry raised objects or water/wall stitching.
                if (terrain != Terrain.EMPTY && terrain != Terrain.EMPTY_DECO && terrain != Terrain.EMPTY_SP) continue;

                // Coarse 4x4 noise produces readable material patches rather than
                // confetti. The same chunk always gets the same patch shape.
                int bx = x >> 2;
                int by = y >> 2;
                int h = salt;
                h ^= bx * 0x45d9f3b;
                h = Integer.rotateLeft(h, 13);
                h ^= by * 0x119de1f3;
                h ^= h >>> 16;
                if (Math.floorMod(h, 100) >= 54) continue;

                int visual = DungeonTileSheet.directVisuals.get(terrain, -1);
                if (visual >= 0) {
                    data[x + y * tileW] = DungeonTileSheet.getVisualWithAlts(visual, pos);
                }
            }
        }

        v.map(data, tileW);
        return v;
    }

    @Override
    public String name(int tileX, int tileY) {
        return null;
    }

    @Override
    public String desc(int tileX, int tileY) {
        return null;
    }

    private static final String TEXTURE = "iw_texture";
    private static final String SALT = "iw_salt";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(TEXTURE, textureName);
        bundle.put(SALT, salt);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        textureName = bundle.getString(TEXTURE);
        texture = textureName;
        salt = bundle.getInt(SALT);
    }
}

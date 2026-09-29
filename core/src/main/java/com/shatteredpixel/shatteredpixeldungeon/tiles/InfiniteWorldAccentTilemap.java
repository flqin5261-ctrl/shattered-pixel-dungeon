/*
 * Shattered Pixel Dungeon - Assist Edition
 * Mixed-material overlays for Infinite World.
 */
package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

import java.util.Arrays;

/**
 * Deterministic visual-only chunk overlay. Older worlds use sparse floor patches;
 * v6 can also use full floor and raised/wall overlays so neighboring world regions
 * can visibly use different upstream environment tilesheets without changing
 * collision or logical terrain.
 */
public class InfiniteWorldAccentTilemap extends CustomTilemap {

    public static final int MODE_FLOOR = 0;
    public static final int MODE_WALLS = 1;

    private String textureName;
    private int salt;
    private int coverage = 54;
    private int mode = MODE_FLOOR;

    public InfiniteWorldAccentTilemap() {
        // public zero-arg constructor required by Bundlable restore
    }

    public InfiniteWorldAccentTilemap(String textureName, int salt) {
        this(textureName, salt, 54, MODE_FLOOR);
    }

    public InfiniteWorldAccentTilemap(String textureName, int salt, int coverage, int mode) {
        this.textureName = textureName;
        this.texture = textureName;
        this.salt = salt;
        this.coverage = Math.max(0, Math.min(100, coverage));
        this.mode = mode;
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

                if (!includedByPatchNoise(x, y)) continue;

                int visual = mode == MODE_WALLS
                        ? raisedVisual(pos, terrain)
                        : floorVisual(pos, terrain);

                if (visual >= 0) data[x + y * tileW] = visual;
            }
        }

        v.map(data, tileW);
        return v;
    }

    private boolean includedByPatchNoise(int x, int y) {
        if (coverage >= 100) return true;
        int bx = x >> 2;
        int by = y >> 2;
        int h = salt;
        h ^= bx * 0x45d9f3b;
        h = Integer.rotateLeft(h, 13);
        h ^= by * 0x119de1f3;
        h ^= h >>> 16;
        return Math.floorMod(h, 100) < coverage;
    }

    private int floorVisual(int pos, int terrain) {
        // Doors can change state at runtime (locked -> closed -> open, secret ->
        // revealed). Leave them to the base dynamic tilemaps so a static accent
        // layer can never leave the old door sprite painted on top.
        if (mutableDoorTerrain(terrain)) return -1;

        // Reproduce the normal terrain layer with another upstream tilesheet.
        // Water stays on the world's animated water backdrop.
        int visual = DungeonTileSheet.directVisuals.get(terrain, -1);
        if (visual >= 0 && terrain != Terrain.CUSTOM_DECO_WTR) {
            return DungeonTileSheet.getVisualWithAlts(visual, pos);
        }
        if (terrain == Terrain.WATER || terrain == Terrain.CHASM) return -1;

        int width = Dungeon.level.width();
        int right = (pos + 1) % width != 0 ? Dungeon.level.map[pos + 1] : -1;
        int below = pos + width < Dungeon.level.length() ? Dungeon.level.map[pos + width] : -1;
        int left = pos % width != 0 ? Dungeon.level.map[pos - 1] : -1;
        int above = pos >= width ? Dungeon.level.map[pos - width] : -1;

        if (DungeonTileSheet.doorTile(terrain)) {
            return DungeonTileSheet.getRaisedDoorTile(terrain, above);
        } else if (DungeonTileSheet.wallStitcheable(terrain)) {
            return DungeonTileSheet.getRaisedWallTile(terrain, pos, right, below, left);
        } else if (terrain == Terrain.STATUE) {
            return DungeonTileSheet.RAISED_STATUE;
        } else if (terrain == Terrain.STATUE_SP) {
            return DungeonTileSheet.RAISED_STATUE_SP;
        } else if (terrain == Terrain.REGION_DECO) {
            return DungeonTileSheet.RAISED_REGION_DECO;
        } else if (terrain == Terrain.REGION_DECO_ALT) {
            return DungeonTileSheet.RAISED_REGION_DECO_ALT;
        } else if (terrain == Terrain.MINE_CRYSTAL) {
            return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_MINE_CRYSTAL_BLUE_1, pos);
        } else if (terrain == Terrain.MINE_BOULDER) {
            return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_MINE_BOULDER, pos);
        } else if (terrain == Terrain.ALCHEMY) {
            return DungeonTileSheet.RAISED_ALCHEMY_POT;
        } else if (terrain == Terrain.BARRICADE) {
            return DungeonTileSheet.RAISED_BARRICADE;
        }

        // High grass uses a separate raised-terrain atlas in the vanilla renderer,
        // so leave it alone rather than forcing an incompatible region tilesheet.
        return -1;
    }

    private int raisedVisual(int pos, int terrain) {
        if (mutableDoorTerrain(terrain)) return -1;

        // Reproduce DungeonWallsTilemap's upper wall/overhang layer with the
        // alternate region texture. Together with floorVisual this swaps the
        // visible wall material, not just the floor beneath it.
        int width = Dungeon.level.width();
        int size = Dungeon.level.length();
        int[] map = Dungeon.level.map;

        if (DungeonTileSheet.wallStitcheable(terrain)) {
            if (pos + width < size && !DungeonTileSheet.wallStitcheable(map[pos + width])) {
                int below = map[pos + width];
                if (mutableDoorTerrain(below)) return -1;
            } else {
                return DungeonTileSheet.stitchInternalWallTile(
                        terrain,
                        (pos + 1) % width != 0 ? map[pos + 1] : -1,
                        (pos + 1) % width != 0 && pos + width < size ? map[pos + 1 + width] : -1,
                        pos + width < size ? map[pos + width] : -1,
                        pos % width != 0 && pos + width < size ? map[pos - 1 + width] : -1,
                        pos % width != 0 ? map[pos - 1] : -1);
            }
        }

        if (terrain == Terrain.LOCKED_EXIT || terrain == Terrain.UNLOCKED_EXIT) {
            return DungeonTileSheet.EXIT_UNDERHANG;
        } else if (pos + width < size && DungeonTileSheet.wallStitcheable(map[pos + width])) {
            return DungeonTileSheet.stitchWallOverhangTile(
                    terrain,
                    (pos + 1) % width != 0 ? map[pos + 1 + width] : -1,
                    map[pos + width],
                    pos % width != 0 ? map[pos - 1 + width] : -1);
        } else if (pos + width < size) {
            int below = map[pos + width];
            if (mutableDoorTerrain(below)) return -1;
            if (below == Terrain.STATUE) return DungeonTileSheet.STATUE_OVERHANG;
            if (below == Terrain.STATUE_SP) return DungeonTileSheet.STATUE_SP_OVERHANG;
            if (below == Terrain.REGION_DECO) return DungeonTileSheet.REGION_DECO_OVERHANG;
            if (below == Terrain.REGION_DECO_ALT) return DungeonTileSheet.REGION_DECO_ALT_OVERHANG;
            if (below == Terrain.MINE_CRYSTAL) {
                return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.MINE_CRYSTAL_OVERHANG_BLUE, pos + width);
            }
            if (below == Terrain.MINE_BOULDER) {
                return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.MINE_BOULDER_OVERHANG, pos + width);
            }
            if (below == Terrain.ALCHEMY) return DungeonTileSheet.ALCHEMY_POT_OVERHANG;
            if (below == Terrain.BARRICADE) return DungeonTileSheet.BARRICADE_OVERHANG;
        }

        return -1;
    }

    private boolean mutableDoorTerrain(int terrain) {
        return terrain == Terrain.DOOR
                || terrain == Terrain.OPEN_DOOR
                || terrain == Terrain.LOCKED_DOOR
                || terrain == Terrain.HERO_LKD_DR
                || terrain == Terrain.CRYSTAL_DOOR
                || terrain == Terrain.SECRET_DOOR
                || terrain == Terrain.LOCKED_EXIT
                || terrain == Terrain.UNLOCKED_EXIT;
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
    private static final String COVERAGE = "iw_coverage";
    private static final String MODE = "iw_mode";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(TEXTURE, textureName);
        bundle.put(SALT, salt);
        bundle.put(COVERAGE, coverage);
        bundle.put(MODE, mode);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        textureName = bundle.getString(TEXTURE);
        texture = textureName;
        salt = bundle.getInt(SALT);
        coverage = bundle.contains(COVERAGE) ? bundle.getInt(COVERAGE) : 54;
        mode = bundle.contains(MODE) ? bundle.getInt(MODE) : MODE_FLOOR;
    }
}

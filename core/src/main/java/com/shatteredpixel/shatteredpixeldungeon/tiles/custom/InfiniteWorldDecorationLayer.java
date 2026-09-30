/*
 * Shattered Pixel Dungeon - Assist Edition
 *
 * Infinite World environment props use Kenney Tiny Town / Tiny Dungeon assets
 * under CC0 1.0. See docs/assist/THIRD_PARTY_ASSETS.md.
 */
package com.shatteredpixel.shatteredpixeldungeon.tiles.custom;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

import java.util.Arrays;

public class InfiniteWorldDecorationLayer extends CustomTilemap {

    public static final int SOURCE_TOWN = 0;
    public static final int SOURCE_DUNGEON = 1;

    // Keep the original numeric ids 1..5 stable for old 0.5.8 save bundles.
    public static final int ROUND_TREE = 1;
    public static final int BUSH = ROUND_TREE; // legacy code alias
    public static final int MUSHROOMS = 2;
    public static final int SIGN = 3;
    public static final int BARREL = 4;
    public static final int CASKET = 5;
    public static final int CRATE = CASKET; // legacy code alias

    public static final int PINE_TREE_GREEN = 6;
    public static final int PINE_TREE_AUTUMN = 7;
    public static final int ROUND_TREE_AUTUMN = 8;
    public static final int FERN = 9;
    public static final int FENCE_POST = 10;
    public static final int FENCE_RAIL = 11;
    public static final int WARNING_SIGN = 12;
    public static final int ROCK = 13;
    public static final int LOG = 14;
    public static final int EMPTY_TUB = 15;
    public static final int WATER_TROUGH = 16;
    public static final int WOODEN_CRATE = 17;
    public static final int WOODEN_POST = 18;

    public static final int TORCH = 19;
    public static final int RUBBLE = 20;
    public static final int BOOKSHELF = 21;
    public static final int STONE_CROSS = 22;
    public static final int GRAVESTONE = 23;
    public static final int TABLE = 24;
    public static final int STOOL = 25;
    public static final int STONE_BASIN = 26;
    public static final int CUPBOARD = 27;
    public static final int IRON_RAIL = 28;
    public static final int CAMPFIRE = 29;
    public static final int WEAPON_RACK = 30;

    private static final String SOURCE = "source";
    private static final String CELLS = "cells";
    private static final String KINDS = "kinds";

    private int source = SOURCE_TOWN;
    private final SparseArray<Integer> kinds = new SparseArray<>();

    public InfiniteWorldDecorationLayer() {
        updateTexture();
    }

    public InfiniteWorldDecorationLayer(int source, int width, int height) {
        this.source = source;
        setRect(0, 0, width, height);
        updateTexture();
    }

    public static int sourceForKind(int kind) {
        switch (kind) {
            case CASKET:
            case TORCH:
            case RUBBLE:
            case BOOKSHELF:
            case STONE_CROSS:
            case GRAVESTONE:
            case TABLE:
            case STOOL:
            case STONE_BASIN:
            case CUPBOARD:
            case IRON_RAIL:
            case CAMPFIRE:
            case WEAPON_RACK:
                return SOURCE_DUNGEON;
            default:
                return SOURCE_TOWN;
        }
    }

    /**
     * Generator V16 kept a few tiny ground-detail props pass-through. Generator
     * V17 makes every visible environment prop physical, as requested. Keeping
     * the version parameter prevents old V16 saves from gaining new blockers.
     */
    public static boolean isBlockingKind(int kind, int generatorVersion) {
        if (kind <= 0) return false;
        if (generatorVersion >= 17) return true;

        switch (kind) {
            case MUSHROOMS:
            case FERN:
            case TORCH:
            case RUBBLE:
                return false;

            default:
                return true;
        }
    }

    // Legacy helper retained for any older call sites: V16 semantics.
    public static boolean isBlockingKind(int kind) {
        return isBlockingKind(kind, 16);
    }

    public static boolean isClusterFriendly(int kind) {
        switch (kind) {
            case ROUND_TREE:
            case MUSHROOMS:
            case BARREL:
            case PINE_TREE_GREEN:
            case PINE_TREE_AUTUMN:
            case ROUND_TREE_AUTUMN:
            case FERN:
            case FENCE_POST:
            case FENCE_RAIL:
            case ROCK:
            case LOG:
            case WOODEN_CRATE:
            case WOODEN_POST:
            case RUBBLE:
            case IRON_RAIL:
                return true;
            default:
                return false;
        }
    }

    private void updateTexture() {
        texture = source == SOURCE_DUNGEON
                ? Assets.Environment.ASSIST_KENNEY_TINY_DUNGEON
                : Assets.Environment.ASSIST_KENNEY_TINY_TOWN;
    }

    public boolean isEmpty() {
        return kinds.size == 0;
    }

    public void put(int cell, int kind, Level level) {
        int x = cell % level.width();
        int y = cell / level.width();
        kinds.put(x + tileW * y, kind);
    }

    private int kindAt(int x, int y) {
        if (x < 0 || y < 0 || x >= tileW || y >= tileH) return 0;
        Integer kind = kinds.get(x + tileW * y);
        return kind == null ? 0 : kind;
    }

    private int visualForKind(int kind) {
        if (source == SOURCE_TOWN) {
            switch (kind) {
                case ROUND_TREE:         return 5;
                case MUSHROOMS:          return 29;
                case SIGN:               return 83;
                case BARREL:             return 107;
                case PINE_TREE_GREEN:    return 4;
                case PINE_TREE_AUTUMN:   return 10;
                case ROUND_TREE_AUTUMN:  return 11;
                case FERN:               return 17;
                case FENCE_POST:         return 59;
                case FENCE_RAIL:         return 81;
                case WARNING_SIGN:       return 95;
                case ROCK:               return 105;
                case LOG:                return 106;
                case EMPTY_TUB:          return 130;
                case WATER_TROUGH:       return 131;
                case WOODEN_CRATE:       return 57;
                case WOODEN_POST:        return 71;
                default:                 return -1;
            }
        } else {
            switch (kind) {
                case CASKET:       return 66;
                case TORCH:        return 29;
                case RUBBLE:       return 42;
                case BOOKSHELF:    return 63;
                case STONE_CROSS:  return 64;
                case GRAVESTONE:   return 65;
                case TABLE:        return 72;
                case STOOL:        return 73;
                case STONE_BASIN:  return 74;
                case CUPBOARD:     return 75;
                case IRON_RAIL:    return 79;
                case CAMPFIRE:     return 120;
                case WEAPON_RACK:  return 122;
                default:           return -1;
            }
        }
    }

    private String keyForKind(int kind) {
        switch (kind) {
            case ROUND_TREE:         return "round_tree";
            case MUSHROOMS:          return "mushrooms";
            case SIGN:               return "sign";
            case BARREL:             return "barrel";
            case CASKET:             return "casket";
            case PINE_TREE_GREEN:    return "pine_tree_green";
            case PINE_TREE_AUTUMN:   return "pine_tree_autumn";
            case ROUND_TREE_AUTUMN:  return "round_tree_autumn";
            case FERN:               return "fern";
            case FENCE_POST:         return "fence_post";
            case FENCE_RAIL:         return "fence_rail";
            case WARNING_SIGN:       return "warning_sign";
            case ROCK:               return "rock";
            case LOG:                return "log";
            case EMPTY_TUB:          return "empty_tub";
            case WATER_TROUGH:       return "water_trough";
            case WOODEN_CRATE:       return "wooden_crate";
            case WOODEN_POST:        return "wooden_post";
            case TORCH:              return "torch";
            case RUBBLE:             return "rubble";
            case BOOKSHELF:          return "bookshelf";
            case STONE_CROSS:        return "stone_cross";
            case GRAVESTONE:         return "gravestone";
            case TABLE:              return "table";
            case STOOL:              return "stool";
            case STONE_BASIN:        return "stone_basin";
            case CUPBOARD:           return "cupboard";
            case IRON_RAIL:          return "iron_rail";
            case CAMPFIRE:           return "campfire";
            case WEAPON_RACK:        return "weapon_rack";
            default:                 return null;
        }
    }

    @Override
    public Tilemap create() {
        updateTexture();
        Tilemap v = super.create();
        int[] data = new int[tileW * tileH];
        Arrays.fill(data, -1);
        for (int cell : kinds.keyArray()) {
            int visual = visualForKind(kinds.get(cell));
            if (visual >= 0) data[cell] = visual;
        }
        v.map(data, tileW);
        return v;
    }

    @Override
    public String name(int x, int y) {
        String key = keyForKind(kindAt(x, y));
        return key == null ? null : Messages.get(this, key + "_name");
    }

    @Override
    public String desc(int x, int y) {
        String key = keyForKind(kindAt(x, y));
        return key == null ? null : Messages.get(this, key + "_desc");
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(SOURCE, source);

        int[] cells = kinds.keyArray();
        int[] values = new int[cells.length];
        for (int i = 0; i < cells.length; i++) values[i] = kinds.get(cells[i]);
        bundle.put(CELLS, cells);
        bundle.put(KINDS, values);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        source = bundle.getInt(SOURCE);
        updateTexture();

        kinds.clear();
        int[] cells = bundle.getIntArray(CELLS);
        int[] values = bundle.getIntArray(KINDS);
        for (int i = 0; i < Math.min(cells.length, values.length); i++) {
            kinds.put(cells[i], values[i]);
        }
    }
}

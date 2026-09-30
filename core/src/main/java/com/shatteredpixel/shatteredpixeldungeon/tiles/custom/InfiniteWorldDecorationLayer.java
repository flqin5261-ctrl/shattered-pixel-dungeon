/*
 * Shattered Pixel Dungeon - Assist Edition
 *
 * Additional Infinite World decoration sprites are sourced from Kenney's
 * Tiny Town / Tiny Dungeon packs under CC0 1.0. See docs/assist/THIRD_PARTY_ASSETS.md.
 *
 * This layer is presentation-only: props never alter terrain collision,
 * pathfinding or interaction state.
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

    // Tiny Town nature / roadside props.
    public static final int WEEDS = 1;
    public static final int FLOWERS = 2;
    public static final int TREE_GREEN = 3;
    public static final int TREE_SLENDER = 4;
    public static final int TREE_AUTUMN = 5;
    public static final int MUSHROOMS = 6;
    public static final int STONE_PATCH = 7;
    public static final int SIGN = 8;
    public static final int ROCK = 9;
    public static final int STUMP = 10;
    public static final int BARREL = 11;

    // Tiny Dungeon interior / ruin props.
    public static final int RUBBLE = 12;
    public static final int BRAZIER = 13;
    public static final int ARCANE_BRAZIER = 14;
    public static final int PEDESTAL = 15;
    public static final int SARCOPHAGUS = 16;
    public static final int CRYSTAL_ORB = 17;
    public static final int TABLE = 18;
    public static final int STOOL = 19;
    public static final int SHELF = 20;
    public static final int ARMOR_STAND = 21;
    public static final int FENCE = 22;
    public static final int VASE = 23;

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

    public static boolean usesDungeonAtlas(int kind) {
        return kind >= RUBBLE;
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
        // Both packed CC0 sheets are 12x11 tightly packed 16x16 tiles.
        // Indices were verified against the actual pinned PNGs used by CI.
        switch (kind) {
            // Tiny Town.
            case WEEDS:         return source == SOURCE_TOWN ? 3 : -1;
            case FLOWERS:       return source == SOURCE_TOWN ? 2 : -1;
            case TREE_GREEN:    return source == SOURCE_TOWN ? 5 : -1;
            case TREE_SLENDER:  return source == SOURCE_TOWN ? 7 : -1;
            case TREE_AUTUMN:   return source == SOURCE_TOWN ? 11 : -1;
            case MUSHROOMS:     return source == SOURCE_TOWN ? 29 : -1;
            case STONE_PATCH:   return source == SOURCE_TOWN ? 43 : -1;
            case SIGN:          return source == SOURCE_TOWN ? 83 : -1;
            case ROCK:          return source == SOURCE_TOWN ? 105 : -1;
            case STUMP:         return source == SOURCE_TOWN ? 106 : -1;
            case BARREL:        return source == SOURCE_TOWN ? 107 : -1;

            // Tiny Dungeon.
            case RUBBLE:        return source == SOURCE_DUNGEON ? 12 : -1;
            case BRAZIER:       return source == SOURCE_DUNGEON ? 29 : -1;
            case ARCANE_BRAZIER:return source == SOURCE_DUNGEON ? 32 : -1;
            case PEDESTAL:      return source == SOURCE_DUNGEON ? 43 : -1;
            case SARCOPHAGUS:   return source == SOURCE_DUNGEON ? 66 : -1;
            case CRYSTAL_ORB:   return source == SOURCE_DUNGEON ? 68 : -1;
            case TABLE:         return source == SOURCE_DUNGEON ? 72 : -1;
            case STOOL:         return source == SOURCE_DUNGEON ? 73 : -1;
            case SHELF:         return source == SOURCE_DUNGEON ? 75 : -1;
            case ARMOR_STAND:   return source == SOURCE_DUNGEON ? 74 : -1;
            case FENCE:         return source == SOURCE_DUNGEON ? 78 : -1;
            case VASE:          return source == SOURCE_DUNGEON ? 113 : -1;
            default:            return -1;
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
        switch (kindAt(x, y)) {
            case WEEDS:          return Messages.get(this, "weeds_name");
            case FLOWERS:        return Messages.get(this, "flowers_name");
            case TREE_GREEN:     return Messages.get(this, "tree_green_name");
            case TREE_SLENDER:   return Messages.get(this, "tree_slender_name");
            case TREE_AUTUMN:    return Messages.get(this, "tree_autumn_name");
            case MUSHROOMS:      return Messages.get(this, "mushrooms_name");
            case STONE_PATCH:    return Messages.get(this, "stone_patch_name");
            case SIGN:           return Messages.get(this, "sign_name");
            case ROCK:           return Messages.get(this, "rock_name");
            case STUMP:          return Messages.get(this, "stump_name");
            case BARREL:         return Messages.get(this, "barrel_name");
            case RUBBLE:         return Messages.get(this, "rubble_name");
            case BRAZIER:        return Messages.get(this, "brazier_name");
            case ARCANE_BRAZIER: return Messages.get(this, "arcane_brazier_name");
            case PEDESTAL:       return Messages.get(this, "pedestal_name");
            case SARCOPHAGUS:    return Messages.get(this, "sarcophagus_name");
            case CRYSTAL_ORB:    return Messages.get(this, "crystal_orb_name");
            case TABLE:          return Messages.get(this, "table_name");
            case STOOL:          return Messages.get(this, "stool_name");
            case SHELF:          return Messages.get(this, "shelf_name");
            case ARMOR_STAND:    return Messages.get(this, "armor_stand_name");
            case FENCE:          return Messages.get(this, "fence_name");
            case VASE:           return Messages.get(this, "vase_name");
            default:             return null;
        }
    }

    @Override
    public String desc(int x, int y) {
        switch (kindAt(x, y)) {
            case WEEDS:          return Messages.get(this, "weeds_desc");
            case FLOWERS:        return Messages.get(this, "flowers_desc");
            case TREE_GREEN:     return Messages.get(this, "tree_green_desc");
            case TREE_SLENDER:   return Messages.get(this, "tree_slender_desc");
            case TREE_AUTUMN:    return Messages.get(this, "tree_autumn_desc");
            case MUSHROOMS:      return Messages.get(this, "mushrooms_desc");
            case STONE_PATCH:    return Messages.get(this, "stone_patch_desc");
            case SIGN:           return Messages.get(this, "sign_desc");
            case ROCK:           return Messages.get(this, "rock_desc");
            case STUMP:          return Messages.get(this, "stump_desc");
            case BARREL:         return Messages.get(this, "barrel_desc");
            case RUBBLE:         return Messages.get(this, "rubble_desc");
            case BRAZIER:        return Messages.get(this, "brazier_desc");
            case ARCANE_BRAZIER: return Messages.get(this, "arcane_brazier_desc");
            case PEDESTAL:       return Messages.get(this, "pedestal_desc");
            case SARCOPHAGUS:    return Messages.get(this, "sarcophagus_desc");
            case CRYSTAL_ORB:    return Messages.get(this, "crystal_orb_desc");
            case TABLE:          return Messages.get(this, "table_desc");
            case STOOL:          return Messages.get(this, "stool_desc");
            case SHELF:          return Messages.get(this, "shelf_desc");
            case ARMOR_STAND:    return Messages.get(this, "armor_stand_desc");
            case FENCE:          return Messages.get(this, "fence_desc");
            case VASE:           return Messages.get(this, "vase_desc");
            default:             return null;
        }
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

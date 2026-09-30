/*
 * Shattered Pixel Dungeon - Assist Edition
 *
 * Additional Infinite World decoration sprites are sourced from Kenney's
 * Tiny Town / Tiny Dungeon packs under CC0 1.0. See docs/assist/THIRD_PARTY_ASSETS.md.
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

    public static final int BUSH = 1;
    public static final int MUSHROOMS = 2;
    public static final int SIGN = 3;
    public static final int BARREL = 4;
    public static final int CRATE = 5;

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

    private void updateTexture() {
        texture = source == SOURCE_DUNGEON
                ? Assets.Environment.ASSIST_KENNEY_TINY_DUNGEON
                : Assets.Environment.ASSIST_KENNEY_TINY_TOWN;
    }

    public boolean isEmpty() {
        return kinds.size() == 0;
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
        // Packed sheets are 12 columns, 16x16 px per tile.
        // Tiny Town: bush=5, mushrooms=29, sign=83, barrel=107.
        // Tiny Dungeon: crate=66.
        switch (kind) {
            case BUSH:      return source == SOURCE_TOWN ? 5 : -1;
            case MUSHROOMS: return source == SOURCE_TOWN ? 29 : -1;
            case SIGN:      return source == SOURCE_TOWN ? 83 : -1;
            case BARREL:    return source == SOURCE_TOWN ? 107 : -1;
            case CRATE:     return source == SOURCE_DUNGEON ? 66 : -1;
            default:        return -1;
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
            case BUSH:      return Messages.get(this, "bush_name");
            case MUSHROOMS: return Messages.get(this, "mushrooms_name");
            case SIGN:      return Messages.get(this, "sign_name");
            case BARREL:    return Messages.get(this, "barrel_name");
            case CRATE:     return Messages.get(this, "crate_name");
            default:        return null;
        }
    }

    @Override
    public String desc(int x, int y) {
        switch (kindAt(x, y)) {
            case BUSH:      return Messages.get(this, "bush_desc");
            case MUSHROOMS: return Messages.get(this, "mushrooms_desc");
            case SIGN:      return Messages.get(this, "sign_desc");
            case BARREL:    return Messages.get(this, "barrel_desc");
            case CRATE:     return Messages.get(this, "crate_desc");
            default:        return null;
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

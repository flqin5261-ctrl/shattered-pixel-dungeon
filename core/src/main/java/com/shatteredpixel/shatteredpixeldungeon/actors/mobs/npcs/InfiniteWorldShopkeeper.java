/*
 * Shattered Pixel Dungeon - Assist Edition
 */
package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;

public class InfiniteWorldShopkeeper extends Shopkeeper {

    private static final String SHOP_CHUNK_X = "shop_chunk_x";
    private static final String SHOP_CHUNK_Y = "shop_chunk_y";

    private int shopChunkX;
    private int shopChunkY;

    public InfiniteWorldShopkeeper() {
        super();
    }

    public InfiniteWorldShopkeeper(int chunkX, int chunkY) {
        super();
        shopChunkX = chunkX;
        shopChunkY = chunkY;
    }

    public int shopChunkX() {
        return shopChunkX;
    }

    public int shopChunkY() {
        return shopChunkY;
    }

    @Override
    public String chatText() {
        return Messages.get(this, "talk_world");
    }

    @Override
    public void flee() {
        if (Dungeon.level instanceof InfiniteWorldLevel) {
            ((InfiniteWorldLevel) Dungeon.level).markInfiniteWorldMerchantGone(shopChunkX, shopChunkY);
        }
        super.flee();
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(SHOP_CHUNK_X, shopChunkX);
        bundle.put(SHOP_CHUNK_Y, shopChunkY);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        shopChunkX = bundle.getInt(SHOP_CHUNK_X);
        shopChunkY = bundle.getInt(SHOP_CHUNK_Y);
    }
}

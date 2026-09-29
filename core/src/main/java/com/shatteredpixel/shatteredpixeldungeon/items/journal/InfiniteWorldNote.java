/*
 * Shattered Pixel Dungeon - Assist Edition
 * Field notes for rare Infinite World anomaly districts.
 */
package com.shatteredpixel.shatteredpixeldungeon.items.journal;

import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class InfiniteWorldNote extends DocumentPage {

    {
        image = ItemSpriteSheet.GUIDE_PAGE;
    }

    @Override
    public Document document() {
        return Document.INFINITE_WORLD_NOTES;
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", document().pageTitle(page()));
    }
}

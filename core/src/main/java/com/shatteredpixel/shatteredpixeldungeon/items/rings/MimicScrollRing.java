/*
 * Shattered Pixel Dungeon Assist
 */
package com.shatteredpixel.shatteredpixeldungeon.items.rings;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTerror;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Reflection;

public class MimicScrollRing extends MimicRing {

    @SuppressWarnings("unchecked")
    private static final Class<? extends Item>[] ABILITIES = new Class[]{
            ScrollOfUpgrade.class,
            ScrollOfIdentify.class,
            ScrollOfRemoveCurse.class,
            ScrollOfMirrorImage.class,
            ScrollOfRecharging.class,
            ScrollOfTeleportation.class,
            ScrollOfLullaby.class,
            ScrollOfMagicMapping.class,
            ScrollOfRage.class,
            ScrollOfRetribution.class,
            ScrollOfTerror.class,
            ScrollOfTransmutation.class
    };

    @Override
    protected Class<? extends Item>[] abilityPool() {
        return ABILITIES;
    }

    @Override
    protected int mimicImage() {
        return ItemSpriteSheet.RING_DIAMOND;
    }

    @Override
    protected void invokeStoredAbility(Hero hero, Class<? extends Item> abilityClass) {
        Item item = Reflection.newInstance(abilityClass);
        if (item instanceof Scroll) {
            // Execute the original scroll object so targeting, talents, status
            // effects, UI selectors, and special rules remain the real mechanics.
            ((Scroll)item).execute(hero, Scroll.AC_READ);
        }
    }
}

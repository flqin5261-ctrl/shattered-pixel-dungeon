/*
 * Shattered Pixel Dungeon Assist
 */
package com.shatteredpixel.shatteredpixeldungeon.items.rings;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Reflection;

public class MimicPotionRing extends MimicRing {

    @SuppressWarnings("unchecked")
    private static final Class<? extends Item>[] ABILITIES = new Class[]{
            PotionOfStrength.class,
            PotionOfHealing.class,
            PotionOfMindVision.class,
            PotionOfFrost.class,
            PotionOfLiquidFlame.class,
            PotionOfToxicGas.class,
            PotionOfHaste.class,
            PotionOfInvisibility.class,
            PotionOfLevitation.class,
            PotionOfParalyticGas.class,
            PotionOfPurity.class,
            PotionOfExperience.class
    };

    @Override
    protected Class<? extends Item>[] abilityPool() {
        return ABILITIES;
    }

    @Override
    protected int mimicImage() {
        return ItemSpriteSheet.RING_AMETHYST;
    }

    @Override
    protected void invokeStoredAbility(Hero hero, Class<? extends Item> abilityClass) {
        Item item = Reflection.newInstance(abilityClass);
        if (item instanceof Potion) {
            // Drinking the temporary original potion preserves the real potion
            // effect and the normal "potion used" talent/event pathway.
            ((Potion)item).execute(hero, Potion.AC_DRINK);
        }
    }
}

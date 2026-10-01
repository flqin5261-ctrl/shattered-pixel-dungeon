/*
 * Shattered Pixel Dungeon Assist
 */
package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

/**
 * 万法之法: each paid cast chains real effects from randomly selected vanilla
 * wands. The number of effects is 1 + level/3, while the parent Wand still pays
 * only one charge and one normal action for the whole cast.
 */
public class WandOfAllLaws extends Wand {

    @SuppressWarnings("unchecked")
    private static final Class<? extends Wand>[] EFFECTS = new Class[]{
            WandOfMagicMissile.class,
            WandOfLightning.class,
            WandOfDisintegration.class,
            WandOfFireblast.class,
            WandOfCorrosion.class,
            WandOfBlastWave.class,
            WandOfLivingEarth.class,
            WandOfFrost.class,
            WandOfPrismaticLight.class,
            WandOfWarding.class,
            WandOfTransfusion.class,
            WandOfCorruption.class,
            WandOfRegrowth.class
    };

    {
        image = ItemSpriteSheet.WAND_PRISMATIC_LIGHT;
        collisionProperties = Ballistica.MAGIC_BOLT;
    }

    public int effectCount() {
        return 1 + Math.max(0, level()) / 3;
    }

    @Override
    public void fx(Ballistica bolt, Callback callback) {
        castRandomEffect(0, effectCount(), bolt.collisionPos, callback);
    }

    private void castRandomEffect(final int index, final int total,
                                  final int target, final Callback done) {
        if (index >= total) {
            curItem = this;
            done.call();
            return;
        }

        Wand effect = Reflection.newInstance(EFFECTS[Random.Int(EFFECTS.length)]);
        if (effect == null) {
            castRandomEffect(index + 1, total, target, done);
            return;
        }

        // Each strike rolls independently, and uses this wand's actual current
        // level. Native fx + onZap are both executed so fire, frost, corrosion,
        // disintegration, warding, knockback, etc. are genuine wand mechanics.
        effect.level(Math.max(0, buffedLvl()));
        effect.cursed = false;
        effect.curChargeKnown = true;

        final Ballistica effectShot =
                new Ballistica(curUser.pos, target, effect.collisionProperties(target));

        curItem = effect;
        effect.fx(effectShot, new Callback() {
            @Override
            public void call() {
                Item previous = curItem;
                try {
                    curItem = effect;
                    effect.onZap(effectShot);
                } finally {
                    curItem = previous;
                }
                castRandomEffect(index + 1, total, target, done);
            }
        });
    }

    @Override
    public void onZap(Ballistica attack) {
        // The actual effects are executed in fx(). The parent Wand's normal
        // callback still reaches here once so wandUsed() happens exactly once.
    }

    @Override
    public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
        Wand effect = Reflection.newInstance(EFFECTS[Random.Int(EFFECTS.length)]);
        if (effect != null) {
            effect.level(Math.max(0, buffedLvl()));
            effect.onHit(staff, attacker, defender, damage);
        }
    }

    @Override
    public String statsDesc() {
        return Messages.get(this, "stats_desc", effectCount());
    }

    @Override
    public String upgradeStat1(int level) {
        return Integer.toString(1 + Math.max(0, level) / 3);
    }
}

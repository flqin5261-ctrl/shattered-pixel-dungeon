/*
 * Shattered Pixel Dungeon Assist
 */
package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
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
public class WandOfAllLaws extends DamageWand {

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
    public int min(int lvl) {
        return 2 + Math.max(0, lvl);
    }

    @Override
    public int max(int lvl) {
        return 8 + 2 * Math.max(0, lvl);
    }

    private Wand prepareRandomEffect() {
        Wand effect = Reflection.newInstance(EFFECTS[Random.Int(EFFECTS.length)]);
        if (effect == null) return null;

        int effectLevel = Math.max(0, buffedLvl());
        if (effect instanceof WandOfRegrowth) {
            effectLevel = Math.min(effectLevel, WandOfRegrowth.LEVEL_CAP);
        }

        // Give the temporary wand the same effective level and charge state a
        // real wand of that level would have. This makes both damage and native
        // multi-charge behaviours scale with 万法之法 rather than only increasing
        // the number of chained casts.
        effect.level(effectLevel);
        effect.updateLevel();
        effect.curCharges = effect.maxCharges;
        effect.cursed = false;
        effect.curChargeKnown = true;
        return effect;
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

        final Wand effect = prepareRandomEffect();
        if (effect == null) {
            castRandomEffect(index + 1, total, target, done);
            return;
        }

        // Each strike rolls independently. Native fx + onZap are both executed
        // so fire, frost, corrosion, disintegration, warding, knockback, etc.
        // remain genuine original wand mechanics.

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
        // In addition to the randomly selected native effects, 万法之法 has its
        // own guaranteed direct damage packet. This scales every level, so levels
        // are never "cast-count only".
        Char ch = Actor.findChar(attack.collisionPos);
        if (ch != null && ch != curUser) {
            wandProc(ch, chargesPerCast());
            ch.damage(damageRoll(), this);
        }
    }

    @Override
    public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
        Wand effect = prepareRandomEffect();
        if (effect != null) {
            effect.onHit(staff, attacker, defender, damage);
        }
    }

    @Override
    public String statsDesc() {
        int lvl = levelKnown ? buffedLvl() : 0;
        return Messages.get(this, "stats_desc", min(lvl), max(lvl), effectCount());
    }

    @Override
    public String upgradeStat1(int level) {
        return min(level) + "-" + max(level);
    }

    @Override
    public String upgradeStat2(int level) {
        return Integer.toString(1 + Math.max(0, level) / 3);
    }
}

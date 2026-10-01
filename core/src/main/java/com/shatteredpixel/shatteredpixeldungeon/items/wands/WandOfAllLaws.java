/*
 * Shattered Pixel Dungeon Assist
 */
package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

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

    private float launchInterval(int total) {
        // Keep every native animation, but avoid creating too many projectile/
        // particle systems in the same frame. High-count casts get a slightly
        // wider cadence so fast phones still show the whole spell storm cleanly.
        if (total <= 5) return 0.10f;
        if (total <= 15) return 0.12f;
        return 0.14f;
    }

    @Override
    public void fx(Ballistica bolt, final Callback callback) {
        final Hero caster = curUser;
        final int target = bolt.collisionPos;
        final ArrayList<Wand> effects = new ArrayList<>();

        for (int i = 0; i < effectCount(); i++) {
            Wand effect = prepareRandomEffect();
            if (effect != null) effects.add(effect);
        }

        if (effects.isEmpty() || caster == null || caster.sprite == null || caster.sprite.parent == null) {
            curItem = this;
            callback.call();
            return;
        }

        final float interval = launchInterval(effects.size());
        final int[] completed = new int[]{0};
        final boolean[] parentFinished = new boolean[]{false};

        for (int i = 0; i < effects.size(); i++) {
            final Wand effect = effects.get(i);
            Delayer launch = new Delayer(i * interval);
            launch.listener = new Tweener.Listener() {
                @Override
                public void onComplete(Tweener tweener) {
                    if (parentFinished[0] || caster == null || !caster.isAlive()) {
                        if (++completed[0] >= effects.size() && !parentFinished[0]) {
                            parentFinished[0] = true;
                            curUser = caster;
                            curItem = WandOfAllLaws.this;
                            callback.call();
                        }
                        return;
                    }

                    curUser = caster;
                    curItem = effect;

                    final Ballistica effectShot =
                            new Ballistica(caster.pos, target, effect.collisionProperties(target));

                    // Every random wand gets its complete native fx. Its own
                    // callback resolves its own onZap, but that callback does NOT
                    // control when the next random wand launches; the Delayer
                    // queue above does. This preserves full animation and normal
                    // damage timing without serial stalls.
                    effect.fx(effectShot, new Callback() {
                        @Override
                        public void call() {
                            Item previous = curItem;
                            Hero previousUser = curUser;
                            try {
                                curUser = caster;
                                curItem = effect;
                                effect.onZap(effectShot);
                            } finally {
                                curUser = previousUser;
                                curItem = previous;
                            }

                            completed[0]++;
                            if (completed[0] >= effects.size() && !parentFinished[0]) {
                                parentFinished[0] = true;
                                curUser = caster;
                                curItem = WandOfAllLaws.this;
                                callback.call();
                            }
                        }
                    });
                }
            };
            caster.sprite.parent.add(launch);
        }
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

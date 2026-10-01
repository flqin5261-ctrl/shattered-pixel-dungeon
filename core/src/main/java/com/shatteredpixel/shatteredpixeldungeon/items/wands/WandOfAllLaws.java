/*
 * Shattered Pixel Dungeon Assist
 */
package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * 万法之法 is now a true Artifact rather than a rare Wand injection.
 *
 * Its active cast still delegates to genuine vanilla Wand implementations so
 * their projectiles, particles, terrain effects and status effects remain native.
 * One artifact charge pays for the complete chained cast.
 */
public class WandOfAllLaws extends Artifact {

    public static final String AC_CAST = "CAST";

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
        // Keep the recognizable prism-wand sprite even though the item now uses
        // the Artifact equipment/uniqueness/charge systems.
        image = ItemSpriteSheet.WAND_PRISMATIC_LIGHT;

        levelCap = 10;
        chargeCap = 10;
        charge = chargeCap;
        partialCharge = 0f;

        defaultAction = AC_CAST;
        usesTargeting = true;
    }

    public int effectCount() {
        return 1 + Math.max(0, visiblyUpgraded()) / 3;
    }

    public int min(int lvl) {
        return 2 + Math.max(0, lvl);
    }

    public int max(int lvl) {
        return 8 + 2 * Math.max(0, lvl);
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero)
                && charge > 0
                && !cursed
                && hero.buff(MagicImmune.class) == null) {
            actions.add(AC_CAST);
        }
        return actions;
    }

    @Override
    public int targetingPos(Hero user, int dst) {
        if (user == null) return dst;
        return new Ballistica(user.pos, dst, Ballistica.MAGIC_BOLT).collisionPos;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (!AC_CAST.equals(action)) return;
        if (hero.buff(MagicImmune.class) != null) return;

        curUser = hero;

        if (!isEquipped(hero)) {
            GLog.i(Messages.get(Artifact.class, "need_to_equip"));
            usesTargeting = false;
        } else if (charge <= 0) {
            GLog.i(Messages.get(this, "no_charge"));
            usesTargeting = false;
        } else if (cursed) {
            GLog.w(Messages.get(this, "cursed"));
            usesTargeting = false;
        } else {
            usesTargeting = true;
            GameScene.selectCell(caster);
        }
    }

    public final CellSelector.Listener caster = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer target) {
            if (target == null || curUser == null) return;
            castAt(curUser, target);
        }

        @Override
        public String prompt() {
            return Messages.get(WandOfAllLaws.class, "prompt");
        }
    };

    private Wand prepareRandomEffect() {
        Wand effect = Reflection.newInstance(EFFECTS[Random.Int(EFFECTS.length)]);
        if (effect == null) return null;

        int effectLevel = Math.max(0, buffedLvl());
        if (effect instanceof WandOfRegrowth) {
            effectLevel = Math.min(effectLevel, WandOfRegrowth.LEVEL_CAP);
        }

        effect.level(effectLevel);
        effect.updateLevel();
        effect.curCharges = effect.maxCharges;
        effect.cursed = false;
        effect.curChargeKnown = true;
        return effect;
    }

    private float launchInterval(int total) {
        if (total <= 5) return 0.10f;
        if (total <= 15) return 0.12f;
        return 0.14f;
    }

    private void castAt(final Hero casterHero, int selectedTarget) {
        final Ballistica parentShot =
                new Ballistica(casterHero.pos, selectedTarget, Ballistica.MAGIC_BOLT);
        final int target = parentShot.collisionPos;

        final ArrayList<Wand> effects = new ArrayList<>();
        for (int i = 0; i < effectCount(); i++) {
            Wand effect = prepareRandomEffect();
            if (effect != null) effects.add(effect);
        }

        charge--;
        partialCharge = Math.max(0f, partialCharge);
        updateQuickslot();
        Invisibility.dispel(casterHero);
        casterHero.busy();

        if (effects.isEmpty() || casterHero.sprite == null || casterHero.sprite.parent == null) {
            finishCast(casterHero, target);
            return;
        }

        final float interval = launchInterval(effects.size());
        final int[] completed = new int[]{0};
        final boolean[] finished = new boolean[]{false};

        for (int i = 0; i < effects.size(); i++) {
            final Wand effect = effects.get(i);
            Delayer launch = new Delayer(i * interval);
            launch.listener = new Tweener.Listener() {
                @Override
                public void onComplete(Tweener tweener) {
                    if (finished[0]) return;

                    if (casterHero == null || !casterHero.isAlive()) {
                        if (++completed[0] >= effects.size()) {
                            finished[0] = true;
                            finishCast(casterHero, target);
                        }
                        return;
                    }

                    curUser = casterHero;
                    curItem = effect;

                    final Ballistica effectShot =
                            new Ballistica(casterHero.pos, target, effect.collisionProperties(target));

                    effect.fx(effectShot, new Callback() {
                        @Override
                        public void call() {
                            Item previous = curItem;
                            Hero previousUser = curUser;
                            try {
                                curUser = casterHero;
                                curItem = effect;
                                effect.onZap(effectShot);
                            } finally {
                                curUser = previousUser;
                                curItem = previous;
                            }

                            completed[0]++;
                            if (completed[0] >= effects.size() && !finished[0]) {
                                finished[0] = true;
                                finishCast(casterHero, target);
                            }
                        }
                    });
                }
            };
            casterHero.sprite.parent.add(launch);
        }
    }

    private void finishCast(Hero casterHero, int target) {
        curUser = casterHero;
        curItem = this;

        Char ch = Actor.findChar(target);
        if (ch != null && ch != casterHero) {
            artifactProc(ch, visiblyUpgraded(), 1);
            int lvl = Math.max(0, buffedLvl());
            ch.damage(Random.NormalIntRange(min(lvl), max(lvl)), this);
        }

        if (casterHero != null) {
            Talent.onArtifactUsed(casterHero);
            updateQuickslot();
            if (casterHero.isAlive()) casterHero.spendAndNext(1f);
        }
    }

    @Override
    protected ArtifactBuff passiveBuff() {
        return new Recharge();
    }

    @Override
    public void charge(Hero target, float amount) {
        if (target == null || charge >= chargeCap || cursed
                || target.buff(MagicImmune.class) != null) {
            return;
        }

        partialCharge += 0.10f * amount;
        while (partialCharge >= 1f && charge < chargeCap) {
            partialCharge -= 1f;
            charge++;
        }
        if (charge >= chargeCap) partialCharge = 0f;
        updateQuickslot();
    }

    public class Recharge extends ArtifactBuff {
        @Override
        public boolean act() {
            if (charge < chargeCap
                    && !cursed
                    && target.buff(MagicImmune.class) == null
                    && Regeneration.regenOn()) {
                float gain = 1f / 80f;
                gain *= RingOfEnergy.artifactChargeMultiplier(target);
                gain *= assistOverlevelChargeMultiplier();
                partialCharge += gain;

                while (partialCharge >= 1f && charge < chargeCap) {
                    partialCharge -= 1f;
                    charge++;
                }
                if (charge >= chargeCap) partialCharge = 0f;
                updateQuickslot();
            }

            spend(TICK);
            return true;
        }
    }

    @Override
    public void resetForTrinity(int visibleLevel) {
        super.resetForTrinity(visibleLevel);
        charge = chargeCap;
        partialCharge = 0f;
    }

    @Override
    public String desc() {
        int lvl = Math.max(0, buffedLvl());
        return super.desc() + "\n\n" + Messages.get(this, "stats_desc",
                min(lvl), max(lvl), effectCount(), charge, chargeCap);
    }
}

/*
 * Shattered Pixel Dungeon Assist
 */
package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfAllLaws;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

/**
 * Artifact incarnation of 万法之法.
 *
 * The old WandOfAllLaws class is intentionally retained for save compatibility,
 * but new generation uses this artifact. Casting delegates the actual spell
 * storm to the old wand implementation, so every native wand effect and its
 * original animation/mechanics remain intact.
 */
public class AllLawsArtifact extends Artifact {

    public static final String AC_CAST = "CAST";

    {
        image = ItemSpriteSheet.ARTIFACT_SPELLBOOK;

        levelCap = 10;
        chargeCap = 10;
        charge = chargeCap;
        partialCharge = 0f;
        exp = 0;

        defaultAction = AC_CAST;
        usesTargeting = true;
        unique = true;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero) && charge > 0 && !cursed && hero.buff(MagicImmune.class) == null) {
            actions.add(AC_CAST);
        }
        return actions;
    }

    @Override
    public int targetingPos(Hero user, int dst) {
        if (user == null) return dst;
        WandOfAllLaws wand = preparedDelegate();
        return new Ballistica(user.pos, dst, wand.collisionProperties(dst)).collisionPos;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (!AC_CAST.equals(action)) return;

        curUser = hero;
        curItem = this;

        if (hero.buff(MagicImmune.class) != null) {
            GLog.w(Messages.get(this, "no_magic"));
            usesTargeting = false;
        } else if (!isEquipped(hero)) {
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

    private WandOfAllLaws preparedDelegate() {
        WandOfAllLaws wand = new WandOfAllLaws();
        int level = Math.max(0, nativeLevel());
        wand.level(level);
        wand.updateLevel();
        wand.curCharges = wand.maxCharges;
        wand.curChargeKnown = true;
        wand.cursed = false;
        wand.levelKnown = true;
        return wand;
    }

    private final CellSelector.Listener caster = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer target) {
            if (target == null || curUser == null) return;
            if (!isEquipped(curUser) || charge <= 0 || cursed) return;
            if (target == curUser.pos) {
                GLog.i(Messages.get(AllLawsArtifact.class, "self_target"));
                return;
            }

            final Hero hero = curUser;
            final AllLawsArtifact artifact = AllLawsArtifact.this;
            final WandOfAllLaws wand = preparedDelegate();
            final Ballistica shot =
                    new Ballistica(hero.pos, target, wand.collisionProperties(target));

            if (shot.collisionPos == hero.pos) return;

            hero.busy();
            curUser = hero;
            curItem = wand;

            wand.fx(shot, new Callback() {
                @Override
                public void call() {
                    Hero oldUser = curUser;
                    Item oldItem = curItem;
                    try {
                        curUser = hero;
                        curItem = wand;
                        wand.onZap(shot);
                    } finally {
                        curUser = oldUser;
                        curItem = oldItem;
                    }

                    charge--;
                    Invisibility.dispel(hero);
                    Talent.onArtifactUsed(hero);
                    gainUseExperience();
                    updateQuickslot();
                    hero.spendAndNext(1f);
                }
            });
        }

        @Override
        public String prompt() {
            return Messages.get(AllLawsArtifact.class, "prompt");
        }
    };

    private void gainUseExperience() {
        if (nativeLevel() >= levelCap) return;

        exp++;
        int threshold = 3 + nativeLevel();
        if (exp >= threshold) {
            exp = 0;
            upgrade();
            charge = Math.min(chargeCap, charge + 1);
            GLog.p(Messages.get(this, "levelup"));
        }
    }

    @Override
    public void resetForTrinity(int visibleLevel) {
        super.resetForTrinity(visibleLevel);
        chargeCap = 10;
        charge = chargeCap;
        partialCharge = 0f;
    }

    @Override
    protected ArtifactBuff passiveBuff() {
        return new AllLawsRecharge();
    }

    @Override
    public void charge(Hero target, float amount) {
        if (charge >= chargeCap || cursed || target.buff(MagicImmune.class) != null) return;

        partialCharge += 0.15f * amount * assistOverlevelChargeMultiplier();
        while (partialCharge >= 1f && charge < chargeCap) {
            partialCharge -= 1f;
            charge++;
        }
        if (charge >= chargeCap) partialCharge = 0f;
        updateQuickslot();
    }

    public class AllLawsRecharge extends ArtifactBuff {
        @Override
        public boolean act() {
            if (charge < chargeCap
                    && !cursed
                    && target.buff(MagicImmune.class) == null
                    && Regeneration.regenOn()) {
                float gain = (1f / 32f)
                        * RingOfEnergy.artifactChargeMultiplier(target)
                        * assistOverlevelChargeMultiplier();
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
    public String desc() {
        int lvl = Math.max(0, buffedLvl());
        int effects = 1 + lvl / 3;
        return super.desc() + "\n\n" + Messages.get(this, "stats_desc",
                2 + lvl, 8 + 2 * lvl, effects, charge, chargeCap);
    }
}

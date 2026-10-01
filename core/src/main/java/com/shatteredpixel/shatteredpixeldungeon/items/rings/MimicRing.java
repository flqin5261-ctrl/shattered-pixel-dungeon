/*
 * Shattered Pixel Dungeon Assist
 * Low-level limited mimic equipment.
 */
package com.shatteredpixel.shatteredpixeldungeon.items.rings;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.BreakthroughCertificate;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Common implementation for the two limited mimic rings.
 *
 * The copied consumable effect itself has no numerical upgrade scaling. A Scroll
 * of Upgrade rerolls the stored ability, while charge capacity grows exactly like
 * a normal 2-charge wand: 2 -> ... -> 10. Recharge timing mirrors Wand.Charger.
 */
public abstract class MimicRing extends Ring {

    public static final String AC_INVOKE = "INVOKE";

    private static final int INITIAL_CHARGES = 2;
    private static final int MAX_CHARGES = 10;
    private static final int MAX_CHARGE_UPGRADES = MAX_CHARGES - INITIAL_CHARGES;

    private static final float BASE_CHARGE_DELAY = 10f;
    private static final float SCALING_CHARGE_ADDITION = 40f;
    private static final float NORMAL_SCALE_FACTOR = 0.875f;
    private static final float CHARGE_BUFF_BONUS = 0.25f;

    private static final String ABILITY_INDEX = "mimic_ability_index";
    private static final String CUR_CHARGES = "mimic_cur_charges";
    private static final String PARTIAL_CHARGE = "mimic_partial_charge";
    private static final String CHARGE_UPGRADES = "mimic_charge_upgrades";

    protected int abilityIndex = 0;
    protected int chargeUpgrades = 0;
    protected int curCharges = INITIAL_CHARGES;
    protected float partialCharge = 0f;

    {
        defaultAction = AC_INVOKE;
        bones = true;
    }

    public MimicRing() {
        super();
        // Normal dungeon generation calls random(), but debug/Assist item grants
        // instantiate items directly. Roll here as well so a granted ring does
        // not always default to the first scroll/potion in its pool.
        rollInitialAbility();
        curCharges = maxCharges();
        partialCharge = 0f;
    }

    protected abstract Class<? extends Item>[] abilityPool();

    protected abstract int mimicImage();

    protected abstract void invokeStoredAbility(Hero hero, Class<? extends Item> abilityClass);

    @Override
    public void reset() {
        super.reset();
        image = mimicImage();
    }

    /**
     * These are fixed-name limited rings, not part of the vanilla 12-gem
     * unidentified ring mapping. This avoids changing the old-save gem table.
     */
    @Override
    public boolean isKnown() {
        return true;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (!actions.contains(AC_INVOKE)) {
            actions.add(AC_INVOKE);
        }
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (AC_INVOKE.equals(action)) {
            return Messages.get(MimicRing.class, "ac_invoke");
        }
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        if (!AC_INVOKE.equals(action)) {
            super.execute(hero, action);
            return;
        }

        // Preserve Item.execute() bookkeeping (cancel targeting + curUser/curItem)
        // without letting another equipment action fire.
        super.execute(hero, action);

        if (!isEquipped(hero)) {
            GLog.w(Messages.get(MimicRing.class, "need_equip"));
            return;
        }
        if (curCharges <= 0) {
            GLog.w(Messages.get(MimicRing.class, "no_charge"));
            return;
        }

        Class<? extends Item>[] pool = abilityPool();
        if (pool == null || pool.length == 0) {
            return;
        }
        if (abilityIndex < 0 || abilityIndex >= pool.length) {
            abilityIndex = 0;
        }

        // The charge belongs to the ring, not the temporary emulated consumable.
        // Spend exactly one before handing control to any asynchronous inventory UI.
        curCharges--;
        partialCharge = Math.max(0f, partialCharge);
        updateQuickslot();

        invokeStoredAbility(hero, pool[abilityIndex]);
    }

    protected String storedAbilityName() {
        Class<? extends Item>[] pool = abilityPool();
        if (pool == null || pool.length == 0) return "?";
        if (abilityIndex < 0 || abilityIndex >= pool.length) abilityIndex = 0;
        return Messages.titleCase(Messages.get(pool[abilityIndex], "name"));
    }

    public String currentAbilityName() {
        return storedAbilityName();
    }

    public int maxCharges() {
        return Math.min(MAX_CHARGES, INITIAL_CHARGES + Math.max(0, chargeUpgrades));
    }

    public int currentCharges() {
        return curCharges;
    }

    public int maxChargesAfterUpgrade() {
        return Math.min(MAX_CHARGES, maxCharges() + 1);
    }

    public int currentChargesAfterUpgrade() {
        int nextMax = maxChargesAfterUpgrade();
        return maxCharges() < MAX_CHARGES ? Math.min(nextMax, curCharges + 1) : curCharges;
    }

    protected void rollInitialAbility() {
        Class<? extends Item>[] pool = abilityPool();
        abilityIndex = (pool == null || pool.length == 0) ? 0 : Random.Int(pool.length);
    }

    protected void rerollAbility() {
        Class<? extends Item>[] pool = abilityPool();
        if (pool == null || pool.length <= 1) {
            abilityIndex = 0;
            return;
        }
        int old = Math.max(0, Math.min(abilityIndex, pool.length - 1));
        int next;
        do {
            next = Random.Int(pool.length);
        } while (next == old);
        abilityIndex = next;
    }

    @Override
    public Item random() {
        super.random();
        // Limited mimic rings never gain a numerical ring level.
        super.level(0);
        chargeUpgrades = 0;
        rollInitialAbility();
        curCharges = maxCharges();
        partialCharge = 0f;
        return this;
    }

    @Override
    public Item upgrade() {
        // Deliberately do NOT call Ring.upgrade()/Item.upgrade(): copied scroll
        // and potion effects never gain a numerical potency level. The upgrade is
        // instead both a reroll token and one step of wand-like charge growth.
        rerollAbility();

        int oldMax = maxCharges();
        if (chargeUpgrades < MAX_CHARGE_UPGRADES) {
            chargeUpgrades++;
        }
        int newMax = maxCharges();
        if (newMax > oldMax) {
            curCharges = Math.min(newMax, curCharges + 1);
        }

        // Keep the ordinary ring's small curse-cleansing chance.
        if (Random.Int(3) == 0) {
            cursed = false;
        }

        // Do not emit a GLog line here. Bulk Assist upgrades call upgrade() many
        // times; one line per reroll could cover the entire phone screen.
        updateQuickslot();
        return this;
    }

    @Override
    public Item degrade() {
        // No numerical level exists to degrade.
        return this;
    }

    @Override
    public Item level(int value) {
        // External systems such as transmutation may try to transfer an old
        // item's level. The defining rule of the mimic ring is that it remains +0.
        return super.level(0);
    }

    @Override
    protected String statsInfo() {
        return Messages.get(MimicRing.class, "stats",
                storedAbilityName(), curCharges, maxCharges());
    }

    @Override
    public String status() {
        return curCharges + "/" + maxCharges();
    }

    @Override
    protected RingBuff buff() {
        return new MimicCharge();
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(ABILITY_INDEX, abilityIndex);
        bundle.put(CUR_CHARGES, curCharges);
        bundle.put(PARTIAL_CHARGE, partialCharge);
        bundle.put(CHARGE_UPGRADES, chargeUpgrades);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);

        Class<? extends Item>[] pool = abilityPool();
        int maxIndex = pool == null ? 0 : Math.max(0, pool.length - 1);
        abilityIndex = Math.max(0, Math.min(bundle.getInt(ABILITY_INDEX), maxIndex));

        chargeUpgrades = bundle.contains(CHARGE_UPGRADES)
                ? Math.max(0, Math.min(MAX_CHARGE_UPGRADES, bundle.getInt(CHARGE_UPGRADES)))
                : 0;
        curCharges = Math.max(0, Math.min(maxCharges(), bundle.getInt(CUR_CHARGES)));
        partialCharge = bundle.getFloat(PARTIAL_CHARGE);
        if (Float.isNaN(partialCharge) || Float.isInfinite(partialCharge) || partialCharge < 0f) {
            partialCharge = 0f;
        }
        if (curCharges >= maxCharges()) {
            partialCharge = 0f;
        }
    }

    public class MimicCharge extends RingBuff {

        @Override
        public boolean act() {
            if (curCharges < maxCharges() && target.buff(MagicImmune.class) == null) {
                recharge();
            }

            while (partialCharge >= 1f && curCharges < maxCharges()) {
                partialCharge--;
                curCharges++;
                updateQuickslot();
            }

            if (curCharges >= maxCharges()) {
                curCharges = maxCharges();
                partialCharge = 0f;
            }

            spend(TICK);
            return true;
        }

        private void recharge() {
            int missingCharges = Math.max(0, maxCharges() - curCharges);
            float turnsToCharge = (float)(BASE_CHARGE_DELAY
                    + SCALING_CHARGE_ADDITION * Math.pow(NORMAL_SCALE_FACTOR, missingCharges));

            if (Regeneration.regenOn()) {
                float certificateScale = 1f;
                if (target instanceof Hero) {
                    BreakthroughCertificate certificate =
                            BreakthroughCertificate.equipped((Hero)target);
                    if (certificate != null) {
                        certificateScale = certificate.effectiveWandChargeMultiplier((Hero)target);
                    }
                }
                partialCharge += (1f / turnsToCharge)
                        * RingOfEnergy.wandChargeMultiplier(target)
                        * certificateScale;
            }

            for (Recharging bonus : target.buffs(Recharging.class)) {
                if (bonus != null && bonus.remainder() > 0f) {
                    partialCharge += CHARGE_BUFF_BONUS * bonus.remainder();
                }
            }
        }
    }
}

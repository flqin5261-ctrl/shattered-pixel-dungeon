/*
 * Shattered Pixel Dungeon - Assist Edition
 * Infinite World post-breakthrough certificate.
 */
package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GenesisEcho;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class BreakthroughCertificate extends EquipableItem {

    private static final String CERT_LEVEL = "cert_level";
    private static final String REVIVE_CHARGE = "revive_charge";
    private static final String REGEN_PROGRESS = "regen_progress";
    private static final Class[] NEGATIVE_EFFECT_CLASSES = new Class[]{
            Burning.class, Poison.class, Paralysis.class, Vertigo.class,
            Blindness.class, Cripple.class, Weakness.class, Vulnerable.class,
            Slow.class, Charm.class, Hex.class
    };

    private static int consumableBoostDepth = 0;
    private static Hero consumableBoostHero = null;

    private int certificateLevel = 30;
    private float reviveCharge = 0f;
    private float regenProgress = 0f;

    {
        image = ItemSpriteSheet.TOKEN;
        unique = true;
        cursed = false;
        cursedKnown = true;
        levelKnown = true;
        keptThoughLostInvent = true;
        bones = false;
    }

    public int certificateLevel() {
        return certificateLevel;
    }

    public void syncToHeroLevel(Hero hero) {
        if (hero == null) return;
        int next = hero.lvl >= 60 ? 60 : hero.lvl >= 50 ? 50 : hero.lvl >= 40 ? 40 : 30;
        if (next > certificateLevel) {
            certificateLevel = next;
            reviveCharge = Math.min(reviveCharge, reviveChargeRequired());
            GLog.p("破界之印已经成长为 " + name() + "。");
            if (isEquipped(hero)) {
                hero.updateHT(true);
                Dungeon.observe();
            }
            Item.updateQuickslot();
        }

        // Level 60 is the permanent endgame awakening. This is intentionally
        // outside the 'next > certificateLevel' block so old lv60 saves also
        // receive Genesis Echo once after updating.
        if (hero.lvl >= 60 && certificateLevel >= 60 && Dungeon.infiniteWorld) {
            GenesisEcho.unlock(hero);
        }
    }

    public float healthMultiplier() {
        switch (certificateLevel) {
            case 60: return 1.30f;
            case 50: return 1.22f;
            case 40: return 1.15f;
            default:return 1.10f;
        }
    }

    public int strengthBonus() {
        switch (certificateLevel) {
            case 60: return 8;
            case 50: return 6;
            case 40: return 4;
            default:return 2;
        }
    }

    public float damageMultiplier() {
        switch (certificateLevel) {
            case 60: return 1.16f;
            case 50: return 1.12f;
            case 40: return 1.08f;
            default:return 1.05f;
        }
    }

    public float accuracyMultiplier() {
        switch (certificateLevel) {
            case 60: return 1.16f;
            case 50: return 1.12f;
            case 40: return 1.08f;
            default:return 1.05f;
        }
    }

    public float evasionMultiplier() {
        switch (certificateLevel) {
            case 60: return 1.32f;
            case 50: return 1.24f;
            case 40: return 1.16f;
            default:return 1.10f;
        }
    }

    public float expMultiplier() {
        switch (certificateLevel) {
            // At level 60 there is no further Hero level to gain, so the old
            // +60% experience bonus is removed instead of wasting a stat line.
            case 60: return 1.00f;
            case 50: return 1.40f;
            case 40: return 1.25f;
            default:return 1.15f;
        }
    }

    public float hungerMultiplier() {
        switch (certificateLevel) {
            case 60: return 0.62f;
            case 50: return 0.70f;
            case 40: return 0.78f;
            default:return 0.85f;
        }
    }

    public float goldMultiplier() {
        switch (certificateLevel) {
            case 60: return 1.30f;
            case 50: return 1.22f;
            case 40: return 1.15f;
            default:return 1.10f;
        }
    }

    public float speedMultiplier() {
        switch (certificateLevel) {
            case 60: return 1.25f;
            case 50: return 1.18f;
            case 40: return 1.12f;
            default:return 1.08f;
        }
    }

    public int visionBonus() {
        switch (certificateLevel) {
            case 60: return 4;
            case 50: return 3;
            case 40: return 2;
            default:return 1;
        }
    }

    public float shopPriceMultiplier() {
        switch (certificateLevel) {
            case 60: return 0.80f;
            case 50: return 0.84f;
            case 40: return 0.88f;
            default:return 0.92f;
        }
    }

    public float chestBonusChance() {
        switch (certificateLevel) {
            case 60: return 0.25f;
            case 50: return 0.17f;
            case 40: return 0.12f;
            default:return 0.08f;
        }
    }

    public float regenInterval() {
        switch (certificateLevel) {
            case 60: return 10f;
            case 50: return 12f;
            case 40: return 15f;
            default:return 18f;
        }
    }

    public float reviveChargeRequired() {
        switch (certificateLevel) {
            case 60: return 80f;
            case 50: return 120f;
            case 40: return 160f;
            default:return 200f;
        }
    }

    public float reviveHpFraction() {
        switch (certificateLevel) {
            case 60: return 0.70f;
            case 50: return 0.55f;
            case 40: return 0.45f;
            default:return 0.35f;
        }
    }

    public float damageTakenMultiplier() {
        switch (certificateLevel) {
            case 60: return 0.80f;
            case 50: return 0.84f;
            case 40: return 0.88f;
            default:return 0.92f;
        }
    }

    public float negativeEffectMultiplier() {
        switch (certificateLevel) {
            case 60: return 0.50f;
            case 50: return 0.65f;
            case 40: return 0.75f;
            default:return 0.85f;
        }
    }

    public float wandChargeMultiplier() {
        switch (certificateLevel) {
            case 60: return 1.80f;
            case 50: return 1.55f;
            case 40: return 1.35f;
            default:return 1.20f;
        }
    }

    public float consumableDurationMultiplier() {
        switch (certificateLevel) {
            case 60: return 1.80f;
            case 50: return 1.55f;
            case 40: return 1.35f;
            default:return 1.20f;
        }
    }

    public int searchDistanceBonus() {
        switch (certificateLevel) {
            case 60: return 4;
            case 50: return 3;
            case 40: return 2;
            default:return 1;
        }
    }

    public float searchChanceBonus() {
        switch (certificateLevel) {
            case 60: return 0.60f;
            case 50: return 0.45f;
            case 40: return 0.30f;
            default:return 0.20f;
        }
    }

    private int copies(Hero hero) {
        return certificateLevel >= 60 ? GenesisEcho.sealCopies(hero) : 1;
    }

    private float stackedMultiplier(float base, Hero hero) {
        int copies = copies(hero);
        float result = 1f;
        for (int i = 0; i < copies; i++) result *= base;
        return result;
    }

    public float effectiveHealthMultiplier(Hero hero) {
        return stackedMultiplier(healthMultiplier(), hero);
    }

    public int effectiveStrengthBonus(Hero hero) {
        return strengthBonus() * copies(hero);
    }

    public float effectiveDamageMultiplier(Hero hero) {
        return stackedMultiplier(damageMultiplier(), hero);
    }

    public float effectiveAccuracyMultiplier(Hero hero) {
        return stackedMultiplier(accuracyMultiplier(), hero);
    }

    public float effectiveEvasionMultiplier(Hero hero) {
        return stackedMultiplier(evasionMultiplier(), hero);
    }

    public float effectiveExpMultiplier(Hero hero) {
        return stackedMultiplier(expMultiplier(), hero);
    }

    public float effectiveHungerMultiplier(Hero hero) {
        return stackedMultiplier(hungerMultiplier(), hero);
    }

    public float effectiveGoldMultiplier(Hero hero) {
        return stackedMultiplier(goldMultiplier(), hero);
    }

    public float effectiveSpeedMultiplier(Hero hero) {
        return stackedMultiplier(speedMultiplier(), hero);
    }

    public int effectiveVisionBonus(Hero hero) {
        return visionBonus() * copies(hero);
    }

    public float effectiveShopPriceMultiplier(Hero hero) {
        return stackedMultiplier(shopPriceMultiplier(), hero);
    }

    public float effectiveChestBonusChance(Hero hero) {
        return Math.min(1f, chestBonusChance() * copies(hero));
    }

    public float effectiveDamageTakenMultiplier(Hero hero) {
        return stackedMultiplier(damageTakenMultiplier(), hero);
    }

    public float effectiveNegativeEffectMultiplier(Hero hero) {
        return stackedMultiplier(negativeEffectMultiplier(), hero);
    }

    public float effectiveWandChargeMultiplier(Hero hero) {
        return stackedMultiplier(wandChargeMultiplier(), hero);
    }

    public float effectiveConsumableDurationMultiplier(Hero hero) {
        return stackedMultiplier(consumableDurationMultiplier(), hero);
    }

    public int effectiveSearchDistanceBonus(Hero hero) {
        return searchDistanceBonus() * copies(hero);
    }

    public float effectiveSearchChanceBonus(Hero hero) {
        return Math.min(1f, searchChanceBonus() * copies(hero));
    }

    public static boolean isSupportedNegativeEffect(Class effect) {
        if (effect == null) return false;
        for (Class<?> cls : NEGATIVE_EFFECT_CLASSES) {
            if (cls.isAssignableFrom(effect)) return true;
        }
        return false;
    }

    public static void beginConsumableBoost(Hero hero) {
        if (hero == null) return;
        // 元级肇法 is a permanent tier-7 authority and must keep the >=999
        // consumable duration even after 奇迹·世界 is unequipped.
        if (equipped(hero) == null && !GenesisEcho.ultraSpellcast(hero)) return;
        consumableBoostHero = hero;
        consumableBoostDepth++;
    }

    public static void endConsumableBoost(Hero hero) {
        if (hero == null || hero != consumableBoostHero) return;
        consumableBoostDepth = Math.max(0, consumableBoostDepth - 1);
        if (consumableBoostDepth == 0) consumableBoostHero = null;
    }

    public static float adjustConsumableDuration(Char target, Class<? extends Buff> buffClass, float duration) {
        if (!(target instanceof Hero) || target != consumableBoostHero || consumableBoostDepth <= 0) return duration;
        if (isSupportedNegativeEffect(buffClass)) return duration;
        Hero hero = (Hero)target;
        if (GenesisEcho.ultraSpellcast(hero)) {
            return Math.max(999f, duration);
        }
        BreakthroughCertificate cert = equipped(hero);
        return cert == null ? duration : duration * cert.effectiveConsumableDurationMultiplier(hero);
    }

    public boolean reviveReady() {
        return reviveCharge >= reviveChargeRequired();
    }

    public int reviveChargePercent() {
        return Math.max(0, Math.min(100,
                Math.round(100f * reviveCharge / reviveChargeRequired())));
    }

    public void processHeroTime(Hero hero, float time) {
        if (hero == null || time <= 0f || !isEquipped(hero)) return;

        reviveCharge = Math.min(reviveChargeRequired(), reviveCharge + time);
        regenProgress += time;

        float interval = regenInterval();
        while (regenProgress >= interval) {
            regenProgress -= interval;
            if (hero.isAlive() && hero.HP > 0 && hero.HP < hero.HT) {
                int heal = Math.max(1, Math.round(hero.HT * 0.01f)) * copies(hero);
                hero.HP = Math.min(hero.HT, hero.HP + heal);
                if (hero.sprite != null) hero.sprite.showStatus(0x00FF00, "+" + heal);
            }
        }
        Item.updateQuickslot();
    }

    public boolean consumeRevive(Hero hero) {
        if (!reviveReady() || hero == null || !isEquipped(hero)) return false;
        reviveCharge = 0f;
        hero.HP = Math.max(1, Math.round(hero.HT * reviveHpFraction()));
        Item.updateQuickslot();
        return true;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.remove(AC_DROP);
        actions.remove(AC_THROW);
        return actions;
    }

    @Override
    public void doDrop(Hero hero) {
        GLog.w(name() + "无法被丢弃。");
    }

    @Override
    public void cast(final Hero user, int dst) {
        GLog.w(name() + "无法被投掷或摧毁。");
    }

    @Override
    public String name() {
        if (certificateLevel >= 60) return Messages.get(this, "miracle_name");
        return Messages.get(this, "name", certificateLevel);
    }

    @Override
    public String desc() {
        if (certificateLevel >= 60) {
            Hero hero = Dungeon.hero;
            boolean live = hero != null && isEquipped(hero);
            int liveCopies = live ? copies(hero) : 1;
            String result = Messages.get(this, "miracle_desc",
                    Math.round(((live ? effectiveHealthMultiplier(hero) : healthMultiplier())-1f)*100f),
                    live ? effectiveStrengthBonus(hero) : strengthBonus(),
                    Math.round(((live ? effectiveDamageMultiplier(hero) : damageMultiplier())-1f)*100f),
                    Math.round(((live ? effectiveAccuracyMultiplier(hero) : accuracyMultiplier())-1f)*100f),
                    Math.round(((live ? effectiveEvasionMultiplier(hero) : evasionMultiplier())-1f)*100f),
                    Math.round((1f-(live ? effectiveDamageTakenMultiplier(hero) : damageTakenMultiplier()))*100f),
                    Math.round((1f-(live ? effectiveNegativeEffectMultiplier(hero) : negativeEffectMultiplier()))*100f),
                    Math.round((1f-(live ? effectiveHungerMultiplier(hero) : hungerMultiplier()))*100f),
                    Math.round(((live ? effectiveGoldMultiplier(hero) : goldMultiplier())-1f)*100f),
                    Math.round(((live ? effectiveSpeedMultiplier(hero) : speedMultiplier())-1f)*100f),
                    live ? effectiveVisionBonus(hero) : visionBonus(),
                    live ? effectiveSearchDistanceBonus(hero) : searchDistanceBonus(),
                    Math.round((live ? effectiveSearchChanceBonus(hero) : searchChanceBonus())*100f),
                    Math.round(((live ? effectiveWandChargeMultiplier(hero) : wandChargeMultiplier())-1f)*100f),
                    Math.round(((live ? effectiveConsumableDurationMultiplier(hero) : consumableDurationMultiplier())-1f)*100f),
                    Math.round((1f-(live ? effectiveShopPriceMultiplier(hero) : shopPriceMultiplier()))*100f),
                    Math.round((live ? effectiveChestBonusChance(hero) : chestBonusChance())*100f),
                    Math.round(regenInterval()),
                    liveCopies,
                    Math.round(reviveChargeRequired()),
                    Math.round(reviveHpFraction()*100f),
                    reviveChargePercent());
            return result + "\n\n" + Messages.get(this, "miracle_layers", liveCopies);
        }

        return Messages.get(this, "desc",
                Math.round((healthMultiplier()-1f)*100f),
                strengthBonus(),
                Math.round((damageMultiplier()-1f)*100f),
                Math.round((accuracyMultiplier()-1f)*100f),
                Math.round((evasionMultiplier()-1f)*100f),
                Math.round((1f-damageTakenMultiplier())*100f),
                Math.round((1f-negativeEffectMultiplier())*100f),
                Math.round((expMultiplier()-1f)*100f),
                Math.round((1f-hungerMultiplier())*100f),
                Math.round((goldMultiplier()-1f)*100f),
                Math.round((speedMultiplier()-1f)*100f),
                visionBonus(),
                searchDistanceBonus(),
                Math.round(searchChanceBonus()*100f),
                Math.round((wandChargeMultiplier()-1f)*100f),
                Math.round((consumableDurationMultiplier()-1f)*100f),
                Math.round((1f-shopPriceMultiplier())*100f),
                Math.round(chestBonusChance()*100f),
                Math.round(regenInterval()),
                1,
                Math.round(reviveChargeRequired()),
                Math.round(reviveHpFraction()*100f),
                reviveChargePercent());
    }

    @Override
    public String status() {
        return reviveChargePercent() + "%";
    }

    @Override
    public boolean doEquip(Hero hero) {
        detach(hero.belongings.backpack);
        BreakthroughCertificate old = hero.belongings.breakthroughCertificate;
        if (old == null || old.doUnequip(hero, true, false)) {
            hero.belongings.breakthroughCertificate = this;
            syncToHeroLevel(hero);
            activate(hero);
            hero.updateHT(true);
            Dungeon.observe();
            hero.spendAndNext(timeToEquip(hero));
            return true;
        }
        collect(hero.belongings.backpack);
        return false;
    }

    public void forceEquip(Hero hero) {
        if (hero == null) return;
        detach(hero.belongings.backpack);
        hero.belongings.breakthroughCertificate = this;
        syncToHeroLevel(hero);
        activate(hero);
        hero.updateHT(true);
        Dungeon.observe();
        Item.updateQuickslot();
    }

    @Override
    public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        if (collect) makeRoomForSelfBeforeUnequip(hero);
        if (super.doUnequip(hero, collect, single)) {
            hero.belongings.breakthroughCertificate = null;
            BreakthroughBlessing buff = hero.buff(BreakthroughBlessing.class);
            if (buff != null) buff.detach();
            hero.updateHT(false);
            Dungeon.observe();
            return true;
        }
        return false;
    }

    @Override
    public boolean isEquipped(Hero hero) {
        return hero != null && hero.belongings.breakthroughCertificate() == this;
    }

    @Override
    public void activate(Char ch) {
        if (ch instanceof Hero) {
            BreakthroughBlessing blessing = Buff.affect(ch, BreakthroughBlessing.class);
            blessing.certificate = this;
        }
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public Item upgrade() {
        return this;
    }

    @Override
    public int value() {
        return 0;
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(CERT_LEVEL, certificateLevel);
        bundle.put(REVIVE_CHARGE, reviveCharge);
        bundle.put(REGEN_PROGRESS, regenProgress);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        certificateLevel = bundle.contains(CERT_LEVEL) ? bundle.getInt(CERT_LEVEL) : 30;
        reviveCharge = bundle.contains(REVIVE_CHARGE) ? bundle.getFloat(REVIVE_CHARGE) : 0f;
        regenProgress = bundle.contains(REGEN_PROGRESS) ? bundle.getFloat(REGEN_PROGRESS) : 0f;
    }

    private void makeRoomForSelfBeforeUnequip(Hero hero) {
        if (hero == null || Dungeon.level == null) return;
        Bag backpack = hero.belongings.backpack;
        if (backpack.items.size() < backpack.capacity()) return;

        Item chosen = null;
        for (Item item : backpack.items) {
            if (item == this) continue;
            if (!item.unique && !(item instanceof Bag)) {
                chosen = item;
                break;
            }
        }
        if (chosen == null) {
            for (Item item : backpack.items) {
                if (item != this) {
                    chosen = item;
                    break;
                }
            }
        }

        if (chosen != null) {
            chosen.detachAll(backpack);
            Dungeon.level.drop(chosen, hero.pos).sprite.drop();
            GLog.i("背包已满，为收回破界装备腾出的物品已安全放在角色身边。");
        }
    }

    public static BreakthroughCertificate equipped(Hero hero) {
        return hero == null ? null : hero.belongings.breakthroughCertificate();
    }

    private static int nextTier(int tier) {
        if (tier < 40) return 40;
        if (tier < 50) return 50;
        if (tier < 60) return 60;
        return 0;
    }

    private static BreakthroughCertificate previewForTier(int tier) {
        BreakthroughCertificate preview = new BreakthroughCertificate();
        preview.certificateLevel = tier;
        return preview;
    }

    public static class BreakthroughBlessing extends Buff {
        private BreakthroughCertificate certificate;

        {
            type = buffType.POSITIVE;
            announced = true;
            revivePersists = true;
        }

        @Override
        public boolean act() {
            spend(TICK);
            return true;
        }

        @Override
        public int icon() {
            return BuffIndicator.AMULET;
        }

        @Override
        public String iconTextDisplay() {
            BreakthroughCertificate cert = certificate;
            if (cert == null && target instanceof Hero) cert = equipped((Hero)target);
            return cert == null ? "" : Integer.toString(cert.certificateLevel());
        }

        @Override
        public String name() {
            BreakthroughCertificate cert = certificate;
            if (cert == null && target instanceof Hero) cert = equipped((Hero)target);
            if (cert == null) return Messages.get(BreakthroughCertificate.class, "blessing_name");
            if (cert.certificateLevel() >= 60) {
                return Messages.get(BreakthroughCertificate.class, "miracle_blessing_name");
            }
            return Messages.get(BreakthroughCertificate.class, "blessing_name_level", cert.certificateLevel());
        }

        @Override
        public String desc() {
            BreakthroughCertificate cert = certificate;
            if (cert == null && target instanceof Hero) cert = equipped((Hero)target);
            if (cert == null) return Messages.get(BreakthroughCertificate.class, "blessing_inactive");

            String current;
            if (cert.certificateLevel() >= 60) {
                Hero hero = target instanceof Hero ? (Hero)target : Dungeon.hero;
                int liveCopies = hero == null ? 1 : cert.copies(hero);
                current = Messages.get(BreakthroughCertificate.class, "miracle_blessing_desc",
                        Math.round((cert.effectiveHealthMultiplier(hero)-1f)*100f),
                        cert.effectiveStrengthBonus(hero),
                        Math.round((cert.effectiveDamageMultiplier(hero)-1f)*100f),
                        Math.round((cert.effectiveAccuracyMultiplier(hero)-1f)*100f),
                        Math.round((cert.effectiveEvasionMultiplier(hero)-1f)*100f),
                        Math.round((1f-cert.effectiveDamageTakenMultiplier(hero))*100f),
                        Math.round((1f-cert.effectiveNegativeEffectMultiplier(hero))*100f),
                        Math.round((1f-cert.effectiveHungerMultiplier(hero))*100f),
                        Math.round((cert.effectiveGoldMultiplier(hero)-1f)*100f),
                        Math.round((cert.effectiveSpeedMultiplier(hero)-1f)*100f),
                        cert.effectiveVisionBonus(hero),
                        cert.effectiveSearchDistanceBonus(hero),
                        Math.round(cert.effectiveSearchChanceBonus(hero)*100f),
                        Math.round((cert.effectiveWandChargeMultiplier(hero)-1f)*100f),
                        Math.round((cert.effectiveConsumableDurationMultiplier(hero)-1f)*100f),
                        Math.round((1f-cert.effectiveShopPriceMultiplier(hero))*100f),
                        Math.round(cert.effectiveChestBonusChance(hero)*100f),
                        Math.round(cert.regenInterval()),
                        liveCopies,
                        Math.round(cert.reviveChargeRequired()),
                        Math.round(cert.reviveHpFraction()*100f),
                        cert.reviveChargePercent());
                current += "\n\n" + Messages.get(BreakthroughCertificate.class, "miracle_layers", liveCopies);
            } else {
                current = Messages.get(BreakthroughCertificate.class, "blessing_desc",
                        cert.certificateLevel(),
                        Math.round((cert.healthMultiplier()-1f)*100f),
                        cert.strengthBonus(),
                        Math.round((cert.damageMultiplier()-1f)*100f),
                        Math.round((cert.accuracyMultiplier()-1f)*100f),
                        Math.round((cert.evasionMultiplier()-1f)*100f),
                        Math.round((1f-cert.damageTakenMultiplier())*100f),
                        Math.round((1f-cert.negativeEffectMultiplier())*100f),
                        Math.round((cert.expMultiplier()-1f)*100f),
                        Math.round((1f-cert.hungerMultiplier())*100f),
                        Math.round((cert.goldMultiplier()-1f)*100f),
                        Math.round((cert.speedMultiplier()-1f)*100f),
                        cert.visionBonus(),
                        cert.searchDistanceBonus(),
                        Math.round(cert.searchChanceBonus()*100f),
                        Math.round((cert.wandChargeMultiplier()-1f)*100f),
                        Math.round((cert.consumableDurationMultiplier()-1f)*100f),
                        Math.round((1f-cert.shopPriceMultiplier())*100f),
                        Math.round(cert.chestBonusChance()*100f),
                        Math.round(cert.regenInterval()),
                        1,
                        Math.round(cert.reviveChargeRequired()),
                        Math.round(cert.reviveHpFraction()*100f),
                        cert.reviveChargePercent());
            }

            int next = nextTier(cert.certificateLevel());
            if (next == 0) {
                return current + "\n\n" + Messages.get(BreakthroughCertificate.class, "blessing_max");
            }

            BreakthroughCertificate preview = previewForTier(next);
            return current + "\n\n" + Messages.get(BreakthroughCertificate.class, "blessing_next",
                    next,
                    Math.round((preview.healthMultiplier()-1f)*100f),
                    preview.strengthBonus(),
                    Math.round((preview.damageMultiplier()-1f)*100f),
                    Math.round((preview.accuracyMultiplier()-1f)*100f),
                    Math.round((preview.evasionMultiplier()-1f)*100f),
                    Math.round((1f-preview.damageTakenMultiplier())*100f),
                    Math.round((1f-preview.negativeEffectMultiplier())*100f),
                    Math.round((preview.expMultiplier()-1f)*100f),
                    Math.round((1f-preview.hungerMultiplier())*100f),
                    Math.round((preview.goldMultiplier()-1f)*100f),
                    Math.round((preview.speedMultiplier()-1f)*100f),
                    preview.visionBonus(),
                    preview.searchDistanceBonus(),
                    Math.round(preview.searchChanceBonus()*100f),
                    Math.round((preview.wandChargeMultiplier()-1f)*100f),
                    Math.round((preview.consumableDurationMultiplier()-1f)*100f),
                    Math.round((1f-preview.shopPriceMultiplier())*100f),
                    Math.round(preview.chestBonusChance()*100f),
                    Math.round(preview.regenInterval()),
                    1,
                    Math.round(preview.reviveChargeRequired()),
                    Math.round(preview.reviveHpFraction()*100f));
        }
    }
}

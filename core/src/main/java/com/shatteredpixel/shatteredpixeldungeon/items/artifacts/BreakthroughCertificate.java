/*
 * Shattered Pixel Dungeon - Assist Edition
 * Infinite World post-breakthrough certificate.
 */
package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class BreakthroughCertificate extends EquipableItem {

    private static final String CERT_LEVEL = "cert_level";
    private static final String REVIVE_CHARGE = "revive_charge";
    private static final String REGEN_PROGRESS = "regen_progress";

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
            GLog.p("突破之证已经成长为 " + name() + "。");
            if (isEquipped(hero)) {
                hero.updateHT(true);
                Dungeon.observe();
            }
            Item.updateQuickslot();
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
            case 60: return 4;
            case 50: return 3;
            case 40: return 2;
            default:return 1;
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
            case 60: return 1.14f;
            case 50: return 1.10f;
            case 40: return 1.07f;
            default:return 1.04f;
        }
    }

    public float expMultiplier() {
        switch (certificateLevel) {
            case 60: return 1.15f;
            case 50: return 1.12f;
            case 40: return 1.08f;
            default:return 1.05f;
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
            case 60: return 1.10f;
            case 50: return 1.07f;
            case 40: return 1.05f;
            default:return 1.03f;
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
            case 60: return 180f;
            case 50: return 220f;
            case 40: return 260f;
            default:return 300f;
        }
    }

    public float reviveHpFraction() {
        switch (certificateLevel) {
            case 60: return 0.50f;
            case 50: return 0.42f;
            case 40: return 0.36f;
            default:return 0.30f;
        }
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
                int heal = Math.max(1, Math.round(hero.HT * 0.01f));
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
    public String name() {
        return Messages.get(this, "name", certificateLevel);
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc",
                Math.round((healthMultiplier()-1f)*100f),
                strengthBonus(),
                Math.round((damageMultiplier()-1f)*100f),
                Math.round((accuracyMultiplier()-1f)*100f),
                Math.round((evasionMultiplier()-1f)*100f),
                Math.round((expMultiplier()-1f)*100f),
                Math.round((1f-hungerMultiplier())*100f),
                Math.round((goldMultiplier()-1f)*100f),
                Math.round((speedMultiplier()-1f)*100f),
                visionBonus(),
                Math.round((1f-shopPriceMultiplier())*100f),
                Math.round(chestBonusChance()*100f),
                Math.round(regenInterval()),
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
            return cert == null
                    ? Messages.get(BreakthroughCertificate.class, "blessing_name")
                    : Messages.get(BreakthroughCertificate.class, "blessing_name_level", cert.certificateLevel());
        }

        @Override
        public String desc() {
            BreakthroughCertificate cert = certificate;
            if (cert == null && target instanceof Hero) cert = equipped((Hero)target);
            if (cert == null) return Messages.get(BreakthroughCertificate.class, "blessing_inactive");

            String current = Messages.get(BreakthroughCertificate.class, "blessing_desc",
                    cert.certificateLevel(),
                    Math.round((cert.healthMultiplier()-1f)*100f),
                    cert.strengthBonus(),
                    Math.round((cert.damageMultiplier()-1f)*100f),
                    Math.round((cert.accuracyMultiplier()-1f)*100f),
                    Math.round((cert.evasionMultiplier()-1f)*100f),
                    Math.round((cert.expMultiplier()-1f)*100f),
                    Math.round((1f-cert.hungerMultiplier())*100f),
                    Math.round((cert.goldMultiplier()-1f)*100f),
                    Math.round((cert.speedMultiplier()-1f)*100f),
                    cert.visionBonus(),
                    Math.round((1f-cert.shopPriceMultiplier())*100f),
                    Math.round(cert.chestBonusChance()*100f),
                    Math.round(cert.regenInterval()),
                    Math.round(cert.reviveChargeRequired()),
                    Math.round(cert.reviveHpFraction()*100f),
                    cert.reviveChargePercent());

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
                    Math.round((preview.expMultiplier()-1f)*100f),
                    Math.round((1f-preview.hungerMultiplier())*100f),
                    Math.round((preview.goldMultiplier()-1f)*100f),
                    Math.round((preview.speedMultiplier()-1f)*100f),
                    preview.visionBonus(),
                    Math.round((1f-preview.shopPriceMultiplier())*100f),
                    Math.round(preview.chestBonusChance()*100f),
                    Math.round(preview.regenInterval()),
                    Math.round(preview.reviveChargeRequired()),
                    Math.round(preview.reviveHpFraction()*100f));
        }
    }
}

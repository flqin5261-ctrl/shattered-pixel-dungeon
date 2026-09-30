/*
 * Shattered Pixel Dungeon - Assist Edition
 * Permanent level-60 Infinite World endgame buff.
 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.BreakthroughCertificate;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldProgression;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldState;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.HashSet;

public class GenesisEcho extends Buff {

    {
        type = buffType.POSITIVE;
        announced = true;
        revivePersists = true;

        // Permanent control immunity: these remain active even when 奇迹·世界
        // is unequipped. This is real Char immunity, not description-only text.
        immunities.add(Paralysis.class);
        immunities.add(Roots.class);
        immunities.add(Vertigo.class);
        immunities.add(Blindness.class);
        immunities.add(Daze.class);
        immunities.add(Cripple.class);
        immunities.add(Charm.class);
        immunities.add(Terror.class);
        immunities.add(Amok.class);
        immunities.add(Drowsy.class);
        immunities.add(Sleep.class);
        immunities.add(Slow.class);
        immunities.add(Chill.class);
        immunities.add(Frost.class);
    }

    @Override
    public HashSet<Class> immunities() {
        HashSet<Class> result = super.immunities();

        Hero hero = target instanceof Hero ? (Hero)target : Dungeon.hero;
        if (miracleLinked(hero)) {
            // 奇迹·世界 linked curse layer: damage-over-time and output/defense
            // degrading states are blocked at Buff.attachTo()/Char.isImmune().
            result.add(Burning.class);
            result.add(Poison.class);
            result.add(Bleeding.class);
            result.add(Corrosion.class);
            result.add(Ooze.class);
            result.add(Weakness.class);
            result.add(Vulnerable.class);
            result.add(Degrade.class);
            result.add(Hex.class);
            result.add(Doom.class);
        }

        return result;
    }

    public static boolean blocksNegativeBuff(Hero hero, Buff buff) {
        if (hero == null || buff == null || !miracleLinked(hero)) return false;

        // This second gate catches harmful NEGATIVE buffs which are added by new
        // content but are not yet in the explicit immunity set above. Hunger is
        // neutral and is intentionally not classified as a curse.
        return buff.type == buffType.NEGATIVE;
    }

    public static boolean damageImmune(Hero hero) {
        return miracleLinked(hero);
    }

    private static boolean controlClass(Class<?> cls) {
        return cls != null && (
                Paralysis.class.isAssignableFrom(cls)
                || Roots.class.isAssignableFrom(cls)
                || Vertigo.class.isAssignableFrom(cls)
                || Blindness.class.isAssignableFrom(cls)
                || Daze.class.isAssignableFrom(cls)
                || Cripple.class.isAssignableFrom(cls)
                || Charm.class.isAssignableFrom(cls)
                || Terror.class.isAssignableFrom(cls)
                || Amok.class.isAssignableFrom(cls)
                || Drowsy.class.isAssignableFrom(cls)
                || Sleep.class.isAssignableFrom(cls)
                || Slow.class.isAssignableFrom(cls)
                || Chill.class.isAssignableFrom(cls)
                || Frost.class.isAssignableFrom(cls));
    }

    private static boolean curseClass(Class<?> cls) {
        return cls != null && (
                Burning.class.isAssignableFrom(cls)
                || Poison.class.isAssignableFrom(cls)
                || Bleeding.class.isAssignableFrom(cls)
                || Corrosion.class.isAssignableFrom(cls)
                || Ooze.class.isAssignableFrom(cls)
                || Weakness.class.isAssignableFrom(cls)
                || Vulnerable.class.isAssignableFrom(cls)
                || Degrade.class.isAssignableFrom(cls)
                || Hex.class.isAssignableFrom(cls)
                || Doom.class.isAssignableFrom(cls));
    }

    public static void cleanseBlockedEffects(Hero hero) {
        if (hero == null || !active(hero)) return;
        boolean linked = miracleLinked(hero);

        for (Buff buff : hero.buffs()) {
            if (buff instanceof GenesisEcho) continue;
            Class<?> cls = buff.getClass();
            if (controlClass(cls) || (linked && (curseClass(cls) || buff.type == buffType.NEGATIVE))) {
                buff.detach();
            }
        }
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
        return "∞";
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        Hero hero = target instanceof Hero ? (Hero)target : Dungeon.hero;
        BreakthroughCertificate seal = BreakthroughCertificate.equipped(hero);
        boolean linked = seal != null && seal.certificateLevel() >= 60;

        InfiniteWorldState st = InfiniteWorldProgression.state();
        if (!linked) {
            return Messages.get(this, "desc_unlinked",
                    st.genesisKillHpBonus,
                    st.genesisKillStrBonus);
        }

        return Messages.get(this, "desc_linked",
                st.genesisKillHpBonus,
                st.genesisKillStrBonus,
                Math.round((seal.healthMultiplier()-1f)*100f),
                seal.strengthBonus(),
                Math.round((seal.damageMultiplier()-1f)*100f),
                Math.round((seal.accuracyMultiplier()-1f)*100f),
                Math.round((seal.evasionMultiplier()-1f)*100f),
                Math.round((1f-seal.damageTakenMultiplier())*100f),
                Math.round((1f-seal.negativeEffectMultiplier())*100f),
                Math.round((1f-seal.hungerMultiplier())*100f),
                Math.round((seal.goldMultiplier()-1f)*100f),
                Math.round((seal.speedMultiplier()-1f)*100f),
                seal.visionBonus(),
                seal.searchDistanceBonus(),
                Math.round(seal.searchChanceBonus()*100f),
                Math.round((seal.wandChargeMultiplier()-1f)*100f),
                Math.round((seal.consumableDurationMultiplier()-1f)*100f),
                Math.round((1f-seal.shopPriceMultiplier())*100f),
                Math.round(seal.chestBonusChance()*100f),
                Math.round(seal.regenInterval()));
    }

    public static boolean active(Hero hero) {
        return hero != null && hero.buff(GenesisEcho.class) != null;
    }

    public static GenesisEcho unlock(Hero hero) {
        if (hero == null || !Dungeon.infiniteWorld) return null;
        InfiniteWorldState st = InfiniteWorldProgression.state();
        boolean first = !st.genesisEchoUnlocked;
        st.genesisEchoUnlocked = true;

        GenesisEcho echo = Buff.affect(hero, GenesisEcho.class);
        cleanseBlockedEffects(hero);
        if (first) {
            GLog.p("奇迹·世界完成升格——常驻权能「创世回响」已经觉醒。");
        }
        return echo;
    }

    public static void ensure(Hero hero) {
        if (hero == null || !Dungeon.infiniteWorld) return;
        InfiniteWorldState st = InfiniteWorldProgression.state();
        if (st.genesisEchoUnlocked && hero.buff(GenesisEcho.class) == null) {
            Buff.affect(hero, GenesisEcho.class);
        }
        if (st.genesisEchoUnlocked) cleanseBlockedEffects(hero);
    }

    public static boolean miracleLinked(Hero hero) {
        if (!active(hero)) return false;
        BreakthroughCertificate seal = BreakthroughCertificate.equipped(hero);
        return seal != null && seal.certificateLevel() >= 60;
    }

    // One normal equipment layer, plus a second identical layer while Genesis
    // Echo is linked to the equipped level-60 Miracle World.
    public static int sealCopies(Hero hero) {
        return miracleLinked(hero) ? 2 : 1;
    }

    public static boolean blocksInstantDeath(Hero hero, Object cause) {
        if (!active(hero) || cause == null) return false;

        // Direct die() calls normally arrive while HP is still positive.
        if (hero.HP > 0) return true;

        Class<?> cls = cause instanceof Class ? (Class<?>)cause : cause.getClass();
        String n = cls.getSimpleName().toLowerCase();
        return n.contains("grim")
                || n.contains("doom")
                || n.contains("deathmark")
                || n.contains("sacrific")
                || n.contains("execute");
    }

    public static boolean blocksForcedTeleport(Char ch, Class source) {
        if (!(ch instanceof Hero) || !active((Hero)ch)) return false;

        // The player's own Teleportation Scroll remains voluntary.
        return source == null
                || !"ScrollOfTeleportation".equals(source.getSimpleName());
    }

    public static void onEnemySlain(Hero hero) {
        if (hero == null || !active(hero) || !Dungeon.infiniteWorld) return;

        InfiniteWorldState st = InfiniteWorldProgression.state();
        st.genesisKillHpBonus++;
        st.genesisKillStrBonus++;

        // updateHT(true) also heals the newly gained permanent max-HP point.
        hero.updateHT(true);
    }

    public static int permanentHpBonus(Hero hero) {
        if (hero == null || !active(hero) || !Dungeon.infiniteWorld) return 0;
        return InfiniteWorldProgression.state().genesisKillHpBonus;
    }

    public static int permanentStrBonus(Hero hero) {
        if (hero == null || !active(hero) || !Dungeon.infiniteWorld) return 0;
        return InfiniteWorldProgression.state().genesisKillStrBonus;
    }

    public static boolean freeShop(Hero hero) {
        return active(hero);
    }

    // Geometric distribution: most purchases give no extra copy, some give one
    // or more, and there is intentionally no fixed numeric maximum.
    public static int bonusPurchaseCopies(Hero hero) {
        if (!active(hero)) return 0;
        int extra = 0;
        while (Random.Float() < 0.35f) extra++;
        return extra;
    }
}

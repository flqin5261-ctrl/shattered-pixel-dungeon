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

public class GenesisEcho extends Buff {

    {
        type = buffType.POSITIVE;
        announced = true;
        revivePersists = true;

        // Core crowd-control immunity. These remain even when Miracle Echo is
        // unequipped; the copied equipment bonuses are handled separately.
        immunities.add(Paralysis.class);
        immunities.add(Roots.class);
        immunities.add(Vertigo.class);
        immunities.add(Charm.class);
        immunities.add(Terror.class);
        immunities.add(Amok.class);
        immunities.add(Drowsy.class);
        immunities.add(Sleep.class);
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
        return Messages.get(this, linked ? "desc_linked" : "desc_unlinked",
                st.genesisKillHpBonus,
                st.genesisKillStrBonus);
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
        if (first) {
            GLog.p("奇迹·回响完成升格——常驻权能「创世回响」已经觉醒。");
        }
        return echo;
    }

    public static void ensure(Hero hero) {
        if (hero == null || !Dungeon.infiniteWorld) return;
        InfiniteWorldState st = InfiniteWorldProgression.state();
        if (st.genesisEchoUnlocked && hero.buff(GenesisEcho.class) == null) {
            Buff.affect(hero, GenesisEcho.class);
        }
    }

    public static boolean miracleLinked(Hero hero) {
        if (!active(hero)) return false;
        BreakthroughCertificate seal = BreakthroughCertificate.equipped(hero);
        return seal != null && seal.certificateLevel() >= 60;
    }

    // One normal equipment layer, plus a second identical layer while Genesis
    // Echo is linked to the equipped level-60 Miracle Echo.
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

/*
 * Shattered Pixel Dungeon - Assist Edition
 * Permanent level-60 Infinite World endgame buff.
 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.BreakthroughCertificate;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldProgression;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldState;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
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
        immunities.add(Dread.class);
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

    public static boolean blocksByAuthority(Hero hero, Class<?> cls) {
        if (hero == null || cls == null || !active(hero)) return false;
        return controlClass(cls) || (miracleLinked(hero) && curseClass(cls));
    }

    public static void showImmunityFeedback(Hero hero, Class<?> cls) {
        if (hero == null || cls == null || !active(hero)) return;
        String text;
        if (Burning.class.isAssignableFrom(cls)) text = "灼烧敕免";
        else if (Poison.class.isAssignableFrom(cls)) text = "中毒敕免";
        else if (Bleeding.class.isAssignableFrom(cls)) text = "流血敕免";
        else if (Corrosion.class.isAssignableFrom(cls)) text = "腐蚀敕免";
        else if (Ooze.class.isAssignableFrom(cls)) text = "黏液敕免";
        else if (Paralysis.class.isAssignableFrom(cls)) text = "麻痹敕免";
        else if (Roots.class.isAssignableFrom(cls)) text = "束缚敕免";
        else if (Blindness.class.isAssignableFrom(cls)) text = "致盲敕免";
        else if (Daze.class.isAssignableFrom(cls)) text = "眩晕敕免";
        else if (Vertigo.class.isAssignableFrom(cls)) text = "混乱敕免";
        else if (Cripple.class.isAssignableFrom(cls)) text = "残废敕免";
        else if (Charm.class.isAssignableFrom(cls)) text = "魅惑敕免";
        else if (Terror.class.isAssignableFrom(cls) || Dread.class.isAssignableFrom(cls)) text = "恐惧敕免";
        else if (Amok.class.isAssignableFrom(cls)) text = "狂乱敕免";
        else if (Drowsy.class.isAssignableFrom(cls) || Sleep.class.isAssignableFrom(cls)) text = "睡眠敕免";
        else if (Slow.class.isAssignableFrom(cls)) text = "迟缓敕免";
        else if (Chill.class.isAssignableFrom(cls) || Frost.class.isAssignableFrom(cls)) text = "寒冷敕免";
        else if (Weakness.class.isAssignableFrom(cls)) text = "虚弱敕免";
        else if (Vulnerable.class.isAssignableFrom(cls)) text = "易伤敕免";
        else if (Degrade.class.isAssignableFrom(cls)) text = "降级敕免";
        else if (Hex.class.isAssignableFrom(cls) || Doom.class.isAssignableFrom(cls)) text = "诅咒敕免";
        else text = "负面敕免";
        GLog.p(text);
        if (hero.sprite != null) hero.sprite.showStatus(0x66FFCC, text);
    }

    public static boolean damageImmune(Hero hero) {
        return active(hero);
    }

    public static void showForcedMovementDenial(Hero hero) {
        if (hero == null || !active(hero)) return;
        String text = "位移敕免";
        GLog.p(text);
        if (hero.sprite != null) hero.sprite.showStatus(0x66FFCC, text);
    }

    public static void showForcedTeleportDenial(Hero hero) {
        if (hero == null || !active(hero)) return;
        String text = "传送敕免";
        GLog.p(text);
        if (hero.sprite != null) hero.sprite.showStatus(0x66FFCC, text);
    }

    public static void showCurseDenial(Hero hero) {
        if (hero == null || !miracleLinked(hero)) return;
        String text = "诅咒敕免";
        GLog.p(text);
        if (hero.sprite != null) hero.sprite.showStatus(0x66FFCC, text);
    }

    public static void showDamageDenial(Hero hero, Object source) {
        if (hero == null || !active(hero)) return;
        String text = "伤害敕免";
        if (source != null) {
            Class<?> cls = source instanceof Class ? (Class<?>)source : source.getClass();
            if (Burning.class.isAssignableFrom(cls)) text = "灼烧敕免";
            else if (Poison.class.isAssignableFrom(cls)) text = "中毒敕免";
            else if (Bleeding.class.isAssignableFrom(cls)) text = "流血敕免";
            else if (Corrosion.class.isAssignableFrom(cls)) text = "腐蚀敕免";
            else if (Ooze.class.isAssignableFrom(cls)) text = "黏液敕免";
        }
        GLog.p(text);
        if (hero.sprite != null) hero.sprite.showStatus(0x66FFCC, text);
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
                || Dread.class.isAssignableFrom(cls)
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
        Hero hero = target instanceof Hero ? (Hero)target : null;
        if (hero != null && hero.isAlive()) {
            hero.HP = hero.HT;
            if (hero.buff(EquipmentSummary.class) == null) {
                Buff.affect(hero, EquipmentSummary.class);
            }
            // Repair/keep alive every stacked artifact's own native runtime.
            // This preserves each artifact's original recharge/cooldown formula.
            for (Artifact artifact : hero.belongings.equippedArtifacts()) {
                if (artifact != null) artifact.ensurePassiveRuntime(hero);
            }
        }
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
        String base = Messages.get(this, miracleLinked(hero) ? "desc_linked" : "desc_unlinked");
        if (hero == null) return base;
        return base + Messages.get(this, "live_stats", hero.STR());
    }

    public static boolean active(Hero hero) {
        return hero != null && hero.buff(GenesisEcho.class) != null;
    }

    public static GenesisEcho unlock(Hero hero) {
        if (hero == null || !Dungeon.infiniteWorld) return null;
        InfiniteWorldState st = InfiniteWorldProgression.state();
        boolean first = !st.genesisEchoUnlocked;
        st.genesisEchoUnlocked = true;
        retireLegacyKillGrowth(st);

        GenesisEcho echo = Buff.affect(hero, GenesisEcho.class);
        Buff.affect(hero, EquipmentSummary.class);

        // Attaching Genesis Echo can instantly enable Miracle Talent Mastery and
        // the second level-60 seal layer. Recompute cached HP/FOV only after the
        // buff exists, otherwise the awakening frame still shows the old values.
        hero.updateHT(true);
        if (Dungeon.level != null && Dungeon.hero == hero) Dungeon.observe();

        cleanseBlockedEffects(hero);
        identifyOwnedItems(hero);
        hero.HP = hero.HT;
        if (first) {
            GLog.p("奇迹·世界完成升格——「八荒·亘古元敕」已经觉醒。");
        }
        return echo;
    }

    public static void ensure(Hero hero) {
        if (hero == null || !Dungeon.infiniteWorld) return;
        InfiniteWorldState st = InfiniteWorldProgression.state();
        boolean growthMigrated = st.genesisEchoUnlocked && retireLegacyKillGrowth(st);
        boolean echoAttached = false;
        if (st.genesisEchoUnlocked && hero.buff(GenesisEcho.class) == null) {
            Buff.affect(hero, GenesisEcho.class);
            echoAttached = true;
        }
        if (growthMigrated || echoAttached) {
            hero.updateHT(true);
            if (Dungeon.level != null && Dungeon.hero == hero) Dungeon.observe();
        }
        if (st.genesisEchoUnlocked) {
            cleanseBlockedEffects(hero);
            identifyOwnedItems(hero);
            hero.HP = hero.HT;
            Buff.affect(hero, EquipmentSummary.class);
            for (Artifact artifact : hero.belongings.equippedArtifacts()) {
                if (artifact != null) artifact.ensurePassiveRuntime(hero);
            }
            syncTier7Buff(hero);
        }
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

    public static boolean miracleTalentMastery(Hero hero) {
        return miracleLinked(hero);
    }

    public static Talent selectedTier7(Hero hero) {
        if (hero == null || !active(hero) || hero.talents.size() < 7) return null;
        for (java.util.Map.Entry<Talent, Integer> e : hero.talents.get(6).entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) return e.getKey();
        }
        return null;
    }

    public static boolean hasTier7(Hero hero, Talent talent) {
        return talent != null && talent == selectedTier7(hero);
    }

    public static boolean ultraReach(Hero hero) {
        return hasTier7(hero, Talent.GENESIS_REACH);
    }

    public static boolean ultraTeleport(Hero hero) {
        return hasTier7(hero, Talent.GENESIS_TELEPORT);
    }

    public static boolean ultraSpellcast(Hero hero) {
        return hasTier7(hero, Talent.GENESIS_SPELLCAST);
    }

    public static boolean ultraFortune(Hero hero) {
        return hasTier7(hero, Talent.GENESIS_FORTUNE);
    }

    public static void syncTier7Buff(Hero hero) {
        if (hero == null) return;
        Talent selected = selectedTier7(hero);
        GenesisTalentAuthority buff = hero.buff(GenesisTalentAuthority.class);
        if (selected == null) {
            if (buff != null) buff.detach();
        } else if (buff == null) {
            Buff.affect(hero, GenesisTalentAuthority.class);
        }
    }

    public static boolean miracleExecutionActive(Hero hero) {
        return miracleLinked(hero);
    }

    public static boolean wandExecutionActive(Hero hero) {
        return miracleExecutionActive(hero) || ultraSpellcast(hero);
    }

    public static boolean tryMiracleExecute(Hero hero, Char enemy) {
        if (!miracleExecutionActive(hero) || enemy == null || enemy == hero || !enemy.isAlive()) return false;
        if (enemy.alignment == Char.Alignment.ALLY) return false;
        forceSlay(hero, enemy);
        return true;
    }

    public static boolean tryWandExecute(Hero hero, Char enemy) {
        if (!wandExecutionActive(hero) || enemy == null || enemy == hero || !enemy.isAlive()) return false;
        if (enemy.alignment == Char.Alignment.ALLY) return false;
        forceSlay(hero, enemy);
        return true;
    }

    public static void forceSlay(Hero hero, Char enemy) {
        if (hero == null || enemy == null || enemy == hero || !enemy.isAlive()) return;
        if (enemy.alignment == Char.Alignment.ALLY) return;
        enemy.deathMarked = false;
        enemy.HP = 0;
        enemy.die(hero);
        if (enemy.isAlive()) {
            enemy.deathMarked = false;
            enemy.destroy();
            if (enemy.sprite != null) enemy.sprite.die();
        }
        GLog.p("敕杀：" + enemy.name());
    }

    public static boolean teleportToVisited(Hero hero, int cell) {
        if (!ultraTeleport(hero) || Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return false;
        if (!Dungeon.level.visited[cell]) {
            GLog.w("该位置仍被迷雾遮蔽，无法施行无界遁诰。");
            return false;
        }
        if ((!Dungeon.level.passable[cell] && !Dungeon.level.avoid[cell])
                || (Actor.findChar(cell) != null && Actor.findChar(cell) != hero)) {
            GLog.w("该位置无法落脚。");
            return false;
        }

        // Clear any queued walk before moving. ScrollOfTeleportation.appear()
        // already calls Hero.move(..., false), which performs occupyCell and
        // InfiniteWorld recordHeroMove exactly once.
        hero.interrupt();
        ScrollOfTeleportation.appear(hero, cell, 0.12f);
        hero.resetNavigationAfterTeleport();

        if (Dungeon.level instanceof InfiniteWorldLevel) {
            ((InfiniteWorldLevel)Dungeon.level).beginGenesisTeleportGrace(hero);
        }

        // Dungeon.observe() already performs the required fog update, so do not
        // issue a second full GameScene.updateFog() here.
        Dungeon.observe();

        GLog.p("无界遁诰");
        return true;
    }

    public static void onUltraWandZap(Hero hero, int aimedCell, int collisionCell) {
        if (hero == null || !wandExecutionActive(hero)) return;
        Char target = Actor.findChar(aimedCell);
        if (target == null) target = Actor.findChar(collisionCell);
        tryWandExecute(hero, target);
    }

    public static class EquipmentSummary extends Buff {
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
            return BuffIndicator.WEAPON;
        }

        @Override
        public String iconTextDisplay() {
            Hero hero = target instanceof Hero ? (Hero)target : Dungeon.hero;
            if (hero == null) return "+";
            return Integer.toString(Math.min(99, hero.belongings.equippedWeapons().size()));
        }

        @Override
        public String name() {
            return "纵横八荒·装备共鸣";
        }

        @Override
        public String desc() {
            Hero hero = target instanceof Hero ? (Hero)target : Dungeon.hero;
            if (hero == null) return "当前没有可读取的装备数据。";

            StringBuilder out = new StringBuilder();
            out.append("_实战数据_\n");

            java.util.ArrayList<KindOfWeapon> weapons = hero.belongings.equippedWeapons();
            int weaponMin = 0;
            int weaponMax = 0;
            int weaponBlock = 0;
            for (KindOfWeapon weapon : weapons) {
                if (weapon == null) continue;
                weaponMin += Math.max(0, weapon.min());
                weaponMax += Math.max(0, weapon.max());
                weaponBlock += Math.max(0, weapon.defenseFactor(hero));
            }
            KindOfWeapon primary = hero.belongings.attackingWeapon();
            if (!weapons.isEmpty() && !(primary instanceof MissileWeapon)) {
                int force = RingOfForce.armedDamageBonus(hero);
                weaponMin += force;
                weaponMax += force;
            }

            BreakthroughCertificate cert = BreakthroughCertificate.equipped(hero);
            if (cert != null) {
                float mult = cert.effectiveDamageMultiplier(hero);
                weaponMin = Math.round(weaponMin * mult);
                weaponMax = Math.round(weaponMax * mult);
            }

            int unarmedMin = RingOfForce.unarmedMin(hero);
            int unarmedMax = RingOfForce.unarmedMax(hero);
            if (cert != null) {
                float mult = cert.effectiveDamageMultiplier(hero);
                unarmedMin = Math.round(unarmedMin * mult);
                unarmedMax = Math.round(unarmedMax * mult);
            }

            out.append("武器：").append(weapons.size()).append("件");
            if (!weapons.isEmpty()) {
                out.append("，合击伤害 ").append(weaponMin).append("～").append(weaponMax);
            }
            out.append("\n空手伤害：").append(unarmedMin).append("～").append(unarmedMax);

            int armorMin = 0;
            int armorMax = 0;
            java.util.ArrayList<Armor> armors = hero.belongings.equippedArmors();
            for (Armor armor : armors) {
                if (armor == null) continue;
                armorMin += Math.max(0, armor.DRMin());
                armorMax += Math.max(0, armor.DRMax());
            }
            out.append("\n装备减伤：").append(armorMin).append("～").append(armorMax + weaponBlock);
            if (weaponBlock > 0) {
                out.append("（武器格挡上限 +").append(weaponBlock).append("）");
            }
            out.append("\n防具：").append(armors.size()).append("件，护甲值真实叠加");
            Armor primaryArmor = hero.belongings.armor();
            if (primaryArmor != null) {
                out.append("；主防具：").append(primaryArmor.name());
                out.append("。破损纹章仅由主防具承载并触发护盾");
            }

            java.util.ArrayList<Ring> rings = hero.belongings.equippedRings();
            out.append("\n\n_戒指真实叠加_\n共 ").append(rings.size()).append(" 枚");
            HashSet<Class> seen = new HashSet<>();
            for (Ring ring : rings) {
                if (ring == null || !seen.add(ring.getClass())) continue;
                String info = ring.stackedStatsInfo();
                if (info == null) info = "";
                info = info.replace("\n\n", "；").replace("\n", "；");
                out.append("\n").append(ring.name());
                if (!info.isEmpty()) out.append("：").append(info);
            }

            int wandCount = hero.belongings.equippedWands().size();
            out.append("\n\n_法杖合流_\n已装备 ").append(wandCount)
                    .append(" 根；释放任意已装备法杖时，其余已装备法杖的正常命中伤害与状态效果同时作用于同一目标。");

            java.util.ArrayList<Artifact> artifacts = hero.belongings.equippedArtifacts();
            out.append("\n\n_神器共鸣_\n已装备 ").append(artifacts.size()).append(" 件");
            Artifact selected = hero.belongings.displayArtifact();
            if (selected != null) {
                out.append("；快捷神器：").append(selected.name());
            }

            return out.toString();
        }
    }

    public static class GenesisTalentAuthority extends Buff {
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
            return "VII";
        }

        @Override
        public String name() {
            Hero hero = target instanceof Hero ? (Hero)target : Dungeon.hero;
            Talent t = selectedTier7(hero);
            return t == null ? "亘古元敕" : t.title();
        }

        @Override
        public String desc() {
            Hero hero = target instanceof Hero ? (Hero)target : Dungeon.hero;
            Talent t = selectedTier7(hero);
            if (t == null) return "当前没有选择第七层天赋。";
            return t.desc();
        }
    }

    public static boolean blocksInstantDeath(Hero hero, Object cause, boolean fromDamagePipeline) {
        if (!active(hero)) return false;

        // A direct die() call is an instant-death mechanic and is always rejected.
        // Ordinary lethal combat/environment damage is allowed while 奇迹·世界 is
        // not equipped, so damage-pipeline deaths need a narrower cause check.
        if (!fromDamagePipeline) return true;
        if (cause == null) return false;

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

    private static boolean retireLegacyKillGrowth(InfiniteWorldState st) {
        if (st == null) return false;
        boolean changed = st.genesisKillHpBonus != 0 || st.genesisKillStrBonus != 0;
        st.genesisKillHpBonus = 0;
        st.genesisKillStrBonus = 0;
        return changed;
    }

    // 0.6.19: 纵横八荒 no longer grants kill-based HP/STR growth. Keep this
    // hook as a compatibility no-op so older call sites and saves remain safe.
    public static void onEnemySlain(Hero hero) {
    }

    public static int permanentHpBonus(Hero hero) {
        return 0;
    }

    public static int permanentStrBonus(Hero hero) {
        return 0;
    }

    public static boolean unrestrictedEquipment(Hero hero) {
        return active(hero);
    }

    public static boolean freeShop(Hero hero) {
        return active(hero);
    }

    public static void onItemAcquired(Hero hero, com.shatteredpixel.shatteredpixeldungeon.items.Item item) {
        if (hero == null || item == null || !active(hero)) return;
        if (!item.isIdentified()) {
            item.identify();
            GLog.p("通识古今：已知悉「" + item.name() + "」的全部信息。");
        }
    }

    public static void identifyOwnedItems(Hero hero) {
        if (hero == null || !active(hero)) return;
        for (com.shatteredpixel.shatteredpixeldungeon.items.Item item : hero.belongings) {
            if (item != null && !item.isIdentified()) item.identify();
        }
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

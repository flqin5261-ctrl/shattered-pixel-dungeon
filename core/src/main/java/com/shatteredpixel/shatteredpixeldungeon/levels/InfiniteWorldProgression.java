/*
 * Shattered Pixel Dungeon - Assist Edition
 * Infinite World long-run progression and dynamic enemy scaling.
 */
package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.BreakthroughCertificate;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.BreakthroughToken;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

public final class InfiniteWorldProgression {

    public static final int PRE_BREAKTHROUGH_LEVEL_CAP = 30;
    public static final int POST_BREAKTHROUGH_LEVEL_CAP = 60;
    public static final int PRE_BREAKTHROUGH_EQUIPMENT_CAP = 50;
    public static final int POST_BREAKTHROUGH_EQUIPMENT_CAP = 120;
    public static final int PRE_BREAKTHROUGH_ARTIFACT_CAP = 10;
    public static final int POST_BREAKTHROUGH_ARTIFACT_CAP = 30;
    public static final int BREAKTHROUGH_BRANCH = 99;

    private InfiniteWorldProgression() {}

    public static InfiniteWorldState state() {
        if (Dungeon.infiniteWorldState == null) {
            Dungeon.infiniteWorldState = new InfiniteWorldState();
        }
        return Dungeon.infiniteWorldState;
    }

    public static boolean breakthroughCompleted() {
        return Dungeon.infiniteWorld && state().breakthroughCompleted;
    }

    public static int heroLevelCap(Hero hero) {
        if (!Dungeon.infiniteWorld) return Hero.MAX_LEVEL;
        return breakthroughCompleted()
                ? POST_BREAKTHROUGH_LEVEL_CAP
                : PRE_BREAKTHROUGH_LEVEL_CAP;
    }

    public static int equipmentUpgradeCap() {
        if (!Dungeon.infiniteWorld) return Integer.MAX_VALUE;
        return breakthroughCompleted()
                ? POST_BREAKTHROUGH_EQUIPMENT_CAP
                : PRE_BREAKTHROUGH_EQUIPMENT_CAP;
    }

    public static int artifactUpgradeCap() {
        if (!Dungeon.infiniteWorld) return 10;
        return breakthroughCompleted()
                ? POST_BREAKTHROUGH_ARTIFACT_CAP
                : PRE_BREAKTHROUGH_ARTIFACT_CAP;
    }

    public static boolean canUpgradeEquipment(Item item) {
        if (!Dungeon.infiniteWorld || item == null) return true;
        if (item instanceof Artifact) return true;
        if (item instanceof Weapon || item instanceof Armor
                || item instanceof Ring || item instanceof Wand) {
            return item.trueLevel() < equipmentUpgradeCap();
        }
        return true;
    }

    public static void onHeroAtBreakthroughGate(Hero hero) {
        if (!Dungeon.infiniteWorld || hero == null || hero.lvl < PRE_BREAKTHROUGH_LEVEL_CAP
                || breakthroughCompleted() || state().breakthroughTrialActive) {
            return;
        }
        issueBreakthroughToken(hero);
    }

    public static void issueBreakthroughToken(Hero hero) {
        if (hero == null || breakthroughCompleted()) return;
        if (hero.belongings.getItem(BreakthroughToken.class) != null) return;

        BreakthroughToken token = new BreakthroughToken();
        if (!token.collect(hero.belongings.backpack)) {
            // This progression-critical item must be in the backpack even if the
            // ordinary capacity is full. Allow one temporary over-cap slot rather
            // than dropping it into the streaming world and risking duplication.
            hero.belongings.backpack.items.add(token);
        }
        GLog.p("你已达到30级。挑战信物已放入背包；只有完成十波突破试炼，等级上限才会提高到60级。");
    }

    public static void testLevelUp(Hero hero) {
        if (hero == null) return;
        if (!Dungeon.infiniteWorld) {
            hero.earnExp(hero.maxExp(), InfiniteWorldProgression.class);
            return;
        }

        if (hero.lvl >= PRE_BREAKTHROUGH_LEVEL_CAP && !breakthroughCompleted()) {
            issueBreakthroughToken(hero);
            GLog.i("当前处于30级突破门槛；请使用挑战信物测试突破试炼。");
            return;
        }

        if (hero.lvl >= POST_BREAKTHROUGH_LEVEL_CAP) {
            GLog.i("角色已经达到无界模式60级上限。");
            return;
        }

        hero.earnExp(hero.maxExp(), InfiniteWorldProgression.class);
    }

    private static class PowerProfile {
        int weapon;
        int armor;
        int wand;
        int ring1;
        int ring2;
        int artifactVisible;
        int artifactCount;
    }

    private static PowerProfile powerProfile(Hero hero) {
        PowerProfile p = new PowerProfile();
        if (hero == null) return p;

        for (Item item : hero.belongings) {
            if (item == null) continue;

            if (item instanceof Artifact) {
                p.artifactCount++;
                p.artifactVisible = Math.max(p.artifactVisible,
                        Math.min(POST_BREAKTHROUGH_ARTIFACT_CAP,
                                Math.max(0, ((Artifact)item).buffedVisiblyUpgraded())));
                continue;
            }

            int lvl = Math.max(0,
                    Math.min(POST_BREAKTHROUGH_EQUIPMENT_CAP, item.trueLevel()));
            if (item instanceof Weapon) {
                p.weapon = Math.max(p.weapon, lvl);
            } else if (item instanceof Armor) {
                p.armor = Math.max(p.armor, lvl);
            } else if (item instanceof Wand) {
                p.wand = Math.max(p.wand, lvl);
            } else if (item instanceof Ring) {
                if (lvl >= p.ring1) {
                    p.ring2 = p.ring1;
                    p.ring1 = lvl;
                } else if (lvl > p.ring2) {
                    p.ring2 = lvl;
                }
            }
        }
        return p;
    }

    public static void testLevelToStageCap(Hero hero) {
        if (hero == null) return;
        int target = Dungeon.infiniteWorld
                ? heroLevelCap(hero)
                : Hero.MAX_LEVEL;
        int guard = 0;
        while (hero.lvl < target && guard++ < 80) {
            int before = hero.lvl;
            hero.earnExp(hero.maxExp(), InfiniteWorldProgression.class);
            if (hero.lvl <= before) break;
        }
        if (Dungeon.infiniteWorld && hero.lvl >= PRE_BREAKTHROUGH_LEVEL_CAP
                && !breakthroughCompleted()) {
            issueBreakthroughToken(hero);
            GLog.i("测试：已到30级突破门槛。");
        } else {
            GLog.i("测试：角色已升至当前阶段上限 " + hero.lvl + " 级。");
        }
    }

    public static int gearScore(Hero hero) {
        PowerProfile p = powerProfile(hero);
        float score = 0.30f * p.weapon
                + 0.30f * p.armor
                + 0.18f * p.wand
                + 0.12f * (p.ring1 + p.ring2)
                + 0.30f * p.artifactVisible
                + 1.5f * p.artifactCount;
        return Math.max(0, Math.round(score));
    }

    private static float offensiveUpgradeScore(Hero hero) {
        PowerProfile p = powerProfile(hero);
        return Math.max(p.weapon, 0.85f * p.wand)
                + 0.20f * (p.ring1 + p.ring2)
                + 0.45f * p.artifactVisible
                + p.artifactCount;
    }

    private static float defensiveUpgradeScore(Hero hero) {
        PowerProfile p = powerProfile(hero);
        return p.armor
                + 0.20f * (p.ring1 + p.ring2)
                + 0.35f * p.artifactVisible
                + 0.75f * p.artifactCount;
    }

    public static int dynamicMonsterLevel(Hero hero) {
        if (hero == null) return 1;
        int gear = Math.min(80, gearScore(hero));
        int artifacts = 0;
        for (Item item : hero.belongings) if (item instanceof Artifact) artifacts++;
        int level = Math.round(hero.lvl * 0.78f + gear * 0.22f + Math.min(3, artifacts));
        return clamp(level, 1, POST_BREAKTHROUGH_LEVEL_CAP);
    }

    public static void applyDynamicScaling(Mob mob, Hero hero) {
        applyDynamicScaling(mob, hero, 1f, 1f, 1f, 1f, 1f, 0);
    }

    public static void applyTrialScaling(Mob mob, Hero hero, int wave) {
        int w = clamp(wave, 1, 5);
        // Compress the old ten-wave difficulty span into five waves. The first
        // wave stays slightly forgiving, while wave 5 still reaches roughly the
        // old finale's stat multipliers.
        float health = 0.82f + 0.17f * (w - 1);
        float damage = 0.84f + 0.13f * (w - 1);
        float accuracy = 0.92f + 0.0625f * (w - 1);
        float defense = 0.92f + 0.0625f * (w - 1);
        int levelBonus = Math.round((w - 1) * 2.0f);
        if (w == 5) {
            // User feedback: the old compressed finale was a little too sharp.
            // Keep it above wave 4, but remove the large last-step spike.
            health = 1.40f;
            damage = 1.28f;
            accuracy = 1.13f;
            defense = 1.13f;
            levelBonus = 7;
        }
        applyDynamicScaling(mob, hero, health, damage, accuracy, defense, 1f, levelBonus);
        // Trial rewards are temporary because the Hero snapshot is restored afterwards.
        mob.EXP = 0;
        mob.maxLvl = 0;
    }

    private static void applyDynamicScaling(Mob mob, Hero hero,
                                            float healthWave, float damageWave,
                                            float accuracyWave, float defenseWave,
                                            float lootWave, int levelBonus) {
        if (mob == null || hero == null) return;

        int gear = gearScore(hero);
        float offense = Math.min(160f, offensiveUpgradeScore(hero));
        float defense = Math.min(160f, defensiveUpgradeScore(hero));
        int dynamicLevel = clamp(dynamicMonsterLevel(hero) + levelBonus, 1,
                POST_BREAKTHROUGH_LEVEL_CAP);

        // Equipment levels in this mode can reach +50/+120, far beyond upstream
        // balance. HP therefore answers offensive upgrades aggressively, while
        // damage answers defensive upgrades separately. A glass-cannon weapon does
        // not make enemies hit 20x harder, and heavy armor does not turn every mob
        // into a giant HP sponge.
        float healthScale = clamp(0.95f + 0.025f * (hero.lvl - 1) + 0.16f * offense,
                0.95f, 30.0f) * healthWave;
        float damageScale = clamp(0.95f + 0.018f * (hero.lvl - 1) + 0.12f * defense,
                0.95f, 24.0f) * damageWave;
        float accuracyScale = clamp(1.00f + 0.006f * (hero.lvl - 1) + 0.0030f * gear,
                1.00f, 1.85f) * accuracyWave;
        float defenseScale = clamp(1.00f + 0.005f * (hero.lvl - 1) + 0.0027f * gear,
                1.00f, 1.75f) * defenseWave;
        float lootScale = clamp(1.00f + 0.007f * hero.lvl + 0.004f * gear,
                1.00f, 2.25f) * lootWave;

        mob.applyAssistDynamicScaling(dynamicLevel, healthScale, damageScale,
                accuracyScale, defenseScale, lootScale);
    }

    public static boolean beginBreakthroughTrial(Hero hero, BreakthroughToken token) {
        if (!Dungeon.infiniteWorld || hero == null || token == null) return false;
        InfiniteWorldState st = state();

        if (st.breakthroughCompleted) {
            GLog.i("突破试炼已经完成。");
            return false;
        }
        if (hero.lvl < PRE_BREAKTHROUGH_LEVEL_CAP) {
            GLog.w("只有达到30级后才能使用挑战信物。");
            return false;
        }
        if (st.breakthroughTrialActive) return false;

        token.detach(hero.belongings.backpack);

        Bundle snapshot = new Bundle();
        hero.storeInBundle(snapshot);
        st.breakthroughHeroSnapshot = snapshot;

        Bundle quickslotSnapshot = new Bundle();
        Dungeon.quickslot.storeLayoutAsPlaceholders(quickslotSnapshot);
        st.breakthroughQuickslotSnapshot = quickslotSnapshot;
        st.breakthroughTrialGold = Dungeon.gold;
        st.breakthroughTrialEnergy = Dungeon.energy;
        st.breakthroughReturnDepth = Dungeon.depth;
        st.breakthroughReturnBranch = Dungeon.branch;
        st.breakthroughReturnPos = hero.pos;
        st.breakthroughWave = 0;
        st.breakthroughCountdown = -1f;
        st.breakthroughTrialActive = true;

        GLog.p("挑战信物化为一道门。五波敌人将在试炼场中依次出现；每波固定5只。");
        Level.beforeTransition();
        InterlevelScene.mode = InterlevelScene.Mode.RETURN;
        InterlevelScene.returnDepth = Dungeon.depth;
        InterlevelScene.returnBranch = BREAKTHROUGH_BRANCH;
        InterlevelScene.returnPos = -1;
        Game.switchScene(InterlevelScene.class);
        return true;
    }

    public static void finishBreakthroughTrial(boolean success) {
        if (!Dungeon.infiniteWorld || Dungeon.infiniteWorldState == null) return;
        InfiniteWorldState st = Dungeon.infiniteWorldState;
        if (!st.breakthroughTrialActive) return;

        int trialPos = Dungeon.hero != null ? Dungeon.hero.pos : 0;
        Hero restored = new Hero();
        if (st.breakthroughHeroSnapshot != null && !st.breakthroughHeroSnapshot.isNull()) {
            restored.restoreFromBundle(st.breakthroughHeroSnapshot);
        } else if (Dungeon.hero != null) {
            // Extremely defensive fallback. A normal run always has the snapshot.
            Bundle fallback = new Bundle();
            Dungeon.hero.storeInBundle(fallback);
            restored.restoreFromBundle(fallback);
        }

        if (Dungeon.level != null && Dungeon.level.length() > 0) {
            restored.pos = Math.max(0, Math.min(Dungeon.level.length() - 1, trialPos));
        }
        Dungeon.hero = restored;
        Dungeon.gold = st.breakthroughTrialGold;
        Dungeon.energy = st.breakthroughTrialEnergy;

        // Restore the pre-trial quickslot layout against the newly restored Hero
        // inventory. This removes stale references to trial-only items while
        // preserving slots that existed before entering the challenge.
        Dungeon.quickslot.reset();
        if (st.breakthroughQuickslotSnapshot != null) {
            Dungeon.quickslot.restorePlaceholders(st.breakthroughQuickslotSnapshot);
            Dungeon.quickslot.rebindFromBelongings(restored.belongings);
        }
        QuickSlotButton.refresh();

        st.breakthroughTrialActive = false;
        st.breakthroughWave = 0;
        st.breakthroughCountdown = -1f;
        st.breakthroughHeroSnapshot = null;
        st.breakthroughQuickslotSnapshot = null;

        if (success) {
            st.breakthroughCompleted = true;
            grantBreakthroughCertificate(restored);
            GLog.p("突破试炼完成！你获得了突破之证-lv30。等级上限提升至60级，装备强化上限提升至+120，神器上限提升至+30。");
        } else {
            st.breakthroughCompleted = false;
            issueBreakthroughToken(restored);
            GLog.w("突破试炼失败。挑战期间的损耗已恢复，新的挑战信物已经发放。");
        }

        Level.beforeTransition();
        InterlevelScene.mode = InterlevelScene.Mode.RETURN;
        InterlevelScene.returnDepth = st.breakthroughReturnDepth;
        InterlevelScene.returnBranch = st.breakthroughReturnBranch;
        InterlevelScene.returnPos = st.breakthroughReturnPos;
        Game.switchScene(InterlevelScene.class);
    }

    public static void grantBreakthroughCertificate(Hero hero) {
        if (hero == null || !breakthroughCompleted()) return;
        BreakthroughCertificate cert = hero.belongings.getItem(BreakthroughCertificate.class);
        if (cert == null) cert = new BreakthroughCertificate();
        cert.forceEquip(hero);
    }

    public static void syncBreakthroughCertificate(Hero hero) {
        if (hero == null || !breakthroughCompleted()) return;
        BreakthroughCertificate cert = hero.belongings.getItem(BreakthroughCertificate.class);
        if (cert != null) cert.syncToHeroLevel(hero);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

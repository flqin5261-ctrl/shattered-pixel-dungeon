/*
 * Shattered Pixel Dungeon - Assist Edition
 * Mutually-exclusive random events for Infinite World.
 */
package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.InfiniteWorldEventBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.InfiniteWorldEventBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.InfiniteWorldUndercoverMob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public final class InfiniteWorldRandomEvent {

    public static final int NONE = 0;
    public static final int MONSTER_CARNIVAL = 1;
    public static final int DARK_DAY = 2;
    public static final int PROSPERITY = 3;
    public static final int UNDERCOVER = 4;
    public static final int BLOOMING_PATH = 5;

    public static final float MIN_DURATION = 500f;
    public static final float MAX_DURATION = 5000f;

    private static final int UNDERCOVER_TARGET = 24;
    private static final int DARK_BOSS_TARGET = 2;

    private static int lastBloomWorldX = Integer.MIN_VALUE;
    private static int lastBloomWorldY = Integer.MIN_VALUE;
    private static boolean cleaningUp = false;

    private InfiniteWorldRandomEvent() {}

    private static InfiniteWorldState state() {
        return Dungeon.infiniteWorldState;
    }

    public static int activeType() {
        return Dungeon.infiniteWorld && state() != null ? state().randomEventType : NONE;
    }

    public static boolean isActive(int type) {
        return activeType() == type;
    }

    public static float remaining() {
        return state() == null ? 0f : Math.max(0f, state().randomEventRemaining);
    }

    public static float duration() {
        return state() == null ? 0f : Math.max(0f, state().randomEventDuration);
    }

    public static String eventName(int type) {
        switch (type) {
            case MONSTER_CARNIVAL: return "怪物狂欢日";
            case DARK_DAY: return "暗无天日";
            case PROSPERITY: return "财源滚滚";
            case UNDERCOVER: return "谁是卧底";
            case BLOOMING_PATH: return "一路繁花";
            default: return "随机事件";
        }
    }

    public static String eventDescription(int type) {
        switch (type) {
            case MONSTER_CARNIVAL:
                return "怪物刷新速度与数量大幅提高，普通掉落率提高，怪物有机会额外掉落神器，击杀经验翻倍。事件结束时场上怪物会被无奖励清场。";
            case DARK_DAY:
                return "角色周围始终维持且只维持两只事件首领。正常击败首领可获得巨额经验和大量物资；事件结束时残余首领无经验、无掉落清除。";
            case PROSPERITY:
                return "角色每消耗 1 点行动值，就直接获得一次随机金币；奖励没有总量上限，事件结束后已经获得的金币永久保留。";
            case UNDERCOVER:
                return "周围出现海量外观混杂的远程怪物，全部跳过攻击动画。约 60% 是不会主动攻击的和平方；杀错扣 50 生命，击败真正敌人获得巨额经验和海量金币。";
            case BLOOMING_PATH:
                return "角色每移动一步，周围都会生成随机种类、随机数量的物资。抽取范围覆盖物品图鉴，包括通常无法正常取得的物资；事件结束后物资不会回收。";
            default:
                return "";
        }
    }

    public static void onLevelReady(InfiniteWorldLevel level) {
        if (level == null || !Dungeon.infiniteWorld) return;
        ensureSchedule();
        level.restorePersistentRandomEventLoot();
        ensureBuff();
        if (isActive(UNDERCOVER)) {
            clearNonUndercoverEnemies(level);
        }
        // Do not GameScene.add() event actors while a level is still being built
        // under InterlevelScene. The first Hero move/action will safely refill the
        // event population after GameScene has taken ownership of the level.
    }

    public static void onWindowShifted(InfiniteWorldLevel level) {
        if (level == null || !Dungeon.infiniteWorld) return;
        level.restorePersistentRandomEventLoot();
        maintainSpecialPopulation(level);
    }

    public static void onHeroAction(InfiniteWorldLevel level, Hero hero, float time) {
        if (level == null || hero == null || time <= 0f || !Dungeon.infiniteWorld) return;

        InfiniteWorldState st = state();
        if (st.randomEventType == NONE) {
            ensureSchedule();
            // Spectator terrain QA should not unexpectedly start gameplay events.
            if (!InfiniteWorldLevel.assistSpectatorActive()) {
                st.randomEventNextTrigger -= time;
                if (st.randomEventNextTrigger <= 0f) {
                    start(level, hero, 1 + Random.Int(5), randomDuration(), false);
                }
            }
            return;
        }

        float effective = Math.min(time, Math.max(0f, st.randomEventRemaining));

        if (st.randomEventType == PROSPERITY) {
            processProsperity(hero, effective);
        }

        st.randomEventRemaining -= time;
        BuffIndicator.refreshHero();

        if (st.randomEventRemaining <= 0f) {
            finish(level, false, false);
            return;
        }

        maintainSpecialPopulation(level);
    }

    public static void onHeroMoved(InfiniteWorldLevel level, Hero hero) {
        if (level == null || hero == null || !Dungeon.infiniteWorld) return;

        if (isActive(BLOOMING_PATH)) {
            int wx = level.eventWorldXForLocalCell(hero.pos);
            int wy = level.eventWorldYForLocalCell(hero.pos);
            if (wx != lastBloomWorldX || wy != lastBloomWorldY) {
                lastBloomWorldX = wx;
                lastBloomWorldY = wy;
                spawnBloomLoot(level, hero);
            }
        }

        maintainSpecialPopulation(level);
    }

    public static void forceStart(int type) {
        if (!(Dungeon.level instanceof InfiniteWorldLevel) || Dungeon.hero == null) {
            GLog.w("随机事件只能在无界地牢中启动。");
            return;
        }
        if (type < MONSTER_CARNIVAL || type > BLOOMING_PATH) return;

        InfiniteWorldLevel level = (InfiniteWorldLevel)Dungeon.level;
        if (activeType() != NONE) {
            finish(level, true, true);
        }
        start(level, Dungeon.hero, type, randomDuration(), true);
    }

    public static void forceStop() {
        if (!(Dungeon.level instanceof InfiniteWorldLevel) || activeType() == NONE) {
            GLog.i("当前没有正在进行的随机事件。");
            return;
        }
        finish((InfiniteWorldLevel)Dungeon.level, true, false);
    }

    private static void ensureSchedule() {
        InfiniteWorldState st = state();
        if (st == null || st.randomEventType != NONE) return;
        if (st.randomEventNextTrigger <= 0f) {
            st.randomEventNextTrigger = randomGap();
        }
    }

    private static float randomDuration() {
        return MIN_DURATION + Random.Float() * (MAX_DURATION - MIN_DURATION);
    }

    private static float randomGap() {
        // Long enough that the world is not permanently event-filled, but still
        // random enough for an event to begin while simply travelling.
        return 700f + Random.Float() * 1800f;
    }

    private static void start(InfiniteWorldLevel level, Hero hero, int type,
                              float duration, boolean forced) {
        InfiniteWorldState st = state();
        st.randomEventType = type;
        st.randomEventDuration = Math.max(MIN_DURATION, Math.min(MAX_DURATION, duration));
        st.randomEventRemaining = st.randomEventDuration;
        st.randomEventGoldRemainder = 0f;
        st.randomEventSerial++;
        st.randomEventNextTrigger = 0f;
        lastBloomWorldX = Integer.MIN_VALUE;
        lastBloomWorldY = Integer.MIN_VALUE;

        if (type == UNDERCOVER) {
            clearAllEnemies(level);
        }

        ensureBuff();
        maintainSpecialPopulation(level);

        GLog.newLine();
        GLog.p("【随机事件】" + eventName(type) + " 开始！持续 "
                + Math.round(st.randomEventDuration) + " 行动值。");
        GLog.i(eventDescription(type));
        if (forced) GLog.i("该事件由辅助菜单主动激活。");
    }

    private static void finish(InfiniteWorldLevel level, boolean forced, boolean switching) {
        if (level == null) return;
        InfiniteWorldState st = state();
        int ended = st.randomEventType;
        if (ended == NONE) return;

        cleaningUp = true;
        try {
            if (ended == MONSTER_CARNIVAL) {
                clearAllEnemies(level);
            } else if (ended == DARK_DAY) {
                clearEventBosses(level);
            } else if (ended == UNDERCOVER) {
                clearAllEnemies(level);
            }
        } finally {
            cleaningUp = false;
        }

        if (Dungeon.hero != null) {
            Buff.detach(Dungeon.hero, InfiniteWorldEventBuff.class);
        }

        st.randomEventType = NONE;
        st.randomEventRemaining = 0f;
        st.randomEventDuration = 0f;
        st.randomEventGoldRemainder = 0f;
        st.randomEventNextTrigger = randomGap();
        lastBloomWorldX = Integer.MIN_VALUE;
        lastBloomWorldY = Integer.MIN_VALUE;

        if (!switching) {
            GLog.newLine();
            GLog.w("【随机事件结束】" + eventName(ended) + "。");
            if (ended == MONSTER_CARNIVAL) {
                GLog.i("狂欢散场，剩余怪物已全部无经验、无掉落清除。");
            } else if (ended == DARK_DAY) {
                GLog.i("黑暗退去，残余事件首领已无经验、无掉落清除。");
            } else if (ended == UNDERCOVER) {
                GLog.i("卧底事件结束，场上的事件怪物已全部消失。");
            } else if (ended == PROSPERITY) {
                GLog.i("财运结束，但已经获得的金币全部保留。");
            } else if (ended == BLOOMING_PATH) {
                GLog.i("繁花散去，事件期间生成的物资全部留在世界中。");
            }
            if (forced) GLog.i("该事件由辅助菜单主动关闭。");
        }
    }

    private static void ensureBuff() {
        if (Dungeon.hero == null) return;
        if (activeType() == NONE) {
            Buff.detach(Dungeon.hero, InfiniteWorldEventBuff.class);
            return;
        }
        Buff.affect(Dungeon.hero, InfiniteWorldEventBuff.class);
        BuffIndicator.refreshHero();
    }

    private static void processProsperity(Hero hero, float time) {
        InfiniteWorldState st = state();
        st.randomEventGoldRemainder += time;
        int units = (int)Math.floor(st.randomEventGoldRemainder);
        if (units <= 0) return;
        st.randomEventGoldRemainder -= units;

        long gained = 0L;
        for (int i = 0; i < units; i++) {
            int amount;
            if (Random.Int(100) < 2) {
                amount = Random.IntRange(5000, 50000);
            } else {
                amount = Random.IntRange(1, 1000);
            }
            gained += amount;
        }

        int safeGain = gained > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)gained;
        if (Dungeon.gold > Integer.MAX_VALUE - safeGain) {
            Dungeon.gold = Integer.MAX_VALUE;
        } else {
            Dungeon.gold += safeGain;
        }
        Statistics.goldCollected += safeGain;
        Badges.validateGoldCollected();

        if (hero.sprite != null) {
            hero.sprite.showStatusWithIcon(CharSprite.NEUTRAL,
                    "+" + safeGain, FloatingText.GOLD);
        }
    }

    private static void spawnBloomLoot(InfiniteWorldLevel level, Hero hero) {
        int drops = Random.IntRange(1, 5);
        for (int i = 0; i < drops; i++) {
            Item item = randomCatalogItem();
            if (item == null) continue;

            if (item.stackable) {
                item.quantity(Random.IntRange(1, 8));
            }

            int cell = level.findRandomEventDropCell(hero.pos, 1, 5);
            if (cell < 0) cell = hero.pos;
            level.dropPersistentRandomEventLoot(item, cell);
        }
    }

    public static Item randomCatalogItem() {
        Catalog[] catalogs = Catalog.values();
        for (int attempt = 0; attempt < 60; attempt++) {
            Catalog catalog = catalogs[Random.Int(catalogs.length)];
            ArrayList<Class<?>> classes = new ArrayList<>(catalog.items());
            if (classes.isEmpty()) continue;

            Class<?> cls = classes.get(Random.Int(classes.size()));
            if (!Item.class.isAssignableFrom(cls)) continue;

            @SuppressWarnings({"rawtypes", "unchecked"})
            Item item = (Item)Reflection.newInstance((Class)cls);
            if (item == null) continue;

            try {
                Item randomized = item.random();
                return randomized == null ? item : randomized;
            } catch (Exception ignored) {
                return item;
            }
        }

        return Generator.randomUsingDefaults(Generator.Category.POTION);
    }

    public static int adjustExperience(Mob mob, int exp) {
        if (exp <= 0 || mob == null) return exp;
        if (isActive(MONSTER_CARNIVAL)
                && !(mob instanceof InfiniteWorldEventBoss)
                && !(mob instanceof InfiniteWorldUndercoverMob)) {
            long doubled = (long)exp * 2L;
            return doubled > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)doubled;
        }
        return exp;
    }

    public static float lootChanceMultiplier(Mob mob) {
        if (mob != null && isActive(MONSTER_CARNIVAL)
                && !(mob instanceof InfiniteWorldEventBoss)
                && !(mob instanceof InfiniteWorldUndercoverMob)) {
            return 3f;
        }
        return 1f;
    }

    public static void onMobRolledLoot(Mob mob) {
        if (mob == null || cleaningUp || !isActive(MONSTER_CARNIVAL)) return;
        if (mob instanceof InfiniteWorldEventBoss || mob instanceof InfiniteWorldUndercoverMob) return;
        if (!(Dungeon.level instanceof InfiniteWorldLevel)) return;

        if (Random.Int(100) < 9) {
            Item artifact = Generator.randomUsingDefaults(Generator.Category.ARTIFACT);
            if (artifact != null) {
                InfiniteWorldLevel level = (InfiniteWorldLevel)Dungeon.level;
                int cell = level.findRandomEventDropCell(mob.pos, 0, 2);
                if (cell < 0) cell = mob.pos;
                level.dropPersistentRandomEventLoot(artifact, cell);
                GLog.p("狂欢掉落：一只怪物额外掉落了神器 " + artifact.name() + "！");
            }
        }
    }

    public static void dropDarkDayBossRewards(InfiniteWorldEventBoss boss) {
        if (boss == null || cleaningUp || !isActive(DARK_DAY)
                || !(Dungeon.level instanceof InfiniteWorldLevel)) return;

        InfiniteWorldLevel level = (InfiniteWorldLevel)Dungeon.level;
        int count = Random.IntRange(14, 24);
        for (int i = 0; i < count; i++) {
            Item item = randomCatalogItem();
            if (item == null) continue;
            if (item.stackable) item.quantity(Random.IntRange(1, 6));

            int cell = level.findRandomEventDropCell(boss.pos, 0, 5);
            if (cell < 0) cell = boss.pos;
            level.dropPersistentRandomEventLoot(item, cell);
        }

        Gold gold = new Gold(Random.IntRange(3000, 20000));
        int goldCell = level.findRandomEventDropCell(boss.pos, 0, 4);
        if (goldCell < 0) goldCell = boss.pos;
        level.dropPersistentRandomEventLoot(gold, goldCell);
    }

    public static void onEventBossDefeated() {
        if (cleaningUp || !isActive(DARK_DAY)) return;
        if (Dungeon.level instanceof InfiniteWorldLevel) {
            maintainSpecialPopulation((InfiniteWorldLevel)Dungeon.level);
        }
    }

    public static void onUndercoverDefeated(boolean peaceful) {
        if (cleaningUp || !isActive(UNDERCOVER) || Dungeon.hero == null) return;

        if (peaceful) {
            Dungeon.hero.damage(50, InfiniteWorldRandomEvent.class);
            if (Dungeon.hero.sprite != null) {
                Dungeon.hero.sprite.showStatus(CharSprite.NEGATIVE, "-50");
            }
            GLog.n("你误杀了和平方怪物：扣除 50 点生命！");
        } else {
            int gold = Random.IntRange(2500, 25000);
            if (Dungeon.gold > Integer.MAX_VALUE - gold) Dungeon.gold = Integer.MAX_VALUE;
            else Dungeon.gold += gold;
            Statistics.goldCollected += gold;
            Badges.validateGoldCollected();
            if (Dungeon.hero.sprite != null) {
                Dungeon.hero.sprite.showStatusWithIcon(CharSprite.NEUTRAL,
                        "+" + gold, FloatingText.GOLD);
            }
            GLog.p("你找到了真正的敌人！获得巨额经验，并获得 " + gold + " 金币。");
        }

        if (Dungeon.level instanceof InfiniteWorldLevel) {
            maintainSpecialPopulation((InfiniteWorldLevel)Dungeon.level);
        }
    }

    private static void maintainSpecialPopulation(InfiniteWorldLevel level) {
        if (level == null || Dungeon.hero == null || !Dungeon.hero.isAlive() || cleaningUp) return;

        if (isActive(DARK_DAY)) {
            ArrayList<InfiniteWorldEventBoss> bosses = new ArrayList<>();
            int hx = Dungeon.hero.pos % level.width();
            int hy = Dungeon.hero.pos / level.width();
            for (Mob mob : level.mobs.toArray(new Mob[0])) {
                if (mob instanceof InfiniteWorldEventBoss) {
                    int mx = mob.pos % level.width();
                    int my = mob.pos / level.width();
                    if (Math.max(Math.abs(mx - hx), Math.abs(my - hy)) > 28) {
                        mob.despawnFromInfiniteWorld();
                    } else {
                        bosses.add((InfiniteWorldEventBoss)mob);
                    }
                }
            }
            while (bosses.size() > DARK_BOSS_TARGET) {
                InfiniteWorldEventBoss extra = bosses.remove(bosses.size() - 1);
                extra.despawnFromInfiniteWorld();
            }
            int attempts = 0;
            while (bosses.size() < DARK_BOSS_TARGET && attempts++ < 12) {
                InfiniteWorldEventBoss boss = new InfiniteWorldEventBoss(Random.Int(4));
                int cell = level.findRandomEventSpawnCell(boss, 7, 18);
                if (cell < 0) break;
                boss.pos = cell;
                boss.state = boss.HUNTING;
                GameScene.add(boss, 0f);
                bosses.add(boss);
            }
        } else if (isActive(UNDERCOVER)) {
            // The event promise is strict: every monster on screen belongs to
            // this event, so no normal/summoned hostile is allowed to mix in.
            clearNonUndercoverEnemies(level);

            int count = 0;
            for (Mob mob : level.mobs.toArray(new Mob[0])) {
                if (mob instanceof InfiniteWorldUndercoverMob) count++;
            }

            int attempts = 0;
            while (count < UNDERCOVER_TARGET && attempts++ < UNDERCOVER_TARGET * 4) {
                boolean peaceful = Random.Int(100) < 60;
                InfiniteWorldUndercoverMob mob =
                        new InfiniteWorldUndercoverMob(Random.Int(4), peaceful);
                int cell = level.findRandomEventSpawnCell(mob, 5, 22);
                if (cell < 0) break;
                mob.pos = cell;
                mob.state = peaceful ? mob.PASSIVE : mob.HUNTING;
                GameScene.add(mob, Random.Float() * 0.2f);
                count++;
            }
        }
    }

    private static void clearAllEnemies(InfiniteWorldLevel level) {
        for (Mob mob : level.mobs.toArray(new Mob[0])) {
            if (mob.alignment == Char.Alignment.ENEMY) {
                mob.despawnFromInfiniteWorld();
            }
        }
    }

    private static void clearNonUndercoverEnemies(InfiniteWorldLevel level) {
        for (Mob mob : level.mobs.toArray(new Mob[0])) {
            if (mob.alignment == Char.Alignment.ENEMY
                    && !(mob instanceof InfiniteWorldUndercoverMob)) {
                mob.despawnFromInfiniteWorld();
            }
        }
    }

    private static void clearEventBosses(InfiniteWorldLevel level) {
        for (Mob mob : level.mobs.toArray(new Mob[0])) {
            if (mob instanceof InfiniteWorldEventBoss) {
                mob.despawnFromInfiniteWorld();
            }
        }
    }

    private static void clearUndercoverMobs(InfiniteWorldLevel level) {
        for (Mob mob : level.mobs.toArray(new Mob[0])) {
            if (mob instanceof InfiniteWorldUndercoverMob) {
                mob.despawnFromInfiniteWorld();
            }
        }
    }
}

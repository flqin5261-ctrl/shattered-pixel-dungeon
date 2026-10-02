/*
 * Shattered Pixel Dungeon - Assist Edition
 * Event-only boss used by the Infinite World "暗无天日" random event.
 */
package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldRandomEvent;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DM300Sprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GooSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.KingSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.TenguSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class InfiniteWorldEventBoss extends Mob {

    private static final String PERSONA = "assist_event_boss_persona";
    private int persona = 0;

    {
        properties.add(Property.BOSS);
        maxLvl = 60;
        viewDistance = 12;
        lootChance = 0f;
        applyPersona();
    }

    public InfiniteWorldEventBoss() {
        super();
    }

    public InfiniteWorldEventBoss(int persona) {
        super();
        this.persona = Math.floorMod(persona, 4);
        applyPersona();
        configureForHero(Dungeon.hero);
    }

    private void applyPersona() {
        switch (persona) {
            case 1:
                spriteClass = TenguSprite.class;
                break;
            case 2:
                spriteClass = DM300Sprite.class;
                break;
            case 3:
                spriteClass = KingSprite.class;
                break;
            case 0:
            default:
                spriteClass = GooSprite.class;
                break;
        }
    }

    public void configureForHero(Hero hero) {
        int lvl = hero == null ? 1 : Math.max(1, hero.lvl);
        int heroHT = hero == null ? 100 : Math.max(20, hero.HT);
        HT = HP = Math.max(320, heroHT * 3 + lvl * 35);
        defenseSkill = 10 + lvl;
        EXP = 900 + lvl * 90;
        maxLvl = 60;
    }

    @Override
    public String name() {
        String base;
        switch (persona) {
            case 1:
                base = Messages.get(Tengu.class, "name");
                break;
            case 2:
                base = Messages.get(DM300.class, "name");
                break;
            case 3:
                base = Messages.get(DwarfKing.class, "name");
                break;
            case 0:
            default:
                base = Messages.get(Goo.class, "name");
                break;
        }
        return "暗日·" + base;
    }

    @Override
    public String description() {
        return "被“暗无天日”事件扭曲后出现的首领。事件期间角色周围始终会维持两只这类首领；正常击败可获得巨额经验与大量物资。";
    }

    @Override
    public int damageRoll() {
        int lvl = Dungeon.hero == null ? 1 : Math.max(1, Dungeon.hero.lvl);
        return Random.NormalIntRange(12 + lvl, 24 + lvl * 2);
    }

    @Override
    public int attackSkill(Char target) {
        int lvl = Dungeon.hero == null ? 1 : Math.max(1, Dungeon.hero.lvl);
        return 18 + lvl * 2;
    }

    @Override
    public int drRoll() {
        int lvl = Dungeon.hero == null ? 1 : Math.max(1, Dungeon.hero.lvl);
        return super.drRoll() + Random.NormalIntRange(lvl / 3, Math.max(2, lvl));
    }

    @Override
    protected boolean canAttack(Char enemy) {
        if (enemy == null || Dungeon.level == null) return false;
        if (Dungeon.level.distance(pos, enemy.pos) <= 6) {
            Ballistica shot = new Ballistica(pos, enemy.pos,
                    Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
            if (shot.collisionPos == enemy.pos) return true;
        }
        return super.canAttack(enemy);
    }

    @Override
    public void rollToDropLoot() {
        InfiniteWorldRandomEvent.dropDarkDayBossRewards(this);
    }

    @Override
    public void die(Object cause) {
        super.die(cause);
        InfiniteWorldRandomEvent.onEventBossDefeated();
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(PERSONA, persona);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        persona = Math.floorMod(bundle.getInt(PERSONA), 4);
        applyPersona();
    }
}

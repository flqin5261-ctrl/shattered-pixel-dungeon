/*
 * Shattered Pixel Dungeon - Assist Edition
 * Event-only mob used by the Infinite World "谁是卧底" random event.
 */
package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldRandomEvent;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CrabSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GnollSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.RatSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SkeletonSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class InfiniteWorldUndercoverMob extends Mob {

    private static final String PERSONA = "assist_undercover_persona";
    private static final String PEACEFUL = "assist_undercover_peaceful";

    private int persona = 0;
    private boolean peaceful = false;

    {
        maxLvl = 60;
        viewDistance = 12;
        lootChance = 0f;
        applyPersona();
    }

    public InfiniteWorldUndercoverMob() {
        super();
    }

    public InfiniteWorldUndercoverMob(int persona, boolean peaceful) {
        super();
        this.persona = Math.floorMod(persona, 4);
        this.peaceful = peaceful;
        applyPersona();
        configureForHero(Dungeon.hero);
        state = peaceful ? PASSIVE : HUNTING;
    }

    private void applyPersona() {
        switch (persona) {
            case 1:
                spriteClass = GnollSprite.class;
                break;
            case 2:
                spriteClass = CrabSprite.class;
                break;
            case 3:
                spriteClass = SkeletonSprite.class;
                break;
            case 0:
            default:
                spriteClass = RatSprite.class;
                break;
        }
    }

    public boolean peaceful() {
        return peaceful;
    }

    public void configureForHero(Hero hero) {
        int lvl = hero == null ? 1 : Math.max(1, hero.lvl);
        int heroHT = hero == null ? 100 : Math.max(20, hero.HT);
        HT = HP = Math.max(40, heroHT / 2 + lvl * 5);
        defenseSkill = 6 + lvl;
        EXP = peaceful ? 0 : 450 + lvl * 45;
        maxLvl = 60;
    }

    @Override
    public String name() {
        switch (persona) {
            case 1:
                return Messages.get(Gnoll.class, "name");
            case 2:
                return Messages.get(Crab.class, "name");
            case 3:
                return Messages.get(Skeleton.class, "name");
            case 0:
            default:
                return Messages.get(Rat.class, "name");
        }
    }

    @Override
    public String description() {
        return "“谁是卧底”事件中的可疑怪物。它们外观混杂，无法从名称判断阵营；其中多数是和平方，真正的敌人才会主动远程攻击。";
    }

    @Override
    public int damageRoll() {
        int lvl = Dungeon.hero == null ? 1 : Math.max(1, Dungeon.hero.lvl);
        return Random.NormalIntRange(5 + lvl / 2, 10 + lvl);
    }

    @Override
    public int attackSkill(Char target) {
        int lvl = Dungeon.hero == null ? 1 : Math.max(1, Dungeon.hero.lvl);
        return 16 + lvl * 2;
    }

    @Override
    protected boolean canAttack(Char enemy) {
        if (peaceful || enemy == null || Dungeon.level == null) return false;
        if (Dungeon.level.distance(pos, enemy.pos) > 10) return false;

        Ballistica shot = new Ballistica(pos, enemy.pos,
                Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
        return shot.collisionPos == enemy.pos;
    }

    @Override
    protected boolean doAttack(Char enemy) {
        if (peaceful) {
            spend(TICK);
            return true;
        }

        // Deliberately skip sprite.attack(): every hostile undercover mob attacks
        // as an instant ranged strike with no attack animation.
        attack(enemy);
        spend(attackDelay());
        return true;
    }

    @Override
    public void rollToDropLoot() {
        // Rewards are granted explicitly by the event controller.
    }

    @Override
    public void die(Object cause) {
        boolean wasPeaceful = peaceful;
        if (wasPeaceful) {
            // Keep the unit attackable while alive, but prevent a peaceful kill
            // from granting ordinary enemy XP, kill statistics or Genesis rewards.
            alignment = Alignment.NEUTRAL;
        }
        super.die(cause);
        InfiniteWorldRandomEvent.onUndercoverDefeated(wasPeaceful);
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(PERSONA, persona);
        bundle.put(PEACEFUL, peaceful);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        persona = Math.floorMod(bundle.getInt(PERSONA), 4);
        peaceful = bundle.getBoolean(PEACEFUL);
        applyPersona();
        if (peaceful && isAlive()) state = PASSIVE;
    }
}

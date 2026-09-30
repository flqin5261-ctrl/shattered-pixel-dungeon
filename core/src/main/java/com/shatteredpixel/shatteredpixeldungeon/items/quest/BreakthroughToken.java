/*
 * Shattered Pixel Dungeon - Assist Edition
 */
package com.shatteredpixel.shatteredpixeldungeon.items.quest;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldProgression;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

public class BreakthroughToken extends Item {

    public static final String AC_USE = "USE";

    {
        image = ItemSpriteSheet.TOKEN;
        unique = true;
        defaultAction = AC_USE;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = new ArrayList<>();
        actions.add(AC_USE);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (AC_USE.equals(action)) {
            InfiniteWorldProgression.beginBreakthroughTrial(hero, this);
        }
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (AC_USE.equals(action)) return "使用信物";
        return super.actionName(action, hero);
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    @Override
    public int value() {
        return 0;
    }
}

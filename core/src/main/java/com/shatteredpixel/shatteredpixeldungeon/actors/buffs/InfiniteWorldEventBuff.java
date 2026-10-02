/*
 * Shattered Pixel Dungeon - Assist Edition
 * Persistent HUD marker for Infinite World random events.
 */
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldRandomEvent;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

public class InfiniteWorldEventBuff extends Buff {

    {
        type = buffType.NEUTRAL;
        revivePersists = true;
    }

    @Override
    public boolean act() {
        // Duration is driven by Hero action-value, not Actor time.
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        switch (InfiniteWorldRandomEvent.activeType()) {
            case InfiniteWorldRandomEvent.MONSTER_CARNIVAL:
                return BuffIndicator.RAGE;
            case InfiniteWorldRandomEvent.DARK_DAY:
                return BuffIndicator.SHADOWS;
            case InfiniteWorldRandomEvent.PROSPERITY:
                return BuffIndicator.BLESS;
            case InfiniteWorldRandomEvent.UNDERCOVER:
                return BuffIndicator.DISGUISE;
            case InfiniteWorldRandomEvent.BLOOMING_PATH:
                return BuffIndicator.NATURE_POWER;
            default:
                return BuffIndicator.CHALLENGE;
        }
    }

    @Override
    public String name() {
        return InfiniteWorldRandomEvent.eventName(InfiniteWorldRandomEvent.activeType());
    }

    @Override
    public String desc() {
        int type = InfiniteWorldRandomEvent.activeType();
        return InfiniteWorldRandomEvent.eventDescription(type)
                + "\n\n剩余行动值："
                + Math.max(0, Math.round(InfiniteWorldRandomEvent.remaining()));
    }

    @Override
    public String iconTextDisplay() {
        float remaining = InfiniteWorldRandomEvent.remaining();
        if (remaining >= 1000f) {
            return String.format(java.util.Locale.US, "%.1fk", remaining / 1000f);
        }
        return Integer.toString(Math.max(0, Math.round(remaining)));
    }

    @Override
    public float iconFadePercent() {
        float duration = InfiniteWorldRandomEvent.duration();
        if (duration <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, 1f - InfiniteWorldRandomEvent.remaining() / duration));
    }
}

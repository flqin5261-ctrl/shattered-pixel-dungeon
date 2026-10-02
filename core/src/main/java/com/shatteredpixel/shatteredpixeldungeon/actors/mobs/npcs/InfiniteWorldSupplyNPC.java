/*
 * Shattered Pixel Dungeon - Assist Edition
 * Infinite World uses the original NPC appearances without their finite-floor quests.
 */
package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BlacksmithSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GhostSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ImpSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WandmakerSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

public class InfiniteWorldSupplyNPC extends NPC {

    public static final int GHOST = 0;
    public static final int WANDMAKER = 1;
    public static final int BLACKSMITH = 2;
    public static final int IMP = 3;

    private static final String PERSONA = "assist_persona";
    private static final String SITE_X = "assist_site_x";
    private static final String SITE_Y = "assist_site_y";

    private int persona = GHOST;
    private int siteChunkX;
    private int siteChunkY;

    {
        properties.add(Property.IMMOVABLE);
        applyPersona();
    }

    public InfiniteWorldSupplyNPC() {
        super();
    }

    public InfiniteWorldSupplyNPC(int persona, int siteChunkX, int siteChunkY) {
        super();
        this.persona = Math.max(GHOST, Math.min(IMP, persona));
        this.siteChunkX = siteChunkX;
        this.siteChunkY = siteChunkY;
        applyPersona();
    }

    private void applyPersona() {
        switch (persona) {
            case WANDMAKER:
                spriteClass = WandmakerSprite.class;
                break;
            case BLACKSMITH:
                spriteClass = BlacksmithSprite.class;
                break;
            case IMP:
                spriteClass = ImpSprite.class;
                break;
            case GHOST:
            default:
                spriteClass = GhostSprite.class;
                flying = true;
                break;
        }
    }

    public int persona() {
        return persona;
    }

    public int siteChunkX() {
        return siteChunkX;
    }

    public int siteChunkY() {
        return siteChunkY;
    }

    @Override
    public String name() {
        switch (persona) {
            case WANDMAKER:
                return Messages.get(Wandmaker.class, "name");
            case BLACKSMITH:
                return Messages.get(Blacksmith.class, "name");
            case IMP:
                return Messages.get(Imp.class, "name");
            case GHOST:
            default:
                return Messages.get(Ghost.class, "name");
        }
    }

    @Override
    public String description() {
        switch (persona) {
            case WANDMAKER:
                return Messages.get(Wandmaker.class, "desc");
            case BLACKSMITH:
                return Messages.get(Blacksmith.class, "desc");
            case IMP:
                return Messages.get(Imp.class, "desc");
            case GHOST:
            default:
                return Messages.get(Ghost.class, "desc");
        }
    }

    @Override
    public int defenseSkill(Char enemy) {
        return INFINITE_EVASION;
    }

    @Override
    public void damage(int dmg, Object src) {
        // Infinite-world supply contacts are scenery/service NPCs.
    }

    @Override
    public boolean add(Buff buff) {
        return false;
    }

    @Override
    public boolean reset() {
        return true;
    }

    @Override
    public boolean interact(Char c) {
        if (sprite != null) sprite.turnTo(pos, c.pos);
        if (c != Dungeon.hero || !(Dungeon.level instanceof InfiniteWorldLevel)) {
            return true;
        }

        final InfiniteWorldLevel level = (InfiniteWorldLevel)Dungeon.level;
        final String text;

        if (level.infiniteWorldNpcRewardClaimed(siteChunkX, siteChunkY)) {
            text = Messages.get(this, "already");
        } else {
            Item reward = level.claimInfiniteWorldNpcReward(this);
            if (reward == null) {
                text = Messages.get(this, "already");
            } else {
                String rewardName = reward.name();
                if (!reward.collect()) {
                    if (Dungeon.level != null) {
                        Dungeon.level.drop(reward, Dungeon.hero.pos).sprite.drop();
                    }
                }
                switch (persona) {
                    case WANDMAKER:
                        text = Messages.get(this, "wandmaker_gift", rewardName);
                        break;
                    case BLACKSMITH:
                        text = Messages.get(this, "blacksmith_gift", rewardName);
                        break;
                    case IMP:
                        text = Messages.get(this, "imp_gift", rewardName);
                        break;
                    case GHOST:
                    default:
                        text = Messages.get(this, "ghost_gift", rewardName);
                        break;
                }
            }
        }

        Game.runOnRenderThread(new Callback() {
            @Override
            public void call() {
                GameScene.show(new WndQuest(InfiniteWorldSupplyNPC.this, text));
            }
        });
        return true;
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(PERSONA, persona);
        bundle.put(SITE_X, siteChunkX);
        bundle.put(SITE_Y, siteChunkY);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        persona = bundle.getInt(PERSONA);
        siteChunkX = bundle.getInt(SITE_X);
        siteChunkY = bundle.getInt(SITE_Y);
        applyPersona();
    }
}

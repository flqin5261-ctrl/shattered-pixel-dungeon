/*
 * Shattered Pixel Dungeon Assist
 */
package com.shatteredpixel.shatteredpixeldungeon.items.rings;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTerror;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUpgrade;
import com.watabou.utils.Reflection;

public class MimicScrollRing extends MimicRing {

    @SuppressWarnings("unchecked")
    private static final Class<? extends Item>[] ABILITIES = new Class[]{
            ScrollOfUpgrade.class,
            ScrollOfIdentify.class,
            ScrollOfRemoveCurse.class,
            ScrollOfMirrorImage.class,
            ScrollOfRecharging.class,
            ScrollOfTeleportation.class,
            ScrollOfLullaby.class,
            ScrollOfMagicMapping.class,
            ScrollOfRage.class,
            ScrollOfRetribution.class,
            ScrollOfTerror.class,
            ScrollOfTransmutation.class
    };

    @Override
    protected Class<? extends Item>[] abilityPool() {
        return ABILITIES;
    }

    @Override
    protected int mimicImage() {
        return ItemSpriteSheet.RING_DIAMOND;
    }

    @Override
    protected boolean consumeChargeBeforeInvoke(Class<? extends Item> abilityClass) {
        // Upgrade is asynchronous: charge is consumed only when the player
        // actually confirms an upgrade in WndUpgrade. This also lets the same
        // selected item remain open for multiple ring charges.
        return abilityClass != ScrollOfUpgrade.class;
    }

    @Override
    protected void invokeStoredAbility(Hero hero, Class<? extends Item> abilityClass) {
        if (abilityClass == ScrollOfUpgrade.class) {
            showUpgradeSelector(hero);
            return;
        }

        Item item = Reflection.newInstance(abilityClass);
        if (item instanceof Scroll) {
            // Execute the original scroll object so targeting, talents, status
            // effects, UI selectors, and special rules remain the real mechanics.
            ((Scroll)item).execute(hero, Scroll.AC_READ);
        }
    }

    public void showUpgradeSelector(Hero hero) {
        if (hero == null || !isEquipped(hero) || !hasMimicCharge()) return;
        new MimicUpgradeSelector(this).execute(hero, Scroll.AC_READ);
    }

    public Item upgradeSelectedItem(Hero hero, Item item) {
        if (hero == null || item == null || !isEquipped(hero) || !item.isUpgradable()) {
            return item;
        }
        if (!consumeMimicCharge()) return item;

        // Apply the exact Scroll of Upgrade mechanics, without consuming a real
        // scroll from the backpack. One confirmed +1 costs one mimic-ring charge.
        return ScrollOfUpgrade.applyUpgrade(hero, item);
    }

    /**
     * Temporary anonymous ScrollOfUpgrade used only for the ordinary inventory
     * selector. Once an item is selected, the real upgrader shown to WndUpgrade
     * is the MimicScrollRing itself.
     */
    private static class MimicUpgradeSelector extends ScrollOfUpgrade {

        private final MimicScrollRing ring;

        MimicUpgradeSelector(MimicScrollRing ring) {
            this.ring = ring;
            anonymize();
        }

        @Override
        protected void onItemSelected(Item item) {
            if (item != null && ring.hasMimicCharge()) {
                GameScene.show(new WndUpgrade(ring, item, false));
            }
        }
    }
}

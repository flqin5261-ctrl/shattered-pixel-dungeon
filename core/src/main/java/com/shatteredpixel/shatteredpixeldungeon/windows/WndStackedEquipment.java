/*
 * Shattered Pixel Dungeon Assist
 */
package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventorySlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * Compact one-row view for 纵横八荒 stacked equipment.
 *
 * Normal mode:
 * - tap an item -> make it the representative/primary item and open its action window.
 *
 * Selector mode:
 * - used by upgrade/identify/etc. inventory selectors so every stacked item can be
 *   targeted instead of only the first representative item.
 */
public class WndStackedEquipment extends Window {

    public enum Category {
        WEAPON, WAND, RING, ARTIFACT
    }

    private static final int SLOT = 24;
    private static final int GAP = 2;
    private static final int WIDTH_MIN = 96;
    private static final int WIDTH_MAX = 160;

    private final Window owner;
    private final WndBag.ItemSelector selector;

    public WndStackedEquipment(Category category) {
        this(null, category, null);
    }

    public WndStackedEquipment(Window owner, Category category, WndBag.ItemSelector selector) {
        super();
        this.owner = owner;
        this.selector = selector;

        final ArrayList<Item> items = itemsFor(category);
        int desired = Math.max(WIDTH_MIN, items.size() * (SLOT + GAP) - GAP);
        int width = Math.min(WIDTH_MAX, desired);

        RenderedTextBlock title = PixelScene.renderTextBlock(titleFor(category), 8);
        title.hardlight(TITLE_COLOR);
        title.maxWidth(width);
        title.setPos(0, 1);
        add(title);

        float paneY = title.bottom() + 3;

        Component content = new Component();
        float left = 0;
        for (final Item item : items) {
            InventorySlot slot = new InventorySlot(item) {
                @Override
                protected void onClick() {
                    if (selector != null) {
                        if (!selector.itemSelectable(item)) return;
                        if (selector.hideAfterSelecting()) {
                            hide();
                            if (owner != null) owner.hide();
                        }
                        selector.onSelect(item);
                        return;
                    }

                    Dungeon.hero.belongings.makeStackPrimary(item);
                    Item.updateQuickslot();
                    hide();
                    if (owner != null) owner.hide();
                    // Show a fresh action window rather than nesting it under this
                    // compact ScrollPane. This fixes the previous "tap does nothing"
                    // behavior on touch devices and makes the selected item obvious.
                    GameScene.show(new WndUseItem(null, item));
                }
            };
            slot.setRect(left, 0, SLOT, SLOT);
            if (selector != null && !selector.itemSelectable(item)) {
                slot.enable(false);
            }
            content.add(slot);
            left += SLOT + GAP;
        }
        content.setSize(Math.max(width, Math.max(1, left - GAP)), SLOT);

        ScrollPane scroll = new ScrollPane(content);
        add(scroll);

        // construct -> add -> resize -> setRect avoids ScrollPane clipping.
        resize(width, (int)Math.ceil(paneY + SLOT + 2));
        scroll.setRect(0, paneY, width, SLOT);
    }

    public static ArrayList<Item> itemsFor(Category category) {
        ArrayList<Item> result = new ArrayList<>();
        Belongings b = Dungeon.hero.belongings;
        switch (category) {
            case WEAPON:
                for (KindOfWeapon item : b.equippedWeapons()) result.add(item);
                break;
            case WAND:
                for (Wand item : b.equippedWands()) result.add(item);
                break;
            case RING:
                for (Ring item : b.equippedRings()) result.add(item);
                break;
            case ARTIFACT:
                for (Artifact item : b.equippedArtifacts()) result.add(item);
                break;
        }
        return result;
    }

    private static String titleFor(Category category) {
        switch (category) {
            case WEAPON: return "已装备武器";
            case WAND: return "已装备法杖";
            case RING: return "已装备戒指";
            case ARTIFACT: return "已装备神器";
            default: return "已装备物品";
        }
    }
}

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
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * 纵横八荒 stacked-equipment picker.
 *
 * IMPORTANT: InventorySlot's own pointer area cannot be trusted while nested in
 * a ScrollPane on mobile. WndBag already solved this by disabling each slot's
 * native pointer and forwarding ScrollPane.onClick(x,y) to the matching slot.
 * Keep the same interaction path here so both normal switching and inventory
 * selectors (upgrade/identify/etc.) receive the exact tapped item.
 */
public class WndStackedEquipment extends Window {

    public enum Category {
        WEAPON, WAND, RING, ARTIFACT
    }

    private static final int SLOT = 24;
    private static final int GAP = 2;
    private static final int COLS = 6;
    private static final int WIDTH = COLS * SLOT + (COLS - 1) * GAP;

    private final Window owner;
    private final WndBag.ItemSelector selector;
    private final ArrayList<StackInventorySlot> slots = new ArrayList<>();

    public WndStackedEquipment(Category category) {
        this(null, category, null);
    }

    public WndStackedEquipment(Window owner, Category category, WndBag.ItemSelector selector) {
        super();
        this.owner = owner;
        this.selector = selector;

        final ArrayList<Item> items = itemsFor(category);

        RenderedTextBlock title = PixelScene.renderTextBlock(
                selector != null ? selector.textPrompt() : titleFor(category), 8);
        title.hardlight(TITLE_COLOR);
        title.maxWidth(WIDTH);
        title.setPos(0, 1);
        add(title);

        float paneY = title.bottom() + 3;

        Component content = new Component();
        int col = 0;
        int row = 0;
        for (final Item item : items) {
            StackInventorySlot slot = new StackInventorySlot(item);
            slot.setRect(col * (SLOT + GAP), row * (SLOT + GAP), SLOT, SLOT);
            content.add(slot);
            slots.add(slot);
            slot.disableNativePointer();

            if (selector != null && !selector.itemSelectable(item)) {
                slot.enable(false);
                slot.disableNativePointer();
            }

            col++;
            if (col >= COLS) {
                col = 0;
                row++;
            }
        }

        int rows = Math.max(1, row + (col > 0 ? 1 : 0));
        int contentHeight = rows * SLOT + Math.max(0, rows - 1) * GAP;
        content.setSize(WIDTH, contentHeight);

        int maxViewport = Math.max(SLOT,
                PixelScene.uiCamera.height - chrome.marginVer() - (int)paneY - 8);
        int viewportHeight = Math.min(contentHeight, maxViewport);

        ScrollPane scroll = new ScrollPane(content) {
            @Override
            public void onClick(float x, float y) {
                StackInventorySlot slot = slotAt(x, y);
                if (slot != null) slot.clickFromPane();
            }
        };
        add(scroll);

        // Same safe lifecycle as the fixed backpack:
        // new ScrollPane -> add -> resize -> setRect.
        resize(WIDTH, (int)Math.ceil(paneY + viewportHeight + 2));
        scroll.setRect(0, paneY, WIDTH, viewportHeight);
    }

    private StackInventorySlot slotAt(float x, float y) {
        for (StackInventorySlot slot : slots) {
            if (slot.active && slot.inside(x, y)) return slot;
        }
        return null;
    }

    private class StackInventorySlot extends InventorySlot {

        private final Item stackItem;

        StackInventorySlot(Item item) {
            super(item);
            this.stackItem = item;
        }

        void disableNativePointer() {
            hotArea.active = false;
            hotArea.blockLevel = PointerArea.NEVER_BLOCK;
        }

        @Override
        public void update() {
            super.update();
            // ScrollPane owns touch handling; keep child pointer areas disabled.
            disableNativePointer();
        }

        void clickFromPane() {
            onClick();
        }

        @Override
        protected void onClick() {
            if (stackItem == null || Dungeon.hero == null) return;

            if (selector != null) {
                if (!selector.itemSelectable(stackItem)) return;

                if (selector.hideAfterSelecting()) {
                    hide();
                    if (owner != null) owner.hide();
                }

                // Pass the EXACT stacked item to the original selector. Upgrade
                // scrolls therefore upgrade item #2/#3/etc., not the representative.
                selector.onSelect(stackItem);
                return;
            }

            // Normal bag use: tapping a concrete stacked item makes it the
            // representative/primary item, then opens THAT item's action window.
            Dungeon.hero.belongings.makeStackPrimary(stackItem);
            Item.updateQuickslot();

            hide();
            if (owner != null) owner.hide();

            GameScene.show(new WndUseItem(null, stackItem));
        }
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

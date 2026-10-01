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
import com.watabou.noosa.Game;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * 纵横八荒 stacked-equipment picker.
 *
 * Touch handling mirrors WndBag: ScrollPane owns pointer events and forwards
 * taps to the exact item slot. In normal mode a single tap opens/selects that
 * item, while a double tap quickly unequips it. Selector mode remains immediate
 * single-tap so upgrade/identify/etc. always receive the exact chosen item.
 */
public class WndStackedEquipment extends Window {

    public enum Category {
        WEAPON, WAND, RING, ARTIFACT
    }

    private static final int SLOT = 24;
    private static final int GAP = 2;
    private static final int PAD = 2;
    private static final int MAX_COLS = 5;
    private static final float EQUIP_DOUBLE_TAP_WINDOW = 0.28f;

    private final Window owner;
    private final WndBag.ItemSelector selector;
    private final ArrayList<StackInventorySlot> slots = new ArrayList<>();

    private StackInventorySlot pendingTapSlot;
    private float pendingTapTimer;

    public WndStackedEquipment(Category category) {
        this(null, category, null);
    }

    public WndStackedEquipment(Window owner, Category category, WndBag.ItemSelector selector) {
        super();
        this.owner = owner;
        this.selector = selector;

        final ArrayList<Item> items = itemsFor(category);

        // Never build a picker wider than the active UI camera. The old fixed
        // six-column width could center partly off-screen on narrow phones,
        // clipping the leftmost item as shown in the user's screenshot.
        int maxWindowWidth = Math.max(SLOT + PAD * 2,
                PixelScene.uiCamera.width - chrome.marginHor() - 4);
        int maxColsForScreen = Math.max(1,
                (maxWindowWidth - PAD * 2 + GAP) / (SLOT + GAP));
        maxColsForScreen = Math.min(MAX_COLS, maxColsForScreen);

        // Selector windows must stay wide enough for prompts such as
        // "选择要强化 +120 的装备". The old implementation shrank to the
        // number of candidate items, so a one-item category became a ~28px-wide
        // window and wrapped Chinese text one character per line.
        int cols;
        if (selector != null) {
            cols = maxColsForScreen;
        } else {
            cols = Math.min(maxColsForScreen, Math.max(1, items.size()));
        }
        final int width = PAD * 2 + cols * SLOT + Math.max(0, cols - 1) * GAP;

        RenderedTextBlock title = PixelScene.renderTextBlock(
                selector != null ? selector.textPrompt() : titleFor(category), 8);
        title.hardlight(TITLE_COLOR);
        title.maxWidth(width - PAD * 2);
        title.setPos(PAD, 1);
        add(title);

        float paneY = title.bottom() + 3;

        Component content = new Component();
        int col = 0;
        int row = 0;

        // When a selector has fewer items than available columns, keep the item
        // row centered inside the wider prompt-safe window instead of pinning a
        // lone slot to the left edge.
        int visibleCols = Math.min(cols, Math.max(1, items.size()));
        float firstRowLeft = PAD;
        if (selector != null && items.size() < cols) {
            float rowWidth = visibleCols * SLOT + Math.max(0, visibleCols - 1) * GAP;
            firstRowLeft = (width - rowWidth) / 2f;
        }

        for (final Item item : items) {
            StackInventorySlot slot = new StackInventorySlot(item);
            float rowLeft = (selector != null && items.size() < cols) ? firstRowLeft : PAD;
            slot.setRect(rowLeft + col * (SLOT + GAP), PAD + row * (SLOT + GAP), SLOT, SLOT);
            content.add(slot);
            slots.add(slot);
            slot.disableNativePointer();

            if (selector != null && !selector.itemSelectable(item)) {
                slot.enable(false);
                slot.disableNativePointer();
            }

            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }

        int rows = Math.max(1, row + (col > 0 ? 1 : 0));
        int contentHeight = PAD * 2 + rows * SLOT + Math.max(0, rows - 1) * GAP;
        content.setSize(width, contentHeight);

        int maxViewport = Math.max(SLOT + PAD * 2,
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

        // Safe ScrollPane lifecycle: construct -> add -> resize -> setRect.
        resize(width, (int)Math.ceil(paneY + viewportHeight + 2));
        scroll.setRect(0, paneY, width, viewportHeight);
    }

    private StackInventorySlot slotAt(float x, float y) {
        for (StackInventorySlot slot : slots) {
            if (slot.active && slot.inside(x, y)) return slot;
        }
        return null;
    }

    @Override
    public void update() {
        super.update();
        if (pendingTapSlot != null && pendingTapTimer > 0f) {
            pendingTapTimer -= Game.elapsed;
            if (pendingTapTimer <= 0f) {
                StackInventorySlot slot = pendingTapSlot;
                pendingTapSlot = null;
                pendingTapTimer = 0f;
                if (slot != null && slot.active) slot.performSingleTap();
            }
        }
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

                selector.onSelect(stackItem);
                return;
            }

            if (pendingTapSlot == this && pendingTapTimer > 0f) {
                pendingTapSlot = null;
                pendingTapTimer = 0f;
                if (WndBag.quickToggleEquipment(stackItem)) {
                    hide();
                    if (owner != null) owner.hide();
                } else {
                    performSingleTap();
                }
                return;
            }

            pendingTapSlot = this;
            pendingTapTimer = EQUIP_DOUBLE_TAP_WINDOW;
        }

        void performSingleTap() {
            if (stackItem == null || Dungeon.hero == null) return;

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

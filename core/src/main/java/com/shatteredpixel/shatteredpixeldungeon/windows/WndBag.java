/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GenesisEcho;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventorySlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.RightClickMenu;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PointF;

import java.util.ArrayList;

public class WndBag extends WndTabbed {
	
	//only one bag window can appear at a time
	public static Window INSTANCE;

	protected static final int COLS_P   = 5;
	protected static final int COLS_L   = 5;
	
	protected static int SLOT_WIDTH_P   = 28;
	protected static int SLOT_WIDTH_L   = 28;

	protected static int SLOT_HEIGHT_P	= 28;
	protected static int SLOT_HEIGHT_L	= 28;

	protected static final int SLOT_MARGIN	= 1;
	
	protected static final int TITLE_HEIGHT	= 14;
	
	private ItemSelector selector;

	private int nCols;
	private int nRows;

	private int slotWidth;
	private int slotHeight;

	protected int count;
	protected int col;
	protected int row;
	
	private static Bag lastBag;

	private Component itemContent;
	private ScrollPane itemScroll;
	private final ArrayList<BagInventorySlot> itemSlots = new ArrayList<>();

	private static final float EQUIP_DOUBLE_TAP_WINDOW = 0.28f;
	private BagInventorySlot pendingTapSlot;
	private float pendingTapTimer;

	public WndBag( Bag bag ) {
		this(bag, null);
	}

	public WndBag( Bag bag, ItemSelector selector ) {
		
		super();
		
		if( INSTANCE != null ){
			INSTANCE.hide();
		}
		INSTANCE = this;
		
		this.selector = selector;
		
		lastBag = bag;

		slotWidth = PixelScene.landscape() ? SLOT_WIDTH_L : SLOT_WIDTH_P;
		slotHeight = PixelScene.landscape() ? SLOT_HEIGHT_L : SLOT_HEIGHT_P;

		nCols = PixelScene.landscape() ? COLS_L : COLS_P;

		int windowWidth = slotWidth * nCols + SLOT_MARGIN * (nCols - 1);
		while (slotWidth >= 24 && (windowWidth + chrome.marginHor()) > PixelScene.uiCamera.width){
			slotWidth--;
			windowWidth = slotWidth * nCols + SLOT_MARGIN * (nCols - 1);
		}

		placeTitle( bag, windowWidth );

		itemContent = new Component();
		placeItems( bag );

		int rowsUsed = row + (col > 0 ? 1 : 0);
		int contentHeight = Math.max(slotHeight,
				rowsUsed * slotHeight + Math.max(0, rowsUsed - 1) * SLOT_MARGIN);
		itemContent.setSize(windowWidth, contentHeight);

		int maxViewportHeight = Math.max(slotHeight,
				PixelScene.uiCamera.height - chrome.marginVer() - tabHeight() - TITLE_HEIGHT - 6);
		int viewportHeight = Math.min(contentHeight, maxViewportHeight);

		itemScroll = new ScrollPane(itemContent){
			@Override
			public void onClick(float x, float y) {
				BagInventorySlot slot = slotAt(x, y);
				if (slot != null) slot.clickFromPane();
			}
		};
		add(itemScroll);

		int windowHeight = TITLE_HEIGHT + viewportHeight;
		// resize() recenters WndBag; lay out the ScrollPane only after the final
		// window position is known so its content camera lines up with the slots.
		resize( windowWidth, windowHeight );
		itemScroll.setRect(0, TITLE_HEIGHT, windowWidth, viewportHeight);

		int i = 1;
		for (Bag b : Dungeon.hero.belongings.getBags()) {
			if (b != null) {
				BagTab tab = new BagTab( b, i++ );
				add( tab );
				tab.select( b == bag );
				if  (b == bag){
					selected = tab;
				}
			}
		}

		layoutTabs();
	}

	public ItemSelector getSelector() {
		return selector;
	}

	public static WndBag lastBag(ItemSelector selector ) {
		
		if (lastBag != null && Dungeon.hero.belongings.backpack.contains( lastBag )) {
			
			return new WndBag( lastBag, selector );
			
		} else {
			
			return new WndBag( Dungeon.hero.belongings.backpack, selector );
			
		}
	}

	public static WndBag getBag( ItemSelector selector ) {
		if (selector.preferredBag() == Belongings.Backpack.class){
			return new WndBag( Dungeon.hero.belongings.backpack, selector );

		} else if (selector.preferredBag() != null){
			Bag bag = Dungeon.hero.belongings.getItem( selector.preferredBag() );
			if (bag != null)    return new WndBag( bag, selector );
			//if a specific preferred bag isn't present, then the relevant items will be in backpack
			else                return new WndBag( Dungeon.hero.belongings.backpack, selector );
		}

		return lastBag( selector );
	}
	
	protected void placeTitle( Bag bag, int width ){

		float titleWidth;
		if (Dungeon.energy == 0) {
			ItemSprite gold = new ItemSprite(ItemSpriteSheet.GOLD, null);
			gold.x = width - gold.width();
			gold.y = (TITLE_HEIGHT - gold.height()) / 2f;
			PixelScene.align(gold);
			add(gold);

			BitmapText amt = new BitmapText(Integer.toString(Dungeon.gold), PixelScene.pixelFont);
			amt.hardlight(TITLE_COLOR);
			amt.measure();
			amt.x = width - gold.width() - amt.width() - 1;
			amt.y = (TITLE_HEIGHT - amt.baseLine()) / 2f - 1;
			PixelScene.align(amt);
			add(amt);

			titleWidth = amt.x;
		} else {

			Image gold = Icons.get(Icons.COIN_SML);
			gold.x = width - gold.width() - 0.5f;
			gold.y = 0;
			PixelScene.align(gold);
			add(gold);

			BitmapText amt = new BitmapText(Integer.toString(Dungeon.gold), PixelScene.pixelFont);
			amt.hardlight(TITLE_COLOR);
			amt.measure();
			amt.x = width - gold.width() - amt.width() - 2f;
			amt.y = 0;
			PixelScene.align(amt);
			add(amt);

			titleWidth = amt.x;

			Image energy = Icons.get(Icons.ENERGY_SML);
			energy.x = width - energy.width();
			energy.y = gold.height();
			PixelScene.align(energy);
			add(energy);

			amt = new BitmapText(Integer.toString(Dungeon.energy), PixelScene.pixelFont);
			amt.hardlight(0x44CCFF);
			amt.measure();
			amt.x = width - energy.width() - amt.width() - 1;
			amt.y = energy.y;
			PixelScene.align(amt);
			add(amt);

			titleWidth = Math.min(titleWidth, amt.x);
		}

		String title = selector != null ? selector.textPrompt() : null;
		RenderedTextBlock txtTitle = PixelScene.renderTextBlock(
				title != null ? Messages.titleCase(title) : Messages.titleCase( bag.name() ), 8 );
		txtTitle.hardlight( TITLE_COLOR );
		txtTitle.maxWidth( (int)titleWidth - 2 );
		txtTitle.setPos(
				1,
				(TITLE_HEIGHT - txtTitle.height()) / 2f - 1
		);
		PixelScene.align(txtTitle);
		add( txtTitle );
	}
	
	protected void placeItems( Bag container ) {
		
		// Equipped items. Under 纵横八荒 the root bag intentionally shows one
		// representative per stack; tapping it opens the complete one-row stack.
		Belongings stuff = Dungeon.hero.belongings;
		if (GenesisEcho.unrestrictedEquipment(Dungeon.hero)) {
			placeItem(stuff.displayWeapon() != null ? stuff.displayWeapon() : new Placeholder(ItemSpriteSheet.WEAPON_HOLDER),
					WndStackedEquipment.Category.WEAPON);
			placeItem(stuff.armor != null ? stuff.armor : new Placeholder(ItemSpriteSheet.ARMOR_HOLDER));
			placeItem(stuff.displayArtifact() != null ? stuff.displayArtifact() : new Placeholder(ItemSpriteSheet.ARTIFACT_HOLDER),
					WndStackedEquipment.Category.ARTIFACT);
			placeItem(stuff.breakthroughCertificate != null ? stuff.breakthroughCertificate : new Placeholder(ItemSpriteSheet.ARTIFACT_HOLDER));
			placeItem(stuff.displayWand() != null ? stuff.displayWand() : new Placeholder(ItemSpriteSheet.WAND_HOLDER),
					WndStackedEquipment.Category.WAND);
			placeItem(stuff.displayRing() != null ? stuff.displayRing() : new Placeholder(ItemSpriteSheet.RING_HOLDER),
					WndStackedEquipment.Category.RING);
		} else {
			placeItem( stuff.weapon != null ? stuff.weapon : new Placeholder( ItemSpriteSheet.WEAPON_HOLDER ) );
			placeItem( stuff.armor != null ? stuff.armor : new Placeholder( ItemSpriteSheet.ARMOR_HOLDER ) );
			placeItem( stuff.artifact != null ? stuff.artifact : new Placeholder( ItemSpriteSheet.ARTIFACT_HOLDER ) );
			placeItem( stuff.breakthroughCertificate != null ? stuff.breakthroughCertificate : new Placeholder( ItemSpriteSheet.ARTIFACT_HOLDER ) );
			placeItem( stuff.misc != null ? stuff.misc : new Placeholder( ItemSpriteSheet.SOMETHING ) );
			placeItem( stuff.ring != null ? stuff.ring : new Placeholder( ItemSpriteSheet.RING_HOLDER ) );
		}

		int equipped = 6;

		//the container itself if it's not the root backpack
		if (container != Dungeon.hero.belongings.backpack){
			placeItem(container);
			count--; //don't count this one, as it's not actually inside of itself
		} else if (stuff.secondWep != null && !GenesisEcho.unrestrictedEquipment(Dungeon.hero)) {
			// In the normal rules the Champion's second weapon is a separate equipped
			// slot. Under 纵横八荒 it is already represented inside the weapon stack.
			placeItem(stuff.secondWep);
			equipped++;
		}

		// Items in the bag, except other containers (they have tags at the bottom)
		for (Item item : container.items.toArray(new Item[0])) {
			if (!(item instanceof Bag)) {
				placeItem( item );
			} else {
				count++;
			}
		}
		
		// Free Space
		while ((count - equipped) < container.capacity()) {
			placeItem( null );
		}
	}
	
	protected void placeItem(final Item item) {
		placeItem(item, null);
	}

	protected void placeItem(final Item item, final WndStackedEquipment.Category stackCategory) {

		count++;

		int x = col * (slotWidth + SLOT_MARGIN);
		int y = row * (slotHeight + SLOT_MARGIN);

		BagInventorySlot slot = new BagInventorySlot(item, stackCategory);
		slot.setRect( x, y, slotWidth, slotHeight );
		itemContent.add(slot);
		itemSlots.add(slot);
		slot.disableNativePointer();

		if (item == null || (selector != null && stackCategory == null && !selector.itemSelectable(item))){
			slot.enable(false);
			slot.disableNativePointer();
		}

		if (++col >= nCols) {
			col = 0;
			row++;
		}

	}

	private BagInventorySlot slotAt(float x, float y) {
		for (BagInventorySlot slot : itemSlots) {
			if (slot.active && slot.inside(x, y)) return slot;
		}
		return null;
	}

	private class BagInventorySlot extends InventorySlot {

		private final Item bagItem;
		private final WndStackedEquipment.Category stackCategory;

		BagInventorySlot(Item item, WndStackedEquipment.Category stackCategory) {
			super(item);
			this.bagItem = item;
			this.stackCategory = stackCategory;
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
			if (bagItem == null) return;

			// Selectors (upgrade/identify/etc.) must stay single-tap and immediate.
			if (selector != null) {
				if (stackCategory != null && GenesisEcho.unrestrictedEquipment(Dungeon.hero)) {
					Game.scene().addToFront(new WndStackedEquipment(WndBag.this, stackCategory, selector));
					return;
				}
				if (selector.hideAfterSelecting()) hide();
				selector.onSelect(bagItem);
				return;
			}

			// Normal backpack interaction is delayed very slightly so two taps can be
			// distinguished. Double tap toggles equip/unequip; single tap keeps the
			// original behavior (open stack or item action window).
			if (pendingTapSlot == this && pendingTapTimer > 0f) {
				pendingTapSlot = null;
				pendingTapTimer = 0f;
				if (quickToggleEquipment(bagItem)) {
					// Keep the backpack open for bulk equipment management. Rebuild
					// the visible slots immediately because equip/unequip moves items
					// between the backpack and the equipped collections.
					refreshAfterEquipmentToggle();
				} else {
					performSingleTap();
				}
				return;
			}

			pendingTapSlot = this;
			pendingTapTimer = EQUIP_DOUBLE_TAP_WINDOW;
		}

		void performSingleTap() {
			if (bagItem == null) return;

			if (stackCategory != null && GenesisEcho.unrestrictedEquipment(Dungeon.hero)) {
				Game.scene().addToFront(new WndStackedEquipment(WndBag.this, stackCategory, null));
				return;
			}

			if (lastBag != bagItem && !lastBag.contains(bagItem) && !bagItem.isEquipped(Dungeon.hero)) {
				hide();
			} else {
				Game.scene().addToFront(new WndUseItem(WndBag.this, bagItem));
			}
		}
	}

	@Override
	public void update() {
		super.update();
		if (pendingTapSlot != null && pendingTapTimer > 0f) {
			pendingTapTimer -= Game.elapsed;
			if (pendingTapTimer <= 0f) {
				BagInventorySlot slot = pendingTapSlot;
				pendingTapSlot = null;
				pendingTapTimer = 0f;
				if (slot != null && slot.active) slot.performSingleTap();
			}
		}
	}

	float currentScrollY() {
		if (itemScroll == null || itemScroll.content() == null
				|| itemScroll.content().camera == null) {
			return 0f;
		}
		return itemScroll.content().camera.scroll.y;
	}

	WndBag refreshAfterEquipmentToggle() {
		float oldScrollY = currentScrollY();
		ItemSelector keepSelector = selector;

		Bag targetBag = lastBag;
		if (targetBag == null
				|| (targetBag != Dungeon.hero.belongings.backpack
				&& !Dungeon.hero.belongings.backpack.contains(targetBag))) {
			targetBag = Dungeon.hero.belongings.backpack;
		}

		// The constructor hides the existing WndBag INSTANCE. Recreate it in-place
		// and restore the old scroll offset so ten consecutive equips/unequips do
		// not force the player back to the top each time.
		WndBag fresh = new WndBag(targetBag, keepSelector);
		Game.scene().addToFront(fresh);
		if (fresh.itemScroll != null) {
			fresh.itemScroll.scrollTo(0, oldScrollY);
		}
		return fresh;
	}

	static boolean quickToggleEquipment(Item item) {
		if (item == null || Dungeon.hero == null || !Dungeon.hero.ready) return false;
		Hero hero = Dungeon.hero;

		if (item instanceof EquipableItem) {
			EquipableItem equipable = (EquipableItem)item;
			item.execute(hero, equipable.isEquipped(hero)
					? EquipableItem.AC_UNEQUIP
					: EquipableItem.AC_EQUIP);
			return true;
		}

		// Wands are only wearable through 纵横八荒, but should behave exactly like
		// the other equipment categories for backpack double-tap.
		if (item instanceof Wand && GenesisEcho.unrestrictedEquipment(hero)) {
			Wand wand = (Wand)item;
			wand.execute(hero, wand.isEquipped(hero)
					? EquipableItem.AC_UNEQUIP
					: EquipableItem.AC_EQUIP);
			return true;
		}

		return false;
	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (event.pressed && KeyBindings.getActionForKey( event ) == SPDAction.INVENTORY) {
			onBackPressed();
			return true;
		} else {
			return super.onSignal(event);
		}
	}
	
	@Override
	public void onBackPressed() {
		if (selector != null) {
			selector.onSelect( null );
		}
		super.onBackPressed();
	}
	
	@Override
	protected void onClick( Tab tab ) {
		hide();
		Window w = new WndBag(((BagTab) tab).bag, selector);
		if (Game.scene() instanceof GameScene){
			GameScene.show(w);
		} else {
			Game.scene().addToFront(w);
		}
	}
	
	@Override
	public void hide() {
		super.hide();
		if (INSTANCE == this){
			INSTANCE = null;
		}
	}
	
	@Override
	protected int tabHeight() {
		return 20;
	}
	
	private Image icon( Bag bag ) {
		if (bag instanceof VelvetPouch) {
			return Icons.get( Icons.SEED_POUCH );
		} else if (bag instanceof ScrollHolder) {
			return Icons.get( Icons.SCROLL_HOLDER );
		} else if (bag instanceof MagicalHolster) {
			return Icons.get( Icons.WAND_HOLSTER );
		} else if (bag instanceof PotionBandolier) {
			return Icons.get( Icons.POTION_BANDOLIER );
		} else {
			return Icons.get( Icons.BACKPACK );
		}
	}
	
	private class BagTab extends IconTab {

		private Bag bag;
		private int index;
		
		public BagTab( Bag bag, int index ) {
			super( icon(bag) );
			
			this.bag = bag;
			this.index = index;
		}

		@Override
		public GameAction keyAction() {
			switch (index){
				case 1: default:
					return SPDAction.BAG_1;
				case 2:
					return SPDAction.BAG_2;
				case 3:
					return SPDAction.BAG_3;
				case 4:
					return SPDAction.BAG_4;
				case 5:
					return SPDAction.BAG_5;
			}
		}

		@Override
		protected String hoverText() {
			return Messages.titleCase(bag.name());
		}
	}
	
	public static class Placeholder extends Item {

		public Placeholder(int image ) {
			this.image = image;
		}

		@Override
		public String name() {
			return null;
		}

		@Override
		public boolean isIdentified() {
			return true;
		}
		
		@Override
		public boolean isEquipped( Hero hero ) {
			return true;
		}
	}

	public abstract static class ItemSelector {
		public abstract String textPrompt();
		public Class<?extends Bag> preferredBag(){
			return null; //defaults to last bag opened
		}
		public boolean hideAfterSelecting(){
			return true; //defaults to hiding the window when an item is picked
		}
		public abstract boolean itemSelectable( Item item );
		public abstract void onSelect( Item item );
	}
}

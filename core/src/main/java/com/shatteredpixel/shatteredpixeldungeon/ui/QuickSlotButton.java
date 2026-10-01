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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GenesisEcho;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.MimicRing;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndKeyBindings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndStackedEquipment;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUseItem;
import com.watabou.input.GameAction;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

public class QuickSlotButton extends Button {
	
	private static QuickSlotButton[] instance = new QuickSlotButton[QuickSlot.SIZE];
	private int slotNum;

	private ItemSlot slot;
	
	private Image crossB;
	private Image crossM;
	
	public static int targetingSlot = -1;
	public static Char lastTarget = null;

	private static final float GENESIS_WAND_DOUBLE_TAP = 0.35f;
	private static final float GENESIS_ARTIFACT_DOUBLE_TAP = 0.28f;
	private static final float MIMIC_RING_DOUBLE_TAP = 0.28f;
	private Wand genesisTapWand;
	private float genesisTapTimer;
	private Artifact genesisTapArtifact;
	private float genesisArtifactTapTimer;
	private MimicRing mimicTapRing;
	private float mimicRingTapTimer;
	
	public QuickSlotButton( int slotNum ) {
		super();
		this.slotNum = slotNum;
		item( select( slotNum ) );
		
		instance[slotNum] = this;
	}
	
	@Override
	public void destroy() {
		super.destroy();
		
		reset();
	}

	public static void reset() {
		instance = new QuickSlotButton[QuickSlot.SIZE];

		lastTarget = null;
		targetingSlot = -1;
	}
	
	@Override
	protected void createChildren() {
		super.createChildren();
		
		slot = new ItemSlot() {
			@Override
			protected void onClick() {
				if (!Dungeon.hero.isAlive() || !Dungeon.hero.ready){
					return;
				}

				Item tappedItem = select(slotNum);

				// Active mimic rings work before level 60 as low-level equipment.
				// Single tap invokes the stored consumable; double tap opens a picker
				// for the other equipped mimic ring.
				if (tappedItem instanceof MimicRing
						&& Dungeon.hero.belongings.isStackEquipped(tappedItem)) {
					MimicRing ring = (MimicRing)tappedItem;

					if (mimicTapRing == ring && mimicRingTapTimer > 0f) {
						mimicTapRing = null;
						mimicRingTapTimer = 0f;
						openMimicRingSelector();
						return;
					}

					mimicTapRing = ring;
					mimicRingTapTimer = MIMIC_RING_DOUBLE_TAP;
					return;
				}

				// 纵横八荒 artifacts use the normal quickslots. A single tap uses the
				// current artifact; a double tap opens the equipped-artifact picker so
				// the player chooses exactly which artifact this quickslot should hold.
				if (tappedItem instanceof Artifact
						&& GenesisEcho.unrestrictedEquipment(Dungeon.hero)
						&& Dungeon.hero.belongings.isStackEquipped(tappedItem)) {
					Artifact artifact = (Artifact)tappedItem;

					if (genesisTapArtifact == artifact && genesisArtifactTapTimer > 0f) {
						genesisTapArtifact = null;
						genesisArtifactTapTimer = 0f;
						openArtifactSelector();
						return;
					}

					genesisTapArtifact = artifact;
					genesisArtifactTapTimer = GENESIS_ARTIFACT_DOUBLE_TAP;
					return;
				}

				if (tappedItem instanceof Wand && GenesisEcho.ultraSpellcast(Dungeon.hero)) {
					Wand wand = (Wand)tappedItem;

					if (genesisTapWand == wand && genesisTapTimer > 0f) {
						genesisTapWand = null;
						genesisTapTimer = 0f;
						GameScene.ready();
						wand.genesisScreenCast(Dungeon.hero);
						return;
					}

					genesisTapWand = wand;
					genesisTapTimer = GENESIS_WAND_DOUBLE_TAP;

					if (Dungeon.hero.belongings.contains(tappedItem)) {
						GameScene.cancelCellSelector();
						GameScene.centerNextWndOnInvPane();
						tappedItem.execute(Dungeon.hero);
						useTargeting();
					}
					return;
				}

				if (targetingSlot == slotNum && lastTarget != null) {
					int cell = autoAim(lastTarget, select(slotNum));

					if (cell != -1){
						GameScene.handleCell(cell);
					} else {
						//couldn't auto-aim, just target the position and hope for the best.
						GameScene.handleCell( lastTarget.pos );
					}
				} else {
					Item item = select(slotNum);
					if (Dungeon.hero.belongings.contains(item) && !GameScene.cancel()) {
						GameScene.centerNextWndOnInvPane();
						item.execute(Dungeon.hero);
						if (item.usesTargeting) {
							useTargeting();
						}
					}
				}
			}

			@Override
			protected void onRightClick() {
				QuickSlotButton.this.onLongClick();
			}

			@Override
			protected void onMiddleClick() {
				onClick();
			}

			@Override
			public GameAction keyAction() {
				return QuickSlotButton.this.keyAction();
			}
			@Override
			public GameAction secondaryTooltipAction(){
				return QuickSlotButton.this.secondaryTooltipAction();
			}
			@Override
			protected boolean onLongClick() {
				return QuickSlotButton.this.onLongClick();
			}
			@Override
			protected void onPointerDown() {
				sprite.lightness( 0.7f );
			}
			@Override
			protected void onPointerUp() {
				sprite.resetColor();
			}

			@Override
			protected String hoverText() {
				if (item == null){
					return Messages.titleCase(Messages.get(WndKeyBindings.class, "quickslot_" + (slotNum+1)));
				} else {
					return super.hoverText();
				}
			}
		};
		slot.showExtraInfo( false );
		add( slot );
		
		crossB = Icons.TARGET.get();
		crossB.visible = false;
		add( crossB );
		
		crossM = new Image();
		crossM.copy( crossB );
	}
	
	@Override
	protected void layout() {
		super.layout();
		
		slot.fill( this );
		
		crossB.x = x + (width - crossB.width) / 2;
		crossB.y = y + (height - crossB.height) / 2;
		PixelScene.align(crossB);
	}

	public void alpha( float value ){
		slot.alpha(value);
	}

	@Override
	public void update() {
		super.update();
		if (genesisTapTimer > 0f) {
			genesisTapTimer -= Game.elapsed;
			if (genesisTapTimer <= 0f) {
				genesisTapTimer = 0f;
				genesisTapWand = null;
			}
		}

		if (genesisArtifactTapTimer > 0f) {
			genesisArtifactTapTimer -= Game.elapsed;
			if (genesisArtifactTapTimer <= 0f) {
				Artifact artifact = genesisTapArtifact;
				genesisArtifactTapTimer = 0f;
				genesisTapArtifact = null;
				useArtifactFromQuickslot(artifact);
			}
		}

		if (mimicRingTapTimer > 0f) {
			mimicRingTapTimer -= Game.elapsed;
			if (mimicRingTapTimer <= 0f) {
				MimicRing ring = mimicTapRing;
				mimicRingTapTimer = 0f;
				mimicTapRing = null;
				useMimicRingFromQuickslot(ring);
			}
		}
		if (targetingSlot != -1 && lastTarget != null && lastTarget.sprite != null){
			crossM.point(lastTarget.sprite.center(crossM));
		}
	}

	private void useArtifactFromQuickslot(Artifact artifact) {
		if (artifact == null || Dungeon.hero == null || !Dungeon.hero.isAlive()
				|| !Dungeon.hero.ready || !Dungeon.hero.belongings.isStackEquipped(artifact)
				|| select(slotNum) != artifact) {
			return;
		}

		if (!GameScene.cancel()) {
			GameScene.centerNextWndOnInvPane();
			String action = artifact.defaultAction();
			if (action == null || "NONE".equals(action)) {
				GameScene.show(new WndUseItem(null, artifact));
			} else {
				artifact.execute(Dungeon.hero);
				if (artifact.usesTargeting) useTargeting();
			}
		}
	}

	private void useMimicRingFromQuickslot(MimicRing ring) {
		if (ring == null || Dungeon.hero == null || !Dungeon.hero.isAlive()
				|| !Dungeon.hero.ready || !Dungeon.hero.belongings.isStackEquipped(ring)
				|| select(slotNum) != ring) {
			return;
		}

		if (!GameScene.cancel()) {
			GameScene.centerNextWndOnInvPane();
			ring.execute(Dungeon.hero);
		}
	}

	private void openMimicRingSelector() {
		if (Dungeon.hero == null || !Dungeon.hero.ready) return;

		GameScene.cancelCellSelector();
		GameScene.ready();
		Game.scene().addToFront(new WndStackedEquipment(
				null,
				WndStackedEquipment.Category.RING,
				new WndBag.ItemSelector() {
					@Override
					public String textPrompt() {
						return "选择快捷戒指";
					}

					@Override
					public boolean itemSelectable(Item item) {
						return item instanceof MimicRing
								&& Dungeon.hero.belongings.isStackEquipped(item);
					}

					@Override
					public void onSelect(Item item) {
						if (item instanceof MimicRing) {
							set(slotNum, item);
						}
					}
				}
		));
	}

	private void openArtifactSelector() {
		if (Dungeon.hero == null || !Dungeon.hero.ready) return;

		GameScene.cancelCellSelector();
		GameScene.ready();
		Game.scene().addToFront(new WndStackedEquipment(
				null,
				WndStackedEquipment.Category.ARTIFACT,
				new WndBag.ItemSelector() {
					@Override
					public String textPrompt() {
						return "选择快捷神器";
					}

					@Override
					public boolean itemSelectable(Item item) {
						return item instanceof Artifact
								&& Dungeon.hero.belongings.isStackEquipped(item);
					}

					@Override
					public void onSelect(Item item) {
						if (item instanceof Artifact) {
							set(slotNum, item);
						}
					}
				}
		));
	}

	@Override
	public GameAction keyAction() {
		switch (slotNum){
			case 0:
				return SPDAction.QUICKSLOT_1;
			case 1:
				return SPDAction.QUICKSLOT_2;
			case 2:
				return SPDAction.QUICKSLOT_3;
			case 3:
				return SPDAction.QUICKSLOT_4;
			case 4:
				return SPDAction.QUICKSLOT_5;
			case 5:
				return SPDAction.QUICKSLOT_6;
			default:
				return super.keyAction();
		}
	}

	@Override
	public GameAction secondaryTooltipAction() {
		return SPDAction.QUICKSLOT_SELECTOR;
	}

	@Override
	protected String hoverText() {
		if (slot.item == null){
			return Messages.titleCase(Messages.get(WndKeyBindings.class, "quickslot_" + (slotNum+1)));
		} else {
			return super.hoverText();
		}
	}
	
	@Override
	protected void onClick() {
		if (Dungeon.hero.ready && !GameScene.cancel()) {
			GameScene.selectItem(itemSelector);
		}
	}

	@Override
	protected void onRightClick() {
		onClick();
	}

	@Override
	protected void onMiddleClick() {
		onClick();
	}

	@Override
	protected boolean onLongClick() {
		onClick();
		return true;
	}

	private WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(QuickSlotButton.class, "select_item");
		}

		@Override
		public boolean itemSelectable(Item item) {
			if (item instanceof Artifact && GenesisEcho.unrestrictedEquipment(Dungeon.hero)
					&& Dungeon.hero.belongings.isStackEquipped(item)) {
				return true;
			}
			return item.defaultAction() != null;
		}

		@Override
		public void onSelect(Item item) {
			if (item != null) {
				set( slotNum , item );
			}
		}
	};

	public static int lastVisible = instance.length;

	public static void set(Item item){
		for (int i = 0; i < lastVisible; i++) {
			if (select(i) == null || select(i) == item) {
				set(i, item);
				return;
			}
		}
		set(0, item);
	}

	public static void set(int slotNum, Item item){
		Dungeon.quickslot.setSlot( slotNum , item );
		refresh();

		//Remember if the player adds the waterskin as one of their first actions.
		if (Statistics.duration + Actor.now() <= 10){
			boolean containsWaterskin = false;
			for (int i = 0; i < instance.length; i++) {
				if (select(i) instanceof Waterskin) containsWaterskin = true;
			}
			if (containsWaterskin) SPDSettings.quickslotWaterskin(true);
		}
	}

	private static Item select(int slotNum){
		return Dungeon.quickslot.getItem( slotNum );
	}
	
	public void item( Item item ) {
		slot.item( item );
		enableSlot();
	}

	public void enable( boolean value ) {
		active = value;
		if (value) {
			enableSlot();
		} else {
			slot.enable( false );
		}
	}
	
	private void enableSlot() {
		slot.enable(Dungeon.quickslot.isNonePlaceholder( slotNum )
				&& (!Dungeon.hero.belongings.lostInventory() || Dungeon.quickslot.getItem(slotNum).keptThroughLostInventory()));
	}

	public void slotMargins( int left, int top, int right, int bottom){
		slot.setMargins(left, top, right, bottom);
	}

	public static void useTargeting(int idx){
		instance[idx].useTargeting();
	}

	private void useTargeting() {

		if (lastTarget != null &&
				Actor.chars().contains( lastTarget ) &&
				lastTarget.isAlive() &&
				lastTarget.alignment != Char.Alignment.ALLY &&
				Dungeon.level.heroFOV[lastTarget.pos]) {

			targetingSlot = slotNum;
			CharSprite sprite = lastTarget.sprite;

			if (sprite.parent != null) {
				sprite.parent.addToFront(crossM);
				crossM.point(sprite.center(crossM));
			}

			crossB.point(slot.sprite.center(crossB));
			crossB.visible = true;

		} else {

			lastTarget = null;
			targetingSlot = -1;

		}

	}

	public static int autoAim(Char target){
		//will use generic projectile logic if no item is specified
		return autoAim(target, new Item());
	}

	//FIXME: this is currently very expensive, should either optimize ballistica or this, or both
	public static int autoAim(Char target, Item item){
		if (Dungeon.hero == null || target == null){
			return -1;
		}

		//first try to directly target
		if (item.targetingPos(Dungeon.hero, target.pos) == target.pos) {
			return target.pos;
		}

		//Otherwise pick nearby tiles to try and 'angle' the shot, auto-aim basically.
		PathFinder.buildDistanceMap( target.pos, BArray.not( new boolean[Dungeon.level.length()], null ), 2 );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE
					&& item.targetingPos(Dungeon.hero, i) == target.pos)
				return i;
		}

		//couldn't find a cell, give up.
		return -1;
	}

	public static void refresh() {
		for (int i = 0; i < instance.length; i++) {
			if (instance[i] != null) {
				instance[i].item(select(i));
				instance[i].enable(instance[i].active);
			}
		}
		if (Toolbar.SWAP_INSTANCE != null){
			Toolbar.SWAP_INSTANCE.updateVisuals();
		}
		//Remember if the player removes the waterskin as one of their first actions.
		if (Statistics.duration + Actor.now() <= 10){
			boolean containsWaterskin = false;
			for (int i = 0; i < instance.length; i++) {
				if (select(i) instanceof Waterskin) containsWaterskin = true;
			}
			if (!containsWaterskin) SPDSettings.quickslotWaterskin(false);
		}
	}
	
	public static void target( Char target ) {
		if (target != null
				&& target.alignment != Char.Alignment.ALLY
				&& !Char.hasProp(target, Char.Property.OBJECT)) {
			lastTarget = target;
			
			TargetHealthIndicator.instance.target( target );
			InventoryPane.lastTarget = target;
		}
	}
	
	public static void cancel() {
		if (targetingSlot != -1) {
			for (QuickSlotButton btn : instance) {
				btn.crossB.visible = false;
				btn.crossM.remove();
				targetingSlot = -1;
			}
		}
	}
}

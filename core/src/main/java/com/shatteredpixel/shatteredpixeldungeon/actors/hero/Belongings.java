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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GenesisEcho;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.KindofMisc;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.BreakthroughCertificate;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Iterator;

public class Belongings implements Iterable<Item> {

	private Hero owner;

	public static class Backpack extends Bag {
		{
			image = ItemSpriteSheet.BACKPACK;
		}
		public int capacity(){
			// Restore the normal 20-slot baseline. Once Genesis Echo awakens,
			// "Infinite Space" guarantees at least five empty backpack slots no
			// matter how many items are already stored.
			int cap = super.capacity();
			Hero hero = owner instanceof Hero ? (Hero)owner : Dungeon.hero;
			for (Item item : items){
				if (item instanceof Bag){
					cap++;
				}
			}
			if (hero != null && hero.belongings.secondWep != null){
				//secondary weapons still occupy an inv. slot
				cap--;
			}
			if (GenesisEcho.active(hero)) {
				cap = Math.max(cap, items.size() + 5);
			}
			return cap;
		}
	}

	public Backpack backpack;
	
	public Belongings( Hero owner ) {
		this.owner = owner;
		
		backpack = new Backpack();
		backpack.owner = owner;
	}

	public KindOfWeapon weapon = null;
	public Armor armor = null;
	public Artifact artifact = null;
	public BreakthroughCertificate breakthroughCertificate = null;
	public KindofMisc misc = null;
	public Ring ring = null;

	// 纵横八荒 stacked equipment. These are deliberately separate from the
	// classic slots so old saves and vanilla combat code remain compatible.
	// The classic slot stays the primary/representative item; additional items
	// are truly equipped and remain owned by the Hero without occupying backpack
	// cells.
	private final ArrayList<KindOfWeapon> stackedWeapons = new ArrayList<>();
	private final ArrayList<Armor> stackedArmors = new ArrayList<>();
	private final ArrayList<Wand> stackedWands = new ArrayList<>();
	private final ArrayList<Artifact> stackedArtifacts = new ArrayList<>();
	private final ArrayList<Ring> stackedRings = new ArrayList<>();

	//used when thrown weapons temporary become the current weapon
	public KindOfWeapon thrownWeapon = null;

	//used to ensure that the duelist always uses the weapon she's using the ability of
	public KindOfWeapon abilityWeapon = null;

	//used by the champion subclass
	public KindOfWeapon secondWep = null;

	//*** these accessor methods are so that worn items can be affected by various effects/debuffs
	// we still want to access the raw equipped items in cases where effects should be ignored though,
	// such as when equipping something, showing an interface, or dealing with items from a dead hero

	//normally the primary equipped weapon, but can also be a thrown weapon or an ability's weapon
	public KindOfWeapon attackingWeapon(){
		if (thrownWeapon != null) return thrownWeapon;
		if (abilityWeapon != null) return abilityWeapon;
		return weapon();
	}

	//we cache whether belongings are lost to avoid lots of calls to hero.buff(LostInventory.class)
	private boolean lostInvent;
	public void lostInventory( boolean val ){
		lostInvent = val;
	}

	public boolean lostInventory(){
		return lostInvent;
	}

	public KindOfWeapon weapon(){
		if (!lostInventory() || (weapon != null && weapon.keptThroughLostInventory())){
			return weapon;
		} else {
			return null;
		}
	}

	public Armor armor(){
		if (!lostInventory() || (armor != null && armor.keptThroughLostInventory())){
			return armor;
		} else {
			return null;
		}
	}

	public Artifact artifact(){
		if (!lostInventory() || (artifact != null && artifact.keptThroughLostInventory())){
			return artifact;
		} else {
			return null;
		}
	}

	public BreakthroughCertificate breakthroughCertificate(){
		if (!lostInventory() || (breakthroughCertificate != null && breakthroughCertificate.keptThroughLostInventory())){
			return breakthroughCertificate;
		} else {
			return null;
		}
	}

	public KindofMisc misc(){
		if (!lostInventory() || (misc != null && misc.keptThroughLostInventory())){
			return misc;
		} else {
			return null;
		}
	}

	public Ring ring(){
		if (!lostInventory() || (ring != null && ring.keptThroughLostInventory())){
			return ring;
		} else {
			return null;
		}
	}

	public KindOfWeapon secondWep(){
		if (!lostInventory() || (secondWep != null && secondWep.keptThroughLostInventory())){
			return secondWep;
		} else {
			return null;
		}
	}

	// ***
	
	private static final String WEAPON		= "weapon";
	private static final String ARMOR		= "armor";
	private static final String ARTIFACT   = "artifact";
	private static final String BREAKTHROUGH_CERT = "breakthrough_cert";
	private static final String MISC       = "misc";
	private static final String RING       = "ring";

	private static final String SECOND_WEP = "second_wep";
	private static final String STACKED_WEAPONS = "genesis_stacked_weapons";
	private static final String STACKED_ARMORS = "genesis_stacked_armors";
	private static final String STACKED_WANDS = "genesis_stacked_wands";
	private static final String STACKED_ARTIFACTS = "genesis_stacked_artifacts";
	private static final String STACKED_RINGS = "genesis_stacked_rings";

	public void storeInBundle( Bundle bundle ) {
		
		backpack.storeInBundle( bundle );
		
		bundle.put( WEAPON, weapon );
		bundle.put( ARMOR, armor );
		bundle.put( ARTIFACT, artifact );
		bundle.put( BREAKTHROUGH_CERT, breakthroughCertificate );
		bundle.put( MISC, misc );
		bundle.put( RING, ring );
		bundle.put( SECOND_WEP, secondWep );
		bundle.put( STACKED_WEAPONS, stackedWeapons );
		bundle.put( STACKED_ARMORS, stackedArmors );
		bundle.put( STACKED_WANDS, stackedWands );
		bundle.put( STACKED_ARTIFACTS, stackedArtifacts );
		bundle.put( STACKED_RINGS, stackedRings );
	}

	public static boolean bundleRestoring = false;
	
	public void restoreFromBundle( Bundle bundle ) {
		bundleRestoring = true;
		backpack.clear();
		backpack.restoreFromBundle( bundle );
		
		weapon = (KindOfWeapon) bundle.get(WEAPON);
		if (weapon() != null)       weapon().activate(owner);
		
		armor = (Armor)bundle.get( ARMOR );
		if (armor() != null)        armor().activate( owner );

		artifact = (Artifact) bundle.get(ARTIFACT);
		if (artifact() != null)     artifact().activate(owner);

		breakthroughCertificate = (BreakthroughCertificate) bundle.get(BREAKTHROUGH_CERT);
		if (breakthroughCertificate() != null) breakthroughCertificate().activate(owner);

		misc = (KindofMisc) bundle.get(MISC);
		if (misc() != null)         misc().activate( owner );

		ring = (Ring) bundle.get(RING);
		if (ring() != null)         ring().activate( owner );

		secondWep = (KindOfWeapon) bundle.get(SECOND_WEP);
		if (secondWep() != null)    secondWep().activate(owner);

		stackedWeapons.clear();
		stackedArmors.clear();
		stackedWands.clear();
		stackedArtifacts.clear();
		stackedRings.clear();

		if (bundle.contains(STACKED_WEAPONS)) {
			for (Bundlable item : bundle.getCollection(STACKED_WEAPONS)) {
				if (item instanceof KindOfWeapon) {
					KindOfWeapon weapon = (KindOfWeapon)item;
					stackedWeapons.add(weapon);
					weapon.activate(owner);
				}
			}
		}
		if (bundle.contains(STACKED_ARMORS)) {
			for (Bundlable item : bundle.getCollection(STACKED_ARMORS)) {
				if (item instanceof Armor) {
					Armor armor = (Armor)item;
					stackedArmors.add(armor);
					armor.activate(owner);
				}
			}
		}
		if (bundle.contains(STACKED_WANDS)) {
			for (Bundlable item : bundle.getCollection(STACKED_WANDS)) {
				if (item instanceof Wand) {
					Wand wand = (Wand)item;
					stackedWands.add(wand);
					wand.charge(owner);
				}
			}
		}
		if (bundle.contains(STACKED_ARTIFACTS)) {
			for (Bundlable item : bundle.getCollection(STACKED_ARTIFACTS)) {
				if (item instanceof Artifact) {
					Artifact artifact = (Artifact)item;
					stackedArtifacts.add(artifact);
					artifact.activate(owner);
				}
			}
		}
		if (bundle.contains(STACKED_RINGS)) {
			for (Bundlable item : bundle.getCollection(STACKED_RINGS)) {
				if (item instanceof Ring) {
					Ring ring = (Ring)item;
					stackedRings.add(ring);
					ring.activate(owner);
				}
			}
		}

		bundleRestoring = false;
	}

	public void clear(){
		backpack.clear();
		weapon = secondWep = null;
		armor = null;
		artifact = null;
		breakthroughCertificate = null;
		misc = null;
		ring = null;
		stackedWeapons.clear();
		stackedArmors.clear();
		stackedWands.clear();
		stackedArtifacts.clear();
		stackedRings.clear();
	}
	
	public boolean equipStackedWeapon(KindOfWeapon item) {
		if (item == null || isStackEquipped(item)) return false;
		if (weapon == null) weapon = item;
		else stackedWeapons.add(item);
		return true;
	}

	public boolean equipStackedArmor(Armor item) {
		if (item == null || isStackEquipped(item)) return false;
		if (armor == null) armor = item;
		else stackedArmors.add(item);
		return true;
	}

	public boolean equipStackedWand(Wand item) {
		if (item == null || isStackEquipped(item)) return false;
		stackedWands.add(item);
		return true;
	}

	public boolean equipStackedMisc(KindofMisc item) {
		if (item == null || isStackEquipped(item)) return false;
		if (item instanceof Artifact) {
			if (artifact == null) artifact = (Artifact)item;
			else stackedArtifacts.add((Artifact)item);
			return true;
		}
		if (item instanceof Ring) {
			if (ring == null) ring = (Ring)item;
			else stackedRings.add((Ring)item);
			return true;
		}
		return false;
	}

	public boolean removeStackedEquipment(Item item) {
		if (item == null) return false;
		if (weapon == item) {
			weapon = stackedWeapons.isEmpty() ? null : stackedWeapons.remove(0);
			return true;
		}
		if (secondWep == item) {
			secondWep = null;
			return true;
		}
		if (stackedWeapons.remove(item)) return true;
		if (armor == item) {
			armor = stackedArmors.isEmpty() ? null : stackedArmors.remove(0);
			return true;
		}
		if (stackedArmors.remove(item)) return true;
		if (stackedWands.remove(item)) return true;
		if (artifact == item) {
			artifact = stackedArtifacts.isEmpty() ? null : stackedArtifacts.remove(0);
			return true;
		}
		if (ring == item) {
			ring = stackedRings.isEmpty() ? null : stackedRings.remove(0);
			return true;
		}
		if (misc == item) {
			misc = null;
			return true;
		}
		if (stackedArtifacts.remove(item)) return true;
		return stackedRings.remove(item);
	}

	public boolean isStackEquipped(Item item) {
		if (item == null) return false;
		return item == weapon || item == secondWep || item == armor || item == artifact || item == misc || item == ring
				|| stackedWeapons.contains(item) || stackedArmors.contains(item) || stackedWands.contains(item)
				|| stackedArtifacts.contains(item) || stackedRings.contains(item);
	}

	public boolean isGenesisStackExtra(Item item) {
		return item != null && (stackedWeapons.contains(item)
				|| stackedArmors.contains(item)
				|| stackedWands.contains(item)
				|| stackedArtifacts.contains(item)
				|| stackedRings.contains(item));
	}

	/**
	 * Makes an already-equipped stacked item the representative/primary item for
	 * its category without unequipping anything. This is what the expandable
	 * stacked-equipment row uses when the player taps a specific item.
	 */
	public boolean makeStackPrimary(Item item) {
		if (item == null || !isStackEquipped(item)) return false;

		if (item instanceof KindOfWeapon) {
			KindOfWeapon selected = (KindOfWeapon)item;
			if (weapon == selected) return true;
			if (secondWep == selected) {
				KindOfWeapon old = weapon;
				weapon = selected;
				secondWep = old;
				return true;
			}
			int idx = stackedWeapons.indexOf(selected);
			if (idx >= 0) {
				KindOfWeapon old = weapon;
				weapon = selected;
				if (old == null) stackedWeapons.remove(idx);
				else stackedWeapons.set(idx, old);
				return true;
			}
		}

		if (item instanceof Wand) {
			Wand selected = (Wand)item;
			int idx = stackedWands.indexOf(selected);
			if (idx < 0) return false;
			if (idx > 0) {
				stackedWands.remove(idx);
				stackedWands.add(0, selected);
			}
			return true;
		}

		if (item instanceof Artifact) {
			Artifact selected = (Artifact)item;
			if (artifact == selected) return true;
			if (misc == selected) {
				Artifact old = artifact;
				artifact = selected;
				misc = old;
				return true;
			}
			int idx = stackedArtifacts.indexOf(selected);
			if (idx >= 0) {
				Artifact old = artifact;
				artifact = selected;
				if (old == null) stackedArtifacts.remove(idx);
				else stackedArtifacts.set(idx, old);
				return true;
			}
		}

		if (item instanceof Ring) {
			Ring selected = (Ring)item;
			if (ring == selected) return true;
			if (misc == selected) {
				Ring old = ring;
				ring = selected;
				misc = old;
				return true;
			}
			int idx = stackedRings.indexOf(selected);
			if (idx >= 0) {
				Ring old = ring;
				ring = selected;
				if (old == null) stackedRings.remove(idx);
				else stackedRings.set(idx, old);
				return true;
			}
		}

		return false;
	}

	public Artifact cyclePrimaryArtifact() {
		ArrayList<Artifact> items = equippedArtifacts();
		if (items.isEmpty()) return null;
		if (items.size() == 1) return items.get(0);

		// Rotate instead of swapping with just one item, otherwise 3+ artifacts
		// would bounce between the first two forever.
		Artifact oldPrimary = artifact;
		if (misc instanceof Artifact) {
			artifact = (Artifact)misc;
			if (!stackedArtifacts.isEmpty()) {
				misc = stackedArtifacts.remove(0);
				if (oldPrimary != null) stackedArtifacts.add(oldPrimary);
			} else {
				misc = oldPrimary;
			}
		} else if (!stackedArtifacts.isEmpty()) {
			artifact = stackedArtifacts.remove(0);
			if (oldPrimary != null) stackedArtifacts.add(oldPrimary);
		}
		return displayArtifact();
	}

	public ArrayList<KindOfWeapon> equippedWeapons() {
		ArrayList<KindOfWeapon> result = new ArrayList<>();
		if (weapon != null) result.add(weapon);
		if (secondWep != null && !result.contains(secondWep)) result.add(secondWep);
		for (KindOfWeapon item : stackedWeapons) if (item != null && !result.contains(item)) result.add(item);
		return result;
	}

	public ArrayList<Armor> equippedArmors() {
		ArrayList<Armor> result = new ArrayList<>();
		if (armor != null) result.add(armor);
		for (Armor item : stackedArmors) if (item != null && !result.contains(item)) result.add(item);
		return result;
	}

	public ArrayList<Wand> equippedWands() {
		return new ArrayList<>(stackedWands);
	}

	public ArrayList<Artifact> equippedArtifacts() {
		ArrayList<Artifact> result = new ArrayList<>();
		if (artifact != null) result.add(artifact);
		if (misc instanceof Artifact && !result.contains(misc)) result.add((Artifact)misc);
		for (Artifact item : stackedArtifacts) if (item != null && !result.contains(item)) result.add(item);
		return result;
	}

	public ArrayList<Ring> equippedRings() {
		ArrayList<Ring> result = new ArrayList<>();
		if (ring != null) result.add(ring);
		if (misc instanceof Ring && !result.contains(misc)) result.add((Ring)misc);
		for (Ring item : stackedRings) if (item != null && !result.contains(item)) result.add(item);
		return result;
	}

	public KindOfWeapon displayWeapon() {
		ArrayList<KindOfWeapon> items = equippedWeapons();
		return items.isEmpty() ? null : items.get(0);
	}

	public Armor displayArmor() {
		ArrayList<Armor> items = equippedArmors();
		return items.isEmpty() ? null : items.get(0);
	}

	public Wand displayWand() {
		return stackedWands.isEmpty() ? null : stackedWands.get(0);
	}

	public Artifact displayArtifact() {
		ArrayList<Artifact> items = equippedArtifacts();
		return items.isEmpty() ? null : items.get(0);
	}

	public Ring displayRing() {
		ArrayList<Ring> items = equippedRings();
		return items.isEmpty() ? null : items.get(0);
	}

	public static void preview( GamesInProgress.Info info, Bundle bundle ) {
		if (bundle.contains( ARMOR )){
			Armor armor = ((Armor)bundle.get( ARMOR ));
			if (armor instanceof ClassArmor){
				info.armorTier = 6;
			} else {
				info.armorTier = armor.tier;
			}
		} else {
			info.armorTier = 0;
		}
	}

	//ignores lost inventory debuff
	public ArrayList<Bag> getBags(){
		ArrayList<Bag> result = new ArrayList<>();

		result.add(backpack);

		for (Item i : this){
			if (i instanceof Bag){
				result.add((Bag)i);
			}
		}

		return result;
	}
	
	@SuppressWarnings("unchecked")
	public<T extends Item> T getItem( Class<T> itemClass ) {

		boolean lostInvent = lostInventory();

		for (Item item : this) {
			if (itemClass.isInstance( item )) {
				if (!lostInvent || item.keptThroughLostInventory()) {
					return (T) item;
				}
			}
		}
		
		return null;
	}

	public<T extends Item> ArrayList<T> getAllItems( Class<T> itemClass ) {
		ArrayList<T> result = new ArrayList<>();

		boolean lostInvent = lostInventory();

		for (Item item : this) {
			if (itemClass.isInstance( item )) {
				if (!lostInvent || item.keptThroughLostInventory()) {
					result.add((T) item);
				}
			}
		}

		return result;
	}
	
	public boolean contains( Item contains ){

		boolean lostInvent = lostInventory();
		
		for (Item item : this) {
			if (contains == item) {
				if (!lostInvent || item.keptThroughLostInventory()) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public Item getSimilar( Item similar ){

		boolean lostInvent = lostInventory();
		
		for (Item item : this) {
			if (similar != item && similar.isSimilar(item)) {
				if (!lostInvent || item.keptThroughLostInventory()) {
					return item;
				}
			}
		}
		
		return null;
	}
	
	public ArrayList<Item> getAllSimilar( Item similar ){
		ArrayList<Item> result = new ArrayList<>();

		boolean lostInvent = lostInventory();
		
		for (Item item : this) {
			if (item != similar && similar.isSimilar(item)) {
				if (!lostInvent || item.keptThroughLostInventory()) {
					result.add(item);
				}
			}
		}
		
		return result;
	}

	//triggers when a run ends, so ignores lost inventory effects
	public void identify() {
		for (Item item : this) {
			item.identify(false);
		}
	}
	
	public void observe() {
		if (weapon() != null) {
			if (ShardOfOblivion.passiveIDDisabled() && weapon() instanceof Weapon){
				((Weapon) weapon()).setIDReady();
			} else {
				weapon().identify();
				Badges.validateItemLevelAquired(weapon());
			}
		}
		if (secondWep() != null){
			if (ShardOfOblivion.passiveIDDisabled() && secondWep() instanceof Weapon){
				((Weapon) secondWep()).setIDReady();
			} else {
				secondWep().identify();
				Badges.validateItemLevelAquired(secondWep());
			}
		}
		if (armor() != null) {
			if (ShardOfOblivion.passiveIDDisabled()){
				armor().setIDReady();
			} else {
				armor().identify();
				Badges.validateItemLevelAquired(armor());
			}
		}
		if (artifact() != null) {
			//oblivion shard does not prevent artifact IDing
			artifact().identify();
			Badges.validateItemLevelAquired(artifact());
		}
		if (misc() != null) {
			if (ShardOfOblivion.passiveIDDisabled() && misc() instanceof Ring){
				((Ring) misc()).setIDReady();
			} else {
				misc().identify();
				Badges.validateItemLevelAquired(misc());
			}
		}
		if (ring() != null) {
			if (ShardOfOblivion.passiveIDDisabled()){
				ring().setIDReady();
			} else {
				ring().identify();
				Badges.validateItemLevelAquired(ring());
			}
		}
		if (ShardOfOblivion.passiveIDDisabled()){
			GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready_worn"));
		}
		for (Item item : backpack) {
			if (item instanceof EquipableItem || item instanceof Wand) {
				item.cursedKnown = true;
			}
		}
		Item.updateQuickslot();
	}
	
	public void uncurseEquipped() {
		ScrollOfRemoveCurse.uncurse( owner, armor(), weapon(), artifact(), misc(), ring(), secondWep());
	}
	
	public Item randomUnequipped() {
		if (owner.buff(LostInventory.class) != null) return null;

		return Random.element( backpack.items );
	}
	
	public int charge( float charge ) {
		
		int count = 0;
		
		for (Wand.Charger charger : owner.buffs(Wand.Charger.class)){
			charger.gainCharge(charge);
			count++;
		}
		
		return count;
	}

	@Override
	public Iterator<Item> iterator() {
		return new ItemIterator();
	}
	
	private class ItemIterator implements Iterator<Item> {

		private final ArrayList<Item> equippedItems = new ArrayList<>();
		private final Iterator<Item> backpackIterator = backpack.iterator();
		private int index = 0;
		private Item lastItem = null;
		private boolean lastFromBackpack = false;

		ItemIterator() {
			addEquipped(weapon);
			addEquipped(armor);
			addEquipped(artifact);
			addEquipped(breakthroughCertificate);
			addEquipped(misc);
			addEquipped(ring);
			addEquipped(secondWep);
			for (KindOfWeapon item : stackedWeapons) addEquipped(item);
			for (Armor item : stackedArmors) addEquipped(item);
			for (Wand item : stackedWands) addEquipped(item);
			for (Artifact item : stackedArtifacts) addEquipped(item);
			for (Ring item : stackedRings) addEquipped(item);
		}

		private void addEquipped(Item item) {
			if (item != null && !equippedItems.contains(item)) equippedItems.add(item);
		}

		@Override
		public boolean hasNext() {
			return index < equippedItems.size() || backpackIterator.hasNext();
		}

		@Override
		public Item next() {
			if (index < equippedItems.size()) {
				lastFromBackpack = false;
				lastItem = equippedItems.get(index++);
				return lastItem;
			}
			lastFromBackpack = true;
			lastItem = backpackIterator.next();
			return lastItem;
		}

		@Override
		public void remove() {
			if (lastItem == null) return;
			if (lastFromBackpack) {
				backpackIterator.remove();
			} else {
				removeStackedEquipment(lastItem);
				if (lastItem == armor) armor = null;
				if (lastItem == breakthroughCertificate) breakthroughCertificate = null;
			}
			lastItem = null;
		}
	}
}

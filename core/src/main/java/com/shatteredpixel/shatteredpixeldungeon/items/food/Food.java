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

package com.shatteredpixel.shatteredpixeldungeon.items.food;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class Food extends Item {

	public static final float TIME_TO_EAT	= 3f;
	
	public static final String AC_EAT	= "EAT";
	
	public float energy = Hunger.HUNGRY;

	private static final String STARTER_UNKNOWN = "starter_unknown";
	private static final String STARTER_REAL_IMAGE = "starter_real_image";
	private boolean starterUnknown = false;
	private int starterRealImage = -1;
	
	{
		stackable = true;
		image = ItemSpriteSheet.RATION;

		defaultAction = AC_EAT;

		bones = true;
	}
	
	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_EAT );
		return actions;
	}
	
	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_EAT )) {

			if (starterUnknown) identify();
			
			detach( hero.belongings.backpack );
			Catalog.countUse(getClass());
			
			satisfy(hero);
			GLog.i( Messages.get(this, "eat_msg") );
			
			hero.sprite.operate( hero.pos );
			hero.busy();
			SpellSprite.show( hero, SpellSprite.FOOD );
			eatSFX();
			
			hero.spend( eatingTime() );

			Talent.onFoodEaten(hero, energy, this);
			
			Statistics.foodEaten++;
			Badges.validateFoodEaten();
			
		}
	}

	protected void eatSFX(){
		Sample.INSTANCE.play( Assets.Sounds.EAT );
	}

	protected float eatingTime(){
		if (Dungeon.hero.hasTalent(Talent.IRON_STOMACH)
			|| Dungeon.hero.hasTalent(Talent.ENERGIZING_MEAL)
			|| Dungeon.hero.hasTalent(Talent.MYSTICAL_MEAL)
			|| Dungeon.hero.hasTalent(Talent.INVIGORATING_MEAL)
			|| Dungeon.hero.hasTalent(Talent.FOCUSED_MEAL)
			|| Dungeon.hero.hasTalent(Talent.ENLIGHTENING_MEAL)){
			return TIME_TO_EAT - 2;
		} else {
			return TIME_TO_EAT;
		}
	}
	
	protected void satisfy( Hero hero ){
		float foodVal = energy;
		if (Dungeon.isChallenged(Challenges.NO_FOOD)){
			foodVal /= 3f;
		}

		Artifact.ArtifactBuff buff = hero.buff( HornOfPlenty.hornRecharge.class );
		if (buff != null && buff.isCursed()){
			foodVal *= 0.67f;
			GLog.n( Messages.get(Hunger.class, "cursedhorn") );
		}

		Buff.affect(hero, Hunger.class).satisfy(foodVal);
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	public Food markStarterUnknown() {
		if (!starterUnknown) {
			starterUnknown = true;
			starterRealImage = image;
		}
		image = ItemSpriteSheet.RATION;
		levelKnown = false;
		cursedKnown = false;
		return this;
	}

	@Override
	public boolean isIdentified() {
		return !starterUnknown;
	}

	@Override
	public Item identify(boolean byHero) {
		if (starterUnknown) {
			starterUnknown = false;
			if (starterRealImage >= 0) image = starterRealImage;
		}
		return super.identify(byHero);
	}

	@Override
	public String name() {
		return starterUnknown ? Messages.get(Food.class, "starter_unknown_name") : super.name();
	}

	@Override
	public String desc() {
		return starterUnknown ? Messages.get(Food.class, "starter_unknown_desc") : super.desc();
	}

	@Override
	public boolean isSimilar(Item item) {
		if (item instanceof Food && starterUnknown != ((Food)item).starterUnknown) return false;
		return super.isSimilar(item);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STARTER_UNKNOWN, starterUnknown);
		bundle.put(STARTER_REAL_IMAGE, starterRealImage);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		starterUnknown = bundle.contains(STARTER_UNKNOWN) && bundle.getBoolean(STARTER_UNKNOWN);
		starterRealImage = bundle.contains(STARTER_REAL_IMAGE) ? bundle.getInt(STARTER_REAL_IMAGE) : -1;
		if (starterUnknown) image = ItemSpriteSheet.RATION;
	}

	@Override
	public int value() {
		return 10 * quantity;
	}
}

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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GenesisEcho;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindofMisc;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldProgression;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Artifact extends KindofMisc {

	protected Buff passiveBuff;
	protected Buff activeBuff;

	//level is used internally to track upgrades to artifacts, size/logic varies per artifact.
	//already inherited from item superclass
	//exp is used to count progress towards levels for some artifacts
	protected int exp = 0;
	//levelCap is the artifact's maximum level
	protected int levelCap = 0;
	// Infinite World post-breakthrough overlevel. Native artifact internals still
	// use their original caps; this extension supplies extra effective power and
	// visible levels 11..30 without rewriting every artifact subclass.
	protected int assistVisibleOverlevel = 0;
	protected float assistOverlevelProgress = 0f;

	//the current artifact charge
	protected int charge = 0;
	//the build towards next charge, usually rolls over at 1.
	//better to keep charge as an int and use a separate float than casting.
	protected float partialCharge = 0;
	//the maximum charge, varies per artifact, not all artifacts use this.
	protected int chargeCap = 0;

	//used by some artifacts to keep track of duration of effects or cooldowns to use.
	protected int cooldown = 0;

	@Override
	public boolean doEquip( final Hero hero ) {

		if (!GenesisEcho.unrestrictedEquipment(hero)
				&& ((hero.belongings.artifact != null && hero.belongings.artifact.getClass() == this.getClass())
				|| (hero.belongings.misc != null && hero.belongings.misc.getClass() == this.getClass()))){

			GLog.w( Messages.get(Artifact.class, "cannot_wear_two") );
			return false;

		} else {

			if (super.doEquip( hero )){

				identify();
				return true;

			} else {

				return false;

			}

		}

	}

	public void activate( Char ch ) {
		if (passiveBuff != null){
			if (passiveBuff.target != null) passiveBuff.detach();
			passiveBuff = null;
		}
		passiveBuff = passiveBuff();
		if (passiveBuff != null) passiveBuff.attachTo(ch);
	}

	/**
	 * 纵横八荒 can keep multiple artifacts equipped outside the two vanilla misc
	 * slots. Every equipped artifact must keep its own passive ArtifactBuff in
	 * both the Hero buff set and the Actor scheduler. If a save/load, slot swap,
	 * or older stacked-equipment build lost that runtime actor, rebuild only that
	 * artifact's own passive buff; its charge/cooldown fields stay untouched.
	 */
	public boolean ensurePassiveRuntime(Hero hero) {
		if (hero == null || !isEquipped(hero)) return false;

		// Repair invalid recharge state written by earlier Assist overlevel builds.
		// In particular, Ethereal Chains/Cloak/Tome could accumulate a negative
		// partialCharge when an overlevel made a native recharge denominator
		// negative. Without this migration an old save would remain "stuck" even
		// after the formula itself was fixed.
		if (Float.isNaN(partialCharge) || Float.isInfinite(partialCharge) || partialCharge < 0f) {
			partialCharge = 0f;
		}
		if (charge < 0) charge = 0;
		if (chargeCap > 0 && charge > chargeCap) charge = chargeCap;
		if (cooldown < 0) cooldown = 0;

		ArtifactBuff expected = passiveBuff instanceof ArtifactBuff ? (ArtifactBuff)passiveBuff : null;
		boolean attached = expected != null
				&& expected.target == hero
				&& hero.buffs().contains(expected)
				&& Actor.contains(expected);

		if (attached) return true;

		// A restored save may already contain this exact artifact's passive buff
		// while the item field itself has not been rebound yet. Reuse it instead of
		// spawning a duplicate runtime.
		for (ArtifactBuff existing : hero.buffs(ArtifactBuff.class)) {
			if (existing.artifact() == this) {
				passiveBuff = existing;
				if (Actor.chars().contains(hero) && !Actor.contains(existing)) {
					Actor.add(existing);
				}
				return true;
			}
		}

		if (expected != null && expected.target != null) {
			expected.detach();
		}
		passiveBuff = passiveBuff();
		if (passiveBuff == null) return true;

		boolean ok = passiveBuff.attachTo(hero);
		// attachTo normally schedules immediately if the Hero is already an Actor.
		// Keep a defensive scheduler check for old saves restored mid-scene.
		if (ok && Actor.chars().contains(hero) && !Actor.contains(passiveBuff)) {
			Actor.add(passiveBuff);
		}
		return ok;
	}

	/**
	 * Per-equipped-artifact experience hook. Subclasses whose native recharge
	 * depends on hero XP override this; time/event based artifacts do nothing.
	 */
	public void onEquippedHeroGainExp(float levelPercent, Hero hero) {
		// no-op by default
	}

	/**
	 * Converts fractional artifact charge into whole charge in O(1).
	 *
	 * Assist permits very large stacked Ring of Energy bonuses and custom
	 * equipment levels. Repeatedly subtracting 1 from partialCharge can therefore
	 * take millions of iterations, and Float.POSITIVE_INFINITY would never
	 * decrease at all. This helper preserves the same whole/fractional result
	 * without a value-proportional loop and saturates before int overflow.
	 */
	protected final int addUncappedChargeProgress(float gain) {
		if (Float.isNaN(partialCharge) || partialCharge < 0f) partialCharge = 0f;
		if (Float.isNaN(gain) || gain <= 0f) return 0;

		double total = (double)partialCharge + (double)gain;
		long room = (long)Integer.MAX_VALUE - Math.max(0, charge);
		if (!Double.isFinite(total) || total >= room) {
			int before = charge;
			charge = Integer.MAX_VALUE;
			partialCharge = 0f;
			return Math.max(0, charge - before);
		}

		int whole = (int)Math.floor(total);
		if (whole <= 0) {
			partialCharge = (float)total;
			return 0;
		}

		charge += whole;
		partialCharge = (float)(total - whole);
		return whole;
	}

	@Override
	public boolean doUnequip( Hero hero, boolean collect, boolean single ) {
		if (super.doUnequip( hero, collect, single )) {

			if (passiveBuff != null) {
				if (passiveBuff.target != null) passiveBuff.detach();
				passiveBuff = null;
			}

			return true;

		} else {

			return false;

		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public int level() {
		int base = super.level();
		if (Dungeon.infiniteWorld && levelCap > 0 && assistVisibleOverlevel > 0) {
			base += Math.round((assistVisibleOverlevel * levelCap) / 10f);
		}
		return base;
	}

	/**
	 * Native artifact level, clamped to the original class cap. Assist visible
	 * overlevels (+11..+30) may safely improve high-level power where subclasses
	 * explicitly use level(), but native recharge denominators/state-machine
	 * thresholds must never receive an out-of-range level.
	 */
	public int nativeLevel() {
		int base = super.level();
		if (levelCap > 0) base = Math.min(levelCap, base);
		return Math.max(0, base);
	}

	@Override
	public int visiblyUpgraded() {
		if (!levelKnown || levelCap <= 0) return 0;
		return Math.round((super.level()*10)/(float)levelCap) + assistVisibleOverlevel;
	}

	@Override
	public int buffedVisiblyUpgraded() {
		return visiblyUpgraded();
	}

	@Override
	public int buffedLvl() {
		return level();
	}

	// Transfers a displayed artifact level without ever pushing the native
	// subclass progression past its original cap. +11..+30 lives exclusively in
	// Assist overlevel state so artifact-specific level state machines stay valid.
	public void transferUpgrade(int transferLvl) {
		if (levelCap <= 0) return;
		int visible = Math.max(0, transferLvl);
		int naturalVisible = Math.min(10, visible);
		int targetNative = Math.min(levelCap, Math.round((naturalVisible*levelCap)/10f));
		int delta = Math.max(0, targetNative - super.level());
		if (delta > 0) upgrade(delta);
		assistVisibleOverlevel = Math.max(0, visible - 10);
		updateQuickslot();
	}

	// Assist edition: pre-breakthrough artifacts cap at +10; after the level-30
	// trial the visible cap becomes +30. Levels above +10 are kept as a separate
	// overlevel so native artifact-specific progression is not corrupted.
	public int assistBoostVisibleLevel(int amount) {
		if (levelCap <= 0 || amount <= 0) return visiblyUpgraded();

		int currentVisible = visiblyUpgraded();
		int targetVisible = Math.min(InfiniteWorldProgression.artifactUpgradeCap(),
				currentVisible + amount);
		int naturalTarget = Math.min(10, targetVisible);
		int targetInternal = Math.min(levelCap, Math.round((naturalTarget*levelCap)/10f));

		int guard = 0;
		while (super.level() < targetInternal && guard++ < 64) {
			int before = super.level();
			upgrade();
			if (super.level() <= before) break;
		}

		assistVisibleOverlevel = Math.max(0, targetVisible - 10);
		updateQuickslot();
		return visiblyUpgraded();
	}

	@Override
	public void onHeroGainExp(float levelPercent, Hero hero) {
		super.onHeroGainExp(levelPercent, hero);

		// Once a native artifact reaches its original +10-equivalent cap, a
		// post-breakthrough Infinite World Hero can continue training the equipped
		// artifact toward +30. Roughly 1.5 Hero-levels of experience grants one
		// extra visible artifact level, so +30 remains a long-run goal rather than
		// an instant jump.
		if (!Dungeon.infiniteWorld || hero == null || levelPercent <= 0f
				|| !InfiniteWorldProgression.breakthroughCompleted()
				|| levelCap <= 0 || !isEquipped(hero)
				|| Math.round((super.level()*10)/(float)levelCap) < 10
				|| visiblyUpgraded() >= InfiniteWorldProgression.POST_BREAKTHROUGH_ARTIFACT_CAP) {
			return;
		}

		assistOverlevelProgress += levelPercent;
		while (assistOverlevelProgress >= 1.5f
				&& visiblyUpgraded() < InfiniteWorldProgression.POST_BREAKTHROUGH_ARTIFACT_CAP) {
			assistOverlevelProgress -= 1.5f;
			assistVisibleOverlevel++;
			updateQuickslot();
			GLog.p("神器成长：" + name() + " 提升至 +" + visiblyUpgraded());
		}
	}

	public void resetForTrinity(int visibleLevel){
		int clampedVisible = Math.max(0, visibleLevel);
		int naturalVisible = Math.min(10, clampedVisible);
		level(Math.min(levelCap, Math.round((naturalVisible*levelCap)/10f)));
		assistVisibleOverlevel = Math.max(0, clampedVisible - 10);
		exp = Integer.MIN_VALUE; //ensures no native levelling
		charge = chargeCap;
		cooldown = 0;
	}

	public static void artifactProc(Char target, int artifLevel, int chargesUsed){
		if (Dungeon.hero.subClass == HeroSubClass.PRIEST && target.buff(GuidingLight.Illuminated.class) != null) {
			target.buff(GuidingLight.Illuminated.class).detach();
			target.damage(5+Dungeon.hero.lvl, GuidingLight.INSTANCE);
		}

		if (target.alignment != Char.Alignment.ALLY
				&& Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.SEARING_LIGHT)
				&& Dungeon.hero.buff(Talent.SearingLightCooldown.class) == null){
			Buff.affect(target, GuidingLight.Illuminated.class);
			Buff.affect(Dungeon.hero, Talent.SearingLightCooldown.class, 20f);
		}

		if (target.alignment != Char.Alignment.ALLY
				&& Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.SUNRAY)){
			// 15/25% chance
			if (Random.Int(20) < 1 + 2*Dungeon.hero.pointsInTalent(Talent.SUNRAY)){
				Buff.prolong(target, Blindness.class, 4f);
			}
		}
	}

	@Override
	public String info() {
		if (cursed && cursedKnown && !isEquipped( Dungeon.hero )) {
			return super.info() + "\n\n" + Messages.get(Artifact.class, "curse_known");
			
		} else if (!isIdentified() && cursedKnown && !isEquipped( Dungeon.hero)) {
			return super.info() + "\n\n" + Messages.get(Artifact.class, "not_cursed");
			
		} else {
			String info = super.info();
			if (Dungeon.infiniteWorld && visiblyUpgraded() > 10) {
				int extra = visiblyUpgraded() - 10;
				int rechargeBonus = Math.round((assistOverlevelChargeMultiplier() - 1f) * 100f);
				info += "\n\n" + Messages.get(Artifact.class, "assist_overlevel_power",
						visiblyUpgraded(), buffedLvl(), rechargeBonus);
			}
			return info;
			
		}
	}

	@Override
	public String status() {
		
		//if the artifact isn't IDed, or is cursed, don't display anything
		if (!isIdentified() || cursed){
			return null;
		}

		//display the current cooldown
		if (cooldown != 0)
			return Messages.format( "%d", cooldown );

		//display as percent
		if (chargeCap == 100)
			return Messages.format( "%d%%", charge );

		//display as #/#
		if (chargeCap > 0)
			return Messages.format( "%d/%d", charge, chargeCap );

		//if there's no cap -
		//- but there is charge anyway, display that charge
		if (charge != 0)
			return Messages.format( "%d", charge );

		//otherwise, if there's no charge, return null.
		return null;
	}

	@Override
	public Item random() {
		//always +0
		
		//30% chance to be cursed
		if (Random.Float() < 0.3f) {
			cursed = true;
		}
		return this;
	}

	@Override
	public int value() {
		int price = 100;
		if (level() > 0)
			price += 20*visiblyUpgraded();
		if (cursed && cursedKnown) {
			price /= 2;
		}
		if (price < 1) {
			price = 1;
		}
		return price;
	}


	protected ArtifactBuff passiveBuff() {
		return null;
	}

	protected ArtifactBuff activeBuff() {return null; }

	public float assistOverlevelChargeMultiplier(){
		if (!Dungeon.infiniteWorld || visiblyUpgraded() <= 10) return 1f;
		return 1f + 0.025f * (visiblyUpgraded() - 10);
	}
	
	public void charge(Hero target, float amount){
		//do nothing by default;
	}

	public class ArtifactBuff extends Buff {

		public Artifact artifact() {
			return Artifact.this;
		}

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo( target )) {
				//if we're loading in and the hero has partially spent a turn, delay for 1 turn
				if (target instanceof Hero && Dungeon.hero == null && cooldown() == 0 && target.cooldown() > 0) {
					spend(TICK);
				}
				return true;
			}
			return false;
		}

		public int itemLevel() {
			return Artifact.this.buffedLvl();
		}

		public boolean isCursed() {
			return target.buff(MagicImmune.class) == null && cursed;
		}

		public void charge(Hero target, float amount){
			Artifact.this.charge(target, amount * Artifact.this.assistOverlevelChargeMultiplier());
		}

	}
	
	private static final String EXP = "exp";
	private static final String CHARGE = "charge";
	private static final String PARTIALCHARGE = "partialcharge";
	private static final String ASSIST_OVERLEVEL = "assist_overlevel";
	private static final String ASSIST_OVERLEVEL_PROGRESS = "assist_overlevel_progress";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put( EXP , exp );
		bundle.put( CHARGE , charge );
		bundle.put( PARTIALCHARGE , partialCharge );
		bundle.put( ASSIST_OVERLEVEL, assistVisibleOverlevel );
		bundle.put( ASSIST_OVERLEVEL_PROGRESS, assistOverlevelProgress );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		exp = bundle.getInt( EXP );
		if (chargeCap > 0)  charge = Math.min( chargeCap, bundle.getInt( CHARGE ));
		else                charge = bundle.getInt( CHARGE );
		partialCharge = bundle.getFloat( PARTIALCHARGE );
		assistVisibleOverlevel = bundle.contains(ASSIST_OVERLEVEL)
				? bundle.getInt(ASSIST_OVERLEVEL) : 0;
		assistOverlevelProgress = bundle.contains(ASSIST_OVERLEVEL_PROGRESS)
				? bundle.getFloat(ASSIST_OVERLEVEL_PROGRESS) : 0f;
	}
}

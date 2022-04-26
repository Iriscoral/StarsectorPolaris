package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.DerivedWeaponStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.PLSPModPlugin;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PLSP_StarlightAdapter extends BaseHullMod {
	
	private static String getString(String key) {
		return Global.getSettings().getString("HullMod", "PLSP_" + key);
	}
	
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return !isForModSpec;
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getFluxCapacity().modifyFlat(id, -stats.getVariant().getNumFluxCapacitors() * 100f);
		stats.getFluxDissipation().modifyFlat(id, stats.getVariant().getNumFluxCapacitors() * 5f);
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		if (ship == null) {
			return;
		}

		ShipVariantAPI variant = ship.getVariant();
		if (variant == null) {
			return;
		}

		float pad = 10f;
		float padS = 2f;
		float bonus = getRangeBonus(ship);

		tooltip.addPara("%s " + getString("starlightadapterTEXT1"), pad, Misc.getHighlightColor(), "#", "1", "+1%");
		tooltip.addPara("%s " + getString("starlightadapterTEXT2"), padS, Misc.getHighlightColor(), "#", "30%");
		tooltip.addPara("%s " + getString("starlightadapterTEXT3"), padS, Misc.getPositiveHighlightColor(), "#", "+" + (int)bonus + "%");
		tooltip.addPara("%s " + getString("starlightadapterTEXT4"), pad, Misc.getHighlightColor(), "#", "50%");
	}
	
	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive()) {
			return;
		}

		MutableShipStatsAPI stats = ship.getMutableStats();
		String id = "PLSP_StarlightAdapter";
		ShipVariantAPI variant = ship.getVariant();

		boolean applyRequired = !stats.getBallisticWeaponRangeBonus().getPercentBonuses().containsKey(id) || !stats.getEnergyWeaponRangeBonus().getPercentBonuses().containsKey(id);
		if (variant != null && applyRequired) {
			float bonus = getRangeBonus(ship);
			stats.getBallisticWeaponRangeBonus().modifyPercent(id, bonus);
			stats.getEnergyWeaponRangeBonus().modifyPercent(id, bonus);
		}

		FluxTrackerAPI flux = ship.getFluxTracker();
		if (flux.isOverloaded()) {

		} else {
			if (flux.isVenting()) {
				int alphaLevel = (int)(flux.getFluxLevel() * 200f);
				ship.setJitter(ship, new Color(40, 60, 80, alphaLevel), 1f, 10, 0f, 12f);
			} else {
				if (PLSPModPlugin.modifiedVentingAI && ship.getShipAI() != null && Math.random() < 0.02) {
					//if (true) {
					AIPlus(ship);
				}
			}
		}
	}

	//Originally by DR
	private static float getArmorLevel(ShipAPI ship) {
		if (ship == null || !Global.getCombatEngine().isEntityInPlay(ship)) {
			return 0f;
		}

		float current = 0f;
		float total = 0f;
		float worst = 1f;
		ArmorGridAPI armorGrid = ship.getArmorGrid();
		for (int x = 0; x < armorGrid.getGrid().length; x++) {
			for (int y = 0; y < armorGrid.getGrid()[x].length; y++) {
				float fraction = armorGrid.getArmorFraction(x, y);
				current += fraction;
				total += 1f;
				if (fraction < worst) {
					worst = fraction;
				}
			}
		}

		return (current / total) * (float) Math.sqrt(worst * 0.75f + 0.25f);
	}

	//Originally by DR, modified by AnyIDElse
	private static boolean isInBurst(ShipAPI ship) {
		int burstLevel = 0;
		for (WeaponAPI weapon : ship.getUsableWeapons()) {
			if (((!weapon.isBeam() && weapon.getDerivedStats().getBurstFireDuration() > 0f && weapon.getDerivedStats().getBurstFireDuration() <= 15f)
					|| weapon.isBurstBeam()) && (weapon.getChargeLevel() >= 0.75f || weapon.isFiring()) && weapon.getCooldownRemaining() <= 0.25f) {
				switch (weapon.getSize()) {
					case LARGE:
						return true;
					case MEDIUM:
						burstLevel += 3;
						break;
					case SMALL:
						burstLevel ++;
						break;
					default:
						break;
				}
			}
		}

		switch (ship.getHullSize()) {
			case CAPITAL_SHIP:
				return burstLevel >= 9;
			case CRUISER:
				return burstLevel >= 5;
			case DESTROYER:
				return burstLevel >= 3;
			case FRIGATE:
				return burstLevel > 0;
			default:
				return false;
		}
	}

	//Originally by DR, modified by AnyIDElse
	private static final float DAMAGE_FACTOR = 0.02f;
	private static final float DAMAGE_POWER = 1.25f;
	private static final float EMP_FACTOR = 0.67f;
	private static float getThreatLevel(ShipAPI ship) {

		FluxTrackerAPI shipFlux = ship.getFluxTracker();
		MutableShipStatsAPI stats = ship.getMutableStats();
		// do not consider self's time mult

		float shipVentRate = stats.getFluxDissipation().getModifiedValue() * 2f * stats.getVentRateMult().getModifiedValue();
		float shipTTV = 20f;
		if (shipVentRate > 0f) {
			shipTTV = shipFlux.getCurrFlux() / shipVentRate;
		}

		float range = ship.getCollisionRadius() * 15f;

		List<DamagingProjectileAPI> nearbyThreats = new ArrayList<>();
		for (DamagingProjectileAPI tmp : PLSP_Util.getEnemyProjectilesAndMissilesWithinRange(ship.getLocation(), range, ship.getOwner())) {
			if (tmp instanceof MissileAPI) {
				MissileAPI missile = (MissileAPI)tmp;
				if (missile.isFizzling() || missile.isFlare()) continue;
				if (missile.getMissileAI() != null &&
						missile.getMissileAI().getClass().getSimpleName().contentEquals("RocketAI") &&
						!PLSP_Util.willProjectileHitShipWithInSec(tmp, ship, (int)shipTTV + 2)) continue;
				nearbyThreats.add(missile);
			} else {
				if (PLSP_Util.willProjectileHitShipWithInSec(tmp, ship, (int)shipTTV + 2)) {
					nearbyThreats.add(tmp);
				}
			}
		}

		FleetMemberAPI member = ship.getFleetMember();
		float shipStrength = member != null ? 0.1f + member.getFleetPointCost() : 1f;
		float armorLevel = getArmorLevel(ship);
		float empCap = EMP_FACTOR * stats.getEmpDamageTakenMult().modified;
		float damageCap = DAMAGE_FACTOR
				* (armorLevel * stats.getArmorDamageTakenMult().modified
				+ (1f - armorLevel) * stats.getHullDamageTakenMult().modified);

		float threatLevel = 0f;
		for (DamagingProjectileAPI threat : nearbyThreats) {
			float damage = threat.getDamageAmount() + threat.getEmpAmount() * empCap;
			damage /= (float) Math.sqrt(Math.max((ship.getHitpoints() / 25000f) * armorLevel
					* ship.getArmorGrid().getArmorRating(), 0.1f));

			threatLevel += Math.pow(damage * (1f + (threat.getDamage().getType().getArmorMult() - 1f) * armorLevel * damageCap * 1.25f), DAMAGE_POWER);

			//Global.getCombatEngine().addFloatingText(threat.getLocation(), "" + damage, 15f, Color.white, ship, 0f, 0f);
		}

		for (BeamAPI beam : Global.getCombatEngine().getBeams()) {
			if (beam.getDamageTarget() != ship) continue;

			float damage;
			if (beam.getWeapon().isBurstBeam()) {
				damage = beam.getWeapon().getDerivedStats().getBurstDamage() * 1.2f
						+ beam.getWeapon().getDerivedStats().getEmpPerSecond() * shipTTV * empCap;
			} else {
				damage = beam.getWeapon().getDerivedStats().getDps() * shipTTV
						+ beam.getWeapon().getDerivedStats().getEmpPerSecond() * shipTTV * empCap;
			}
			damage /= (float) Math.sqrt(Math.max((ship.getHitpoints() / 25000f) * armorLevel
					* ship.getArmorGrid().getArmorRating(), 0.1f));
			threatLevel += Math.pow(damage * (1f + (beam.getDamage().getType().getArmorMult() - 1f) * armorLevel * damageCap * 0.75f), DAMAGE_POWER);
		}

		float opportunityLevel = 0f;
		for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, range)) {
			float falloff = 1f;
			float distance = MathUtils.getDistance(ship, enemy);
			if (distance >= range * 0.5f) {
				falloff = (1f - distance / range) * 2f;
			}

			FluxTrackerAPI enemyFT = enemy.getFluxTracker();
			MutableShipStatsAPI enemyMS = enemy.getMutableStats();
			float fluxDifference = ((enemyFT.getMaxFlux() - enemyFT.getCurrFlux()) - (shipFlux.getMaxFlux()
					- shipFlux.getCurrFlux()))
					/ (shipFlux.getMaxFlux() + 1f);
			if ((enemyFT.isOverloadedOrVenting() || fluxDifference <= -0.5f)
					&& (member == null || !member.isCivilian())) {
				FleetMemberAPI enemyMember = enemy.getFleetMember();
				if (enemyMember != null) {
					if (ship.getShipTarget() == enemy) {
						opportunityLevel += 100f * Math.max(-fluxDifference, 0.5f) * (float) Math.sqrt(
								enemyMember.getFleetPointCost()) / shipStrength;
					} else {
						opportunityLevel += 30f * Math.max(-fluxDifference, 0.5f) * (float) Math.sqrt(
								enemyMember.getFleetPointCost()) / shipStrength;
					}
				}
			}

			float enemyTime = enemyMS.getTimeMult().getModifiedValue();

			float enemyVentRate = enemyMS.getFluxDissipation().getModifiedValue() * 2f * enemyMS.getVentRateMult().getModifiedValue() * enemyTime;
			float enemyTTV = 0f;
			if (enemyVentRate > 0f) {
				enemyTTV = enemyFT.getCurrFlux() / enemyVentRate;
			}

			if (enemyFT.isOverloaded() && enemyFT.getOverloadTimeRemaining() * enemyTime > shipTTV + 2.5f) {
				continue;
			}
			if (enemyFT.isVenting() && enemyTTV > shipTTV + 2.5f) {
				continue;
			}

			float speedFactor = (float) Math.sqrt(enemy.getMaxSpeed() * enemyTime
					/ (ship.getMaxSpeed() + 20f));

			if (distance <= range * 0.5f) {
				FleetMemberAPI enemyMember = enemy.getFleetMember();
				if (enemyMember != null) {
					float fall = (range * 0.5f - distance) / (range * 0.5f);
					if (ship.getShipTarget() == enemy) {
						threatLevel += speedFactor * 50f * fall
								* (float) Math.sqrt(enemyMember.getFleetPointCost()) / shipStrength;
					} else {
						threatLevel += speedFactor * 20f * fall * (float) Math.sqrt(
								(enemyMember.getFleetPointCost())) / shipStrength;
					}
				}
			}

			for (WeaponAPI weapon : enemy.getUsableWeapons()) {
				float speedDiversion = Math.max(0f,
						enemy.getMaxSpeed() - ship.getMaxSpeed() * 0.5f);
				float rangeSlip = speedDiversion * Math.max(weapon.getCooldownRemaining(), shipTTV);
				float weaponDist = MathUtils.getDistance(ship, weapon.getLocation());
				float weaponRange = weapon.getRange() + rangeSlip;
				float availableFlux = Math.min(enemyFT.getMaxFlux() - enemyFT.getCurrFlux()
						+ speedDiversion * Math.max(weapon.getCooldownRemaining(), shipTTV), enemyFT.getMaxFlux());
				if ((!weapon.isFiring() && weapon.getCooldownRemaining() <= shipTTV)
						&& (weapon.getAmmo() > 0 || !weapon.usesAmmo())
						&& weapon.getFluxCostToFire() <= availableFlux
						&& ((PLSP_Util.getTimeToAim(weapon, ship.getLocation()) <= shipTTV
						|| weapon.getSpec().getAIHints().contains(AIHints.DO_NOT_AIM)) && weaponRange >= weaponDist)) {

					/*
					float damage;
					DerivedWeaponStatsAPI weaponStats = weapon.getDerivedStats();
					if (weapon.isBurstBeam()) {
						damage = weaponStats.getBurstDamage() + weaponStats.getEmpPerSecond() * EMP_FACTOR
								* weaponStats.getBurstFireDuration();
					} else if (weapon.usesAmmo() && weapon.getSpec().getAmmoPerSecond() == 0f) {
						damage = weaponStats.getDamagePerShot() + weaponStats.getEmpPerShot() * EMP_FACTOR;
					} else if (weapon.usesAmmo() && weaponStats.getSustainedDps() < weaponStats.getDps()) {
						damage = Math.max((weaponStats.getDamagePerShot() + weaponStats.getEmpPerShot() * EMP_FACTOR),
								(weaponStats.getDps() + weaponStats.getEmpPerSecond()
										* 0.25f) * weapon.getAmmo()
										/ weapon.getMaxAmmo()
										+ weaponStats.getSustainedDps() * (1f
										- (float)weapon.getAmmo() / weapon.getMaxAmmo()));
					} else {
						damage = Math.max((weaponStats.getDamagePerShot() + weaponStats.getEmpPerShot() * EMP_FACTOR),
								weaponStats.getDps() + weaponStats.getEmpPerSecond() * EMP_FACTOR);
					}*/

					WeaponSpecAPI spec = weapon.getSpec();
					DerivedWeaponStatsAPI weaponStats = weapon.getDerivedStats();

					float damage;
					if (weapon.isBeam()) {
						if (weapon.isBurstBeam()) {
							float burstTime = Math.min(spec.getBurstDuration(), shipTTV);
							damage = 2.5f * weaponStats.getBurstDamage() * burstTime / spec.getBurstDuration()
									+ weaponStats.getEmpPerSecond() * empCap;
						} else {
							damage = (weaponStats.getDps() + weaponStats.getEmpPerSecond() * empCap) * shipTTV;
						}
					} else {
						if (weapon.usesAmmo()) {
							int burstSize = Math.min((int)(spec.getBurstSize() * (1 + shipTTV / weapon.getCooldown())),
									weapon.getAmmo());
							damage = (weaponStats.getDamagePerShot() + weaponStats.getEmpPerShot() * empCap) * burstSize;

							float timeLeft = shipTTV - weapon.getCooldown() * burstSize;
							if (timeLeft > 0f) {
								int sustainedSize = (int)(spec.getAmmoPerSecond() * timeLeft);
								damage += (weaponStats.getDamagePerShot() + weaponStats.getEmpPerShot() * empCap) * sustainedSize;
							}
						} else {
							int burstSize = (int)(spec.getBurstSize() * (1 + shipTTV / weapon.getCooldown()));
							damage = (weaponStats.getDamagePerShot() + weaponStats.getEmpPerShot() * empCap) * burstSize;
						}

						if (ship.getMaxSpeed() > 0f && weapon.getProjectileSpeed() > 0f) {
							damage *= weapon.getProjectileSpeed() / (ship.getMaxSpeed() + weapon.getProjectileSpeed());
						}
					}

					damage = falloff * 1.1f // we have a 1.1 now
							* (float) Math.pow(damage * (1f + (weapon.getDamageType().getArmorMult() - 1f) * armorLevel)
							* damageCap, DAMAGE_POWER);

					//Global.getCombatEngine().addFloatingText(weapon.getLocation(), "" + damage, 15f, Color.white, enemy, 0f, 0f);
					threatLevel += speedFactor * damage * enemyTime;
				}
			}
		}

		float allyLevel = 0f;
		for (ShipAPI ally : AIUtils.getNearbyAllies(ship, range * 0.5f)) {
			if (ally == ship || ally.isDrone() || ally.isFighter()) continue;
			if (ally.getHullSpec().isCivilianNonCarrier() || ally.isStationModule()) continue;
			if (ally.getFluxTracker().isOverloadedOrVenting()) continue;

			FleetMemberAPI allyMember = ally.getFleetMember();
			if (allyMember != null) {
				allyLevel += allyMember.getFleetPointCost();
			} else {
				if (ally.isFrigate()) {
					allyLevel += 4f;
				} else if (ally.isDestroyer()) {
					allyLevel += 8f;
				} else if (ally.isCruiser()) {
					allyLevel += 14f;
				} else if (ally.isCapital()) {
					allyLevel += 28f;
				}
			}
		}

		threatLevel = (float) Math.pow(threatLevel, 0.75f) / (float) Math.sqrt(shipStrength);
		threatLevel -= (float) Math.sqrt(allyLevel * 0.3f) * 5f;
		threatLevel += opportunityLevel;//move to here
		return Math.max(threatLevel, 0f);
	}

	//Originally by DR, modified by AnyIDElse
	private static final Map<HullSize, Float> RESERVED_FLUX_DEFAULTS = new HashMap<>(4);
	static {
		RESERVED_FLUX_DEFAULTS.put(HullSize.FRIGATE, 0.4f);
		RESERVED_FLUX_DEFAULTS.put(HullSize.DESTROYER, 0.4f);
		RESERVED_FLUX_DEFAULTS.put(HullSize.CRUISER, 0.5f);
		RESERVED_FLUX_DEFAULTS.put(HullSize.CAPITAL_SHIP, 0.6f);
	}
	private static void AIPlus(ShipAPI ship) {
		FluxTrackerAPI shipFlux = ship.getFluxTracker();
		MutableShipStatsAPI stats = ship.getMutableStats();

		if (ship.getSystem() != null && ship.getSystem().isActive()) {
			return;
		} else if (ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive()) {
			return;
		}

		float maxVentTime = 10f;
		float ventRate = stats.getFluxDissipation().getModifiedValue() * 2f * stats.getVentRateMult().getModifiedValue();
		if (ventRate > 0f) {
			maxVentTime = shipFlux.getMaxFlux() / ventRate;
		}

		float armorLevel = getArmorLevel(ship);
		float threatLevel = getThreatLevel(ship);
		float decisionLevel = 4f * (float) Math.sqrt(ship.getHitpoints() * 0.01f) + 0.5f
				* (float) Math.sqrt(ship.getHitpoints() * 0.01f)
				* (float) Math.sqrt(armorLevel * ship.getArmorGrid().getArmorRating() * 0.1f)
				/ (maxVentTime * 0.125f);

		float reserved = RESERVED_FLUX_DEFAULTS.get(ship.getHullSize());
		decisionLevel *= (shipFlux.getCurrFlux() + 0.5f * shipFlux.getHardFlux()
				- reserved * shipFlux.getMaxFlux()) / shipFlux.getMaxFlux();

		FleetMemberAPI member = ship.getFleetMember();
		float shipStrength = member != null ? 0.1f + member.getFleetPointCost() : 1f;
		if (threatLevel > shipStrength) {
			decisionLevel *= shipStrength / threatLevel;
		}

		//Global.getCombatEngine().maintainStatusForPlayerShip("14223", "", "des orig", decisionLevel+"", false);


		decisionLevel -= threatLevel;

		if (isInBurst(ship)) {
			decisionLevel *= 0.25f;
		}

		ShipwideAIFlags flags = ship.getAIFlags();
		if (flags.hasFlag(AIFlags.DO_NOT_VENT)) {
			decisionLevel *= 0.25f;
		}

		if (flags.hasFlag(AIFlags.KEEP_SHIELDS_ON)) {
			decisionLevel *= 0.5f;
		}

		if (flags.hasFlag(AIFlags.SAFE_VENT)) {
			decisionLevel *= 1.5f;
		}

		if (flags.hasFlag(AIFlags.PURSUING)) {
			decisionLevel *= 0.75f;
		}

		if (flags.hasFlag(AIFlags.IN_ATTACK_RUN)) {
			decisionLevel *= 0.75f;
		}

		if (flags.hasFlag(AIFlags.POST_ATTACK_RUN)) {
			decisionLevel *= 1.2f;
		}

		if (flags.hasFlag(AIFlags.BACK_OFF)) {
			decisionLevel *= 1.2f;
		}

		if (flags.hasFlag(AIFlags.RUN_QUICKLY)) {
			decisionLevel *= 1.2f;
		}

		if (shipFlux.getFluxLevel() <= 0.25f) {
			decisionLevel *= shipFlux.getFluxLevel() * 4f;
		} else if (shipFlux.getFluxLevel() >= 0.95f) {
			decisionLevel *= shipFlux.getFluxLevel() * 10f - 8.5f;
		}

		float threshold = ((0.65f * (float) Math.sqrt(ship.getMaxHitpoints() * 0.02f) + 0.05f * (float) Math.sqrt(
				ship.getMaxHitpoints() * 0.02f)
				* (float) Math.sqrt(ship.getArmorGrid().getArmorRating() * 0.25f))
				* maxVentTime * 0.125f) * (1.5f - reserved);

		//Global.getCombatEngine().maintainStatusForPlayerShip("123", "", "threat", threatLevel+"", false);
		//Global.getCombatEngine().maintainStatusForPlayerShip("1223", "", "des", decisionLevel+"", false);
		//Global.getCombatEngine().maintainStatusForPlayerShip("12223", "", "threshold", threshold+"", false);

		if (decisionLevel > threshold) {
			ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
			ship.getAIFlags().removeFlag(AIFlags.DO_NOT_USE_SHIELDS);
		} else if (decisionLevel > threshold * 0.8f) {
			ship.getAIFlags().setFlag(AIFlags.DO_NOT_USE_SHIELDS, 3f);
		}
	}

	private static float getRangeBonus(ShipAPI ship) {
		float cap = ship.getVariant().getNumFluxCapacitors();
		float vent = ship.getVariant().getNumFluxVents();

		return Math.min(Math.max(0f, (vent - cap) * 1f), 30f);
	}
}
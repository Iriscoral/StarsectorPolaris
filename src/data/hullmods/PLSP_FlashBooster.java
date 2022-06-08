package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_DataBase;
import data.scripts.util.PLSP_Util.I18nSection;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class PLSP_FlashBooster extends BaseHullMod {
	private static final String id = "PLSP_FlashBooster";
	private static final Map<HullSize, Float> BOOST_RATIO = new HashMap<>();
	private static final Map<WeaponSize, Float> SIZE_FACTOR = new HashMap<>();
	static {
		BOOST_RATIO.put(HullSize.DEFAULT, 40f);
		BOOST_RATIO.put(HullSize.FIGHTER, 40f);
		BOOST_RATIO.put(HullSize.FRIGATE, 40f);
		BOOST_RATIO.put(HullSize.DESTROYER, 40f);
		BOOST_RATIO.put(HullSize.CRUISER, 40f);
		BOOST_RATIO.put(HullSize.CAPITAL_SHIP, 40f);
		SIZE_FACTOR.put(WeaponSize.SMALL, 1f);
		SIZE_FACTOR.put(WeaponSize.MEDIUM, 2f);
		SIZE_FACTOR.put(WeaponSize.LARGE, 4f);
	}
	
	public static final I18nSection strings = I18nSection.getInstance("HullMod", "PLSP_");

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return !isForModSpec;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 10f;
		float padS = 2f;

		tooltip.addPara("%s " + strings.get("flashboosterTEXT1"), pad, Misc.getHighlightColor(), "#");
		tooltip.addPara("    %s " + strings.get("flashboosterTEXT2"), padS, Misc.getPositiveHighlightColor(), "#", "+40%");
		tooltip.addPara("    %s " + strings.get("flashboosterTEXT3"), padS, Misc.getPositiveHighlightColor(), "#", "+20%");
		tooltip.addPara("    %s " + strings.get("flashboosterTEXT4"), padS, Misc.getHighlightColor(), "#", "1", "2", "4");
		tooltip.addPara("%s " + strings.get("flashboosterTEXT5"), padS, Misc.getNegativeHighlightColor(), "#");
		tooltip.addPara("%s " + strings.get("HAO"), pad, Misc.getNegativeHighlightColor(), "#");
	}
	
	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		MutableShipStatsAPI stats = ship.getMutableStats();
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.getCustomData().containsKey(id)) {
			engine.getCustomData().put(id, new HashMap<>());
		}

		Map<ShipAPI, BoosterState> shipsMap = (Map)engine.getCustomData().get(id);
		if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
			if (!ship.isAlive()) {
				shipsMap.remove(ship);
			}
			return;
		}

		if (!shipsMap.containsKey(ship)) {
			shipsMap.put(ship, new BoosterState(ship));
		} else {
			BoosterState data = shipsMap.get(ship);

			float activeFactor = 0f;
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				if (weapon.isDecorative()) continue;
				if (weapon.getType() == WeaponType.MISSILE) continue;

				float chargeLevel = weapon.getChargeLevel();
				float cooldown = weapon.getCooldownRemaining();
				NumberContainer nContainer = data.weaponDatas.get(weapon);
				if (ship.getFluxTracker().isOverloadedOrVenting()) {
					nContainer.theEffectLevel = 0f;
					continue;
				}

				if (chargeLevel > nContainer.lastChargeLevel && chargeLevel > 0f) {
					nContainer.reachedFiring = true;
					// up-charging
				} else  if (chargeLevel == 1f && weapon.isFiring()) {
					// beam firing or proj weapon firing multi bullets
					nContainer.reachedFiring = true;

					float timeElapsed = amount * 0.15f;
					if (weapon.getSpec().getBurstSize() > 1f) {
						timeElapsed /= weapon.getSpec().getBurstSize();
					}

					nContainer.theEffectLevel += timeElapsed;
					nContainer.theEffectLevel = Math.min(nContainer.theEffectLevel, 1f);
				} else if (nContainer.reachedFiring) {
					// frame after proj weapon fired one round
					nContainer.reachedFiring = false;

					float weaponCooldown = weapon.getCooldown() * 0.2f + weapon.getSpec().getChargeTime() * 0.2f;
					if (weapon.getSpec().getBurstSize() > 1f) {
						weaponCooldown /= weapon.getSpec().getBurstSize();
					} else if (weapon.isBurstBeam()) {
						weaponCooldown /= weapon.getSpec().getBurstDuration() * 2f;
					}

					nContainer.theEffectLevel += weaponCooldown;
					nContainer.theEffectLevel = Math.min(nContainer.theEffectLevel, 1f);
				} else if (cooldown < nContainer.lastCooldown) {
					// normal cooldowning
				} else {
					// idle
					nContainer.theEffectLevel -= amount / nContainer.factor;
					nContainer.theEffectLevel = Math.max(nContainer.theEffectLevel, 0f);
				}

				nContainer.lastChargeLevel = chargeLevel;
				nContainer.lastCooldown = cooldown;
				activeFactor += (nContainer.factor * nContainer.theEffectLevel);
			}

			float ratio = activeFactor / data.totalFactor;
			if ((float) Math.random() < ratio * amount) {
				float angle = (float) Math.random() * 360f;
				Vector2f start = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * MathUtils.getRandomNumberInRange(0.5f, 0.8f), angle);
				Vector2f end = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * MathUtils.getRandomNumberInRange(0.5f, 0.8f), angle + MathUtils.getRandomNumberInRange(30f, 90f));
				engine.spawnEmpArcVisual(start, ship, end, null, (float) Math.random() * 30f, PLSP_ColorData.NORMAL_EMP_ARC_FRINGE, PLSP_ColorData.NORMAL_EMP_ARC_CORE);
			}

			// float boost = ratio * BOOST_RATIO.get(ship.getHullSize());
			float boost = ratio * 40f;
			stats.getMissileRoFMult().modifyPercent(id, boost);
			stats.getBallisticRoFMult().modifyPercent(id, boost);
			stats.getEnergyRoFMult().modifyPercent(id, boost);
			stats.getBeamWeaponDamageMult().modifyPercent(id, boost * 0.5f);

			if (ship == engine.getPlayerShip()) {
				float percent = 100f * ratio;
				engine.maintainStatusForPlayerShip(id, "graphics/icons/hullsys/active_flare_launcher.png", Global.getSettings().getHullModSpec("PLSP_flashbooster").getDisplayName(), strings.get("flashboosterA") + (int)percent + "%", false);
			}
		}
	}
	
	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return PLSP_DataBase.isPLSPShip(ship);
	}
	
	private final static class BoosterState {
		Map<WeaponAPI, NumberContainer> weaponDatas;
		float totalFactor = 0f;

		private BoosterState(ShipAPI ship) {
			weaponDatas = new HashMap<>();
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				if (weapon.isDecorative()) continue;
				if (weapon.getType() == WeaponType.MISSILE) continue;

				float factor = SIZE_FACTOR.get(weapon.getSize());
				if (weapon.hasAIHint(AIHints.PD)) factor *= 0.5f;

				totalFactor += factor;
				weaponDatas.put(weapon, new NumberContainer(factor));
			}

			if (totalFactor == 0f) totalFactor = 1f;
		}
	}

	private final static class NumberContainer {
		float lastChargeLevel;
		float lastCooldown;
		boolean reachedFiring;
		float factor;
		float theEffectLevel;

		private NumberContainer(float number) {
			this.lastChargeLevel = 0f;
			this.lastCooldown = 0f;
			this.reachedFiring = false;
			this.factor = number;
			this.theEffectLevel = 0f;
		}
	}
}
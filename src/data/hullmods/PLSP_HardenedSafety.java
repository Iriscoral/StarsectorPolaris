package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_DataBase;
import data.scripts.util.PLSP_Util.I18nSection;
import data.scripts.weapons.ai.PLSP_InvertedBeamMoteAI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class PLSP_HardenedSafety extends BaseHullMod {
	private static final String id = "PLSP_HardenedSafety";
	private static final Color JITTER_COLOR = new Color(90, 165, 255, 55);
	private static final Color JITTER_UNDER_COLOR = new Color(90, 165, 255, 155);
	private static final Map<HullSize, Float> spawnDelay = new HashMap<>();
	private static final Map<HullSize, Float> speedPenalty = new HashMap<>();
	static {
		spawnDelay.put(HullSize.DEFAULT, 1f);
		spawnDelay.put(HullSize.FIGHTER, 1f);
		spawnDelay.put(HullSize.FRIGATE, 2f);
		spawnDelay.put(HullSize.DESTROYER, 2f);
		spawnDelay.put(HullSize.CRUISER, 1.5f);
		spawnDelay.put(HullSize.CAPITAL_SHIP, 1f);

		speedPenalty.put(HullSize.DEFAULT, 20f);
		speedPenalty.put(HullSize.FIGHTER, 20f);
		speedPenalty.put(HullSize.FRIGATE, 20f);
		speedPenalty.put(HullSize.DESTROYER, 20f);
		speedPenalty.put(HullSize.CRUISER, 20f);
		speedPenalty.put(HullSize.CAPITAL_SHIP, 15f);
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

		tooltip.addPara("%s " + strings.get("hardenedsafetyTEXT1"), pad, Misc.getPositiveHighlightColor(), "#", "30%");
		tooltip.addPara("%s " + strings.get("hardenedsafetyTEXT2"), padS, Misc.getPositiveHighlightColor(), "#", "800", "30%");
		tooltip.addPara("%s " + strings.get("hardenedsafetyTEXT3"), padS, Misc.getPositiveHighlightColor(), "#", "800", "1400", "30%");
		tooltip.addPara("%s " + strings.get("hardenedsafetyTEXT4"), pad, Misc.getHighlightColor(), "#", "20", "20", "20", "15");
		tooltip.addPara("%s " + strings.get("hardenedsafetyTEXT5"), padS, Misc.getPositiveHighlightColor(), "#", "2", "2", "1.5", "1");
		tooltip.addPara("%s " + strings.get("hardenedsafetyDO"), pad, Misc.getNegativeHighlightColor(), "#", Global.getSettings().getHullModSpec("safetyoverrides").getDisplayName());
		tooltip.addPara("%s " + strings.get("HAO"), padS, Misc.getNegativeHighlightColor(), "#");
	}
	
	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getStat(Stats.EXPLOSION_DAMAGE_MULT).modifyMult(id, 0.5f);
		stats.getDynamic().getStat(Stats.EXPLOSION_RADIUS_MULT).modifyMult(id, 0.5f);

		stats.getMaxSpeed().modifyFlat(id, -speedPenalty.get(hullSize));
		stats.getZeroFluxSpeedBoost().modifyFlat(id, (int)(speedPenalty.get(hullSize) * 0.5f));

		stats.getWeaponRangeThreshold().modifyFlat(id, 800f);
		stats.getWeaponRangeMultPastThreshold().modifyPercent(id, 30f);
		stats.getProjectileSpeedMult().modifyPercent(id, 30f);
	}
	
	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.getCustomData().containsKey(id)) {
			engine.getCustomData().put(id, new HashMap<>());
		}

		Map<ShipAPI, HSState> shipsMap = (Map)engine.getCustomData().get(id);
		if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
			if (!ship.isAlive()) {
				shipsMap.remove(ship);
			}
			return;
		}

		if (!shipsMap.containsKey(ship)) {
			shipsMap.put(ship, new HSState(ship));
		} else {
			HSState data = shipsMap.get(ship);
			FluxTrackerAPI flux = ship.getFluxTracker();

			if (flux.isOverloaded()) {

				// ship.setJitter(this, JITTER_COLOR, 0.5f + flux.getOverloadTimeRemaining() * 0.1f, 3, 0f, 10f);
				ship.setJitterUnder(this, JITTER_UNDER_COLOR, 0.5f + flux.getOverloadTimeRemaining() * 0.1f, 25, 0f, 17f);

				data.timer += engine.getElapsedInLastFrame();
				if (data.timer >= spawnDelay.get(ship.getHullSize())) {
					data.timer = 0f;

					Vector2f start = MathUtils.getRandomPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * MathUtils.getRandomNumberInRange(0.5f, 1f));
					float angle = VectorUtils.getAngle(ship.getLocation(), start) + MathUtils.getRandomNumberInRange(-30f, 30f);

					float rampUp = 0.1f;
					Vector2f particleVel = MathUtils.getPoint(null, 100f, angle);
					engine.addNebulaParticle(start, particleVel, 25f, 2f, rampUp, 0f, 1f, PLSP_ColorData.SHALLOW_EMP_ARC_CORE);

					MissileAPI proj = (MissileAPI)engine.spawnProjectile(ship, null, "motelauncher", start, angle, null);
					proj.setArmingTime(0.25f);
					proj.setEmpResistance(10000);

					proj.setMissileAI(new PLSP_InvertedBeamMoteAI(proj, ship));
					proj.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
				}
			}
		}
	}
	
	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getVariant().getHullMods().contains("safetyoverrides")) {
			ship.getVariant().removeMod("safetyoverrides");
			Global.getSoundPlayer().playUISound("cr_allied_warning", 1f, 1f);
		}
	}
	
	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return (!ship.getVariant().getHullMods().contains("safetyoverrides") && PLSP_DataBase.isPLSPShip(ship));
	}
	
	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().getHullMods().contains("safetyoverrides")) {
			return String.format(strings.get("hardenedsafetyUN1"), Global.getSettings().getHullModSpec("safetyoverrides").getDisplayName());
		}
		
		return null;
	}

	public final static class HSState implements DamageTakenModifier {
		private ShipAPI ship;
		float timer;

		public HSState(ShipAPI ship) {
			this.ship = ship;
			this.timer = 0f;

			ship.addListener(this);
		}

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {

			Vector2f from;
			if (param instanceof DamagingProjectileAPI) {
				from = ((DamagingProjectileAPI)param).getSpawnLocation();
			} else if (param instanceof BeamAPI) {
				from = ((BeamAPI)param).getFrom();
			} else {
				return null;
			}

			float dist = Misc.getDistance(from, point);
			if (param instanceof MissileAPI) {
				MissileAPI missile = (MissileAPI)param;
				dist = Math.max(dist, missile.getFlightTime() * missile.getMaxSpeed());
			}

			float minRangeForEffect = 800f;
			float maxRangeForEffect = 1400f;
			float effectLevel = 0f;
			if (dist > maxRangeForEffect) {
				effectLevel = 1f;
			} else if (dist > minRangeForEffect) {
				effectLevel = (dist - minRangeForEffect) / (maxRangeForEffect - minRangeForEffect);
			}

			damage.getModifier().modifyMult(id, 1f - effectLevel * 0.3f);
			return id;
		}
	}
}
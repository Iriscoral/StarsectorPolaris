package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.FindShipFilter;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_Util;
import data.scripts.util.PLSP_Util.I18nSection;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PLSP_DarkCollapseStats extends BaseShipSystemScript {
	private static final Vector2f ZERO = new Vector2f();
	private static final Color DARK_EFFECT = new Color(50, 0, 50);
	private static final float RANGE_FACTOR = 1000f;
	private static final Map<HullSize, Float> DAMAGE = new HashMap<>();
	static {
		DAMAGE.put(HullSize.DEFAULT, 1000f);
		DAMAGE.put(HullSize.FIGHTER, 1000f);
		DAMAGE.put(HullSize.FRIGATE, 800f);
		DAMAGE.put(HullSize.DESTROYER, 600f);
		DAMAGE.put(HullSize.CRUISER, 400f);
		DAMAGE.put(HullSize.CAPITAL_SHIP, 400f);
	}

	private DarkCollapseState data = null;

	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "PLSP_");

	public static float getRange(ShipAPI ship) {
		if (ship == null) return RANGE_FACTOR;
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE_FACTOR);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();
		// if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
		if (!engine.isEntityInPlay(ship) || !ship.isAlive()) {
			return;
		}

		if (data == null) {
			data = new DarkCollapseState(getTarget(ship));
		} else {
			float amount = engine.getElapsedInLastFrame();
			if (engine.isPaused()) amount = 0f;

			if (!data.launched && (data.target == Global.getCombatEngine().getPlayerShip() || ship == Global.getCombatEngine().getPlayerShip())) {
				data.target.getFluxTracker().showOverloadFloatyIfNeeded(strings.get("darkcollapseS3"), PLSP_ColorData.TEXT_GREY, 4f, true);
				data.launched = true;
			}

			ship.setJitterUnder(ship, PLSP_ColorData.PHASE_GLOW, effectLevel, 10, 0f, 20f);
			ship.setJitter(ship, PLSP_ColorData.PHASE_GLOW, effectLevel, 5, 0f, 10f);

			if (state == State.IN) {
				Color effectColor = Misc.interpolateColor(PLSP_ColorData.PHASE_MAIN, DARK_EFFECT, effectLevel);

				MutableShipStatsAPI statsT = data.target.getMutableStats();
				statsT.getBallisticWeaponRangeBonus().modifyMult(id, 1f - effectLevel);
				statsT.getEnergyWeaponRangeBonus().modifyMult(id, 1f - effectLevel);
				statsT.getMissileWeaponRangeBonus().modifyMult(id, 1f - effectLevel);
				statsT.getBallisticRoFMult().modifyMult(id, 1f - effectLevel);
				statsT.getEnergyRoFMult().modifyMult(id, 1f - effectLevel);
				statsT.getMissileRoFMult().modifyMult(id, 1f - effectLevel);

				data.target.setJitter(ship, effectColor, effectLevel, 5, 0f, 5f);
				data.target.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
				data.target.setAngularVelocity(data.target.getAngularVelocity() * 0.5f);
				data.target.getVelocity().scale(0.9f);

				data.visualTimer -= amount;
				if (data.visualTimer <= 0f) {
					float particleSize = data.target.getCollisionRadius() * (0.5f + 0.5f * effectLevel + 0.75f * (float)Math.random()) + 100f;
					float distance = data.target.getCollisionRadius() * 3f;
					Vector2f particleLoc = MathUtils.getRandomPointInCircle(data.target.getLocation(), distance);
					Vector2f particleVel = MathUtils.getPointOnCircumference(null, effectLevel * 200f, 360f * (float)Math.random());
					engine.addNebulaParticle(particleLoc, particleVel, particleSize,
							0.5f + (float)Math.random() * 0.75f, effectLevel, effectLevel, 1.3f - effectLevel, effectColor);
					data.visualTimer += 0.1f;
				}

				if (amount > 0f)
				for (int i = 0; i < 3; i++) {
					float time = 1f - effectLevel * 0.5f;
					float distance = data.target.getCollisionRadius() * 4f * (2f - effectLevel) + 100f * (float)Math.random();
					Color particleColor = Misc.interpolateColor(PLSP_ColorData.PHASE_GLOW, DARK_EFFECT, (float)Math.random());

					Vector2f particleLoc = MathUtils.getRandomPointOnCircumference(data.target.getLocation(), distance);
					Vector2f particleVel = MathUtils.getPointOnCircumference(null, distance, VectorUtils.getAngle(particleLoc, data.target.getLocation()));
					particleVel.scale(1f / time);
					Global.getCombatEngine().addSmoothParticle(particleLoc, particleVel, (float)Math.random() * 15f + 5f, 1f, time, particleColor);
				}

				float size = data.target.getCollisionRadius() * (4f - effectLevel * 2f) * 2f;
				data.vortex.setSize(size, size);
				data.vortex.setColor(effectColor);
				data.vortex.setAlphaMult(Math.min(effectLevel * 2f, 1f));
				data.vortex.setAngle(data.vortex.getAngle() + amount * (30f + effectLevel * 60f));
				PLSP_Util.simpleFrameRender(data.vortex, data.target.getLocation(), CombatEngineLayers.ABOVE_PARTICLES_LOWER);
			} else if (effectLevel == 1f) {
				// float size = getRange(ship) * 0.5f - 30f; // minus 30
				float size = RANGE_FACTOR * 0.5f - 30f; // minus 30
				ship.setJitter(ship, new Color(10, 10, 10, 200), 2f, 15, 0f, 20f);

				PLSP_DarkCollapseVisual.DCVParams p = new PLSP_DarkCollapseVisual.DCVParams(size, PLSP_ColorData.PHASE_MAIN);
				p.thickness = 50f;
				PLSP_DarkCollapseVisual.spawnCrossRift(data.target.getLocation(), p);

				PLSP_Util.easyRippleOut(data.target.getLocation(), ZERO, size * 2.5f, 500f, 0.5f);
				Global.getSoundPlayer().playSound("PLSP_darkcollapse_burst", 1f, 1f, ship.getLocation(), ZERO);

				List<ShipAPI> targets = AIUtils.getNearbyAllies(data.target, size);
				targets.add(data.target);
				AOE(ship, data.target.getLocation(), targets);
				data.target.getEngineController().forceFlameout();

				for (DamagingProjectileAPI proj : PLSP_Util.getEnemyProjectilesAndMissilesWithinRange(data.target.getLocation(), data.target.getCollisionRadius() + size, ship.getOwner())) {
					Global.getCombatEngine().removeEntity(proj);
				}

				for (CombatEntityAPI ast : CombatUtils.getAsteroidsWithinRange(data.target.getLocation(), data.target.getCollisionRadius() + size)) {
					Global.getCombatEngine().removeEntity(ast);
				}
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		if (data != null) {

			data.target.getMutableStats().getBallisticWeaponRangeBonus().unmodifyMult(id);
			data.target.getMutableStats().getEnergyWeaponRangeBonus().unmodifyMult(id);
			data.target.getMutableStats().getMissileWeaponRangeBonus().unmodifyMult(id);
			data.target.getMutableStats().getBallisticRoFMult().unmodifyMult(id);
			data.target.getMutableStats().getEnergyRoFMult().unmodifyMult(id);
			data.target.getMutableStats().getMissileRoFMult().unmodifyMult(id);

			data = null;
		}
	}

	private static void AOE(ShipAPI source, Vector2f center, List<ShipAPI> targets) {
		for (ShipAPI target : targets) {
			//if (target.isPhased()) continue;

			float checkRange = target.getCollisionRadius() * 1.5f;
			List<Vector2f> locations = new ArrayList<>();
			float clampAngle = MathUtils.clampAngle(VectorUtils.getAngle(center, target.getLocation()));
			float fixedAngle = MathUtils.clampAngle(clampAngle - 90f);
			for (float i = -checkRange - 10f; i <= checkRange + 10f; i += 20f) {
				Vector2f damageC = MathUtils.getPointOnCircumference(target.getLocation(), i, fixedAngle);
				Vector2f lL = MathUtils.getPointOnCircumference(damageC, -checkRange, clampAngle);
				Vector2f rL = MathUtils.getPointOnCircumference(damageC, checkRange, clampAngle);
				Vector2f collisionPoint = PLSP_Util.getShipCollisionPoint(lL, rL, target);
				if (collisionPoint != null) {
					locations.add(collisionPoint);
				}
			}

			for (Vector2f damagePoint : locations) {
				float dis = MathUtils.getDistance(damagePoint, center);
				float basicDamage = DAMAGE.get(target.getHullSize());
				Global.getCombatEngine().applyDamage(target, damagePoint, basicDamage * (1f - Math.min(1f, dis / 2000f)), DamageType.ENERGY, 0f, false, false, source, true);
			}
		}
	}

	private static final FindShipFilter FILTER = new FindShipFilter(){
		@Override
		public boolean matches(ShipAPI ship) {
			return !ship.isPhased() && !ship.isFighter() && !ship.isDrone() && !ship.isCapital() && !ship.isStation() && !ship.isStationModule();
		}
	};

	private static ShipAPI getTarget(ShipAPI ship) {
		return PLSP_Util.findSingleEnemy(ship, getRange(ship), true, false, FILTER, null);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) return "";

		ShipAPI target = getTarget(ship);
		if (target == null) {
			return strings.get("darkcollapseS1");
		}

		return strings.get("darkcollapseS2");
	}


	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (system.isActive()) return true;
		ShipAPI target = getTarget(ship);
		return target != null;
	}

	private final static class DarkCollapseState {
		ShipAPI target;
		SpriteAPI vortex;
		boolean launched;
		float visualTimer;

		private DarkCollapseState(ShipAPI ship) {
			launched = false;
			visualTimer = 0.1f;

			target = ship;
			vortex = Global.getSettings().getSprite("misc", "PLSP_chaosVortex");
			vortex.setAdditiveBlend();
		}
	}
}
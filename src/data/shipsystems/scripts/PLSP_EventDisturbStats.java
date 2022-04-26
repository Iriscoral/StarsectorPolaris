package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.plugins.MagicRenderPlugin;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class PLSP_EventDisturbStats extends BaseShipSystemScript {

	public static final String DATA_KEY = "PLSP_EventDisturbStats";
	private static final Vector2f ZERO = new Vector2f();
	private static final float RANGE_FACTOR = 1000f;

	private EventDisturbStats data = null;

	private static String getString(String key) {
		return Global.getSettings().getString("ShipSystem", "PLSP_" + key);
	}

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
		if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
			return;
		}

		if (data == null) {
			ShipAPI target = getTarget(ship);
			data = new EventDisturbStats(target);
		} else {

			if (!data.launched && (data.target == engine.getPlayerShip() || ship == engine.getPlayerShip())) {
				data.target.getFluxTracker().showOverloadFloatyIfNeeded(getString("eventdisturbS3"), PLSP_ColorData.TEXT_GREY, 4f, true);
				data.launched = true;
			}

			ship.setJitter(ship, PLSP_ColorData.LIGHT_YELLOW_JITTER, effectLevel, 10, 0f, 10f);
			data.target.setJitterUnder(ship, PLSP_ColorData.SHINY_YELLOW, effectLevel, 10, 0f, 20f);

			if (state == State.IN) {
				for (int i = 0; i < 4; i++) {
					float time = 0.5f - effectLevel * 0.3f;
					float distance = data.target.getCollisionRadius() * 6f * (1f - effectLevel * 0.8f) * ((float)Math.random() * 0.2f + 0.8f);
					Vector2f particleLoc = MathUtils.getRandomPointInCircle(data.target.getLocation(), distance);
					Vector2f particleVel = MathUtils.getPointOnCircumference(null, distance, VectorUtils.getAngle(particleLoc, data.target.getLocation()));
					particleVel.scale(1f / time);
					engine.addSmoothParticle(particleLoc, particleVel, (float) Math.random() * 13f + 2f, 1f, time, PLSP_ColorData.LIGHT_YELLOW);
				}
			}

			if (effectLevel == 1f) {

				SpriteAPI vortex = Global.getSettings().getSprite("misc", "PLSP_whiteRing");
				vortex.setSize(data.target.getCollisionRadius() * 2f, data.target.getCollisionRadius() * 2f);
				vortex.setColor(PLSP_ColorData.SHINY_YELLOW);
				vortex.setAdditiveBlend();
				modifiedObjectBasedRender(vortex, data.target, 1500f, 0.1f, 0.1f, 0.3f, CombatEngineLayers.BELOW_SHIPS_LAYER);

				float extraRange = getRange(ship) * 0.5f;
				Global.getCombatEngine().addLayeredRenderingPlugin(new PLSP_EventDisturbVisual(data.target, extraRange));
			}
		}
	}

	public static void modifiedObjectBasedRender(SpriteAPI sprite, ShipAPI anchor, float expand, float fadein, float full, float fadeout, CombatEngineLayers layer) {
		Vector2f loc = new Vector2f(anchor.getLocation());
		float size = sprite.getWidth();
		MagicRenderPlugin.addObjectspace(sprite, anchor, loc, ZERO, null, new Vector2f(size + expand, size + expand), 180f, 0f, true, fadein, fadein + full, fadein + full + fadeout, true, layer);
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		if (data != null) {
			data = null;
		}
	}

	public static boolean isTargetValid(ShipAPI ship, ShipAPI target) {
		if (target == null) return false;
		if (target.isPhased()) return false;
		if (target.isDrone() || target.isFighter()) return false;
		if (ship.getOwner() != target.getOwner()) return false;
		return target.getCustomData().get(DATA_KEY) == null;
	}

	public static ShipAPI getTarget(ShipAPI entity) {

		float range = getRange(entity);

		ShipAPI located = entity.getShipTarget();
		if (isTargetValid(entity, located) && MathUtils.getDistance(located, entity) <= range) {
			return located;
		}

		if (entity.getAI() == null) {
			if (isTargetValid(entity, entity)) return entity;
			return null;
		}

		List<DamagingProjectileAPI> projs = PLSP_Util.getEnemyProjectilesAndMissilesWithinRange(entity.getLocation(), entity.getCollisionRadius() + range + 500f, entity.getOwner());
		List<ShipAPI> targets = AIUtils.getNearbyAllies(entity, range);
		targets.add(entity);

		WeightedRandomPicker<ShipAPI> toPick = new WeightedRandomPicker<>();
		for (ShipAPI tmp : targets) {
			if (!isTargetValid(entity, tmp)) continue;

			float damage = 0f;
			for (DamagingProjectileAPI proj : projs) {
				if (!MathUtils.isWithinRange(tmp, proj, 600f)) continue;
				if (proj.getDamageType() == DamageType.FRAGMENTATION) {
					damage += proj.getDamageAmount() * 0.25f;
				} else {
					damage += proj.getDamageAmount();
				}
			}

			if (entity == tmp) damage *= 1.5f;
			toPick.add(tmp, damage);
		}

		return toPick.pick();
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) return "";

		ShipAPI located = ship.getShipTarget();
		if (isTargetValid(ship, located) && MathUtils.isWithinRange(ship, located, getRange(ship))) {
			return getString("eventdisturbS1");
		}

		return getString("eventdisturbS2");
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return getTarget(ship) != null;
	}

	private final static class EventDisturbStats {
		ShipAPI target;
		boolean launched;

		private EventDisturbStats(ShipAPI ship) {
			launched = false;
			target = ship;
		}
	}
}
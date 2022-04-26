package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.plugin.PLSP_SystemPlugin.LocalData;
import data.scripts.plugin.PLSP_SystemPlugin.TimeData;
import data.scripts.plugins.MagicRenderPlugin;
import data.scripts.util.MagicLensFlare;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

public class PLSP_TimeDisturbanceStats extends BaseShipSystemScript {
	private static final String DATA_KEY = "PLSP_SystemPlugin";
	private static final Vector2f ZERO = new Vector2f(0, 0);
	private static final float RANGE_FACTOR = 800f;
	
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

		float range = getRange(ship);
		float modifiedRange = range + 200f;
		if (effectLevel == 1f) {
			for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, range)) {
				applyEffect(enemy, engine, 3f);
			}

			for (DamagingProjectileAPI proj : PLSP_Util.getEnemyProjectilesAndMissilesWithinRange(ship.getLocation(), modifiedRange, ship.getOwner())) {
				engine.spawnExplosion(proj.getLocation(), ZERO, PLSP_ColorData.DARK_GREY, proj.getCollisionRadius(), 4f);
				PLSP_Util.addLight(proj.getLocation(), proj.getCollisionRadius() * 0.5f, 3f, 0.5f, PLSP_ColorData.LIGHT_GREY);
				engine.removeEntity(proj);
			}

			for (int i = 10; i > 0; i-=1) {
				float rangeP = ship.getCollisionRadius() * MathUtils.getRandomNumberInRange(0.5f, 1.5f);
				Vector2f spawnLocation = MathUtils.getRandomPointOnCircumference(ship.getLocation(), rangeP);
				Vector2f vel = VectorUtils.getDirectionalVector(ship.getLocation(), spawnLocation);
				float dis = MathUtils.getDistance(spawnLocation, ship.getLocation());
				vel.scale(dis * (float)Math.random() * 0.25f);
				engine.addHitParticle(spawnLocation, vel, (float)Math.random() * 60f + 30f, (float)Math.random() * 0.4f + 0.6f, (float)Math.random() * 0.3f + 0.4f, PLSP_ColorData.LIGHT_GREY2);
			}

			SpriteAPI sprite = Global.getSettings().getSprite("misc", "PLSP_commissionRing");
			sprite.setSize(modifiedRange * 2f, modifiedRange * 2f);
			sprite.setAdditiveBlend();
			modifiedObjectBasedRender(sprite, ship, 0.5f, 0.2f, 0.1f, 0.2f, CombatEngineLayers.UNDER_SHIPS_LAYER);

			PLSP_Util.visualEmpStorm(ship.getLocation(), 7, 45f, 30f, ship.getCollisionRadius() * 0.8f, 50f, ship, PLSP_ColorData.GRIM_EMP_ARC_FRINGE, PLSP_ColorData.GRIM_EMP_ARC_CORE, false);
			PLSP_Util.visualEmpStorm(ship.getLocation(), 6, 45f, 30f, ship.getCollisionRadius() * 1.5f, 50f, ship, PLSP_ColorData.GRIM_EMP_ARC_FRINGE, PLSP_ColorData.GRIM_EMP_ARC_CORE, false);
			PLSP_Util.easyRippleOut(ship.getLocation(), ship.getVelocity(), modifiedRange * 3f, 100f + ship.getCollisionRadius(), 3f, 90f);
			effectLevel = 3f;
		}

		ship.setJitter(id, PLSP_ColorData.LIGHT_GREY, Math.max((effectLevel - 0.5f) * 3f, 0f), 3, 0f, 10f);

		if ((float)Math.random() < 10f * engine.getElapsedInLastFrame() * effectLevel || effectLevel >= 1f) {
			MagicLensFlare.createSharpFlare(engine, ship, ship.getLocation(), 10f, range * 3f * effectLevel, 0f, PLSP_ColorData.VIVID_EMP_ARC_FRINGE, PLSP_ColorData.VIVID_EMP_ARC_CORE);
		}
	}

	public static void modifiedObjectBasedRender(SpriteAPI sprite, ShipAPI anchor, float circle, float fadein, float full, float fadeout, CombatEngineLayers layer) {
		Vector2f loc = new Vector2f(anchor.getLocation());
		float time = fadein + full + fadeout;
		MagicRenderPlugin.addObjectspace(sprite, anchor, loc, ZERO, null, null, 180f, 360f * circle / time, true, fadein, fadein + full, fadein + full + fadeout, true, layer);
	}
	
	private static void applyEffect(ShipAPI enemy, CombatEngineAPI engine, float time) {
		LocalData localData = (LocalData)engine.getCustomData().get(DATA_KEY);
		Map<ShipAPI, TimeData> theData = localData.timeData;

		if (!theData.containsKey(enemy)) {
			theData.put(enemy, new TimeData(time));
			if ((enemy.getFluxTracker().showFloaty() || enemy == Global.getCombatEngine().getPlayerShip()) && !enemy.isFighter() && !enemy.isDrone()) {
				String key = "time";
				enemy.getFluxTracker().showOverloadFloatyIfNeeded(Global.getSettings().getString("ShipSystem", "PLSP_" + key), PLSP_ColorData.TEXT_RED, 4f, true);
			}
		} else {
			for (Map.Entry<ShipAPI, TimeData> entry : theData.entrySet()) {
				ShipAPI ship = entry.getKey();
				if (ship == enemy) {
					TimeData data = entry.getValue();
					data.addTime(time);
				}
			}
		}
	}
	
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {}
	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(getString("timedisturbanceS1"), false);
		}
		return null;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) return "";

		List<ShipAPI> targets = AIUtils.getNearbyEnemies(ship, getRange(ship));
		if (!targets.isEmpty()) {
			return getString("timedisturbanceS2") + "" + targets.size();
		}

		return getString("timedisturbanceS3");
	}
}
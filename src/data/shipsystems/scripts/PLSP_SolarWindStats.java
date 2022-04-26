package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import data.scripts.plugin.PLSP_SystemPlugin.LocalData;
import data.scripts.plugin.PLSP_SystemPlugin.SolarData;
import data.scripts.plugin.PLSP_TrailLine;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Map;

public class PLSP_SolarWindStats extends BaseShipSystemScript {

	private static final String DATA_KEY = "PLSP_SystemPlugin";
	private static final float RANGE_FACTOR = 1400f;
	private static final Color COLD = new Color(169, 200, 187, 200);
	private static final Color HOT = new Color(246, 140, 131, 200);
	private static final Color SOLAR = new Color(255, 217, 163, 200);
	private static final Vector2f ZERO = new Vector2f();

	private static String getString(String key) {
		return Global.getSettings().getString("ShipSystem", "PLSP_" + key);
	}

	public static float getRange(ShipAPI ship) {
		if (ship == null) return RANGE_FACTOR;
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE_FACTOR);
	}
	
	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		CombatEngineAPI engine = Global.getCombatEngine();
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) {
			return;
		}

		if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
			return;
		}

		float range = getRange(ship) * effectLevel;
		float amount = engine.getElapsedInLastFrame();

		Color color = Misc.interpolateColor(COLD, HOT, effectLevel);
		if (effectLevel == 1f) {
			float speed = 1200f;
			float duration = range / speed;
			for (int i = 0; i < 360; i += 20) {
				float actualSpeed = speed * (0.75f + (float)Math.random() * 0.5f);
				Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * 0.5f, i);
				Vector2f vel = MathUtils.getPoint(null, actualSpeed, i);

				float size = (float)Math.random() * 150f + 75f;
				float sizeMult = 4f + (float)Math.random() * 2f;
				float brightness = (float)Math.random() * 0.5f + 0.5f;

				Color nebulaColor = Misc.interpolateColor(color, SOLAR, (float)Math.random());
				engine.addNebulaParticle(point, vel, size, sizeMult, 0.5f, brightness, duration, nebulaColor);
			}

			for (int i = 0; i < 12; i++) {
				float actualSpeed = speed * (0.5f + (float)Math.random() * 1f);
				Vector2f point = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius());
				Vector2f vel = MathUtils.getPoint(null, actualSpeed, VectorUtils.getAngle(ship.getLocation(), point));

				float size = (float)Math.random() * 150f + 50f;
				float sizeMult = 4f + (float)Math.random() * 2f;
				float brightness = (float)Math.random() * 0.5f + 0.5f;

				Color nebulaColor = Misc.interpolateColor(color, SOLAR, (float)Math.random());
				engine.addNebulaParticle(point, vel, size, sizeMult, 0.5f, brightness, duration, nebulaColor);
			}

			for (int i = 0; i < 360; i+= 30) {
				float actualSpeed = speed * (0.75f + (float) Math.random() * 1f);
				float angleVel = (float) Math.random() * 3f - (float) Math.random() * 3f;

				float angle = i + (float) Math.random() * 20f - (float) Math.random() * 20f;
				Vector2f point = MathUtils.getPoint(ship.getLocation(), ship.getCollisionRadius(), angle);

				float startSize = 30f + (float) Math.random() * 30f;
				float endSize = 5f + (float) Math.random() * 10f;
				float brightness = (float) Math.random() * 0.5f + 0.5f;

				Color rayColor = Misc.interpolateColor(color, SOLAR, (float) Math.random());
				SpriteAPI spriteTexture = Global.getSettings().getSprite("fx", "PLSP_trails_clean");
				PLSP_TrailLine line = new PLSP_TrailLine(null, 0f, duration * 0.25f, point, angle, 0.1f, 0.2f, 0.7f, startSize, endSize, actualSpeed, angleVel, spriteTexture, rayColor, brightness);
				line.setExpiredInstant(false);
			}

			Global.getSoundPlayer().playSound("PLSP_solarwind_burst", 1f, 1f, ship.getLocation(), ZERO);
			PLSP_Util.easyRippleOut(ship.getLocation(), ship.getVelocity(), range * 1.5f, 50f, 2f);

			LocalData localData = (LocalData)engine.getCustomData().get(DATA_KEY);
			Map<Vector2f, SolarData> theData = localData.solarData;
			theData.put(new Vector2f(ship.getLocation()), new SolarData(ship, color, speed, range * 0.95f));
		}

		if (state == State.IN && PLSP_Util.timesPerSec(20f, amount)) {
			float spawnRange = (range * 0.25f + (float)Math.random() * ship.getCollisionRadius() * 5f) * (1f - effectLevel * effectLevel);
			Vector2f point = MathUtils.getRandomPointOnCircumference(ship.getLocation(), spawnRange);
			Vector2f vel = Vector2f.sub(point, ship.getLocation(), null);

			float size = (float)Math.random() * 100f + 50f;
			vel.scale(150f / size);
			vel.scale(-1f);

			float time = 0.6f;
			float sizeMult = 1f + (float)Math.random();
			float brightness = (float)Math.random() * 0.5f + 0.5f;

			engine.addNebulaParticle(point, vel, size, sizeMult, 0.5f, brightness, time, color);
		}

		if (PLSP_Util.timesPerSec(1.5f, amount)) {
			PLSP_Util.easyRippleOut(ship.getLocation(), ship.getVelocity(), range * 1.5f, 5f, 2f);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {}
	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(getString("solarwindS1"), true);
		}
		return null;
	}
}
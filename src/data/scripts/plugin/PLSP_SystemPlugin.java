package data.scripts.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PLSP_SystemPlugin extends BaseEveryFrameCombatPlugin {
	private static final String DATA_KEY = "PLSP_SystemPlugin";
	private static final Vector2f ZERO = new Vector2f();
	private CombatEngineAPI engine;

	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (engine == null || engine.isPaused()) {
			return;
		}

		final LocalData localData = (LocalData)engine.getCustomData().get(DATA_KEY);
		final Map<ShipAPI, TimeData> timeData = localData.timeData;
		final Map<Vector2f, SolarData> solarData = localData.solarData;

		if (!timeData.isEmpty()) {
			Iterator<Map.Entry<ShipAPI, TimeData>> iter = timeData.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<ShipAPI, TimeData> entry = iter.next();
				ShipAPI ship = entry.getKey();
				TimeData data = entry.getValue();

				MutableShipStatsAPI stats = ship.getMutableStats();
				String id = "TD_Slow";
				float clock = data.clock - amount;
				float effectLevel = data.effectLevel;

				if (clock > 1f) {
					effectLevel = Math.min(1f, effectLevel + amount);
				} else {
					effectLevel = Math.max(0f, effectLevel - amount);
				}

				if (clock <= 0f || (!ship.isAlive() && !ship.isHulk())) {
					stats.getTimeMult().unmodify(id);
					iter.remove();
					continue;
				}

				stats.getTimeMult().modifyMult(id, 1f - effectLevel * 0.66667f);
				ship.setJitter(id, new Color(50, 85, 90, 180), effectLevel, 3, 0f, 5f);

				data.effectLevel = effectLevel;
				data.clock = clock;
			}
		}

		if (!solarData.isEmpty()) {
			Iterator<Map.Entry<Vector2f, SolarData>> iter = solarData.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Vector2f, SolarData> entry = iter.next();
				Vector2f source = entry.getKey();
				SolarData data = entry.getValue();

				float currentDistant = data.currentDistant + amount * data.speed;
				if (currentDistant >= data.maxRange) {
					iter.remove();
					continue;
				}

				float clock = data.clock + amount;
				boolean damage = false;
				if (clock >= 0.1f) {
					damage = true;
					clock -= 0.1f;
				}

				float currentWidth = data.startingWidth * (1f + currentDistant / data.maxRange);

				for (ShipAPI target : CombatUtils.getShipsWithinRange(source, currentDistant + currentWidth)) {
					if (target.getOwner() == data.owner) continue;
					if (target.isPhased()) continue;

					Vector2f effectSource = data.getEffectingPoint(source, target.getLocation(), currentDistant);
					float rangeFromCurrent = MathUtils.getDistance(target, effectSource); // not loc here
					if (rangeFromCurrent > currentWidth) continue;

					float effectLevel = 1f - rangeFromCurrent / currentWidth;

					if (damage) {
						if (target.isDrone() || target.isFighter()) {
							engine.applyDamage(target, target.getLocation(), 55f, DamageType.HIGH_EXPLOSIVE, 0f, false, true, data.source);
							engine.addSmoothParticle(target.getLocation(), ZERO, 30f, 1f, 0.5f, data.color);
						} else {
							PLSP_Util.AOE(data.source, effectSource, 25f, DamageType.HIGH_EXPLOSIVE, target);
						}
					}

					if (target.isStation() || target.isStationModule()) continue;

					target.setAngularVelocity(target.getAngularVelocity() * (1f - 0.75f * effectLevel));
					target.getVelocity().scale(1f - 0.25f * effectLevel);

					if (PLSP_Util.timesPerSec(10f, amount)) {
						float facing = (float) Math.random() * 360f;
						if (!VectorUtils.isZeroVector(target.getVelocity())) {
							facing = VectorUtils.getFacing(target.getVelocity());
							facing += (float) Math.random() * 60f;
							facing -= (float) Math.random() * 60f;
						}

						Vector2f start = MathUtils.getPointOnCircumference(target.getLocation(), target.getCollisionRadius(), facing);
						Vector2f point = CollisionUtils.getCollisionPoint(start, target.getLocation(), target);
						if (point != null) {
							float size = (float)Math.random() * target.getCollisionRadius() * 0.5f + 30f;
							float sizeMult = 1f + (float)Math.random();

							float brightness = (float)Math.random() * 0.5f + 0.5f;
							Vector2f vel = VectorUtils.resize(new Vector2f(target.getVelocity()), -100f / size);

							engine.addNebulaParticle(point, vel, size, sizeMult, 0.75f, brightness, 0.75f, data.color);
						}
					}
				}

				for (DamagingProjectileAPI proj : PLSP_Util.getProjectilesAndMissilesWithinRange(source, currentDistant + currentWidth)) {
					if (proj.getOwner() == data.owner) continue;

					Vector2f effectSource = data.getEffectingPoint(source, proj.getLocation(), currentDistant);
					float rangeFromCurrent = MathUtils.getDistance(proj, effectSource); // not loc here
					if (rangeFromCurrent > currentWidth) continue;

					float effectLevel = 1f - rangeFromCurrent / currentWidth;

					if (damage && proj instanceof MissileAPI) {
						engine.applyDamage(proj, proj.getLocation(), 55f, DamageType.HIGH_EXPLOSIVE, 0f, false, true, data.source);
						engine.addSmoothParticle(proj.getLocation(), ZERO, 30f, 1f, 0.5f, data.color);
					}

					proj.setAngularVelocity(proj.getAngularVelocity() * (1f - 0.75f * effectLevel));
					proj.getVelocity().scale(1f - 0.25f * effectLevel);

					if (PLSP_Util.timesPerSec(10f, amount)) {
						float size = (float)Math.random() * proj.getCollisionRadius() + 15f;
						float brightness = (float)Math.random() * 0.5f + 0.5f;

						Vector2f vel = new Vector2f(proj.getVelocity());
						vel.scale(-1f);
						if (vel.length() > 100f) {
							VectorUtils.resize(vel, 100f);
						}

						engine.addSmoothParticle(proj.getLocation(), vel, size, brightness, 0.75f, data.color);
					}
				}

				data.currentDistant = currentDistant;
				data.clock = clock;
			}
		}
	}

	@Override
	public void init(CombatEngineAPI engine) {
		this.engine = engine;
		Global.getCombatEngine().getCustomData().put(DATA_KEY, new LocalData());
	}

	public static final class LocalData {
		public final Map<ShipAPI, TimeData> timeData = new HashMap<>(100);
		public final Map<Vector2f, SolarData> solarData = new HashMap<>(100);
	}
	
	public final static class TimeData {
		float clock;
		float effectLevel;

		public TimeData(float time) {
			this.clock = time;
			this.effectLevel = 0f;
		}

		public void addTime(float time) {
			this.clock += time;
		}
	}

	public final static class SolarData {
		ShipAPI source;
		Color color;
		float speed;
		float maxRange;
		int owner;

		float currentDistant;
		float startingWidth;
		float clock;

		public SolarData(ShipAPI source, Color color, float speed, float maxRange) {
			this.source = source;
			this.color = color;
			this.speed = speed;
			this.maxRange = maxRange;
			this.owner = source.getOwner();

			this.currentDistant = 0f;
			this.startingWidth = 100f;
			this.clock = 0f;
		}

		public Vector2f getEffectingPoint(Vector2f source, Vector2f targetLocation, float currentDistant) {
			float angle = VectorUtils.getAngle(source, targetLocation);
			return MathUtils.getPoint(source, currentDistant, angle);
		}
	}
}
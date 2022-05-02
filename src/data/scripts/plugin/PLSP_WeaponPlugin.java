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
import java.util.List;
import java.util.*;

public class PLSP_WeaponPlugin extends BaseEveryFrameCombatPlugin {
	private static final String DATA_KEY = "PLSP_WeaponPlugin";
	private static final Vector2f ZERO = new Vector2f();
	private CombatEngineAPI engine;

	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (engine == null || engine.isPaused()) {
			return;
		}

		final LocalData localData = (LocalData)engine.getCustomData().get(DATA_KEY);
		final Map<DamagingProjectileAPI, ClockData> clockData = localData.clockData;
		final Map<DamagingProjectileAPI, InterferometerData> interferometerData = localData.interferometerData;
		final Map<DamagingProjectileAPI, CombatEntityAPI> flakData = localData.flakData;
		final Map<Vector2f, CulverinData> culverinData = localData.culverinData;

		final List<DamagingProjectileAPI> toRemove = localData.toRemove;

		for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
			if (!engine.isEntityInPlay(projectile)) {
				continue;
			}

			String projectileID = projectile.getProjectileSpecId();
			ShipAPI ship = projectile.getSource();
			Vector2f location = projectile.getLocation();
			if (projectileID == null) {
				continue;
			}

			switch (projectileID) {
				case "PLSP_another_shot":
					if ((float)Math.random() > 0.5f) {
						Vector2f end = MathUtils.getPointOnCircumference(location, (float)Math.random() * 50f + 75f, projectile.getFacing() + 180f);
						engine.spawnEmpArcVisual(location, ship, end, null, (float) Math.random() * 5f + 5f, new Color(255, 255, 255, 100), new Color(255, 255, 255));
					}
					break;
			}

			if (projectile.getElapsed() > 0) {
				continue;
			}

			switch (projectileID) {
				case "PLSP_interferometerhyperbolic_shot":
				case "PLSP_interferometerresonant_shot":
				case "PLSP_interferometerdegeneracy_shot":
					Vector2f targetPoint = MathUtils.getPoint(projectile.getSpawnLocation(), projectile.getWeapon().getRange(), projectile.getWeapon().getCurrAngle());
					interferometerData.put(projectile, new InterferometerData(targetPoint, projectile.getWeapon().getCurrAngle()));
					break;
			}
		}

		if (!clockData.isEmpty()) {
			Iterator<Map.Entry<DamagingProjectileAPI, ClockData>> iter = clockData.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<DamagingProjectileAPI, ClockData> entry = iter.next();
				DamagingProjectileAPI projectile = entry.getKey();
				ClockData data = entry.getValue();
				float clock = data.clock + amount;

				if (projectile == null || projectile.didDamage() || !engine.isEntityInPlay(projectile)) {
					iter.remove();
					continue;
				}



				data.clock = clock;
			}
		}

		if (!interferometerData.isEmpty()) {
			Iterator<Map.Entry<DamagingProjectileAPI, InterferometerData>> iter = interferometerData.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<DamagingProjectileAPI, InterferometerData> entry = iter.next();
				DamagingProjectileAPI projectile = entry.getKey();
				InterferometerData data = entry.getValue();

				if (projectile == null || projectile.didDamage() || !engine.isEntityInPlay(projectile)) {
					iter.remove();
					continue;
				}

				if (data.secondStage) {
					float angleAcc = 5f * MathUtils.getShortestRotation(projectile.getFacing(), data.targetAngle) / data.secondStageTime;
					projectile.setFacing(projectile.getFacing() + angleAcc * amount);

				} else {
					float dif = MathUtils.getShortestRotation(projectile.getFacing(), data.targetAngle);
					float angleAcc = 10f * dif;

					projectile.setFacing(projectile.getFacing() + angleAcc * amount);
					if (Math.abs(dif) < 0.5f) {
						data.secondStage = true;
						data.targetAngle = VectorUtils.getAngle(projectile.getLocation(), data.targetPoint);

						float targetRange = MathUtils.getDistance(projectile.getLocation(), data.targetPoint);
						data.secondStageTime = targetRange / projectile.getVelocity().length();
					}

				}
			}
		}

		if (!flakData.isEmpty()) {
			Iterator<Map.Entry<DamagingProjectileAPI, CombatEntityAPI>> iter = flakData.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<DamagingProjectileAPI, CombatEntityAPI> entry = iter.next();
				DamagingProjectileAPI proj = entry.getKey();
				CombatEntityAPI target = entry.getValue();

				if (!engine.isEntityInPlay(proj) || proj.isFading()) {
					iter.remove();
					continue;
				}

				if (target == null || !engine.isEntityInPlay(target)) {
					proj.setCollisionClass(CollisionClass.PROJECTILE_FIGHTER);
					iter.remove();
					continue;
				}

				if (target.getShield() == null || !target.getShield().isOn() || !target.getShield().isWithinArc(proj.getLocation()) || MathUtils.getDistance(proj, target.getShield().getLocation()) > target.getShield().getRadius()) {
					if (CollisionUtils.isPointWithinBounds(proj.getLocation(), target)) {
						proj.setCollisionClass(CollisionClass.PROJECTILE_FIGHTER);
						iter.remove();
					}
				}
			}
		}

		if (!culverinData.isEmpty()) {
			Iterator<Map.Entry<Vector2f, CulverinData>> iter = culverinData.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Vector2f, CulverinData> entry = iter.next();
				Vector2f location = entry.getKey();
				CulverinData data = entry.getValue();
				float timer = data.timer - amount;
				float tick = data.damageTick - amount;

				if (timer <= 0f) {
					iter.remove();
					continue;
				}

				if (tick <= 0f) {
					for (MissileAPI target : CombatUtils.getMissilesWithinRange(location, data.radius))  {
						if (target.getOwner() != data.source.getOwner()) {
							float damage = target.getVelocity().length() * 0.15f + Math.abs(target.getAngularVelocity()) * 0.1f + 3f;
							engine.applyDamage(target, target.getLocation(), damage, DamageType.KINETIC, 0f, false, false, data.source);
						}
					}

					for (ShipAPI target : CombatUtils.getShipsWithinRange(location, data.radius)) {
						if (target.getOwner() != data.source.getOwner()) {
							float damage = target.getVelocity().length() * 0.15f + Math.abs(target.getAngularVelocity()) * 0.1f + 3f;
							Vector2f damageLocation = PLSP_Util.getShipCollisionPoint(location, target.getLocation(), target);
							if (damageLocation != null) engine.applyDamage(target, damageLocation, damage, DamageType.KINETIC, 0f, false, false, data.source);
						}
					}

					tick = 0.1f;
				}

				data.timer = timer;
				data.damageTick = tick;
			}
		}

		if (!toRemove.isEmpty()) {
			for (DamagingProjectileAPI remove : toRemove) {
				engine.removeEntity(remove);
			}
		}
		toRemove.clear();
	}

	@Override
	public void init(CombatEngineAPI engine) {
		this.engine = engine;
		Global.getCombatEngine().getCustomData().put(DATA_KEY, new LocalData());
		engine.addLayeredRenderingPlugin(new PLSP_RenderPlugin());
	}

	public static final class LocalData {
		private final List<DamagingProjectileAPI> toRemove = new ArrayList<>();
		private final Map<DamagingProjectileAPI, ClockData> clockData = new HashMap<>(100);
		private final Map<DamagingProjectileAPI, InterferometerData> interferometerData = new HashMap<>(100);
		public final Map<DamagingProjectileAPI, CombatEntityAPI> flakData = new HashMap<>(100);
		public final Map<Vector2f, CulverinData> culverinData = new HashMap<>(100);
	}

	private final static class InterferometerData {
		Vector2f targetPoint;
		float targetAngle;
		float secondStageTime;
		boolean secondStage;

		private InterferometerData(Vector2f targetPoint, float targetAngle) {
			this.targetPoint = targetPoint;
			this.targetAngle = targetAngle;
			this.secondStage = false;
		}
	}

	public final static class CulverinData {
		ShipAPI source;
		float radius;

		float timer;
		float damageTick;

		public CulverinData(ShipAPI source, float radius, float timer) {
			this.source = source;
			this.radius = radius;

			this.timer = timer;
			this.damageTick = 0.1f;
		}
	}

	private final static class ClockData {
		float clock;

		private ClockData() {
			this.clock = 0f;
		}
	}
}
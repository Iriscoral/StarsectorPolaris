package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_ShellSystemAI implements ShipSystemAIScript {
	private ShipSystemAPI system;
	private ShipAPI ship;

	private final IntervalUtil tracker = new IntervalUtil(0.5f, 0.6f);

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (system.isActive()) return;
		tracker.advance(amount);
		if (!tracker.intervalElapsed()) return;
		if (!AIUtils.canUseSystemThisFrame(ship)) return;

		float accumulator = estimateIncomingDamage(ship, ship.getCollisionRadius() + 1000f, 4f);
		if (accumulator > 1200 || accumulator > (ship.getMaxFlux() - ship.getCurrFlux()) * 0.5f
				|| accumulator > ship.getHitpoints() * 0.5f) ship.useSystem();
	}

	private static float estimateIncomingDamage(ShipAPI ship, float range, float damageWindowSeconds) {
		float accumulator = 0f;

		for (BeamAPI beam : Global.getCombatEngine().getBeams()) {
			if (beam.getDamageTarget() != ship) continue;

			float factor = beam.getDamage().getType() == DamageType.FRAGMENTATION ? 0.25f : 1f;
			accumulator += beam.getDamage().getDamage() * damageWindowSeconds * factor;
		}

		for (DamagingProjectileAPI proj : PLSP_Util.getEnemyProjectilesAndMissilesWithinRange(ship.getLocation(), range, ship.getOwner())) {

			Vector2f endPoint = new Vector2f(proj.getVelocity());
			endPoint.scale(damageWindowSeconds);
			Vector2f.add(endPoint, proj.getLocation(), endPoint);

			if (PLSP_Util.getShipCollisionPoint(proj.getLocation(), endPoint, ship) == null) continue;

			float factor = proj.getDamageType() == DamageType.FRAGMENTATION ? 0.25f : 1f;
			accumulator += proj.getDamageAmount() * factor + proj.getEmpAmount() * 0.5f;// * Math.max(0, Math.min(1, Math.pow(1 - MathUtils.getDistance(proj, ship) / safeDistance, 2)));
		}

		return accumulator;
	}
}
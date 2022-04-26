package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.PLSP_Util;
import data.shipsystems.scripts.PLSP_SolarWindStats;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_SolarWindAI implements ShipSystemAIScript {
	private CombatEngineAPI engine;
	private ShipwideAIFlags flags;
	private ShipAPI ship;
	private ShipSystemAPI system;
	private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.2f);

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.flags = flags;
		this.engine = engine;
	}
	
	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (engine.isPaused() || !ship.isAlive()) {
			return;
		}

		tracker.advance(amount);
		if (!tracker.intervalElapsed()) {
			return;
		}

		float range = 0.95f * PLSP_SolarWindStats.getRange(ship);
		float[] pSet = compareFPWorth(ship, range);
		if (pSet == null) {
			return;
		}

		float fPoint = pSet[0];
		float ePoint = pSet[1];
		boolean use = fPoint < ePoint || (ePoint > 0 && ship.getFluxTracker().getFluxLevel() < 0.25f);
		if (!use) {
			float damage = 0f;
			for (DamagingProjectileAPI proj : AIUtils.getNearbyEnemyMissiles(ship, range)) {
				damage += proj.getDamageAmount() + proj.getEmpAmount() * 0.25f;
				if (proj.getDamageType() == DamageType.FRAGMENTATION) {
					damage -= proj.getDamageAmount() * 0.75f;
				}
			}
			use = damage >= 4000f;
		}
		if (use && AIUtils.canUseSystemThisFrame(ship)) {
			ship.useSystem();
		}
	}

	private static float[] compareFPWorth(ShipAPI ship, float range) {
		float ePoint = 0;
		float fPoint = 0;

		for (ShipAPI tmp : Global.getCombatEngine().getShips()) {
			if (MathUtils.isWithinRange(ship, tmp, range) && !tmp.isHulk() && !tmp.isShuttlePod() && CombatUtils.isVisibleToSide(tmp, ship.getOwner())) {
				float colDist = ship.getCollisionRadius() + tmp.getCollisionRadius();
				float distance = Math.max(0, MathUtils.getDistance(ship, tmp) - colDist);
				float maxRange = Math.max(1, range - colDist);
				if (tmp.getOwner() != ship.getOwner()) {
					ePoint += PLSP_Util.getFPStrength(tmp) * (1 - distance / maxRange);
				} else {
					fPoint += PLSP_Util.getFPStrength(tmp) * (1 - distance / maxRange);
				}
			}
		}

		if (ePoint == 0f) {
			return null;
		}

		float[] pSet = new float[2];
		pSet[0] = fPoint;
		pSet[1] = ePoint;
		return pSet;
	}
}
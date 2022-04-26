package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.PLSP_Util;
import data.shipsystems.scripts.PLSP_EventDisturbStats;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_EventDisturbAI implements ShipSystemAIScript {
	private CombatEngineAPI engine;
	private ShipAPI ship;
	private ShipSystemAPI system;

	private final IntervalUtil tracker = new IntervalUtil(0.5f, 0.6f);

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.engine = engine;
		this.system = system;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (engine == null || engine.isPaused()) {
			return;
		}

		tracker.advance(amount);
		if (tracker.intervalElapsed()) {
			ShipAPI alliedTarget = PLSP_EventDisturbStats.getTarget(ship);
			if (alliedTarget == null) return;

			boolean doNotUse = alliedTarget.getShipTarget() == null || alliedTarget.getShipTarget().getOwner() == alliedTarget.getOwner();
			if (doNotUse) doNotUse = AIUtils.getNearbyEnemies(alliedTarget, 1600f).isEmpty();
			if (doNotUse) doNotUse = PLSP_Util.getEnemyProjectilesAndMissilesWithinRange(alliedTarget.getLocation(), 1000f, alliedTarget.getOwner()).isEmpty();

			if (AIUtils.canUseSystemThisFrame(ship) && !doNotUse) {
				ship.useSystem();
			}
		}
	}
}

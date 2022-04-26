package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.scripts.PLSP_SensorDisturbStats;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_SensorDisturbAI implements ShipSystemAIScript {
	private CombatEngineAPI engine;
	private ShipwideAIFlags flags;
	private ShipAPI ship;
	private ShipSystemAPI system;
	private final IntervalUtil tracker = new IntervalUtil(0.1f, 0.2f);

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

		boolean use = target != null && target.isAlive() && target.getOwner() != ship.getOwner() &&
				MathUtils.isWithinRange(ship, target, PLSP_SensorDisturbStats.getRange(ship)) && !target.isFighter() && !target.isDrone();
		if (use && ship.getFluxTracker().getFluxLevel() <= 0.7f && AIUtils.canUseSystemThisFrame(ship)) {
			ship.useSystem();
		}
	}
}
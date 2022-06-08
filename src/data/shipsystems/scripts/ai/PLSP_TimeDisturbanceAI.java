package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.scripts.PLSP_TimeDisturbanceStats;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_TimeDisturbanceAI implements ShipSystemAIScript {
	private CombatEngineAPI engine;
	private ShipAPI ship;
	private ShipSystemAPI system;

	private final IntervalUtil tracker = new IntervalUtil(0.1f, 0.2f);

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
			float range = PLSP_TimeDisturbanceStats.getRange(ship);
			boolean use = target != null && target.isAlive() && target.getOwner() != ship.getOwner() &&
					MathUtils.isWithinRange(ship, target, range) && !target.isFighter() && !target.isDrone();

			if (use && AIUtils.canUseSystemThisFrame(ship)) {
				ship.useSystem();
			}
		}
	}
}

package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.geom.Line2D;

public class PLSP_AxisDriveAI implements ShipSystemAIScript {
	private CombatEngineAPI engine;
	private ShipwideAIFlags flags;
	private ShipAPI ship;
	private ShipSystemAPI system;
	private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.3f);

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
		if (tracker.intervalElapsed()) {
			if (!AIUtils.canUseSystemThisFrame(ship)) return;

			boolean wantActive = (flags.hasFlag(AIFlags.PURSUING)
					|| flags.hasFlag(AIFlags.RUN_QUICKLY)
					|| flags.hasFlag(AIFlags.BACKING_OFF))
					&& !flags.hasFlag(AIFlags.WANTED_TO_SLOW_DOWN)
					&& !flags.hasFlag(AIFlags.AVOIDING_BORDER);

			if (wantActive) {
				float range = 800f;
				if (target != null) {
					range = Math.min(range,  MathUtils.getDistance(ship, target));
				}

				if (!noFriendlyWithinArc(ship, range)) {
					wantActive = false;
				}
			}

			if (!system.isActive() && wantActive) {
				ship.useSystem();
			}
		}
	}

	private static boolean noFriendlyWithinArc(ShipAPI entity, float range) {

		for (ShipAPI tmp : AIUtils.getNearbyAllies(entity, range)) {
			if (tmp.isPhased() || tmp.isFighter() || tmp.isDrone()) {
				continue;
			}

			if (isWithinArc(entity, tmp, 15f)) {
				return false;
			}
		}

		return true;
	}

	public static boolean isWithinArc(CombatEntityAPI source, CombatEntityAPI target, float arc) {

		Vector2f targetLocation = target.getLocation();
		Vector2f sourceLocation = source.getLocation();
		float range = 1000f; // ?

		Vector2f endArcLeft = MathUtils.getPointOnCircumference(sourceLocation, range, -arc);
		Vector2f endArcRight = MathUtils.getPointOnCircumference(sourceLocation, range, arc);
		float radSquared = target.getCollisionRadius() * target.getCollisionRadius();
		return (Line2D.ptSegDistSq(
				sourceLocation.x,
				sourceLocation.y,
				endArcLeft.x,
				endArcLeft.y,
				targetLocation.x,
				targetLocation.y) <= radSquared)
				|| (Line2D.ptSegDistSq(
				sourceLocation.x,
				sourceLocation.y,
				endArcRight.x,
				endArcRight.y,
				targetLocation.x,
				targetLocation.y) <= radSquared);
	}
}
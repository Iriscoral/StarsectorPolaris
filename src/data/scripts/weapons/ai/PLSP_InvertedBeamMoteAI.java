package data.scripts.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PLSP_InvertedBeamMoteAI implements MissileAIPlugin {

	private static final float MAX_HARD_AVOID_RANGE = 200;
	private static final float AVOID_RANGE = 50;

	private final MissileAPI missile;
	private final CombatEntityAPI notAttack;

	private final IntervalUtil tracker = new IntervalUtil(0.05f, 0.1f);
	private final IntervalUtil updateListTracker = new IntervalUtil(0.05f, 0.1f);
	private final List<CombatEntityAPI> hardAvoidList = new ArrayList<>();

	private final float r;
	private float elapsed;

	private CombatEntityAPI target;

	public PLSP_InvertedBeamMoteAI(MissileAPI missile, CombatEntityAPI notAttack) {
		this.missile = missile;
		this.notAttack = notAttack;

		this.r = (float)Math.random();
		this.elapsed = -(float)Math.random() * 0.5f;

		updateHardAvoidList();
	}

	private void updateHardAvoidList() {
		hardAvoidList.clear();

		CollisionGridAPI grid = Global.getCombatEngine().getAiGridShips();
		Iterator<Object> iter = grid.getCheckIterator(missile.getLocation(), MAX_HARD_AVOID_RANGE * 2f, MAX_HARD_AVOID_RANGE * 2f);
		while (iter.hasNext()) {
			Object o = iter.next();
			if (!(o instanceof ShipAPI)) continue;

			ShipAPI ship = (ShipAPI) o;
			if (ship.isFighter()) continue;

			hardAvoidList.add(ship);
		}

		grid = Global.getCombatEngine().getAiGridAsteroids();
		iter = grid.getCheckIterator(missile.getLocation(), MAX_HARD_AVOID_RANGE * 2f, MAX_HARD_AVOID_RANGE * 2f);
		while (iter.hasNext()) {
			Object o = iter.next();
			if (!(o instanceof CombatEntityAPI)) continue;

			CombatEntityAPI asteroid = (CombatEntityAPI) o;
			hardAvoidList.add(asteroid);
		}
	}

	private void doFlocking() {
		CombatEntityAPI spawnSource = notAttack;
		if (spawnSource == null) return;

		CombatEngineAPI engine = Global.getCombatEngine();

		float sourceRejoin = spawnSource.getCollisionRadius() + 200f;
		float sourceRepel = spawnSource.getCollisionRadius() + 50f;
		float sourceCohesion = spawnSource.getCollisionRadius() + 600f;

		float sin = (float) Math.sin(elapsed * 1f);
		float mult = 1f + sin * 0.25f;
		float avoidRange = AVOID_RANGE * mult;

		Vector2f total = new Vector2f();
		for (CombatEntityAPI other : hardAvoidList) {
			float dist = Misc.getDistance(missile.getLocation(), other.getLocation());
			float hardAvoidRange = other.getCollisionRadius() + avoidRange + 50f;
			if (dist < hardAvoidRange) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(other.getLocation(), missile.getLocation()));
				float f = 1f - dist / (hardAvoidRange);
				dir.scale(f * 5f);
				Vector2f.add(total, dir, total);
			}
		}

		if (spawnSource instanceof ShipAPI && ((ShipAPI)spawnSource).isAlive()) {
			float dist = Misc.getDistance(missile.getLocation(), spawnSource.getLocation());
			if (dist > sourceRejoin) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.getLocation(), spawnSource.getLocation()));
				float f = dist / (sourceRejoin  + 400f) - 1f;
				dir.scale(f * 0.5f);

				Vector2f.add(total, dir, total);
			}

			if (dist < sourceRepel) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(spawnSource.getLocation(), missile.getLocation()));
				float f = 1f - dist / sourceRepel;
				dir.scale(f * 5f);
				Vector2f.add(total, dir, total);
			}

			if (dist < sourceCohesion && spawnSource.getVelocity().length() > 20f) {
				Vector2f dir = new Vector2f(spawnSource.getVelocity());
				Misc.normalise(dir);
				float f = 1f - dist / sourceCohesion;
				dir.scale(f * 1f);
				Vector2f.add(total, dir, total);
			}

			// if not strongly going anywhere, circle the spawnSource ship; only kicks in for lone motes
			if (total.length() <= 0.05f) {
				float offset = r > 0.5f ? 90f : -90f;
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(
						Misc.getAngleInDegrees(missile.getLocation(), spawnSource.getLocation()) + offset);
				float f = 1f;
				dir.scale(f * 1f);
				Vector2f.add(total, dir, total);
			}
		}

		if (total.length() > 0f) {
			float dir = Misc.getAngleInDegrees(total);
			engine.headInDirectionWithoutTurning(missile, dir, 10000);

			if (r > 0.5f) {
				missile.giveCommand(ShipCommand.TURN_LEFT);
			} else {
				missile.giveCommand(ShipCommand.TURN_RIGHT);
			}
			missile.getEngineController().forceShowAccelerating();
		}
	}

	@Override
	public void advance(float amount) {
		if (missile.isFizzling()) return;
		if (missile.getSource() == null) return;

		updateListTracker.advance(amount);
		if (updateListTracker.intervalElapsed()) {
			updateHardAvoidList();
		}

		elapsed += amount;
		if (elapsed < 0.5f) return;

		if (isTargetValid()) {
			CombatEngineAPI engine = Global.getCombatEngine();
			Vector2f targetLoc = engine.getAimPointWithLeadForAutofire(missile, 1.5f, target, 50);
			engine.headInDirectionWithoutTurning(missile, Misc.getAngleInDegrees(missile.getLocation(), targetLoc), 10000);
			if (r > 0.5f) {
				missile.giveCommand(ShipCommand.TURN_LEFT);
			} else {
				missile.giveCommand(ShipCommand.TURN_RIGHT);
			}
			missile.getEngineController().forceShowAccelerating();
		} else {
			doFlocking();
		}

		tracker.advance(amount);
		if (tracker.intervalElapsed()) {
			acquireNewTargetIfNeeded();
		}
	}

	private boolean isTargetValid() {
		if (target == null) return false;
		if (target.getOwner() == missile.getOwner()) return false;

		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.isEntityInPlay(target)) return false;

		if (target instanceof ShipAPI) {
			ShipAPI shipTarget = (ShipAPI)target;
			if (!shipTarget.isAlive() || shipTarget.isPhased()) return false;
		}

		return true;
	}

	private void acquireNewTargetIfNeeded() {

		// want to: target nearest missile

		float minDist = Float.MAX_VALUE;
		CombatEntityAPI closest = null;
		for (MissileAPI other : AIUtils.getNearbyEnemyMissiles(missile, 1000f)) {
			float distToTarget = Misc.getDistance(missile.getLocation(), other.getLocation());

			if (distToTarget < minDist) {
				closest = other;
				minDist = distToTarget;
			}
		}

		if (closest != null) {
			target = closest;
			return;
		}

		for (ShipAPI other : AIUtils.getNearbyEnemies(missile, 1000f)) {

			if (other == notAttack) continue;
			float distToTarget = Misc.getDistance(missile.getLocation(), other.getLocation());

			if (distToTarget < minDist) {
				closest = other;
				minDist = distToTarget;
			}
		}

		target = closest;
	}

	public CombatEntityAPI getTarget() {
		return target;
	}

	public void setTarget(CombatEntityAPI target) {
		this.target = target;
	}
}
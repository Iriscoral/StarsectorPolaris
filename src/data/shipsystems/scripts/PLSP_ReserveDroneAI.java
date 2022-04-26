package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.geom.Line2D;

public class PLSP_ReserveDroneAI implements ShipAIPlugin {
	private final ShipAPI ship;
	private final ShipAPI target;
	private final ShipAPI motherShip;
	private final WeaponSlotAPI spawnSlot;
	private boolean returning = false;
	private boolean done = false;
	private float alpha = 0.1f;
	private float doNotMove = 1f;
	private float doNotAttack = 3f;
	private static final float DEFAULT_ACC_THRESHOLD = 30f;
	private static final float DEFAULT_FACING_THRESHOLD = 3f;

	public PLSP_ReserveDroneAI(ShipAPI ship, ShipAPI target, ShipAPI motherShip, WeaponSlotAPI spawnSlot) {
		this.ship = ship;
		this.target = target;
		this.motherShip = motherShip;
		this.spawnSlot = spawnSlot;
	}

	@Override
	public void advance(float amount) {
		ship.setExtraAlphaMult(alpha);
		if (alpha == 0f) {
			Global.getCombatEngine().removeEntity(ship);
		}

		if (done) {
			alpha = Math.max(alpha - amount * 2f, 0f);
			ship.giveCommand(ShipCommand.DECELERATE, null, 0);
			return;
		}

		if (returning) {
			if (!motherShip.isAlive()) {
				Global.getCombatEngine().applyDamage(ship, ship.getLocation(), 100000f, DamageType.ENERGY, 0f, true, true, ship);
				done = true;
				return;
			}

			Vector2f spawnPoint = spawnSlot.computePosition(motherShip);
			returnMove(spawnPoint);
			float distance = MathUtils.getDistance(ship.getLocation(), spawnPoint);
			if (distance <= 25f) {
				done = true;
				motherShip.getSystem().setCooldownRemaining(motherShip.getSystem().getCooldownRemaining() - 10f);
			}
			return;
		}

		if (target == null || !Global.getCombatEngine().isEntityInPlay(target) || !target.isAlive()) {
			returning = true;
			return;
		}

		alpha = Math.min(alpha + amount, 1f);

		doNotMove -= amount;
		if (doNotMove <= 0f) aggressiveMove(target.getLocation());

		doNotAttack -= amount;
		boolean ammoConsumedAll = true;
		for (int i = 0; i < ship.getWeaponGroupsCopy().size(); i++) {
			WeaponGroupAPI group = ship.getWeaponGroupsCopy().get(i);
			for (WeaponAPI weapon : group.getWeaponsCopy()) {
				if (!weapon.usesAmmo() || weapon.getAmmo() == 0) continue;

				boolean shouldFire = weapon.hasAIHint(WeaponAPI.AIHints.DO_NOT_AIM) || isWithinArc(target, weapon);
				if (shouldFire && doNotAttack <= 0f) {
					ship.giveCommand(ShipCommand.FIRE, null, i);
					ship.giveCommand(ShipCommand.SELECT_GROUP, null, i);
				}

				if (weapon.getAmmo() > 0) {
					ammoConsumedAll = false;
				}
			}
		}

		if (ammoConsumedAll) {
			returning = true;
		}
	}

	private void aggressiveMove(Vector2f location) {
		turnToward(location);
		strafeToward(location);

		float degreeAngle = VectorUtils.getAngle(ship.getLocation(), location);
		float angleDif = MathUtils.getShortestRotation(ship.getFacing(), degreeAngle);
		if (Math.abs(angleDif) < DEFAULT_ACC_THRESHOLD) {
			ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
		}
	}

	private void returnMove(Vector2f location) {
		turnToward(location);

		float degreeAngle = VectorUtils.getAngle(ship.getLocation(), location);
		float angleDif = MathUtils.getShortestRotation(ship.getFacing(), degreeAngle);
		if (Math.abs(angleDif) < DEFAULT_ACC_THRESHOLD) {
			ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
		} else {
			ship.giveCommand(ShipCommand.DECELERATE, null, 0);
		}
	}

	private void turnToward(Vector2f location) {
		float degreeAngle = VectorUtils.getAngle(ship.getLocation(), location);
		float angleDif = MathUtils.getShortestRotation(ship.getFacing(), degreeAngle);

		if ((Math.abs(angleDif) < DEFAULT_FACING_THRESHOLD)) return;

		ShipCommand direction = angleDif > 0 ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
		ship.giveCommand(direction, null, 0);
	}

	private void strafeToward(Vector2f location) {
		float degreeAngle = VectorUtils.getAngle(ship.getLocation(), location);
		float angleDif = MathUtils.getShortestRotation(ship.getFacing(), degreeAngle);

		if ((Math.abs(angleDif) < DEFAULT_FACING_THRESHOLD)) return;

		ShipCommand direction = angleDif < 0 ? ShipCommand.STRAFE_LEFT : ShipCommand.STRAFE_RIGHT;
		ship.giveCommand(direction, null, 0);
	}

	private static boolean isWithinArc(CombatEntityAPI entity, WeaponAPI weapon) {
		// Check if weapon is in range
		if (!MathUtils.isWithinRange(entity, weapon.getLocation(), weapon.getRange())) {
			return false;
		}

		// Check if weapon is aimed at the target's center
		if (weapon.distanceFromArc(entity.getLocation()) == 0) {
			return true;
		}

		// Check if weapon is aimed at any part of the target
		Vector2f target = entity.getLocation();
		Vector2f wep = weapon.getLocation();
		Vector2f endArc = MathUtils.getPointOnCircumference(wep, weapon.getRange(),
				weapon.getCurrAngle());
		float radSquared = entity.getCollisionRadius() * entity.getCollisionRadius() * 0.15f; // Use 0.15

		// Check if target is partially in weapon arc
		return Line2D.ptSegDistSq(
				wep.x,
				wep.y,
				endArc.x,
				endArc.y,
				target.x,
				target.y) <= radSquared;
	}

	@Override
	public boolean needsRefit() {
		return false;
	}

	@Override
	public void setDoNotFireDelay(float amount) {}

	@Override
	public void forceCircumstanceEvaluation() {}

	@Override
	public void cancelCurrentManeuver() {}

	private final ShipwideAIFlags AIFlags = new ShipwideAIFlags();
	private final ShipAIConfig AIConfig = new ShipAIConfig();

	@Override
	public ShipwideAIFlags getAIFlags() {
		return AIFlags;
	}

	@Override
	public ShipAIConfig getConfig() {
		return AIConfig;
	}
}
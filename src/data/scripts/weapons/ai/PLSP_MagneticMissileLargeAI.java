// by Deathfly
package data.scripts.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.util.MagicLensFlare;
import data.scripts.util.PLSP_ColorData;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

public class PLSP_MagneticMissileLargeAI implements MissileAIPlugin, GuidedMissileAI {

	//initialization variable
	private CombatEngineAPI engine;
	private final MissileAPI missile;
	private CombatEntityAPI target;
	private static final Vector2f zero = new Vector2f(0f, 0f);
	//adjustable DATA
	// Distance under witch the missile cease to lead the target aim directly for it
	private final float damping = 0.1f;
	private int mirvAmount = 0;
	private float mirvCooldown = 2f;
	private final float accThreshold = 7.5f;

	//////////////////////
	//  DATA COLLECTING //
	//////////////////////
	public PLSP_MagneticMissileLargeAI(MissileAPI missile, ShipAPI launchingShip) {
		this.missile = missile;

		setTarget(assignCurrentTarget(missile));
		if (getTarget() == null) {
			reAssignTarget(missile);
		}
	}

	//////////////////////
	//   MAIN AI LOOP   //
	//////////////////////
	@Override
	public void advance(float amount) {
		if (engine != Global.getCombatEngine()) {
			this.engine = Global.getCombatEngine();
		}

		//cancelling IF: skip the AI if the game is paused, the missile is engineless or fading
		if (Global.getCombatEngine().isPaused() || missile.isFading() || missile.isFizzling()) {
			return;
		}

		//Current Target Check
		if (target == null // unset
				|| (target instanceof ShipAPI && !((ShipAPI) target).isAlive()) // dead
				|| (target instanceof ShipAPI && ((ShipAPI) target).isDrone()) // is drone
				|| (missile.getOwner() == target.getOwner()) // friendly
				|| !Global.getCombatEngine().isEntityInPlay(target) // completely removed
				|| (target instanceof ShipAPI && ((ShipAPI) target).isPhased())) { // phased out
			setTarget(reAssignTarget(missile));
			return;
		}

		Vector2f mLoc = missile.getLocation();
		Vector2f tLoc = target.getLocation();
		float mFacing = missile.getFacing();

		//debug line.
		// aimAngle = angle deviate from the lead direction
		float aimAngle = tLoc == null ? 0 : MathUtils.getShortestRotation(mFacing, VectorUtils.getAngle(mLoc, tLoc));
		boolean alwaysAcc = Math.abs(aimAngle) < accThreshold;

		// Universal Missile Attitude Control (for test purpose ONLY)
		int Thrust = 0, Turn = 0, Strafe = 0;
		// Regular Missile Heading Control
		if (aimAngle < 0) {
			Turn = 1;
		} else if (aimAngle > 0) {
			Turn = -1;
		}
		if (Math.abs(aimAngle) < Math.abs(missile.getAngularVelocity()) * damping) {
			missile.setAngularVelocity(aimAngle / damping);
		}

		// Regular Missile Course Correct
		float MFlightAng = VectorUtils.getAngle(new Vector2f(), missile.getVelocity());
		float MFlightCC = MathUtils.getShortestRotation(mFacing, MFlightAng);
		if (Math.abs(aimAngle) < 5) {
			if (MFlightCC < -20) {
				Strafe = 1;
			} else if (MFlightCC > 20) {
				Strafe = -1;
			}
		}

		// Regular Missile ACCELERATE Control
		//Missile ACCELERATE Control with Evading Maneuver
		if (alwaysAcc) {
			Thrust = 1;
		}
		if (Thrust == 1) {
			missile.giveCommand(ShipCommand.ACCELERATE);
		} else if (Thrust == -1) {
			missile.giveCommand(ShipCommand.ACCELERATE_BACKWARDS);
		}
		if (Turn == 1) {
			missile.giveCommand(ShipCommand.TURN_RIGHT);
		} else if (Turn == -1) {
			missile.giveCommand(ShipCommand.TURN_LEFT);
		}
		if (Strafe == 1) {
			missile.giveCommand(ShipCommand.STRAFE_LEFT);
		} else if (Strafe == -1) {
			missile.giveCommand(ShipCommand.STRAFE_RIGHT);
		}

		if (mirvCooldown > 0f) {
			mirvCooldown -= amount;
		}

		if (alwaysAcc && MathUtils.getDistance(target, missile) <= 500f && mirvCooldown <= 0f) {
			if (mirvAmount == 0) {
				mirv(missile);
				mirvCooldown = 1f;
				mirvAmount++;
			} else {
				mirv(missile);

				engine.spawnExplosion(missile.getLocation(), zero, EFFECT, missile.getCollisionRadius(), 1f);
				engine.removeEntity(missile);
			}
		}
	}

	private static final Color EFFECT = new Color(80, 225, 255, 200);
	private void mirv(MissileAPI missile) {
		Vector2f spawnPoint = MathUtils.getPoint(missile.getLocation(), 10f, missile.getFacing());
		for (int i = 0; i < 2; i++) {
			float angle = missile.getFacing() + (float)Math.random() * (float)Math.random() * 10f * (i == 0 ? 1f : -1f);
			DamagingProjectileAPI projectile = (DamagingProjectileAPI)engine.spawnProjectile(missile.getSource(), missile.getWeapon(), "PLSP_magnetic1", spawnPoint, angle, null);

			float vel = 0.8f + (float)Math.random() * 0.8f;
			projectile.getVelocity().scale(vel);

			for (int i2 = 0; i2 < 2; i2++) {
				float angle2 = missile.getFacing() + (float)Math.random() * 15f - (float)Math.random() * 15f;
				DamagingProjectileAPI projectile2 = (DamagingProjectileAPI)engine.spawnProjectile(missile.getSource(), missile.getWeapon(), "PLSP_magnetic2", spawnPoint, angle2, null);

				float vel2 = 0.6f + (float)Math.random() * 1.6f;
				projectile2.getVelocity().scale(vel2);
			}
		}

		Global.getSoundPlayer().playSound("PLSP_magneticl_burst", 1f, 1f, missile.getLocation(), zero);
		MagicLensFlare.createSharpFlare(Global.getCombatEngine(), missile.getSource(), spawnPoint, 4f, 150f, 20f, PLSP_ColorData.BRIGHT_EMP_ARC_FRINGE, PLSP_ColorData.BRIGHT_EMP_ARC_CORE);
	}

	//////////////////////
	//	TARGETTING	//
	//////////////////////
	//will be called when firing
	private CombatEntityAPI assignCurrentTarget(MissileAPI missile) {
		ShipAPI source = missile.getSource();
		ShipAPI currentTarget = source.getShipTarget();

		// try "lock'n'fire" targeting behavior frist. 
		// get target form ship,
		if (currentTarget != null
				&& currentTarget.isAlive()
				&& currentTarget.getOwner() != missile.getOwner()) {
			//return the ship's target if it's valid
			return currentTarget;
		} else {
			// If we don't got any valid target form ship, try "fire at target by clicking on them" behavior.
			// get nearest target form cursor
			List<ShipAPI> directTargets = CombatUtils.getShipsWithinRange(source.getMouseTarget(), 50f);
			if (!directTargets.isEmpty()) {
				Collections.sort(directTargets, new CollectionUtils.SortEntitiesByDistance(source.getMouseTarget()));
				for (ShipAPI tmp : directTargets) {
					if (tmp.isAlive() && tmp.getOwner() != source.getOwner()) {
						return tmp;
					}
				}
			}
		}

		//still no target?
		return null;
	}

	//will be called if current target is invalid
	private CombatEntityAPI reAssignTarget(MissileAPI missile) {
		ShipAPI newtarget = null;
		ShipAPI source = missile.getSource();
		int side = source.getOwner();
		float searchRange = missile.getMaxSpeed() * 2f * (missile.getMaxFlightTime() - missile.getElapsed());

		float distance, closestDistance = Float.MAX_VALUE;
		for (ShipAPI tmp : AIUtils.getNearbyEnemies(missile, searchRange)) {
			if (!CombatUtils.isVisibleToSide(tmp, side)) {
				continue;
			}

			float mod = 0f;
			if (tmp.isFighter()) {
				mod = 10f * searchRange;
			} else if (tmp.isDrone()) {
				mod = 20f * searchRange;
			}

			distance = MathUtils.getDistance(tmp, missile.getLocation()) + mod;
			if (distance < closestDistance) {
				newtarget = tmp;
				closestDistance = distance;
			}
		}

		if (newtarget == null) {
			newtarget = AIUtils.getNearestEnemy(missile);
			if (newtarget != null && !CombatUtils.isVisibleToSide(newtarget, side)) {
				newtarget = null;
			}
		}

		return newtarget;
	}

	@Override
	public CombatEntityAPI getTarget() {
		return target;
	}

	@Override
	public void setTarget(CombatEntityAPI target) {
		this.target = target;
	}
}
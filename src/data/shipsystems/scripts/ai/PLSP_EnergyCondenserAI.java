package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_EnergyCondenserAI implements ShipSystemAIScript {
	private CombatEngineAPI engine;
	private ShipwideAIFlags flags;
	private ShipAPI ship;
	private ShipSystemAPI system;

	private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.3f);
	private float keepOnDur = 0f;
	private float sinceLastOn = 0f;

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

		if (keepOnDur > 0f) {
			keepOnDur -= amount;
			return;
		}

		tracker.advance(amount);
		if (!tracker.intervalElapsed()) {
			return;
		}

		boolean keepOn = target != null && target.isAlive() && target.getOwner() != ship.getOwner();
		if (keepOn) keepOn = isInBurst(ship);

		boolean keepOff = ship.getHardFluxLevel() > 0.8f;
		boolean toggleOff = !keepOn && sinceLastOn > 2f;
		if (keepOff) {
			keepOn = false;
			toggleOff = true;
		}

		// if (!AIUtils.canUseSystemThisFrame(ship)) return;

		if (!system.isOn()) {
			if (keepOn) {
				ship.useSystem();
				keepOnDur = 2f;
				sinceLastOn = 0f;
			}
		} else {
			if (!keepOn) {
				sinceLastOn += tracker.getIntervalDuration();
			}
			if (toggleOff) {
				ship.useSystem();
			}
		}
	}

	private static boolean isInBurst(ShipAPI ship) {
		int burstLevel = 0;
		int totalLevel = 0;

		for (WeaponAPI weapon : ship.getUsableWeapons()) {
			if (weapon.getType() != WeaponAPI.WeaponType.MISSILE && (!weapon.isBeam() || weapon.isBurstBeam())) {
				boolean useCheck = weapon.getChargeLevel() > 0f || weapon.isFiring();
				switch (weapon.getSize()) {
					case LARGE:
						if (useCheck) return true;
					case MEDIUM:
						if (useCheck) burstLevel += 3;
						totalLevel += 3;
						break;
					case SMALL:
						if (useCheck) burstLevel++;
						totalLevel++;
						break;
					default:
						break;
				}
			}
		}

		return burstLevel >= 6 || (float)burstLevel / totalLevel > 0.5f;
	}
}
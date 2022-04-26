package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

public class PLSP_AxisDecEveryFrameEffect implements EveryFrameWeaponEffectPlugin {

	private float lastLevel = 0f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (weapon == null || weapon.getShip() == null) {
			return;
		}

		WeaponAPI decora = null;
		for (WeaponAPI temp : weapon.getShip().getAllWeapons()) {
			if (temp.getId().contentEquals("PLSP_axis_dec")) {
				decora = temp;
				decora.getAnimation().setFrame(1);
				break;
			}
		}

		if (decora == null) {
			return;
		}

		if (!weapon.getShip().isAlive()) {
			decora.getAnimation().setAlphaMult(0f);
			return;
		}

		float effect = 0f;
		if (weapon.isFiring() || weapon.getChargeLevel() > 0) {
			if (weapon.getChargeLevel() >= lastLevel) {
				effect = Math.min((float)Math.pow(weapon.getChargeLevel(), 3) * 1.2f, 1f);
			} else {
				effect = Math.max(weapon.getChargeLevel() * 3f - 2f, 0f);
			}
		}

		decora.getAnimation().setAlphaMult(effect);
		lastLevel = weapon.getChargeLevel();
	}
}
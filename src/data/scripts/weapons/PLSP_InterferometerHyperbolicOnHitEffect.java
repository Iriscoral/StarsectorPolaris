package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_InterferometerHyperbolicOnHitEffect implements OnHitEffectPlugin {
	private static final Vector2f ZERO = new Vector2f();

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		float volume = 1f;
		if (target instanceof MissileAPI || shieldHit) volume = 0.5f;

		Global.getSoundPlayer().playSound("PLSP_interferometerhyperbolic_explode", 1f, volume, point, ZERO);
	}
}
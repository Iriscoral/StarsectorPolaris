package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import data.scripts.plugin.PLSP_WeaponPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Map;

public class PLSP_ICFlakOnHitEffect implements OnHitEffectPlugin {
	private static final String DATA_KEY = "PLSP_WeaponPlugin";
	private static final Color PARTICLE_COLOR = new Color(255, 255, 155, 255);
	private static final Color EXPLOSION_COLOR = new Color(170, 255, 255, 155);

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (target == null) return;
		DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f, 65f, 40f,
				projectile.getDamageAmount(), projectile.getDamageAmount() * 0.5f, CollisionClass.PROJECTILE_FF,
				CollisionClass.PROJECTILE_FIGHTER, 3f, 3f,
				1f, 50, PARTICLE_COLOR, EXPLOSION_COLOR);
		spec.setDamageType(projectile.getDamageType());
		spec.setSoundSetId("explosion_flak");
		spec.setUseDetailedExplosion(false);

		DamagingProjectileAPI explosion = Global.getCombatEngine().spawnDamagingExplosion(spec, projectile.getSource(), projectile.getLocation(), false);
		explosion.addDamagedAlready(target);

		PLSP_WeaponPlugin.LocalData localData = (PLSP_WeaponPlugin.LocalData) engine.getCustomData().get(DATA_KEY);
		Map<DamagingProjectileAPI, CombatEntityAPI> flakData = localData.flakData;

		for (int i = 0; i < 5; i++) {
			float angle;
			if (target.getCollisionClass() == CollisionClass.SHIP || target.getCollisionClass() == CollisionClass.ASTEROID) {
				angle = projectile.getFacing() + 180f + 120f * (float)Math.random() - 120f * (float)Math.random();
			} else {
				angle = 72f * i + 60f * (float)Math.random() - 60f * (float)Math.random();
			}

			DamagingProjectileAPI proj = (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(projectile.getSource(), projectile.getWeapon(), "PLSP_icflak2", projectile.getLocation(), angle, null);
			proj.addDamagedAlready(target);
			proj.setCollisionClass(CollisionClass.NONE);

			flakData.put(proj, target);
		}
	}
}

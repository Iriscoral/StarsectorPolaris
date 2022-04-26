package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_PolarizationBeamEffect implements BeamEffectPlugin {

	private final IntervalUtil fireInterval = new IntervalUtil(0.5f, 0.75f);
	private boolean wasZero = true;

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		if (target == null || beam.getBrightness() < 1f) return;

		float dur = beam.getDamage().getDpsDuration();
		if (!wasZero) dur = 0f;
		wasZero = beam.getDamage().getDpsDuration() <= 0f;
		fireInterval.advance(dur);

		if (fireInterval.intervalElapsed()) {
			float angle = VectorUtils.getAngle(beam.getFrom(), beam.getRayEndPrevFrame()) + 180f;
			float differ = 30f + 45f * (float)Math.random();
			angle += Math.random() > 0.5 ? differ : -differ;

			Vector2f vel = MathUtils.getPoint(target.getVelocity(), 150f, angle);

			MissileAPI proj = (MissileAPI)engine.spawnProjectile(beam.getSource(), beam.getWeapon(), "PLSP_polarization", beam.getRayEndPrevFrame(), angle, vel);
			proj.addDamagedAlready(beam.getSource());

			SpriteAPI mask = Global.getSettings().getSprite("misc", "PLSP_polarizationMask");
			PLSP_Util.simpleObjectBasedRender(mask, proj, 0.25f, 9999f, 0.25f, CombatEngineLayers.ABOVE_PARTICLES);

			// proj.setArmingTime(0.25f);
			// proj.setEmpResistance(10000);
		}
	}
}
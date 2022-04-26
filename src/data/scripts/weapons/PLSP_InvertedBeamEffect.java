package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.weapons.ai.PLSP_InvertedBeamMoteAI;
import org.lazywizard.lazylib.VectorUtils;

public class PLSP_InvertedBeamEffect implements BeamEffectPlugin {

	private final IntervalUtil fireInterval = new IntervalUtil(0.6f, 0.8f);
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

			MissileAPI proj = (MissileAPI)engine.spawnProjectile(beam.getSource(), beam.getWeapon(), "motelauncher", beam.getRayEndPrevFrame(), angle, null);
			proj.setArmingTime(0.25f);
			proj.setEmpResistance(10000);
			proj.getDamage().setFluxComponent(1000f);

			proj.setMissileAI(new PLSP_InvertedBeamMoteAI(proj, target));
			proj.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
		}
	}
}
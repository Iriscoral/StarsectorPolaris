package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.PLSP_ColorData;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_AxisDriveStats extends BaseShipSystemScript {

	private float facing = -1f;

	private static String getString(String key) {
		return Global.getSettings().getString("ShipSystem", "PLSP_" + key);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		ship.setJitter(ship, PLSP_ColorData.DARK_BLUE_JITTER, effectLevel + 0.1f, 10, 0f, 20f);
		ship.addAfterimage(PLSP_ColorData.DARK_BLUE_AE, 0f, 0f, -ship.getVelocity().x * 0.2f, -ship.getVelocity().y * 0.2f, effectLevel, 0.2f - (effectLevel * 0.2f), effectLevel * 0.2f, 0.5f, false, true, false);

		if (facing < 0f) {
			Vector2f vel = ship.getVelocity();
			facing = ship.getFacing();
			if (!VectorUtils.isZeroVector(vel)) {
				facing = VectorUtils.getFacing(vel);
			}

			facing = MathUtils.clampAngle(facing);
		} else {
			CombatUtils.applyForce(ship, facing, 3500f);

			stats.getHighExplosiveDamageTakenMult().modifyMult(id, 0f);
			stats.getKineticDamageTakenMult().modifyMult(id, 0f);
			stats.getMaxTurnRate().modifyPercent(id, 500f * effectLevel);
			stats.getMaxTurnRate().modifyFlat(id, 50f * effectLevel);
			stats.getTurnAcceleration().modifyPercent(id, 1000f * effectLevel);
			stats.getTurnAcceleration().modifyFlat(id, 200f * effectLevel);
		}
	}
	
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) return;

		if (facing > 0f) {
			ship.getVelocity().scale(0.3f);
			facing = -1f;
		}

		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getHighExplosiveDamageTakenMult().unmodify(id);
		stats.getKineticDamageTakenMult().unmodify(id);
	}
	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(getString("axisdriveS1"), false);
		}
		return null;
	}
}
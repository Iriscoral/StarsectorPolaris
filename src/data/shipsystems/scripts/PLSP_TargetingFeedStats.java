package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_Util;

public class PLSP_TargetingFeedStats extends BaseShipSystemScript {

	private static String getString(String key) {
		return Global.getSettings().getString("ShipSystem", "PLSP_" + key);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		for (ShipAPI fighter : PLSP_Util.getFighters(ship)){
			FluxTrackerAPI tracker = fighter.getFluxTracker();
			if (tracker.isOverloaded()) {
				tracker.stopOverload();
			}

			if (fighter.getShield() != null) {
				if (fighter.getShield().isOn()) {
					fighter.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
				} else {
					fighter.getShield().toggleOn();
				}
			}

			tracker.decreaseFlux(tracker.getHardFlux());
			fighter.setJitterUnder(ship, PLSP_ColorData.DEEP_BLUE_JITTER_UNDER, effectLevel, 15, 4f);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(getString("targetingfeedS1"), false);
		}
		return null;
	}
}

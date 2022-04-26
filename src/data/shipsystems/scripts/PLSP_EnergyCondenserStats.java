package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class PLSP_EnergyCondenserStats extends BaseShipSystemScript {
	
	private static String getString(String key) {
		return Global.getSettings().getString("ShipSystem", "PLSP_" + key);
	}
	
	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		stats.getEnergyRoFMult().modifyPercent(id, 100f * effectLevel);
		stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - 0.5f * effectLevel);
	}
	
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEnergyRoFMult().unmodify(id);
		stats.getEnergyWeaponFluxCostMod().unmodify(id);
	}
	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(getString("energycondenserS1") + (int) (100f * effectLevel) + "%", false);
		}
		if (index == 1) {
			return new StatusData(getString("energycondenserS2") + (int) (50f * effectLevel) + "%", false);
		}
		if (index == 2) {
			return new StatusData(getString("energycondenserS3"), true);
		}
		return null;
	}
}

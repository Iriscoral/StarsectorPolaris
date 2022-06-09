package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.PLSP_Util.I18nSection;

public class PLSP_EnergyCondenserStats extends BaseShipSystemScript {
	
	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "PLSP_");
	
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
			return new StatusData(strings.get("energycondenserS1") + (int) (100f * effectLevel) + "%", false);
		}
		if (index == 1) {
			return new StatusData(strings.get("energycondenserS2") + (int) (50f * effectLevel) + "%", false);
		}
		if (index == 2) {
			return new StatusData(strings.get("energycondenserS3"), true);
		}
		return null;
	}
}

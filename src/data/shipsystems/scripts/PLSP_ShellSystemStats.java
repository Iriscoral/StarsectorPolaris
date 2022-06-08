package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.PLSP_Util.I18nSection;

public class PLSP_ShellSystemStats extends BaseShipSystemScript {
	
	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "PLSP_");

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) {
			return;
		}

		if (ship.getShield() != null && ship.getShield().isOn()) {
			ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
		}

		stats.getShieldUnfoldRateMult().modifyPercent(id, 400f);
		stats.getShieldDamageTakenMult().modifyMult(id, 1f - 0.9f * effectLevel);
		stats.getShieldUpkeepMult().modifyMult(id, 0f);
		stats.getShieldArcBonus().modifyMult(id, 2f);
	}
	
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getShieldDamageTakenMult().unmodify(id);
		stats.getShieldUnfoldRateMult().unmodify(id);
		stats.getShieldUpkeepMult().unmodify(id);
		stats.getShieldArcBonus().unmodify(id);
	}
	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(strings.get("shellsystemS1") + (int)(90f * effectLevel) + "%", false);
		}
		if (index == 1) {
			return new StatusData(strings.get("shellsystemS2"), true);
		}
		return null;
	}
}
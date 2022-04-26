package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.util.PLSP_ColorData;

public class PLSP_BlueDriveStats extends BaseShipSystemScript {

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		ship.setJitter(ship, PLSP_ColorData.DARK_BLUE_JITTER, effectLevel + 0.1f, 10, 0f, 20f);
		ship.addAfterimage(PLSP_ColorData.DARK_BLUE_AE, 0f, 0f, -ship.getVelocity().x * 0.2f, -ship.getVelocity().y * 0.2f, effectLevel, 0.2f - (effectLevel * 0.2f), effectLevel * 0.2f, 0.5f, false, true, false);
		ship.setPhased(true);
		ship.setExtraAlphaMult(1f - 0.75f * effectLevel);

		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id);
			stats.getShieldUpkeepMult().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, 600f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 2000f * effectLevel);
			stats.getShieldUpkeepMult().modifyMult(id, 0f);
		}
	}
	
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getShieldUpkeepMult().unmodify(id);

		ShipAPI ship = (ShipAPI)stats.getEntity();
		ship.setPhased(false);
		ship.setExtraAlphaMult(1f);
	}
	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}
}
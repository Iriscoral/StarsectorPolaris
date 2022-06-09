package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_Util.I18nSection;

public class PLSP_PoleGeneratorStats extends BaseShipSystemScript {

	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "PLSP_");

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		id = id + "_" + ship.getId();

		float jitterLevel = effectLevel;
		if (state == State.IN) {
			jitterLevel = Math.min(effectLevel / (1f / ship.getSystem().getChargeUpDur()), 1f);
		} else if (state == State.ACTIVE) {
			jitterLevel = 1f;
		}
		ship.setJitter(this, PLSP_ColorData.DARK_CLAY_JITTER, jitterLevel, 3, 0, jitterLevel * 5f);
		ship.setJitterUnder(this, PLSP_ColorData.BLUE_JITTER_UNDER, jitterLevel, 25, 0f, 3f + jitterLevel * 3f);

		effectLevel *= effectLevel;
		ship.getEngineController().fadeToOtherColor(this, PLSP_ColorData.GREEN_ENGINE, PLSP_ColorData.NONE, effectLevel, 0.5f);
		ship.getEngineController().extendFlame(this, -0.25f, -0.25f, 0.5f);

		float shipTimeMult = 1f + 2f * effectLevel;
		stats.getTimeMult().modifyMult(id, shipTimeMult);
		if (ship == Global.getCombatEngine().getPlayerShip()) {
			Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
		} else {
			Global.getCombatEngine().getTimeMult().unmodify(id);
		}

		if (state == State.OUT) {
			stats.getMaxSpeed().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, 50f);
			stats.getAcceleration().modifyPercent(id, 300f * effectLevel);
			stats.getDeceleration().modifyPercent(id, 300f * effectLevel);
			stats.getTurnAcceleration().modifyFlat(id, 20f * effectLevel);
			stats.getTurnAcceleration().modifyPercent(id, 100f * effectLevel);//no max turn rate
			stats.getShieldDamageTakenMult().modifyMult(id, 1f - effectLevel * 0.5f);
			stats.getArmorDamageTakenMult().modifyMult(id, 1f - effectLevel * 0.5f);
			stats.getHullDamageTakenMult().modifyMult(id, 1f - effectLevel * 0.5f);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}
		id = id + "_" + ship.getId();
		Global.getCombatEngine().getTimeMult().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getShieldDamageTakenMult().unmodify(id);
		stats.getArmorDamageTakenMult().unmodify(id);
		stats.getHullDamageTakenMult().unmodify(id);
		stats.getTimeMult().unmodify(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(strings.get("polegeneratorS1") , false);
		}
		if (index == 1) {
			return new StatusData(strings.get("polegeneratorS2"), false);
		}
		if (index == 2) {
			return new StatusData(strings.get("polegeneratorS3") + (int) (50f * effectLevel) + "%", false);
		}
		return null;
	}
}
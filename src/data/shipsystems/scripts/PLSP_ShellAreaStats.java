package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.PLSP_Util.I18nSection;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.Color;
import java.util.List;

public class PLSP_ShellAreaStats extends BaseShipSystemScript {
	private static final float RANGE_FACTOR = 1500f;

	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "PLSP_");

	public static float getRange(ShipAPI ship) {
		if (ship == null) return RANGE_FACTOR;
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE_FACTOR);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused() || !ship.isAlive()) {
			if (!ship.isAlive()) {
				unapply(stats, id);
			}
			return;
		}

		if (ship.getShield() != null && ship.getShield().isOn()) {
			ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
		}

		stats.getShieldUnfoldRateMult().modifyPercent(id, 100f);
		stats.getShieldDamageTakenMult().modifyMult(id, 1f - 0.8f * effectLevel);
		stats.getShieldUpkeepMult().modifyMult(id, 0f);

		id = id + ship.getId();

		for (ShipAPI tmp : AIUtils.getAlliesOnMap(ship)) {
			MutableShipStatsAPI statsT = tmp.getMutableStats();
			if (!tmp.isPhased() && tmp.getShield() != null && MathUtils.getDistance(ship, tmp) <= getRange(ship)) {
				float distant = MathUtils.getDistance(ship, tmp);
				float mod = Math.max(0f, -2f * (float)Math.pow(0.0004f * (distant - 2000f), 3f));
				mod = Math.max(mod, 0.2f);

				// mod may up to 1.02
				// 50% reduce at 0 range
				// 37% reduce at 200 range
				// 26% reduce at 400 range
				// 17% reduce at 600 range
				// 10% reduce at 840 range or farther

				statsT.getShieldDamageTakenMult().modifyMult(id, 1f - 0.5f * mod * effectLevel);
				tmp.setJitter(ship, new Color(255, 100, 255, (int)(55f * mod) + 5), effectLevel, 15, 4f);
			} else {
				statsT.getShieldDamageTakenMult().unmodifyMult(id);
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		stats.getShieldUnfoldRateMult().unmodifyPercent(id);
		stats.getShieldDamageTakenMult().unmodifyMult(id);
		stats.getShieldUpkeepMult().unmodifyMult(id);

		id = id + ship.getId();
		for (ShipAPI target : AIUtils.getAlliesOnMap(ship)) {
			MutableShipStatsAPI statsT = target.getMutableStats();
			statsT.getShieldDamageTakenMult().unmodifyMult(id);
		}
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(strings.get("shellareaS3") + (int)(80f * effectLevel) + "%", false);
		}
		if (index == 1) {
			return new StatusData(strings.get("shellareaS4"), false);
		}
		if (index == 2) {
			return new StatusData(strings.get("shellareaS5"), true);
		}
		return null;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) return "";
		List<ShipAPI> targets = AIUtils.getNearbyAllies(ship, getRange(ship));
		if (!targets.isEmpty()) {
			return strings.get("shellareaS1") + targets.size();
		}
		return strings.get("shellareaS2");
	}
}
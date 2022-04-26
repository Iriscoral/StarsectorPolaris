package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.PLSP_Util;

import java.util.Random;

public class PLSP_RepairLinkStats extends BaseShipSystemScript {

	private static final Random rand = new Random();
	private PLSP_RepairLinkVisual visual = null;

	private static String getString(String key) {
		return Global.getSettings().getString("ShipSystem", "PLSP_" + key);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
			return;
		}

		if (visual == null) {
			visual = new PLSP_RepairLinkVisual(ship);
			engine.addLayeredRenderingPlugin(visual);
		}

		float amount = engine.getElapsedInLastFrame();
		for (ShipAPI fighter : PLSP_Util.getFighters(ship)) {
			fighter.getMutableStats().getCombatEngineRepairTimeMult().modifyMult(id, 1f - 0.95f * effectLevel);
			fighter.setHitpoints(Math.min(fighter.getHitpoints() + fighter.getMaxHitpoints() * 0.5f * amount * effectLevel, fighter.getMaxHitpoints()));

			fighter.getFluxTracker().decreaseFlux(fighter.getFluxTracker().getMaxFlux() * amount * effectLevel);
			if (fighter.getFluxTracker().isOverloaded()) {
				fighter.getFluxTracker().stopOverload();
			}

			if (fighter.getShield() != null) {
				if (fighter.getShield().isOn()) {
					fighter.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
				} else {
					fighter.getShield().toggleOn();
				}
			}

			if ((float)Math.random() < 0.5f * effectLevel) {
				ArmorGridAPI armorGrid = fighter.getArmorGrid();
				int x = rand.nextInt(armorGrid.getGrid().length);
				int y = rand.nextInt(armorGrid.getGrid()[0].length);
				armorGrid.setArmorValue(x, y, armorGrid.getMaxArmorInCell());
			}
		}
	}
	
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		if (visual != null) {
			visual.setValid(false);
			visual = null;
		}

		for (ShipAPI fighter : PLSP_Util.getFighters(ship)) {
			fighter.getMutableStats().getCombatEngineRepairTimeMult().unmodifyMult(id);
		}
	}
	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(getString("repairlinkS1") + " 50%", false);
		}
		if (index == 1) {
			return new StatusData(getString("repairlinkS2") + " 100%", false);
		}
		return null;
	}
}
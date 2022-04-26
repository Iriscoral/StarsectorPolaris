package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc.FindShipFilter;
import data.scripts.util.PLSP_Util;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_ReserveDroneStats extends BaseShipSystemScript {

	private static String getString(String key) {
		return Global.getSettings().getString("ShipSystem", "PLSP_" + key);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null || effectLevel != 1f) {
			return;
		}

		ShipAPI target = getTarget(ship);
		CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(ship.getOwner());
		boolean suppressed = manager.isSuppressDeploymentMessages();
		if (!suppressed) {
			manager.setSuppressDeploymentMessages(true);
		}

		for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
			if (!slot.isSystemSlot()) continue;

			Vector2f point = slot.computePosition(ship);
			ShipAPI newShip = manager.spawnShipOrWing("PLSP_subobject_wing", point, ship.getFacing(), 0f);
			newShip.setCRAtDeployment(1f);
			newShip.setCurrentCR(1f);
			newShip.setOwner(ship.getOwner());
			newShip.setOriginalOwner(ship.getOwner());
			newShip.setCollisionClass(CollisionClass.FIGHTER);
			newShip.setExtraAlphaMult(0f);

			PLSP_ReserveDroneAI ai = new PLSP_ReserveDroneAI(newShip, target, ship, slot);
			newShip.setShipAI(ai);
		}

		if (!suppressed) {
			manager.setSuppressDeploymentMessages(false);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) return "";

		if (ship.isPullBackFighters()) {
			return getString("reservedroneS2");
		}

		ShipAPI target = getTarget(ship);
		if (target == null) {
			return getString("reservedroneS3");
		}

		return getString("reservedroneS4");
	}


	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (system.isActive()) return true;
		ShipAPI target = getTarget(ship);
		return target != null;
	}

	private static final FindShipFilter FILTER = new FindShipFilter(){
		@Override
		public boolean matches(ShipAPI ship) {
			return !ship.isPhased() && !ship.isFighter() && !ship.isDrone();
		}
	};

	private static ShipAPI getTarget(ShipAPI ship) {
		if (ship.isPullBackFighters()) return null;
		return PLSP_Util.findSingleEnemy(ship, Float.MAX_VALUE, true, false, FILTER, null);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(getString("reservedroneS1"), false);
		}
		return null;
	}
}

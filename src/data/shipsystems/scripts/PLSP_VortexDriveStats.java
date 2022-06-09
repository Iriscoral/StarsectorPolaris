package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_Util;
import data.scripts.util.PLSP_Util.I18nSection;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_VortexDriveStats extends BaseShipSystemScript {

	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "PLSP_");

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			stats.getMaxSpeed().modifyFlat(id, 600f); // no effectLevel
			stats.getAcceleration().modifyFlat(id, 2000f);
			return;
		}

		ship.setJitter(ship, PLSP_ColorData.DARK_BLUE_JITTER, effectLevel + 0.1f, 10, 0f, 20f);
		ship.addAfterimage(PLSP_ColorData.DARK_GREEN_AE, 0f, 0f, -ship.getVelocity().x * 0.2f, -ship.getVelocity().y * 0.2f, effectLevel, 0.2f - (effectLevel * 0.2f), effectLevel * 0.2f, 0.5f, false, true, false);
		ship.setPhased(true);
		ship.setExtraAlphaMult(1f - 0.75f * effectLevel);

		if (Global.getCombatEngine().isPaused() || !ship.isAlive()) {
			return;
		}

		if (state == ShipSystemStatsScript.State.OUT) {
			if (stats.getMaxSpeed().getFlatMods().containsKey(id)) {
				Vector2f vel = new Vector2f(ship.getVelocity());
				vel.scale(0.5f);
				PLSP_Util.easyRippleOut(ship.getLocation(), vel, ship.getCollisionRadius() * 4f, 120f, 1f, 120f);
				stats.getMaxSpeed().unmodify(id);
				stats.getShieldUpkeepMult().unmodify(id);
			}

			boolean hasStation = false;
			for (CombatEntityAPI nearby : CombatUtils.getEntitiesWithinRange(ship.getLocation(), ship.getCollisionRadius() * 2f)) {
				if (nearby == ship) continue;
				if (nearby instanceof ShipAPI) {
					ShipAPI targetShip = (ShipAPI)nearby;
					if (targetShip.isFighter() || targetShip.isDrone() || targetShip.isPhased()) {
						continue;
					} else if (targetShip.isStation() || (targetShip.isStationModule() && targetShip.getParentStation() != null && targetShip.getParentStation().isStation())) {
						hasStation = true;
						continue;
					}
				}

				float force = (float)Math.sqrt(nearby.getMass()) * 220f / Math.max(MathUtils.getDistance(nearby, ship), 200f);
				if (!(nearby instanceof ShipAPI)) {
					force *= 0.35f;
				}

				CombatUtils.applyForce(nearby, VectorUtils.getAngle(ship.getLocation(), nearby.getLocation()), force);
			}

			if (hasStation) {
				ship.getVelocity().scale(0.95f);
			}
		} else {
			stats.getMaxSpeed().modifyFlat(id, 600f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 2000f * effectLevel);
			stats.getShieldUpkeepMult().modifyMult(id, 0f);
		}
	}
	
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getShieldUpkeepMult().unmodify(id);

		ship.setPhased(false);
		ship.setExtraAlphaMult(1f);
	}
	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(strings.get("vortexdriveS1"), false);
		}
		if (index == 1) {
			return new StatusData(strings.get("vortexdriveS2"), false);
		}
		return null;
	}
}
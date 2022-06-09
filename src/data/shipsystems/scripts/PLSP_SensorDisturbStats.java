package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.PLSP_Util.I18nSection;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PLSP_SensorDisturbStats extends BaseShipSystemScript {
	private static final float RANGE_FACTOR = 2000f;

	private SensorDisturbState data = null;

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
		if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
			return;
		}

		if (data == null) {
			data = new SensorDisturbState(ship);
		} else {
			id = id + ship.getId();

			data.visual.updateEffectLevel(effectLevel);
			data.visualEntity.getLocation().set(ship.getLocation());

			for (ShipAPI tmp : AIUtils.getNearbyEnemies(ship, getRange(ship))) {
				if (!tmp.isPhased() && !data.targetData.containsKey(tmp)) {
					data.targetData.put(tmp, 0f);
				}
			}

			for (ShipAPI tmp : data.targetData.keySet()) {
				MutableShipStatsAPI statsT = tmp.getMutableStats();
				float distant = MathUtils.getDistance(ship, tmp);

				float function = (float)Math.max(Math.exp(-0.002f * distant + 0.1f), 0.1f) * effectLevel;
				// function may up to 1.1

				if (tmp.isPhased() || distant > 2100f) {
					statsT.getSightRadiusMod().unmodify(id);
					statsT.getAutofireAimAccuracy().unmodify(id);
					statsT.getBallisticWeaponRangeBonus().unmodify(id);
					statsT.getEnergyWeaponRangeBonus().unmodify(id);
					statsT.getMissileWeaponRangeBonus().unmodify(id);
				} else {
					float mod = Math.max(1f - function, 0f);
					mod = (float)Math.pow(mod, 0.6f);
					// 100% reduce at 50 range
					// 55% reduce at 200 range
					// 33% reduce at 400 range
					// 21% reduce at 600 range
					// 14% reduce at 800 range
					// 6% reduce at 1200 range or farther

					statsT.getSightRadiusMod().modifyMult(id, mod);
					statsT.getAutofireAimAccuracy().modifyMult(id, mod);
					statsT.getBallisticWeaponRangeBonus().modifyMult(id, mod);
					statsT.getEnergyWeaponRangeBonus().modifyMult(id, mod);
					statsT.getMissileWeaponRangeBonus().modifyMult(id, mod);
				}

				data.targetData.put(tmp, function);
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		if (data != null) {
			id = id + ship.getId();
			for (ShipAPI target : data.targetData.keySet()) {
				MutableShipStatsAPI statsT = target.getMutableStats();
				statsT.getSightRadiusMod().unmodify(id);
				statsT.getAutofireAimAccuracy().unmodify(id);
				statsT.getBallisticWeaponRangeBonus().unmodify(id);
				statsT.getEnergyWeaponRangeBonus().unmodify(id);
				statsT.getMissileWeaponRangeBonus().unmodify(id);
			}

			data.targetData.clear();
			data.visual.setValid(false);

			data = null;
		}
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData(strings.get("sensordisturbS3"), false);
		}
		return null;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) return "";
		List<ShipAPI> targets = AIUtils.getNearbyEnemies(ship, getRange(ship));
		if (!targets.isEmpty()) {
			return strings.get("sensordisturbS1") + targets.size();
		}
		return strings.get("sensordisturbS2");
	}

	private final static class SensorDisturbState {
		Map<ShipAPI, Float> targetData;

		PLSP_SensorDisturbVisual visual;
		CombatEntityAPI visualEntity;

		private SensorDisturbState(ShipAPI anchor) {
			targetData = new HashMap<>();

			visual = new PLSP_SensorDisturbVisual(anchor, targetData);
			visualEntity = Global.getCombatEngine().addLayeredRenderingPlugin(visual);
		}
	}
}
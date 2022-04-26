package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PLSP_Commission extends BaseHullMod {
	private static final String id = "PLSP_commission";
	private static final String DATA_KEY = "PLSP_CommissionData";
	private static final Color BORDER = new Color(147, 102, 50, 0);
	private static final Color NAME = new Color(65, 230, 243, 255);

	private static final Map<HullSize, Float> mag = new HashMap<>();
	static {
		mag.put(HullSize.FRIGATE, 0.4f);
		mag.put(HullSize.DESTROYER, 0.6f);
		mag.put(HullSize.CRUISER, 0.8f);
		mag.put(HullSize.CAPITAL_SHIP, 1f);
	}

    private String getString(String key) {
        return Global.getSettings().getString("HullMod", "PLSP_" + key);
    }

	@Override
	public Color getBorderColor() {
		//return BORDER;
		return null;
	}

	@Override
	public Color getNameColor() {
		return NAME;
	}

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 10f;
        float padS = 2f;

		tooltip.addPara("%s " + getString("commissionTEXT1"), pad, Misc.getHighlightColor(), "#", "1000", Global.getSettings().getHullModSpec(id).getDisplayName());
        tooltip.addPara("%s " + getString("commissionTEXT2"), padS, Misc.getHighlightColor(), "#");
		tooltip.addPara("%s " + getString("commissionTEXT3"), padS, Misc.getPositiveHighlightColor(), "#", "0.4", "0.6", "0.8", "1");
		tooltip.addPara("%s " + getString("commissionTEXT4"), padS, Misc.getPositiveHighlightColor(), "#", "+20%");
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
    	CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.getCustomData().containsKey(DATA_KEY)) {
			engine.getCustomData().put(DATA_KEY, new HashMap<>());
		}

		Map<ShipAPI, CommState> shipsMap = (Map)engine.getCustomData().get(DATA_KEY);
		if (!engine.isEntityInPlay(ship) || !ship.isAlive()) {
			if (!ship.isAlive() && shipsMap.containsKey(ship)) {
				CommState data = shipsMap.get(ship);
				data.resetAlpha();
				shipsMap.remove(ship);
			}
			return;
		}

		if (ship.isStationModule()) return;
		if (!shipsMap.containsKey(ship)) {
			shipsMap.put(ship, new CommState(ship));
		} else {
			CommState data = shipsMap.get(ship);

			float total = 0f;
			for (ShipAPI ally : AIUtils.getNearbyAllies(ship, 1000f)) {
				if (ally.isDrone() || ally.isFighter()) continue;
				if (ally.isStation() || ally.isStationModule()) continue;

				float bonus = (float)Math.exp(-0.003f * MathUtils.getDistance(ship, ally) + 2.3f);
				bonus *= mag.get(ally.getHullSize()) * mag.get(ship.getHullSize());
				total += bonus;
			}
			total = Math.min(total, 20f);

			MutableShipStatsAPI stats = ship.getMutableStats();
			stats.getBallisticWeaponRangeBonus().modifyPercent(id, total);
			stats.getEnergyWeaponRangeBonus().modifyPercent(id, total);
			stats.getMissileWeaponRangeBonus().modifyPercent(id, total);

			data.alpha = total / 20f;
			ringRender(data, amount);
		}
    }

	private static void ringRender(CommState state, float amount) {
		state.sprite.setAlphaMult(state.alpha);
		state.sprite.setAngle(state.angle);
		state.angle += 0.1f * amount;

		if (state.angle > 10000f) {
			state.angle -= 10000f;
		}

		PLSP_Util.simpleFrameRender(state.sprite, state.ship.getLocation(), CombatEngineLayers.UNDER_SHIPS_LAYER);
	}

	public final static class CommState {
		public float alpha;
		public float angle;
		public SpriteAPI sprite;
		public ShipAPI ship;

		public CommState(ShipAPI ship) {
			alpha = 0f;
			angle = 0f;

			this.ship = ship;
			sprite = Global.getSettings().getSprite("misc", "PLSP_commissionRing");

			float radius = ship.getShieldRadiusEvenIfNoShield() * 2.5f + 50f;
			sprite.setSize(radius, radius);
		}

		public void resetAlpha() {
			sprite.setAlphaMult(0f);
			alpha = 0f;
		}
	}
}
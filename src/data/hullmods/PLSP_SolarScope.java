package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.*;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_DataBase;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.HashMap;
import java.util.Map;

public class PLSP_SolarScope extends BaseHullMod {
	private static final String id = "PLSP_SolarScope";
	private static final IntervalUtil CHECKER = new IntervalUtil(0.5f, 0.5f);
	private static final Map<HullSize, Integer> BOOST_RATIO = new HashMap<>();
	static {
		BOOST_RATIO.put(HullSize.DEFAULT, 0);
		BOOST_RATIO.put(HullSize.FIGHTER, 0);
		BOOST_RATIO.put(HullSize.FRIGATE, 2);
		BOOST_RATIO.put(HullSize.DESTROYER, 3);
		BOOST_RATIO.put(HullSize.CRUISER, 4);
		BOOST_RATIO.put(HullSize.CAPITAL_SHIP, 5);
	}
	
	private static String getString(String key) {
		return Global.getSettings().getString("HullMod", "PLSP_" + key);
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return !isForModSpec;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 10f;
		float padS = 2f;

		tooltip.addPara("%s " + getString("solarscopeTEXT1"), pad, Misc.getPositiveHighlightColor(), "#", Global.getSettings().getHullModSpec("PLSP_solarscope").getDisplayName(), "2%", "3%", "4%", "5%");
		tooltip.addPara("%s " + getString("solarscopeTEXT2"), padS, Misc.getHighlightColor(), "#", "30%");
		tooltip.addPara("%s " + getString("HAO"), pad, Misc.getNegativeHighlightColor(), "#");
	}
	
	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
			return;
		}

		int count = BOOST_RATIO.get(ship.getHullSize());
		for (ShipAPI ally : AIUtils.getAlliesOnMap(ship)) {
			if (ally.isDrone() || ally.isFighter()) continue;
			if (ally.getVariant().getHullMods().contains("PLSP_solarscope")) {
				count += BOOST_RATIO.get(ally.getHullSize());
			}
		}

		count = Math.min(count, 30);
		ship.getMutableStats().getSystemRegenBonus().modifyPercent(id, count);
		ship.getMutableStats().getSystemCooldownBonus().modifyMult(id, 1f - count * 0.01f);
	}
	
	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return PLSP_DataBase.isPLSPShip(ship);
	}
}
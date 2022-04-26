package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_DataBase;

public class PLSP_NodeLock extends BaseHullMod {

	private static String getString(String key) {
		return Global.getSettings().getString("HullMod", "PLSP_" + key);
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return !isForModSpec;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 10f;
		float padS = 2f;

		tooltip.addPara("%s " + getString("nodelockTEXT1"), pad, Misc.getPositiveHighlightColor(), "#");
		tooltip.addPara("%s " + getString("nodelockTEXT2"), padS, Misc.getHighlightColor(), "#");
	}
	
	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return PLSP_DataBase.isPLSPShip(ship);
	}
}
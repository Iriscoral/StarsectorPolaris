package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_DataBase;
import data.scripts.util.PLSP_Util.I18nSection;

public class PLSP_NodeLock extends BaseHullMod {

	public static final I18nSection strings = I18nSection.getInstance("HullMod", "PLSP_");

	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return !isForModSpec;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 10f;
		float padS = 2f;

		tooltip.addPara("%s " + strings.get("nodelockTEXT1"), pad, Misc.getPositiveHighlightColor(), "#");
		tooltip.addPara("%s " + strings.get("nodelockTEXT2"), padS, Misc.getHighlightColor(), "#");
	}
	
	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return PLSP_DataBase.isPLSPShip(ship);
	}
}
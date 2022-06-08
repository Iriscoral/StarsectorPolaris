package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_DataBase;
import data.scripts.util.PLSP_Util.I18nSection;

public class PLSP_MagnetDamper extends BaseHullMod {

	public static final I18nSection strings = I18nSection.getInstance("HullMod", "PLSP_");

	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return !isForModSpec;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 10f;
		float padS = 2f;

		tooltip.addPara("%s " + strings.get("magnetdamperTEXT1"), pad, Misc.getPositiveHighlightColor(), "#", "+15%");
		tooltip.addPara("%s " + strings.get("magnetdamperTEXT2"), padS, Misc.getPositiveHighlightColor(), "#", "+5%");
		tooltip.addPara("%s " + strings.get("HAO"), pad, Misc.getNegativeHighlightColor(), "#");
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMaxArmorDamageReduction().modifyFlat(id, 0.05f);
		stats.getMinArmorFraction().modifyFlat(id, 0.15f);
		stats.getBreakProb().modifyMult(id, 0f);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return PLSP_DataBase.isPLSPShip(ship);
	}
}
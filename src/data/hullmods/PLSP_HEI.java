package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_Util.I18nSection;

public class PLSP_HEI extends BaseHullMod {

	public static final I18nSection strings = I18nSection.getInstance("HullMod", "PLSP_");

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return !isForModSpec;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 10f;
		float padS = 2f;

		tooltip.addPara("%s " + strings.get("heiTEXT1"), pad, Misc.getPositiveHighlightColor(), "#", "-10");
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, -10f);
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}
}
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_DataBase;
import data.scripts.util.PLSP_Util;
import data.scripts.util.PLSP_Util.I18nSection;

public class PLSP_FluxLink extends BaseHullMod {
	private static final String id = "PLSP_FluxLink";
	
	public static final I18nSection strings = I18nSection.getInstance("HullMod", "PLSP_");

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return !isForModSpec;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 10f;
		float padS = 2f;

		tooltip.addPara("%s " + strings.get("fluxlinkTEXT1"), pad, Misc.getHighlightColor(), "#");
		tooltip.addPara("%s " + strings.get("fluxlinkTEXT2"), padS, Misc.getHighlightColor(), "#", "90%");
		tooltip.addPara("%s " + strings.get("fluxlinkTEXT3"), padS, Misc.getNegativeHighlightColor(), "#");
		tooltip.addPara("%s " + strings.get("fluxlinkDO"), pad, Misc.getNegativeHighlightColor(), "#");
		tooltip.addPara("%s " + strings.get("HAO"), padS, Misc.getNegativeHighlightColor(), "#");
	}
	
	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive()) {
			return;
		}

		FluxTrackerAPI flux = ship.getFluxTracker();
		if (flux.isOverloadedOrVenting() || flux.getFluxLevel() > 0.9f) {
			return;
		}

		if (ship.getSystem() != null && ship.getSystem().getId().contentEquals("PLSP_repairlink") && ship.getSystem().isActive()) {
			return;
		}

		if (ship.isPullBackFighters()) {
			return;
		}

		ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
		for (ShipAPI fighter : PLSP_Util.getFighters(ship)) {
			FluxTrackerAPI fluxF = fighter.getFluxTracker();
			float dissipation = fighter.getMutableStats().getFluxDissipation().modified * amount;
			float mult = 1.0f;
			float soft = Math.min((fluxF.getCurrFlux() - fluxF.getHardFlux() - dissipation) * mult, flux.getMaxFlux() - flux.getCurrFlux() - 2f);
			float hard = Math.min((fluxF.getHardFlux() - dissipation) * mult, flux.getMaxFlux() - flux.getCurrFlux() - soft - 2f);
			soft = Math.max(soft, 0f);
			hard = Math.max(hard, 0f);

			if (ship.getSystem() != null && ship.getSystem().getId().contentEquals("PLSP_targetingfeed") && ship.getSystem().isActive()) {
				soft = Math.max(soft - hard, 0f);
				hard = 0f;
			}

			if (soft > 0f || hard > 0f) {
				flux.increaseFlux(soft, false);
				flux.increaseFlux(hard, true);
				// flux.increaseFlux(soft + hard, false);
				fluxF.decreaseFlux((soft + hard) / mult);
				fighter.setJitterUnder(fighter, PLSP_ColorData.VIOLET_JITTER_UNDER, 1f, 10, 0f, 18f);

				if (fluxF.getCurrFlux() == 0f) {
					fluxF.stopOverload();
				}
			}
		}
	}
	
	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		int bays = (int)ship.getMutableStats().getNumFighterBays().getModifiedValue();
		if (bays == 0) {
			ship.getVariant().removeMod("PLSP_FluxLink");
			Global.getSoundPlayer().playUISound("cr_allied_warning", 1f, 1f);
		}
	}
	
	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		int bays = (int)ship.getMutableStats().getNumFighterBays().getModifiedValue();
		return PLSP_DataBase.isPLSPShip(ship) && bays > 0;
	}
	
	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		int bays = (int)ship.getMutableStats().getNumFighterBays().getModifiedValue();
		if (bays == 0) {
			return strings.get("fluxlinkDO");
		}
		return null;
	}
}
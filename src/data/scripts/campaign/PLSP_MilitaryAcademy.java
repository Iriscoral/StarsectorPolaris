package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class PLSP_MilitaryAcademy extends BaseIndustry {
	public static final String INDUSTRY_ID = "PLSP_militaryacademy";
	public static final String MA_KEY = "$PLSP_MilitaryAcademy_Faction";
	
	private static String getString(String key) {
		return Global.getSettings().getString("Misc", "PLSP_" + key);
	}

	public static float getMaxLevel() {
		return 6f;
	}

	public static float getMaxLevel(MarketAPI market) {
		if (market.getIndustry(INDUSTRY_ID) == null) return 0f;

		PLSP_MilitaryAcademy ma = (PLSP_MilitaryAcademy)market.getIndustry(INDUSTRY_ID);
		float levelMult = ma.getDeficitMult(Commodities.SHIPS, Commodities.SUPPLIES);
		return getMaxLevel() * levelMult;
	}

	public static float getNumBonus() {
		return 5f;
	}

	public static float getNumBonus(MarketAPI market) {
		if (market.getIndustry(INDUSTRY_ID) == null) return 0f;

		PLSP_MilitaryAcademy ma = (PLSP_MilitaryAcademy)market.getIndustry(INDUSTRY_ID);
		float numMult = ma.getDeficitMult(Commodities.SUPPLIES, Commodities.HEAVY_MACHINERY, Commodities.CREW);
		return getNumBonus() * numMult;
	}
	
	@Override
	public void apply() {
		super.apply(false);
		int size = market.getSize();

		demand(Commodities.SUPPLIES, size - 1);
		demand(Commodities.SHIPS, size - 2);
		demand(Commodities.HEAVY_MACHINERY, size - 2);
		demand(Commodities.CREW, size - 3);
		applyIncomeAndUpkeep(size);

		if (isFunctional()) {
			Global.getSector().getMemoryWithoutUpdate().set(MA_KEY, market.getPrimaryEntity().getId());
		} else {
			unapply();
		}
	}
	
	@Override
	public void unapply() {
		super.unapply();
		market.getStats().getDynamic().getMod(Stats.OFFICER_MAX_LEVEL_MOD).unmodifyFlat(getModId());
		market.getStats().getDynamic().getMod(Stats.OFFICER_MAX_ELITE_SKILLS_MOD).unmodifyFlat(getModId());
		market.getStats().getDynamic().getMod(Stats.OFFICER_PROB_MOD).unmodifyFlat(getModId());
		market.getStats().getDynamic().getMod(Stats.OFFICER_ADDITIONAL_PROB_MULT_MOD).unmodifyFlat(getModId());

		Global.getSector().getMemoryWithoutUpdate().unset(MA_KEY);
	}

	@Override
	public boolean isAvailableToBuild() {
		return false;
	}
	
	@Override
	public boolean showWhenUnavailable() {
		return false;
	}

	@Override
	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		return mode != IndustryTooltipMode.NORMAL || isFunctional();
	}
	
	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
			String levelStr = "+" + Math.round(getMaxLevel(market)) + "%";
			String numStr = "+" + Math.round(getNumBonus(market)) + "%";

			float opad = 10f;
			tooltip.addPara(getString("alphacoreID"), opad, Misc.getHighlightColor(), levelStr);
			tooltip.addPara(getString("alphacoreIN"), opad, Misc.getHighlightColor(), numStr);
			tooltip.addPara(getString("alphacoreAD"), Misc.getGrayColor(), opad);
		}
	}

	@Override
	public void notifyBeingRemoved(MarketAPI.MarketInteractionMode mode, boolean forUpgrade) {
		unapply();
		if (aiCoreId != null) {
			CargoAPI cargo = getCargoForInteractionMode(mode);
			if (cargo != null) {
				cargo.addCommodity(aiCoreId, 1f);
			}
		}
	}
	
	private static final float ALPHA_CORE_BONUS = 0.15f;
	@Override
	protected void applyAlphaCoreModifiers() {
		market.getStats().getDynamic().getMod(Stats.OFFICER_MAX_LEVEL_MOD).modifyFlat(getModId(1), ALPHA_CORE_BONUS, getString("alphacore") + " (" + getNameForModifier() + ")");
		market.getStats().getDynamic().getMod(Stats.OFFICER_MAX_ELITE_SKILLS_MOD).modifyFlat(getModId(1), ALPHA_CORE_BONUS, getString("alphacore") + " (" + getNameForModifier() + ")");
	}
	
	@Override
	protected void applyNoAICoreModifiers() {
		market.getStats().getDynamic().getMod(Stats.OFFICER_MAX_LEVEL_MOD).unmodifyFlat(getModId(1));
		market.getStats().getDynamic().getMod(Stats.OFFICER_MAX_ELITE_SKILLS_MOD).unmodifyFlat(getModId(1));
	}
	
	@Override
	protected void applyAlphaCoreSupplyAndDemandModifiers() {
		demandReduction.modifyFlat(getModId(0), DEMAND_REDUCTION, getString("alphacore"));
	}
	
	@Override
	protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
		float opad = 10f;
		String pre = getString("alphacoreCA");
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = getString("alphacore") + ".";
		}

		String str = "+" + (int)(100f * ALPHA_CORE_BONUS) + "%";
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
			TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);
			text.addPara(pre + getString("alphacoreRC") + " " +
					getString("alphacoreID"), 0f, Misc.getHighlightColor(),
					"" + (int)((1f - UPKEEP_MULT) * 100f) + "%", "" + DEMAND_REDUCTION,
					str);
			tooltip.addImageWithText(opad);
			return;
		}

		tooltip.addPara(pre + getString("alphacoreRC") + " " +
					getString("alphacoreID"), opad, Misc.getHighlightColor(),
				"" + (int)((1f - UPKEEP_MULT) * 100f) + "%", "" + DEMAND_REDUCTION, str);
	}

	@Override
	public float getDeficitMult(String... commodities) {
		float deficit = (float)getMaxDeficit(commodities).two;
		float demand = 0f;
		for (String id : commodities) {
			demand = Math.max(demand, (float)getDemand(id).getQuantity().getModifiedInt());
		}

		if (deficit < 0f) {
			deficit = 0f;
		}

		if (demand < 1f) {
			demand = 1f;
			deficit = 0f;
		}

		float mult = (demand - deficit) / demand;
		if (mult < 0f) {
			mult = 0f;
		}

		if (mult > 1f) {
			mult = 1f;
		}

		return mult;
	}
}
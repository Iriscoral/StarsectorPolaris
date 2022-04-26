package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.PLSP_MilitaryAcademyIntel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PLSP_MAConfirm extends BaseCommandPlugin {

	private static String getString(String key) {
		return Global.getSettings().getString("CMD", "PLSP_" + key);
	}

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		MemoryAPI memory = memoryMap.get(MemKeys.LOCAL);
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

		List<SpecialItemData> skills = (List<SpecialItemData>)memory.get("$PLSP_MA_targetSkills");
		PersonAPI target = (PersonAPI)memory.get("$PLSP_MA_selectedOfficer");

		int creditsCost = PLSP_MAData.getCreditsCost(skills);
		playerFleet.getCargo().getCredits().subtract(creditsCost);
		AddRemoveCommodity.addCreditsLossText(creditsCost, dialog.getTextPanel());

		if (memory.getString("$PLSP_MA_reqType").contentEquals(PLSP_MAData.SPOT_EXPERIENCE_ID)) {
			playerFleet.getCargo().removeCommodity("alpha_core", 1);
			AddRemoveCommodity.addCommodityLossText("alpha_core", 1, dialog.getTextPanel());
		}

		RepActions action = RepActions.TRADE_EFFECT;
		Global.getSector().adjustPlayerReputation(new RepActionEnvelope(action, 0.05f, null,
				dialog.getTextPanel(), false, true,
				getString("trust")), "plsp");

		List<String> skillsToAdd = new ArrayList<>();
		for (SpecialItemData data : skills) {
			skillsToAdd.add(data.getData());
		}

		int timeCost = PLSP_MAData.getTimeCost(skills);
		new PLSP_MilitaryAcademyIntel(dialog.getInteractionTarget(), target, skillsToAdd, timeCost);

		return true;
	}
}

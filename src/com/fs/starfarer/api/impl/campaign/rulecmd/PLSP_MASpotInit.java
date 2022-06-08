package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_Util.I18nSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PLSP_MASpotInit extends BaseCommandPlugin {

	public static final I18nSection strings = I18nSection.getInstance("CMD", "PLSP_");

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		MemoryAPI memory = memoryMap.get(MemKeys.LOCAL);

		List<SpecialItemData> spot = new ArrayList<>();
		spot.add(new SpecialItemData(PLSP_MAData.SKILL_PICKER_ITEM_ID, PLSP_MAData.SPOT_EXPERIENCE_SKILL));

		memory.set("$PLSP_MA_targetSkills", spot);

		dialog.getOptionPanel().clearOptions();
		dialog.getOptionPanel().addOption(strings.get("return"), "PLSP_MA_spotExperience");
		dialog.getOptionPanel().addOption(strings.get("backtomenu"), "PLSP_MA_backToStart");
		FireAll.fire("PLSP_MA_confirmSE_firer", dialog, memoryMap, "MAConfirm");

		return true;
	}
}

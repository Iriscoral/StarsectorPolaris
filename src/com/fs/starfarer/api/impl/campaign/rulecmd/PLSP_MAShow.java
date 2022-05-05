package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Map;

public class PLSP_MAShow extends BaseCommandPlugin {

	private static PersonAPI fakePerson = null;
	
	private static String getString(String key) {
		return Global.getSettings().getString("CMD", "PLSP_" + key);
	}

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		MemoryAPI memory = memoryMap.get(MemKeys.LOCAL);
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		OptionPanelAPI optionPanel = dialog.getOptionPanel();

		switch (params.get(0).string) {
			case "PLSP_MAServices": {
				optionPanel.clearOptions();
				optionPanel.addOption(getString("normalexperience"), "PLSP_MA_normalExperience", getString("normalexperience_tooltip"));
				optionPanel.addOption(getString("spotexperience"), "PLSP_MA_spotExperience", getString("spotexperience_tooltip"));
				optionPanel.addOption(getString("help"), "PLSP_MA_help");
				optionPanel.addOption(getString("cutlink"), "cutCommLink");
				optionPanel.setShortcut("cutCommLink", Keyboard.KEY_ESCAPE, false, false, false, false);
				memory.set("$PLSP_MA_target_map", null, 0);

				if (dialog.getInteractionTarget().getFaction().isAtBest(playerFleet.getFaction(), RepLevel.FAVORABLE)) {
					optionPanel.setEnabled("PLSP_MA_spotExperience", false);
					optionPanel.setTooltip("PLSP_MA_spotExperience", getString("norep"));
				}
				break;
			}
			case "PLSP_MAOfficers": {
				optionPanel.clearOptions();
				dialog.showCustomDialog(500f, 500f, new PLSP_MAShowOfficersDelegate(dialog, memory));

				optionPanel.addOption(getString("back"), "PLSP_MA_backToStart");
				optionPanel.setShortcut("PLSP_MA_backToStart", Keyboard.KEY_ESCAPE, false, false, false, false);
				break;
			}
			case "PLSP_MASkills": {
				PersonAPI selectedOfficer = (PersonAPI)memory.get("$PLSP_MA_selectedOfficer");
				CargoAPI conditionCargo = PLSP_MAData.getSkillCargo(selectedOfficer);
				dialog.showCargoPickerDialog(getString("selectskill"), getString("confirm"), getString("cancel"),
						false, 320f, conditionCargo,
						new PLSP_MASkillPickerListener(dialog, memory, memoryMap, selectedOfficer, playerFleet));
				break;
			}
			case "PLSP_MASpotData": {
				dialog.getTextPanel().addPara(getString("spotexperience_des"), Misc.getHighlightColor(), Global.getSettings().getSkillSpec(PLSP_MAData.SPOT_SKILL_ID).getName());
				dialog.getTextPanel().addPara(getString("spotexperience_des2"), Misc.getHighlightColor(), String.valueOf(PLSP_MAData.SPOT_EXPERIENCE_COST), String.valueOf(PLSP_MAData.SPOT_EXPERIENCE_ALPHA_COST));

				if (fakePerson == null) {
					fakePerson = Global.getFactory().createPerson();
					fakePerson.getStats().increaseSkill(PLSP_MAData.SPOT_SKILL_ID);
				}
				dialog.getTextPanel().addSkillPanel(fakePerson, false);

				optionPanel.clearOptions();
				optionPanel.addOption(getString("continue"), "PLSP_MA_spotExperience_2");
				optionPanel.addOption(getString("return"), "PLSP_MA_backToStart");
				optionPanel.setShortcut("PLSP_MA_backToStart", Keyboard.KEY_ESCAPE, false, false, false, false);
				if (!PLSP_MAData.canAffordSpot(playerFleet)) {
					optionPanel.setEnabled("PLSP_MA_spotExperience_2", false);
					optionPanel.setTooltip("PLSP_MA_spotExperience_2", getString("cantafford2"));
				}

				break;
			}
			default:
				break;
		}
		return false;
	}
}

package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PLSP_MASkillPickerListener implements CargoPickerListener {
	private static final List<SpecialItemData> PICKED_ITEMS = new ArrayList<>();
	private static final List<SpecialItemData> SELECTED_ITEMS = new ArrayList<>();
	private final InteractionDialogAPI dialog;
	private final MemoryAPI memory;
	private final Map<String, MemoryAPI> memoryMap;
	private final PersonAPI officer;
	private final CampaignFleetAPI playerFleet;

	private static String getString(String key) {
		return Global.getSettings().getString("CMD", "PLSP_" + key);
	}

	public PLSP_MASkillPickerListener(InteractionDialogAPI dialog, MemoryAPI memory, Map<String, MemoryAPI> memoryMap, PersonAPI officer, CampaignFleetAPI playerFleet) {
		this.dialog = dialog;
		this.memory = memory;
		this.memoryMap = memoryMap;
		this.officer = officer;
		this.playerFleet = playerFleet;
	}

	@Override
	public void pickedCargo(CargoAPI cargo) {
		PICKED_ITEMS.clear();
		for (CargoStackAPI stack : cargo.getStacksCopy()) {
			if (!stack.isSpecialStack()) continue;

			PICKED_ITEMS.add(stack.getSpecialDataIfSpecial());
		}

		if (!PLSP_MAData.canAffordNormal(PICKED_ITEMS, playerFleet)) {
			PICKED_ITEMS.clear();
		} else if (PICKED_ITEMS.size() > PLSP_MAData.getSkillAmountLimit(officer)) {
			PICKED_ITEMS.clear();
		}

		if (PICKED_ITEMS.isEmpty()) {
			cancelledCargoSelection();
		} else {
			memory.set("$PLSP_MA_targetSkills", new ArrayList<>(PICKED_ITEMS));
			dialog.getOptionPanel().clearOptions();
			FireAll.fire("PLSP_MA_confirmNE_firer", dialog, memoryMap, "MAConfirm");
		}
	}

	@Override
	public void cancelledCargoSelection() {
		dialog.getOptionPanel().clearOptions();
		dialog.getOptionPanel().addOption(getString("return"), "PLSP_MA_selectedOfficer");
		dialog.getOptionPanel().addOption(getString("reselectofficer"), "PLSP_MA_normalExperience");
		dialog.getOptionPanel().addOption(getString("backtomenu"), "PLSP_MA_backToStart");
	}

	@Override
	public void recreateTextPanel(TooltipMakerAPI panel, CargoAPI cargo, CargoStackAPI pickedUp, boolean pickedUpFromSource, CargoAPI combined) {
		float pad = 3f;
		float opad = 10f;
		float lpad = 20f;
		Color highlight = Misc.getHighlightColor();
		Color negative = Misc.getNegativeHighlightColor();
		Color positive = Misc.getPositiveHighlightColor();

		TooltipMakerAPI imageTooltip = panel.beginImageWithText(officer.getPortraitSprite(), 64f);
		imageTooltip.setParaOrbitronLarge();
		imageTooltip.addPara(officer.getNameString(), opad);
		imageTooltip.setParaFontDefault();
		imageTooltip.addPara(officer.getPersonalityAPI().getDisplayName(), opad);
		imageTooltip.addPara(getString("officerlevel") + officer.getStats().getLevel(), opad);
		panel.addImageWithText(0f);

		SELECTED_ITEMS.clear();
		for (CargoStackAPI stack : combined.getStacksCopy()) {
			if (!stack.isSpecialStack()) continue;

			SELECTED_ITEMS.add(stack.getSpecialDataIfSpecial());
		}

		if (SELECTED_ITEMS.isEmpty()) return;

		Color selectedColor = SELECTED_ITEMS.size() > PLSP_MAData.getSkillAmountLimit(officer) ? negative : positive;
		imageTooltip.setParaOrbitronLarge();
		panel.addPara(String.format(getString("selectedskills"), SELECTED_ITEMS.size(), PLSP_MAData.getSkillAmountLimit(officer)), opad, highlight, selectedColor, "" + SELECTED_ITEMS.size());
		imageTooltip.setParaFontDefault();

		for (SpecialItemData itemData : SELECTED_ITEMS) {
			panel.addPara("    " + Global.getSettings().getSkillSpec(itemData.getData()).getName(), pad);
		}

		imageTooltip.setParaOrbitronLarge();
		panel.addPara(getString("creditscost"), highlight, opad);
		imageTooltip.setParaFontDefault();
		panel.addPara("    " + PLSP_MAData.getCreditsCost(SELECTED_ITEMS), pad, positive);

		if (!PLSP_MAData.canAffordNormal(SELECTED_ITEMS, playerFleet)) {
			imageTooltip.setParaOrbitronLarge();
			panel.addPara(getString("cantafford2"), negative, lpad);
		} else if (SELECTED_ITEMS.size() > PLSP_MAData.getSkillAmountLimit(officer)) {
			imageTooltip.setParaOrbitronLarge();
			panel.addPara(getString("amountlimit"), negative, lpad);
		} else {
			panel.addPara(getString("ready"), positive, lpad);
		}
	}
}
package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_Util.I18nSection;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PLSP_MAShowOfficersDelegate implements CustomDialogDelegate {

	private static final float TITLE_HEIGHT = 20f;
	private static final float OFFICER_AREA_HEIGHT = 40f;
	private static final float OFFICER_SPRITE_HEIGHT = 128f;
	private static final float SKILL_SPRITE_HEIGHT = 64f;
	private static final float BUTTON_HEIGHT = 20f;
	private static final float BUTTON_WIDTH = 120f;
	private static final float UNDER_PADDING = 20f;
	private static final int MAX_DISPLAY_COUNT = 7;

	private static final Color BASE_COLOR = Misc.getBasePlayerColor();
	private static final Color DARK_COLOR = Misc.getDarkPlayerColor();
	private static final Color BRIGHT_COLOR = Misc.getBrightPlayerColor();

	public static final I18nSection strings = I18nSection.getInstance("CMD", "PLSP_");
	private final PLSP_MAShowOfficersPlugin plugin = new PLSP_MAShowOfficersPlugin(this);
	private InteractionDialogAPI dialog;
	private MemoryAPI memory;
	private CustomPanelAPI panel;
	private float width;
	private float height;

	public PLSP_MAShowOfficersDelegate(InteractionDialogAPI dialog, MemoryAPI memory) {
		this.dialog = dialog;
		this.memory = memory;
	}

	@Override
	public void createCustomDialog(CustomPanelAPI panel) {

		this.panel = panel;
		this.width = panel.getPosition().getWidth();
		this.height = panel.getPosition().getHeight();

		TooltipMakerAPI title = panel.createUIElement(width - 10f, TITLE_HEIGHT, false);
		title.setTitleOrbitronLarge();
		title.addTitle(strings.get("selectofficer")).setAlignment(Alignment.MID);
		panel.addUIElement(title).inTMid(0f);

		float pad = 10f;
		float padS = 2f;

		float totalListHeight = height - TITLE_HEIGHT - UNDER_PADDING;
		TooltipMakerAPI totalList = panel.createUIElement(width, totalListHeight, true);

		List<String> images = new ArrayList<>();
		List<OfficerDataAPI> officerData = Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy();
		for (OfficerDataAPI singleOfficerData : officerData) {
			PersonAPI person = singleOfficerData.getPerson();
			if (person.isPlayer()) continue;
			if (person.isAICore()) continue;
			if (person.isDefault()) continue;

			boolean canSelect = !person.getMemoryWithoutUpdate().getBoolean(PLSP_MAData.DATA_KEY);
			boolean spot = memory.getString("$PLSP_MA_reqType").contentEquals("PLSP_MA_spotExperience");

			TooltipMakerAPI subArea = totalList.beginImageWithText(person.getPortraitSprite(), OFFICER_SPRITE_HEIGHT);
			subArea.setParaFontOrbitron();
			subArea.addPara(person.getNameString(), pad);
			subArea.setParaFontDefault();
			subArea.addPara(person.getPersonalityAPI().getDisplayName(), padS);

			int maxLevel = Global.getSettings().getInt("officerMaxLevel");
			String levelString = strings.get("officerlevel") + person.getStats().getLevel();
			if (person.getStats().getLevel() >= maxLevel) {
				levelString += strings.get("maxlevel");
				if (!spot) {
					canSelect = false;
				}
			}
			subArea.addPara(levelString, padS);

			for (SkillLevelAPI skill : person.getStats().getSkillsCopy()) {
				if (images.size() >= MAX_DISPLAY_COUNT) break;

				if (skill.getSkill().isAptitudeEffect()) continue;
				images.add(skill.getSkill().getSpriteName());
			}

			for (int i = images.size(); i < MAX_DISPLAY_COUNT; i++) { // i don't know why
				images.add(Global.getSettings().getSpriteName("misc", "PLSP_emptySkill"));
			}

			float widthForSkills = width - OFFICER_SPRITE_HEIGHT - pad - 5f;
			subArea.addImages(widthForSkills, SKILL_SPRITE_HEIGHT, padS, pad, images.toArray(new String[0]));
			totalList.addImageWithText(OFFICER_AREA_HEIGHT);
			images.clear();

			String buttonString = canSelect ? strings.get("select") : strings.get("noselect");
			ButtonAPI bt = subArea.addButton(buttonString, person, BASE_COLOR, DARK_COLOR, BUTTON_WIDTH, BUTTON_HEIGHT, pad);
			plugin.addButton(bt, person);
			if (!canSelect) {
				bt.setEnabled(false);
			}

			subArea.setForceProcessInput(true);
		}

		totalList.addPara("", UNDER_PADDING);
		totalList.setForceProcessInput(true);
		panel.addUIElement(totalList).belowLeft(title, pad);
	}

	@Override
	public boolean hasCancelButton() {
		return false;
	}

	@Override
	public String getConfirmText() {
		return getCancelText();
	}

	@Override
	public String getCancelText() {
		return strings.get("cancel");
	}

	@Override
	public void customDialogConfirm() {
		customDialogCancel();
	}

	@Override
	public void customDialogCancel() {
		Map<String, MemoryAPI> memoryMap = dialog.getPlugin().getMemoryMap();
		MemoryAPI memory = memoryMap.get(MemKeys.LOCAL);
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

		if (selectedOfficer != null) {
			memory.set("$PLSP_MA_selectedOfficer", selectedOfficer, 0f);
			memory.set("$PLSP_MA_selectedOfficerName", selectedOfficer.getNameString(), 0f);
			dialog.getOptionPanel().clearOptions();
			dialog.getOptionPanel().addOption(strings.get("continue"), "PLSP_MA_selectedOfficer");
			dialog.getOptionPanel().addOption(strings.get("back"), "PLSP_MA_backToStart");
		} else {
			dialog.getOptionPanel().clearOptions();
			FireBest.fire(null, dialog, memoryMap, "MAMainOpts");
		}
	}

	@Override
	public CustomUIPanelPlugin getCustomPanelPlugin() {
		return plugin;
	}

	private PersonAPI selectedOfficer = null;
	public void setSelectedOfficer(PersonAPI officer) {
		selectedOfficer = officer;
	}
}
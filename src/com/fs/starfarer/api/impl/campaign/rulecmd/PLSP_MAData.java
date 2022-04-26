package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import data.scripts.util.PLSP_DataBase;

import java.util.List;

public class PLSP_MAData {
	public static final String DATA_KEY = "$PLSP_MAData";
	public static final String SKILL_PICKER_ITEM_ID = "PLSP_skillpicker";
	public static final String SPOT_SKILL_ID = "PLSP_spot_resilience";

	public static final String NORMAL_EXPERIENCE_ID = "PLSP_MA_normalExperience";
	public static final int NORMAL_EXPERIENCE_COST = 65000;
	public static final int NORMAL_EXPERIENCE_TIME = 10;
	public static final String SPOT_EXPERIENCE_ID = "PLSP_MA_spotExperience";
	public static final int SPOT_EXPERIENCE_COST = 250000;
	public static final int SPOT_EXPERIENCE_ALPHA_COST = 1;
	public static final int SPOT_EXPERIENCE_TIME = 30;

	private static CargoAPI skillCargo = null;
	public static CargoAPI getSkillCargo(PersonAPI officer) {
		if (skillCargo == null) skillCargo = Global.getFactory().createCargo(true);
		skillCargo.clear();

		for (String id : Global.getSettings().getSkillIds()) {
			SkillSpecAPI skill = Global.getSettings().getSkillSpec(id);
			if (skill.isAptitudeEffect()) continue;
			if (skill.getTags().contains(Skills.TAG_PLAYER_ONLY)) continue;
			if (skill.getTags().contains(Skills.TAG_AI_CORE_ONLY)) continue;
			if (!skill.isCombatOfficerSkill()) continue;
			if (PLSP_DataBase.getSkillEffects(skill.getId()) == null) continue;

			if (officer.getStats().getSkillLevel(id) > 0f) continue;

			SpecialItemData item = new SpecialItemData("PLSP_skillpicker", id);
			skillCargo.addSpecial(item, 1);
		}

		return skillCargo;
	}

	public static int getTimeCost(List<SpecialItemData> skills) {
		int time = 0;
		for (SpecialItemData data : skills) {
			String id = data.getData();
			if (id.contentEquals(SPOT_SKILL_ID)) {
				time += SPOT_EXPERIENCE_TIME;
			} else {
				time += NORMAL_EXPERIENCE_TIME;
			}
		}

		return time;
	}

	public static int getCreditsCost(List<SpecialItemData> skills) {
		return skills.size() * NORMAL_EXPERIENCE_COST;
	}

	public static boolean canAffordNormal(List<SpecialItemData> skills, CampaignFleetAPI playerFleet) {
		int credits = getCreditsCost(skills);
		return playerFleet.getCargo().getCredits().get() >= credits;
	}

	public static boolean canAffordSpot(CampaignFleetAPI playerFleet) {
		boolean credits = playerFleet.getCargo().getCredits().get() >= SPOT_EXPERIENCE_COST;
		boolean alpha = playerFleet.getCargo().getCommodityQuantity("alpha_core") >= SPOT_EXPERIENCE_ALPHA_COST;
		return credits && alpha;
	}

	public static int getSkillAmountLimit(PersonAPI officer) {
		int maxLevel = Global.getSettings().getInt("officerMaxLevel");
		int officerLevel = officer.getStats().getLevel();
		return maxLevel - officerLevel;
	}
}

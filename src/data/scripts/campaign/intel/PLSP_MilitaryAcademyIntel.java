package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.PLSP_MAData;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;
import java.util.Set;

public class PLSP_MilitaryAcademyIntel extends BaseIntelPlugin {
	private float duration = 60f;
	private boolean triggeredEnd = false;
	private boolean lastMACheck = true;
	private IntervalUtil checker = new IntervalUtil(1f, 1f);
	private SectorEntityToken entity;
	private PersonAPI person;
	private List<String> skillsToAdd;
	private float timeLeft;

	public PLSP_MilitaryAcademyIntel(SectorEntityToken entity, PersonAPI person, List<String> skillsToAdd, long timeLeft) {
		this.entity = entity;
		this.person = person;
		this.skillsToAdd = skillsToAdd;
		this.timeLeft = (float) timeLeft;

		person.getMemoryWithoutUpdate().set(PLSP_MAData.DATA_KEY, true);
		setImportant(true);

		Global.getSector().addScript(this);
		Global.getSector().getIntelManager().addIntel(this);
		Global.getLogger(PLSP_MilitaryAcademyIntel.class).info("MA Intel Active.");
	}

	@Override
	public void reportMadeVisibleToPlayer() {
		if (!isEnding() && !isEnded()) {
			duration = Math.max(duration * 0.5f, Math.min(duration * 2f, 60f));
		}
	}

	@Override
	public void advanceImpl(float amount) {
		if (triggeredEnd) {
			person.getMemoryWithoutUpdate().set(PLSP_MAData.DATA_KEY, false);
			endAfterDelay();
			return;
		}

		for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getMembersWithFightersCopy()) {
			if (member.getCaptain() != null && member.getCaptain() == person) {
				member.setCaptain(null);
			}
		}

		if (timeLeft <= 0f) {
			triggeredEnd = true;
			sendUpdateIfPlayerHasIntel(new Object(), false, false);

			if (lastMACheck) {
				Global.getLogger(PLSP_MilitaryAcademyIntel.class).info("MA Success Intel Done.");
				for (String id : skillsToAdd) {
					person.getStats().increaseSkill(id);

					if (!id.contentEquals(PLSP_MAData.SPOT_SKILL_ID)) {
						person.getStats().setLevel(person.getStats().getLevel() + 1);
					}
				}

				OfficerDataAPI oData = Global.getSector().getPlayerFleet().getFleetData().getOfficerData(person);
				if (oData != null) {
					oData.makeSkillPicks();
				}
			} else {
				Global.getLogger(PLSP_MilitaryAcademyIntel.class).info("MA Fail Intel Done.");
			}
		} else {
			float days = Global.getSector().getClock().convertToDays(amount);
			timeLeft -= days;

			checker.advance(days);
			lastMACheck = MACheck(entity);
			if (checker.intervalElapsed() && !lastMACheck) {
				timeLeft = -1f;
			}
		}
	}

	@Override
	protected void notifyEnding() {
		super.notifyEnding();
		Global.getSector().getListenerManager().removeListener(this);
	}

	@Override
	public String getSmallDescriptionTitle() {
		if (isEnding()) {
			if (lastMACheck) {
				getString("title_1");
			} else {
				getString("title_2");
			}
		}
		return getString("title_0");
	}

	@Override
	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		float pad = 3f;
		float opad = 10f;

		if (mode == ListInfoMode.IN_DESC) initPad = opad;
		FactionAPI faction = getFactionForUIColors();

		bullet(info);
		if (isUpdate) {
			// 3 possible updates: de-posted/expired, failed, completed
			if (isEnding()) {
				if (lastMACheck) {
					info.addPara(getString("ends_1"), initPad, tc, h, person.getName().getFullName());
				} else {
					info.addPara(getString("ends_2"), initPad, tc, h, person.getName().getFullName());
				}
			}
		} else {
			// either in small description, or in tooltip/intel list
			if (isEnding()) {
				if (lastMACheck) {
					info.addPara(getString("ends_1"), initPad, tc, h, person.getName().getFullName());
				} else {
					info.addPara(getString("ends_2"), initPad, tc, h, person.getName().getFullName());
				}
			} else {
				info.addPara(getString("short_0"), initPad, tc, h, person.getName().getFullName());
			}
		}

		unindent(info);
	}

	@Override
	public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
		Color c = getTitleColor(mode);
		info.addPara(getSmallDescriptionTitle(), c, 0f);
		addBulletPoints(info, mode);
	}

	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		float pad = 3f;
		float opad = 10f;

		info.addImage(person.getPortraitSprite(), width, 128, opad);
		String[] images = new String[skillsToAdd.size()];
		for (int i = 0; i < skillsToAdd.size(); i++) {
			images[i] = Global.getSettings().getSkillSpec(skillsToAdd.get(i)).getSpriteName();
		}
		info.addImages(width, 64, opad, pad, images);

		addBulletPoints(info, ListInfoMode.IN_DESC);

		if (isEnding()) {
			if (lastMACheck) {
				info.addPara(getString("des_1"), opad, h, entity.getName());
			} else {
				info.addPara(getString("des_2"), opad, h, entity.getName());
			}
		} else {
			info.addPara(getString("des_0"), opad, h, entity.getName(), "" + (int)timeLeft);
		}
	}

	private String getString(String key) {
		return Global.getSettings().getString("Intel", "PLSP_MA_" + key);
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return entity;
	}


	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(getFactionForUIColors().getId());
		return tags;
	}

	@Override
	public String getIcon() {
		return Global.getSettings().getSpriteName("intel", "PLSP_MA");
	}

	@Override
	public void sendUpdateIfPlayerHasIntel(Object listInfoParam, boolean onlyIfImportant, boolean sendIfHidden) {
		Global.getSector().getCampaignUI().addMessage(this, CommMessageAPI.MessageClickAction.INTEL_TAB, this);
	}

	private static boolean MACheck(SectorEntityToken entity) {
		if (entity != null && entity.getMarket() != null) {
			return MACheck(entity.getMarket());
		}

		return false;
	}

	private static boolean MACheck(MarketAPI market) {
		return market.getFaction() == Global.getSector().getFaction("plsp") && market.hasIndustry("PLSP_militaryacademy") && market.getIndustry("PLSP_militaryacademy").isFunctional();
	}
}
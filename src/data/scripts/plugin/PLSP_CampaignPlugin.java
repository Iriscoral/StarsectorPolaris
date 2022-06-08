package data.scripts.plugin;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import data.scripts.campaign.PLSP_MilitaryAcademy;
import data.scripts.util.PLSP_Util;

import java.util.Random;

public class PLSP_CampaignPlugin extends BaseCampaignEventListener implements EveryFrameScript {
	private final Random random = new Random();

	public PLSP_CampaignPlugin() {
		super(true);
	}
	
	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}

	@Override
	public void reportPlayerOpenedMarket(MarketAPI market) {
		if (MACheck(market)) {
			for (PersonAPI person : market.getPeopleCopy()) {
				if (person.getMemoryWithoutUpdate().contains("$PLSP_MA_isPort")) {
					return;
				}
			}

			ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
			PersonAPI person = market.getFaction().createRandomPerson();
			String rankId = Ranks.GROUND_MAJOR;
			if (market.getSize() >= 6) {
				rankId = Ranks.GROUND_GENERAL;
			} else if (market.getSize() >= 4) {
				rankId = Ranks.GROUND_COLONEL;
			}
			person.setPostId("PLSP_MA_Port");
			person.setRankId(rankId);
			person.getMemoryWithoutUpdate().set("$PLSP_MA_isPort", true);

			market.getCommDirectory().addPerson(person);
			market.addPerson(person);

			ip.addPerson(person);
			ip.getData(person).getLocation().setMarket(market);
			ip.checkOutPerson(person, "permanent_staff");
		} else {
			for (PersonAPI person : market.getPeopleCopy()) {
				if (person.getMemoryWithoutUpdate().contains("$PLSP_MA_isPort")) {
					ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
					ip.removePerson(person);
					market.getCommDirectory().removePerson(person);
					market.removePerson(person);
					break;
				}
			}
		}
	}

	@Override
	public void reportFleetSpawned(CampaignFleetAPI fleet) {
		String maEntityId = Global.getSector().getMemoryWithoutUpdate().getString(PLSP_MilitaryAcademy.MA_KEY);
		if (maEntityId == null || maEntityId.isEmpty()) return;

		SectorEntityToken maEntity = Global.getSector().getEntityById(maEntityId);
		if (maEntity == null) return;

		MarketAPI maMarket = maEntity.getMarket();
		FactionAPI maFaction = maMarket.getFaction();
		if (!fleet.isEmpty() && fleet.getFaction() == maFaction) {

			float maxLevel = PLSP_MilitaryAcademy.getMaxLevel(maMarket);
			float numBonus = PLSP_MilitaryAcademy.getNumBonus(maMarket);

			if (fleet.getMarket() == null || !fleet.getMarket().hasIndustry(PLSP_MilitaryAcademy.INDUSTRY_ID)) {
				maxLevel *= 0.5f;
				numBonus *= 0.5f;
			}

			int actualNumBonus = (int)(numBonus);
			int actualMaxLevel = (int)(maxLevel);
			PLSP_Util.addOfficers(fleet, actualNumBonus, actualMaxLevel, 1, random);

			for (OfficerDataAPI data : fleet.getFleetData().getOfficersCopy()) {
				if (Math.random() > 0.8) {
					data.getPerson().getStats().setSkillLevel("PLSP_spot_resilience", 1);
				}
			}
		}
	}

	@Override
	public void advance(float amount) {}

	private static boolean MACheck(MarketAPI market) {
		return market.getFaction() == Global.getSector().getFaction("plsp") && market.hasIndustry("PLSP_militaryacademy") && market.getIndustry("PLSP_militaryacademy").isFunctional();
	}
}
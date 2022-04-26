package data.scripts.ungprules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;
import data.scripts.utils.UNGP_BaseBuff;
import org.lazywizard.lazylib.MathUtils;

import java.util.LinkedList;

public class PLSP_DepressingLineup extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {

	private static final String KEY_TIME = "$PLSP_DepressingLineup_Time";
	private static final String KEY_STORAGE = "$PLSP_DepressingLineup_Storage";
	private static final float DAYS_FROM_SUPPORT = 3f;
	private static final float DAYS_PER_CHECK = 0.2f;
	private static final float RANGE_TO_SUPPORT = 1000f;

    private float crReduce;
    private float crRelief;

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        crReduce = getValueByDifficulty(0, difficulty);
        crRelief = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.1f, 0.2f);
        if (index == 1) return difficulty.getLinearValue(0.04f, 0.08f);
        return 0f;
    }

	@Override
	public void advanceInCampaign(float amount, UNGP_CampaignPlugin.TempCampaignParams tempCampaignParams) {

    	if (Global.getSector().isPaused()) return;

    	CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    	if (fleet == null || !fleet.isAlive()) return;

    	LinkedList<Integer> effectingFleets = (LinkedList<Integer>)Global.getSector().getMemoryWithoutUpdate().get(KEY_STORAGE);
    	float timer = (float)Global.getSector().getMemoryWithoutUpdate().get(KEY_TIME);

    	timer += Global.getSector().getClock().convertToDays(amount);
    	if (timer >= DAYS_PER_CHECK) {
    		timer -= DAYS_PER_CHECK;

			int validFleets = 0;
			for (CampaignFleetAPI other : fleet.getContainingLocation().getFleets()) {
				if (other == fleet) continue;
				if (!other.isFriendlyTo(fleet)) continue;

				if (MathUtils.getDistance(other, fleet) <= RANGE_TO_SUPPORT) {
					validFleets++;
				}
			}

			effectingFleets.removeFirst();
			effectingFleets.addLast(validFleets);
		}

		Global.getSector().getMemoryWithoutUpdate().set(KEY_TIME, timer);

    	int highestCount = 0;
    	for (int count : effectingFleets) {
    		if (count > highestCount) highestCount = count;
		}

		float actualReduce = crReduce - highestCount * crRelief;
		if (actualReduce <= 0f) return;

		boolean needsSync = false;
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			BuffManagerAPI.Buff test = member.getBuffManager().getBuff(buffID);
			if (test instanceof LineupBuff) {
				LineupBuff buff = (LineupBuff) test;
				buff.setDur(0.1f);

				if (buff.getEffect() != actualReduce) {
					buff.updateEffect(actualReduce);
					needsSync = true;
				}
			} else {
				member.getBuffManager().addBuff(new LineupBuff(buffID, actualReduce, 0.1f));
				needsSync = true;
			}
		}

		if (needsSync) {
			fleet.forceSync();
		}
	}

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
		if (index == 1) return getFactorString(RANGE_TO_SUPPORT);
		if (index == 2) return getFactorString(DAYS_FROM_SUPPORT);
		if (index == 3) return getPercentString(getValueByDifficulty(1, difficulty) * 100f);
		return null;
    }

	@Override
	public void applyGlobalStats() {
		if (!Global.getSector().getMemoryWithoutUpdate().contains(KEY_STORAGE)) {
			LinkedList<Integer> effectingFleets = new LinkedList<>();
			for (int i = 0; i < DAYS_FROM_SUPPORT / DAYS_PER_CHECK; i++) {
				effectingFleets.add(10);
			}

			Global.getSector().getMemoryWithoutUpdate().set(KEY_STORAGE, effectingFleets);
		}

		if (!Global.getSector().getMemoryWithoutUpdate().contains(KEY_TIME)) {
			Global.getSector().getMemoryWithoutUpdate().set(KEY_TIME, 0f);
		}
	}

	private class LineupBuff extends UNGP_BaseBuff {

    	float effect;

		public LineupBuff(String id, float effect, float dur) {
			super(id, dur);
			this.effect = effect;
		}

		@Override
		public void apply(FleetMemberAPI member) {
			decreaseMaxCR(member.getStats(), id, effect, rule.getName());
		}

		public void updateEffect(float effect) {
			this.effect = effect;
		}

		public float getEffect() {
			return effect;
		}
	}
}
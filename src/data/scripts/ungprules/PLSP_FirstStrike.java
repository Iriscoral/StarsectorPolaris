package data.scripts.ungprules;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class PLSP_FirstStrike extends UNGP_BaseRuleEffect implements UNGP_CombatTag {

    private float rangeBonus = 1f;

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        rangeBonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(150f, 300f);
        return 0f;
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {}

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
    	enemy.getMutableStats().getSystemRangeBonus().modifyFlat(buffID, rangeBonus);
	}

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {}
}
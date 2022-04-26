package data.scripts.ungprules;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_PoleGuard extends UNGP_BaseRuleEffect implements UNGP_CombatTag {

    private float damageBonus = 1f;

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        damageBonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(8f, 8f);
        return 0f;
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {}

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {}

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    	if (!ship.isAlive() || ship.isDrone() || ship.isFighter()) return;

    	if (!ship.hasListenerOfClass(ConcentrateListener.class)) {
			ship.addListener(new ConcentrateListener(engine, ship));
		}
	}

	private class ConcentrateListener implements DamageDealtModifier {

    	private final CombatEngineAPI engine;
    	private final ShipAPI self;

		private ConcentrateListener(CombatEngineAPI engine, ShipAPI self) {
			this.engine = engine;
			this.self = self;
		}

		@Override
		public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {

			if (self == engine.getPlayerShip()) return null;
			if (self.getParentStation() == engine.getPlayerShip()) return null;

			if (target != engine.getPlayerShip().getShipTarget()) return null;
			// yes, i mean friendly fires are included

			damage.getModifier().modifyPercent(buffID, damageBonus);
			return buffID;
		}
	}
}
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_DataBase;
import data.scripts.util.PLSP_Util;
import data.scripts.util.PLSP_Util.I18nSection;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PLSP_SpotResilience {
	private static final String DATA_KEY = "PLSP_SpotResilience";

	public static final I18nSection strings = I18nSection.getInstance("Misc", "PLSP_");

	public static class SpotResilienceData {
		PersonAPI person;
		String picked;
		boolean elite;
		boolean creation = false;
		boolean announced = false;

		SpotResilienceData(PersonAPI person) {
			this.person = person;
			this.picked = null;
			this.elite = person.getStats().getSkillLevel("PLSP_spot_resilience") > 1;
		}
	}

	private static SpotResilienceData getData(PersonAPI captain) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.getCustomData().containsKey(DATA_KEY)) {
			engine.getCustomData().put(DATA_KEY, new HashMap<>());
		}

		Map<PersonAPI, SpotResilienceData> dataMap = (Map)engine.getCustomData().get(DATA_KEY);
		if (dataMap.containsKey(captain)) {
			return dataMap.get(captain);
		} else {
			SpotResilienceData data = new SpotResilienceData(captain);
			dataMap.put(captain, data);
			return data;
		}
	}

	private static void message(ShipAPI source, String message, Color color) {
		String shipName = " (" + source.getHullSpec().getHullName() + " - Class)";
		String name = source.getCaptain().getNameString() + shipName;
		Global.getCombatEngine().getCombatUI().addMessage(1, source, Misc.getPositiveHighlightColor(), name, color, message);
	}

	public static class Level1 implements ShipSkillEffect {

		@Override
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			ShipAPI ship = (ShipAPI)stats.getEntity();
			if (ship == null || ship.getCaptain() == null || ship.getCaptain().isDefault()) return;
			if (PLSP_Util.isInRefit()) return;

			SpotResilienceData data = getData(ship.getCaptain());
			if (data.picked == null) {
				data.picked = PLSP_DataBase.pickUnpickedSkill(ship.getCaptain(), ship);
			}

			if (data.picked != null) {
				String skillId = data.picked;
				List<ShipSkillEffect> effects = PLSP_DataBase.getSkillEffects(skillId);
				for (ShipSkillEffect effect : effects) {
					effect.apply(stats, hullSize, id, level);

					if (!data.creation && effect instanceof AfterShipCreationSkillEffect) {
						AfterShipCreationSkillEffect a = (AfterShipCreationSkillEffect)effect;
						a.applyEffectsAfterShipCreation(ship, id);
					}
				}

				if (data.elite) {
					effects = PLSP_DataBase.getSkillEliteEffects(skillId);
					for (ShipSkillEffect effect : effects) {
						effect.apply(stats, hullSize, id, level);

						if (!data.creation && effect instanceof AfterShipCreationSkillEffect) {
							AfterShipCreationSkillEffect a = (AfterShipCreationSkillEffect)effect;
							a.applyEffectsAfterShipCreation(ship, id);
						}
					}
				}

				data.creation = true;

				if (!data.announced && PLSP_Util.isPlayerShipOwner(ship) && !ship.isAlly()) {
					String skillName = Global.getSettings().getSkillSpec(data.picked).getName();
					message(ship, String.format(strings.get("spotresilienceMessage"), skillName), Misc.getHighlightColor());
					data.announced = true;
				}
			}
		}

		@Override
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			ShipAPI ship = (ShipAPI)stats.getEntity();
			if (ship == null || ship.getCaptain() == null || ship.getCaptain().isDefault()) return;
			if (PLSP_Util.isInRefit()) return;

			SpotResilienceData data = getData(ship.getCaptain());
			if (data != null && data.picked != null) {
				String skillId = data.picked;
				List<ShipSkillEffect> effects = PLSP_DataBase.getSkillEffects(skillId);
				for (ShipSkillEffect effect : effects) {
					effect.unapply(stats, hullSize, id);

					if (effect instanceof AfterShipCreationSkillEffect) {
						AfterShipCreationSkillEffect a = (AfterShipCreationSkillEffect)effect;
						a.unapplyEffectsAfterShipCreation(ship, id);
					}
				}

				if (data.elite) {
					effects = PLSP_DataBase.getSkillEliteEffects(skillId);
					for (ShipSkillEffect effect : effects) {
						effect.unapply(stats, hullSize, id);

						if (!data.creation && effect instanceof AfterShipCreationSkillEffect) {
							AfterShipCreationSkillEffect a = (AfterShipCreationSkillEffect)effect;
							a.unapplyEffectsAfterShipCreation(ship, id);
						}
					}
				}

				data.creation = false;
			}
		}

		@Override
		public String getEffectDescription(float level) {
			return strings.get("spotresilience");
		}

		@Override
		public String getEffectPerLevelDescription() {
			return null;
		}

		@Override
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class Level2 implements ShipSkillEffect {

		@Override
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}

		@Override
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}

		@Override
		public String getEffectDescription(float level) {
			return strings.get("spotresilienceElite");
		}

		@Override
		public String getEffectPerLevelDescription() {
			return null;
		}

		@Override
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
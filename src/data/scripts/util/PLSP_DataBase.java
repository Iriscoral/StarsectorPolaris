package data.scripts.util;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.skills.*;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.*;

public class PLSP_DataBase {
	private static final List<String> NormalShipId = new ArrayList<>();
	private static final List<String> DroneShipId = new ArrayList<>();
	private static final List<String> FighterShipId = new ArrayList<>();
	private static final List<String> StationBaseId = new ArrayList<>();
	static {
		NormalShipId.add("PLSP_crescent");
		NormalShipId.add("PLSP_quadrant");
		NormalShipId.add("PLSP_facula");
		NormalShipId.add("PLSP_collapse");
		NormalShipId.add("PLSP_aberration");
		NormalShipId.add("PLSP_blazar");
		NormalShipId.add("PLSP_zodiac");
		NormalShipId.add("PLSP_quasar");
		NormalShipId.add("PLSP_spectrum");
		NormalShipId.add("PLSP_heliumflash");
		NormalShipId.add("PLSP_meridian");
		NormalShipId.add("PLSP_horizon");
		NormalShipId.add("PLSP_transit");
		NormalShipId.add("PLSP_evolution");
		NormalShipId.add("PLSP_lightcone");
		NormalShipId.add("PLSP_celestialobjectscholar");
		NormalShipId.add("PLSP_meteoroid");
		NormalShipId.add("PLSP_pemumbra");
		NormalShipId.add("PLSP_corona");
		NormalShipId.add("PLSP_flocculus");
		NormalShipId.add("PLSP_axis");
		NormalShipId.add("PLSP_magnitude");
		FighterShipId.add("PLSP_perturbation");
		FighterShipId.add("PLSP_redshift");
		FighterShipId.add("PLSP_blueshift");
		FighterShipId.add("PLSP_comettail");
		FighterShipId.add("PLSP_occultation");
		FighterShipId.add("PLSP_subobject");
		FighterShipId.add("PLSP_moonphase");
	}
	
	public static boolean isPLSPStation(ShipAPI ship) {
		return confirmShipID(ship, StationBaseId);
	}
	
	public static boolean isPLSPDrone(ShipAPI ship) {
		return confirmShipID(ship, DroneShipId);
	}
	
	public static boolean isPLSPFighter(ShipAPI ship) {
		return confirmShipID(ship, FighterShipId);
	}
	
	public static boolean isPLSPShip(ShipAPI ship) {
		return confirmShipID(ship, NormalShipId);
	}

	public static List<String> getPLSPNormalShipIds() {
		return new ArrayList<>(NormalShipId);
	}

	public static List<String> getPLSPFighterShipIds() {
		return new ArrayList<>(FighterShipId);
	}

	public static List<String> getPLSPDroneShipIds() {
		return new ArrayList<>(DroneShipId);
	}
	
	private static boolean confirmShipID(ShipAPI ship, List<String> ID) {
		String sid = ship.getHullSpec().getBaseHullId();
		return ID.contains(sid);
	}

	public static Boolean getBool(Map<String, Object> map, String key, Boolean defaultValue) {
		if (map.containsKey(key)) {
			return (Boolean)map.get(key);
		} else {
			return defaultValue;
		}
	}

	public static void put(Map<String, Object> map, String key, boolean bool) {
		map.put(key, bool);
	}

	public static String getString(Map<String, Object> map, String key) {
		if (map.containsKey(key)) {
			return (String) map.get(key);
		} else {
			return "null";
		}
	}

	public static void put(Map<String, Object> map, String key, String str) {
		map.put(key, str);
	}

	private static final Random random = new Random();
	private static final HashMap<String, List<ShipSkillEffect>> SKILL_EFFECTS = new HashMap<>();
	static {
		ShipSkillEffect helmsmanship1 = new Helmsmanship.Level1(); // fuc it
		ShipSkillEffect helmsmanship2 = new Helmsmanship.Level2(); // why no reflect
		List<ShipSkillEffect> helmsmanship = new ArrayList<>(Arrays.asList(helmsmanship1, helmsmanship2));
		SKILL_EFFECTS.put(Skills.HELMSMANSHIP, helmsmanship);

		ShipSkillEffect combat_endurance1 = new CombatEndurance.Level1();
		ShipSkillEffect combat_endurance2 = new CombatEndurance.Level2();
		ShipSkillEffect combat_endurance3 = new CombatEndurance.Level3();
		List<ShipSkillEffect> combat_endurance = new ArrayList<>(Arrays.asList(combat_endurance1, combat_endurance2, combat_endurance3));
		SKILL_EFFECTS.put(Skills.COMBAT_ENDURANCE, combat_endurance);

		ShipSkillEffect impact_mitigation1 = new ImpactMitigation.Level2();
		ShipSkillEffect impact_mitigation2 = new ImpactMitigation.Level4();
		List<ShipSkillEffect> impact_mitigation = new ArrayList<>(Arrays.asList(impact_mitigation1, impact_mitigation2));
		SKILL_EFFECTS.put(Skills.IMPACT_MITIGATION, impact_mitigation);

		ShipSkillEffect damage_control1 = new DamageControl.Level2();
		ShipSkillEffect damage_control2 = new DamageControl.Level3();
		ShipSkillEffect damage_control3 = new DamageControl.Level4();
		List<ShipSkillEffect> damage_control = new ArrayList<>(Arrays.asList(damage_control1, damage_control2, damage_control3));
		SKILL_EFFECTS.put(Skills.DAMAGE_CONTROL, damage_control);

		ShipSkillEffect field_modulation1 = new FieldModulation.Level1();
		ShipSkillEffect field_modulation2 = new FieldModulation.Level2();
		List<ShipSkillEffect> field_modulation = new ArrayList<>(Arrays.asList(field_modulation1, field_modulation2));
		SKILL_EFFECTS.put(Skills.FIELD_MODULATION, field_modulation);

		ShipSkillEffect point_defense1 = new PointDefense.Level1();
		ShipSkillEffect point_defense2 = new PointDefense.Level2();
		List<ShipSkillEffect> point_defense = new ArrayList<>(Arrays.asList(point_defense1, point_defense2));
		SKILL_EFFECTS.put(Skills.POINT_DEFENSE, point_defense);

		ShipSkillEffect target_analysis1 = new TargetAnalysis.Level1();
		ShipSkillEffect target_analysis2 = new TargetAnalysis.Level2();
		ShipSkillEffect target_analysis3 = new TargetAnalysis.Level3();
		List<ShipSkillEffect> target_analysis = new ArrayList<>(Arrays.asList(target_analysis1, target_analysis2, target_analysis3));
		SKILL_EFFECTS.put(Skills.TARGET_ANALYSIS, target_analysis);

		ShipSkillEffect ballistic_mastery1 = new BallisticMastery.Level1();
		ShipSkillEffect ballistic_mastery2 = new BallisticMastery.Level2();
		List<ShipSkillEffect> ballistic_mastery = new ArrayList<>(Arrays.asList(ballistic_mastery1, ballistic_mastery2));
		SKILL_EFFECTS.put(Skills.BALLISTIC_MASTERY, ballistic_mastery);

		ShipSkillEffect systems_expertise1 = new SystemsExpertise.Level1();
		ShipSkillEffect systems_expertise2 = new SystemsExpertise.Level2();
		ShipSkillEffect systems_expertise3 = new SystemsExpertise.Level3();
		ShipSkillEffect systems_expertise4 = new SystemsExpertise.Level4();
		List<ShipSkillEffect> systems_expertise = new ArrayList<>(Arrays.asList(systems_expertise1, systems_expertise2, systems_expertise3, systems_expertise4));
		SKILL_EFFECTS.put(Skills.SYSTEMS_EXPERTISE, systems_expertise);

		ShipSkillEffect missile_specialization1 = new MissileSpecialization.Level1();
		ShipSkillEffect missile_specialization2 = new MissileSpecialization.Level2();
		List<ShipSkillEffect> missile_specialization = new ArrayList<>(Arrays.asList(missile_specialization1, missile_specialization2));
		SKILL_EFFECTS.put(Skills.MISSILE_SPECIALIZATION, missile_specialization);

		ShipSkillEffect gunnery_implants1 = new GunneryImplants.Level1();
		ShipSkillEffect gunnery_implants2 = new GunneryImplants.Level2();
		ShipSkillEffect gunnery_implants3 = new GunneryImplants.Level3();
		List<ShipSkillEffect> gunnery_implants = new ArrayList<>(Arrays.asList(gunnery_implants1, gunnery_implants2, gunnery_implants3));
		SKILL_EFFECTS.put(Skills.GUNNERY_IMPLANTS, gunnery_implants);

		ShipSkillEffect energy_weapon_mastery1 = new EnergyWeaponMastery.Level1();
		List<ShipSkillEffect> energy_weapon_mastery = new ArrayList<>(Arrays.asList(energy_weapon_mastery1));
		SKILL_EFFECTS.put(Skills.ENERGY_WEAPON_MASTERY, energy_weapon_mastery);

		ShipSkillEffect ordnance_expert1 = new OrdnanceExpertise.Level1();
		List<ShipSkillEffect> ordnance_expert = new ArrayList<>(Arrays.asList(ordnance_expert1));
		SKILL_EFFECTS.put(Skills.ORDNANCE_EXPERTISE, ordnance_expert);

		ShipSkillEffect polarized_armor1 = new PolarizedArmor.Level1();
		ShipSkillEffect polarized_armor2 = new PolarizedArmor.Level2();
		ShipSkillEffect polarized_armor3 = new PolarizedArmor.Level3();
		List<ShipSkillEffect> polarized_armor = new ArrayList<>(Arrays.asList(polarized_armor1, polarized_armor2, polarized_armor3));
		SKILL_EFFECTS.put(Skills.POLARIZED_ARMOR, polarized_armor);
	}

	public static List<String> getAllAvailableSkills() {
		return new ArrayList<>(SKILL_EFFECTS.keySet());
	}

	public static List<ShipSkillEffect> getSkillEffects(String id) {
		return SKILL_EFFECTS.get(id);
	}

	public static String pickUnpickedSkill(PersonAPI person, ShipAPI ship) {
		if (person.getFaction().getId().contentEquals("plsp") && isPLSPShip(ship)) {
			if (person.getStats().getSkillLevel(Skills.SYSTEMS_EXPERTISE) == 0) return Skills.SYSTEMS_EXPERTISE;
		}

		WeightedRandomPicker<String> canSelect = new WeightedRandomPicker<>();
		for (String id : getAllAvailableSkills()) {
			switch (id) {
				case Skills.FIELD_MODULATION:
					if (ship.getShield() == null && ship.getPhaseCloak() == null) canSelect.add(id, 0.1f);
					break;
				case Skills.SYSTEMS_EXPERTISE:
					if (ship.getSystem() == null) canSelect.add(id, 0.1f);
					break;
				default:
					canSelect.add(id);
					break;
			}
		}

		while (!canSelect.isEmpty()) {
			String id = canSelect.pickAndRemove();
			if (person.getStats().getSkillLevel(id) == 0) {
				return id;
			}
		}

		return null;
	}

	/*
	public class CompleteSkillEffect {
		String id;
		List<ShipSkillEffect> selfEffect = new ArrayList<>();
		List<ShipSkillEffect> wingsEffect = new ArrayList<>();

		public CompleteSkillEffect(String id, List<ShipSkillEffect> selfEffect) {
			this.id = id;
			this.selfEffect.addAll(selfEffect);
		}

		public CompleteSkillEffect(String id, List<ShipSkillEffect> selfEffect, List<ShipSkillEffect> wingsEffect) {
			this(id, selfEffect);
			this.wingsEffect.addAll(wingsEffect);
		}
	}
	 */
	
	private PLSP_DataBase() {}
}
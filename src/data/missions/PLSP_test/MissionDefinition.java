package data.missions.PLSP_test;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import data.scripts.util.PLSP_DataBase;
import data.scripts.util.PLSP_Util.I18nSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MissionDefinition implements MissionDefinitionPlugin {
	
	public static final I18nSection strings = I18nSection.getInstance("Misc", "PLSP_");

	@Override
	public void defineMission(MissionDefinitionAPI api) {
		api.initFleet(FleetSide.PLAYER, "PLSP", FleetGoal.ATTACK, false, 5);
		api.initFleet(FleetSide.ENEMY, "SIM", FleetGoal.ATTACK, true);
		api.setFleetTagline(FleetSide.PLAYER, strings.get("list"));
		api.setFleetTagline(FleetSide.ENEMY, strings.get("fearpng"));
		api.addBriefingItem(strings.get("variantTest"));

		List<String> ships = PLSP_DataBase.getPLSPNormalShipIds();
		List<String> variants = new ArrayList<>();
		for (String id : Global.getSettings().getAllVariantIds()) {
			ShipVariantAPI variant = Global.getSettings().getVariant(id);
			if (ships.contains(variant.getHullSpec().getHullId()) && !id.startsWith("mission_")) {
				variants.add(id);
			}
		}
		Collections.sort(variants);

		boolean first = true;
		for (String variant : variants) {
			api.addToFleet(FleetSide.PLAYER, variant, FleetMemberType.SHIP, first);
			first = false;
		}
		
		api.addToFleet(FleetSide.ENEMY, "remnant_station2_Standard", FleetMemberType.SHIP, false);

		float width = 9000f;
		float height = 9000f;
		api.initMap(-width, width, -height, height);
	}
}
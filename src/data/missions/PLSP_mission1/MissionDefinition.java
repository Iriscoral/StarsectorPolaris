package data.missions.PLSP_mission1;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {
	
	private static String getString(String key) {
		return Global.getSettings().getString("Misc", "PLSP_" + key);
	}

	@Override
	public void defineMission(MissionDefinitionAPI api) {
		api.initFleet(FleetSide.PLAYER, "SDS", FleetGoal.ATTACK, false, 5);
		api.initFleet(FleetSide.ENEMY, "PLSP", FleetGoal.ATTACK, true);

		api.setFleetTagline(FleetSide.PLAYER, getString("mission1A"));
		api.setFleetTagline(FleetSide.ENEMY, getString("mission1B"));

		api.addBriefingItem(getString("missionbase"));
		api.addBriefingItem(getString("mission1C"));

		api.addToFleet(FleetSide.PLAYER, "falcon_CS", FleetMemberType.SHIP, "SDS Agile", true);
		api.addToFleet(FleetSide.PLAYER, "wolf_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "vigilance_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "vigilance_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "vigilance_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "vigilance_Standard", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.ENEMY, "PLSP_aberration_combat", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "PLSP_aberration_combat", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "PLSP_aberration_assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "PLSP_crescent_assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "PLSP_crescent_assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "PLSP_evolution_combat", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "PLSP_evolution_combat", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "PLSP_evolution_combat", FleetMemberType.SHIP, false);

		float width = 12000f;
		float height = 16000f;
		api.initMap(-width * 0.5f, width * 0.5f, -height * 0.5f, height * 0.5f);
		float minX = -width * 0.5f;
		float minY = -height * 0.5f;
		for (int i = 0; i < 10; i++) {
			float x = (float) Math.random() * width - width * 0.5f;
			float y = (float) Math.random() * height - height * 0.5f;
			float radius = (400f + (float) Math.random() * 1600f) * 0.3f;
			api.addNebula(x, y, radius);
		}
		api.addObjective(minX + width * 0.5f, minY + height * 0.5f, "sensor_array");
		api.addObjective(minX + width * 0.3f, minY + height * 0.75f, "comm_relay");
		api.addObjective(minX + width * 0.7f, minY + height * 0.7f, "nav_buoy");
		api.addAsteroidField(0f, 0f, (float) Math.random() * 360f, width, 10f, 20f, 10);
	}
}
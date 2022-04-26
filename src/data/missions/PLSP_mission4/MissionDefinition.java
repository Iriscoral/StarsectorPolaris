package data.missions.PLSP_mission4;

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
		api.initFleet(FleetSide.PLAYER, "PLSP", FleetGoal.ATTACK, false, 10);
		api.initFleet(FleetSide.ENEMY, "TTS", FleetGoal.ATTACK, true);

		api.setFleetTagline(FleetSide.PLAYER, getString("mission4A"));
		api.setFleetTagline(FleetSide.ENEMY, getString("mission4B"));

		api.addBriefingItem(getString("missionbase"));
		api.addBriefingItem(getString("mission4C"));

		api.addToFleet(FleetSide.PLAYER, "PLSP_axis_combat", FleetMemberType.SHIP, "PLSP Infinite Skies", true);

		api.addToFleet(FleetSide.ENEMY, "odyssey_Lance", FleetMemberType.SHIP, false).getCaptain().setPersonality("reckless");
		api.addToFleet(FleetSide.ENEMY, "doom_Strike", FleetMemberType.SHIP, false).getCaptain().setPersonality("aggressive");
		api.addToFleet(FleetSide.ENEMY, "heron_Attack", FleetMemberType.SHIP, false).getCaptain().setPersonality("aggressive");
		api.addToFleet(FleetSide.ENEMY, "fury_Attack", FleetMemberType.SHIP, false).getCaptain().setPersonality("reckless");
		api.addToFleet(FleetSide.ENEMY, "shrike_Attack", FleetMemberType.SHIP, false).getCaptain().setPersonality("aggressive");
		api.addToFleet(FleetSide.ENEMY, "shrike_Support", FleetMemberType.SHIP, false).getCaptain().setPersonality("aggressive");
		api.addToFleet(FleetSide.ENEMY, "shrike_Support", FleetMemberType.SHIP, false).getCaptain().setPersonality("aggressive");
		api.addToFleet(FleetSide.ENEMY, "afflictor_Strike", FleetMemberType.SHIP, false).getCaptain().setPersonality("aggressive");
		api.addToFleet(FleetSide.ENEMY, "afflictor_Strike", FleetMemberType.SHIP, false).getCaptain().setPersonality("aggressive");
		api.addToFleet(FleetSide.ENEMY, "omen_PD", FleetMemberType.SHIP, false).getCaptain().setPersonality("aggressive");

		float width = 24000f;
		float height = 18000f;
		api.initMap(-width * 0.5f, width * 0.5f, -height * 0.5f, height * 0.5f);
		float minX = -width * 0.5f;
		float minY = -height * 0.5f;
		for (int i = 0; i < 25; i++) {
			float x = (float) Math.random() * width - width * 0.5f;
			float y = (float) Math.random() * height - height * 0.5f;
			float radius = 1500f + (float) Math.random() * 1500f;
			api.addNebula(x, y, radius);
		}
		api.addObjective(minX + width * 0.7f, minY + height * 0.7f, "nav_buoy");
		api.addAsteroidField(0f, 0f, (float) Math.random() * 360f, width, 20f, 70f, 100);
	}
}
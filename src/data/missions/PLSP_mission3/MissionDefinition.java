package data.missions.PLSP_mission3;

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
		api.initFleet(FleetSide.ENEMY, "SDS", FleetGoal.ATTACK, true);

		api.setFleetTagline(FleetSide.PLAYER, getString("mission3A"));
		api.setFleetTagline(FleetSide.ENEMY, getString("mission3B"));

		api.addBriefingItem(getString("missionbase"));
		api.addBriefingItem(getString("mission3C"));
		api.addBriefingItem(getString("mission3D"));

		api.addToFleet(FleetSide.PLAYER, "PLSP_transit_combat", FleetMemberType.SHIP, "PLSP Disintegration", true);
		api.addToFleet(FleetSide.PLAYER, "PLSP_quasar_combat", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "PLSP_pemumbra_assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "PLSP_pemumbra_assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "PLSP_lightcone_support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "PLSP_lightcone_support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "PLSP_blazar_mix", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "PLSP_facula_defensive", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "PLSP_facula_defensive", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "PLSP_quadrant_support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "PLSP_quadrant_support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "PLSP_meteoroid_assault", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.ENEMY, "conquest_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "conquest_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "eagle_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "falcon_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "falcon_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "drover_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hammerhead_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hammerhead_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hammerhead_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sunder_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sunder_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "centurion_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "centurion_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "centurion_Assault", FleetMemberType.SHIP, false);

		api.defeatOnShipLoss("PLSP Disintegration");

		float width = 24000f;
		float height = 20000f;
		api.initMap(-width * 0.5f, width * 0.5f, -height * 0.5f, height * 0.5f);
		float minX = -width * 0.5f;
		float minY = -height * 0.5f;
		for (int i = 0; i < 30; i++) {
			float x = (float) Math.random() * width - width * 0.5f;
			float y = (float) Math.random() * height - height * 0.5f;
			float radius = 500f + (float) Math.random() * 300f; 
			api.addNebula(x, y, radius);
		}
		api.addNebula(minX + width * 0.8f - 1000, minY + height * 0.4f, 2000);
		api.addNebula(minX + width * 0.8f - 1000, minY + height * 0.5f, 2000);
		api.addNebula(minX + width * 0.8f - 1000, minY + height * 0.6f, 2000);
		api.addObjective(minX + width * 0.2f + 400 + 3000, minY + height * 0.2f + 400 + 2000, "sensor_array");
		api.addObjective(minX + width * 0.4f + 2000, minY + height * 0.7f, "sensor_array");
		api.addObjective(minX + width * 0.75f - 2000, minY + height * 0.7f, "comm_relay");
		api.addObjective(minX + width * 0.2f + 3000, minY + height * 0.5f, "nav_buoy");
		api.addObjective(minX + width * 0.3f, minY + height * 0.75f, "comm_relay");
		api.addAsteroidField(0f, 0f, (float) Math.random() * 360f, width, 20f, 70f, 10);
		api.addAsteroidField(minX, minY + height * 0.5f, 0, height, 20f, 70f, 50);
	}
}
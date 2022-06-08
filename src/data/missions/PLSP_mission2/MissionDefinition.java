package data.missions.PLSP_mission2;

import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.combat.EscapeRevealPlugin;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import data.scripts.util.PLSP_Util.I18nSection;

public class MissionDefinition implements MissionDefinitionPlugin {


	
	public static final I18nSection strings = I18nSection.getInstance("Misc", "PLSP_");

	@Override
	public void defineMission(MissionDefinitionAPI api) {
		api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ESCAPE, false, 5);
		api.initFleet(FleetSide.ENEMY, "", FleetGoal.ATTACK, true);

		api.setFleetTagline(FleetSide.PLAYER, strings.get("mission2A"));
		api.setFleetTagline(FleetSide.ENEMY, strings.get("mission2B"));

		api.addBriefingItem(strings.get("mission2C"));
		api.addBriefingItem(strings.get("mission2D"));

		api.addToFleet(FleetSide.PLAYER, "PLSP_flocculus_defensive", FleetMemberType.SHIP, "ISS Fortune", true);
		api.addToFleet(FleetSide.PLAYER, "wolf_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "lasher_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "lasher_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "mule_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "buffalo_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "buffalo_pirates_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "buffalo_d_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "buffalo_d_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tarsus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tarsus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "kite_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "kite_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "kite_pirates_Raider", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "onslaught_Outdated", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_d_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_d_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "falcon_p_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "condor_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sunder_Overdriven", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hammerhead_Overdriven", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hammerhead_Overdriven", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Overdriven", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "mudskipper2_Hellbore", FleetMemberType.SHIP, false);

		api.defeatOnShipLoss("ISS Fortune");

		float width = 24000f;
		float height = 18000f;
		api.initMap(-width * 0.5f, width * 0.5f, -height * 0.5f, height * 0.5f);
		float minX = -width * 0.5f;
		float minY = -height * 0.5f;
		for (int i = 0; i < 10; i++) {
			float x = (float) Math.random() * width - width * 0.5f;
			float y = (float) Math.random() * height - height * 0.5f;
			float radius = 1000f + (float) Math.random() * 1000f; 
			api.addNebula(x, y, radius);
		}
		api.addNebula(minX + width * 0.8f - 2000, minY + height * 0.4f, 2000);
		api.addNebula(minX + width * 0.8f - 2000, minY + height * 0.5f, 2000);
		api.addNebula(minX + width * 0.8f - 2000, minY + height * 0.6f, 2000);
		api.addObjective(minX + width * 0.3f, minY + height * 0.3f, "nav_buoy");
		api.addObjective(minX + width * 0.3f, minY + height * 0.7f, "sensor_array");
		api.addObjective(minX + width * 0.2f, minY + height * 0.5f, "comm_relay");
		api.addAsteroidField(minX, minY + height * 0.5f, 0, height, 20f, 70f, 50);
		BattleCreationContext context = new BattleCreationContext(null, null, null, null);
		context.setInitialEscapeRange(7000f);
		api.addPlugin(new EscapeRevealPlugin(context));
	}
}
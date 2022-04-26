package data.scripts.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin.MagneticFieldParams;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Arrays;
import java.util.ArrayList;

public class Triglav {
	
	private static String getString(String key) {
		return Global.getSettings().getString("Misc", "PLSP_" + key);
	}
	
	public void generate(SectorAPI sector) {
		StarSystemAPI system = sector.createStarSystem("Triglav");
		system.getLocation().set(-2400, 5000);
		system.setBackgroundTextureFilename("graphics/backgrounds/PLSP_background.jpg");
		system.setLightColor(new Color(200, 200, 210));
		system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "PLSP_starsystem_music");

		PlanetAPI star = system.initStar("Triglav", "star_white", 800f, 650f, 3f, 0.75f, 1f);
		star.setCustomDescriptionId("plspstar");

		PlanetAPI PLSP_planet1 = system.addPlanet("PLSP_planet1", star, "Perun", "terran-eccentric", 40, 220, 8000, 120);
		PLSP_planet1.setCustomDescriptionId("PLSP_planet1");
		PLSP_planet1.getSpec().setGlowColor(new Color(255, 255, 255));
		PLSP_planet1.getSpec().setUseReverseLightForGlow(true);
		PLSP_planet1.getSpec().setCloudColor(new Color(220, 220, 220, 150));
		PLSP_planet1.applySpecChanges();

		PlanetAPI PLSP_planet2 = system.addPlanet("PLSP_planet2", star, "Veles", "barren-bombarded", 0, 120, 3200, 160);
		PLSP_planet2.setCustomDescriptionId("PLSP_planet2");
		PLSP_planet2.getSpec().setPlanetColor(new Color(225, 255, 245));
		PLSP_planet2.getSpec().setUseReverseLightForGlow(true);
		PLSP_planet2.getSpec().setCloudColor(new Color(220, 220, 235, 150));
		PLSP_planet2.applySpecChanges();

		PlanetAPI PLSP_planet3 = system.addPlanet("PLSP_planet3", star, "Hors", "rocky_ice", 185, 100, 18000, 400);
		PLSP_planet3.setCustomDescriptionId("PLSP_planet3");
		PLSP_planet3.getSpec().setGlowColor(new Color(255, 255, 255));
		PLSP_planet3.getSpec().setUseReverseLightForGlow(true);
		PLSP_planet3.getSpec().setCloudColor(new Color(200, 200, 200, 150));
		PLSP_planet3.applySpecChanges();

		PlanetAPI PLSP_planet4 = system.addPlanet("PLSP_planet4", star, "Vesna", "gas_giant", 330, 360, 12000, 320);
		PLSP_planet4.setCustomDescriptionId("PLSP_planet4");
		PLSP_planet4.getSpec().setGlowColor(new Color(155, 155, 155));
		PLSP_planet4.getSpec().setUseReverseLightForGlow(true);
		PLSP_planet4.getSpec().setCloudColor(new Color(200, 200, 200, 150));

		PlanetAPI PLSP_planet5 = system.addPlanet("PLSP_planet5", PLSP_planet4, "Belobog", "desert", 60, 80, 900, 50);
		PLSP_planet5.setCustomDescriptionId("PLSP_planet5");
		PLSP_planet5.getSpec().setGlowColor(new Color(245, 255, 250));
		PLSP_planet5.getSpec().setUseReverseLightForGlow(true);
		PLSP_planet5.getSpec().setCloudColor(new Color(255, 180, 100, 200));
		PLSP_planet5.applySpecChanges();

		PlanetAPI PLSP_planet6 = system.addPlanet("PLSP_planet6", PLSP_planet4, "Chernobog", "toxic_cold", 180, 100, 1800, 100);
		PLSP_planet6.setCustomDescriptionId("PLSP_planet6");
		PLSP_planet6.getSpec().setGlowColor(new Color(245, 255, 250));
		PLSP_planet6.getSpec().setUseReverseLightForGlow(true);
		PLSP_planet6.getSpec().setCloudColor(new Color(220, 220, 200, 150));
		PLSP_planet6.applySpecChanges();

		SectorEntityToken PLSP_relay = system.addCustomEntity("PLSP_relay", getString("relay"), "comm_relay", "plsp"); 
		PLSP_relay.setOrbit(Global.getFactory().createCircularOrbit(star, 230, 4300, 130));
		SectorEntityToken PLSP_sensor = system.addCustomEntity("PLSP_sensor", getString("sensor"), "sensor_array", "plsp");
		PLSP_sensor.setOrbit(Global.getFactory().createCircularOrbit(star, 350, 4300, 130));
		SectorEntityToken PLSP_nav = system.addCustomEntity("PLSP_nav", getString("nav"), "nav_buoy", "plsp");
		PLSP_nav.setOrbit(Global.getFactory().createCircularOrbit(star, 110, 4300, 130));
		//PLSP_relay.setCustomDescriptionId("PLSP_relay");

		JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("PLSP_planet1_jump_point", "Veles " + getString("jumpPoint"));
		jumpPoint.setOrbit(Global.getFactory().createCircularOrbit(PLSP_planet2, 0, 1200, 130));
		jumpPoint.setRelatedPlanet(PLSP_planet2);
		jumpPoint.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPoint);
		system.autogenerateHyperspaceJumpPoints(true, true);

		system.addAsteroidBelt(star, 100, 4600, 128, 400, 500);
		system.addAsteroidBelt(star, 120, 4000, 256, 400, 500);
		system.addAsteroidBelt(star, 180, 4200, 128, 400, 500);
		system.addAsteroidBelt(star, 190, 7300, 128, 400, 500);
		system.addAsteroidBelt(star, 200, 7400, 128, 400, 500);
		system.addAsteroidBelt(star, 240, 7600, 256, 400, 500);
		system.addAsteroidBelt(star, 150, 9000, 128, 400, 500);
		system.addAsteroidBelt(PLSP_planet4, 50, 1400, 64, 45, 80);
		system.addAsteroidBelt(PLSP_planet4, 80, 1400, 128, 45, 80);
		system.addRingBand(star, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 7200, 180f);
		system.addRingBand(star, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 4300, 220f);
		system.addRingBand(star, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 8400, 180f);
		system.addRingBand(star, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 8500, 190f);
		system.addRingBand(PLSP_planet4, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 1200, 180f);

		SectorEntityToken PLSP_planet4_field = system.addTerrain(Terrain.MAGNETIC_FIELD,
				new MagneticFieldParams(PLSP_planet4.getRadius() + 200f, // terrain effect band width 
					(PLSP_planet4.getRadius() + 200f) / 2f, // terrain effect middle radius
					PLSP_planet4, // entity that it's around
					PLSP_planet4.getRadius() + 50f, // visual band start
					PLSP_planet4.getRadius() + 300f, // visual band end
					new Color(50, 20, 100, 40), // base color
					0.25f, // probability to spawn aurora sequence, checked once/day when no aurora in progress
					new Color(140, 100, 235),
					new Color(180, 110, 210),
					new Color(150, 140, 190),
					new Color(140, 190, 210),
					new Color(90, 200, 170), 
					new Color(65, 230, 160),
					new Color(20, 220, 70)
				)
		);

		PLSP_planet4_field.setCircularOrbit(PLSP_planet4, 0, 0, 100);
		SectorEntityToken PLSP_nebula = Misc.addNebulaFromPNG("data/campaign/terrain/PLSP_nebula.png", // png
		  0, 0, // center of nebula
		  system, // location to add to
		  "terrain", "nebula_blue", // "nebula_blue", // texture to use, uses xxx_map for map
		  4, 4, StarAge.AVERAGE); // number of cells in texture
		
		MarketAPI PLSP_planet1Market = PLSP_Util.addMarketplace("plsp", PLSP_planet1, 6,
		new ArrayList<>(Arrays.asList("population_6", "terran", "habitable", "trade_center", "ore_rich", "organics_abundant", "volatiles_abundant")),
		new ArrayList<>(Arrays.asList("population", "megaport", "mining", "heavybatteries", "highcommand", "orbitalworks", "starfortress_high")),
		new ArrayList<>(Arrays.asList("open_market", "black_market", "generic_military", "storage")),
		0.3f, true, true, false);

		MarketAPI PLSP_planet2Market = PLSP_Util.addMarketplace("sindrian_diktat", PLSP_planet2, 4,
		new ArrayList<>(Arrays.asList("thin_atmosphere", "hot", "rare_ore_rich", "population_4", "uninhabitable")),
		new ArrayList<>(Arrays.asList("population", "spaceport", "mining", "militarybase", "heavybatteries", "battlestation_mid")),
		new ArrayList<>(Arrays.asList("open_market", "black_market", "generic_military", "storage")),
		0.3f, false, false, false);

		MarketAPI PLSP_planet3Market = PLSP_Util.addMarketplace("pirates", PLSP_planet3, 3,
		new ArrayList<>(Arrays.asList("ruins_vast", "very_cold", "dark", "free_market", "population_3", "uninhabitable")),
		new ArrayList<>(Arrays.asList("population", "spaceport", "mining", "orbitalstation")),
		new ArrayList<>(Arrays.asList("open_market", "black_market", "storage")),
		0.3f, false, false, false);

		MarketAPI PLSP_planet5Market = PLSP_Util.addMarketplace("plsp", PLSP_planet5, 5,
		new ArrayList<>(Arrays.asList("ruins_scattered", "low_gravity", "poor_light", "meteor_impacts", "stealth_minefields", "population_5", "desert")),
		new ArrayList<>(Arrays.asList("population", "spaceport", "refining", "militarybase", "lightindustry", "heavybatteries", "orbitalstation_high")),
		new ArrayList<>(Arrays.asList("open_market", "black_market", "generic_military", "storage")),
		0.3f, false, false, false);

		MarketAPI PLSP_planet6Market = PLSP_Util.addMarketplace("plsp", PLSP_planet6, 5,
		new ArrayList<>(Arrays.asList("rare_ore_abundant", "poor_light", "stealth_minefields", "volatiles_plentiful", "population_5", "toxic_atmosphere")),
		new ArrayList<>(Arrays.asList("population", "megaport", "mining", "militarybase", "waystation", "fuelprod", "orbitalstation_high")),
		new ArrayList<>(Arrays.asList("open_market", "black_market", "storage")),
		0.3f, false, false, true);
	}

	private void spawnOneWayJumpPointForPlanet(PlanetAPI planet) {
		JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(planet.getId() + "_jump_point", planet.getName() + " Jump Point");
		jumpPoint.setRelatedPlanet(planet);
		//jumpPoint.setStandardWormholeToStarOrPlanetVisual(planet);

		jumpPoint.clearDestinations();
		JumpPointAPI.JumpDestination dest = new JumpPointAPI.JumpDestination(planet, planet.getName());
		dest.setMinDistFromToken(planet.getRadius() + 50f);
		dest.setMaxDistFromToken(planet.getRadius() + 400f);
		jumpPoint.addDestination(dest);

		Global.getSector().getHyperspace().addEntity(jumpPoint);

		float range = (float)Math.sqrt(planet.getLocation().x * planet.getLocation().x + planet.getLocation().y * planet.getLocation().y);
		float angle = VectorUtils.getAngle(planet.getStarSystem().getCenter().getLocation(), planet.getLocation());
		if (planet.getOrbit() != null) {
			OrbitAPI orb = Global.getFactory().createCircularOrbit(planet.getStarSystem().getHyperspaceAnchor(),
					angle,
					jumpPoint.getRadius() * 2f + 50f + range * 0.1f,//how far from star
					planet.getOrbit().getOrbitalPeriod());
			jumpPoint.setOrbit(orb);
		} else {
			Vector2f location = MathUtils.getPoint(planet.getStarSystem().getHyperspaceAnchor().getLocation(), angle, range);
			jumpPoint.getLocation().set(location);
		}
	}
}
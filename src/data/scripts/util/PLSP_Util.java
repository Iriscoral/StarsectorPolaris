package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.sun.istack.internal.Nullable;
import data.scripts.plugins.MagicRenderPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PLSP_Util {
	private static final Random random = new Random();

	public static Color HSVtoRGB(float h /* 0 ~ 360 */, float s /* 0 ~ 1 */, float v /* 0 ~ 1 */ ) {
		if (s == 0f) {
			int iv = (int)(v * 255f);
			return new Color(iv, iv, iv);
		}

		while (h > 360f) {
			h -= 360f;
		}

		h /= 60f;
		int i = (int)Math.floor(h);
		float f = h - i;

		int p = (int)(v * (1f - s) * 255f);
		int q = (int)(v * (1f - s * f) * 255f);
		int t = (int)(v * (1f - s * (1f - f)) * 255f);
		int o = (int)(v * 255f);

		switch (i) {
			case 0:
				return new Color(o, t, p);
			case 1:
				return new Color(q, o, p);
			case 2:
				return new Color(p, o, t);
			case 3:
				return new Color(p, q, o);
			case 4:
				return new Color(t, p, o);
			default:
				return new Color(o, p, q);
		}
	}

	public static String md5(String data) {
		StringBuilder sb = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance("md5");
			byte[] md5 = md.digest(data.getBytes(StandardCharsets.UTF_8));

			for (byte b : md5) {
				sb.append(Integer.toHexString(b & 0xff));
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

	public static String retainDecimal(float num, int accuracy) {
		if (accuracy <= 0) return String.valueOf((int)num);

		StringBuilder format = new StringBuilder("#.");
		for (int i = 0; i < accuracy; i++) {
			format.append("0");
		}

		DecimalFormat df = new DecimalFormat(format.toString());
		String afterFormat = df.format(num);
		if (afterFormat.startsWith(".")) {
			afterFormat = "0" + afterFormat;
		}

		return afterFormat;
	}

	public static void light(Vector2f point) {
		light(point, Color.RED);
	}

	public static void light(Vector2f point, Color color) {
		light(point, color, 20f);
	}

	public static void light(Vector2f point, Color color, float size) {
		Global.getCombatEngine().addSmoothParticle(point, new Vector2f(), size, 1f, 1f, color);
	}

	public static void showText(CombatEntityAPI anchor, Vector2f at, float amount) {
		Global.getCombatEngine().addFloatingText(at, "" + amount, 25f, Color.RED, anchor, 1f, 1f);
	}

	public static void showText(CombatEntityAPI anchor, Vector2f at, String text) {
		Global.getCombatEngine().addFloatingText(at, text, 25f, Color.RED, anchor, 1f, 1f);
	}

	public static boolean timesPerSec(float times, float amount) {
		return (float)Math.random() < amount * times;
	}

	public static MarketAPI addMarketplace(String factionID, SectorEntityToken primaryEntity, int size, ArrayList<String> marketConditions, ArrayList<String> marketIndustries, ArrayList<String> submarkets, float tarrif, boolean hasCore, boolean hasForge, boolean hasSynchrotron) {
		EconomyAPI globalEconomy = Global.getSector().getEconomy();
		String marketID = primaryEntity.getId();
		MarketAPI newMarket = Global.getFactory().createMarket(marketID, primaryEntity.getName(), size);
		newMarket.setFactionId(factionID);
		newMarket.setPrimaryEntity(primaryEntity);
		newMarket.getTariff().modifyFlat("generator", tarrif);
		newMarket.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
		if (null != submarkets) {
			for (String market : submarkets) {
				newMarket.addSubmarket(market);
			}
		}
		if (null != marketConditions) {
			for (String condition : marketConditions) {
				newMarket.addCondition(condition);
				if (condition.contentEquals(Conditions.FREE_PORT)) {
					newMarket.setFreePort(true);
				}
			}
		}
		if (null != newMarket.getConditions() && !newMarket.getConditions().isEmpty()) {
			for (MarketConditionAPI condition : newMarket.getConditions()) {
				condition.setSurveyed(true);
			}
		}
		if (null != marketIndustries) {
			for (String industry : marketIndustries) {
				newMarket.addIndustry(industry);
				if (hasCore) {
					if ((float)Math.random() < ((float)size * 0.1f) - 0.5f) { // fuc it
						newMarket.getIndustry(industry).setAICoreId(Commodities.ALPHA_CORE);
					} else if ((float)Math.random() < ((float)size * 0.1f) - 0.25f) {
						newMarket.getIndustry(industry).setAICoreId(Commodities.BETA_CORE);
					} else {
						newMarket.getIndustry(industry).setAICoreId(Commodities.GAMMA_CORE);
					}
				}
				if (hasForge && industry.contentEquals(Industries.ORBITALWORKS)) {
					newMarket.getIndustry(industry).setSpecialItem(new SpecialItemData(Items.PRISTINE_NANOFORGE, null));
				}
				if (hasSynchrotron && industry.contentEquals(Industries.FUELPROD)) {
					newMarket.getIndustry(industry).setSpecialItem(new SpecialItemData(Items.SYNCHROTRON, null));
				}
			}
		}
		globalEconomy.addMarket(newMarket, true);
		primaryEntity.setMarket(newMarket);
		primaryEntity.setFaction(factionID);
		return newMarket;
	}

	public static void phaseOn(ShipAPI ship, String id, float effectLevel, float MaxTimeMult, float shipAlphaMult, MutableShipStatsAPI stats) {
		id = id + "_" + ship.getId();
		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f);
		float accelPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(0f);
		stats.getMaxSpeed().modifyPercent(id, speedPercentMod * effectLevel);
		stats.getAcceleration().modifyPercent(id, accelPercentMod * effectLevel);
		stats.getDeceleration().modifyPercent(id, accelPercentMod * effectLevel);

		float speedMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult();
		float accelMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult();
		stats.getMaxSpeed().modifyMult(id, speedMultMod * effectLevel);
		stats.getAcceleration().modifyMult(id, accelMultMod * effectLevel);
		stats.getDeceleration().modifyMult(id, accelMultMod * effectLevel);

		ship.setPhased(true);
		ship.setExtraAlphaMult(1f - (1f - shipAlphaMult) * effectLevel);
		ship.setApplyExtraAlphaToEngines(true);

		if (MaxTimeMult != 1f) {
			float shipTimeMult = 1f + (MaxTimeMult - 1f) * effectLevel * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
			stats.getTimeMult().modifyMult(id, shipTimeMult);
			if (ship == Global.getCombatEngine().getPlayerShip()) {
				Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
			} else {
				Global.getCombatEngine().getTimeMult().unmodify(id);
			}
		}
	}

	public static void phaseOff(ShipAPI ship, String id, MutableShipStatsAPI stats) {
		id = id + "_" + ship.getId();

		Global.getCombatEngine().getTimeMult().unmodify(id);
		stats.getTimeMult().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);

		ship.setPhased(false);
		ship.setExtraAlphaMult(1f);
		ship.setApplyExtraAlphaToEngines(false);
	}

	public static boolean shieldHit(BeamAPI beam, ShipAPI target) {
		return target.getShield() != null && target.getShield().isOn() && target.getShield().isWithinArc(beam.getTo());
	}

	public static boolean isLosingCR(ShipAPI ship) {
		return ship.getTimeDeployedForCRReduction() >= ship.getMutableStats().getPeakCRDuration().computeEffective(ship.getHullSpec().getNoCRLossTime());
	}

	public static boolean isInRefit() {
		return Global.getCombatEngine().isInCampaign() || Global.getCombatEngine().getCombatUI() == null;
	}

	public static boolean isPlayerOrAllyOwner(CombatEntityAPI entity) {
		return entity.getOwner() == Misc.OWNER_PLAYER;
	}

	public static boolean isExactlyPlayerOwner(ShipAPI ship) {
		return ship.getOwner() == Misc.OWNER_PLAYER && !ship.isAlly();
	}

	public static boolean isPlayerShipOwner(ShipAPI ship) {
		return ship.getOwner() == Global.getCombatEngine().getPlayerShip().getOwner();
	}

	public static void visualEmpStorm(Vector2f point, int empAmount, float empMaxLength, float empMinLength, float stormDistance, float stormRandomDistance, CombatEntityAPI anchor, Color colora, Color colorb, boolean sound) {
		if (colora == null) {
			colora = new Color(70, 170, 255, 155);
		}
		if (colorb == null) {
			colorb = new Color(255, 255, 255);
		}
		for (int i = 0; i < empAmount; i++) {
			float angle1 = MathUtils.getRandomNumberInRange(0f, 360f);
			float angle2 = angle1 + (float) Math.random() * (empMaxLength - empMinLength) + empMinLength;
			float distance = (float) Math.random() * stormRandomDistance + stormDistance;
			Vector2f start = MathUtils.getPointOnCircumference(point, distance, angle1);
			Vector2f end = MathUtils.getPointOnCircumference(point, distance, angle2);
			Global.getCombatEngine().spawnEmpArcVisual(start, anchor, end, null, 20f + (float) Math.random() * 10f, colora, colorb);
			if (sound) {
				Global.getSoundPlayer().playSound("system_emp_emitter_impact", 1f, 1f, point, new Vector2f());
			}
		}
	}

	public static RippleDistortion easyRippleIn(Vector2f location, Vector2f velocity, float size, float intensity, float fadesize) {
		if (intensity == -1f) {
			intensity = size / 4f;
		}
		if (velocity == null) {
			velocity = new Vector2f();
		}
		RippleDistortion ripple = new RippleDistortion(location, velocity);
		ripple.setSize(size);
		ripple.setIntensity(intensity);
		ripple.fadeOutSize(fadesize);
		ripple.fadeInIntensity(fadesize);
		ripple.setFrameRate(60f);

		DistortionShader.addDistortion(ripple);
		return ripple;
	}

	public static RippleDistortion easyRippleOut(Vector2f location, Vector2f velocity, float size, float intensity, float fadesize) {
		return easyRippleOut(location, velocity, size, intensity, fadesize, 60f);
	}

	public static RippleDistortion easyRippleOut(Vector2f location, Vector2f velocity, float size, float intensity, float fadesize, float frameRate) {
		if (intensity == -1f) {
			intensity = size / 4f;
		}
		if (velocity == null) {
			velocity = new Vector2f();
		}
		RippleDistortion ripple = new RippleDistortion(location, velocity);
		ripple.setSize(size);
		ripple.setIntensity(intensity);
		ripple.setFrameRate(frameRate);
		ripple.fadeInSize(fadesize);
		ripple.fadeOutIntensity(fadesize);

		DistortionShader.addDistortion(ripple);
		return ripple;
	}

	public static void addLight(Vector2f center, float size, float intensity, float fadeOut, Color color) {
		if (intensity == -1f) {
			intensity = size / 10f;
		}
		if (fadeOut == -1f) {
			fadeOut = 1f;
		}
		StandardLight light = new StandardLight();
		light.setLocation(center);
		light.setColor(color);
		light.setSize(size);
		light.setIntensity(intensity);
		light.fadeOut(fadeOut);
		LightShader.addLight(light);
	}

	public static void addRandomLightInCircle(Vector2f center, float radius, float size, float intensity, float fadeOut, Color color) {
		if (intensity == -1f) {
			intensity = size / 10f;
		}
		if (fadeOut == -1f) {
			fadeOut = 1f;
		}
		Vector2f lightpoint = MathUtils.getRandomPointInCircle(center, radius);
		StandardLight light = new StandardLight();
		light.setLocation(lightpoint);
		light.setColor(color);
		light.setSize(size);
		light.setIntensity(intensity);
		light.fadeOut(fadeOut);
		LightShader.addLight(light);
	}

	public static void addCustomHitParticleInCircle(Vector2f center, float radius, Color color) {
		Vector2f spawnParticle = MathUtils.getRandomPointInCircle(center, radius);
		Vector2f vel = VectorUtils.getDirectionalVector(center, spawnParticle);
		float dis = MathUtils.getDistance(spawnParticle, center);
		vel.scale(dis * (float)Math.random() * 0.5f);
		Global.getCombatEngine().addHitParticle(spawnParticle, vel, (float)Math.random() * 60f + 30f, (float)Math.random() * 0.4f + 0.6f, (float)Math.random() * 0.3f + 0.4f, color);
	}

	public static Vector2f getPointWithinEntity(CombatEntityAPI entity, int accuracy) {
		for (int i = 0; i < accuracy; i++) {
			Vector2f point = MathUtils.getRandomPointInCircle(entity.getLocation(), entity.getCollisionRadius());
			if (CollisionUtils.isPointWithinBounds(point, entity)) {
				return point;
			}
		}
		return entity.getLocation();
	}

	public static float getTimeToAim(WeaponAPI weapon, Vector2f aimAt) {
		float turnSpeed;
		float time;
		if (Math.abs(weapon.distanceFromArc(aimAt)) >= 10f) {
			turnSpeed = weapon.getShip().getMutableStats().getMaxTurnRate().getModifiedValue();
			time = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), VectorUtils.getAngle(
					weapon.getLocation(), aimAt))) / turnSpeed;
		} else {
			turnSpeed = Math.max(weapon.getTurnRate(),
					weapon.getShip().getMutableStats().getMaxTurnRate().getModifiedValue());
			time = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), VectorUtils.getAngle(
					weapon.getLocation(), aimAt))) / turnSpeed;
		}

		// Divide by zero - can't turn, only a threat if already aimed
		if (Float.isNaN(time) || turnSpeed <= 0f) {
			if (weapon.distanceFromArc(aimAt) == 0) {
				return 0f;
			} else {
				return Float.MAX_VALUE;
			}
		}

		return time;
	}

	public static Vector2f getShipCollisionPoint(Vector2f segStart, Vector2f segEnd, ShipAPI ship) {
		if (ship.getCollisionClass() == CollisionClass.NONE) {
			return null;
		} else {
			ShieldAPI shield = ship.getShield();
			if (shield != null && !shield.isOff()) {
				Vector2f circleCenter = shield.getLocation();
				float circleRadius = shield.getRadius();
				if (MathUtils.isPointWithinCircle(segStart, circleCenter, circleRadius)) {
					return shield.isWithinArc(segStart) ? segStart : getCollisionPointEX(segStart, segEnd, ship);
				} else {
					Vector2f tmp1 = MagicFakeBeam.getCollisionPointOnCircumference(segStart, segEnd, circleCenter, circleRadius);
					return tmp1 != null && shield.isWithinArc(tmp1) ? tmp1 : getCollisionPointEX(segStart, segEnd, ship);
				}
			} else {
				return getCollisionPointEX(segStart, segEnd, ship);
			}
		}
	}

	public static Vector2f getCollisionPointEX(Vector2f lineStart, Vector2f lineEnd, CombatEntityAPI target) {
		if (CollisionUtils.isPointWithinBounds(lineStart, target)) {
			return lineStart;
		}
		return CollisionUtils.getCollisionPoint(lineStart, lineEnd, target);
	}

	public static boolean willProjectileHitShipWithInFrame(DamagingProjectileAPI proj, ShipAPI ship, int frame) {
		Vector2f projVel = proj.getVelocity();
		if (projVel.length() == 0f) return false;
		Vector2f shipVel = ship.getVelocity();
		float projSpeed = projVel.length() * Global.getCombatEngine().getElapsedInLastFrame();
		float shipSpeed = shipVel.length() * Global.getCombatEngine().getElapsedInLastFrame();
		Vector2f relativeLocAfterProjVel = MathUtils.getPointOnCircumference(proj.getLocation(), proj.getCollisionRadius() + projSpeed * frame, VectorUtils.getFacing(projVel));
		Vector2f relativeLocAfterShipVel = MathUtils.getPointOnCircumference(relativeLocAfterProjVel, shipSpeed * frame, VectorUtils.getFacing(shipVel) + 180f);
		return getShipCollisionPoint(proj.getLocation(), relativeLocAfterShipVel, ship) != null;
	}

	public static boolean willProjectileHitShipWithInSec(DamagingProjectileAPI proj, ShipAPI ship, int sec) {
		return willProjectileHitShipWithInFrame(proj, ship, Math.round((float)sec / Global.getCombatEngine().getElapsedInLastFrame()));
	}

	public static List<DamagingProjectileAPI> getProjectilesAndMissilesWithinRange(Vector2f location, float range) {
		List<DamagingProjectileAPI> projectiles = new ArrayList<>();
		for (DamagingProjectileAPI tmp : Global.getCombatEngine().getProjectiles()) {
			if (MathUtils.isWithinRange(tmp.getLocation(), location, range)) {
				projectiles.add(tmp);
			}
		}
		return projectiles;
	}

	public static List<DamagingProjectileAPI> getEnemyProjectilesAndMissilesWithinRange(Vector2f location, float range, int owner) {
		List<DamagingProjectileAPI> projectiles = new ArrayList<>();
		for (DamagingProjectileAPI tmp : getProjectilesAndMissilesWithinRange(location, range)) {
			if (owner != tmp.getOwner()) {
				projectiles.add(tmp);
			}
		}
		return projectiles;
	}

	public static List<DamagingProjectileAPI> getFriendlyProjectilesWithinRange(Vector2f location, float range, int owner) {
		List<DamagingProjectileAPI> projectiles = new ArrayList<>();
		for (DamagingProjectileAPI tmp : Global.getCombatEngine().getProjectiles()) {
			if (tmp instanceof MissileAPI) continue;
			if (tmp.getOwner() != owner) continue;

			if (MathUtils.isWithinRange(tmp.getLocation(), location, range)) {
				projectiles.add(tmp);
			}
		}
		return projectiles;
	}

	public static List<DamagingProjectileAPI> getEnemyProjectilesWithinRange(Vector2f location, float range, int owner) {
		List<DamagingProjectileAPI> projectiles = new ArrayList<>();
		for (DamagingProjectileAPI tmp : Global.getCombatEngine().getProjectiles()) {
			if (tmp instanceof MissileAPI) continue;
			if (tmp.getOwner() == owner) continue;

			if (MathUtils.isWithinRange(tmp.getLocation(), location, range)) {
				projectiles.add(tmp);
			}
		}
		return projectiles;
	}

	public static List<MissileAPI> getAllieMissilesOnMap(CombatEntityAPI entity) {
		List<MissileAPI> missiles = new ArrayList<>();
		for (MissileAPI tmp : Global.getCombatEngine().getMissiles()) {
			if (tmp.getOwner() == entity.getOwner() || tmp.isFizzling()) {
				missiles.add(tmp);
			}
		}
		return missiles;
	}

	public static List<MissileAPI> getNearbyAllieMissiles(CombatEntityAPI entity, float range) {
		List<MissileAPI> missiles = new ArrayList<>();
		for (MissileAPI enemy : getAllieMissilesOnMap(entity)) {
			if (MathUtils.isWithinRange(entity, enemy, range)) {
				missiles.add(enemy);
			}
		}
		return missiles;
	}

	public static MissileAPI getNearestAllieMissile(CombatEntityAPI entity) {
		MissileAPI closest = null;
		float distanceSquared, closestDistanceSquared = Float.MAX_VALUE;
		for (MissileAPI tmp : getAllieMissilesOnMap(entity)) {
			distanceSquared = MathUtils.getDistanceSquared(tmp.getLocation(), entity.getLocation());
			if (distanceSquared < closestDistanceSquared) {
				closest = tmp;
				closestDistanceSquared = distanceSquared;
			}
		}
		return closest;
	}

	public static MissileAPI getNearestMissileWithinRange(CombatEntityAPI entity, float range) {
		MissileAPI missile = AIUtils.getNearestMissile(entity);
		if (missile != null && MathUtils.isWithinRange(entity, missile, range)) {
			return missile;
		}
		return null;
	}

	public static MissileAPI getNearestEnemyMissileWithinRange(CombatEntityAPI entity, float range) {
		MissileAPI missile = AIUtils.getNearestEnemyMissile(entity);
		if (missile != null && MathUtils.isWithinRange(entity, missile, range)) {
			return missile;
		}
		return null;
	}

	public static MissileAPI getNearestAllieMissileWithinRange(CombatEntityAPI entity, float range) {
		MissileAPI missile = getNearestAllieMissile(entity);
		if (missile != null && MathUtils.isWithinRange(entity, missile, range)) {
			return missile;
		}
		return null;
	}

	public static ShipAPI getNearestEnemyWithinRange(CombatEntityAPI entity, float range) {
		ShipAPI enemy = AIUtils.getNearestEnemy(entity);
		if (enemy != null && MathUtils.isWithinRange(entity, enemy, range)) {
			return enemy;
		}
		return null;
	}

	public static List<ShipAPI> getFighters(ShipAPI carrier) {
		return getFighters(carrier, true);
	}

	public static List<ShipAPI> getFighters(ShipAPI carrier, boolean includeReturn) {
		List<ShipAPI> result = new ArrayList<>();
		for (FighterWingAPI wing : carrier.getAllWings()){
			result.addAll(wing.getWingMembers());

			if (includeReturn) {
				for (FighterWingAPI.ReturningFighter ret : wing.getReturning()) {
					result.add(ret.fighter);
				}
			}
		}

		return result;
	}

	public static MarketAPI pickMarket(FactionAPI faction) {
		return pickMarket(faction, null, null);
	}

	public static MarketAPI pickMarket(FactionAPI faction, boolean withPriority) {
		if (withPriority) return pickMarket(faction, "Triglav", "PLSP_planet1");
		return pickMarket(faction);
	}

	public static MarketAPI pickMarket(FactionAPI faction, String prioritySystem, String priorityPlanet) {
		if (priorityPlanet != null) {
			PlanetAPI planetToP = (PlanetAPI)Global.getSector().getEntityById(priorityPlanet);
			if (planetToP != null && planetToP.getMarket() != null && planetToP.getMarket().getFaction() == faction) {
				return planetToP.getMarket();
			}
		}

		Random random = new Random();
		WeightedRandomPicker<MarketAPI> markets = new WeightedRandomPicker<>(random);
		for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			float base = 0.1f;
			if (prioritySystem != null && system.getId().contentEquals(prioritySystem)) {
				base = 100000f;
			}

			for (PlanetAPI planet : system.getPlanets()) {
				if (planet.getMarket() != null && planet.getMarket().getFaction() == faction) {
					markets.add(planet.getMarket(), base * planet.getMarket().getSize());
				}
			}
			for (SectorEntityToken entity : system.getEntitiesWithTag(Tags.STATION)) {
				if (entity.getMarket() != null && entity.getMarket().getFaction() == faction) {
					markets.add(entity.getMarket(), base * entity.getMarket().getSize());
				}
			}
		}

		return markets.pick();
	}

	public static void AOE(ShipAPI source, Vector2f centerPoint, float damage, DamageType damageType, ShipAPI target) {
		AOE(source, centerPoint, damage, damageType, Collections.singletonList(target));
	}

	public static void AOE(ShipAPI source, Vector2f centerPoint, float damage, float empDamage, DamageType damageType, ShipAPI target) {
		AOE(source, centerPoint, damage, empDamage, damageType, Collections.singletonList(target));
	}

	public static void AOE(ShipAPI source, Vector2f centerPoint, float damage, DamageType damageType, List<ShipAPI> targets) {
		AOE(source, centerPoint, damage, 0f, damageType, targets);
	}

	public static void AOE(ShipAPI source, Vector2f centerPoint, float damage, float empDamage, DamageType damageType, List<ShipAPI> targets) {
		for (ShipAPI target : targets) {
			float checkRange = target.getCollisionRadius() * 1.5f;
			if (target.getShield() != null) {
				checkRange = Math.max(checkRange, target.getShield().getRadius());
			}
			List<Vector2f> toHitLocations = new ArrayList<>();
			float horiAngle = MathUtils.clampAngle(VectorUtils.getAngle(centerPoint, target.getLocation()));
			float verticalAngle = MathUtils.clampAngle(horiAngle - 90f);
			for (float i = -checkRange - 10f; i <= checkRange + 10f; i += 20f) {
				Vector2f targetMiddleV2f = MathUtils.getPointOnCircumference(target.getLocation(), i, verticalAngle);
				Vector2f a_V2f = MathUtils.getPointOnCircumference(targetMiddleV2f, -checkRange, horiAngle);
				Vector2f b_V2f = MathUtils.getPointOnCircumference(targetMiddleV2f, checkRange, horiAngle);
				Vector2f collisionPoint = getCollisionPointEX(a_V2f, b_V2f, target);
				if (collisionPoint != null) {
					toHitLocations.add(collisionPoint);
				}
			}
			if (!toHitLocations.isEmpty()) {
				for (Vector2f damagePoint : toHitLocations) {
					float dis = MathUtils.getDistance(damagePoint, centerPoint);
					Global.getCombatEngine().applyDamage(target, damagePoint, damage * (1f - Math.min(1f, dis / 2000f)), damageType, empDamage, false, false, source);
				}
			}
		}
	}

	public static void simpleFrameRender(SpriteAPI sprite, Vector2f loc, CombatEngineLayers layer) {
		MagicRenderPlugin.addSingleframe(sprite, loc, layer);
	}

	public static void simpleMultiFrameRender(SpriteAPI sprite, Vector2f loc, float fadein, float full, float fadeout, CombatEngineLayers layer) {
		MagicRenderPlugin.addBattlespace(sprite, loc, null, null, 0f, fadein, fadein + full, fadein + full + fadeout, layer);
	}

	public static void simpleObjectBasedRender(SpriteAPI sprite, CombatEntityAPI anchor, float fadein, float full, float fadeout, CombatEngineLayers layer) {
		Vector2f loc = new Vector2f(anchor.getLocation());
		MagicRenderPlugin.addObjectspace(sprite, anchor, loc, new Vector2f(), null, null, 180f, 0f, true, fadein, fadein + full, fadein + full + fadeout, true, layer);
	}

	public static boolean NEX() {
		return Global.getSettings().getModManager().isModEnabled("nexerelin");
	}

	public static ShipAPI findSingleEnemy(ShipAPI ship, float maxRange) {
		return findSingleEnemy(ship, maxRange, true, false, null, null);
	}

	public static ShipAPI findSingleEnemy(ShipAPI ship, float maxRange, boolean controllable, boolean greedy, Misc.FindShipFilter filter, Preference preference) {
		ShipAPI potentialTarget = ship.getShipTarget() != null && ship.getShipTarget().getOwner() != ship.getOwner() && ship.getShipTarget().isAlive() ? ship.getShipTarget() : null;
		ShipAPI target = controllable ? potentialTarget : null;
		boolean targetPickedByControl = target != null;

		if (targetPickedByControl) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > maxRange + radSum) target = null;
			else if (filter != null && !filter.matches(target)) target = null;
		} else if (greedy) {
			if (ship == Global.getCombatEngine().getPlayerShip()) {
				target = findClosestShipEnemyOf(ship, ship.getMouseTarget(), maxRange, filter, preference);
			}

			if (target == null) {
				target = findClosestShipEnemyOf(ship, ship.getLocation(), maxRange, filter, preference);
			}
		}

		if (targetPickedByControl && target == null && greedy) {
			findSingleEnemy(ship, maxRange, false, true, filter, preference);
		}

		return target;
	}

	public static ShipAPI findClosestShipEnemyOf(ShipAPI ship, Vector2f locFromForSorting, float maxRange, Misc.FindShipFilter filter, Preference preference) {
		float minDist = Float.MAX_VALUE;
		ShipAPI closest = null;

		for (ShipAPI other : AIUtils.getEnemiesOnMap(ship)) {
			if (filter != null && !filter.matches(other)) continue;

			float dist = MathUtils.getDistance(ship.getLocation(), other.getLocation());
			float distSort = MathUtils.getDistance(locFromForSorting, other.getLocation());
			float radSum = ship.getCollisionRadius() + other.getCollisionRadius();

			dist -= preference == null ? 0f : preference.getPreference(other);
			if (dist > maxRange + radSum) continue;
			if (distSort < minDist) {
				closest = other;
				minDist = distSort;
			}
		}
		return closest;
	}

	public static float getFPWorthOfHostility(ShipAPI ship, float range) {
		float retVal = 0f;
		for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, range)) {
			float colDist = ship.getCollisionRadius() + enemy.getCollisionRadius();
			float distance = Math.max(0f, MathUtils.getDistance(ship, enemy) - colDist);
			float maxRange = Math.max(1f, range - colDist);
			retVal += getFPStrength(enemy) * (1f - distance / maxRange);
		}
		return retVal;
	}

	public static float getFPStrength(ShipAPI ship) {
		DeployedFleetMemberAPI member = Global.getCombatEngine().getFleetManager(ship.getOwner()).getDeployedFleetMember(ship);
		return (member == null || member.getMember() == null) ? 0f : member.getMember().getMemberStrength();
	}

	public static float getDangerLevel(StarSystemAPI system, FactionAPI faction, CampaignFleetAPI theFleet) {
		float enemyLevel = 0f;
		float friendlyLevel = 0f;
		for (CampaignFleetAPI fleet : system.getFleets()) {
			if (fleet.getFaction().isHostileTo(faction)) {
				float fleetlevel = 0f;
				for (FleetMemberAPI member : fleet.getFleetData().getMembersInPriorityOrder()) {
					if (member.isCivilian() || !member.canBeDeployedForCombat()) {
						continue;
					}
					fleetlevel += member.getMemberStrength();
					if (fleet.isPlayerFleet()) {
						fleetlevel += member.getMemberStrength();
						fleetlevel += 2f;
					}
				}
				enemyLevel += fleetlevel;
			} else if (fleet.getFaction() == faction) {
				if (theFleet == null || fleet != theFleet) {
					float fleetlevel = 0f;
					for (FleetMemberAPI member : fleet.getFleetData().getMembersInPriorityOrder()) {
						if (member.isCivilian() || !member.canBeDeployedForCombat()) {
							continue;
						}
						fleetlevel += member.getMemberStrength();
					}
					friendlyLevel += fleetlevel;
				}
			}
		}
		if (friendlyLevel <= 0f) {
			return 0f;
		}
		return Math.round(enemyLevel / friendlyLevel);
	}

	public static float getDangerLevel(StarSystemAPI system, FactionAPI faction) {
		return getDangerLevel(system, faction, null);
	}

	public static void addOfficers(CampaignFleetAPI fleet, int numOfficers, int maxOfficerLevel, int eliteSkillNum, Random random) {
		List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
		if (members.isEmpty()) return;

		OfficerLevelupPlugin plugin = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
		if (numOfficers < 1) numOfficers = 1;
		if (maxOfficerLevel < 1) maxOfficerLevel = 1;

		WeightedRandomPicker<FleetMemberAPI> picker = new WeightedRandomPicker<>(random);
		for (FleetMemberAPI member : members) {
			if (member.isFighterWing()) continue;
			if (member.isFlagship()) continue;
			if (member.isCivilian()) continue;
			if (!member.getCaptain().isDefault()) continue;

			float weight = (float) member.getFleetPointCost();
			picker.add(member, weight);
		}

		if (picker.isEmpty()) return;

		FleetMemberAPI flagship = fleet.getFlagship();
		if (flagship == null) flagship = members.get(0);
		picker.remove(flagship);

		// int commanderOfficerLevelBonus = (int) commander.getStats().getDynamic().getMod(Stats.OFFICER_MAX_LEVEL_MOD).computeEffective(0);
		int commanderOfficerLevelBonus = 0;
		int officerLevelLimit = plugin.getMaxLevel(null) + commanderOfficerLevelBonus;
		for (int i = 0; i < numOfficers; i++) {
			FleetMemberAPI member = picker.pickAndRemove();
			if (member == null) break;

			int level = maxOfficerLevel - random.nextInt(3);
			if (level < 1) level = 1;
			if (level > officerLevelLimit) level = officerLevelLimit;

			SkillPickPreference pref = FleetFactoryV3.getSkillPrefForShip(member);
			PersonAPI person = OfficerManagerEvent.createOfficer(fleet.getFaction(), level, pref, false, fleet, true, true, eliteSkillNum, random);
			if (person.getPersonalityAPI().getId().equals(Personalities.TIMID)) {
				person.setPersonality(Personalities.CAUTIOUS);
			}

			member.setCaptain(person);
		}
	}

	public static void inflate(CampaignFleetAPI fleet, FleetParamsV3 params) {
		DefaultFleetInflaterParams p;
		if (fleet.getInflater() != null && fleet.getInflater().getParams() instanceof DefaultFleetInflaterParams) {
			p = (DefaultFleetInflaterParams)fleet.getInflater().getParams();
		} else {
			p = new DefaultFleetInflaterParams();
		}

		p.persistent = true;
		p.seed = random.nextLong();
		p.timestamp = params.timestamp;
		p.allWeapons = params.allWeapons;

		p.quality = params.qualityOverride != null ? params.qualityOverride : params.quality + params.qualityMod;
		p.mode = params.modeOverride != null ? params.modeOverride : params.mode;
		p.factionId = params.factionId != null ? params.factionId : fleet.getFaction().getId();
		p.averageSMods = params.averageSMods;

		if (fleet.getInflater() == null) {
			FleetInflater inflater = Global.getSector().getPluginPicker().pickFleetInflater(fleet, p);
			fleet.setInflater(inflater);
		}

		fleet.getInflater().inflate(fleet);
	}

	public static class Preference {
		public float fighter = 0f;
		public float frigate = 0f;
		public float destroyer = 0f;
		public float cruiser = 0f;
		public float battleship = 0f;

		public float carrier = 0f;
		public float phase = 0f;
		public float stationOrModule = 0f;

		public float getPreference(ShipAPI target) {
			float total = 0;

			if (target.isStation() || target.isStationModule()) {
				total += stationOrModule;
			} else if (target.isCapital()) {
				total += battleship;
			} else if (target.isCruiser()) {
				total += cruiser;
			} else if (target.isDestroyer()) {
				total += destroyer;
			} else if (target.isFrigate()) {
				total += frigate;
			} else {
				total += fighter;
			}

			if (target.getShield() == null && target.getPhaseCloak() != null) {
				total += phase;
			}
			if (target.getNumFighterBays() > 0) {
				total += carrier;
			}

			return total;
		}
	}

	public static class I18nSection {
		private final String category;
		private final String keyPrefix;

		public I18nSection(String category, String keyPrefix) {
			this.category = category;
			if (keyPrefix != null) {
				this.keyPrefix = keyPrefix;
			} else {
				this.keyPrefix = "";
			}

			sections.add(this);
		}

		public I18nSection(String category) {
			this(category, null);
		}

		public String format(String keyMainBody, @Nullable Object... args) {
			if (args != null && args.length > 0) {
				return absFormat(keyMainBody, args);
			}
			return get(keyMainBody);
		}

		public String get() {
			try {
				return Global.getSettings().getString(category, keyPrefix);
			} catch (Exception e) {
				return "[NULL]";
			}
		}

		public String get(String key) {
			try {
				return Global.getSettings().getString(category, keyPrefix + key);
			} catch (Exception e) {
				return "[NULL]";
			}
		}

		private String absFormat(String key, Object... args) {
			String result;
			try {
				result = String.format(get(key), args);
			} catch (Exception e) {
				return "[NULL]";
			}

			return result;
		}

		private static final List<I18nSection> sections = new ArrayList<>();
		public static I18nSection getInstance(String category, String keyPrefix) {
			for (I18nSection section : sections) {
				if (section.category.contentEquals(category) && section.keyPrefix.contentEquals(keyPrefix)) {
					return section;
				}
			}

			return new I18nSection(category, keyPrefix);
		}
	}
}
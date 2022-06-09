package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import data.scripts.plugins.MagicRenderPlugin;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class PLSP_DisplacerStats extends BaseShipSystemScript {

	private static final Color NORMAL_COLOR = new Color(185, 220, 225);
	private static final Color RARE_COLOR = new Color(80, 105, 115);
	private static final Vector2f ZERO = new Vector2f();
	private static final float RANGE_FACTOR = 1000f;

	private DisplacerState data = null;

	public static float getRange(ShipAPI ship) {
		if (ship == null) return RANGE_FACTOR;
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE_FACTOR);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();
		 if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
			return;
		}

		if (data == null) {
			data = new DisplacerState(ship);
		} else {
			float amount = engine.getElapsedInLastFrame();

			float alphaLevel = Math.max(1f - effectLevel * 2f, 0f);
			ship.setExtraAlphaMult(alphaLevel);

			if (state == State.IN) {

				if (PLSP_Util.timesPerSec(50f * alphaLevel, amount)) {
					float dis = MathUtils.getRandomNumberInRange(10f, 250f);
					Vector2f point = MathUtils.getRandomPointOnCircumference(ship.getLocation(), dis);
					Vector2f vel = Vector2f.sub(point, ship.getLocation(), null);
					vel.scale(5f);
					float size = (float)Math.random() * 20f + 10f;
					float brightness = (float)Math.random() * 0.2f + 0.8f;
					Global.getCombatEngine().addSmoothParticle(point, vel, size, brightness, 0.25f, PLSP_ColorData.SHALLOW_BLUE);
				}

				data.trailTimer -= amount * alphaLevel;
				if (data.trailTimer < 0f) {
					float angle = data.expectedAngle + 270f * (float)Math.random() - 270f * (float)Math.random();
					float radius = ship.getCollisionRadius() * (0.2f + 0.5f * (float) Math.random());
					Vector2f spawnLocation = MathUtils.getPointOnCircumference(ship.getLocation(), radius, angle);

					float startSize = 30f + 30f * (float) Math.random();
					float endSize = 5f + 10f * (float) Math.random();

					SpriteAPI spriteTexture = Global.getSettings().getSprite("fx", "PLSP_trails_clean");
					float opacity = 0.5f + 0.5f * (float)Math.random();

					float mixFactor = (float)Math.random();
					Color color = Misc.interpolateColor(NORMAL_COLOR, RARE_COLOR, mixFactor);
					color = Misc.interpolateColor(color, PLSP_ColorData.BRIGHT_EMP_ARC_FRINGE, mixFactor);

					PLSP_DisplacerLine lineObject = new PLSP_DisplacerLine(ship, data.targetLocation, spawnLocation, angle, 1200f * (1f + effectLevel), 210f * (1f + effectLevel), spriteTexture, startSize, endSize, color, opacity);
					data.lines.add(lineObject);

					data.trailTimer = 0.03f;
				}
			}

			if (data.lastAlphaLevel <= 0f && alphaLevel > 0f) {
				for (PLSP_DisplacerLine entity : data.lines) {
					entity.updateDestination(ship.getLocation());
				}
			}

			if (data.lastAlphaLevel <= 0.2f && alphaLevel > 0.2f) {

				SpriteAPI vortex = Global.getSettings().getSprite("misc", "PLSP_chaosVortex");
				vortex.setAngle((float)Math.random() * 360f);
				vortex.setSize(ship.getCollisionRadius() * 3f, ship.getCollisionRadius() * 3f);
				vortex.setColor(PLSP_ColorData.BRIGHT_EMP_ARC_FRINGE);
				vortex.setAdditiveBlend();
				modifiedObjectBasedRender(vortex, ship, 2f, 0.1f, 0.3f, 0.2f, CombatEngineLayers.BELOW_SHIPS_LAYER);

				PLSP_Util.easyRippleOut(ship.getLocation(), ZERO, ship.getCollisionRadius() * 3f, 500f, 0.5f, 120f);
				for (DamagingProjectileAPI proj : PLSP_Util.getEnemyProjectilesAndMissilesWithinRange(ship.getLocation(), ship.getCollisionRadius() * 2f, ship.getOwner())) {
					Global.getCombatEngine().addSmoothParticle(proj.getLocation(), ZERO, 2f + (float)Math.random() * 5f + proj.getCollisionRadius(), 2f, 0.5f, PLSP_ColorData.NORMAL_BLUE);
					Global.getCombatEngine().addSmoothParticle(proj.getLocation(), proj.getVelocity(), proj.getCollisionRadius() + 20f, (float)Math.random() * 0.2f + 0.8f, 0.25f, PLSP_ColorData.BRIGHT_EMP_ARC_FRINGE);
					Global.getCombatEngine().removeEntity(proj);
				}

				for (CombatEntityAPI asteroid : CombatUtils.getAsteroidsWithinRange(ship.getLocation(), ship.getCollisionRadius() * 2f)) {
					Global.getCombatEngine().addSmoothParticle(asteroid.getLocation(), ZERO, 2f + (float)Math.random() * 5f + asteroid.getCollisionRadius(), 2f, 0.5f, PLSP_ColorData.NORMAL_BLUE);
					Global.getCombatEngine().addSmoothParticle(asteroid.getLocation(), asteroid.getVelocity(), asteroid.getCollisionRadius() + 20f, (float)Math.random() * 0.2f + 0.8f, 0.25f, PLSP_ColorData.BRIGHT_EMP_ARC_FRINGE);
					Global.getCombatEngine().removeEntity(asteroid);
				}
			}

			data.lastAlphaLevel = alphaLevel;
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) {
			return;
		}

		if (data != null) {
			ship.setExtraAlphaMult(1f);
			data.lines.clear();

			data = null;
		}
	}

	public static void modifiedObjectBasedRender(SpriteAPI sprite, ShipAPI anchor, float mult, float fadein, float full, float fadeout, CombatEngineLayers layer) {
		Vector2f loc = new Vector2f(anchor.getLocation());
		float size = sprite.getWidth();
		MagicRenderPlugin.addObjectspace(sprite, anchor, loc, new Vector2f(), null, new Vector2f(size * mult, size * mult), 180f, 0f, true, fadein, fadein + full, fadein + full + fadeout, true, layer);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}

	public final static class DisplacerState {
		private float lastAlphaLevel;

		private float trailTimer;
		private List<PLSP_DisplacerLine> lines;

		private float expectedAngle;
		private Vector2f targetLocation;

		private DisplacerState(ShipAPI ship) {
			lastAlphaLevel = 1f;

			trailTimer = 0.03f;
			lines = new ArrayList<>();

			Vector2f vel = ship.getVelocity();
			expectedAngle = ship.getFacing();
			if (!VectorUtils.isZeroVector(vel)) {
				expectedAngle = VectorUtils.getFacing(vel);
			}

			targetLocation = MathUtils.getPointOnCircumference(ship.getLocation(), getRange(ship), expectedAngle);
		}
	}
}
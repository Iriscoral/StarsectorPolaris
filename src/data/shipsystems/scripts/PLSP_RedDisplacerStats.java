package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.MagicLensFlare;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class PLSP_RedDisplacerStats extends BaseShipSystemScript {

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

		float amount = Global.getCombatEngine().getElapsedInLastFrame();
		if (PLSP_Util.timesPerSec(3f, amount * effectLevel)) {
			Vector2f point = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 1.2f);
			float angle = (float)Math.random() * 360f;
			float length = ship.getCollisionRadius() * ((float)Math.random() * 2f + effectLevel * 0.5f + 0.5f);
			MagicLensFlare.createSharpFlare(Global.getCombatEngine(), ship, point, 4f, length, angle, PLSP_ColorData.RED, PLSP_ColorData.BRIGHT_EMP_ARC_CORE);
		}

		if (PLSP_Util.timesPerSec(3f, amount)) {
			float dis = MathUtils.getRandomNumberInRange(10f, 250f);
			Vector2f point = MathUtils.getRandomPointOnCircumference(ship.getLocation(), dis);
			Vector2f vel = Vector2f.sub(point, ship.getLocation(), null);
			vel.scale(5f);
			float size = (float)Math.random() * 15f + 10f;
			float brightness = (float)Math.random() * 0.2f + 0.8f;
			Global.getCombatEngine().addSmoothParticle(point, vel, size, brightness, 0.25f, PLSP_ColorData.LIGHT_RED);
		}

		if (effectLevel == 1f) {
			Global.getCombatEngine().spawnExplosion(ship.getLocation(), new Vector2f(), PLSP_ColorData.LIGHT_RED, ship.getCollisionRadius() * 4f, 2f);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}
}
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PLSP_StreamVisualEffect implements EveryFrameWeaponEffectPlugin {
	private static final String id = "PLSP_StreamVisual";
	private float lastAngle = 0f;
	private float lastChargeLevel = 0f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (!engine.getCustomData().containsKey(id)) {
			engine.getCustomData().put(id, new HashMap<>());
		}

		Map<WeaponAPI, StreamState> weaponsMap = (Map)engine.getCustomData().get(id);
		if (weapon.getShip() == null || !engine.isEntityInPlay(weapon.getShip()) || !weapon.getShip().isAlive()) {
			if ((weapon.getShip() == null || !weapon.getShip().isAlive()) && weaponsMap.containsKey(weapon)) {
				StreamState data = weaponsMap.get(weapon);
				data.resetAlpha();
				weaponsMap.remove(weapon);
			}
			return;
		}

		if (!weaponsMap.containsKey(weapon)) {
			weaponsMap.put(weapon, new StreamState());
		} else {
			StreamState data = weaponsMap.get(weapon);

			float angle = weapon.getCurrAngle();
			float chargeLevel = weapon.getChargeLevel();
			if (chargeLevel > 0.1f && chargeLevel < 0.999f && chargeLevel > lastChargeLevel) {
				Vector2f from = weapon.getLocation();
				Vector2f to = MathUtils.getPoint(from, weapon.getRange(), angle);
				Vector2f collisionPoint = getBeamCollisionPoint(from, to, weapon.getRange(), weapon.getShip());

				Vector2f midPoint = MathUtils.getMidpoint(from, collisionPoint);
				float range = MathUtils.getDistance(from, collisionPoint);
				float alpha = (chargeLevel - 0.1f) * chargeLevel;

				float startX = data.seed + chargeLevel * 10f * amount;
				if (startX > 10000f) {
					startX -= 10000f;
				}

				data.sprite.setAlphaMult(alpha);
				data.sprite.setAngle(angle);
				data.frac = range / data.sprite.getWidth();
				data.fixedPoint = MathUtils.getPoint(midPoint, data.sprite.getWidth() * startX + range * 0.5f - data.sprite.getWidth() * 0.5f, angle + 180f);
				data.seed = startX;
			} else {
				data.resetAlpha();
			}

			if (weapon.isFiring()) {
				//weapon.setCurrAngle(angle * 0.25f + lastAngle * 0.75f);
			}

			lastAngle = angle;
			lastChargeLevel = chargeLevel;
		}
	}

	private static Vector2f getBeamCollisionPoint(Vector2f from, Vector2f end, float range, ShipAPI source) {
		Vector2f point = end;
		float shortestDistant = Float.MAX_VALUE;

		List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(from, range + 500f);
		for (CombatEntityAPI entity : entities) {
			if (entity == source || entity.getCollisionClass() == CollisionClass.ASTEROID) continue;
			float distant = MathUtils.getDistance(entity, from);
			if (distant > shortestDistant) continue;

			Vector2f collisionPoint;
			if (entity instanceof ShipAPI) {
				collisionPoint = PLSP_Util.getShipCollisionPoint(from, end, (ShipAPI)entity);
			} else {
				collisionPoint = CollisionUtils.getCollisionPoint(from, end, entity);
			}

			if (collisionPoint != null) {
				shortestDistant = distant;
				point = collisionPoint;
			}
		}

		return point;
	}

	public final static class StreamState {
		public float frac;
		public float seed;
		public Vector2f fixedPoint = new Vector2f();
		public SpriteAPI sprite;

		public StreamState() {
			frac = 0f;
			seed = 0f;
			sprite = Global.getSettings().getSprite("misc", "PLSP_stream_visual");
		}

		public void resetAlpha() {
			sprite.setAlphaMult(0f);
		}
	}
}
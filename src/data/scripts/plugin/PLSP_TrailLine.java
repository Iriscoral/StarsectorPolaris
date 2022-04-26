package data.scripts.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugins.MagicTrailPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class PLSP_TrailLine extends SimpleEntity implements CombatLayeredRenderingPlugin {

	private boolean expired = false;
	private boolean expiredInstant = true;
	private boolean expiredWhenReachedTargetLocation = true;
	private boolean reachedTargetLocation = false;
	private CombatEntityAPI renderCenter;

	private boolean expiring = false;
	private float effectLevel = 1f;

	private final Vector2f location;
	private final Vector2f velocity;
	private float facing;
	private float angleVel;

	private float uid;
	private SpriteAPI spriteTexture;
	private Vector2f targetLocation;
	private float accuracy;

	private float elapsed = 0f;
	private float timeLimit;

	private Color color;
	private float opacity;

	private float in;
	private float full;
	private float out;
	private float startSize;
	private float endSize;

	public PLSP_TrailLine(Vector2f targetLocation, float accuracy, float timeLimit, Vector2f location, float facing, float in, float full, float out, float startSize, float endSize, float speed, float angleVel, SpriteAPI spriteTexture, Color color, float opacity) {
		super(location);

		this.targetLocation = targetLocation; // not new, and can be null
		this.accuracy = accuracy;
		this.timeLimit = timeLimit; // -1 for unlimited time

		this.location = new Vector2f(location);
		this.facing = facing;

		this.in = in;
		this.full = full;
		this.out = out;
		this.startSize = startSize;
		this.endSize = endSize;

		this.velocity = MathUtils.getPoint(null, speed, facing);
		this.angleVel = angleVel;

		this.uid = MagicTrailPlugin.getUniqueID();
		this.spriteTexture = spriteTexture;
		this.color = color;
		this.opacity = opacity;

		this.renderCenter = Global.getCombatEngine().addLayeredRenderingPlugin(this);
	}

	public boolean updateTargetLocation(Vector2f targetLocation, boolean forcedUpdate) {
		if (targetLocation == null) {
			this.targetLocation = null;
			this.reachedTargetLocation = false;
			return true;
		}

		if (forcedUpdate || MathUtils.getDistance(targetLocation, this.targetLocation) > accuracy) {
			this.targetLocation = targetLocation;
			this.reachedTargetLocation = false;
			return true;
		}

		return false;
	}

	public void resize(float startSize, float endSize) {
		this.startSize = startSize;
		this.endSize = endSize;
	}

	public void velocityScale(float mult, boolean largerAngleImpact) {
		this.velocity.scale(mult);
		this.angleVel *= mult;

		if (largerAngleImpact) {
			this.angleVel *= 1f + mult * 0.25f;
		}
	}

	public void resetElapsed() {
		this.elapsed = 0f;
	}

	public boolean isExpiredWhenReachedTargetLocation() {
		return expiredWhenReachedTargetLocation;
	}

	public void setExpiredWhenReachedTargetLocation(boolean expiredWhenReachedTargetLocation) {
		this.expiredWhenReachedTargetLocation = expiredWhenReachedTargetLocation;
	}

	public boolean isReachedTargetLocation() {
		return reachedTargetLocation;
	}

	public void setExpiredInstant(boolean expiredInstant) {
		this.expiredInstant = expiredInstant;
	}

	public boolean isExpiredInstant() {
		return expiredInstant;
	}

	@Override
	public final void advance(float amount) {
		if (expired) return;

		elapsed += amount;

		if (targetLocation != null && !reachedTargetLocation) {
			float angleToTarget = VectorUtils.getAngle(location, targetLocation);
			float angleDif = MathUtils.getShortestRotation(facing, angleToTarget);
			if (Math.abs(angleDif) > amount * angleVel) {
				facing += amount * angleVel * Math.signum(angleDif);
			} else {
				facing += angleDif;
			}
		}

		facing = MathUtils.clampAngle(facing);
		velocity.set(MathUtils.getPoint(null, velocity.length(), facing));

		location.x += velocity.x * amount;
		location.y += velocity.y * amount;
		renderCenter.getLocation().set(location);

		if (expiring) effectLevel -= amount;
		if (effectLevel <= 0f) expired = true;
		effectLevel = Math.max(effectLevel, 0f);

		MagicTrailPlugin.AddTrailMemberSimple(this, uid, spriteTexture, location, 0f, facing, startSize, endSize, color, opacity * effectLevel, in, full, out, true);
		advanceImpl(amount);

		boolean timeout = timeLimit > 0f && elapsed > timeLimit;
		if (timeout) {
			if (expiredInstant) {
				expired = true;
			} else {
				expiring = true;
			}
		}

		boolean reached = targetLocation != null && MathUtils.getDistance(location, targetLocation) < accuracy;
		if (reached) {
			if (!reachedTargetLocation) {
				reachedTargetLocation = true;
				onReachedTargetLocation();
			}

			if (expiredWhenReachedTargetLocation) {
				if (expiredInstant) {
					expired = true;
				} else {
					expiring = true;
				}
			}
		}
	}

	public void advanceImpl(float amount) {

	}

	public void onReachedTargetLocation() { // can change expiredWhenReachedTargetLocation here if you want

	}

	private EnumSet<CombatEngineLayers> activeLayers = null;

	public void setActiveLayers(EnumSet<CombatEngineLayers> activeLayers) {
		this.activeLayers = activeLayers;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return activeLayers;
	}

	private float renderRadius = 0f;

	public void setRenderRadius(float renderRadius) {
		this.renderRadius = renderRadius;
	}

	@Override
	public float getRenderRadius() {
		return renderRadius;
	}

	@Override
	public void render(CombatEngineLayers layer, ViewportAPI viewport) {}

	@Override
	public Vector2f getLocation() {
		return location;
	}

	@Override
	public Vector2f getVelocity() {
		return velocity;
	}

	@Override
	public float getFacing() {
		return facing;
	}

	@Override
	public void setFacing(float facing) {
		this.facing = facing;
	}

	public CombatEntityAPI getRenderCenter() {
		return renderCenter;
	}

	public float getAngleVel() {
		return angleVel;
	}

	public void setAngleVel(float angleVel) {
		this.angleVel = angleVel;
	}

	public float getUid() {
		return uid;
	}

	public SpriteAPI getSpriteTexture() {
		return spriteTexture;
	}

	public void setSpriteTexture(SpriteAPI spriteTexture) {
		this.spriteTexture = spriteTexture;
	}

	public Vector2f getTargetLocation() {
		return targetLocation;
	}

	public float getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
	}

	public float getElapsed() {
		return elapsed;
	}

	public void setElapsed(float elapsed) {
		this.elapsed = elapsed;
	}

	public float getTimeLimit() {
		return timeLimit;
	}

	public void setTimeLimit(float timeLimit) {
		this.timeLimit = timeLimit;
	}

	public float getOpacity() {
		return opacity;
	}

	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}

	public float getIn() {
		return in;
	}

	public void setIn(float in) {
		this.in = in;
	}

	public float getFull() {
		return full;
	}

	public void setFull(float full) {
		this.full = full;
	}

	public float getOut() {
		return out;
	}

	public void setOut(float out) {
		this.out = out;
	}

	public float getStartSize() {
		return startSize;
	}

	public void setStartSize(float startSize) {
		this.startSize = startSize;
	}

	public float getEndSize() {
		return endSize;
	}

	public void setEndSize(float endSize) {
		this.endSize = endSize;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	@Override
	public void init(CombatEntityAPI entity) {}

	@Override
	public void cleanup() {}

	@Override
	public boolean isExpired() {
		return expired;
	}

	public boolean isExpiring() {
		return expiring;
	}
}
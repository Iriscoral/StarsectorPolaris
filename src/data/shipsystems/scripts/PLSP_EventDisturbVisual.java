package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class PLSP_EventDisturbVisual extends BaseCombatLayeredRenderingPlugin {

	private static final Vector2f ZERO = new Vector2f();
	private static final float MAX_ACTIVE_TIME = 1f;
	private static final float MAX_TIME = 10f;

	private final ShipAPI anchor;
	private final Map<DamagingProjectileAPI, VisualData> targetData;
	private final float initRange;
	private float extraRange;
	private float activeTimeLeft;
	private float liveTimeLeft;

	private final int segments = 40;
	private final SpriteAPI bgSprite;

	private boolean valid = true;
	private boolean active = false;
	private boolean fading = false;
	private float lifeLevel = 0f;

	public PLSP_EventDisturbVisual(ShipAPI anchor, float extraRange) {
		this.anchor = anchor;
		this.anchor.setCustomData(PLSP_EventDisturbStats.DATA_KEY, true);

		this.targetData = new HashMap<>();
		this.initRange = extraRange;
		this.extraRange = extraRange;
		this.activeTimeLeft = MAX_ACTIVE_TIME;
		this.liveTimeLeft = MAX_TIME;

		this.bgSprite = Global.getSettings().getSprite("misc", "PLSP_denseFog");
		this.bgSprite.setAdditiveBlend();
		this.bgSprite.setColor(PLSP_ColorData.LIGHT_YELLOW);
		this.bgSprite.setAlphaMult(0.1f);

		float size = (anchor.getCollisionRadius() + extraRange) * 2f;
		this.bgSprite.setSize(size, size);
	}

	@Override
	public float getRenderRadius() {
		return PLSP_EventDisturbStats.getRange(anchor) + 500f;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER, CombatEngineLayers.UNDER_SHIPS_LAYER);
	}

	@Override
	public void advance(float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused()) {
			return;
		}

		liveTimeLeft -= amount;
		entity.getLocation().set(anchor.getLocation());

		for (VisualData p : targetData.values()) {
			p.textElapsed += amount;
		}

		extraRange = initRange * (activeTimeLeft / MAX_ACTIVE_TIME * 0.25f + 0.75f);
		if (anchor.isAlive() && activeTimeLeft > 0f && liveTimeLeft > 0f) {
			for (DamagingProjectileAPI proj : PLSP_Util.getEnemyProjectilesAndMissilesWithinRange(anchor.getLocation(), anchor.getCollisionRadius() + extraRange, anchor.getOwner())) {
				boolean gM = false;
				if (proj instanceof MissileAPI) {
					MissileAPI m = (MissileAPI)proj;
					gM = m.isGuided() && !m.isFizzling();
				}

				if (PLSP_Util.willProjectileHitShipWithInSec(proj, anchor, 3) || gM) {
					if (!targetData.containsKey(proj)) { // once caught, no spare
						targetData.put(proj, new VisualData(anchor, proj));
						if (!active) {
							active = true;
						}
					}
				}
			}

			for (DamagingProjectileAPI proj : targetData.keySet()) {
				VisualData data = targetData.get(proj);
				if (!engine.isEntityInPlay(proj)) {
					data.advanceEffectLevel(-amount * 5f);
					continue;
				}

				proj.getVelocity().scale(0.95f);

				data.advanceEffectLevel(amount * 3f);
				if (data.readyToClear()) {
					engine.addSmoothParticle(proj.getLocation(), ZERO, 2f + (float)Math.random() * 5f + proj.getCollisionRadius(), 2f, 0.5f, PLSP_ColorData.LIGHT_YELLOW);
					PLSP_Util.addLight(proj.getLocation(), proj.getCollisionRadius() + 15f, 3f, 0.5f, PLSP_ColorData.LIGHT_YELLOW);
					engine.removeEntity(proj);
				}
			}
		} else {
			fading = true;

			for (DamagingProjectileAPI proj : targetData.keySet()) {
				VisualData data = targetData.get(proj);
				data.advanceEffectLevel(-amount * 5f);
			}
		}

		if (active) {
			activeTimeLeft -= amount;
		}

		if (fading) {
			lifeLevel -= amount * 2f;
			lifeLevel = Math.max(lifeLevel, 0f);
		} else {
			lifeLevel += amount;
			lifeLevel = Math.min(lifeLevel, 1f);
		}

		if (fading && lifeLevel <= 0f) {
			targetData.clear();
			valid = false;

			anchor.removeCustomData(PLSP_EventDisturbStats.DATA_KEY);
		}
	}

	@Override
	public void init(CombatEntityAPI entity) {
		super.init(entity);
	}

	@Override
	public boolean isExpired() {
		return !valid;
	}

	@Override
	public void render(CombatEngineLayers layer, ViewportAPI viewport) {

		if (lifeLevel <= 0f) return;
		if (layer == CombatEngineLayers.UNDER_SHIPS_LAYER) {
			float hpLevel = activeTimeLeft / MAX_ACTIVE_TIME * 0.5f + 0.25f;
			if (active) hpLevel += 0.25f;
			bgSprite.setAlphaMult(0.1f * lifeLevel * hpLevel);
			bgSprite.renderAtCenter(anchor.getLocation().x, anchor.getLocation().y);
		}

		for (DamagingProjectileAPI target : targetData.keySet()) {
			VisualData p = targetData.get(target);

			float alphaMult = viewport.getAlphaMult() * p.effectLevel * p.alphaFactor;
			alphaMult *= 0.5f;
			if (lifeLevel < 0.5f) alphaMult *= lifeLevel * 2f;

			if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {
				renderAura(anchor, p.target, segments, p.auraTex, p.textElapsed, alphaMult, lifeLevel);
			}
		}
	}

	public void renderAura(ShipAPI anchor, DamagingProjectileAPI target, int segments, SpriteAPI auraTex, float textElapsed, float alphaMult, float widthMult) {

		float range = MathUtils.getDistance(anchor.getLocation(), target.getLocation());
		float lengthPerSeg = range / segments;

		GL11.glPushMatrix();
		GL11.glTranslatef(anchor.getLocation().getX(), anchor.getLocation().getY(), 0f);
		GL11.glRotatef(VectorUtils.getAngle(anchor.getLocation(), target.getLocation()), 0f, 0f, 1f);

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		auraTex.bindTexture();

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

		float texPerSegment = 0.01f * auraTex.getTextureHeight() * lengthPerSeg; // fixed

		float base = 0f;
		float maxTex = auraTex.getTextureHeight();

		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_POLYGON_SMOOTH);

		GL11.glBegin(GL11.GL_QUAD_STRIP);

		float leftEdgeOfShowingTex = 0f;
		for (int i = 0; i < segments; i++) {

			float outRange = getOutRange(anchor, target, i, segments, widthMult);

			float x = i * lengthPerSeg;
			float yMin = -outRange;
			float yMax = outRange;

			Color effectColor = PLSP_ColorData.SHINY_YELLOW;

			float alphaEffect = alphaMult;
			if (i < 5) alphaEffect *= i / 5f;
			else if (i > segments - 5) alphaEffect *= (segments - i - 1) / 5f;

			GL11.glColor4ub((byte)effectColor.getRed(), (byte)effectColor.getGreen(), (byte)effectColor.getBlue(), (byte)(int)(effectColor.getAlpha() * alphaEffect));
			GL11.glTexCoord2f(leftEdgeOfShowingTex + textElapsed, base);
			GL11.glVertex2f(x, yMin);

			GL11.glColor4ub((byte)effectColor.getRed(), (byte)effectColor.getGreen(), (byte)effectColor.getBlue(), (byte)(int)(effectColor.getAlpha() * alphaEffect));
			GL11.glTexCoord2f(leftEdgeOfShowingTex + textElapsed, maxTex);
			GL11.glVertex2f(x, yMax);

			leftEdgeOfShowingTex += texPerSegment;
		}

		GL11.glEnd();
		GL11.glPopMatrix();
	}

	private static float getOutRange(ShipAPI anchor, CombatEntityAPI target, int i, int segments, float widthMult) {
		float anchorEffect = anchor.getCollisionRadius();
		float targetEffect = target.getCollisionRadius();
		float minRange = 10f * widthMult;

		double anchorRangeFactor = quickPow(0.9d, i);
		double targetRangeFactor = quickPow(0.9d, segments - i) * widthMult;

		double finalRange = anchorEffect * anchorRangeFactor + targetEffect * targetRangeFactor;
		finalRange = Math.max(finalRange, minRange);

		return (float)finalRange;
	}

	private static double quickPow(double under, int upper) {
		double res = 1f;
		double base = under;
		while (upper != 0) {
			if ((upper & 1) == 1) {
				res = res * base;
			}

			base *= base;
			upper >>= 1;
		}
		return res;
	}

	public static class VisualData {

		ShipAPI anchor;
		DamagingProjectileAPI target;

		private final SpriteAPI auraTex;
		private float textElapsed = 0f;

		private float effectLevel = 0f;
		private float alphaFactor;

		public VisualData(ShipAPI anchor, DamagingProjectileAPI target) {
			this.anchor = anchor;
			this.target = target;

			this.auraTex = Global.getSettings().getSprite("misc", "PLSP_eventLine");

			float damage = target.getDamageAmount() * target.getDamageType().getArmorMult() + target.getEmpAmount() * 0.25f;
			this.alphaFactor = Math.min(damage * 0.005f, 1f);
		}

		public void advanceEffectLevel(float amount) {
			effectLevel += amount;
			effectLevel = Math.max(effectLevel, 0f);
			effectLevel = Math.min(effectLevel, 1f);
		}

		public boolean readyToClear() {
			return effectLevel >= 1f;
		}

		public float getEffectLevel() {
			return effectLevel;
		}
	}
}
package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Noise;
import data.scripts.util.PLSP_ColorData;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Arrays;
import java.util.EnumSet;

public class PLSP_DarkCollapseVisual extends BaseCombatLayeredRenderingPlugin {

	public static final Color EXPLOSION_DARK = new Color(15, 0, 20);
	public static final Color EXPLOSION_UNDER = new Color(25, 0, 100, 100);

	public static class DCVParams {
		public float fadeIn = 0.2f;
		public float fadeOut = 0.3f;
		public float hitGlowSizeMult = 0.75f;
		public float thickness = 25f;
		public float noiseMag = 1f;
		public float noisePeriod = 0.1f;
		public boolean withHitGlow = true;
		public Color underGlow = EXPLOSION_UNDER;
		public Color invertForDarkening = null;

		public float radius;
		public Color color;

		public DCVParams(float radius, Color borderColor) {
			this.radius = radius * 0.75f;
			this.color = borderColor;
		}

		public DCVParams(float radius, float thickness, Color color) {
			this.radius = radius * 0.75f;
			this.thickness = thickness;
			this.color = color;
		}
	}

	public static void spawnCrossRift(Vector2f location, DCVParams params) {
		CombatEngineAPI engine = Global.getCombatEngine();

		CombatEntityAPI prev = null;
		for (int i = 0; i < 2; i++) {
			DCVParams p = new DCVParams(params.radius, params.thickness, params.color);
			p.radius *= 0.75f + 0.5f * (float)Math.random();
			p.withHitGlow = prev == null;

			Vector2f loc = new Vector2f(location);
			CombatEntityAPI e = engine.addLayeredRenderingPlugin(new PLSP_DarkCollapseVisual(p));
			e.getLocation().set(loc);

			if (prev != null) {
				float dist = Misc.getDistance(prev.getLocation(), loc);
				Vector2f vel = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(loc, prev.getLocation()));
				vel.scale(dist / (p.fadeIn + p.fadeOut) * 0.7f);
				e.getVelocity().set(vel);
			}

			prev = e;
		}
	}

	private FaderUtil fader;
	private SpriteAPI atmosphereTex;

	private float[] noise;
	private float[] noise1;
	private float[] noise2;

	private final DCVParams p;

	private int segments;
	private float basicAngle;
	private float noiseElapsed = 0f;

	private boolean spawnedHitGlow = false;

	public PLSP_DarkCollapseVisual(DCVParams p) {
		this.p = p;
	}

	@Override
	public float getRenderRadius() {
		return p.radius + 500f;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.of(CombatEngineLayers.ABOVE_PARTICLES_LOWER, CombatEngineLayers.ABOVE_PARTICLES);
	}

	@Override
	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		fader.advance(amount);

		if (p.noiseMag > 0) {
			noiseElapsed += amount;
			if (noiseElapsed > p.noisePeriod) {
				noiseElapsed = 0;
				noise1 = Arrays.copyOf(noise2, noise2.length);
				noise2 = Noise.genNoise(segments, p.noiseMag);
			}
			float f = noiseElapsed / p.noisePeriod;
			for (int i = 0; i < noise.length; i++) {
				float n1 = noise1[i];
				float n2 = noise2[i];
				noise[i] = n1 + (n2 - n1) * f;
			}
		}

		if (!p.withHitGlow) {
			return;
		}

		if (!spawnedHitGlow) {
			float size = Math.min(p.radius * 7f, p.radius + 150f);
			float coreSize = Math.max(size, p.radius * 4f);
			if (coreSize > size) {
				size = coreSize;
			}

			size *= p.hitGlowSizeMult;
			coreSize *= p.hitGlowSizeMult;

			CombatEngineAPI engine = Global.getCombatEngine();
			Vector2f point = entity.getLocation();
			Vector2f vel = entity.getVelocity();
			float dur = fader.getDurationOut();
			engine.addHitParticle(point, vel, size * 3f, 1f, dur, p.color);
			engine.addHitParticle(point, vel, coreSize * 1.5f, 1f, dur, Color.white);

			Color invert = p.color;
			if (p.invertForDarkening != null) {
				invert = p.invertForDarkening;
			}
			Color c = new Color(255 - invert.getRed(), 255 - invert.getGreen(), 255 - invert.getBlue(), 127);
			c = Misc.interpolateColor(c, Color.white, 0.4f);
			float durMult = 1f;
			for (int i = 0; i < 7; i++) {
				dur = 4f + 4f * (float) Math.random();
				dur *= durMult;
				dur *= 0.5f;

				float nSize = size * 1f;
				Vector2f pt = Misc.getPointAtRadius(point, nSize * 0.5f);
				Vector2f v = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
				v.scale(nSize + nSize * (float) Math.random() * 0.5f);
				v.scale(0.15f);
				Vector2f.add(vel, v, v);

				v = new Vector2f(entity.getVelocity());
				engine.addNegativeNebulaParticle(pt, v, nSize * 1f, 2f, p.fadeIn / dur, 0f, dur, c);
			}

			dur = p.fadeIn + p.fadeOut + 2f;
			dur *= durMult;
			float rampUp = (p.fadeIn + p.fadeOut) / dur;
			rampUp = 0f;

			c = p.underGlow;
			for (int i = 0; i < 15; i++) {
				Vector2f loc = new Vector2f(point);
				loc = Misc.getPointWithinRadius(loc, size * 1f);
				float s = size * 3f * (0.25f + (float) Math.random() * 0.25f);
				engine.addNebulaParticle(loc, entity.getVelocity(), s, 1.5f, rampUp, 0f, dur, c);
			}
			spawnedHitGlow = true;
		}
	}

	@Override
	public void init(CombatEntityAPI entity) {
		super.init(entity);

		fader = new FaderUtil(0f, p.fadeIn, p.fadeOut);
		fader.setBounceDown(true);
		fader.fadeIn();

		atmosphereTex = Global.getSettings().getSprite("combat", "corona_hard");
		basicAngle = (float)Math.toRadians(360f * (float)Math.random());

		float perSegment = 2f;
		segments = (int) ((p.radius * 2f * 3.14f) / perSegment);
		if (segments < 8) {
			segments = 8;
		}

		noise1 = Noise.genNoise(segments, p.noiseMag);
		noise2 = Noise.genNoise(segments, p.noiseMag);
		noise = Arrays.copyOf(noise1, noise1.length);
	}

	@Override
	public boolean isExpired() {
		return fader.isFadedOut();
	}

	@Override
	public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		float x = entity.getLocation().x;
		float y = entity.getLocation().y;

		float effectLevel = fader.getBrightness();
		if (fader.isFadingOut()) {
			effectLevel = 2f - fader.getBrightness();
		}

		float alphaMult = viewport.getAlphaMult();
		if (effectLevel < 0.5f) {
			alphaMult *= effectLevel * 2f;
		} else if (effectLevel > 1.5f) {
			alphaMult *= (2f - effectLevel) * 2f;
		}

		float radius = p.radius;
		if (fader.isFadingOut()) {
			radius *= 0.75f - effectLevel * 0.25f;
		}

		int currentSegments = segments;
		if (fader.isFadingOut()) {
			currentSegments = (int)(segments / effectLevel);
		}

		if (layer == CombatEngineLayers.ABOVE_PARTICLES_LOWER) {
			renderAtmosphere(x, y, radius, p.thickness, alphaMult, effectLevel, currentSegments, atmosphereTex, noise, p.color, true);
			renderAtmosphere(x, y, radius - 2f, p.thickness, alphaMult, effectLevel, currentSegments, atmosphereTex, noise, p.color, true);
		} else if (layer == CombatEngineLayers.ABOVE_PARTICLES) {
			float circleAlpha = 1f;
			if (alphaMult < 0.5f) {
				circleAlpha = alphaMult * 2f;
			}

			float tCircleBorder = 1f;
			Color effectColor = Misc.interpolateColor(EXPLOSION_DARK, PLSP_ColorData.PHASE_GLOW, effectLevel * effectLevel * 0.25f);

			renderCircle(x, y, radius, circleAlpha, effectLevel, currentSegments, noise, effectColor);
			renderAtmosphere(x, y, radius, tCircleBorder, circleAlpha, effectLevel, currentSegments, atmosphereTex, noise, effectColor, false);
		}
	}

	private void renderCircle(float x, float y, float radius, float alphaMult, float effectLevel, int segments, float[] noise, Color color) {
		if (fader.isFadingIn()) {
			alphaMult = 1f;
		}

		float startRad = (float) Math.toRadians(0);
		float endRad = (float) Math.toRadians(360);
		float spanRad = Misc.normalizeAngle(endRad - startRad);
		float anglePerSegment = spanRad / segments;

		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 0);
		GL11.glRotatef(0, 0, 0, 1);
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) ((float) color.getAlpha() * alphaMult));

		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glVertex2f(0, 0);
		for (float i = 0; i < segments + 1; i++) {
			boolean last = i == segments;
			if (last) i = 0;

			float theta = anglePerSegment * i;
			float cos = (float) FastTrig.cos(theta);
			float sin = (float) FastTrig.sin(theta);

			float m1 = 0.75f * getAngleBonusMult(basicAngle + theta, effectLevel) + 0.65f * noise[(int) i];
			if (p.noiseMag <= 0) {
				m1 = 1f;
			}

			float x1 = cos * radius * m1;
			float y1 = sin * radius * m1;

			GL11.glVertex2f(x1, y1);
			if (last) break;
		}

		GL11.glEnd();
		GL11.glPopMatrix();

	}

	private void renderAtmosphere(float x, float y, float radius, float thickness, float alphaMult, float effectLevel, int segments, SpriteAPI tex, float[] noise, Color color, boolean additive) {

		float startRad = (float) Math.toRadians(0);
		float endRad = (float) Math.toRadians(360);
		float spanRad = Misc.normalizeAngle(endRad - startRad);
		float anglePerSegment = spanRad / segments;

		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 0);
		GL11.glRotatef(0, 0, 0, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);

		tex.bindTexture();

		GL11.glEnable(GL11.GL_BLEND);
		if (additive) {
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		} else {
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}

		GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) ((float) color.getAlpha() * alphaMult));
		float incr = 1f / segments;
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		for (int i = 0; i < segments + 1; i++) {
			boolean last = i == segments;
			if (last) i = 0;

			float theta = anglePerSegment * i;
			float cos = (float) FastTrig.cos(theta);
			float sin = (float) FastTrig.sin(theta);

			float m1 = 0.75f * getAngleBonusMult(basicAngle + theta, effectLevel) + 0.65f * noise[i];
			float m2 = m1;
			if (p.noiseMag <= 0) {
				m1 = 1f;
				m2 = 1f;
			}

			float x1 = cos * radius * m1;
			float y1 = sin * radius * m1;
			float x2 = cos * (radius + thickness * m2);
			float y2 = sin * (radius + thickness * m2);

			GL11.glTexCoord2f(0.5f, 0.05f);
			GL11.glVertex2f(x1, y1);

			GL11.glTexCoord2f(0.5f, 0.95f);
			GL11.glVertex2f(x2, y2);
			if (last) break;
		}

		GL11.glEnd();
		GL11.glPopMatrix();
	}

	// effectLevel 0 - Star like
	// effectLevel 2 - Circle, all at 1
	private float getAngleBonusMult(float theta, float effectLevel) {
		if (effectLevel < 0.1f) effectLevel = 0.1f;

		double pi2 = Math.PI * 0.5d;
		double thetaD = (theta % pi2 + pi2) % pi2;

		double cos = Math.max(FastTrig.cos(thetaD), 0d);
		double sin = Math.max(FastTrig.sin(thetaD), 0d);

		double power = effectLevel;
		double cosP = Math.pow(cos, power);
		double sinP = Math.pow(sin, power);

		double upper = 2d / effectLevel;
		double result = upper / Math.pow(cosP + sinP, 1d / power);

		return (float)result;
	}
}
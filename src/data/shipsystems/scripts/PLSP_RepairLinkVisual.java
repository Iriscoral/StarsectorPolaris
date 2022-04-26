package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

public class PLSP_RepairLinkVisual extends BaseCombatLayeredRenderingPlugin {

	private final ShipAPI anchor;
	private final Map<ShipAPI, float[]> wings = new HashMap<>();
	private final List<ShipAPI> toRemove = new ArrayList<>();

	private SpriteAPI lineTex;

	private WeaponSlotAPI leftSource;
	private WeaponSlotAPI rightSource;
	private boolean valid = true;
	private float effectLevel = 1f;

	public PLSP_RepairLinkVisual(ShipAPI anchor) {
		this.anchor = anchor;
	}

	@Override
	public float getRenderRadius() {
		return 1000000f;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.of(CombatEngineLayers.CONTRAILS_LAYER);
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	@Override
	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		if (!valid) {
			effectLevel -= amount * 3f;
			if (effectLevel <= 0f) {
				effectLevel = 0f;
				return;
			}
		}

		entity.getLocation().set(anchor.getLocation());

		for (ShipAPI wing : PLSP_Util.getFighters(anchor)){
			if (!wings.containsKey(wing)) wings.put(wing, new float[]{0f, (float)(Math.random() * 100f)});
		}

		for (ShipAPI wing : wings.keySet()) {
			float[] wingLevel = wings.get(wing);
			if (wing.isAlive()) {
				wingLevel[0] += amount * 2f;
				wingLevel[0] = Math.min(wingLevel[0], 1f);

				float distant = MathUtils.getDistance(anchor, wing);
				wingLevel[1] -= amount * 4f * (1f + (float)Math.sqrt(distant) * 0.01f);
			} else {
				wingLevel[0] -= amount * 3f;
				wingLevel[0] = Math.max(wingLevel[0], 0f);
				if (wingLevel[0] == 0f) {
					toRemove.add(wing);
				}
			}

			wings.put(wing, wingLevel);
		}

		for (ShipAPI remove : toRemove) {
			wings.remove(remove);
		}
		toRemove.clear();
	}

	@Override
	public void init(CombatEntityAPI entity) {
		super.init(entity);
		advance(0f);

		lineTex = Global.getSettings().getSprite("misc", "PLSP_linkBand");

		for (WeaponSlotAPI slot : anchor.getHullSpec().getAllWeaponSlotsCopy()) {
			if (!slot.isSystemSlot()) continue;

			if (slot.getId().contentEquals("LEFT_SOURCE")) {
				leftSource = slot;
			} else if (slot.getId().contentEquals("RIGHT_SOURCE")) {
				rightSource = slot;
			}
		}
	}

	@Override
	public boolean isExpired() {
		return !valid && effectLevel <= 0f;
	}

	@Override
	public void render(CombatEngineLayers layer, ViewportAPI viewport) {

		Vector2f leftPoint = leftSource == null ? anchor.getLocation() : leftSource.computePosition(anchor);
		Vector2f rightPoint = rightSource == null ? anchor.getLocation() : rightSource.computePosition(anchor);

		float alphaMult = viewport.getAlphaMult() * effectLevel;
		if (layer == CombatEngineLayers.CONTRAILS_LAYER) {
			for (ShipAPI wing : wings.keySet()) {
				Vector2f sub = Vector2f.sub(wing.getLocation(), anchor.getLocation(), null);
				float left = anchor.getFacing() + 90f;
				float angle = Math.abs(MathUtils.getShortestRotation(left, VectorUtils.getAngle(anchor.getLocation(), wing.getLocation())));
				float shift = (float)(sub.length() * FastTrig.cos(Math.toRadians(angle))); // positive means left, abs(200) means mid

				float offset = Math.max(400f, Math.abs(shift));
				Vector2f leftMidPoint = MathUtils.getPoint(anchor.getLocation(), offset, left);
				Vector2f leftFarPoint = MathUtils.getPoint(anchor.getLocation(), offset + 30f, left);
				Vector2f rightMidPoint = MathUtils.getPoint(anchor.getLocation(), offset, left + 180f);
				Vector2f rightFarPoint = MathUtils.getPoint(anchor.getLocation(), offset + 30f, left + 180f);

				float[] wingLevel = wings.get(wing);
				float leftAlpha = 0f, rightAlpha = 0f;
				if (shift > 90f) {
					leftAlpha = 1f;
				} else if (shift > -10f) {
					leftAlpha = Math.max((shift + 10f) * 0.01f, 0f);
				}
				if (shift < -90f) {
					rightAlpha = 1f;
				} else if (shift < 10f) {
					rightAlpha = Math.max((-shift + 10f) * 0.01f, 0f);
				}

				double accuracy = 0.01d;
				if (leftAlpha > 0f) renderLine(leftPoint, leftMidPoint, leftFarPoint, wing.getLocation(), accuracy, alphaMult * leftAlpha * wingLevel[0], wingLevel[1]);
				if (rightAlpha > 0f) renderLine(rightPoint, rightMidPoint, rightFarPoint, wing.getLocation(), accuracy, alphaMult * rightAlpha * wingLevel[0], wingLevel[1]);
			}
		}
	}

	public void renderLine(Vector2f anchor, Vector2f mid, Vector2f far, Vector2f target, double t, float alphaMult, float textElapsed) {

		GL11.glPushMatrix();
		GL11.glTranslatef(0f, 0f, 0f);
		GL11.glRotatef(0f, 0f, 0f, 1f);

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		lineTex.bindTexture();

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

		Color paramColor = lineTex.getColor();
		GL11.glColor4ub((byte)paramColor.getRed(), (byte)paramColor.getGreen(), (byte)paramColor.getBlue(), (byte)(paramColor.getAlpha() * alphaMult));

		float base = 0f;
		float maxTex = lineTex.getTextureHeight();
		GL11.glBegin(GL11.GL_QUAD_STRIP);

		double x = anchor.getX();
		double y = anchor.getY();
		double leftEdgeOfShowingTex = 0d;
		for (double k = 0; k <= 1d; k += t) {
			double nextK = k + t;

			double r = 1d - k;
			double nextR = 1d - nextK;

			double nextX = Math.pow(nextR, 2) * anchor.getX() + 2d * nextK * nextR * mid.getX() + Math.pow(nextK, 2) * target.getX();
			double nextY = Math.pow(nextR, 2) * anchor.getY() + 2d * nextK * nextR * mid.getY() + Math.pow(nextK, 2) * target.getY();
			double distance = Math.hypot(x - nextX, y - nextY);

			GL11.glTexCoord2f((float)(leftEdgeOfShowingTex + textElapsed), base);
			GL11.glVertex2f((float)x, (float)y);

			x = Math.pow(r, 2) * anchor.getX() + 2d * k * r * far.getX() + Math.pow(k, 2) * target.getX();
			y = Math.pow(r, 2) * anchor.getY() + 2d * k * r * far.getY() + Math.pow(k, 2) * target.getY();
			GL11.glTexCoord2f((float)(leftEdgeOfShowingTex + textElapsed), maxTex);
			GL11.glVertex2f((float)x, (float)y);

			x = nextX;
			y = nextY;
			leftEdgeOfShowingTex += distance * 0.01d;
		}

		GL11.glEnd();
		GL11.glPopMatrix();
	}
}
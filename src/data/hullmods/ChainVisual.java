package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.*;

public class ChainVisual extends BaseCombatLayeredRenderingPlugin {

	private static final Color LINECOLOR = new Color(80, 156, 255);

	private final ShipAPI anchor;
	private final Map<ShipAPI, Float> wings = new HashMap<>();
	private final List<ShipAPI> toRemove = new ArrayList<>();

	private boolean valid = true;
	private float effectLevel = 1f;

	public ChainVisual(ShipAPI anchor) {
		this.anchor = anchor;
	}

	@Override
	public float getRenderRadius() {
		return 1000000f;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.of(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
	}

	@Override
	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		if (!anchor.isAlive()) {
			effectLevel -= amount * 3f;
			if (effectLevel <= 0f) {
				effectLevel = 0f;
				valid = false;
			}

			return;
		}

		Vector2f location = anchor.getLocation();
		for (WeaponSlotAPI slot : anchor.getHullSpec().getAllWeaponSlotsCopy()) {
			if (!slot.isSystemSlot()) continue;

			location = slot.computePosition(anchor);
			break;
		}
		entity.getLocation().set(location);

		for (ShipAPI wing : PLSP_Util.getFighters(anchor)){
			if (!wings.containsKey(wing)) wings.put(wing, 0f);
		}

		for (ShipAPI wing : wings.keySet()) {
			float wingLevel = wings.get(wing);
			if (wing.isAlive()) {
				wingLevel += amount * 2f;
				wingLevel = Math.min(wingLevel, 1f);
			} else {
				wingLevel -= amount * 3f;
				wingLevel = Math.max(wingLevel, 0f);
				if (wingLevel == 0f) {
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
	}

	@Override
	public boolean isExpired() {
		return !valid;
	}

	@Override
	public void render(CombatEngineLayers layer, ViewportAPI viewport) {

		float alphaMult = viewport.getAlphaMult() * effectLevel;
		if (layer == CombatEngineLayers.ABOVE_PARTICLES_LOWER) {
			for (ShipAPI wing : wings.keySet()) {
				Vector2f midPoint = MathUtils.getMidpoint(entity.getLocation(), wing.getLocation());
				midPoint.setX(midPoint.getX() + 15f);
				midPoint.setY(midPoint.getY() + 10f); // for test

				float wingLevel = wings.get(wing);
				renderLine(entity.getLocation(), midPoint, wing.getLocation(), 0.1d, alphaMult * wingLevel);
			}
		}
	}

	public void renderLine(Vector2f anchor, Vector2f mid, Vector2f target, double t, float alphaMult) {

		GL11.glPushMatrix();
		GL11.glTranslatef(0f, 0f, 0f);
		GL11.glRotatef(0f, 0f, 0f, 1f);

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GL11.glColor4ub((byte)LINECOLOR.getRed(), (byte)LINECOLOR.getGreen(), (byte)LINECOLOR.getBlue(), (byte)(int)(LINECOLOR.getAlpha() * alphaMult));

		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glBegin(GL11.GL_LINE_STRIP);
		for (double k = 0; k <= 1d; k += t) {
			double r = 1d - k;
			double x = Math.pow(r, 2) * anchor.getX() + 2d * k * r * mid.getX() + Math.pow(k, 2) * target.getX();
			double y = Math.pow(r, 2) * anchor.getY() + 2d * k * r * mid.getY() + Math.pow(k, 2) * target.getY();

			GL11.glVertex2f((float)x, (float)y);
			Global.getLogger(this.getClass()).info("drawing " + k);
		}

		GL11.glEnd();
		GL11.glPopMatrix();
	}
}
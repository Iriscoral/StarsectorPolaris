package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugin.PLSP_TrailLine;
import data.scripts.util.MagicLensFlare;
import data.scripts.util.PLSP_ColorData;
import data.scripts.util.PLSP_Util;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class PLSP_DisplacerLine extends PLSP_TrailLine {

	private final ShipAPI anchor;

	public PLSP_DisplacerLine(ShipAPI anchor, Vector2f targetLocation, Vector2f spawnLocation, float facing, float speed, float angleVel, SpriteAPI spriteTexture, float startSize, float endSize, Color color, float opacity) {
		super(targetLocation, 50f, 3f, spawnLocation, facing, 0.2f, 1f, 0.3f, startSize, endSize, speed, angleVel, spriteTexture, color, opacity);

		this.anchor = anchor;
	}

	public void updateDestination(Vector2f targetLocation) {
		if (updateTargetLocation(targetLocation, false)) {
			velocityScale(2f, true);
		}
	}

	@Override
	public void advanceImpl(float amount) {

		if (PLSP_Util.timesPerSec(1f, amount)) {
			float angle = (float)Math.random() * 360f;
			float length = anchor.getCollisionRadius() * ((float)Math.random() * 2f + 1f);
			MagicLensFlare.createSharpFlare(Global.getCombatEngine(), anchor, getLocation(), 4f, length, angle, getColor(), PLSP_ColorData.BRIGHT_EMP_ARC_CORE);
		}
	}
}
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class Chain extends BaseHullMod {

	public static final String KEY = "YOUR_KEY";

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null || engine.isPaused()) return;
		if (!ship.isAlive()) return;

		if (ship.getCustomData().get(KEY) == null) {
			ChainVisual visual = new ChainVisual(ship);
			engine.addLayeredRenderingPlugin(visual);

			ship.setCustomData(KEY, visual);
		}
	}
}
package data.scripts.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class PLSP_LockerPlugin extends BaseEveryFrameCombatPlugin {
	private static final String DATA_KEY = "PLSP_LockerPlugin";
	private final List<ShipAPI> lockers = new ArrayList<>();
	private CombatEngineAPI engine;

	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (engine == null || engine.isPaused()) {
			return;
		}

		for (ShipAPI ship : engine.getShips()) {
			if (!ship.isAlive()) continue;

			if (ship.getVariant().getHullMods().contains("PLSP_nodelock")) {
				lockers.add(ship);
			}
		}

		for (BattleObjectiveAPI object : Global.getCombatEngine().getObjectives()) {
			if (!object.getCustomData().containsKey(DATA_KEY)) {
				object.setCustomData(DATA_KEY, new ArrayList<>(2));
			}

			ArrayList<ShipAPI> functionalLockers = (ArrayList)object.getCustomData().get(DATA_KEY);
			if (functionalLockers == null) continue;

			// i don't know why i wrote these
			if (!functionalLockers.isEmpty() && !functionalLockers.get(functionalLockers.size() - 1).isAlive()) functionalLockers.remove(functionalLockers.size() - 1);
			if (!functionalLockers.isEmpty() && !functionalLockers.get(0).isAlive()) functionalLockers.remove(0);

			for (ShipAPI locker : lockers) {
				if (MathUtils.isWithinRange(locker.getLocation(), object.getLocation(), 700f)) {
					if (functionalLockers.isEmpty()) {
						functionalLockers.add(locker); // empty
					} else if (functionalLockers.size() == 1 && functionalLockers.get(0).getOwner() != locker.getOwner()) {
						functionalLockers.add(locker); // double lock
					}
				} else {
					functionalLockers.remove(locker);
				}
			}

			if (!functionalLockers.isEmpty()) { // one side or double lock
				object.setOwner(functionalLockers.get(0).getOwner());
			}
		}

		lockers.clear();
	}

	@Override
	public void init(CombatEngineAPI engine) {
		this.engine = engine;
	}
}
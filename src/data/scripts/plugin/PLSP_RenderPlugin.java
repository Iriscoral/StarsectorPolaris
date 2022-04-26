package data.scripts.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.hullmods.PLSP_ClusterAddition.ModulatorAdditionState;
import data.hullmods.PLSP_ClusterModulator.ModulatorState;
import data.scripts.weapons.PLSP_StreamVisualEffect.StreamState;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class PLSP_RenderPlugin extends BaseCombatLayeredRenderingPlugin {
	private static final String DATA_KEY = "PLSP_RenderPlugin";
	private static final Vector2f ZERO = new Vector2f();
	private CombatEngineAPI engine;

	@Override
	public void init(CombatEntityAPI entity) {
		super.init(entity);
		engine = Global.getCombatEngine();
	}

	@Override
	public void render(CombatEngineLayers layer, ViewportAPI view) {
		if (engine == null){
			return;
		}

		String idC = "PLSP_ClusterModulator";
		if (!engine.getCustomData().containsKey(idC)) {
			engine.getCustomData().put(idC, new HashMap<>());
		}

		Map<ShipAPI, ModulatorState> modulatorStateMap = (Map)engine.getCustomData().get(idC);
		if (!modulatorStateMap.isEmpty()) {
			for (Map.Entry<ShipAPI, ModulatorState> entry : modulatorStateMap.entrySet()) {
				ShipAPI anchor = entry.getKey();
				ModulatorState data = entry.getValue();
				if (anchor == null || data == null) {
					continue;
				}

				if (layer == CombatEngineLayers.UNDER_SHIPS_LAYER) {
					data.spriteBand.renderRegionAtCenter(data.fixedPointToRenderBand.getX(), data.fixedPointToRenderBand.getY(), data.bandSeed, 0f, data.spriteBand.getTextureWidth() * data.frac, data.spriteBand.getTextureHeight());
					Vector2f loc = anchor.getLocation();
					Vector2f locS = anchor.getShieldCenterEvenIfNoShield();
					data.spriteRing.renderAtCenter(locS.getX(), locS.getY());
					data.spriteMistM.renderAtCenter(locS.getX(), locS.getY());
					data.spriteMistI.renderAtCenter(locS.getX(), locS.getY());
					data.spriteMistO.renderAtCenter(locS.getX(), locS.getY());
					data.spriteCore.renderAtCenter(loc.getX(), loc.getY());
				}

				if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {
					data.spriteCore.renderAtCenter(anchor.getLocation().getX(), anchor.getLocation().getY());
				}
			}
		}

		String idA = "PLSP_ClusterAddition";
		if (!engine.getCustomData().containsKey(idA)) {
			engine.getCustomData().put(idA, new HashMap<>());
		}

		Map<ShipAPI, ModulatorAdditionState> modulatorAdditionStateMap = (Map)engine.getCustomData().get(idA);
		if (!modulatorAdditionStateMap.isEmpty()) {
			for (Map.Entry<ShipAPI, ModulatorAdditionState> entry : modulatorAdditionStateMap.entrySet()) {
				ShipAPI anchor = entry.getKey();
				ModulatorAdditionState data = entry.getValue();
				if (anchor == null || data == null) {
					continue;
				}

				if (layer == CombatEngineLayers.UNDER_SHIPS_LAYER) {
					data.spriteBand.renderRegionAtCenter(data.fixedPointToRenderBand.getX(), data.fixedPointToRenderBand.getY(), data.bandSeed, 0f, data.spriteBand.getTextureWidth() * data.frac, data.spriteBand.getTextureHeight());
				}
			}
		}

		String idS = "PLSP_StreamVisual";
		if (!engine.getCustomData().containsKey(idS)) {
			engine.getCustomData().put(idS, new HashMap<>());
		}

		Map<WeaponAPI, StreamState> streamStateMap = (Map)engine.getCustomData().get(idS);
		if (!streamStateMap.isEmpty()) {
			for (Map.Entry<WeaponAPI, StreamState> entry : streamStateMap.entrySet()) {
				WeaponAPI anchor = entry.getKey();
				StreamState data = entry.getValue();
				if (anchor == null || data == null) {
					continue;
				}

				if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
					data.sprite.renderRegionAtCenter(data.fixedPoint.getX(), data.fixedPoint.getY(), data.seed, 0f, data.sprite.getTextureWidth() * data.frac, data.sprite.getTextureHeight());
				}
			}
		}
	}

	@Override
	public float getRenderRadius() {
		return Float.MAX_VALUE;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.allOf(CombatEngineLayers.class);
	}
}
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.PLSP_ClusterModulator.ModulatorState;
import data.scripts.util.PLSP_DataBase;
import data.scripts.util.PLSP_Util;
import data.scripts.util.PLSP_Util.I18nSection;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class PLSP_ClusterAddition extends BaseHullMod {

	private static final String idFromBase = "PLSP_ClusterModulator";
	private static final String id = "PLSP_ClusterAddition";
	private static final Vector2f ZERO = new Vector2f();

	public static final I18nSection strings = I18nSection.getInstance("HullMod", "PLSP_");

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return !isForModSpec;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 10f;
		float padS = 2f;

		tooltip.addPara("%s " + strings.get("clusteradditionTEXT1"), pad, Misc.getHighlightColor(), "#", Global.getSettings().getHullModSpec("PLSP_clustermodulator").getDisplayName());
		tooltip.addPara("    %s " + strings.get("clusteradditionTEXT2"), padS, Misc.getHighlightColor(), "#", "600");
		tooltip.addPara("    %s " + strings.get("clusteradditionTEXT3"), padS, Misc.getHighlightColor(), "#");
		tooltip.addPara("%s " + strings.get("clusteradditionDO"), pad, Misc.getNegativeHighlightColor(), "#", Global.getSettings().getHullModSpec("PLSP_clustermodulator").getDisplayName());
		tooltip.addPara("%s " + strings.get("HAO"), padS, Misc.getNegativeHighlightColor(), "#");
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.getCustomData().containsKey(idFromBase)) {
			engine.getCustomData().put(idFromBase, new HashMap<>());
		}

		Map<ShipAPI, ModulatorState> shipsMapBase = (Map)engine.getCustomData().get(idFromBase);
		if (!engine.getCustomData().containsKey(id)) {
			engine.getCustomData().put(id, new HashMap<>());
		}

		Map<ShipAPI, ModulatorAdditionState> shipsMap = (Map)engine.getCustomData().get(id);
		if (!engine.isEntityInPlay(ship) || !ship.isAlive()) {
			if (!ship.isAlive() && shipsMap.containsKey(ship)) {
				ModulatorAdditionState data = shipsMap.get(ship);
				data.resetAlpha();
				shipsMap.remove(ship);
			}
			return;
		}

		if (!ship.getVariant().hasHullMod("PLSP_clustermodulator")) return;

		if (!shipsMap.containsKey(ship) && shipsMapBase.containsKey(ship)) {
			shipsMap.put(ship, new ModulatorAdditionState(ship));
		} else {
			ModulatorAdditionState data = shipsMap.get(ship);

			data.advanceVisual(amount);
			if (engine.isPaused()) return;

			if (data.sendingInSec > 0f) { // recv was registered in main hullmod, so here only concern send
				float damageP = 0.05f * data.sendingInSec;
				ship.getMutableStats().getHullDamageTakenMult().modifyPercent(id, damageP * 0.5f);
				ship.getMutableStats().getShieldDamageTakenMult().modifyPercent(id, damageP);
				ship.getMutableStats().getArmorDamageTakenMult().modifyPercent(id, damageP * 0.5f);
				ship.getMutableStats().getEmpDamageTakenMult().modifyPercent(id, damageP * 0.5f);
			} else {
				ship.getMutableStats().getHullDamageTakenMult().unmodifyPercent(id);
				ship.getMutableStats().getShieldDamageTakenMult().unmodifyPercent(id);
				ship.getMutableStats().getArmorDamageTakenMult().unmodifyPercent(id);
				ship.getMutableStats().getEmpDamageTakenMult().unmodifyPercent(id);
			}

			boolean availableToIncreaseEffectLevel = data.linkingTarget != null && data.linkingTarget.isAlive();

			FluxTrackerAPI flux = ship.getFluxTracker();
			if (flux.isOverloadedOrVenting()) {
				if (availableToIncreaseEffectLevel) {
					data.effectLevel -= amount * 2f;
					data.effectLevel = Math.max(data.effectLevel, 0f);
					availableToIncreaseEffectLevel = false;
				}
			}

			float maxRange = 600f;
			ShipAPI mostEffectiveTarget = findEffectiveTarget(shipsMapBase, ship, maxRange); // so here pick target to send flux
			if (mostEffectiveTarget != data.linkingTarget) {
				if (mostEffectiveTarget == null && availableToIncreaseEffectLevel) {
					data.effectLevel -= amount * 2f;
					data.effectLevel = Math.max(data.effectLevel, 0f);
					availableToIncreaseEffectLevel = false;
				}
				if (mostEffectiveTarget != null && data.effectLevel <= 0f) {
					data.unapplyLinkingTarget(shipsMapBase);
					data.linkingTarget = mostEffectiveTarget;
					availableToIncreaseEffectLevel = true;
				}
			}

			if (availableToIncreaseEffectLevel && MathUtils.getDistance(ship, data.linkingTarget) > maxRange + 20f) {
				data.effectLevel -= amount * 2f;
				data.effectLevel = Math.max(data.effectLevel, 0f);
				availableToIncreaseEffectLevel = false;
			}

			if (data.linkingTarget != null) {
				FluxTrackerAPI fluxT = data.linkingTarget.getFluxTracker();
				data.sendingInSec = getEffect(ship, data.linkingTarget) * data.effectLevel;
				float sendingInAmount = Math.min(data.sendingInSec * amount, flux.getCurrFlux() - flux.getHardFlux());
				if (sendingInAmount > 0f) {
					data.applyLinkingTarget(shipsMapBase);
					flux.decreaseFlux(sendingInAmount);
					fluxT.increaseFlux(sendingInAmount, false);
				}
			}

			if (availableToIncreaseEffectLevel) {
				data.effectLevel += amount * 2f;
				data.effectLevel = Math.min(data.effectLevel, 1f);
			}

			if (data.linkingTarget != null && data.effectLevel <= 0f) {
				data.unapplyLinkingTarget(shipsMapBase);
			}
		}
	}

	private static ShipAPI findEffectiveTarget(Map<ShipAPI, ModulatorState> mainMap, ShipAPI ship, float maxRange) {
		if (PLSP_Util.isLosingCR(ship)) return null;

		FluxTrackerAPI flux = ship.getFluxTracker();
		if (flux.getHardFlux() == flux.getCurrFlux()) return null;
		if (flux.getFluxLevel() < 0.03f) return null;
		if (flux.isOverloadedOrVenting()) return null;

		if (!ship.isPullBackFighters() && flux.getCurrFlux() < flux.getMaxFlux() * 0.01f + ship.getMutableStats().getFluxDissipation().getModifiedValue() * 3f + 10f) {
			return null;
		}

		float range = maxRange;
		float effect = 0f;
		ShipAPI target = null;
		ShipAPI invalidTarget = mainMap.get(ship).linkingTarget;
		for (ShipAPI ally : mainMap.keySet()) {
			if (ally == ship) continue;
			if (ally.getOwner() != ship.getOwner()) continue;
			if (ally == invalidTarget) continue;
			if (MathUtils.getDistance(ally, ship) > maxRange) continue;
			if (PLSP_Util.isLosingCR(ally)) continue;

			FluxTrackerAPI fluxN = ally.getFluxTracker();
			if (fluxN.isOverloadedOrVenting()) continue;
			if (fluxN.getFluxLevel() > flux.getFluxLevel()) continue;
			if (fluxN.getFluxLevel() > 0.95f) continue;

			float tempEffect = getEffect(ship, ally);
			float tempRange = MathUtils.getDistance(ship, ally);
			if (tempEffect > effect || (tempEffect == effect && tempRange < range - 20f)) {
				range = tempRange;
				effect = tempEffect;
				target = ally;
			}
		}

		return target;
	}

	private static float getEffect(ShipAPI s1, ShipAPI s2) {
		return 0.5f * Math.min(s1.getMutableStats().getFluxDissipation().getModifiedValue(), s2.getMutableStats().getFluxDissipation().getModifiedValue());
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return PLSP_DataBase.isPLSPShip(ship) && ship.getVariant().getHullMods().contains("PLSP_clustermodulator");
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (!ship.getVariant().getHullMods().contains("PLSP_clustermodulator")) {
			ship.getVariant().removeMod("PLSP_clusteraddition");
		}
	}

	public final static class ModulatorAdditionState {

		ShipAPI source;
		public ShipAPI linkingTarget; // this means, you are sending flux to this ship
		public float effectLevel; // actually who is sending flux to you are stored in receivingInSec, and not in this class

		public float sendingInSec;

		public SpriteAPI spriteBand;
		public Vector2f fixedPointToRenderBand;
		public float bandSeed;
		public float frac;

		public ModulatorAdditionState(ShipAPI source) {

			this.source = source;
			this.linkingTarget = null;
			this.effectLevel = 0f;

			this.sendingInSec = 0f;

			this.fixedPointToRenderBand = ZERO;
			this.bandSeed = 0f;
			this.frac = 1f;

			this.spriteBand = Global.getSettings().getSprite("misc", "PLSP_clusterAdditionBand");
			resetAlpha();
		}

		public void applyLinkingTarget(Map<ShipAPI, ModulatorState> mainMap) {
			if (linkingTarget == null || !linkingTarget.isAlive()) return;

			ModulatorState targetData = mainMap.get(linkingTarget);
			if (targetData != null) targetData.receivingInSec.put(source.getId() + id, sendingInSec);
		}

		public void unapplyLinkingTarget(Map<ShipAPI, ModulatorState> mainMap) {
			if (linkingTarget == null || !linkingTarget.isAlive()) return;

			ModulatorState targetData = mainMap.get(linkingTarget);
			if (targetData != null) targetData.receivingInSec.remove(source.getId() + id);

			linkingTarget = null;
			sendingInSec = 0f;
		}

		public void advanceVisual(float amount) {

			if (effectLevel <= 0f) {
				resetAlpha();
				return;
			}

			// band advance
			if (linkingTarget != null) {
				float textureScroll = 4f + effectLevel * (float)Math.sqrt(sendingInSec) * 0.05f;
				float startX = bandSeed - textureScroll * amount;
				if (startX < -10000f) {
					startX += 10000f;
				}

				float angle = VectorUtils.getAngle(linkingTarget.getLocation(), linkingTarget.getLocation());
				float distant = MathUtils.getDistance(linkingTarget.getLocation(), linkingTarget.getLocation());
				Vector2f midPoint = MathUtils.getMidpoint(linkingTarget.getLocation(), linkingTarget.getLocation());

				frac = distant / spriteBand.getWidth();
				bandSeed = startX;

				fixedPointToRenderBand = MathUtils.getPoint(midPoint, spriteBand.getWidth() * startX + distant * 0.5f - spriteBand.getWidth() * 0.5f, angle + 180f);
				spriteBand.setAngle(angle);
				spriteBand.setAlphaMult(effectLevel);
			}
		}

		public void resetAlpha() {
			spriteBand.setAlphaMult(0f);
		}
	}
}
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_DataBase;
import data.scripts.util.PLSP_Util;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class PLSP_ClusterModulator extends BaseHullMod {
	private static final String id = "PLSP_ClusterModulator";
	private static final Vector2f ZERO = new Vector2f();

	private static String getString(String key) {
		return Global.getSettings().getString("HullMod", "PLSP_" + key);
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return !isForModSpec;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 10f;
		float padS = 2f;

		tooltip.addPara("%s " + getString("clustermodulatorTEXT1"), pad, Misc.getHighlightColor(), "#", "800", Global.getSettings().getHullModSpec("PLSP_clustermodulator").getDisplayName());
		tooltip.addPara("    %s " + getString("clustermodulatorTEXT2"), padS, Misc.getHighlightColor(), "#");
		tooltip.addPara("    %s " + getString("clustermodulatorTEXT3"), padS, Misc.getHighlightColor(), "#");
		tooltip.addPara("    %s " + getString("clustermodulatorTEXT4"), padS, Misc.getNegativeHighlightColor(), "#", "20", "+1%", "+0.5%");
		//tooltip.addPara("    %s " + getString("clustermodulatorTEXT5"), padS, Misc.getNegativeHighlightColor(), "#", "20", "+0.5%");
		tooltip.addPara("%s " + getString("clustermodulatorTEXT6"), padS, Misc.getHighlightColor(), "#");
		tooltip.addPara("%s " + getString("clustermodulatorTEXT7"), padS, Misc.getHighlightColor(), "#");
		tooltip.addPara("%s " + getString("HAO"), pad, Misc.getNegativeHighlightColor(), "#");
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.getCustomData().containsKey(id)) {
			engine.getCustomData().put(id, new HashMap<>());
		}

		Map<ShipAPI, ModulatorState> shipsMap = (Map)engine.getCustomData().get(id);
		if (!engine.isEntityInPlay(ship) || !ship.isAlive()) {
			if (!ship.isAlive() && shipsMap.containsKey(ship)) {
				ModulatorState data = shipsMap.get(ship);
				data.resetAlpha();
				shipsMap.remove(ship);
			}
			return;
		}

		if (!shipsMap.containsKey(ship)) {
			shipsMap.put(ship, new ModulatorState(ship));
		} else {
			ModulatorState data = shipsMap.get(ship);

			data.advanceVisual(amount);
			if (engine.isPaused()) return;

			float totalInSec = data.getTotalTransferInSec();
			if (totalInSec > 0f) {
				float damageP = 0.05f * totalInSec;
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

			float maxRange = 800f;
			ShipAPI mostEffectiveTarget = findEffectiveTarget(shipsMap, ship, maxRange); // so here pick target to send flux
			if (mostEffectiveTarget != data.linkingTarget) {
				if (mostEffectiveTarget == null && availableToIncreaseEffectLevel) { // means should cut off
					data.effectLevel -= amount * 2f;
					data.effectLevel = Math.max(data.effectLevel, 0f);
					availableToIncreaseEffectLevel = false;
				}
				if (mostEffectiveTarget != null && data.effectLevel <= 0f) { // means should connect another one
					data.unapplyLinkingTarget(shipsMap);
					data.linkingTarget = mostEffectiveTarget;
					availableToIncreaseEffectLevel = true;
				}
			} // otherwise keep current

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
					data.applyLinkingTarget(shipsMap);
					flux.decreaseFlux(sendingInAmount);
					fluxT.increaseFlux(sendingInAmount, false);
				}
			}

			if (availableToIncreaseEffectLevel) {
				data.effectLevel += amount * 2f;
				data.effectLevel = Math.min(data.effectLevel, 1f);
			}

			if (data.linkingTarget != null && data.effectLevel <= 0f) {
				data.unapplyLinkingTarget(shipsMap);
			}
		}
	}

	private static ShipAPI findEffectiveTarget(Map<ShipAPI, ModulatorState> shipsMap, ShipAPI ship, float maxRange) {
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
		for (ShipAPI ally : shipsMap.keySet()) {
			if (ally == ship) continue;
			if (ally.getOwner() != ship.getOwner()) continue;
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
		return Math.min(s1.getMutableStats().getFluxDissipation().getModifiedValue(), s2.getMutableStats().getFluxDissipation().getModifiedValue());
	}

	private static float log(float num) {
		return (float)Math.log(Math.max(num, 1f));
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return PLSP_DataBase.isPLSPShip(ship);
	}

	public final static class ModulatorState {

		ShipAPI source;
		ShipAPI linkingTarget; // this means, you are sending flux to this ship
		public float effectLevel; // actually who is sending flux to you are stored in receivingInSec

		public float sendingInSec; // how much flux will be send within sec
		public Map<String, Float> receivingInSec; // how much flux other sends you within sec

		public SpriteAPI spriteBand;
		public Vector2f fixedPointToRenderBand;
		public float bandSeed;
		public float frac;

		public SpriteAPI spriteRing;
		public float ringSize;

		public SpriteAPI spriteCore;
		public float coreBlink;

		public SpriteAPI spriteMistM;
		public SpriteAPI spriteMistI;
		public SpriteAPI spriteMistO;
		public float mistAngleM;
		public float mistAngleI;
		public float mistAngleO;

		public ModulatorState(ShipAPI source) {
			this.source = source;
			this.linkingTarget = null;
			this.effectLevel = 0f;

			this.sendingInSec = 0f;
			this.receivingInSec = new HashMap<>();

			this.spriteBand = Global.getSettings().getSprite("misc", "PLSP_clusterBand");
			this.fixedPointToRenderBand = ZERO;
			this.bandSeed = 0f;
			this.frac = 1f;

			this.spriteRing = Global.getSettings().getSprite("misc", "PLSP_clusterRing");
			this.ringSize = 0f;

			this.spriteCore = Global.getSettings().getSprite("misc", "PLSP_clusterCore");
			this.coreBlink = 0f;

			this.spriteMistM = Global.getSettings().getSprite("misc", "PLSP_clusterMistM");
			this.spriteMistI = Global.getSettings().getSprite("misc", "PLSP_clusterMistI");
			this.spriteMistO = Global.getSettings().getSprite("misc", "PLSP_clusterMistO");
			this.mistAngleM = (float)Math.random() * 360f;
			this.mistAngleI = (float)Math.random() * 360f;
			this.mistAngleO = (float)Math.random() * 360f;

			resetAlpha();
		}

		public float getTotalTransferInSec() {
			float totalInSec = sendingInSec;
			for (float receiving : receivingInSec.values()) {
				totalInSec += receiving;
			}

			return totalInSec;
		}

		public void applyLinkingTarget(Map<ShipAPI, ModulatorState> shipsMap) {
			if (linkingTarget == null || !linkingTarget.isAlive()) return;

			ModulatorState targetData = shipsMap.get(linkingTarget);
			if (targetData != null) targetData.receivingInSec.put(source.getId(), sendingInSec);
		}

		public void unapplyLinkingTarget(Map<ShipAPI, ModulatorState> shipsMap) {
			if (linkingTarget == null || !linkingTarget.isAlive()) return;

			ModulatorState targetData = shipsMap.get(linkingTarget);
			if (targetData != null) targetData.receivingInSec.remove(source.getId());

			linkingTarget = null;
			sendingInSec = 0f;
		}

		public void advanceVisual(float amount) {

			if (effectLevel <= 0f) {
				resetAlpha();
				return;
			}

			// band advance
			if (linkingTarget != null && linkingTarget.isAlive()) {
				float textureScroll = 5f + effectLevel * (float)Math.sqrt(sendingInSec) * 0.05f;
				float startX = bandSeed - textureScroll * amount;
				if (startX < -10000f) {
					startX += 10000f;
				}

				float angle = VectorUtils.getAngle(source.getLocation(), linkingTarget.getLocation());
				float distant = MathUtils.getDistance(source.getLocation(), linkingTarget.getLocation());
				Vector2f midPoint = MathUtils.getMidpoint(source.getLocation(), linkingTarget.getLocation());

				frac = distant / spriteBand.getWidth();
				bandSeed = startX;

				fixedPointToRenderBand = MathUtils.getPoint(midPoint, spriteBand.getWidth() * startX + distant * 0.5f - spriteBand.getWidth() * 0.5f, angle + 180f);
				spriteBand.setAngle(angle);
				spriteBand.setAlphaMult(effectLevel);
			}

			// context
			float totalInSec = getTotalTransferInSec();
			boolean moreSendThanReceive = sendingInSec * 3f > totalInSec; // normally 2f, but we have it 3f

			// ring advance
			float ringExpandSpeed = amount * (log(totalInSec) * 0.5f + 1f);
			if (moreSendThanReceive) {
				ringSize += ringExpandSpeed;
				if (ringSize >= 1f) {
					ringSize -= 1f;
				}
			} else {
				ringSize -= ringExpandSpeed;
				if (ringSize <= 0f) {
					ringSize += 1f;
				}
			}

			float ringDisplayRadius = (source.getShieldRadiusEvenIfNoShield() * 3.3f + 60f) * ringSize;
			float ringAlphaMult = -8f * (ringSize - 0.5f) * (ringSize - 0.5f) + 2f;
			spriteRing.setSize(ringDisplayRadius, ringDisplayRadius);
			spriteRing.setAlphaMult(effectLevel * ringAlphaMult);

			// core advance
			float coreDisplayRadius = source.getCollisionRadius() * 2f + 20f;
			spriteCore.setSize(coreDisplayRadius, coreDisplayRadius);

			double sine = Math.min(Math.abs(FastTrig.sin(coreBlink)) * 0.25d, effectLevel);
			spriteCore.setAlphaMult(Math.min(effectLevel, 0.75f) + (float)sine);
			coreBlink += amount * 2f;
			if (coreBlink > 10000f) {
				coreBlink -= 10000f;
			}

			// mist advance, M
			float mMistDisplayRadius = source.getShieldRadiusEvenIfNoShield() * 2.4f + 50f;
			spriteMistM.setSize(mMistDisplayRadius, mMistDisplayRadius);
			spriteMistM.setAlphaMult((float)Math.sqrt(effectLevel));
			spriteMistM.setAngle(mistAngleM);
			mistAngleM += 5f * amount * (effectLevel + 1f) * (Math.max(0.2f, log(totalInSec)) + 1f);
			if (mistAngleM > 10000f) {
				mistAngleM -= 10000f;
			}

			// mist advance, I
			float iMistDisplayRadius = source.getShieldRadiusEvenIfNoShield() + 100f;
			spriteMistI.setSize(iMistDisplayRadius, iMistDisplayRadius);
			spriteMistI.setAlphaMult(Math.min((float)Math.sqrt(effectLevel) * 1.5f, 1f));
			spriteMistI.setAngle(mistAngleI);
			mistAngleI -= 8f * amount * (Math.max(5f, (float)Math.sqrt(totalInSec)) + 1f);
			if (mistAngleI < -10000f) {
				mistAngleI += 10000f;
			}

			// mist advance, O
			float oMistDisplayRadius = source.getShieldRadiusEvenIfNoShield() * 4f * Math.max(0.6f, effectLevel);
			spriteMistO.setSize(oMistDisplayRadius, oMistDisplayRadius);
			spriteMistO.setAlphaMult(effectLevel * 0.7f + 0.05f);
			spriteMistO.setAngle(mistAngleO);
			mistAngleO -= 2f * amount * (Math.min(0.8f, effectLevel) + 1f);
			if (mistAngleO < -10000f) {
				mistAngleO += 10000f;
			}
		}

		public void resetAlpha() {
			spriteBand.setAlphaMult(0f);
			spriteRing.setAlphaMult(0f);
			spriteMistM.setAlphaMult(0f);
			spriteMistI.setAlphaMult(0f);
			spriteMistO.setAlphaMult(0f);
			spriteCore.setAlphaMult(0f);
		}
	}
}
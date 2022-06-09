//Originally by Nicke535, licensed under CC-BY-NC-SA 4.0 (https://creativecommons.org/licenses/by-nc-sa/4.0/)
//Forked from MagicGuidedProjectileScript and modified by AnyIDElse
package data.scripts.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class PLSP_GuidingProjPlugin extends BaseEveryFrameCombatPlugin {
    private static final String DATA_KEY = "PLSP_GuidingProjPlugin";

    private final static class GuidingProjData {
        private final float maxTargetingRange;
        private final float maxAngleForTargeting;
        private final float maxTurnRate;

        //Number of iterations to run for calculations.
        //At 0 it's indistinguishable from a dumbchaser, at 15 it's frankly way too high. 4-7 recommended for slow weapons, 2-3 for weapons with more firerate/lower accuracy
        private final int iterationForAccuracy;

        //Factor for how good the AI judges target leading
        //At 1f it tries to shoot the "real" intercept point, while at 0f it's indistinguishable from a dumbchaser.
        private final float rateForAccuracy;

        private CombatEntityAPI target;
        private float noGuidanceTime; // Counter for delaying targeting

        private GuidingProjData(DamagingProjectileAPI proj, float noGuidanceTime, float maxTargetingRange, float maxAngleForTargeting, float maxTurnRate, float extraAccuracy) {
            float accuracyBonus = proj.getSource().getMutableStats().getAutofireAimAccuracy().modified + 1f;
            accuracyBonus = Math.max(accuracyBonus, 0.1f);

            this.target = null;
            this.noGuidanceTime = noGuidanceTime / accuracyBonus;

            this.maxTargetingRange = maxTargetingRange;
            this.maxAngleForTargeting = Math.min(maxAngleForTargeting * 0.5f * (float) Math.sqrt(accuracyBonus), 180f);
            this.maxTurnRate = maxTurnRate * (float) Math.sqrt(accuracyBonus);

			accuracyBonus = accuracyBonus + extraAccuracy;
            this.iterationForAccuracy = (int) Math.min(4f * accuracyBonus, 8f);
            this.rateForAccuracy = Math.max(Math.min(0.5f * accuracyBonus, 1f), 0f);
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        Global.getCombatEngine().getCustomData().put(DATA_KEY, new LocalData());
    }

	public static final class LocalData {
		final Map<DamagingProjectileAPI, GuidingProjData> guidingProjs = new HashMap<>(100);
		final Map<CombatEntityAPI, Integer> alreadyTargeted = new HashMap<>(100);
	}

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }

		final LocalData localData = (LocalData)Global.getCombatEngine().getCustomData().get(DATA_KEY);
        final Map<DamagingProjectileAPI, GuidingProjData> guidingProjs = localData.guidingProjs;
		final Map<CombatEntityAPI, Integer> alreadyTargeted = localData.alreadyTargeted;

		for (DamagingProjectileAPI projectile : Global.getCombatEngine().getProjectiles()) {
            if (projectile.getElapsed() > 0f) {//only run once when projectile was created.
                continue;
            }

            String projectileID = projectile.getProjectileSpecId();
            if (projectileID == null) {
                continue;
            }

            switch (projectileID) {
                case "PLSP_guidingpds_shot":
                    guidingProjs.put(projectile, new GuidingProjData(projectile, 0.1f, 1000f, 240f, 360f, 0.15f));
                    break;
                case "PLSP_guidingpdm_shot":
                    guidingProjs.put(projectile, new GuidingProjData(projectile, 0.1f, 1200f, 240f, 360f, 0.3f));
                    break;
                case "PLSP_guidingpdl_shot":
                    guidingProjs.put(projectile, new GuidingProjData(projectile, 0.05f, 1200f, 360f, 720f, 0.15f));
                    break;
                default:
                    break;
            }
        }

		if (!guidingProjs.isEmpty()) {
            Iterator<Map.Entry<DamagingProjectileAPI, GuidingProjData>> iter = guidingProjs.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<DamagingProjectileAPI, GuidingProjData> entry = iter.next();
                DamagingProjectileAPI projectile = entry.getKey();
                GuidingProjData data = entry.getValue();
				if (projectile.didDamage() || projectile.isFading() || !Global.getCombatEngine().isEntityInPlay(projectile)) {
                    iter.remove();
                    continue;
                }

                //Delays targeting if we have that enabled
                if (data.noGuidanceTime > 0f) {
                    data.noGuidanceTime -= amount;
                    return;
                }

                //Check if we need to find a new target
                if (data.target != null) {
                    if (!Global.getCombatEngine().isEntityInPlay(data.target)) {
                        data.target = null;
                    }
                    if (data.target instanceof ShipAPI) {
                        ShipAPI shipTarget = (ShipAPI) data.target;
                        if (!shipTarget.isAlive() || shipTarget.isPhased() || shipTarget.getOwner() == projectile.getOwner()) {
                            data.target = null;
                        }
                    }
                }

                //If we need to retarget, check our retarget strategy and act accordingly
                if (data.target == null) {
                    reacquireTarget(projectile, data, alreadyTargeted);
                }

                //We start our guidance stuff...
                if (data.target != null) {
                	if (!alreadyTargeted.containsKey(data.target)) {
                		alreadyTargeted.put(data.target, 1);
					} else {
						alreadyTargeted.put(data.target, alreadyTargeted.get(data.target) + 1);
					}
                    float newFacing = projectile.getFacing();
                    Vector2f targetPoint = getApproximateInterception(projectile, data);
                    float angleToHit = VectorUtils.getAngle(projectile.getLocation(), targetPoint);
                    float angleDiff = MathUtils.getShortestRotation(newFacing, angleToHit);
                    if (angleDiff > 0f) {
                        newFacing += Math.min(angleDiff, data.maxTurnRate * amount);
                    } else if (angleDiff < 0f) {
                        newFacing += Math.max(angleDiff, -data.maxTurnRate * amount);
                    }

                    projectile.setFacing(newFacing);
                    projectile.getVelocity().set(MathUtils.getPoint(null, projectile.getVelocity().length(), newFacing));
                }
            }
        }

		alreadyTargeted.clear();
    }

    private static void reacquireTarget(DamagingProjectileAPI proj, GuidingProjData data, Map<CombatEntityAPI, Integer> alreadyTargeted) {
        Vector2f centerOfDetection = proj.getLocation();

        List<CombatEntityAPI> potentialTargets = new ArrayList<>();
        if (proj.getWeapon().hasAIHint(WeaponAPI.AIHints.PD)) {
			for (MissileAPI potTarget : CombatUtils.getMissilesWithinRange(centerOfDetection, data.maxTargetingRange)) {
				if (potTarget.getOwner() != proj.getOwner() && Math.abs(MathUtils.getShortestRotation(proj.getFacing(), VectorUtils.getAngle(proj.getLocation(), potTarget.getLocation()))) < data.maxAngleForTargeting) {
					if (potTarget.isFlare() && proj.getSource().getMutableStats().getDynamic().getMod(Stats.PD_IGNORES_FLARES).computeEffective(0f) > 0f) {
						continue;
					}
					potentialTargets.add(potTarget);
				}
			}
		}

        for (ShipAPI potTarget : CombatUtils.getShipsWithinRange(centerOfDetection, data.maxTargetingRange)) {
            if (potTarget.getOwner() == proj.getOwner() || !potTarget.isAlive()
                    || Math.abs(MathUtils.getShortestRotation(proj.getFacing(), VectorUtils.getAngle(proj.getLocation(), potTarget.getLocation()))) > data.maxAngleForTargeting
                    || potTarget.isHulk() || potTarget.isPhased()) {
                continue;
            }
            potentialTargets.add(potTarget);
        }

        if (!potentialTargets.isEmpty()) {
			CombatEntityAPI newTarget = null;
            float minDist = 10000000f;
            for (CombatEntityAPI tmp : potentialTargets) {
                float distance = MathUtils.getDistance(tmp, centerOfDetection);
                distance += 5f * Math.abs(MathUtils.getShortestRotation(proj.getFacing(), VectorUtils.getAngle(proj.getLocation(), tmp.getLocation())));
                if (tmp instanceof MissileAPI) {
                    distance -= 500f;
                } else if (tmp instanceof ShipAPI && ((ShipAPI)tmp).isFighter()) {
                    distance -= 200f;
                }

                float projVel = Math.max(proj.getVelocity().length(), 1f);
                float tmpVel = Math.max(tmp.getVelocity().length(), 1f);
                distance *= tmpVel / projVel;

                if (alreadyTargeted.containsKey(tmp)) {
                	distance += alreadyTargeted.get(tmp) * 50f;
				}

                if (distance < minDist) {
                    newTarget = tmp;
                    minDist = distance;
                }
            }
            data.target = newTarget;
        }
    }

    private static Vector2f getApproximateInterception(DamagingProjectileAPI proj, GuidingProjData data) {
        Vector2f returnPoint = new Vector2f(data.target.getLocation());
        if (proj.getVelocity().lengthSquared() < 0.01f) {
        	return returnPoint;
		}

        for (int i = 0; i < data.iterationForAccuracy; i++) {
            float arrivalTime = MathUtils.getDistance(proj.getLocation(), returnPoint) / proj.getVelocity().length();
            returnPoint.x = data.target.getLocation().x + (data.target.getVelocity().x * arrivalTime * data.rateForAccuracy);
            returnPoint.y = data.target.getLocation().y + (data.target.getVelocity().y * arrivalTime * data.rateForAccuracy);
        }

        return returnPoint;
    }
}
package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import data.scripts.plugin.PLSP_CampaignPlugin;
import data.scripts.util.MagicSettings;
import data.scripts.util.PLSP_BlackList;
import data.scripts.util.PLSP_Util;
import data.scripts.weapons.ai.PLSP_MagneticMissileAI;
import data.scripts.weapons.ai.PLSP_MagneticMissileLargeAI;
import data.scripts.world.systems.Triglav;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

import java.util.ArrayList;
import java.util.List;

public class PLSPModPlugin extends BaseModPlugin {
	public static boolean modifiedVentingAI = true;
	
	private static String getString(String key) {
		return Global.getSettings().getString("Misc", "PLSP_" + key);
	}
	
	@Override
	public void onApplicationLoad() {

		if (!PLSP_BlackList.getCode().contentEquals("9a5d472831d9924a56940a2ebe783bd")) {
			throw new RuntimeException();
		}

		for (String id : PLSP_BlackList.getBlackListModId()) {
			if (Global.getSettings().getModManager().isModEnabled(id)) {
				throw new RuntimeException(String.format(getString("incMod"), Global.getSettings().getModManager().getModSpec("Polaris_Prime").getName(), Global.getSettings().getModManager().getModSpec(id).getName()));
			}
		}

		List<String> hullIds = new ArrayList<>();
		List<String> weaponIds = new ArrayList<>();
		for (ShipHullSpecAPI hull : Global.getSettings().getAllShipHullSpecs()) {
			hullIds.add(hull.getHullId());
		}
		for (WeaponSpecAPI weapon : Global.getSettings().getAllWeaponSpecs()) {
			weaponIds.add(weapon.getWeaponId());
		}
		if (!intersectionConfirm(PLSP_BlackList.getBlackListShipId(), hullIds) || !intersectionConfirm(PLSP_BlackList.getBlackListWeaponId(), weaponIds)) {
			throw new RuntimeException(Global.getSettings().getModManager().getModSpec("Polaris_Prime").getName() + " " + getString("imMod"));
		}

		if (Global.getSettings().getModManager().isModEnabled("ungp")) {
			ModSpecAPI spec = Global.getSettings().getModManager().getModSpec("ungp");
			if (Integer.parseInt(spec.getVersionInfo().getMinor()) < 6) {
				throw new RuntimeException("Your UNGP is too old, get a new one in fossic.org!");
			}
		}

		ShaderLib.init();
		LightData.readLightDataCSV("data/lights/PLSP_light_data.csv");
		TextureData.readTextureDataCSV("data/lights/PLSP_texture_data.csv");

		modifiedVentingAI = MagicSettings.getBoolean("plsp", "modifiedVentingAI");
	}
	
	@Override
	public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
		switch (missile.getProjectileSpecId()) {
			case "PLSP_magnetic_missile":
				return new PluginPick<MissileAIPlugin>(new PLSP_MagneticMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
			case "PLSP_magnetic_missile_large":
				return new PluginPick<MissileAIPlugin>(new PLSP_MagneticMissileLargeAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
			default:
		}
		return null;
	}
	
	@Override
	public void onNewGame() {
		ProcgenUsedNames.notifyUsed("Triglav");
		ProcgenUsedNames.notifyUsed("Perun");
		ProcgenUsedNames.notifyUsed("Veles");
		ProcgenUsedNames.notifyUsed("Hors");
		ProcgenUsedNames.notifyUsed("Vesna");
		ProcgenUsedNames.notifyUsed("Belobog");
		ProcgenUsedNames.notifyUsed("Chernobog");

		if (PLSP_Util.NEX()) {
			PLSP_SetNEXSettings.generate(Global.getSector());
		} else {
			newGenerate(Global.getSector());
		}

		MarketAPI market = PLSP_Util.pickMarket(Global.getSector().getFaction("plsp"));
		if (market != null) {
			market.addIndustry("PLSP_militaryacademy");
			market.getIndustry("PLSP_militaryacademy").setAICoreId("alpha_core");
		}

		SharedData.getData().getPersonBountyEventData().addParticipatingFaction("plsp");
		Global.getSector().addScript(new PLSP_CampaignPlugin());
	}

	@Override
	public void onGameLoad(boolean newGame){
	}

	@Override
	public void beforeGameSave(){
	}
	
	public static void newGenerate(SectorAPI sector) {
		new Triglav().generate(sector);
		relationAdj(sector);
	}
	
	private static void relationAdj(SectorAPI sector) {
		FactionAPI plsp = sector.getFaction("plsp");

		plsp.setRelationship("pirates", RepLevel.HOSTILE);
		plsp.setRelationship("sindrian_diktat", RepLevel.HOSTILE);
		plsp.setRelationship("independent", RepLevel.SUSPICIOUS);
		plsp.setRelationship("luddic_path", RepLevel.VENGEFUL);
		plsp.setRelationship("luddic_church", RepLevel.INHOSPITABLE);
		plsp.setRelationship("tritachyon", RepLevel.INHOSPITABLE);
		plsp.setRelationship("derelict", RepLevel.HOSTILE);
		plsp.setRelationship("remnant", RepLevel.HOSTILE);

		plsp.setRelationship("cabal", RepLevel.VENGEFUL);
	}

	private static boolean intersectionConfirm(List<String> listA, List<String> listB) {
		listA.retainAll(listB);
		return listA.isEmpty();
	}
}
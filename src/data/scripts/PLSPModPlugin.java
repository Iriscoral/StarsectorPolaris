package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
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
import data.scripts.util.PLSP_Util.I18nSection;
import data.scripts.weapons.ai.PLSP_MagneticMissileAI;
import data.scripts.weapons.ai.PLSP_MagneticMissileLargeAI;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

import java.util.ArrayList;
import java.util.List;

public class PLSPModPlugin extends BaseModPlugin {
	public static boolean modifiedVentingAI = true;
	
	public static final I18nSection strings = I18nSection.getInstance("Misc", "PLSP_");
	
	@Override
	public void onApplicationLoad() {

		if (!PLSP_BlackList.getCode().contentEquals("9a5d472831d9924a56940a2ebe783bd")) {
			throw new RuntimeException();
		}

		for (String id : PLSP_BlackList.getBlackListModId()) {
			if (Global.getSettings().getModManager().isModEnabled(id)) {
				throw new RuntimeException(String.format(strings.get("incMod"), Global.getSettings().getModManager().getModSpec("Polaris_Prime").getName(), Global.getSettings().getModManager().getModSpec(id).getName()));
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
			throw new RuntimeException(Global.getSettings().getModManager().getModSpec("Polaris_Prime").getName() + " " + strings.get("imMod"));
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
				return new PluginPick<MissileAIPlugin>(new PLSP_MagneticMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SET);
			case "PLSP_magnetic_missile_large":
				return new PluginPick<MissileAIPlugin>(new PLSP_MagneticMissileLargeAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SET);
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
			new PLSP_NEXGenerate().generate(Global.getSector());
		} else {
			new PLSP_NormalGenerate().generate(Global.getSector());
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

	private static boolean intersectionConfirm(List<String> listA, List<String> listB) {
		listA.retainAll(listB);
		return listA.isEmpty();
	}
}
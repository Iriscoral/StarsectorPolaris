package data.scripts;

import com.fs.starfarer.api.campaign.SectorAPI;
import exerelin.campaign.SectorManager;

public class PLSP_SetNEXSettings {
	
	public static void generate(SectorAPI sector) {
		if (SectorManager.getManager().isCorvusMode()) {
			PLSPModPlugin.newGenerate(sector);
		}
	}
}
package data.scripts;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import data.scripts.world.systems.Triglav;

public class PLSP_NormalGenerate implements SectorGeneratorPlugin {

	@Override
	public void generate(SectorAPI sector) {
		new Triglav().generate(sector);
		relationAdj(sector);
	}

	private void relationAdj(SectorAPI sector) {
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
}
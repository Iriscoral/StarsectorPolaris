package data.scripts.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.characters.LevelBasedEffect;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.PLSP_DataBase;

import java.util.List;

public class PLSP_SkillPicker extends BaseSpecialItemPlugin {
	private SkillSpecAPI skill;
	private static PersonAPI fakePerson = null;

	private static String getString(String key) {
		return Global.getSettings().getString("CMD", "PLSP_" + key);
	}
	
	@Override
	public void init(CargoStackAPI stack) {
		super.init(stack);
		String id = stack.getSpecialDataIfSpecial().getData();
		skill = Global.getSettings().getSkillSpec(id);
	}
	
	@Override
	public String getDesignType() {
		return "Polaris";
	}

	@Override
	public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
		SpriteAPI sprite = Global.getSettings().getSprite(skill.getSpriteName());
		float cx = x + w / 2f;
		float cy = y + h / 2f;
		if (sprite != null) {
			// sprite.setSize(64f, 64f);
			sprite.setNormalBlend();
			sprite.setAlphaMult(alphaMult);
			sprite.renderAtCenter(cx, cy);
		}
	}

	@Override
	public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
		tooltip.setTitleOrbitronLarge();
		tooltip.addTitle(skill.getName());

		float opad = 10f;
		float pad = 3f;
		tooltip.setParaFontOrbitron();
		tooltip.addPara(skill.getDescription(), opad);
		tooltip.setParaFontDefault();
		tooltip.addPara(skill.getAuthor(), Misc.getGrayColor(), opad);

		List<ShipSkillEffect> effects = PLSP_DataBase.getSkillEffects(skill.getId());
		if (!effects.isEmpty()) {
			tooltip.addPara("", opad);
			for (ShipSkillEffect effect : effects) {
				String prefix = "%s";

				if (effect.getScopeDescription() == LevelBasedEffect.ScopeDescription.PILOTED_SHIP) {
					prefix = getString("pilotedship");
				} else if (effect.getScopeDescription() == LevelBasedEffect.ScopeDescription.SHIP_FIGHTERS) {
					prefix = getString("pilotedfighter");
				}

				String base = effect.getEffectDescription(1);
				if (base != null) {
					tooltip.addPara(prefix, pad, Misc.getHighlightColor(), base);
				} else if (effect instanceof BaseSkillEffectDescription) {
					if (fakePerson == null) {
						fakePerson = Global.getFactory().createPerson();
					}
					fakePerson.getStats().setSkillLevel(skill.getId(), 1f);

					tooltip.addPara(prefix, pad, Misc.getHighlightColor(), "");
					BaseSkillEffectDescription desc = (BaseSkillEffectDescription)effect;
					desc.createCustomDescription(fakePerson.getStats(), skill, tooltip, getTooltipWidth());
				}
			}
		}

		if (skill.getScopeStr() != null && !skill.getScopeStr().isEmpty()) {
			tooltip.addPara(skill.getScopeStr(), opad);
		}
		if (skill.getScopeStr2() != null && !skill.getScopeStr2().isEmpty()) {
			tooltip.addPara(skill.getScopeStr2(), opad);
		}
	}

	@Override
	public boolean hasRightClickAction() {
		return false;
	}
}
package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;

public class PLSP_MAShowOfficersPlugin implements CustomUIPanelPlugin {

	private final HashMap<ButtonAPI, PersonAPI> buttonsToOfficer = new HashMap<>();
	private final PLSP_MAShowOfficersDelegate delegate;

	public PLSP_MAShowOfficersPlugin(PLSP_MAShowOfficersDelegate delegate) {
		this.delegate = delegate;
	}

	public void addButton(ButtonAPI button, PersonAPI officer) {
		buttonsToOfficer.put(button, officer);
	}

	@Override
	public void positionChanged(PositionAPI position) {}

	@Override
	public void renderBelow(float alphaMult) {}

	@Override
	public void render(float alphaMult) {}

	@Override
	public void advance(float amount) {}

	@Override
	public void processInput(List<InputEventAPI> events) {
		if (buttonsToOfficer.isEmpty()) return;

		for (InputEventAPI event : events) {
			if (event.isConsumed()) continue;

			if (event.isMouseMoveEvent()) {
				for (ButtonAPI button : buttonsToOfficer.keySet()) {
					if (button.isChecked()) {
						PersonAPI officer = buttonsToOfficer.get(button);
						delegate.setSelectedOfficer(officer);
						try {
							Robot robot = new Robot();
							robot.keyPress(KeyEvent.VK_ESCAPE);
							return;
						} catch (AWTException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
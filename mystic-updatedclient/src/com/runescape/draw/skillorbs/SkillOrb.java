package com.runescape.draw.skillorbs;

import java.text.NumberFormat;

import com.runescape.Client;
import com.runescape.cache.graphics.Sprite;
import com.runescape.draw.Rasterizer2D;
import com.runescape.util.SecondsTimer;
import com.runescape.util.SkillConstants;

/**
 * Represents a skill orb.
 * 
 * @author Professor Oak
 * @author Christian_
 */
public class SkillOrb {

	/**
	 * The skill this orb is intended for.
	 */
	private final int skill;
	
	/**
	 * The sprite icon for this skill orb.
	 */
	private final Sprite icon;

	/**
	 * The show timer. Resets when this orb
	 * receives experience.
	 */
	private SecondsTimer showTimer = new SecondsTimer();

	/**
	 * The orb's current alpha (transparency)
	 */
	private int alpha;

	/**
	 * Constructs this skill orb
	 * @param skill
	 */
	public SkillOrb(int skill, Sprite icon) {
		this.skill = skill;
		this.icon = icon;
	}

	/**
	 * Called upon the player receiving experience.
	 * 
	 * Resets the attributes of the orb 
	 * to make sure the orb is drawn
	 * properly.
	 */
	public void receivedExperience() {
		alpha = 255;
		showTimer.start(12);
	}

	/**
	 * Draws this skill orb
	 * @param x
	 * @param y
	 */
	public void draw(int x, int y) {
		final int percentProgress = percentage();
		Client.cacheSprite[359].drawAdvancedSprite(x, y, alpha);
		Rasterizer2D.setDrawingArea(60 + y, x + 1, x + 30, (int) (45 - (currentLevel() >= 99 ? 100 : percentProgress)) + 1 + y);
		Client.cacheSprite[360].drawAdvancedSprite(x + 1, 1 + y, alpha);
		Rasterizer2D.setDrawingArea((int) ((currentLevel() >= 99 ? 100 : percentProgress) - 38) + y, x + 30, x + 56, 1 + y);
		Client.cacheSprite[360].drawAdvancedSprite(x + 2, 1 + y, alpha);
		Rasterizer2D.defaultDrawingAreaSize();
		icon.drawAdvancedSprite(x + 30 - icon.myWidth / 2, 28 - icon.myHeight / 2 + y, alpha);
	}

	/**
	 * Draws a tooltip containing information about
	 * this skill orb.
	 */
	public void drawTooltip() {
		final int percentProgress = percentage();
		NumberFormat nf = NumberFormat.getInstance();
		int mouse_Y = Client.instance.mouseX;
		int mouseY = Client.instance.mouseY;

		Rasterizer2D.drawBoxOutline(mouse_Y, mouseY + 5, 122, 82, 0x513419);
		Rasterizer2D.drawTransparentBox(mouse_Y + 1, mouseY + 6, 122, 83, 0x646473,90);

		Client.instance.newSmallFont.drawBasicString(SkillConstants.SKILL_NAMES_ORDER[skill], mouse_Y + 122 / 6 +  Client.instance.newSmallFont.getTextWidth(SkillConstants.SKILL_NAMES_ORDER[skill]) - 25 , mouseY + 20, 16777215, 1); 
		Client.instance.newSmallFont.drawBasicString("Level: @gre@" + Client.instance.maximumLevels[skill] , mouse_Y + 5, mouseY + 35, 16777215, 1); 
		Client.instance.newSmallFont.drawBasicString("Exp: @gre@" + nf.format(Client.instance.currentExp[skill]), mouse_Y + 5, mouseY + 50, 16777215, 1); 
		Client.instance.newSmallFont.drawBasicString("Exp Left: @gre@" + nf.format(remainderExp()), mouse_Y + 5, mouseY + 65, 16777215, 1);

		Rasterizer2D.drawBox(mouse_Y + 2, mouseY + 70, 118, 14, 0x666666);
		if(currentLevel() < 99) {
			Rasterizer2D.drawBox(mouse_Y + 2, mouseY + 70, percentProgress + 18, 14, Client.getProgressColor(percentProgress));
		} else {
			Rasterizer2D.drawBox(mouse_Y + 2, mouseY + 70,118, 14, Client.getProgressColor(percentProgress));
		}

		Client.instance.newSmallFont.drawCenteredString(percentProgress+ "% ", mouse_Y + 118 / 2 + 10, mouseY + 82, 0xFFFFFF, 1);
	}

	private int currentLevel() {
		return Client.instance.maximumLevels[skill];
	}

	private int startExp() {
		return Client.getXPForLevel(currentLevel());
	}

	private int requiredExp() {
		return Client.getXPForLevel(currentLevel() + 1);
	}
	
	private int obtainedExp() {
		return Client.instance.currentExp[skill] - startExp();
	}
	
	private int remainderExp() {
		return requiredExp() - (startExp() + obtainedExp());
	}

	private int percentage() {
		//Attempt to calculate percent progress..
		int percent = 0;
		try {
			percent = (int) (((double) obtainedExp() / (double) (requiredExp() - startExp())) * 100);
			//Max percent progress is 100!
			if(percent > 100) {
				percent = 100;
			}
		} catch(ArithmeticException e) {
			e.printStackTrace();
		}
		return percent;
	}

	/**
	 * Gets the timer
	 * @return
	 */
	public SecondsTimer getShowTimer() {
		return showTimer;
	}

	/**
	 * Gets the skill
	 * @return
	 */
	public int getSkill() {
		return skill;
	}

	/**
	 * Gets the alpha
	 * @return
	 */
	public int getAlpha() {
		return alpha;
	}

	/**
	 * Decrements alpha
	 */
	public void decrementAlpha() {
		alpha -= 5;
	}
}

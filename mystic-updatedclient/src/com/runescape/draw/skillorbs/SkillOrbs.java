package com.runescape.draw.skillorbs;

import com.runescape.Client;
import com.runescape.util.SkillConstants;

/**
 * Handles skill orbs.
 * 
 * @author Professor Oak
 * @author Christian_
 */
public class SkillOrbs {

	/**
	 * The array containing all skill orbs.
	 * Each skill orb per available skill.
	 */
	public static final SkillOrb[] orbs = new SkillOrb[SkillConstants.SKILL_COUNT];
	
	/**
	 * Initializes orbs and their sprites.
	 */
	public static void init() {
		for(int i = 0; i < SkillConstants.SKILL_COUNT; i++) {
			orbs[i] = new SkillOrb(i, Client.cacheSprite[361 + i]);
		}
	}
	
	/**
	 * Processes all orbs.
	 */
	public static void process() {
	
		//Our counter
		int totalOrbs = 0;
		
		//Count valid orbs..
		for(SkillOrb orb : orbs) {
			if(draw(orb)) {
				totalOrbs++;
			}
		}
				
		//Is the bounty hunter interface open? Then the orbs may need to be re-positioned.
		final boolean blockingInterfaceOpen = Client.instance.openWalkableInterface == 23300;
		boolean hpOverlay = Client.instance.shouldDrawCombatBox();
		
		//Positionining of orbs		
		int y = -2;
		int x = (int)(Client.frameWidth / 3.1) - (totalOrbs * 30);
		
		if(blockingInterfaceOpen) {
			x -= (totalOrbs * 10);
		} else {
			if(hpOverlay) {
				if(x < 130) {
					x = 130;
				}
				y = 12;
			}
		}
		
		if(x < 5) {
			x = 5;
		}
		
		//Current skillorb hover
		SkillOrb hover = null;
		
		//Draw orbs and get current hover...
		for(SkillOrb orb : orbs) {
			if(draw(orb)) {
				
				//Fade orb if needed
				if(orb.getShowTimer().finished()) {
					orb.decrementAlpha();
				}
					
				//Draw orb
				orb.draw(x, y);
				
				//Check if this orb is being hovered
				if(Client.instance.hover(x, y, Client.cacheSprite[359])) {
					hover = orb;
				}
				
				//Increase x, space between orbs
				x += 62;				
				if(x > (blockingInterfaceOpen ? 300 : 460)) {
					break;
				}
			}
		}
		
		//Draw hover tooltip
		if(hover != null) {
			hover.drawTooltip();
		}
	}
	
	/**
	 * Should a skillorb be drawn?
	 * @param orb
	 * @return
	 */
	private static boolean draw(SkillOrb orb) {
		return !orb.getShowTimer().finished() || orb.getAlpha() > 0;
	}
}

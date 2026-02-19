package com.runescape;

import java.io.File;

/**
 * The main configuration for the Client
 * 
 * @author Seven
 */
public final class Configuration {
	
	private Configuration() {
		
	}

	/**
	 * Sends client-related debug messages to the client output stream
	 */
	public static boolean client_debug = true;

	/**
	 * The address of the server that the client will be connecting to
	 */
	public static String server_address = "localhost"; // 108.61.41.186            localhost
	/**
	 * The port of the server that the client will be connecting to
	 */
	public static int server_port = 43894;
	
	public static final int CLIENT_VERSION = 317;

	//public static final String CACHE_DIRECTORY = "./Cache/";//System.getProperty("user.home") + File.separator + "OSRSPKV"+CLIENT_VERSION+"/";
    public static final String CACHE_DIRECTORY = System.getProperty("user.home") + File.separator + "Dodian-Exorth"+CLIENT_VERSION+"/";

	public static boolean JAGCACHED_ENABLED = false;

	/**
	 * Toggles a security feature called RSA to prevent packet sniffers
	 */
	public static final boolean ENABLE_RSA = false;

	/**
	 * A string which indicates the Client's name.
	 */
	public static final String CLIENT_NAME = "Dodian 3.0";

	/**
	 * Dumps map region images when new regions are loaded.
	 */
	public static boolean dumpMapRegions = false;

	/**
	 * Displays debug messages on loginscreen and in-game
	 */
	public static boolean clientData = false;

	/**
	 * Enables the use of music played through the client
	 */
	public static boolean enableMusic = false;

	/**
	 * Toggles the ability for a player to see roofs in-game
	 */
	public static boolean enableRoofs = false;

	/**
	 * Used for change worlds button on login screen
	 */
	public static boolean worldSwitch = false;

	/**
	 * Enables extra frames in-between animations to give the animation a smooth
	 * look
	 */
	public static boolean enableTweening = false;

	/**
	 * Used to repack indexes Index 1 = Models Index 2 = Animations Index 3 =
	 * Sounds/Music Index 4 = Maps
	 */
	public static boolean repackIndexOne = false, repackIndexTwo = false, repackIndexThree = false,
			repackIndexFour = false;

	/**
	 * Dump Indexes Index 1 = Models Index 2 = Animations Index 3 = Sounds/Music
	 * Index 4 = Maps
	 */
	public static boolean dumpIndexOne = false, dumpIndexTwo = false, dumpIndexThree = false, dumpIndexFour = false;

	/**
	 * Used to merge all the OS Buddy XP Drops so the counter doesn't get too
	 * big if you are training a lot of different skills
	 */
	public static boolean xp_merge = true;

	/**
	 * Enables fog effects
	 */
	public static boolean enableFog = true;

	/**
	 * Does the escape key close current interface?
	 */
	public static boolean escapeCloseInterface = true;
	
	/**
	 * Enables/Disables Revision 554 hitmarks
	 */
	public static boolean hitmarks554 = false;

	/**
	 * npcBits can be changed to what your server's bits are set to.
	 */
	public static final int npcBits = 14;

	public static final String CLIENT_BUILD = "3.0 BETA";

	/**
	 * Enables the use of run energy
	 */
	public static boolean runEnergy = true;

	/**
	 * Displays health above entities heads
	 */
	public static boolean hpAboveHeads = false;

	/**
	 * Displays names above entities
	 */
	public static boolean namesAboveHeads = false;

	/**
	 * Displays OS Buddy orbs on HUD
	 */
	public static boolean enableOrbs = true;

	/**
	 * Enables/Disables Revision 554 health bar
	 */
	public static boolean hpBar554 = false;

	/**
	 * Enables the HUD to display 10 X the amount of hitpoints
	 */
	public static boolean tenXHp = false;
	
	/**
	 * Should it be snow in the game? White floor.
	 */
	public static boolean snow = false;
	
	/**
	 * Should it always be a left click to attack a player,
	 * no matter if they are higher combat level than us?
	 */
	public static boolean alwaysLeftClickAttack = true;
	
	/**
	 * Is the combat overlay box enabled?
	 */
	public static boolean combatOverlayBox = false;
	
	/**
	 * Enables bounty hunter interface
	 */
	public static boolean bountyHunterInterface = true;

	public static int xpPosition;

}

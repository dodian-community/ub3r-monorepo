package com.fox;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;

public class Settings {

	public static final String SERVER_NAME = "Dodian";
	public static final String LAUNCHER_VERSION = "1.0";
	public static final String DOWNLOAD_URL = "https://dodian.net/client/DodianClient.jar";
	
	public static final String SAVE_NAME = "Dodian.jar";
	public static final String SAVE_DIR = System.getProperty("user.home") + File.separator;
	
	public static final String SERVER_IP = "dodian.net";
	public static final int SERVER_PORT = 43594;
	
	public static final boolean enableMusicPlayer = false;
	
	// Frame Settings
	public static final Dimension frameSize = new Dimension(600, 350);
	public static final Color borderColor = new Color(156, 27, 47);
	public static final Color backgroundColor = new Color(219, 204, 159);
	public static final Color primaryColor = new Color(156, 129, 100); // 226, 166, 59
	public static final Color iconShadow = new Color(0, 0, 0);
	public static final Color buttonDefaultColor = new Color(255, 255, 255);
	
	// link settings
	public static final String youtube = "";
	public static final String twitter = "";
	public static final String facebook = "";
	
	public static final String community = "http://dodian.net";
	public static final String leaders = "https://dodian.net/index.php?pageid=highscores";
	public static final String store = "https://dodian.net/index.php?pageid=droplist";
	public static final String vote = "https://dodian.net/index.php?pageid=vote";
	public static final String bugs = "https://dodian.net/forumdisplay.php?f=120";
	
}

package com.osroyale.login.impl;

import com.osroyale.*;
import com.osroyale.engine.impl.KeyHandler;
import com.osroyale.engine.impl.MouseHandler;
import com.osroyale.login.LoginComponent;
import com.osroyale.login.LoginModeState;
import com.osroyale.login.ScreenType;

/**
 * Handles the main screen of login (Elvarg landing & credentials interface).
 */
public class MainScreen extends LoginComponent {

	private static final int EMAIL_CHARACTER_LIMIT = 28;
	private static final String REMEMBER_ME_LABEL = "Save to Account Manager";
	public static int loginScreenState = 0;
	public static String accountCreationStatus = "";
	public static final LoginModeState loginMode = new LoginModeState();

	public MainScreen() {
	}

	private static Sprite classicBackground;
	private static Sprite logoSprite;
	private static Sprite titleboxSprite;
	private static Sprite titlebuttonSprite;
	private static boolean rawLoaded = false;

	@Override
	public void render(Client client) {
		refresh(client);
		load(client, 10);

        /* Message Check */
		if (client.loginMessage1.length() > 0 || client.loginMessage2.length() > 0) {
			client.loginRenderer.setScreen(new MessageScreen());
		}

		/* Raw Sprite Initialization */
		if (!rawLoaded && Client.spriteCache != null) {
			rawLoaded = true;
			classicBackground = Client.spriteCache.createMirroredTitleBackground("/login/title_background.png");
			logoSprite = Client.spriteCache.loadResourceSprite("/login/logo.png");
			titleboxSprite = Client.spriteCache.loadResourceSprite("/login/titlebox.png");
			titlebuttonSprite = Client.spriteCache.loadResourceSprite("/login/titlebutton.png");
		}

		int alpha = 255;

		/* 1. Classic 317 Mirrored Background */
		if (classicBackground != null) {
			classicBackground.drawARGBSprite(0, 0, alpha);
		} else {
			Sprite bg = Client.spriteCache.get(57);
			if (bg != null) bg.drawARGBSprite(0, 0, alpha);
		}

		/* 2. Classic Logo */
		if (logoSprite != null) {
			int logoX = (765 - logoSprite.width) / 2;
			logoSprite.drawARGBSprite(logoX, 15, alpha);
		}

		/* 3. Classic Title Box */
		int boxX = 202;
		int boxY = 180;
		if (titleboxSprite != null) {
			boxX = (765 - titleboxSprite.width) / 2;
			titleboxSprite.drawARGBSprite(boxX, boxY, alpha);
		}

		int btnW = titlebuttonSprite != null ? titlebuttonSprite.width : 147;

		/* State 0: Elvarg Landing Screen (Welcome to Dodian + Create Account / Existing User) */
		if (loginScreenState == 0) {
			client.boldText.drawCenteredText(0xFFFF00, boxX + 180, "Welcome to " + Configuration.NAME, boxY + 45, true);

			// Left Button: Create Account
			int btn1X = boxX + 25;
			int btnY = boxY + 95;
			if (titlebuttonSprite != null) {
				titlebuttonSprite.drawARGBSprite(btn1X, btnY, alpha);
				client.boldText.drawCenteredText(0xFFFFFF, btn1X + (btnW / 2), "Create Account", btnY + 25, true);
			}

			// Right Button: Existing User
			int btn2X = boxX + 188;
			if (titlebuttonSprite != null) {
				titlebuttonSprite.drawARGBSprite(btn2X, btnY, alpha);
				client.boldText.drawCenteredText(0xFFFFFF, btn2X + (btnW / 2), "Existing User", btnY + 25, true);
			}
		}

		/* State 1: Waiting on Discord Auth (shown the instant Create Account is clicked, before the browser opens) */
		if (loginScreenState == 1) {
			client.boldText.drawCenteredText(0xFFFF00, boxX + 180, "Account creation is done through Discord", boxY + 40, true);
			client.regularText.drawCenteredText(0xFFFFFF, boxX + 180, "A browser window will open - complete Discord", boxY + 70, true);
			client.regularText.drawCenteredText(0xFFFFFF, boxX + 180, "authorization there, then return to this window", boxY + 90, true);
			client.regularText.drawCenteredText(0xFFFFFF, boxX + 180, "to finish creating your account.", boxY + 110, true);

			// Cancel Button
			int btn1X = boxX + 25 + (btnW / 2);
			int btnY = boxY + 145;
			if (titlebuttonSprite != null) {
				titlebuttonSprite.drawARGBSprite(btn1X, btnY, alpha);
				client.boldText.drawCenteredText(0xFFFFFF, btn1X + (btnW / 2), "Cancel", btnY + 25, true);
			}
		}

		/* State 2: Login / Account Creation Credentials Screen */
		if (loginScreenState == 2) {
			if (!accountCreationStatus.isEmpty()) {
				client.regularText.drawCenteredText(0xFFFF00, boxX + 180, accountCreationStatus, boxY + 28, true);
			}

			int startY = accountCreationStatus.isEmpty() ? boxY + 50 : boxY + 56;

			// Username Prompt & Input
			client.boldText.drawText(true, boxX + 40, 0xFFFFFF, "Username: " + Utility.formatName(client.myUsername) + ((client.loginScreenCursorPos == 0) & (Client.tick % 40 < 20) ? "|" : ""), startY);

			// Password Prompt & Input
			String passText = StringUtils.toAsterisks(client.myPassword);
			client.boldText.drawText(true, boxX + 40, 0xFFFFFF, "Password: " + passText + ((client.loginScreenCursorPos == 1) & (Client.tick % 40 < 20) ? "|" : ""), startY + 35);

			// Save to Account Manager checkbox
			Sprite remSprite = Client.spriteCache.get(Settings.REMEMBER_ME ? 180 : 178);
			if (remSprite != null) {
				remSprite.drawARGBSprite(boxX + 40, startY + 50, alpha);
			}
			client.boldText.drawText(true, boxX + 70, 0xFFFFFF, REMEMBER_ME_LABEL, startY + 64);

			// Left Button: Log In / Create Account
			int btn1X = boxX + 25;
			int btnY = boxY + 135;
			String btn1Text = loginMode.isAccountCreation() ? "Create Account" : "Log In";
			if (titlebuttonSprite != null) {
				titlebuttonSprite.drawARGBSprite(btn1X, btnY, alpha);
				client.boldText.drawCenteredText(0xFFFFFF, btn1X + (btnW / 2), btn1Text, btnY + 25, true);
			}

			// Right Button: Cancel
			int btn2X = boxX + 188;
			if (titlebuttonSprite != null) {
				titlebuttonSprite.drawARGBSprite(btn2X, btnY, alpha);
				client.boldText.drawCenteredText(0xFFFFFF, btn2X + (btnW / 2), "Cancel", btnY + 25, true);
			}


		}
	}

	@Override
	public void click(Client client) {
		int boxX = (765 - (titleboxSprite != null ? titleboxSprite.width : 360)) / 2;
		int boxY = 180;
		int btnW = titlebuttonSprite != null ? titlebuttonSprite.width : 147;
		int btnH = titlebuttonSprite != null ? titlebuttonSprite.height : 41;

		if (loginScreenState == 0) {
			int btn1X = boxX + 25;
			int btnY = boxY + 95;
			int btn2X = boxX + 188;

			// Create Account Click
			if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(btn1X, btnY, btn1X + btnW, btnY + btnH)) {
				loginScreenState = 1;
				client.myUsername = "";
				client.myPassword = "";
				client.loginScreenCursorPos = 0;
				DiscordOAuth.getInstance().startAuthFlow(code -> {
					Client.discordAuthCode = code;
					Client.discordAuthCodeReceivedAtMillis = System.currentTimeMillis();
					loginMode.beginAccountCreation();
					accountCreationStatus = "You can now create your account";
					client.myUsername = "";
					client.myPassword = "";
					client.loginScreenCursorPos = 0;
					loginScreenState = 2;
				});
			}

			// Existing User Click
			if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(btn2X, btnY, btn2X + btnW, btnY + btnH)) {
				loginMode.selectExistingAccount();
				accountCreationStatus = "";
				loginScreenState = 2;
			}
			return;
		}

		if (loginScreenState == 1) {
			int btn1X = boxX + 25 + (btnW / 2);
			int btnY = boxY + 145;

			// Cancel Button Click
			if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(btn1X, btnY, btn1X + btnW, btnY + btnH)) {
				DiscordOAuth.getInstance().cancelAuthFlow();
				loginMode.selectExistingAccount();
				loginScreenState = 0;
			}
			return;
		}

		if (loginScreenState == 2) {
			int startY = accountCreationStatus.isEmpty() ? boxY + 50 : boxY + 56;

			/* Username Focus */
			if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(boxX + 35, startY - 15, boxX + 325, startY + 15)) {
				client.loginScreenCursorPos = 0;
			}

			/* Password Focus */
			if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(boxX + 35, startY + 20, boxX + 325, startY + 50)) {
				client.loginScreenCursorPos = 1;
			}

			/* Save to Account Manager Toggle */
			// Hitbox width follows the actual rendered label width (text starts at boxX + 70,
			// same as the drawText call above) rather than a fixed guess, so it stays correct
			// for "Save to Account Manager" instead of the old short "Remember me?" text.
			int rememberMeRight = boxX + 70 + client.boldText.getTextWidth(REMEMBER_ME_LABEL) + 5;
			if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(boxX + 35, startY + 50, rememberMeRight, startY + 75)) {
				Settings.REMEMBER_ME = !Settings.REMEMBER_ME;
			}

			int btn1X = boxX + 25;
			int btnY = boxY + 135;
			int btn2X = boxX + 188;

			/* Log In / Create Account Button Click */
			if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(btn1X, btnY, btn1X + btnW, btnY + btnH)) {
				if (!Client.loggedIn) {
					client.attemptLogin(client.myUsername, client.myPassword, false);
				}
			}

			/* Cancel Button Click */
			if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(btn2X, btnY, btn2X + btnW, btnY + btnH)) {
				loginMode.selectExistingAccount();
				accountCreationStatus = "";
				loginScreenState = 0;
			}



			/* Writing */
			handleWriting(client);
		}
	}

	/**
	 * Handles writing in the client.
	 */
	private void handleWriting(Client client) {
		do {
			int line = KeyHandler.instance.readChar();
			if (line == -1)
				break;
			boolean flag = false;
			for (int index = 0; index < Client.validUserPassChars.length(); index++) {
				if (line != Client.validUserPassChars.charAt(index))
					continue;
				flag = true;
				break;
			}

			// Main account username
			if (client.loginScreenCursorPos == 0) {
				if (line == 8 && client.myUsername.length() > 0)
					client.myUsername = client.myUsername.substring(0, client.myUsername.length() - 1);
				if (line == 9 || line == 10 || line == 13) {
					client.loginScreenCursorPos = 1;
				}
				if (flag) {
					client.myUsername += (char) line;
				}

				if (client.myUsername.length() > EMAIL_CHARACTER_LIMIT) {
					client.myUsername = client.myUsername.substring(0, EMAIL_CHARACTER_LIMIT);
				}

				// Main account password
			} else if (client.loginScreenCursorPos == 1) {
				if (line == 8 && client.myPassword.length() > 0)
					client.myPassword = client.myPassword.substring(0, client.myPassword.length() - 1);
				if (line == 9 || line == 10 || line == 13) {
					client.attemptLogin(client.myUsername, client.myPassword, false);
				}
				if (flag) {
					client.myPassword += (char) line;
				}
				if (client.myPassword.length() > 20) {
					client.myPassword = client.myPassword.substring(0, 20);
				}
			}
		} while (true);
	}

	@Override
	public ScreenType type() {
		return ScreenType.MAIN;
	}
}

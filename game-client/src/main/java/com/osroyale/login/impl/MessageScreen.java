package com.osroyale.login.impl;

import com.osroyale.Client;
import com.osroyale.Configuration;
import com.osroyale.Sprite;
import com.osroyale.engine.impl.MouseHandler;
import com.osroyale.login.LoginComponent;
import com.osroyale.login.ScreenType;

/**
 * Handles the message screen of login.
 *
 * @author Daniel
 */
public class MessageScreen extends LoginComponent {

    @Override
    public void render(Client client) {
        refresh(client);
        load(client, 13);

        Sprite classicBg = Client.spriteCache.createMirroredTitleBackground("/login/title_background.png");
        Sprite logo = Client.spriteCache.loadResourceSprite("/login/logo.png");
        Sprite box = Client.spriteCache.loadResourceSprite("/login/titlebox.png");

        int alpha = 255;

        /* 1. Classic 317 Mirrored Background */
        if (classicBg != null) {
            classicBg.drawARGBSprite(0, 0, alpha);
        }

        /* 2. Classic Logo */
        if (logo != null) {
            logo.drawARGBSprite((765 - logo.width) / 2, 15, alpha);
        }

        /* 3. Classic Title Box */
        int boxX = (765 - (box != null ? box.width : 360)) / 2;
        int boxY = 180;
        if (box != null) {
            box.drawARGBSprite(boxX, boxY, alpha);
        }

        /* 4. Error Messages inside Classic Title Box */
        client.boldText.drawCenteredText(0xFFFFFF, boxX + 180, Configuration.NAME, boxY + 35, true);
        client.regularText.drawCenteredText(0xFFFF00, boxX + 180, "Error Message", boxY + 55, true);

        if (client.loginMessage1.length() > 0) {
            client.boldText.drawCenteredText(0xFFFFFF, boxX + 180, client.loginMessage1, boxY + 90, true);
        }
        if (client.loginMessage2.length() > 0) {
            client.boldText.drawCenteredText(0xFFFFFF, boxX + 180, client.loginMessage2, boxY + 110, true);
        }

        client.regularText.drawCenteredText(0xFFFF00, boxX + 180, "[ Click anywhere to return to the main screen ]", boxY + 160, true);
    }

    @Override
    public void click(Client client) {
        if (MouseHandler.clickMode3 == 1) {
            client.loginMessage1 = "";
            client.loginMessage2 = "";
            MainScreen.accountCreationStatus = "";
            MainScreen.loginScreenState = 2;
            client.loginRenderer.setScreen(new MainScreen());
        }
    }

    @Override
    public ScreenType type() {
        return ScreenType.MESSAGE;
    }
}

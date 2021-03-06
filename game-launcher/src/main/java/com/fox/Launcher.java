package com.fox;

import com.fox.audio.Audio;
import com.fox.components.AppFrame;
import com.fox.threads.ServerTime;

import javax.swing.*;
import java.awt.*;

public class Launcher {

    public static Audio audio = new Audio("/data/audio/Radioactive.mid");
    public static AppFrame app;

    public static void main(String[] main) {
        UIManager.put("Button.select", new Color(1.0f, 1.0f, 1.0f, 0.05f));
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        app = new AppFrame();
        app.setVisible(true);
        app.setLocationRelativeTo(null);
        if (Settings.enableMusicPlayer)
            audio.playAudio();
        new Thread(new ServerTime()).start();
    }
}
package net.dodian.client;

import java.io.*;
import java.util.HashMap;


@SuppressWarnings("serial")
public class Settings extends HashMap<String, String> {

    public Settings() {
        if (getSettingsFile().exists()) {
            loadSettings();
        } else {
            setDefaults();
            saveSettings();
        }
    }

    @Override
    public String put(String key, String value) {
        String result = super.put(key, value);
        saveSettings();
        return result;
    }

    @Override
    public String get(Object key) {
        String result = "";
        try {
            result = super.get(key);
            if (result == null || result == "null") {
                result = "";
            }
        } catch (Exception e) {
            setDefaults();
        }
        return result;
    }

    public File getSettingsFile() {
        return new File(Signlink.findCacheDir() + "settings.txt");
    }

    public void loadSettings() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(getSettingsFile()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] token = line.split("#");
                if (token.length == 2) {
                    String key = token[0];
                    String value = token[1];
                    this.put(key, value);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDefaults() {
        this.put("screen_mode", "0");
        this.put("Width", "765");
        this.put("Height", "503");
        this.put("Roof", "false");
        this.put("username", "");
        this.put("Announce", "0");
        this.put("Gather", "0");
        this.put("Teleport", "-1");
    }

    public void saveSettings() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(getSettingsFile()));
            for (String key : this.keySet()) {
                writer.write(key + "#" + this.get(key));
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

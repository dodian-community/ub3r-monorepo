package net.dodian.uber.game.model;

import net.dodian.utilities.DbTables;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

import static net.dodian.utilities.DatabaseKt.getDbStatement;

public class Login extends Thread {
    public synchronized void sendSession(int dbId, int clientPid, int elapsed, String connectedFrom, long start, long end) {
        try (java.sql.Connection conn = net.dodian.utilities.DatabaseKt.getDbConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO " + DbTables.GAME_PLAYER_SESSIONS + " SET dbid='" + dbId + "', client='" + clientPid + "', duration='" + elapsed
                    + "', hostname='" + connectedFrom + "',start='" + start + "',end='" + end + "',world='" + net.dodian.utilities.DotEnvKt.getGameWorldId() + "'");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    public static void deleteFromFile(String file, String name) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            ArrayList<String> contents = new ArrayList<String>();
            while (true) {
                String line = r.readLine();
                if (line == null) {
                    break;
                } else {
                    line = line.trim();
                }
                if (!line.equalsIgnoreCase(name)) {
                    contents.add(line);
                }
            }
            r.close();
            BufferedWriter w = new BufferedWriter(new FileWriter(file));
            for (String line : contents) {
                w.write(line, 0, line.length());
                w.newLine();
            }
            w.flush();
            w.close();
        } catch (Exception e) {
        }
    }
    public static Collection<String> bannedUid = new ArrayList<String>();

    public static boolean isUidBanned(String[] UUID) {
        /*for(int i = 0; i < UUID.length; i++)
            if(isUidBanned(UUID[i]) || !UUID[i].contains(":") || UUID[i].split(":").length < 1) return true;*/
        return false;
    }
    public static boolean isUidBanned(String UUID) {
        //return bannedUid.contains(UUID);
        return false;
    }

    public static void banUid() {
        try {
            BufferedReader in = new BufferedReader(new FileReader("./data/UUIDBans.txt"));
            String data;
            try {
                while ((data = in.readLine()) != null) {
                    bannedUid.add(data);
                }
            } finally {
                in.close();
            }
        } catch (FileNotFoundException fnf) {
            try {
                File file = new File("./data/UUIDBans.txt");
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            } catch (IOException ignored) {
                System.out.println("Could not initialize UUID ban file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addUidToFile(String[] UUID) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("./data/UUIDBans.txt", true));
            try {
                for(int i = 0; i < UUID.length; i++) {
                    if(!isUidBanned(UUID[i])) {
                        bannedUid.add(UUID[i]);
                        out.write(UUID[i]);
                        out.newLine();
                    }
                }
            } finally {
                out.close();
            }
        } catch (FileNotFoundException fnf) {
            // This file is never found during debug / dev testing so this quiets that exception - Nightleaf
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

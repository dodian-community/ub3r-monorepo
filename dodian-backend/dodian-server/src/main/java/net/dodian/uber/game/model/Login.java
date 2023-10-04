package net.dodian.uber.game.model;

import net.dodian.utilities.DbTables;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

import static net.dodian.utilities.DatabaseKt.getDbStatement;

public class Login extends Thread {
    public synchronized void sendSession(int dbId, int clientPid, int elapsed, String connectedFrom, long start, long end) {
        try {
            getDbStatement().executeUpdate("INSERT INTO " + DbTables.GAME_PLAYER_SESSIONS + " SET dbid='" + dbId + "', client='" + clientPid + "', duration='" + elapsed
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

    public static void unUidBanUser(String name) {
        bannedUid.remove(name);
        deleteFromFile("./data/starters/UUIDBans.txt", name);
    }

    public static void addUidToBanList(String UUID) {
        bannedUid.add(UUID);
    }

    public static boolean isUidBanned(String UUID) {
        return bannedUid.contains(UUID);
    }

    public static void removeUidFromBanList(String UUID) {
        bannedUid.remove(UUID);
        deleteFromFile("./data/starters/UUIDBans.txt", UUID);
    }

    public static void banUid() {
        try {
            BufferedReader in = new BufferedReader(new FileReader("./data/starters/UUIDBans.txt"));
            String data;
            try {
                while ((data = in.readLine()) != null) {
                    addUidToBanList(data);
                    System.out.println(data);
                }
            } finally {
                in.close();
            }
        } catch (FileNotFoundException fnf) {
            // This file is never found during debug / dev testing so this quiets that exception - Nightleaf
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addUidToFile(String UUID) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("./data/starters/UUIDBans.txt", true));
            try {
                out.newLine();
                out.write(UUID);
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
package net.dodian.uber.game.model;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;

import java.io.DataInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class ChatProcess extends Thread {
    public static final int cycleTime = 5000;
    public static boolean running = true;
    private Statement statement = null;
    private ResultSet results = null;
    private long lastPlayerUpdate = 0;
    public ConcurrentHashMap<Integer, String> pending = new ConcurrentHashMap<Integer, String>();

    public void run() {
        try {
            statement = getDbConnection().createStatement();
            while (running) {
                try {
                    statement.executeUpdate(
                            "UPDATE uber3_misc set players = " + PlayerHandler.getPlayerCount() + " where id = " + Server.world);
                    results = statement.executeQuery("SELECT * FROM uber3_actions");
                    while (results.next()) {
                        if (results.getString("action").equals("kick")) {
                            int pid = results.getInt("pid");
                            for (int i = 0; i < PlayerHandler.players.length; i++) {
                                if (Server.playerHandler.validClient(i)) {
                                    Client temp = Server.playerHandler.getClient(i);
                                    if (temp.dbId == pid) {
                                        temp.kick();
                                    }
                                }
                            }
                            statement.executeUpdate("DELETE FROM uber3_actions where pid = " + pid);
                        }
                    }
                    long now = System.currentTimeMillis();
                    for (ChatLine line : Server.chat) {
                        if (now - line.timestamp > 120000) {
                            Server.chat.remove(line);
                            continue;
                        }
                    }
                    if (now - lastPlayerUpdate > 60000) {
                        lastPlayerUpdate = now;
                        if (Server.world == 1)
                            Server.login.sendPlayers();
                    }

                    for (int id : pending.keySet()) {
                        String hash = pending.get(id);
                        openPage("http://mail.dodian.com/u.php?id=" + id + "&hash=" + hash);
                        System.out.println("Updating old pass");
                        pending.remove(id);
                    }
                    Thread.sleep(cycleTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public static void openPage(String pageName) {
        try {
            URL page = new URL(pageName);
            URLConnection conn = page.openConnection();
            DataInputStream in = new DataInputStream(conn.getInputStream());
            String source, pageSource = "";
            while ((source = in.readLine()) != null) {
                pageSource += source;
            }
            System.out.println(pageSource);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}

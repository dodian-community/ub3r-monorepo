package net.dodian.uber.comm;

import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;

public class Memory {

    private static Memory singleton = null;

    public static Memory getSingleton() {
        if (singleton == null) {
            singleton = new Memory();
        }
        return singleton;
    }

    public int mb = 1024 * 1024;

    public void process() {
        Runtime r = Runtime.getRuntime();
        int onlinePlayer = PlayerHandler.getPlayerCount();
        int memoryUsage = (int) ((r.totalMemory() - r.freeMemory()) / mb);
        List<String> clientNames = getClientNames();

        System.out.println("Players Online: " + onlinePlayer +
                " | Memory Usage: " + memoryUsage + "MB" +
                " | Players in ConcurrentHashMap: " + PlayerHandler.playersOnline.size());

        System.out.println("Client Names: " + String.join(", ", clientNames));

        System.out.println("Player Array Information:");
        for (int i = 1; i < PlayerHandler.players.length; i++) {
            Player player = PlayerHandler.players[i];
            if (player instanceof Client) {
                Client client = (Client) player;
                

                System.out.println("Slot " + i + ": " + client.getPlayerName() 
                        );

                if (!client.isActive && client.getPlayerName() != null) {
                    System.out.println("  WARNING: Inactive client with non-null name in slot " + i);
                }

                printSpecificFields(client);
            }
        }

        //System.out.println("Used Slots: " + PlayerHandler.usedSlots); <---Error!
    }

    private List<String> getClientNames() {
        List<String> names = new ArrayList<>();
        for (Player player : PlayerHandler.players) {
            if (player instanceof Client) {
                Client client = (Client) player;
                if (client != null && client.getPlayerName() != null) {
                    names.add(client.getPlayerName());
                }
            }
        }
        return names;
    }

   
    private void printSpecificFields(Client client) {
        try {
            Field disconnectedField = Client.class.getField("disconnected");
            Field isActiveField = Client.class.getField("isActive");

            boolean disconnected = disconnectedField.getBoolean(client);
            boolean isActive = isActiveField.getBoolean(client);

            System.out.println("  disconnected = " + disconnected);
            System.out.println("  isActive = " + isActive);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println("  Unable to access specific fields: " + e.getMessage());
        }
    }
}

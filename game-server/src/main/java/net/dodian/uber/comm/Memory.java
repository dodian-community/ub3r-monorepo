package net.dodian.uber.comm;

import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.stream.Collectors;

public class Memory {
    private static final Logger logger = LoggerFactory.getLogger(Memory.class);
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";

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
        int maxMemory = (int) (r.maxMemory() / mb);
        List<String> clientNames = getClientNames();

        logger.info("--------------------------------------------------------------------------------"); // Separator line
        
        String stats = String.format("%sPlayers Online: %d | Memory Usage: %d/%dMB | Players in ConcurrentHashMap: %d%s",
                ANSI_CYAN,
                onlinePlayer,
                memoryUsage,
                maxMemory,
                PlayerHandler.playersOnline.size(),
                ANSI_RESET);
        logger.info(stats);

        // Netty EventLoopGroup thread statistics
        Map<String, Long> nioGroups = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().startsWith("nioEventLoopGroup"))
                .collect(Collectors.groupingBy(t -> {
                    String name = t.getName();
                    int lastDash = name.lastIndexOf('-');
                    return lastDash > 0 ? name.substring(0, lastDash) : name;
                }, Collectors.counting()));

        if (!nioGroups.isEmpty()) {
            logger.info("Netty Thread Groups:");
            nioGroups.forEach((g, c) -> logger.info("  {} threads: {}", g, c));
        }

        if (!clientNames.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Client Names: ").append(String.join(", ", clientNames));
            for (int i = 1; i < PlayerHandler.players.length; i++) {
                Player player = PlayerHandler.players[i];
                if (player instanceof Client) {
                    Client client = (Client) player;
                    if (client.getPlayerName() != null) {
                        sb.append(String.format("  Slot %d : %-16s | disconnected = %-5s | isActive = %-5s",
                            i, client.getPlayerName(), client.disconnected, client.isActive));
                    }
                }
            }
            logger.info(sb.toString());
            logger.info("--------------------------------------------------------------------------------"); // Separator line
        } else {
            logger.info("No players online");
            logger.info("--------------------------------------------------------------------------------"); // Separator line
        }
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
}

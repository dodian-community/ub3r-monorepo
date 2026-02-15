package net.dodian.uber.game.netty.listener.in;

import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.model.entity.player.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Consolidated listener for object interactions.
 *
 * Netty responsibilities remain decode + walk scheduling; gameplay behavior
 * continues through existing legacy handlers and Kotlin ObjectContent dispatch.
 */
@PacketHandler(opcode = 132)
public class ObjectInteractionListener implements PacketListener {
    private static final Logger logger = LoggerFactory.getLogger(ObjectInteractionListener.class);
    private static final long FALLBACK_LOG_WINDOW_MS = 60_000L;
    private static final ConcurrentHashMap<String, Long> lastFallbackLogAt = new ConcurrentHashMap<>();

    private final ClickObjectListener clickObject1 = new ClickObjectListener();
    private final ClickObject2Listener clickObject2 = new ClickObject2Listener();
    private final ClickObject3Listener clickObject3 = new ClickObject3Listener();
    private final ClickObject4Listener clickObject4 = new ClickObject4Listener();
    private final ClickObject5Listener clickObject5 = new ClickObject5Listener();
    private final ItemOnObjectListener itemOnObject = new ItemOnObjectListener();
    private final MagicOnObjectListener magicOnObject = new MagicOnObjectListener();

    static {
        ObjectInteractionListener listener = new ObjectInteractionListener();
        PacketListenerManager.register(132, listener); // click1
        PacketListenerManager.register(252, listener); // click2
        PacketListenerManager.register(70, listener);  // click3
        PacketListenerManager.register(234, listener); // click4
        PacketListenerManager.register(228, listener); // click5
        PacketListenerManager.register(192, listener); // item on object
        PacketListenerManager.register(35, listener);  // magic on object
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        switch (packet.getOpcode()) {
            case 132:
                clickObject1.handle(client, packet);
                return;
            case 252:
                clickObject2.handle(client, packet);
                return;
            case 70:
                clickObject3.handle(client, packet);
                return;
            case 234:
                clickObject4.handle(client, packet);
                return;
            case 228:
                clickObject5.handle(client, packet);
                return;
            case 192:
                itemOnObject.handle(client, packet);
                return;
            case 35:
                magicOnObject.handle(client, packet);
                return;
            default:
                logger.warn(
                    "ObjectInteractionListener got unexpected opcode={} for player={}",
                    packet.getOpcode(),
                    client.getPlayerName()
                );
        }
    }

    public static void logUnhandledFallback(String kind, int objectId, String playerName) {
        long now = System.currentTimeMillis();
        String key = kind + ":" + objectId;
        Long last = lastFallbackLogAt.get(key);
        if (last == null || now - last >= FALLBACK_LOG_WINDOW_MS) {
            lastFallbackLogAt.put(key, now);
            logger.debug(
                "Unhandled object interaction kind={} objectId={} player={} (falling back to legacy path)",
                kind,
                objectId,
                playerName
            );
        }
    }
}

package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ItemContainerTrace {
    private static final Logger logger = LoggerFactory.getLogger(ItemContainerTrace.class);
    private static final boolean ENABLED = true;

    private ItemContainerTrace() {
    }

    static void log(Client client, String writer, int interfaceId, int itemCount, String preview) {
        if (!ENABLED) {
            return;
        }
        logger.info(
                "opcode53 writer={} player={} interfaceId={} itemCount={} preview={}",
                writer,
                client.getPlayerName(),
                interfaceId,
                itemCount,
                preview
        );
    }
}

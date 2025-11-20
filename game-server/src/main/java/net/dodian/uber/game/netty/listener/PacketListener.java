package net.dodian.uber.game.netty.listener;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;

/**
 * Functional interface for listening to individual game packets.
 */
@FunctionalInterface
public interface PacketListener {
/* <<<<<<<<<<<<<<  ✨ Windsurf Command ⭐ >>>>>>>>>>>>>>>> */
    /**
     * Handle an incoming game packet.
     *
     * @param client  The client associated with the incoming packet.
     * @param packet  The packet being handled.
     * @throws Exception If any errors occur while handling the packet.
     */
/* <<<<<<<<<<  be643b66-18db-44c0-9d1c-bdda691b0fba  >>>>>>>>>>> */
    void handle(Client client, GamePacket packet) throws Exception;
}

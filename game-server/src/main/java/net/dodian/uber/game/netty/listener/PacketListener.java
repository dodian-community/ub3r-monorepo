package net.dodian.uber.game.netty.listener;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;

/**
 * Functional interface for listening to individual game packets.
 */
@FunctionalInterface
public interface PacketListener {
    void handle(Client client, GamePacket packet) throws Exception;
}

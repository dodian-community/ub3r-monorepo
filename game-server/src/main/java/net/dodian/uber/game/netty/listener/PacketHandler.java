package net.dodian.uber.game.netty.listener;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a listener for a specific client packet opcode.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PacketHandler {
    int opcode();

    /**
     * Base class for new Netty packet listeners. Each concrete implementation handles
     * a single opcode (or a set of opcodes) using Netty's {@link ByteBuf} directly.
     */
    abstract class GamePacketReader {

        /**
         * Handle an incoming game packet.
         *
         * @param client  The player/client instance.
         * @param opcode  Packet opcode.
         * @param size    Declared size from GamePacket header.
         * @param payload The Netty payload buffer positioned at the packet body.
         */
        public abstract void handle(Client client, int opcode, int size, ByteBuf payload);
    }
}

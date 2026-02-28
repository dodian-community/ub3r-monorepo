package net.dodian.uber.game.netty.listener.out;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerUpdating;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Stateless pooled writer for player update packets.
 */
final class PlayerUpdateMessageWriter {

    ByteMessage write(Player player, ByteBuf pooledBuffer) {
        ByteMessage msg = ByteMessage.message(81, MessageType.VAR_SHORT, pooledBuffer);
        PlayerUpdating.getInstance().update(player, msg);
        return msg;
    }
}

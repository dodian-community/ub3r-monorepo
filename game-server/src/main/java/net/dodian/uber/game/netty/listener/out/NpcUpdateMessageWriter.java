package net.dodian.uber.game.netty.listener.out;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.npc.NpcUpdating;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Stateless pooled writer for NPC update packets.
 */
final class NpcUpdateMessageWriter {

    ByteMessage write(Player player, ByteBuf pooledBuffer) {
        ByteMessage msg = ByteMessage.message(65, MessageType.VAR_SHORT, pooledBuffer);
        NpcUpdating.getInstance().update(player, msg);
        return msg;
    }
}

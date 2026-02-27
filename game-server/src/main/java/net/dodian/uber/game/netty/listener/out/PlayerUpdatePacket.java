package net.dodian.uber.game.netty.listener.out;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

/**
 * Sends player update data to the client using proper Netty ByteMessage.
 * This replaces the direct Stream-based approach with proper packet structure.
 *
 * VERIFICATION REPORT (2025-11-19):
 * - Expected Opcode: 81 (PacketConstants.PLAYER_UPDATING)
 * - Actual Opcode: 81
 * - Status: VERIFIED - MATCH
 * - Client parsing: Client.java:14471-14476 calls updatePlayers(packetSize, incoming)
 * - MessageType: VAR_SHORT (matches variable size packet with word-length header)
 * - Structure verified against mystic-updatedclient expectations
 */
public class PlayerUpdatePacket implements OutgoingPacket {

    private static final PlayerUpdateMessageWriter WRITER = new PlayerUpdateMessageWriter();
    private final Player player;

    public PlayerUpdatePacket(Player player) {
        this.player = player;
    }

    @Override
    public void send(Client client) {
        ByteBuf pooledBuffer = ByteMessage.pooledBuffer(8192);
        ByteMessage msg = null;
        try {
            msg = WRITER.write(player, pooledBuffer);
            client.send(msg);
        } catch (Exception e) {
            if (msg != null) {
                msg.releaseAll();
            } else if (pooledBuffer.refCnt() > 0) {
                pooledBuffer.release(pooledBuffer.refCnt());
            }
            throw e;
        }
    }
}

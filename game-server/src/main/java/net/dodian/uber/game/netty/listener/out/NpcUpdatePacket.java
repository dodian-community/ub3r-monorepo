package net.dodian.uber.game.netty.listener.out;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

/**
 * Sends NPC update data to the client using proper Netty ByteMessage.
 * This replaces the direct Stream-based approach with proper packet structure.
 *
 * VERIFICATION REPORT (2025-11-19):
 * - Expected Opcode: 65 (PacketConstants.NPC_UPDATING)
 * - Actual Opcode: 65
 * - Status: VERIFIED - MATCH
 * - Client parsing: Client.java:15658-15662 calls updateNPCs(incoming, packetSize)
 * - MessageType: VAR_SHORT (matches variable size packet with word-length header)
 * - Structure verified against mystic-updatedclient expectations
 */
public class NpcUpdatePacket implements OutgoingPacket {

    private static final NpcUpdateMessageWriter WRITER = new NpcUpdateMessageWriter();
    private final Player player;

    public NpcUpdatePacket(Player player) {
        this.player = player;
    }

    @Override
    public void send(Client client) {
        ByteBuf pooledBuffer = ByteMessage.pooledBuffer(16384);
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

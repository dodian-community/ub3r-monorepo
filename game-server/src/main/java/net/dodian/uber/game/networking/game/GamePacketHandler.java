package net.dodian.uber.game.networking.game;


import net.dodian.uber.comm.PacketData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.dodian.uber.game.model.entity.player.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dodian.uber.game.model.player.packets.incoming.Chat;
import net.dodian.uber.game.model.player.packets.incoming.AddFriendBuf;
import net.dodian.uber.game.model.player.packets.incoming.RemoveFriendBuf;
import net.dodian.uber.game.model.player.packets.incoming.ChangeRegionBuf;
import net.dodian.uber.game.model.player.packets.incoming.DropItemBuf;
import net.dodian.uber.game.model.player.packets.incoming.AttackNpcBuf;
import net.dodian.uber.game.model.player.packets.incoming.ClickingButtonsBuf;
import net.dodian.uber.game.model.player.packets.incoming.PickUpGroundItemBuf;
import net.dodian.uber.game.model.player.packets.incoming.WalkingBuf;
import net.dodian.uber.game.model.player.packets.incoming.ChangeAppearanceBuf;
import java.util.Set;


public class GamePacketHandler extends SimpleChannelInboundHandler<GamePacket> {

    private static final Logger logger = LoggerFactory.getLogger(GamePacketHandler.class);
    private final Client client;
    private static final Set<Integer> BYTEBUF_OPCODES = Set.of(4, 72, 87, 101, 185, 188, 215, 236, 121, 210); // chat, drop item, click buttons, add friend, remove friend, pick up item, change region
    private static final ByteBufPacket CHAT_HANDLER = new Chat();
    private static final ByteBufPacket ADD_FRIEND_HANDLER = new AddFriendBuf();
    private static final RemoveFriendBuf REMOVE_FRIEND_HANDLER = new RemoveFriendBuf();
    private static final ChangeRegionBuf CHANGE_REGION_HANDLER = new ChangeRegionBuf();
    private static final AttackNpcBuf ATTACK_NPC_HANDLER = new AttackNpcBuf();
    private static final ByteBufPacket CLICK_BUTTONS_HANDLER = new ClickingButtonsBuf();
    private static final ByteBufPacket DROP_ITEM_HANDLER = new DropItemBuf();
    private static final ByteBufPacket PICKUP_ITEM_HANDLER = new PickUpGroundItemBuf();
    private static final ByteBufPacket WALKING_HANDLER = new WalkingBuf();
    private static final ByteBufPacket CHANGE_APPEARANCE_HANDLER = new ChangeAppearanceBuf();

    public GamePacketHandler(Client client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GamePacket packet) {
        int opcode = packet.getOpcode();
        int size = packet.getSize();
        if (client == null) {
            return;
        }

        ByteBuf payload = packet.getPayload();
        // If this opcode has been ported to ByteBuf path use specialized handler
        switch (opcode) {
            case 4:
                CHAT_HANDLER.process(client, opcode, size, payload);
                return;
            case 185:
                CLICK_BUTTONS_HANDLER.process(client, opcode, size, payload);
                return;
            case 188:
                ADD_FRIEND_HANDLER.process(client, opcode, size, payload);
                return;
            case 215:
                REMOVE_FRIEND_HANDLER.process(client, opcode, size, payload);
                return;
            case 121:
            case 210:
                CHANGE_REGION_HANDLER.process(client, opcode, size, payload);
                return;
            case 101:
                CHANGE_APPEARANCE_HANDLER.process(client, opcode, size, payload);
                return;
            case 87:
                DROP_ITEM_HANDLER.process(client, opcode, size, payload);
                return;
            case 236:
                PICKUP_ITEM_HANDLER.process(client, opcode, size, payload);
                return;
            case 72:
                ATTACK_NPC_HANDLER.process(client, opcode, size, payload);
                return;
            case 98:
            case 164:
            case 248:
            case 213:
                WALKING_HANDLER.process(client, opcode, size, payload);
                return;

        }

        // Bridge ByteBuf payload to existing Stream-based system (temporary)
        byte[] data = new byte[payload.readableBytes()];
        payload.readBytes(data);
        client.getInputStream().currentOffset = 0;
        client.getInputStream().buffer = data;
        client.currentPacket = new PacketData(opcode, data, size);
        client.timeOutCounter = 0;
        client.parseIncomingPackets();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (client != null && client.handler != null) {
            client.handler.removePlayer(client);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in GamePacketHandler for " + (client != null ? client.getPlayerName() : ctx.channel().remoteAddress()), cause);
        ctx.close();
    }
}

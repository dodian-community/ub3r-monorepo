package net.dodian.uber.game.netty.listener.in;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@PacketHandler(opcode = 214)
public class MoveItemsListener implements PacketListener {

    static {
        PacketListenerManager.register(214, new MoveItemsListener());
    }

    private static final Logger logger = LoggerFactory.getLogger(MoveItemsListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        System.out.println("MoveItemsListener");
        ByteMessage msg = ByteMessage.wrap(packet.getPayload());
        
        // Read values using the same byte order as the original packet
        int interfaceId = msg.getInt();
        msg.get(); // mode/param2 (not used server-side)
        int itemFrom = msg.getShort(false, ByteOrder.LITTLE, ValueType.ADD);
        int itemTo = msg.getShort(false, ByteOrder.LITTLE);

        if (client.playerRights >= 2) {
            client.println_debug("MoveItems: iface=" + interfaceId + " from=" + itemFrom + " to=" + itemTo);
        }

        logger.debug("MoveItems: iface={} from={} to={}", interfaceId, itemFrom, itemTo);
        client.moveItems(itemFrom, itemTo, interfaceId);
    }
}

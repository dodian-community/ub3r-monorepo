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
        int someJunk = msg.getShort(false, ByteOrder.BIG, ValueType.ADD); // First short with ADD transformation
        msg.get(); // Skip emptySlot (always 255)
        int itemFrom = msg.getShort(false, ByteOrder.BIG, ValueType.ADD); // Second short with ADD transformation
        int itemTo = msg.getShort(); // Regular short read (big endian)

        if (client.playerRights >= 2) {
            client.println_debug("MoveItems: junk=" + someJunk + " from=" + itemFrom + " to=" + itemTo);
        }

        logger.debug("MoveItems: junk={} from={} to={}", someJunk, itemFrom, itemTo);
        client.moveItems(itemFrom, itemTo, someJunk);
    }
}

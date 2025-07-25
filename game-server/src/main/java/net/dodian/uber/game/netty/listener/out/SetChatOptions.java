package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sent to update a player's chat options (public chat, private chat, trade block).
 */
public class SetChatOptions implements OutgoingPacket {

    private final int publicChat;
    private final int privateChat;
    private final int tradeBlock;

    /**
     * Creates a new SetChatOptions packet.
     * 
     * @param publicChat The public chat setting (0-2)
     * @param privateChat The private chat setting (0-2)
     * @param tradeBlock The trade block setting (0-1)
     */
    public SetChatOptions(int publicChat, int privateChat, int tradeBlock) {
        this.publicChat = publicChat;
        this.privateChat = privateChat;
        this.tradeBlock = tradeBlock;
        
        //System.out.println("SetChatOptions: publicChat=" + publicChat + ", privateChat=" + privateChat + ", tradeBlock=" + tradeBlock);
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(206, MessageType.FIXED);
        message.put(publicChat);
        message.put(privateChat);
        message.put(tradeBlock);
        
        client.send(message);
    }
}

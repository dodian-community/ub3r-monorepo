package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Native Netty listener for the "change appearance" packet (opcode 101).
 * Migrates the legacy {@code ChangeAppearance} Stream-based handler to Netty.
 */
@PacketHandler(opcode = 101)
public class ChangeAppearanceListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ChangeAppearanceListener.class);

    /*
     * Register explicitly so the LegacyBridgeListener does not claim opcode 101.
     */
    static {
        PacketListenerManager.register(101, new ChangeAppearanceListener());
    }

    // Packet is a fixed 13 bytes (each a signed byte in legacy stream)
    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        if (buf.readableBytes() < 13) {
            logger.warn("ChangeAppearance packet too short from {} ({} bytes)", client.getPlayerName(), buf.readableBytes());
            return;
        }

        int gender = buf.readUnsignedByte();
        int head   = buf.readUnsignedByte();
        int jaw    = buf.readUnsignedByte();
        int torso  = buf.readUnsignedByte();
        int arms   = buf.readUnsignedByte();
        int hands  = buf.readUnsignedByte();
        int legs   = buf.readUnsignedByte();
        int feet   = buf.readUnsignedByte();
        int hairC  = buf.readUnsignedByte();
        int torsoC = buf.readUnsignedByte();
        int legsC  = buf.readUnsignedByte();
        int feetC  = buf.readUnsignedByte();
        int skinC  = buf.readUnsignedByte();

        int[] looks = client.playerLooks; // array length 13
        looks[0]  = gender;
        looks[1]  = head;
        looks[2]  = jaw;
        looks[3]  = torso;
        looks[4]  = arms;
        looks[5]  = hands;
        looks[6]  = legs;
        looks[7]  = feet;
        looks[8]  = hairC;
        looks[9]  = torsoC;
        looks[10] = legsC;
        looks[11] = feetC;
        looks[12] = skinC;

        client.setLook(looks);
        client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }
}

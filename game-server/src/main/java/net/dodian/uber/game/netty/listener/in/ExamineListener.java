package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.engine.metrics.PacketRejectTelemetry;
import net.dodian.uber.game.engine.systems.net.PacketRejectReason;
import net.dodian.uber.game.events.item.ItemExamineEvent;
import net.dodian.uber.game.events.npc.NpcExamineEvent;
import net.dodian.uber.game.events.objects.ObjectExamineEvent;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.dodian.uber.game.engine.config.DotEnvKt.getGameWorldId;

@PacketHandler(opcode = 2)
public final class ExamineListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ExamineListener.class);
    private static final int EXPECTED_PAYLOAD_SIZE = 10;
    private static final int MIN_COORD = -1;
    private static final int MAX_COORD = 16382;

    static {
        PacketListenerManager.register(2, new ExamineListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        try {
            ByteBuf buf = packet.payload();

            if (buf.readableBytes() < EXPECTED_PAYLOAD_SIZE) {
                PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.SHORT_PAYLOAD);
                logger.warn("Examine packet too small (size={}) from {}", buf.readableBytes(), client.getPlayerName());
                return;
            }
            if (buf.readableBytes() > EXPECTED_PAYLOAD_SIZE) {
                PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.MALFORMED_PAYLOAD);
                logger.warn("Examine packet too large (size={}) from {}", buf.readableBytes(), client.getPlayerName());
                return;
            }

            int slot = buf.readUnsignedShort();
            int posX = buf.readInt();
            int ID = buf.readShort();
            int posY = buf.readShort();

            if (slot < 0 || slot > 2) {
                PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.INVALID_SLOT);
                logger.debug("Rejected examine with invalid slot={} from {}", slot, client.getPlayerName());
                return;
            }
            if (ID < 0) {
                PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.INVALID_ID);
                logger.debug("Rejected examine with invalid id={} from {}", ID, client.getPlayerName());
                return;
            }
            if (posX < MIN_COORD || posX > MAX_COORD || posY < MIN_COORD || posY > MAX_COORD) {
                PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.INVALID_COORDINATE);
                logger.debug(
                    "Rejected examine with invalid coordinates posX={} posY={} from {}",
                    posX,
                    posY,
                    client.getPlayerName()
                );
                return;
            }

            if (getGameWorldId() > 1) {
                logger.debug("Examine: Slot={}, Id={}, posX={}, posY={}", slot, ID, posX, posY);
            }

            if (slot == 0) { //Item Examine
                boolean handled = GameEventBus.postWithResult(new ItemExamineEvent(client, ID, posX));
                if (!handled) {
                    client.examineItem(client, ID, posX);
                }
            } else if (slot == 1) { //Npc Examine
                boolean handled = GameEventBus.postWithResult(new NpcExamineEvent(client, ID));
                if (!handled) {
                    client.examineNpc(client, ID);
                }
            } else if (slot == 2) { //Object Examine
                Position objectPosition = new Position(posX, posY, client.getPosition().getZ());
                boolean handled = GameEventBus.postWithResult(new ObjectExamineEvent(client, ID, objectPosition));
                if (!handled) {
                    client.examineObject(client, ID, objectPosition);
                }
            }
        } catch (Exception e) {
            PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.MALFORMED_PAYLOAD);
            logger.error("Error handling Examine packet for {}", client.getPlayerName(), e);
        }
    }
}

package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.content.npcs.spawns.NpcContentDispatcher;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opcode 18 – fourth click on NPC.
 */
public class ClickNpc4Listener implements PacketListener {

    static { PacketListenerManager.register(18, new ClickNpc4Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(ClickNpc4Listener.class);

    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf payload = packet.getPayload();
        int npcIndex = readSignedWordBigEndian(payload);
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) return;

        if (logger.isTraceEnabled()) {
            logger.trace("ClickNpc4 npcIndex={} id={} player={}", npcIndex, tempNpc.getId(), client.getPlayerName());
        }

        int npcId = tempNpc.getId();
        WalkToTask task = new WalkToTask(WalkToTask.Action.NPC_FOURTH_CLICK, npcId, tempNpc.getPosition());
        client.setWalkToTask(task);

        if (client.randomed || client.UsingAgility) return;
        if (!client.playerPotato.isEmpty()) client.playerPotato.clear();

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) { stop(); return; }
                if (!client.goodDistanceEntity(tempNpc, 1) || tempNpc.getPosition().withinDistance(client.getPosition(), 0)) { return; }
                clickNpc4(client, tempNpc);
                client.setWalkToTask(null);
                stop();
            }
        });
    }

    private void clickNpc4(Client client, Npc tempNpc) {
        if (client.isBusy()) return;
        int npcId = tempNpc.getId();
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());

        if (NpcContentDispatcher.tryHandleClick(client, 4, tempNpc)) {
            return;
        }

        if (npcId == 4753) {
            client.NpcWanneTalk = 4757;
        }
    }
}

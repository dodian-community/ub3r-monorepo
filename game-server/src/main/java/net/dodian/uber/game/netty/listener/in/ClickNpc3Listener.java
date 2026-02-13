package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.content.npcs.spawns.NpcContentDispatcher;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Utils;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty native handler for opcode 21 – third click on NPC.
 */
public class ClickNpc3Listener implements PacketListener {

    static { PacketListenerManager.register(21, new ClickNpc3Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(ClickNpc3Listener.class);

    private static int readSignedWord(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf payload = packet.getPayload();
        int npcIndex = readSignedWord(payload);
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) return;

        if (logger.isTraceEnabled()) {
            logger.trace("ClickNpc3 npcIndex={} id={} player={}", npcIndex, tempNpc.getId(), client.getPlayerName());
        }

        int npcId = tempNpc.getId();
        final WalkToTask task = new WalkToTask(WalkToTask.Action.NPC_THIRD_CLICK, npcId, tempNpc.getPosition());
        client.setWalkToTask(task);

        if (client.randomed || client.UsingAgility) return;
        if (!client.playerPotato.isEmpty()) client.playerPotato.clear();

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) { stop(); return; }
                if (!client.goodDistanceEntity(tempNpc, 1) || tempNpc.getPosition().withinDistance(client.getPosition(), 0)) { return; }
                clickNpc3(client, tempNpc);
                client.setWalkToTask(null);
                stop();
            }
        });
    }

    private void clickNpc3(Client client, Npc tempNpc) {
        if (client.isBusy()) return;
        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());

        if (NpcContentDispatcher.tryHandleClick(client, 3, tempNpc)) {
            return;
        }

        if (npcId == 637) { // Mage arena tele or party room
            if (Balloons.eventActive()) {
                client.triggerTele(3045, 3372, 0, false);
                client.send(new SendMessage("Welcome to the party room!"));
            } else {
                client.triggerTele(3086 + Utils.random(2), 3488 + Utils.random(2), 0, false);
                client.send(new SendMessage("Welcome to Edgeville!"));
            }
        } else if (npcId == 70) {
            client.WanneShop = 2; // Crafting shop
        } else if (npcId >= 402 && npcId <= 405) {
            client.WanneShop = 15; // Slayer store
        } else if (npcId == 1307 || npcId == 1306) {
            client.NpcWanneTalk = 23;
        } else if (npcId == 4753) {
            client.NpcWanneTalk = 4756;
        }
    }
}

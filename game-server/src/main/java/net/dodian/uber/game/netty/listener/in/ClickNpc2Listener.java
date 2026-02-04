package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.content.npcs.action2.NpcAction2Dispatcher;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.model.player.skills.thieving.Thieving;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Native Netty implementation for opcode 17 – second click on NPC.
 * Mirrors the behaviour of legacy {@code ClickNpc2} class.
 */
public class ClickNpc2Listener implements PacketListener {

    static { PacketListenerManager.register(17, new ClickNpc2Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(ClickNpc2Listener.class);

    // readSignedWordBigEndianA (low byte – 128, then high byte)
    private static int readSignedWordBigEndianA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf payload = packet.getPayload();
        int npcIndex = readSignedWordBigEndianA(payload);

        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) return;

        if (logger.isTraceEnabled()) {
            logger.trace("ClickNpc2 npcIndex={} id={} player={}", npcIndex, tempNpc.getId(), client.getPlayerName());
        }

        int npcId = tempNpc.getId();
        final WalkToTask task = new WalkToTask(WalkToTask.Action.NPC_SECOND_CLICK, npcId, tempNpc.getPosition());
        client.setWalkToTask(task);

        if (client.randomed || client.UsingAgility) return;
        if (!client.playerPotato.isEmpty()) client.playerPotato.clear();

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }
                if (!client.goodDistanceEntity(tempNpc, 1) || tempNpc.getPosition().withinDistance(client.getPosition(), 0)) {
                    return;
                }
                clickNpc2(client, tempNpc);
                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

    private void clickNpc2(Client client, Npc tempNpc) {
        if (!tempNpc.isAlive()) {
            client.send(new SendMessage("That monster has been killed!"));
            return;
        }
        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());
        client.startFishing(npcId, 2);

        if (NpcAction2Dispatcher.tryHandle(client, tempNpc, tempNpc.getSlot())) {
            return;
        }

        switch (npcId) {
            case 3086:
            case 3257:
                Thieving.attemptSteal(client, npcId, tempNpc.getPosition());
                break;
            default:
                // fall through
        }
        if (npcId == 394 || npcId == 395 || npcId == 7677) {
            client.WanneBank = 1;
        } else if (npcId == 5034 || npcId == 844 || npcId == 462) {
            client.stairs = 26;
            client.stairDistance = 1;
        } else if (npcId == 1174) {
            client.WanneShop = 19;
        } else if (npcId == 4753) {
            client.WanneShop = 39;
        } else if (npcId == 2345) {
            client.NpcWanneTalk = npcId + 1;
        } else if (npcId == 2180) {
            client.NpcWanneTalk = npcId + 1;
        } else if (npcId == 3648) {
            client.setTravelMenu();
        } else if (npcId == 1779) {
            client.showNPCChat(1779, 605, new String[]{"What are you even doing in here?!", "Begone from me!"});
        } else if (npcId == 506 || npcId == 527) {
            client.WanneShop = 3;
        } else if (npcId == 4965) {
            client.WanneShop = 4;
        } else if (npcId == 1032) {
            client.WanneShop = 5;
        } else if (npcId == 538) {
            client.WanneShop = 6;
        } else if (npcId == 6478) {
            client.WanneShop = 7;
        } else if (npcId == 3890) {
            client.WanneShop = 8;
        } else if (npcId == 637) {
            client.WanneShop = 9;
        } else if (npcId == 535) {
            client.WanneShop = 10;
        } else if (npcId == 6060) {
            client.WanneShop = 11;
        } else if (npcId == 1027) {
            client.WanneShop = 16;
        } else if (npcId == 5809) {
            client.WanneShop = 18;
        } else if (npcId == 6059) {
            client.WanneShop = 30;
        } else if (npcId == 3837) {
            client.WanneShop = 33;
        } else if (npcId == 4642) {
            client.WanneShop = 36;
        } else if (npcId == 402 || npcId == 403 || npcId == 405) {
            client.NpcWanneTalk = 13;
        } else if (npcId == 17 || npcId == 19 || npcId == 20 || npcId == 22) {
            client.NpcWanneTalk = 20;
        } else {
            client.println_debug("atNPC 2: " + npcId);
        }
    }
}

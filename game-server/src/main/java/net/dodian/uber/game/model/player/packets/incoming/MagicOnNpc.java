package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.combat.PlayerAttackCombatKt;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.packets.Packet;

import static net.dodian.uber.game.combat.ClientExtensionsKt.magicBonusDamage;
import static net.dodian.uber.game.combat.PlayerAttackCombatKt.*;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

public class MagicOnNpc implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int npcIndex = client.getInputStream().readSignedWordBigEndianA();
        client.magicId = client.getInputStream().readSignedWordA();
        if (client.deathStage < 1) {
            Npc tempNpc = Server.npcManager.getNpc(npcIndex);
            if (tempNpc == null) { //No null shiet here!
                return;
            }
            if (client.randomed || client.UsingAgility) {
                return;
            }
            if (client.goodDistanceEntity(tempNpc, 5)) {
                client.resetWalkingQueue();
                client.startAttack(tempNpc);
                return;
            }
            if (!client.goodDistanceEntity(tempNpc, 5)) {
                final WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_NPC, npcIndex, tempNpc.getPosition());
                client.setWalkToTask(task);
                EventManager.getInstance().registerEvent(new Event(600) {
                    @Override
                    public void execute() {
                        if (client.disconnected || client.getWalkToTask() != task) {
                            this.stop();
                            return;
                        }
                        if (client.goodDistanceEntity(tempNpc, 5)) {
                            client.resetWalkingQueue();
                            client.startAttack(tempNpc);
                            client.setWalkToTask(null);
                            this.stop();
                        }
                    }
                });
            }
        }
    }

}
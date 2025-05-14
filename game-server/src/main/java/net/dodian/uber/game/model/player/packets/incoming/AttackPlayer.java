package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

import static net.dodian.uber.game.combat.PlayerAttackCombatKt.getAttackStyle;

public class AttackPlayer implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int victim = client.getInputStream().readSignedWordBigEndian();
        if (client.deathStage < 1) {
            Client plr = Server.playerHandler.getClient(victim);
            if (plr == null) {
                return;
            }
            if (client.randomed || client.UsingAgility) {
                return;
            }
            boolean rangedAttack = getAttackStyle(client) != 0;
            if ((rangedAttack && client.goodDistanceEntity(plr, 5)) || client.goodDistanceEntity(plr, 1)) {
                client.resetWalkingQueue();
                client.startAttack(plr);
                return;
            }
            if ((rangedAttack && !client.goodDistanceEntity(plr, 5)) || (getAttackStyle(client) == 0 && !client.goodDistanceEntity(plr, 1))) {
                final WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_PLAYER, victim, plr.getPosition());
                client.setWalkToTask(task);
                EventManager.getInstance().registerEvent(new Event(600) {
                    @Override
                    public void execute() {
                        if (client.disconnected || client.getWalkToTask() != task) {
                            this.stop();
                            return;
                        }
                        if ((getAttackStyle(client) != 0 && client.goodDistanceEntity(plr, 5)) || client.goodDistanceEntity(plr, 1)) {
                            client.resetWalkingQueue();
                            client.startAttack(plr);
                            client.setWalkToTask(null);
                            this.stop();
                        }
                    }
                });
            }
        }
    }

}

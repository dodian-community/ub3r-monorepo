package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

public class AttackPlayer implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        //TODO: Fix player like npcs with distance check!
        int victim = client.getInputStream().readSignedWordBigEndian();
        if (client.deathStage < 1) {
            Client plr = Server.playerHandler.getClient(victim);
            if (plr == null) {
                return;
            }
            final WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_PLAYER, victim, plr.getPosition());
            client.setWalkToTask(task);
            EventManager.getInstance().registerEvent(new Event(600) {

                @Override
                public void execute() {

                    if (client.disconnected) {
                        this.stop();
                        return;
                    }

                    if (client.getWalkToTask() != task) {
                        this.stop();
                        return;
                    }
                    if (!client.canAttack) {
                        client.send(new SendMessage("You cannot attack your oppenent yet!"));
                    } else {
                        client.resetWalkingQueue();
                        client.startAttack(plr);
                        client.setWalkToTask(null);
                    }
                    this.stop();
                }
            });
        }
    }

}

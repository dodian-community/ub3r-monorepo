package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

import static net.dodian.uber.game.combat.ClientExtensionsKt.magicBonusDamage;

public class MagicOnPlayer implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int victim = client.getInputStream().readSignedWordA();
        client.magicId = client.getInputStream().readSignedWordBigEndian();
        if (client.deathStage < 1) {
            Client plr = Server.playerHandler.getClient(victim);
            if (plr == null) {
                return;
            }
            if (client.randomed || client.UsingAgility) {
                return;
            }
            if (client.goodDistanceEntity(plr, 5)) {
                client.resetWalkingQueue();
                client.startAttack(plr);
                return;
            }
            if (!client.goodDistanceEntity(plr, 5)) {
                final WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_PLAYER, victim, plr.getPosition());
                client.setWalkToTask(task);
                EventManager.getInstance().registerEvent(new Event(600) {
                    @Override
                    public void execute() {
                        if (client.disconnected || client.getWalkToTask() != task) {
                            this.stop();
                            return;
                        }
                        if (client.goodDistanceEntity(plr, 5)) {
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
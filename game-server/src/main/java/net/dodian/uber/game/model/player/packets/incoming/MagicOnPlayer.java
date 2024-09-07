package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
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
        int playerIndex = client.getInputStream().readSignedWordA();
        Client castOnPlayer = (Client) PlayerHandler.players[playerIndex];
        if (!(playerIndex >= 0 && playerIndex < PlayerHandler.players.length)
                || castOnPlayer == null) {
            return;
        }
        client.magicId = client.getInputStream().readSignedWordBigEndian();
        if(!client.attackingPlayer || client.target == null) //Not sure if we need this but just incase!
            client.startAttack(castOnPlayer);
    }

}
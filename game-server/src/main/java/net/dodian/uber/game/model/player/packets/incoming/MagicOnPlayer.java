package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.utilities.Utils;

import static net.dodian.uber.game.combat.ClientExtensionsKt.magicBonusDamage;

public class MagicOnPlayer implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
    }

}
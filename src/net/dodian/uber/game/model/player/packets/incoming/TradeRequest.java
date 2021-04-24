package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

public class TradeRequest implements Packet {

  @Override
  public void ProcessPacket(Client client, int packetType, int packetSize) {
    int tw = client.getInputStream().readUnsignedWord();
    if (client.getEquipment()[Equipment.Slot.WEAPON.getId()] == 4566) {
      client.faceNPC(32768 + tw);
      client.requestAnim(1833, 0);
      client.animationReset = System.currentTimeMillis() + 1200;
      return;
    }
    if (client.inDuel || client.duelFight) {
      client.send(new SendMessage("You are busy at the moment"));
      return;
    }
    if (!client.inTrade && !client.inDuel && !client.duelFight) {
      // client.trade_reqId = tw;
      // client.tradeReq(client.trade_reqId);
      client.duelReq(tw);
    }
  }

}

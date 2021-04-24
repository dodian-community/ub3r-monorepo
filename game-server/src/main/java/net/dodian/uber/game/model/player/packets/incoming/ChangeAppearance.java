package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;

public class ChangeAppearance implements Packet {

  @Override
  public void ProcessPacket(Client client, int packetType, int packetSize) {
    int gender = client.getInputStream().readSignedByte();
    int head = client.getInputStream().readSignedByte();
    int jaw = client.getInputStream().readSignedByte();
    int torso = client.getInputStream().readSignedByte();
    int arms = client.getInputStream().readSignedByte();
    int hands = client.getInputStream().readSignedByte();
    int legs = client.getInputStream().readSignedByte();
    int feet = client.getInputStream().readSignedByte();
    int hairC = client.getInputStream().readSignedByte();
    int torsoC = client.getInputStream().readSignedByte();
    int legsC = client.getInputStream().readSignedByte();
    int feetC = client.getInputStream().readSignedByte();
    int skinC = client.getInputStream().readSignedByte();

    /*client.getPlayerLook()[0] = gender;
    client.setGender(gender);
    client.setHead(head);
    client.setBeard(jaw);
    client.setTorso(torso);
    client.setArms(arms);
    client.setHands(hands);
    client.setLegs(legs);
    client.setFeet(feet);
    client.getPlayerLook()[1] = hairC;
    client.getPlayerLook()[2] = torsoC;
    client.getPlayerLook()[3] = legsC;
    client.getPlayerLook()[4] = feetC;
    client.getPlayerLook()[5] = skinC;*/
    client.playerLooks[0] = gender;
    client.playerLooks[1] = head;
    client.playerLooks[2] = jaw;
    client.playerLooks[3] = torso;
    client.playerLooks[4] = arms;
    client.playerLooks[5] = hands;
    client.playerLooks[6] = legs;
    client.playerLooks[7] = feet;
    client.playerLooks[8] = hairC;
    client.playerLooks[9] = torsoC;
    client.playerLooks[10] = legsC;
    client.playerLooks[11] = feetC;
    client.playerLooks[12] = skinC;
    client.setLook(client.playerLooks);
    //client.send(new SendMessage("Thanks for setting your character! Please relog for equip to show up!"));
    client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
  }

}

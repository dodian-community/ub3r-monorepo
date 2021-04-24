package net.dodian.uber.game.model.player.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

public class SetRegionSong implements OutgoingPacket {

  private int songId;

  public SetRegionSong(int songId) {
    this.songId = songId;
  }

  @Override
  public void send(Client client) {
    client.getOutputStream().createFrame(74);
    client.getOutputStream().writeWordBigEndian(songId);
    client.flushOutStream();
  }

}

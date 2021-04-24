package net.dodian.uber.game.model.player.packets;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.incoming.AddFriend;
import net.dodian.uber.game.model.player.packets.incoming.AddIgnore;
import net.dodian.uber.game.model.player.packets.incoming.AttackNpc;
import net.dodian.uber.game.model.player.packets.incoming.AttackPlayer;
import net.dodian.uber.game.model.player.packets.incoming.Bank10;
import net.dodian.uber.game.model.player.packets.incoming.Bank5;
import net.dodian.uber.game.model.player.packets.incoming.BankAll;
import net.dodian.uber.game.model.player.packets.incoming.BankX1;
import net.dodian.uber.game.model.player.packets.incoming.BankX2;
import net.dodian.uber.game.model.player.packets.incoming.ChangeAppearance;
import net.dodian.uber.game.model.player.packets.incoming.ChangeRegion;
import net.dodian.uber.game.model.player.packets.incoming.Chat;
import net.dodian.uber.game.model.player.packets.incoming.ClickItem;
import net.dodian.uber.game.model.player.packets.incoming.ClickItem2;
import net.dodian.uber.game.model.player.packets.incoming.ClickNpc;
import net.dodian.uber.game.model.player.packets.incoming.ClickNpc2;
import net.dodian.uber.game.model.player.packets.incoming.ClickNpc3;
import net.dodian.uber.game.model.player.packets.incoming.ClickObject;
import net.dodian.uber.game.model.player.packets.incoming.ClickObject2;
import net.dodian.uber.game.model.player.packets.incoming.ClickObject3;
import net.dodian.uber.game.model.player.packets.incoming.ClickingButtons;
import net.dodian.uber.game.model.player.packets.incoming.ClickingStuff;
import net.dodian.uber.game.model.player.packets.incoming.Commands;
import net.dodian.uber.game.model.player.packets.incoming.Dialogue;
import net.dodian.uber.game.model.player.packets.incoming.DropItem;
import net.dodian.uber.game.model.player.packets.incoming.DuelRequest;
import net.dodian.uber.game.model.player.packets.incoming.FollowPlayer;
import net.dodian.uber.game.model.player.packets.incoming.ItemOnGroundItem;
import net.dodian.uber.game.model.player.packets.incoming.ItemOnItem;
import net.dodian.uber.game.model.player.packets.incoming.ItemOnObject;
import net.dodian.uber.game.model.player.packets.incoming.MagicOnItems;
import net.dodian.uber.game.model.player.packets.incoming.MagicOnNpc;
import net.dodian.uber.game.model.player.packets.incoming.MagicOnPlayer;
import net.dodian.uber.game.model.player.packets.incoming.MouseClicks;
import net.dodian.uber.game.model.player.packets.incoming.MoveItems;
import net.dodian.uber.game.model.player.packets.incoming.PickUpGroundItem;
import net.dodian.uber.game.model.player.packets.incoming.RemoveFriend;
import net.dodian.uber.game.model.player.packets.incoming.RemoveIgnore;
import net.dodian.uber.game.model.player.packets.incoming.RemoveItem;
import net.dodian.uber.game.model.player.packets.incoming.SendPrivateMessage;
import net.dodian.uber.game.model.player.packets.incoming.Trade;
import net.dodian.uber.game.model.player.packets.incoming.TradeRequest;
import net.dodian.uber.game.model.player.packets.incoming.UpdateChat;
import net.dodian.uber.game.model.player.packets.incoming.UseItemOnNpc;
import net.dodian.uber.game.model.player.packets.incoming.UseItemOnPlayer;
import net.dodian.uber.game.model.player.packets.incoming.Walking;
import net.dodian.uber.game.model.player.packets.incoming.WearItem;

public class PacketHandler {

  private static Packet packets[] = new Packet[255];

  static {
    packets[4] = new Chat();
    packets[14] = new UseItemOnPlayer();
    packets[16] = new ClickItem2();
    packets[17] = new ClickNpc2();
    packets[21] = new ClickNpc3();
    packets[25] = new ItemOnGroundItem();
    packets[39] = new FollowPlayer();
    packets[40] = new Dialogue();
    packets[41] = new WearItem();
    packets[43] = new Bank10();
    packets[53] = new ItemOnItem();
    packets[57] = new UseItemOnNpc();
    packets[70] = new ClickObject3();
    packets[72] = new AttackNpc();
    packets[73] = new AttackPlayer();
    packets[74] = new RemoveIgnore();
    packets[87] = new DropItem();
    packets[95] = new UpdateChat();
    packets[98] = new Walking();
    packets[101] = new ChangeAppearance();
    packets[103] = new Commands();
    packets[117] = new Bank5();
    packets[121] = new ChangeRegion();
    packets[122] = new ClickItem();
    packets[126] = new SendPrivateMessage();
    packets[128] = new TradeRequest();
    packets[129] = new BankAll();
    packets[130] = new ClickingStuff();
    packets[131] = new MagicOnNpc();
    packets[132] = new ClickObject();
    packets[133] = new AddIgnore();
    packets[135] = new BankX1();
    packets[139] = new Trade();
    packets[145] = new RemoveItem();
    packets[153] = new DuelRequest();
    packets[155] = new ClickNpc();
    packets[164] = new Walking();
    packets[185] = new ClickingButtons();
    packets[188] = new AddFriend();
    packets[192] = new ItemOnObject();
    packets[208] = new BankX2();
    packets[214] = new MoveItems();
    packets[215] = new RemoveFriend();
    packets[236] = new PickUpGroundItem();
    packets[237] = new MagicOnItems();
    packets[241] = new MouseClicks();
    packets[248] = new Walking();
    packets[249] = new MagicOnPlayer();
    packets[252] = new ClickObject2();
  }

  public static void process(Client client, int packetType, int packetSize) {
    if (packetType == -1)
      return;
    if (packetType < 0 || packets.length <= packetType)
      return;
    Packet packet = packets[packetType];
    if (packet == null)
      return;
    packet.ProcessPacket(client, packetType, packetSize);
  }

}
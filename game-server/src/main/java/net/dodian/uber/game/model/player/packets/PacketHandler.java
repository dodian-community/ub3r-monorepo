package net.dodian.uber.game.model.player.packets;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.incoming.*;

public class PacketHandler {

    private static Packet[] packets = new Packet[255];

    static {
        packets[25] = new ItemOnGroundItem();
        packets[87] = new DropItem();
        packets[236] = new PickUpGroundItem();
        packets[41] = new WearItem();
        packets[214] = new MoveItems();

        packets[14] = new UseItemOnPlayer();
        packets[53] = new ItemOnItem();
        packets[57] = new UseItemOnNpc();
        packets[192] = new ItemOnObject();

        packets[72] = new AttackNpc();
        packets[73] = new AttackPlayer();
        packets[131] = new MagicOnNpc();
        packets[249] = new MagicOnPlayer();
        packets[237] = new MagicOnItems();
        packets[35] = new MagicOnObject();

        packets[4] = new Chat();
        packets[40] = new Dialogue();
        packets[95] = new UpdateChat();
        packets[103] = new Commands();

        packets[126] = new SendPrivateMessage();
        packets[188] = new AddFriend();
        packets[215] = new RemoveFriend();
        packets[133] = new AddIgnore();
        packets[74] = new RemoveIgnore();

        packets[248] = new Walking();
        packets[164] = new Walking();
        packets[98] = new Walking();
        packets[39] = new FollowPlayer();
        packets[121] = new ChangeRegion();
        packets[210] = new ChangeRegion();

        packets[132] = new ClickObject();
        packets[252] = new ClickObject2();
        packets[70] = new ClickObject3();
        packets[234] = new ClickObject4();

        packets[122] = new ClickItem();
        packets[16] = new ClickItem2();
        packets[75] = new ClickItem3();

        packets[155] = new ClickNpc();
        packets[17] = new ClickNpc2();
        packets[21] = new ClickNpc3();

        packets[128] = new TradeRequest();
        packets[139] = new Trade();
        packets[153] = new DuelRequest();

        packets[145] = new RemoveItem();
        packets[117] = new Bank5();
        packets[43] = new Bank10();
        packets[129] = new BankAll();
        packets[135] = new BankX1();
        packets[208] = new BankX2();

        packets[130] = new ClickingStuff();
        packets[185] = new ClickingButtons();
        packets[241] = new MouseClicks();
        packets[101] = new ChangeAppearance();
        /* Unused packets! */
        //packets[60] = new InputName();
        packets[3] = null; //Client focus change!
        packets[86] = null; //Camera angle!
    }

    public static void process(Client client, int packetType, int packetSize) {
        if (packetType < 0 || packets.length <= packetType || packetType == -1) {
            return;
        }
        Packet packet = packets[packetType];
        if (packet == null) {
            return;
        }
        packet.ProcessPacket(client, packetType, packetSize);
    }

}
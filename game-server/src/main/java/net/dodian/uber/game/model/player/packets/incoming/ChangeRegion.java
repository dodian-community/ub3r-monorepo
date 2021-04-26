package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.party.Balloons;

public class ChangeRegion implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        client.replaceDoors();
        Balloons.updateBalloons(client);
        GlobalObject.updateObject(client);
        if (client.getPosition().getZ() == 0) {
            /* NMZ object removal!*/
            for (int x = 0; x <= 9; x++)
                for (int y = 0; y <= 8; y++)
                    client.ReplaceObject2(2600 + x, 3111 + y, -1, 0, 10);

            client.ReplaceObject2(2869, 9813, 2343, 0, 10); //Brick
            client.ReplaceObject2(2870, 9813, 2343, 0, 10); //Brick
            client.ReplaceObject2(2871, 9813, 2343, 0, 10); //Brick

            client.ReplaceObject2(2866, 9797, 2343, 0, 10); //Brick
            client.ReplaceObject2(2866, 9798, 2343, 0, 10); //Brick
            client.ReplaceObject2(2866, 9799, 2343, 0, 10); //Brick
            client.ReplaceObject2(2866, 9800, 2343, 0, 10); //Brick

            client.ReplaceObject2(2885, 9794, 882, 0, 10); // Shortcut entrance Taverly
            client.ReplaceObject2(2899, 9728, 882, 0, 10); // Shortcut exit Taverly

            client.ReplaceObject2(2542, 3097, 12260, 0, 10); //Teleport

            client.ReplaceObject2(2613, 3084, 3994, -3, 11);
            client.ReplaceObject2(2628, 3151, 2104, -3, 11);
            client.ReplaceObject2(2629, 3151, 2105, -3, 11);
            client.ReplaceObject2(2733, 3374, 6420, -1, 11);
            client.ReplaceObject2(2688, 3481, 27978, 1, 11); //Blood altar
            client.ReplaceObject2(2626, 3116, 14905, -1, 11); //Nature altar
            client.ReplaceObject2(2595, 3409, 133, -1, 10); // Dragon lair
            client.ReplaceObject2(2863, 3427, 3828, 0, 10); //Kalphite lair entrance!

            client.ReplaceObject2(2669, 3316, -1, -1, 11); // Remove door?
            client.ReplaceObject2(2713, 3483, -1, -1, 0); // Remove seers door?
            client.ReplaceObject2(2716, 3472, -1, -1, 0); // Remove seers door?
            client.ReplaceObject2(2594, 3102, -1, -1, 0); // Remove Yanille door?
            client.ReplaceObject2(2816, 3438, -1, -1, 0); // Remove Catherby door?
            /*
             * Danno: Box off new area from noobs =]
             */

            client.ReplaceObject2(2998, 3931, 6951, 0, 0);
            client.ReplaceObject2(2904, 9678, 6951, 0, 10);
            // slayer update
            // ReplaceObject2(2904, 9678, -1, -1, 11);
            // ReplaceObject2(2691, 9774, 2107, 0, 11);

            // Ancient slayer dunegon
            client.ReplaceObject(2661, 9815, 2391, 0, 0);
            client.ReplaceObject(2662, 9815, 2392, -2, 0);
        }
        if (client.inWildy() || client.duelFight) {
            client.getOutputStream().createFrameVarSize(104);
            client.getOutputStream().writeByteC(3);
            client.getOutputStream().writeByteA(1);
            client.getOutputStream().writeString("Attack");
            client.getOutputStream().endFrameVarSize();
        } else {
            client.getOutputStream().createFrameVarSize(104);
            client.getOutputStream().writeByteC(3);
            client.getOutputStream().writeByteA(0);
            client.getOutputStream().writeString("null");
            client.getOutputStream().endFrameVarSize();
        }
        client.updatePlayerDisplay();
        int wild = client.getWildLevel();
        if (wild > 0) {
            client.setWildLevel(wild);
        } else {
            client.updatePlayerDisplay();
        }
        if (!client.pLoaded) {
            client.pLoaded = true;
        }
        if (!client.IsPMLoaded) {
            client.refreshFriends();
            client.IsPMLoaded = true;
        }
    }

}

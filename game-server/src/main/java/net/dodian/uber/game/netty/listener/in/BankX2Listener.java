package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.party.Balloons;
import net.dodian.uber.game.netty.listener.out.InventoryInterface;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the second half of an "X" withdraw/deposit (opcode 208).
 * Reads the user-entered amount and performs the appropriate action based on the
 * stored Xinterface/Xremove* variables on the {@link Client}.
 */
public class BankX2Listener implements PacketListener {

    static { PacketListenerManager.register(208, new BankX2Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(BankX2Listener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        int enteredAmount = buf.readInt(); // big-endian dword
        if (enteredAmount < 1) return;

        if (logger.isTraceEnabled()) {
            logger.trace("BankX2 amount={} iface={} removeId={} slot={} player={}", enteredAmount,
                    client.XinterfaceID, client.XremoveID, client.XremoveSlot, client.getPlayerName());
        }

        // --- BEGIN: direct copy of legacy logic (minus initial reads) ---
        try {
            if (client.convoId == 1001) {
                client.send(new RemoveInterfaces());
                Server.slots.rollDice(client, enteredAmount);
                return;
            }
            if (client.XinterfaceID == 4753) { // Herb making code
                client.send(new RemoveInterfaces());
                int slot = client.XremoveSlot - 1;
                int id = client.herbOptions.get(slot).getId();
                boolean grimy = client.GetItemName(id).toLowerCase().contains("grimy");
                int coins = client.getInvAmt(995);
                if (grimy) {
                    enteredAmount = Math.min(enteredAmount, coins / 200);
                    enteredAmount = Math.min(enteredAmount, client.getInvAmt(id));
                    if (enteredAmount > 0) {
                        if (client.getInvAmt(id) <= enteredAmount) client.herbOptions.remove(slot);
                        int otherHerb = -1;
                        for (int h = 0; h < Utils.grimy_herbs.length && otherHerb == -1; h++)
                            if (id == client.GetNotedItem(Utils.grimy_herbs[h]))
                                otherHerb = client.GetNotedItem(Utils.herbs[h]);
                        client.deleteItem(995, enteredAmount * 200);
                        client.deleteItem(id, enteredAmount);
                        client.addItem(otherHerb, enteredAmount);
                        client.checkItemUpdate();
                        client.showNPCChat(client.NpcTalkTo, 598, new String[]{"Here is your all of ", enteredAmount + " " + client.GetItemName(id).toLowerCase()});
                        if (!client.herbOptions.isEmpty()) client.nextDiag = 4756;
                    } else {
                        client.showNPCChat(client.NpcTalkTo, 605, new String[]{"You need 1 herb and 200 coins", "for me to grind it for you."});
                    }
                } else if (!client.playerHasItem(228))
                    client.showNPCChat(client.NpcTalkTo, 605, new String[]{"You need noted vial of water for me to do that!"});
                else { // Make unfinished potions!
                    int otherHerb = -1;
                    for (int h = 0; h < Utils.herb_unf.length && otherHerb == -1; h++)
                        if (id == client.GetNotedItem(Utils.herb_unf[h]))
                            otherHerb = client.GetNotedItem(Utils.herbs[h]);
                    int vials = client.getInvAmt(228), herbs = client.getInvAmt(otherHerb);
                    enteredAmount = Math.min(enteredAmount, coins / 1_000); // Check coins first!
                    enteredAmount = Math.min(enteredAmount, vials); // Vials after
                    enteredAmount = Math.min(enteredAmount, herbs); // Herb last
                    if (enteredAmount > 0) {
                        if (herbs <= enteredAmount) client.herbOptions.remove(slot);
                        client.deleteItem(995, enteredAmount * 1_000);
                        client.deleteItem(228, enteredAmount);
                        client.deleteItem(otherHerb, enteredAmount);
                        client.addItem(id, enteredAmount);
                        client.checkItemUpdate();
                        client.showNPCChat(client.NpcTalkTo, 598, new String[]{"Here is your all of ", enteredAmount + " " + client.GetItemName(id).toLowerCase()});
                        if (!client.herbOptions.isEmpty()) client.nextDiag = 4757;
                    } else {
                        client.showNPCChat(client.NpcTalkTo, 605, new String[]{"You need atleast 1 herb, 1 vial of water and 1000 coins", "for me to turn it into a unfinish potion."});
                    }
                }
                client.XinterfaceID = -1;
                return;
            }
            if (client.XinterfaceID == 3838) { // Claim battlestaffs
                client.send(new RemoveInterfaces());
                int amount = client.dailyReward.isEmpty() ? 0 : Integer.parseInt(client.dailyReward.get(2));
                int totalAmount = amount;
                amount = Math.min(enteredAmount, amount);
                int coins = client.getInvAmt(995);
                amount = coins == 0 ? 0 : Math.min(amount, coins / 7000);
                if (coins < 7000) {
                    client.showNPCChat(3837, 597, new String[]{"You do not have enough coins to purchase one battlestaff."});
                } else {
                    client.deleteItem(995, amount * 7_000);
                    client.addItem(1392, amount);
                    client.dailyReward.set(2, (totalAmount - amount) + "");
                    client.checkItemUpdate();
                    client.showNPCChat(3837, 595, new String[]{"Here is " + amount + " battlestaffs for you."});
                }
                return;
            }
            if (client.enterAmountId > 0) {
                client.send(new RemoveInterfaces());
                if (client.enterAmountId == 1) { // cooking amt
                    if (client.inTrade || client.inDuel) {
                        client.send(new SendMessage("Cannot cook in duel or trade"));
                        return;
                    }
                    client.cookAmount = enteredAmount;
                    client.enterAmountId = 0;
                    client.cooking = true;
                    return;
                }
            }
            if (client.XinterfaceID == 5064) { // remove from bag to bank
                if (client.IsBanking)
                    client.bankItem(client.playerItems[client.XremoveSlot] - 1, client.XremoveSlot, enteredAmount);
                else if (client.isPartyInterface)
                    Balloons.offerItems(client, client.playerItems[client.XremoveSlot] - 1, enteredAmount, client.XremoveSlot);
                client.checkItemUpdate();
            } else if (client.XinterfaceID == 5382) { // remove from bank
                client.fromBank(client.bankItems[client.XremoveSlot] - 1, client.XremoveSlot, enteredAmount);
                client.checkItemUpdate();
            } else if (client.XinterfaceID == 2274) { // remove from party
                Balloons.removeOfferItems(client, client.offeredPartyItems.get(client.XremoveSlot).getId(), enteredAmount, client.XremoveSlot);
                client.checkItemUpdate();
            } else if (client.XinterfaceID == 3322 && client.inDuel && client.canOffer) { // bag to duel
                client.stakeItem(client.XremoveID, client.XremoveSlot, enteredAmount);
            } else if (client.XinterfaceID == 6669 && client.inDuel && client.canOffer) { // from duel window
                client.fromDuel(client.XremoveID, client.XremoveSlot, enteredAmount);
            } else if (client.XinterfaceID == 3900 && client.XremoveID != -1) { // buy from shop
                int id = client.XremoveID, slot = client.XremoveSlot;
                client.XremoveID = -1; client.XremoveSlot = -1;
                client.buyItem(id, slot, enteredAmount);
                client.checkItemUpdate();
                client.send(new InventoryInterface(3824, 3822));
            } else if (client.XinterfaceID == 3823 && client.XremoveID != -1) { // sell to shop
                int id = client.XremoveID, slot = client.XremoveSlot;
                client.XremoveID = -1; client.XremoveSlot = -1;
                client.sellItem(id, slot, enteredAmount);
                client.checkItemUpdate();
                client.send(new InventoryInterface(3824, 3822));
            } else if (client.XinterfaceID == 3322 && client.inTrade && client.canOffer) { // bag to trade window
                client.tradeItem(client.XremoveID, client.XremoveSlot, enteredAmount);
            } else if (client.XinterfaceID == 3415 && client.inTrade && client.canOffer) { // from trade window
                client.fromTrade(client.XremoveID, client.XremoveSlot, enteredAmount);
            }
        } finally {
            // reset vars to avoid stale state
            client.XinterfaceID = -1;
            client.enterAmountId = 0;
        }
        // --- END legacy logic ---
    }
}

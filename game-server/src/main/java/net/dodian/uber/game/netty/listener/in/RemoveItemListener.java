package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.systems.content.ui.SkillingInterfaceItemService;
import net.dodian.uber.game.model.ShopManager;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.content.events.partyroom.Balloons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fully-ported Netty version of legacy {@code RemoveItem} (opcode 145).
 */
@PacketHandler(opcode = 145)
public class RemoveItemListener implements PacketListener {

    static {
        PacketListenerManager.register(145, new RemoveItemListener());
    }

    private static final Logger logger = LoggerFactory.getLogger(RemoveItemListener.class);
    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }
        // mystic client sends: int interfaceId, then two unsigned shorts with ADD transform
        int interfaceID = ByteBufReader.readInt(buf);
        int removeSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int removeID = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int bankSlot = removeSlot;

        if ((interfaceID == 5382 || (interfaceID >= 50300 && interfaceID <= 50310)) && client.bankStyleViewOpen) {
            return;
        }

        if (interfaceID == 5382 || (interfaceID >= 50300 && interfaceID <= 50310)) {
            bankSlot = client.resolveBankSlot(interfaceID, removeSlot);
            removeID = client.resolveBankItemId(interfaceID, removeSlot, removeID);
        }

        if (client.playerRights == 2) {
            client.println_debug("RemoveItem: " + removeID + " InterID: " + interfaceID + " slot: " + removeSlot);
        }

        if (interfaceID == 3322 && client.inDuel && client.canOffer) { // bag -> duel window
            client.stakeItem(removeID, removeSlot, 1);
        } else if (interfaceID == 6669 && client.inDuel && client.canOffer) { // duel window -> bag
            client.fromDuel(removeID, removeSlot, 1);
        } else if (interfaceID == 1688) { // unequip item
            if (client.hasSpace()) {
                int id = client.getEquipment()[removeSlot];
                int amount = client.getEquipmentN()[removeSlot];
                if (client.remove(removeSlot, false))
                    client.addItem(id, amount);
                client.checkItemUpdate();
            } else {
                client.send(new SendMessage("Not enough space to unequip this item!"));
            }
        } else if (interfaceID == 5064) { // bag -> bank
            if (client.IsBanking)
                client.bankItem(removeID, removeSlot, 1);
            else if (client.isPartyInterface)
                Balloons.offerItems(client, removeID, 1, removeSlot);
            client.checkItemUpdate();
        } else if (interfaceID == 5382 || (interfaceID >= 50300 && interfaceID <= 50310)) { // bank -> bag (mystic tabs: 50300-50310)
            if (bankSlot >= 0) {
                client.fromBank(removeID, bankSlot, 1);
            }
        } else if (interfaceID == 2274) { // party remove
            Balloons.removeOfferItems(client, removeID, 1, removeSlot);
        } else if (interfaceID == 3322 && client.inTrade && client.canOffer) { // bag -> trade
            client.tradeItem(removeID, removeSlot, 1);
        } else if (interfaceID == 3415 && client.inTrade && client.canOffer) { // trade -> bag
            client.fromTrade(removeID, removeSlot, 1);
        } else if (SkillingInterfaceItemService.handleContainerAmount(client, interfaceID, removeID, removeSlot, 1)) {
        } else if (interfaceID == 3823) { // shop sell value
            if (!Server.shopping || client.tradeLocked) {
                client.send(new SendMessage(client.tradeLocked ? "You are trade locked!" : "Currently selling stuff to the store has been disabled!"));
                return;
            }
            if (Server.itemManager.getShopBuyValue(removeID) < 0 || !Server.itemManager.isTradable(removeID)) {
                client.send(new SendMessage("You cannot sell " + client.getItemName(removeID).toLowerCase() + " in this store."));
                return;
            }
            boolean isIn = false;
            if (ShopManager.ShopSModifier[client.MyShopID] > 1) {
                for (int j = 0; j <= ShopManager.ShopItemsStandard[client.MyShopID]; j++) {
                    if (removeID == (ShopManager.ShopItems[client.MyShopID][j] - 1)) {
                        isIn = true;
                        break;
                    }
                }
            } else isIn = true;
            if (!isIn && (ShopManager.ShopBModifier[client.MyShopID] == 2 && !ShopManager.findDefaultItem(client.MyShopID, removeID))) {
                client.send(new SendMessage("You cannot sell " + client.getItemName(removeID).toLowerCase() + " in this store."));
            } else {
                int currency = client.MyShopID == 55 ? 11997 : 995;
                int shopValue = client.MyShopID == 55 ? 1000 : (int) Math.floor(client.GetShopBuyValue(removeID));
                String shopAdd = formatValueSuffix(shopValue);
                client.send(new SendMessage(client.getItemName(removeID) + ": shop will buy for " + shopValue + " " + client.getItemName(currency).toLowerCase() + shopAdd));
            }
        } else if (interfaceID == 3900) { // shop buy value
            int currency = client.MyShopID == 55 ? 11997 : 995;
            int shopValue = client.MyShopID == 55 ? client.eventShopValues(removeSlot) : (int) Math.floor(client.GetShopSellValue(removeID));
            shopValue = client.MyShopID >= 7 && client.MyShopID <= 11 ? (int) (shopValue * 1.5) : shopValue;
            shopValue = client.MyShopID >= 9 && client.MyShopID <= 11 ? (int) (shopValue * 1.5) : shopValue;
            String shopAdd = formatValueSuffix(shopValue);
            client.send(new SendMessage(client.getItemName(removeID) + ": currently costs " + shopValue + " " + client.getItemName(currency).toLowerCase() + shopAdd));
        }
        client.CheckGear();
    }

    private static String formatValueSuffix(int value) {
        int thousand = 1_000;
        int million = 1_000_000;
        if (value >= thousand && value < million) {
            return " (" + (value / thousand) + "K)";
        } else if (value >= million) {
            int leftover = value - ((value / million) * million);
            return " (" + (value / million) + (leftover / 100_000 > 0 ? "." + (leftover / 100_000) : "") + " million)";
        }
        return "";
    }
}

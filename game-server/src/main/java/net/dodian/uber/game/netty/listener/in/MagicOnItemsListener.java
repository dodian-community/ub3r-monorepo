package net.dodian.uber.game.netty.listener.in;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.netty.listener.out.SendSideTab;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of legacy MagicOnItems (opcode 237).
 * Handles superheat, item enchant, low/high alchemy on inventory items.
 */
@PacketHandler(opcode = 237)
public class MagicOnItemsListener implements PacketListener {

    static {
        PacketListenerManager.register(237, new MagicOnItemsListener());
    }

    private static final Logger logger = LoggerFactory.getLogger(MagicOnItemsListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteMessage msg = ByteMessage.wrap(packet.getPayload());
        // decode order based on client build: slot (big), itemId (little+ADD), dummy (big), spellId (little+ADD)
        int castOnSlot = msg.getShort(true, ByteOrder.BIG, ValueType.NORMAL);
        int castOnItem = msg.getShort(false, ByteOrder.BIG, ValueType.ADD);
        msg.getShort(true, ByteOrder.BIG, ValueType.NORMAL); // unused / interface id
        int castSpell = msg.getShort(false, ByteOrder.BIG, ValueType.ADD);

        // quick sanity
        if (castOnSlot < 0 || castOnSlot > 28) {
            client.disconnected = true;
            return;
        }

        int value = (int) Server.itemManager.getAlchemy(castOnItem);

        if (System.currentTimeMillis() - client.lastMagic < 1800 || !client.playerHasItem(castOnItem) || client.playerItems[castOnSlot] != castOnItem + 1) {
            client.send(new SendSideTab(6));
            return;
        }
        if (client.randomed || client.randomed2) {
            return;
        }

        // Superheat
        if (castSpell == 1173) {
            if (!checkLevel(client, 43)) return;
            client.superHeat(castOnItem);
            return;
        }

        // Enchant gems
        if (handleEnchant(client, castSpell, castOnItem)) {
            return;
        }

        // Low or High alchemy
        if (castSpell == 1162 || castSpell == 1178) {
            handleAlchemy(client, castOnSlot, castOnItem, value);
        }
    }

    private boolean checkLevel(Client client, int lvl) {
        if (client.getLevel(Skill.MAGIC) < lvl) {
            client.send(new SendMessage("You need a magic level of " + lvl + " to cast this spell."));
            return false;
        }
        return true;
    }

    private boolean handleEnchant(Client client, int spell, int itemId) {
        int reqLevel, runeCost, exp, resultItem = 0;
        switch (spell) {
            case 1155: // sapphire
                reqLevel = 7; runeCost = 2; exp = 175;
                if (itemId == 1637) resultItem = 2550; else if (itemId == 1694) resultItem = 1727;
                break;
            case 1165: // emerald
                reqLevel = 27; runeCost = 4; exp = 370;
                if (itemId == 1696) resultItem = 1729;
                break;
            case 1176: // ruby
                reqLevel = 49; runeCost = 6; exp = 590;
                if (itemId == 1641) resultItem = 2568; else if (itemId == 1698) resultItem = 1725;
                break;
            case 1180: // diamond
                reqLevel = 57; runeCost = 8; exp = 670;
                if (itemId == 1643) resultItem = 2570; else if (itemId == 1700) resultItem = 1731;
                break;
            case 1187: // dragonstone
                reqLevel = 68; runeCost = 10; exp = 780;
                if (itemId == 1645) resultItem = 2572; else if (itemId == 1702) resultItem = 1704;
                break;
            case 6003: // onyx
                reqLevel = 87; runeCost = 10; exp = 1150;
                if (itemId == 6575) resultItem = 6583; else if (itemId == 6577) resultItem = 11128; else if (itemId == 6581) resultItem = 6585;
                break;
            default:
                return false;
        }
        if (!checkLevel(client, reqLevel)) return true;
        if (client.hasRunes(new int[]{564}, new int[]{runeCost})) { // 564 = cosmic
            client.send(new SendMessage("You need " + runeCost + " cosmic runes to cast this spell!"));
            return true;
        }
        if (resultItem == 0) {
            client.send(new SendMessage("Cant enchant this item!"));
            return true;
        }
        client.lastMagic = System.currentTimeMillis();
        client.deleteItem(itemId, 1);
        client.deleteRunes(new int[]{564}, new int[]{runeCost});
        client.addItem(resultItem, 1);
        client.checkItemUpdate();
        client.requestAnim(720, 0);
        client.callGfxMask(115, 100);
        client.send(new SendSideTab(6));
        client.giveExperience(exp, Skill.MAGIC);
        return true;
    }

    private void handleAlchemy(Client client, int slot, int itemId, int value) {
        if (!client.playerHasItem(561) || (itemId == 561 && !client.playerHasItem(561, 2))) {
            client.send(new SendMessage("Requires nature rune!"));
            return;
        }
        if (itemId == 995 || client.premiumItem(itemId) || (itemId >= 2415 && itemId <= 2417) || value < 1) {
            client.send(new SendMessage("This item can't be alched"));
            return;
        }
        client.lastMagic = System.currentTimeMillis();
        client.deleteItem(itemId, slot, 1);
        client.deleteItem(561, 1);
        client.addItem(995, value);
        client.checkItemUpdate();
        client.giveExperience(600, Skill.MAGIC);
        client.requestAnim(713, 0);
        client.callGfxMask(113, 100);
        client.send(new SendSideTab(6));
        client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }
}

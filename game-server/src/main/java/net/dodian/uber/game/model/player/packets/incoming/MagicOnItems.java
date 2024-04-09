package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.packets.outgoing.SendSideTab;
import net.dodian.uber.game.model.player.skills.Skill;

public class MagicOnItems implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int castOnSlot = client.getInputStream().readSignedWord();
        int castOnItem = client.getInputStream().readSignedWordA();
        client.getInputStream().readSignedWord();
        int castSpell = client.getInputStream().readSignedWordA();
        int value = (int) (double) Server.itemManager.getAlchemy(castOnItem);
        if (!(System.currentTimeMillis() - client.lastMagic >= 1800) || !client.playerHasItem(castOnItem) || !(client.playerItems[castOnSlot] == (castOnItem + 1))) {
            client.send(new SendSideTab(6));
            return;
        }
        if (client.randomed || client.randomed2) {
            return;
        }
        if (castSpell == 1173) {
            client.resetAction();
            if (client.getLevel(Skill.MAGIC) < 43) {
                client.send(new SendMessage("You need a magic level of 43 to cast this spell."));
                return;
            }
            client.superHeat(castOnItem);
        }
        if (castSpell == 1155) {// Sapphire
            if (client.getLevel(Skill.MAGIC) < 7) {
                client.send(new SendMessage("You need a magic level of 7 to cast this spell."));
                return;
            }
            if (client.hasRunes(new int[]{564}, new int[]{2})) {
                client.send(new SendMessage("You need 2 cosmic runes to cast this spell!"));
                return;
            }
            int item = 0;
            if (castOnItem == 1637)
                item = 2550;
            else if (castOnItem == 1694)
                item = 1727;
            else
                client.send(new SendMessage("Cant enchant this item!"));
            if (item == 0) {
                return;
            }
            client.lastMagic = System.currentTimeMillis();
            client.deleteItem(castOnItem, 1);
            client.deleteRunes(new int[]{564}, new int[]{2});
            client.addItem(item, 1);
            client.checkItemUpdate();
            client.requestAnim(720, 0);
            client.callGfxMask(115, 100);
            client.send(new SendSideTab(6));
            client.giveExperience(175, Skill.MAGIC);
        }
        if (castSpell == 1165) {// Emerald
            if (client.getLevel(Skill.MAGIC) < 27) {
                client.send(new SendMessage("You need a magic level of 27 to cast this spell."));
                return;
            }
            if (client.hasRunes(new int[]{564}, new int[]{4})) {
                client.send(new SendMessage("You need 4 cosmic runes to cast this spell!"));
                return;
            }
            int item = 0;
            if (castOnItem == 1696)
                item = 1729;
            else
                client.send(new SendMessage("Cant enchant this item!"));
            if (item == 0) {
                return;
            }
            client.lastMagic = System.currentTimeMillis();
            client.deleteItem(castOnItem, 1);
            client.deleteRunes(new int[]{564}, new int[]{4});
            client.addItem(item, 1);
            client.checkItemUpdate();
            client.requestAnim(720, 0);
            client.callGfxMask(115, 100);
            client.send(new SendSideTab(6));
            client.giveExperience(370, Skill.MAGIC);
        }
        if (castSpell == 1176) {// Ruby
            if (client.getLevel(Skill.MAGIC) < 49) {
                client.send(new SendMessage("You need a magic level of 49 to cast this spell."));
                return;
            }
            if (client.hasRunes(new int[]{564}, new int[]{6})) {
                client.send(new SendMessage("You need 6 cosmic runes to cast this spell!"));
                return;
            }
            int item = 0;
            if (castOnItem == 1641)
                item = 2568;
            else if (castOnItem == 1698)
                item = 1725;
            else
                client.send(new SendMessage("Cant enchant this item!"));
            if (item == 0) {
                return;
            }
            client.lastMagic = System.currentTimeMillis();
            client.deleteItem(castOnItem, 1);
            client.deleteRunes(new int[]{564}, new int[]{6});
            client.addItem(item, 1);
            client.checkItemUpdate();
            client.requestAnim(720, 0);
            client.callGfxMask(115, 100);
            client.send(new SendSideTab(6));
            client.giveExperience(590, Skill.MAGIC);
        }
        if (castSpell == 1180) {// Diamond
            if (client.getLevel(Skill.MAGIC) < 57) {
                client.send(new SendMessage("You need a magic level of 57 to cast this spell."));
                return;
            }
            if (client.hasRunes(new int[]{564}, new int[]{8})) {
                client.send(new SendMessage("You need 8 cosmic runes to cast this spell!"));
                return;
            }
            int item = 0;
            if (castOnItem == 1643)
                item = 2570;
            else if (castOnItem == 1700)
                item = 1731;
            else
                client.send(new SendMessage("Cant enchant this item!"));
            if (item == 0) {
                return;
            }
            client.lastMagic = System.currentTimeMillis();
            client.deleteItem(castOnItem, 1);
            client.deleteRunes(new int[]{564}, new int[]{8});
            client.addItem(item, 1);
            client.checkItemUpdate();
            client.requestAnim(720, 0);
            client.callGfxMask(115, 100);
            client.send(new SendSideTab(6));
            client.giveExperience(670, Skill.MAGIC);
        }
        if (castSpell == 1187) {// Dragonstone
            if (client.getLevel(Skill.MAGIC) < 68) {
                client.send(new SendMessage("You need a magic level of 68 to cast this spell."));
                return;
            }
            if (client.hasRunes(new int[]{564}, new int[]{10})) {
                client.send(new SendMessage("You need 10 cosmic runes to cast this spell!"));
                return;
            }
            int item = 0;
            if (castOnItem == 1645)
                item = 2572;
            else if (castOnItem == 1702)
                item = 1704;
            else
                client.send(new SendMessage("Cant enchant this item!"));
            if (item == 0) {
                return;
            }
            client.lastMagic = System.currentTimeMillis();
            client.deleteItem(castOnItem, 1);
            client.deleteRunes(new int[]{564}, new int[]{10});
            client.addItem(item, 1);
            client.checkItemUpdate();
            client.requestAnim(720, 0);
            client.callGfxMask(115, 100);
            client.send(new SendSideTab(6));
            client.giveExperience(780, Skill.MAGIC);
        }
        if (castSpell == 6003) {// Onyx
            if (client.getLevel(Skill.MAGIC) < 87) {
                client.send(new SendMessage("You need a magic level of 87 to cast this spell."));
                return;
            }
            if (client.hasRunes(new int[]{564}, new int[]{10})) {
                client.send(new SendMessage("You need 10 cosmic runes to cast this spell!"));
                return;
            }
            int item = 0;
            if (castOnItem == 6575)
                item = 6583;
            else if (castOnItem == 6577)
                item = 11128;
            else if (castOnItem == 6581)
                item = 6585;
            else
                client.send(new SendMessage("Cant enchant this item!"));
            if (item == 0) {
                return;
            }
            client.lastMagic = System.currentTimeMillis();
            client.deleteItem(castOnItem, 1);
            client.deleteRunes(new int[]{564}, new int[]{10});
            client.addItem(item, 1);
            client.checkItemUpdate();
            client.requestAnim(720, 0);
            client.callGfxMask(115, 100);
            client.send(new SendSideTab(6));
            client.giveExperience(1150, Skill.MAGIC);
        }
        if (castSpell == 1162 || castSpell == 1178) // Low Alch
        {
            if (!client.playerHasItem(561) || (castOnItem == 561 && !client.playerHasItem(561, 2))) {
                client.send(new SendMessage("Requires nature rune!"));
                return;
            }
            if (castOnItem == 995 || client.premiumItem(castOnItem) || (castOnItem >= 2415 && castOnItem <= 2417) || value < 1) {
                client.send(new SendMessage("This item can't be alched"));
                return;
            }
            client.lastMagic = System.currentTimeMillis();
            client.deleteItem(castOnItem, castOnSlot, 1);
            client.deleteItem(561, 1);
            client.addItem(995, value);
            client.checkItemUpdate();
            client.giveExperience(600, Skill.MAGIC);
            // animation(113, absY, absX);
            // stillgfx(113, absY, absX);
            client.requestAnim(713, 0);
            client.callGfxMask(113, 100);
            client.send(new SendSideTab(6));
            client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
        }

    }

}

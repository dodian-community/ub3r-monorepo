package net.dodian.uber.game.model.player.skills.prayer;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;

/**
 * @author Dashboard
 */
public class Prayer {

    public static boolean buryBones(Client client, int itemId, int itemSlot) {
        Bones bone = Bones.getBone(itemId);
        if (bone == null || !client.playerHasItem(itemId))
            return false;
        client.requestAnim(827, 0);
        client.giveExperience(bone.getExperience(), Skill.PRAYER);
        client.deleteItem(itemId, itemSlot, 1);
        client.send(new SendMessage("You bury the " + client.GetItemName(itemId).toLowerCase()));
        return true;
    }

    public static boolean altarBones(Client client, int itemId) {
        Bones bone = Bones.getBone(itemId);
        if (bone == null || !client.playerHasItem(itemId)) {
            client.boneItem = -1;
            return false;
        }
        client.deleteItem(itemId, 1);
        client.requestAnim(3705, 0);
        double extra = (double) (client.getLevel(Skill.FIREMAKING) + 1) / 100;
        double chance = 2.0 + extra;
        client.giveExperience((int) (bone.getExperience() * chance), Skill.PRAYER);
        client.send(new SendMessage("You sacrifice the " + client.GetItemName(itemId).toLowerCase() + " and your multiplier was " + chance + " (" + (int) (chance * 100) + "%)"));
        client.triggerRandom((int) (bone.getExperience() * chance));
        return true;
    }

}

package net.dodian.uber.game.skills.prayer

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.prayer.Bones
import net.dodian.uber.game.netty.listener.out.SendMessage

object PrayerInteractionService {
    @JvmStatic
    fun buryBones(client: Client, itemId: Int, itemSlot: Int): Boolean {
        val bone = Bones.getBone(itemId) ?: return false
        if (!client.playerHasItem(itemId)) return false
        client.requestAnim(827, 0)
        client.giveExperience(bone.getExperience(), Skill.PRAYER)
        client.deleteItem(itemId, itemSlot, 1)
        client.checkItemUpdate()
        client.send(SendMessage("You bury the ${client.GetItemName(itemId).lowercase()}"))
        return true
    }

    @JvmStatic
    fun altarBones(client: Client, itemId: Int): Boolean {
        val bone = Bones.getBone(itemId)
        if (bone == null || !client.playerHasItem(itemId) || client.randomed) {
            client.resetAction()
            return false
        }
        client.prayerAction = 3
        client.deleteItem(itemId, 1)
        client.checkItemUpdate()
        client.requestAnim(3705, 0)
        client.stillgfx(624, Position(client.skillX, client.skillY, client.position.z), 0)
        val extra = (client.getLevel(Skill.FIREMAKING) + 1).toDouble() / 100
        val chance = 2.0 + extra
        val experience = (bone.getExperience() * chance).toInt()
        client.giveExperience(experience, Skill.PRAYER)
        client.send(
            SendMessage(
                "You sacrifice the ${client.GetItemName(itemId).lowercase()} and your multiplier was $chance (${(chance * 100).toInt()}%)"
            )
        )
        client.triggerRandom(experience)
        return true
    }
}

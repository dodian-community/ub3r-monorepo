package net.dodian.uber.game.content.skills.prayer

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.content.skills.core.runtime.SkillingRandomEventService
import net.dodian.uber.game.model.player.skills.prayer.Bones
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.action.SkillingActionService

object PrayerInteractionService {
    @JvmStatic
    fun buryBones(client: Client, itemId: Int, itemSlot: Int): Boolean {
        val bone = Bones.getBone(itemId) ?: return false
        if (!client.playerHasItem(itemId)) return false
        client.performAnimation(827, 0)
        SkillProgressionService.gainXp(client, bone.getExperience(), Skill.PRAYER)
        client.deleteItem(itemId, itemSlot, 1)
        client.checkItemUpdate()
        client.sendMessage("You bury the ${client.getItemName(itemId).lowercase()}")
        return true
    }

    @JvmStatic
    fun altarBones(client: Client, itemId: Int): Boolean {
        val bone = Bones.getBone(itemId)
        if (bone == null || !client.playerHasItem(itemId) || client.randomed) {
            client.resetAction()
            return false
        }
        client.prayerOfferingState = PrayerOfferingState(itemId, client.interactionAnchorX, client.interactionAnchorY)
        client.deleteItem(itemId, 1)
        client.checkItemUpdate()
        client.performAnimation(3705, 0)
        client.stillgfx(624, Position(client.interactionAnchorX, client.interactionAnchorY, client.position.z), 0)
        val extra = (client.getLevel(Skill.FIREMAKING) + 1).toDouble() / 100
        val chance = 2.0 + extra
        val experience = (bone.getExperience() * chance).toInt()
        SkillProgressionService.gainXp(client, experience, Skill.PRAYER)
        client.send(
            SendMessage(
                "You sacrifice the ${client.getItemName(itemId).lowercase()} and your multiplier was $chance (${(chance * 100).toInt()}%)"
            )
        )
        SkillingRandomEventService.trigger(client, experience)
        return true
    }

    @JvmStatic
    fun startAltarOffering(client: Client, request: PrayerOfferingRequest) {
        client.prayerOfferingState = PrayerOfferingState(request.boneItemId, request.altarX, request.altarY)
        SkillingActionService.startAltarBones(client)
    }
}

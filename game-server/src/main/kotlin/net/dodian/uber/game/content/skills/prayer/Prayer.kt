package net.dodian.uber.game.content.skills.prayer

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.systems.skills.action.SkillingRandomEventService
import net.dodian.uber.game.model.player.skills.prayer.Bones
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.action.SkillingActionService
import net.dodian.uber.game.systems.action.PolicyPreset
import net.dodian.uber.game.systems.skills.plugin.SkillPlugin
import net.dodian.uber.game.systems.skills.plugin.skillPlugin

object Prayer {
    @JvmStatic
    fun attempt(client: Client, itemId: Int, itemSlot: Int): Boolean = buryBones(client, itemId, itemSlot)

    @JvmStatic
    fun startOffering(client: Client, request: PrayerOfferingRequest) = startAltarOffering(client, request)

    @JvmStatic
    fun buryBones(client: Client, itemId: Int, itemSlot: Int): Boolean {
        val bone = Bones.getBone(itemId) ?: return false
        if (!client.playerHasItem(itemId)) return false
        client.performAnimation(PrayerData.BURY_ANIMATION, 0)
        ProgressionService.addXp(client, bone.getExperience(), Skill.PRAYER)
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
        client.performAnimation(PrayerData.ALTAR_ANIMATION, 0)
        client.stillgfx(PrayerData.ALTAR_GFX, Position(client.interactionAnchorX, client.interactionAnchorY, client.position.z), 0)
        val chance = PrayerData.altarMultiplier(client.getLevel(Skill.FIREMAKING))
        val experience = (bone.getExperience() * chance).toInt()
        ProgressionService.addXp(client, experience, Skill.PRAYER)
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

object AltarObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(409, 20377)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (client.currentPrayer < client.maxPrayer) {
            client.pray(client.maxPrayer)
            client.sendMessage("You restore your prayer points!")
        } else {
            client.sendMessage("You are at maximum prayer points!")
        }
        return true
    }

    override fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        if (objectId != 409) {
            return false
        }
        if (Bones.getBone(itemId) == null || !client.playerHasItem(itemId) || client.randomed) {
            client.resetAction()
            return false
        }
        client.setInteractionAnchor(position.x, position.y, position.z)
        Prayer.startOffering(client, PrayerOfferingRequest(itemId, position.x, position.y))
        return true
    }
}

object BuryBonesItems : ItemContent {
    override val itemIds: IntArray = Bones.values().map { it.getItemId() }.toIntArray()

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        return Prayer.attempt(client, itemId, itemSlot)
    }
}

object PrayerSkillPlugin : SkillPlugin {
    override val definition =
        skillPlugin(name = "Prayer", skill = Skill.PRAYER) {
            objectClick(preset = PolicyPreset.PRODUCTION, option = 1, *AltarObjects.objectIds) { client, objectId, position, obj ->
                AltarObjects.onFirstClick(client, objectId, position, obj)
            }
        }
}

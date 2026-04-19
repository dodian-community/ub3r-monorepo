package net.dodian.uber.game.skill.prayer

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.skill.runtime.action.SkillingRandomEventService
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.bindItemContentClick
import net.dodian.uber.game.api.plugin.skills.bindObjectContentClick
import net.dodian.uber.game.api.plugin.skills.bindObjectContentUseItem
import net.dodian.uber.game.api.plugin.skills.skillPlugin

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
        if (bone == null || !client.playerHasItem(itemId) || client.skillingEventState.isRandomEventOpen) {
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
        startAction(client)
    }

    @JvmStatic
    fun startAction(client: Client) = startAltarOfferingAction(client)

    @JvmStatic
    fun stopAction(client: Client) {
        client.clearPrayerOfferingState()
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun stopFromReset(client: Client, fullReset: Boolean) {
        stopAction(client)
    }

    @JvmStatic
    fun startAltarOfferingAction(client: Client) = PrayerActions.startAltarOfferingAction(client)
}

private class AltarObjectContent : ObjectContent {
    override val objectIds: IntArray = PrayerRouteIds.ALTAR_OBJECT_IDS

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
        if (objectId != PrayerRouteIds.MAIN_ALTAR_OBJECT_ID) {
            return false
        }
        if (Bones.getBone(itemId) == null || !client.playerHasItem(itemId) || client.skillingEventState.isRandomEventOpen) {
            client.resetAction()
            return false
        }
        client.setInteractionAnchor(position.x, position.y, position.z)
        Prayer.startOffering(client, PrayerOfferingRequest(itemId, position.x, position.y))
        return true
    }
}

object BuryBonesItems : ItemContent {
    override val itemIds: IntArray = PrayerRouteIds.BONE_ITEM_IDS

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        return Prayer.attempt(client, itemId, itemSlot)
    }
}

object PrayerSkillPlugin : SkillPlugin {
    override val definition =
        skillPlugin(name = "Prayer", skill = Skill.PRAYER) {
            val altarObjects = AltarObjectContent()
            bindObjectContentClick(
                preset = PolicyPreset.PRODUCTION,
                option = 1,
                content = altarObjects,
            )
            bindObjectContentUseItem(
                preset = PolicyPreset.PRODUCTION,
                content = altarObjects,
                itemIds = PrayerRouteIds.BONE_ITEM_IDS,
            )
            bindItemContentClick(
                preset = PolicyPreset.PRODUCTION,
                option = 1,
                content = BuryBonesItems,
            )
        }
}

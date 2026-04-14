package net.dodian.uber.game.skill.runecrafting

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.skill.runtime.action.SkillingRandomEventService
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.bindItemContentClick
import net.dodian.uber.game.api.plugin.skills.bindObjectContentClick
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.util.Misc
import org.slf4j.LoggerFactory

object Runecrafting {
    private val logger = LoggerFactory.getLogger(Runecrafting::class.java)

    @JvmStatic
    fun fillPouch(client: Client, pouchId: Int): Boolean {
        val slot = resolvePouchSlot(pouchId) ?: return false
        if (client.getLevel(Skill.RUNECRAFTING) < client.runePouchesLevel[slot]) {
            client.sendMessage("You need level ${client.runePouchesLevel[slot]} runecrafting to do this!")
            return true
        }
        if (client.runePouchesAmount[slot] >= client.runePouchesMaxAmount[slot]) {
            client.sendMessage("This pouch is currently full of essence!")
            return true
        }
        val now = System.currentTimeMillis()
        val lastAltarAt = client.runecraftingState?.lastAltarCraftAtMillis ?: 0L
        if (now - lastAltarAt <= 1200L) {
            logger.warn("Rapid pouch fill after altar craft player={} pouchId={} deltaMs={}", client.playerName, pouchId, now - lastAltarAt)
        }
        val max = client.runePouchesMaxAmount[slot] - client.runePouchesAmount[slot]
        val amount = minOf(client.getInvAmt(RunecraftingData.RUNE_ESSENCE_ID), max)
        if (amount > 0) {
            repeat(amount) {
                client.deleteItem(RunecraftingData.RUNE_ESSENCE_ID, 1)
            }
            client.runePouchesAmount[slot] += amount
            client.checkItemUpdate()
        } else {
            client.sendMessage("No essence in your inventory!")
        }
        return true
    }

    @JvmStatic
    fun emptyPouch(client: Client, pouchId: Int): Boolean {
        val slot = resolvePouchSlot(pouchId) ?: return false
        if (client.getLevel(Skill.RUNECRAFTING) < client.runePouchesLevel[slot]) {
            client.sendMessage("You need level ${client.runePouchesLevel[slot]} runecrafting to do this!")
            return true
        }
        var amount = client.freeSlots()
        if (amount <= 0) {
            client.sendMessage("Not enough inventory slot to empty the pouch!")
            return true
        }
        amount = minOf(amount, client.runePouchesAmount[slot])
        if (amount > 0) {
            repeat(amount) {
                client.addItem(RunecraftingData.RUNE_ESSENCE_ID, 1)
            }
            client.runePouchesAmount[slot] -= amount
            client.checkItemUpdate()
        } else {
            client.sendMessage("No essence in your pouch!")
        }
        return true
    }

    @JvmStatic
    fun checkPouch(client: Client, pouchId: Int): Boolean {
        val slot = resolvePouchSlot(pouchId) ?: return false
        client.sendMessage("There is ${client.runePouchesAmount[slot]} rune essence in this pouch!")
        return true
    }

    @JvmStatic
    fun start(client: Client, request: RunecraftingRequest): Boolean {
        if (!client.contains(RunecraftingData.RUNE_ESSENCE_ID)) {
            client.sendMessage("You do not have any rune essence!")
            return false
        }
        if (client.getLevel(Skill.RUNECRAFTING) < request.requiredLevel) {
            client.send(
                SendMessage(
                    "You must have ${request.requiredLevel} runecrafting to craft ${client.getItemName(request.runeId).lowercase()}",
                ),
            )
            return false
        }

        val essenceCount = client.getInvAmt(RunecraftingData.RUNE_ESSENCE_ID)
        if (essenceCount <= 0) {
            client.sendMessage("You do not have any rune essence!")
            return false
        }

        var extra = 0
        repeat(essenceCount) {
            client.deleteItem(RunecraftingData.RUNE_ESSENCE_ID, 1)
            val chance = (client.getLevel(Skill.RUNECRAFTING) + 1) / 2
            val roll = 1 + Misc.random(99)
            if (roll <= chance) {
                extra++
            }
        }

        val crafted = essenceCount + extra
        client.sendMessage("You craft $crafted ${client.getItemName(request.runeId).lowercase()}s")
        client.addItem(request.runeId, crafted)
        client.checkItemUpdate()
        val xp = request.experiencePerEssence * essenceCount
        ProgressionService.addXp(client, xp, Skill.RUNECRAFTING)
        SkillingRandomEventService.trigger(client, xp)
        client.runecraftingState = RunecraftingState(lastAltarCraftAtMillis = System.currentTimeMillis())
        return true
    }

    private fun resolvePouchSlot(pouchId: Int): Int? {
        val slot = if (pouchId == 5509) 0 else (pouchId - 5508) / 2
        return if (slot in 0..3) slot else null
    }
}

object RunePouchItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(
        5508, 5509, 5510, 5511, 5512, 5513, 5514, 5515,
    )

    override fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        return Runecrafting.checkPouch(client, itemId)
    }
}

private class RunecraftingObjectContent : ObjectContent {
    override val objectIds: IntArray = RunecraftingData.altarObjectIds

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val altar = RunecraftingData.byObjectId(objectId) ?: return false
        return Runecrafting.start(client, altar.request)
    }
}

object RunecraftingSkillPlugin : SkillPlugin {
    override val definition =
        skillPlugin(name = "Runecrafting", skill = Skill.RUNECRAFTING) {
            val runecraftingObjects = RunecraftingObjectContent()
            bindObjectContentClick(
                preset = PolicyPreset.GATHERING,
                option = 1,
                content = runecraftingObjects,
            )
            bindItemContentClick(
                preset = PolicyPreset.GATHERING,
                option = 2,
                content = RunePouchItems,
            )
        }
}

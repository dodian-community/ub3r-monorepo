package net.dodian.uber.game.skills.runecrafting

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.skills.core.runtime.SkillingRandomEventService
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc

object RunecraftingService {
    @JvmStatic
    fun start(client: Client, request: RunecraftingRequest): Boolean {
        if (!client.contains(RunecraftingDefinitions.RUNE_ESSENCE_ID)) {
            client.send(SendMessage("You do not have any rune essence!"))
            return false
        }
        if (client.getLevel(Skill.RUNECRAFTING) < request.requiredLevel) {
            client.send(
                SendMessage(
                    "You must have ${request.requiredLevel} runecrafting to craft ${client.GetItemName(request.runeId).lowercase()}",
                ),
            )
            return false
        }

        val essenceCount = client.getInvAmt(RunecraftingDefinitions.RUNE_ESSENCE_ID)
        if (essenceCount <= 0) {
            client.send(SendMessage("You do not have any rune essence!"))
            return false
        }

        var extra = 0
        repeat(essenceCount) {
            client.deleteItem(RunecraftingDefinitions.RUNE_ESSENCE_ID, 1)
            val chance = (client.getLevel(Skill.RUNECRAFTING) + 1) / 2
            val roll = 1 + Misc.random(99)
            if (roll <= chance) {
                extra++
            }
        }

        val crafted = essenceCount + extra
        client.send(SendMessage("You craft $crafted ${client.GetItemName(request.runeId).lowercase()}s"))
        client.addItem(request.runeId, crafted)
        client.checkItemUpdate()
        val xp = request.experiencePerEssence * essenceCount
        SkillProgressionService.gainXp(client, xp, Skill.RUNECRAFTING)
        SkillingRandomEventService.trigger(client, xp)
        client.runecraftingState = RunecraftingState(lastAltarCraftAtMillis = System.currentTimeMillis())
        return true
    }
}

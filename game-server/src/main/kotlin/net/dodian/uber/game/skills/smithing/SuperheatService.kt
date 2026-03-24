package net.dodian.uber.game.skills.smithing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Ground
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendSideTab
import net.dodian.uber.game.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.skills.core.runtime.RuneCostService
import net.dodian.utilities.Misc

object SuperheatService {
    private const val NATURE_RUNE = 561
    private const val MAGIC_XP = 500

    @JvmStatic
    fun cast(client: Client, itemId: Int) {
        client.resetAction(false)
        when (itemId) {
            1781, 401, 1783 -> superheatGlass(client)
            436, 438 -> superheatRecipe(client, 2349)
            440 -> {
                if (client.playerHasItem(453, 2)) {
                    superheatRecipe(client, 2353)
                } else {
                    superheatRecipe(client, 2351)
                }
            }
            444 -> superheatRecipe(client, 2357)
            447 -> superheatRecipe(client, 2359)
            449 -> superheatRecipe(client, 2361)
            451 -> superheatRecipe(client, 2363)
            else -> {
                client.send(SendMessage("You can only use this spell on ores or glass material!"))
                client.callGfxMask(85, 100)
                client.send(SendSideTab(6))
            }
        }
    }

    private fun superheatRecipe(client: Client, barId: Int) {
        val recipe = SmithingDefinitions.findSmeltingRecipe(barId) ?: run {
            client.send(SendMessage("You can only use this spell on ores or glass material!"))
            client.callGfxMask(85, 100)
            client.send(SendSideTab(6))
            return
        }
        if (client.getLevel(Skill.SMITHING) < recipe.levelRequired) {
            client.send(SendMessage("You need level ${recipe.levelRequired} smithing to do this!"))
            return
        }
        if (!client.playerHasItem(NATURE_RUNE, 1)) {
            client.send(SendMessage("You need 1 nature runes to cast this spell!"))
            return
        }
        for (requirement in recipe.oreRequirements) {
            if (!client.playerHasItem(requirement.itemId, requirement.amount)) {
                client.send(missingRequirementMessage(client, recipe))
                return
            }
        }

        client.lastMagic = System.currentTimeMillis()
        client.requestAnim(725, 0)
        client.callGfxMask(148, 100)
        RuneCostService.consume(client, intArrayOf(NATURE_RUNE), intArrayOf(1))
        recipe.oreRequirements.forEach { requirement ->
            repeat(requirement.amount) {
                client.deleteItem(requirement.itemId, 1)
            }
        }

        val success = recipe.successChancePercent >= 100 ||
            net.dodian.utilities.Range(1, 100).value <= recipe.successChancePercent + ((client.getLevel(Skill.SMITHING) + 1) / 4)
        if (success) {
            client.addItem(recipe.barId, 1)
            SkillProgressionService.gainXp(client, recipe.experience, Skill.SMITHING)
            SkillProgressionService.gainXp(client, MAGIC_XP, Skill.MAGIC)
        } else if (recipe.failureMessage != null) {
            client.send(SendMessage(recipe.failureMessage))
            SkillProgressionService.gainXp(client, MAGIC_XP, Skill.MAGIC)
        }
        client.checkItemUpdate()
        client.send(SendSideTab(6))
    }

    private fun superheatGlass(client: Client) {
        if (!client.playerHasItem(NATURE_RUNE, 3)) {
            client.send(SendMessage("Need 3 nature runes to cast this spell on glass material!"))
            return
        }
        if (!client.playerHasItem(1783, 1) || (!client.playerHasItem(1781, 1) && !client.playerHasItem(401, 1))) {
            client.send(SendMessage("You need atleast 1 bucket of sand along with seaweed or soda ash to cast this!"))
            return
        }

        client.lastMagic = System.currentTimeMillis()
        client.requestAnim(725, 0)
        client.callGfxMask(148, 100)
        val sandCount = client.getInvAmt(1783)
        val ashCount = client.getInvAmt(1781) + client.getInvAmt(401)
        val count = sandCount.coerceAtMost(ashCount)
        var moltenCount = 0
        repeat(count) {
            client.deleteItem(1783, 1)
            client.deleteItem(if (client.playerHasItem(1781, 1)) 1781 else 401, 1)
            moltenCount++
            if (Misc.chance(100) <= 30) {
                moltenCount++
            }
        }
        RuneCostService.consume(client, intArrayOf(NATURE_RUNE), intArrayOf(3))
        repeat(moltenCount) {
            if (!client.addItem(1775, 1)) {
                Ground.addFloorItem(client, 1775, 1)
            }
        }
        client.checkItemUpdate()
        SkillProgressionService.gainXp(client, count * 40, Skill.CRAFTING)
        SkillProgressionService.gainXp(client, MAGIC_XP, Skill.MAGIC)
        client.send(SendSideTab(6))
    }

    private fun missingRequirementMessage(client: Client, recipe: SmeltingRecipe): SendMessage =
        when (recipe.barId) {
            2349 -> SendMessage("You need a tin and copper to do this!")
            2353 -> SendMessage("You need a iron ore and 2 coal to do this!")
            2359 -> SendMessage("You need a mithril ore and 3 coal to do this!")
            2361 -> SendMessage("You need a adamantite ore and 4 coal to do this!")
            2363 -> SendMessage("You need a runite ore and 6 coal to do this!")
            else -> {
                val missing = recipe.oreRequirements.firstOrNull { !client.playerHasItem(it.itemId, it.amount) }
                if (missing == null) {
                    SendMessage("You can only use this spell on ores or glass material!")
                } else {
                    SendMessage("You need ${missing.amount} ${client.GetItemName(missing.itemId).lowercase()} to do this!")
                }
            }
        }
}

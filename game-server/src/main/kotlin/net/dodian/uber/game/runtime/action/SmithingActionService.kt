package net.dodian.uber.game.runtime.action

import net.dodian.uber.game.Constants
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage

object SmithingActionService {
    private val possibleBars = intArrayOf(2349, 2351, 2353, 2359, 2361, 2363)
    private val barXp = intArrayOf(13, 25, 38, 50, 63, 75)
    private val barLevelRequired = intArrayOf(1, 15, 30, 55, 70, 85)

    @JvmStatic
    fun startSmithing(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.SMITHING,
            onStop = { player, _ -> stop(player) },
        ) {
            if (resolveSpec(player) == null) {
                return@start
            }
            player.setFocus(player.skillX, player.skillY)
            player.send(SendMessage("You start hammering the bar..."))
            player.requestAnim(0x382, 0)
            player.IsAnvil = true

            while (player.IsAnvil && player.smithing[4] > 0 && player.smithing[5] > 0) {
                val spec = resolveSpec(player) ?: return@start
                wait(spec.delayTicks)
                if (!isActive()) {
                    return@start
                }
                if (!performSmith(player, spec)) {
                    return@start
                }
                player.smithing[5]--
                if (player.smithing[5] <= 0) {
                    return@start
                }
            }
        }
    }

    private fun resolveSpec(player: Client): SmithingSpec? {
        if (player.isBusy()) {
            player.send(SendMessage("You are currently busy to be smithing!"))
            stop(player)
            return null
        }
        if (!player.GoodDistance(player.skillX, player.skillY, player.position.x, player.position.y, 1)) {
            stop(player)
            return null
        }
        if (!player.IsItemInBag(2347)) {
            player.send(SendMessage("You need a ${player.GetItemName(2347)} to hammer bars."))
            stop(player)
            return null
        }
        val targetItem = player.smithing[4]
        if (targetItem <= 0 || !player.smithCheck(targetItem)) {
            stop(player)
            return null
        }

        val smithType = player.smithing[2]
        val rowIndex = smithType - 1
        if (rowIndex !in Constants.smithing_frame.indices) {
            stop(player)
            return null
        }

        val barId =
            if (smithType >= 4) {
                2349 + ((smithType + 1) * 2)
            } else {
                2349 + ((smithType - 1) * 2)
            }

        var barsRequired = 0
        var outputCount = 1
        var levelRequired = player.smithing[1]
        for (entry in Constants.smithing_frame[rowIndex]) {
            if (entry[0] != targetItem) {
                continue
            }
            barsRequired = entry[3]
            if (levelRequired == 0) {
                levelRequired = entry[2]
            }
            outputCount = entry[1]
            break
        }

        if (barsRequired <= 0 || levelRequired <= 0) {
            stop(player)
            return null
        }

        var xpBase = 0
        outer@ for (i in Constants.smithing_frame.indices) {
            for (entry in Constants.smithing_frame[i]) {
                if (entry[0] == targetItem) {
                    if (!player.AreXItemsInBag(possibleBars[i], entry[3])) {
                        player.send(SendMessage("You are missing bars needed to smith this!"))
                        stop(player)
                        return null
                    }
                    xpBase = barXp[i]
                    break@outer
                }
            }
        }

        if (player.getLevel(Skill.SMITHING) < levelRequired) {
            player.send(SendMessage("You need $levelRequired Smithing to smith a ${player.GetItemName(targetItem)}."))
            stop(player)
            return null
        }

        if (!player.AreXItemsInBag(barId, barsRequired)) {
            player.send(
                SendMessage(
                    "You need $barsRequired ${player.GetItemName(barId)} to smith a ${player.GetItemName(targetItem)}.",
                ),
            )
            player.rerequestAnim()
            stop(player)
            return null
        }

        val barIndex = player.CheckSmithing(player.smithing[3]) - 1
        val requiredLevel = barLevelRequired.getOrElse(barIndex) { 1 }
        val diff = player.getLevel(Skill.SMITHING) - requiredLevel
        val delayTicks = 5 - when {
            diff >= 14 -> 2
            diff >= 7 -> 1
            else -> 0
        }

        return SmithingSpec(
            targetItem = targetItem,
            barId = barId,
            barsRequired = barsRequired,
            outputCount = outputCount,
            levelRequired = levelRequired,
            delayTicks = delayTicks.coerceAtLeast(1),
            experience = xpBase * barsRequired * 30,
        )
    }

    private fun performSmith(player: Client, spec: SmithingSpec): Boolean {
        repeat(spec.barsRequired) {
            player.deleteItem(spec.barId, 1)
        }
        player.giveExperience(spec.experience, Skill.SMITHING)
        player.addItem(spec.targetItem, spec.outputCount)
        player.checkItemUpdate()
        player.send(SendMessage("You smith a ${player.GetItemName(spec.targetItem)}"))
        player.requestAnim(0x382, 0)
        player.triggerRandom(spec.experience)
        return true
    }

    private fun stop(player: Client) {
        if (player.IsAnvil) {
            player.resetSM()
            player.send(RemoveInterfaces())
        }
    }

    private data class SmithingSpec(
        val targetItem: Int,
        val barId: Int,
        val barsRequired: Int,
        val outputCount: Int,
        val levelRequired: Int,
        val delayTicks: Int,
        val experience: Int,
    )
}

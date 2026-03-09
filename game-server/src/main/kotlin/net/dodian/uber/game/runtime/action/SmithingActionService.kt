package net.dodian.uber.game.runtime.action

import net.dodian.uber.game.skills.smithing.SmithingDefinitions
import net.dodian.uber.game.skills.smithing.SmithingRequest
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage

object SmithingActionService {
    private val possibleBars = intArrayOf(2349, 2351, 2353, 2359, 2361, 2363)
    private val barXp = intArrayOf(13, 25, 38, 50, 63, 75)
    private val barLevelRequired = intArrayOf(1, 15, 30, 55, 70, 85)

    @JvmStatic
    fun startSmithing(client: Client, request: SmithingRequest) {
        client.setActiveSmithingSelection(
            net.dodian.uber.game.skills.smithing.ActiveSmithingSelection(
                request.tierId,
                request.barId,
                request.anvilX,
                request.anvilY,
            ),
        )
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.SMITHING,
            onStop = { player, _ -> stop(player) },
        ) {
            if (resolveSpec(player, request) == null) {
                return@start
            }
            player.setFocus(request.anvilX, request.anvilY)
            player.send(SendMessage("You start hammering the bar..."))
            player.requestAnim(0x382, 0)
            var remaining = request.amount

            while (remaining > 0) {
                val spec = resolveSpec(player, request) ?: return@start
                wait(spec.delayTicks)
                if (!isActive()) {
                    return@start
                }
                if (!performSmith(player, spec)) {
                    return@start
                }
                remaining--
                if (remaining <= 0) {
                    return@start
                }
            }
        }
    }

    private fun resolveSpec(player: Client, request: SmithingRequest): SmithingSpec? {
        if (player.isBusy()) {
            player.send(SendMessage("You are currently busy to be smithing!"))
            stop(player)
            return null
        }
        if (!player.GoodDistance(request.anvilX, request.anvilY, player.position.x, player.position.y, 1)) {
            stop(player)
            return null
        }
        if (!player.IsItemInBag(2347)) {
            player.send(SendMessage("You need a ${player.GetItemName(2347)} to hammer bars."))
            stop(player)
            return null
        }
        if (player.getLevel(Skill.SMITHING) < request.product.levelRequired) {
            player.send(SendMessage("You need ${request.product.levelRequired} Smithing to smith a ${player.GetItemName(request.product.itemId)}."))
            stop(player)
            return null
        }
        if (!player.AreXItemsInBag(request.barId, request.product.barsRequired)) {
            player.send(
                SendMessage(
                    "You need ${request.product.barsRequired} ${player.GetItemName(request.barId)} to smith a ${player.GetItemName(request.product.itemId)}.",
                ),
            )
            player.rerequestAnim()
            stop(player)
            return null
        }

        val tierIndex = SmithingDefinitions.findSmithingTierByTypeId(request.tierId)?.let { it.typeId - 1 } ?: 0
        val xpBase = barXp.getOrElse(tierIndex) { 13 }
        val requiredLevel = barLevelRequired.getOrElse(tierIndex) { 1 }
        val diff = player.getLevel(Skill.SMITHING) - requiredLevel
        val delayTicks = 5 - when {
            diff >= 14 -> 2
            diff >= 7 -> 1
            else -> 0
        }

        return SmithingSpec(
            targetItem = request.product.itemId,
            barId = request.barId,
            barsRequired = request.product.barsRequired,
            outputCount = request.product.outputAmount,
            delayTicks = delayTicks.coerceAtLeast(1),
            experience = xpBase * request.product.barsRequired * 30,
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
        player.clearActiveSmithingSelection()
        player.send(RemoveInterfaces())
        player.rerequestAnim()
    }

    private data class SmithingSpec(
        val targetItem: Int,
        val barId: Int,
        val barsRequired: Int,
        val outputCount: Int,
        val delayTicks: Int,
        val experience: Int,
    )
}

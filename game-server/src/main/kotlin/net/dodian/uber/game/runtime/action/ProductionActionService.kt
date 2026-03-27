package net.dodian.uber.game.runtime.action

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.skills.core.runtime.sendFilterMessage
import net.dodian.uber.game.skills.core.runtime.ProductionSpec
import net.dodian.uber.game.skills.core.runtime.ProductionTask
import net.dodian.uber.game.skills.core.runtime.RuneCostService
import net.dodian.uber.game.skills.core.runtime.SkillingRandomEventService

object ProductionActionService {
    @JvmStatic
    @Deprecated(
        message = "Use ContentActions.queueProductionSelection for content-facing calls.",
        replaceWith = ReplaceWith(
            expression = "ContentActions.queueProductionSelection(client, request, interfaceModelZoom, titleLineBreaks)",
            imports = arrayOf("net.dodian.uber.game.runtime.api.content.ContentActions"),
        ),
    )
    fun queueSelection(
        client: Client,
        request: ProductionRequest,
        interfaceModelZoom: Int = if (request.skillId == Skill.HERBLORE.id) 150 else 190,
        titleLineBreaks: Int = if (request.skillId == Skill.HERBLORE.id) 4 else 5,
    ) {
        client.resetAction()
        client.setPendingProductionSelection(PendingProductionSelection(request, interfaceModelZoom, titleLineBreaks))
        client.sendFrame246(1746, interfaceModelZoom, request.productId)
        client.send(SendString("\\n".repeat(titleLineBreaks) + client.GetItemName(request.productId), 2799))
        client.sendFrame164(4429)
    }

    @JvmStatic
    @Deprecated(
        message = "Use ContentActions.startPendingProduction for content-facing calls.",
        replaceWith = ReplaceWith(
            expression = "ContentActions.startPendingProduction(client, cycleCount)",
            imports = arrayOf("net.dodian.uber.game.runtime.api.content.ContentActions"),
        ),
    )
    fun startPending(client: Client, cycleCount: Int): Boolean {
        val selection = client.getPendingProductionSelection() ?: return false
        client.send(RemoveInterfaces())
        client.clearPendingProductionSelection()
        return start(client, selection.request, cycleCount)
    }

    @JvmStatic
    @Deprecated(
        message = "Use ContentActions.startProduction for content-facing calls.",
        replaceWith = ReplaceWith(
            expression = "ContentActions.startProduction(client, request, cycleCount)",
            imports = arrayOf("net.dodian.uber.game.runtime.api.content.ContentActions"),
        ),
    )
    fun start(
        client: Client,
        request: ProductionRequest,
        cycleCount: Int,
    ): Boolean {
        if (cycleCount < 1) {
            client.clearActiveProductionSelection()
            return false
        }
        client.setActiveProductionSelection(ActiveProductionSelection(request, cycleCount))
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.PRODUCTION,
        ) {
            while (true) {
                val active = player.getActiveProductionSelection() ?: return@start
                if (active.remainingCycles <= 0) return@start
                if (!isActive()) return@start
                if (!executeCycle(player)) return@start
                val updated = player.getActiveProductionSelection() ?: return@start
                if (updated.remainingCycles <= 0) return@start
                wait(updated.request.tickDelay.coerceAtLeast(1))
            }
        }
        return true
    }

    @JvmStatic
    fun executeCycle(client: Client): Boolean {
        val active = client.getActiveProductionSelection()
        if (active == null || active.remainingCycles < 1) {
            client.resetAction()
            return false
        }
        if (client.isBusy) {
            client.sendFilterMessage("You are currently busy to be doing this!")
            return false
        }
        val request = active.request
        val itemOne = request.primaryItemId
        val itemTwo = request.secondaryItemId
        val itemMake = request.productId
        var amount = request.amountPerCycle

        if (request.mode == ProductionMode.SUPER_COMBAT) {
            if (!client.playerHasItem(itemOne) || (!client.playerHasItem(111) && !client.playerHasItem(269)) || !client.playerHasItem(2440) || !client.playerHasItem(2442)) {
                client.resetAction()
                val text =
                    if (!client.playerHasItem(111) && !client.playerHasItem(269)) client.GetItemName(269).lowercase()
                    else if (!client.playerHasItem(itemOne)) client.GetItemName(2436).lowercase()
                    else if (!client.playerHasItem(2440)) client.GetItemName(2440).lowercase()
                    else client.GetItemName(2442).lowercase()
                client.sendFilterMessage("You do not have anymore $text.")
                return false
            }
            client.deleteItem(itemOne, amount)
            client.deleteItem(if (!client.playerHasItem(269)) 111 else 269, amount)
            client.deleteItem(2440, amount)
            client.deleteItem(2442, amount)
            client.addItem(itemMake, amount)
        } else if (request.mode == ProductionMode.OVERLOAD) {
            if (!client.playerHasItem(itemOne) || !client.playerHasItem(2444) || !client.playerHasItem(12695)) {
                client.resetAction()
                val text =
                    if (!client.playerHasItem(itemOne)) client.GetItemName(itemOne).lowercase()
                    else if (!client.playerHasItem(2444)) client.GetItemName(2444).lowercase()
                    else client.GetItemName(12695).lowercase()
                client.sendFilterMessage("You do not have anymore $text.")
                return false
            }
            client.deleteItem(itemOne, amount)
            client.deleteItem(2444, amount)
            client.deleteItem(12695, amount)
            client.addItem(itemMake, amount)
        } else if (request.mode == ProductionMode.CHARGED_ORB) {
            if (!client.playerHasItem(itemOne) || !client.playerHasItem(itemTwo, 3)) {
                client.resetAction()
                client.sendFilterMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk.")
                return false
            }
            client.callGfxMask(if (itemMake == 569) 152 else 149 + ((itemMake - 571) / 2), 100)
            client.deleteItem(itemOne, amount)
            RuneCostService.consume(client, intArrayOf(itemTwo), intArrayOf(3))
            client.addItem(itemMake, amount)
        } else if (request.mode == ProductionMode.MOLTEN_GLASS) {
            if (!client.playerHasItem(itemOne) || !client.playerHasItem(itemTwo)) {
                client.resetAction()
                client.sendFilterMessage("You need one bucket of sand and one soda ash")
                return false
            }
            client.deleteItem(itemOne, amount)
            client.addItem(1925, amount)
            client.deleteItem(itemTwo, amount)
            client.addItem(itemMake, amount)
        } else {
            if (!client.playerHasItem(itemOne) || (itemTwo != -1 && !client.playerHasItem(itemTwo))) {
                client.resetAction()
                val missingName = if (!client.playerHasItem(itemOne)) client.GetItemName(itemOne).lowercase() else client.GetItemName(itemTwo).lowercase()
                client.sendFilterMessage("You do not have anymore $missingName.")
                return false
            }
            if (client.getInvAmt(itemOne) < amount || (itemTwo != -1 && client.getInvAmt(itemTwo) < amount)) {
                amount = if (itemTwo == -1) client.getInvAmt(itemOne) else minOf(client.getInvAmt(itemOne), client.getInvAmt(itemTwo))
            }
            val spec =
                ProductionSpec(
                    actionName = "Production",
                    skillId = request.skillId,
                    productId = itemMake,
                    amountPerCycle = amount,
                    primaryItemId = itemOne,
                    secondaryItemId = itemTwo,
                    experiencePerUnit = request.experiencePerUnit,
                    animationId = request.animationId,
                    tickDelay = request.tickDelay,
                )
            if (!ProductionCycleTask(client, spec).runCycle()) {
                client.resetAction()
                return false
            }
        }

        client.checkItemUpdate()
        if (request.animationId != -1) {
            client.requestAnim(request.animationId, 0)
        }
        val xp = request.experiencePerUnit * amount
        val skill = Skill.getSkill(request.skillId)
        if (skill != null) {
            SkillProgressionService.gainXp(client, xp, skill)
        }
        SkillingRandomEventService.trigger(client, xp)
        client.setActiveProductionSelection(active.copy(remainingCycles = active.remainingCycles - 1))
        if (request.completionMessage.isNotEmpty()) {
            client.sendFilterMessage(request.completionMessage)
        }
        return true
    }

    private class ProductionCycleTask(
        client: Client,
        spec: ProductionSpec,
    ) : ProductionTask(client, spec) {
        override fun performCycle(): Boolean {
            if (spec.amountPerCycle <= 0) {
                return false
            }
            client.deleteItem(spec.primaryItemId, spec.amountPerCycle)
            if (spec.secondaryItemId != -1) {
                client.deleteItem(spec.secondaryItemId, spec.amountPerCycle)
            }
            client.addItem(spec.productId, spec.amountPerCycle)
            return true
        }
    }
}

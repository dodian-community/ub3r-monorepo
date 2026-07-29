package net.dodian.uber.skills.smithing

import net.dodian.uber.game.api.content.ContentAttributeKey
import net.dodian.uber.game.api.plugin.skills.SkillActionHandle
import net.dodian.uber.game.api.plugin.skills.SkillItemGridEntry
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.productionAction

/** Plugin-owned anvil registry, selection state, and legacy interface presentation. */
object SmithingAnvilService {
    private const val MODULE_ID = "skill.smithing"
    private const val HAMMER_ITEM_ID = 2347
    private const val ANVIL_INTERFACE_ID = 994
    private val frameIds = intArrayOf(1119, 1120, 1121, 1122, 1123)
    private val possibleBars = intArrayOf(2349, 2351, 2353, 2359, 2361, 2363)
    private val barXp = intArrayOf(13, 25, 38, 50, 63, 75)
    private val barLevelRequired = intArrayOf(1, 15, 30, 55, 70, 85)
    private val activeSelectionKey = ContentAttributeKey<ActiveSmithingSelection>(MODULE_ID, "activeAnvilSelection")
    private val activeActionKey = ContentAttributeKey<SkillActionHandle>(MODULE_ID, "activeAnvilAction")
    private val remainingKey = ContentAttributeKey<Int>(MODULE_ID, "anvilRemaining")

    fun resolveTierId(barId: Int): Int = SmithingData.findSmithingTierByBar(barId)?.typeId ?: -1

    fun openForBar(player: SkillPlayer, barId: Int, anvilX: Int, anvilY: Int): Boolean {
        if (!player.inventory.contains(HAMMER_ITEM_ID)) {
            player.ui.message("You need a ${player.inventory.itemName(HAMMER_ITEM_ID)} to hammer bars.")
            return false
        }
        val tier = SmithingData.findSmithingTierByBar(barId)
        if (tier == null) {
            player.ui.message("You cannot smith this item.")
            return false
        }
        player.attributes.put(activeSelectionKey, ActiveSmithingSelection(tier.typeId, tier.barId, anvilX, anvilY))
        sendSmithingUi(player, tier)
        return true
    }

    fun canSmithProduct(itemId: Int): Boolean = SmithingData.findTierForProduct(itemId) != null

    fun resolveRequest(
        player: SkillPlayer,
        interfaceId: Int,
        itemId: Int,
        slot: Int,
        amount: Int,
        fallbackAnchorX: Int,
        fallbackAnchorY: Int,
    ): SmithingRequest? {
        val selection = player.attributes.get(activeSelectionKey)
        var tier = selection?.let { SmithingData.findSmithingTierByTypeId(it.tierId) }
        var product = tier?.let { resolveProductFromInterface(it, interfaceId, itemId, slot) }
        if (product == null) {
            resolveAcrossTiersByItemId(itemId)?.let {
                tier = it.tier
                product = it.product
            }
        }
        val hasAnchor = fallbackAnchorX >= 0 && fallbackAnchorY >= 0
        if (tier == null || product == null || (selection == null && !hasAnchor)) {
            player.ui.message("Illegal smithing!")
            return null
        }

        val resolvedTier = tier ?: return null
        val resolvedProduct = product ?: return null
        val anvilX = selection?.anvilX ?: fallbackAnchorX
        val anvilY = selection?.anvilY ?: fallbackAnchorY
        if (selection == null || selection.tierId != resolvedTier.typeId ||
            selection.anvilX != anvilX || selection.anvilY != anvilY
        ) {
            player.attributes.put(
                activeSelectionKey,
                ActiveSmithingSelection(resolvedTier.typeId, resolvedTier.barId, anvilX, anvilY),
            )
        }
        player.ui.close()
        return SmithingRequest(
            tierId = resolvedTier.typeId,
            product = resolvedProduct,
            amount = amount.coerceAtLeast(1),
            barId = resolvedTier.barId,
            anvilX = anvilX,
            anvilY = anvilY,
        )
    }

    fun activeSelection(player: SkillPlayer): ActiveSmithingSelection? =
        player.attributes.get(activeSelectionKey)

    fun start(player: SkillPlayer, request: SmithingRequest): Boolean {
        stopAction(player, ActionStopReason.USER_INTERRUPT, clearSelection = false)
        player.attributes.put(remainingKey, request.amount.coerceAtLeast(1))
        var initialDelayPending = true
        val handle = productionAction("smithing.anvil") {
            delay { smithingDelayTicks(this, request.tierId) }
            onStart {
                world.face(SkillPosition(request.anvilX, request.anvilY, world.position.z))
                ui.message("You start hammering the bar...")
                actions.animate(0x382, 0)
            }
            onCycleSignal {
                if (initialDelayPending) {
                    initialDelayPending = false
                    return@onCycleSignal CycleSignal.continueWithoutSuccess()
                }
                performSmithingCycle(this, request)
            }
            onStop {
                attributes.remove(activeActionKey)
                attributes.remove(remainingKey)
                attributes.remove(activeSelectionKey)
                ui.close()
                actions.resetAnimation()
            }
        }.start(player)
        if (handle == null) {
            player.attributes.remove(remainingKey)
            return false
        }
        player.attributes.put(activeActionKey, handle)
        return true
    }

    fun stopAction(
        player: SkillPlayer,
        reason: ActionStopReason = ActionStopReason.USER_INTERRUPT,
        clearSelection: Boolean = true,
    ) {
        player.attributes.get(activeActionKey)?.cancel(reason)
        player.attributes.remove(activeActionKey)
        player.attributes.remove(remainingKey)
        if (clearSelection) player.attributes.remove(activeSelectionKey)
    }

    fun firstBarInInventory(player: SkillPlayer): Int =
        possibleBars.firstOrNull { player.inventory.contains(it) } ?: -1

    fun clear(player: SkillPlayer) {
        stopAction(player)
    }

    private fun performSmithingCycle(player: SkillPlayer, request: SmithingRequest): CycleSignal {
        val remaining = player.attributes.get(remainingKey)
            ?: return CycleSignal.stop(ActionStopReason.INVALID_TARGET)
        if (remaining <= 0) return CycleSignal.stop(ActionStopReason.COMPLETED)
        if (player.actions.busy) {
            player.ui.message("You are currently busy to be smithing!")
            return CycleSignal.stop(ActionStopReason.BUSY)
        }
        if (player.world.distanceTo(request.anvilX, request.anvilY) > 1) {
            return CycleSignal.stop(ActionStopReason.MOVED_AWAY)
        }
        if (!player.inventory.contains(HAMMER_ITEM_ID)) {
            player.ui.message("You need a ${player.inventory.itemName(HAMMER_ITEM_ID)} to hammer bars.")
            return CycleSignal.stop(ActionStopReason.MISSING_TOOL)
        }
        if (player.skills.current(Skill.SMITHING) < request.product.levelRequired) {
            player.ui.message(
                "You need ${request.product.levelRequired} Smithing to smith a " +
                    "${player.inventory.itemName(request.product.itemId)}.",
            )
            return CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)
        }
        if (!player.inventory.contains(request.barId, request.product.barsRequired)) {
            player.ui.message(
                "You need ${request.product.barsRequired} ${player.inventory.itemName(request.barId)} to smith a " +
                    "${player.inventory.itemName(request.product.itemId)}.",
            )
            return CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)
        }

        val experience = barXp.getOrElse(request.tierId - 1) { 13 } * request.product.barsRequired * 30
        val committed = player.inventory.transaction {
            remove(request.barId, request.product.barsRequired)
            add(request.product.itemId, request.product.outputAmount)
        }
        if (!committed) return CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)

        player.skills.gainXp(experience, Skill.SMITHING)
        player.ui.message("You smith a ${player.inventory.itemName(request.product.itemId)}")
        player.actions.animate(0x382, 0)
        player.actions.triggerRandomEvent(experience)
        val nextRemaining = remaining - 1
        player.attributes.put(remainingKey, nextRemaining)
        return if (nextRemaining <= 0) CycleSignal.completeSuccess() else CycleSignal.success()
    }

    private fun smithingDelayTicks(player: SkillPlayer, tierId: Int): Int {
        val requiredLevel = barLevelRequired.getOrElse(tierId - 1) { 1 }
        val difference = player.skills.current(Skill.SMITHING) - requiredLevel
        return (5 - when {
            difference >= 14 -> 2
            difference >= 7 -> 1
            else -> 0
        }).coerceAtLeast(1)
    }

    private fun resolveProductFromInterface(
        tier: SmithingTier,
        interfaceId: Int,
        itemId: Int,
        slot: Int,
    ): SmithingProduct? {
        resolveProductByItemId(tier, itemId)?.let { return it }
        if (slot >= 0) {
            tier.products.getOrNull(slot)?.let { return it }
            if (slot > 0) tier.products.getOrNull(slot - 1)?.let { return it }
        }
        if (interfaceId in frameIds && slot >= 0) {
            val displayItems = SmithingData.displayItemsForFrame(tier, interfaceId)
            val fallbackItemIds = listOfNotNull(
                displayItems.getOrNull(slot)?.itemId,
                if (slot > 0) displayItems.getOrNull(slot - 1)?.itemId else null,
            )
            fallbackItemIds.filter { it > 0 }.forEach { fallbackItemId ->
                tier.products.firstOrNull { it.itemId == fallbackItemId }?.let { return it }
            }
        }
        return null
    }

    private fun resolveProductByItemId(tier: SmithingTier, itemId: Int): SmithingProduct? {
        tier.products.firstOrNull { it.itemId == itemId }?.let { return it }
        if (itemId > 0) {
            tier.products.firstOrNull { it.itemId == itemId + 1 }?.let { return it }
            tier.products.firstOrNull { it.itemId == itemId - 1 }?.let { return it }
        }
        return null
    }

    private fun resolveAcrossTiersByItemId(itemId: Int): ResolvedSmithingSelection? {
        SmithingData.smithingTiers.forEach { candidateTier ->
            resolveProductByItemId(candidateTier, itemId)?.let {
                return ResolvedSmithingSelection(candidateTier, it)
            }
        }
        return null
    }

    private fun sendSmithingUi(player: SkillPlayer, tier: SmithingTier) {
        clearSpecialLines(player)
        val barNamePrefix = "${tier.displayName} "
        tier.products.take(22).forEach { product ->
            val color = if (player.inventory.contains(tier.barId, product.barsRequired)) "@gre@" else "@red@"
            val barWord = if (product.barsRequired > 1) "bars" else "bar"
            player.ui.string("$color${product.barsRequired}$barWord", product.barCountLineId)
            val itemName = player.inventory.itemName(product.itemId)
            val stripped = if (itemName.startsWith(barNamePrefix)) itemName.removePrefix(barNamePrefix) else itemName
            val nameColor = if (player.skills.current(Skill.SMITHING) >= product.levelRequired) "@whi@" else "@bla@"
            player.ui.string(nameColor + stripped, product.itemNameLineId)
        }
        frameIds.forEach { frameId ->
            player.ui.itemGrid(
                frameId,
                SmithingData.displayItemsForFrame(tier, frameId).map {
                    SkillItemGridEntry(it.itemId, it.amount)
                },
            )
        }
        tier.products.getOrNull(22)?.takeIf { tier.typeId <= 3 }?.let { special ->
            val specialName = player.inventory.itemName(special.itemId).removePrefix("${tier.displayName} ")
            val specialColor = if (player.skills.current(Skill.SMITHING) >= special.levelRequired) "@whi@" else "@bla@"
            player.ui.string(specialColor + specialName, 11461)
        }
        player.ui.open(ANVIL_INTERFACE_ID)
    }

    private fun clearSpecialLines(player: SkillPlayer) {
        intArrayOf(1132, 1096, 11459, 11461, 1135, 1134).forEach { player.ui.string("", it) }
    }

    private data class ResolvedSmithingSelection(
        val tier: SmithingTier,
        val product: SmithingProduct,
    )
}

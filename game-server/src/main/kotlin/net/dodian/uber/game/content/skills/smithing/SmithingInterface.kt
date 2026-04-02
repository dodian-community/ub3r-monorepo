package net.dodian.uber.game.content.skills.smithing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.game.netty.listener.out.SetSmithing
import net.dodian.uber.game.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.systems.action.PlayerActionCancellationService

object SmithingInterface {
    private val frameIds = intArrayOf(1119, 1120, 1121, 1122, 1123)
    private val possibleBars = intArrayOf(2349, 2351, 2353, 2359, 2361, 2363)
    private val furnaceFrameIds = SmithingData.frameIds()

    @JvmStatic
    fun open(client: Client) {
        SmithingData.smeltingRecipes.forEachIndexed { index, recipe ->
            client.sendInterfaceModel(furnaceFrameIds[index], 150, recipe.barId)
        }
        client.sendChatboxInterface(2400)
    }

    @JvmStatic
    fun isSmeltingInterfaceFrame(interfaceId: Int): Boolean = furnaceFrameIds.contains(interfaceId)

    @JvmStatic
    fun startFromMapping(client: Client, mapping: FurnaceButtonMapping): Boolean {
        val recipe = SmithingData.findSmeltingRecipe(mapping.barId) ?: return false
        client.setPendingSmeltingBarId(recipe.barId)
        return if (mapping.amount <= 0) {
            promptPendingX(client)
        } else {
            startSmelting(client, SmeltingRequest(recipe, mapping.amount))
        }
    }

    @JvmStatic
    fun startFromInterfaceItem(client: Client, barId: Int, amount: Int): Boolean {
        val recipe = SmithingData.findSmeltingRecipe(barId) ?: return false
        client.setPendingSmeltingBarId(recipe.barId)
        return startSmelting(client, SmeltingRequest(recipe, amount))
    }

    @JvmStatic
    fun selectPendingRecipe(client: Client, barId: Int): Boolean {
        val recipe = SmithingData.findSmeltingRecipe(barId) ?: return false
        client.setPendingSmeltingBarId(recipe.barId)
        return true
    }

    @JvmStatic
    fun selectPendingRecipeFromOre(client: Client, itemId: Int): Boolean {
        val recipe = SmithingData.findSmeltingRecipeByOre(itemId) ?: return false
        client.setPendingSmeltingBarId(recipe.barId)
        return true
    }

    @JvmStatic
    fun startFromPending(client: Client, amount: Int): Boolean {
        val recipe = SmithingData.findSmeltingRecipe(client.getPendingSmeltingBarId()) ?: return false
        return startSmelting(client, SmeltingRequest(recipe, amount))
    }

    @JvmStatic
    fun promptPendingX(client: Client): Boolean {
        if (SmithingData.findSmeltingRecipe(client.getPendingSmeltingBarId()) == null) return false
        client.enterAmountId = 2
        client.send(SendFrame27())
        return true
    }

    @JvmStatic
    fun resolveTierId(barId: Int): Int = SmithingData.findSmithingTierByBar(barId)?.typeId ?: -1

    @JvmStatic
    fun openForBar(client: Client, barId: Int, anvilX: Int, anvilY: Int): Boolean {
        if (!client.hasItemInInventory(2347)) {
            client.sendMessage("You need a ${client.getItemName(2347)} to hammer bars.")
            return false
        }
        val tier = SmithingData.findSmithingTierByBar(barId)
        if (tier == null) {
            client.sendMessage("You cannot smith this item.")
            return false
        }
        client.setActiveSmithingSelection(ActiveSmithingSelection(tier.typeId, tier.barId, anvilX, anvilY))
        sendSmithingUi(client, tier)
        return true
    }

    @JvmStatic
    fun canSmithProduct(itemId: Int): Boolean = SmithingData.findTierForProduct(itemId) != null

    @JvmStatic
    fun startSmithingFromInterfaceItem(client: Client, itemId: Int, amount: Int): Boolean {
        val selection = client.getActiveSmithingSelection()
        if (selection == null) {
            client.sendMessage("Illigal Smithing !")
            return true
        }
        val tier = SmithingData.findSmithingTierByTypeId(selection.tierId)
        val product = tier?.products?.firstOrNull { it.itemId == itemId }
        if (tier == null || product == null) {
            client.sendMessage("Illigal Smithing !")
            return true
        }
        val request = SmithingRequest(
            tierId = tier.typeId,
            product = product,
            amount = amount.coerceAtLeast(1),
            barId = tier.barId,
            anvilX = selection.anvilX,
            anvilY = selection.anvilY,
        )
        client.send(RemoveInterfaces())
        net.dodian.uber.game.systems.action.SmithingActionService.startSmithing(client, request)
        return true
    }

    @JvmStatic
    fun firstBarInInventory(client: Client): Int = possibleBars.firstOrNull(client::contains) ?: -1

    @JvmStatic
    fun resetRuntimeState(client: Client) {
        client.clearActiveSmithingSelection()
        client.IsAnvil = false
    }

    private fun startSmelting(client: Client, request: SmeltingRequest): Boolean {
        if (client.getLevel(Skill.SMITHING) < request.recipe.levelRequired) {
            client.sendMessage("You need level ${request.recipe.levelRequired} smithing to do this!")
            return true
        }
        client.setPendingSmeltingBarId(request.recipe.barId)
        client.send(RemoveInterfaces())
        PlayerActionCancellationService.cancel(
            client,
            PlayerActionCancelReason.NEW_ACTION,
            fullResetAnimation = false,
            closeInterfaces = false,
            resetCompatibilityState = false,
        )
        client.setSmeltingSelection(SmeltingSelection(request.recipe, request.amount.coerceAtLeast(1)))
        Smithing.startSmelting(client)
        return true
    }

    private fun sendSmithingUi(client: Client, tier: SmithingTier) {
        clearSpecialLines(client)
        val barNamePrefix = "${tier.displayName} "
        tier.products.take(22).forEach { product ->
            val color = if (client.hasItemsInInventory(tier.barId, product.barsRequired)) "@gre@" else "@red@"
            val barWord = if (product.barsRequired > 1) "bars" else "bar"
            client.send(net.dodian.uber.game.netty.listener.out.SendString("$color${product.barsRequired}$barWord", product.barCountLineId))
            val itemName = client.getItemName(product.itemId)
            val stripped = if (itemName.startsWith(barNamePrefix)) itemName.removePrefix(barNamePrefix) else itemName
            val nameColor = if (client.getLevel(Skill.SMITHING) >= product.levelRequired) "@whi@" else "@bla@"
            client.send(net.dodian.uber.game.netty.listener.out.SendString(nameColor + stripped, product.itemNameLineId))
        }
        frameIds.forEach { frameId ->
            val items = SmithingData.displayItemsForFrame(tier, frameId)
            client.send(SetSmithing(frameId, items.map { intArrayOf(it.itemId, it.amount) }.toTypedArray()))
        }
        val special = tier.products.getOrNull(22)
        if (special != null && tier.typeId <= 3) {
            val specialName = client.getItemName(special.itemId).removePrefix("${tier.displayName} ")
            val specialColor = if (client.getLevel(Skill.SMITHING) >= special.levelRequired) "@whi@" else "@bla@"
            client.send(net.dodian.uber.game.netty.listener.out.SendString(specialColor + specialName, 11461))
        }
        client.openInterface(994)
    }

    private fun clearSpecialLines(client: Client) {
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 1132))
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 1096))
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 11459))
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 11461))
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 1135))
        client.send(net.dodian.uber.game.netty.listener.out.SendString("", 1134))
    }
}

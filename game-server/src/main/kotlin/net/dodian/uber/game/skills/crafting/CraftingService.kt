package net.dodian.uber.game.skills.crafting

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.runtime.action.SkillingActionService

object CraftingService {
    private val normalCraftIds = intArrayOf(
        1129, 1129, 1129, 1059, 1059, 1059, 1061, 1061, 1061, 1063, 1063, 1063,
        1095, 1095, 1095, 1169, 1169, 1169, 1167, 1167, 1167
    )
    private val normalCraftLevels = intArrayOf(14, 1, 7, 11, 18, 38, 9)
    private val normalCraftExp = intArrayOf(33, 18, 21, 29, 38, 52, 20)

    @JvmStatic
    fun performShaft(client: Client) {
        if (client.isBusy) {
            client.send(SendMessage("You are currently busy to be fletching!"))
            return
        }
        if (client.IsCutting || client.isFiremaking) {
            client.resetAction()
        }
        client.send(RemoveInterfaces())
        if (client.playerHasItem(1511)) {
            client.deleteItem(1511, 1)
            client.addItem(52, 15)
            client.checkItemUpdate()
            client.requestAnim(1248, 0)
            client.giveExperience(50, Skill.FLETCHING)
            client.triggerRandom(50)
        } else {
            client.resetAction()
        }
    }

    @JvmStatic
    fun startShafting(client: Client) {
        client.craftingState = CraftingState(mode = CraftingMode.SHAFTING)
        SkillingActionService.startShafting(client)
    }

    @JvmStatic
    fun performSpin(client: Client) {
        if (client.playerHasItem(1779)) {
            client.deleteItem(1779, 1)
            client.addItem(1777, 1)
            client.giveExperience(50, Skill.CRAFTING)
            client.triggerRandom(50)
        } else if (client.playerHasItem(1737)) {
            client.deleteItem(1737, 1)
            client.addItem(1759, 1)
            client.giveExperience(100, Skill.CRAFTING)
            client.triggerRandom(100)
        } else {
            client.send(SendMessage("You do not have anything to spin!"))
            client.resetAction(true)
            return
        }
        client.checkItemUpdate()
    }

    @JvmStatic
    fun startSpinning(client: Client) {
        client.craftingState = CraftingState(mode = CraftingMode.SPINNING)
        SkillingActionService.startSpinning(client)
    }

    @JvmStatic
    fun openLeatherMenu(client: Client, hideIndex: Int) {
        val hide = CraftingDefinitions.hideDefinition(hideIndex) ?: return
        client.send(SendString("What would you like to make?", 8898))
        client.send(SendString("Vambraces", 8889))
        client.send(SendString("Chaps", 8893))
        client.send(SendString("Body", 8897))
        client.sendFrame246(8883, 250, hide.glovesId)
        client.sendFrame246(8884, 250, hide.chapsId)
        client.sendFrame246(8885, 250, hide.bodyId)
        client.sendFrame164(8880)
    }

    @JvmStatic
    fun startStandardLeatherCraft(client: Client, productIndex: Int, amount: Int) {
        client.send(RemoveInterfaces())
        if (productIndex < 0 || productIndex >= normalCraftLevels.size) {
            return
        }
        val productId = normalCraftIds[productIndex * 3]
        if (client.getLevel(Skill.CRAFTING) >= normalCraftLevels[productIndex]) {
            client.craftingState =
                CraftingState(
                    mode = CraftingMode.LEATHER,
                    selectedItemId = 1741,
                    productId = productId,
                    remaining = if (amount == 10) client.getInvAmt(1741) else amount,
                    requiredLevel = normalCraftLevels[productIndex],
                    experience = normalCraftExp[productIndex] * 8,
                )
            SkillingActionService.startCrafting(client)
        } else {
            client.send(SendMessage("You need level ${normalCraftLevels[productIndex]} crafting to craft a ${client.GetItemName(productId).lowercase()}"))
            client.send(RemoveInterfaces())
        }
    }

    @JvmStatic
    fun startStandardLeatherCraft(client: Client, buttonId: Int) {
        val buttonIndex =
            intArrayOf(
                33187, 33186, 33185,
                33190, 33189, 33188,
                33193, 33192, 33191,
                33196, 33195, 33194,
                33199, 33198, 33197,
                33202, 33201, 33200,
                33205, 33204, 33203,
            ).indexOf(buttonId)
        if (buttonIndex == -1) {
            return
        }
        val amount = intArrayOf(1, 5, 10)[buttonIndex % 3]
        startStandardLeatherCraft(client, buttonIndex / 3, amount)
    }

    @JvmStatic
    fun startHideCraft(client: Client, productGroup: Int, amount: Int) {
        val hide = CraftingDefinitions.hideDefinition(client.cIndex) ?: run {
            client.send(SendMessage("Can't make this??"))
            return
        }
        val selectedItemId = hide.itemId
        val requestedAmount = if (amount == 27) client.getInvAmt(selectedItemId) else amount
        val experience = hide.experience * 8

        val required: Int
        val productId: Int
        if (productGroup == 0) {
            required = hide.glovesLevel
            productId = hide.glovesId
        } else if (productGroup == 1) {
            required = hide.chapsLevel
            productId = hide.chapsId
        } else {
            required = hide.bodyLevel
            productId = hide.bodyId
        }

        if (required != -1 && client.getLevel(Skill.CRAFTING) >= required) {
            client.craftingState =
                CraftingState(
                    mode = CraftingMode.LEATHER,
                    selectedItemId = selectedItemId,
                    productId = productId,
                    remaining = requestedAmount,
                    requiredLevel = required,
                    experience = experience,
                )
            client.send(RemoveInterfaces())
            SkillingActionService.startCrafting(client)
            return
        }
        if (required >= 0 && productId != -1) {
            client.send(SendMessage("You need level $required crafting to craft a ${client.GetItemName(productId).lowercase()}"))
            client.send(RemoveInterfaces())
            return
        }
        client.send(SendMessage("Can't make this??"))
    }

    @JvmStatic
    fun startHideCraft(client: Client, buttonId: Int) {
        val buttonIndex = intArrayOf(34185, 34184, 34183, 34182, 34189, 34188, 34187, 34186, 34193, 34192, 34191, 34190).indexOf(buttonId)
        if (buttonIndex == -1) {
            return
        }
        val amount = intArrayOf(1, 5, 10, 27)[buttonIndex % 4]
        startHideCraft(client, buttonIndex / 4, amount)
    }

    @JvmStatic
    fun performCraft(client: Client) {
        val state = client.craftingState ?: run {
            client.resetAction(true)
            return
        }
        if (state.mode != CraftingMode.LEATHER) {
            client.resetAction(true)
            return
        }
        if (client.getLevel(Skill.CRAFTING) < state.requiredLevel) {
            client.send(SendMessage("You need ${state.requiredLevel} crafting to make a ${client.GetItemName(state.productId).lowercase()}"))
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(1733) || !client.playerHasItem(1734) || !client.playerHasItem(state.selectedItemId, 1)) {
            client.send(
                SendMessage(
                    if (!client.playerHasItem(1733)) "You need a needle to craft!"
                    else if (!client.playerHasItem(1734)) "You have run out of thread!"
                    else "You have run out of ${client.GetItemName(state.selectedItemId).lowercase()}!"
                )
            )
            client.resetAction(true)
            return
        }
        if (state.remaining <= 0) {
            client.resetAction(true)
            return
        }
        client.requestAnim(1249, 0)
        client.deleteItem(state.selectedItemId, 1)
        client.deleteItem(1734, 1)
        client.send(SendMessage("You crafted a ${client.GetItemName(state.productId).lowercase()}"))
        client.addItem(state.productId, 1)
        client.checkItemUpdate()
        client.giveExperience(state.experience, Skill.CRAFTING)
        val updated = state.copy(remaining = state.remaining - 1)
        client.craftingState = updated
        if (updated.remaining < 1) {
            client.resetAction(true)
        }
        client.triggerRandom(state.experience)
    }
}

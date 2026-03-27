package net.dodian.uber.game.content.skills.crafting

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.content.skills.core.runtime.SkillingRandomEventService
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.systems.action.SkillingActionService

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
            client.sendMessage("You are currently busy to be fletching!")
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
            client.performAnimation(1248, 0)
            SkillProgressionService.gainXp(client, 50, Skill.FLETCHING)
            SkillingRandomEventService.trigger(client, 50)
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
            SkillProgressionService.gainXp(client, 50, Skill.CRAFTING)
            SkillingRandomEventService.trigger(client, 50)
        } else if (client.playerHasItem(1737)) {
            client.deleteItem(1737, 1)
            client.addItem(1759, 1)
            SkillProgressionService.gainXp(client, 100, Skill.CRAFTING)
            SkillingRandomEventService.trigger(client, 100)
        } else {
            client.sendMessage("You do not have anything to spin!")
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
    fun spinDelayMs(client: Client): Long {
        val craftingLevel = client.getLevel(Skill.CRAFTING)
        return when {
            craftingLevel >= 70 -> 600L
            craftingLevel >= 40 -> 1200L
            else -> 1800L
        }
    }

    @JvmStatic
    fun openLeatherMenu(client: Client, hideIndex: Int) {
        val hide = CraftingDefinitions.hideDefinition(hideIndex) ?: return
        client.sendString("What would you like to make?", 8898)
        client.sendString("Vambraces", 8889)
        client.sendString("Chaps", 8893)
        client.sendString("Body", 8897)
        client.sendInterfaceModel(8883, 250, hide.glovesId)
        client.sendInterfaceModel(8884, 250, hide.chapsId)
        client.sendInterfaceModel(8885, 250, hide.bodyId)
        client.sendChatboxInterface(8880)
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
            client.sendMessage("You need level ${normalCraftLevels[productIndex]} crafting to craft a ${client.getItemName(productId).lowercase()}")
            client.send(RemoveInterfaces())
        }
    }

    @JvmStatic
    fun startHideCraft(client: Client, productGroup: Int, amount: Int) {
        val hide = CraftingDefinitions.hideDefinition(client.cIndex) ?: run {
            client.sendMessage("Can't make this??")
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
            client.sendMessage("You need level $required crafting to craft a ${client.getItemName(productId).lowercase()}")
            client.send(RemoveInterfaces())
            return
        }
        client.sendMessage("Can't make this??")
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
            client.sendMessage("You need ${state.requiredLevel} crafting to make a ${client.getItemName(state.productId).lowercase()}")
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(1733) || !client.playerHasItem(1734) || !client.playerHasItem(state.selectedItemId, 1)) {
            client.send(
                SendMessage(
                    if (!client.playerHasItem(1733)) "You need a needle to craft!"
                    else if (!client.playerHasItem(1734)) "You have run out of thread!"
                    else "You have run out of ${client.getItemName(state.selectedItemId).lowercase()}!"
                )
            )
            client.resetAction(true)
            return
        }
        if (state.remaining <= 0) {
            client.resetAction(true)
            return
        }
        client.performAnimation(1249, 0)
        client.deleteItem(state.selectedItemId, 1)
        client.deleteItem(1734, 1)
        client.sendMessage("You crafted a ${client.getItemName(state.productId).lowercase()}")
        client.addItem(state.productId, 1)
        client.checkItemUpdate()
        SkillProgressionService.gainXp(client, state.experience, Skill.CRAFTING)
        val updated = state.copy(remaining = state.remaining - 1)
        client.craftingState = updated
        if (updated.remaining < 1) {
            client.resetAction(true)
        }
        SkillingRandomEventService.trigger(client, state.experience)
    }
}

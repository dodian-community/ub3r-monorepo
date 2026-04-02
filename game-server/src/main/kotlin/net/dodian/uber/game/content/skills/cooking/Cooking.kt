package net.dodian.uber.game.content.skills.cooking

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.systems.skills.SkillingRandomEventService
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.action.SkillingActionService

object Cooking {
    @JvmStatic
    fun start(client: Client, itemId: Int) {
        if (client.isBusy) {
            client.sendMessage("You are currently busy to be cooking!")
            return
        }
        val recipe = CookingData.findRecipe(itemId) ?: run {
            return
        }
        start(client, CookingRequest(itemId, CookingData.recipes.indexOf(recipe), client.getInvAmt(itemId)))
    }

    @JvmStatic
    fun start(client: Client, request: CookingRequest) {
        client.cookingState = CookingState(request.itemId, request.cookIndex, request.amount)
        SkillingActionService.startCooking(client)
    }

    @JvmStatic
    fun attempt(client: Client, itemId: Int) = start(client, itemId)

    @JvmStatic
    fun startFromEnteredAmount(client: Client, amount: Int) {
        val current = client.cookingState ?: return
        start(client, CookingRequest(current.itemId, current.cookIndex, amount))
    }

    @JvmStatic
    fun performCycle(client: Client) {
        val state = client.cookingState
        if (client.isBusy || state == null || state.remaining < 1) {
            client.resetAction(true)
            return
        }
        val cookIndex = state.cookIndex
        val itemId = state.itemId
        val recipe = CookingData.recipeByIndex(cookIndex) ?: run {
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(itemId)) {
            client.sendMessage("You are out of fish")
            client.resetAction(true)
            return
        }
        if (client.getLevel(Skill.COOKING) < recipe.requiredLevel) {
            client.sendMessage("You need ${recipe.requiredLevel} cooking to cook the ${Server.itemManager.getName(itemId).lowercase()}.")
            client.resetAction(true)
            return
        }

        var ran = recipe.burnRollBase - client.getLevel(Skill.COOKING)
        if (client.equipment[Equipment.Slot.HANDS.id] == 775) ran -= 4
        if (client.equipment[Equipment.Slot.HEAD.id] == 1949) ran -= 4
        if (client.equipment[Equipment.Slot.HEAD.id] == 1949 && client.equipment[Equipment.Slot.HANDS.id] == 775) ran -= 2
        ran = ran.coerceIn(0, 100)
        val burn = 1 + net.dodian.utilities.Utils.random(99) <= ran

        if (recipe.experience <= 0) {
            client.resetAction(true)
            return
        }
        client.cookingState = state.copy(remaining = state.remaining - 1)
        client.deleteItem(itemId, 1)
        client.setFocus(client.interactionAnchorX, client.interactionAnchorY)
        client.performAnimation(883, 0)
        if (!burn) {
            client.addItem(recipe.cookedItemId, 1)
            client.sendMessage("You cook the ${client.getItemName(itemId)}")
            ProgressionService.addXp(client, recipe.experience, Skill.COOKING)
        } else {
            client.addItem(recipe.burntItemId, 1)
            client.sendMessage("You burn the ${client.getItemName(itemId)}")
        }
        client.checkItemUpdate()
        SkillingRandomEventService.trigger(client, recipe.experience)
    }
}

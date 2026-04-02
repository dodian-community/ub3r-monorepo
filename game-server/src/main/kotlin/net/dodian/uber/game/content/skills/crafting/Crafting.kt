package net.dodian.uber.game.content.skills.crafting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.systems.skills.SkillingRandomEventService
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.netty.listener.out.SetGoldItems
import net.dodian.uber.game.systems.action.ProductionActionService
import net.dodian.uber.game.systems.action.ProductionRequest
import net.dodian.uber.game.systems.action.SkillingActionService

object Crafting {
    private val normalCraftIds = intArrayOf(
        1129, 1129, 1129, 1059, 1059, 1059, 1061, 1061, 1061, 1063, 1063, 1063,
        1095, 1095, 1095, 1169, 1169, 1169, 1167, 1167, 1167
    )
    private val normalCraftLevels = intArrayOf(14, 1, 7, 11, 18, 38, 9)
    private val normalCraftExp = intArrayOf(33, 18, 21, 29, 38, 52, 20)

    @JvmStatic
    fun open(client: Client, hideIndex: Int) = openLeatherMenu(client, hideIndex)

    @JvmStatic
    fun startLeather(client: Client, productIndex: Int, amount: Int) =
        startStandardLeatherCraft(client, productIndex, amount)

    @JvmStatic
    fun startHide(client: Client, productGroup: Int, amount: Int) = startHideCraft(client, productGroup, amount)

    @JvmStatic
    fun startTanning(client: Client, request: TanningRequest) = TanningService.start(client, request)

    @JvmStatic
    fun openTanning(client: Client) = TanningService.open(client)

    @JvmStatic
    fun startGoldJewelry(client: Client, request: GoldJewelryRequest) = GoldJewelryService.start(client, request)

    @JvmStatic
    fun openGoldJewelry(client: Client) = GoldJewelryService.openInterface(client)

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
            ProgressionService.addXp(client, 50, Skill.FLETCHING)
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
            ProgressionService.addXp(client, 50, Skill.CRAFTING)
            SkillingRandomEventService.trigger(client, 50)
        } else if (client.playerHasItem(1737)) {
            client.deleteItem(1737, 1)
            client.addItem(1759, 1)
            ProgressionService.addXp(client, 100, Skill.CRAFTING)
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
        val hide = CraftingData.hideDefinition(hideIndex) ?: return
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
        val hide = CraftingData.hideDefinition(client.cIndex) ?: run {
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
        ProgressionService.addXp(client, state.experience, Skill.CRAFTING)
        val updated = state.copy(remaining = state.remaining - 1)
        client.craftingState = updated
        if (updated.remaining < 1) {
            client.resetAction(true)
        }
        SkillingRandomEventService.trigger(client, state.experience)
    }
}

object GoldJewelryService {
    private const val GOLD_BAR_ID = 2357
    private const val GOLD_CRAFT_ANIMATION = 0x383

    @JvmStatic
    fun openInterface(client: Client) {
        var slot: Int
        for (index in 0 until GoldJewelryDefinitions.interfaceSlots.size - 1) {
            slot = GoldJewelryDefinitions.interfaceSlots[index]
            if (!client.playerHasItem(GoldJewelryDefinitions.requiredMoulds[index])) {
                client.changeInterfaceStatus(slot - 5, true)
                client.changeInterfaceStatus(slot - 1, false)
                continue
            } else {
                client.changeInterfaceStatus(slot - 5, false)
                client.changeInterfaceStatus(slot - 1, true)
            }
            val itemsToShow = IntArray(7)
            for (itemSlot in itemsToShow.indices) {
                itemsToShow[itemSlot] = resolveShownItem(client, index, itemSlot)
                if (itemSlot != 0 && itemsToShow[itemSlot] != GoldJewelryDefinitions.jewelryByGroup[index][itemSlot]) {
                    client.sendInterfaceModel(slot + 13 + itemSlot - 1 - index, GoldJewelryDefinitions.frameSizes[index], GoldJewelryDefinitions.blackFrames[index])
                } else if (itemSlot != 0) {
                    client.sendInterfaceModel(slot + 13 + itemSlot - 1 - index, GoldJewelryDefinitions.frameSizes[index], -1)
                }
            }
            client.send(SetGoldItems(slot, itemsToShow))
        }
        client.openInterface(4161)
    }

    @JvmStatic
    fun start(client: Client, request: GoldJewelryRequest): Boolean {
        val index = resolveIndex(request.interfaceId)
        val product = GoldJewelryDefinitions.product(index, request.slot) ?: return false
        if (product.requiredLevel > client.getLevel(Skill.CRAFTING)) {
            client.sendMessage("You need a crafting level of ${product.requiredLevel} to make this.")
            return true
        }
        if (!client.playerHasItem(GOLD_BAR_ID)) {
            client.sendMessage("You need at least one gold bar.")
            return true
        }
        val requiredGem = GoldJewelryDefinitions.requiredGemItems[request.slot]
        if (request.slot != 0 && !client.playerHasItem(requiredGem)) {
            client.sendMessage("You need a ${client.getItemName(requiredGem).lowercase()} to make this.")
            return true
        }

        client.send(RemoveInterfaces())
        return ProductionActionService.start(
            client,
            ProductionRequest(
                skillId = Skill.CRAFTING.id,
                productId = product.productId,
                amountPerCycle = 1,
                primaryItemId = GOLD_BAR_ID,
                secondaryItemId = if (request.slot == 0) -1 else requiredGem,
                experiencePerUnit = product.experience,
                animationId = GOLD_CRAFT_ANIMATION,
                tickDelay = 3,
                completionMessage = "You craft a ${client.getItemName(product.productId).lowercase()}",
            ),
            request.amount,
        )
    }

    @JvmStatic
    fun findStrungAmulet(amuletId: Int): Int {
        val product = GoldJewelryDefinitions.findProductByAmulet(amuletId) ?: return -1
        return GoldJewelryDefinitions.strungAmulets.getOrElse(product.slot) { -1 }
    }

    private fun resolveShownItem(client: Client, index: Int, slot: Int): Int {
        val requiredGem = GoldJewelryDefinitions.requiredGemItems[slot]
        if (!client.playerHasItem(requiredGem) && requiredGem != -1) {
            return GoldJewelryDefinitions.blanks[index][slot]
        }
        return GoldJewelryDefinitions.jewelryByGroup[index][slot]
    }

    private fun resolveIndex(interfaceId: Int): Int {
        var index = 0
        for (candidate in 0..2) {
            if (GoldJewelryDefinitions.interfaceSlots[candidate] == interfaceId) {
                index = candidate
            }
        }
        return index
    }
}

object TanningService {
    private val titleByType = mapOf(2 to "Green", 3 to "Blue", 4 to "Red", 5 to "Black")
    private val costByType = mapOf(2 to "1,000gp", 3 to "2,000gp", 4 to "5,000gp", 5 to "10,000gp")

    @JvmStatic
    fun open(client: Client) {
        client.sendString("Regular Leather", 14777)
        client.sendString("50gp", 14785)
        client.sendString("", 14781)
        client.sendString("", 14789)
        client.sendString("", 14778)
        client.sendString("", 14786)
        client.sendString("", 14782)
        client.sendString("", 14790)

        val soon = intArrayOf(14779, 14787, 14783, 14791, 14780, 14788, 14784, 14792)
        var type = 2
        for (i in soon.indices) {
            val label =
                if (i % 2 == 0) {
                    titleByType[type].orEmpty()
                } else {
                    costByType[type].orEmpty().also { type++ }
                }
            client.sendString(label, soon[i])
        }

        client.sendInterfaceModel(14769, 250, 1741)
        client.sendInterfaceModel(14773, 250, -1)
        client.sendInterfaceModel(14771, 250, 1753)
        client.sendInterfaceModel(14772, 250, 1751)
        client.sendInterfaceModel(14775, 250, 1749)
        client.sendInterfaceModel(14776, 250, 1747)
        client.openInterface(14670)
    }

    @JvmStatic
    fun start(client: Client, request: TanningRequest): Boolean {
        val definition = TanningDefinitions.find(request.hideType) ?: return false
        if (!client.playerHasItem(995, definition.coinCost)) {
            client.sendMessage("You need atleast ${definition.coinCost} coins to do this!")
            return true
        }
        var amount = request.amount
        amount = if (client.getInvAmt(995) > amount * definition.coinCost) client.getInvAmt(995) / definition.coinCost else amount
        amount = minOf(amount, client.getInvAmt(definition.hideId))
        repeat(amount.coerceAtLeast(0)) {
            client.deleteItem(definition.hideId, 1)
            client.deleteItem(995, definition.coinCost)
            client.addItem(definition.leatherId, 1)
            client.checkItemUpdate()
        }
        return true
    }
}

object ResourceFillingService {
    private data class FillEntry(val emptyItemId: Int, val filledItemId: Int, val emote: Int)

    private val waterSourceEntries =
        arrayOf(
            FillEntry(229, 227, 832),
            FillEntry(1980, 4458, 832),
            FillEntry(1935, 1937, 832),
            FillEntry(1825, 1823, 832),
            FillEntry(1827, 1823, 832),
            FillEntry(1829, 1823, 832),
            FillEntry(1831, 1823, 832),
            FillEntry(1925, 1929, 832),
            FillEntry(1923, 1921, 832),
        )

    private val cookingPotEntries = arrayOf(FillEntry(1925, 1783, 895))
    private val sinkEntries = arrayOf(FillEntry(1925, 1929, 832))

    @JvmStatic
    fun handleObjectUse(client: Client, objectId: Int): Boolean {
        val entries =
            when (objectId) {
                879, 873, 874, 6232, 12279, 14868, 20358, 25929 -> waterSourceEntries
                14890 -> cookingPotEntries
                884, 878, 6249 -> sinkEntries
                else -> return false
            }

        for (entry in entries) {
            if (!client.playerHasItem(entry.emptyItemId)) {
                continue
            }
            client.deleteItem(entry.emptyItemId, 1)
            client.addItem(entry.filledItemId, 1)
            client.checkItemUpdate()
            client.performAnimation(entry.emote, 0)
            return true
        }
        return true
    }
}

object ResourceFillingObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(
        873, 874, 878, 879, 884,
        6232, 6249,
        8689,
        12279,
        14868, 14890,
        20358,
        25929,
    )

    override fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        if (objectId == 879 || objectId == 873 || objectId == 874 || objectId == 6232 ||
            objectId == 12279 || objectId == 14868 || objectId == 20358 || objectId == 25929
        ) {
            return ResourceFillingService.handleObjectUse(client, objectId)
        }
        if (objectId == 884 || objectId == 878 || objectId == 6249) {
            return ResourceFillingService.handleObjectUse(client, objectId)
        }
        if (objectId == 14890) {
            return ResourceFillingService.handleObjectUse(client, objectId)
        }
        if (objectId == 8689 && itemId == 1925) {
            client.setFocus(position.x, position.y)
            client.deleteItem(itemId, 1)
            client.addItem(itemId + 2, 1)
            client.checkItemUpdate()
            return true
        }
        return false
    }
}

object SpinningWheelObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(14889, 14896, 14909, 25824)

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            14889, 25824 -> {
                client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                Crafting.startSpinning(client)
                true
            }
            14896, 14909 -> {
                client.addItem(1779, 1)
                client.checkItemUpdate()
                true
            }
            else -> false
        }
    }
}

package net.dodian.uber.game.content.interfaces.crafting

import net.dodian.uber.game.skills.crafting.CraftingService
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object CraftingInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        listOf(
            buttonBinding(
                interfaceId = CraftingInterfaceComponents.LEATHER_INTERFACE_ID,
                componentId = CraftingInterfaceComponents.hideCraftGroup.componentId,
                componentKey = CraftingInterfaceComponents.hideCraftGroup.componentKey,
                rawButtonIds = CraftingInterfaceComponents.hideCraftGroup.rawButtonIds,
                requiredInterfaceId = CraftingInterfaceComponents.LEATHER_INTERFACE_ID,
            ) { client, request ->
                val amount = CraftingInterfaceComponents.hideCraftGroup.amountByButton[request.rawButtonId] ?: return@buttonBinding false
                val productGroup = CraftingInterfaceComponents.hideCraftGroup.rawButtonIds.indexOf(request.rawButtonId) / 4
                CraftingService.startHideCraft(client, productGroup, amount)
                true
            },
            buttonBinding(
                interfaceId = CraftingInterfaceComponents.LEATHER_INTERFACE_ID,
                componentId = CraftingInterfaceComponents.standardCraftGroup.componentId,
                componentKey = CraftingInterfaceComponents.standardCraftGroup.componentKey,
                rawButtonIds = CraftingInterfaceComponents.standardCraftGroup.rawButtonIds,
                requiredInterfaceId = CraftingInterfaceComponents.LEATHER_INTERFACE_ID,
            ) { client, request ->
                val amount = CraftingInterfaceComponents.standardCraftGroup.amountByButton[request.rawButtonId] ?: return@buttonBinding false
                val productIndex = CraftingInterfaceComponents.standardCraftGroup.rawButtonIds.indexOf(request.rawButtonId) / 3
                CraftingService.startStandardLeatherCraft(client, productIndex, amount)
                true
            },
        )
}


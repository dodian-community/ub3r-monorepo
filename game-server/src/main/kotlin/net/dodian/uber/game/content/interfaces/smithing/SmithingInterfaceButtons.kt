package net.dodian.uber.game.content.interfaces.smithing

import net.dodian.uber.game.skills.smithing.SmeltingInterfaceService
import net.dodian.uber.game.skills.smithing.SmithingDefinitions
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object SmithingInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        buildList {
            SmithingDefinitions.smeltingButtonMappings.forEach { mapping ->
                add(
                    buttonBinding(
                        interfaceId = 2400,
                        componentId = mapping.barId,
                        componentKey = "smithing.smelting.${mapping.barId}.${mapping.amount}",
                        rawButtonIds = intArrayOf(mapping.buttonId),
                        requiredInterfaceId = 2400,
                    ) { client, _ ->
                        SmeltingInterfaceService.startFromMapping(client, mapping)
                        true
                    }
                )
            }
            SmithingDefinitions.smeltingRecipes.forEachIndexed { index, recipe ->
                val frameButtonId = SmithingDefinitions.frameIds()[index]
                add(
                    buttonBinding(
                        interfaceId = 2400,
                        componentId = frameButtonId,
                        componentKey = "smithing.smelting.recipe.${recipe.barId}",
                        rawButtonIds = intArrayOf(frameButtonId),
                        requiredInterfaceId = 2400,
                    ) { client, _ ->
                        SmeltingInterfaceService.selectPendingRecipe(client, recipe.barId)
                        true
                    }
                )
            }
        }
}

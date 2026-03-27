package net.dodian.uber.game.content.interfaces.smithing

import net.dodian.uber.game.skills.smithing.SmeltingInterfaceService
import net.dodian.uber.game.skills.smithing.SmithingDefinitions
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object SmithingInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        buildList {
            SmithingDefinitions.smeltingButtonMappings
                .groupBy { it.barId to it.amount }
                .forEach { (key, mappings) ->
                    val (barId, amount) = key
                    val rawButtonIds = mappings.map { it.buttonId }.distinct().toIntArray()
                    val primary = mappings.first()
                add(
                    buttonBinding(
                        interfaceId = 2400,
                            componentId = barId,
                            componentKey = "smithing.smelting.$barId.$amount",
                            rawButtonIds = rawButtonIds,
                        requiredInterfaceId = 2400,
                    ) { client, _ ->
                            SmeltingInterfaceService.startFromMapping(client, primary)
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

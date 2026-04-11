package net.dodian.uber.game.content.ui

import net.dodian.uber.game.content.skills.smithing.SmithingInterface as SmithingSkillInterface
import net.dodian.uber.game.content.skills.smithing.SmithingData
import net.dodian.uber.game.content.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.content.ui.buttons.buttonBinding

object SmithingInterface : InterfaceButtonContent {
    override val bindings =
        buildList {
            SmithingData.smeltingButtonMappings
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
                            SmithingSkillInterface.startFromMapping(client, primary)
                            true
                        },
                    )
                }
            SmithingData.smeltingRecipes.forEachIndexed { index, recipe ->
                val frameButtonId = SmithingData.frameIds()[index]
                add(
                    buttonBinding(
                        interfaceId = 2400,
                        componentId = frameButtonId,
                        componentKey = "smithing.smelting.recipe.${recipe.barId}",
                        rawButtonIds = intArrayOf(frameButtonId),
                        requiredInterfaceId = 2400,
                    ) { client, _ ->
                        SmithingSkillInterface.selectPendingRecipe(client, recipe.barId)
                        true
                    },
                )
            }
        }
}

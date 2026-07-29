package net.dodian.uber.game.ui

import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding
import net.dodian.uber.skills.api.SkillMultiSelection

object FletchingInterface : InterfaceButtonContent {
    private val longbowButtons = intArrayOf(34170, 34169, 34168, 34167)
    private val shortbowButtons = intArrayOf(34174, 34173, 34172, 34171)
    private val amountByButton = mapOf(34170 to 1, 34169 to 5, 34168 to 10, 34167 to 27, 34174 to 1, 34173 to 5, 34172 to 10, 34171 to 27)

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "fletching.bows.longbow", longbowButtons) { client, request ->
                val amount = amountByButton[request.rawButtonId] ?: return@buttonBinding false
                selectBow(client, 1, amount)
            },
            buttonBinding(-1, 1, "fletching.bows.shortbow", shortbowButtons) { client, request ->
                val amount = amountByButton[request.rawButtonId] ?: return@buttonBinding false
                selectBow(client, 0, amount)
            },
        )

    private fun selectBow(client: net.dodian.uber.game.model.entity.player.Client, entryIndex: Int, amount: Int): Boolean {
        val player = client.asSkillPlayer()
        val config = player.production.pending() ?: return false
        val entry = config.entries.getOrNull(entryIndex) ?: return false
        return player.production.select(SkillMultiSelection(config.key, entry.recipe.key, amount))
    }
}

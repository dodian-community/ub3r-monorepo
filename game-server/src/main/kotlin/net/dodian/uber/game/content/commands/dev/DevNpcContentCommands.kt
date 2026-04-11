package net.dodian.uber.game.content.commands.dev

import net.dodian.uber.game.engine.systems.interaction.commands.*

import net.dodian.uber.game.engine.systems.interaction.commands.CommandContent
import net.dodian.uber.game.engine.systems.interaction.commands.CommandContext
import net.dodian.uber.game.engine.systems.interaction.commands.commands
import net.dodian.uber.game.engine.systems.interaction.npcs.NpcClickMetrics

object DevNpcContentCommands : CommandContent {
    override fun definitions() =
        commands {
            command("npcclickmetrics", "npcmetrics") {
                handleNpcMetrics(this)
            }
        }
}

private fun handleNpcMetrics(context: CommandContext): Boolean {
    if (!context.specialRights) {
        return false
    }
    val decodeOption2 = NpcClickMetrics.count("decode.option.2")
    val fallbackOption2 = NpcClickMetrics.count("fallback.option.2")
    val staleQueue = NpcClickMetrics.count("queue.stale")
    val compatRejected = NpcClickMetrics.count("reject.compat230_disabled")
    context.reply(
        "NPC click metrics: decode.option.2=$decodeOption2 fallback.option.2=$fallbackOption2 queue.stale=$staleQueue compat230.disabled=$compatRejected",
    )
    return true
}

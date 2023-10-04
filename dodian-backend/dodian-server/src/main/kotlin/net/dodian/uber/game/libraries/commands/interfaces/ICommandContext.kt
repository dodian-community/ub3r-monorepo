package net.dodian.uber.game.libraries.commands.interfaces

import net.dodian.uber.game.libraries.commands.CommandArguments
import net.dodian.uber.game.modelkt.entity.player.Player

interface ICommandContext {
    val name: String
    val arguments: CommandArguments
    val sender: Player
}
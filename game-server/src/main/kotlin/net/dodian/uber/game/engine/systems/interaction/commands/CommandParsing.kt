package net.dodian.uber.game.engine.systems.interaction.commands

internal val numericToken = Regex(".*\\d.*")

internal fun CommandContext.tailFrom(index: Int): String = stringFrom(index)

internal fun CommandContext.playerNameTail(index: Int = 1): String = stringFrom(index)

package net.dodian.uber.game.libraries.commands

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import kotlin.reflect.KFunction3

typealias CommandArguments = List<String>
typealias CommandSuggestions = List<String>

typealias Player = Client